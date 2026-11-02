# Lab 10 — StockFlow: Integration Testing with `@SpringBootTest`

> **Start from your completed Lab 9 project.** This lab is an increment on StockFlow, not a rebuild.
> You will copy your Lab 9 module and add a fifth test layer on top of it. **Not one line of main code
> changes.**
>
> **Read first:** `COURSE_10_INTEGRATION_TESTING.md` (in this folder). This assignment tells you *what*
> to build; the course document explains *why* each mechanism behaves as it does. Section numbers below
> reference it as **[C-§n]**.

| | |
| --- | --- |
| **Module** | `_10_integration_testing` (base package `com.example._10_integration_testing`) |
| **Starting point** | your finished Lab 9 StockFlow (`_9_docker`), 167 tests green; Docker files carry over untouched |
| **Stack** | Spring Boot 4.0.6 · Spring Framework 7.0.8 · Spring Security 7.0.5 · Java 21 · Maven · H2 in tests |
| **New dependencies** | **none** — `spring-boot-resttestclient` and `spring-boot-webmvc-test` already arrive through `spring-boot-starter-webmvc-test` |
| **Deliverable** | the module below with `./mvnw test` fully green, plus the Lab 9 Postman collection shipped unchanged (§13) |
| **Reference test count** | **208** (167 carried forward from Lab 9 + 41 new, in 6 new test classes, in **2** new application contexts) |

---

## 1. Problem Statement & Goal

StockFlow has 167 green tests in four layers, and not one of them sends a real HTTP request through the
real JWT filter into a real controller, a real service transaction and a real database. Each layer
proves its own slice; the **seams** between them are untested [C-§1.2]. Store the seeded admin's
password unencoded in `AdminUserSeeder` and every test stays green while the real `POST /api/auth/login`
for `admin` answers 401. Reorder two `requestMatchers` lines and the only tests that notice are the
slices that happen to import `SecurityConfig`.

This lab adds the missing layer: **full-context integration tests** that boot the whole application
with `@SpringBootTest`, drive it with the Spring Boot 4 test clients — `RestTestClient` over a real
socket and `MockMvcTester` in-process — authenticate with real tokens, seed and clean the database with
`@Sql`, and prove, with two deliberately ordered classes, exactly when transactional rollback isolates a
test and when it does not. It finishes with the authorization matrix of Lab 9 §8.2 asserted end to end
over real HTTP — the first time in the course that the JWT filter, the URL rules and method security are
exercised together.

You will also learn the discipline that keeps such a suite fast: **41 tests, 2 contexts, 19 seconds for
the whole run** (on the reference machine — a slower machine scales every timing in this document, the
Lab 9 baseline included; the context *count* is what must match).

## 2. Learning Objectives (what this assesses)

By the end you can:

1. Write `@SpringBootTest` classes with the right `webEnvironment`, and keep the annotations on a base
   class so every subclass shares one cached context [C-§2].
2. Drive the API with `RestTestClient` (`@AutoConfigureRestTestClient`) against a real port, and with
   `MockMvcTester` (`@AutoConfigureMockMvc`) in-process, using each client's fluent assertions [C-§3].
3. Authenticate integration tests through the real login endpoint with a cached-token helper, and
   explain why `@WithMockUser` is not used [C-§3.4, §3.5].
4. Build request data with an object mother that returns builders [C-§4].
5. Seed a known catalogue and clean up with `@Sql`, in the right execution phase and the right
   transaction mode [C-§5].
6. Demonstrate — not just state — why `@Transactional` rollback isolates a `MOCK` test and cannot
   isolate a `RANDOM_PORT` test [C-§6].
7. Prove the authorization matrix over real HTTP, including which component renders each refusal
   [C-§7].
8. Read the start-up log to count contexts and diagnose a suite that has stopped sharing them [C-§2.5].

## 3. Starting Point

### 3.1 Create the Lab 10 module

Copy your finished Lab 9 project and rename it. From the repository root:

1. Copy `_9_docker` to a new folder `_10_integration_testing`.
2. Delete the copied `target/` directory and the Lab 9 documents (`COURSE_09_DOCKER.md`,
   `LAB_09_DOCKER_STOCKFLOW.md`). **Keep** `Dockerfile`, `.dockerignore`, `compose.yaml` and
   `StockFlow_Docker.postman_collection.json` — they carry forward unchanged.
3. Rename the package folders: `src/main/java/com/example/_9_docker` →
   `src/main/java/com/example/_10_integration_testing`, and the same under `src/test/java/com/example/`.
4. Replace every occurrence of the text `_9_docker` with `_10_integration_testing` across `*.java`
   and `pom.xml`. This covers `package`/`import` statements and the pom `<artifactId>`/`<name>`.
   Nothing else carries the module name: `spring.application.name` is `stockflow`, and the
   `Dockerfile`, `compose.yaml` and `.dockerignore` copy `target/*.jar` — `grep -r _9_docker .` after
   the replacement should find nothing. Your IDE's rename refactoring does it, or, from Git Bash inside
   the new module:

   ```bash
   grep -rl "_9_docker" --include='*.java' --include='pom.xml' . | xargs sed -i 's/_9_docker/_10_integration_testing/g'
   ```
5. Update the pom `<description>` to `Lab 10 - StockFlow: integration testing with @SpringBootTest`.
6. **Confirm the baseline before adding anything** (from inside the new module): `./mvnw test` →
   **167 tests, all green.**

`application.properties` (main and test), the JWT configuration, the seeded `admin` / `admin12345`
account and the Postgres datasource for local runs all carry over unchanged.

### 3.2 What this lab leaves alone

**All of `src/main`.** Every class of Lab 9 stays byte-for-byte the same. All 24 existing test classes
stay the same too. Everything you write lives in **one new test package**,
`com.example._10_integration_testing.integration`, plus two SQL files under `src/test/resources/sql/`.

### 3.3 Facts about the test environment you will rely on

These are consequences of files you already have [C-§2.3]; know them before you start:

- `src/test/resources/application.properties` replaces main's, so a `@SpringBootTest` runs on an
  **embedded H2** database (`jdbc:h2:mem:<uuid>`, one per context, schema generated by Hibernate).
- `AdminUserSeeder` runs in every context: **`admin` / `admin12345` exists**, committed at start-up.
- `JwtProperties` use their code defaults (Lab 7): the same secret in every context, so a token from one
  context validates in another.
- No CLERK exists until you register one.

## 4. The Big New Idea

Two ideas, really [C-§2, C-§6]:

**The full context, chosen per class.** `@SpringBootTest` boots the *whole* application. With
`webEnvironment = MOCK` (the default) the request is dispatched in-process on the test thread — no
port, fastest, and `@Transactional` on the test rolls the controller's write back because the service's
transaction *joins* the test's. With `RANDOM_PORT` a real Tomcat answers on a free port — the real wire,
real serialisation, real headers — and the request is handled on a **worker thread** whose transaction
commits on its own, so the test's rollback reaches nothing and cleanup must be explicit.

**One context per environment.** The whole configuration sits on two abstract base classes. Every
concrete test class extends one of them and adds no annotation that changes the context key. Result:
six classes, forty-one tests, **two** contexts.

## 5. Setup Delta

**No new dependencies.** Confirm it rather than trust it:

```bash
./mvnw dependency:tree -Dscope=test | grep -E "resttestclient|webmvc-test"
```

You should see `spring-boot-starter-webmvc-test` → `spring-boot-webmvc-test` (MockMvc, `MockMvcTester`)
and `spring-boot-resttestclient` (`RestTestClient`, `@AutoConfigureRestTestClient`) already present.

**No new properties.** Neither `application.properties` changes.

Two build facts you depend on without writing them:

- **Surefire runs every test class in one JVM** (`forkCount=1`, `reuseForks=true` are the defaults), so
  the Spring test context cache spans the whole run. Change either and every class boots its own
  context.
- **JUnit's `@TestMethodOrder`** is available without configuration; two classes in this lab use it on
  purpose (§7.9, §7.10).

## 6. File Inventory

**NEW (11 files, all under `src/test`)**

```
resources/sql/seed-catalogue.sql
resources/sql/cleanup-catalogue.sql
java/…/integration/support/RequestMother.java
java/…/integration/AbstractRestIntegrationTest.java          ← RANDOM_PORT base: RestTestClient + login helper
java/…/integration/AbstractMockMvcIntegrationTest.java       ← MOCK base: MockMvcTester + login helper
java/…/integration/CatalogueEndToEndTest.java                (4 tests)
java/…/integration/ValidationAndConflictTest.java            (7 tests)
java/…/integration/ProductListingTest.java                   (7 tests)
java/…/integration/TransactionalRollbackIsolationTest.java   (3 tests)
java/…/integration/RealSocketCleanupIsolationTest.java       (3 tests)
java/…/integration/AuthorizationMatrixTest.java              (17 tests)
```

**CHANGED:** nothing beyond the §3.1 rename (package names, pom coordinates and description).
**Main sources:** no behavioural change — every class is identical to Lab 9 apart from its package name.

---

## 7. Implementation Inventory (class-by-class)

Names below are normative: the reference tests, the acceptance criteria and the instructor's grading
script use them.

### 7.1 `resources/sql/seed-catalogue.sql` — NEW

Reproduce it exactly; the listing tests assert against these rows.

```sql
insert into categories (id, name, description) values
  (9001, 'Fasteners', 'Bolts, nuts, screws'),
  (9002, 'Tools',     'Hand tools');

insert into suppliers (id, name, email, phone) values
  (9001, 'Acme Industrial', 'sales@acme.example', '+90 212 000 00 00');

insert into products (id, sku, name, description, unit_price, quantity_on_hand, reorder_level, category_id, supplier_id) values
  (9001, 'SKU-001', 'Hex Bolt M8',       'Zinc plated, 40 mm', 0.25,  500, 100, 9001, 9001),
  (9002, 'SKU-002', 'Hex Nut M8',        'Zinc plated',        0.10,   40, 100, 9001, null),
  (9003, 'SKU-003', 'Claw Hammer',       '450 g, fibreglass',  12.50,   3,   5, 9002, 9001),
  (9004, 'SKU-004', 'Screwdriver Set',   '6 pieces',           18.00,  25,   5, 9002, null),
  (9005, 'SKU-005', 'Wood Screw 4x40',   'Box of 200',         0.05,  100, 100, 9001, 9001);
```

Why these numbers: ids start at **9001** so they can never collide with identity-generated ids
[C-§5.2]; column names are Hibernate's snake_case forms of the entity fields, and the two foreign keys
are the explicit `@JoinColumn` names `category_id` / `supplier_id`. Facts the tests derive from the
rows:

| Fact | Value |
| --- | --- |
| default order (`sort = "sku"`) | SKU-001, 002, 003, 004, 005 |
| `sort=unitPrice,desc` | SKU-004 (18.00), SKU-003 (12.50), SKU-001 (0.25), SKU-002 (0.10), SKU-005 (0.05) |
| low stock (`quantity_on_hand <= reorder_level`) | SKU-002 (40 ≤ 100), SKU-003 (3 ≤ 5), SKU-005 (100 ≤ 100 — equality counts) |
| low stock **and** category 9001 | SKU-002, SKU-005 |
| product 9001 | category `Fasteners`, supplier `Acme Industrial`, not low |
| product 9002 | supplier `null`, low |

### 7.2 `resources/sql/cleanup-catalogue.sql` — NEW

```sql
delete from stock_movements;
delete from products;
delete from suppliers;
delete from categories;
```

Children first, so no foreign key fails. **Do not** delete `users` or `refresh_tokens`: `admin` and the
registered clerk are shared fixtures for the whole suite [C-§5.3].

### 7.3 `integration/support/RequestMother` — NEW

A `public final class` with a private constructor and five `public static` methods, each returning the
**Lombok builder** of a request DTO, already filled with valid defaults [C-§4]:

| Method | Returns | Defaults |
| --- | --- | --- |
| `category()` | `CategoryRequestDto.CategoryRequestDtoBuilder` | name `Bearings`, description `Ball and roller bearings` |
| `supplier()` | `SupplierRequestDto.SupplierRequestDtoBuilder` | name `Acme Industrial`, email `sales@acme.example`, phone `+90 212 000 00 00` |
| `product(Long categoryId)` | `ProductRequestDto.ProductRequestDtoBuilder` | sku `BRG-6204`, name `Deep groove ball bearing 6204`, description `20x47x14 mm, sealed`, unitPrice `new BigDecimal("4.75")`, reorderLevel `5`, categoryId = the argument, no supplier |
| `movementIn(int quantity)` | `StockMovementRequestDto.StockMovementRequestDtoBuilder` | type `IN`, quantity = the argument, reason `Goods received` |
| `movementOut(int quantity)` | same | type `OUT`, quantity = the argument, reason `Sales order` |

Usage everywhere below: `RequestMother.product(categoryId).build()` for "a valid product";
`RequestMother.product(null).sku("").build()` for "exactly these things wrong".

### 7.4 `integration/AbstractRestIntegrationTest` — NEW (the real-socket base)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public abstract class AbstractRestIntegrationTest { … }
```

| Member | Spec |
| --- | --- |
| `public static final String ADMIN_USERNAME = "admin"`, `ADMIN_PASSWORD = "admin12345"` | the seeded account |
| `public static final String CLERK_USERNAME = "clerk.it"`, `CLERK_PASSWORD = "clerk12345"` | the account this base registers |
| `private static final Map<String, String> ACCESS_TOKENS = new ConcurrentHashMap<>()` | JVM-wide token cache, keyed by username [C-§3.5] |
| `@Autowired protected RestTestClient client` | bound to the running server by `@AutoConfigureRestTestClient` |
| `@BeforeEach void ensureClerkExists()` | `POST /api/auth/register` with `new RegisterRequest(CLERK_USERNAME, "clerk.it@stockflow.local", CLERK_PASSWORD)`; `.expectStatus().value(status -> assertThat(status).isIn(201, 409))` — 201 the first time, 409 every time after; both mean the clerk exists |
| `protected String loginAs(String username, String password)` | `ACCESS_TOKENS.computeIfAbsent(username, u -> …)`: `POST /api/auth/login` with `new LoginRequest(u, password)`, `.expectStatus().isOk()`, `.expectBody(TokenResponse.class).returnResult().getResponseBody()`; assert `tokenType` is `"Bearer"`; return `getAccessToken()` |
| `protected String adminBearer()` | `"Bearer " + loginAs(ADMIN_USERNAME, ADMIN_PASSWORD)` |
| `protected String clerkBearer()` | `"Bearer " + loginAs(CLERK_USERNAME, CLERK_PASSWORD)` |
| `protected CategoryResponseDto createCategoryAsAdmin(CategoryRequestDto request)` | `POST /api/categories` with `adminBearer()`, `contentType(APPLICATION_JSON)`, `.body(request)`; `.expectStatus().isCreated()`; return the `CategoryResponseDto` body (assert not null) |
| `protected ProductResponseDto createProductAsAdmin(ProductRequestDto request)` | the same for `POST /api/products` |

The full `loginAs` is the one method worth copying verbatim from the course [C-§3.5]. Note the header
constant: `HttpHeaders.AUTHORIZATION`.

### 7.5 `integration/AbstractMockMvcIntegrationTest` — NEW (the mock-servlet base)

```java
@SpringBootTest                 // webEnvironment = MOCK is the default
@AutoConfigureMockMvc
public abstract class AbstractMockMvcIntegrationTest { … }
```

| Member | Spec |
| --- | --- |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | as above. **Admin only** — the MOCK context has its own H2, so the clerk registered by the socket suite does not exist here [C-§3.5] |
| `private static final Map<String, String> ACCESS_TOKENS` | its own cache |
| `@Autowired protected MockMvcTester mvc` | registered by `@AutoConfigureMockMvc`; includes the real security chain (every servlet `Filter` bean is added) but **not** the `springSecurity()` configurer — Boot 4 ships that in a separate module, `spring-boot-security-test`, which this project does not depend on — which is why `@WithMockUser` would give 401 here and why we send real tokens instead [C-§3.4] |
| `@Autowired protected ObjectMapper objectMapper` | `tools.jackson.databind.ObjectMapper` (Jackson 3) |
| `protected String json(Object body)` | `objectMapper.writeValueAsString(body)` — `MockMvcTester.content(...)` takes a `String` |
| `protected String bodyOf(MvcTestResult result)` | `new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8)` — `getContentAsString()` declares a checked exception; the byte route does not |
| `protected long idOf(MvcTestResult result)` | `objectMapper.readTree(bodyOf(result)).get("id").asLong()` |
| `protected String loginAs(String username, String password)` | as in §7.4 but with the tester: `mvc.post().uri("/api/auth/login").contentType(APPLICATION_JSON).content(json(new LoginRequest(u, password))).exchange()`; `assertThat(result).hasStatusOk()`; `objectMapper.readValue(bodyOf(result), TokenResponse.class)`; return the access token |
| `protected String adminBearer()` | `"Bearer " + loginAs(…)` |

### 7.6 `integration/CatalogueEndToEndTest` — NEW (extends `AbstractRestIntegrationTest`)

Class annotation: `@Sql(scripts = "/sql/cleanup-catalogue.sql", executionPhase =
Sql.ExecutionPhase.AFTER_TEST_METHOD)` — every test writes through the server, so every test cleans up.

| Test | Steps and assertions |
| --- | --- |
| `adminCreatesCatalogueThenClerkRecordsMovement` | (1) `createCategoryAsAdmin(RequestMother.category().build())` → id not null, name `Bearings`. (2) `POST /api/products` as admin with `RequestMother.product(category.id)`: `isCreated()`, `expectHeader().exists(LOCATION)`, `expectBody(ProductResponseDto.class).returnResult()`; assert sku `BRG-6204`, unitPrice `isEqualByComparingTo("4.75")`, quantityOnHand 0, reorderLevel 5, **lowStock true** (0 ≤ 5), `category.id`/`category.name`, supplier null, and `assertThat(created.getResponseHeaders().getLocation()).hasPath("/api/products/" + product.getId())` (AssertJ's `UriAssert`). (3) `POST /api/products/{id}/movements` **as clerk** with `movementIn(40)`: `isCreated()`, `expectHeader().doesNotExist(LOCATION)`, `expectBody(StockMovementResponseDto.class).value(m -> …)`: type IN, quantity 40, quantityAfter 40, occurredAt not null, `product.sku`, `recordedBy.username == CLERK_USERNAME`. (4) the same `POST` as clerk with `movementOut(15)`: `isCreated()`, `jsonPath("$.type") == "OUT"`, `$.quantityAfter == 25`. (5) `GET /api/products/{id}` as clerk: `$.quantityOnHand == 25`, `$.lowStock == false`, `$.category.name == "Bearings"`. (6) `GET /api/products/{id}/movements` as clerk: `$.totalElements == 2`, `$.content[0].type == "OUT"` (newest first — the controller's default sort is `occurredAt` desc), `$.content[0].quantityAfter == 25`, `$.content[1].quantityAfter == 40` |
| `locationHeaderResolvesToTheCreatedResource` | `POST /api/categories` as admin with `category().name("Abrasives")`; take `getResponseHeaders().getLocation()`; assert not null and path `/api/categories/{id}`; then `client.get().uri(location)` (the absolute URI) as clerk → 200, `$.id == created.getId()`, `$.name == "Abrasives"` |
| `updateAttachesASupplier` | create category + product; `POST /api/suppliers` as admin with `supplier()` → `SupplierResponseDto`; `PUT /api/products/{id}` as admin with `product(category.id).supplierId(acme.id)` → 200, `$.id` unchanged, `$.supplier.id`, `$.supplier.name == "Acme Industrial"` |
| `deleteThenNotFound` | create category + product; `DELETE /api/products/{id}` as admin → `isNoContent()` and `expectBody().isEmpty()`; `GET` the same id as admin → `isNotFound()`, `$.title == "Resource not found"`, `$.status == 404` |

### 7.7 `integration/ValidationAndConflictTest` — NEW (extends `AbstractMockMvcIntegrationTest`)

Class annotation: the same AFTER-cleanup `@Sql`. Private helper `MvcTestResult postAsAdmin(String uri,
String jsonBody)` = `mvc.post().uri(uri).header(AUTHORIZATION, adminBearer()).contentType(APPLICATION_JSON).content(jsonBody).exchange()`.
All assertions use `assertThat(result)` from AssertJ on the `MvcTestResult` [C-§3.3]. Rows that say
"create category + product" do it with `postAsAdmin` + `idOf` (the MOCK base has no create helpers), and
every movement in this class is recorded by **admin** — the only account in this context.
`extractingPath(...).asNumber()` yields the parsed `Integer`, so compare with `isEqualTo(400)`, never `400L`.

| Test | Request | Assertions |
| --- | --- | --- |
| `blankCategoryNameIs400` | `POST /api/categories`, `category().name("   ")` | `hasStatus(HttpStatus.BAD_REQUEST)`, `hasContentTypeCompatibleWith(APPLICATION_PROBLEM_JSON)`, `bodyJson().extractingPath("$.title")` = `Validation failed`, `$.status` `asNumber()` = 400, `$.errors.name` = `Name is required` |
| `invalidProductReportsAllFieldErrors` | `POST /api/products`, `product(null).sku("").unitPrice(new BigDecimal("-1")).reorderLevel(-3)` | 400; `extractingPath("$.errors").asMap()` contains `sku → SKU is required`, `unitPrice → Unit price cannot be negative`, `reorderLevel → Reorder level cannot be negative`, `categoryId → Category id is required`, and `hasSize(4)` |
| `malformedJsonIs400` | `POST /api/categories`, body `{"name": ` | 400, `$.title` = `Malformed request`, `bodyJson().doesNotHavePath("$.errors")` |
| `duplicateCategoryNameIs409` | post `category()` twice | first `hasStatus(CREATED)`; second `hasStatus(CONFLICT)`, `$.title` = `Duplicate value`, `$.detail` `asString().contains("Bearings")` |
| `deletingCategoryWithProductsIs409` | create category (take `idOf`) and a product in it; `DELETE /api/categories/{id}` as admin | `CONFLICT`, `$.title` = `Resource in use`, `$.detail` contains `products`; then `GET /api/categories/{id}` → `hasStatusOk()` and `$.name == "Bearings"` (the refusal happened before any SQL) |
| `outLargerThanStockIs409AndStockUnchanged` | create category + product; `movementIn(10)` → CREATED; `movementOut(25)` | `CONFLICT`, `$.title` = `Insufficient stock`; then `GET /api/products/{id}` → `$.quantityOnHand` `asNumber()` = 10 |
| `movementOnUnknownProductIs404` | `POST /api/products/999999/movements`, `movementIn(1)` | `NOT_FOUND`, `$.title` = `Resource not found` |

### 7.8 `integration/ProductListingTest` — NEW (extends `AbstractRestIntegrationTest`)

Class annotations (two `@Sql`, stacked): `@Sql(scripts = "/sql/seed-catalogue.sql")` (default phase,
before **every** method) and the AFTER-cleanup `@Sql`. A `private static final
ParameterizedTypeReference<PagedResponse<ProductResponseDto>> PAGE_OF_PRODUCTS = new
ParameterizedTypeReference<>() { };` and a helper `PagedResponse<ProductResponseDto> list(String uri)` =
`GET uri` **as clerk** → `isOk()` → `expectBody(PAGE_OF_PRODUCTS).returnResult().getResponseBody()`
(assert not null). Order assertions use AssertJ `extracting(ProductResponseDto::getSku).containsExactly(...)`.

| Test | URI | Assertions |
| --- | --- | --- |
| `defaultListingIsSortedBySku` | `/api/products` | skus 001…005 in order; page 0; size 20; totalElements 5; totalPages 1; last true |
| `secondPageOfTwo` | `?page=1&size=2` | SKU-003, SKU-004; page 1; size 2; totalElements 5; totalPages 3; last false |
| `lastPageReportsLast` | `?page=2&size=2` | SKU-005 only; last true |
| `sortParameterOverridesDefault` | `?sort=unitPrice,desc` | SKU-004, 003, 001, 002, 005 |
| `lowStockFilter` | `?lowStock=true` | SKU-002, 003, 005; every element `isLowStock()`; totalElements 3 |
| `categoryAndLowStockFiltersCombine` | `?categoryId=9001&lowStock=true` | SKU-002, SKU-005; every `category.name` is `Fasteners` |
| `nestedSummariesOfSeededRows` | `GET /api/products/9001` and `/9002` (jsonPath form) | 9001: `$.sku == "SKU-001"`, `$.category.name == "Fasteners"`, `$.supplier.name == "Acme Industrial"`, `$.lowStock == false`; 9002: `jsonPath("$.supplier").isEmpty()`, `$.lowStock == true` |

### 7.9 `integration/TransactionalRollbackIsolationTest` — NEW (extends `AbstractMockMvcIntegrationTest`)

Class annotations: **`@Transactional`** and `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`.
Fields: `@Autowired CategoryRepository categoryRepository; @Autowired UserRepository userRepository;`
constant `PROBE_NAME = "Rollback Probe"`. No `@Sql` anywhere — that is the point [C-§6.2].

| Order | Test | Assertions |
| --- | --- | --- |
| 1 | `createdRowIsVisibleInsideTheTestTransaction` | `TestTransaction.isActive()` and `isFlaggedForRollback()` are true; `POST /api/categories` as admin with `category().name(PROBE_NAME)` → `hasStatus(CREATED)`; `categoryRepository.existsByNameIgnoreCase(PROBE_NAME)` is **true** |
| 2 | `rowWasRolledBack` | `existsByNameIgnoreCase(PROBE_NAME)` is **false**; `categoryRepository.count()` is 0 |
| 3 | `startupSeedIsNotAffected` | `userRepository.existsByUsername(ADMIN_USERNAME)` is true |

### 7.10 `integration/RealSocketCleanupIsolationTest` — NEW (extends `AbstractRestIntegrationTest`)

Class annotation: `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` **only** — `@Transactional`
and `@Sql` go on individual methods here, and where they go is the lesson [C-§6.3, C-§5.4]. Field:
`@Autowired CategoryRepository categoryRepository;` constant `PROBE_NAME = "Socket Probe"`.

| Order | Annotations | Test | Assertions |
| --- | --- | --- | --- |
| 1 | `@Transactional` | `serverCommitsOnItsOwnThread` | `TestTransaction.isActive()` true; `POST /api/categories` as admin with `category().name(PROBE_NAME)` → `isCreated()`; `existsByNameIgnoreCase(PROBE_NAME)` is **true** — already committed by the worker thread |
| 2 | `@Sql(scripts = "/sql/cleanup-catalogue.sql", executionPhase = AFTER_TEST_METHOD)` | `rowSurvivedTheRollback` | `existsByNameIgnoreCase(PROBE_NAME)` is **still true** — test 1's rollback reached nothing |
| 3 | — | `cleanupScriptEmptiedTheTable` | `existsByNameIgnoreCase(PROBE_NAME)` false; `count()` is 0 |

Do **not** put `@Transactional` on test 2: inside a transactional test the AFTER script runs *inside*
the test transaction and is rolled back with it (§10.4, gotcha 2).

### 7.11 `integration/AuthorizationMatrixTest` — NEW (extends `AbstractRestIntegrationTest`)

Class annotation: the AFTER-cleanup `@Sql`. A `private static final
ParameterizedTypeReference<List<UserResponse>> USER_LIST = new ParameterizedTypeReference<>() { };`.
Seventeen tests, all through real tokens [C-§7]:

| # | Test | Caller | Request | Assertions |
| --- | --- | --- | --- | --- |
| 1 | `healthIsPublic` | anonymous | `GET /actuator/health` | 200, `$.status == "UP"` |
| 2 | `registerIsPublic` | anonymous | `POST /api/auth/register`, username `"matrix." + System.nanoTime()`, email `username + "@stockflow.local"`, password `matrix12345` | 201; `expectBody(UserResponse.class).value(u -> …)`: username echoes, roles `containsExactly(Role.CLERK)` |
| 3 | `wrongPasswordIs401` | anonymous | `POST /api/auth/login` with `new LoginRequest(ADMIN_USERNAME, "not-the-password")` | 401, `$.title == "Authentication failed"`, `$.status == 401` |
| 4 | `anonymousReadIs401` | anonymous | `GET /api/products` | 401; `expectHeader().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON)`; `$.title == "Unauthorized"`; `$.detail` contains `Bearer` |
| 5 | `tamperedTokenIs401` | tampered admin token | `GET /api/products` | 401. Tamper the **first** character of the signature segment: `int s = token.lastIndexOf('.') + 1; char c = token.charAt(s); char flipped = c == 'A' ? 'B' : 'A'; tampered = token.substring(0, s) + flipped + token.substring(s + 1)` [C-§7.3] |
| 6 | `clerkCanRead` | clerk | `GET /api/products` | 200, `jsonPath("$.content").isArray()` |
| 7 | `clerkCannotCreateCategory` | clerk | `POST /api/categories`, `category()` | 403, `$.title == "Access denied"`, `$.status == 403` |
| 8 | `clerkCannotUpdateProduct` | clerk | `PUT /api/products/999999`, `product(1L)` | 403 — the URL rule refuses before existence is checked |
| 9 | `clerkCannotDeleteCategory` | clerk | `DELETE /api/categories/999999` | 403 |
| 10 | `clerkCanRecordMovement` | clerk | admin creates category + product first; `POST /api/products/{id}/movements`, `movementIn(12)` | 201, `$.quantityAfter == 12`, `$.recordedBy.username == CLERK_USERNAME` |
| 11 | `clerkCannotListUsers` | clerk | `GET /api/admin/users` | 403, `$.title == "Access denied"` |
| 12 | `clerkSeesOwnProfileAtMe` | clerk | `GET /api/users/me` | 200, `$.username == CLERK_USERNAME`, `$.roles[0] == "CLERK"` |
| 13 | `clerkCanReadOwnProfileByUsername` | clerk | `GET /api/users/{CLERK_USERNAME}` | 200, `$.username == CLERK_USERNAME` |
| 14 | `clerkCannotReadAnotherProfile` | clerk | `GET /api/users/admin` | 403, `$.title == "Access denied"`, `$.status == 403` — method security, rendered by `GlobalExceptionHandler` |
| 15 | `adminCanListUsers` | admin | `GET /api/admin/users` | 200; `expectBody(USER_LIST)`; usernames `contains(ADMIN_USERNAME, CLERK_USERNAME)` |
| 16 | `adminCanReadAnyProfile` | admin | `GET /api/users/{CLERK_USERNAME}` | 200, `$.username == CLERK_USERNAME` |
| 17 | `adminCanWriteTheCatalogue` | admin | `createCategoryAsAdmin(category())`, then `DELETE /api/categories/{id}` | 204 |

### 7.12 Imports you will need (test sources)

The Boot 4 packages are the ones people get wrong; copy from here.

```java
// Spring Boot test
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;          // optional: @LocalServerPort int port

// Spring Framework test clients and assertions
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;                  // only for the stretch goal
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.ParameterizedTypeReference;

// HTTP
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

// JUnit 5 and AssertJ
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.assertj.core.api.Assertions.assertThat;

// Jackson 3 (the application's mapper)
import tools.jackson.databind.ObjectMapper;

// JDK
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
```

`assertThat(mvcTestResult)` is the plain AssertJ `Assertions.assertThat` — `MvcTestResult` implements
`AssertProvider<MvcTestResultAssert>`, so no extra import is needed.

---

## 8. The API Contract These Tests Rely On

Unchanged from Lab 9 §8 — this is a reminder, not a change list.

| Endpoint | Anonymous | CLERK | ADMIN | Notes |
| --- | --- | --- | --- | --- |
| `POST /api/auth/register`, `/login`, `/refresh` | ✔ | ✔ | ✔ | register → 201 CLERK; 409 on a taken username/email |
| `GET /actuator/health` | ✔ | ✔ | ✔ | `{"groups":[…],"status":"UP"}` |
| `GET /api/categories`, `/suppliers`, `/products` (+`/{id}`, `/{id}/movements`) | 401 | ✔ | ✔ | paged: `content, page, size, totalElements, totalPages, last` |
| `POST`/`PUT`/`DELETE` on categories, suppliers, products | 401 | **403** | ✔ | 201 + `Location` on create; 204 on delete; 409 `Resource in use` when referenced |
| `POST /api/products/{id}/movements` | 401 | ✔ | ✔ | 201, **no `Location`**; 409 `Insufficient stock`; `recordedBy` = the principal |
| `GET /api/users/me` | 401 | ✔ | ✔ | |
| `GET /api/users/{username}` | 401 | own only, else **403** | ✔ | method security |
| `GET /api/admin/users` | 401 | **403** | ✔ | URL rule |

Error bodies are RFC 7807 `ProblemDetail`s (`application/problem+json`) with these `title`s: `Unauthorized`
(entry point), `Authentication failed` (bad login), `Access denied` (both 403 paths), `Resource not
found`, `Duplicate value`, `Resource in use`, `Insufficient stock`, `Validation failed` (with an
`errors` map), `Malformed request`, `Invalid parameter`.

## 9. Rules the New Tests Encode

- A created product starts at `quantityOnHand = 0` and is therefore **low stock** whenever
  `reorderLevel ≥ 0`.
- Only a stock movement changes `quantityOnHand`; a `PUT` never does.
- `OUT` greater than the balance → 409 and **no change** to the balance.
- Deleting a referenced category or product → 409 and the row **survives**.
- `lowStock` means `quantityOnHand <= reorderLevel` — equality counts.
- Refusals are rendered identically whether the URL layer or method security decided them.

## 10. Testing

### 10.1 Counting convention

Every row of a §7 test table is exactly **one** `@Test` method, named in its `Test` column. Inside a row,
`;` separates the assertions or calls of that one method, and a numbered (1)…(6) sequence is one method
with several requests. The per-class totals below are what the acceptance criteria check.

### 10.2 Test inventory (6 new classes, 41 tests; 24 carried classes, 167 tests)

| Class | Base / environment | Tests | Cleans up via |
| --- | --- | --- | --- |
| `CatalogueEndToEndTest` | REST / `RANDOM_PORT` | **4** | class-level `@Sql` AFTER |
| `ValidationAndConflictTest` | MockMvc / `MOCK` | **7** | class-level `@Sql` AFTER |
| `ProductListingTest` | REST / `RANDOM_PORT` | **7** | seed BEFORE + cleanup AFTER |
| `TransactionalRollbackIsolationTest` | MockMvc / `MOCK` | **3** (ordered) | `@Transactional` rollback |
| `RealSocketCleanupIsolationTest` | REST / `RANDOM_PORT` | **3** (ordered) | method-level `@Sql` AFTER on test 2 |
| `AuthorizationMatrixTest` | REST / `RANDOM_PORT` | **17** | class-level `@Sql` AFTER |

### 10.3 The contexts you should see

Run the suite and count the Boot start-up lines:

```bash
./mvnw test 2>&1 | grep -E "Started [A-Za-z]+Test in"
```

```powershell
./mvnw test | Select-String -Pattern "Started [A-Za-z]+Test in"
```

The reference prints **9** lines: six for the eight `@WebMvcTest` classes, one for the six `@DataJpaTest`
classes, and **exactly two** for this lab — one named after whichever `AbstractRestIntegrationTest` subclass ran first (≈4 s, Tomcat
included) and one after whichever `AbstractMockMvcIntegrationTest` subclass ran first (≈0.6 s). If you
see more than two lines naming `integration` classes, a subclass carries an annotation that changed the
context key [C-§2.4].

### 10.4 Gotchas — read before you debug

1. **`@WithMockUser` → 401 on the auto-configured `MockMvcTester`.** Boot 4.0.6 adds the security chain
   to MockMvc but applies `springSecurity()` only through the separate `spring-boot-security-test`
   module, which this project does not have. Send a real Bearer token (this lab), build the tester with
   `MockMvcTester.from(context, b -> b.apply(springSecurity()).build())`, or add that module — not
   here, no new dependencies [C-§3.4].
2. **An `AFTER_TEST_METHOD` `@Sql` inside a `@Transactional` test is rolled back with the test** —
   verified. Either keep `@Transactional` off that method (§7.10) or add
   `config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)` [C-§5.4].
3. **`@Transactional` on a `RANDOM_PORT` test cleans nothing.** The server commits on its worker thread
   [C-§6.3]. That is the demonstration of §7.10, not a technique.
4. **Class-level `@Sql` runs before/after *every* method**, not once per class. That is what you want
   for the seed; it also means every listing test pays one seed + one cleanup (fast: ~0.14 s for all
   seven).
5. **Two H2 databases.** The MOCK context and the RANDOM_PORT context each have their own. A user
   registered in one does not exist in the other; a token minted in one *does* validate in the other
   (same secret, stateless).
6. **Generics need `ParameterizedTypeReference`.** `expectBody(PagedResponse.class)` would give you a
   `PagedResponse<LinkedHashMap>`.
7. **`getContentAsString()` throws a checked exception.** Use `getContentAsByteArray()` + `new String(…,
   UTF_8)`.
8. **Never add `@MockitoBean` to an integration class.** It creates a new context, and it means the
   test wanted to be a slice.
9. **Leading slash in `@Sql(scripts = "/sql/…")`.** Without it the path is resolved relative to the
   test class's package.
10. **Unique-name seeds fail if a previous test leaked rows.** If `seed-catalogue.sql` throws a
    unique-constraint violation, some real-socket test in your suite is missing its cleanup `@Sql`.
11. **The `ensureClerkExists` register call runs before every socket test.** After the first it is a
    single `existsByUsername` query plus a 409 — cheap. Do not "optimise" it into `@BeforeAll` (static;
    no `client`).
12. **`expectBody().isEmpty()`** is the 204 assertion; **`jsonPath("$.supplier").isEmpty()`** is how you
    assert a JSON `null`.

## 11. Suggested Build Order

1. Copy the module (§3.1) → `./mvnw test` = **167**.
2. Write the two SQL scripts (§7.1, §7.2) exactly.
3. Write `RequestMother` (§7.3).
4. Write `AbstractRestIntegrationTest` (§7.4) and a throwaway subclass with one test:
   `client.get().uri("/actuator/health").exchange().expectStatus().isOk()` and a login. Run it alone.
   Read the log: `Tomcat started on port …`, `jdbc:h2:mem:…`, `Seeded default ADMIN user 'admin'`.
5. Write `CatalogueEndToEndTest` (§7.6). Delete the throwaway.
6. Write `AbstractMockMvcIntegrationTest` (§7.5) and `ValidationAndConflictTest` (§7.7).
7. Write `ProductListingTest` (§7.8) — this is where a wrong column name in the seed surfaces.
8. Write `TransactionalRollbackIsolationTest` (§7.9); run it alone, then with `ValidationAndConflictTest`
   — order must not matter.
9. Write `RealSocketCleanupIsolationTest` (§7.10). Try, once, putting `@Transactional` on test 2 and
   watch test 3 fail (gotcha 2). Take it off.
10. Write `AuthorizationMatrixTest` (§7.11).
11. Full run → **208**; count the `Started …` lines → 9, two of them yours (§10.3).
12. Run the integration package on its own and confirm it is independent of the other 167:
    `./mvnw test -Dtest='com.example._10_integration_testing.integration.*Test'` → 41.

## 12. Running and Reading the Suite

```bash
./mvnw test                                                                 # everything: 208
./mvnw test -Dtest='com.example._10_integration_testing.integration.*Test'  # the new layer only: 41
./mvnw test -Dtest=AuthorizationMatrixTest                                  # one class
./mvnw test -Dtest='AuthorizationMatrixTest#healthIsPublic'                 # one method
```

The two ordered classes cannot be run one method at a time: `rowSurvivedTheRollback` alone is red by
construction (nothing created the row). Run the whole class.

Reference timings (surefire, one run, reference machine — a slower machine scales all of them): `AuthorizationMatrixTest` 5.4 s (4.1 s of it the socket context),
`TransactionalRollbackIsolationTest` 0.8 s (0.6 s the MOCK context), the other four classes 0.04–0.20 s
each; whole suite 19.0 s.

## 13. Postman — the Lab 9 Collection, Unchanged

`StockFlow_Docker.postman_collection.json` ships with this module **exactly as it left Lab 9** (26
requests, 55 assertions). Nothing in `src/main` changed, so it still runs green twice in a row against
`docker compose up -d` — do that once to prove the point (`docker compose down -v` afterwards).

The **automated equivalent** of that collection is now in your test suite. Where a Postman request lives
in code:

| Postman request(s) | Test |
| --- | --- |
| 0 health · 25 anonymous 401 | `AuthorizationMatrixTest.healthIsPublic`, `anonymousReadIs401` |
| 1 register bob (201 or 409) | `AbstractRestIntegrationTest.ensureClerkExists` |
| 2–3 login admin / bob | `AbstractRestIntegrationTest.loginAs` |
| 4–8 create category, supplier, product | `CatalogueEndToEndTest.adminCreatesCatalogueThenClerkRecordsMovement`, `updateAttachesASupplier` |
| 9–10 IN 250, OUT 30 · 13–14 read back product and ledger | `adminCreatesCatalogueThenClerkRecordsMovement` |
| 11 OUT too large → 409 · 12 quantity 0 → 400 | `ValidationAndConflictTest.outLargerThanStockIs409AndStockUnchanged`; the zero-quantity 400 has no integration equivalent (`invalidProductReportsAllFieldErrors` exercises the same validation handler; `ProductControllerTest` covers the movement fields) |
| 15 `?lowStock=true` | `ProductListingTest.lowStockFilter` |
| 16–18 bob writes → 403 | `AuthorizationMatrixTest.clerkCannotCreateCategory`, `clerkCannotUpdateProduct`, `clerkCannotDeleteCategory` (the matrix deletes a category where Postman 18 deletes a product; the URL rule is the same) |
| 19 delete a category with products → 409 | `ValidationAndConflictTest.deletingCategoryWithProductsIs409` |
| 20 delete a product with ledger history → 409 | no integration equivalent — `ProductServiceImplTest` covers the rule |
| 21 duplicate name → 409 | `ValidationAndConflictTest.duplicateCategoryNameIs409` |
| 22–24 `/api/users/bob`, `/api/users/admin`, `/api/admin/users` as bob | `AuthorizationMatrixTest` 13, 14, 11 |

The difference in kind: the collection needs a running stack and a human (or newman) to press *Run*;
the suite needs `./mvnw test`, boots its own application on its own database, and fails the build.

## 14. Acceptance Criteria

- [ ] Module copied and renamed; **no file under `src/main` differs** from your Lab 9 module.
- [ ] `./mvnw test` → **208 tests**, 0 failures, 0 errors.
- [ ] The integration package alone → **41 tests** across the six classes with the counts of §10.2.
- [ ] The start-up log shows **exactly two** `Started …Test in` lines for classes in the `integration`
      package (§10.3).
- [ ] `AbstractRestIntegrationTest` carries `@SpringBootTest(webEnvironment = RANDOM_PORT)` +
      `@AutoConfigureRestTestClient`; `AbstractMockMvcIntegrationTest` carries `@SpringBootTest` +
      `@AutoConfigureMockMvc`; **no concrete class** adds `@SpringBootTest`, `@MockitoBean`,
      `@TestPropertySource` or `@DirtiesContext`.
- [ ] Every protected request in every test carries a **real** Bearer token obtained through
      `POST /api/auth/login`; nothing in the `integration` package imports `WithMockUser` or `MockitoBean`.
- [ ] Both SQL scripts match §7.1/§7.2; `ProductListingTest` seeds before and cleans after every method.
- [ ] `TransactionalRollbackIsolationTest` is `@Transactional` at class level and has **no** `@Sql`;
      `RealSocketCleanupIsolationTest` has `@Transactional` on test 1 only and `@Sql` AFTER on test 2 only.
- [ ] `AuthorizationMatrixTest` asserts 401 with `title` `Unauthorized` for anonymous, 401
      `Authentication failed` for a bad login, and 403 `Access denied` for **both** a URL-rule refusal
      and a method-security refusal.
- [ ] `RequestMother` exists with the five methods of §7.3 and is the only place request bodies are built:
      every test that sends a request body obtains it from `RequestMother` (all classes except
      `ProductListingTest`, which only reads seeded rows); no integration class calls a request DTO's
      `builder()` directly.
- [ ] `StockFlow_Docker.postman_collection.json` is present and unchanged, and runs green against the
      Compose stack.

## 15. Optional Stretch Goals (not graded)

1. **Reproduce gotcha 2 as a test.** Write a `@Transactional` MOCK test whose `AFTER_TEST_METHOD` script
   inserts a row, and a following test that asserts the row is absent; then add
   `@SqlConfig(transactionMode = ISOLATED)` and assert it is present. Clean it up.
2. **Measure `@DirtiesContext`.** Put `@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)` on
   `AuthorizationMatrixTest`, run it, and record the time. Remove it.
3. **`@WithMockUser` the other way.** Give `ValidationAndConflictTest` its own tester built with
   `MockMvcTester.from(context, b -> b.apply(springSecurity()).build())` and replace the admin token
   with `@WithMockUser(roles = "ADMIN")`. Both must be green.
4. **Port one test to `TestRestTemplate`** (`@AutoConfigureTestRestTemplate`,
   `org.springframework.boot.resttestclient.TestRestTemplate`) to see the older API — and confirm it
   compiles with no deprecation warning.
5. **`@Nested`.** Regroup `AuthorizationMatrixTest` into `Anonymous`, `Clerk`, `Admin` nested classes and
   check the context count did not change.
6. **Parallel execution.** Enable `junit.jupiter.execution.parallel.enabled=true` in
   `junit-platform.properties` and find out which of the six classes cannot run in parallel, and why.
7. **Preview of Lab 11.** Replace H2 with a real PostgreSQL via Testcontainers and
   `@ServiceConnection`; the 41 tests should pass unchanged.

## 16. Instructor Notes

- **Grading shortcut:** `./mvnw test` → 208; `grep "Started .*Test in"` → 9 lines, 2 in `integration`;
  `grep -rl "import .*WithMockUser\|import .*MockitoBean" src/test/java/com/example/_10_integration_testing/integration`
  → nothing (the base-class Javadoc mentions `@WithMockUser` by name, so grep for the *import*); the
  shipped collection runs green against Compose.
- **The three most common failures:** (1) `@WithMockUser` on the auto-configured tester → 401 where 400
  was expected — send a token (or explain that Boot 4 put `springSecurity()` into `spring-boot-security-test`,
  which the project does not depend on); (2) `@Transactional` on the socket class "for cleanup" → test 3 of
  `RealSocketCleanupIsolationTest` fails because the AFTER script was rolled back; (3) a missing cleanup
  on some socket test → `seed-catalogue.sql` fails with a unique-constraint violation in a class that
  looks unrelated.
- **Wrong Boot 4 packages** are the second-day failure: `org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc`
  no longer exists; `org.springframework.boot.test.web.client.TestRestTemplate` no longer exists. §7.12
  is the map.
- **"TestRestTemplate is deprecated"** will be quoted at you from blog posts. It is not (the 4.0.6 jar
  has no `@Deprecated` on it); it moved to `spring-boot-resttestclient` and became opt-in. Correct the
  word to *superseded* and move on.
- **The ordered classes are teaching devices.** A student who applies `@Order` elsewhere "to make tests
  pass" has learned the wrong lesson; ask them which state leaked.
- **The two contexts are the quiet achievement.** A first attempt typically puts `@SpringBootTest` on
  every class with slightly different attributes and starts six contexts in ~25 s. The base-class rule
  brings it to two in ~7 s. Make students find their own count before telling them the reference's.
- **What comes next:** Lab 11 replaces H2 with a real PostgreSQL in Docker via Testcontainers and
  `@ServiceConnection` — the same 41 tests, a real database engine, and the Docker you set up in Lab 9
  finally used from a test.
