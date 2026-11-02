# Lab 09 — StockFlow: Build an Inventory API From Scratch, Then Ship It in Docker

> **Two parts, one project.** In **Part 1** you build **StockFlow** — the course's new running
> project, an inventory system with a JWT-secured API — from an empty directory, class by class. In
> **Part 2** you package it into a container image and run it, with its own PostgreSQL, using Docker
> Compose. Part 1 is the larger piece of work; Part 2 is the reason this lab exists.
>
> **Read first:** `COURSE_09_DOCKER.md` (in this folder) before starting Part 2. Part 1 uses only
> techniques from Labs 1–8; where it does, this document tells you exactly what to write. Section
> numbers of the course document are referenced below as **[C-§n]**.

|                    |                                                                                                                                                                                 |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Module**         | `_9_docker` (base package `com.example._9_docker`)                                                                                                                              |
| **Starting point** | an empty directory — nothing is copied from TaskFlow                                                                                                                            |
| **Stack**          | Spring Boot 4.0.6 · Spring Security 7.0.5 · Java 21 · Maven · PostgreSQL 16 (H2 in tests) · Docker Engine 28.x (Docker Desktop 4.44) with Compose v2.39                         |
| **Deliverables**   | the module, `./mvnw test` fully green (**167 tests**), a `Dockerfile`, a `.dockerignore`, a `compose.yaml`, and your own Postman collection run against the containerized stack |

---

## 1. Problem Statement & Goal

A small warehouse needs to know, at any moment, how many of each product it holds — and to trust that
number. Every stock count in a spreadsheet drifts from reality, because anyone can overwrite it.
StockFlow fixes that with one rule: **stock is never edited, only moved.** A product's quantity on hand
is the running total of an append-only ledger of movements — deliveries in, orders out — each one
recorded by a named clerk. The catalogue (categories, suppliers, products) is managed by an
administrator; recording movements is the clerks' job.

That gives Part 1 a domain with real rules — a movement that would take stock below zero is refused;
a category with products cannot be deleted; a product with history cannot be deleted — on top of the
security foundation you built in Labs 6–8.

Then the second problem. StockFlow "works on your machine" because your machine has a JDK, a Maven
cache, and a PostgreSQL with a known password. Nobody else's does. **Part 2** turns the project into a
single artifact — a container image — that runs identically anywhere Docker runs, and describes the
whole app-plus-database stack in one file so that one command brings it up. The proof at the end is
concrete: stock recorded into the containerized system survives the containers being destroyed and
recreated.

---

## 2. Learning Objectives (what this assesses)

**Part 1 — the from-scratch build**

- Assemble a complete layered Spring Boot 4 application from nothing: entities with relationships,
  repositories with derived and JPQL queries, DTOs with validation, services owning id-resolution and
  business rules, controllers with correct HTTP semantics, and RFC 7807 errors.
- Rebuild the stateless-JWT security foundation from memory of Labs 6–8: BCrypt users, a
  `UserDetailsService`, a `SecurityFilterChain` with URL rules, a `OncePerRequestFilter` for Bearer
  tokens, refresh-token rotation, method security, and 401/403 handlers.
- Model an **append-only ledger** whose entries are the single source of truth for a derived value.
- Refuse every business conflict with a **409**, decided in the service, so that a foreign-key or
  arithmetic accident never surfaces as a 500.
- Write the four test layers again — and put the method-security tests in the one layer where they
  run.

**Part 2 — containerization**

- Write a **multi-stage Dockerfile** that builds with a JDK, splits the Boot 4 layered jar with the
  `tools` jarmode, and ships a slim non-root JRE image.
- Prove the **layer cache** works, with build logs.
- Run the image standalone with **ports and environment variables**, then as a **Compose** stack
  with service-name DNS, a **healthcheck**, `depends_on` that waits for _healthy_, and a **named
  volume**.
- Demonstrate, with real data, the difference between `docker compose down` and `down -v`.

---

## 3. The Two Big Ideas

### 3.1 (Part 1) A ledger is the source of truth; the balance is derived

`Product.quantityOnHand` looks like an ordinary column. It is not: **no request can set it.** The
product API creates a product at zero and updates never touch the number. The only code that changes
it is `StockMovementServiceImpl.record`, which — inside one transaction — appends a ledger line and
adjusts the balance, so the two can never disagree. Every ledger line also stores `quantityAfter`, the
balance right after it was applied, which makes the history readable without replaying it.

This is why movements have no `PUT` or `DELETE`: a ledger you can edit is not a ledger. A mistake is
corrected by recording a compensating movement.

### 3.2 (Part 2) The image is the unit of deployment, and it contains no configuration

A Docker **image** is your application plus everything it needs to run, frozen. The same
`stockflow:1.0` must run on your laptop against a local Postgres, in Compose against a containerized
one, and later in CI and production — differing only in the **environment variables** injected when
it starts. That is what makes "works on my machine" go away: there is no _my machine_ left in the
artifact. [C-§1, C-§5.3]

---

## 4. The StockFlow Domain Model

```
User (username, email, BCrypt password, roles {CLERK|ADMIN})
  └── 1─N RefreshToken (opaque token, expiresAt, revoked)          ← the JWT foundation, unchanged from Lab 7/8

Category (name unique, description)   ┐
Supplier (name unique, email, phone)  ┘── each N─1 from ─┐
                                                         ▼
Product (sku unique, name, description, unitPrice, quantityOnHand, reorderLevel,
         category [mandatory], supplier [optional])
  └── 1─N StockMovement (type IN|OUT, quantity>0, reason, quantityAfter, occurredAt, recordedBy → User)
```

| Entity                 | Purpose                   | Rules                                                                                                                                                                                                                                   |
| ---------------------- | ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Category`             | groups products           | unique name (case-insensitive); **cannot be deleted while products reference it (409)**                                                                                                                                                 |
| `Supplier`             | where a product is bought | unique name; **cannot be deleted while products reference it (409)**                                                                                                                                                                    |
| `Product`              | a stocked item            | unique SKU; `unitPrice` is a `BigDecimal(12,2)`; `quantityOnHand` starts at 0 and **is never set by a request**; `lowStock = quantityOnHand <= reorderLevel`; supplier optional; **cannot be deleted once it has ledger history (409)** |
| `StockMovement`        | one ledger line           | append-only; `quantity > 0`; direction in `type`; OUT beyond stock on hand → **409**; `recordedBy` is always the principal; `occurredAt` stamped on persist                                                                             |
| `User`, `RefreshToken` | authentication            | as in Lab 7/8, roles renamed to `CLERK` / `ADMIN`                                                                                                                                                                                       |

No inverse (`@OneToMany`) collections are mapped anywhere. Nothing needs to walk from a category to
all its products, and an unmapped inverse side cannot be lazily loaded by accident in a list endpoint.

---

## 5. Tech Stack & Project Setup (required)

Create the module folder `_9_docker` with the standard Maven layout, and copy the Maven wrapper
(`mvnw`, `mvnw.cmd`, `.mvn/`) from any earlier module — or generate the project at start.spring.io
with the dependencies listed below and Java 21.

### 5.1 `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.0.6</version>
		<relativePath/>
	</parent>
	<groupId>com.example</groupId>
	<artifactId>_9_docker</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>_9_docker</name>
	<description>Lab 9 - StockFlow: Docker &amp; containerization fundamentals</description>
	<properties>
		<java.version>21</java.version>
		<postgresql.version>42.7.11</postgresql.version>
		<jjwt.version>0.13.0</jjwt.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<!-- jjwt 0.13.x is split: the API you compile against, plus the runtime implementation
		     and the Jackson bridge that serialises claims (Lab 7). -->
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-api</artifactId>
			<version>${jjwt.version}</version>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-impl</artifactId>
			<version>${jjwt.version}</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-jackson</artifactId>
			<version>${jjwt.version}</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<!-- Lab 9: Actuator, used ONLY for /actuator/health so a container platform can ask
		     "is this app alive?". The full treatment is Lab 15. -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<!-- Spring Boot 4 test slices are separate starters (each brings starter-test). -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<configuration>
					<annotationProcessorPaths>
						<path>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</path>
					</annotationProcessorPaths>
				</configuration>
			</plugin>
		</plugins>
	</build>
</project>
```

### 5.2 `src/main/resources/application.properties`

```properties
spring.application.name=stockflow

spring.datasource.url=jdbc:postgresql://localhost:5432/stockflow
spring.datasource.username=postgres
spring.datasource.password=123456

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Password for the ADMIN account seeded at startup by AdminUserSeeder (seeded credentials:
# admin / admin12345). Override in real deployments; the seeder only creates the user when absent.
app.security.admin-password=admin12345

# --- stateless JWT configuration (bound to JwtProperties) ---
# HS256 signing secret: Base64, at least 256 bits. This 48-byte value is a DEV DEFAULT only —
# inject a per-environment secret via APP_SECURITY_JWT_SECRET (Part 2 does exactly that).
app.security.jwt.secret=pp0gXRqpg69kqphvhFq5XK2JB4lxe8noQUoDG4RAslaDXvP/3idY/I+PV5M1EmNB
app.security.jwt.access-token-ttl=PT15M
app.security.jwt.refresh-token-ttl=P7D

# --- Lab 9: Actuator health, the one endpoint a container orchestrator needs ---
# Only /actuator/health is exposed. It answers {"groups":["liveness","readiness"],"status":"UP"} when
# the app AND its datasource are healthy, 503 otherwise. Lab 15 teaches the rest of Actuator.
management.endpoints.web.exposure.include=health
```

`src/test/resources/application.properties` contains only `spring.jpa.show-sql=true` and
`spring.jpa.properties.hibernate.format_sql=true`; the test slices auto-configure H2. Create the local
database once: `CREATE DATABASE stockflow;`.

### 5.3 `Application`

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 5.4 Spring Boot 4 / Spring Security 7 specifics you MUST know

These have all been taught (Labs 3, 7, 8); they are collected here because a from-scratch build hits
every one of them.

- **Test slices moved.** `@DataJpaTest` is `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`;
  `TestEntityManager` is `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`;
  `@WebMvcTest` is `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`. `@MockitoBean`
  is `org.springframework.test.context.bean.override.mockito.MockitoBean`.
- **Jackson 3.** `ObjectMapper` is `tools.jackson.databind.ObjectMapper`.
- **Removed in Spring Security 7:** `WebSecurityConfigurerAdapter`, `antMatchers`, `authorizeRequests`.
  Use a `SecurityFilterChain` bean with `authorizeHttpRequests` + `requestMatchers`.
- **The authorization annotations** live in `org.springframework.security.access.prepost`
  (`PreAuthorize`, `PostAuthorize`), not under `…authorization`. `AuthorizationDeniedException` is
  `org.springframework.security.authorization.AuthorizationDeniedException`. The filter chain's
  `AccessDeniedException` is `org.springframework.security.access.AccessDeniedException` — **not**
  `java.nio.file`'s.
- `SecurityConfig` also needs `org.springframework.http.HttpMethod` for the method-specific matchers.
- **`@EnableMethodSecurity`** (`org.springframework.security.config.annotation.method.configuration`)
  is required or every `@PreAuthorize` is inert; `prePostEnabled` defaults to true.
- **`-parameters`** is inherited from the parent POM, so `#recordedByUsername` in SpEL resolves.
- **Test-only imports** you will need beyond the slices:
  `org.springframework.test.context.junit.jupiter.SpringJUnitConfig`,
  `org.springframework.security.test.context.support.WithMockUser` and `WithAnonymousUser`,
  `org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers`,
  `org.springframework.security.authorization.AuthorizationDeniedException`.
- A `@WebMvcTest` slice does **not** load Boot's security auto-config: `@Import(SecurityConfig.class)`,
  `@MockitoBean UserDetailsService`, and a `MockMvc` built with `springSecurity()` (recipe in §10.2).

---

# PART 1 — Build StockFlow From Nothing

## 6. Part 1 — File Inventory

**Main sources (67 files)**

```
Application.java
config/      JwtProperties
controller/  AuthController, UserController, AdminController,
             CategoryController, SupplierController, ProductController
dto/         RegisterRequest, LoginRequest, RefreshRequest, TokenResponse, UserResponse,
             UserSummaryDto, PagedResponse,
             CategoryRequestDto, CategoryResponseDto, CategorySummaryDto,
             SupplierRequestDto, SupplierResponseDto, SupplierSummaryDto,
             ProductRequestDto, ProductResponseDto, ProductSummaryDto,
             StockMovementRequestDto, StockMovementResponseDto
exception/   ResourceNotFoundException, DuplicateResourceException, InvalidTokenException,
             ResourceInUseException, InsufficientStockException, GlobalExceptionHandler
model/       Role, MovementType, User, RefreshToken, Category, Supplier, Product, StockMovement
repository/  UserRepository, RefreshTokenRepository, CategoryRepository, SupplierRepository,
             ProductRepository, StockMovementRepository
security/    JwtService, JwtAuthenticationFilter, JwtAuthenticationEntryPoint,
             RestAccessDeniedHandler, UserDetailsServiceImpl, AdminUserSeeder, SecurityConfig
service/     AuthService(+Impl), RefreshTokenService(+Impl), UserService(+Impl),
             CategoryService(+Impl), SupplierService(+Impl), ProductService(+Impl),
             StockMovementService(+Impl)
```

**Tests (24 classes, 167 tests)** — inventoried in §10.

**Lombok house style, used everywhere below:** entities `@Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder`; DTOs `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` (the four
**catalogue request** DTOs also `@Builder`; the auth DTOs do not); services and controllers
`@RequiredArgsConstructor`. **Field order is normative:** the tests construct DTOs positionally through
`@AllArgsConstructor`, so declare fields in exactly the order the tables list them.

---

## 7. Part 1 — Complete Implementation Inventory (class-by-class)

### 7.1 `model` — two enums + six entities

**`Role`** — `enum { CLERK, ADMIN }`. A self-registered user is a CLERK; ADMIN is only ever seeded.

**`MovementType`** — `enum { IN, OUT }`. No `ADJUST`: an adjustment is an IN or OUT with a reason.

**`User`** — `@Entity @Table(name = "users")`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`.

| Field             | Mapping                                                                                                                                                                                                                            |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Long id`         | `@Id @GeneratedValue(strategy = IDENTITY)`                                                                                                                                                                                         |
| `String username` | `@Column(nullable = false, unique = true)`, `@EqualsAndHashCode.Include`                                                                                                                                                           |
| `String email`    | `@Column(nullable = false, unique = true)`                                                                                                                                                                                         |
| `String password` | `@Column(nullable = false)` — always a BCrypt hash                                                                                                                                                                                 |
| `Set<Role> roles` | `@Builder.Default = new HashSet<>()`, `@ElementCollection(fetch = EAGER)`, `@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))`, `@Enumerated(STRING)`, `@Column(name = "role", nullable = false)` |

**`RefreshToken`** — `@Table(name = "refresh_tokens")`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`:
`id`; `String token` (`nullable = false, unique = true`, `@EqualsAndHashCode.Include`);
`User user` (`@ManyToOne(fetch = EAGER, optional = false)`, `@JoinColumn(name = "user_id", nullable = false)`);
`Instant expiresAt` (`nullable = false`); `boolean revoked` (`@Builder.Default = false`, `nullable = false`).

**`Category`** — `@Table(name = "categories")`: `id`; `String name` (`nullable = false, unique = true,
length = 100`); `String description` (`length = 500`). No inverse collection.

**`Supplier`** — `@Table(name = "suppliers")`: `id`; `String name` (`nullable = false, unique = true,
length = 100`); `String email` (`length = 150`); `String phone` (`length = 40`). No inverse collection.

**`Product`** — `@Table(name = "products")`:

| Field                  | Mapping                                                                                             |
| ---------------------- | --------------------------------------------------------------------------------------------------- |
| `Long id`              | identity                                                                                            |
| `String sku`           | `@Column(nullable = false, unique = true, length = 40)`                                             |
| `String name`          | `@Column(nullable = false, length = 150)`                                                           |
| `String description`   | `@Column(length = 1000)`                                                                            |
| `BigDecimal unitPrice` | `@Column(nullable = false, precision = 12, scale = 2)` — money is never a `double`                  |
| `int quantityOnHand`   | `@Builder.Default = 0`, `nullable = false` — **changed only by `StockMovementServiceImpl.record`**  |
| `int reorderLevel`     | `@Builder.Default = 0`, `nullable = false`                                                          |
| `Category category`    | `@ManyToOne(fetch = LAZY, optional = false)`, `@JoinColumn(name = "category_id", nullable = false)` |
| `Supplier supplier`    | `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "supplier_id")` — nullable                          |

Plus one derived method, the low-stock rule written exactly once:

```java
public boolean isLowStock() {
    return quantityOnHand <= reorderLevel;
}
```

**`StockMovement`** — `@Table(name = "stock_movements")`:

| Field                | Mapping                                                                                                |
| -------------------- | ------------------------------------------------------------------------------------------------------ |
| `Long id`            | identity                                                                                               |
| `Product product`    | `@ManyToOne(fetch = LAZY, optional = false)`, `@JoinColumn(name = "product_id", nullable = false)`     |
| `MovementType type`  | `@Enumerated(STRING)`, `@Column(nullable = false, length = 8)`                                         |
| `int quantity`       | `nullable = false` — always positive                                                                   |
| `String reason`      | `@Column(length = 255)`                                                                                |
| `int quantityAfter`  | `nullable = false` — the product's balance right after this line                                       |
| `Instant occurredAt` | `@Column(nullable = false, updatable = false)`                                                         |
| `User recordedBy`    | `@ManyToOne(fetch = LAZY, optional = false)`, `@JoinColumn(name = "recorded_by_id", nullable = false)` |

with a lifecycle callback so no caller has to remember the timestamp:

```java
@PrePersist
void stampOccurredAt() {
    if (occurredAt == null) {
        occurredAt = Instant.now();
    }
}
```

### 7.2 `repository` — six interfaces

All extend `JpaRepository<Entity, Long>`. Every finder that feeds DTO mapping declares an
`@EntityGraph` over the LAZY `@ManyToOne` sides so a page maps without an N+1 (Lab 3).

**`UserRepository`** — `Optional<User> findByUsername(String)`, `boolean existsByUsername(String)`,
`boolean existsByEmail(String)`.

**`RefreshTokenRepository`** — `Optional<RefreshToken> findByToken(String)`.

**`CategoryRepository`** — `boolean existsByNameIgnoreCase(String name)`;
`boolean existsByNameIgnoreCaseAndIdNot(String name, Long id)` — the update-time duplicate check that
excludes the row being updated, so renaming a category to its own current name is not a duplicate.

**`SupplierRepository`** — the same two methods.

**`ProductRepository`**

```java
@Override @EntityGraph(attributePaths = {"category", "supplier"})
Page<Product> findAll(Pageable pageable);

@Override @EntityGraph(attributePaths = {"category", "supplier"})
Optional<Product> findById(Long id);

@EntityGraph(attributePaths = {"category", "supplier"})
Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

// The low-stock rule in JPQL — it mirrors Product.isLowStock(); keep the two in step.
@EntityGraph(attributePaths = {"category", "supplier"})
@Query("select p from Product p where p.quantityOnHand <= p.reorderLevel")
Page<Product> findLowStock(Pageable pageable);

@EntityGraph(attributePaths = {"category", "supplier"})
@Query("select p from Product p where p.category.id = :categoryId and p.quantityOnHand <= p.reorderLevel")
Page<Product> findLowStockByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

boolean existsBySkuIgnoreCase(String sku);
boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
boolean existsByCategoryId(Long categoryId);     // asked BEFORE a category delete → 409, not a 500
boolean existsBySupplierId(Long supplierId);     // asked BEFORE a supplier delete
```

**`StockMovementRepository`**

```java
@EntityGraph(attributePaths = {"product", "recordedBy"})
Page<StockMovement> findByProductId(Long productId, Pageable pageable);   // ordering comes from the Pageable

boolean existsByProductId(Long productId);       // asked BEFORE a product delete
```

### 7.3 `dto` — eighteen classes

Every constraint message is the `message = "…"` attribute; `@NotBlank`, `@NotNull`, `@Email` and
`@Positive` have no `value` attribute, so a short `@NotBlank(message = "…")` form would not compile.

**Authentication (unchanged from Lab 7):**

| Class              | Fields / constraints                                                                                                                                                                                                                                                                            |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `RegisterRequest`  | `username` `@NotBlank(message = "Username is required")`; `email` `@NotBlank(message = "Email is required")` `@Email(message = "Email must be a valid address")`; `password` `@NotBlank(message = "Password is required")` `@Size(min = 8, message = "Password must be at least 8 characters")` |
| `LoginRequest`     | `username` `@NotBlank(message = "Username is required")`; `password` `@NotBlank(message = "Password is required")`                                                                                                                                                                              |
| `RefreshRequest`   | `refreshToken` `@NotBlank(message = "Refresh token is required")`                                                                                                                                                                                                                               |
| `TokenResponse`    | `String accessToken, refreshToken, tokenType; long expiresIn`                                                                                                                                                                                                                                   |
| `UserResponse`     | `Long id; String username, email; Set<Role> roles` — **no password field**                                                                                                                                                                                                                      |
| `UserSummaryDto`   | `Long id; String username`                                                                                                                                                                                                                                                                      |
| `PagedResponse<T>` | `List<T> content; int page, size; long totalElements; int totalPages; boolean last` + `static <E,T> PagedResponse<T> from(Page<E> page, List<T> content)`                                                                                                                                       |

**Catalogue:**

| Class                                  | Fields / constraints                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `CategoryRequestDto` (`@Builder`)      | `name` `@NotBlank(message = "Name is required")` `@Size(max = 100, message = "Name must be at most 100 characters")`; `description` `@Size(max = 500, message = "Description must be at most 500 characters")`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `CategoryResponseDto`                  | `id, name, description`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `CategorySummaryDto`                   | `id, name`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `SupplierRequestDto` (`@Builder`)      | `name` `@NotBlank(message = "Name is required")` `@Size(max = 100, message = "Name must be at most 100 characters")`; `email` `@Email(message = "Email must be a valid address")` `@Size(max = 150, message = "Email must be at most 150 characters")`; `phone` `@Size(max = 40, message = "Phone must be at most 40 characters")`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `SupplierResponseDto`                  | `id, name, email, phone`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `SupplierSummaryDto`                   | `id, name`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `ProductRequestDto` (`@Builder`)       | `sku` `@NotBlank(message = "SKU is required")` `@Size(max = 40, message = "SKU must be at most 40 characters")`; `name` `@NotBlank(message = "Name is required")` `@Size(max = 150, message = "Name must be at most 150 characters")`; `description` `@Size(max = 1000, message = "Description must be at most 1000 characters")`; `BigDecimal unitPrice` `@NotNull(message = "Unit price is required")` `@DecimalMin(value = "0.00", message = "Unit price cannot be negative")` `@Digits(integer = 10, fraction = 2, message = "Unit price must have at most 10 integer and 2 decimal digits")`; `int reorderLevel` `@Min(value = 0, message = "Reorder level cannot be negative")` (missing → 0); `Long categoryId` `@NotNull(message = "Category id is required")`; `Long supplierId` (optional). **No `quantityOnHand`.** |
| `ProductResponseDto`                   | `id, sku, name, description, unitPrice, int quantityOnHand, int reorderLevel, boolean lowStock, CategorySummaryDto category, SupplierSummaryDto supplier`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `ProductSummaryDto`                    | `id, sku, name`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `StockMovementRequestDto` (`@Builder`) | `MovementType type` `@NotNull(message = "Type is required")`; `Integer quantity` `@NotNull(message = "Quantity is required")` `@Positive(message = "Quantity must be greater than zero")`; `reason` `@Size(max = 255, message = "Reason must be at most 255 characters")`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `StockMovementResponseDto`             | `id, type, int quantity, reason, int quantityAfter, Instant occurredAt, ProductSummaryDto product, UserSummaryDto recordedBy`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |

### 7.4 `exception` — five exceptions + the handler

| Class                        | Constructor → message                                                                                                                                  | Status |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | ------ |
| `ResourceNotFoundException`  | `(String resource, Long id)` → `"<resource> not found with id: <id>"`; `(String resource, String identifier)` → `"<resource> not found: <identifier>"` | 404    |
| `DuplicateResourceException` | `(String field, String value)` → `"<field> already in use: <value>"`                                                                                   | 409    |
| `InvalidTokenException`      | `(String message)`                                                                                                                                     | 401    |
| `ResourceInUseException`     | `(String resource, Long id, String referencedBy)` → `"<resource> <id> cannot be deleted: it is still referenced by <referencedBy>"`                    | 409    |
| `InsufficientStockException` | `(String sku, int onHand, int requested)` → `"Insufficient stock for <sku>: <onHand> on hand, <requested> requested"`                                  | 409    |

**`GlobalExceptionHandler`** — `@RestControllerAdvice`; every handler returns a `ProblemDetail`:

| Exception                             | Status | `title`                 | `detail`                                                                                                                    |
| ------------------------------------- | ------ | ----------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `AuthenticationException`             | 401    | `Authentication failed` | `Invalid username or password`                                                                                              |
| `InvalidTokenException`               | 401    | `Invalid token`         | the message                                                                                                                 |
| `AuthorizationDeniedException`        | 403    | `Access denied`         | `You do not have permission to perform this action.` (generic on purpose)                                                   |
| `ResourceNotFoundException`           | 404    | `Resource not found`    | the message                                                                                                                 |
| `DuplicateResourceException`          | 409    | `Duplicate value`       | the message                                                                                                                 |
| `ResourceInUseException`              | 409    | `Resource in use`       | the message                                                                                                                 |
| `InsufficientStockException`          | 409    | `Insufficient stock`    | the message                                                                                                                 |
| `MethodArgumentNotValidException`     | 400    | `Validation failed`     | `Request validation failed`, plus an `errors` property: `Map<field, message>` (`LinkedHashMap`, fallback `"Invalid value"`) |
| `HttpMessageNotReadableException`     | 400    | `Malformed request`     | `Request body is missing or malformed`                                                                                      |
| `MethodArgumentTypeMismatchException` | 400    | `Invalid parameter`     | `Parameter '<name>' has an invalid value`                                                                                   |
| `Exception`                           | 500    | `Internal server error` | `An unexpected error occurred`                                                                                              |

The `HttpMessageNotReadableException` entry is new in this lab: without it, a body that is not valid
JSON (or `"type":"SIDEWAYS"`) falls into the catch-all and is reported as a 500 — a crash for the
client's mistake. The 403 handler and the three 409 handlers exist for the same reason. (Spring picks
the handler by exception-type specificity, not by declaration order — the ordering above is for
readability.)

### 7.5 `config/JwtProperties`

`@ConfigurationProperties(prefix = "app.security.jwt")`, `@Getter @Setter`: `String secret` (default =
the dev value from §5.2), `Duration accessTokenTtl = Duration.ofMinutes(15)`,
`Duration refreshTokenTtl = Duration.ofDays(7)`. Registered via
`@EnableConfigurationProperties(JwtProperties.class)` on `SecurityConfig`, so it exists in the
`@WebMvcTest` slices too.

### 7.6 `security` — seven classes

**`JwtService`** — a plain class (declared as a `@Bean` in `SecurityConfig`). Constructor takes
`JwtProperties`; derives `SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))` and keeps
`accessTokenTtl`.

```java
public String generateAccessToken(String username, Collection<String> roles)   // sub, "roles" claim, iat, exp; signWith(key, Jwts.SIG.HS256)
public boolean isValid(String token)          // parse; false on JwtException | IllegalArgumentException
public String extractUsername(String token)   // subject
public List<String> extractRoles(String token)// the "roles" claim as strings (empty list if absent)
public Instant extractExpiration(String token)
public long getAccessTokenTtlSeconds()
```

Parsing: `Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload()`.

**`JwtAuthenticationFilter extends OncePerRequestFilter`** — `@RequiredArgsConstructor(JwtService)`.
In `doFilterInternal`: read `Authorization`; if it starts with `"Bearer "` **and** the
`SecurityContext` holds no `Authentication` yet, take the token; if `jwtService.isValid(token)`, build
`SimpleGrantedAuthority("ROLE_" + role)` for each role in the claim, create a
`UsernamePasswordAuthenticationToken(username, null, authorities)`, set details from
`new WebAuthenticationDetailsSource().buildDetails(request)`, and put it in the context. Always
`chain.doFilter(request, response)` — an invalid token is not rejected here; it simply leaves the
request anonymous for the authorization layer to refuse.

**`JwtAuthenticationEntryPoint implements AuthenticationEntryPoint`** — writes status 401,
`Content-Type: application/problem+json`, body
`{"type":"about:blank","title":"Unauthorized","status":401,"detail":"Authentication required: a valid Bearer access token is missing or invalid."}`.
Written by hand (no `ObjectMapper`) so it behaves identically in every slice.

**`RestAccessDeniedHandler implements AccessDeniedHandler`** — status 403, same content type, body
`{"type":"about:blank","title":"Access denied","status":403,"detail":"You do not have permission to perform this action."}`
— the same `title`/`status`/`detail` as the advice's `AuthorizationDeniedException` handler, so a
URL-rule 403 and a method-rule 403 look alike to a client.

**`UserDetailsServiceImpl implements UserDetailsService`** — `@Service`; loads the `User` by username
(`UsernameNotFoundException("User not found: " + username)` if absent) and returns
`new org.springframework.security.core.userdetails.User(username, passwordHash, authorities)` where
each role maps to `SimpleGrantedAuthority("ROLE_" + role.name())`.

**`AdminUserSeeder implements ApplicationRunner`** — a plain `@Component` (so test slices never run
it), `@Slf4j`; injects `UserRepository`, `PasswordEncoder`, and
`@Value("${app.security.admin-password:admin12345}") String adminPassword`. If no user `admin` exists,
saves one with email `admin@stockflow.local`, the BCrypt-encoded password, and roles `{ADMIN}`, and
logs it. Idempotent.

**`SecurityConfig`** — `@Configuration @EnableWebSecurity @EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)`. Beans: `PasswordEncoder`
(`BCryptPasswordEncoder`), `JwtService`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`,
`RestAccessDeniedHandler`, `AuthenticationManager` (from `AuthenticationConfiguration`), and the
chain:

```java
http
    .csrf(csrf -> csrf.disable())                                  // Bearer API: no cookie, no CSRF
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/actuator/health").permitAll()      // the liveness question, no token
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            // ORDER MATTERS: this must sit ABOVE the ADMIN rule for POST /api/products/**.
            // First matching rule wins; the broader pattern would refuse every clerk.
            .requestMatchers(HttpMethod.POST, "/api/products/*/movements").authenticated()
            .requestMatchers(HttpMethod.POST,   "/api/categories/**", "/api/suppliers/**", "/api/products/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT,    "/api/categories/**", "/api/suppliers/**", "/api/products/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/suppliers/**", "/api/products/**").hasRole("ADMIN")
            .anyRequest().authenticated())
    .exceptionHandling(ex -> ex
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)   // 401
            .accessDeniedHandler(restAccessDeniedHandler))           // 403
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
return http.build();
```

### 7.6.1 Guided implementation walkthrough — follow one request through JWT security

The inventory above is the contract; this subsection turns the JWT part into an implementation
sequence. Work top-to-bottom and compile after each class. Keep this request path in mind:

1. A client logs in. `AuthServiceImpl` checks the password, then `JwtService` signs an access token
   containing the username and role names.
2. The client sends `Authorization: Bearer <access-token>` on a later request.
3. `JwtAuthenticationFilter` verifies the token and, if valid, places an authenticated principal and
   authorities into Spring's `SecurityContext`.
4. The authorization rules decide whether that principal may call the endpoint. If not authenticated,
   Spring calls the 401 entry point; if authenticated but forbidden, it calls the 403 handler.

An access token is **signed, not encrypted**: its claims can be read by its holder. Never put a
password or other secret in its claims. The signature proves the claims were issued by someone with
the server's key and have not been changed; the expiration limits how long the token is accepted.

#### A. `JwtProperties`: get configuration into Java

1. Add `@ConfigurationProperties(prefix = "app.security.jwt")` and mutable `secret`,
   `accessTokenTtl`, and `refreshTokenTtl` properties with Lombok getters/setters.
2. Give the two durations the documented defaults. Spring converts values such as `PT15M` and `P7D`
   from `application.properties` into `Duration` objects.
3. Register the class using `@EnableConfigurationProperties(JwtProperties.class)` on
   `SecurityConfig`. `JwtService` should receive this object; it should not read property strings
   directly.
4. Check: constructing `JwtService` with the configured secret should succeed. A malformed or
   undersized signing key should fail at startup rather than silently issue weak tokens.

#### B. `JwtService`: issue and read access tokens

Implement the constructor and methods in this order:

1. **Constructor:** Base64-decode `properties.getSecret()`, pass the bytes to
   `Keys.hmacShaKeyFor(...)`, and retain the resulting `SecretKey`. Also retain
   `properties.getAccessTokenTtl()`. This gives every method the same key and expiry policy.
2. **Private `parse(token)` helper:** build a JJWT parser, configure it with
   `.verifyWith(key)`, build it, parse **signed claims**, and return the payload. Centralizing parsing
   means every extraction method verifies the signature and expiration the same way.
3. **`generateAccessToken(username, roles)`:** capture `Instant.now()` once; calculate expiry by
   adding the access-token TTL; set the subject to the username, the `roles` claim to a copied list,
   `iat` to the captured time, and `exp` to the calculated time; sign with the key and HS256; return
   the compact token string. Do not put the `ROLE_` prefix in this claim: login passes bare names such
   as `CLERK` and `ADMIN`.
4. **`isValid(token)`:** call `parse(token)` inside a `try`; return `true` only if parsing succeeds.
   Catch `JwtException` and `IllegalArgumentException` and return `false`. JJWT rejects malformed,
   tampered, wrong-key, and expired tokens while parsing. This method is deliberately a predicate:
   callers do not need exception handling for ordinary invalid client tokens.
5. **Claim readers:** call `parse(token)` and return the subject (`extractUsername`), the `roles`
   claim converted to `List<String>` (`extractRoles`, or an empty list if absent/not a collection),
   and the expiration converted to `Instant` (`extractExpiration`). These methods expect a valid
   token; the filter checks `isValid` before calling them.
6. **TTL reader:** return `accessTokenTtl.toSeconds()` for the `expiresIn` field of the login
   response.

Checkpoint: generate a token for `bob` with `List.of("CLERK")`; `isValid` should be true, the
subject and roles should round-trip, and changing the signature should make `isValid` false.

#### C. `JwtAuthenticationFilter.doFilterInternal`: turn a Bearer token into a principal

This method does not decide whether the URL is allowed, and it does not write an error response.
Its job is only to establish authentication when it can. The authorization layer and the configured
401/403 handlers make the access decision afterward.

Use this exact sequence:

1. Declare the override with the servlet exceptions in its signature:
   `throws ServletException, IOException`. Read the header with
   `request.getHeader("Authorization")`.
2. Inspect `SecurityContextHolder.getContext().getAuthentication()`. Only attempt Bearer
   authentication when there is no authentication already present; do not replace an authentication
   established by an earlier filter.
3. Confirm the header is non-null and starts with the exact prefix `Bearer ` (including the space).
   If it does, remove that prefix to get the token. If it is absent or uses another scheme, do not
   try to parse it.
4. Call `jwtService.isValid(token)`. Only on `true`, extract the username and roles. Map each bare
   role to `new SimpleGrantedAuthority("ROLE_" + role)`: Spring's `hasRole("CLERK")` checks for the
   authority `ROLE_CLERK`.
5. Create `new UsernamePasswordAuthenticationToken(username, null, authorities)`. The three-argument
   constructor represents an authenticated principal; the password is null because this request is
   authenticated by the already-verified token, not by resubmitting a password.
6. Attach request metadata with
   `authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request))`, then
   install it using `SecurityContextHolder.getContext().setAuthentication(authentication)`.
7. At the end, call `filterChain.doFilter(request, response)` exactly once, whether the header was
   absent, malformed, invalid, or valid. In particular, an invalid token should leave the request
   anonymous and continue; the protected URL then produces the standard 401. Returning early here
   would skip the rest of Spring Security and the controller chain.

Imports to look up while implementing this class are `SecurityContextHolder`,
`UsernamePasswordAuthenticationToken`, `SimpleGrantedAuthority`,
`WebAuthenticationDetailsSource`, `FilterChain`, and the servlet request/response types. The full
override signature is `protected void doFilterInternal(HttpServletRequest request,
HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException`.

Checkpoint: a valid CLERK token reaches a protected read and has `ROLE_CLERK`; a missing, expired, or
tampered token remains anonymous and receives 401; a valid CLERK token on an ADMIN-only write receives 403. These are the behaviors covered by `JwtAuthenticationFilterTest`.

#### D. The 401 and 403 handlers: convert security failures to API responses

`JwtAuthenticationEntryPoint.commence(request, response, authException)` handles a request with no
usable authentication:

1. Set the response status to 401.
2. Set content type to `application/problem+json`.
3. Write the documented fixed Problem Detail JSON body to the response output stream using UTF-8.
   This handler is intentionally independent of Jackson, so it works in security test slices.

`RestAccessDeniedHandler.handle(request, response, accessDeniedException)` follows the same three
steps with status 403 and the generic access-denied body. Keep it generic: do not disclose which role
or rule caused the denial. These handlers cover URL authorization failures; method-security denials
are rendered by `GlobalExceptionHandler` and should have the same public title/detail.

#### E. `UserDetailsServiceImpl`: connect database users to password authentication

1. Ask `UserRepository.findByUsername(username)` for the stored user.
2. If absent, throw `UsernameNotFoundException` with the username in the message. Spring's
   authentication flow uses this to reject login.
3. Convert each `Role` to `SimpleGrantedAuthority("ROLE_" + role.name())`.
4. Return Spring Security's `User` with the database username, stored BCrypt hash, and authorities.
   Do not return the application's JPA `User` entity as the security principal.

`AuthenticationManager.authenticate(...)` uses this service and the configured `PasswordEncoder`;
the manager compares the submitted password with the stored BCrypt hash.

#### F. How login, refresh, and the filter fit together

- **Login:** `AuthServiceImpl.login` submits username/password to the `AuthenticationManager`. If it
  succeeds, collect the granted authorities, strip `ROLE_` to produce bare claim names, call
  `generateAccessToken`, load the application `User`, create a refresh-token row, and return both
  tokens plus the access-token TTL. Let `BadCredentialsException` propagate to the 401 advice.
- **Refresh-token creation:** generate an unpredictable opaque value (UUID is sufficient for this
  lab), set its owning user and `expiresAt = now + refreshTokenTtl`, set `revoked` false, and save it.
  A refresh token is database-backed and is not the signed JWT access token.
- **Refresh verification:** look up the presented token; reject unknown, revoked, or expired tokens
  with `InvalidTokenException`. Compare the expiry to the current instant. Keep this read operation
  read-only.
- **Rotation:** verify first; mark the old row revoked and save it; create a new token for the same
  user. This makes each refresh token single-use. The refresh endpoint then issues a fresh access
  token for that user and returns the newly rotated refresh token.
- **Registration:** check username and email uniqueness before saving; hash the password with
  `PasswordEncoder`; assign only `CLERK`; never return the password/hash in `UserResponse`.

Checkpoint: login failure must create neither token; refreshing an expired/revoked/unknown token
must create no replacement; successful refresh must return a different refresh token and a new access
token.

#### G. `SecurityConfig`: wire the pieces in a safe order

Configure the beans first, then the filter chain:

1. Register `JwtProperties`; expose a `BCryptPasswordEncoder` as `PasswordEncoder`.
2. Expose `JwtService`, the JWT filter, the 401 entry point, and the 403 handler as beans. Obtain the
   `AuthenticationManager` from `AuthenticationConfiguration` so it uses Spring's configured user
   details service and password encoder.
3. Enable method security. Without `@EnableMethodSecurity`, service `@PreAuthorize` annotations are
   ordinary metadata and do not enforce anything.
4. Disable CSRF for this stateless Bearer-token API and set session creation policy to `STATELESS`.
5. Add URL rules from most specific to broad: public auth and health; admin URLs; the authenticated
   movement POST; catalogue write methods for ADMIN; then `anyRequest().authenticated()`. The
   movement POST rule must precede the broader `/api/products/**` ADMIN write rule, or clerks cannot
   record stock.
6. Attach the entry point and access-denied handler, then add the JWT filter before
   `UsernamePasswordAuthenticationFilter` so the context is populated before authorization runs.
7. Build and return the `SecurityFilterChain`.

Checkpoint: test these separately: anonymous protected request → 401; authenticated CLERK read →
allowed; CLERK catalogue write → 403; ADMIN catalogue write → allowed; CLERK movement POST → allowed.
The URL rules prove HTTP behavior; `MethodSecurityTest` separately proves service annotations.

### 7.7 `service` — seven interface + implementation pairs

Every implementation is `@Service @RequiredArgsConstructor @Transactional(readOnly = true)` with
`@Transactional` on writes — except `RefreshTokenServiceImpl`, which annotates per method
(`@Transactional` on `create`/`rotate`, `readOnly = true` on `verify`); mapping is manual in a private
`toResponse`.

**`AuthService` / `AuthServiceImpl`** — injects `UserRepository, PasswordEncoder,
AuthenticationManager, JwtService, RefreshTokenService` (in that order).

- `UserResponse register(RegisterRequest)` — 409 `DuplicateResourceException("username", …)` /
  `("email", …)`; saves with the BCrypt-encoded password and roles `{CLERK}`.
- `TokenResponse login(LoginRequest)` — `authenticationManager.authenticate(new
UsernamePasswordAuthenticationToken(username, password))` (a `BadCredentialsException` propagates →
  401); collects the authorities, **strips the `ROLE_` prefix** to get bare role names as a
  `List<String>` (`.toList()` — the test captures a `List`); `jwtService.generateAccessToken(username,
roles)`; loads the `User` by username (404 `("User", username)` if the row is somehow missing);
  `refreshTokenService.create(user)`; returns `TokenResponse(access, refresh.getToken(), "Bearer",
jwtService.getAccessTokenTtlSeconds())`.
- `TokenResponse refresh(RefreshRequest)` — `refreshTokenService.rotate(token)`; new access token for
  the rotated token's user (roles from `user.getRoles()`); returns the **rotated** refresh token.

**`RefreshTokenService` / `RefreshTokenServiceImpl`** — injects `RefreshTokenRepository, JwtProperties`.
`create(User)` → UUID token, `expiresAt = now + refreshTokenTtl`, `revoked = false`.
`verify(String)` → 401 `InvalidTokenException` with `"Refresh token is not recognized"` /
`"…has been revoked"` / `"…has expired"`. `rotate(String)` → `verify`, mark the presented token revoked
and save, then `create` for the same user (single-use rotation).

**`UserService` / `UserServiceImpl`** — injects `UserRepository`.

- `@PreAuthorize("hasRole('ADMIN') or #username == authentication.name") UserResponse getByUsername(String username)` — 404 if absent.
- `UserResponse getCurrentUser()` — reads the principal from `SecurityContextHolder` (404 `("User",
"current")` if none) and loads it **through a private helper, not through `getByUsername`** — an
  internal call would bypass the proxy and silently skip the annotation (Lab 8's self-invocation trap).
- `@PreAuthorize("hasRole('ADMIN')") List<UserResponse> getAllUsers()`.

**`CategoryService` / `CategoryServiceImpl`** — injects `CategoryRepository, ProductRepository`.
Interface: `CategoryResponseDto create(CategoryRequestDto)`, `PagedResponse<CategoryResponseDto>
getAll(Pageable)`, `CategoryResponseDto getById(Long)`, `CategoryResponseDto update(Long,
CategoryRequestDto)`, `void delete(Long)`.

| Method                             | Annotation                                         | Rules                                                                                                                                                    |
| ---------------------------------- | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `create(CategoryRequestDto)`       | `@Transactional @PreAuthorize("hasRole('ADMIN')")` | `existsByNameIgnoreCase` → 409 `("category name", name)`                                                                                                 |
| `getAll(Pageable)`                 | —                                                  | maps `findAll(pageable)` into `PagedResponse`                                                                                                            |
| `getById(Long)`                    | —                                                  | 404 `("Category", id)`                                                                                                                                   |
| `update(Long, CategoryRequestDto)` | `@Transactional @PreAuthorize("hasRole('ADMIN')")` | 404; `existsByNameIgnoreCaseAndIdNot(name, id)` → 409; sets name + description; **then `save` and map the returned entity** (the unit test stubs `save`) |
| `delete(Long)`                     | `@Transactional @PreAuthorize("hasRole('ADMIN')")` | `existsById` else 404; **`productRepository.existsByCategoryId(id)` → 409 `ResourceInUseException("Category", id, "products")`**; then `deleteById`      |

**`SupplierService` / `SupplierServiceImpl`** — the same five signatures with `Supplier…Dto` types;
identical shape with `("supplier name", …)`,
`existsBySupplierId`, and `ResourceInUseException("Supplier", id, "products")`; `update` sets name,
email, phone, then `save`s and maps the returned entity.

**`ProductService` / `ProductServiceImpl`** — injects `ProductRepository, CategoryRepository,
SupplierRepository, StockMovementRepository` (in that order). Interface: `ProductResponseDto
create(ProductRequestDto)`, `PagedResponse<ProductResponseDto> getAll(Long categoryId, boolean
lowStock, Pageable)`, `ProductResponseDto getById(Long)`, `ProductResponseDto update(Long,
ProductRequestDto)`, `void delete(Long)`.

| Method                                                | Annotation                                         | Rules                                                                                                                                                                                                                                                                                                                                                                                                          |
| ----------------------------------------------------- | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `create(ProductRequestDto)`                           | `@Transactional @PreAuthorize("hasRole('ADMIN')")` | `existsBySkuIgnoreCase` → 409 `("sku", sku)` **before** any lookup; resolve category (404 `("Category", id)`); resolve supplier only if `supplierId != null` (404 `("Supplier", id)`); build with `quantityOnHand` left at 0                                                                                                                                                                                   |
| `getAll(Long categoryId, boolean lowStock, Pageable)` | —                                                  | four branches: both → `findLowStockByCategoryId`; category only → `findByCategoryId`; lowStock only → `findLowStock`; neither → `findAll`                                                                                                                                                                                                                                                                      |
| `getById(Long)`                                       | —                                                  | 404 `("Product", id)`                                                                                                                                                                                                                                                                                                                                                                                          |
| `update(Long, ProductRequestDto)`                     | `@Transactional @PreAuthorize("hasRole('ADMIN')")` | 404; `existsBySkuIgnoreCaseAndIdNot(sku, id)` → 409; sets sku, name, description, unitPrice, reorderLevel; **re-resolves the category via `categoryRepository.findById` on every update** (404 if unknown) and the supplier the same way — the supplier repository is **never called when `supplierId` is null** (null clears it); **never touches `quantityOnHand`**; then `save` and map the returned entity |
| `delete(Long)`                                        | `@Transactional @PreAuthorize("hasRole('ADMIN')")` | `existsById` else 404; **`stockMovementRepository.existsByProductId(id)` → 409 `("Product", id, "stock movements")`** — history is never cascaded away                                                                                                                                                                                                                                                         |

`toResponse` maps `p.isLowStock()` into `lowStock`, and nested `CategorySummaryDto` /
`SupplierSummaryDto` (null supplier → null).

**`StockMovementService` / `StockMovementServiceImpl`** — injects `StockMovementRepository,
ProductRepository, UserRepository` (in that order). **The only code that changes stock.**

```java
@Override
@Transactional
@PreAuthorize("isAuthenticated() and #recordedByUsername == authentication.name")
public StockMovementResponseDto record(Long productId, StockMovementRequestDto request,
        String recordedByUsername) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    User recorder = userRepository.findByUsername(recordedByUsername)
            .orElseThrow(() -> new ResourceNotFoundException("User", recordedByUsername));

    int quantity = request.getQuantity();
    int after;
    if (request.getType() == MovementType.IN) {
        after = product.getQuantityOnHand() + quantity;
    } else {
        if (quantity > product.getQuantityOnHand()) {
            throw new InsufficientStockException(product.getSku(), product.getQuantityOnHand(), quantity);
        }
        after = product.getQuantityOnHand() - quantity;
    }

    product.setQuantityOnHand(after);
    productRepository.save(product);

    StockMovement movement = StockMovement.builder()
            .product(product).type(request.getType()).quantity(quantity)
            .reason(request.getReason()).quantityAfter(after).recordedBy(recorder)
            .build();
    return toResponse(stockMovementRepository.save(movement));
}
```

Both writes happen in one transaction: a ledger line without the matching balance change is
impossible. The `@PreAuthorize` pins the recorder to the principal (the controller passes
`Principal.getName()`), exactly as TaskFlow pinned a project's owner. `getByProduct(Long productId,
Pageable)` — `productRepository.existsById(productId)` else 404 `("Product", id)` (an `existsById`
probe, **not** `findById` — the unit test stubs `existsById`), then maps `findByProductId`.

### 7.8 `controller` — six controllers

All use `ResponseEntity`, `@Valid`, and `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")`
for `Location` on create. **No authorization logic lives in a controller.**

| Controller                             | Endpoints                                                                                                                                                                                                                                                                                                                                                                                                  |
| -------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `AuthController` `/api/auth`           | `POST /register` → 201 `UserResponse`; `POST /login` → 200 `TokenResponse`; `POST /refresh` → 200 `TokenResponse`                                                                                                                                                                                                                                                                                          |
| `UserController` `/api/users`          | `GET /me` → 200; `GET /{username}` → 200 (a literal `/me` segment outranks the template)                                                                                                                                                                                                                                                                                                                   |
| `AdminController` `/api/admin`         | `GET /users` → 200 `List<UserResponse>`                                                                                                                                                                                                                                                                                                                                                                    |
| `CategoryController` `/api/categories` | `POST` 201 + `Location`; `GET` paged (`@PageableDefault(size = 20, sort = "name")`); `GET /{id}`; `PUT /{id}`; `DELETE /{id}` 204                                                                                                                                                                                                                                                                          |
| `SupplierController` `/api/suppliers`  | same five, `sort = "name"`                                                                                                                                                                                                                                                                                                                                                                                 |
| `ProductController` `/api/products`    | the five (`sort = "sku"`; list takes `@RequestParam(required = false) Long categoryId` and `@RequestParam(defaultValue = "false") boolean lowStock`) **plus** the nested ledger: `POST /{id}/movements` (`@Valid StockMovementRequestDto`, `Principal`) → **201 with no `Location`**; `GET /{id}/movements` paged with `@PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)` |

Why the movement `POST` returns 201 without a `Location`: every other create in the course points
`Location` at the new resource's own URL, but a ledger line is not individually addressable in this
API — it is read only as part of its product's history. A `Location` that 404s would be worse than
none; the body already carries the new balance.

### 7.9 Guided implementation walkthrough — the rest of Part 1

Section 7 above is the exact contract. Use the recipes below to turn that contract into code. Keep
each layer doing one job: entities describe persisted state; repositories fetch it; services enforce
rules and map entities to DTOs; controllers translate HTTP into service calls. Controllers should
not contain business decisions, and DTOs should not contain entities.

#### A. Bootstrap and model classes

1. **Application setup:** create the `@SpringBootApplication` class in the root package and make its
   `main` method call `SpringApplication.run`. Copy the properties from §5.2, then compile before
   adding domain logic. The datasource settings belong in properties/environment, not Java code.
2. **Enums:** define exactly `Role { CLERK, ADMIN }` and `MovementType { IN, OUT }`. Persist enums as
   strings so the database does not depend on enum ordinal positions.
3. **Build each entity from identity outward:** add `@Entity`, the specified table name, an identity
   `Long id`, then the scalar fields and column constraints from §7.1. Add the Lombok annotations
   listed in §6. Avoid `@Data` on entities: generated `equals`, `hashCode`, and `toString` can traverse
   lazy relationships or recurse.
4. **Map only owning sides:** put `@ManyToOne` and `@JoinColumn` on `Product.category`,
   `Product.supplier`, `StockMovement.product`, `StockMovement.recordedBy`, and
   `RefreshToken.user`. Do not add inverse collections. Use `LAZY` for catalogue/history links and
   `EAGER` only for the user roles and refresh-token user as specified.
5. **Preserve defaults with Lombok builders:** annotate `roles`, `quantityOnHand`, `reorderLevel`,
   and `revoked` with both `@Builder.Default` and their initial value; otherwise Lombok's builder
   ignores the field initializer and may produce null/zero/false unexpectedly.
6. **Keep inventory invariants in the model:** implement `Product.isLowStock()` as the single Java
   expression `quantityOnHand <= reorderLevel`. In `StockMovement`, add `@PrePersist`; only set
   `occurredAt` when it is null, so explicitly supplied historical timestamps in tests remain intact.
7. **Checkpoint:** persist a product with its required category and optional supplier; clear the
   persistence context and reload it. Persist a movement with product and recorder and verify the
   generated timestamp. This catches mapping errors before service code depends on the entities.

#### B. Repository interfaces

1. Make each interface extend `JpaRepository<Entity, Long>`; do not write CRUD implementations.
   Spring Data generates standard persistence methods.
2. Add simple derived methods first. Read each method name as a query: for example,
   `existsByNameIgnoreCaseAndIdNot(name, id)` means case-insensitive name match while excluding one
   id. Keep parameter names/types aligned with the property path.
3. Add `@EntityGraph` to product and movement reads that are immediately mapped to nested DTOs.
   Include every required LAZY relation in `attributePaths`; otherwise mapping a page can cause
   N+1 queries or access a detached proxy.
4. Add product low-stock queries with JPQL against entity/property names—not table/column names—and
   make the category-filtered version combine both predicates. Keep these predicates identical to
   `Product.isLowStock()`.
5. Pass `Pageable` into paged queries and let its sort determine order. Do not sort the returned page
   in Java. For movement history, the controller supplies `occurredAt` descending.
6. Add the `existsBy...` probes used before deletes. The service needs an explicit answer before
   deleting a referenced row; relying on a database constraint would turn a normal conflict into a
7.
8. **Checkpoint:** run the repository tests in §10.3 before implementing services. A misspelled
   derived property or JPQL path should fail here, close to its cause.

#### C. DTOs and exception classes

1. Declare DTO fields in exactly the §7.3 order because the tests use Lombok's positional
   `@AllArgsConstructor`. Add getters/setters and the requested no-argument/all-argument
   constructors; only catalogue request DTOs also get `@Builder`.
2. Put Bean Validation constraints on request DTO fields, using the exact `message` text in §7.3.
   Keep `quantityOnHand`, `recordedBy`, roles, and IDs resolved from the authenticated principal out
   of client-controlled request fields. In particular, `ProductRequestDto` must not contain a stock
   balance.
3. Keep response DTOs deliberately flat: nested category/supplier/product/user summaries expose only
   the id and display fields listed in the inventory. Never expose a password or password hash.
4. Implement `PagedResponse.from(page, content)` by copying the mapped content and the page's number,
   size, total element count, total page count, and last-page flag. Use the `Page` metadata rather
   than recalculating totals from the current page.
5. Give each domain exception the constructor and exact message from §7.4. Extend
   `RuntimeException`; constructors should only format and pass the message to `super(...)`, not
   choose HTTP status codes.
6. Implement each `GlobalExceptionHandler` method as a translation: create a `ProblemDetail` with
   the required status, set its title and detail, and return it. For validation errors, iterate field
   errors into a `LinkedHashMap` (preserving order), use `"Invalid value"` if a message is null, and
   add that map as the `errors` property. Handle malformed JSON/type conversion separately from the
   generic 500 fallback.
7. **Checkpoint:** send invalid DTOs and malformed JSON through a controller slice. They should
   return 400 and the documented body; do not let client input fall into the catch-all 500 handler.

#### D. Authentication and user service methods

The security wiring details are in §7.6.1. Use these service recipes after those collaborators are
available:

1. **`AuthServiceImpl.register`:** check username first, then email; throw the matching
   `DuplicateResourceException` before saving. Build a user with the submitted username/email,
   BCrypt-encode the password, and assign only `{CLERK}`. Save, then map only id, username, email,
   and roles to `UserResponse`.
2. **`AuthServiceImpl.login`:** authenticate the submitted credentials first. On success, obtain
   authorities from the returned authentication and remove the leading `ROLE_` from each name; keep
   the result as a `List<String>`. Generate the access token, load the matching application `User`,
   create a refresh token, and return both tokens, `Bearer`, and the access-token TTL in seconds. Do
   not catch `BadCredentialsException` here; the exception handler maps it to 401. A failed login
   must not issue either token.
3. **`AuthServiceImpl.refresh`:** rotate the supplied refresh token; take the returned token's user;
   generate a new access token from that user's role names; return the new access token and the
   rotated refresh token. Never return the old refresh token as usable.
4. **`RefreshTokenServiceImpl.create`:** make a new opaque UUID token, associate the user, set
   expiry to the current instant plus configured refresh TTL, leave it unrevoked, and save it.
   **`verify`:** find by token, then check unknown → revoked → expired and throw the corresponding
   `InvalidTokenException`. **`rotate`:** call verify, revoke and save the old row, then call create
   for its user. Keep rotation transactional so the old and new state change together.
5. **`UserServiceImpl.getByUsername`:** load by username or throw `ResourceNotFoundException`; map to
   `UserResponse`. Leave the method-level rule on the public method. **`getCurrentUser`:** read the
   current authentication name from `SecurityContextHolder`, handle absent/anonymous principal as
   the specified not-found case, and use a private lookup helper rather than calling the annotated
   method from inside the same class. **`getAllUsers`:** fetch all users and map each one; its
   `@PreAuthorize` is ADMIN-only.
6. **Checkpoint:** unit-test positive flows and failure short-circuiting. For each duplicate/invalid
   credential/token case, verify downstream saves or token issuance did not happen.

#### E. Catalogue services: categories and suppliers

Implement `CategoryServiceImpl` first, then mirror its shape in `SupplierServiceImpl` with the
supplier fields/repository methods:

1. **Create:** check the case-insensitive name before saving; throw 409 on duplicate; build/save the
   entity and map the saved entity to the response DTO. Mark it transactional and ADMIN-only.
2. **Get all:** call `findAll(pageable)`, map each entity to a response DTO, and pass both the source
   `Page` and mapped list to `PagedResponse.from`.
3. **Get by id:** call `findById`; turn empty into the resource-specific 404; map the found entity.
4. **Update:** load or 404; check the new name with the `AndIdNot` probe; update only mutable fields;
   save and map the entity returned from `save` (do not assume the mock or persistence layer returns
   the same instance). A no-op rename must pass because the current id is excluded.
5. **Delete:** check `existsById` or throw 404; query whether products reference the id and throw
   `ResourceInUseException` if so; only then call `deleteById`. Mark writes transactional and
   ADMIN-only. Keep the read methods available to any authenticated caller.
6. **Supplier differences:** duplicate field label is `supplier name`; update name/email/phone;
   dependency probe is `existsBySupplierId`; conflict text identifies `products`.
7. **Checkpoint:** for every conflict path, verify `save` or `deleteById` was never called after the
   decision to reject.

#### F. Product service methods

1. **Create:** check duplicate SKU before any category/supplier lookup. Resolve mandatory category or
   404; resolve supplier only when its id is non-null or 404. Build with the resolved entities and
   request fields, intentionally leaving `quantityOnHand` at its entity default of zero. Save and
   map the saved product.
2. **List:** select exactly one repository method based on the two optional filters: both set →
   low-stock-by-category; category only → category query; low-stock only → low-stock query; neither
   → ordinary paged find-all. Map the returned page without losing its pagination metadata.
3. **Get by id:** load or throw Product 404, then map. Centralize mapping in a helper that constructs
   category and optional supplier summaries, copies scalar fields, and computes `lowStock` by calling
   `product.isLowStock()`.
4. **Update:** load product or 404; reject another product's case-insensitive SKU; resolve category
   on every update or 404; resolve supplier only when supplied (null clears the relation without
   calling the supplier repository). Copy only SKU, name, description, price, reorder level, and
   relationships. Never set `quantityOnHand`. Save the returned entity and map it.
5. **Delete:** confirm product exists; check `StockMovementRepository.existsByProductId`; reject
   history with `ResourceInUseException`; otherwise delete. Do not cascade away ledger records.
6. Put `@Transactional` and ADMIN method security on create/update/delete. Keep reads read-only and
   accessible to authenticated users through the URL policy.
7. **Checkpoint:** test the four list branches individually and use an argument captor or saved
   entity assertion to prove product creation starts at zero and update preserves the current
   balance.

#### G. Stock-movement service methods

1. **Record:** start one transaction and load the product, then the recorder user named by the
   controller's principal. Either missing row becomes a 404 before any writes.
2. Read the validated positive quantity. For `IN`, calculate current balance plus quantity. For
   `OUT`, compare requested quantity to current balance first; if larger, throw
   `InsufficientStockException` before either save. Otherwise subtract; equality is allowed and
   results in zero.
3. Set the product's balance to the calculated result and save it. Build a new `StockMovement` with
   product, type, quantity, reason, `quantityAfter`, and recorder; save it and map the **saved
   movement** to its response. The transaction makes the balance update and ledger append atomic.
4. In the private mapper, create `ProductSummaryDto` and `UserSummaryDto` from the associated entities
   and copy the ledger fields. Do not expose a movement edit/delete operation.
5. **Get by product:** use `productRepository.existsById` to produce a product 404 if absent; then
   fetch the paged movement list and map to `PagedResponse`. Do not load the full product just to
   verify it exists.
6. Keep `@PreAuthorize` on `record`: the username argument must equal the authenticated principal.
   The controller gets that argument from `Principal.getName()`; never accept recorder identity in
   the JSON request.
7. **Checkpoint:** assert OUT-too-large calls neither repository `save`; assert IN/OUT persist a
   product balance and ledger row with matching `quantityAfter`.

#### H. Controller methods: one HTTP translation pattern

For every controller method, follow the same small recipe: bind path/query/body parameters; put
`@Valid` on request DTO bodies; call exactly one service operation; return its result with the status
from §7.8. Controllers do not look up repositories, hash passwords, calculate stock, or perform role
checks.

1. **Auth endpoints:** register delegates to `register` and returns 201 with `UserResponse`; login
   and refresh delegate to their matching service methods and return 200 with `TokenResponse`.
2. **User/admin endpoints:** `/api/users/me` delegates to `getCurrentUser`; `/{username}` delegates
   to `getByUsername`; `/api/admin/users` delegates to `getAllUsers`. Keep the literal `me` route so
   it wins over the username template. The service annotations and URL security rules—not controller
   `if` statements—enforce access.
3. **Category/supplier CRUD:** POST delegates to create, returns 201 and a `Location` pointing at
   the created id; GET collection binds `Pageable` with the documented default sort; GET by id
   delegates to lookup; PUT delegates to update and returns 200; DELETE delegates to delete and
   returns 204 with no body. Apply the same pattern to both resources.
4. **Product CRUD/list:** use `sku` as the default list sort; bind optional `categoryId` and
   `lowStock` and forward both plus `Pageable` unchanged. Use the same create/get/update/delete status
   pattern as category/supplier.
5. **Movement endpoints:** POST receives the product id, validated movement request, and
   `Principal`; pass `principal.getName()` to the service; return 201 with the body but no `Location`.
   GET history binds descending `occurredAt` pagination and delegates to `getByProduct`.
6. Build create locations from the current request URI plus `/{id}` using
   `ServletUriComponentsBuilder`; do not hand-concatenate host names or ports.
7. **Checkpoint:** use `@WebMvcTest` with the imported security configuration. Verify HTTP status,
   JSON field names, validation errors, `Location`, no movement `Location`, and URL-level 401/403.
   Mock the service interface so these tests isolate HTTP translation; test service rules separately.

#### I. Seeder and finishing the class/test work

1. **`AdminUserSeeder.run`:** first query whether username `admin` exists. If yes, return without
   modifying anything. Otherwise build the user with `admin@stockflow.local`, BCrypt-encode the
   configured admin password, assign only `{ADMIN}`, save, and log that the seed account was
   created. This makes startup idempotent and avoids resetting a real admin password on every run.
2. Implement interfaces before implementations, then compile. For each service, implement a small
   private mapper before duplicating mapping logic in list/detail/create paths. For each controller,
   make it compile against the service interface before adding slice tests.
3. Write tests in the four layers described in §10.1: mocks for service decisions, H2 slices for
   persistence/query mappings, MVC slices for HTTP/security-filter wiring, and the small proxied
   method-security context for `@PreAuthorize`. Use the exact inventories in §10.3 as a checklist;
   do not try to prove service authorization with a Mockito-only unit test.
4. After each build-order stage in §11, run the narrow test class first, then the full suite. Fix the
   earliest failing layer before adding the next one; a repository query error should be corrected
   in the repository, not worked around in a controller.

---

## 8. Part 1 — API Contract & Authorization Matrix

### 8.1 Endpoints

| Method                | Path                                       | Who           | Success                                                 | Errors                                                                                 |
| --------------------- | ------------------------------------------ | ------------- | ------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `POST`                | `/api/auth/register`                       | public        | 201 `UserResponse`                                      | 400; 409 username/email                                                                |
| `POST`                | `/api/auth/login`                          | public        | 200 `TokenResponse`                                     | 400; 401                                                                               |
| `POST`                | `/api/auth/refresh`                        | public        | 200 `TokenResponse`                                     | 400; 401                                                                               |
| `GET`                 | `/actuator/health`                         | public        | 200 `{"groups":["liveness","readiness"],"status":"UP"}` | 503 when DOWN                                                                          |
| `GET`                 | `/api/users/me`                            | any token     | 200                                                     | 401                                                                                    |
| `GET`                 | `/api/users/{username}`                    | self or ADMIN | 200                                                     | 401; 403; 404                                                                          |
| `GET`                 | `/api/admin/users`                         | ADMIN         | 200                                                     | 401; 403                                                                               |
| `POST` `PUT` `DELETE` | `/api/categories[/{id}]`                   | ADMIN         | 201 / 200 / 204                                         | 400; 401; 403; 404; **409** dup name; **409** in use                                   |
| `GET`                 | `/api/categories[/{id}]`                   | any token     | 200                                                     | 401; 404                                                                               |
| `POST` `PUT` `DELETE` | `/api/suppliers[/{id}]`                    | ADMIN         | as above                                                | as above                                                                               |
| `GET`                 | `/api/suppliers[/{id}]`                    | any token     | 200                                                     | 401; 404                                                                               |
| `POST` `PUT` `DELETE` | `/api/products[/{id}]`                     | ADMIN         | 201 / 200 / 204                                         | 400; 401; 403; 404 (product, category, supplier); **409** dup SKU; **409** has history |
| `GET`                 | `/api/products` (`?categoryId=&lowStock=`) | any token     | 200 paged                                               | 400 bad boolean; 401                                                                   |
| `GET`                 | `/api/products/{id}`                       | any token     | 200                                                     | 400 non-numeric; 401; 404                                                              |
| `POST`                | `/api/products/{id}/movements`             | any token     | **201, no Location**                                    | 400; 401; 404 product; **409 insufficient stock**                                      |
| `GET`                 | `/api/products/{id}/movements`             | any token     | 200 paged, newest first                                 | 401; 404                                                                               |

### 8.2 Authorization matrix

|                                                        | anonymous | CLERK (bob)  |    ADMIN     |
| ------------------------------------------------------ | :-------: | :----------: | :----------: |
| register / login / refresh / health                    |    ✅     |      ✅      |      ✅      |
| read categories, suppliers, products, a ledger         |    401    |      ✅      |      ✅      |
| create / update / delete a category, supplier, product |    401    |   **403**    |      ✅      |
| record a stock movement                                |    401    | ✅ (as self) | ✅ (as self) |
| `GET /api/users/me`                                    |    401    |      ✅      |      ✅      |
| `GET /api/users/{other}`                               |    401    |   **403**    |      ✅      |
| `GET /api/admin/users`                                 |    401    |   **403**    |      ✅      |

Every catalogue write is refused twice for a CLERK — by the URL rule (the 403 you see over HTTP)
and by `@PreAuthorize` on the service (the defense-in-depth layer `MethodSecurityTest` proves).

### 8.3 Sample bodies

`POST /api/products` request / 201 response:

```json
// request (note: no quantityOnHand)
{ "sku": "BOLT-M8-50", "name": "M8 x 50 bolt", "unitPrice": 0.35, "reorderLevel": 100,
  "categoryId": 1, "supplierId": 1 }
// 201
{ "id": 1, "sku": "BOLT-M8-50", "name": "M8 x 50 bolt", "description": null, "unitPrice": 0.35,
  "quantityOnHand": 0, "reorderLevel": 100, "lowStock": true,
  "category": { "id": 1, "name": "Fasteners" }, "supplier": { "id": 1, "name": "Acme Metals" } }
```

`POST /api/products/1/movements` → 201, then the refusal:

```json
// request                                          // 201
{ "type": "IN", "quantity": 250, "reason": "Delivery" }
{ "id": 1, "type": "IN", "quantity": 250, "reason": "Delivery", "quantityAfter": 250,
  "occurredAt": "2026-09-04T17:57:53.337918900Z",
  "product": { "id": 1, "sku": "BOLT-M8-50", "name": "M8 x 50 bolt" },
  "recordedBy": { "id": 2, "username": "bob" } }

// request { "type": "OUT", "quantity": 300 }       // 409
{ "title": "Insufficient stock", "status": 409,
  "detail": "Insufficient stock for BOLT-M8-50: 250 on hand, 300 requested",
  "instance": "/api/products/1/movements" }
```

`DELETE /api/categories/1` while products reference it → 409:

```json
{
  "title": "Resource in use",
  "status": 409,
  "detail": "Category 1 cannot be deleted: it is still referenced by products",
  "instance": "/api/categories/1"
}
```

---

## 9. Part 1 — Business Rules

- **Stock is never edited, only moved.** `quantityOnHand` starts at 0 and changes only through
  `StockMovementServiceImpl.record`; product update leaves it untouched; no request DTO carries it.
- **The ledger is append-only.** No update or delete endpoint for movements.
- **An OUT larger than the stock on hand is a 409** (`Insufficient stock`) — the request is
  well-formed; it conflicts with current state. Exactly the on-hand quantity is allowed (→ 0).
- **`quantityAfter`** on every ledger line equals the product's balance after that line.
- **Low stock** = `quantityOnHand <= reorderLevel`, defined once in `Product.isLowStock()` and mirrored
  in the repository's JPQL; `reorderLevel = 0` means a product is low only at zero.
- **Uniqueness is case-insensitive** for category name, supplier name, and SKU → 409
  `Duplicate value`; an update excludes its own row from the check.
- **Deleting anything still referenced is a 409 `Resource in use`**, decided by the service before
  the database is asked: category with products, supplier with products, product with movements.
  The foreign key is never the thing that says no.
- **Catalogue writes are ADMIN; recording movements is any authenticated user, as themselves.** The
  `POST …/movements` URL rule is listed before the ADMIN product rule, because the first match wins.
- **`recordedBy` and a user's roles are server-controlled**, never request fields.
- Carried from Labs 6–8 unchanged: BCrypt; `ROLE_` prefix mapping; username/email unique → 409;
  stateless JWT, CSRF off; refresh-token rotation; seeded `admin` / `admin12345`; 401 for anonymous,
  403 for forbidden, with the same ProblemDetail shape from both layers.
- **A body that is not valid JSON is a 400 `Malformed request`**, never a 500.

---

## 10. Part 1 — Testing (four layers; **167 tests**, all green with `./mvnw test` on H2)

### 10.1 Where each kind of test belongs (Lab 8's lesson, applied)

| Layer                                                          | Proves                                                                                          | Cannot prove                                                            |
| -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| Mockito unit (`@InjectMocks`)                                  | mapping, id-resolution, 404/409 decisions, the stock arithmetic                                 | authorization — the annotations are **inert** without a proxy           |
| `@DataJpaTest`                                                 | derived query names resolve; the low-stock JPQL; entity graphs; `@PrePersist`                   | authorization                                                           |
| `@WebMvcTest` slice                                            | status codes, JSON, `Location`, validation messages, URL-level 401/403, ProblemDetail rendering | rules on the service implementation (the service is a mocked interface) |
| minimal `@EnableMethodSecurity` context (`MethodSecurityTest`) | the `@PreAuthorize` rules, allow **and** deny                                                   | HTTP wiring                                                             |

### 10.2 The two recipes

**`@WebMvcTest` with security** (every controller test):

```java
@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;              // tools.jackson.databind
    @MockitoBean private CategoryService categoryService;      // one per service the controller injects
    @MockitoBean private UserDetailsService userDetailsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }
}
```

`@WithMockUser(roles = "ADMIN")` yields authority `ROLE_ADMIN`; `roles = "CLERK"` yields `ROLE_CLERK`.
`ProductControllerTest` needs **two** service mocks (`ProductService`, `StockMovementService`).

**`MethodSecurityTest`** — `@SpringJUnitConfig(MethodSecurityTest.Config.class)` where `Config` is a
static `@Configuration @EnableMethodSecurity` class declaring the five real service implementations
as `@Bean`s (`CategoryService categoryService(CategoryRepository, ProductRepository)`,
`SupplierService supplierService(SupplierRepository, ProductRepository)`,
`ProductService(ProductRepository, CategoryRepository, SupplierRepository, StockMovementRepository)`, `StockMovementService(StockMovementRepository, ProductRepository,
UserRepository)`, `UserService(UserRepository)`); the five repositories are `@MockitoBean` fields; the
services are autowired **by interface** (they are JDK proxies). `@WithMockUser` / `@WithAnonymousUser`
supply the principal; a denial is `AuthorizationDeniedException`.

### 10.3 Test inventory (24 classes)

**Counting convention for the tables below:** items separated by `;` are separate test methods; a
`+` or `/` _inside_ one item means one test with several assertions or calls. Where a class's stated
count matters for grading, count the `;`-separated items.

**Security & auth foundation (12 classes, 61 tests)**

| Class                                   | Layer                                                                         | Tests — what to verify                                                                                                                                                                                                                                                                                                                                                                                           |
| --------------------------------------- | ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `security/JwtServiceTest`               | plain unit                                                                    | **5** — round-trip subject + roles and `isValid`; TTL in seconds = 900; negative-TTL token is invalid; **tampered** token is invalid (change the **first** char of the signature segment — the last char's low bits are padding and may decode identically); a token signed with a different secret is invalid                                                                                                   |
| `security/JwtAuthenticationFilterTest`  | `@WebMvcTest(CategoryController.class)` with the **real** `JwtService`/filter | **6** — a CLERK token reaches a protected `GET` (200); an ADMIN token reaches a protected `POST` (201 + `Location`); a CLERK token is refused the `POST` (403, `title == "Access denied"`) — the roles claim drives `hasRole`; missing token 401; tampered 401; expired 401                                                                                                                                      |
| `security/UserDetailsServiceImplTest`   | Mockito                                                                       | **2** — loads a user with `ROLE_` authorities and the stored hash; `UsernameNotFoundException` when absent                                                                                                                                                                                                                                                                                                       |
| `service/AuthServiceImplTest`           | Mockito (real `BCryptPasswordEncoder`; 5-arg constructor)                     | **9** — register assigns CLERK and returns id/username/email; register stores a BCrypt hash; 409 username (no save); 409 email (no save); login authenticates via the manager then issues access + refresh; login puts **bare** role names in the claim; login propagates `BadCredentialsException` and issues nothing; refresh rotates and mints a new access token; refresh propagates `InvalidTokenException` |
| `service/RefreshTokenServiceImplTest`   | Mockito                                                                       | **7** — create persists a UUID, unrevoked, future expiry; verify ok; verify throws unknown / revoked / expired; rotate revokes and returns a new token for the same user; rotate throws and issues nothing when invalid                                                                                                                                                                                          |
| `service/UserServiceImplTest`           | Mockito                                                                       | **4** — getByUsername maps; 404 when absent; getCurrentUser resolves the principal from the SecurityContext; getAllUsers maps every user                                                                                                                                                                                                                                                                         |
| `repository/UserRepositoryTest`         | `@DataJpaTest`                                                                | **4** — findByUsername with eager roles; empty when absent; existsByUsername; existsByEmail                                                                                                                                                                                                                                                                                                                      |
| `repository/RefreshTokenRepositoryTest` | `@DataJpaTest`                                                                | **3** — findByToken with its user; empty when unknown; a token can be marked revoked and read back                                                                                                                                                                                                                                                                                                               |
| `controller/AuthControllerTest`         | slice                                                                         | **5** — register 201 with no password field; 409 username taken; 400 blank username; 400 bad email; 400 short password                                                                                                                                                                                                                                                                                           |
| `controller/AuthControllerLoginTest`    | slice                                                                         | **6** — login 200 (`Bearer`, `expiresIn`); 401 bad credentials; 400 blank username; refresh 200 with the rotated token; 401 invalid; 400 blank                                                                                                                                                                                                                                                                   |
| `controller/UserControllerTest`         | slice                                                                         | **6** — `/me` 401 anonymous; `/me` 200; `/{username}` 200 own; 403 ProblemDetail for another's (mock throws `AuthorizationDeniedException`); 401 anonymous; `/me` still routes to `me()` (`verify(never()).getByUsername("me")`)                                                                                                                                                                                 |
| `controller/AdminControllerTest`        | slice                                                                         | **4** — 401 anonymous; 403 CLERK; the 403 **body** is the ProblemDetail (`RestAccessDeniedHandler`); 200 ADMIN with the list                                                                                                                                                                                                                                                                                     |

**StockFlow domain (12 classes, 106 tests)**

| Class                                    | Layer                                                                                 | Tests — what to verify                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| ---------------------------------------- | ------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `model/EntityMappingTest`                | `@DataJpaTest`                                                                        | **3** — persist Product (category + supplier) and StockMovement (recorder), `em.clear()`, reload every relationship and `occurredAt`; a minimal Product defaults to 0 / 0 / null supplier and **is low stock**; `isLowStock` at 11/10/0 vs reorder 10                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `repository/CategoryRepositoryTest`      | `@DataJpaTest`                                                                        | **2** — `existsByNameIgnoreCase` with four probes (three spellings of an existing name → true, an absent name → false); `existsByNameIgnoreCaseAndIdNot` excludes self and catches the other row                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `repository/ProductRepositoryTest`       | `@DataJpaTest` (2 categories, 1 supplier, 4 products; BOLT 5/100 and SAW 0/0 are low) | **6** — findAll paginates + sorts, and `Hibernate.isInitialized(product.getCategory())` is **true** on the page (the graph fetched it) while a plain `em.find` after `em.clear()` leaves it **false** (an uninitialised proxy); findByCategoryId; findLowStock = BOLT, SAW; findLowStockByCategoryId; SKU probes incl. AndIdNot; one test covering existsByCategoryId and existsBySupplierId — true for referenced, false for fresh rows                                                                                                                                                                                                                                                                                                                                                                                |
| `repository/StockMovementRepositoryTest` | `@DataJpaTest`                                                                        | **4** — seed three lines for one product with **explicit** `occurredAt` (since `@PrePersist` only fills a null) and persist them out of order: IN 250 at `2026-09-01T09:00:00Z` (quantityAfter 250), OUT 20 at `09-02T10:00:00Z` (230), OUT 30 at `09-03T11:00:00Z` (200). findByProductId sorted by the Pageable (`occurredAt` desc → quantityAfter 200, 230, 250) with `Hibernate.isInitialized` true for product and recorder; empty for a product with no history; existsByProductId; `@PrePersist` stamps `occurredAt` when omitted                                                                                                                                                                                                                                                                                |
| `service/CategoryServiceImplTest`        | Mockito                                                                               | **9** — create maps; create dup 409 (`never()).save`); getAll maps a page; getById 404; update; update dup 409 (`never()).save`); delete (`verify deleteById`); delete 404 (`never()).deleteById`); delete in use 409 (`never()).deleteById`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `service/SupplierServiceImplTest`        | Mockito                                                                               | **6** — create; create dup 409 (`never()).save`); update (excludes self); getById 404; delete; delete in use 409 (`never()).deleteById`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `service/ProductServiceImplTest`         | Mockito                                                                               | **15** — create resolves category + supplier and starts at 0 (lowStock true); create without supplier never asks the supplier repo; unknown category 404 (`never()).save`); unknown supplier 404; dup SKU 409 before any lookup (`verifyNoInteractions` on the category and supplier repositories); the four `getAll` branches (4 tests); getById maps lowStock; update rewrites fields and **never touches quantityOnHand** (and null supplierId clears it); update dup SKU 409; delete; delete with history 409; delete 404                                                                                                                                                                                                                                                                                           |
| `service/StockMovementServiceImplTest`   | Mockito                                                                               | **8** — IN adds and the saved product carries 300 (`ArgumentCaptor`); OUT subtracts; OUT of exactly on-hand → 0; OUT too large → 409 and **neither** save is called; unknown product 404; unknown recorder 404; getByProduct maps; getByProduct 404                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `controller/CategoryControllerTest`      | slice                                                                                 | **12** — 401 anonymous; 201 + `Location` ADMIN; 403 CLERK; 400 blank name (`$.errors.name`); 409 dup (`Duplicate value`); 200 paged CLERK; 404; PUT 200 ADMIN; PUT 403 CLERK; DELETE 204; DELETE 409 (`Resource in use`, detail contains "still referenced by products"); **400 `Malformed request`** for a body that is not JSON                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `controller/SupplierControllerTest`      | slice                                                                                 | **7** — 401; 201 + `Location`; 403 CLERK; 400 bad email (`$.errors.email`); `GET /{id}` 200; DELETE 204; DELETE 409                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `controller/ProductControllerTest`       | slice (two service mocks)                                                             | **17** — 401; 201 ADMIN with `lowStock`, nested category and supplier; 403 CLERK; 400 with three field errors in one test (blank sku, missing categoryId, `unitPrice` with 3 decimals); list passes `categoryId` + `lowStock` to the service (`verify`); `?lowStock=maybe` → 400 `Invalid parameter`; GET 200; `/abc` → 400; PUT 200; DELETE 204; DELETE 409; **movements**: 201 as CLERK with `recordedBy` from the principal and **no `Location`**; 401 anonymous; 400 (type null + quantity 0 → both field errors); 409 `Insufficient stock` with the exact detail; ledger 200 paged; ledger 404                                                                                                                                                                                                                     |
| `security/MethodSecurityTest`            | minimal context                                                                       | **17** — category create denied CLERK (`never()).save`); category create allowed ADMIN; category update + delete denied CLERK (one test, both calls); category read allowed CLERK; supplier create denied CLERK; supplier create allowed ADMIN; product create + update + delete denied CLERK (one test, three calls, `never()).save` / `deleteById`); product create allowed ADMIN; product read allowed CLERK; **record** allowed as self (300 after IN 50 on 250); record denied when `recordedByUsername ≠ principal` (no repository call); record denied anonymous even naming `"anonymousUser"`; ADMIN records as self; getByUsername own; getByUsername other denied (`never()).findByUsername`); getByUsername as ADMIN for anyone **and** `getAllUsers` allowed for ADMIN (one test); getAllUsers denied CLERK |

---

## 11. Part 1 — Suggested Build Order

1. `pom.xml`, `application.properties`, `Application`. `./mvnw -q compile`.
2. **The security foundation**, exactly as in Lab 7/8: `Role`, `User`, `RefreshToken`;
   `UserRepository`, `RefreshTokenRepository`; the five auth DTOs + `UserSummaryDto` + `PagedResponse`;
   `ResourceNotFoundException`, `DuplicateResourceException`, `InvalidTokenException`; `JwtProperties`;
   `JwtService`; `UserDetailsServiceImpl`; `RefreshTokenService(+Impl)`; `AuthService(+Impl)`;
   `UserService(+Impl)`; `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`,
   `RestAccessDeniedHandler`, `AdminUserSeeder`; `SecurityConfig` with **only** the auth/admin/health/
   anyRequest rules for now; `GlobalExceptionHandler` with its first five handlers; `AuthController`,
   `UserController`, `AdminController`. Write and green their 12 test classes (61 tests) — except
   `JwtAuthenticationFilterTest`, which targets `CategoryController` and waits for step 5.
3. **Catalogue model + data layer:** `MovementType`, `Category`, `Supplier`, `Product`, `StockMovement`;
   the four repositories; `EntityMappingTest` and the three repository tests (15 tests). These prove
   every derived query name resolves before anything depends on it.
4. `ResourceInUseException`, `InsufficientStockException`, and their handlers plus the
   `HttpMessageNotReadableException` handler.
5. The eleven catalogue DTOs; `CategoryService(+Impl)`, `SupplierService(+Impl)` and their controllers
   - tests; **add the catalogue URL rules to `SecurityConfig`**; now `JwtAuthenticationFilterTest`.
6. `ProductService(+Impl)`, `ProductController` (catalogue endpoints only) + tests.
7. `StockMovementService(+Impl)`, the two nested endpoints, **the `POST …/movements` URL rule above the
   ADMIN product rule**; the remaining product-controller tests and `StockMovementServiceImplTest`.
8. `MethodSecurityTest`. Then `./mvnw test` → **167 green**.

---

## 12. Part 1 — Manual Testing Flow (curl, local Postgres)

On an **empty** `stockflow` database (`ddl-auto=update` keeps rows between runs — if you have run
the app before, drop and recreate the database, or expect 409s on the creates and different ids) with
`./mvnw spring-boot:run` running:

1. `GET /actuator/health` with no token → **200** `{"groups":["liveness","readiness"],"status":"UP"}`.
2. Register `bob` (→ CLERK), log in as `admin` / `admin12345` and as `bob`; keep both access tokens.
3. As admin: `POST /api/categories {"name":"Fasteners"}` → 201; `POST /api/suppliers` → 201;
   `POST /api/products {"sku":"BOLT-M8-50","name":"M8 x 50 bolt","unitPrice":0.35,"reorderLevel":100,"categoryId":1,"supplierId":1}`
   → 201 with `"quantityOnHand":0,"lowStock":true`.
4. As bob: `POST /api/categories` → **403**; `POST /api/products/1/movements {"type":"IN","quantity":250}`
   → **201**, `quantityAfter: 250`, `recordedBy.username: bob`; `{"type":"OUT","quantity":300}` → **409
   Insufficient stock**; `{"type":"OUT","quantity":30}` → 201, `quantityAfter: 220`.
5. `GET /api/products/1` → `quantityOnHand: 220`, `lowStock: false`; `GET /api/products?lowStock=true` →
   empty; `GET /api/products/1/movements` → 2 lines, newest first.
6. As admin: `DELETE /api/categories/1` → **409 Resource in use**; `DELETE /api/products/1` → **409**.
7. `POST /api/categories` with body `{"name": ` → **400 Malformed request** (not 500).

---

## 13. Part 1 — Acceptance Criteria

- [ ] The 67 main classes exist with the mappings, constraints, and signatures of §7; `./mvnw test`
      is **fully green, 167 tests**.
- [ ] `quantityOnHand` appears in no request DTO and is written only by `StockMovementServiceImpl`.
- [ ] Every ledger line carries `quantityAfter`; movements have no update or delete endpoint.
- [ ] OUT beyond on-hand → 409 with `title == "Insufficient stock"`, and nothing is written.
- [ ] Deleting a referenced category, supplier, or product → 409 `Resource in use`, decided by the
      service (a `verify(never()).deleteById` in the unit test).
- [ ] Duplicate names/SKUs are refused case-insensitively; a no-op rename is not a duplicate.
- [ ] Catalogue writes are 403 for a CLERK from the URL rule **and** denied by `@PreAuthorize`
      (both tested); `POST …/movements` is allowed for a CLERK and pinned to the principal.
- [ ] `/actuator/health` answers 200 `UP` with no token.
- [ ] A malformed body is a 400, never a 500.
- [ ] The §12 walk produces every stated status.

---

# PART 2 — Ship It in Docker

## 14. The new concepts (read the course document)

Everything in this part is taught in `COURSE_09_DOCKER.md`: what a container is [C-§1], images vs.
containers and the CLI [C-§2], the Dockerfile and the **layer cache** [C-§3], **multi-stage builds**
and the Boot 4 **`tools` jarmode** [C-§4], ports / volumes / environment variables [C-§5], and
**Compose** [C-§6]. Read it first; this part tells you what to write and what you should see.

## 15. Part 2 — Prerequisites

- **Docker Desktop installed and running.** Verify with `docker info --format '{{.ServerVersion}}'`.
  If you see _error during connect … cannot find the file / connection refused_, the daemon is not
  running — start Docker Desktop and wait for the whale icon to stop animating. This is the single
  most common "Docker is broken" report, and Docker is not broken. [C-§1.3]
- `docker compose version` prints v2.x (Compose is bundled with Docker Desktop).
- Port **8080** free on your machine (stop `./mvnw spring-boot:run`), and — because the Compose file
  publishes the containerized Postgres on **5433** — that port free too. Your local Postgres on 5432
  is not touched.
- Part 1 green: `./mvnw test`.

## 16. Part 2 — The `Dockerfile`

Create `Dockerfile` (no extension) in the module root:

```dockerfile
# syntax=docker/dockerfile:1

# ---- Stage 1: build the fat jar --------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Stage 2: explode the fat jar into layers ------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /extract
COPY --from=build /build/target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Stage 3: the runtime image --------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S stockflow && adduser -S -G stockflow stockflow
WORKDIR /app
COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./
USER stockflow
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "application.jar"]
```

The reference Dockerfile carries the rationale below as comments above each instruction; the
instructions themselves are identical. Line by line, and why — you will be asked to explain each:

| Lines                                                             | Why                                                                                                                                                                                                                                                                     |
| ----------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `FROM maven:… AS build`                                           | a JDK + Maven, used only to build; **never shipped** [C-§4.2]. The official image rather than `./mvnw`: a CRLF-converted wrapper fails in a Linux shell with `bad interpreter` (the module's `.gitattributes` pins `mvnw` to LF, but an editor or a copy can undo that) |
| `COPY pom.xml` → `dependency:go-offline` → `COPY src` → `package` | **cache ordering**: the dependency download is its own layer, invalidated only when `pom.xml` changes [C-§3.2]                                                                                                                                                          |
| `-DskipTests`                                                     | the image build is not the test pipeline; tests ran locally and will run in CI [C-§4.2]                                                                                                                                                                                 |
| `AS extract` … `extract --layers`                                 | splits the 60 MB fat jar into `dependencies/` (~65 MB), `spring-boot-loader/` (empty in Boot 4), `snapshot-dependencies/`, `application/` (a ~115 KB **thin jar**) [C-§4.3]                                                                                             |
| `eclipse-temurin:21-jre-alpine`                                   | a JRE, not a JDK; Alpine is small and its busybox has `wget` for the healthcheck [C-§4.4]                                                                                                                                                                               |
| `addgroup -S … adduser -S …` + `USER stockflow`                   | never run as root inside a container [C-§4.4]                                                                                                                                                                                                                           |
| the four `COPY --from=extract`                                    | most-stable layer first; after a code change only the last (115 KB) layer is new [C-§4.3]                                                                                                                                                                               |
| `EXPOSE 8080`                                                     | documentation only; publishing is `-p` [C-§5.1]                                                                                                                                                                                                                         |
| `HEALTHCHECK`                                                     | Docker asks `/actuator/health` every 10 s; `start-period=40s` gives the JVM time to boot before failures count [C-§7]                                                                                                                                                   |
| `ENTRYPOINT ["java", "-jar", "application.jar"]`                  | the thin jar's manifest carries `Main-Class` and `Class-Path: lib/…`, so a plain `java -jar` is the whole launch — **not** `JarLauncher` [C-§4.3]; exec form so SIGTERM reaches the JVM [C-§3.1]                                                                        |

## 17. Part 2 — The `.dockerignore`

```
target/
.git/
.gitignore
.idea/
*.iml
.vscode/
*.md
*.postman_collection.json
postman/
mvnw
mvnw.cmd
.mvn/
.env
```

`target/` so a stale local build is never copied in and 60 MB is not uploaded as build context; the
wrapper because the Maven image has `mvn`; `.env` because a secrets file must never end up in an
image. [C-§3.3]

## 18. Part 2 — Build the image and run it standalone

```bash
docker build -t stockflow:1.0 .
```

The first build takes minutes (it downloads three base images and ~90 jars). Then inspect what you
made, and compare with the reference measurements:

| Check      | Command                                                                            | Reference result                                                                                                                                                                                                             |
| ---------- | ---------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| it exists  | `docker images stockflow`                                                          | `stockflow  1.0`                                                                                                                                                                                                             |
| size       | `docker image inspect stockflow:1.0 --format '{{.Size}}'`                          | **≈132 MB** (126 MiB) — the compressed content size on Docker Desktop; the layers unpack to ≈277 MB, which is what a Linux engine reports. A naive single-stage image of the same app is 395 MB by the same measure [C-§4.1] |
| the layers | `docker history stockflow:1.0`                                                     | one **64.7 MB** `dependencies/` layer, one **115 kB** `application/` layer, 4 kB for the empty loader layer — these per-layer figures are the same on every engine                                                           |
| the user   | `docker run --rm --entrypoint sh stockflow:1.0 -c id`                              | `uid=100(stockflow) gid=101(stockflow)`                                                                                                                                                                                      |
| the layout | `docker run --rm --entrypoint sh stockflow:1.0 -c 'ls /app; ls /app/lib \| wc -l'` | `application.jar  lib` and **89** jars                                                                                                                                                                                       |

> **`docker images` will show a much bigger number** — ≈408 MB for this image on Docker Desktop.
> That column adds the unpacked snapshot to the compressed blobs; it does not mean your multi-stage
> build did nothing. Judge by `inspect`, by `history`'s per-layer figures, and by the naive-vs-layered
> ratio [C-§8].

Now run it against your **local** PostgreSQL. Inside a container `localhost` is the container, so the
database host is `host.docker.internal` (Docker Desktop's name for the host machine) — and every
setting is an environment variable [C-§5.3]:

```bash
docker run -d --name stockflow -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/stockflow \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=123456 \
  stockflow:1.0

docker ps                                   # STATUS goes (health: starting) → (healthy) within ~40 s
docker logs -f stockflow                    # "Started Application in 3.x seconds" — Ctrl-C leaves it running
curl -s localhost:8080/actuator/health      # {"groups":["liveness","readiness"],"status":"UP"}
docker exec -it stockflow sh                # a shell INSIDE the container: `id`, `ls /app`, `exit`
docker stop stockflow && docker rm stockflow
```

Log in as `admin` / `admin12345` and hit a few endpoints; it is the same application, now running from
an image that contains no database address.

## 19. Part 2 — `compose.yaml`

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: stockflow
      POSTGRES_USER: stockflow
      POSTGRES_PASSWORD: stockflow
    volumes:
      - stockflow-pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U stockflow -d stockflow"]
      interval: 5s
      timeout: 3s
      retries: 10
    ports:
      - "5433:5432"

  app:
    build: .
    image: stockflow:1.0
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/stockflow
      SPRING_DATASOURCE_USERNAME: stockflow
      SPRING_DATASOURCE_PASSWORD: stockflow
      SPRING_JPA_SHOW_SQL: "false"
      APP_SECURITY_JWT_SECRET: ${STOCKFLOW_JWT_SECRET:-pp0gXRqpg69kqphvhFq5XK2JB4lxe8noQUoDG4RAslaDXvP/3idY/I+PV5M1EmNB}
      APP_SECURITY_ADMIN_PASSWORD: ${STOCKFLOW_ADMIN_PASSWORD:-admin12345}
    ports:
      - "8080:8080"

volumes:
  stockflow-pgdata:
```

What each part does [C-§6.2]: the database host is the **service name** `postgres`, resolved by
Compose's DNS; the **named volume** `stockflow-pgdata` holds the data and outlives the containers;
`pg_isready` is the Postgres healthcheck that `depends_on … condition: service_healthy` waits for — so
the app never starts before the database can accept connections; the app's own health comes from the
image's `HEALTHCHECK`; **5433** on the host avoids your local Postgres; `SPRING_JPA_SHOW_SQL` shows an
ordinary property overridden per environment; the two secrets use `${VAR:-fallback}` so a real
deployment sets `STOCKFLOW_JWT_SECRET` in the environment (or a git-ignored `.env`) and never relies on
the fallback. Run `docker compose config` to see the fully resolved file.

## 20. Part 2 — Bring it up, prove persistence, tear it down

```bash
docker compose up -d --build
docker compose ps            # postgres healthy first, then app (health: starting) → healthy
                             # reference: 12 s after `up -d` returns (≈20 s from the command start on
                             # a first `--build`, ≈7 s on a re-`up`)
docker compose logs -f app   # Ctrl-C to stop following
```

Then, with `curl` or Postman against `http://localhost:8080`:

1. `GET /actuator/health` → `UP`. From inside Compose this also proves the app reached `postgres:5432`.
2. Log in as `admin`; create a category and a product; log in as `bob` (register first) and record
   `IN 120` → `quantityAfter: 120`.
3. **`docker compose down`** — containers and network removed. `docker volume ls` still shows
   `…_stockflow-pgdata`.
4. `docker compose up -d` — new containers. Log in again (the old token is fine — it is stateless —
   but the refresh tokens live in the database and survived too). `GET /api/products/1` →
   **`quantityOnHand: 120`**. Two containers were destroyed and recreated; the number came back because
   it was in the volume, never in a container.
5. **`docker compose down -v`** — the volume is removed. `docker compose up -d` → the seeder creates
   `admin` again, and the products are gone.

## 21. Part 2 — Prove the layer cache

1. `docker build -t stockflow:1.0 .` once more; note the time (seconds, everything cached).
2. Change one line in any source file (a comment will do). Build again and **read the log** — the
   reference run:

```
CACHED  [build 3/6] COPY pom.xml .
CACHED  [build 4/6] RUN mvn -q -B dependency:go-offline      ← no download
RE-RUN  [build 5/6] COPY src ./src
RE-RUN  [build 6/6] RUN mvn -q -B package -DskipTests
CACHED  [stage-2 4/7] COPY … /extracted/dependencies/ ./      ← the 65 MB layer reused
RE-RUN  [stage-2 7/7] COPY … /extracted/application/ ./       ← 115 KB
```

(`CACHED` is BuildKit's own word; `RE-RUN` is this document's annotation — BuildKit shows a
re-executed step as `DONE <seconds>`.) Reference: **12 seconds**. Now change one dependency version in
`pom.xml` and build again: the
`dependency:go-offline` layer re-runs and the build is back to minutes — which is the point. [C-§3.2]

## 22. Part 2 — Build a Postman collection (and test the containerized app)

> **A ready-to-import reference collection ships with this module:
> `StockFlow_Docker.postman_collection.json`** (26 requests, 55 assertions; runs green top-to-bottom
> twice in a row against the Compose stack). Use it to check your work — but **building your own is
> the exercise.**

Point `baseUrl` at `http://localhost:8080` with the Compose stack up, and build, in order:

| #     | Request                                                              | As       | Assert                                                                                 |
| ----- | -------------------------------------------------------------------- | -------- | -------------------------------------------------------------------------------------- |
| 0     | `GET /actuator/health`                                               | no token | 200, `status == "UP"` — the same question the healthcheck asks                         |
| 1     | Register `bob`                                                       | —        | 201 **or 409** (so the collection re-runs)                                             |
| 2–3   | Login `admin`, login `bob`                                           | —        | 200; capture each `accessToken` into a collection variable via a post-response script  |
| 4–5   | Create category `Fasteners`; list categories                         | admin    | 201 or 409; then resolve `{{categoryId}}` from the list (so a re-run finds it)         |
| 6–7   | Create supplier; list suppliers                                      | admin    | same pattern → `{{supplierId}}`                                                        |
| 8     | Create product with sku `BOLT-{{$timestamp}}`                        | admin    | 201; `quantityOnHand == 0`, `lowStock == true`; capture `{{productId}}`                |
| 9     | `POST …/{{productId}}/movements` IN 250                              | bob      | 201; `quantityAfter == 250`; `recordedBy.username == "bob"`; **no `Location` header**  |
| 10    | OUT 30                                                               | bob      | 201, `quantityAfter == 220`                                                            |
| 11    | OUT 500                                                              | bob      | **409**, `title == "Insufficient stock"`, detail contains `220 on hand, 500 requested` |
| 12    | quantity 0                                                           | bob      | 400, `errors.quantity == "Quantity must be greater than zero"`                         |
| 13    | `GET /api/products/{{productId}}`                                    | bob      | `quantityOnHand == 220`, `lowStock == false`                                           |
| 14    | `GET …/movements`                                                    | bob      | `totalElements == 2`, first is `OUT`, `quantityAfter` sequence `[220, 250]`            |
| 15    | `GET /api/products?lowStock=true`                                    | bob      | the product is **not** in the list                                                     |
| 16–18 | Create category / update product / delete product                    | bob      | **403** `Access denied` each                                                           |
| 19–20 | Delete the category; delete the product                              | admin    | **409** `Resource in use` each                                                         |
| 21    | Create category `fasteners` (lower case)                             | admin    | 409 `Duplicate value`                                                                  |
| 22–24 | `GET /api/users/bob`; `GET /api/users/admin`; `GET /api/admin/users` | bob      | 200; 403; 403                                                                          |
| 25    | `GET /api/products`                                                  | no token | 401                                                                                    |

Run it in the **Collection Runner** twice in a row: both runs must be all green. Then run it once more
after a `docker compose down` / `up` — everything created in run 1 is still there.

## 23. Part 2 — Acceptance Criteria

- [ ] `Dockerfile` is three-stage; the build stage uses the Maven image; dependencies are resolved on
      their own layer **before** `src` is copied; tests are skipped in the image build.
- [ ] The runtime stage is `eclipse-temurin:21-jre-alpine`, copies the four `tools` layers in
      most-stable-first order, runs as a **non-root** user, declares `EXPOSE 8080` and a
      `HEALTHCHECK`, and launches with `java -jar application.jar`.
- [ ] `docker image inspect` reports ≈132 MB on Docker Desktop (≈277 MB unpacked on a Linux engine);
      `docker history` shows a ~65 MB dependency layer and a ~115 kB application layer; `id` inside the
      container is not root.
- [ ] `.dockerignore` excludes `target/`, `.git/`, the docs, the wrapper, and `.env`.
- [ ] `docker run` with `-p 8080:8080` and `-e SPRING_DATASOURCE_*` against `host.docker.internal`
      reaches `(healthy)` and `/actuator/health` answers `UP`.
- [ ] `compose.yaml` declares `postgres:16` with a `pg_isready` healthcheck and a named volume, and the
      app with `depends_on … condition: service_healthy`, the database host as the **service name**, and
      **all** configuration and secrets as environment variables (nothing hard-coded in the image).
- [ ] `docker compose up -d` reaches `app` healthy; the API works against the containerized database.
- [ ] Stock recorded before `docker compose down` is present after `docker compose up`; it is gone
      after `down -v`.
- [ ] A source-only rebuild shows `dependency:go-offline` and the `dependencies/` COPY as `CACHED`
      and completes in seconds.
- [ ] Your own Postman collection runs green twice against the Compose stack, and again after a
      `down`/`up`.

## 24. Optional Stretch Goals (not graded)

1. **Optimistic locking.** Two clerks recording against the same product at once do a
   read-modify-write on the same row. Add `@Version private long version;` to `Product`, handle
   `ObjectOptimisticLockingFailureException` as a 409, and write a test that provokes it.
2. **JVM memory in a container.** Add `-XX:MaxRAMPercentage=75.0` to the `ENTRYPOINT` and explain
   what the JVM uses as "RAM" inside a container with a cgroup memory limit (`docker run -m 512m`).
3. **A `.env` file.** Move the two secrets into `.env`, confirm `docker compose config` picks them up,
   and confirm `.env` is in both `.gitignore` and `.dockerignore`.
4. **Compose profiles.** Add a `tools` profile with a `pgadmin` service so it starts only with
   `--profile tools`.
5. **Distroless.** Rebuild the runtime stage on `gcr.io/distroless/java21-debian12` and discover what breaks
   (there is no shell for `HEALTHCHECK` — how would you check health then?).
6. **Sneak preview of Lab 11.** Read what Testcontainers does; you now understand every piece it
   builds on.

## 25. Instructor Notes

- **Part 1 is a from-scratch rebuild on purpose.** Students who finished Lab 8 have all the pieces;
  assembling them unaided — including the security foundation — is the skill Lab 7 introduced and this
  lab consolidates. The domain is new so nothing can be pasted from TaskFlow unchanged.
- **The two most common Part 2 failures** are not Docker failures: the daemon not running (§15), and
  a port already in use (a forgotten `./mvnw spring-boot:run` on 8080, or a local Postgres if a student
  maps 5432 instead of 5433).
- **`localhost` inside a container** trips almost everyone once. `docker run` needs
  `host.docker.internal`; Compose needs the service name. A student whose app "cannot connect to
  localhost:5432" from inside a container has learned the lesson the hard way — let them.
- **`depends_on` without `condition: service_healthy`** produces an app that crash-loops on first
  start and then works on `docker compose restart app` — an intermittent failure that is very
  instructive to diagnose from `docker compose logs app`.
- **`docker images` vs `docker image inspect`.** Docker Desktop's newer image store (containerd)
  keeps both the compressed blobs and the unpacked snapshot and lists their total — ~408 MB for this
  image — while `inspect` reports the compressed content size (~132 MB). Point students at the
  per-layer figures in `docker history` and the naive-vs-layered ratio before they conclude their
  multi-stage build did nothing (§18).
- **The naive-vs-layered comparison** (395 MB vs 132 MB, same measure) is worth building live: a one-stage
  Dockerfile from the Maven image takes two minutes and makes §4 of the course document concrete.
- **Grading shortcut:** `./mvnw test` → 167; `docker build` then `docker history` for the two
  headline layers; `docker compose up -d`, the persistence walk of §20, and the shipped collection
  run twice.
- **What comes next:** Lab 10 adds full-context `@SpringBootTest` integration tests (the natural
  place to re-verify this authorization matrix over real HTTP), Lab 11 replaces H2 with a real
  Postgres via Testcontainers — which requires exactly the Docker you set up here — and Lab 12 replaces
  `ddl-auto=update` with Flyway migrations.
