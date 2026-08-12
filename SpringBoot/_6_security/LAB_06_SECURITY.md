# Lab 06 — Spring Security Fundamentals: Authentication, Authorization & BCrypt

> **Hand this single document to the student.** It is fully self-contained: it specifies *what* to
> add (the requirements, the exact API contract, the **authorization matrix**, and a
> **class-by-class implementation checklist** in Section 6) and gives *guidance* on *how* to
> approach the genuinely new concepts. A student can implement the entire feature from this file
> alone, starting from a finished Lab 5 project.

---

## 1. Problem Statement & Goal

Through Labs 1–5 the **Bookstore REST API** has been completely **open**: anyone who can reach the
server can create, update, or delete any book, author, publisher, or category. That is fine for a
teaching data layer and unacceptable for anything real. Lab 6 is an **increment on the Lab 5
Bookstore** (the `_5_mapstruct` project — four entities, MapStruct mappers, projections, search,
pagination, `PagedResponse`, `ProblemDetail` errors): you add the first real **authentication and
authorization** layer, turning open CRUD into a system with **registered users**, **logins**, and
**protected write operations**.

Here is the twist that motivates the whole lab: **the moment you add the Spring Security starter to
the pom, Spring Boot 4 / Spring Security 7 secures *every* endpoint by default and turns on CSRF
aggressively.** Your Lab 5 endpoints immediately start returning `401`/`403` and your tests go red.
That breakage is not a bug — it is *secure-by-default*, and undoing it *intentionally* (rather than
by accident) is the skill this lab teaches. You will write **one explicit `SecurityFilterChain`
bean** that re-opens exactly the traffic you mean to open and locks down the rest:

- **Public**: all `GET` reads and the new `POST /api/auth/register` endpoint.
- **Any authenticated user** (USER or ADMIN): book writes (`POST`/`PUT`/`DELETE` on `/api/books`).
- **ADMIN only**: catalog administration — `POST`/`PUT`/`DELETE` on `/api/categories`,
  `/api/authors`, `/api/publishers`.

You will back this with a **`User` JPA entity** (username, **BCrypt-hashed** password, roles), a
custom **`UserDetailsService`** that loads users from the database and maps roles to authorities, a
**`BCryptPasswordEncoder`** bean, a **registration** endpoint, an exposed **`AuthenticationManager`**
bean (which Lab 7 will call directly), and a **seeded initial ADMIN** so the catalog is administrable
out of the box.

You will deliver this added to your Lab 5 codebase, matching the **API Contract and authorization
matrix** in Section 12, backed by the same PostgreSQL database, with automated tests at four layers.
The base package for this lab is **`com.example._6_security`**.

## 2. Learning Objectives (what this assesses)

1. Explain **how Spring Security actually works**: every request passes through a chain of servlet
   filters (`DelegatingFilterProxy` → `FilterChainProxy` → your `SecurityFilterChain`) before it
   reaches a controller, and a `SecurityFilterChain` **bean** is how you plug your rules into that
   pipeline.
2. Distinguish **authentication** (proving who you are) from **authorization** (what you may do), and
   describe how the `Authentication`, the `SecurityContext`, and `SecurityContextHolder` carry the
   principal through one request.
3. Write a modern **`SecurityFilterChain`** in **Spring Security 7** from a `HttpSecurity` object
   using `authorizeHttpRequests` + `requestMatchers`, and recognise that `authorizeRequests`,
   `antMatchers`, and `WebSecurityConfigurerAdapter` are all **absent** from the Spring Security 7
   stack used here (`WebSecurityConfigurerAdapter` and the `antMatchers`/`mvcMatchers`/`regexMatchers`
   methods were removed in Spring Security **6.0** — deprecated in 5.7/5.8; `authorizeRequests` was
   removed in **7.0**) — old tutorials using any of them will not compile on this stack.
4. Store passwords correctly: never plaintext; wire a **`BCryptPasswordEncoder`** bean and use
   `encode` on registration and `matches` on login (BCrypt is salted and adaptive).
5. Implement the **`UserDetails` / `UserDetailsService`** contract against a JPA table, adapting your
   `User` entity into a Spring Security `UserDetails` and mapping each role to a `ROLE_<name>`
   **authority**.
6. Understand the **`AuthenticationManager` / `DaoAuthenticationProvider`** wiring (your
   `UserDetailsService` + `PasswordEncoder`), and **expose** the `AuthenticationManager` as a bean.
7. Secure URLs by **HTTP method and role/authority** with `hasRole` / `authenticated` /
   `permitAll`, and explain the **`ROLE_` prefix convention** (`hasRole("ADMIN")` matches the
   authority `ROLE_ADMIN`).
8. Make a **deliberate CSRF decision** (disabled here, for a non-browser API authenticating with an
   explicit `Authorization` header) and know it is revisited with the stateless-token rationale in
   Lab 7.
9. Test a secured web layer: `@WebMvcTest` slices that **import** the real `SecurityConfig`, mock the
   `UserDetailsService`, and use **`@WithMockUser`** to assert `401` anonymous, `USER` write OK, and
   `403` on ADMIN-only endpoints for a plain USER.

**Assumed background:** you have a working Lab 5 Bookstore (or are starting from the `_5_mapstruct`
reference) with the four entities, MapStruct mappers, projections, `GET /api/books/search`,
`GET /api/books/summaries`, pagination, `PagedResponse`, and `ProblemDetail` errors. This document
re-states the Lab 5 pieces you must touch so you can build the increment even if the details are
rusty.

## 3. The One Big New Idea: A Filter Chain That Decides Who May Do What

Before your controller ever runs, the request travels through a **chain of servlet filters**. Spring
Security inserts its own chain (via a `DelegatingFilterProxy` that delegates to a `FilterChainProxy`)
into that pipeline. The chain is configured by a single **`SecurityFilterChain` bean** you declare
from a `HttpSecurity` builder. Inside it you answer three questions:

1. **Which requests are public?** — `requestMatchers(...).permitAll()`
2. **Which require *any* logged-in user?** — `requestMatchers(...).authenticated()`
3. **Which require a *specific role*?** — `requestMatchers(...).hasRole("ADMIN")`

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
            // ... method+role rules ...
            .anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults())
        .formLogin(Customizer.withDefaults());
    return http.build();
}
```

The second half of the idea is **where the users come from**. When a request presents credentials
(via HTTP Basic), Spring Security's `DaoAuthenticationProvider` calls **your**
`UserDetailsService.loadUserByUsername(...)`, which loads the `User` row from **your** database and
adapts it into a `UserDetails` — including the user's **authorities**, derived from its roles with
the **`ROLE_` prefix** (`Role.ADMIN` → authority `ROLE_ADMIN`). The `PasswordEncoder` then compares
the presented password to the stored **BCrypt hash**. Rules (`hasRole("ADMIN")`) and authorities
(`ROLE_ADMIN`) line up precisely because of that prefix convention. That is the entire mechanism.

## 4. What's New vs. Lab 5

Lab 6 **adds** a small, well-defined set of files and touches **no existing controller, service,
mapper, entity, or repository** in the Bookstore domain. All of Lab 5 stays byte-for-byte behaviour
(the *responses* are identical; only *access* changes).

| Area | Change |
| ---- | ------ |
| `pom.xml` | Adds `spring-boot-starter-security` (main) and `spring-security-test` (test). |
| `model` | **NEW** `User` entity and `Role` enum. |
| `repository` | **NEW** `UserRepository`. |
| `security` (**NEW package**) | **NEW** `UserDetailsServiceImpl`, `SecurityConfig`, `AdminUserSeeder`. |
| `dto` | **NEW** `RegisterRequest` and `UserResponse`. |
| `service` | **NEW** `AuthService` / `AuthServiceImpl`. |
| `controller` | **NEW** `AuthController` (`POST /api/auth/register`). |
| `exception` | **Reuses** the existing `DuplicateResourceException` → `409` for a taken username. |
| `application.properties` | Adds `app.security.admin-password` (default `admin12345`). |
| `test` | **NEW** `UserRepositoryTest`, `UserDetailsServiceImplTest`, `AuthServiceImplTest`, `AuthControllerTest`; existing controller slice tests are **updated** to import `SecurityConfig`, mock `UserDetailsService`, and add `@WithMockUser` / anonymous / forbidden cases. |

No changes to the `Book`/`Author`/`Publisher`/`Category` entities, DTOs, mappers, projections, or the
`specification` package. The base package for this lab is **`com.example._6_security`**.

## 5. Tech Stack & Project Setup (required)

Same stack as Lab 5 — **Java 21**, **Maven**, **Spring Boot 4.0.6**, **PostgreSQL** at runtime, **H2**
(test scope) for tests, **Lombok**, **MapStruct 1.6.3** — plus **Spring Security 7** (via
`spring-boot-starter-security`). Continue with the layered architecture
(`controller → service → repository → entity`, plus `dto`, `exception`, `specification`, `mapper`,
`projection`, and now `security`).

Add two dependencies to the pom (the MapStruct processor paths from Lab 5 stay exactly as they are):

```xml
<!-- Main: Spring Security 7. Adding this starter secures EVERY endpoint by default
     (secure-by-default) and turns on CSRF, so the pre-existing Bookstore endpoints start
     returning 401/403 until an explicit SecurityFilterChain is written — that breakage is
     the whole motivation for this lab. -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Test: @WithMockUser, SecurityMockMvcConfigurers.springSecurity(), the security
     MockMvc request post-processors used by the slice tests. -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 5.1 Spring Boot 4.0 specifics you MUST know (or you will get stuck)

Unchanged from Lab 5 but still trip people up:

- **Test slices live in dedicated starters** (`spring-boot-starter-data-jpa-test`,
  `spring-boot-starter-webmvc-test`); `spring-boot-starter-test` no longer brings them transitively.
- **The test-slice imports are renamed** (NOT the 3.x packages — see Section 14.1):
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
  `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`,
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.
- **Jackson 3**: the `ObjectMapper` is `tools.jackson.databind.ObjectMapper` (not `com.fasterxml...`).

### 5.2 Spring Boot 4.0 / Spring Security 7 specifics you MUST know (the REMOVED APIs)

The Spring Security 7 stack this lab runs on has dropped a large surface of the old 5/6 configuration
API across the 6.0 and 7.0 releases. **Every one of these is absent from this stack — code using them
will not compile**, which is why the vast majority of online Spring Security tutorials are wrong for
this lab:

- **`WebSecurityConfigurerAdapter` was removed in Spring Security 6.0** (deprecated in 5.7). There is
  **no base class to extend**. You configure security by declaring a **`SecurityFilterChain` `@Bean`**
  that takes a `HttpSecurity` and returns `http.build()`. (This "component, not adapter" model has
  been the recommendation since Security 5.7 and is now the *only* way.)
- **`authorizeRequests(...)` was removed in Spring Security 7.0.** Use **`authorizeHttpRequests(...)`**
  (note the `Http`).
- **`antMatchers(...)` / `mvcMatchers(...)` / `regexMatchers(...)` were removed in Spring Security 6.0.**
  Use **`requestMatchers(...)`** — with an overload that takes an `HttpMethod` first
  (`requestMatchers(HttpMethod.POST, "/api/books/**")`).
- **The lambda DSL is mandatory.** The old chained-and-`.and()` style is gone; every configurer takes
  a `Customizer` lambda (`http.httpBasic(Customizer.withDefaults())`, `http.csrf(csrf ->
  csrf.disable())`, `http.authorizeHttpRequests(auth -> auth. ...)`).
- **Secure-by-default.** In Spring Boot 4.0, adding the security starter secures **all** endpoints
  and enables CSRF; you *must* provide a `SecurityFilterChain` to open anything up. There is no
  "security is off until you turn it on" — it is the opposite.
- **Obtaining the `AuthenticationManager`:** inject the framework-provided
  `AuthenticationConfiguration` and return `configuration.getAuthenticationManager()` from a `@Bean`.
  (The old `AuthenticationManagerBuilder` / `configure(AuthenticationManagerBuilder)` override is
  gone with the adapter.)
- **`@EnableWebSecurity`** is declared explicitly on your config class. In the running app the
  starter would auto-configure it, but declaring it makes the `HttpSecurity` prototype bean and the
  `AuthenticationConfiguration` available **inside the `@WebMvcTest` slices** that `@Import` your
  `SecurityConfig` (the web slice does not pull in the security auto-configuration on its own —
  Section 6.9).

### 5.3 The deliberate CSRF decision (read this before you disable it)

You will call `.csrf(csrf -> csrf.disable())`. That is an **intentional, documented** decision, not a
shortcut — comment it as such. CSRF protection defends **cookie/session-authenticated browser form
posts**, where the browser attaches the session cookie *automatically* and a malicious page can forge
a state-changing request. This API is exercised by **non-browser clients authenticating with HTTP
Basic** — an explicit `Authorization` header that a cross-site page **cannot silently attach** — so
CSRF tokens would add friction without adding protection here. The full CSRF treatment and the
stateless-token rationale are **deferred to Lab 7** (JWT). Leave the one-line disable with a comment
explaining *why*; do not silently drop it.

## 6. Complete Implementation Inventory (class-by-class checklist)

This section lists **every Java class you create or change** for Lab 6. Use it as your build
checklist. Sections 7–14 explain the harder concepts; this section tells you **what each class is for
and what it must do** — not how to code it (that is your job). Classes not listed here are
**unchanged from Lab 5**.

### 6.1 `model` package — two NEW types

#### `Role` — an enum of the two Bookstore authorities

A plain enum with exactly two constants. Stored as strings in the roles table and mapped to Spring
Security authorities with the `ROLE_` prefix (so `Role.ADMIN` → `ROLE_ADMIN`).

```java
public enum Role {
    USER,
    ADMIN
}
```

#### `User` — the application user (a JPA `@Entity`)

A Lombok-annotated entity (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`) mapped
to table **`users`**. The `password` column **always holds a BCrypt hash, never plaintext** — hashing
happens in `AuthServiceImpl` before persistence.

| Field | Type | Mapping | Notes |
| ----- | ---- | ------- | ----- |
| `id` | `Long` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | surrogate key |
| `username` | `String` | `@Column(nullable = false, unique = true)` | the login name; include it in `@EqualsAndHashCode` |
| `password` | `String` | `@Column(nullable = false)` | the **BCrypt hash** |
| `roles` | `Set<Role>` | `@ElementCollection(fetch = FetchType.EAGER)` + `@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))` + `@Enumerated(EnumType.STRING)` + `@Column(name = "role", nullable = false)` | roles live in a dedicated join table |

Details that matter:

- Use **`@EqualsAndHashCode(onlyExplicitlyIncluded = true)`** on the class and
  **`@EqualsAndHashCode.Include`** on `username` (identity by natural key, not by every field).
- Use **`@Builder.Default`** on `roles` initialised to `new HashSet<>()` so the builder does not
  reset it to `null`.
- **`fetch = FetchType.EAGER`** on the roles is deliberate: authorities must be available the moment
  the user is loaded for authentication (the `UserDetailsService` reads them immediately, outside any
  open session in the test slices).

### 6.2 `repository` package — one NEW interface

#### `UserRepository` — `extends JpaRepository<User, Long>`

Two derived queries the auth machinery needs:

| Method (exact signature) | Used by |
| ------------------------ | ------- |
| `Optional<User> findByUsername(String username);` | `UserDetailsServiceImpl.loadUserByUsername` |
| `boolean existsByUsername(String username);` | `AuthServiceImpl.register` (duplicate check) and `AdminUserSeeder` (idempotent seed) |

### 6.3 `security` package — the heart of the lab (three NEW classes; this package is new)

#### `UserDetailsServiceImpl` — bridges your `User` table to Spring Security

A `@Service` implementing `org.springframework.security.core.userdetails.UserDetailsService`, with
`@RequiredArgsConstructor` injecting the `UserRepository`. The `DaoAuthenticationProvider` calls
`loadUserByUsername` during authentication.

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

It must:

1. Load the user by username, throwing **`UsernameNotFoundException`** (a Spring Security type) when
   absent — **not** your own `ResourceNotFoundException` (the provider expects this specific type).
2. Map **each `Role` to a `SimpleGrantedAuthority("ROLE_" + role.name())`** — this `ROLE_` prefix is
   what makes `hasRole("ADMIN")` in `SecurityConfig` line up with `Role.ADMIN`.
3. Return the framework's `org.springframework.security.core.userdetails.User` as the `UserDetails`
   adapter, carrying the **stored BCrypt hash** as the password (the provider compares against it).

> **Do not confuse the two `User` classes.** `com.example._6_security.model.User` is *your* JPA
> entity; `org.springframework.security.core.userdetails.User` is the framework's `UserDetails`
> adapter. Fully-qualify the framework one (as above) to keep them distinct.

#### `SecurityConfig` — the single `SecurityFilterChain` + the three security beans

A `@Configuration` annotated **`@EnableWebSecurity`** (Section 5.2) that declares **three** beans:

| Bean (exact signature) | Purpose |
| ---------------------- | ------- |
| `PasswordEncoder passwordEncoder()` | `return new BCryptPasswordEncoder();` — used to `encode` on register and `matches` on login. |
| `SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception` | The access rules (the authorization matrix, Section 12.2), plus `httpBasic` and `formLogin`. |
| `AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception` | `return configuration.getAuthenticationManager();` — exposes the manager so it is injectable. Lab 6 does not call it directly (httpBasic/formLogin drive authentication); **Lab 7's JWT login will**. |

The `securityFilterChain` body, using the **Spring Security 7 lambda DSL only**:

```java
http
    .csrf(csrf -> csrf.disable())               // DELIBERATE — see Section 5.3
    .authorizeHttpRequests(auth -> auth
        // Public: self-service registration + all read traffic
        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

        // Catalog administration: only ADMIN may mutate categories/authors/publishers
        .requestMatchers(HttpMethod.POST, "/api/categories/**", "/api/authors/**", "/api/publishers/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.PUT, "/api/categories/**", "/api/authors/**", "/api/publishers/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/authors/**", "/api/publishers/**").hasRole("ADMIN")

        // Book writes: any authenticated user (USER or ADMIN)
        .requestMatchers(HttpMethod.POST, "/api/books/**").authenticated()
        .requestMatchers(HttpMethod.PUT, "/api/books/**").authenticated()
        .requestMatchers(HttpMethod.DELETE, "/api/books/**").authenticated()

        // Everything else must be authenticated
        .anyRequest().authenticated())
    .httpBasic(Customizer.withDefaults())        // primary path for API/non-browser clients (401 on anonymous)
    .formLogin(Customizer.withDefaults());        // default browser form login
return http.build();
```

Rule-ordering point: **matchers are evaluated top-to-bottom, first match wins.** The two `permitAll`
rules (register, then all `GET`) come **first** so reads are public regardless of the method-specific
rules below. The `.anyRequest().authenticated()` at the bottom is the safety net for anything not
explicitly listed.

Imports you will need (all Spring Security 7 / Spring Framework):
`org.springframework.security.web.SecurityFilterChain`,
`org.springframework.security.config.annotation.web.builders.HttpSecurity`,
`org.springframework.security.config.annotation.web.configuration.EnableWebSecurity`,
`org.springframework.security.config.Customizer`,
`org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration`,
`org.springframework.security.authentication.AuthenticationManager`,
`org.springframework.security.crypto.password.PasswordEncoder`,
`org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`,
`org.springframework.http.HttpMethod`.

#### `AdminUserSeeder` — seeds one ADMIN at startup

A `@Component` implementing **`ApplicationRunner`** (with `@RequiredArgsConstructor` for the
`UserRepository` + `PasswordEncoder`, and `@Slf4j`). On startup it creates a single `admin` account
so the catalog-administration endpoints are usable out of the box.

| Detail | Requirement |
| ------ | ----------- |
| Password source | `@Value("${app.security.admin-password:admin12345}")` — configurable, with a documented default |
| Idempotency | `if (userRepository.existsByUsername("admin")) return;` before creating |
| The user | `User.builder().username("admin").password(passwordEncoder.encode(adminPassword)).roles(new HashSet<>(Set.of(Role.ADMIN))).build()`, then `save` |
| Logging | `log.info("Seeded default ADMIN user 'admin'")` |

> **Why `ApplicationRunner` `@Component`, not a `@Bean` in `@Configuration`?** The web/JPA test slices
> (`@WebMvcTest`, `@DataJpaTest`) **do not scan arbitrary `@Component`s**, so the seeder never runs
> during those tests and cannot pollute their contexts. It runs only in the full application.

### 6.4 `dto` package — two NEW classes

#### `RegisterRequest` — inbound payload for `POST /api/auth/register`

A Lombok class (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor`) with Jakarta Validation. The
raw password is validated for a minimum length here, then BCrypt-hashed in the service and never
stored or returned as-is.

| Field | Type | Validation |
| ----- | ---- | ---------- |
| `username` | `String` | `@NotBlank(message = "Username is required")` |
| `password` | `String` | `@NotBlank(message = "Password is required")` + `@Size(min = 8, message = "Password must be at least 8 characters")` |

#### `UserResponse` — outbound view of a user

A Lombok class carrying **`id`, `username`, `Set<Role> roles`** — and **deliberately no password
field**: the BCrypt hash must never cross the API boundary. DTO-only, as everywhere else in the
Bookstore — the `User` entity is never serialized directly.

### 6.5 `service` package — `AuthService` / `AuthServiceImpl` (NEW)

#### `AuthService` (interface) — one method

```java
UserResponse register(RegisterRequest request);
```

#### `AuthServiceImpl` — `@Service @RequiredArgsConstructor @Transactional(readOnly = true)`

Injects `UserRepository` and `PasswordEncoder`. The `register` method is annotated `@Transactional`
(a write) and must:

1. **Reject duplicates**: `if (userRepository.existsByUsername(request.getUsername()))` → throw the
   existing **`DuplicateResourceException("username", request.getUsername())`** (the shared exception,
   which the `GlobalExceptionHandler` already renders as a `409` `ProblemDetail` titled
   `"Duplicate value"` — no new handler needed).
2. **Hash the password**: build the `User` with
   `password(passwordEncoder.encode(request.getPassword()))` — **never** store the raw password.
3. **Assign the default role**: self-registered users always start with **`Role.USER`**
   (`roles(new HashSet<>(Set.of(Role.USER)))`) — ADMIN is only ever granted by the seeder, never by
   self-registration.
4. **Save and map**: `User saved = userRepository.save(user);` then return
   `new UserResponse(saved.getId(), saved.getUsername(), saved.getRoles())`.

### 6.6 `controller` package — `AuthController` (NEW)

A `@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor` injecting `AuthService`.
One handler:

| Handler signature | Mapping | Returns |
| ----------------- | ------- | ------- |
| `register(@Valid @RequestBody RegisterRequest request)` | `@PostMapping("/register")` | `201 CREATED`, `UserResponse` body |

Body: `UserResponse created = authService.register(request); return
ResponseEntity.status(HttpStatus.CREATED).body(created);`. There is **no** custom login endpoint in
Lab 6 — authentication is handled by `httpBasic`/`formLogin`. (Lab 7 adds a JWT `POST /api/auth/login`
here.)

### 6.7 `exception` package — no new classes

Reuse the existing **`DuplicateResourceException`** for a taken username; the existing
`GlobalExceptionHandler` maps it to `409` (Section 6.5). No new exception, no new error code.

### 6.8 `application.properties` — one new property

```properties
# Lab 6: password for the ADMIN account seeded at startup by AdminUserSeeder.
# Default is "admin12345" (seeded credentials: admin / admin12345). Override in real deployments.
app.security.admin-password=admin12345
```

### 6.9 Test classes — four NEW + updated existing slice tests

All must pass with **`./mvnw test`** using H2 (Section 14.1 for the Spring Boot 4.0 import packages).

| Class | Layer | Status | What you must verify |
| ----- | ----- | ------ | -------------------- |
| **`UserRepositoryTest`** | `@DataJpaTest` | NEW | `findByUsername` returns the user with eagerly-loaded roles; `existsByUsername` reflects presence. See 6.9.1. |
| **`UserDetailsServiceImplTest`** | Mockito | NEW | Loads a user and exposes each role as a `ROLE_` authority; throws `UsernameNotFoundException` when absent. See 6.9.2. |
| **`AuthServiceImplTest`** | Mockito | NEW | `register` hashes the password (BCrypt, never raw), assigns default `USER`, and rejects duplicates without saving. See 6.9.3. |
| **`AuthControllerTest`** | `@WebMvcTest` | NEW | `POST /register` runs **anonymously** (permitAll) → `201` with no password field; `409` duplicate; `400` validation. See 6.9.4. |
| **`BookControllerTest`** | `@WebMvcTest` | UPDATED | Import `SecurityConfig`, mock `UserDetailsService`; add anonymous-`401` and `@WithMockUser(roles="USER")` write cases. See 6.9.5. |
| **`CategoryControllerTest`** (and `Author`/`Publisher`) | `@WebMvcTest` | UPDATED | Same wiring; add anonymous-`401`, `@WithMockUser(roles="USER")`→`403`, `@WithMockUser(roles="ADMIN")`→`201` cases. See 6.9.5. |

#### 6.9.1 `UserRepositoryTest` (`@DataJpaTest`)

Persist one user (`username="alice"`, `password="bcrypt-hash"`, `roles=Set.of(Role.USER)`) via
`TestEntityManager`, `flush()` + `clear()`, then assert: `findByUsername("alice")` is present with
`password == "bcrypt-hash"` and `roles` `containsExactly(Role.USER)`; `findByUsername("nobody")` is
empty; `existsByUsername` is `true` for `"alice"` and `false` for `"nobody"`.

#### 6.9.2 `UserDetailsServiceImplTest` (Mockito)

`@ExtendWith(MockitoExtension.class)`, `@Mock UserRepository`, `@InjectMocks UserDetailsServiceImpl`.

- **loads roles as authorities**: stub `findByUsername("bob")` → a `User` with
  `roles = Set.of(Role.USER, Role.ADMIN)`; assert the returned `UserDetails` has `getUsername() ==
  "bob"`, `getPassword() == "bcrypt-hash"`, and its authorities
  `extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN")`.
- **throws when absent**: stub `findByUsername("ghost")` → `Optional.empty()`; assert
  `loadUserByUsername("ghost")` throws `UsernameNotFoundException` with a message containing `"ghost"`.

#### 6.9.3 `AuthServiceImplTest` (Mockito)

`@Mock UserRepository`; use a **real** `BCryptPasswordEncoder` (`private final PasswordEncoder
passwordEncoder = new BCryptPasswordEncoder();`) and build `service = new
AuthServiceImpl(userRepository, passwordEncoder)` in `@BeforeEach`.

- **registerSuccess**: stub `existsByUsername("alice")` → `false` and `save(any())` to set id `1L` and
  return the arg; assert the `UserResponse` has `id == 1L`, `username == "alice"`, and
  `roles.containsExactly(Role.USER)`.
- **registerHashesPassword**: capture the saved `User` with an `ArgumentCaptor<User>`; assert
  `saved.getPassword()` **is not** `"password123"` and `passwordEncoder.matches("password123",
  saved.getPassword())` **is true** (proves a real BCrypt hash was stored).
- **registerDuplicate**: stub `existsByUsername("alice")` → `true`; assert `register(...)` throws
  `DuplicateResourceException` (message contains `"username"`) and `verify(userRepository,
  never()).save(any())`.

#### 6.9.4 `AuthControllerTest` (`@WebMvcTest(AuthController.class)`)

`@Import(SecurityConfig.class)`; `@MockitoBean AuthService`; **also** `@MockitoBean UserDetailsService`
(the imported `SecurityConfig` builds a `DaoAuthenticationProvider` that needs one — the slice does
not load the real `@Service` impl). Build `MockMvc` from the `WebApplicationContext` with
`SecurityMockMvcConfigurers.springSecurity()` applied (Section 6.9.6). Because `POST /api/auth/register`
is `permitAll`, these run **anonymously (no `@WithMockUser`)**:

- **registerSuccess**: stub `register(any())` → `new UserResponse(1L, "alice", Set.of(Role.USER))`;
  `POST /api/auth/register` with `{"username":"alice","password":"password123"}` → `201`,
  `$.id == 1`, `$.username == "alice"`, `$.roles[0] == "USER"`, and **`$.password` does not exist**.
- **registerDuplicate**: stub `register` to throw `DuplicateResourceException("username","alice")` →
  `409`, `$.title == "Duplicate value"`.
- **registerBlankUsername**: `username=""` → `400`, `$.errors.username == "Username is required"`.
- **registerShortPassword**: `password="short"` → `400`,
  `$.errors.password == "Password must be at least 8 characters"`.

#### 6.9.5 Updated slice tests — the `@WebMvcTest`-security gotcha

By default, `@WebMvcTest` does **not** apply your `SecurityConfig` (the web slice omits the security
auto-configuration). If you leave the existing Lab 5 slice tests as-is, they will either run
**unsecured** (proving nothing about your rules) or fail to build the context. Fix every controller
slice test the same way:

1. Add **`@Import(SecurityConfig.class)`** to the test class so the slice enforces the **real** rules.
2. Add a **`@MockitoBean UserDetailsService userDetailsService;`** — `SecurityConfig`'s
   `AuthenticationManager`/`DaoAuthenticationProvider` requires one, and the slice does not load the
   `@Service` impl.
3. Build `MockMvc` from the context with `springSecurity()` applied (Section 6.9.6) so the filter
   chain runs **and** `@WithMockUser`'s `SecurityContext` is bridged into each request.
4. Use **`@WithMockUser`** to simulate an authenticated principal per test method:
   `@WithMockUser(roles = "USER")` or `@WithMockUser(roles = "ADMIN")`. A method with **no**
   `@WithMockUser` is anonymous.

Add these representative cases (they are the acceptance evidence for the matrix):

- **`BookControllerTest`**: `POST /api/books` with **no** `@WithMockUser` → **`401`**;
  `@WithMockUser(roles = "USER")` `POST` → **`201`** (book writes allow any authenticated user);
  `@WithMockUser(roles = "USER")` `DELETE /api/books/{id}` → **`204`**. All `GET` cases stay
  **anonymous** and still return `200` (reads are public).
- **`CategoryControllerTest`** (mirror in `AuthorControllerTest`, `PublisherControllerTest`):
  `POST /api/categories` anonymous → **`401`**; `@WithMockUser(roles = "USER")` `POST` → **`403`**
  (catalog writes are ADMIN-only); `@WithMockUser(roles = "ADMIN")` `POST` → **`201`**;
  `@WithMockUser(roles = "ADMIN")` `DELETE` → **`204`**. `GET` cases stay anonymous → `200`/`404`.

> **RETROFIT EVERY PRE-EXISTING WRITE TEST — do not skip this.** It is not enough to *add* the new
> anonymous-`401` and forbidden-`403` cases: **every write-path test method you already had from
> Lab 5** — `create`, `update`, and `delete` on the **book** controller *and* on the **catalog**
> controllers (`Category`/`Author`/`Publisher`), **including the validation-`400` and
> duplicate-`409` cases** — must now carry an **authorized** `@WithMockUser`:
> `@WithMockUser(roles = "ADMIN")` for the catalog controllers, `@WithMockUser(roles = "USER")` for
> the book controller. The reason is ordering: the security filter chain runs **before** the
> controller, so an unauthenticated (or under-privileged) request is rejected with `401`/`403` and
> **never reaches** the `400`/`409` business logic the test asserts. A student who only adds the new
> authz cases and leaves the old write tests un-annotated gets a **red bar** — those pre-existing
> tests now see `401`/`403` instead of the `201`/`400`/`409` they expect.

> **`@WithMockUser(roles = "ADMIN")` and the `ROLE_` prefix:** `@WithMockUser` **adds the `ROLE_`
> prefix for you** — `roles = "ADMIN"` grants authority `ROLE_ADMIN`, which is exactly what
> `hasRole("ADMIN")` checks. Do **not** write `roles = "ROLE_ADMIN"` (that would become
> `ROLE_ROLE_ADMIN`). Use `authorities = "..."` only if you want to set a raw authority string.

#### 6.9.6 The MockMvc-with-security setup (shared by every slice test)

Every secured slice test builds `MockMvc` the same way so both the security filter chain and
`@WithMockUser` work:

```java
@Autowired private WebApplicationContext context;
private MockMvc mockMvc;

@BeforeEach
void setUpMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())   // installs the security filter chain
            .build();
}
```

> **First, REMOVE the old `@Autowired MockMvc` field.** The Lab 5 slice tests injected `MockMvc`
> directly (`@Autowired private MockMvc mockMvc;`, provided by the slice's auto-configured `MockMvc`).
> **Delete that field** and replace it with the `@Autowired WebApplicationContext context` field plus
> the `@BeforeEach`-built `mockMvc` above. The two must **not** coexist: an auto-configured `MockMvc`
> does **not** have `springSecurity()` applied, so if it lingers — or is used instead of the
> hand-built one — the security filter chain never runs and your `401`/`403` assertions silently pass
> for the wrong reason. There must be exactly **one** `mockMvc` in the class: the one built in
> `@BeforeEach`.

### 6.10 Suggested file count for this increment

| Package | Files |
| ------- | ----- |
| `model` | +2 (`User`, `Role`) |
| `repository` | +1 (`UserRepository`) |
| `security` (new) | +3 (`UserDetailsServiceImpl`, `SecurityConfig`, `AdminUserSeeder`) |
| `dto` | +2 (`RegisterRequest`, `UserResponse`) |
| `service` | +2 (`AuthService`, `AuthServiceImpl`) |
| `controller` | +1 (`AuthController`) |
| `pom.xml` / `application.properties` | edits |
| `test` | +4 (`UserRepositoryTest`, `UserDetailsServiceImplTest`, `AuthServiceImplTest`, `AuthControllerTest`); edit the existing controller slice tests |
| **Total new files** | **11 new main + 4 new test Java files**, plus pom/properties edits and updates to the existing slice tests |

---

## 7. The `SecurityFilterChain` (required + guidance)

### 7.1 Why one bean replaces the old adapter

Spring Security 7 removed `WebSecurityConfigurerAdapter`. Instead of *extending* a base class and
overriding `configure(HttpSecurity)`, you *declare* a `SecurityFilterChain` **bean** built from the
`HttpSecurity` the framework hands you. The bean **is** your filter chain; `http.build()` produces it.
This is a component, not an inheritance hook — which is why it composes cleanly and why you can
`@Import` it into a test slice.

### 7.2 `authorizeHttpRequests` + `requestMatchers`, top to bottom

`authorizeHttpRequests` takes a lambda in which you list matchers **in priority order** — the first
matcher that matches a request decides it. Put the broad `permitAll` reads first, then the
method+role rules, then `anyRequest().authenticated()` as the catch-all. Each `requestMatchers`
overload used here takes an **`HttpMethod` first**, so `POST /api/books/**` and `GET /api/books/**`
are distinct rules. `hasRole("ADMIN")` requires the `ROLE_ADMIN` authority; `authenticated()` requires
any logged-in principal; `permitAll()` allows everyone including anonymous.

### 7.3 HTTP Basic and form login

`.httpBasic(Customizer.withDefaults())` enables the primary path for API/non-browser clients: an
anonymous request to a protected endpoint gets a **`401`** with a `WWW-Authenticate: Basic` challenge,
and a client re-sends with an `Authorization: Basic <base64 user:pass>` header.
`.formLogin(Customizer.withDefaults())` adds the default browser login form. Both drive the same
`AuthenticationManager` → `DaoAuthenticationProvider` → your `UserDetailsService` + `PasswordEncoder`.

## 8. Passwords, `UserDetailsService`, and the `ROLE_` mapping (required + guidance)

### 8.1 BCrypt: encode on register, match on login

`BCryptPasswordEncoder` produces a **salted, adaptive** one-way hash. On registration you call
`encode(rawPassword)` and store the result; you **never** store or log the raw password. On login the
provider calls `matches(rawPassword, storedHash)` — you never compare hashes for equality yourself
(each `encode` produces a different salt, so equal passwords have different hashes; only `matches`
knows how to verify).

### 8.2 The `UserDetailsService` contract

`DaoAuthenticationProvider` calls `loadUserByUsername(username)` to obtain a `UserDetails` (username,
password hash, authorities), then compares the presented password to that hash with the
`PasswordEncoder`. Your impl loads the JPA `User`, maps its roles to authorities, and adapts it to a
`UserDetails`. Throwing **`UsernameNotFoundException`** (not a generic exception) is part of the
contract.

### 8.3 Roles vs. authorities — the `ROLE_` prefix convention

Spring Security has **one** concept under the hood: *authorities* (strings). "Roles" are just
authorities that **conventionally start with `ROLE_`**. The convenience methods bake the prefix in:
`hasRole("ADMIN")` checks for authority `ROLE_ADMIN`; `@WithMockUser(roles = "ADMIN")` grants
`ROLE_ADMIN`. Your `UserDetailsServiceImpl` therefore maps `Role.ADMIN` → `new
SimpleGrantedAuthority("ROLE_" + role.name())` = `ROLE_ADMIN`, and everything lines up. If you ever
need a *raw* authority with no prefix, use `hasAuthority("...")` / `@WithMockUser(authorities = "...")`
instead. **Document this mapping** (it is an acceptance item).

## 9. The `AuthenticationManager` (required + guidance)

Expose the `AuthenticationManager` by injecting the framework's `AuthenticationConfiguration` and
returning `configuration.getAuthenticationManager()`. In Lab 6 nothing calls it directly —
`httpBasic`/`formLogin` drive authentication internally — but exposing it now is deliberate: **Lab 7's
JWT login endpoint authenticates the submitted credentials by calling this manager**, so the bean has
to exist and be injectable. This is the seam between Lab 6 and Lab 7.

## 10. Registration Flow (required + guidance)

`POST /api/auth/register` is **public** (`permitAll`) — a user cannot authenticate before they have an
account. The controller validates the `RegisterRequest` (`@Valid`), the service checks for a duplicate
username (→ `409`), BCrypt-hashes the password, assigns **`Role.USER`**, saves, and returns a
`UserResponse` (**never** the entity, **never** the password) with `201`. ADMIN is granted **only** by
the `AdminUserSeeder`, never by self-registration — this is a security invariant, not a convenience.

## 11. Architecture & Code Constraints (required)

- Layered as before: `controller → service → repository → entity`, plus `dto`, `exception`,
  `specification`, `mapper`, `projection`, and the new `security` package.
- The API still exposes **DTOs only** — registration returns `UserResponse`, never the `User` entity,
  and the password hash never crosses the boundary.
- **Authentication/authorization config lives in `security`**; the `UserDetailsService` is the only
  bridge from your `User` table to the framework. Controllers/services contain **no** security logic
  in Lab 6 (method-level security is Lab 8).
- The reads stay public; **writes** are gated by the matrix (Section 12.2). No existing endpoint's
  request/response **body** changes — only its **access**.
- CSRF is disabled **deliberately and documented** (Section 5.3); revisited in Lab 7.

## 12. API Contract (required — the authoritative spec)

### 12.1 `POST /api/auth/register` — NEW

| Method | Path | Auth | Success | Errors |
| ------ | ---- | ---- | ------- | ------ |
| POST | `/api/auth/register` | public (`permitAll`) | `201`, `UserResponse` | `400` validation (blank username / password < 8); `409` username taken |

Request body:

```json
{ "username": "alice", "password": "password123" }
```

Response body (`201`) — **no password field**:

```json
{ "id": 1, "username": "alice", "roles": ["USER"] }
```

`409` body (username taken) is the standard `ProblemDetail`:

```json
{ "title": "Duplicate value", "status": 409, "detail": "username already in use: alice" }
```

### 12.2 Authorization matrix — every existing endpoint

Reads are public; book writes need any authenticated user; catalog (category/author/publisher) writes
need ADMIN. **Anonymous** access to a protected endpoint → **`401`**; an **authenticated but
under-privileged** user (e.g. a plain USER hitting an ADMIN-only endpoint) → **`403`**.

| Endpoint | Anonymous | USER (ROLE_USER) | ADMIN (ROLE_ADMIN) |
| -------- | :-------: | :--------------: | :----------------: |
| `POST /api/auth/register` | ✅ 201 | ✅ 201 | ✅ 201 |
| `GET /api/books`, `GET /api/books/{id}`, `GET /api/books/search`, `GET /api/books/summaries` | ✅ 200 | ✅ 200 | ✅ 200 |
| `POST /api/books` | ❌ 401 | ✅ 201 | ✅ 201 |
| `PUT /api/books/{id}` | ❌ 401 | ✅ 200 | ✅ 200 |
| `DELETE /api/books/{id}` | ❌ 401 | ✅ 204 | ✅ 204 |
| `GET /api/categories`, `GET /api/categories/{id}` | ✅ 200 | ✅ 200 | ✅ 200 |
| `POST /api/categories` | ❌ 401 | ⛔ 403 | ✅ 201 |
| `PUT /api/categories/{id}` | ❌ 401 | ⛔ 403 | ✅ 200 |
| `DELETE /api/categories/{id}` | ❌ 401 | ⛔ 403 | ✅ 204 |
| `GET /api/authors`, `GET /api/authors/{id}` | ✅ 200 | ✅ 200 | ✅ 200 |
| `GET /api/authors/{id}/books` (nested read) | ✅ 200 | ✅ 200 | ✅ 200 |
| `POST` / `PUT` / `DELETE` `/api/authors/**` | ❌ 401 | ⛔ 403 | ✅ 2xx |
| `GET /api/publishers`, `GET /api/publishers/{id}` | ✅ 200 | ✅ 200 | ✅ 200 |
| `GET /api/publishers/{id}/books` (nested read) | ✅ 200 | ✅ 200 | ✅ 200 |
| `POST` / `PUT` / `DELETE` `/api/publishers/**` | ❌ 401 | ⛔ 403 | ✅ 2xx |

The nested read endpoints `GET /api/authors/{id}/books` and `GET /api/publishers/{id}/books` are
public like every other read: they match the `GET /api/**` `permitAll` rule, so no authentication is
required. The **response bodies** of every existing endpoint are exactly as in Lab 5
(`BookResponseDto`, `PagedResponse`, `ProblemDetail`, projections). Lab 6 changes **access**, not shape.

## 13. Business Rules (required)

- **Secure-by-default is intentional.** Only the traffic the `SecurityFilterChain` explicitly opens is
  reachable; everything else requires authentication.
- **Reads are public**: all `GET /api/**` and `POST /api/auth/register` are `permitAll`.
- **Book writes** (`POST`/`PUT`/`DELETE` on `/api/books/**`) require **any authenticated user**.
- **Catalog writes** (`POST`/`PUT`/`DELETE` on `/api/categories`, `/api/authors`, `/api/publishers`)
  require **ADMIN**.
- **Anonymous → 401**, **authenticated-but-forbidden → 403** (distinguish the two).
- Passwords are **BCrypt-hashed** on registration and **never** stored, logged, or returned in
  plaintext; `UserResponse` has **no** password field.
- Self-registration always yields **`Role.USER`**; **ADMIN** is granted only by the seeder.
- Roles map to authorities with the **`ROLE_` prefix** (`Role.ADMIN` → `ROLE_ADMIN`), so
  `hasRole("ADMIN")` works.
- A duplicate username on register → **`409`** (`DuplicateResourceException`).
- **CSRF is disabled deliberately** (non-browser API, HTTP Basic) and revisited in Lab 7.

## 14. Testing (required + guidance)

Provide tests at four levels; all pass with `./mvnw test`, using H2 for the DB layer.

### 14.1 Spring Boot 4.0 / Spring Security test imports

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

### 14.2 The `@WebMvcTest`-security gotcha (the exam trap)

A plain `@WebMvcTest` does **not** load your `SecurityConfig`, so without help the slice tests do not
exercise your rules. For **every** controller slice test: **(1)** `@Import(SecurityConfig.class)`;
**(2)** add `@MockitoBean UserDetailsService userDetailsService` (the imported config's
`AuthenticationManager`/`DaoAuthenticationProvider` needs one and the slice does not load the
`@Service` impl); **(3)** build `MockMvc` via `MockMvcBuilders.webAppContextSetup(context).apply(
SecurityMockMvcConfigurers.springSecurity()).build()`; **(4)** drive the authenticated principal with
`@WithMockUser(roles = "USER" | "ADMIN")` (and omit it for anonymous cases). Remember `@WithMockUser`
adds the `ROLE_` prefix itself — pass `"ADMIN"`, not `"ROLE_ADMIN"` (Section 6.9.5).

> **Retrofit the OLD write tests too, not just the new ones.** Every pre-existing `create` / `update` /
> `delete` test on the book **and** catalog (`Category`/`Author`/`Publisher`) controllers — **including
> the `400`-validation and `409`-duplicate cases** — must gain an **authorized** `@WithMockUser`:
> `@WithMockUser(roles = "ADMIN")` for the catalog controllers, `@WithMockUser(roles = "USER")` for the
> book controller. Because the filter chain runs before the controller, an un-annotated write test now
> hits `401`/`403` and never reaches its `400`/`409` (or `201`/`204`) assertion — leaving those old
> tests **red** even though your rules are correct (Section 6.9.5).

### 14.3 Data, service, and web tests

- **`@DataJpaTest`** (`UserRepositoryTest`): the JPA layer against H2 — `findByUsername` (roles eager)
  and `existsByUsername` (Section 6.9.1).
- **Mockito** (`UserDetailsServiceImplTest`, `AuthServiceImplTest`): the `ROLE_` authority mapping and
  `UsernameNotFoundException`; the BCrypt hashing, default-`USER`, and duplicate-rejection logic
  (Sections 6.9.2–6.9.3). Use a **real** `BCryptPasswordEncoder` in the auth-service test so
  `matches(...)` proves a real hash.
- **`@WebMvcTest`** (`AuthControllerTest` + the updated Book/Category/Author/Publisher slice tests):
  the registration contract and the authorization matrix — `401` anonymous, `403` USER-on-ADMIN,
  `201`/`204` for the right role (Sections 6.9.4–6.9.5).

## 15. Suggested Build Order

1. Start from a working Lab 5 project (or the `_5_mapstruct` reference) and confirm `./mvnw test` is
   green.
2. Add `spring-boot-starter-security` to the pom, **run the app / tests, and watch everything return
   `401`/`403`** — feel the secure-by-default breakage before you fix it. Add `spring-security-test`.
3. Add the `model` types (`Role`, `User`) and `UserRepository`; write `UserRepositoryTest` and get it
   green (`@DataJpaTest`).
4. Add `UserDetailsServiceImpl`; write `UserDetailsServiceImplTest` (the `ROLE_` mapping).
5. Write `SecurityConfig` (the three beans, the matrix, `httpBasic`/`formLogin`, deliberate CSRF
   disable). Re-run the app — reads work anonymously again, writes now require auth.
6. Add `RegisterRequest`, `UserResponse`, `AuthService`/`AuthServiceImpl`, `AuthController`; write
   `AuthServiceImplTest` and `AuthControllerTest`.
7. Add `AdminUserSeeder` and the `app.security.admin-password` property.
8. Update the existing controller slice tests (import `SecurityConfig`, mock `UserDetailsService`, add
   `@WithMockUser` / anonymous / forbidden cases — Section 6.9.5).
9. Run `./mvnw test` (all Lab 5 tests plus the new/updated ones) and exercise the API manually
   (Section 16).

## 16. Manual Testing Flow

Exercise the running app two ways: quickly with `curl` (16.1), then by **building your own Postman
collection** and running it (16.2). Both drive the Section 12.2 authorization matrix against the app you
wrote — 16.2 turns it into a repeatable suite you re-run after every change.

### 16.1 With `curl`

With the app running (the seeder has created `admin` / `admin12345`) and a few books seeded, using
`curl` with **HTTP Basic**:

1. **Public read (anonymous):**
   `curl -i http://localhost:8080/api/books` → **`200`**, the Lab 5 paged list.
2. **Anonymous write → 401:**
   `curl -i -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d '{...}'`
   → **`401 Unauthorized`** (with a `WWW-Authenticate: Basic` header).
3. **Register a plain USER:**
   `curl -i -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"username":"alice","password":"password123"}'`
   → **`201`**, `{"id":...,"username":"alice","roles":["USER"]}` (no password field).
4. **USER write succeeds:**
   `curl -i -u alice:password123 -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d '{...valid book...}'`
   → **`201`** with the created book.
5. **USER hits an ADMIN-only endpoint → 403:**
   `curl -i -u alice:password123 -X POST http://localhost:8080/api/categories -H "Content-Type: application/json" -d '{"name":"Software"}'`
   → **`403 Forbidden`** (authenticated, but lacks `ROLE_ADMIN`).
6. **ADMIN catalog write succeeds:**
   `curl -i -u admin:admin12345 -X POST http://localhost:8080/api/categories -H "Content-Type: application/json" -d '{"name":"Software"}'`
   → **`201`** with the created category.
7. **Wrong password → 401:**
   `curl -i -u alice:wrong http://localhost:8080/api/books -X POST -d '{...}'` → **`401`**.

The `401` vs `403` distinction in steps 2, 5, and 7 is the whole point: **401 = "who are you?"**
(not authenticated), **403 = "I know who you are, and no"** (authenticated, insufficient authority).

### 16.2 Build a Postman collection (and test your app)

`curl` proves the flow once; a **Postman collection** makes it a repeatable regression suite you re-run
after every change. **Build this collection yourself** — it is part of the lab, and the seven requests
below map one-to-one onto the Section 12.2 authorization matrix.

**Set up the collection**

1. Create a collection named **`Bookstore Security`**.
2. Add a collection **variable** `baseUrl` = `http://localhost:8080` (collection → *Variables*). Every
   request URL uses `{{baseUrl}}`, so you retarget the whole suite by editing one value.
3. Add collection variables for credentials so they live in one place:
   `userName`=`alice`, `userPass`=`password123`, `adminName`=`admin`, `adminPass`=`admin12345`.

**Create these requests** (one per row). Set each request's **Authorization** tab as noted — Postman
builds the `Authorization: Basic …` header for you. For bodies, pick **Body → raw → JSON** (that sets
`Content-Type: application/json` automatically) and reuse the same valid `BookRequestDto` JSON your Lab 5
tests use (`title`, `isbn`, `publicationYear`, `price`, `authorId`, `publisherId`, `categoryIds`).

| # | Request name | Method + URL | Authorization | Body | Expect |
| - | ------------ | ------------ | ------------- | ---- | :----: |
| 1 | Register alice | `POST {{baseUrl}}/api/auth/register` | **No Auth** | `{ "username": "{{userName}}", "password": "{{userPass}}" }` | `201` |
| 2 | Public read (anon) | `GET {{baseUrl}}/api/books` | **No Auth** | — | `200` |
| 3 | Anonymous write | `POST {{baseUrl}}/api/books` | **No Auth** | valid book JSON | `401` |
| 4 | USER write | `POST {{baseUrl}}/api/books` | **Basic Auth** `{{userName}}` / `{{userPass}}` | valid book JSON | `201` |
| 5 | USER hits ADMIN op | `POST {{baseUrl}}/api/categories` | **Basic Auth** `{{userName}}` / `{{userPass}}` | `{ "name": "Software" }` | `403` |
| 6 | ADMIN write | `POST {{baseUrl}}/api/categories` | **Basic Auth** `{{adminName}}` / `{{adminPass}}` | `{ "name": "Software" }` | `201` |
| 7 | Wrong password | `POST {{baseUrl}}/api/books` | **Basic Auth** `{{userName}}` / `wrong` | valid book JSON | `401` |

**Make each request a test.** In a request's **Scripts → Post-response** tab, add assertions so the
collection *checks* your app instead of you eyeballing codes. Examples:

Request 1 (Register) — status and no password leak:

```javascript
pm.test("201 Created", () => pm.response.to.have.status(201));
pm.test("no password in response, role is USER", () => {
    const body = pm.response.json();
    pm.expect(body).to.not.have.property("password");
    pm.expect(body.roles).to.include("USER");
});
```

Request 3 (anonymous write → `401`) and request 5 (USER→ADMIN → `403`):

```javascript
pm.test("blocked with the right code", () => pm.response.to.have.status(401)); // use 403 in request 5
```

**Run the collection.** Open the **Collection Runner** (collection → *Run*), run all seven top-to-bottom,
and confirm the status sequence is exactly **`201, 200, 401, 201, 403, 201, 401`** and every `pm.test`
passes. That sequence *is* the Section 12.2 matrix: if any request returns a different code, your
`SecurityFilterChain` rules (or the `ROLE_` mapping) are wrong. Re-run this collection whenever you touch
security config.

> **401 vs 403 in Postman:** requests 3 and 7 (no valid identity) must be **`401`**; request 5 (valid
> identity, wrong role) must be **`403`**. Seeing one where you expect the other points straight at your
> authorization matrix or the `ROLE_` prefix.

## 17. Acceptance Criteria (how it will be graded)

- [ ] The pom adds `spring-boot-starter-security` (main) and `spring-security-test` (test); the app
      builds and runs.
- [ ] `User` (`@Entity`, table `users`; `username` unique/not-null; `password` not-null; `roles` as an
      `@ElementCollection` EAGER enum in `user_roles`) and `Role` (`USER`, `ADMIN`) exist.
- [ ] `UserRepository` exposes `findByUsername` and `existsByUsername`.
- [ ] `UserDetailsServiceImpl` loads the user, maps each role to a **`ROLE_<name>`** authority, adapts
      to the framework `UserDetails`, and throws `UsernameNotFoundException` when absent.
- [ ] `SecurityConfig` (`@Configuration @EnableWebSecurity`) declares `PasswordEncoder`
      (`BCryptPasswordEncoder`), `SecurityFilterChain`, and `AuthenticationManager` (via
      `AuthenticationConfiguration`) beans, using **only** the Spring Security 7 lambda DSL
      (`authorizeHttpRequests` / `requestMatchers`) — no removed 5/6 APIs.
- [ ] The filter chain **permits** all `GET /api/**` and `POST /api/auth/register`, requires
      **authentication** for book writes, and restricts **catalog writes** to `hasRole("ADMIN")`;
      `httpBasic` and `formLogin` are enabled; **CSRF is disabled with a documented rationale**.
- [ ] `POST /api/auth/register` returns `201` `UserResponse` (id, username, roles) with **no password
      field**, `400` on validation, and `409` on a duplicate username; the password is BCrypt-hashed
      and self-registered users get `Role.USER`.
- [ ] `AdminUserSeeder` (an `ApplicationRunner`) idempotently seeds an `admin` / `${app.security.
      admin-password:admin12345}` account with `Role.ADMIN` and does not run in test slices.
- [ ] The authorization matrix (Section 12.2) holds: anonymous write → `401`, plain USER on an
      ADMIN-only endpoint → `403`, correct role → `2xx`.
- [ ] `./mvnw test` is green: `UserRepositoryTest`, `UserDetailsServiceImplTest`, `AuthServiceImplTest`,
      `AuthControllerTest`, and the updated Book/Category/Author/Publisher slice tests
      (`@Import(SecurityConfig.class)` + `@MockitoBean UserDetailsService` + `@WithMockUser`).
- [ ] The role-vs-authority (`ROLE_` prefix) mapping is documented in code.

- [ ] A **Postman collection** (built per Section 16.2) exercises the authorization matrix end-to-end —
      register → public read → anonymous `401` → USER `201` → USER-on-ADMIN `403` → ADMIN `201` →
      wrong-password `401` — and every `pm.test` passes in the Collection Runner.

## 18. Optional Stretch Goals (not graded)

1. Return a **custom `403`/`401` `ProblemDetail`** by wiring an `AccessDeniedHandler` and
   `AuthenticationEntryPoint` into the filter chain (instead of the default empty bodies).
2. Add a `GET /api/auth/me` that returns the current principal's `UserResponse` from the
   `SecurityContext` (`Authentication` injected via `@AuthenticationPrincipal`), and secure it
   `authenticated()`.
3. Make the `User.password` column length explicit (`@Column(length = 60)`) and discuss the fixed
   BCrypt hash length.
4. Add a second `SecurityFilterChain` with a higher `@Order` scoped to a `securityMatcher("/api/auth/
   **")` and discuss multiple chains.
5. Swap `BCryptPasswordEncoder` for `PasswordEncoderFactories.createDelegatingPasswordEncoder()` and
   note the `{bcrypt}` hash prefix and upgrade path.

## 19. Instructor Notes

Lab 5 taught mapping and read-optimization; Lab 6 adds the first **security** layer and is the
foundation the whole Security phase stands on — Lab 7 (JWT) and Lab 8 (method security) both assume
the student understands the **filter chain**, the **`AuthenticationManager`**, the
**`UserDetailsService`**, and **`authorizeHttpRequests`** taught here. The pedagogical hook is
*secure-by-default breakage*: adding the starter turns the open Lab 5 API into a wall of `401`/`403`,
and the lab is about **re-opening exactly what you mean to** with one explicit `SecurityFilterChain`.
The student should leave able to (a) explain the servlet filter chain and where their bean plugs in,
(b) write a Spring Security 7 chain with `authorizeHttpRequests`/`requestMatchers` and know the
removed 5/6 APIs (`WebSecurityConfigurerAdapter`, `authorizeRequests`, `antMatchers`), (c) implement a
DB-backed `UserDetailsService` and expose the `AuthenticationManager`, (d) hash/verify with
`BCryptPasswordEncoder`, (e) secure URLs by method and role and articulate the `ROLE_` prefix
mapping, and (f) reason about `401` vs `403`. Emphasize the two most common failure modes: the
**`@WebMvcTest`-security gotcha** (a slice does not load `SecurityConfig` unless you `@Import` it and
mock the `UserDetailsService`, and you must build `MockMvc` with `springSecurity()` and drive
principals with `@WithMockUser`), and the **`ROLE_` double-prefix mistake** (`@WithMockUser(roles =
"ADMIN")` already prefixes — never pass `"ROLE_ADMIN"`). Flag the **deliberate CSRF-disabled**
decision as a *reasoned* choice for a non-browser HTTP-Basic API, explicitly revisited with the
stateless-token rationale in Lab 7. Note the Spring Boot 4.0 / Spring Security 7 specifics (Sections
5.1, 5.2, 14.1): secure-by-default, the lambda DSL, the removed configuration surface, obtaining the
`AuthenticationManager` from `AuthenticationConfiguration`, the renamed test-slice packages, and
Jackson 3.
