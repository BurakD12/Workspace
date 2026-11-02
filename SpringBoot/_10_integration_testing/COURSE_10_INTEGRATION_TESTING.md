# Course 10 — Integration Testing with `@SpringBootTest`: The Full Context, Real HTTP, and Tests That Clean Up After Themselves

**Prerequisites:** a finished StockFlow (Part 1 of `LAB_09_DOCKER_STOCKFLOW.md`) with its 167 green
tests. You already write Mockito unit tests, `@DataJpaTest` slices, `@WebMvcTest` slices with
`springSecurity()`, and the minimal `@EnableMethodSecurity` context of `MethodSecurityTest`. This course
assumes all four and builds the fifth layer on top of them.

**Stack:** Spring Boot 4.0.6 · Spring Framework 7.0.8 (`spring-test`) · Spring Security 7.0.5 · JUnit 5 ·
AssertJ · H2 in tests. Every number below (timings, context counts, statuses) is a measurement taken
from the reference `_10_integration_testing` module, not an estimate.

---

## What you'll learn

StockFlow has 167 tests and every one of them lies a little. The Mockito tests prove the service logic
against repositories that always answer what the test says. The `@DataJpaTest` slices prove the queries
against a database with no services in front of it. The `@WebMvcTest` slices prove the controllers and
the security chain against a service that is a Mockito mock. Each layer proves *its* slice — and no test
anywhere sends a real HTTP request, through the real JWT filter and the real URL rules, into a real
controller, a real service, a real transaction and a real database, and reads what comes back. The
**seams** between the layers are untested. This course is about testing the seams.

By the end you will be able to:

1. Place a test on the **pyramid** and say why it belongs there — and what it costs to put it one
   layer higher than necessary.
2. Boot the **whole application** in a test with `@SpringBootTest`, choose the right
   **`webEnvironment`**, and know exactly which `application.properties` the test sees.
3. Explain **context caching** — the one mechanism that makes a 208-test suite run in 19 seconds — and
   name the four things that silently break it.
4. Drive the API with the **Spring Boot 4 test-client stack**: `RestTestClient` against a real socket
   or MockMvc, `MockMvcTester` for AssertJ-style server-side assertions, and the honest story about
   `TestRestTemplate`.
5. **Authenticate** integration tests against a JWT-secured API with a login helper, and know why
   `@WithMockUser` does nothing on the auto-configured `MockMvcTester` in Boot 4.0.6.
6. Build test data with **builders and object mothers** so a test states only what it is about.
7. Seed and clean database state with **`@Sql`** and `executionPhase`, and avoid the trap where a
   cleanup script is silently rolled back.
8. Explain **test isolation** precisely: why `@Transactional` rollback works with `webEnvironment =
   MOCK`, why it cannot work on a real socket, and what to do instead.
9. Prove the **authorization matrix** end to end over real HTTP.

---

## 1. The test pyramid, and the gap in StockFlow's

### 1.1 The four layers you already have

| Layer | Example in StockFlow | Proves | Cannot prove |
| --- | --- | --- | --- |
| Unit (Mockito) | `ProductServiceImplTest` | mapping, id resolution, the 404/409 decisions, the stock arithmetic | anything about SQL, HTTP, security proxies |
| Persistence slice (`@DataJpaTest`) | `ProductRepositoryTest`, `StockMovementRepositoryTest` | derived queries resolve; the low-stock JPQL; entity graphs; `@PrePersist` | what the service does with the rows |
| Web slice (`@WebMvcTest` + `springSecurity()`) | `CategoryControllerTest` | status codes, JSON, `Location`, validation messages, URL-level 401/403 | anything past the controller — the service is a mock |
| Minimal method-security context | `MethodSecurityTest` | the `@PreAuthorize` rules on the real implementations | HTTP wiring |

Each of these is fast because it is small: a `@WebMvcTest` context starts in **0.3–0.4 s** on the
reference machine, and the classes that share one start it once.

### 1.2 What none of them proves

Consider four bugs, each invisible to all 167 tests:

- Someone "simplifies" `AdminUserSeeder` and stores `adminPassword` raw instead of
  `passwordEncoder.encode(adminPassword)`. Everything still compiles. `UserDetailsServiceImplTest` hands
  the service a hand-built `User`; `AuthServiceImplTest` mocks the `AuthenticationManager`;
  `AuthControllerLoginTest` mocks `AuthService`; no test runs the seeder at all. Green, green, green — and
  the seeded `admin` can no longer log in, because `BCryptPasswordEncoder.matches` rejects a stored value
  that is not a BCrypt hash. The seam is start-up seeding → `UserDetailsService` →
  `DaoAuthenticationProvider`, and only a real `POST /api/auth/login` crosses it.
- Someone swaps two `requestMatchers` lines in `SecurityConfig` so `POST /api/products/**` (ADMIN) now
  precedes `POST /api/products/*/movements` (authenticated). Clerks can no longer record stock. The
  slice tests import `SecurityConfig` and would catch this one — but only because Lab 6 made them; a
  team that tests URL rules in isolation from the real filter order gets no such warning.
- The values JPA actually returns are not the values a slice test constructs: a `BigDecimal unitPrice`
  comes back rescaled to the column's `scale = 2`, an `Instant occurredAt` truncated to the column's
  precision. The web slice serialises whatever DTO the mocked service returned, so its JSON assertions
  never see a database-shaped value.
- The `Location` header is built from `ServletUriComponentsBuilder.fromCurrentRequest()`. With a real
  server and a real port it must resolve to the resource that was actually created. No slice proves that.

The common thread: each bug lives at a **seam** — between start-up seeding and authentication,
between security and MVC, between JPA and JSON. An integration test is a test that makes the
neighbours real.

### 1.3 The cost, and the rule that follows from it

Making everything real costs start-up time. Measured on the reference run:

| Context | First start in the JVM |
| --- | --- |
| a `@WebMvcTest` slice | 0.3–0.4 s (3.5 s for the very first context, which pays JVM warm-up) |
| full application, `webEnvironment = MOCK` | 0.6 s |
| full application, `RANDOM_PORT` (embedded Tomcat) | 4.1 s |

So the pyramid is a statement about **numbers**: many narrow unit tests at the bottom, fewer slice tests,
and a *small* number of *broad* integration tests at the top — each of which earns its place by
crossing a seam that nothing below it crosses.

```
            ┌───────────────┐
            │  integration  │   41 tests · 2 contexts · the seams
            ├───────────────┤
            │    slices     │   web + persistence + method security
            ├───────────────┤
            │     unit      │   Mockito
            └───────────────┘
```

---

## 2. `@SpringBootTest`: what it boots and how

### 2.1 What the annotation does

`@SpringBootTest` on a JUnit 5 class (it carries `@ExtendWith(SpringExtension.class)`) tells the Spring
TestContext framework to build an `ApplicationContext` the way `SpringApplication.run` would: it
searches upward from the test's package for a class annotated `@SpringBootConfiguration` — your
`Application` — and boots **everything** that class would boot: component scan, every auto-configuration,
every `@ConfigurationProperties`, every `ApplicationRunner`. Compare `@WebMvcTest`, which scans only
controllers, advice and MVC infrastructure and adds one MockMvc-shaped auto-configuration set.

Nothing is mocked unless you mock it. That is the point, and it is also the cost.

### 2.2 `webEnvironment`: the four values

| Value | What you get | Use it when |
| --- | --- | --- |
| `MOCK` (default) | a `WebApplicationContext` backed by a **mock servlet environment**; no server, no port; requests are dispatched in-process through MockMvc | you want the full stack without TCP — fastest, and `@Transactional` rollback works (§6.2) |
| `RANDOM_PORT` | a real embedded server (Tomcat) on a **free port**; inject it with `@LocalServerPort` | you want real HTTP: serialisation, headers, filters, thread hand-off — the real thing |
| `DEFINED_PORT` | a real server on `server.port` (8080 by default) | almost never — a second test class or a running app on the same port fails the suite |
| `NONE` | a plain `ApplicationContext`, no web environment at all | a batch/messaging application, or a test that needs beans but no web layer |

`MOCK` and `RANDOM_PORT` are the two you will use, and — crucially for §2.4 — they are **different
contexts**: a class that needs a port cannot share with a class that does not.

### 2.3 Which `application.properties` the test sees

StockFlow's `src/test/resources/application.properties` has the **same name** as the main one, and the
test classpath comes first, so in tests it *replaces* main's file entirely. It contains only two
SQL-logging lines (`show-sql`, `format_sql`). Consequences you rely on in every `@SpringBootTest`:

- There is **no `spring.datasource.url`**, so Boot auto-configures the embedded H2 that is on the test
  classpath: `jdbc:h2:mem:<random-uuid>` — a fresh, private database **per context** (the UUID is why
  two contexts never see each other's rows; §3.5 comes back to this).
- `spring.jpa.hibernate.ddl-auto` defaults to `create-drop` for an embedded database: the schema is
  generated from the entities at start-up and dropped at shutdown.
- `app.security.jwt.*` is absent, so `JwtProperties` fall back to their code defaults (Lab 7): the same
  secret and TTLs in every context — a token minted in one context validates in another.
- `AdminUserSeeder` is an `ApplicationRunner` and runs: **`admin` / `admin12345` exists** in every
  context, committed at start-up, outside any test transaction.

To *add* properties for one test class use `@SpringBootTest(properties = "key=value")` or
`@TestPropertySource`. Each distinct set of properties is a distinct context (§2.4) — reach for them
deliberately, not casually.

### 2.4 Context caching: the mechanism that makes the suite fast

The TestContext framework keeps every context it builds in a **cache** for the life of the JVM, keyed by
the *merged context configuration*: the configuration classes, active profiles, property sources and
inlined properties, context initializers, **context customizers** (everything `@MockitoBean`,
`@AutoConfigureMockMvc`, `@AutoConfigureRestTestClient` and friends contribute), the `webEnvironment`,
and the parent context. Two test classes with an **identical** key share one context — the second class
pays nothing. Two classes that differ in *any* component get two contexts.

Surefire runs all test classes in **one JVM** by default (`forkCount=1`, `reuseForks=true`), so the cache
spans the whole `./mvnw test`. The default cache size is 32 contexts, evicted least-recently-used;
`spring.test.context.cache.maxSize` changes it.

The reference run makes the effect visible. Every context Boot starts logs one `Started <ClassName> in
N seconds` line, named after the *first* class that needed it:

```
Started AdminControllerTest in 3.531 seconds        ← 6 @WebMvcTest slices, one context each
Started AuthControllerLoginTest in 0.385 seconds
Started CategoryControllerTest in 0.417 seconds
Started ProductControllerTest in 0.401 seconds
Started SupplierControllerTest in 0.313 seconds
Started UserControllerTest in 0.32 seconds
Started EntityMappingTest in 0.186 seconds          ← the @DataJpaTest slices share one
Started AuthorizationMatrixTest in 4.117 seconds    ← RANDOM_PORT: shared by 4 classes
Started TransactionalRollbackIsolationTest in 0.595 seconds  ← MOCK: shared by 2 classes
```

Nine Boot contexts for **30** test classes: the eight `@WebMvcTest` classes share six, the six
`@DataJpaTest` classes share one, and the six new integration classes cost **two**, because each group
inherits its annotations from one abstract base class and adds nothing of its own. (The Mockito unit
tests and the minimal `MethodSecurityTest` context start no Spring Boot application, so they log no
`Started` line.) That is the design
rule: **put the `@SpringBootTest` configuration on a base class and keep subclasses annotation-free.**

Four things that silently break sharing:

1. `@MockitoBean` / `@MockitoSpyBean` in an integration test — a new customizer, a new context. If you
   need a mock, you are probably writing a slice test.
2. `properties = …`, `@TestPropertySource`, `@ActiveProfiles` that differ between classes.
3. `@DirtiesContext` — it *removes* the context from the cache after the class or method, so the next
   user rebuilds it (4 s on a socket context). Legitimate when a test has to corrupt shared state and
   cannot undo it; a smell everywhere else.
4. Mixing `MOCK` and `RANDOM_PORT` freely. Two environments are fine (the reference has exactly two);
   one per test class is not.

### 2.5 Reading the start-up log

Count the `Started … in` lines. If the number grows with the number of test classes, something on a
subclass is breaking the key. If a class you expected to share appears in the list, diff its
annotations against the base class.

---

## 3. Driving the API: the Spring Boot 4 test-client story

### 3.1 What changed in Boot 4 — stated precisely

In Spring Boot 3, `@SpringBootTest(webEnvironment = RANDOM_PORT)` gave you a `TestRestTemplate` bean
for free. In Spring Boot 4 **`@SpringBootTest` registers no HTTP client at all**; you opt in to the one
you want. Three you will meet on the servlet stack (the reactive stack's `WebTestClient` also exists
and can bind to MockMvc, but is outside this course):

| Client | Package | Opt-in | Status in 4.0.6 |
| --- | --- | --- | --- |
| **`RestTestClient`** | `org.springframework.test.web.servlet.client` (Spring Framework 7) | `@AutoConfigureRestTestClient` (`org.springframework.boot.resttestclient.autoconfigure`, module `spring-boot-resttestclient`) | the recommended client; new in Framework 7 |
| **`MockMvcTester`** | `org.springframework.test.web.servlet.assertj` (Framework 6.2+) | `@AutoConfigureMockMvc` (`org.springframework.boot.webmvc.test.autoconfigure`) registers a bean | AssertJ-native MockMvc |
| `TestRestTemplate` | `org.springframework.boot.resttestclient` (moved from `org.springframework.boot.test.web.client`) | `@AutoConfigureTestRestTemplate` | **present and not deprecated** — the 4.0.6 jar contains no `@Deprecated` on it. It is *superseded* by `RestTestClient`, and this course does not use it |

You will read "TestRestTemplate is deprecated in Boot 4" in blog posts. It is not; it moved and became
opt-in. Say "superseded" and you are correct.

Both opt-in modules are already on StockFlow's test classpath: `spring-boot-starter-webmvc-test` pulls
in `spring-boot-webmvc-test` (MockMvc, `MockMvcTester`) **and** `spring-boot-resttestclient`. Lab 10 adds
**no dependency**.

**How `@AutoConfigureRestTestClient` decides what to bind to** (from the 4.0.6 auto-configuration
source): if the context has a running local web server (`RANDOM_PORT` / `DEFINED_PORT`) the client is
`bindToServer()` with that server's base URL; otherwise, if a `MockMvc` bean exists, it is
`bindTo(mockMvc)`; otherwise `bindToApplicationContext(context)`. One client, one API, and the
environment decides whether a request crosses TCP or not.

### 3.2 `RestTestClient` anatomy

A request is a chain: method → URI → headers → body → `exchange()`. A response is a chain of
expectations, then optionally the extracted body.

```java
EntityExchangeResult<ProductResponseDto> created = client.post().uri("/api/products")
        .header(HttpHeaders.AUTHORIZATION, adminBearer())
        .contentType(MediaType.APPLICATION_JSON)
        .body(RequestMother.product(bearings.getId()).build())   // serialised by the app's Jackson
        .exchange()
        .expectStatus().isCreated()
        .expectHeader().exists(HttpHeaders.LOCATION)
        .expectBody(ProductResponseDto.class)                    // deserialised into the real DTO
        .returnResult();

ProductResponseDto product = created.getResponseBody();
URI location = created.getResponseHeaders().getLocation();
```

The pieces you will use:

- **URI:** `uri("/api/products/{id}", id)` expands templates; `uri(URI)` takes an absolute URI — which is
  how you *follow a `Location` header*, because on `RANDOM_PORT` the server produced
  `http://localhost:<port>/api/categories/7`.
- **Status:** `expectStatus().isOk() / isCreated() / isNoContent() / isBadRequest() / isUnauthorized() /
  isForbidden() / isNotFound() / isEqualTo(int)`, or `value(Consumer<Integer>)` for "201 **or** 409".
- **Headers:** `expectHeader().exists(name) / doesNotExist(name) / location(String) /
  contentTypeCompatibleWith(MediaType) / value(name, Consumer<String>)`.
- **Typed body:** `expectBody(Class)` gives a `BodySpec` with `isEqualTo`, `value(Consumer<T>)` and
  `returnResult()`. For a **generic** body you must pass a `ParameterizedTypeReference` — type erasure
  means `PagedResponse<ProductResponseDto>.class` does not exist:

  ```java
  private static final ParameterizedTypeReference<PagedResponse<ProductResponseDto>> PAGE_OF_PRODUCTS =
          new ParameterizedTypeReference<>() { };
  …
  .expectBody(PAGE_OF_PRODUCTS).returnResult().getResponseBody()
  ```

- **Untyped body:** `expectBody()` gives `jsonPath("$.title").isEqualTo("Access denied")`,
  `jsonPath("$.content").isArray()`, `jsonPath("$.supplier").isEmpty()` (matches JSON `null`),
  `.json("{…}")` for lenient whole-document comparison, and `.isEmpty()` for a 204 with no body.
- **`expectAll(...)`** groups several expectations and reports every failure, not just the first.

### 3.3 `MockMvcTester` anatomy

`MockMvcTester` is MockMvc with AssertJ instead of `andExpect(...)` matchers. Build the request the same
way; the result is an `MvcTestResult`, and `assertThat(result)` opens the assertions:

```java
MvcTestResult result = mvc.post().uri("/api/categories")
        .header(HttpHeaders.AUTHORIZATION, adminBearer())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(RequestMother.category().name("   ").build()))   // a String, not an Object
        .exchange();

assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .extractingPath("$.title").isEqualTo("Validation failed");
assertThat(result).bodyJson().extractingPath("$.errors").asMap()
        .containsEntry("name", "Name is required");
```

What it adds over `RestTestClient`: because the request never left the JVM, you can assert on the
**server side** of the exchange — `assertThat(result).handler()` (which controller method ran),
`.failure()` (the unresolved exception), `.request()` — and the JSON assertions are richer:
`extractingPath("$.x").asString() / asNumber() / asMap() / asArray() / convertTo(Class)`,
`hasPath / doesNotHavePath`, `isLenientlyEqualTo(...)`.

Two mechanical facts:

- `mvc.post()…content(String)` wants a **String**; serialise with the application's own
  `tools.jackson.databind.ObjectMapper` (Jackson 3 — `writeValueAsString` throws only unchecked
  exceptions).
- `MockHttpServletResponse.getContentAsString()` declares a checked `UnsupportedEncodingException` in
  both overloads. `new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)` does not.

**When to use which.** `RestTestClient` on `RANDOM_PORT` for anything whose *point* is the real wire:
end-to-end flows, headers, the authorization matrix. `MockMvcTester` on `MOCK` when the point is the
**server-side outcome** — a `ProblemDetail` body, a resolved exception — and a socket adds nothing but
4 seconds. Both are integration tests; the difference is a TCP hop.

### 3.4 The `@WithMockUser` gotcha, verified

In Labs 6–9 every `@WebMvcTest` built its own MockMvc with
`.apply(SecurityMockMvcConfigurers.springSecurity())`, and `@WithMockUser` worked. It is tempting to
autowire the `MockMvcTester` bean from `@AutoConfigureMockMvc` and keep using `@WithMockUser`. In Boot
4.0.6 that produces **401** for a request that should be 400, and the reason is instructive:

- Boot's `SpringBootMockMvcBuilderCustomizer` adds **every servlet `Filter` bean** in the context to
  MockMvc. The `springSecurityFilterChain` is registered as one, so the real security chain *is* in
  front of the controllers — that is why an anonymous request correctly gets 401.
- But nothing on StockFlow's classpath applies `springSecurity()`. In Boot 3 that was done by
  `spring-boot-test-autoconfigure`; Boot 4 moved it into a **separate module, `spring-boot-security-test`**
  (`SecurityMockMvcAutoConfiguration`, hooked into `@AutoConfigureMockMvc` through its own `.imports`
  file), which neither `spring-boot-starter-webmvc-test` nor `spring-boot-starter-security` brings in.
  The `spring-boot-webmvc-test` 4.0.6 jar itself contains no security MockMvc configuration, and
  `spring-security-test` registers only test-execution listeners. `springSecurity()` is what installs the
  request post-processor that copies the `@WithMockUser` principal into the request's security context.
  Without it the annotation sets a `SecurityContext` on the test thread that the chain's
  `SecurityContextHolderFilter` immediately replaces with an empty one.

Three correct ways out:

1. **Send a real token** (the reference does this): log in through `POST /api/auth/login` in-process,
   cache the access token, send `Authorization: Bearer …`. Both web environments then authenticate the
   same way, and the JWT filter is exercised on every request.
2. **Build the tester yourself**, the Lab 6 recipe in tester form:
   `MockMvcTester.from(webApplicationContext, builder -> builder.apply(springSecurity()).build())`.
   Now `@WithMockUser` works. You lose Boot's customisations (auto-added filters — `springSecurity()`
   adds the chain itself, so security still runs — and print settings), which does not matter here.
3. **Add the module.** A test-scoped `org.springframework.boot:spring-boot-security-test` dependency
   (version managed by the Boot parent) makes `@AutoConfigureMockMvc` apply `springSecurity()` itself,
   and `@WithMockUser` works on the auto-configured tester — verified on the reference module: the same
   request that answered 401 answers 400 with the field error once the module is present.

None is wrong. This lab's "no new dependencies" rule chooses 1; choose one per suite and state it in the
base class.

### 3.5 Authenticating integration tests

A real socket means a real filter chain means a real token on every protected request. The reference
puts one helper in each base class:

```java
private static final Map<String, String> ACCESS_TOKENS = new ConcurrentHashMap<>();

protected String loginAs(String username, String password) {
    return ACCESS_TOKENS.computeIfAbsent(username, u -> {
        TokenResponse tokens = client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(u, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .returnResult().getResponseBody();
        assertThat(tokens).isNotNull();
        assertThat(tokens.getTokenType()).isEqualTo("Bearer");
        return tokens.getAccessToken();
    });
}
```

Three design points hide in those lines:

- **It goes through the real endpoint.** The login *is* part of what an integration test should prove;
  minting a token with `JwtService` directly would bypass `AuthenticationManager`, BCrypt and the
  `UserDetailsService`.
- **It caches per username for the JVM.** A BCrypt verification is deliberately slow (that is what
  BCrypt is for), and the access token lives 15 minutes — longer than any suite. Caching is safe because
  the token is **stateless** and signed with the same code-default secret in every context.
- **Users are fixtures too.** `admin` is seeded at start-up; the clerk must be *registered*, and the only
  honest way is `POST /api/auth/register` — which answers 201 the first time and 409 every time after.
  The base class does this in `@BeforeEach` and accepts both. And because each context has its own H2
  (§2.3), a clerk registered by the socket suite **does not exist** in the MOCK context; the reference
  keeps the MOCK suite admin-only for that reason.

---

## 4. Test data: builders and object mothers

### 4.1 The smell

An integration test that creates a product needs a SKU, a name, a price, a reorder level and a category
id. Written inline, that is six lines per test, and every test that only cares about *one* of those
fields still shows all six. Readers cannot tell which line matters.

### 4.2 Two patterns, one class

A **builder** is a fluent way to set fields. StockFlow's request DTOs already have Lombok `@Builder`,
so `ProductRequestDto.builder().sku(…).name(…)….build()` exists. A builder does **not** know what a
sensible default is.

An **object mother** is a class that *owns* the defaults. Its methods return something already valid,
and a test overrides only its deviation from normal:

```java
public final class RequestMother {
    public static ProductRequestDto.ProductRequestDtoBuilder product(Long categoryId) {
        return ProductRequestDto.builder()
                .sku("BRG-6204")
                .name("Deep groove ball bearing 6204")
                .unitPrice(new BigDecimal("4.75"))
                .reorderLevel(5)
                .categoryId(categoryId);
    }
    …
}

RequestMother.product(categoryId).build();                                  // a valid product
RequestMother.product(categoryId).unitPrice(new BigDecimal("-1")).build();  // one thing wrong
```

The trick that makes the two patterns compose: the mother returns the **builder**, not the built
object. Defaults live in one place; each test reads as "a normal product, except…".

### 4.3 Rules of thumb

- Name methods after the *thing*, not the test: `category()`, `product(categoryId)`, `movementIn(40)`.
- Keep the mother in the test tree (`integration/support`), never in `src/main`.
- Defaults must be **valid** and **stable** — no random values without a seed, no timestamps that
  change between runs.
- Assert only what the test is about. If the test is "409 on duplicate name", do not also assert the
  description round-tripped.

---

## 5. Seeding and cleaning with `@Sql`

### 5.1 SQL versus the API for fixtures

An end-to-end story ("admin creates a category, then a product, then a clerk records a movement") should
build its data **through the API** — the calls *are* the test. A listing test that needs five products
with known prices and known stock levels should not: five POSTs plus movements to reach a specific
`quantityOnHand` is slow, verbose, and couples the paging test to the write endpoints. A SQL script
gives you exact rows, exact ids, in one statement.

### 5.2 Mechanics

```java
@Sql(scripts = "/sql/seed-catalogue.sql")                                   // BEFORE_TEST_METHOD is the default
@Sql(scripts = "/sql/cleanup-catalogue.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ProductListingTest extends AbstractRestIntegrationTest { … }
```

- `scripts` are classpath resources; the leading slash means "from the classpath root" —
  `src/test/resources/sql/seed-catalogue.sql`. `statements = "delete from …"` inlines SQL instead.
- `executionPhase`: `BEFORE_TEST_METHOD` (default), `AFTER_TEST_METHOD`, `BEFORE_TEST_CLASS`,
  `AFTER_TEST_CLASS`.
- **Class-level `@Sql` runs around every test method** of the class. Method-level `@Sql` *overrides*
  the class-level ones by default; `@SqlMergeMode(MERGE)` combines them instead.
- `@Sql` is repeatable, so a seed and a cleanup can sit side by side.
- `@SqlConfig` tunes the data source, transaction manager, statement separator, comment prefix and —
  the one that matters — `transactionMode` (§5.4).
- Column names are the snake_case forms Hibernate derives from the fields: `unitPrice` →
  `unit_price`, `quantityOnHand` → `quantity_on_hand`. The two foreign keys, `category_id` and
  `supplier_id`, are the explicit `@JoinColumn(name = …)` values on `Product`. Table names come from
  `@Table`.
- H2 accepts explicit values for `generated by default as identity` columns. The reference seeds ids
  from **9001** so they can never collide with identity-generated ids, which start at 1.

### 5.3 Cleanup discipline

Delete **children first**: `stock_movements` → `products` → `suppliers`, `categories`. Leave shared
fixtures alone — the reference cleanup never touches `users` or `refresh_tokens`, because `admin` and
`clerk.it` are shared by the whole suite. Do **not** reach for `ddl-auto=create-drop` per test or
`@DirtiesContext`: both throw away the context (§2.4).

### 5.4 The trap: a cleanup script that is rolled back

Scripts run on the test thread. Their transaction mode defaults to **inferred**: with no test-managed
transaction they run in their own transaction and **commit**; inside a `@Transactional` test they run
**inside the test's transaction** — including an `AFTER_TEST_METHOD` script, which executes *before* the
transactional listener rolls the method back. Verified on the reference module: an `AFTER_TEST_METHOD`
insert in a `@Transactional` test left **no row**; the same script with
`@SqlConfig(transactionMode = ISOLATED)` left the row **committed**.

Rule: on a `@Transactional` test, an `@Sql` that must survive needs
`config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)`. Better: do not combine
`@Transactional` with `@Sql` cleanup at all — §6 explains when each belongs.

---

## 6. Isolation: rollback versus the real socket

### 6.1 What `@Transactional` on a test does

`TransactionalTestExecutionListener` opens a transaction before the test method (early enough to cover
`@BeforeEach`), and **rolls it back** afterwards — `@Rollback(true)` is the default. Everything the
test did through a `PlatformTransactionManager`-managed resource on **that thread** is undone. The
`TestTransaction` API lets a test look at or steer this: `isActive()`, `isFlaggedForRollback()`,
`flagForCommit()`, `end()`, `start()`.

### 6.2 Why it works with `webEnvironment = MOCK`

The mock servlet dispatches the request **on the test thread**. When the controller calls
`categoryService.create(...)`, the service's `@Transactional` (propagation `REQUIRED`) asks the
transaction manager for the current transaction, finds the test-managed one already bound to the
thread, and **joins** it. One thread, one transaction. At the end of the method the listener rolls it
back, controller write included.

The reference proves it with three deliberately ordered tests:

1. `POST /api/categories` → 201, and `categoryRepository.existsByNameIgnoreCase("Rollback Probe")` is
   **true** — same thread, same transaction, the uncommitted row is visible.
2. The next test asserts the row is **gone** and the table is empty. Nothing cleaned it; the rollback
   did.
3. `admin` still exists — it was committed by the `ApplicationRunner` at start-up, outside any test
   transaction.

### 6.3 Why it cannot work on a real socket

On `RANDOM_PORT` the request leaves the test thread, crosses a TCP connection, and is handled by a
Tomcat worker thread (`http-nio-<port>-exec-N`). Spring binds transactions to threads
(`TransactionSynchronizationManager` is `ThreadLocal`-based). The service on the worker thread finds
**no** transaction, opens its own, and **commits** it when the service method returns. The test-managed
transaction on the test thread holds no work; rolling it back changes nothing.

The reference proves that too:

1. Inside a `@Transactional` test: `POST /api/categories` → 201; the repository, read from the test's
   own still-open transaction, already sees the row — because the server committed it.
2. The next (non-transactional) test asserts the row **survived** the rollback, and carries
   `@Sql(cleanup, AFTER_TEST_METHOD)`.
3. The third asserts the table is empty — the script did what rollback could not.

### 6.4 The options for real-socket cleanup

| Option | Cost | Verdict |
| --- | --- | --- |
| `@Sql(AFTER_TEST_METHOD)` cleanup script | one statement per table, per test | **default choice**; explicit and cheap |
| `@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)` | rebuilds the context — 4 s per test on a socket context | only when state cannot be undone |
| unique data per test, no cleanup | rows accumulate; every count-based assertion becomes order-dependent | fragile |
| `JdbcTemplate` deletes in `@AfterEach` | same as the script, in Java | fine; less declarative |

### 6.5 On ordered tests

Both isolation classes use `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` because the
dependency between the methods *is* the demonstration. Everywhere else, tests must be
order-independent: JUnit's default method order is deterministic but deliberately non-obvious, so a
hidden dependency shows up as a test that passes in the suite and fails alone (or the reverse).

---

## 7. The authorization matrix, end to end

### 7.1 Three layers, one path

Lab 9 tested each security layer alone: the JWT filter in `JwtAuthenticationFilterTest`, URL rules in
the controller slices with `@WithMockUser`, `@PreAuthorize` rules in `MethodSecurityTest`. An
integration matrix sends **real tokens** through the **real chain** and checks what a client would
actually see. It is also the only test that exercises the three *renderers* together:

| Refusal | Decided by | Rendered by | Status / `title` |
| --- | --- | --- | --- |
| no or bad token on a protected URL | `JwtAuthenticationFilter` sets no authentication → `AuthorizationFilter` denies → `ExceptionTranslationFilter` | `JwtAuthenticationEntryPoint` | 401 `Unauthorized` |
| wrong password at login | `AuthenticationManager` (called from `AuthServiceImpl.login`) | `GlobalExceptionHandler` | 401 `Authentication failed` |
| role forbidden by a URL rule | `AuthorizationFilter` | `RestAccessDeniedHandler` | 403 `Access denied` |
| role or ownership forbidden by `@PreAuthorize` | the method-security proxy | `GlobalExceptionHandler` (`AuthorizationDeniedException`) | 403 `Access denied` |

Two different 403 paths, one identical body — a client cannot tell which layer refused, which is the
intent. Both `title`s were asserted over real HTTP in the reference matrix.

### 7.2 The matrix as measured

| Caller | Request | Status |
| --- | --- | --- |
| anonymous | `GET /actuator/health` | 200 |
| anonymous | `POST /api/auth/register` | 201 (role `CLERK`) |
| anonymous | `POST /api/auth/login` wrong password | 401 |
| anonymous | `GET /api/products` | 401 |
| tampered token | `GET /api/products` | 401 |
| clerk | `GET /api/products` | 200 |
| clerk | `POST /api/categories` · `PUT /api/products/999999` (no such id) · `DELETE /api/categories/999999` | 403 each |
| clerk | `POST /api/products/{id}/movements` | 201, `recordedBy.username` = clerk |
| clerk | `GET /api/admin/users` | 403 |
| clerk | `GET /api/users/me` · `GET /api/users/{own}` | 200 |
| clerk | `GET /api/users/admin` | 403 (method security) |
| admin | `GET /api/admin/users` · `GET /api/users/{any}` | 200 |
| admin | `POST /api/categories` then `DELETE` | 201, 204 |

Note the row "PUT on an id that does not exist → 403": the URL rule refuses **before** the controller
runs, so existence never comes into it. That is `@PreAuthorize`-style "hide existence" behaviour, here
delivered by the URL layer.

### 7.3 Tampering correctly

A 32-byte HS256 signature base64url-encodes to 43 characters; the **last** character carries only 4
significant bits, so flipping it can decode to the identical signature and the "tampered" token stays
valid roughly one run in sixteen. Flip the **first** character of the signature segment (all 6 bits
significant). The reference helper does exactly that.

### 7.4 What the matrix does not replace

The unit and slice tests stay: they are fast, they pinpoint *which* rule failed, and they check exact
messages. The matrix is the safety net for **wiring** mistakes — a reordered matcher, a filter added in
the wrong place, an entry point that stopped producing JSON — which are precisely the mistakes no
isolated test can see.

---

## 8. Reading the evidence: what a healthy integration suite looks like

The reference numbers, from one `./mvnw test`:

| Measure | Value |
| --- | --- |
| tests | **208** (167 carried from Lab 9 + 41 new), all green |
| wall time for the whole suite | 19.0 s |
| Boot contexts started | 9, of which **2** belong to the integration package |
| `RANDOM_PORT` context start | 4.1 s, then shared by 4 classes and 31 tests |
| `MOCK` context start | 0.6 s, then shared by 2 classes and 10 tests |
| slowest integration class | `AuthorizationMatrixTest`, 5.4 s — 4.1 s of it the context |
| the other five classes | 0.04 s – 0.8 s each |

Signs of trouble:

- the `Started …` count tracks the class count — subclasses are adding annotations;
- `@MockitoBean` in a class under `integration/` — that test wants to be a slice;
- `@DirtiesContext` sprinkled as a cleanup tool;
- a test that passes alone and fails in the suite (state leaked in) or fails alone and passes in the
  suite (state depended on);
- a `@Transactional` test on a `RANDOM_PORT` class "to clean up" — it cleans nothing (§6.3) and it
  swallows any `@Sql` cleanup you add (§5.4).

---

## Checklist: you are ready to build Lab 10 when you can...

- [ ] Name a bug that every slice and unit test in StockFlow would miss, and say which seam it lives on.
- [ ] Say what `@SpringBootTest` boots, and how `MOCK`, `RANDOM_PORT`, `DEFINED_PORT` and `NONE` differ.
- [ ] Explain why StockFlow's `@SpringBootTest`s run on H2 with `admin` present although no test sets a
      datasource URL or seeds a user.
- [ ] List what goes into the context-cache key, and the four things that silently break sharing.
- [ ] State the Boot 4 test-client story precisely: what is auto-configured (nothing), how
      `RestTestClient` and `MockMvcTester` are opted into, where `TestRestTemplate` went, and whether it
      is deprecated (no).
- [ ] Say how `@AutoConfigureRestTestClient` decides between a real server and MockMvc.
- [ ] Explain why `@WithMockUser` yields 401 on the auto-configured `MockMvcTester`, and name both fixes.
- [ ] Write an object mother that returns builders, and say why it returns builders.
- [ ] Use `@Sql` with both execution phases, and explain when an `AFTER_TEST_METHOD` script is silently
      rolled back.
- [ ] Explain, in terms of threads and transaction propagation, why rollback works under `MOCK` and
      not under `RANDOM_PORT` — and what to do instead.
- [ ] Draw the authorization matrix and say which class renders each 401 and each 403.

Now build it: **`LAB_10_INTEGRATION_TESTING.md`**.
