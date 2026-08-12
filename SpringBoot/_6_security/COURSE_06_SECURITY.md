# Course 06 — Spring Security Fundamentals: the Filter Chain, Users, and URL Rules

> **Learn first.** This is the study material you read *before* you build Lab 6. By the end you will
> understand how a single `SecurityFilterChain` bean turns the wide-open Bookstore from Labs 1–5 into a
> system with registered users, logins, and write operations that only the right caller may perform —
> and why simply adding the security starter makes every one of your Lab 3 endpoints start returning
> `401`/`403` until you write that bean.

## What you'll learn

We are taking the Lab 3/4/5 Bookstore (`Author 1‑N Book N‑1 Publisher`, `Book N‑N Category`) and adding
its **first real authentication and authorization layer**. Nothing about the domain changes; we add a
`User` table, a registration endpoint, and a set of access rules on top of the existing CRUD:

```
POST /api/auth/register     # public: create a USER account (BCrypt-hashed password)
GET  /api/books             # public: anyone may read
POST /api/books             # authenticated: any logged-in user may write books
POST /api/categories        # ADMIN only: catalog administration
```

One blunt fact frames the whole lab. In Spring Boot 4 / Spring Security 7, the moment you put
`spring-boot-starter-security` on the classpath, **every endpoint is secured by default** and CSRF is
enforced aggressively. Your Lab 3 endpoints do not keep working — they start answering `401 Unauthorized`
with an auto-generated login. That breakage *is the motivation*: security is on, and it is now your job
to describe, precisely and intentionally, who may do what. That description is a `SecurityFilterChain`.

The seven modules below build up the tools you need:

1. **The servlet filter chain** — how a request reaches your controller through a chain of filters, and what `DelegatingFilterProxy`, `FilterChainProxy`, and a `SecurityFilterChain` bean each do — from zero.
2. **Authentication vs. Authorization** — proving *who you are* vs. deciding *what you may do*, and how `Authentication` / `SecurityContext` / `SecurityContextHolder` carry the principal through one request.
3. **The `SecurityFilterChain` bean in Spring Security 7** — `authorizeHttpRequests` + `requestMatchers`, and why `authorizeRequests`, `antMatchers`, and `WebSecurityConfigurerAdapter` are **removed** (old tutorials will not compile).
4. **Passwords done right** — `PasswordEncoder` and BCrypt: salting, adaptive cost, and `encode` (register) vs. `matches` (login).
5. **`UserDetails` and `UserDetailsService`** — from `InMemoryUserDetailsManager` to a DB-backed custom service adapting a JPA `User`.
6. **The `AuthenticationManager`** — how a login is actually verified via `DaoAuthenticationProvider` wiring your `UserDetailsService` + `PasswordEncoder`, and how to obtain the manager as a bean.
7. **Login mechanisms and URL rules** — HTTP Basic vs. form login; `hasRole` / `hasAuthority` / `authenticated`; and the `ROLE_` prefix convention (`hasRole("ADMIN")` matches authority `ROLE_ADMIN`).

You already know layered CRUD, JPA relationships, DTOs, and the `@WebMvcTest`/`@DataJpaTest` slices from
Labs 1–5. You have **never** seen a servlet filter, a `SecurityContext`, or a password encoder — that is
what we fix here. This is the foundation Lab 7 (JWT) and Lab 8 (method security) build directly on.

---

## 1. How Spring Security actually works: the servlet filter chain

Before any of your `@RestController` code runs, an HTTP request travels through a **chain of servlet
filters**. A servlet `Filter` is a plain Jakarta EE interface — `doFilter(request, response, chain)` —
that can inspect or modify a request, then either pass it down the chain (`chain.doFilter(...)`) or
stop it dead (write a response and return). Filters run *before* the `DispatcherServlet` that dispatches
to your controller, which makes them the perfect place to enforce security: a request that fails an
auth check never reaches your business code at all.

Spring Security is, at its core, **one servlet filter that contains a chain of security-specific
filters**. Three pieces connect the plain servlet world to Spring's application context:

| Piece | What it is | Its job |
|-------|-----------|---------|
| `DelegatingFilterProxy` | A servlet filter registered in the servlet container | A thin bridge: it holds no security logic itself, it just **delegates** each request to a Spring bean named `springSecurityFilterChain`. This is how a container-managed filter reaches a Spring-managed bean. |
| `FilterChainProxy` | The Spring bean behind that name | The real entry point. It holds a **list of `SecurityFilterChain`s** and, for each request, picks the *first* chain whose matcher matches, then runs that chain's filters in order. |
| `SecurityFilterChain` | A matcher + an ordered list of security filters | One "profile" of security rules. **This is the bean you write in Lab 6.** |

So the flow is: container → `DelegatingFilterProxy` → `FilterChainProxy` → the matching
`SecurityFilterChain` → (its internal filters run) → `DispatcherServlet` → your controller.

### What lives inside a SecurityFilterChain

A `SecurityFilterChain` is itself an ordered list of small, single-purpose filters. You rarely touch
them by name, but knowing they exist demystifies the whole system. A few of the important ones:

| Filter (illustrative order) | Responsibility |
|-----------------------------|----------------|
| `SecurityContextHolderFilter` | Loads any existing `SecurityContext` at the start of the request and clears it at the end. |
| `BasicAuthenticationFilter` | Reads an `Authorization: Basic …` header and attempts authentication (Module 7). |
| `UsernamePasswordAuthenticationFilter` | Handles form-login POSTs to `/login`. |
| `AuthorizationFilter` | The **last** gate: checks the authenticated user against your `authorizeHttpRequests` rules and throws `AccessDeniedException` if they fail. |
| `ExceptionTranslationFilter` | Catches auth/access exceptions and turns them into `401` (not authenticated) or `403` (authenticated but not allowed). |

You do **not** implement these. You *configure* them by building a `SecurityFilterChain` from an
`HttpSecurity` object, and Spring Security assembles the right filters in the right order for you. In
Lab 7 you will insert *one* custom filter (a JWT filter) into this chain — which is exactly why the
mental model matters now.

> **The key realisation:** "securing an endpoint" is not something your controller does. It is decided
> upstream, in a filter, long before the controller method is invoked. Your controller stays blissfully
> unaware of authentication — it can assume that if it runs at all, the request already passed the gate.

---

## 2. Authentication vs. Authorization, and the SecurityContext

Two words that sound alike name two genuinely different questions:

| Term | Question it answers | Failure status | Example |
|------|--------------------|----------------|---------|
| **Authentication** (authN) | *Who are you?* Can you prove your identity? | `401 Unauthorized` | "Log in as `alice` with this password." |
| **Authorization** (authZ) | *What are you allowed to do?* | `403 Forbidden` | "`alice` is a USER, so she may not administer categories." |

The ordering is fixed: you authenticate **first** (establish identity), then authorize (check that
identity against a rule). A `401` means "I don't know who you are"; a `403` means "I know exactly who you
are, and you're not allowed." Spring Security's `ExceptionTranslationFilter` produces exactly this split,
and the Bookstore relies on it — an anonymous book write is `401`, a plain USER's category write is `403`.

### The three types that carry the logged-in user

Once a filter has authenticated someone, that fact has to travel with the request so later filters (and,
if it needs it, your code) can read it. Spring Security uses three collaborating types:

| Type | What it holds |
|------|---------------|
| `Authentication` | The result of authentication: the **principal** (usually a `UserDetails`), the credentials, whether it is authenticated, and — crucially — the collection of **`GrantedAuthority`** the user holds. |
| `SecurityContext` | A tiny holder with one job: keep the current `Authentication`. |
| `SecurityContextHolder` | A static access point to the `SecurityContext` for the current request. **Thread-bound by default** (`ThreadLocal`), so each request thread sees only its own principal. |

Reading the current user anywhere in the app looks like this:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();                                  // the principal's name
Collection<? extends GrantedAuthority> roles = auth.getAuthorities(); // e.g. [ROLE_ADMIN]
```

The lifecycle within **one** request: `SecurityContextHolderFilter` establishes an (empty) context at the
start → an authentication filter fills in the `Authentication` if credentials are present → the
`AuthorizationFilter` reads the authorities to enforce your rules → and at the end the context is
**cleared** so nothing leaks to the next request that reuses the thread. Because it is per-request and
thread-bound, there is no cross-request identity confusion.

> **A `GrantedAuthority` is just a string with meaning.** `new SimpleGrantedAuthority("ROLE_ADMIN")` is a
> granted authority. Authorization rules are, at bottom, string comparisons against this collection —
> which is why the `ROLE_` prefix convention in Module 7 matters so much.

---

## 3. The `SecurityFilterChain` bean in Spring Boot 4 / Spring Security 7

This is where you take back control of the secured-by-default app. You declare **one `@Bean` of type
`SecurityFilterChain`**, built from an injected `HttpSecurity` object, and inside it you describe your
access rules with the lambda DSL. Here is the shape, straight from the reference `SecurityConfig`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        // ... more rules ...
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
```

`HttpSecurity` is a **builder**. Each `.method(...)` call configures one aspect (CSRF, the authorization
rules, the login mechanisms), and `http.build()` assembles the finished `SecurityFilterChain`. The
`authorizeHttpRequests(auth -> …)` block is the authorization gate from Module 1's `AuthorizationFilter`;
`requestMatchers(...)` selects which requests a rule applies to; the terminal methods (`permitAll()`,
`authenticated()`, `hasRole("ADMIN")`) say what is required.

### The 5/6 APIs are REMOVED — not deprecated

This is the single most important fact for anyone following an older tutorial. On the **Spring
Security 7** stack this lab runs on, the classic configuration surface is **gone** — removed across the
6.0 and 7.0 releases — and code using it will not even *compile*:

| Old (Spring Security 5/6) | Removed in | Use instead |
|---------------------------|-----------------------------|-------------|
| `authorizeRequests(...)` | **7.0** | `authorizeHttpRequests(...)` |
| `antMatchers(...)` / `mvcMatchers(...)` / `regexMatchers(...)` | **6.0** | `requestMatchers(...)` |
| `extends WebSecurityConfigurerAdapter` + `configure(HttpSecurity)` | **6.0** (deprecated 5.7) | a `SecurityFilterChain` `@Bean` |
| `authorizeRequests().antMatchers("/x").permitAll()` (chained, non-lambda) | **6.0 / 7.0** | the lambda DSL (`auth -> auth.requestMatchers(...)`) |

The migration had been signalled for years (the lambda DSL and the bean-based config were the
*recommended* style throughout 6.x). Spring Security **6.0** removed `WebSecurityConfigurerAdapter` and
the `antMatchers`/`mvcMatchers`/`regexMatchers` methods; **7.0** finished the job by removing
`authorizeRequests`. On this lab's 7.x stack **none** of them exist. A tutorial that opens with
`extends WebSecurityConfigurerAdapter` is a red flag that it predates this lab's stack. The reference
documents this explicitly in its Javadoc:

```java
/**
 * Everything here uses the Spring Security 7 lambda DSL (authorizeHttpRequests + requestMatchers).
 * The Spring Security 5/6 APIs authorizeRequests, antMatchers, and WebSecurityConfigurerAdapter are
 * REMOVED in Spring Security 7, so they are deliberately not used.
 */
```

### Secured-by-default, and why `@EnableWebSecurity` is explicit here

With `spring-boot-starter-security` on the classpath and **no** `SecurityFilterChain` bean of your own,
Spring Boot auto-configures a default chain that (a) requires authentication for **every** request,
(b) generates a single `user` account with a **random password printed to the console at startup**, and
(c) enables CSRF. That is the state your Lab 3 app is in the instant you add the starter — everything is
`401`. Defining your own `SecurityFilterChain` bean makes Boot **back off** its default and use yours.

The reference annotates the config with `@EnableWebSecurity` **explicitly**. In a fully auto-configured
app you can often omit it, but the reference declares it on purpose so the `HttpSecurity` prototype bean
and the `AuthenticationConfiguration` are available **both** in the running app **and** inside the
`@WebMvcTest` slices that `@Import` this class (Spring Boot 4's web slice does not pull in the security
auto-configuration on its own — see the Testing notes).

---

## 4. Passwords done right: `PasswordEncoder` and BCrypt

**A password is never stored as plaintext.** If your `users` table holds raw passwords, a single database
leak hands an attacker every account — and, because people reuse passwords, accounts on *other* sites too.
The fix is to store a **one-way hash**: a value computed from the password that cannot be reversed back
into it. On login you hash the submitted password the same way and compare hashes; you never need the
original.

But a plain hash (SHA-256 of the password) is not enough, for two reasons that BCrypt is specifically
designed to defeat:

| Threat | Plain fast hash | BCrypt's answer |
|--------|-----------------|-----------------|
| **Rainbow tables** — precomputed hash→password lookup tables | Identical passwords hash identically, so one table cracks everyone | **Per-password salt**: a random value mixed in before hashing, stored *inside* the hash string. Two users with password `secret` get different hashes; precomputation is useless. |
| **Brute force with fast hardware** (GPUs try billions of hashes/sec) | A fast hash is a liability — speed helps the attacker | **Adaptive cost**: BCrypt is deliberately *slow*, with a tunable work factor. As hardware gets faster, you raise the cost; the attacker's economics never improve. |

A BCrypt hash is self-describing — the cost factor and salt are baked into the string, e.g.
`$2a$10$N9qo8uLOickgx2ZMRZoMy...`, where `10` is the cost. That is why `matches` later can re-derive the
same hash without you storing the salt separately.

### The `PasswordEncoder` bean

Spring Security abstracts all of this behind one interface with two methods you actually call:

```java
public interface PasswordEncoder {
    String encode(CharSequence rawPassword);              // hash on the way IN (registration)
    boolean matches(CharSequence raw, String encoded);    // verify on the way IN (login)
}
```

The reference wires the BCrypt implementation as a single bean:

```java
/** BCrypt: adaptive, salted password hashing. Used to encode on register and match on login. */
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

- **`encode`** is called exactly once per password, at **registration**, before the `User` is saved. The
  raw password never touches the database.
- **`matches`** is called at **login**, by Spring Security's `DaoAuthenticationProvider` (Module 6). You
  do not call it yourself in Lab 6 — the framework does — but you *do* rely on it working: it hashes the
  submitted password (re-using the salt from the stored hash) and compares.

The reference registration service uses `encode` and nothing else:

```java
User user = User.builder()
        .username(request.getUsername())
        // Never store the raw password: BCrypt-hash it on the way in.
        .password(passwordEncoder.encode(request.getPassword()))
        .roles(new HashSet<>(Set.of(Role.USER)))
        .build();
```

The `AuthServiceImpl` test proves the contract directly: after `register("alice", "password123")`, the
saved `User`'s password is **not** equal to `"password123"`, yet
`passwordEncoder.matches("password123", saved.getPassword())` is `true`. Hashed on the way in, verifiable
on the way out, never stored raw.

> **Spring Security 7 specifics.** `BCryptPasswordEncoder` is unchanged and remains a perfectly good
> default. If you want the framework's "upgradeable" encoder, `PasswordEncoderFactories
> .createDelegatingPasswordEncoder()` produces one that prefixes hashes with an id like `{bcrypt}` so you
> can migrate algorithms later. Lab 6 keeps it simple with a direct `BCryptPasswordEncoder` — one
> algorithm, no prefix — which is exactly what the single-algorithm Bookstore needs.

---

## 5. `UserDetails` and `UserDetailsService`: from in-memory to DB-backed

Spring Security does not know about *your* `User` entity. It authenticates against its own contract:

| Type | Contract |
|------|----------|
| `UserDetails` | What Spring Security needs to know about a user: `getUsername()`, `getPassword()` (the **stored hash**), `getAuthorities()`, and the account-status flags (`isEnabled()`, `isAccountNonLocked()`, …). |
| `UserDetailsService` | A single method — `UserDetails loadUserByUsername(String username)` — that Spring calls during authentication to fetch a user by name, throwing `UsernameNotFoundException` if there is none. |

Your job is to *adapt* your storage to `UserDetails` by implementing `UserDetailsService`.

### Start in memory to see it work

The fastest way to understand the contract is the built-in `InMemoryUserDetailsManager`, which holds a
handful of users in a map. You would never ship it for a real app, but it makes the moving parts obvious:

```java
// FOR ILLUSTRATION — not what the Bookstore ships. Users hard-coded in memory.
@Bean
UserDetailsService inMemoryUsers(PasswordEncoder encoder) {
    UserDetails admin = org.springframework.security.core.userdetails.User
            .withUsername("admin")
            .password(encoder.encode("admin12345"))   // still a BCrypt hash, even in memory
            .roles("ADMIN")                            // .roles(...) prepends ROLE_ for you
            .build();
    return new InMemoryUserDetailsManager(admin);
}
```

Note `.roles("ADMIN")` on the framework's `User` builder: it stores the authority as `ROLE_ADMIN` (the
builder adds the prefix). That is the same convention you will re-create by hand in the DB-backed version.

### The Bookstore's DB-backed `UserDetailsService`

Real users live in the `users` table (Module's JPA `User` entity). The reference replaces the in-memory
manager with a custom service that loads from `UserRepository` and adapts the row to a `UserDetails`:

```java
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();

        // Use the framework's built-in UserDetails implementation as the adapter.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), authorities);
    }
}
```

Three things to notice:

1. **Lookup by username**, throwing `UsernameNotFoundException` when absent — the exact contract Spring
   expects. The `UserRepository` is the tiny Lab 3-style Spring Data interface:

   ```java
   public interface UserRepository extends JpaRepository<User, Long> {
       Optional<User> findByUsername(String username);
       boolean existsByUsername(String username);
   }
   ```

2. **Role → authority mapping.** Each `Role` enum value becomes `new SimpleGrantedAuthority("ROLE_" +
   role.name())`, so `Role.ADMIN` becomes the authority `ROLE_ADMIN`. This hand-written prefix is what
   makes `hasRole("ADMIN")` line up (Module 7). The `UserDetailsServiceImplTest` asserts exactly this: a
   user with `USER` and `ADMIN` yields authorities `ROLE_USER` and `ROLE_ADMIN`.

3. **`getPassword()` returns the stored BCrypt hash**, not a plaintext password. `DaoAuthenticationProvider`
   (Module 6) will feed this hash into `passwordEncoder.matches(...)`.

### Why the roles are fetched EAGERly

The `User` entity stores roles in a `user_roles` join table via `@ElementCollection(fetch = FetchType.EAGER)`:

```java
@Builder.Default
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
@Enumerated(EnumType.STRING)
@Column(name = "role", nullable = false)
private Set<Role> roles = new HashSet<>();
```

Authentication reads `user.getRoles()` right after loading the user, often outside an open transaction, so
the roles must already be populated — hence **EAGER**. A `LAZY` collection would risk a
`LazyInitializationException` at exactly the wrong moment. (For a large, rarely-needed collection LAZY is
usually right; for the tiny, always-needed authorities set, EAGER is the correct call.)

---

## 6. The `AuthenticationManager` and how a login is verified

You have a `UserDetailsService` (loads users) and a `PasswordEncoder` (checks hashes). Something has to
*orchestrate* them into an actual credential check. That orchestrator is the **`AuthenticationManager`**,
and its default worker for username/password login is the **`DaoAuthenticationProvider`**.

The verification of one login, step by step:

1. A filter (HTTP Basic or form login) builds an **unauthenticated** `UsernamePasswordAuthenticationToken`
   from the submitted username + raw password and hands it to `AuthenticationManager.authenticate(...)`.
2. The manager (a `ProviderManager`) delegates to its `DaoAuthenticationProvider`.
3. The provider calls **your** `UserDetailsService.loadUserByUsername(username)` to fetch the stored
   `UserDetails` (with the BCrypt hash and authorities).
4. It calls `passwordEncoder.matches(rawPassword, userDetails.getPassword())`. On mismatch it throws
   `BadCredentialsException` → `401`.
5. On success it returns a **new, authenticated** `Authentication` carrying the principal and authorities,
   which the filter places into the `SecurityContext` (Module 2).

```
login filter → AuthenticationManager → DaoAuthenticationProvider
                                            ├── UserDetailsService.loadUserByUsername(...)
                                            └── PasswordEncoder.matches(raw, storedHash)
                                        → authenticated Authentication → SecurityContext
```

### You don't build the provider — Spring Security wires it from your beans

Here is the elegant part: the reference **never constructs a `DaoAuthenticationProvider` by hand**. Because
there is a `UserDetailsService` bean *and* a `PasswordEncoder` bean in the context, **Spring Security itself**
— through the global-authentication machinery that `@EnableWebSecurity` switches on (an
`InitializeUserDetailsBeanManagerConfigurer`) — assembles a `DaoAuthenticationProvider` from them and
registers it with the global `AuthenticationManager`. This is Spring Security's own wiring, **not** Spring
Boot's security auto-configuration. That is precisely why the `@WebMvcTest` slice — which `@Import`s
`SecurityConfig` (and hence `@EnableWebSecurity`) but does **not** load Boot's security auto-config — still
needs a (mocked) `UserDetailsService` bean to satisfy it. Providing those two beans is the whole wiring.

### Exposing the `AuthenticationManager` as a bean

Lab 6 authenticates via `httpBasic`/`formLogin`, so it does not *call* the manager itself. But the
reference still exposes it as a bean, because **Lab 7's JWT login endpoint will call it directly** to
authenticate a username/password before issuing a token:

```java
/**
 * Expose the AuthenticationManager built from the AuthenticationConfiguration so it can be
 * injected elsewhere. Lab 6 does not call it directly (httpBasic/formLogin drive
 * authentication), but Lab 7's JWT login endpoint will authenticate credentials through it.
 */
@Bean
AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
}
```

`AuthenticationConfiguration.getAuthenticationManager()` returns the fully-wired global manager — the same
one the login filters use — so an injected `AuthenticationManager` and the framework's own login path
share identical behaviour. This bean is the seam Lab 7 plugs into; declaring it now is a deliberate
forward-investment, not dead code.

> **Spring Security 7 specifics.** `AuthenticationConfiguration` remains the standard way to obtain the
> manager as a bean. Do **not** reach for `WebSecurityConfigurerAdapter.authenticationManagerBean()` — the
> adapter is removed (Module 3). If you ever need to wire a provider explicitly, construct
> `new DaoAuthenticationProvider(userDetailsService)` and `setPasswordEncoder(...)`, but for the Bookstore
> the bean-driven auto-configuration above is cleaner and is what the reference uses.

---

## 7. Login mechanisms and URL rules: HTTP Basic, form login, roles vs. authorities

Two remaining pieces: *how* a caller presents credentials, and *how* you express the access rules.

### HTTP Basic vs. form login

The reference enables **both**, each suited to a different kind of client:

| Mechanism | How the client authenticates | Anonymous access to a protected URL yields | Best for |
|-----------|------------------------------|--------------------------------------------|----------|
| `httpBasic(Customizer.withDefaults())` | Sends `Authorization: Basic base64(user:pass)` on **every** request | `401` with a `WWW-Authenticate` header | APIs, `curl`, tests, non-browser clients |
| `formLogin(Customizer.withDefaults())` | POSTs username/password to `/login`; server sets a session cookie | A redirect to the login **page** | Browsers with an interactive login form |

```java
.httpBasic(Customizer.withDefaults())
.formLogin(Customizer.withDefaults());
```

`Customizer.withDefaults()` accepts each mechanism's sensible defaults (default `/login` page, standard
Basic realm). HTTP Basic is the primary path the Bookstore's tests exercise, because it needs no session
and no form — a request either carries valid credentials or it is `401`.

### Expressing rules: `permitAll`, `authenticated`, `hasRole`, `hasAuthority`

Inside `authorizeHttpRequests`, each `requestMatchers(...)` selects requests and a terminal method states
the requirement:

| Terminal method | Grants access to |
|-----------------|------------------|
| `permitAll()` | Everyone, including anonymous callers |
| `authenticated()` | Any successfully authenticated user, regardless of role |
| `hasRole("ADMIN")` | A user holding authority **`ROLE_ADMIN`** (note the implicit prefix) |
| `hasAuthority("ROLE_ADMIN")` | A user holding the **exact** authority string `ROLE_ADMIN` (no prefix added) |
| `denyAll()` | No one |

### The `ROLE_` prefix convention — the one gotcha everyone hits

`hasRole` and `hasAuthority` differ by exactly one behaviour:

- `hasRole("ADMIN")` **automatically prepends `ROLE_`** and checks for authority `ROLE_ADMIN`.
- `hasAuthority("ADMIN")` checks for the authority **`ADMIN`** literally, with no prefix.

This is why every layer of the Bookstore agrees on the prefix. The `UserDetailsServiceImpl` maps
`Role.ADMIN` to `new SimpleGrantedAuthority("ROLE_" + role.name())` → `ROLE_ADMIN`; the rule
`hasRole("ADMIN")` looks for `ROLE_ADMIN`; the two meet. The `Role` enum documents the pact:

```java
/**
 * ... mapped to Spring Security authorities with the "ROLE_" prefix, so Role.ADMIN becomes
 * authority ROLE_ADMIN and hasRole("ADMIN") matches it.
 */
public enum Role { USER, ADMIN }
```

> **The classic bug:** writing `hasRole("ROLE_ADMIN")`. That looks for `ROLE_ROLE_ADMIN` (the prefix is
> added *on top of* your string) and **nobody ever matches** — every ADMIN silently gets `403`. Rule of
> thumb: pass the bare role name to `hasRole` (`"ADMIN"`), and the full authority to `hasAuthority`
> (`"ROLE_ADMIN"`). In tests, `@WithMockUser(roles = "ADMIN")` follows the same convention — it grants the
> authority `ROLE_ADMIN`, so `roles = "ROLE_ADMIN"` would be the same double-prefix mistake.

### The Bookstore authorization matrix

Putting it together, the reference `authorizeHttpRequests` block encodes this policy — order matters,
because the **first matching rule wins**, so specific rules precede the catch-all:

```java
.authorizeHttpRequests(auth -> auth
        // --- Public: self-service registration + all read traffic ---
        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

        // --- Catalog administration: only ADMIN may mutate categories/authors/publishers ---
        .requestMatchers(HttpMethod.POST, "/api/categories/**", "/api/authors/**", "/api/publishers/**")
        .hasRole("ADMIN")
        .requestMatchers(HttpMethod.PUT, "/api/categories/**", "/api/authors/**", "/api/publishers/**")
        .hasRole("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/authors/**", "/api/publishers/**")
        .hasRole("ADMIN")

        // --- Book writes: any authenticated user (USER or ADMIN) ---
        .requestMatchers(HttpMethod.POST, "/api/books/**").authenticated()
        .requestMatchers(HttpMethod.PUT, "/api/books/**").authenticated()
        .requestMatchers(HttpMethod.DELETE, "/api/books/**").authenticated()

        // --- Everything else must be authenticated ---
        .anyRequest().authenticated())
```

| Request | Rule | Anonymous | USER | ADMIN |
|---------|------|-----------|------|-------|
| `POST /api/auth/register` | `permitAll()` | ✅ | ✅ | ✅ |
| `GET /api/**` (any read) | `permitAll()` | ✅ | ✅ | ✅ |
| `POST/PUT/DELETE /api/books/**` | `authenticated()` | `401` | ✅ | ✅ |
| `POST/PUT/DELETE /api/categories\|authors\|publishers/**` | `hasRole("ADMIN")` | `401` | `403` | ✅ |
| anything else | `authenticated()` | `401` | ✅ | ✅ |

Read the matrix as three tiers: **reads and registration are open**, **book writes need any login**, and
**catalog administration is ADMIN-only**. The status split from Module 2 shows through cleanly — anonymous
is `401` (who are you?), a USER hitting an ADMIN route is `403` (I know you, you're not allowed).

### The deliberate CSRF decision

The reference **disables CSRF** — a real choice, documented, not laziness:

```java
// CSRF defends cookie/session-authenticated *browser* form posts, where the browser attaches the
// session cookie automatically and an attacker's page can forge a request. This API is exercised by
// non-browser clients authenticating with HTTP Basic (an explicit Authorization header a cross-site
// page cannot silently attach), so CSRF tokens add friction without adding protection here.
.csrf(csrf -> csrf.disable())
```

CSRF (Cross-Site Request Forgery) protection exists for the **cookie/session** model: a browser attaches
your session cookie to *every* request to your domain automatically, so a malicious page can trigger an
authenticated write without your knowledge. The defence is a per-session token the attacker cannot read.
But this API authenticates with an **`Authorization` header** that a cross-site page **cannot** attach
silently — so the CSRF attack vector does not apply, and the token machinery would only add friction. The
full treatment of CSRF (and the stateless-token rationale that makes disabling it clearly correct) is
**deferred to Lab 7**, where `SessionCreationPolicy.STATELESS` removes sessions entirely.

### Seeding an initial ADMIN

There is a bootstrap problem: catalog administration needs an ADMIN, but registration only ever creates
`USER` accounts (`Role.USER`). The reference solves it with an `ApplicationRunner` that seeds one admin at
startup if none exists:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin-password:admin12345}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername("admin")) {
            return;                                    // idempotent: never seed twice
        }
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode(adminPassword))   // hashed, like any password
                .roles(new HashSet<>(Set.of(Role.ADMIN)))
                .build();
        userRepository.save(admin);
        log.info("Seeded default ADMIN user 'admin'");
    }
}
```

Two deliberate points: it is a plain `@Component implements ApplicationRunner` (**not** a `@Bean` in a
`@Configuration`) so the `@WebMvcTest`/`@DataJpaTest` slices — which do not scan arbitrary `@Component`s —
never run it and cannot pollute their contexts; and the seed password is **externalised**
(`app.security.admin-password`, defaulting to `admin12345`) so production overrides it via config, never
by editing code.

---

## Testing notes (Spring Boot 4.0 packages)

Security testing has one gotcha that dominates everything else, plus the usual renamed 4.0 slice packages:

```java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;                 // controller slice
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;              // repository slice
import org.springframework.security.test.context.support.WithMockUser;                // fake a principal
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers; // springSecurity()
import tools.jackson.databind.ObjectMapper;                                           // Jackson 3 (NOT com.fasterxml)
```

### The `@WebMvcTest` gotcha: security is applied to slices, so you must import the config

A `@WebMvcTest` boots only the web layer — it does **not** load your `SecurityConfig`. If you forget to
import it, the slice runs with the slice's *default* security (or none), and your carefully written
authorization rules are **never exercised**: a test asserting `403` for a USER can pass for the wrong
reason, or every request sails through. The reference fixes this in two coordinated steps, visible in
every controller test:

```java
@WebMvcTest(BookController.class)
@Import(SecurityConfig.class)          // (1) load the REAL rules into the slice
class BookControllerTest {

    @MockitoBean private BookService bookService;
    @MockitoBean private UserDetailsService userDetailsService;   // (2) satisfy the security wiring

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())   // (3) run the filter chain
                .build();
    }
}
```

1. **`@Import(SecurityConfig.class)`** pulls the real `SecurityFilterChain` (and the `PasswordEncoder` /
   `AuthenticationManager` beans) into the slice, so the exact production rules are under test. This is
   *also* why `SecurityConfig` declares `@EnableWebSecurity` explicitly (Module 3) — the web slice does not
   auto-configure security on its own.
2. **`@MockitoBean UserDetailsService`** is required because the imported config's auto-wired
   `DaoAuthenticationProvider` needs a `UserDetailsService` bean to exist — the slice does not load the
   real `@Service`. Mocking it satisfies the wiring without a database.
3. **`.apply(springSecurity())`** installs the security filter chain into `MockMvc` so requests actually
   pass through the `AuthorizationFilter`, and `@WithMockUser` bridges a fake `SecurityContext` into each
   request.

### What the tests assert

- **`@WithMockUser(roles = "…")`** fabricates an authenticated principal with authority `ROLE_…`. The
  `BookControllerTest` proves the matrix: anonymous `POST /api/books` → `isUnauthorized()` (401); the same
  POST under `@WithMockUser(roles = "USER")` → `isCreated()`; `GET` endpoints have **no** `@WithMockUser`
  (they are `permitAll`). The `CategoryControllerTest` proves the ADMIN tier: anonymous POST → `401`,
  `@WithMockUser(roles = "USER")` POST → `isForbidden()` (403), `@WithMockUser(roles = "ADMIN")` POST →
  `isCreated()`.
- **`AuthControllerTest`** runs `POST /api/auth/register` **anonymously** (no `@WithMockUser`, because it is
  `permitAll`) and asserts `201` with `roles[0] == "USER"` and that the response body has **no** `password`
  field — the hash never crosses the API boundary.
- **`UserDetailsServiceImplTest`** (`@ExtendWith(MockitoExtension.class)`, no Spring context) mocks
  `UserRepository` and asserts the role→authority mapping (`ROLE_USER`, `ROLE_ADMIN`) and the
  `UsernameNotFoundException` path.
- **`AuthServiceImplTest`** uses a **real** `BCryptPasswordEncoder` (not a mock) to prove `register` stores
  a hash — `saved.getPassword()` is not the raw password, yet `matches(raw, saved.getPassword())` is `true`
  — assigns default role `USER`, and throws `DuplicateResourceException` (→ `409`) without saving when the
  username is taken.

---

## Checklist: you are ready to build Lab 6 when you can...

- [ ] Explain the request path `DelegatingFilterProxy → FilterChainProxy → SecurityFilterChain → DispatcherServlet`, and say what each of the three named pieces does.
- [ ] State that "securing an endpoint" happens in a **filter**, before your controller runs, and name a couple of the internal filters (`AuthorizationFilter`, `ExceptionTranslationFilter`).
- [ ] Distinguish authentication (`401`, who are you) from authorization (`403`, what may you do), and describe how `Authentication` / `SecurityContext` / `SecurityContextHolder` carry the principal through **one** request.
- [ ] Write a `SecurityFilterChain` `@Bean` from `HttpSecurity` using `authorizeHttpRequests` + `requestMatchers`, and explain why `authorizeRequests`, `antMatchers`, and `WebSecurityConfigurerAdapter` are all **absent** from this lab's Spring Security 7 stack (`WebSecurityConfigurerAdapter`/`antMatchers` removed in 6.0, `authorizeRequests` in 7.0).
- [ ] Explain "secured-by-default": adding the starter with no chain of your own makes every endpoint `401`, and defining a `SecurityFilterChain` bean makes Boot back off.
- [ ] Say why passwords are hashed, how BCrypt's **salt** defeats rainbow tables and its **adaptive cost** defeats brute force, and wire a `BCryptPasswordEncoder` bean.
- [ ] Use `PasswordEncoder.encode` at registration and know that `matches` is called for you at login by `DaoAuthenticationProvider`.
- [ ] Implement the `UserDetailsService` contract: `loadUserByUsername`, throwing `UsernameNotFoundException`, mapping each `Role` to a `ROLE_<name>` `SimpleGrantedAuthority`, and returning the stored **hash** as the password.
- [ ] Explain why the `roles` `@ElementCollection` is fetched **EAGER** (authorities must be present the moment the user is loaded for authentication).
- [ ] Trace how the `AuthenticationManager` verifies a login via `DaoAuthenticationProvider` wiring your `UserDetailsService` + `PasswordEncoder`, and expose the manager as a bean from `AuthenticationConfiguration` (for Lab 7).
- [ ] Compare `httpBasic()` vs. `formLogin()`, and express rules with `permitAll` / `authenticated` / `hasRole` / `hasAuthority`.
- [ ] State the `ROLE_` prefix convention: `hasRole("ADMIN")` matches authority `ROLE_ADMIN`, and `hasRole("ROLE_ADMIN")` is the classic double-prefix bug that `403`s every admin.
- [ ] Reproduce the Bookstore matrix — reads/register public, book writes `authenticated`, catalog admin `hasRole("ADMIN")` — and explain why first-match ordering puts specific rules before `anyRequest()`.
- [ ] Justify disabling CSRF for this header-authenticated API (and that the full treatment is deferred to Lab 7), and seed an initial ADMIN with an idempotent, externally-configured `ApplicationRunner`.
- [ ] In a `@WebMvcTest`, `@Import(SecurityConfig.class)`, `@MockitoBean` a `UserDetailsService`, and `.apply(springSecurity())` so the real rules run — and know that without the import the slice does **not** load your security config.
