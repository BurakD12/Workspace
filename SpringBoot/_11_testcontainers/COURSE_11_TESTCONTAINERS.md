# Course 11 — Real Dependencies in Tests with Testcontainers: Why H2 Lies, Disposable PostgreSQL, and `@ServiceConnection`

**Prerequisites:** a finished Lab 10 (`_10_integration_testing`) with its 208 green tests: Mockito unit
tests, `@DataJpaTest` and `@WebMvcTest` slices, and the two-context `@SpringBootTest` integration suite.
You also need **Docker Desktop installed and running** — the same engine you used in Lab 9 to run
StockFlow and its `postgres:16` database with `compose.yaml`.

**Stack:** Spring Boot 4.0.6 · Testcontainers 2.0.5 (managed by Boot's BOM) · PostgreSQL 16 · PostgreSQL
JDBC 42.7.11 · JUnit 5 · AssertJ. The H2 version Lab 10 tested on was 2.4.240. Every number below
(timings, container counts, context counts, error messages) was measured on the reference
`_11_testcontainers` module — Windows 11, Docker Desktop 28.3.2 — not estimated. Timings are labelled
"reference machine": yours will differ, the *ratios* will not.

---

## What you'll learn

Lab 10 ended with 208 green tests, and every one that touched a database ran against **H2**, an
in-memory Java database. StockFlow in production — `compose.yaml`, Lab 9 — runs on **PostgreSQL 16**. So
the honest summary of Lab 10 is: *"the seams are tested — on a database we never ship."* This course
closes that last gap. You will replace H2 entirely with a real PostgreSQL that the test suite starts in a
throwaway Docker container, and wire it into Spring Boot with zero configuration.

By the end you will be able to:

1. List the **concrete, verified** ways H2 behaves differently from PostgreSQL — and, just as important,
   the popular "differences" that turned out to be the **same** on the versions we use.
2. Explain what **Testcontainers** does under the hood: Docker, image pull, start, wait strategy, random
   mapped port, Ryuk, stop — and what a run looks like with Docker switched off.
3. Use the JUnit 5 extension (**`@Testcontainers` / `@Container`**) and say exactly how a `static` field
   differs from an instance field, with test evidence. Use the Testcontainers **2.x** module names and the
   new, non-generic `PostgreSQLContainer`.
4. Wire a container into Spring Boot with **`@ServiceConnection`** — no URL, no username, no password, no
   `@DynamicPropertySource` — and avoid the two traps that come with it: `ddl-auto` defaulting to `none`,
   and the **`@Bean` return-type gotcha** (including why "add `name = "postgres"`" is *not* the fix).
5. Keep the suite fast with a **singleton container**, explain why `@Container` on a shared base class
   breaks Spring's context cache, measure per-class versus shared, and use **container reuse** safely in
   your local dev loop — never in CI.

---

## 1. Why H2 lies

### 1.1 The bargain Lab 10 made

H2 is convenient. It lives on the classpath, starts in milliseconds, and Boot auto-configures it when no
`spring.datasource.url` is set: a fresh `jdbc:h2:mem:<uuid>` per application context, with
`ddl-auto=create-drop` [Lab 10 C-§2.3]. Nothing to install, nothing to clean up.

The price is that every repository and integration test proves "this works on H2". That is a *different
database engine* with its own parser, its own type system, its own sort rules and its own transaction
semantics. H2 has a "PostgreSQL compatibility mode" (`MODE=PostgreSQL`), and it is tempting to believe
that mode closes the gap. It does not — it changes some syntax and some defaults; it does not turn H2 into
Postgres.

### 1.2 What we actually measured

The table below was produced by running the same statements against **H2 2.4.240** — in its default
mode **and** in `MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH` (the strongest
"pretend to be Postgres" settings) — and against **`postgres:16`** (database collation `en_US.utf8`,
provider `libc`). Only differences that were observed are listed.

| # | Statement | PostgreSQL 16 | H2 (both modes unless noted) |
| --- | --- | --- | --- |
| 1 | `ORDER BY name` over `apple, Banana, cherry, fig, _widget, Zebra` | `apple, Banana, cherry, fig, _widget, Zebra` | `Banana, Zebra, _widget, apple, cherry, fig` |
| 2 | a `jsonb` column, read with `body ->> 'a'` | `1` | default mode: even the column type is rejected — `Unknown data type: "JSONB"`; PG mode: the `jsonb` column is **accepted** (create table and insert work), but the `->>` operator is a syntax error |
| 3 | `INSERT … RETURNING id` | returns the id | syntax error |
| 4 | `INSERT … ON CONFLICT (sku) DO NOTHING` (row exists) | `INSERT 0 0` — no row, no error | syntax error |
| 5 | `SELECT … FROM generate_series(1,3) AS x` then use `x` | works | default mode: works; **PG mode**: `Column "x" not found` |
| 6 | a statement **after** a failed statement in the same transaction | `ERROR: current transaction is aborted, commands ignored until end of transaction block` (SQLState **25P02**) until rollback | the next statement just works |

Read the last column carefully: rows 2–4 are things a StockFlow developer could plausibly write tomorrow
(a native query with `RETURNING`, an upsert, a `jsonb` attributes column queried with `->>`). On H2 each
one fails — for `jsonb`, PG mode lets you *declare* the column, so the failure only appears once a query
uses a `jsonb` operator. The team either avoids a perfectly good Postgres feature or skips the test — and production is the first
place the query ever runs. Row 5 shows that "compatibility mode" can be *less* compatible than the
default for a given statement. Rows 1 and 6 are the sneaky ones: nothing fails on H2, it just gives a
**different answer**. They get their own subsections.

### 1.3 Collation: same data, different order

Sorting text is not "compare the bytes". PostgreSQL delegates to the database's **collation** — here
`en_US.utf8` from the C library — which sorts the way a dictionary does: case is (mostly) ignored on the
first pass and punctuation such as `_` — and even spaces — are weighed very lightly, so `apple` comes
before `Banana`. H2
compares by code point: every uppercase letter (`A`–`Z`, 65–90) before `_` (95) before every lowercase
letter (97–122).

StockFlow sorts by name in production: `GET /api/products?sort=name` hands a `Sort` to a Spring Data
repository and the database does the ordering. The reference proves the difference with five products:

| Inserted | PostgreSQL `Sort.by("name")` | H2 would give |
| --- | --- | --- |
| `Zebra, apple, _widget, cherry, Banana` | `apple, Banana, cherry, _widget, Zebra` | `Banana, Zebra, _widget, apple, cherry` |

Why did Lab 10's `ProductListingTest` never notice? Because it **never sorts by name**. It asserts the
default order (by SKU — all of the form `SKU-nnn`, uppercase letters plus digits, where code-point order and
dictionary order agree) and `?sort=unitPrice,desc` (numbers). No assertion ever compared two names, so the
collation difference could not show. Do not conclude that "capitalised names are safe" either: because the
collation also ignores spaces on its first pass, PostgreSQL sorts `Hex Bolt, Hex Nut, Hexagon` as
`Hexagon, Hex Bolt, Hex Nut`, while H2's code-point order keeps `Hex Bolt, Hex Nut, Hexagon`. A test that
passes on H2 tells you nothing about how real names — lowercase, punctuated or multi-word — will sort.

### 1.4 The aborted transaction (25P02)

PostgreSQL treats a transaction as all-or-nothing in a strict sense: once **any** statement inside it
fails, the transaction is marked aborted and **every** following statement is refused with SQLState
`25P02` until you roll back. H2 lets you carry on.

The reference demonstrates it inside a `@DataJpaTest` (which wraps each test in a transaction):

```java
jdbcTemplate.update("insert into categories (name) values ('Fasteners')");   // duplicate name
// → DataIntegrityViolationException (unique violation, SQLState 23505)

jdbcTemplate.queryForObject("select count(*) from categories", Long.class);
// Postgres → UncategorizedSQLException, "...current transaction is aborted, commands ignored
//            until end of transaction block", getSQLException().getSQLState() = "25P02"
// H2       → returns the count as if nothing happened
```

Two details matter when you assert on this:

- The exception Spring throws for the second statement is an **`UncategorizedSQLException`** (Spring has
  no specific translation for 25P02). Its `getSQLException()` is the `PSQLException` with SQLState
  `25P02`.
- That `PSQLException`'s own **cause** is the earlier failure — `23505`, `duplicate key value violates
  unique constraint "uk…"` (a Hibernate-generated constraint name). So AssertJ's `.rootCause()` gives you
  **23505**, not 25P02. Assert on `getSQLException().getSQLState()`.

**Why does H2 hide a real bug here?** Picture a `@Transactional` service method that writes with plain
JDBC (a `JdbcTemplate` or a native statement — exactly what the reference test does):

```java
try {
    jdbcTemplate.update("insert into categories (name) values (?)", name);
} catch (DataIntegrityViolationException e) {
    // "it's a duplicate — just log it and continue"
}
jdbcTemplate.update("insert into audit_log (message) values (?)", "category " + name);
// on Postgres this second statement fails with 25P02; on H2 it succeeds and the method commits
```

On H2 the test is green. On Postgres the "recovered" transaction is dead and the next statement blows
up. This "catch the constraint violation and carry on" pattern is a classic production-only failure.

> **Why plain JDBC and not `categoryRepository.saveAndFlush(...)`?** A Spring Data repository method is
> itself `@Transactional`. When the exception leaves the repository proxy, Spring marks the *surrounding*
> transaction **rollback-only**, so even on H2 the service's commit then fails with
> `UnexpectedRollbackException` (and Hibernate's session should not be used after a failed flush anyway).
> The repository version therefore fails on **both** engines. The H2-green trap is specific to statements
> that do not go through a transactional proxy: `JdbcTemplate`, native SQL, raw JDBC.

**Why StockFlow is safe:** its services never use that pattern. They **pre-check** — `existsByNameIgnoreCase`,
`existsBySkuIgnoreCase`, `existsByUsername`, and `existsByCategoryId` / `existsBySupplierId` before a delete — and throw
`DuplicateResourceException` / `ResourceInUseException` *before* any SQL has a chance to fail. The
transaction is never poisoned, so the 409s you tested in Lab 10 behave identically on Postgres. Now you
know that design choice was not only about nice error messages.

### 1.5 What turned out the SAME (do not repeat these myths)

The internet is full of lists of "H2 vs Postgres differences". We tested the commonly quoted ones on
H2 2.4.240 and PostgreSQL 16. These behaved **identically**:

| Claim you will read | What we observed |
| --- | --- |
| "Explicit ids break the identity sequence on one engine but not the other" | In **both**, inserting explicit ids into a `generated by default as identity` column does **not** advance the identity; the next generated id is 1 in both. (Lab 10's seed scripts use ids from 9001 for exactly this reason, and they work unchanged on Postgres.) |
| "`numeric(12,2)` rounds differently" | `0.125` → `0.13` in both. |
| "`ILIKE` is Postgres-only" | Works in both. |
| "Constraint violations report different SQLStates" | Unique violation is `23505` in both. |

Learn the rule, not the list: lists go stale with every release of either engine. The only way to know
your queries behave like production is to **run them on the production engine**.

### 1.6 The conclusion

Testing on H2 means proving a slightly different program. The fix is not "be careful with SQL" — it is to
run tests against the very image production runs: `postgres:16`, the same tag as `compose.yaml`. That
image is already on your machine from Lab 9. All we need is a way for a test to start it, find it, and
throw it away. That is Testcontainers.

---

## 2. What Testcontainers is

### 2.1 A Java library that drives Docker

**Testcontainers** is a Java library that starts and stops Docker containers **from your test code**.
It does not contain a database or a container runtime. It talks to the Docker engine you installed in Lab
9 (through the `docker-java` client, version 3.7.1 on our classpath), exactly as the `docker` CLI does —
on Windows, over Docker Desktop's named pipe.

So "Testcontainers" really means: *"`docker run postgres:16`, wait until it's ready, tell me where it is,
and `docker rm -f` it when I'm done — all from Java, with no shell."*

On Windows the first run writes a file `C:\Users\<you>\.testcontainers.properties` containing a line like
`docker.client.strategy=org.testcontainers.dockerclient.NpipeSocketClientProviderStrategy` (headed
`#Modified by Testcontainers`). That is Testcontainers remembering how it found Docker. Leave it alone; §5.6
adds one line to it.

### 2.2 The lifecycle of one container

```
 new PostgreSQLContainer("postgres:16")     nothing happens yet — just configuration
            │
        start()
            │  1. pull       image not present locally? download it (postgres:16 already is, from Lab 9)
            │  2. create     with env vars POSTGRES_DB / USER / PASSWORD = test / test / test
            │  3. start      Docker publishes container port 5432 on a RANDOM free host port
            │  4. wait       block until the wait strategy says "ready" (Postgres accepting connections)
            ▼
   getJdbcUrl()  →  jdbc:postgresql://localhost:58564/test?loggerLevel=OFF
   getHost()     →  localhost      getMappedPort(5432) → 58564   (different every run)
   getUsername() / getPassword() / getDatabaseName() → test / test / test
            │
        stop()          (or Ryuk, when the JVM exits)
            ▼
   container removed — data gone
```

Three ideas carry the whole library:

- **Random mapped port.** Inside the container Postgres listens on 5432, as always. On your host it is
  published on a free ephemeral port that Docker picks (in practice never 5432), so it does not collide with the Lab 9
  `compose.yaml` database you might have running, or with another test JVM. The price: **you cannot know
  the URL in advance**. You must ask the container *after* it started. Section 4 is about how Spring Boot
  asks.
- **Wait strategy.** A started Docker container is not a ready database; Postgres needs a moment to
  initialise. `start()` does not return until the module's wait strategy confirms the server is ready, so
  the first query never races the boot.
- **Disposable.** Every fresh container is an empty database. No leftover rows from yesterday, no
  "works on my machine because my local DB has that row".

### 2.3 Ryuk: the cleanup you do not write

What if the test JVM crashes, or you hit Ctrl+C before `stop()` runs? Testcontainers starts one extra,
tiny container first — **Ryuk** (image `testcontainers/ryuk:0.14.0`). The JVM holds a connection to it;
when that connection closes for *any* reason, Ryuk removes every container the JVM created. That is why
StockFlow's shared container (§5.2) never calls `stop()`: Ryuk removes it at JVM exit.

You will see Ryuk in `docker ps` while tests run. It is normal.

### 2.4 What it looks like in the log

Every container start produces lines like these, in this order (verified on the reference run):

```
Creating container for image: postgres:16
Container postgres:16 is starting: 3f9c…
Container postgres:16 started in PT1.0062091S
Container is started (JDBC URL: jdbc:postgresql://localhost:58564/test?loggerLevel=OFF)
```

For StockFlow's shared container, which asks for reuse (§5.6), the reuse `WARN` is printed **between**
`Creating container…` and `is starting` — before the container starts, not after.

`PT1.0062091S` is an ISO-8601 duration: 1.006 seconds. On the reference machine one `postgres:16` start
costs **≈ 1.0–1.3 s** when the image is already present. Counting the `started in` lines is how you count
containers, just as counting `Started … in` lines counted contexts in Lab 10 [Lab 10 C-§2.5].

### 2.5 Without Docker

Docker is now a **hard requirement** of `./mvnw test`. If the engine is not running, Testcontainers
cannot find it and throws an `IllegalStateException` with the message:

```
Could not find a valid Docker environment. Please see logs and check configuration
```

and on later attempts in the same JVM: `Previous attempts to find a Docker environment failed. Will not
retry.`

How that surfaces depends on *where* the container is started. StockFlow starts its shared container in
a **static initializer** (§5.2), and the JVM has strict rules for a static initializer that throws:

- the **first** test class that loads the base class fails with `java.lang.ExceptionInInitializerError`;
- **every later** subclass fails with `java.lang.NoClassDefFoundError: Could not initialize class
  …AbstractPostgresContainerTest` — the JVM never retries a failed class initialisation.

A class using the JUnit extension (§3) fails inside the extension instead. Pure Mockito unit tests and the
`@WebMvcTest` slices do not touch a database, so they still pass.

This course's decision is to **fail loudly**. Testcontainers offers ways to *skip* container tests when
Docker is absent (for example `@Testcontainers(disabledWithoutDocker = true)`), but a suite that turns
green by silently skipping its database tests is worse than one that turns red. If you see
`NoClassDefFoundError: Could not initialize class …AbstractPostgresContainerTest`, scroll up to the first
error: it is almost always "Docker is not running".

---

## 3. JUnit 5 integration: `@Testcontainers` and `@Container`

### 3.1 The dependencies — and the Testcontainers 2.x rename

Three test-scoped dependencies, **no `<version>` tags**: the Spring Boot 4.0.6 parent already imports
the Testcontainers 2.0.5 BOM.

| Artifact | Gives you |
| --- | --- |
| `org.springframework.boot:spring-boot-testcontainers` | `@ServiceConnection` (§4) |
| `org.testcontainers:testcontainers-junit-jupiter` | the JUnit 5 extension: `@Testcontainers`, `@Container` |
| `org.testcontainers:testcontainers-postgresql` | `PostgreSQLContainer` (pulls in `testcontainers-jdbc` and `testcontainers-database-commons`) |

And one dependency **removed**: `com.h2database:h2`.

> **Testcontainers 2.x renamed every module** with a `testcontainers-` prefix. Most tutorials, and
> older Boot 3 projects, use the 1.x names `org.testcontainers:postgresql` and
> `org.testcontainers:junit-jupiter`. Those are **not managed** by Boot 4's BOM — written without a
> version they fail to resolve; written with one they drag a 1.x artifact into a 2.x world. If a blog post
> says `<artifactId>postgresql</artifactId>` under `org.testcontainers`, translate it.

`./mvnw dependency:tree -Dscope=test` makes the chain visible:
`spring-boot-testcontainers:4.0.6 → testcontainers:2.0.5 → docker-java-api / docker-java-transport-zerodep
3.7.1`, and `testcontainers-postgresql:2.0.5 → testcontainers-jdbc → testcontainers-database-commons`.

### 3.2 Two `PostgreSQLContainer` classes — use the new one

Testcontainers 2.0.5 ships **two** classes with the same simple name. Your IDE will offer both.

| | New (use this) | Old (deprecated) |
| --- | --- | --- |
| Class | `org.testcontainers.postgresql.PostgreSQLContainer` | `org.testcontainers.containers.PostgreSQLContainer<SELF>` |
| Generic? | **no** — `PostgreSQLContainer pg` | yes — the infamous `PostgreSQLContainer<?> pg` |
| Status | current | `@Deprecated` in 2.0.5 |
| Constructors | `(String image)` or `(DockerImageName)` — nothing else | the same two, plus a deprecated no-arg constructor |

The methods you need are the same on both: `start()`, `stop()`, `isRunning()`, `getJdbcUrl()`,
`getUsername()`, `getPassword()`, `getDatabaseName()`, `getHost()`, `getMappedPort(int)`,
`getContainerId()`, `withDatabaseName/withUsername/withPassword(...)`, `withReuse(boolean)`. The defaults
are database `test`, user `test`, password `test`.

If you see `<?>` after `PostgreSQLContainer` in your code, you imported the old one.

### 3.3 Managing it by hand

A container is just an object with `start()` and `stop()`. Without any extension:

```java
PostgreSQLContainer pg = new PostgreSQLContainer("postgres:16");
pg.start();
try (Connection c = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())) {
    // talk to a real PostgreSQL 16
} finally {
    pg.stop();
}
```

That works, but every test class would repeat the start/stop dance. The JUnit 5 extension does it for you.

### 3.4 The extension

```java
@Testcontainers                                   // org.testcontainers.junit.jupiter
class SomeTest {

    @Container                                    // org.testcontainers.junit.jupiter
    private static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:16");
    …
}
```

`@Testcontainers` registers a JUnit 5 extension. The extension looks for fields annotated `@Container`
and manages their lifecycle according to **how the field is declared**:

| Field | Started | Stopped | Effect |
| --- | --- | --- | --- |
| `static` | once, before the first test of the class | after the last test of the class | **one container per class**, shared by its tests |
| instance (non-static) | before **each** test method | after **each** test method | **a fresh container per test** |

Why the difference? JUnit 5 creates a **new instance of the test class for every test method** (the
default `PER_METHOD` lifecycle). An instance field therefore belongs to one test; a static field belongs to
the class.

### 3.5 The evidence: `ContainerLifecycleTest`

The reference contains a four-test class with **no Spring at all** that proves the table. It declares both
kinds of field:

```java
@Container private static final PostgreSQLContainer PER_CLASS = new PostgreSQLContainer("postgres:16");
@Container private final PostgreSQLContainer perTest = new PostgreSQLContainer("postgres:16");
```

and runs its tests in a fixed order (`@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`):

1. The static container is running on a **random host port** — `getMappedPort(5432)` is not 5432 — and
   its JDBC URL starts with `jdbc:postgresql://` and contains `host:port`.
2. It is **real PostgreSQL**: a plain JDBC `select version()` returns a string starting `PostgreSQL 16.`.
3. Test 3 records both containers' `getContainerId()` into **static** fields (static because the next test
   runs on a new instance — an instance field would be gone).
4. Test 4 compares: the static container's id is **the same** as test 3 saw; the instance container's id
   is **different** — a new container was started for this test.

How many containers did that class start? One static plus one per test for the instance field:
**1 + 4 = 5**, at about a second each — the class takes ~8.5–9 s on the reference machine. That number is
the whole argument of §5 in miniature: a container per test is correct, isolated, and slow.

### 3.6 What the extension does not know

The extension knows JUnit: classes and methods. It knows **nothing** about Spring's context cache. That is
fine for a class like `ContainerLifecycleTest`, which has no Spring context. It becomes a serious trap the
moment a cached Spring context holds a connection pool pointing at a container the extension has just
stopped (§5.3).

---

## 4. Zero-config wiring with `@ServiceConnection` (Boot 3.1+, used here on Boot 4)

### 4.1 The problem: a URL nobody knows in advance

StockFlow's datasource needs a URL, a username and a password. The container's URL contains a random port
that exists only after `start()`. So the URL cannot go in `application.properties`.

**The old way** (before Boot 3.1, and many tutorials still) was `@DynamicPropertySource`: a static
method that registers property *suppliers*, evaluated lazily when the context starts:

```java
// For contrast only — NOT used in this course
@DynamicPropertySource
static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", PG::getJdbcUrl);
    registry.add("spring.datasource.username", PG::getUsername);
    registry.add("spring.datasource.password", PG::getPassword);
}
```

It works, but you must know the right property names for every technology, repeat them in every project,
and keep them in sync.

### 4.2 The modern way: `@ServiceConnection` (Boot 3.1+, used here on Boot 4)

```java
@ServiceConnection                                // org.springframework.boot.testcontainers.service.connection
protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");
```

One annotation, and the datasource points at the container. What happens:

1. Boot's datasource auto-configuration does not read `spring.datasource.*` directly. It asks for a
   **`JdbcConnectionDetails`** bean (`org.springframework.boot.jdbc.autoconfigure`) — an interface with
   `getJdbcUrl()`, `getUsername()`, `getPassword()`. Normally Boot builds one from your properties.
2. `spring-boot-testcontainers` scans the test class **and its superclasses** for `@ServiceConnection`
   fields. The field **must be `static`**. Boot checks, and a non-static field fails the context with
   (verified) `java.lang.IllegalStateException: @ServiceConnection field 'pg' must be static` — the
   field's name in quotes.
3. For each such field, Boot looks for a **`ContainerConnectionDetailsFactory`** that can handle that
   container. The JDBC one is `JdbcContainerConnectionDetailsFactory` (from `spring-boot-jdbc`); it
   accepts any `JdbcDatabaseContainer<?>` — which `PostgreSQLContainer` is — under **any** connection
   name. On StockFlow's classpath it is the **only** database-related factory.
4. The factory produces a `JdbcConnectionDetails` that reads `getJdbcUrl()` / `getUsername()` /
   `getPassword()` straight from the container. That bean **wins** over properties.

Result: no `spring.datasource.*` anywhere in the test tree, no `@DynamicPropertySource`, and the reference's
`PostgresWiringTest` proves it — the injected `JdbcConnectionDetails` equals the container's URL, user and
password; the `DataSource` is an ordinary `HikariDataSource` whose `getJdbcUrl()` equals the container's;
and `select version()` through the context's `JdbcTemplate` starts with `PostgreSQL 16.`.

> **Field: runtime type decides.** For a field, Boot reads the field's *value* and uses its runtime class.
> Verified: a field declared `static GenericContainer<?> pg = new PostgreSQLContainer("postgres:16")`
> works, because the object really is a `PostgreSQLContainer`. Remember this — §4.5 is the case where it
> is **not** true.

### 4.3 `@DataJpaTest` no longer needs `@AutoConfigureTestDatabase(replace = NONE)`

`@DataJpaTest` carries `@AutoConfigureTestDatabase`, which by default replaces any **non-test**
datasource with an embedded one. Many older Testcontainers tutorials add
`@AutoConfigureTestDatabase(replace = Replace.NONE)` to stop an embedded database from replacing their
container. With `@ServiceConnection` on Boot 4.0.6 that line is **unnecessary**:

- `Replace` has four values: `NON_TEST`, `ANY`, `AUTO_CONFIGURED`, `NONE`. The **default is `NON_TEST`**
  (verified in the 4.0.6 bytecode).
- `NON_TEST` replaces only a datasource that is *not* a test datasource. In the 4.0.6 bytecode these count
  as **test** datasources and are left alone: connection details from a **service connection** (or from
  Docker Compose), and a datasource URL bound from **`@DynamicPropertySource`** (§4.1) or a `jdbc:tc:` URL.

So StockFlow's six `@DataJpaTest` classes change by exactly one thing — `extends
AbstractPostgresContainerTest` — and nothing else.

What if a `@DataJpaTest` class **forgets** the `extends`? With H2 removed from the pom there is no service
connection and no embedded database to replace the datasource *with*, so the context fails with (verified):

```
java.lang.IllegalStateException: Failed to replace DataSource with an embedded database for tests. If you
want an embedded database please put a supported one on the classpath or tune the replace attribute of
@AutoConfigureTestDatabase.
```

Adding `@AutoConfigureTestDatabase(replace = NONE)` to that class does not fix it — it only moves the
failure, because now nothing supplies a URL at all (verified):
`DataSourceProperties$DataSourceBeanCreationException: Failed to determine a suitable driver class`.
The fix is the missing `extends`.

### 4.4 The `ddl-auto=none` trap

In Lab 10 the schema appeared by magic: for an **embedded** database Boot defaults
`spring.jpa.hibernate.ddl-auto` to `create-drop`. PostgreSQL is **not** embedded, so the default becomes
**`none`** — Hibernate creates nothing. Swap H2 for the container and change nothing else, and every
database test fails with (observed on the reference module):

```
ERROR: relation "categories" does not exist
```

The fix is one line in `src/test/resources/application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Why `update` and not `create-drop`? Because of §5.5: every application context in the run shares **one**
container. With `create-drop`, each newly started context drops and recreates the schema — and its rows —
underneath the contexts already sitting in the cache. The reference suite happens to pass with
`create-drop` too (verified: 221 green), but that is **luck of class order**, not design. `update` never
drops anything. (Lab 12 replaces this line with Flyway migrations.)

### 4.5 The `@Bean` gotcha — as it really behaves in Boot 4.0.6

`@ServiceConnection` can also sit on a **`@Bean` method** that returns a container, in a
`@TestConfiguration` / `@Configuration` class. Spring then manages the container as a bean (starts it,
stops it with the context). There is a trap here, and the explanation you will find online is wrong.

**The rule, verified:** for a `@Bean` method Boot decides which connection details the container can
provide from the method's **declared return type**, read from the bean *definition* — before the bean is
created, before the method has run. It cannot look at the object, because the object does not exist yet.
(Contrast §4.2: a field has a value, so Boot uses its runtime type.)

The reference proves it with three tiny configurations, all returning `new PostgreSQLContainer("postgres:16")`:

| Declared return type | `@ServiceConnection` | Result |
| --- | --- | --- |
| `GenericContainer<?>` | plain | context **fails**: `ConnectionDetailsNotFoundException` — "No ConnectionDetails found for source '@ServiceConnection source for Bean 'postgres' defined in …'. You may need to add a 'name' to your @ServiceConnection annotation" |
| `GenericContainer<?>` | `name = "postgres"` | context **still fails**: same exception, same "No ConnectionDetails found…" (the hint sentence is gone) |
| `PostgreSQLContainer` | plain | **works**: one `JdbcConnectionDetails` bean whose URL equals the container's |

```java
@Bean
@ServiceConnection
GenericContainer<?> postgres() {                  // ✗ the JDBC factory only accepts JdbcDatabaseContainer types
    return new PostgreSQLContainer("postgres:16");
}

@Bean
@ServiceConnection
PostgreSQLContainer postgres() {                  // ✓ declared type is precise enough
    return new PostgreSQLContainer("postgres:16");
}
```

What to take away:

- **The fix is the precise return type.** Nothing else.
- **A name does not help.** The JDBC factory accepts *any* name, so the name was never the problem, and a
  name cannot rescue a type mismatch — the type check applies regardless.
- **The exception is `ConnectionDetailsNotFoundException`** (`org.springframework.boot.autoconfigure.service.connection`),
  and it is the context's startup failure itself — it has **no cause**. A class called
  `ConnectionDetailsFactoryNotFoundException` also exists, but it is internal: Boot catches it and rethrows
  the former. Neither extends the other.
- **In the failing cases no container is ever created** — the failure happens while bean definitions are
  registered.

> **Correcting a common claim.** You will read — including in this course's own roadmap — that a
> container declared as a `@Bean` "needs `@ServiceConnection(name = "postgres")` or it fails with
> `ConnectionDetailsFactoryNotFoundException`". For a JDBC database on Boot 4.0.6 every part of that is
> false: the exception you get is `ConnectionDetailsNotFoundException`, the name does not fix it, and the
> real fix is the declared type. The myth most likely comes from the exception's own hint ("You may need
> to add a 'name'…"). Names *do* matter for some other services — factories that match a plain
> `GenericContainer` by connection name, as you may meet with a later lab's Redis container — but not here.

**How do you test a context that is supposed to fail?** Not with `@SpringBootTest` — a failing context
fails the class, and a broken context must never end up in the shared cache. The reference uses
**`ApplicationContextRunner`** (`org.springframework.boot.test.context.runner`): it builds a small,
throw-away context with exactly the configuration you give it, and lets you assert on it with AssertJ:

```java
private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ServiceConnectionAutoConfiguration.class));

runner.withUserConfiguration(SomeConfig.class).run(context -> {
    assertThat(context).hasFailed();
    assertThat(context.getStartupFailure()).isInstanceOf(ConnectionDetailsNotFoundException.class);
});
```

`hasFailed()` / `hasNotFailed()`, `getStartupFailure()`, `hasSingleBean(Type.class)` and
`getBean(Type.class)` are the assertions you will use. These runner contexts print no `Started … in` line.
The context is closed at the end of `run(...)`, which also stops any container bean it started.

---

## 5. Container lifecycle, reuse, and keeping tests fast

### 5.1 Where the time goes

Two costs now add up in a run:

| Cost | Reference machine |
| --- | --- |
| one `postgres:16` container start (image present) | ≈ 1.0–1.3 s |
| one Spring context start (`Started … in`) | `@DataJpaTest` ≈ 3.9 s (the first Spring context of the JVM, so it also pays class loading and Hibernate start-up); first `@WebMvcTest` ≈ 3 s, then ≈ 0.3–0.5 s each; `RANDOM_PORT` ≈ 1.5–2 s; `MOCK` ≈ 0.7–1 s |

Spring's context cache already makes a context start **once** and serve many classes [Lab 10 C-§2.4]. The
container must follow the same rule, or each class pays a container *and* — as you will see — a context.

### 5.2 The singleton container

The reference holds the suite's one database container in an abstract base class with **no annotations**:

```java
public abstract class AbstractPostgresContainerTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withReuse(true);       // §5.6 — a no-op unless you opt in locally

    static {
        POSTGRES.start();
    }
}
```

Why each piece:

- **`static final` + a static initializer.** The JVM runs a class's static initializer **exactly once**,
  when the class is first **initialised** (first used: a static field read, an instance created, or a
  subclass being initialised) — not merely loaded. Here that happens when the first database test class
  is initialised (Spring reading the inherited `@ServiceConnection` field also triggers it). So the
  container starts once per test run — and every later class finds it already running.
- **Nobody calls `stop()`.** Ryuk removes the container when the JVM exits (§2.3).
- **`@ServiceConnection` on the inherited field.** Boot scans superclasses (§4.2), so every subclass's
  context gets its connection details from the same container. Same container, same URL — and because the
  subclasses add no annotations of their own, Spring's context cache key does not change either.
- **No `@Testcontainers`, no `@Container` — on purpose.** That is §5.3.
- **`postgres:16`** — the tag `compose.yaml` runs, already pulled in Lab 9.

Every database test extends it: the six `@DataJpaTest` classes directly, and the Lab 10 integration
classes through `AbstractRestIntegrationTest` / `AbstractMockMvcIntegrationTest`, which now say `extends
AbstractPostgresContainerTest`.

You will see INFO lines like `Could not detect default configuration classes for test class [...]` for
many Spring test classes — they were already there in Lab 10 — and now also, several times, for
`[...AbstractPostgresContainerTest]`: Spring's test framework noticing the base class. They are harmless;
ignore them.

### 5.3 The trap: `@Container` on a shared base class

It looks natural to "do it properly" with the extension on the base class:

```java
@Testcontainers                                  // ✗ do not do this on a shared base
public abstract class AbstractPostgresContainerTest {
    @Container @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");
}
```

Now run the suite and follow the two lifecycles, which no longer agree:

1. A database class runs. The extension starts the container on, say, port 59434. No context with this
   class's key is cached yet, so Spring builds a fresh one; its Hikari pool connects to `localhost:59434`.
   The class passes.
2. The class ends. The **extension stops the container** — that is what a static `@Container` means
   (§3.4). Before the *next* class, it starts the same static container again, on a *new* random port.
3. If the next class's context key is **not yet cached**, Spring builds a fresh context against the new
   port, and that class passes too. If the key **is** cached, Spring **reuses the cached context** — whose
   pool still points at the old, stopped container's port.
4. In that second case every database access waits Hikari's 30-second connection timeout and fails with:

```
org.postgresql.util.PSQLException: Connection to localhost:59434 refused. Check that the hostname and
port are correct and that the postmaster is accepting TCP/IP connections.
```

So the pattern is: the **first class of each database context** (`@DataJpaTest`, `RANDOM_PORT`, `MOCK`)
passes; **every later class that reuses an already-cached context** hangs and fails. (`@WebMvcTest` classes
never touch the database, so they are unaffected.) In the reference experiment `AuthorizationMatrixTest` —
the first `RANDOM_PORT` class, not the first database class — passed 17/17, and then the classes that
reused cached contexts hung. Thirty seconds per attempt, times every such test: the reference run **did
not finish within 10 minutes** and was killed. If you ever see this, press **Ctrl+C**, do not wait.

The lesson: the JUnit extension's lifecycle is per *class*; Spring's context lifecycle is per *cache
entry*, which outlives classes. Only the singleton (§5.2) keeps the two in step: a container that lives
exactly as long as the JVM, like the cache.

### 5.4 Per-class done right — and what it costs

You *can* have a container per class with Spring, if you also throw away the context with it:

```java
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractPostgresContainerTest {
    @Container @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");
}
```

`@DirtiesContext(AFTER_CLASS)` removes the context from the cache when the class ends — at the same moment
the extension stops the container — so the next class builds a fresh context against the fresh container.
Correct, and green. Measured on the reference machine:

| Strategy | Tests | Contexts | `postgres:16` starts | `./mvnw test` total |
| --- | --- | --- | --- | --- |
| **Singleton** (shipped) | 221 green | **9** | **7** | 37.0–40.9 s |
| Per class, done right (`@Testcontainers` + `@DirtiesContext(AFTER_CLASS)`) | 221 green | 20 | 20 | 1:01 min |
| Per class, done wrong (`@Testcontainers`, no `@DirtiesContext`) | — | — | — | did not finish in 10 min |

Where do 20 and 20 come from? There are 14 database test classes (6 `@DataJpaTest`, 2 new Lab 11
`@DataJpaTest`s, 6 integration). Per class, each pays its own container and its own context: 14 + the 6
containers of the two classes that do not extend the base (5 in `ContainerLifecycleTest`, 1 in
`ServiceConnectionBeanGotchaTest`) = 20 containers; 14 + the 6
`@WebMvcTest` contexts = 20 contexts. The singleton collapses the 14 to **1** container and **3** contexts
(the repository slice, `MOCK`, `RANDOM_PORT`). About 20 seconds of every run, gone — and the gap grows with
every database test class you add.

### 5.5 One container = one database: the new isolation consequence

In Lab 10 every context had its **own** H2 (`jdbc:h2:mem:<uuid>`), so contexts could never see each
other's rows [Lab 10 C-§2.3]. Now all three database contexts — the repository slice, `MOCK` and
`RANDOM_PORT` — talk to the **same** database, `test`, in the one container. Consequences:

- **Lab 10's cleanup discipline is now load-bearing.** `@Transactional` rollback (MOCK, `@DataJpaTest`) and
  the `@Sql(… AFTER_TEST_METHOD)` cleanup script (RANDOM_PORT) leave the catalogue tables empty between
  classes. That is why repository tests that count rows still pass, whatever ran before them.
- **Admin is seeded idempotently** by each `@SpringBootTest` context's `AdminUserSeeder`, which checks
  `existsByUsername` first — so the second context does not create a duplicate.
- **The clerk registered by the socket suite may now exist in the MOCK context** — or may not, depending on
  which class ran first. Order-dependent, so **do not rely on it** either way. (Lab 10's base class
  already accepts 201 *or* 409 on registration, so it is unaffected.)
- `users` and `refresh_tokens` rows accumulate during the run. Harmless: nothing counts them.

The general rule: once a database is shared, every test must leave it as it found it, or tolerate what
others left behind.

### 5.6 Container reuse: a faster local dev loop

Ryuk removes the container at JVM exit, so every `./mvnw test` pays a fresh start. **Reuse** lets a
container survive the JVM, so the next run attaches to it instead. It needs **two** switches:

1. **In code:** `.withReuse(true)` on the container (the reference has it, see §5.2).
2. **On your machine:** add this line to `C:\Users\<you>\.testcontainers.properties` (keep the lines that
   are already there, §2.1):

   ```properties
   testcontainers.reuse.enable=true
   ```

With only switch 1 (the default for anyone who has not opted in), reuse is a **no-op** plus a warning:

```
WARN tc.postgres:16 -- Reuse was requested but the environment does not support the reuse of containers
To enable reuse of containers, you must set 'testcontainers.reuse.enable=true' in a file located at
C:\Users\<you>\.testcontainers.properties
```

With both, run 1 starts the container normally; run 2 logs

```
Reusing container with ID: 50a27596… and hash: 10c5c78a…
Reusing existing container (…) and not creating a new one
```

Counting containers with reuse on: the `Container postgres:16 started in` line is **still printed** for a
reused container, so the counts do not change — `-Dtest=PostgresWiringTest` shows **1** `started` line,
the full suite **7**, on run 1 and on run 2 alike (verified). What distinguishes run 2 is the extra
`Reusing existing container (…) and not creating a new one` line — exactly **one** of them, for the shared
container; the other six (`ContainerLifecycleTest`, the gotcha test) do not ask for reuse.

How it works: Testcontainers hashes the container's configuration and labels the container
`org.testcontainers.hash=…`. A later run with the **same** configuration finds the labelled container and
attaches. **Change anything** — the image tag, an environment variable, a `withX(...)` call — and the hash
changes, so a new container is started (and the old one is left running).

**What it buys — measured on the reference machine:**

| Run | Without reuse | With reuse |
| --- | --- | --- |
| one class, `./mvnw test -Dtest=PostgresWiringTest` | 11.0 s / 12.7 s | 9.5 s / 8.9 s |
| full suite, twice | (37.0–40.9 s) | 35.4 s / 37.5 s — both 221 green |

So reuse saves **~2–3 s per run** — one container start plus readiness — which matters when you run one
class twenty times while writing it. On the full suite the gain is lost in the noise: that run is
dominated by contexts and tests, and starts only one shared container anyway.

**The costs — know them before you switch it on:**

- **Data persists between runs.** A reused container is the *same* database as last time: yesterday's rows
  are still there, and so is the schema (`ddl-auto=update` keeps it). StockFlow's suite tolerates that —
  registration accepts 201 or 409, the Lab 10 scripts clean the catalogue — and that is the **only**
  reason it stays green on run 2. A suite that assumes an empty database at start will not.
- **Ryuk does not remove it.** A reused container keeps running after the JVM exits, using memory, until
  you remove it yourself:

  ```bash
  docker ps --filter label=org.testcontainers.hash     # find it
  docker rm -f <id>                                     # remove it
  ```

- **When you need a fresh container:** after changing an entity (an old column or constraint survives
  `update`), when a test fails in a way that might be leftover data, before measuring anything, and
  whenever you want to know the suite passes from a clean slate. Remove the container, or turn the
  property off.
- **Never in CI.** A CI build must start from nothing every time; that is its whole value. CI machines do
  not have your `~/.testcontainers.properties`, so `withReuse(true)` stays a no-op there — which is
  exactly why it is safe to leave in the code. Reuse is a **personal, local** convenience, and the
  shipped suite never needs it.

---

## 6. Reading the evidence: what a healthy run looks like

The reference numbers, from one `./mvnw test` on the reference machine:

| Measure | Value |
| --- | --- |
| tests | **221** = 208 carried from Lab 10 + 13 new in 4 classes (4 + 3 + 3 + 3), 0 skipped |
| Spring Boot contexts (`Started … in` lines) | **9**: 1 `@DataJpaTest`, 6 `@WebMvcTest`, 2 integration |
| `postgres:16` starts (`Container postgres:16 started in` lines) | **7**: 1 shared + 5 `ContainerLifecycleTest` + 1 gotcha test 3 — plus 1 Ryuk |
| one container start | ≈ 1.0–1.3 s |
| `./mvnw test` total | 37.0–40.9 s |

The context lines look like Lab 10's, with one name changed: surefire on Windows ran `containers.*`
first, so the `@DataJpaTest` context is now named after the first database class to need it:

```
Started PostgresWiringTest in …            ← the @DataJpaTest slice, shared by all 8 repository-style classes
Started AdminControllerTest in …           ← 6 @WebMvcTest slices, as in Lab 10
…
Started AuthorizationMatrixTest in …       ← RANDOM_PORT
Started TransactionalRollbackIsolationTest in …   ← MOCK
```

The container lines follow the same class order. In a full run the **first** `postgres:16` lines belong
to `ContainerLifecycleTest` (`containers.C…` sorts first): Ryuk, then its five containers. The shared
container starts next, when `PostgresWiringTest` initialises the base class — and for it the reuse `WARN`
appears after `Creating container for image: postgres:16` and **before** `Container postgres:16 is
starting` (§2.4). The gotcha test's one container comes after that.

A word on the total: Lab 10's 19 s was the *test time* on H2; Maven's "Total time" also includes the build,
and now includes container starts. Do not compare the two numbers naively — compare **singleton vs
per-class** on the same machine, as §5.4 does.

Signs of trouble:

- **More than 7 `started in` lines** — someone added a container, or the base class's container is
  being started per class.
- **More than 9 `Started … in` lines** — a subclass is adding annotations (Lab 10's rule still holds), or
  `@DirtiesContext` crept in.
- **`Connection to localhost:NNNNN refused`** after a 30 s hang, in a class that reuses a cached context
  while the first class of that context passed — `@Container` on a shared base (§5.3). Ctrl+C.
- **`Failed to replace DataSource with an embedded database for tests`** — a `@DataJpaTest` class is
  missing `extends AbstractPostgresContainerTest` (§4.3).
- **`relation "…" does not exist`** — `ddl-auto` is back to its non-embedded default, `none` (§4.4).
- **`ExceptionInInitializerError` then a cascade of `NoClassDefFoundError: Could not initialize class
  …AbstractPostgresContainerTest`** — Docker is not running (§2.5).
- **A test that passes on the second run but not the first, or only with reuse on** — it depends on
  leftover data (§5.6).
- **`PostgreSQLContainer<?>`** anywhere — the deprecated class (§3.2).

---

## Checklist: you are ready to build Lab 11 when you can...

- [ ] Name at least four verified H2-vs-PostgreSQL differences, and say which two give a *different
      answer* instead of an error.
- [ ] Name at least three commonly quoted "differences" that were the **same** on H2 2.4.240 and
      PostgreSQL 16.
- [ ] Explain the aborted-transaction state (25P02), why `.rootCause()` reports 23505, and why
      StockFlow's pre-check design never triggers it.
- [ ] Describe a container's lifecycle — pull, create, start, wait, random mapped port, stop — and what
      Ryuk is for.
- [ ] Say what `./mvnw test` does without Docker, and why later classes fail with `NoClassDefFoundError`.
- [ ] Write the three Testcontainers 2.x dependencies without versions, and say why the 1.x names fail.
- [ ] Tell the new `org.testcontainers.postgresql.PostgreSQLContainer` from the deprecated one at a glance.
- [ ] Explain, with `ContainerLifecycleTest` as evidence, how a static and an instance `@Container` field
      differ, and how many containers that class starts.
- [ ] Explain how `@ServiceConnection` produces `JdbcConnectionDetails`, and what `@DynamicPropertySource`
      used to do instead.
- [ ] Say why `@DataJpaTest` needs no `@AutoConfigureTestDatabase` in Boot 4, and why the test
      `application.properties` must set `ddl-auto=update`.
- [ ] State the `@Bean` gotcha correctly: declared return type decides, `ConnectionDetailsNotFoundException`,
      a name does not help, the precise type fixes it — and why a field behaves differently.
- [ ] Use `ApplicationContextRunner` to assert that a context fails to start.
- [ ] Write the singleton base class and explain why it has **no** `@Testcontainers` / `@Container`.
- [ ] Explain the "Connection refused" failure of `@Container` on a shared base, and what per-class
      *done right* costs (20 contexts / 20 containers vs 9 / 7).
- [ ] Explain what sharing one database across three contexts changes for test isolation.
- [ ] Turn reuse on and off, find and remove a reused container, and say when reuse is unsafe — and why
      it never belongs in CI.

Now build it: **`LAB_11_TESTCONTAINERS.md`**.
