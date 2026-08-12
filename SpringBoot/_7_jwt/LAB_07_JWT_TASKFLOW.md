# Lab 07 — TaskFlow: Stateless Authentication with JWT (Build a New Project From Scratch, Then Add JWT)

> **Hand this single document to the student.** It is fully self-contained: it specifies *what* to
> build (the requirements, the exact API contract, the **authorization matrix**, and a
> **class-by-class implementation inventory**) and gives *guidance* on *how* to approach the
> genuinely new concepts. Unlike Labs 4–6 (each an *increment* on an existing Bookstore), **Lab 7 is
> built from nothing**: you create a brand-new application called **TaskFlow**, get it green with the
> Lab 6 security foundation, and *then* replace that foundation with **stateless JWT**. Because it is
> a from-scratch build, this file is long — completeness beats brevity. A student handed only this
> file (plus a Java 21 + Maven install and a PostgreSQL server) can implement the entire project,
> base and JWT, step by step to a green `./mvnw test`.

The document has **two explicit parts**:

- **PART 1 — Build the TaskFlow base from nothing** (Sections 6–13): a complete secured REST API with
  users, projects, and tasks, protected by the **Lab 6 security model** (HTTP Basic + a
  `SecurityFilterChain` + a DB-backed `UserDetailsService` + BCrypt + a seeded ADMIN). You build and
  green this first — it is the whole Bookstore-security skillset re-applied to a new domain.
- **PART 2 — Add stateless JWT** (Sections 14–22): the genuinely new material. You add a `/login` that
  issues a short-lived **access token** plus a persisted **refresh token**, a `/refresh` that rotates
  it, a `OncePerRequestFilter` that authenticates the `Authorization: Bearer` header, and you
  **reconfigure the `SecurityFilterChain` to be stateless** (no session, no login form, no Basic).

Because the project is built in two passes, **five classes are specified twice** — once in their Part 1
shape (Section 7) and once as a Part 2 delta (Section 16): `AuthService`, `AuthServiceImpl`,
`AuthController`, `GlobalExceptionHandler`, and `SecurityConfig`. **Section 23 is an appendix that
collapses both passes into the finished state** (every interface, every endpoint, the full file tree).
When you are checking your work rather than building it, read Section 23.

---

## 1. Problem Statement & Goal

Through Labs 1–6 you worked on **one** codebase (the Bookstore), growing it increment by increment.
Lab 7 changes the exercise: you build a **complete new application, TaskFlow**, from an empty Maven
project — every entity, DTO, repository, service, controller, exception handler, security config, and
test — and *then* you add the one genuinely new capability the Security phase is missing: **stateless,
token-based authentication with JSON Web Tokens (JWT)**.

**TaskFlow** is a small team task-tracker. Users register and log in; each user owns **projects**;
each project holds **tasks** with a status, priority, due date, and an optional assignee. It is a
**private workspace API** — unlike the Bookstore, there are **no public reads**: every domain endpoint
requires an authenticated caller. There is one ADMIN-only administration area.

You will build TaskFlow in two passes:

1. **Part 1 — the base.** Stand up the whole app secured exactly the way Lab 6 secured the Bookstore:
   registration hashes passwords with BCrypt, a `UserDetailsService` loads users from the database,
   and a single `SecurityFilterChain` (with **HTTP Basic** for API clients) enforces the authorization
   matrix. Get `./mvnw test` green. **This proves you can build and secure a REST API from scratch.**

2. **Part 2 — JWT.** Replace the session/Basic login with **stateless tokens**. Add a `POST
   /api/auth/login` that authenticates the credentials through the Lab 6 `AuthenticationManager` and
   returns a **signed access token** (15-minute lifetime) plus a **persisted, rotating refresh token**
   (7-day lifetime). Add a `POST /api/auth/refresh` that trades a valid refresh token for a fresh
   access token. Write a `JwtAuthenticationFilter` that reads `Authorization: Bearer <jwt>` on every
   request and populates the `SecurityContext`. **Reconfigure the `SecurityFilterChain` to
   `STATELESS`**, drop `formLogin`/`httpBasic`, `permitAll` the `/api/auth/**` routes, and register the
   JWT filter with `addFilterBefore`.

The base package for this lab is **`com.example._7_jwt`**. You deliver TaskFlow backed by a PostgreSQL
database (`taskflow_lab`), matching the **API Contract and authorization matrix** in Sections 8 and 17,
with automated tests at four layers.

> **What is deferred to Lab 8 (read this now).** Part 1 sets a project's `owner` to the **authenticated
> principal on create**, but it does **not** enforce per-row ownership: in Lab 7 *any* authenticated
> user may update or delete *any* project or task. **Row-level ownership enforcement** ("only the owner
> may mutate") is **method security** and is deliberately the subject of **Lab 8**. Do not add
> `@PreAuthorize` or ownership checks here.

## 2. Learning Objectives (what this assesses)

**From Part 1 (re-applying the whole stack to a new domain):**

1. Bootstrap a Spring Boot 4.0 project from scratch: `pom.xml`, `application.properties`, a
   `@SpringBootApplication` main class, and the layered package structure
   (`model → repository → service → controller`, plus `dto`, `exception`, `security`, `config`).
2. Model a three-entity domain with JPA relationships (`User 1─N Project`, `Project 1─N Task`,
   `Task N─1 User` assignee), enums stored as strings, LAZY `@ManyToOne`s, and `@EntityGraph` finders
   that avoid N+1 (the Lab 3 discipline).
3. Re-implement the **Lab 6 security foundation** on the new domain: a `User` entity with BCrypt-hashed
   passwords and roles, a `UserDetailsService`, a `BCryptPasswordEncoder` bean, a single
   `SecurityFilterChain` with an authorization matrix, an exposed `AuthenticationManager`, and a seeded
   ADMIN.
4. Expose DTO-only CRUD with manual entity↔DTO mapping, `PagedResponse` pagination, Jakarta Validation,
   and RFC 7807 `ProblemDetail` errors from a `@RestControllerAdvice`.
5. Test a secured web layer with the `@WebMvcTest` + `@Import(SecurityConfig)` + `@MockitoBean
   UserDetailsService` + `springSecurity()` + `@WithMockUser` recipe, plus `@DataJpaTest` and Mockito
   service layers.

**From Part 2 (the genuinely new material):**

6. Explain what a **JWT** is — the three base64url parts (header, payload, signature) — and why a signed
   JWT is **tamper-evident but readable, not encrypted** (so no secrets go in claims).
7. Issue and verify HS256 tokens with **jjwt 0.13.x** (the `jjwt-api` compile dependency plus the
   `jjwt-impl` and `jjwt-jackson` runtime dependencies), using `Jwts.builder()` / `Jwts.parser()`.
8. Design an **access + refresh token** flow: a short-lived stateless access token (no server-side
   revocation, so keep its blast radius small) and a **persisted, single-use, rotating** refresh token
   you *can* revoke.
9. Write a **`OncePerRequestFilter`** that reads `Authorization: Bearer`, validates the token, builds an
   `Authentication`, and places it in the `SecurityContext` — registered with `addFilterBefore(...,
   UsernamePasswordAuthenticationFilter.class)`.
10. Reconfigure a `SecurityFilterChain` to be **`STATELESS`** (`SessionCreationPolicy.STATELESS`),
    `permitAll` the auth routes, remove `formLogin`/`httpBasic`, wire a **401 entry point**, and
    articulate **why CSRF is disabled** for a Bearer-token API.
11. Authenticate a login **inside a controller/service** by calling the **`AuthenticationManager`**
    directly (the seam Lab 6 exposed), and externalise the **signing secret and TTLs** to config.

**Assumed background:** you have completed Labs 1–6 (or can read their references) — JPA relationships
and fetch strategy (Lab 3), pagination + `PagedResponse` + `ProblemDetail` (Lab 3/5), and the **entire
Lab 6 security model** (filter chain, `authorizeHttpRequests`, `UserDetailsService`,
`AuthenticationManager`, BCrypt, `@WithMockUser`). Part 1 re-states everything you must build, so you
can do it even if the details are rusty; Part 2 is where the new concepts are taught.

## 3. The Two Big New Ideas

### 3.1 (Part 1) Building a whole secured REST app from an empty directory

Labs 4–6 handed you a finished project and asked for a small delta. Here you produce **every file**.
The skill assessed is *composition*: you already know each layer in isolation — now assemble a coherent
application from nothing, in the right build order, with the security foundation wired correctly so the
tests are green before you touch JWT. Nothing in Part 1 is conceptually new; the challenge is breadth
and correctness.

### 3.2 (Part 2) Stateless authentication: the server stops remembering you

Lab 6 authenticated with **HTTP Basic** (or a form login): every protected request carried
`username:password`, and Spring Security re-verified it against the database each time — or held a
**server-side session** (`HttpSession` + `JSESSIONID` cookie) so subsequent requests were "remembered".

A **JWT-based API is stateless**: the server keeps **no session**. On login, the server hands the client
a **signed token** that *encodes the identity itself* — the username and roles — inside a tamper-evident
envelope. On every later request the client sends `Authorization: Bearer <token>`; a filter verifies the
signature, reads the claims, and rebuilds the `Authentication` **from the token alone**. No session
lookup, no database hit on the hot path, nothing to replicate across servers.

A JWT has three base64url-encoded parts joined by dots — `header.payload.signature`:

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiJhbGljZSIsInJvbGVzIjpbIlVTRVIiXSwiaWF0Ijo... . 3f9Xq...
      header                              payload (claims)                        HMAC signature
```

- The **payload** carries claims: `sub` (subject = username), a custom `roles` claim, and the standard
  `iat`/`exp` timestamps.
- The **signature** is `HMAC-SHA256(header.payload, secret)`. Anyone can *read* the payload (it is only
  base64, **not** encrypted), but nobody without the secret can *forge or alter* it — any change breaks
  the signature. **Therefore: never put a secret in a claim.**

Because a stateless token cannot be revoked server-side (there is no session to invalidate), you make it
**short-lived** (15 minutes) to bound the damage if it leaks, and you pair it with a **long-lived refresh
token** (7 days) that *is* a database row you control — it can be checked, revoked, and **rotated** (each
use revokes the old row and issues a new one, so a stolen-but-already-used refresh token is dead). That
access/refresh split is the heart of Part 2.

## 4. The TaskFlow Domain Model

Three domain entities plus two security entities (`User`, and — added in Part 2 — `RefreshToken`).

```
                +----------------+
                |     User       |   (id, username*, email*, password[bcrypt], roles{USER,ADMIN})
                +----------------+
                  1 |          | 1  (assignee, optional)
      owns projects |          |
                  N v          |
                +----------------+          N          +----------------+
                |    Project     |------------------->? |     Task       |
                +----------------+   1  has tasks       +----------------+
                | id             |                      | id             |
                | name*          |                      | title*         |
                | description    |                      | description    |
                | owner -> User* |                      | status (enum)  |
                +----------------+                      | priority(enum) |
                                                        | dueDate        |
                                                        | project*->Proj |
                                                        | assignee->User |
                                                        +----------------+
```

- **`User 1─N Project`** — every project has exactly one **owner** (`@ManyToOne`, mandatory), set to the
  authenticated principal on create.
- **`Project 1─N Task`** — every task belongs to exactly one **project** (`@ManyToOne`, mandatory).
- **`Task N─1 User` (assignee)** — a task *may* be assigned to a user (`@ManyToOne`, **nullable**).
- Relationships are modelled **only from the "many" side** (each `@ManyToOne`); no bidirectional
  `@OneToMany` collections are required (list tasks by querying `TaskRepository.findByProjectId`). All
  `@ManyToOne`s are **LAZY**, and the finders use `@EntityGraph` to fetch what mapping needs (Lab 3).
- Enums: `Role {USER, ADMIN}`, `Status {TODO, IN_PROGRESS, DONE}`, `Priority {LOW, MEDIUM, HIGH}`, all
  persisted as **strings** (`@Enumerated(EnumType.STRING)`).

## 5. Tech Stack & Project Setup (required)

**Java 21**, **Maven**, **Spring Boot 4.0.6**, **PostgreSQL** at runtime, **H2** (test scope) for tests,
**Lombok**, **Spring Security 7** (via `spring-boot-starter-security`). Part 2 adds **jjwt 0.13.0**. Use
the layered architecture: `controller → service → repository → model(entity)`, plus `dto`, `exception`,
`security`, and (Part 2) `config`.

### 5.1 The Part 1 `pom.xml`

Generate a project (Spring Initializr or by hand) with `groupId=com.example`, `artifactId=_7_jwt`,
Java 21, Spring Boot 4.0.6, and the parent `spring-boot-starter-parent:4.0.6`. The **Part 1**
dependencies (jjwt is added in Part 2, Section 15.1):

```xml
<properties>
    <java.version>21</java.version>
    <postgresql.version>42.7.11</postgresql.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <!-- Spring Security 7. Adding this starter secures EVERY endpoint by default (secure-by-default)
         and turns on CSRF, so an explicit SecurityFilterChain is required. -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
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
    <!-- Spring Boot 4.0 modularized the test slices into dedicated test starters. Each brings
         spring-boot-starter-test transitively, so we do NOT declare it on its own. -->
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
    <!-- @WithMockUser, SecurityMockMvcConfigurers.springSecurity(), the security MockMvc helpers. -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Keep the standard `spring-boot-maven-plugin`, and configure the `maven-compiler-plugin`
`annotationProcessorPaths` for Lombok (so annotation processing runs):

```xml
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
```

### 5.2 The Part 1 `application.properties`

`src/main/resources/application.properties`:

```properties
spring.application.name=_7_jwt

spring.datasource.url=jdbc:postgresql://localhost:5432/taskflow_lab
spring.datasource.username=postgres
spring.datasource.password=123456

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Password for the ADMIN account seeded at startup by AdminUserSeeder. Default is "admin12345"
# (seeded credentials: admin / admin12345). Override in real deployments; the seeder only creates
# the user when it does not already exist.
app.security.admin-password=admin12345
```

Create the database once: `CREATE DATABASE taskflow_lab;` (adjust username/password to your local
PostgreSQL). The `src/test/resources/application.properties` needs only:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

(The `@DataJpaTest`/`@WebMvcTest` slices auto-configure H2 and a fresh schema; no datasource URL is
needed in test.)

### 5.3 The `Application` main class

```java
package com.example._7_jwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 5.4 Spring Boot 4.0 specifics you MUST know (or you will get stuck)

- **Test slices live in dedicated starters** (`spring-boot-starter-data-jpa-test`,
  `spring-boot-starter-webmvc-test`); `spring-boot-starter-test` no longer brings them transitively.
- **The test-slice imports are renamed** (NOT the 3.x packages):
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
  `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`,
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.
- **Jackson 3**: the `ObjectMapper` is `tools.jackson.databind.ObjectMapper` (not `com.fasterxml...`).

### 5.5 Spring Security 7 specifics you MUST know (the REMOVED APIs)

The Spring Security 7 stack has dropped a large surface of the old 5/6 configuration API. **Code using
any of these will not compile**, which is why most online tutorials are wrong for this stack:

- **`WebSecurityConfigurerAdapter` was removed in Security 6.0.** There is **no base class to extend**;
  you declare a **`SecurityFilterChain` `@Bean`** that takes a `HttpSecurity` and returns `http.build()`.
- **`authorizeRequests(...)` was removed in Security 7.0.** Use **`authorizeHttpRequests(...)`**.
- **`antMatchers`/`mvcMatchers`/`regexMatchers` were removed in Security 6.0.** Use
  **`requestMatchers(...)`** (with an `HttpMethod`-first overload).
- **The lambda DSL is mandatory** — every configurer takes a `Customizer` lambda
  (`http.csrf(csrf -> csrf.disable())`, `http.authorizeHttpRequests(auth -> auth. ...)`).
- **Secure-by-default.** Adding the security starter secures **all** endpoints and enables CSRF; you
  must provide a `SecurityFilterChain` to open anything up.
- **Obtain the `AuthenticationManager`** by injecting `AuthenticationConfiguration` and returning
  `configuration.getAuthenticationManager()` from a `@Bean`.
- **`@EnableWebSecurity`** is declared explicitly on your config class so the `HttpSecurity` prototype
  bean and the `AuthenticationConfiguration` are available **inside `@WebMvcTest` slices** that
  `@Import` your `SecurityConfig`.

---

# PART 1 — Build the TaskFlow Base From Nothing

Goal of Part 1: a complete, secured TaskFlow whose `./mvnw test` is **green**, using the **Lab 6
security model** (HTTP Basic, a `SecurityFilterChain`, a DB-backed `UserDetailsService`, BCrypt, a
seeded ADMIN). No JWT yet.

## 6. Part 1 — File Inventory (what you create)

```
src/main/java/com/example/_7_jwt/
├── Application.java
├── model/
│   ├── Role.java            (enum USER, ADMIN)
│   ├── Status.java          (enum TODO, IN_PROGRESS, DONE)
│   ├── Priority.java        (enum LOW, MEDIUM, HIGH)
│   ├── User.java            (@Entity users)
│   ├── Project.java         (@Entity projects)
│   └── Task.java            (@Entity tasks)
├── repository/
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   └── TaskRepository.java
├── dto/
│   ├── RegisterRequest.java     UserResponse.java     UserSummaryDto.java
│   ├── ProjectRequestDto.java   ProjectResponseDto.java   ProjectSummaryDto.java
│   ├── TaskRequestDto.java      TaskResponseDto.java
│   └── PagedResponse.java
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   └── GlobalExceptionHandler.java
├── service/
│   ├── AuthService.java / AuthServiceImpl.java       (register only, in Part 1)
│   ├── UserService.java / UserServiceImpl.java
│   ├── ProjectService.java / ProjectServiceImpl.java
│   └── TaskService.java / TaskServiceImpl.java
├── security/
│   ├── UserDetailsServiceImpl.java
│   ├── SecurityConfig.java        (HTTP Basic version, in Part 1)
│   └── AdminUserSeeder.java
└── controller/
    ├── AuthController.java        (POST /register only, in Part 1)
    ├── UserController.java        (GET /api/users/me)
    ├── ProjectController.java
    ├── TaskController.java
    └── AdminController.java       (GET /api/admin/users)
```

Sections 7.x describe **what each class is for and what it must do** — not how to code it. Use it as
your build checklist.

## 7. Part 1 — Complete Implementation Inventory (class-by-class)

### 7.1 `model` package — three enums + three entities

Every entity uses Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.

#### `Role`, `Status`, `Priority` — plain enums

```java
public enum Role { USER, ADMIN }
public enum Status { TODO, IN_PROGRESS, DONE }
public enum Priority { LOW, MEDIUM, HIGH }
```

`Role` is stored as strings in the `user_roles` table and mapped to Spring Security authorities with the
`ROLE_` prefix (`Role.ADMIN` → `ROLE_ADMIN`).

#### `User` — the application user (`@Entity`, table `users`)

Add `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on the class. The `password` column **always
holds a BCrypt hash, never plaintext** (hashed in `AuthServiceImpl` before persistence).

| Field | Type | Mapping | Notes |
| ----- | ---- | ------- | ----- |
| `id` | `Long` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | surrogate key |
| `username` | `String` | `@Column(nullable = false, unique = true)` + `@EqualsAndHashCode.Include` | the login name (identity by natural key) |
| `email` | `String` | `@Column(nullable = false, unique = true)` | unique; `@Email` is enforced at the DTO, not here |
| `password` | `String` | `@Column(nullable = false)` | the **BCrypt hash** |
| `roles` | `Set<Role>` | `@Builder.Default` + `@ElementCollection(fetch = FetchType.EAGER)` + `@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))` + `@Enumerated(EnumType.STRING)` + `@Column(name = "role", nullable = false)`, initialised `= new HashSet<>()` | roles in a dedicated join table |

- **`fetch = FetchType.EAGER`** on the roles is deliberate: authorities must be available the moment the
  user is loaded for authentication (`UserDetailsServiceImpl` reads them immediately, outside any open
  session in the test slices).
- **`@Builder.Default`** on `roles` stops the builder from resetting the initialised set to `null`.

#### `Project` — a project owned by a `User` (`@Entity`, table `projects`)

| Field | Type | Mapping |
| ----- | ---- | ------- |
| `id` | `Long` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `name` | `String` | `@Column(nullable = false)` |
| `description` | `String` | `@Column(length = 2000)` |
| `owner` | `User` | `@ManyToOne(fetch = FetchType.LAZY, optional = false)` + `@JoinColumn(name = "owner_id", nullable = false)` |

The owner is set to the authenticated principal on create (`ProjectServiceImpl`). **Do not** reassign it
on update, and **do not** enforce owner-only mutation — that is Lab 8.

#### `Task` — a task inside a `Project` (`@Entity`, table `tasks`)

| Field | Type | Mapping |
| ----- | ---- | ------- |
| `id` | `Long` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `title` | `String` | `@Column(nullable = false)` |
| `description` | `String` | `@Column(length = 2000)` |
| `status` | `Status` | `@Builder.Default` + `@Enumerated(EnumType.STRING)` + `@Column(nullable = false)`, `= Status.TODO` |
| `priority` | `Priority` | `@Builder.Default` + `@Enumerated(EnumType.STRING)` + `@Column(nullable = false)`, `= Priority.MEDIUM` |
| `dueDate` | `LocalDate` | (plain column, nullable) |
| `project` | `Project` | `@ManyToOne(fetch = FetchType.LAZY, optional = false)` + `@JoinColumn(name = "project_id", nullable = false)` |
| `assignee` | `User` | `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = "assignee_id")` — **nullable** |

`status`/`priority` default to `TODO`/`MEDIUM` when a request omits them; `assignee` is null for an
unassigned task.

### 7.2 `repository` package — three interfaces

#### `UserRepository extends JpaRepository<User, Long>`

| Method (exact signature) | Used by |
| ------------------------ | ------- |
| `Optional<User> findByUsername(String username);` | `UserDetailsServiceImpl`, `UserServiceImpl`, `ProjectServiceImpl` |
| `boolean existsByUsername(String username);` | `AuthServiceImpl.register`, `AdminUserSeeder` |
| `boolean existsByEmail(String email);` | `AuthServiceImpl.register` (email duplicate check) |

#### `ProjectRepository extends JpaRepository<Project, Long>`

Override the two finders with an `@EntityGraph` so listing projects does not N+1 when a
`ProjectResponseDto` reads `owner.username`:

| Method | Annotation |
| ------ | ---------- |
| `@Override Page<Project> findAll(Pageable pageable);` | `@EntityGraph(attributePaths = "owner")` |
| `@Override Optional<Project> findById(Long id);` | `@EntityGraph(attributePaths = "owner")` |

#### `TaskRepository extends JpaRepository<Task, Long>`

Every finder pulls the `@ManyToOne project` + `assignee` sides via an entity graph (assignee may be
null):

| Method | Annotation |
| ------ | ---------- |
| `@Override Page<Task> findAll(Pageable pageable);` | `@EntityGraph(attributePaths = {"project", "assignee"})` |
| `@Override Optional<Task> findById(Long id);` | `@EntityGraph(attributePaths = {"project", "assignee"})` |
| `Page<Task> findByProjectId(Long projectId, Pageable pageable);` | `@EntityGraph(attributePaths = {"project", "assignee"})` |
| `Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);` | `@EntityGraph(attributePaths = {"project", "assignee"})` |
| `Page<Task> findByStatus(Status status, Pageable pageable);` | `@EntityGraph(attributePaths = {"project", "assignee"})` |

### 7.3 `dto` package — nine classes

All are Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` (the two **request** DTOs also
add `@Builder`, which the tests use). The API exposes **DTOs only** — entities are never serialized.

#### Request DTOs (with Jakarta Validation)

**`RegisterRequest`** — inbound for `POST /api/auth/register`:

| Field | Validation |
| ----- | ---------- |
| `username` (`String`) | `@NotBlank(message = "Username is required")` |
| `email` (`String`) | `@NotBlank(message = "Email is required")` + `@Email(message = "Email must be a valid address")` |
| `password` (`String`) | `@NotBlank(message = "Password is required")` + `@Size(min = 8, message = "Password must be at least 8 characters")` |

**`ProjectRequestDto`** (`@Builder`): `name` (`@NotBlank(message = "Name is required")`), `description`
(`String`, no validation). The owner is **not** in the request — it is server-controlled.

**`TaskRequestDto`** (`@Builder`):

| Field | Validation |
| ----- | ---------- |
| `title` (`String`) | `@NotBlank(message = "Title is required")` |
| `description` (`String`) | — |
| `status` (`Status`) | — (absent → service applies `TODO`) |
| `priority` (`Priority`) | — (absent → service applies `MEDIUM`) |
| `dueDate` (`LocalDate`) | — |
| `projectId` (`Long`) | `@NotNull(message = "Project id is required")` |
| `assigneeId` (`Long`) | — (nullable) |

#### Response DTOs

- **`UserResponse`**: `id`, `username`, `email`, `Set<Role> roles`. **No password field** — the BCrypt
  hash must never cross the API boundary.
- **`UserSummaryDto`**: `id`, `username` (compact user reference nested in project/task responses).
- **`ProjectResponseDto`**: `id`, `name`, `description`, `UserSummaryDto owner`.
- **`ProjectSummaryDto`**: `id`, `name` (compact project reference nested in task responses).
- **`TaskResponseDto`**: `id`, `title`, `description`, `Status status`, `Priority priority`, `LocalDate
  dueDate`, `ProjectSummaryDto project`, `UserSummaryDto assignee` (assignee null when unassigned).

#### `PagedResponse<T>` — the house page wrapper

Fields `List<T> content`, `int page`, `int size`, `long totalElements`, `int totalPages`, `boolean
last`, plus a static factory so controllers never leak Spring's raw `Page`:

```java
public static <E, T> PagedResponse<T> from(Page<E> page, List<T> content) {
    return new PagedResponse<>(content, page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(), page.isLast());
}
```

### 7.4 `exception` package — two exceptions + one handler

- **`ResourceNotFoundException extends RuntimeException`** — two constructors: `(String resource, Long
  id)` → message `resource + " not found with id: " + id`, and `(String resource, String identifier)` →
  `resource + " not found: " + identifier`. Rendered as **404**.
- **`DuplicateResourceException extends RuntimeException`** — `(String field, String value)` → message
  `field + " already in use: " + value`. Rendered as **409**.

#### `GlobalExceptionHandler` — `@RestControllerAdvice` (RFC 7807 `ProblemDetail`)

The **Part 1** handler maps five exception types. Each builds a `ProblemDetail.forStatusAndDetail(...)`
and sets a title:

| `@ExceptionHandler` | Status | Title | Detail |
| ------------------- | ------ | ----- | ------ |
| `ResourceNotFoundException` | `404` | `"Resource not found"` | `ex.getMessage()` |
| `DuplicateResourceException` | `409` | `"Duplicate value"` | `ex.getMessage()` |
| `MethodArgumentNotValidException` | `400` | `"Validation failed"` | `"Request validation failed"` + an `errors` map (`field → default message`) via `pd.setProperty("errors", errors)` |
| `MethodArgumentTypeMismatchException` | `400` | `"Invalid parameter"` | `"Parameter '" + ex.getName() + "' has an invalid value"` |
| `Exception` | `500` | `"Internal server error"` | `"An unexpected error occurred"` |

For the validation handler, iterate `ex.getBindingResult().getFieldErrors()` into a `LinkedHashMap<String,
String>` (fall back to `"Invalid value"` when a default message is null).

> **Part 2 will add two more handlers here** (`AuthenticationException` → 401 and `InvalidTokenException`
> → 401) — Section 16.9. Don't add them yet; there is no login endpoint in Part 1.

### 7.5 `service` package — four service pairs (interface + `@Service` impl)

All impls are `@Service @RequiredArgsConstructor @Transactional(readOnly = true)`; write methods add
`@Transactional`. Mapping is **manual** (private `toResponse(...)` helpers) — no MapStruct in this lab.

#### `AuthService` / `AuthServiceImpl` — **Part 1: registration only**

Interface (Part 1):

```java
UserResponse register(RegisterRequest request);
```

`AuthServiceImpl` injects `UserRepository` and `PasswordEncoder`. `register` (annotated `@Transactional`)
must:

1. **Reject a duplicate username**: `if (userRepository.existsByUsername(request.getUsername())) throw
   new DuplicateResourceException("username", request.getUsername());`
2. **Reject a duplicate email**: `if (userRepository.existsByEmail(request.getEmail())) throw new
   DuplicateResourceException("email", request.getEmail());`
3. **BCrypt-hash the password**: build the `User` with
   `.password(passwordEncoder.encode(request.getPassword()))` — never store the raw password.
4. **Assign the default role**: self-registered users always start with **`Role.USER`**
   (`.roles(new HashSet<>(Set.of(Role.USER)))`). ADMIN is granted **only** by the seeder.
5. **Save and map**: `User saved = userRepository.save(user);` then `return new UserResponse(saved.getId(),
   saved.getUsername(), saved.getEmail(), saved.getRoles());`

> **Part 1 stops here, but the finished interface has three methods.** Part 2 adds
> `TokenResponse login(LoginRequest)` and `TokenResponse refresh(RefreshRequest)`, and grows
> `AuthServiceImpl`'s constructor from `(UserRepository, PasswordEncoder)` to five arguments —
> **Section 16.8**. The complete final interface is listed there and again in **Section 23.1**.

#### `UserService` / `UserServiceImpl` — read-only user queries

| Interface method | Impl responsibility |
| ---------------- | ------------------- |
| `UserResponse getByUsername(String username)` | `findByUsername(...).orElseThrow(() -> new ResourceNotFoundException("User", username))`, map to `UserResponse` |
| `UserResponse getCurrentUser()` | read the principal from `SecurityContextHolder.getContext().getAuthentication()`; if null or not authenticated → `ResourceNotFoundException("User", "current")`; else `getByUsername(authentication.getName())` |
| `List<UserResponse> getAllUsers()` | `userRepository.findAll().stream().map(this::toResponse).toList()` (for the ADMIN listing) |

`getCurrentUser` is **mechanism-agnostic**: it reads whatever populated the `SecurityContext` — HTTP
Basic in Part 1, the JWT filter in Part 2 — with no code change.

#### `ProjectService` / `ProjectServiceImpl` — project CRUD (ownership = principal on create)

Inject `ProjectRepository` and `UserRepository`.

| Interface method | Impl responsibility |
| ---------------- | ------------------- |
| `ProjectResponseDto createProject(ProjectRequestDto request, String ownerUsername)` | `@Transactional`. Resolve `owner = userRepository.findByUsername(ownerUsername).orElseThrow(() -> new ResourceNotFoundException("User", ownerUsername))`; build `Project` with name/description/owner; save; map. |
| `PagedResponse<ProjectResponseDto> getAllProjects(Pageable pageable)` | `findAll(pageable)` → `PagedResponse.from(page, mapped)` |
| `ProjectResponseDto getProjectById(Long id)` | `findById(...).orElseThrow(...)` → map |
| `ProjectResponseDto updateProject(Long id, ProjectRequestDto request)` | `@Transactional`. Load, set name/description, **leave owner untouched**, save, map. |
| `void deleteProject(Long id)` | `@Transactional`. `if (!existsById(id)) throw ResourceNotFoundException; deleteById(id);` |

`toResponse(Project p)` builds a `UserSummaryDto` from the owner (guard null) and returns a
`ProjectResponseDto`.

> **Ownership rule (and the Lab 8 boundary):** `createProject` sets the owner to the caller's principal.
> `updateProject` does **not** reassign the owner and does **not** check that the caller *is* the owner —
> any authenticated user can mutate any project in Lab 7. Per-row ownership enforcement is **Lab 8**.

#### `TaskService` / `TaskServiceImpl` — task CRUD (service owns id-resolution)

Inject `TaskRepository`, `ProjectRepository`, `UserRepository`.

| Interface method | Impl responsibility |
| ---------------- | ------------------- |
| `TaskResponseDto createTask(TaskRequestDto request)` | `@Transactional`. Resolve `projectId` → `Project` (mandatory, 404 if missing); resolve `assigneeId` → `User` (null id → null assignee; non-null unknown id → 404 "User"); build `Task` (status `?: TODO`, priority `?: MEDIUM`); save; map. |
| `PagedResponse<TaskResponseDto> getTasks(Status status, Long assigneeId, Pageable pageable)` | if `status != null` → `findByStatus`; else if `assigneeId != null` → `findByAssigneeId`; else `findAll`. (Status wins if both supplied.) Map to paged. |
| `PagedResponse<TaskResponseDto> getTasksByProject(Long projectId, Pageable pageable)` | `if (!projectRepository.existsById(projectId)) throw ResourceNotFoundException("Project", projectId);` then `findByProjectId(...)` → paged. |
| `TaskResponseDto getTaskById(Long id)` | `findById(...).orElseThrow(...)` → map |
| `TaskResponseDto updateTask(Long id, TaskRequestDto request)` | `@Transactional`. Load; re-resolve project + assignee; set all fields (status `?: TODO`, priority `?: MEDIUM`); save; map. |
| `void deleteTask(Long id)` | `@Transactional`. exists-check then `deleteById`. |

Private helpers: `resolveProject(Long)` (404 if missing), `resolveAssignee(Long)` (null → null, else 404
if unknown), `toResponse(Task)` (builds `ProjectSummaryDto` + nullable `UserSummaryDto`), and
`toPaged(Page<Task>)`.

### 7.6 `security` package — the Lab 6 foundation (three classes)

#### `UserDetailsServiceImpl` — bridges your `User` table to Spring Security

`@Service @RequiredArgsConstructor` implementing
`org.springframework.security.core.userdetails.UserDetailsService`, injecting `UserRepository`.

```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .toList();

    // Adapt to the framework's built-in UserDetails implementation.
    return new org.springframework.security.core.userdetails.User(
            user.getUsername(), user.getPassword(), authorities);
}
```

It must (1) throw **`UsernameNotFoundException`** (a Spring Security type — **not** your
`ResourceNotFoundException`) when absent, (2) map each `Role` to `SimpleGrantedAuthority("ROLE_" +
role.name())` (the `ROLE_` prefix is what makes `hasRole("ADMIN")` line up), and (3) return the framework
`org.springframework.security.core.userdetails.User` carrying the **stored BCrypt hash**.

> **Two `User` classes — do not confuse them.** `com.example._7_jwt.model.User` is *your* JPA entity;
> `org.springframework.security.core.userdetails.User` is the framework's `UserDetails` adapter.
> Fully-qualify the framework one (as above).

#### `SecurityConfig` — **Part 1 (HTTP Basic) version**

`@Configuration @EnableWebSecurity` declaring **three** beans. This is the Lab 6 model applied to
TaskFlow's routes. (Part 2 rewrites this class for JWT — Section 16.11.)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** BCrypt: adaptive, salted password hashing. encode() on register, matches() on login. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())      // DELIBERATE: non-browser API using HTTP Basic (Part 2 revisits)
            .authorizeHttpRequests(auth -> auth
                // Public: self-service registration (a user cannot log in before they exist)
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                // ADMIN-only administration area
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Everything else (projects, tasks, GET /api/users/me) requires authentication
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())     // 401 + WWW-Authenticate: Basic on anonymous
            .formLogin(Customizer.withDefaults());     // default browser form login
        return http.build();
    }

    /**
     * Expose the AuthenticationManager built from the AuthenticationConfiguration. Part 1 does not
     * call it directly (httpBasic/formLogin drive authentication); PART 2's /login endpoint WILL.
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
```

Rule ordering: matchers are evaluated **top-to-bottom, first match wins**. `register` is `permitAll`
first; `/api/admin/**` is ADMIN-only; the `anyRequest().authenticated()` catch-all locks down everything
else (projects, tasks, `/api/users/me`). Unlike the Bookstore, **there are no public reads** — TaskFlow
is a private workspace.

Imports you will need (given as **full** FQNs — note that `HttpSecurity`, `EnableWebSecurity`, and
`AuthenticationConfiguration` all live under `org.springframework.security.config.annotation`, **not**
directly under `org.springframework.security`):

```java
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpMethod;
```

> **The deliberate CSRF decision.** `.csrf(csrf -> csrf.disable())` is intentional, not a shortcut. CSRF
> protects **cookie/session-authenticated browser form posts** (where the browser attaches the session
> cookie automatically). This API is driven by **non-browser clients sending an explicit
> `Authorization` header** a cross-site page cannot silently attach, so CSRF tokens add friction without
> protection. **Part 2 makes this decision unambiguous** (a Bearer-token API has no ambient cookie at
> all). Leave the one-line disable with a comment.

#### `AdminUserSeeder` — seeds one ADMIN at startup

`@Component @RequiredArgsConstructor @Slf4j` implementing **`ApplicationRunner`**, injecting
`UserRepository` + `PasswordEncoder`. Self-registration only ever yields `USER`, so an ADMIN must be
bootstrapped.

| Detail | Requirement |
| ------ | ----------- |
| Password source | `@Value("${app.security.admin-password:admin12345}") private String adminPassword;` |
| Idempotency | `if (userRepository.existsByUsername("admin")) return;` before creating |
| The user | `User.builder().username("admin").email("admin@taskflow.local").password(passwordEncoder.encode(adminPassword)).roles(new HashSet<>(Set.of(Role.ADMIN))).build()`, then `save` |
| Logging | `log.info("Seeded default ADMIN user 'admin'")` |

> **Why an `ApplicationRunner` `@Component`, not a `@Bean` in a `@Configuration`?** The web/JPA test
> slices (`@WebMvcTest`, `@DataJpaTest`) **do not scan arbitrary `@Component`s**, so the seeder never
> runs during those tests and cannot pollute their contexts. It runs only in the full application.

### 7.7 `controller` package — five controllers (Part 1)

All are `@RestController @RequiredArgsConstructor`. Handlers return `ResponseEntity`.

#### `AuthController` — `@RequestMapping("/api/auth")` (Part 1: register only)

| Handler | Mapping | Returns |
| ------- | ------- | ------- |
| `register(@Valid @RequestBody RegisterRequest request)` | `@PostMapping("/register")` | `201 CREATED`, `UserResponse` body |

Body: `UserResponse created = authService.register(request); return
ResponseEntity.status(HttpStatus.CREATED).body(created);`. (Part 2 adds `/login` and `/refresh` here.)

#### `UserController` — `@RequestMapping("/api/users")`

| Handler | Mapping | Returns |
| ------- | ------- | ------- |
| `me()` | `@GetMapping("/me")` | `200`, `userService.getCurrentUser()` |

Requires authentication (anonymous → 401 from the filter chain).

#### `ProjectController` — `@RequestMapping("/api/projects")`

Injects `ProjectService` **and** `TaskService` (for the nested task listing).

| Handler | Mapping | Returns |
| ------- | ------- | ------- |
| `create(@Valid @RequestBody ProjectRequestDto request, Principal principal)` | `@PostMapping` | `201` + `Location` header; `createProject(request, principal.getName())` — **owner = the caller's principal** |
| `getAll(@PageableDefault(size = 20, sort = "name") Pageable pageable)` | `@GetMapping` | `200`, `PagedResponse<ProjectResponseDto>` |
| `getById(@PathVariable Long id)` | `@GetMapping("/{id}")` | `200` |
| `update(@PathVariable Long id, @Valid @RequestBody ProjectRequestDto request)` | `@PutMapping("/{id}")` | `200` |
| `delete(@PathVariable Long id)` | `@DeleteMapping("/{id}")` | `204` |
| `getTasks(@PathVariable Long id, @PageableDefault(size = 20, sort = "title") Pageable pageable)` | `@GetMapping("/{id}/tasks")` | `200`, paged tasks; delegates to `taskService.getTasksByProject(id, pageable)` |

Build the `Location` with `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
.buildAndExpand(created.getId()).toUri()` and `ResponseEntity.created(location).body(created)`. The
`Principal` argument is injected by Spring from the `SecurityContext` — its `getName()` is the username.

#### `TaskController` — `@RequestMapping("/api/tasks")`

| Handler | Mapping | Returns |
| ------- | ------- | ------- |
| `create(@Valid @RequestBody TaskRequestDto request)` | `@PostMapping` | `201` + `Location` |
| `getAll(@RequestParam(required = false) Status status, @RequestParam(required = false) Long assigneeId, @PageableDefault(size = 20, sort = "title") Pageable pageable)` | `@GetMapping` | `200`, paged; delegates to `getTasks(status, assigneeId, pageable)` |
| `getById(@PathVariable Long id)` | `@GetMapping("/{id}")` | `200` |
| `update(@PathVariable Long id, @Valid @RequestBody TaskRequestDto request)` | `@PutMapping("/{id}")` | `200` |
| `delete(@PathVariable Long id)` | `@DeleteMapping("/{id}")` | `204` |

#### `AdminController` — `@RequestMapping("/api/admin")`

| Handler | Mapping | Returns |
| ------- | ------- | ------- |
| `listUsers()` | `@GetMapping("/users")` | `200`, `List<UserResponse>` (**not** paged); `userService.getAllUsers()` |

The whole `/api/admin/**` area is `hasRole("ADMIN")` in `SecurityConfig`: anonymous → 401, plain USER →
403, ADMIN → 200.

## 8. Part 1 — API Contract & Authorization Matrix

### 8.1 Endpoints

| Method | Path | Auth (Part 1) | Success | Errors |
| ------ | ---- | ------------- | ------- | ------ |
| POST | `/api/auth/register` | public | `201` `UserResponse` | `400` validation; `409` username/email taken |
| GET | `/api/users/me` | authenticated | `200` `UserResponse` | `401` anonymous |
| POST | `/api/projects` | authenticated | `201` + `Location`, `ProjectResponseDto` | `400`; `401` |
| GET | `/api/projects` | authenticated | `200` `PagedResponse<ProjectResponseDto>` | `401` |
| GET | `/api/projects/{id}` | authenticated | `200` | `401`; `404` |
| PUT | `/api/projects/{id}` | authenticated | `200` | `400`; `401`; `404` |
| DELETE | `/api/projects/{id}` | authenticated | `204` | `401`; `404` |
| GET | `/api/projects/{id}/tasks` | authenticated | `200` `PagedResponse<TaskResponseDto>` | `401`; `404` |
| POST | `/api/tasks` | authenticated | `201` + `Location`, `TaskResponseDto` | `400`; `401`; `404` (unknown project/assignee) |
| GET | `/api/tasks` (`?status=&assigneeId=`) | authenticated | `200` paged | `400` (bad enum); `401` |
| GET | `/api/tasks/{id}` | authenticated | `200` | `400` (non-numeric id); `401`; `404` |
| PUT | `/api/tasks/{id}` | authenticated | `200` | `400`; `401`; `404` |
| DELETE | `/api/tasks/{id}` | authenticated | `204` | `401`; `404` |
| GET | `/api/admin/users` | **ADMIN** | `200` `List<UserResponse>` | `401` anonymous; `403` USER |

### 8.2 Authorization matrix

**Anonymous → 401** ("who are you?"); **authenticated-but-forbidden → 403** ("I know who you are, and
no").

| Endpoint | Anonymous | USER | ADMIN |
| -------- | :-------: | :--: | :---: |
| `POST /api/auth/register` | ✅ 201 | ✅ 201 | ✅ 201 |
| `GET /api/users/me` | ❌ 401 | ✅ 200 | ✅ 200 |
| `POST /api/projects` | ❌ 401 | ✅ 201 | ✅ 201 |
| `GET /api/projects`, `/{id}`, `/{id}/tasks` | ❌ 401 | ✅ 200 | ✅ 200 |
| `PUT /api/projects/{id}` | ❌ 401 | ✅ 200 | ✅ 200 |
| `DELETE /api/projects/{id}` | ❌ 401 | ✅ 204 | ✅ 204 |
| `POST /api/tasks` | ❌ 401 | ✅ 201 | ✅ 201 |
| `GET /api/tasks`, `/{id}` | ❌ 401 | ✅ 200 | ✅ 200 |
| `PUT /api/tasks/{id}` | ❌ 401 | ✅ 200 | ✅ 200 |
| `DELETE /api/tasks/{id}` | ❌ 401 | ✅ 204 | ✅ 204 |
| `GET /api/admin/users` | ❌ 401 | ⛔ 403 | ✅ 200 |

### 8.3 Sample bodies

`POST /api/auth/register` request / `201` response (no password field):

```json
// request
{ "username": "alice", "email": "alice@taskflow.io", "password": "password123" }
// 201 response
{ "id": 1, "username": "alice", "email": "alice@taskflow.io", "roles": ["USER"] }
```

`POST /api/projects` `201` response:

```json
{ "id": 7, "name": "Apollo", "description": "Moon landing",
  "owner": { "id": 1, "username": "alice" } }
```

`POST /api/tasks` `201` response:

```json
{ "id": 50, "title": "Design the lander", "description": "Sketch the descent stage",
  "status": "TODO", "priority": "HIGH", "dueDate": "2026-03-01",
  "project": { "id": 7, "name": "Apollo" },
  "assignee": { "id": 2, "username": "bob" } }
```

`409` (duplicate) and `400` (validation) are the standard `ProblemDetail`:

```json
{ "title": "Duplicate value", "status": 409, "detail": "username already in use: alice" }
{ "title": "Validation failed", "status": 400, "detail": "Request validation failed",
  "errors": { "password": "Password must be at least 8 characters" } }
```

## 9. Part 1 — Business Rules

- **Secure-by-default is intentional**: only the traffic `SecurityFilterChain` opens is reachable.
- **Registration is public**; **everything else requires authentication** (no public reads).
- **`/api/admin/**` requires ADMIN**; a plain USER there → 403.
- Passwords are **BCrypt-hashed** on registration, **never** stored/logged/returned in plaintext;
  `UserResponse` has **no** password field.
- **Both `username` and `email` are unique**; a clash on either → **409** (`DuplicateResourceException`
  naming the offending field).
- Self-registration yields **`Role.USER`**; **ADMIN** is granted only by the seeder.
- Roles map to authorities with the **`ROLE_` prefix** (`Role.ADMIN` → `ROLE_ADMIN`).
- A project's **owner** is set to the **authenticated principal on create** and is **not** reassigned on
  update. **Ownership is not enforced** on update/delete in Lab 7 (any authenticated user may mutate any
  row) — that enforcement is **Lab 8**.
- A task's `projectId` is **mandatory** (404 if unknown); `assigneeId` is **optional** (404 only if a
  non-null id is unknown); absent `status`/`priority` default to `TODO`/`MEDIUM`.
- CSRF is disabled **deliberately and documented** (revisited in Part 2).

## 10. Part 1 — Testing (four layers; all green with `./mvnw test` on H2)

### 10.1 Spring Boot 4.0 / Spring Security test imports

```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;         // @DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;        // TestEntityManager
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;            // @WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean;       // @MockitoBean
import org.springframework.security.test.context.support.WithMockUser;           // @WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers; // springSecurity()
import org.springframework.test.web.servlet.setup.MockMvcBuilders;               // webAppContextSetup(...)
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;         // the @MockitoBean type
import tools.jackson.databind.ObjectMapper;                                      // Jackson 3!
```

### 10.2 The `@WebMvcTest`-with-security recipe (shared by every controller slice test)

A plain `@WebMvcTest` does **not** load your `SecurityConfig`, so without help the slice tests do not
exercise your rules. For **every** controller slice test:

1. Add **`@Import(SecurityConfig.class)`** so the slice enforces the real rules.
2. Add a **`@MockitoBean UserDetailsService userDetailsService;`** — the imported config's
   `AuthenticationManager`/`DaoAuthenticationProvider` needs one, and the slice does not load the
   `@Service` impl.
3. Build `MockMvc` from the `WebApplicationContext` with `springSecurity()` applied (so the filter chain
   runs **and** `@WithMockUser`'s `SecurityContext` is bridged into each request):

```java
@Autowired private WebApplicationContext context;
private MockMvc mockMvc;

@BeforeEach
void setUpMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
}
```

4. Drive the principal with **`@WithMockUser(roles = "USER" | "ADMIN")`** (omit it for anonymous cases).
   `@WithMockUser` **adds the `ROLE_` prefix for you** — pass `"ADMIN"`, never `"ROLE_ADMIN"` (that
   becomes `ROLE_ROLE_ADMIN`). Use `@WithMockUser(username = "alice", roles = "USER")` when the handler
   reads the principal name (e.g. `ProjectController.create`).

> There must be exactly **one** `mockMvc` in the class — the one built in `@BeforeEach`. Do **not** also
> `@Autowired` an auto-configured `MockMvc`: it would not have `springSecurity()` applied, so the filter
> chain would not run and your `401`/`403` assertions would silently pass for the wrong reason.

### 10.3 Test classes (Part 1)

| Class | Layer | What to verify |
| ----- | ----- | -------------- |
| **`EntityMappingTest`** | `@DataJpaTest` | Persist a `Task` → `Project` (with owner) + assignee and reload every relationship; a `Task` built without status/priority/assignee defaults to `TODO`/`MEDIUM`/null. |
| **`UserRepositoryTest`** | `@DataJpaTest` | `findByUsername` returns the user with eagerly-loaded roles; empty when absent; `existsByUsername`/`existsByEmail` reflect presence. |
| **`TaskRepositoryTest`** | `@DataJpaTest` | `findByProjectId`/`findByAssigneeId` (unassigned excluded)/`findByStatus` filter correctly; `findAll` paginates and sorts. |
| **`UserDetailsServiceImplTest`** | Mockito | Loads a user and exposes each role as a `ROLE_` authority carrying the stored hash; throws `UsernameNotFoundException` (message contains the username) when absent. |
| **`AuthServiceImplTest`** | Mockito | (Part 1 subset) `register` returns id/username/email with default `USER`; stores a **BCrypt hash** (never the raw password); throws `DuplicateResourceException` for a taken **username** and a taken **email**, saving nothing. |
| **`UserServiceImplTest`** | Mockito | `getByUsername` maps / 404s; `getCurrentUser` resolves the principal from the `SecurityContext`; `getAllUsers` maps every user. Clear the context in `@AfterEach`. |
| **`ProjectServiceImplTest`** | Mockito | `createProject` sets owner = principal; 404 when the principal has no `User` row; `getProjectById`/`deleteProject` 404 when missing; `updateProject` changes name/description but **keeps the owner** (`verify(userRepository, never()).findByUsername(any())`); `getAllProjects` maps a page. |
| **`TaskServiceImplTest`** | Mockito | `createTask` resolves project + assignee and nests both; defaults `TODO`/`MEDIUM` + null assignee (and does not touch `userRepository`); 404 on unknown project / unknown non-null assignee; `getTasks` picks the right finder (status wins); `getTasksByProject` 404s on unknown project; `updateTask` re-resolves. |
| **`AuthControllerTest`** | `@WebMvcTest(AuthController.class)` | `POST /register` runs **anonymously** (permitAll) → `201` with `id/username/email/roles[0]==USER` and **`$.password` absent**; `409` duplicate (`$.title=="Duplicate value"`); `400` blank username / invalid email / short password with the exact `$.errors.*` messages. |
| **`UserControllerTest`** | `@WebMvcTest(UserController.class)` | `GET /api/users/me` anonymous → `401`; `@WithMockUser` → `200` with `username/email/roles[0]`. |
| **`ProjectControllerTest`** | `@WebMvcTest(ProjectController.class)` | anonymous `POST`/`GET`/`DELETE` → `401`; `@WithMockUser(username="alice", roles="USER")` `POST` → `201` + `Location` + `owner.username=="alice"` (capture with `eq("alice")`); `400` blank name; `GET` list/by-id (200) + 404; `PUT` 200; `DELETE` 204; `GET /{id}/tasks` 200 (mock `taskService`). |
| **`TaskControllerTest`** | `@WebMvcTest(TaskController.class)` | anonymous `POST` → `401`; `@WithMockUser(roles="USER")` `POST` 201 + `Location` + nested body; `400` missing `projectId`; `GET` binds `?status`+`?assigneeId` (verify with `eq(Status.DONE), eq(5L)`); `?status=BOGUS` → `400` `"Invalid parameter"`; `/abc` non-numeric id → `400`; `GET /{id}` 200 + 404; `PUT` 200; `DELETE` 204. |
| **`AdminControllerTest`** | `@WebMvcTest(AdminController.class)` | `GET /api/admin/users` anonymous → `401`; `@WithMockUser(roles="USER")` → `403`; `@WithMockUser(roles="ADMIN")` → `200` with the user list and **no password field**. |

**Mockito service tests** use `@ExtendWith(MockitoExtension.class)`, `@Mock` repositories, `@InjectMocks`
the impl. In `AuthServiceImplTest`, use a **real** `BCryptPasswordEncoder` so `matches(...)` proves a
genuine hash — build the service by hand in `@BeforeEach` (`new AuthServiceImpl(userRepository,
passwordEncoder)` in Part 1).

**`@DataJpaTest`** seeds via `TestEntityManager` (`persist` + `flush()` + `clear()` so reloads hit the DB).

## 11. Part 1 — Suggested Build Order

1. Create the Maven project (Section 5.1), `application.properties` (5.2), `Application` (5.3). Create
   the `taskflow_lab` database. Confirm `./mvnw test` runs (no tests yet, but the context is sane once
   you add the first entity).
2. `model` — the three enums, then `User`, `Project`, `Task`. Write **`EntityMappingTest`** and green it.
3. `repository` — `UserRepository`, `ProjectRepository`, `TaskRepository`. Write **`UserRepositoryTest`**
   and **`TaskRepositoryTest`**.
4. `dto` (all nine) + `exception` (two exceptions + `GlobalExceptionHandler`).
5. `security` — `UserDetailsServiceImpl` (+ **`UserDetailsServiceImplTest`**), `SecurityConfig` (the
   HTTP-Basic version), `AdminUserSeeder`.
6. `service` — `UserService`, `ProjectService`, `TaskService`, and `AuthService` (register only), with
   their **Mockito tests**.
7. `controller` — `AuthController`, `UserController`, `ProjectController`, `TaskController`,
   `AdminController`, with their **`@WebMvcTest` slice tests** (Section 10.2 recipe).
8. Run `./mvnw test` — **all green**. Start the app, log in as the seeded `admin` / `admin12345`, and
   exercise the API (Section 12). **Do not proceed to Part 2 until Part 1 is green.**

## 12. Part 1 — Manual Testing Flow (curl, HTTP Basic)

With the app running (the seeder created `admin` / `admin12345`):

1. **Anonymous read → 401:** `curl -i http://localhost:8080/api/projects` → **`401`** (WWW-Authenticate:
   Basic).
2. **Register a USER:** `curl -i -X POST localhost:8080/api/auth/register -H "Content-Type:
   application/json" -d '{"username":"alice","email":"alice@taskflow.io","password":"password123"}'` →
   **`201`**, `{"id":...,"roles":["USER"]}` (no password).
3. **USER creates a project:** `curl -i -u alice:password123 -X POST localhost:8080/api/projects -H
   "Content-Type: application/json" -d '{"name":"Apollo","description":"Moon landing"}'` → **`201`**,
   `owner.username=="alice"`.
4. **USER on the admin area → 403:** `curl -i -u alice:password123 localhost:8080/api/admin/users` →
   **`403`**.
5. **ADMIN lists users → 200:** `curl -i -u admin:admin12345 localhost:8080/api/admin/users` → **`200`**.
6. **Wrong password → 401:** `curl -i -u alice:wrong localhost:8080/api/projects` → **`401`**.

The `401` vs `403` distinction (steps 1/6 vs 4) is the point: **401 = not authenticated**, **403 =
authenticated but insufficient authority**.

## 13. Part 1 — Acceptance Criteria

- [ ] The Maven project builds; `./mvnw test` is **green**; the app starts against `taskflow_lab`.
- [ ] `Role`/`Status`/`Priority` enums and `User`/`Project`/`Task` entities exist with the documented
      fields, JPA mappings (unique `username`+`email`, LAZY `@ManyToOne`s, EAGER roles
      `@ElementCollection`, string enums, `@Builder.Default` defaults).
- [ ] `UserRepository` (`findByUsername`/`existsByUsername`/`existsByEmail`), `ProjectRepository` and
      `TaskRepository` (with the documented `@EntityGraph` finders) exist.
- [ ] All nine DTOs (DTO-only API, no password in `UserResponse`), both exceptions, and the five-handler
      `GlobalExceptionHandler` (RFC 7807 `ProblemDetail`) exist.
- [ ] `AuthServiceImpl.register` BCrypt-hashes the password, assigns `USER`, and 409s on a duplicate
      username **or** email; the three domain services do manual DTO mapping with the documented
      responsibilities; `createProject` sets owner = principal.
- [ ] `UserDetailsServiceImpl` maps roles to `ROLE_<name>` authorities and throws
      `UsernameNotFoundException`; `SecurityConfig` (HTTP Basic) declares `PasswordEncoder`,
      `SecurityFilterChain`, and `AuthenticationManager` with the Section 8.2 matrix; `AdminUserSeeder`
      idempotently seeds `admin`.
- [ ] All five controllers expose the Section 8.1 endpoints with the documented status codes.
- [ ] Tests pass at all four layers with the `@Import(SecurityConfig)` + `@MockitoBean
      UserDetailsService` + `springSecurity()` + `@WithMockUser` recipe.

---

# PART 2 — Add Stateless JWT

Goal of Part 2: replace HTTP Basic with **stateless JWT**. Login issues a signed **access token** (15
min) + a persisted **rotating refresh token** (7 days); a `OncePerRequestFilter` authenticates the
`Authorization: Bearer` header; the `SecurityFilterChain` becomes **`STATELESS`** with no session, no
form, no Basic. This is the genuinely new material — each new concept is explained with *why*.

## 14. The new concepts (read before you code)

### 14.1 Why stateless, and what "no session" buys you

In Part 1 the server either re-checked `username:password` every request (HTTP Basic) or kept an
`HttpSession` and a `JSESSIONID` cookie ("remembering" you). Both couple the server to per-user state.

**Stateless** means `SessionCreationPolicy.STATELESS`: Spring Security creates **no `HttpSession`** and
does not store the `SecurityContext` between requests. Every request must **carry its own proof of
identity** — the JWT — and the JWT filter rebuilds the `Authentication` from that token alone. Benefits:
horizontal scaling with no shared session store, no session-fixation surface, and a clean REST model
(each request is self-describing). Cost: you cannot "log someone out" by invalidating a session — which
is exactly why the access token is short-lived (below).

### 14.2 Why a short access token **and** a rotating refresh token

The access token is **stateless and unrevocable**: once signed, it is valid until it expires, no matter
what happens server-side. If it leaks, an attacker can use it until `exp`. So you keep it **short** (15
minutes) to bound the damage.

But forcing a full username/password login every 15 minutes is terrible UX. The fix is a **refresh
token**: a **long-lived** (7-day) credential the client uses to obtain a new access token **without
re-entering credentials**. Unlike the access token, the refresh token is a **database row you control** —
you can check it, expire it, and **revoke** it. And you **rotate** it: each successful `/refresh`
**revokes the presented token and issues a brand-new one**, so a refresh token is **single-use**. If an
attacker steals a refresh token but the legitimate client has already used (and thus revoked) it, the
stolen copy is dead — and a reused revoked token is a detectable signal.

The refresh token is an **opaque random UUID, not a JWT**: it carries no claims and means nothing except
"look me up in the `refresh_tokens` table". Keeping it opaque means its only authority is the row's
existence and state.

### 14.3 The Bearer filter and where it slots in

On each request the client sends `Authorization: Bearer <access-jwt>`. A **`OncePerRequestFilter`** reads
that header, verifies the signature + expiry, and — if valid — builds a
`UsernamePasswordAuthenticationToken` (principal = username, authorities = the roles claim mapped back to
`ROLE_<name>`) and places it in the `SecurityContext`. It is registered **before** the framework's
`UsernamePasswordAuthenticationFilter` slot with `addFilterBefore(...)`, so the token is validated ahead
of any form-login processing. The filter is **permissive on failure**: a missing/invalid/expired token
simply leaves the context unauthenticated and lets the chain continue — the authorization rules then
produce a 401 via the entry point. It never throws and never writes a response itself.

### 14.4 Why CSRF stays disabled (now unambiguously)

In Part 1, disabling CSRF was a *reasoned* choice for an HTTP-Basic API. In Part 2 it is **unambiguously
correct**: a Bearer-token API authenticates from an `Authorization` header the client sets **explicitly
in code**. A browser never attaches that header automatically (unlike a session cookie), so the CSRF
attack — a malicious page forging a state-changing request on your ambient cookie — **cannot happen**.
There is nothing for CSRF to protect. Leave `.csrf(csrf -> csrf.disable())` with the updated comment.

### 14.5 What a signed JWT is (and is not)

`header.payload.signature`, each base64url. The signature is `HMAC-SHA256(header + "." + payload,
secret)` (HS256). This makes the token **tamper-evident** — any change to header or payload breaks the
signature and verification fails. It does **not** encrypt: anyone can base64-decode and read the payload.
**Therefore only non-sensitive claims go in** — the subject (username), a `roles` claim, and `iat`/`exp`.
The HS256 secret must be **at least 256 bits (32 bytes)** and lives in config (never a claim, never
hard-coded in real deployments).

## 15. Part 2 — Setup delta

### 15.1 Add jjwt to the `pom.xml` (exact version: **0.13.0**)

jjwt is split into three artifacts since 0.11 (current line 0.13.x). Add a `<jjwt.version>0.13.0</jjwt.version>`
property and the three dependencies:

```xml
<properties>
    <!-- ...existing... -->
    <jjwt.version>0.13.0</jjwt.version>
</properties>

<!-- jjwt: the compile-time API + two runtime artifacts. -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>            <!-- Jwts, JwtBuilder/JwtParser: what you code against -->
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>           <!-- runtime implementation (service-loaded) -->
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>        <!-- runtime JSON (de)serializer so the roles list marshals -->
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
```

`jjwt-api` is on the compile classpath (you code against `Jwts`/`Claims`/`Keys`); `jjwt-impl` and
`jjwt-jackson` are `runtime` so you cannot accidentally depend on jjwt internals, and so claims (like the
`roles` list) marshal through Jackson.

> **Why jjwt and not `spring-boot-starter-oauth2-resource-server`?** jjwt is lightweight and gives full
> control to **self-issue** HS256 tokens — exactly what a from-scratch teaching lab wants. The
> resource-server path (Nimbus `JwtEncoder`/`JwtDecoder`) exists and is fine for *validating* third-party
> tokens, but in Spring Security 7 its `NimbusJwtDecoder` applies a `typ`-header validator by default,
> so a self-issued HS256 token with an absent/unexpected `typ` may be rejected unless you adjust the
> `JwtTypeValidator`. Out of scope here.

### 15.2 Add the JWT config to `application.properties`

Append the `app.security.jwt.*` block:

```properties
# --- Lab 7 Part 2: stateless JWT configuration (bound to JwtProperties) ---
# HS256 signing secret. MUST be a Base64-encoded key of at least 256 bits (32 bytes) — this 48-byte
# (384-bit) value is a DEV DEFAULT only; generate a fresh secret per environment and inject it via an
# environment variable (e.g. APP_SECURITY_JWT_SECRET) in real deployments. The JWT payload is signed,
# not encrypted: never put secrets in claims.
app.security.jwt.secret=pp0gXRqpg69kqphvhFq5XK2JB4lxe8noQUoDG4RAslaDXvP/3idY/I+PV5M1EmNB
# Access token: short-lived (no server-side revocation of a stateless JWT, so keep the blast radius
# small). ISO-8601 duration; PT15M = 15 minutes.
app.security.jwt.access-token-ttl=PT15M
# Refresh token: long-lived and persisted (rotated on use). P7D = 7 days.
app.security.jwt.refresh-token-ttl=P7D
```

## 16. Part 2 — Complete Implementation Inventory (the delta, class-by-class)

Part 2 **adds twelve files** and **changes five**. Nothing from Part 1 is deleted. Each entry below
names the subsection that specifies it:

```
src/main/java/com/example/_7_jwt/
├── config/
│   └── JwtProperties.java                NEW      16.1   secret + the two TTLs, bound from config
├── model/
│   └── RefreshToken.java                 NEW      16.4   persisted, revocable, rotating token
├── repository/
│   └── RefreshTokenRepository.java       NEW      16.4   findByToken
├── dto/
│   ├── LoginRequest.java                 NEW      16.3   username + password
│   ├── TokenResponse.java                NEW      16.3   access + refresh + type + expiresIn
│   └── RefreshRequest.java               NEW      16.3   refreshToken
├── exception/
│   ├── InvalidTokenException.java        NEW      16.9   unknown / revoked / expired refresh token
│   └── GlobalExceptionHandler.java       CHANGED  16.9   5 handlers  ->  7 handlers
├── service/
│   ├── RefreshTokenService.java          NEW      16.7   create / verify / rotate
│   ├── RefreshTokenServiceImpl.java      NEW      16.7
│   ├── AuthService.java                  CHANGED  16.8   1 method   ->  3 methods
│   └── AuthServiceImpl.java              CHANGED  16.8   2-arg ctor ->  5-arg ctor
├── security/
│   ├── JwtService.java                   NEW      16.2   issue + verify HS256 tokens
│   ├── JwtAuthenticationFilter.java      NEW      16.5   OncePerRequestFilter, reads Bearer
│   ├── JwtAuthenticationEntryPoint.java  NEW      16.6   401 ProblemDetail
│   └── SecurityConfig.java               CHANGED  16.11  HTTP Basic -> STATELESS JWT
└── controller/
    └── AuthController.java               CHANGED  16.10  1 handler  ->  3 handlers
```

New test classes (Section 19): `JwtServiceTest`, `RefreshTokenRepositoryTest`,
`RefreshTokenServiceImplTest`, `AuthControllerLoginTest`, `JwtAuthenticationFilterTest` — plus the
**changed** `AuthServiceImplTest` (the 5-arg constructor, Section 19.1).

> **Read** the subsections in order (16.1 → 16.11); they are grouped by concern. **Build** in the order
> of **Section 20**, which is sequenced so you can green a test after each step. The two orders differ
> deliberately — do not treat 16.x as a build order.

### 16.1 `config/JwtProperties` — externalised JWT config (NEW)

`@ConfigurationProperties(prefix = "app.security.jwt")` with `@Getter @Setter`. Three fields bound from
the properties (durations bind from ISO-8601 text). Registered via
`@EnableConfigurationProperties(JwtProperties.class)` on `SecurityConfig` (16.11), so it is available in
the app **and** in `@WebMvcTest` slices that import that config.

| Field | Type | Default |
| ----- | ---- | ------- |
| `secret` | `String` | the DEV-DEFAULT Base64 key (48 bytes / 384 bits) |
| `accessTokenTtl` | `Duration` | `Duration.ofMinutes(15)` |
| `refreshTokenTtl` | `Duration` | `Duration.ofDays(7)` |

Set the defaults as field initialisers (so `new JwtProperties()` in unit tests yields a valid key and the
default TTLs).

### 16.2 `security/JwtService` — issues and verifies HS256 tokens (NEW)

A **plain class** (not a `@Service`) constructed from `JwtProperties`, so it is trivially unit-testable
(`new JwtService(props)`) and can be declared as a `@Bean` in `SecurityConfig` (available inside
`@WebMvcTest` slices). Uses jjwt 0.13.x.

Fields: `private final SecretKey key;` and `private final Duration accessTokenTtl;`. Constructor:

```java
public JwtService(JwtProperties properties) {
    // Keys.hmacShaKeyFor validates the key is long enough for HMAC-SHA (>= 256 bits for HS256).
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    this.accessTokenTtl = properties.getAccessTokenTtl();
}
```

Methods (constant `ROLES_CLAIM = "roles"`):

| Method | Behaviour |
| ------ | --------- |
| `String generateAccessToken(String username, Collection<String> roles)` | `Jwts.builder().subject(username).claim("roles", List.copyOf(roles)).issuedAt(Date.from(now)).expiration(Date.from(now.plus(accessTokenTtl))).signWith(key, Jwts.SIG.HS256).compact()` |
| `boolean isValid(String token)` | parse the token; return `true` if it verifies and is unexpired, `false` on any `JwtException`/`IllegalArgumentException`. **Never throws.** |
| `String extractUsername(String token)` | `parse(token).getSubject()` |
| `List<String> extractRoles(String token)` | read the `roles` claim; if a `Collection`, map each element with `String::valueOf` to a `List<String>`; else `List.of()` |
| `Instant extractExpiration(String token)` | `parse(token).getExpiration().toInstant()` |
| `long getAccessTokenTtlSeconds()` | `accessTokenTtl.getSeconds()` — surfaced to clients as `expiresIn` |

Private `Claims parse(String token)`:

```java
return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
```

Imports: `io.jsonwebtoken.{Claims, JwtException, Jwts}`, `io.jsonwebtoken.io.Decoders`,
`io.jsonwebtoken.security.Keys`, `javax.crypto.SecretKey`.

> **Roles claim carries BARE names** (`"USER"`, `"ADMIN"`), not `ROLE_`-prefixed authorities. The filter
> re-adds the prefix when it rebuilds the `Authentication` (16.5). Keep the two representations straight.

### 16.3 `dto` — three NEW payloads

- **`LoginRequest`** (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor`): `username`
  (`@NotBlank(message = "Username is required")`), `password` (`@NotBlank(message = "Password is
  required")`). Credentials are handed to the `AuthenticationManager`; a blank field is rejected as `400`
  first.
- **`TokenResponse`** (same Lombok): `String accessToken`, `String refreshToken`, `String tokenType`
  (always `"Bearer"`), `long expiresIn` (access-token lifetime in seconds). Returned by `/login` and
  `/refresh`.
- **`RefreshRequest`** (same Lombok): `refreshToken` (`@NotBlank(message = "Refresh token is
  required")`).

### 16.4 `model/RefreshToken` + `repository/RefreshTokenRepository` (NEW)

#### `RefreshToken` — a persisted, revocable, rotating token (`@Entity`, table `refresh_tokens`)

Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @EqualsAndHashCode(onlyExplicitlyIncluded = true)`.

| Field | Type | Mapping |
| ----- | ---- | ------- |
| `id` | `Long` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `token` | `String` | `@EqualsAndHashCode.Include` + `@Column(nullable = false, unique = true)` — the opaque UUID (its natural key) |
| `user` | `User` | `@ManyToOne(fetch = FetchType.EAGER, optional = false)` + `@JoinColumn(name = "user_id", nullable = false)` |
| `expiresAt` | `Instant` | `@Column(nullable = false)` |
| `revoked` | `boolean` | `@Builder.Default` + `@Column(nullable = false)`, `= false` |

`user` is EAGER so `rotate` can read `getUser()` after loading the row outside a session.

#### `RefreshTokenRepository extends JpaRepository<RefreshToken, Long>`

| Method | Used by |
| ------ | ------- |
| `Optional<RefreshToken> findByToken(String token);` | `RefreshTokenServiceImpl.verify/rotate` |

### 16.5 `security/JwtAuthenticationFilter` — the Bearer authenticator (NEW)

`@RequiredArgsConstructor` extending `org.springframework.web.filter.OncePerRequestFilter`, injecting
`JwtService`. Constants `HEADER = "Authorization"`, `BEARER_PREFIX = "Bearer "`.

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

    String header = request.getHeader(HEADER);
    if (header != null && header.startsWith(BEARER_PREFIX)
            && SecurityContextHolder.getContext().getAuthentication() == null) {

        String token = header.substring(BEARER_PREFIX.length());
        if (jwtService.isValid(token)) {
            String username = jwtService.extractUsername(token);
            List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(token).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))   // re-add the prefix
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
    chain.doFilter(request, response);
}
```

Key behaviours: it only acts on a `Bearer ` header **and only when the context is not already
authenticated** (so it **yields to an existing context**, e.g. a test's `@WithMockUser` — making it
mechanism-agnostic); on an invalid token it does **nothing** and continues (the entry point produces the
401); it re-adds the `ROLE_` prefix that `JwtService` strips into the claim.

### 16.6 `security/JwtAuthenticationEntryPoint` — the 401 gateway (NEW)

Implements `org.springframework.security.web.AuthenticationEntryPoint`. Because Part 2 removes
`formLogin`/`httpBasic`, Spring Security's fallback entry point would return **403** for anonymous access
to a protected endpoint; this restores the correct **401** and writes a small RFC 7807 body **by hand**
(no `ObjectMapper` dependency, so it is robust in any slice):

```java
@Override
public void commence(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException authException) throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getWriter().write(
            "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
                    + "\"detail\":\"Authentication required: a valid Bearer access token is missing or invalid.\"}");
}
```

### 16.7 `service/RefreshTokenService` + `RefreshTokenServiceImpl` (NEW)

Interface:

```java
RefreshToken create(User user);   // mint + persist a fresh 7-day token
RefreshToken verify(String token);// throws InvalidTokenException if unknown / revoked / expired
RefreshToken rotate(String token);// verify, revoke the old row, issue a new one (single-use)
```

`RefreshTokenServiceImpl` — `@Service @RequiredArgsConstructor`, injecting `RefreshTokenRepository` and
`JwtProperties`:

| Method | Behaviour |
| ------ | --------- |
| `create(User)` | `@Transactional`. Build `RefreshToken.builder().token(UUID.randomUUID().toString()).user(user).expiresAt(Instant.now().plus(jwtProperties.getRefreshTokenTtl())).revoked(false).build()`, `save`, return. |
| `verify(String)` | `@Transactional(readOnly = true)`. `findByToken(...).orElseThrow(() -> new InvalidTokenException("Refresh token is not recognized"))`; if `isRevoked()` → `InvalidTokenException("Refresh token has been revoked")`; if `expiresAt.isBefore(Instant.now())` → `InvalidTokenException("Refresh token has expired")`; else return the row. |
| `rotate(String)` | `@Transactional`. `RefreshToken current = verify(token); current.setRevoked(true); save(current); return create(current.getUser());` — **the presented token is revoked first, then a replacement is minted for the same user**. |

### 16.8 `service/AuthService` + `AuthServiceImpl` — CHANGED (login + refresh added)

Add **two** methods to the interface. The **finished `AuthService` has three methods** — `register`
from Part 1 (Section 7.5) plus `login` and `refresh`. Here it is in full, so there is no ambiguity
about the end state:

```java
public interface AuthService {

    /** Part 1 (Section 7.5) — UNCHANGED. Duplicate checks, BCrypt hash, default USER role. */
    UserResponse register(RegisterRequest request);

    /**
     * NEW. Authenticate the credentials via the AuthenticationManager and, on success, issue a
     * signed access token plus a persisted refresh token. Bad credentials propagate as a Spring
     * Security AuthenticationException (mapped to 401).
     */
    TokenResponse login(LoginRequest request);

    /**
     * NEW. Validate and rotate the presented refresh token, minting a fresh access token. An
     * unknown, revoked, or expired refresh token raises InvalidTokenException (mapped to 401).
     */
    TokenResponse refresh(RefreshRequest request);
}
```

Change `AuthServiceImpl`'s constructor to a **superset** of the Part 1 one — the three new collaborators
come **last** so existing positional construction still works. The five fields, **in this declaration
order** (`@RequiredArgsConstructor` generates the constructor from it, and `AuthServiceImplTest`
constructs the class positionally):

```java
private final UserRepository userRepository;                // Part 1
private final PasswordEncoder passwordEncoder;              // Part 1
private final AuthenticationManager authenticationManager;  // NEW
private final JwtService jwtService;                        // NEW
private final RefreshTokenService refreshTokenService;      // NEW
```

Add constants `BEARER_TYPE = "Bearer"`, `ROLE_PREFIX = "ROLE_"`. `register` is unchanged from Part 1.

`login` (annotated `@Transactional`):

```java
Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

String username = authentication.getName();
List<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(AuthServiceImpl::stripRolePrefix)   // "ROLE_USER" -> "USER" for the claim
        .toList();

String accessToken = jwtService.generateAccessToken(username, roles);

User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", username));
RefreshToken refreshToken = refreshTokenService.create(user);

return new TokenResponse(accessToken, refreshToken.getToken(), BEARER_TYPE,
        jwtService.getAccessTokenTtlSeconds());
```

- `authenticationManager.authenticate(...)` delegates to the **same** `DaoAuthenticationProvider` →
  `UserDetailsService` + BCrypt path Part 1 already wired: it loads the user, matches the hash, and
  throws `BadCredentialsException` (an `AuthenticationException`) on failure — which the
  `GlobalExceptionHandler` maps to **401** (16.9). **This is why Part 1 exposed the
  `AuthenticationManager` bean.**
- `stripRolePrefix(String authority)` is a `private static` helper: `authority.startsWith("ROLE_") ?
  authority.substring(5) : authority`.

`refresh` (annotated `@Transactional`):

```java
RefreshToken rotated = refreshTokenService.rotate(request.getRefreshToken());
User user = rotated.getUser();
List<String> roles = user.getRoles().stream().map(Role::name).toList();
String accessToken = jwtService.generateAccessToken(user.getUsername(), roles);
return new TokenResponse(accessToken, rotated.getToken(), BEARER_TYPE,
        jwtService.getAccessTokenTtlSeconds());
```

`refresh` requires **no credentials** — it authenticates by possession of a valid refresh token, rotates
it, and returns the **new** refresh token alongside the fresh access token. An unknown/revoked/expired
refresh token surfaces `InvalidTokenException` → **401**.

### 16.9 `exception/InvalidTokenException` + `GlobalExceptionHandler` — CHANGED

Add **`InvalidTokenException extends RuntimeException`** with a `(String message)` constructor — thrown
when a presented refresh token is unknown, revoked, or expired.

Add **two** handlers to `GlobalExceptionHandler` (both **401**). Note why they live here: auth failures
for *protected* endpoints (anonymous/invalid Bearer) are produced by the **filter chain** before a
controller runs (the entry point, 16.6) — but `/api/auth/login` and `/api/auth/refresh` authenticate
**inside** the controller, so *their* failures surface as ordinary exceptions:

| `@ExceptionHandler` | Status | Title | Detail |
| ------------------- | ------ | ----- | ------ |
| `org.springframework.security.core.AuthenticationException` | `401` | `"Authentication failed"` | `"Invalid username or password"` — bad credentials at `/login` |
| `InvalidTokenException` | `401` | `"Invalid token"` | `ex.getMessage()` — unknown/revoked/expired refresh token at `/refresh` |

The **finished handler has seven methods** — the five from Section 7.4 plus these two. Check your
class against this complete list:

| # | `@ExceptionHandler` | Status | Title | Added in |
| - | ------------------- | ------ | ----- | -------- |
| 1 | `AuthenticationException` | `401` | `"Authentication failed"` | **Part 2** |
| 2 | `InvalidTokenException` | `401` | `"Invalid token"` | **Part 2** |
| 3 | `ResourceNotFoundException` | `404` | `"Resource not found"` | Part 1 |
| 4 | `DuplicateResourceException` | `409` | `"Duplicate value"` | Part 1 |
| 5 | `MethodArgumentNotValidException` | `400` | `"Validation failed"` (+ `errors` map) | Part 1 |
| 6 | `MethodArgumentTypeMismatchException` | `400` | `"Invalid parameter"` | Part 1 |
| 7 | `Exception` | `500` | `"Internal server error"` | Part 1 |

Declaration order does not matter — Spring dispatches to the **most specific** matching handler, so
the catch-all `Exception` method never shadows the six above it.

### 16.10 `controller/AuthController` — CHANGED (login + refresh added)

Add two handlers alongside `register`. The **finished controller has three handlers**:

| Handler | Mapping | Returns | Added in |
| ------- | ------- | ------- | -------- |
| `register(@Valid @RequestBody RegisterRequest request)` | `@PostMapping("/register")` | `201 CREATED`, `UserResponse` | Part 1 |
| `login(@Valid @RequestBody LoginRequest request)` | `@PostMapping("/login")` | `200`, `authService.login(request)` (`TokenResponse`) | **Part 2** |
| `refresh(@Valid @RequestBody RefreshRequest request)` | `@PostMapping("/refresh")` | `200`, `authService.refresh(request)` (`TokenResponse`) | **Part 2** |

All three live under `/api/auth/**`, which becomes `permitAll` (a client cannot present a token before
it has one). Note `register` alone returns **201**; `login` and `refresh` return **200** — they create
no resource.

### 16.11 `security/SecurityConfig` — CHANGED (reconfigure for stateless JWT)

Rewrite the class. Add `@EnableConfigurationProperties(JwtProperties.class)`. Declare the JWT collaborators
as **`@Bean`s** (not component-scanned) so `@Import(SecurityConfig.class)` into a `@WebMvcTest` fully
wires the chain. Keep `PasswordEncoder` and `AuthenticationManager`.

```java
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean JwtService jwtService(JwtProperties p) { return new JwtService(p); }

    @Bean JwtAuthenticationFilter jwtAuthenticationFilter(JwtService s) {
        return new JwtAuthenticationFilter(s);
    }

    @Bean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) throws Exception {
        http
            // A Bearer-token API has no ambient cookie to forge, so CSRF is safely disabled.
            .csrf(csrf -> csrf.disable())
            // No HttpSession: the SecurityContext is rebuilt per request from the JWT.
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Public: register + login + refresh (a client has no token yet)
                    .requestMatchers("/api/auth/**").permitAll()
                    // ADMIN-only administration area
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // Everything else (projects, tasks, /api/users/me) needs a valid token
                    .anyRequest().authenticated())
            // With no form/basic mechanism, restore 401 (not the default 403) on anonymous/invalid access
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            // Validate the Bearer token before the username/password filter slot
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
```

**The delta from Part 1** (call each out in comments):

1. **`STATELESS`** session policy — no `HttpSession`, no `JSESSIONID`.
2. **`formLogin` and `httpBasic` removed** — pure token API, no login page, no Basic challenge.
3. **`/api/auth/**` is `permitAll`** (was only `POST /api/auth/register`) — login + refresh must be
   reachable without a token.
4. **A 401 entry point** is wired (removing form/basic removed their entry point; the default would 403).
5. **`addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`** — the JWT
   filter runs ahead of the username/password slot.

The `/api/admin/**` rule and the `AuthenticationManager` bean are unchanged. New imports:
`...config.http.SessionCreationPolicy`, `...web.authentication.UsernamePasswordAuthenticationFilter`,
`org.springframework.boot.context.properties.EnableConfigurationProperties`, and your `JwtProperties`.

## 17. Part 2 — API Contract & updated Authorization Matrix

### 17.1 New endpoints

| Method | Path | Auth | Success | Errors |
| ------ | ---- | ---- | ------- | ------ |
| POST | `/api/auth/login` | public (`permitAll`) | `200` `TokenResponse` | `400` blank field; `401` bad credentials |
| POST | `/api/auth/refresh` | public (`permitAll`) | `200` `TokenResponse` | `400` blank; `401` unknown/revoked/expired token |

`POST /api/auth/login` request / `200` response:

```json
// request
{ "username": "alice", "password": "password123" }
// 200 response
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsInJvbGVzIjpbIlVTRVIiXX0...",
  "refreshToken": "a4f1c2e0-...-9b7d", "tokenType": "Bearer", "expiresIn": 900 }
```

`POST /api/auth/refresh` request / `200` response (the **rotated** refresh token comes back):

```json
// request
{ "refreshToken": "a4f1c2e0-...-9b7d" }
// 200 response
{ "accessToken": "eyJ...new...", "refreshToken": "b8e2...-rotated", "tokenType": "Bearer", "expiresIn": 900 }
```

`401` bodies are `ProblemDetail`:

```json
{ "title": "Authentication failed", "status": 401, "detail": "Invalid username or password" }
{ "title": "Invalid token", "status": 401, "detail": "Refresh token has expired" }
```

### 17.2 Updated authorization matrix

Every **domain** endpoint is now reached by presenting a valid **access token** (`Authorization: Bearer
<jwt>`) instead of HTTP Basic; the role/status semantics are identical to Section 8.2. The auth routes
are public:

| Endpoint | No/invalid token | USER token | ADMIN token |
| -------- | :--------------: | :--------: | :---------: |
| `POST /api/auth/register`, `/login`, `/refresh` | ✅ (permitAll) | ✅ | ✅ |
| `GET /api/users/me`, all `/api/projects/**`, all `/api/tasks/**` | ❌ 401 | ✅ 2xx | ✅ 2xx |
| `GET /api/admin/users` | ❌ 401 | ⛔ 403 | ✅ 200 |

## 18. Part 2 — Business Rules

- **Stateless**: `SessionCreationPolicy.STATELESS`; no `HttpSession`/`JSESSIONID`; the `SecurityContext`
  is rebuilt per request from the Bearer token.
- **Access token**: signed HS256, subject = username, a bare-name `roles` claim, `iat`/`exp`; **15-minute
  TTL**; not revocable server-side (hence short).
- **Refresh token**: an **opaque UUID** persisted with an absolute `expiresAt` and a `revoked` flag;
  **7-day TTL**; **single-use** — each `/refresh` **rotates** it (revoke old, issue new).
- **`/login`** authenticates through the `AuthenticationManager` (reusing the Part 1 `UserDetailsService`
  + BCrypt path); bad credentials → **401**.
- **`/refresh`** needs no credentials; an unknown/revoked/expired token → **401**.
- The **JWT filter** authenticates the `Authorization: Bearer` header, never throws, and yields to an
  already-authenticated context; anonymous/invalid access to a protected endpoint → **401** via the entry
  point.
- **CSRF stays disabled** — a Bearer-token API has no ambient cookie to forge.
- The signing **secret and TTLs live in config** (`JwtProperties`), never hard-coded in real deployments;
  the shipped secret is a documented DEV DEFAULT.
- The `roles` claim stores **bare names**; the filter re-adds the `ROLE_` prefix.
- **Ownership enforcement is still deferred to Lab 8** — JWT changes *how you authenticate*, not *what a
  principal may touch*.

## 19. Part 2 — Testing (new + changed)

The Part 1 controller slice tests **keep passing unchanged**: they use `@WithMockUser` (which the JWT
filter yields to) and import `SecurityConfig` (which now also wires the JWT beans — `@WebMvcTest` binds
`JwtProperties` from its defaults, so no test config is needed). The new/changed tests:

| Class | Layer | What to verify |
| ----- | ----- | -------------- |
| **`JwtServiceTest`** | plain unit | Round-trip: issue then parse recovers subject + roles claim and `isValid` is true; `getAccessTokenTtlSeconds()==900`; a **negative-TTL** token is invalid (expired); a **tampered** token is invalid — flip the **first** character of the signature segment, not the last: a 32-byte HS256 signature base64url-encodes to 43 unpadded chars, so the final char's low 2 bits are ignored padding and an `'A'`↔`'B'` swap can decode to the identical signature, leaving the token valid (a ~1-in-16 flake); a token signed with a **different secret** is invalid. Build `JwtProperties` in-test and vary only the TTL/secret. |
| **`RefreshTokenRepositoryTest`** | `@DataJpaTest` | `findByToken` returns the row (with its EAGER user) when present, empty when unknown; a persisted token can be marked `revoked` and read back. Seed via `TestEntityManager`. |
| **`RefreshTokenServiceImplTest`** | Mockito | `create` persists a non-revoked UUID token with a ~7-day future expiry; `verify` returns a valid row, and throws `InvalidTokenException` for unknown / revoked / expired (message contains "not recognized"/"revoked"/"expired"); `rotate` revokes the presented token and returns a **different** token for the same user (`save` called twice), and rejects an invalid token without saving. Use a real `new JwtProperties()`. |
| **`AuthServiceImplTest`** | Mockito | **CHANGED** — build the service with the **5-arg** constructor (`new AuthServiceImpl(userRepository, passwordEncoder, authenticationManager, jwtService, refreshTokenService)`), keep the Part 1 register cases, and add: `login` authenticates via the `AuthenticationManager` then issues access + refresh (`Bearer`, `expiresIn==900`); `login` puts **bare** role names in the claim (capture the roles list → `containsExactly("USER")`); `login` propagates `BadCredentialsException` and issues no tokens (`verifyNoInteractions(jwtService, refreshTokenService)`); `refresh` returns a new access token **and the rotated refresh token**; `refresh` propagates `InvalidTokenException`. Mock `AuthenticationManager`/`JwtService`/`RefreshTokenService`; use a **real** `BCryptPasswordEncoder`. |
| **`AuthControllerLoginTest`** | `@WebMvcTest(AuthController.class)` | (mock `AuthService` + `UserDetailsService`, `@Import(SecurityConfig)`, `springSecurity()`) `POST /login` `200` returns `accessToken/refreshToken/tokenType=Bearer/expiresIn`; `401` on `BadCredentialsException` (`$.title=="Authentication failed"`); `400` blank username; `POST /refresh` `200` returns new + rotated token; `401` on `InvalidTokenException` (`$.title=="Invalid token"`); `400` blank refresh token. Runs **anonymously** (permitAll). |
| **`JwtAuthenticationFilterTest`** | `@WebMvcTest(ProjectController.class)` + `@Import(SecurityConfig)` | End-to-end proof against a real protected endpoint with the **real** `JwtService`/filter/entry point (autowire the real `JwtService` + `JwtProperties` beans; mock only the service layer + `UserDetailsService`): a **valid** Bearer token reaches the protected `GET` (200) and a protected `POST` (201, principal = the token subject "alice"); a **missing** token → 401; a **tampered** token → 401; an **expired** token (build one with a negative-TTL `JwtService` on the same secret) → 401. |

### 19.1 The constructor-change gotcha

`AuthServiceImpl`'s constructor grows from 2 args to 5 in Part 2. The **new collaborators are appended**
(`authenticationManager, jwtService, refreshTokenService` last), so the change is additive — but you
**must update `AuthServiceImplTest`'s `@BeforeEach`** to the 5-arg form, or it will not compile. Mock the
three new collaborators; keep the real `BCryptPasswordEncoder`.

## 20. Part 2 — Suggested Build Order

1. Add the jjwt dependencies (15.1) and the `app.security.jwt.*` properties (15.2).
2. `config/JwtProperties`; `security/JwtService`. Write **`JwtServiceTest`** and green it (pure unit, no
   Spring).
3. `dto` — `LoginRequest`, `TokenResponse`, `RefreshRequest`. `exception/InvalidTokenException`.
4. `model/RefreshToken` + `repository/RefreshTokenRepository`. Write **`RefreshTokenRepositoryTest`**.
5. `service/RefreshTokenService(+Impl)`. Write **`RefreshTokenServiceImplTest`**.
6. `security/JwtAuthenticationFilter` + `security/JwtAuthenticationEntryPoint`.
7. **Reconfigure `SecurityConfig`** (16.11) — STATELESS, permitAll `/api/auth/**`, JWT beans, entry
   point, `addFilterBefore`, drop form/basic.
8. Extend `AuthService`/`AuthServiceImpl` (login + refresh, 5-arg constructor) and `AuthController`
   (`/login`, `/refresh`). Add the two `GlobalExceptionHandler` handlers (16.9).
9. **Update `AuthServiceImplTest`** to the 5-arg constructor + add the login/refresh cases; write
   **`AuthControllerLoginTest`** and **`JwtAuthenticationFilterTest`**.
10. Run `./mvnw test` — **all green** (Part 1 tests unchanged + the new JWT tests). Exercise the token
    flow manually (Section 21).

## 21. Part 2 — Manual Testing Flow (Bearer tokens)

Exercise the token flow two ways: quickly with `curl` (21.1), then by **building your own Postman
collection** (21.2) that logs in, captures the token automatically, and runs the whole authorization flow
as a repeatable suite.

### 21.1 With `curl`

With the app running (seeded `admin` / `admin12345`):

1. **Log in:** `curl -i -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" -d
   '{"username":"admin","password":"admin12345"}'` → **`200`** with `accessToken`, `refreshToken`,
   `"tokenType":"Bearer"`, `"expiresIn":900`. Copy the `accessToken` into `$TOKEN`.
2. **Call a protected endpoint with the token:** `curl -i -H "Authorization: Bearer $TOKEN"
   localhost:8080/api/projects` → **`200`**.
3. **No token → 401:** `curl -i localhost:8080/api/projects` → **`401`** (the JWT entry point's
   ProblemDetail body).
4. **Tampered token → 401:** change one character of `$TOKEN` → **`401`**.
5. **ADMIN-only with an ADMIN token → 200; with a USER token → 403.** Register `alice`, log in as her,
   and hit `/api/admin/users` → **`403`**; the admin's token → **`200`**.
6. **Refresh:** `curl -i -X POST localhost:8080/api/auth/refresh -H "Content-Type: application/json" -d
   '{"refreshToken":"<the refresh token from step 1>"}'` → **`200`** with a **new** access token and a
   **new (rotated)** refresh token.
7. **Replay the old refresh token → 401:** repeat step 6 with the **first** refresh token → **`401`**
   `"Invalid token"` (it was revoked on rotation — proving single-use rotation).

### 21.2 Build a Postman collection (log in, capture the token, and test)

> **A ready-to-import reference collection ships with this module:
> `TaskFlow_JWT.postman_collection.json`** (in Postman: *Import* → drop the file in). Use it to check
> your work — but **building the collection yourself is the exercise**.

`curl` proves the flow once; a **Postman collection** turns it into a repeatable suite — and it shows the
real-world pattern every SPA / mobile client uses: **log in once, capture the token, and send it as a
`Bearer` header on every later request.** Build this collection yourself, one request at a time, following
the spec blocks below top-to-bottom.

**Set up the collection**

1. Create a collection named **`TaskFlow JWT`**.
2. Add collection **variables** (Collection → **Variables** tab): `baseUrl` = `http://localhost:8080`,
   plus **three empty** ones — `accessToken`, `refreshToken`, and `firstRefreshToken`. Post-response
   scripts fill all three, so you never paste a token by hand. (`firstRefreshToken` exists only so
   Request 11 can replay a token that Request 10 has already rotated away — see below.)
3. Protected requests set their **Authorization** tab to **type = Bearer Token**, **Token** =
   `{{accessToken}}`; the current access token rides along automatically.

**The token-capture idea (the star of this lab).** The login and refresh requests carry a
**Scripts → Post-response** script that reads the JSON body and writes the two tokens into the collection
variables with `pm.collectionVariables.set(...)`. That is the Postman equivalent of a browser storing the
token after login and attaching it to every later call. Each spec block below gives you the exact script.

---

**Request 1 — Register alice**  → expected `201`
- **Method & URL:** `POST {{baseUrl}}/api/auth/register`
- **Authorization tab:** `No Auth` (registration is under `/api/auth/**`, which is `permitAll`).
- **Body:** *Body → raw → JSON* (this auto-sets `Content-Type: application/json`):
  ```json
  { "username": "alice", "email": "alice@taskflow.local", "password": "password123" }
  ```
- **Expected response:** `201 Created`, a `UserResponse` (no password field; a self-registered user always
  gets the single `USER` role):
  ```json
  { "id": 2, "username": "alice", "email": "alice@taskflow.local", "roles": ["USER"] }
  ```
  (If you have run this before, alice already exists and you get `409` with a `"Duplicate value"`
  ProblemDetail — drop the DB or pick a fresh username to reset.)
- **Scripts → Post-response (tests):**
  ```javascript
  pm.test("201 Created", () => pm.response.to.have.status(201));
  pm.test("alice is a plain USER", () => {
      const body = pm.response.json();
      pm.expect(body.roles).to.include("USER");
      pm.expect(body.roles).to.not.include("ADMIN");
      pm.expect(body).to.not.have.property("password");
  });
  ```

---

**Request 2 — No token → 401**  → expected `401`
- **Method & URL:** `GET {{baseUrl}}/api/users/me`
- **Authorization tab:** `No Auth` (deliberately send nothing — there is no `Authorization` header).
- **Body:** none.
- **Expected response:** `401 Unauthorized`, the JWT entry point's RFC 7807 ProblemDetail (there are **no**
  public reads in TaskFlow — every domain endpoint needs a Bearer token):
  ```json
  {
    "type": "about:blank",
    "title": "Unauthorized",
    "status": 401,
    "detail": "Authentication required: a valid Bearer access token is missing or invalid."
  }
  ```
- **Scripts → Post-response (tests):**
  ```javascript
  pm.test("401 without a token", () => pm.response.to.have.status(401));
  ```

---

**Request 3 — Login (admin), capture tokens**  → expected `200`
- **Method & URL:** `POST {{baseUrl}}/api/auth/login`
- **Authorization tab:** `No Auth` (you are *obtaining* a token, so you send none).
- **Body:** *Body → raw → JSON* (auto-sets `Content-Type: application/json`) — the seeded ADMIN:
  ```json
  { "username": "admin", "password": "admin12345" }
  ```
- **Expected response:** `200 OK`, a `TokenResponse`. `expiresIn` is `900` (the 15-minute `PT15M` access
  TTL, in seconds); `tokenType` is the literal `"Bearer"`:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbi...",
    "refreshToken": "b7f3c1a2-9d4e-4c8a-9f21-0a1b2c3d4e5f",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
  ```
- **Scripts → Post-response (tests):** capture both tokens into collection variables so every later request
  can use them:
  ```javascript
  pm.test("200 with tokens", () => pm.response.to.have.status(200));
  const body = pm.response.json();
  pm.collectionVariables.set("accessToken", body.accessToken);
  pm.collectionVariables.set("refreshToken", body.refreshToken);
  pm.test("token metadata", () => {
      pm.expect(body.tokenType).to.eql("Bearer");
      pm.expect(body.expiresIn).to.eql(900);   // PT15M in seconds
  });
  ```

---

**Request 4 — Me (with admin Bearer)**  → expected `200`
- **Method & URL:** `GET {{baseUrl}}/api/users/me`
- **Authorization tab:** `Bearer Token` → *Token* `{{accessToken}}` (the admin token just captured).
- **Body:** none.
- **Expected response:** `200 OK`, the admin's `UserResponse` — `roles` includes `ADMIN`:
  ```json
  { "id": 1, "username": "admin", "email": "admin@taskflow.local", "roles": ["ADMIN"] }
  ```
- **Scripts → Post-response (tests):**
  ```javascript
  pm.test("200 OK", () => pm.response.to.have.status(200));
  pm.test("principal is admin with ADMIN role", () => {
      const body = pm.response.json();
      pm.expect(body.username).to.eql("admin");
      pm.expect(body.roles).to.include("ADMIN");
  });
  ```

---

**Request 5 — Create a project (with admin Bearer)**  → expected `201`
- **Method & URL:** `POST {{baseUrl}}/api/projects`
- **Authorization tab:** `Bearer Token` → *Token* `{{accessToken}}`.
- **Body:** *Body → raw → JSON* (auto-sets `Content-Type: application/json`). The owner is **not** in the
  body — the server sets it to the authenticated principal:
  ```json
  { "name": "Website", "description": "Marketing site" }
  ```
- **Expected response:** `201 Created` with a `Location` header (`/api/projects/{id}`) and a
  `ProjectResponseDto` whose `owner` is the caller (admin) as a compact summary:
  ```json
  {
    "id": 1,
    "name": "Website",
    "description": "Marketing site",
    "owner": { "id": 1, "username": "admin" }
  }
  ```
- **Scripts → Post-response (tests):**
  ```javascript
  pm.test("201 Created", () => pm.response.to.have.status(201));
  pm.test("owner is the caller (admin)", () => {
      const body = pm.response.json();
      pm.expect(body.owner.username).to.eql("admin");
  });
  ```

---

**Request 6 — Admin area (with admin Bearer)**  → expected `200`
- **Method & URL:** `GET {{baseUrl}}/api/admin/users`
- **Authorization tab:** `Bearer Token` → *Token* `{{accessToken}}` (still the admin token).
- **Body:** none.
- **Expected response:** `200 OK` — `/api/admin/**` is `hasRole("ADMIN")` and this token carries `ADMIN`.
  A plain `List<UserResponse>` (not paged):
  ```json
  [
    { "id": 1, "username": "admin", "email": "admin@taskflow.local", "roles": ["ADMIN"] },
    { "id": 2, "username": "alice", "email": "alice@taskflow.local", "roles": ["USER"] }
  ]
  ```
- **Scripts → Post-response (tests):**
  ```javascript
  pm.test("200 for an ADMIN token", () => pm.response.to.have.status(200));
  pm.test("body is a user array", () => pm.expect(pm.response.json()).to.be.an("array"));
  ```

---

**Request 7 — Tampered token → 401**  → expected `401`
- **Method & URL:** `GET {{baseUrl}}/api/users/me`
- **Authorization tab:** `Bearer Token` → *Token* `{{accessToken}}xxTAMPERED` (append junk so the
  signature no longer verifies).
- **Body:** none.
- **Expected response:** `401 Unauthorized` — the altered token fails signature verification, so the JWT
  filter authenticates nothing and the entry point fires the same ProblemDetail as Request 2:
  ```json
  {
    "type": "about:blank",
    "title": "Unauthorized",
    "status": 401,
    "detail": "Authentication required: a valid Bearer access token is missing or invalid."
  }
  ```
- **Scripts → Post-response (tests):**
  ```javascript
  pm.test("401 for a tampered token", () => pm.response.to.have.status(401));
  ```

---

**Request 8 — Login (alice), capture her tokens**  → expected `200`
- **Method & URL:** `POST {{baseUrl}}/api/auth/login`
- **Authorization tab:** `No Auth`.
- **Body:** *Body → raw → JSON* (auto-sets `Content-Type: application/json`) — alice, from Request 1:
  ```json
  { "username": "alice", "password": "password123" }
  ```
- **Expected response:** `200 OK`, a `TokenResponse` for alice (same shape as Request 3; her `accessToken`
  carries only the `USER` role):
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZS...",
    "refreshToken": "c9e4d2b3-1a0f-4b7c-8e32-1b2c3d4e5f60",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
  ```
- **Scripts → Post-response (tests):** the capture script overwrites the variables with alice's tokens,
  so the following requests now run as alice. **One extra line here:** stash the same value in
  `firstRefreshToken` as well, so Request 11 still has alice's *original* refresh token after Request 10
  rotates `refreshToken` away:
  ```javascript
  pm.test("200 with tokens", () => pm.response.to.have.status(200));
  const body = pm.response.json();
  pm.collectionVariables.set("accessToken", body.accessToken);
  pm.collectionVariables.set("refreshToken", body.refreshToken);
  // Keep a copy of the FIRST refresh token so Request 11 can prove single-use rotation.
  pm.collectionVariables.set("firstRefreshToken", body.refreshToken);
  ```

---

**Request 9 — USER hits admin → 403**  → expected `403`
- **Method & URL:** `GET {{baseUrl}}/api/admin/users`
- **Authorization tab:** `Bearer Token` → *Token* `{{accessToken}}` (now alice's token, from Request 8).
- **Body:** none.
- **Expected response:** `403 Forbidden` — alice is **authenticated** (valid token, so *not* 401) but lacks
  `ROLE_ADMIN`, so `/api/admin/**` denies her. No `AccessDeniedHandler` is wired in this lab, so the body
  is Spring's **default empty 403** (only the status matters here; a JSON ProblemDetail for 403 is a
  stretch goal). This is the whole point of capturing the token per-login: the identical URL that returned
  `200` for the admin token (Request 6) returns `403` for the USER token.
- **Scripts → Post-response (tests):**
  ```javascript
  pm.test("403 for an under-privileged token", () => pm.response.to.have.status(403));
  ```

---

**Request 10 — Refresh (rotate), capture the NEW tokens**  → expected `200`
- **Method & URL:** `POST {{baseUrl}}/api/auth/refresh`
- **Authorization tab:** `No Auth` (the refresh token in the body *is* the credential; no Bearer header).
- **Body:** *Body → raw → JSON* (auto-sets `Content-Type: application/json`). `{{refreshToken}}` currently
  holds **alice's** refresh token (Request 8 overwrote it):
  ```json
  { "refreshToken": "{{refreshToken}}" }
  ```
- **Expected response:** `200 OK`, a `TokenResponse` with a **fresh** `accessToken` and a **new, rotated**
  `refreshToken` (the row you just presented is revoked and replaced — single-use rotation):
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZS...(new)",
    "refreshToken": "d1f5e3c4-2b1a-4c8d-9f43-2c3d4e5f6071",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
  ```
- **Scripts → Post-response (tests):** overwrite the variables with the rotated tokens:
  ```javascript
  pm.test("200 with rotated tokens", () => pm.response.to.have.status(200));
  const body = pm.response.json();
  pm.collectionVariables.set("accessToken", body.accessToken);
  pm.collectionVariables.set("refreshToken", body.refreshToken);
  ```

---

**Request 11 — Replay the old refresh token → 401**  → expected `401`
- **Method & URL:** `POST {{baseUrl}}/api/auth/refresh`
- **Authorization tab:** `No Auth`.
- **Body:** *Body → raw → JSON* (auto-sets `Content-Type: application/json`). Send **`firstRefreshToken`**
  — the token alice received in Request 8 and already spent in Request 10. Do **not** use
  `{{refreshToken}}`: Request 10 rotated that variable to a fresh, still-valid value, and this request
  would return `200` instead of the `401` you are trying to prove:
  ```json
  { "refreshToken": "{{firstRefreshToken}}" }
  ```
- **Expected response:** `401 Unauthorized` — that refresh token was revoked when Request 10 rotated it, so
  it is dead. This is an `InvalidTokenException` from inside the controller, rendered by the
  `GlobalExceptionHandler` as a ProblemDetail:
  ```json
  {
    "type": "about:blank",
    "title": "Invalid token",
    "status": 401,
    "detail": "Refresh token has been revoked"
  }
  ```
- **Scripts → Post-response (tests):**
  ```javascript
  pm.test("401 replaying a rotated refresh token", () => pm.response.to.have.status(401));
  ```

---

**Run it.** Open the **Collection Runner**, run all eleven requests in order, and confirm the status
sequence is **`201, 401, 200, 200, 201, 200, 401, 200, 403, 200, 401`** with every `pm.test` green.
Because every token is captured into a variable, the run needs **no manual editing** between requests.
(On a *second* run against the same database, Request 1 returns `409` — alice already exists. Drop and
recreate `taskflow_lab`, or change alice's username and email, to get the full sequence again.) That
sequence is the JWT contract end-to-end: **login issues a token → the `Bearer` header authenticates every
request → missing/tampered tokens are `401` → role rules still apply (`403`) → refresh rotates and the old
refresh token is `401`.** Re-run it after any change to `JwtService`, the filter, or `SecurityConfig`.

> **Why the token lives in a variable, not typed in:** this is exactly how a real client works — one login,
> then the stored token rides along on every call. The post-response capture script (Requests 3, 8, 10) is
> the Postman equivalent of a browser storing the token and attaching it to each request — and because the
> variable is overwritten at each login/refresh, the *same* `{{accessToken}}` request is `200` for admin
> but `403` for alice, and the *old* refresh token is dead after rotation.

## 22. Part 2 — Acceptance Criteria

- [ ] `pom.xml` adds `jjwt-api` (compile) + `jjwt-impl` + `jjwt-jackson` (runtime) at **0.13.0**;
      `application.properties` adds `app.security.jwt.secret` / `access-token-ttl=PT15M` /
      `refresh-token-ttl=P7D`; `JwtProperties` binds them.
- [ ] `JwtService` issues HS256 tokens (subject + bare-name `roles` claim + `iat`/`exp`) and validates
      them (`isValid` never throws; rejects expired/tampered/foreign-key tokens); the secret is decoded
      via `Keys.hmacShaKeyFor(Decoders.BASE64.decode(...))`.
- [ ] `RefreshToken` entity + `RefreshTokenRepository.findByToken`, and `RefreshTokenService` create /
      verify / **rotate** (single-use: rotate revokes the old row and issues a new one).
- [ ] `AuthService.login` authenticates via the `AuthenticationManager`, issues an access + refresh token
      (`Bearer`, `expiresIn` seconds), and puts **bare** role names in the claim; `refresh` rotates and
      mints a new access token; both 401 on failure (via `AuthenticationException` / `InvalidTokenException`
      handlers).
- [ ] `AuthController` exposes `POST /api/auth/login` and `/refresh` (both `200`, both `permitAll`).
- [ ] `JwtAuthenticationFilter` (a `OncePerRequestFilter`) authenticates the `Authorization: Bearer`
      header, re-adds the `ROLE_` prefix, yields to an existing context, and never throws;
      `JwtAuthenticationEntryPoint` returns a 401 ProblemDetail.
- [ ] `SecurityConfig` is **STATELESS**, `permitAll`s `/api/auth/**`, keeps `/api/admin/**` ADMIN-only,
      **removes `formLogin`/`httpBasic`**, wires the 401 entry point, and registers the JWT filter with
      `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`; CSRF stays disabled.
- [ ] `./mvnw test` is green: the Part 1 suite (unchanged) **plus** `JwtServiceTest`,
      `RefreshTokenRepositoryTest`, `RefreshTokenServiceImplTest`, the updated `AuthServiceImplTest`,
      `AuthControllerLoginTest`, and `JwtAuthenticationFilterTest`.
- [ ] The manual token flow works, including **rotation** (a replayed refresh token → 401).
- [ ] A **Postman collection** (Section 21.2) logs in, **captures the token** into a collection variable
      via a post-response script, and runs the full flow — register → `401` no-token → Bearer `200` →
      USER-on-admin `403` → refresh rotation (old token `401`) — with every `pm.test` passing in the
      Collection Runner.

---

## 23. Appendix — Final-State Reference (your project when Part 2 is done)

Sections 7 and 16 describe TaskFlow as a **sequence of two builds**, so five classes are specified
twice — once in their Part 1 shape, then again as a Part 2 delta. That is the right way to *build* the
project but a poor way to *check* it. This appendix collapses both passes into the **finished state**:
diff your code against it before you submit.

### 23.1 Service interfaces — every method, complete

```java
// service/AuthService.java          — 1 method in Part 1, 3 when finished
UserResponse  register(RegisterRequest request);      // Part 1 (7.5)
TokenResponse login(LoginRequest request);            // Part 2 (16.8)
TokenResponse refresh(RefreshRequest request);        // Part 2 (16.8)

// service/UserService.java          — unchanged by Part 2
UserResponse       getByUsername(String username);
UserResponse       getCurrentUser();
List<UserResponse> getAllUsers();

// service/ProjectService.java       — unchanged by Part 2
ProjectResponseDto                createProject(ProjectRequestDto request, String ownerUsername);
PagedResponse<ProjectResponseDto> getAllProjects(Pageable pageable);
ProjectResponseDto                getProjectById(Long id);
ProjectResponseDto                updateProject(Long id, ProjectRequestDto request);
void                              deleteProject(Long id);

// service/TaskService.java          — unchanged by Part 2
TaskResponseDto                createTask(TaskRequestDto request);
PagedResponse<TaskResponseDto> getTasks(Status status, Long assigneeId, Pageable pageable);
PagedResponse<TaskResponseDto> getTasksByProject(Long projectId, Pageable pageable);
TaskResponseDto                getTaskById(Long id);
TaskResponseDto                updateTask(Long id, TaskRequestDto request);
void                           deleteTask(Long id);

// service/RefreshTokenService.java  — entirely new in Part 2 (16.7)
RefreshToken create(User user);
RefreshToken verify(String token);
RefreshToken rotate(String token);
```

Every `*Impl` is `@Service @RequiredArgsConstructor`; all but `RefreshTokenServiceImpl` also carry
class-level `@Transactional(readOnly = true)` with `@Transactional` on the write methods.

### 23.2 Repository interfaces — every method, complete

```java
// UserRepository extends JpaRepository<User, Long>
Optional<User> findByUsername(String username);
boolean        existsByUsername(String username);
boolean        existsByEmail(String email);

// ProjectRepository extends JpaRepository<Project, Long>       @EntityGraph(attributePaths = "owner")
@Override Page<Project>     findAll(Pageable pageable);
@Override Optional<Project> findById(Long id);

// TaskRepository extends JpaRepository<Task, Long>  @EntityGraph({"project", "assignee"}) on all five
@Override Page<Task>     findAll(Pageable pageable);
@Override Optional<Task> findById(Long id);
          Page<Task>     findByProjectId(Long projectId, Pageable pageable);
          Page<Task>     findByAssigneeId(Long assigneeId, Pageable pageable);
          Page<Task>     findByStatus(Status status, Pageable pageable);

// RefreshTokenRepository extends JpaRepository<RefreshToken, Long>   — new in Part 2
Optional<RefreshToken> findByToken(String token);
```

### 23.3 Every endpoint, after Part 2

Section 8.1's "Auth" column describes the **Part 1** (HTTP Basic) world. This is the finished contract:
domain endpoints are reached with `Authorization: Bearer <access-jwt>`, and the three `/api/auth/*`
routes are public.

| Method | Path | Access | Success |
| ------ | ---- | ------ | ------- |
| POST | `/api/auth/register` | public | `201` `UserResponse` |
| POST | `/api/auth/login` | public | `200` `TokenResponse` |
| POST | `/api/auth/refresh` | public | `200` `TokenResponse` |
| GET | `/api/users/me` | any valid token | `200` `UserResponse` |
| POST | `/api/projects` | any valid token | `201` + `Location` |
| GET | `/api/projects` | any valid token | `200` `PagedResponse<ProjectResponseDto>` |
| GET | `/api/projects/{id}` | any valid token | `200` |
| PUT | `/api/projects/{id}` | any valid token | `200` |
| DELETE | `/api/projects/{id}` | any valid token | `204` |
| GET | `/api/projects/{id}/tasks` | any valid token | `200` `PagedResponse<TaskResponseDto>` |
| POST | `/api/tasks` | any valid token | `201` + `Location` |
| GET | `/api/tasks` (`?status=&assigneeId=`) | any valid token | `200` paged |
| GET | `/api/tasks/{id}` | any valid token | `200` |
| PUT | `/api/tasks/{id}` | any valid token | `200` |
| DELETE | `/api/tasks/{id}` | any valid token | `204` |
| GET | `/api/admin/users` | **ADMIN token** | `200` `List<UserResponse>` |

No token or an invalid/expired one → **401** (the entry point's ProblemDetail). A valid USER token on
`/api/admin/**` → **403**.

### 23.4 The complete file tree (50 main classes, 18 test classes)

```
src/main/java/com/example/_7_jwt/
├── Application.java
├── config/      JwtProperties
├── model/       Role  Status  Priority  User  Project  Task  RefreshToken
├── repository/  UserRepository  ProjectRepository  TaskRepository  RefreshTokenRepository
├── dto/         RegisterRequest  LoginRequest  RefreshRequest  TokenResponse
│                UserResponse  UserSummaryDto  PagedResponse
│                ProjectRequestDto  ProjectResponseDto  ProjectSummaryDto
│                TaskRequestDto  TaskResponseDto
├── exception/   ResourceNotFoundException  DuplicateResourceException
│                InvalidTokenException  GlobalExceptionHandler
├── service/     AuthService(+Impl)  UserService(+Impl)
│                ProjectService(+Impl)  TaskService(+Impl)  RefreshTokenService(+Impl)
├── security/    UserDetailsServiceImpl  SecurityConfig  AdminUserSeeder
│                JwtService  JwtAuthenticationFilter  JwtAuthenticationEntryPoint
└── controller/  AuthController  UserController  ProjectController
                 TaskController  AdminController

src/test/java/com/example/_7_jwt/
├── model/       EntityMappingTest
├── repository/  UserRepositoryTest  TaskRepositoryTest  RefreshTokenRepositoryTest
├── security/    UserDetailsServiceImplTest  JwtServiceTest  JwtAuthenticationFilterTest
├── service/     AuthServiceImplTest  UserServiceImplTest  ProjectServiceImplTest
│                TaskServiceImplTest  RefreshTokenServiceImplTest
└── controller/  AuthControllerTest  AuthControllerLoginTest  UserControllerTest
                 ProjectControllerTest  TaskControllerTest  AdminControllerTest
```

### 23.5 The five classes that exist in two shapes

If you only ever read one of the two passes, these are the classes you will get wrong:

| Class | Part 1 (Section 7) | Finished (Section 16) |
| ----- | ------------------ | --------------------- |
| `AuthService` | 1 method: `register` | **3 methods**: `register`, `login`, `refresh` (16.8) |
| `AuthServiceImpl` | 2-arg constructor | **5-arg** constructor (16.8) — breaks `AuthServiceImplTest` until updated (19.1) |
| `AuthController` | 1 handler: `POST /register` | **3 handlers** (+ `/login`, `/refresh`) (16.10) |
| `GlobalExceptionHandler` | 5 handlers | **7 handlers** (+ two 401s) (16.9) |
| `SecurityConfig` | HTTP Basic + form login, 3 beans | **STATELESS JWT**, 6 beans, entry point, filter (16.11) |

---

## 24. Optional Stretch Goals (not graded)

1. **Logout / revoke-all**: add `POST /api/auth/logout` that revokes the caller's refresh tokens (a
   `RefreshTokenRepository` derived delete/update by user), and discuss that the *access* token still
   works until `exp` (the stateless trade-off).
2. **`jti` + a denylist**: add a `jti` (JWT id) claim and a small in-memory denylist to enable
   best-effort access-token revocation; weigh it against staying purely stateless.
3. **Kid / key rotation**: put a `kid` header on issued tokens and support verifying against the current
   *or* previous secret, so you can rotate the signing key without invalidating live tokens.
4. **Return `403` ProblemDetail too**: wire an `AccessDeniedHandler` (alongside the entry point) so a
   USER hitting `/api/admin/**` gets a JSON `ProblemDetail` body instead of an empty 403.
5. **RS256 instead of HS256**: switch to an asymmetric key pair (`Keys.keyPairFor` / a loaded private
   key) so verifiers need only the *public* key — and discuss when asymmetric signing matters.
6. **Refresh-token reuse detection**: when a *revoked* refresh token is presented, treat it as a theft
   signal and revoke the whole token family for that user.

## 25. Instructor Notes

Lab 6 taught the security foundation on an existing project; Lab 7 does two things at once and is the
largest lab in the course. **Part 1** verifies the student can *compose an entire secured REST
application from an empty directory* — every layer, wired correctly, green before any new concept is
introduced. Insist they get Part 1 green first; it is the load-bearing check that the Lab 1–6 skills have
actually stuck. **Part 2** is where the new material lives: **stateless authentication**. The pedagogical
arc is "the server stops remembering you" — the `SecurityContext` is rebuilt per request from a signed,
tamper-evident-but-readable token, which forces the access/refresh split (short unrevocable access token +
long-lived, persisted, **rotating** refresh token). Make sure they can articulate *why* each piece exists:
why STATELESS, why the access token is short, why the refresh token is a DB row and single-use, why CSRF
is now unambiguously safe to disable, and why the login authenticates *through the `AuthenticationManager`*
(the seam Lab 6 deliberately exposed). Emphasize the common failure modes: forgetting that the JWT filter
must **yield to an existing context** (or the `@WithMockUser` slice tests break), forgetting the **401
entry point** after removing form/basic (Spring's default is a 403), the **`ROLE_` prefix round-trip**
(strip into the claim, re-add in the filter), and the **`AuthServiceImpl` constructor change** that breaks
the base test until the `@BeforeEach` is updated. Note the Spring Boot 4.0 / Security 7 specifics
throughout (Sections 5.4–5.5): secure-by-default, the lambda DSL, the removed 5/6 config surface,
`AuthenticationConfiguration` for the manager, the renamed test-slice packages, and Jackson 3. Finally,
keep the **Lab 8 boundary** crisp: Lab 7 sets `owner = principal` on create but enforces **no** per-row
ownership — that is method security, and it is next.
