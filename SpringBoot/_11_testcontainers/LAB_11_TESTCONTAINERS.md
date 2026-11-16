# Lab 11 — StockFlow: Real Dependencies in Tests with Testcontainers

> **Start from your completed Lab 10 project.** This lab builds on StockFlow. It is not a rebuild.
> You will copy your Lab 10 module, remove H2, and run every database test against a **real
> PostgreSQL 16** that the tests start in Docker. **Not one line of main code changes.**
>
> **Read first:** `COURSE_11_TESTCONTAINERS.md` (in this folder). This assignment tells you *what* to
> build. The course document explains *why* each mechanism behaves as it does. Section numbers below
> refer to it as **[C-§n]**.

| | |
| --- | --- |
| **Module** | `_11_testcontainers` (base package `com.example._11_testcontainers`) |
| **Starting point** | your finished Lab 10 StockFlow (`_10_integration_testing`), 208 tests green on H2. The Docker files carry over unchanged |
| **Stack** | Spring Boot 4.0.6 · Spring Framework 7.0.7 · Spring Security 7.0.5 · Java 21 · Maven · **PostgreSQL 16 in tests via Testcontainers 2.0.5** |
| **Prerequisite** | Docker Desktop (Lab 9) **installed and running** whenever you run the tests |
| **Dependencies** | **−1** (`com.h2database:h2`) · **+3**, all test scope, no versions: `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql` |
| **Deliverable** | the module below with `./mvnw test` fully green, a filled-in `LAB_11_RESULTS.md` (Part 2, §10), and the Lab 10 Postman collection shipped unchanged (§13) |
| **Reference test count** | **221** (208 carried forward from Lab 10 + 13 new, in 4 new test classes). **9** application contexts, **7** `postgres:16` container starts per run |

---

## 1. Problem Statement & Goal

StockFlow's 208 tests are green, and every one that touches a database runs on **H2**. Production
runs on **PostgreSQL 16** (the `compose.yaml` of Lab 9). They are different engines, and they disagree
in ways a test suite on H2 cannot see [C-§1]. Three examples, all measured for this lab on H2 2.4.240 in
its default mode **and** in `MODE=PostgreSQL`:

- `ORDER BY name` gives `Banana, Zebra, _widget, apple, cherry` on H2 and `apple, Banana, cherry,
  _widget, Zebra` on Postgres. A product list sorted by name is in a different order in production.
- After a failed statement inside a transaction, Postgres refuses **every** later statement in that
  transaction (`current transaction is aborted`, SQLState `25P02`). H2 just carries on. Code that
  catches a failed SQL statement (say, a duplicate insert through `JdbcTemplate`) and keeps going in
  the same transaction passes on H2 and fails in production.
- `INSERT … RETURNING` and `INSERT … ON CONFLICT DO NOTHING` are syntax errors on H2. They are
  everyday Postgres.

This lab closes that gap. You remove H2 from the build entirely. Every database test (the six
`@DataJpaTest` classes, the whole Lab 10 integration suite and the new tests) runs against a real
`postgres:16` that **Testcontainers** starts in Docker from your test code [C-§2]. Spring Boot 4's
**`@ServiceConnection`** wires the datasource to that container with no URL, user or password in any
properties file [C-§4]. One container is shared by the whole suite, so the suite stays fast [C-§5].
You then **measure** this design against the alternatives and write down what you find (§10).

## 2. Learning Objectives (what this assesses)

By the end you can:

1. Name concrete behaviours where H2 and PostgreSQL differ, and prove three of them with tests that
   pass only on real Postgres [C-§1].
2. Explain what Testcontainers does: it starts a disposable Docker container, publishes its port on a
   random host port, and removes it afterwards (Ryuk) [C-§2].
3. Use the JUnit 5 extension (`@Testcontainers` / `@Container`) and show, with container ids, that a
   `static` field means one container per class and an instance field means one per test [C-§3].
4. Wire a container to the application's datasource with `@ServiceConnection`, and prove that the
   `DataSource` really points at the container [C-§4].
5. Reproduce and fix the Boot 4 gotcha where a container declared as a `@Bean` method has the wrong
   declared return type, and explain why adding a `name` does **not** fix it [C-§4].
6. Share one container across the whole suite with a static singleton, explain why `@Testcontainers`
   must **not** go on that shared base, and measure the difference against per-class containers
   [C-§5].
7. Turn on container reuse locally, measure the dev-loop gain, and say when reuse is safe [C-§5].

## 3. Starting Point

### 3.1 Create the Lab 11 module

Copy your finished Lab 10 project and rename it. From the repository root:

1. Copy `_10_integration_testing` to a new folder `_11_testcontainers`.
2. Delete the copied `target/` directory and the Lab 10 documents (`COURSE_10_INTEGRATION_TESTING.md`,
   `LAB_10_INTEGRATION_TESTING.md`). **Keep** `Dockerfile`, `.dockerignore`, `compose.yaml` and
   `StockFlow_Docker.postman_collection.json`. They carry forward unchanged.
3. Rename the package folders: `src/main/java/com/example/_10_integration_testing` →
   `src/main/java/com/example/_11_testcontainers`, and the same under `src/test/java/com/example/`.
4. Replace every occurrence of the text `_10_integration_testing` with `_11_testcontainers` in all
   `*.java` files and in `pom.xml`. This covers the `package`/`import` statements and the pom
   `<artifactId>`/`<name>`. Nothing else contains the module name. Your IDE's rename refactoring does
   it, or, from Git Bash inside the new module:

   ```bash
   grep -rl "_10_integration_testing" --include='*.java' --include='pom.xml' . | xargs sed -i 's/_10_integration_testing/_11_testcontainers/g'
   ```

   Afterwards `grep -r _10_integration_testing .` should find nothing (ignore any hit inside `target/`,
   which you deleted in step 2). On Windows, Git Bash's `sed -i` may also turn CRLF line endings into
   LF in the files it rewrites. That is harmless: the code is the same, and the §14 check ignores
   line-ending differences.
5. Update the pom `<description>` to `Lab 11 - StockFlow: real PostgreSQL in tests with Testcontainers`.
6. **Confirm the baseline before you change the pom** (from inside the new module): `./mvnw test` →
   **208 tests, all green, still on H2.** If this is not green, fix the copy first. Every later step
   assumes a green start.

### 3.2 What this lab leaves alone

**All of `src/main`.** Every main class, `application.properties` (main), the JWT configuration and
the seeded `admin` / `admin12345` account stay byte-for-byte the same as Lab 10 (apart from the package
name). The H2-versus-Postgres demonstration is **test-only**: no repository method, no entity and no
service is added or changed.

Of the 33 existing test source files (30 test classes, the two abstract Lab 10 bases and
`RequestMother`), **25 are untouched**. Eight change **only by an `extends` clause plus its import**
(six `@DataJpaTest` classes and the two bases, §7.2). The two SQL scripts under `src/test/resources/sql/` are unchanged.

### 3.3 Prerequisite: Docker must be running

Testcontainers talks to the Docker engine you installed in Lab 9. Before every test run:

```bash
docker version          # both "Client" and "Server" sections must print; no Server = engine not running
docker image ls postgres  # postgres:16 should already be listed - compose.yaml pulled it in Lab 9
```

If `docker version` prints no `Server` section, start Docker Desktop and wait until it says it is
running. **Without Docker this build fails loudly, on purpose.** There is no "skip if Docker is
missing" switch in this project (gotcha 1 in §9.4 shows what the failure looks like).

### 3.4 Facts about the new test environment you will rely on

Know these before you start [C-§4, C-§5.5]:

- There is **no embedded database** any more. A test that needs a database gets its datasource from
  one `postgres:16` container that is started once per test run. The database name, user and password
  are Testcontainers' defaults (`test` / `test` / `test`). The JDBC URL looks like
  `jdbc:postgresql://localhost:58564/test?loggerLevel=OFF`, and the port is **random on every run**.
- **One container = one database, shared by every application context** of the run: the
  repository-test context, the MOCK integration context and the RANDOM_PORT integration context. In
  Lab 10 each context had its own H2. Lab 10's cleanup discipline (rollback, or the
  `cleanup-catalogue.sql` AFTER script) is what keeps the catalogue tables empty between classes, and
  it keeps working unchanged.
- Rows in `users` and `refresh_tokens` now **accumulate** across contexts within a run (harmless).
  `AdminUserSeeder` checks `existsByUsername` before inserting, so every context sees `admin` exactly
  once. The clerk `clerk.it` registered by the socket suite **may or may not** exist in the MOCK
  context, depending on class order. That is why the MOCK base still never relies on it.
- Hibernate still generates the schema from the entities (`ddl-auto=update`, §5.3). Lab 12 replaces
  this with Flyway.

## 4. The Big New Idea

Two ideas [C-§2, C-§4, C-§5]:

**The test starts its own real database.** A `PostgreSQLContainer` object in your test code asks
Docker to run `postgres:16`, waits until Postgres accepts connections, and tells you where it is
(host, mapped port, JDBC URL, user, password). `@ServiceConnection` on that container tells Spring
Boot: "build the datasource's connection details from this object". Boot creates a
`JdbcConnectionDetails` bean from the container, the normal Hikari pool connects to it, and the rest of
the application cannot tell the difference. No `spring.datasource.url`, no `@DynamicPropertySource`.

**Start it once, not once per class.** A container costs about a second to start. Spring's context
cache (Lab 10) already keeps each application context alive for the whole run, so the database those
contexts point at must stay alive for the whole run too. The answer is a `static final` container in
one abstract base class, started by a `static` initializer the first time any subclass is initialised (first used), and never
stopped by you. Ryuk removes it when the JVM exits. Every database test class extends that base.
Result: 221 tests, **one** shared database container, the same **9** contexts as Lab 10.

## 5. Setup Delta

### 5.1 `pom.xml`: remove H2, add three test dependencies

1. **Delete** the whole `<dependency>` block for `com.h2database:h2` (test scope).
2. **Add** these three dependencies where H2 was (next to the other test dependencies). All three
   versions come from the Spring Boot 4.0.6 parent's BOM (Testcontainers 2.0.5). **Never write a
   `<version>` tag for them**, and do not add a `<dependencyManagement>` section:

   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-testcontainers</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>testcontainers-junit-jupiter</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>testcontainers-postgresql</artifactId>
       <scope>test</scope>
   </dependency>
   ```

   What each one gives you:

   | Artifact | Gives you |
   | --- | --- |
   | `spring-boot-testcontainers` | `@ServiceConnection` and `ServiceConnectionAutoConfiguration`: a container becomes the datasource's connection details |
   | `testcontainers-junit-jupiter` | the JUnit 5 extension: `@Testcontainers`, `@Container` (and it pulls in the Testcontainers core) |
   | `testcontainers-postgresql` | the `org.testcontainers.postgresql.PostgreSQLContainer` class |

   **Testcontainers 2.x renamed every module with a `testcontainers-` prefix.** Blog posts and older
   docs (and the course roadmap) show the 1.x names `org.testcontainers:postgresql` and
   `org.testcontainers:junit-jupiter`. Boot 4's BOM does **not** manage those names, so Maven rejects
   them for having no version (gotcha 4).

3. Optionally add an XML comment above the three blocks saying why they are there. The reference pom
   has one.

The PostgreSQL JDBC driver (`org.postgresql:postgresql`, runtime scope) is already in the pom from
Lab 9. The tests use it too.

### 5.2 Prove the dependency change

```bash
./mvnw dependency:tree -Dscope=test | grep -E "testcontainers|h2"
```

```powershell
./mvnw dependency:tree "-Dscope=test" | Select-String -Pattern "testcontainers|h2"
```

You should see `spring-boot-testcontainers:4.0.6` → `testcontainers:2.0.5` (which brings
`docker-java-api` / `docker-java-transport-zerodep` 3.7.1), `testcontainers-junit-jupiter:2.0.5` and
`testcontainers-postgresql:2.0.5` → `testcontainers-jdbc` → `testcontainers-database-commons`.
**No line contains `com.h2database`.**

### 5.3 `src/test/resources/application.properties`: one new line

The test properties file keeps its two Lab 10 lines (`spring.jpa.show-sql=true`,
`spring.jpa.properties.hibernate.format_sql=true`). Add **one** line at the end, preferably with a
comment above it that explains why:

```properties
spring.jpa.hibernate.ddl-auto=update
```

**Why it is needed.** For an *embedded* database (H2) Spring Boot's default for `ddl-auto` is
`create-drop`. That is why Lab 10 never needed this line. For a *real* database the default is `none`,
so Hibernate creates no tables, and every database test fails with
`ERROR: relation "categories" does not exist` (verified). The test file **replaces** main's
`application.properties` (Course 10 §2.3), so main's own `ddl-auto` setting does not
reach the tests [C-§4.4].

**Why `update` and not `create-drop`.** All application contexts of the run share the one container.
With `create-drop`, each newly started context drops and recreates the schema (and its rows) under the
contexts already cached. This suite happens to pass with `create-drop` too (verified, 221 green), but
that is luck of class order. `update` never drops anything. Write that reason in your comment.

Do **not** add any `spring.datasource.*` line. The URL, user and password come from the container
(§7.1). That is the whole point of `@ServiceConnection`.

### 5.4 What you will see if you run the tests now

Run `./mvnw test` once after §5.1–§5.3, before writing any class. It is red, and that is expected:
the six `@DataJpaTest` classes fail with this `IllegalStateException` (verified text):

```
Failed to replace DataSource with an embedded database for tests. If you want an embedded database
please put a supported one on the classpath or tune the replace attribute of @AutoConfigureTestDatabase.
```

There is no embedded database left to replace it with, and nothing provides a service connection yet.
The integration contexts cannot get a datasource either. The pure unit tests and
the `@WebMvcTest` slices are still green. §7.1 and §7.2 fix it.

## 6. File Inventory

**NEW (5 Java files under `src/test/java/…/`, plus the report in the module root)**

```
support/AbstractPostgresContainerTest.java       ← the shared singleton container (no tests)
containers/PostgresWiringTest.java              (3 tests)  @DataJpaTest: the datasource IS the container
repository/PostgresDialectTest.java             (3 tests)  @DataJpaTest: three things H2 gets wrong
containers/ContainerLifecycleTest.java          (4 tests)  no Spring: static vs instance @Container
containers/ServiceConnectionBeanGotchaTest.java (3 tests)  ApplicationContextRunner: the @Bean gotcha
LAB_11_RESULTS.md                                          your measurement report (§10)
```

`support` and `containers` are new packages under `com.example._11_testcontainers` (test sources only).

**CHANGED**

```
pom.xml                                            h2 removed, 3 test dependencies added (§5.1)
src/test/resources/application.properties          + spring.jpa.hibernate.ddl-auto=update (§5.3)
model/EntityMappingTest.java                       + extends AbstractPostgresContainerTest (§7.2)
repository/CategoryRepositoryTest.java             + extends AbstractPostgresContainerTest
repository/ProductRepositoryTest.java              + extends AbstractPostgresContainerTest
repository/RefreshTokenRepositoryTest.java         + extends AbstractPostgresContainerTest
repository/StockMovementRepositoryTest.java        + extends AbstractPostgresContainerTest
repository/UserRepositoryTest.java                 + extends AbstractPostgresContainerTest
integration/AbstractRestIntegrationTest.java       + extends AbstractPostgresContainerTest
integration/AbstractMockMvcIntegrationTest.java    + extends AbstractPostgresContainerTest
```

**Main sources:** no change. Every class is identical to Lab 10 apart from its package name.

---

## 7. Implementation Inventory (class-by-class)

Names below are normative: the reference tests, the acceptance criteria and the instructor's grading
script use them. Build in the order of this section. Each step ends with something you can run.

### 7.1 `support/AbstractPostgresContainerTest`: NEW (the shared container)

Package `com.example._11_testcontainers.support`. A **`public abstract class`** with **no annotations
on the class at all**: no `@Testcontainers`, no `@SpringBootTest`, nothing. It contains **no tests**,
exactly one field and one static initializer [C-§4.2, C-§5.2].

**The field**

| Aspect | Spec |
| --- | --- |
| Annotation | `@ServiceConnection` (`org.springframework.boot.testcontainers.service.connection.ServiceConnection`), with no attributes |
| Modifiers | `protected static final`. **`static` is required**: Boot refuses a `@ServiceConnection` field that is not static. `protected` so subclasses (e.g. `PostgresWiringTest`) can read it |
| Type | `PostgreSQLContainer` from **`org.testcontainers.postgresql`**. This class is **not generic**: write `PostgreSQLContainer`, not `PostgreSQLContainer<?>`. If your IDE offers `org.testcontainers.containers.PostgreSQLContainer`, that is the old, `@Deprecated` 1.x class. Do not import it (gotcha 5) |
| Name | `POSTGRES` |
| Initial value | a new `PostgreSQLContainer` constructed with the image string **`"postgres:16"`** (the same image `compose.yaml` runs, so it is already pulled), with **`.withReuse(true)`** chained onto the constructor call |

**The static initializer**

A `static { … }` block, placed after the field, whose only statement calls **`start()`** on `POSTGRES`.

Why each choice matters (you will need these answers for §10):

- **`static final` + static initializer.** The JVM runs a static initializer exactly once, the first
  time the class is initialised (first used: here, when Spring reads the `@ServiceConnection` field
  while preparing the first database test class). So the container starts once per test run, *before*
  Spring builds the first context that needs it. You never call `stop()`. Testcontainers' **Ryuk** sidecar container
  removes it when the JVM exits.
- **No `@Testcontainers` / `@Container` on purpose.** The JUnit extension would stop a static container
  after *each class* (and start a new one, on a new random port, for the next class), while Spring
  keeps each context (and its Hikari pool, pointing at the old port) cached. Every later class that
  reuses a cached context would then fail with `Connection … refused` (gotcha 7) [C-§5.3]. You can
  observe this in Part 2 (§10.3).
- **`@ServiceConnection`.** Boot finds the annotated static field on any test class whose class
  hierarchy contains it, reads the field's *runtime* value, sees a JDBC database container, and
  registers a `JdbcConnectionDetails` bean from it (URL, username, password). The Hikari pool uses
  those details.
- **`withReuse(true)`** does nothing unless *you* opt in on your own machine (§10.4). Without the opt-in
  it costs one WARN line per run: `Reuse was requested but the environment does not support the reuse
  of containers`. That line is expected.

**Checkpoint:** this class compiles on its own, but nothing uses it yet.

### 7.2 Make the eight database test classes extend it: CHANGED, an `extends` clause plus an import

Every test class that needs a database must have `AbstractPostgresContainerTest` in its class
hierarchy. That is how `@ServiceConnection` reaches it. For each file below, change **only the class
declaration line** and add the import
`com.example._11_testcontainers.support.AbstractPostgresContainerTest`. **Nothing else in these
classes changes**: no annotation is added or removed, no test is edited. (You may update comments that
mention H2. That is optional.)

| File | Declaration line becomes |
| --- | --- |
| `model/EntityMappingTest.java` | `class EntityMappingTest extends AbstractPostgresContainerTest {` |
| `repository/CategoryRepositoryTest.java` | `class CategoryRepositoryTest extends AbstractPostgresContainerTest {` |
| `repository/ProductRepositoryTest.java` | `class ProductRepositoryTest extends AbstractPostgresContainerTest {` |
| `repository/RefreshTokenRepositoryTest.java` | `class RefreshTokenRepositoryTest extends AbstractPostgresContainerTest {` |
| `repository/StockMovementRepositoryTest.java` | `class StockMovementRepositoryTest extends AbstractPostgresContainerTest {` |
| `repository/UserRepositoryTest.java` | `class UserRepositoryTest extends AbstractPostgresContainerTest {` |
| `integration/AbstractRestIntegrationTest.java` | `public abstract class AbstractRestIntegrationTest extends AbstractPostgresContainerTest {` |
| `integration/AbstractMockMvcIntegrationTest.java` | `public abstract class AbstractMockMvcIntegrationTest extends AbstractPostgresContainerTest {` |

The six concrete Lab 10 integration classes (`CatalogueEndToEndTest`, `ValidationAndConflictTest`,
`ProductListingTest`, `TransactionalRollbackIsolationTest`, `RealSocketCleanupIsolationTest`,
`AuthorizationMatrixTest`) are **not** edited. They inherit the container through their Lab 10 base.

Do **not** add `@AutoConfigureTestDatabase(replace = NONE)` to the `@DataJpaTest` classes, even though
many Boot 3 tutorials do. In Boot 4 its default, `replace = NON_TEST`, already leaves a datasource that
comes from a service connection alone [C-§4.3]. Adding it to some classes but not others would also change the
context cache key and start extra contexts.

**Checkpoint (do not skip):** `./mvnw test` → **208 tests, all green, now on PostgreSQL.** In the log
you should see `Container postgres:16 is starting: …`, `Container postgres:16 started in PT…S` and
`Container is started (JDBC URL: jdbc:postgresql://localhost:…/test?loggerLevel=OFF)` **once**, near
the start of the first database class. You will also see harmless INFO lines of the form
`Could not detect default configuration classes for test class [...]`, many of them: one or more for
almost every Spring test class (they were already there in Lab 10), and now also some naming
`AbstractPostgresContainerTest` (gotcha 12). On your very first run Testcontainers also pulls the Ryuk image (gotcha 10).

### 7.3 `containers/PostgresWiringTest`: NEW (3 tests)

Proof that `@ServiceConnection` did the wiring, with no URL anywhere in the test configuration [C-§4.2].

**Class:** package-private `class PostgresWiringTest extends AbstractPostgresContainerTest`, annotated
`@DataJpaTest` (`org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`) and
`@DisplayName("@ServiceConnection wiring (the datasource IS the container)")`. Nothing else on the
class. With the same annotations as the six repository tests and no extra configuration, it **shares
one context with them**. Whichever of these classes runs first creates that context, and all the
others reuse it. In a full run that is usually this class (surefire runs `containers.*` first), so you
will see `Started PostgresWiringTest in …`. That is expected: the class adds no *extra* context.

**Fields** (all `private`, all `@Autowired`):

| Field | Type |
| --- | --- |
| `dataSource` | `javax.sql.DataSource` |
| `connectionDetails` | `org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails` |
| `jdbcTemplate` | `org.springframework.jdbc.core.JdbcTemplate` (`@DataJpaTest` provides one) |

**Tests.** `POSTGRES` is the inherited container field.

| Test | Arrange / act | Assert |
| --- | --- | --- |
| `connectionDetailsComeFromTheContainer` | read `getJdbcUrl()`, `getUsername()`, `getPassword()` from `connectionDetails` | each `isEqualTo` the same getter on `POSTGRES` (three assertions) |
| `dataSourceIsNotReplaced` | none | `dataSource` `isInstanceOf(com.zaxxer.hikari.HikariDataSource.class)`; cast it to `HikariDataSource`, and its `getJdbcUrl()` `isEqualTo(POSTGRES.getJdbcUrl())`. This proves `@DataJpaTest` did **not** swap in an embedded database |
| `serverIsPostgres16` | `jdbcTemplate.queryForObject("select version()", String.class)` | the string `startsWith("PostgreSQL 16.")` |

Method `@DisplayName`s are free text. The reference numbers them `"1. …"`, `"2. …"`, `"3. …"`.

**Run it alone:** `./mvnw test -Dtest=PostgresWiringTest` → 3 green.

### 7.4 `repository/PostgresDialectTest`: NEW (3 tests)

Three behaviours where PostgreSQL 16 and H2 disagree [C-§1.2–§1.4]. Every test passes on Postgres, and each
one would fail on H2 (default mode and `MODE=PostgreSQL`, both verified). No main code is needed: test 1
goes through the existing `ProductRepository`, tests 2 and 3 use plain SQL through `JdbcTemplate`.

**Class:** package-private `class PostgresDialectTest extends AbstractPostgresContainerTest` in package
`…repository`, annotated `@DataJpaTest` and `@DisplayName("Postgres vs H2: behaviours H2 gets wrong")`.
It shares the repository-test context.

**Fields** (all `private`, all `@Autowired`): `ProductRepository productRepository`,
`TestEntityManager em` (`org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`),
`JdbcTemplate jdbcTemplate`.

**One fact you rely on:** `@DataJpaTest` is `@Transactional`, and `JdbcTemplate` uses the same
`DataSource`, so its statements run **inside the test's transaction** on the same connection as
`em`. Everything every test writes is rolled back at the end. That is also why test 2 can break its own
transaction without harming any other test.

**Schema facts used:** `categories.name` is unique (length 100), and `products.sku` is unique (length
40). `Product.builder()` has `sku`, `name`, `unitPrice`, `reorderLevel`, `category`. `quantityOnHand` is
not set and defaults to 0.

| Test | Arrange | Act | Assert |
| --- | --- | --- | --- |
| `sortByNameUsesTheCollation` | `em.persist` a `Category` built with name `Collation` (keep the returned entity as `c`). Then, for each name in this exact order: `Zebra`, `apple`, `_widget`, `cherry`, `Banana`, `em.persist` a `Product` with sku `"COLL-" + name.toUpperCase()`, that name, unitPrice `new BigDecimal("1.00")`, reorderLevel `0`, category `c`. Then `em.flush()` and `em.clear()` | `productRepository.findAll(PageRequest.of(0, 10, Sort.by("name")))`, then `.map(Product::getName).getContent()` | the list `containsExactly("apple", "Banana", "cherry", "_widget", "Zebra")`. Add a comment: H2 gives `[Banana, Zebra, _widget, apple, cherry]` (it compares code points; Postgres uses the database collation `en_US.utf8`) |
| `failedStatementAbortsTheTransaction` | `em.persist` a `Category` named `Fasteners`; `em.flush()` | **(a)** `jdbcTemplate.update("insert into categories (name) values ('Fasteners')")` (a duplicate). **(b)** then `jdbcTemplate.queryForObject("select count(*) from categories", Long.class)` | (a) `assertThatThrownBy(…)` `isInstanceOf(DataIntegrityViolationException.class)`. (b) `assertThatThrownBy(…)` `.isInstanceOfSatisfying(UncategorizedSQLException.class, e -> …)` where the lambda asserts `e.getSQLException().getSQLState()` `isEqualTo("25P02")`, chained with `.hasMessageContaining("current transaction is aborted")`. Even a plain `SELECT` is refused after the failure. Spring has no specific exception for 25P02, so it arrives as `UncategorizedSQLException`. **Use `getSQLException()`, not `rootCause()`** (gotcha 6) |
| `postgresOnlySql` | none | **(a)** `jdbcTemplate.queryForObject("insert into categories (name) values ('Tools') returning id", Long.class)`. **(b)** `jdbcTemplate.update("insert into categories (name) values ('Tools') on conflict (name) do nothing")`. **(c)** `jdbcTemplate.queryForObject("select count(*) from categories where name = 'Tools'", Long.class)` | (a) the returned id `isNotNull()`. (b) the returned row count `isZero()` (the conflict was skipped silently, with no exception). (c) `isEqualTo(1L)`. Both statements are syntax errors on H2 2.4.240 |

Why StockFlow's own services never hit test 2's trap: they **pre-check** (`existsBy…`) and throw
`DuplicateResourceException` / `ResourceInUseException` before any SQL can fail. They never catch a
`DataIntegrityViolationException` and carry on. And Lab 10's `ProductListingTest` never sorts by name
(only by SKU, all of the form `SKU-nnn`, and by `unitPrice`), so it never saw the collation difference. These tests make both risks visible.

**Run it alone:** `./mvnw test -Dtest=PostgresDialectTest` → 3 green.

### 7.5 `containers/ContainerLifecycleTest`: NEW (4 tests, no Spring)

Testcontainers on its own, to see the JUnit extension's lifecycle rules with your own eyes [C-§3.4, C-§3.5]. **No
Spring annotation anywhere in this class, and it does not extend the base.**

**Class:** package-private `class ContainerLifecycleTest`, annotated:

- `@Testcontainers` (`org.testcontainers.junit.jupiter.Testcontainers`): registers the extension;
- `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`: tests 3 and 4 depend on order **by design**;
- `@DisplayName("Container lifecycle (@Testcontainers / @Container, no Spring)")`.

**Fields**

| Field | Modifiers / annotation | Type | Value | Lifecycle the extension gives it |
| --- | --- | --- | --- | --- |
| `PER_CLASS` | `@Container`, `private static final` | `PostgreSQLContainer` (new package) | `new PostgreSQLContainer("postgres:16")` | started once before the first test, stopped after the last: **one per class** |
| `perTest` | `@Container`, `private final` (instance) | `PostgreSQLContainer` | `new PostgreSQLContainer("postgres:16")` | started before **each** test, stopped after it: **one per test** (JUnit creates a new instance of the class per test method) |
| `perClassIdSeenByTest3` | `private static` | `String` | unset | written by test 3, read by test 4 |
| `perTestIdSeenByTest3` | `private static` | `String` | unset | written by test 3, read by test 4 |

The two `String` fields must be `static`: test 3 and test 4 run on **different instances** of the class,
so an instance field written by test 3 would be invisible to test 4.

**Tests**

| `@Order` | Test | Arrange / act | Assert |
| --- | --- | --- | --- |
| 1 | `startedOnARandomHostPort` | `int hostPort = PER_CLASS.getMappedPort(5432)` | `PER_CLASS.isRunning()` is true; `hostPort` `isNotEqualTo(5432)` (Postgres listens on 5432 *inside* the container, and Docker publishes it on a random free host port); `PER_CLASS.getJdbcUrl()` `startsWith("jdbc:postgresql://")` and `contains(PER_CLASS.getHost() + ":" + hostPort)` |
| 2 | `itIsRealPostgres` | declare `throws Exception`. Open a `java.sql.Connection` with `DriverManager.getConnection(PER_CLASS.getJdbcUrl(), PER_CLASS.getUsername(), PER_CLASS.getPassword())`, create a `Statement`, and execute `select version()` into a `ResultSet`, all three in **one try-with-resources** | `rs.next()` is true; `rs.getString(1)` `startsWith("PostgreSQL 16.")`. Plain JDBC, no Spring: the container is an ordinary database |
| 3 | `recordIds` | store `PER_CLASS.getContainerId()` into `perClassIdSeenByTest3` and `perTest.getContainerId()` into `perTestIdSeenByTest3` | `perTest.isRunning()` is true; `perTestIdSeenByTest3` `isNotEqualTo(perClassIdSeenByTest3)` (two different containers are running) |
| 4 | `staticIsSharedInstanceIsFresh` | none | `perClassIdSeenByTest3` `.as("test 3 must run first")` `isNotNull()`; `PER_CLASS.getContainerId()` `isEqualTo(perClassIdSeenByTest3)` (**same** static container); `perTest.getContainerId()` `isNotEqualTo(perTestIdSeenByTest3)` (a **new** instance container) |

This class starts **5** containers: 1 for the static field + 1 per test for the instance field. At
about 1 s each, the class takes ~8.5–9 s on the reference machine. That cost is exactly why the shared
base exists.

**Run it alone:** `./mvnw test -Dtest=ContainerLifecycleTest` → 4 green, and
`grep -c "Container postgres:16 started"` on its output → 5.

### 7.6 `containers/ServiceConnectionBeanGotchaTest`: NEW (3 tests, graded gotcha)

You can also declare a container as a **`@Bean` method** instead of a field. Then Spring Boot decides
whether the container can provide database connection details from the method's **declared return
type**, which it reads from the bean definition *before the bean is created*. It does not look at the
object the method actually returns [C-§4.5]. On StockFlow's classpath the only factory that turns a
container into a datasource connection is the JDBC one, and it accepts only `JdbcDatabaseContainer`
types. Declare the method as returning `GenericContainer<?>` and nothing matches. The context refuses to
start with `ConnectionDetailsNotFoundException`, whose message suggests adding a `name`. **For a JDBC
database that hint is a red herring**: a name never rescues a type mismatch. The fix is the precise
return type.

This class proves all three facts. It uses an **`ApplicationContextRunner`**: a tiny throw-away
context built per test from exactly the configuration you hand it. A deliberately broken context is
then something you can assert on, and it never pollutes the context cache the rest of the suite
shares. (With `@SpringBootTest`, a broken configuration would simply fail the class.)

**Class:** package-private `class ServiceConnectionBeanGotchaTest`, annotated only with
`@DisplayName("@ServiceConnection on a @Bean: declared type decides (Boot 4 gotcha)")`. No
`@SpringBootTest`, no `@Testcontainers`, and it does **not** extend the base.

**Field**

| Field | Spec |
| --- | --- |
| `runner` | `private final ApplicationContextRunner` (`org.springframework.boot.test.context.runner`), initialised to a `new ApplicationContextRunner()` on which you chain `.withConfiguration(AutoConfigurations.of(ServiceConnectionAutoConfiguration.class))`. `AutoConfigurations` is `org.springframework.boot.autoconfigure.AutoConfigurations`, and `ServiceConnectionAutoConfiguration` is in the same package as `@ServiceConnection`. This one auto-configuration is what turns `@ServiceConnection` beans into connection details |

**Three static nested configuration classes.** Each is a `static class` (package-private) annotated
`@Configuration(proxyBeanMethods = false)`, containing **one** method named `postgres()`, annotated
`@Bean` and a `@ServiceConnection` variant, whose body returns `new PostgreSQLContainer("postgres:16")`.
They differ only in the declared return type and the annotation:

| Nested class | Method annotations | Declared return type of `postgres()` |
| --- | --- | --- |
| `DeclaredAsGenericContainer` | `@Bean` + `@ServiceConnection` | `GenericContainer<?>` (`org.testcontainers.containers.GenericContainer`) |
| `DeclaredAsGenericContainerWithName` | `@Bean` + `@ServiceConnection(name = "postgres")` | `GenericContainer<?>` |
| `DeclaredAsPostgreSQLContainer` | `@Bean` + `@ServiceConnection` | `PostgreSQLContainer` (`org.testcontainers.postgresql`) |

**Tests.** Each test calls `runner.withUserConfiguration(<NestedClass>.class).run(context -> { … })`
and makes its assertions inside the lambda. `context` is an `AssertableApplicationContext`, so plain
`assertThat(context)` gives you `hasFailed()`, `hasNotFailed()` and `hasSingleBean(…)`.

| Test | User configuration | Assertions inside `run(context -> …)` |
| --- | --- | --- |
| `genericReturnTypeFails` | `DeclaredAsGenericContainer.class` | `assertThat(context).hasFailed()`; `assertThat(context.getStartupFailure())` `.isInstanceOf(ConnectionDetailsNotFoundException.class)` `.hasMessageContaining("No ConnectionDetails found")` `.hasMessageContaining("You may need to add a 'name' to your @ServiceConnection annotation")` |
| `aNameDoesNotHelp` | `DeclaredAsGenericContainerWithName.class` | `hasFailed()`; startup failure `isInstanceOf(ConnectionDetailsNotFoundException.class)` and `hasMessageContaining("No ConnectionDetails found")`. (With a name set, the hint sentence is gone from the message, but the failure is the same) |
| `preciseReturnTypeWorks` | `DeclaredAsPostgreSQLContainer.class` | `assertThat(context).hasNotFailed()`; `assertThat(context).hasSingleBean(JdbcConnectionDetails.class)`; get the container with `context.getBean(PostgreSQLContainer.class)` and the details with `context.getBean(JdbcConnectionDetails.class)`; `details.getJdbcUrl()` `isEqualTo(container.getJdbcUrl())`; `details.getUsername()` `isEqualTo(container.getUsername())` |

`ConnectionDetailsNotFoundException` is
`org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsNotFoundException`. Assert
on the **startup failure itself**, not on a cause or root cause: it is the top-level exception and it
has no cause (verified. A first draft that asserted `.rootCause()` failed with "Expecting actual
throwable to have a root cause but it did not").

In tests 1 and 2 the container is **never created**, because the failure happens while bean
definitions are registered. Only test 3 starts a container, and it is stopped when the runner's context
closes at the end of `run(…)`. Runner contexts print **no** `Started … in` line.

Compare this with §7.1: for a **field**, Boot reads the field's *value* and uses its runtime class.
That is why a field works even if declared loosely, while a `@Bean` method must declare the precise
type.

### 7.7 Imports you will need (new test sources)

The Testcontainers 2 and Boot 4 packages are the ones people get wrong. Copy from here.

```java
// Testcontainers 2.x
import org.testcontainers.postgresql.PostgreSQLContainer;          // NOT org.testcontainers.containers.PostgreSQLContainer (deprecated)
import org.testcontainers.containers.GenericContainer;             // only in ServiceConnectionBeanGotchaTest
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Spring Boot: service connections
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsNotFoundException;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

// Spring Boot: test support
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.beans.factory.annotation.Autowired;

// Spring Framework
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

// JUnit 5 and AssertJ
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Pool, JDBC, JDK
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.List;

// Your own classes
import com.example._11_testcontainers.support.AbstractPostgresContainerTest;
import com.example._11_testcontainers.model.Category;
import com.example._11_testcontainers.model.Product;
```

---

## 8. Facts the New Tests Pin Down

Each row is one assertion you will make. It is the "rules" section of this lab:

| Fact | Proved by |
| --- | --- |
| The application's `DataSource` is a Hikari pool on the container's JDBC URL. Nothing was replaced by an embedded database | `PostgresWiringTest` |
| The database the application talks to is PostgreSQL 16 | `PostgresWiringTest.serverIsPostgres16`, `ContainerLifecycleTest.itIsRealPostgres` |
| Postgres sorts text by collation (`apple, Banana, cherry, _widget, Zebra`), not by code point | `PostgresDialectTest.sortByNameUsesTheCollation` |
| After one failed statement, the whole Postgres transaction is unusable (25P02) until rollback | `PostgresDialectTest.failedStatementAbortsTheTransaction` |
| `RETURNING` and `ON CONFLICT DO NOTHING` work | `PostgresDialectTest.postgresOnlySql` |
| Container port 5432 is published on a random host port | `ContainerLifecycleTest.startedOnARandomHostPort` |
| `static @Container` = one per class; instance `@Container` = one per test | `ContainerLifecycleTest` tests 3–4 |
| For a `@Bean` container, the declared return type decides; a `name` does not help | `ServiceConnectionBeanGotchaTest` |

## 9. Testing

### 9.1 Counting convention

Every row of a §7 test table is exactly **one** `@Test` method, named in its `Test` column. Inside a
row, `;` separates the assertions or calls of that one method, and (a)/(b)/(c) are several calls in one
method. `AbstractPostgresContainerTest` contains no tests and is not counted.

### 9.2 Test inventory (4 new classes, 13 tests; 30 carried classes, 208 tests)

| Class | Kind | Tests | Extends the base? | Containers it starts |
| --- | --- | --- | --- | --- |
| `containers.PostgresWiringTest` | `@DataJpaTest` | **3** | yes | none (uses the shared one) |
| `repository.PostgresDialectTest` | `@DataJpaTest` | **3** | yes | none (uses the shared one) |
| `containers.ContainerLifecycleTest` | plain JUnit + `@Testcontainers` | **4** (ordered) | **no** | 5 |
| `containers.ServiceConnectionBeanGotchaTest` | `ApplicationContextRunner` | **3** | **no** | 1 (test 3 only) |

Total: **221 = 208 carried + 13 new (3 + 3 + 4 + 3)**, 0 skipped.

### 9.3 The contexts and containers you should see

Save the output of a full run, then count two kinds of line:

```bash
./mvnw test > target/run.log 2>&1
grep -cE "Started [A-Za-z]+Test in" target/run.log      # application contexts  -> 9
grep -c "Container postgres:16 started" target/run.log  # postgres container starts -> 7
```

```powershell
./mvnw test *> target/run.log
(Select-String -Path target/run.log -Pattern "Started [A-Za-z]+Test in").Count       # -> 9
(Select-String -Path target/run.log -Pattern "Container postgres:16 started").Count  # -> 7
```

**9 contexts**, the same as Lab 10: one `@DataJpaTest` context shared by all eight `@DataJpaTest`
classes (named after whichever ran first, e.g. `Started PostgresWiringTest in …`), six `@WebMvcTest`
contexts, and the two integration contexts (`RANDOM_PORT` and `MOCK`). The two new data-JPA classes add
**no** context. `ServiceConnectionBeanGotchaTest`'s runner contexts print no `Started` line.

**7 container starts**: 1 shared singleton + 5 from `ContainerLifecycleTest` (1 static + 4 instance) +
1 from `ServiceConnectionBeanGotchaTest` test 3. (Testcontainers also starts one Ryuk container. It is
not a `postgres:16` line.) If you see more than 7, some database class has its own container. If you see
more than 9 contexts, some class changed the context key (an extra annotation such as
`@AutoConfigureTestDatabase`).

### 9.4 Gotchas: read before you debug

1. **Docker is not running → everything that touches a database fails.** Testcontainers throws
   `IllegalStateException: Could not find a valid Docker environment. Please see logs and check
   configuration`. Because the container starts in a **static initializer**, the first class that
   loads `AbstractPostgresContainerTest` fails with `java.lang.ExceptionInInitializerError`, and every
   later subclass fails with `java.lang.NoClassDefFoundError: Could not initialize class
   com.example._11_testcontainers.support.AbstractPostgresContainerTest` (standard JVM behaviour: a
   class whose initializer failed is never initialised again in that JVM). You may also see
   `Previous attempts to find a Docker environment failed. Will not retry.`
   `ContainerLifecycleTest` fails inside the extension. Unit tests and `@WebMvcTest` slices still pass.
   **The `NoClassDefFoundError` is not a classpath problem.** Scroll up to the first error. Fix: start
   Docker Desktop, check `docker version`, rerun [C-§2.5].
2. **Forgot `ddl-auto` → `ERROR: relation "categories" does not exist`.** For a non-embedded database
   Boot's default is `none`. Add the line of §5.3 to the **test** properties.
3. **`IllegalStateException: Failed to replace DataSource with an embedded database for tests. If you
   want an embedded database please put a supported one on the classpath or tune the replace attribute
   of @AutoConfigureTestDatabase.`** You removed H2, but this `@DataJpaTest` class does **not** extend
   `AbstractPostgresContainerTest`. With no service connection in its hierarchy, `@DataJpaTest` tries to
   replace the datasource with an embedded one, and there is none on the classpath. Fix: add the
   `extends` (§7.2). Do not "fix" it by adding H2 back, and do not follow the message's hint to add
   `@AutoConfigureTestDatabase(replace = NONE)`. That only moves the failure, because there is still no
   database URL anywhere. The class then fails with
   `DataSourceProperties$DataSourceBeanCreationException: Failed to determine a suitable driver class`
   instead (verified).
4. **Old artifact names.** `org.testcontainers:postgresql` / `org.testcontainers:junit-jupiter` (1.x)
   are not managed by Boot 4's BOM, so Maven rejects the pom because the version is missing. Do not
   "fix" it by adding a version: use the 2.x names `testcontainers-postgresql` /
   `testcontainers-junit-jupiter` (§5.1).
5. **The deprecated generic `PostgreSQLContainer`.** `org.testcontainers.containers.PostgreSQLContainer<SELF>`
   still ships in 2.0.5 and compiles, with deprecation warnings and a `<?>` everywhere. IDE
   auto-import often picks it. Use `org.testcontainers.postgresql.PostgreSQLContainer` (not generic).
   Check the import line of every new class.
6. **Asserting `.rootCause()` in `failedStatementAbortsTheTransaction` gives `23505`, not `25P02`.** The
   verified chain is: `UncategorizedSQLException` → `PSQLException` (25P02, "current transaction is
   aborted") → *its* cause, the earlier `PSQLException` (23505, `duplicate key value violates unique
   constraint …`). The root cause is the original duplicate, so the assertion fails with "expected
   25P02 but was 23505". Use `e.getSQLException().getSQLState()` (§7.4).
7. **`@Testcontainers` / `@Container` on the shared base → `Connection … refused` and 30-second
   hangs.** The extension stops the static container after every class and starts it again, on a
   **new** random port, before the next class. Spring, meanwhile, caches each context (and its pool,
   pointing at the port of the container that was running when the context was built). The suite has
   three database context keys (`@DataJpaTest`, `RANDOM_PORT`, `MOCK`; the `@WebMvcTest` classes never
   touch the database). The **first** class of each key builds a fresh context against the current
   container and passes. **Every later class that reuses an already-cached context** points at a
   stopped container's port: each database access waits Hikari's 30 s connection timeout and fails with
   `org.postgresql.util.PSQLException: Connection to localhost:NNNNN refused. Check that the hostname
   and port are correct and that the postmaster is accepting TCP/IP connections.` So a few classes go
   green and then the run stalls. It does not finish in any useful time. **Press Ctrl+C** and remove the
   annotations (§7.1). Part 2 lets you see it once on purpose (§10.3).
8. **`@ServiceConnection` on a non-static field.** Boot refuses it as the context starts, with
   `java.lang.IllegalStateException: @ServiceConnection field 'POSTGRES' must be static` (verified
   with a field named `pg`, which gave `field 'pg' must be static`: the quotes hold your field's name). Note: in *this* base class you would see a compile error first
   (`non-static variable POSTGRES cannot be referenced from a static context`), because the `static` block calls
   `POSTGRES.start()`; the runtime message appears for a non-static field that compiles. Make the field `static` (§7.1).
9. **In `ServiceConnectionBeanGotchaTest`, neither the method name nor a `@ServiceConnection` name
   matters. The declared return type does.** If `preciseReturnTypeWorks` fails, check that its
   `postgres()` method declares `PostgreSQLContainer` from the **new** package.
10. **The first run is slower and pulls an image.** Testcontainers starts its Ryuk sidecar
    (`testcontainers/ryuk:0.14.0`) and pulls it on first use. `postgres:16` is already local from Lab 9
    (if you deleted it, it is pulled too). Later runs reuse both images.
11. **`ApplicationContextRunner` contexts print no `Started … in` line.** Do not count
    `ServiceConnectionBeanGotchaTest` in your context count.
12. **`Could not detect default configuration classes for test class [...]`** at INFO level is benign,
    and you will see it many times: for almost every Spring test class (it was already in Lab 10's
    log), and now also several times for `AbstractPostgresContainerTest`. Spring's test framework looks
    at each class of the hierarchy for a nested configuration class, finds none, and falls back to
    `@SpringBootConfiguration` detection. Ignore it.
13. **`Reuse was requested but the environment does not support the reuse of containers`** (WARN) on
    every normal run is expected: it comes from `withReuse(true)` without your local opt-in (§10.4).
14. **Shared database, shared rows.** Never write a test that depends on the clerk `clerk.it` existing
    in the MOCK context, or on `users` having an exact row count. Both depend on class order now.

## 10. Part 2: Measurements and Reuse (graded via `LAB_11_RESULTS.md`)

The shared singleton is a design choice, so you will measure it. You will run the suite in three
configurations, record what you see, and answer a few questions in a report file `LAB_11_RESULTS.md` in
the module root (template in §10.6). **No test checks this part.** An acceptance checkbox does.

Rules for measuring:

- **After any code change** (switching the base class for B or C, reverting it), run
  `./mvnw test-compile` once so that compilation is not part of a measured run. **Then do two measured
  runs and record both numbers.** With no code change since the last build, just do the two measured
  runs.
- Take **total time** from Maven's last lines (`Total time:  40.8 s` or `Total time:  01:01 min`).
- Count **contexts** and **container starts** exactly as in §9.3.
- **Timings scale with your machine; counts do not.** Your seconds may be very different from the
  reference numbers below. Your context and `postgres:16 started` counts must match §10.5 exactly,
  in every row (including the reuse rows: a reused container still logs its `started` line, §10.4).

Helper commands for one measured run (replace `NAME` with `singleton`, `perclass`, …):

```bash
./mvnw test > target/NAME.log 2>&1
grep -E "Tests run:.*Fail|Total time" target/NAME.log | tail -2
grep -cE "Started [A-Za-z]+Test in" target/NAME.log
grep -c "Container postgres:16 started" target/NAME.log
```

```powershell
./mvnw test *> target/NAME.log
Select-String -Path target/NAME.log -Pattern "Tests run:.*Fail|Total time" | Select-Object -Last 2
(Select-String -Path target/NAME.log -Pattern "Started [A-Za-z]+Test in").Count
(Select-String -Path target/NAME.log -Pattern "Container postgres:16 started").Count
```

(The logs live in `target/`, so they are never committed and `./mvnw clean` deletes them.)

### 10.1 Measurement A: the shipped singleton

With your finished code, run the helper commands twice. Record: tests green, total time, contexts,
container starts. Expected counts: **221 / 9 / 7**.

### 10.2 Measurement B: per-class containers, done right

Temporarily switch `AbstractPostgresContainerTest` to the textbook "one container per class" style,
**done correctly** [C-§5.4]. Make exactly these four changes:

| Change | What to do |
| --- | --- |
| 1 | Annotate the **class** with `@Testcontainers` |
| 2 | Annotate the **field** with `@Container` **in addition to** `@ServiceConnection` (keep it `protected static final`) |
| 3 | **Remove** the static initializer block **and** the `.withReuse(true)` call. The extension now starts and stops the container |
| 4 | Annotate the **class** with `@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)` (`org.springframework.test.annotation.DirtiesContext`). Every context built for a database class is closed after that class, so the next class builds a fresh context against **its** fresh container |

Run the measurement twice and record the same four values. Expected counts: **221 green, 20 contexts,
20 container starts**. Every database class (8 `@DataJpaTest` + 6 integration = 14) now gets its own
context and its own container, plus the 6 `@WebMvcTest` contexts (14 + 6 = 20 contexts), plus the 6
containers of the two classes that do not extend the base (5 from `ContainerLifecycleTest`, 1 from
`ServiceConnectionBeanGotchaTest`; 14 + 6 = 20 containers).

**Then revert all four changes.** Run the suite once more and confirm **221 / 9 / 7**. Check with
`git diff` (or by eye against §7.1) that the base class is back to the shipped form.

### 10.3 Measurement C (optional, 2 minutes): per-class done wrong

Optional, but it makes gotcha 7 real. Apply changes 1–3 of §10.2 but **not** change 4 (no
`@DirtiesContext`). Run `./mvnw test` and watch. The extension now restarts the container, on a new
port, before every class. The **first** class of each database context (`@DataJpaTest`, `RANDOM_PORT`,
`MOCK`) builds a fresh context against the current container and passes. On the reference machine,
for example, `AuthorizationMatrixTest`, the first `RANDOM_PORT` class, went 17/17 green. But the
**next class that reuses an already-cached context** hangs on `Connection to localhost:NNNNN refused`
(each attempt waits 30 s), because that context's pool still points at a stopped container's port.
As soon as you have seen the `refused` line, **press Ctrl+C**. Do not wait for it to finish (on the
reference machine it had not finished after 10 minutes). Revert, and note in your report which
classes passed and where it hung.

### 10.4 Measurement D: container reuse

Reuse makes the shared container **survive the JVM**, so the next run attaches to it instead of
starting a new one [C-§5.6]. It is a **per-developer opt-in** that lives only on your machine, never in
the project. The shipped suite never needs it.

**1. Opt in.** Open the file `.testcontainers.properties` in your home directory
(`C:\Users\<you>\.testcontainers.properties` on Windows, `~/.testcontainers.properties` elsewhere).
On Windows with Docker Desktop, Testcontainers has usually already created it with a line like
`docker.client.strategy=org.testcontainers.dockerclient.NpipeSocketClientProviderStrategy` under a
`#Modified by Testcontainers` comment. **Keep every existing line.** **Add** this line (create the file
if it does not exist):

```properties
testcontainers.reuse.enable=true
```

From a shell instead of an editor:

```bash
echo "testcontainers.reuse.enable=true" >> ~/.testcontainers.properties
cat ~/.testcontainers.properties
```

```powershell
Add-Content -Path "$HOME\.testcontainers.properties" -Value "testcontainers.reuse.enable=true" -Encoding ascii
Get-Content "$HOME\.testcontainers.properties"
```

**2. Single-class dev loop.** This is the case reuse is for: you rerun one class again and again while
you work on it. Run `./mvnw test -Dtest=PostgresWiringTest` **twice** and record both total times.
In the **second** run's log, find the reuse lines:

```bash
./mvnw test -Dtest=PostgresWiringTest 2>&1 | grep -E "Reusing|Total time"
```

```powershell
./mvnw test "-Dtest=PostgresWiringTest" | Select-String -Pattern "Reusing|Total time"
```

You should see `Reusing container with ID: … and hash: …` and `Reusing existing container (…) and not
creating a new one`. Note that the line `Container postgres:16 started in PT…S` is **still logged**
for a reused container (Testcontainers logs it when it attaches, too), so the `started` count of this
row is **1** in both runs. The `Reusing existing container` line is what tells you no new container
was created. For comparison you also need the **no-reuse** time of the same class: either measure it
first (before step 1), or measure it after step 5 below.

**3. Full suite.** Run `./mvnw test` twice with reuse on and record both total times and the test
count. Both runs must be **221 green**, with **7** `Container postgres:16 started` lines each (the
shared container's line is logged even when it is reused). Run 2 also logs exactly **one**
`Reusing existing container` line: only the shared singleton has `withReuse(true)`. Leftover rows from run 1 do not break run 2: `admin` and
`clerk.it` are accepted via `existsByUsername` / 409, and Lab 10's cleanup scripts leave the catalogue
tables empty.

**4. Look at the survivor.** After the JVM has exited, the reused container is **still running**: Ryuk
does not remove containers labelled for reuse.

```bash
docker ps --filter label=org.testcontainers.hash
```

**5. Turn it off and remove the container.** Delete the `testcontainers.reuse.enable=true` line from
the file (or set it to `false`), then remove the surviving container:

```bash
docker ps -q --filter label=org.testcontainers.hash | xargs -r docker rm -f
```

(`xargs -r` runs nothing when the list is empty, so the command is quiet if nothing is left to remove.
The PowerShell version below handles an empty list on its own.)

```powershell
docker ps -q --filter label=org.testcontainers.hash | ForEach-Object { docker rm -f $_ }
```

Confirm `docker ps --filter label=org.testcontainers.hash` lists nothing, and that a normal
`./mvnw test` prints the "Reuse was requested but the environment does not support the reuse of
containers" WARN again.

**When is reuse safe?** With reuse, the **data and the schema persist** between runs (the reused
container is the same database, and `ddl-auto=update` never drops anything). It is safe only for a
suite that tolerates leftover rows, like this one. Remove the container whenever you need a clean slate
(after changing entities, after a run that died half-way). Any configuration change (image tag,
environment, a `withX(…)` call) changes the container's hash, so Testcontainers starts a new container
and the old one stays behind until you remove it. **CI must never enable reuse** (Lab 25). Write these
points in your own words in the report.

### 10.5 Reference numbers (reference machine: Windows 11, Docker Desktop 28.3.2)

| Configuration | Tests | Total time | Contexts | `postgres:16` starts |
| --- | --- | --- | --- | --- |
| A: singleton (shipped) | 221 green | 40.8 s / 37.0 s / 40.9 s | 9 | 7 |
| B: per-class done right | 221 green | 1:01 min | 20 | 20 |
| C: per-class done wrong | first class of each database context green (e.g. `AuthorizationMatrixTest` 17/17), then `Connection … refused` | did not finish in 10 min (killed) | n/a | n/a |
| D: `PostgresWiringTest` alone, no reuse | 3 green | 11.0 s / 12.7 s | 1 | 1 |
| D: `PostgresWiringTest` alone, reuse on | 3 green | 9.5 s / 8.9 s | 1 | 1 (run 2 also logs `Reusing existing container`) |
| D: full suite, reuse on | 221 green | 35.4 s / 37.5 s | 9 | 7 (run 2 also logs one `Reusing existing container`) |

All counts in this table are deterministic: every row of yours must show the same contexts and
`started` counts. In the reuse rows the `started` count does **not** drop, because the line is logged
for a reused container too. The evidence of reuse is the `Reusing existing container` line (and the
shorter time).

Reading them: the singleton saves about a third of the suite's time against per-class containers.
Reuse saves ~2–3 s per single-class run (one container start plus readiness wait), which matters in a
tight edit-run loop. On the full suite that gain is lost in the noise. (Lab 10's "19 s" was surefire's
test time on H2, a different measure from Maven's total time. Do not compare them directly.)

### 10.6 `LAB_11_RESULTS.md` template

Create `LAB_11_RESULTS.md` in the module root with this structure and fill in every blank cell and
every answer. Short answers are fine (2–4 sentences each). Where a cell has two runs, write both
(`11.0 s / 12.7 s`). The count columns hold one number each, because both runs give the same count.

```markdown
# Lab 11 — Results

Machine: <OS, CPU, RAM, Docker Desktop version>

## Measurements

| Configuration | Tests green | Total time (run 1 / run 2) | Contexts | postgres:16 starts |
| --- | --- | --- | --- | --- |
| A: singleton (shipped) | | | | |
| B: per-class, @DirtiesContext(AFTER_CLASS) | | | | |
| D: PostgresWiringTest alone, no reuse | | | | |
| D: PostgresWiringTest alone, reuse on | | | | |
| D: full suite, reuse on | | | | |

Measurement C (optional) observed: <yes/no; what you saw before Ctrl+C>

"Reusing existing container" line seen in run 2 of the single-class loop: <paste the line>

## Questions

1. Why does configuration B start 20 contexts and 20 containers while A starts 9 and 7?
2. Why must the shared base NOT carry @Testcontainers/@Container? What exactly happens in configuration C?
3. How much time per run did reuse save in the single-class loop, and why is the gain hard to see on the full suite?
4. When is container reuse safe, when do you need a fresh container, and why must CI never enable it?
5. Which of the three PostgresDialectTest behaviours would most likely have hidden a real bug in StockFlow, and how?
```

## 11. Suggested Build Order

1. Copy the module (§3.1) → `./mvnw test` = **208 on H2**.
2. Check Docker (§3.3).
3. Edit the pom (§5.1) and prove it with `dependency:tree` (§5.2). Add the `ddl-auto` line (§5.3).
   Optionally run once and see the red of §5.4.
4. Write `AbstractPostgresContainerTest` (§7.1).
5. Add the eight `extends` clauses (§7.2) → `./mvnw test` = **208 on Postgres**, one
   `Container postgres:16 started` line.
6. Write `PostgresWiringTest` (§7.3); run it alone.
7. Write `PostgresDialectTest` (§7.4); run it alone. If test 2 fails with "but was 23505", reread
   gotcha 6.
8. Write `ContainerLifecycleTest` (§7.5); run it alone and count its 5 container starts.
9. Write `ServiceConnectionBeanGotchaTest` (§7.6); run it alone.
10. Full run → **221**; count **9** contexts and **7** container starts (§9.3).
11. Part 2 (§10): measurements A, B (then revert), C (optional), D (then turn reuse off and remove the
    container). Write `LAB_11_RESULTS.md`.
12. Postman smoke run (§13).
13. Final full run → **221 / 9 / 7**, and the acceptance criteria (§14).

## 12. Running and Reading the Suite

```bash
./mvnw test                                                            # everything: 221
./mvnw test -Dtest='com.example._11_testcontainers.containers.*Test'   # the containers package: 10
./mvnw test -Dtest=PostgresDialectTest                                 # one class: 3
./mvnw test -Dtest='PostgresDialectTest#postgresOnlySql'               # one method
./mvnw test -Dtest='com.example._11_testcontainers.integration.*Test'  # the Lab 10 layer, now on Postgres: 41
```

In PowerShell, quote the whole argument: `./mvnw test "-Dtest=PostgresDialectTest"`.

`ContainerLifecycleTest` is ordered on purpose: `staticIsSharedInstanceIsFresh` run alone is red by
construction ("test 3 must run first"). Run the whole class.

What a healthy log shows for the **shared** container, in order [C-§6]: `Creating container for image:
postgres:16`, the reuse WARN (`Reuse was requested but the environment does not support the reuse of
containers`), `Container postgres:16 is starting: <id>`, `Container postgres:16 started in
PT1.0062091S` (≈1.0–1.3 s), `Container is started (JDBC URL:
jdbc:postgresql://localhost:<port>/test?loggerLevel=OFF)`. Then come Hibernate's `create table`
statements (from `ddl-auto=update` on an empty database) and the familiar `Started …Test in` lines.
In a **full** run these are not the first container lines: surefire runs `containers.*` first, so
`ContainerLifecycleTest`'s 5 containers start (and stop) before the shared one. On Windows,
surefire ran the packages in this order on the reference machine: `containers.*`, `controller.*`,
`integration.*`, `model.*`, `repository.*`, `security.*`, `service.*`. Context start times there
(`Started X in`): the `@DataJpaTest` context ≈ 3.9 s (it is usually the first Spring context of the JVM
and pays class loading and Hibernate start-up), the first `@WebMvcTest` ≈ 3 s and later ones
≈ 0.3–0.5 s each, `RANDOM_PORT` ≈ 1.5–2 s, `MOCK` ≈ 0.7–1 s. Full suite: 37.0–40.9 s total on the
reference machine.

## 13. Postman: the Lab 10 Collection, Unchanged

`StockFlow_Docker.postman_collection.json` ships with this module **exactly as it left Lab 10** (it
has not changed since Lab 9: 26 requests, 55 assertions). Lab 11 changed only how the *tests* get a
database. No endpoint, status code or body changed, so there is nothing new to call. A one-time smoke
run proves the point (from inside the module):

```bash
docker compose up -d --build      # builds the image (the Dockerfile skips tests) and starts app + postgres
docker compose ps                 # wait until the app shows (healthy)
# Postman: import/run the collection against http://localhost:8080 -> all assertions green
docker compose down -v            # stop and remove containers AND the volume
```

The compose stack and the test containers are independent: compose's Postgres listens on host port
5433 with its own `stockflow` database, and the test containers use random ports and the `test`
database. You can run `./mvnw test` while the stack is up.

- [ ] The Lab 10 collection runs green against the Compose stack.

## 14. Acceptance Criteria

- [ ] Module copied and renamed; **no file under `src/main` differs** from your Lab 10 module except for
      package/import lines. Check with
      `diff --strip-trailing-cr -r ../_10_integration_testing/src/main/java/com/example/_10_integration_testing src/main/java/com/example/_11_testcontainers | grep '^[<>]' | grep -v '_1[01]_'`
      → no output, and `diff --strip-trailing-cr -r ../_10_integration_testing/src/main/resources src/main/resources`
      → no output. (`--strip-trailing-cr` makes line-ending-only differences, CRLF vs LF, not count.)
- [ ] **H2 is gone:** no `com.h2database` in `pom.xml`, and `./mvnw dependency:tree` shows no H2.
- [ ] The pom declares `spring-boot-testcontainers`, `org.testcontainers:testcontainers-junit-jupiter`
      and `org.testcontainers:testcontainers-postgresql`, all `<scope>test</scope>`, **none with a
      `<version>`**.
- [ ] `src/test/resources/application.properties` contains `spring.jpa.hibernate.ddl-auto=update` and
      **no** `spring.datasource.*` property.
- [ ] `./mvnw test` → **221 tests**, 0 failures, 0 errors, 0 skipped, with Docker running.
- [ ] The log shows **9** `Started …Test in` lines and **7** `Container postgres:16 started` lines (§9.3).
- [ ] `AbstractPostgresContainerTest` is `public abstract`, has **no class annotations**, and has exactly
      one `@ServiceConnection protected static final PostgreSQLContainer POSTGRES` (new
      `org.testcontainers.postgresql` package, image `postgres:16`, `withReuse(true)`) started in a
      static initializer. **No `@Testcontainers` or `@Container`** in it.
- [ ] The six `@DataJpaTest` classes and the two Lab 10 base classes extend it, and **no other line** in
      them changed (comments excepted). No class adds `@AutoConfigureTestDatabase`.
- [ ] The four new classes exist with the names, test counts and assertions of §7.3–§7.6.
      `ContainerLifecycleTest` and `ServiceConnectionBeanGotchaTest` do **not** extend the base.
- [ ] No test source imports `org.testcontainers.containers.PostgreSQLContainer` (the deprecated class).
- [ ] `LAB_11_RESULTS.md` exists in the module root with every cell of the measurement table filled in
      and all five questions answered. Your counts match §10.5 in every row, reuse rows included
      (timings may differ).
- [ ] Reuse is **off** again on your machine (or you can say why you kept it on), and no reused
      container is left running (`docker ps --filter label=org.testcontainers.hash` is empty).
- [ ] `StockFlow_Docker.postman_collection.json` is present and unchanged, and runs green against the
      Compose stack (§13).

## 15. Optional Stretch Goals (not graded)

1. **A `jsonb` column, on a scratch table.** In a throw-away `@DataJpaTest` method (extending the base),
   use `jdbcTemplate` to `create table scratch_doc (id serial primary key, body jsonb)`, insert
   `'{"a":1}'`, and assert that `select body ->> 'a' from scratch_doc` returns `"1"`. Because the test is
   transactional, the table disappears with the rollback (Postgres DDL is transactional). On H2 it
   fails in two different places. In default mode even the column type fails (`Unknown data type:
   "JSONB"`). In PostgreSQL mode H2 accepts the `jsonb` column and the insert, but the `->>` operator is
   a syntax error. Delete the test afterwards, or keep it out of the graded count.
2. **`@EnabledIfDockerAvailable`.** Testcontainers ships `org.testcontainers.junit.jupiter.EnabledIfDockerAvailable`
   (and `@Testcontainers(disabledWithoutDocker = true)`). This project deliberately does **not** use
   them: a build without Docker should fail loudly, not report "green" with half the tests skipped.
   Write a paragraph: when *would* skipping be the right choice, and what would you need in CI to make
   sure skipped tests still run somewhere?
3. **`@ImportTestcontainers`.** Spring Boot can import containers declared as static fields of another
   class or interface into a context (`@ImportTestcontainers`, in `spring-boot-testcontainers`; look up
   its package in your IDE). Try moving `POSTGRES` into a separate `interface` and importing it, then
   explain how it differs from extending a base class: what happens to the context cache key?
4. **The `@Bean` container in a real test.** Write a `@TestConfiguration` with
   `@Bean @ServiceConnection PostgreSQLContainer postgres()` and a `@SpringBootTest` that imports it,
   instead of extending the base. Count the contexts and containers. Why is this a new context, and
   whose container does it use?
5. **`generate_series`.** Find one more H2/Postgres difference yourself. (Hint: `select x from
   generate_series(1, 3) as x` fails on H2 in PostgreSQL mode.) Write it as a fourth dialect test in a
   scratch class.

## 16. Instructor Notes

- **Grading shortcut:** `./mvnw test` → 221; `grep -cE "Started [A-Za-z]+Test in"` → 9;
  `grep -c "Container postgres:16 started"` → 7; `grep -c h2database pom.xml` → 0;
  `grep -rl "import org.testcontainers.containers.PostgreSQLContainer" src/test` → nothing;
  `grep -nE "^\s*@(Testcontainers|Container)\b" src/test/java/com/example/_11_testcontainers/support/AbstractPostgresContainerTest.java`
  → nothing (the reference's Javadoc names both annotations, so grep for an annotation at the start of a
  line, not for the word); the `src/main` diff of §14 (with `--strip-trailing-cr`) → empty;
  `LAB_11_RESULTS.md` has 9 / 7 and 20 / 20 in the count columns, and 1 / 1 / 7 starts in the three
  D rows. A filled-in **reference report** (the answer key, with the reference-machine numbers and
  model answers to the five questions) ships next to this file as `LAB_11_RESULTS.md`. Remove it from
  the copy you hand to students.
- **Roadmap corrections (the facts in this document are verified against Boot 4.0.6 / Testcontainers
  2.0.5; where the roadmap disagrees, this document is right):**
  - The roadmap says a `@Bean` container "needs `@ServiceConnection(name = "postgres")` or it fails with
    `ConnectionDetailsFactoryNotFoundException`". **False for JDBC on Boot 4.0.6.** The JDBC factory
    accepts any connection name, a name cannot rescue a type mismatch (test 2 proves it), the exception
    you actually get is `ConnectionDetailsNotFoundException` (`ConnectionDetailsFactoryNotFoundException`
    exists but is internal; it is caught and rethrown as the former), and the fix is the precise
    declared return type. The misleading hint in the exception message ("You may need to add a 'name'…")
    is the likely source of the myth. A name *does* matter for factories that match by name on
    `GenericContainer`-based services (e.g. a later lab's Redis container). Say so only in general terms.
  - The roadmap names the module "`org.testcontainers:postgresql`" and the JUnit module "`junit-jupiter`".
    Those are the **1.x** names. Testcontainers 2.x renamed them `testcontainers-postgresql` /
    `testcontainers-junit-jupiter`, and only the new names are in Boot 4's BOM. "Managed by the
    Testcontainers BOM" is true, but through Boot's parent: no explicit `<dependencyManagement>` import
    is needed.
  - The roadmap's base class is called `AbstractIntegrationTest`. Here it is `AbstractPostgresContainerTest`
    in a `support` package, because the `@DataJpaTest` classes extend it too, not only integration tests.
    It is a parent of the Lab 10 bases, not a replacement for them.
  - "Assert a Postgres-specific JPQL/native query that H2 mishandled" is implemented without touching
    `src/main` (instructor decision: the demo is test-only). The collation sort goes through the existing
    `ProductRepository`, and `RETURNING` / `ON CONFLICT` / the aborted transaction use `JdbcTemplate`.
    The roadmap's "Bookstore" is StockFlow.
  - The roadmap's "Why H2 lies" list mentions sequences and constraint behaviour. On H2 2.4.240 these
    were measured to behave **the same** as Postgres: explicit-id inserts do not advance an identity in
    either engine, a unique violation is SQLState `23505` in both, and `numeric(12,2)` rounding and
    `ILIKE` also match. Do not let students claim them as differences.
- **The three most common failures:** (1) `@Testcontainers`/`@Container` copied onto the shared base
  "because every tutorial does it" → 30-second `Connection refused` hangs (gotcha 7); ask them which
  object outlives which. (2) One `@DataJpaTest` class missing its `extends` → "Failed to replace
  DataSource with an embedded database" (gotcha 3). (3) The deprecated `org.testcontainers.containers.PostgreSQLContainer`
  picked by auto-import (gotcha 5). It compiles, so it is easy to miss without the grep.
- **"Docker not running" looks like a classpath bug.** Students see `NoClassDefFoundError` over and over and
  start editing the pom. Teach them to scroll to the **first** error (`ExceptionInInitializerError`,
  "Could not find a valid Docker environment").
- **`create-drop` passes today.** A student who uses `create-drop` gets 221 green (verified). Do not mark
  it wrong by the test result. Ask for the reasoning of §5.3 instead.
- **Part 2 is where the understanding shows.** The counts (9/7 vs 20/20) are deterministic, and a
  student whose B row does not say 20/20 has not applied all four changes. The timings are not
  deterministic. Grade the explanation, not the seconds.
- **What comes next:** Lab 12 replaces `spring.jpa.hibernate.ddl-auto=update` with Flyway migrations,
  and those migrations will run against this same real-Postgres test foundation. Lab 25's CI pipeline
  runs this suite on a runner with Docker, and with reuse **off**.
