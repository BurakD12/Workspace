# Course 07 — Stateless Authentication with JWT: Tokens, Signing, and the Bearer Filter

> **Learn first.** This is the study material you read *before* you build Lab 7. By the end you will
> understand how a self-issued, signed **JSON Web Token** replaces the server-side session and HTTP
> Basic login of Lab 6 — so the TaskFlow API carries *all* of the caller's identity in one header,
> keeps no `HttpSession`, and can therefore run behind a load balancer across any number of instances
> without sticky sessions.

## What you'll learn

We are taking the Lab 6 security foundation — a DB-backed `UserDetailsService`, BCrypt, an exposed
`AuthenticationManager`, and URL role rules on a `SecurityFilterChain` — and **reconfiguring it for
stateless, token-based auth** on the TaskFlow app (`User`, `Project`, `Task`). The domain and the
authorization matrix barely change; what changes is *how a request proves who it is*. Instead of a
session cookie or a `Basic` header re-checked against the database on every call, the client presents:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsInJvbGVzIjpbIlVTRVIiXSwiaWF0Ijo...
```

and the server verifies that token's signature locally — no database round-trip, no session lookup.

The three new public routes, all under `/api/auth/**` (which is `permitAll`, because a client cannot
present a token before it has one):

```
POST /api/auth/register     # public: create a USER account (BCrypt-hashed) — unchanged from Lab 6
POST /api/auth/login        # public: exchange username/password for an access + refresh token
POST /api/auth/refresh      # public: exchange a valid refresh token for a fresh access token
```

The seven modules below build up the tools you need:

1. **What a JWT is** — the three base64url parts (header, payload, signature), the standard claims (`sub`, `iat`, `exp`) plus a custom `roles` claim, and why a signed JWT is *tamper-evident* and *readable-but-not-secret* — **not** encrypted.
2. **Stateless vs. session authentication** — why server-side sessions don't scale horizontally, how a signed token moves trust to the client, and the one hard tradeoff: no easy server-side revocation, which forces short access-token lifetimes.
3. **Signing and verifying: HMAC/HS256 vs. RSA/RS256** — symmetric one-shared-secret signing vs. an asymmetric key pair, and storing the secret safely (externalized config, never hard-coded).
4. **Choosing a JWT library** — `io.jsonwebtoken` jjwt (self-issue, current 0.13.x, split api/impl/jackson) vs. `spring-boot-starter-oauth2-resource-server` (Nimbus `JwtEncoder`/`JwtDecoder`), why this lab picks jjwt, and the Spring Security 7 `typ`-header gotcha on the resource-server path.
5. **Access vs. refresh tokens, expiry and claims** — access short-lived, refresh long-lived; how `exp` drives the refresh flow; and why no secret ever goes in a claim.
6. **Stateless session policy and CSRF for token APIs** — `SessionCreationPolicy.STATELESS` (no `HttpSession`, no `JSESSIONID`) and why CSRF is irrelevant to — and safely disabled for — an `Authorization`-header API.
7. **The once-per-request JWT filter** — a `OncePerRequestFilter` that reads `Authorization: Bearer`, validates the token, builds an `Authentication`, and sets the `SecurityContext`, registered **before** `UsernamePasswordAuthenticationFilter` with `addFilterBefore`.

You already know the servlet filter chain, `Authentication` / `SecurityContext` / `SecurityContextHolder`,
`PasswordEncoder` / BCrypt, `UserDetailsService`, and the `AuthenticationManager` from Lab 6 — this
course **reuses** that model rather than re-teaching it, and focuses entirely on the JWT layer laid on
top. This is the foundation Lab 8 (method security) and Lab 14 (documenting the bearer scheme) build on.

---

## 1. What a JWT is: header, payload, signature

A **JSON Web Token** (JWT, RFC 7519) is a compact, URL-safe string in **three parts joined by dots**:

```
header . payload . signature
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiJhbGljZSIsInJvbGVzIjpbIlVTRVIiXSwiaWF0IjoxNzA... . 4pE1s7...
```

Each of the first two parts is a JSON object that has been **base64url-encoded** (base64 with a
URL-safe alphabet and no padding). The third part is the cryptographic **signature** over the first two.

| Part | Decoded content | Purpose |
|------|-----------------|---------|
| **Header** | `{"alg":"HS256"}` | Which signing algorithm was used. jjwt writes this for you from `signWith(...)`. |
| **Payload** (the *claims*) | `{"sub":"alice","roles":["USER"],"iat":1700000000,"exp":1700000900}` | The statements the token asserts about the caller. |
| **Signature** | `HMACSHA256(base64url(header) + "." + base64url(payload), secret)` | Proof that the header+payload were produced by someone holding the secret and have not been altered since. |

### The claims in a TaskFlow token

The reference `JwtService.generateAccessToken(username, roles)` writes exactly four claims — three
**registered** (standard, reserved-name) claims and one **custom** claim:

| Claim | Standard? | Meaning in TaskFlow | Set by |
|-------|-----------|---------------------|--------|
| `sub` (subject) | registered | the authenticated **username** | `.subject(username)` |
| `iat` (issued-at) | registered | when the token was minted | `.issuedAt(Date.from(now))` |
| `exp` (expiration) | registered | when the token stops being valid | `.expiration(Date.from(expiry))` |
| `roles` | **custom** | the user's bare role names, e.g. `["USER"]` or `["USER","ADMIN"]` | `.claim("roles", List.copyOf(roles))` |

```java
return Jwts.builder()
        .subject(username)
        .claim(ROLES_CLAIM, List.copyOf(roles))   // ROLES_CLAIM = "roles"
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(key, Jwts.SIG.HS256)
        .compact();                                // -> the header.payload.signature string
```

The `roles` claim is what lets the JWT filter (Module 7) rebuild the caller's authorities **without a
database lookup** — the token is self-describing.

### Signed, not encrypted: tamper-evident but readable

This is the single most misunderstood fact about JWTs, and the reference `JwtService` Javadoc states it
plainly: *"A signed JWT is tamper-evident (any change invalidates the signature) but not secret (the
payload is Base64url and readable)."* Base64url is an **encoding, not encryption** — anyone who
intercepts the token can paste the middle segment into a decoder and read every claim.

| Property | Does a signed JWT give it? | Consequence |
|----------|---------------------------|-------------|
| **Integrity** (tamper-evidence) | ✅ Yes | Change one byte of the payload and the signature no longer matches; verification fails. |
| **Authenticity** (proof of issuer) | ✅ Yes | A valid signature proves the token was minted by someone holding the secret — your server. |
| **Confidentiality** (secrecy) | ❌ **No** | The payload is plainly readable. This is why **no secret ever goes in a claim** (Module 5). |

> **The mental model:** a signed JWT is like a **tamper-proof envelope with a transparent window**.
> Anyone can read what's inside; nobody can change it without breaking the seal. If you also need the
> contents hidden, that is a *different* construction (JWE — encrypted JWT), which this lab does not use
> and does not need, because the only "secret" involved is the signing key, which never leaves the server.

---

## 2. Stateless vs. session authentication

Lab 6 authenticated with HTTP Basic against a session-capable chain. The classic web model is
**server-side sessions**: on login the server creates a `HttpSession`, stores the principal in server
memory, and hands the browser a `JSESSIONID` cookie; every later request carries that cookie and the
server looks the session up.

That works beautifully on **one** server. It breaks the moment you run **more than one**:

| Concern | Session model (stateful) | Token model (stateless) |
|---------|--------------------------|-------------------------|
| Where identity lives | Server memory, keyed by `JSESSIONID` | Inside the token, held by the **client** |
| Second server instance | Doesn't know the session — needs sticky sessions or a shared session store (Redis) | Verifies the token's signature locally; no shared state needed |
| Horizontal scaling | Extra infrastructure to share/replicate sessions | Add instances freely; any instance can serve any request |
| Per-request cost | Session lookup (memory or network) | A local signature check (CPU only, no I/O) |
| SPA / mobile clients | Cookie-centric, awkward for native apps | A plain `Authorization` header any HTTP client can send |

**A signed token moves the trust to the client.** Instead of the server *remembering* that "this
session belongs to alice," the client *carries a certificate* that says "I am alice, roles USER,
until 12:15" — and the server only has to check that the certificate's signature is genuine. Because
verification needs nothing but the signing key (which every instance already has), **any instance can
authenticate any request** with zero shared state. That is what "scales horizontally" means here.

### The hard tradeoff: no easy revocation

Statelessness has one genuine cost, and it drives several design decisions in this lab. Because the
server keeps **no record** of an issued access token, it also has **no switch to turn one off**. A
session can be deleted server-side the instant a user logs out or is banned; a valid, unexpired JWT
**stays valid until it expires**, no matter what — you cannot "un-issue" it.

The reference confronts this directly in its config comment:

```properties
# Access token: short-lived (there is no server-side revocation of a stateless JWT, so keep the
# blast-radius small). ISO-8601 duration; PT15M = 15 minutes.
app.security.jwt.access-token-ttl=PT15M
```

The mitigation is to make access tokens **short-lived** (15 minutes here) so a leaked one is only
dangerous briefly, and to pair them with a **revocable refresh token** stored server-side (Module 5)
that *can* be invalidated. Short access-token lifetime is not an arbitrary number — it is the direct,
deliberate answer to "I can't revoke this, so how do I limit the damage?"

---

## 3. Signing and verifying tokens: HMAC/HS256 vs. RSA/RS256

The signature is what makes a JWT trustworthy. There are two families of algorithm, and knowing the
difference explains why this lab holds **exactly one secret**.

| | **HMAC — HS256** (symmetric) | **RSA — RS256** (asymmetric) |
|---|------------------------------|------------------------------|
| Keys | **One shared secret** signs *and* verifies | A **key pair**: private key signs, public key verifies |
| Who can sign | Anyone holding the secret | Only the holder of the **private** key |
| Who can verify | Anyone holding the same secret | Anyone with the **public** key (safe to publish) |
| Best when | One service both issues and validates (this lab) | Many independent services verify tokens a central authority issues |
| Key size rule | Secret ≥ 256 bits (32 bytes) for HS256 | 2048-bit+ RSA key pair |

**TaskFlow issues and validates its own tokens in the same application**, so a symmetric secret is the
natural fit: there is no third party who needs to verify without being able to sign. The reference uses
**HS256** end to end, deriving the signing key from a Base64-encoded secret:

```java
public JwtService(JwtProperties properties) {
    // Keys.hmacShaKeyFor validates the key is long enough for HMAC-SHA (>= 256 bits for HS256).
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    this.accessTokenTtl = properties.getAccessTokenTtl();
}
```

`Keys.hmacShaKeyFor(...)` **throws** if the decoded key is shorter than 256 bits — jjwt refuses to let
you sign HS256 with a weak key, which is a safety feature, not an obstacle. The shipped dev secret is
48 bytes (384 bits) of Base64, comfortably above the floor.

Signing and verifying are symmetric operations on the same key. Issuing: `.signWith(key, Jwts.SIG.HS256)`.
Verifying: `.verifyWith(key)`. The `JwtServiceTest` proves the security properties this buys — a token
signed with **a different secret** fails verification, and so does a token whose signature byte was
flipped:

```java
// rejects a token signed with a different secret (wrong key)
JwtService differentKey = new JwtService(otherPropsWithADifferentSecret);
assertThat(differentKey.isValid(foreignToken)).isFalse();
```

### Storing the secret safely

A signing secret is a **credential**: anyone who has it can mint tokens your server will trust as
genuine, for *any* user and *any* role. So it must never be hard-coded in a committed source file. The
reference externalizes it into config bound to `JwtProperties` and documents the shipped value as a
**dev-only default** that must be overridden per environment:

```properties
# HS256 signing secret. MUST be a Base64-encoded key of at least 256 bits ... this 48-byte value
# is a DEV DEFAULT only; generate a fresh secret per environment and inject it via an environment
# variable (e.g. APP_SECURITY_JWT_SECRET) in real deployments.
app.security.jwt.secret=pp0gXRqpg69kqphvhFq5XK2JB4lxe8noQUoDG4RAslaDXvP/3idY/I+PV5M1EmNB
```

Because Spring Boot's relaxed binding maps `app.security.jwt.secret` to the environment variable
`APP_SECURITY_JWT_SECRET`, production overrides the secret **without touching code**. This is the
*baseline* discipline for secret management; the fuller 12-factor treatment (placeholder defaults,
`.gitignore`d local overrides, why secrets never live in committed property files at all) is the
subject of **Lab 13**. For now the rule is simply: **externalized, overridable, never hard-coded.**

---

## 4. Choosing a JWT library: jjwt vs. the OAuth2 resource server

There are two mainstream ways to work with JWTs in a Spring Boot app, and they are aimed at different jobs.

| | **jjwt** (`io.jsonwebtoken`) | **spring-boot-starter-oauth2-resource-server** |
|---|------------------------------|-----------------------------------------------|
| Primary role | **Issue *and* validate** your own tokens | Primarily **validate** tokens an external authority issued |
| Token issuance | Full control — you build the claims (`Jwts.builder()`) | Possible via Nimbus `JwtEncoder`, but the starter is validation-first |
| Integration | You write a small `OncePerRequestFilter` (Module 7) | Auto-configured `BearerTokenAuthenticationFilter` + `NimbusJwtDecoder` |
| Best fit | A self-contained service that mints its own tokens (**this lab**) | A microservice fleet validating tokens from Keycloak/Auth0/etc. |

**This lab picks jjwt**, because TaskFlow is exactly the self-issuing case: the same app runs
`/api/auth/login` (mint) and every protected endpoint (validate). jjwt gives direct, explicit control
over the builder and parser, which makes the mechanism *visible* — the whole point of a learning lab.
The resource-server path exists and is the right tool once a dedicated identity provider owns issuance,
but pulling it in here would hide the very machinery we want to study.

### The jjwt 0.13.x api/impl/jackson split

Since 0.11, jjwt is deliberately split into **three artifacts** (current line 0.13.x). The reference
`pom.xml` wires all three at the correct scopes:

```xml
<properties>
    <jjwt.version>0.13.0</jjwt.version>
</properties>

<dependency>                              <!-- compile: the API you code against -->
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>                              <!-- runtime: the implementation, service-loaded -->
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>                              <!-- runtime: the JSON (de)serializer bridge -->
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
```

| Artifact | Scope | Why it is at that scope |
|----------|-------|-------------------------|
| `jjwt-api` | **compile** | The interfaces and factories you write against — `Jwts`, `JwtBuilder`, `JwtParser`, `Keys`. |
| `jjwt-impl` | **runtime** | The concrete implementation, discovered by `ServiceLoader`. Kept **off** the compile classpath so you *cannot* accidentally import an internal class — you code only against the stable API. |
| `jjwt-jackson` | **runtime** | Marshals claims (like the `roles` **list**) to/from JSON via Jackson. Without it, jjwt cannot serialize a non-trivial claim. |

The api/impl split is a design safety rail: your code compiles against `jjwt-api` only, so an upgrade
that reshuffles internals never breaks you.

### The 0.13.x API you actually call

jjwt 0.13.x uses the modern builder/parser fluent API (note: **not** the deprecated `parserBuilder()`,
`setSubject()`, or `SignatureAlgorithm.HS256` of pre-0.12 tutorials):

```java
// Issue
Jwts.builder().subject(u).claim("roles", r).issuedAt(...).expiration(...)
    .signWith(key, Jwts.SIG.HS256).compact();

// Verify + read
Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
```

`parseSignedClaims(...)` **verifies the signature and expiry as it parses** and throws a subclass of
`JwtException` on any failure (bad signature, expired, malformed). The reference wraps that in an
`isValid` that never throws, so the filter can branch cleanly:

```java
public boolean isValid(String token) {
    try {
        parse(token);                 // parseSignedClaims under the hood
        return true;
    } catch (JwtException | IllegalArgumentException ex) {
        return false;                 // tampered, expired, or malformed -> just "not valid"
    }
}
```

> **Spring Security 7 specifics — the resource-server `typ` gotcha.** If you *ever* switch to the
> resource-server path to validate a token you self-issued with HS256, be aware that in Spring
> Security 7 `NimbusJwtDecoder` applies a **`typ` (type) header validator by default**. Depending on
> how that `JwtTypeValidator` is configured, a token whose `typ` header is **absent or unexpected** may
> be **rejected or mishandled** unless you adjust the validator (or the header). jjwt does not write a
> `typ` header by default, so a naive "issue with jjwt, validate with NimbusJwtDecoder" combination can
> trip on this until you reconcile the header. This lab sidesteps the issue
> entirely by **validating with the same jjwt parser that issued the token** — one library, one code
> path, no header mismatch.

---

## 5. Access tokens vs. refresh tokens, expiry and claims

Module 2 established the bind: a stateless access token cannot be revoked, so it must be short-lived —
but forcing the user to re-enter their password every 15 minutes is intolerable. The **refresh token**
resolves that tension. Two tokens, two very different jobs:

| | **Access token** | **Refresh token** |
|---|------------------|-------------------|
| Form | Signed **JWT** (HS256) | Opaque **random UUID** (not a JWT, no claims) |
| Sent on | **Every** protected request, in `Authorization: Bearer` | Only to `POST /api/auth/refresh` |
| Lifetime | **Short** — `PT15M` (15 min) | **Long** — `P7D` (7 days) |
| Stored server-side? | **No** (stateless, unrevocable) | **Yes** — a `refresh_tokens` row (revocable, rotatable) |
| Verified by | Local signature + `exp` check | Table lookup: exists? not revoked? not expired? |
| Job | Prove identity cheaply on every call | Mint new access tokens **without** re-sending the password |

The `exp` claim is the engine of the whole flow. When an access token's `exp` passes, the JWT filter
rejects it (401); the client then quietly calls `/api/auth/refresh` with its still-valid refresh token
and gets a fresh access token. The user notices nothing. Only when the *refresh* token also expires
(7 days) must they log in again.

### The refresh token is a server-controlled row

Unlike the stateless access token, the refresh token is deliberately **stateful** — a row we own and
can turn off. The reference `RefreshToken` entity makes it opaque and controllable:

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    private String token;            // an opaque random UUID — carries no claims

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;        // absolute expiry = issued-at + refreshTokenTtl

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;  // the switch a stateless JWT lacks
}
```

The `token` value "carries no claims and means nothing except *look me up in this table*." Because it is
just a database row, we regain the two powers the access token gave up: it can be **checked** (revoked?
expired?) and it can be **turned off**.

### Rotation: every refresh is single-use

The reference does not merely validate a refresh token — it **rotates** it. On each `/refresh`, the
presented token is revoked and a brand-new one issued, so a refresh token is single-use and a
leaked-but-already-used token is dead:

```java
@Override
@Transactional
public RefreshToken rotate(String token) {
    RefreshToken current = verify(token);      // recognized? not revoked? not expired?
    current.setRevoked(true);                  // burn the presented token
    refreshTokenRepository.save(current);
    return create(current.getUser());          // mint a replacement for the same user
}
```

`verify` is where the three server-side checks live — the checks a stateless access token cannot have:

```java
RefreshToken stored = refreshTokenRepository.findByToken(token)
        .orElseThrow(() -> new InvalidTokenException("Refresh token is not recognized"));
if (stored.isRevoked())   throw new InvalidTokenException("Refresh token has been revoked");
if (stored.getExpiresAt().isBefore(Instant.now()))
                          throw new InvalidTokenException("Refresh token has expired");
```

An `InvalidTokenException` maps to **401** via the `GlobalExceptionHandler`, so an unknown, revoked, or
expired refresh token is rejected exactly like a bad login.

### Nothing secret in a claim

Because the payload is readable (Module 1), the rule is absolute: **claims hold identity, never
secrets.** The `roles` claim is fine — a user's roles are not confidential and the token is signed, so
the client cannot forge `["ADMIN"]` without breaking the signature. But a password, an API key, a
credit-card number, or any personal secret would be exposed to anyone who sees the token. The config
comment states the rule inline: *"The payload of a JWT is signed, not encrypted: never put secrets in
claims."*

---

## 6. Stateless session policy and CSRF for token APIs

Lab 6's chain still had sessions available. Lab 7 turns them **off**, and this is the heart of the
"stateless" reconfiguration. The reference `SecurityConfig` sets `SessionCreationPolicy.STATELESS`:

```java
// No HttpSession: the SecurityContext is rebuilt per-request from the JWT.
.sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

| Policy | Behaviour |
|--------|-----------|
| `IF_REQUIRED` (default) | Create a session when something needs one |
| `NEVER` | Don't create one, but *use* an existing one if present |
| **`STATELESS`** | **Never create and never use** an `HttpSession`; no `JSESSIONID` cookie is set |

With `STATELESS`, Spring Security stops writing the security context into a session between requests.
Every request arrives with an **empty** `SecurityContext`, and the JWT filter (Module 7) rebuilds the
`Authentication` from the Bearer token **from scratch, every time**. There is nothing to remember and
nothing to leak across requests — which is precisely what makes the app horizontally scalable
(Module 2). It also means the JWT filter *is* the only thing standing between anonymous and
authenticated; there is no session fallback.

### Why CSRF is irrelevant here — and safely disabled

Lab 6 disabled CSRF and promised the "full rationale" in Lab 7. Here it is, and it is now
**unambiguous**. CSRF (Cross-Site Request Forgery) attacks a specific weakness of the **cookie/session**
model: a browser attaches your session cookie to *every* request to your domain **automatically**, so a
malicious page can trigger an authenticated write without your knowledge. The defence is a per-session
token the attacker's page cannot read.

A Bearer-token API has **no such weakness**. The credential is an `Authorization` header that the
client must **explicitly** attach — a cross-site page **cannot** make the browser send it silently
(browsers never auto-attach `Authorization` headers, and CORS blocks a foreign page from adding one).
There is no ambient credential to forge, so the CSRF attack vector simply does not exist. The reference
disables it deliberately, and the Javadoc calls the decision "now unambiguously correct":

```java
// A Bearer-token API is immune to CSRF (no ambient cookie the browser attaches),
// so CSRF protection is safely and deliberately disabled.
.csrf(csrf -> csrf.disable())
```

> **Spring Security 7 specifics.** CSRF protection is **on by default and aggressive** the moment the
> security starter is on the classpath — it will `403` your `POST`s until you either supply a CSRF token
> or disable it. For a stateless token API, disabling it is correct (not laziness): `STATELESS` sessions
> plus an `Authorization`-header credential remove the exact conditions CSRF exists to defend. The two
> config lines — `STATELESS` and `csrf.disable()` — belong together as one coherent stance.

### Restoring 401 without the login mechanisms

There is one more delta the STATELESS reconfiguration forces. Lab 6 enabled `httpBasic` and
`formLogin`; each of those installed an `AuthenticationEntryPoint` that produced a **401** (or a login
redirect) for anonymous access. Lab 7 **removes both** — a pure token API has no interactive login page
and no Basic challenge. But removing them also removes their entry point, and Spring Security's bare
fallback would answer a protected-but-anonymous request with **403**, which is the wrong status (the
caller isn't *forbidden*, they're *unauthenticated*). So the reference wires an explicit 401 entry point:

```java
.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
```

```java
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());          // 401, not 403
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(
            "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
            + "\"detail\":\"Authentication required: a valid Bearer access token is missing or invalid.\"}");
    }
}
```

It renders a small RFC 7807 ProblemDetail by hand (no `ObjectMapper` dependency) so it is robust in any
slice. This restores the Module-2 status split from Lab 6: **401 = who are you**, **403 = I know you,
you may not** (the `403` still comes from the authorization rules for, e.g., a USER hitting
`/api/admin/**`).

---

## 7. The once-per-request JWT authentication filter

This is where the pieces meet the filter chain from Lab 6. You insert **one** custom filter that reads
the Bearer token, validates it, builds an `Authentication`, and drops it into the `SecurityContext` —
so that by the time Lab 6's `AuthorizationFilter` runs, the request already has a principal.

### Why `OncePerRequestFilter`

Spring's `OncePerRequestFilter` is a base class guaranteeing the filter runs **exactly once per
request**, even when a request is internally dispatched more than once (forwards, `ERROR` dispatches,
async re-dispatch). You override one method, `doFilterInternal`, and the base class handles the
once-only bookkeeping. (You will meet `OncePerRequestFilter` again in Lab 23 as the base for a
request-id MDC filter — the pattern is reused.)

### The reference filter, line by line

```java
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtService jwtService;

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

        chain.doFilter(request, response);   // ALWAYS continue the chain
    }
}
```

Walk the guard conditions — the filter acts only when **all three** hold:

| Condition | Why |
|-----------|-----|
| `header != null && header.startsWith("Bearer ")` | There is a Bearer token to process; a missing or non-Bearer header is simply skipped. |
| `getAuthentication() == null` | The context is **not already populated** — so the filter *yields* to an existing principal (e.g. a test's `@WithMockUser`), which makes it mechanism-agnostic. |
| `jwtService.isValid(token)` | The token verifies against our key **and** has not expired. |

Inside, the roles claim is mapped back to authorities with the **`ROLE_` prefix re-added**. Recall
`AuthServiceImpl` stripped the prefix when *issuing* (the claim stores bare `USER`/`ADMIN`); the filter
re-adds it here (`ROLE_USER`, `ROLE_ADMIN`) so `hasRole("ADMIN")` and `hasRole` rules from Lab 6 line
up exactly. The principal is set to the username string, credentials to `null` (there is no password to
carry — the token *is* the proof), and the request details are attached.

### Deliberately permissive on failure

The filter **never throws and never writes a response.** A missing header, a non-Bearer header, or an
invalid/expired/tampered token all leave the context unauthenticated and let the chain continue via
`chain.doFilter(...)`. The rejection is not the filter's job — the downstream **authorization rules**
see an anonymous request and trigger the `JwtAuthenticationEntryPoint` (Module 6), producing the 401.
This separation keeps the filter simple and single-purpose: *authenticate if you can, otherwise stay
out of the way.*

### Registering it with `addFilterBefore`

The filter must run **before** the slot where username/password login would normally happen, so the
`SecurityContext` is populated by the time authorization is checked. Lab 6 taught that the chain is an
ordered list; you insert into it with `addFilterBefore`:

```java
// Validate the Bearer token before the username/password filter slot.
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

This places the JWT filter immediately ahead of `UsernamePasswordAuthenticationFilter` in the chain.
(Even though `formLogin` is removed, that filter class remains the canonical, stable **reference point**
for "where username/password authentication sits" — a well-known landmark to position against.)

### The complete reconfigured chain

Putting Modules 6 and 7 together, the full `securityFilterChain` bean:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) throws Exception {
    http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()      // register + login + refresh
                    .requestMatchers("/api/admin/**").hasRole("ADMIN") // admin area
                    .anyRequest().authenticated())                    // projects, tasks, /users/me
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

| Delta from the Lab 6 base | What & why |
|---------------------------|------------|
| `SessionCreationPolicy.STATELESS` | No `HttpSession`/`JSESSIONID`; context rebuilt per-request from the JWT. |
| CSRF stays disabled | Now unambiguously correct for a header-authenticated API (Module 6). |
| `formLogin` + `httpBasic` **removed** | Pure token API — no login page, no Basic challenge. |
| Explicit `JwtAuthenticationEntryPoint` | Restores **401** (the removed mechanisms' entry points would have; the bare fallback is 403). |
| `/api/auth/**` → `permitAll` | A client has no token yet when it registers/logs in/refreshes. |
| `addFilterBefore(jwtAuthenticationFilter, …)` | Slots the Bearer authenticator into Lab 6's chain. |
| `AuthenticationManager` bean **preserved** | The `/login` endpoint calls it to verify credentials (Lab 6 path reused). |

### Where the token comes from: the login endpoint

The filter *validates* tokens; the `/login` endpoint *mints* them, and it reuses the Lab 6
`AuthenticationManager` unchanged to check the password before issuing anything:

```java
@Override
@Transactional
public TokenResponse login(LoginRequest request) {
    // Delegates to DaoAuthenticationProvider: loads the user, matches the BCrypt hash,
    // throws BadCredentialsException on any failure.
    Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    String username = authentication.getName();
    List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(AuthServiceImpl::stripRolePrefix)     // ROLE_USER -> USER for the claim
            .toList();

    String accessToken = jwtService.generateAccessToken(username, roles);
    User user = userRepository.findByUsername(username).orElseThrow(...);
    RefreshToken refreshToken = refreshTokenService.create(user);

    return new TokenResponse(accessToken, refreshToken.getToken(), "Bearer",
            jwtService.getAccessTokenTtlSeconds());   // expiresIn = 900
}
```

This closes the loop: **login authenticates via the Lab 6 manager and issues a token → the client sends
that token → the JWT filter validates it and rebuilds the same principal.** A `BadCredentialsException`
(an `AuthenticationException`) surfaces as **401** through the `GlobalExceptionHandler`. The controller
then reads the authenticated username with a plain `Principal` argument — `principal.getName()` returns
the token's subject, exactly the username the filter set.

---

## Testing notes (Spring Boot 4.0 packages)

The JWT layer is tested at three levels — pure unit, service, and the full web slice. Mind the
**renamed 4.0 test-slice packages** and Jackson 3:

```java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;                  // controller slice
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;               // repository slice
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers; // springSecurity()
import org.springframework.test.context.bean.override.mockito.MockitoBean;             // @MockitoBean
import tools.jackson.databind.ObjectMapper;                                            // Jackson 3 (NOT com.fasterxml)
```

### `JwtService` — a pure unit test, no Spring

`JwtServiceTest` constructs `new JwtService(props)` directly (the whole reason `JwtService` is a plain
class, not a `@Service`) and proves the crypto contract without a context:

- **round-trip:** issue then parse recovers the subject and the `roles` claim in order, and `isValid` is `true`.
- **TTL:** `getAccessTokenTtlSeconds()` is `900` for a `PT15M` token.
- **expired:** a negative TTL (`Duration.ofSeconds(-60)`) mints an already-expired token → `isValid` is `false`.
- **tampered:** flipping the final signature character → `isValid` is `false`.
- **wrong key:** a token from one secret fails against a `JwtService` built with a *different* secret.

### The JWT filter — end to end against a real endpoint

`JwtAuthenticationFilterTest` is a `@WebMvcTest(ProjectController.class)` that `@Import(SecurityConfig.class)`
so the **real** `JwtService`, filter, and entry point are wired (only the service layer is mocked). It
autowires the real `JwtService` bean to **mint tokens with the same signing secret** the filter verifies
against, then asserts:

- a **valid** Bearer token reaches the protected `GET` (200) and `POST` (201), with `principal = the token subject` (`alice`);
- a **missing** token → **401**;
- a **tampered** token (flipped last char) → **401**;
- an **expired** token → **401**.

Two wiring points carry over from Lab 6 and remain essential:

```java
@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)                       // (1) the REAL stateless chain + JWT beans
class JwtAuthenticationFilterTest {

    @Autowired private JwtService jwtService;        // real bean — same secret the filter checks
    @MockitoBean private UserDetailsService userDetailsService;   // (2) satisfy the security wiring

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())   // (3) run the filter chain
                .build();
    }
}
```

1. **`@Import(SecurityConfig.class)`** pulls the real STATELESS chain, the `JwtService`/filter/entry-point
   beans, and `JwtProperties` into the slice — so the exact production rules run. (This is *also* why the
   filter, service, and entry point are declared as `@Bean`s in `SecurityConfig` rather than
   component-scanned: importing one config fully wires the chain.)
2. **`@MockitoBean UserDetailsService`** is still required: the imported config's auto-wired
   `DaoAuthenticationProvider` needs a `UserDetailsService` bean to exist, even though JWT auth never
   calls it — the slice does not load the real `@Service`.
3. **`.apply(springSecurity())`** installs the filter chain into `MockMvc` so requests actually pass
   through the JWT filter and the `AuthorizationFilter`.

Because the filter **yields to an already-populated context**, the mechanism-agnostic `@WithMockUser`
tests from Lab 6 keep working untouched alongside real-token tests.

### The auth endpoints and the refresh store

- **`AuthControllerLoginTest`** (`@WebMvcTest(AuthController.class)`, `AuthService` mocked) runs
  `/login` and `/refresh` **anonymously** (both are `permitAll`) and asserts the HTTP contract: `200`
  with `accessToken`/`refreshToken`/`tokenType: "Bearer"`/`expiresIn: 900`; a `BadCredentialsException`
  → **401** with ProblemDetail `title: "Authentication failed"`; an `InvalidTokenException` → **401**
  with `title: "Invalid token"`; and `@NotBlank` violations → **400** with field errors.
- **`AuthServiceImplTest`** (Mockito, no context) verifies `login` delegates to
  `authenticationManager.authenticate(...)`, issues the access token with the **bare** role names, and
  creates a refresh token; and that `refresh` rotates.
- **`RefreshTokenServiceImplTest`** / **`RefreshTokenRepositoryTest`** cover create, the three `verify`
  rejections (unrecognized, revoked, expired), and that `rotate` revokes the old row and returns a new one.

---

## Checklist: you are ready to build Lab 7 when you can...

- [ ] Describe a JWT's three base64url parts (`header.payload.signature`), name the standard claims `sub`/`iat`/`exp` and the custom `roles` claim, and explain that a signed JWT is **tamper-evident and readable — signed, not encrypted** — so no secret ever goes in a claim.
- [ ] Explain why server-side sessions don't scale horizontally, how a signed token **moves trust to the client** so any instance can verify locally, and why the lack of server-side revocation forces **short access-token lifetimes**.
- [ ] Contrast **HS256** (one shared secret signs *and* verifies) with **RS256** (private key signs, public key verifies), say why a self-issuing app uses HS256, and know that `Keys.hmacShaKeyFor` rejects a key shorter than 256 bits.
- [ ] Store the signing secret in **externalized config** (`app.security.jwt.secret`, overridable via `APP_SECURITY_JWT_SECRET`), never hard-coded, and know the disciplined 12-factor version is Lab 13.
- [ ] Justify choosing **jjwt** (self-issue) over `spring-boot-starter-oauth2-resource-server` (validate external tokens), wire the **0.13.x api/impl/jackson split** at the right scopes (api compile; impl + jackson runtime), and use the modern `Jwts.builder()` / `Jwts.parser().verifyWith(...).build().parseSignedClaims(...)` API.
- [ ] State the Spring Security 7 **`typ`-header gotcha**: `NimbusJwtDecoder` applies a `typ`-header validator by default, so a self-issued HS256 token with an absent/unexpected `typ` may be rejected unless the `JwtTypeValidator` is adjusted — and that this lab avoids it by validating with the same jjwt parser that issued the token.
- [ ] Distinguish the **short-lived access JWT** (stateless, sent on every request) from the **long-lived opaque refresh token** (a revocable `refresh_tokens` row), explain how `exp` drives the refresh flow, and implement **rotation** (revoke on use, single-use tokens).
- [ ] Set `SessionCreationPolicy.STATELESS` (no `HttpSession`/`JSESSIONID`, context rebuilt per-request), explain why **CSRF is irrelevant to a Bearer-token API** and safely disabled — despite Spring Security 7 having it on-and-aggressive by default — and why removing `formLogin`/`httpBasic` requires an explicit `AuthenticationEntryPoint` to restore **401** (the bare fallback would be 403).
- [ ] Write a `OncePerRequestFilter` that reads `Authorization: Bearer`, validates via `JwtService`, maps the `roles` claim back to `ROLE_`-prefixed authorities, builds a `UsernamePasswordAuthenticationToken`, and sets the `SecurityContext` — acting only when the header is Bearer, the context is empty, and the token is valid, and **never throwing or writing a response**.
- [ ] Register the JWT filter with `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` so it runs before the username/password slot and slots into Lab 6's chain.
- [ ] Trace the full loop: `/login` authenticates via the Lab 6 `AuthenticationManager` and issues an access + refresh token → the client sends the access token → the JWT filter validates it and rebuilds the same principal → the controller reads it via `Principal.getName()`.
- [ ] In a `@WebMvcTest`, `@Import(SecurityConfig.class)`, `@MockitoBean` a `UserDetailsService`, `.apply(springSecurity())`, and mint tokens from the real `JwtService` bean to prove a valid token reaches a protected write while missing/tampered/expired tokens are **401**.
