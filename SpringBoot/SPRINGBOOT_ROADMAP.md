# Spring Boot Learning Roadmap

A structured, job-ready path for learning Spring Boot backend development through a
series of hands-on labs. Each lab builds directly on the previous one and is themed
around **one main idea**, so concepts compound instead of piling up all at once.

Crucially, **you never apply a technology you haven't been taught first.** Every lab
opens with the concepts to study — Docker before Testcontainers, Redis before
caching-with-Redis, RabbitMQ before messaging — and only then asks you to build with them.

## How These Labs Work

Every lab is built from **three artifacts**, in the order you meet them:

1. **Course Modules (`COURSE_*.md`)** — the **📚 Learn first** material. Each module is a
   short, self-contained teaching document for *one* concept, written from the ground up
   (infrastructure like Docker, Redis, and RabbitMQ is taught assuming **zero** prior
   knowledge). These are the prerequisites you study before touching the exercise.
   *(Authored per lab — the full backlog is listed in Appendix A.)*
2. **A lab assignment (`LAB_NN_*.md`)** — the **🔨 Then build** brief. It explains *what* to
   build, *in what order*, and *what each class is responsible for*, with reference snippets
   only for genuinely new concepts. **This is what the student implements.**
3. **A full reference implementation** — the complete, working project (the "answer key")
   used to check student submissions.

Each lab entry below follows the same rhythm: a short **what & why**, a **📚 Learn first**
list (the concept modules to study), a **🔨 Then build** brief (the exercise that applies
them), and the **🆕 new skills** it unlocks.

## The Destination

These labs target the skill set a company expects from a backend developer: clean layered
architecture, correct HTTP behavior, a well-modeled data layer, security, real testing, and
production-readiness — migrations, configuration, documentation, containerization,
caching, messaging, observability, and deployment.

## Conventions Carried Across All Labs

| Area | Convention |
| ---- | ---------- |
| Stack | Spring Boot 4.x, Java 21, Maven |
| Database | PostgreSQL (local); H2 then **Testcontainers** for tests |
| Architecture | Layered: `controller` → `service` → `repository` → `entity`, with `dto` and `exception` packages |
| API contract | DTOs at every boundary (entities are never exposed) |
| Errors | `ProblemDetail` (RFC 7807) via a `@RestControllerAdvice` global handler |
| HTTP | Explicit `ResponseEntity` and deliberate status codes |
| Boilerplate | Lombok |
| Infrastructure | Introduced from scratch as each lab needs it — Docker, Redis, RabbitMQ, Prometheus/Grafana — and run locally via **Docker Compose** |

## Legend

✅ done · ⬅ current · 🆕 new lab · 📚 learn first · 🔨 then build

---

## Roadmap at a Glance

Twenty-five labs across nine phases. Every lab follows the same rhythm — **📚 Learn first** (study its concept modules), then **🔨 Then build** (implement the exercise that uses them). You never apply a technology you have not been taught first.

- **Phase 1 — Foundations** — Lab 1 Employee Management ✅ · Lab 2 Student Management ✅
- **Phase 2 — Data Layer Mastery** — Lab 3 Bookstore (Entity Relationships & Advanced JPA) ⬅ Current · Lab 4 Dynamic Search with JPA Specifications & the Criteria API 🆕 · Lab 5 DTO Mapping at Scale with MapStruct & Read-Optimized Projections 🆕
- **Phase 3 — Security** — Lab 6 Spring Security Fundamentals 🆕 · Lab 7 Stateless Authentication with JWT 🆕 · Lab 8 Method Security & Fine-Grained Authorization 🆕
- **Phase 4 — Containerization & Testing** — Lab 9 Docker & Containerization Fundamentals 🆕 · Lab 10 Integration Testing with @SpringBootTest 🆕 · Lab 11 Real Dependencies in Tests with Testcontainers 🆕
- **Phase 5 — Production Readiness** — Lab 12 Database Migrations with Flyway 🆕 · Lab 13 Profiles & Externalized Configuration 🆕 · Lab 14 API Documentation with OpenAPI & Swagger UI 🆕 · Lab 15 Operational Endpoints with Spring Boot Actuator 🆕
- **Phase 6 — Caching & Performance** — Lab 16 The Spring Cache Abstraction (In-Memory First) 🆕 · Lab 17 Redis as a Cache & Store 🆕
- **Phase 7 — Messaging, Async & Events** — Lab 18 Asynchronous & Scheduled Tasks 🆕 · Lab 19 Decoupling with Application Events 🆕 · Lab 20 Asynchronous Messaging with RabbitMQ (Spring AMQP) 🆕
- **Phase 8 — Inter-Service Communication** — Lab 21 Calling Other Services with RestClient & WebClient 🆕 · Lab 22 Resilience: Timeouts, Retries & Circuit Breakers 🆕
- **Phase 9 — Observability & Deployment** — Lab 23 Structured Logging & Correlation IDs 🆕 · Lab 24 Metrics & Distributed Tracing with Micrometer 🆕 · Lab 25 CI/CD & Deployment (Capstone) 🆕

---

## Phase 1 — Foundations ✅

The first full backend flow, then HTTP done right. The architecture established here is reused by every later lab.

### Lab 1 — Employee Management ✅

Your first complete backend flow: a layered Employee Management API from a PostgreSQL table all the way to REST endpoints. Establishes the architecture every later lab reuses.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Layered Architecture** — why backends split into controller -> service -> repository -> model, and what each layer owns
- **JPA + Hibernate + PostgreSQL** — mapping an entity to a table and wiring Spring Data JPA to a local PostgreSQL database
- **Entity vs. DTO** — why entities are never exposed, request/response DTOs, and manual mapping
- **Derived Queries & Validation** — Spring Data derived query methods and @Valid bean validation
- **Exceptions & Lombok** — custom exceptions, a GlobalExceptionHandler, and cutting boilerplate with Lombok

**🔨 Then build**

- A full CRUD Employee Management REST API on PostgreSQL using the layered architecture, DTO mapping, validation, and centralized exception handling.

**🆕 New skills unlocked:** End-to-end layered CRUD; JPA persistence; DTO mapping; bean validation.

### Lab 2 — Student Management ✅

The same architecture, now focused on getting HTTP exactly right — deliberate status codes, standardized error bodies, and your first real test layer.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Correct HTTP Semantics** — using ResponseEntity and choosing 200/201/204/400/404/409/500 deliberately, plus the Location header on creation
- **ProblemDetail (RFC 7807)** — standardized error responses with per-field validation details
- **Multiple Unique Constraints** — turning distinct constraint violations into distinct 409 responses
- **Slice Testing with @WebMvcTest** — testing controllers in isolation with @MockitoBean, nested test classes, and jsonPath assertions; plus service-layer unit tests

**🔨 Then build**

- A Student Management API with rigorous HTTP semantics, RFC 7807 error responses, and a @WebMvcTest + service unit-test suite.

**🆕 New skills unlocked:** HTTP status discipline; RFC 7807 errors; @WebMvcTest slice testing.

---

## Phase 2 — Data Layer Mastery

Beyond a single flat table: model related data correctly, query it efficiently, and search it dynamically.

### Lab 3 — Bookstore (Entity Relationships & Advanced JPA) ⬅ Current

Move from one flat table to a realistic, related data model — authors, publishers, categories, and books — and the advanced JPA needed to query it efficiently.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Entity Relationships** — @OneToMany / @ManyToOne / @ManyToMany, owning vs. inverse side, mappedBy, @JoinColumn, @JoinTable
- **Cascades & orphanRemoval** — propagating persistence operations across associations safely
- **Fetch Strategy & the N+1 Problem** — LAZY vs. EAGER and recognising the N+1 query problem
- **Fixing N+1** — JOIN FETCH and @EntityGraph
- **Custom Queries, Pagination & Transactions** — JPQL @Query, Pageable/Page sorting, and @Transactional (read-only vs. read-write)
- **Repository Testing with @DataJpaTest** — the third test layer — verifying mappings and queries against a database

**🔨 Then build**

- A Bookstore REST API with correctly modelled relationships, N+1-free queries, pagination/sorting, and tests at the @DataJpaTest, Mockito service, and @WebMvcTest layers.

**🆕 New skills unlocked:** Relational modelling in JPA; N+1 diagnosis & fixes; pagination/sorting; @DataJpaTest.

### Lab 4 — Dynamic Search with JPA Specifications & the Criteria API 🆕

Add a single GET /api/books/search endpoint to the Bookstore that filters by ANY combination of title, author, category, price range, and publication year, without writing one repository method per filter combination. This is the standard production answer to 'the user wants flexible search,' and it builds directly on Lab 3's repositories, pagination, and N+1 awareness: Specifications run on the JPA Criteria API you meet here, the same JpaSpecificationExecutor.findAll(spec, pageable) reuses Lab 3's Pageable/Page and PagedResponse, and the many-to-many category join revisits Lab 3's collection-join-plus-pagination pitfall. You write small, single-responsibility predicates once and recombine them at runtime.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Why dynamic queries: the combinatorial explosion of derived queries** — Why findByTitleAndAuthorAndPriceBetween... and a pile of @Query variants do not scale to N optional filters (2^N combinations), motivating a single query you assemble at runtime from independent pieces.
- **The JPA Criteria API from zero** — What CriteriaBuilder, CriteriaQuery, Root, Path/Join, and Predicate are, and how to build a type-safe WHERE clause as Java objects (e.g. cb.like, cb.between, cb.equal) instead of a JPQL string, so the compiler catches mistakes a string would not.
- **The Specification<T> interface in Spring Data JPA 4.0** — Implementing toPredicate(Root<T>, CriteriaQuery<?>, CriteriaBuilder) from org.springframework.data.jpa.domain.Specification as a lambda that returns one Predicate, and reading a Specification as 'a reusable WHERE fragment'.
- **Composing Specifications, and the Spring Boot 4.0 way to say 'no filter'** — Gluing independent predicates with the default .and()/.or()/.not() methods and the static allOf()/anyOf() factories; and the 4.0-specific point that the modern 'match everything' base is Specification.unrestricted() (which replaces the legacy where(null) idiom), while a single null-skipping predicate is still the cleanest way to drop an absent filter. Also introduces PredicateSpecification<T> (new in 4.0) so the student recognises it, while this lab standardises on Specification<T> for paginated repository search.
- **JpaSpecificationExecutor and wiring it into a repository** — Extending JpaSpecificationExecutor<Book> to unlock findAll(Specification, Pageable) / findAll(Specification, Sort), how it coexists with the JpaRepository methods and @EntityGraph from Lab 3, and where the executor's generated query plugs the predicate into the WHERE clause.
- **A SearchCriteria + builder pattern for optional filters** — Mapping nullable query params into an immutable SearchCriteria record and a SpecificationBuilder that adds a predicate only when its filter is present, starting from Specification.unrestricted() and joining relationships (author, categories) safely so absent filters add nothing to the SQL.
- **Specifications vs derived queries vs @Query: choosing the right tool** — A decision guide for when each technique wins (fixed simple queries -> derived; complex but static -> @Query; runtime-assembled optional filtering -> Specifications), plus the two Criteria pitfalls that bite here: a many-to-many join duplicating rows (needs distinct) and the Lab 3 rule that you cannot fetch a collection while paginating.

**🔨 Then build**

- Add JpaSpecificationExecutor<Book> to BookRepository, keeping every JpaRepository method and @EntityGraph finder from Lab 3.
- Write individual, single-responsibility Specification<Book> factory methods: titleContains, authorNameContains (a join to author), hasCategory (a join to categories), priceBetween (handling an open-ended min and/or max), and publicationYearBetween.
- Create an immutable BookSearchCriteria record carrying all five optional filters, bound from request params.
- Build a BookSpecificationBuilder that starts from Specification.unrestricted() and .and()s only the criteria that are present, returning one Specification<Book> (no special-casing the all-null 'return everything' request).
- Expose GET /api/books/search accepting every filter as an optional @RequestParam plus a Pageable (@PageableDefault), delegating to the service and returning Lab 3's PagedResponse<BookResponseDto>.
- Handle the many-to-many category join so combining a category filter with paging does not duplicate rows (set query.distinct(true) inside the predicate), and keep the list mapping category-free per Lab 3's collection-vs-pagination rule.
- Write @DataJpaTest cases asserting that each filter alone, no filters (returns all), and a multi-filter AND combination return exactly the right books, that a category filter yields no duplicate rows, and that results stay paginated and sorted.

**🆕 New skills unlocked:** Building type-safe queries with the JPA Criteria API; Implementing and composing Spring Data JPA 4.0 Specifications (and recognising PredicateSpecification / unrestricted()); Designing a reusable SearchCriteria record + builder for optional multi-field filtering; Wiring JpaSpecificationExecutor for paginated, sorted dynamic search; Choosing between derived queries, @Query, and Specifications.

### Lab 5 — DTO Mapping at Scale with MapStruct & Read-Optimized Projections 🆕

Replace the hand-written entity<->DTO mappers from Labs 1-4 with compile-time-generated MapStruct mappers, then add Spring Data projections so read endpoints fetch only the columns they return. This removes the most repetitive, error-prone boilerplate in the layered architecture while staying fully type-safe, and the projections directly attack the over-fetching that pairs with Lab 3's N+1 lessons. We taught manual mapping first on purpose: now that you know exactly what a mapper must do, MapStruct's generated *MapperImpl is demystified code you can read, not magic. Crucially, MapStruct does NOT resolve relationships by id (that is a database lookup) -- the service keeps owning id-resolution from Lab 3, and the mapper only shapes objects.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Why manual mapping first, and what an annotation processor is** — How a compile-time annotation processor runs during javac and generates plain Java source (no reflection at runtime), and why having written mappers by hand first means you can open and trust the generated code.
- **Setting up MapStruct with Lombok on Spring Boot 4.0 / Java 21** — Adding org.mapstruct:mapstruct:1.6.3 as a dependency and the mapstruct-processor (1.6.3), lombok, and lombok-mapstruct-binding (0.2.0) entries inside maven-compiler-plugin annotationProcessorPaths, with Lombok ordered BEFORE MapStruct so Lombok-generated getters/setters exist when MapStruct runs.
- **@Mapper components and Spring dependency injection** — Declaring a @Mapper(componentModel = "spring") interface so the generated mapper becomes an injectable Spring bean, injecting it into a service, and locating the generated *MapperImpl under target/generated-sources to read what it produced.
- **Field mapping: @Mapping, ignore, expression, and @MappingTarget update** — Renaming and ignoring fields and using expression/constant with @Mapping, and writing an in-place update method annotated with @MappingTarget for PUT handlers so untouched fields are not overwritten.
- **Nested relationship mapping (entity -> response only)** — Mapping Book -> BookResponseDto including nested AuthorSummaryDto / PublisherSummaryDto / List<CategorySummaryDto>, and the deliberate asymmetry: the request -> entity mapping IGNORES relationships because resolving authorId/publisherId/categoryIds to managed entities is a repository job the service still owns (carried over from Lab 3), not the mapper's.
- **Spring Data interface-based projections** — Defining closed projection interfaces (only entity-backed getters) and open projections (@Value with SpEL) so a repository query materialises only the named fields instead of full entities, including nested projection getters for associations.
- **Class-based (record) and dynamic projections** — Record/constructor projections, the generic <T> Class<T> dynamic-projection repository signature that lets one method return different shapes, and the rule that class-based projections cannot nest associations while interface-based ones can.

**🔨 Then build**

- Configure MapStruct + the Lombok binding in the Bookstore pom and confirm the generated *MapperImpl classes appear under target/generated-sources after a build.
- Write @Mapper(componentModel = "spring") mappers for Author, Publisher, Category, and Book, then delete the hand-written mapping helpers from the earlier labs.
- Implement Book -> BookResponseDto with nested author/publisher/category mapping, and a request -> entity mapping that @Mapping(ignore = true)s every relationship (the service still resolves ids -> entities and sets them).
- Add an @MappingTarget update method and call it from each PUT service method so partial updates no longer null out untouched fields.
- Introduce a BookListView interface projection (id, title, isbn, nested author last name) and back the paginated list endpoint with a projection query, so list reads stop materialising full Book entities (revisiting Lab 3's list-vs-detail split).
- Add a class-based record projection plus a dynamic <T> projection repository method, and with show-sql on, compare the columns fetched by the projection query against the full-entity query.
- Update the existing @WebMvcTest / service tests to prove the MapStruct output JSON byte-for-byte matches the old manual mapper's output (the API contract from Lab 3 must not change).

**🆕 New skills unlocked:** Configuring MapStruct with the Lombok binding under Spring Boot 4.0 / Java 21; Generating type-safe nested relationship mappers with @Mapper, and keeping id-resolution in the service; Writing in-place update mappings with @MappingTarget for partial PUT updates; Building read-optimized interface, class-based (record), and dynamic projections; Choosing projections vs full-entity fetches to reduce over-fetching.

---

## Phase 3 — Security

Lock the API down — authentication, stateless JWT sessions, and fine-grained authorization.

### Lab 6 — Spring Security Fundamentals 🆕

Adds the first real authentication and authorization layer to the Bookstore, turning open CRUD endpoints into a system with registered users, logins, and protected write operations. It is the foundation the entire Security phase builds on: Lab 7 (JWT) and Lab 8 (method security) both assume you understand the filter chain, the AuthenticationManager, UserDetailsService, and authorizeHttpRequests taught here. Note up front: in Spring Boot 4 / Spring Security 7, adding the security starter secures every endpoint by default and enforces CSRF aggressively, so the Bookstore endpoints from Lab 3 will start returning 401/403 until you write an explicit SecurityFilterChain — that breakage is the motivation for this lab.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **How Spring Security Actually Works: The Servlet Filter Chain** — Why every request passes through a chain of servlet filters before reaching your controller, what the DelegatingFilterProxy and FilterChainProxy do, and how a SecurityFilterChain bean plugs your rules into that pipeline (built from zero, assuming no prior security knowledge). This filter-chain mental model is reused directly by Lab 7's JWT filter and Lab 8's method security.
- **Authentication vs. Authorization (and the SecurityContext)** — The difference between proving who you are (authentication) vs. what you may do (authorization), plus how the Authentication object, the SecurityContext, and SecurityContextHolder carry the logged-in principal through a single request.
- **The SecurityFilterChain Bean in Spring Boot 4 / Spring Security 7** — Writing a modern SecurityFilterChain @Bean from a HttpSecurity object, using authorizeHttpRequests + requestMatchers — and that the Spring Security 5/6 APIs authorizeRequests, antMatchers, and WebSecurityConfigurerAdapter are REMOVED (not deprecated) in Spring Security 7, so older tutorials will not compile. Also covers that web apps are secured-by-default in 4.0.
- **Passwords Done Right: PasswordEncoder and BCrypt** — Why passwords are never stored in plaintext, how BCrypt salting and adaptive hashing work, and wiring a BCryptPasswordEncoder bean used for both encoding on registration and matching on login (encode vs. matches).
- **UserDetails and UserDetailsService: From In-Memory to DB-Backed Users** — The UserDetails / UserDetailsService contract, starting with an InMemoryUserDetailsManager to see it work, then implementing a custom UserDetailsService that loads a user from a JPA table and adapts it to UserDetails.
- **The AuthenticationManager and How a Login Is Verified** — How Spring Security actually checks credentials: the AuthenticationManager / AuthenticationProvider (DaoAuthenticationProvider) wiring your UserDetailsService and PasswordEncoder together, and how to obtain the AuthenticationManager bean — the piece Lab 7 will call directly to authenticate a login request.
- **Login Mechanisms and URL Rules: HTTP Basic, Form Login, Roles vs. Authorities** — How httpBasic() and formLogin() differ, and how to express access rules with hasRole / hasAuthority / authenticated — including the ROLE_ prefix convention (hasRole("ADMIN") matches authority ROLE_ADMIN) so roles and authorities behave as intended.

**🔨 Then build**

- Add a User JPA entity (username, BCrypt-hashed password, roles) plus a UserRepository, building on the Lab 3 Bookstore data model.
- Implement a custom UserDetailsService that loads Bookstore users from the database and maps their roles to granted authorities.
- Configure a BCryptPasswordEncoder bean and a POST /api/auth/register endpoint that hashes the password and persists a new user (returning a DTO, never the entity).
- Write a SecurityFilterChain that permits GET reads and the register endpoint, requires authentication for POST/PUT/DELETE on books, and restricts category/admin operations to an ADMIN role.
- Enable HTTP Basic and form login, expose the AuthenticationManager bean, then verify with an HTTP client that an anonymous write is 401, an authenticated USER write succeeds, and an ADMIN-only call is 403 for a plain USER.
- Seed an initial ADMIN user at startup and document the role-vs-authority mapping used in the Bookstore.

**🆕 New skills unlocked:** Configuring a SecurityFilterChain bean in Spring Security 7 (authorizeHttpRequests / requestMatchers); Implementing a DB-backed UserDetailsService and exposing the AuthenticationManager; Hashing and verifying passwords with BCryptPasswordEncoder; Securing URLs by HTTP method and role/authority; Building registration and login flows on a layered Spring Boot app.

### Lab 7 — Stateless Authentication with JWT 🆕

Replaces session/Basic auth with stateless, token-based authentication so the Bookstore API can scale horizontally and serve SPAs and mobile clients without server-side sessions. It builds directly on Lab 6's filter chain, AuthenticationManager, and UserDetailsService — reusing that filter-chain model rather than re-teaching it — and produces the access/refresh token machinery that real REST APIs depend on.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **What a JWT Is: Header, Payload, Signature** — The three base64url-encoded parts of a JWT, what standard claims (sub, iat, exp, plus a custom roles claim) mean, and why a signed JWT is tamper-evident and readable-but-not-secret rather than encrypted.
- **Stateless vs. Session Authentication** — Why server-side sessions don't scale horizontally, how a signed token moves trust to the client, and the security tradeoffs of going stateless (notably no easy server-side revocation, which motivates short access-token lifetimes).
- **Signing and Verifying Tokens: HMAC/HS256 vs. RSA** — How symmetric (HS256, one shared secret) and asymmetric (RS256, key pair) signatures prove a token was issued by your server and was not tampered with, and how to store the secret/keys safely (externalized config / environment, never hard-coded; the disciplined version of this arrives in Lab 13).
- **Choosing a JWT Library: jjwt vs. spring-boot-starter-oauth2-resource-server** — Comparing io.jsonwebtoken jjwt (lightweight, full control to self-issue tokens; current 0.13.x, split into the jjwt-api compile dependency plus jjwt-impl and jjwt-jackson runtime dependencies) against the OAuth2 resource server's Nimbus-based JwtEncoder/JwtDecoder, and picking jjwt for this self-issued-token lab — noting the resource-server path exists and that, in Spring Security 7, NimbusJwtDecoder validates the JWT 'typ' header by default, which trips up self-issued HS256 tokens unless you configure JwtTypeValidator.
- **Access Tokens vs. Refresh Tokens, Expiry and Claims** — Why access tokens are short-lived and refresh tokens long-lived, how the exp claim drives the refresh flow, and what is safe to put in claims (no secrets — the payload is readable).
- **Stateless Session Policy and CSRF for Token APIs** — Why a Bearer-token API sets SessionCreationPolicy.STATELESS (no HttpSession, no JSESSIONID), and why CSRF protection — on by default and aggressive in Spring Security 7 — is irrelevant to and safely disabled for an API authenticated by an Authorization header rather than a cookie.
- **The Once-Per-Request JWT Authentication Filter** — Writing a OncePerRequestFilter that reads the Authorization: Bearer header, validates the token, builds an Authentication, and places it in the SecurityContext — and registering it before UsernamePasswordAuthenticationFilter with addFilterBefore, slotting into Lab 6's filter chain.

**🔨 Then build**

- Add the jjwt dependencies (jjwt-api compile-scope, jjwt-impl and jjwt-jackson runtime-scope) and a JwtService that issues a signed HS256 access token with subject, roles, and expiry claims.
- Build a POST /api/auth/login endpoint that authenticates credentials via the AuthenticationManager from Lab 6 and returns an access token plus a refresh token.
- Implement a JwtAuthenticationFilter (OncePerRequestFilter) that extracts and validates the Bearer token and populates the SecurityContext.
- Reconfigure the SecurityFilterChain to be stateless (SessionCreationPolicy.STATELESS), disable CSRF for the token API, and register the JWT filter with addFilterBefore.
- Add a refresh-token flow: a POST /api/auth/refresh endpoint that validates a stored/long-lived refresh token and mints a new access token, with rotation or invalidation.
- Verify end to end that a request with a valid Bearer token reaches a protected Bookstore write, an expired or tampered token is rejected as 401, and refresh issues a working new access token.

**🆕 New skills unlocked:** Issuing and validating JWTs with jjwt (api/impl/jackson split); Building a stateless SecurityFilterChain (STATELESS policy, CSRF disabled for a token API); Writing a OncePerRequestFilter for Bearer-token authentication; Designing an access/refresh token flow with expiry and rotation; Reasoning about signing algorithms and secret management.

### Lab 8 — Method Security & Fine-Grained Authorization 🆕

Moves authorization from coarse URL rules down to individual methods and individual rows, so the Bookstore can enforce rules like 'a user may edit only their own review.' It builds on the authenticated principal from Labs 6-7 and completes the Security phase by adding ownership checks and a proper way to test them.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Method Security vs. URL Security: @EnableMethodSecurity** — Why URL-level rules aren't enough (they can't see method arguments or row ownership), how @EnableMethodSecurity turns on annotation-driven authorization and enables @PreAuthorize/@PostAuthorize by default in Spring Security 7, and where method security (an AOP advisor on beans) sits relative to Lab 6's filter chain.
- **@PreAuthorize and @PostAuthorize with SpEL** — Writing pre- and post-invocation authorization rules using Spring Expression Language: hasRole / hasAuthority, the authentication object, and referencing method arguments with #param — and the distinction that @PreAuthorize gates entry while @PostAuthorize inspects the returned object.
- **@PostFilter and @PreFilter for Collections** — Why filtering the elements of a returned collection (dropping rows the principal may not see) needs @PostFilter with filterObject — distinct from @PostAuthorize, which allows-or-denies a single return value rather than trimming a list.
- **Ownership and Row-Level Checks** — Enforcing that the current principal owns a resource before mutating it, using an authentication.name / owner comparison in SpEL or a custom @Bean permission evaluator referenced from a SpEL expression for non-trivial rules.
- **Testing Security with spring-security-test and @WithMockUser** — Adding the spring-security-test dependency and using @WithMockUser (including @WithMockUser(username=..., roles=...)) to assert that protected methods and endpoints allow or deny the right principals, and how it populates the SecurityContext in a test.

**🔨 Then build**

- Enable method security with @EnableMethodSecurity on the Bookstore security configuration.
- Introduce an owner-scoped resource — book Reviews owned by the creating user — with an owner reference (the User from Lab 6) on the new Review entity.
- Annotate service methods with @PreAuthorize so only ADMIN can delete any review while any authenticated user can create one.
- Add an ownership rule (an authentication.name / owner SpEL comparison, or a custom permission-evaluator bean) so a user may update or delete only their own review, returning 403 otherwise.
- Use @PostFilter on a list endpoint to return only the reviews the principal is allowed to see, and @PostAuthorize on a single-resource read to deny returning one they may not.
- Write spring-security-test cases with @WithMockUser covering: owner can edit own review, non-owner is forbidden (403), and ADMIN can override.

**🆕 New skills unlocked:** Enabling and applying @EnableMethodSecurity; Authoring @PreAuthorize / @PostAuthorize rules with SpEL; Filtering collections with @PostFilter vs. allow/deny with @PostAuthorize; Implementing ownership / row-level authorization checks; Writing security tests with spring-security-test and @WithMockUser.

---

## Phase 4 — Containerization & Testing

Learn Docker first, then use it to test against real infrastructure instead of fakes.

### Lab 9 — Docker & Containerization Fundamentals 🆕

Takes the Bookstore app from 'runs on my machine' to a portable, reproducible container image that boots alongside its own PostgreSQL with a single command. This is the foundational infrastructure lab: every later lab that needs real infrastructure — Testcontainers (11), Redis (17), RabbitMQ (20), and the Prometheus/Grafana stack (24) — assumes you can build images and run docker compose, so we teach Docker and Compose from absolute zero here and those labs only add a service, never re-teach Compose.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Why Containers: VMs vs. Containers and the Docker Engine** — What a container and an image are, how containers share the host kernel and differ from virtual machines, and what the Docker engine/daemon, client, and registry (Docker Hub) actually do.
- **Images vs. Containers and the Docker CLI** — The image-as-template vs. container-as-running-instance distinction, image tags and layers, and the core lifecycle commands: docker pull/images, docker run/ps/stop/rm, docker logs, and docker exec.
- **Writing a Dockerfile: Instructions and the Layer Cache** — FROM, WORKDIR, COPY, RUN, ENV, EXPOSE, ENTRYPOINT/CMD, how each instruction creates a cached layer, and ordering instructions (dependencies before source) so code changes do not bust the dependency cache.
- **Multi-Stage Builds and the Spring Boot Layered Jar** — Using multiple FROM stages to build the fat jar with a JDK image and run it on a slim JRE base; extracting the Spring Boot layered jar with the Spring Boot 4 tools jarmode (java -Djarmode=tools -jar app.jar extract --layers, which supersedes the removed layertools mode) for cache-friendly rebuilds; and adding a dedicated non-root user (USER) to the runtime stage so the container does not run as root.
- **Runtime Wiring: Ports, Volumes, and Environment Variables** — Publishing ports (-p host:container), persisting data with named volumes vs. bind mounts, and injecting configuration (SPRING_DATASOURCE_* / SPRING_PROFILES_ACTIVE) via -e environment variables instead of baking it into the image (the deeper 12-factor treatment of this lands in Lab 13).
- **Docker Compose: Multi-Container Apps on a Shared Network** — Declaring app + Postgres services in compose.yaml, service-name DNS on the default bridge network, depends_on with condition: service_healthy backed by a Postgres healthcheck, named volumes for Postgres data, and docker compose up/down/logs — the reusable Compose foundation that Labs 17, 20, and 24 extend with one more service each.

**🔨 Then build**

- Write a multi-stage Dockerfile that builds the Bookstore fat jar in a JDK stage and runs the extracted layered jar on a slim JRE base image as a dedicated non-root user.
- Build the image with docker build (tagged bookstore:1.0) and run it standalone with docker run, publishing port 8080 and confirming /actuator/health responds.
- Author a compose.yaml that starts the Bookstore container plus a postgres:16 service on a shared network, with a named volume for Postgres data and a healthcheck so the app's depends_on waits for Postgres to be ready.
- Wire the app to the Postgres service entirely through SPRING_DATASOURCE_* environment variables in compose (no hardcoded credentials in the image), resolving the database host by its compose service name.
- Bring the whole stack up with docker compose up, exercise the Bookstore REST API end-to-end against the containerized Postgres, then docker compose down (without -v) and prove data survives a restart via the named volume.
- Add a .dockerignore and verify layered-jar caching by changing one source file and observing that only the application layer rebuilds while the dependency layers stay cached.

**🆕 New skills unlocked:** Explaining containers vs. VMs and the Docker architecture; Driving the Docker CLI (build, run, ps, logs, exec, volumes); Writing efficient multi-stage Dockerfiles with layer-cache awareness; Producing small, secure Spring Boot runtime images from a layered jar (tools jarmode) running as a non-root user; Orchestrating multi-container apps with docker compose, healthchecks, and shared networks; Externalizing configuration into a container via environment variables and named volumes.

### Lab 10 — Integration Testing with @SpringBootTest 🆕

Moves beyond the isolated unit and slice tests of Labs 1-3 to full-context integration tests that boot the real Spring container and exercise the Bookstore controller-to-database flow end to end. It teaches where integration tests sit in the test pyramid and how to keep them fast, isolated, and deterministic before we swap in real infrastructure with Testcontainers in Lab 11. Crucially, it uses the Spring Boot 4 testing stack: HTTP test clients are no longer auto-configured, TestRestTemplate is deprecated, and RestTestClient is the new unified client.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **The Test Pyramid: Unit vs. Slice vs. Integration** — The cost/speed/confidence tradeoff across unit tests (no Spring), slice tests (@WebMvcTest/@DataJpaTest from earlier labs), and full @SpringBootTest integration tests, and when each layer is worth its runtime.
- **@SpringBootTest: Booting the Full Application Context** — How @SpringBootTest (org.springframework.boot.test.context.SpringBootTest, unchanged in Spring Boot 4) loads the complete context, the webEnvironment modes (MOCK vs. RANDOM_PORT) and what each means for whether a real servlet container starts, and context caching across test classes for fast suites.
- **Driving the API in Spring Boot 4: RestTestClient and MockMvcTester** — That Spring Boot 4 no longer auto-configures HTTP test clients (TestRestTemplate is deprecated and not provided by @SpringBootTest), so you opt in with @AutoConfigureRestTestClient and inject RestTestClient (org.springframework.test.web.servlet.client.RestTestClient) — one fluent client that targets either MockMvc (webEnvironment=MOCK) or a real socket (RANDOM_PORT) — plus MockMvcTester, the AssertJ-based replacement for raw MockMvc, for fine-grained server-side assertions.
- **Test Data Builders and Fixtures** — Building readable, intention-revealing test data with builder helpers and object mothers for the Bookstore entities/DTOs, avoiding brittle shared setup and duplicated arrange code.
- **Seeding Data with @Sql** — Using @Sql (and @SqlGroup) to seed and clean known database state per class or per method, executionPhase control (BEFORE_TEST_METHOD vs. AFTER_TEST_METHOD), and when SQL seeding beats programmatic setup.
- **Test Isolation: Transactional Rollback Between Tests** — How @Transactional on a test rolls back automatically for isolation, why that rollback does NOT apply to real-socket tests on RANDOM_PORT (the server runs the request in its own transaction on a separate thread), and the cleanup strategies (@Sql AFTER_TEST_METHOD scripts, @DirtiesContext) you must use instead when rollback cannot apply.

**🔨 Then build**

- Stand up a @SpringBootTest(webEnvironment = RANDOM_PORT) @AutoConfigureRestTestClient base test class for the Bookstore that the whole integration suite reuses (shared context for fast runs).
- Write an end-to-end RestTestClient test against the running server that creates an Author, posts a Book referencing it, fetches the Book back, and asserts the nested response DTO and HTTP status codes with the fluent expectStatus/expectBody API.
- Add a MockMvcTester-based integration test (webEnvironment=MOCK) covering a validation failure and a 409 conflict (deleting an Author that still has books), asserting the RFC 7807 ProblemDetail body with AssertJ.
- Create reusable test data builders/object mothers for Author, Book, Publisher, and Category DTOs and refactor the suite to use them.
- Seed a known catalog with @Sql scripts and write a paginated/sorted GET test that asserts page metadata and ordering against the seeded data.
- Demonstrate test isolation: contrast a @Transactional-rollback integration test with a real-socket RestTestClient test on RANDOM_PORT that must clean up via an @Sql AFTER_TEST_METHOD script, and document exactly why rollback cannot reach the server-side transaction.

**🆕 New skills unlocked:** Choosing the right test layer for a given scenario (pyramid reasoning); Writing full-context @SpringBootTest integration tests with the correct webEnvironment; Driving the API with the Spring Boot 4 stack: RestTestClient (mock and real-socket) and MockMvcTester; Building maintainable test data with builders and object mothers; Seeding and cleaning database state with @Sql and executionPhase control; Guaranteeing test isolation via transactional rollback, and knowing when real-socket tests force explicit cleanup instead.

### Lab 11 — Real Dependencies in Tests with Testcontainers 🆕

Replaces the in-memory H2 database used in earlier tests with a real PostgreSQL spun up in a throwaway container, so integration and repository tests run against the exact engine production uses. It builds directly on Lab 9 (Docker must be installed and running) and Lab 10 (the integration suite), closing the 'works in tests, breaks in prod' gap caused by dialect differences. It also sets up the test infrastructure that Lab 12's Flyway migrations and Lab 25's CI pipeline run against.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Why H2 Lies: Real Postgres vs. In-Memory Databases** — Concrete ways H2 (even in PostgreSQL-compatibility mode) diverges from real Postgres — SQL dialect, native queries, types like jsonb/uuid/arrays, sequences, identifier casing, and constraint/locking behavior — and why those gaps hide bugs until production.
- **What Testcontainers Is** — How Testcontainers programmatically launches disposable Docker containers from your test code, its dependence on the Docker engine installed in Lab 9, and the lifecycle (start, expose a mapped random port, stop) it manages for you.
- **JUnit 5 Integration: @Testcontainers and @Container** — Wiring the JUnit 5 extension with @Testcontainers (org.testcontainers.junit.jupiter), declaring a PostgreSQLContainer (org.testcontainers:postgresql module) with @Container, and the difference between a static field (one container per class) and an instance field (one per test method).
- **Zero-Config Wiring with Spring Boot 4 @ServiceConnection** — Using the spring-boot-testcontainers dependency and @ServiceConnection (org.springframework.boot.testcontainers.service.connection) to auto-bind the datasource to the container with no manual @DynamicPropertySource, and the Spring Boot 4 gotcha that a container declared as a @Bean method (rather than a typed @Container field) needs @ServiceConnection(name = "postgres") or it fails with ConnectionDetailsFactoryNotFoundException.
- **Container Lifecycle, Reuse, and Keeping Tests Fast** — Sharing one container across the whole suite via a static singleton in a base class, Testcontainers reuse (testcontainers.reuse.enable in ~/.testcontainers.properties), and why both avoid the per-test startup tax that makes container tests feel slow.

**🔨 Then build**

- Add the spring-boot-testcontainers test dependency plus the Testcontainers JUnit 5 (junit-jupiter) and PostgreSQL (postgresql) modules to the Bookstore build, managed by the Testcontainers BOM.
- Migrate the @DataJpaTest repository tests from H2 to a real PostgreSQLContainer, asserting that a Postgres-specific JPQL/native query that H2 mishandled now behaves correctly.
- Refactor the Lab 10 @SpringBootTest integration suite to use a shared static PostgreSQLContainer wired via @ServiceConnection (no @DynamicPropertySource), removing the H2 test config entirely.
- Demonstrate the Spring Boot 4 gotcha by declaring the container as a @Bean method in a @TestConfiguration, reproducing ConnectionDetailsFactoryNotFoundException, then fixing it with @ServiceConnection(name = "postgres").
- Introduce a single AbstractIntegrationTest base class that holds the static container so it starts once for the whole suite, and measure the wall-clock improvement versus a per-class container.
- Enable Testcontainers container reuse locally and document the dev-loop speedup plus when reuse is safe versus when a fresh container is required.

**🆕 New skills unlocked:** Articulating why real database parity beats H2 for test fidelity; Launching disposable infrastructure from tests with Testcontainers; Wiring @Testcontainers/@Container with the PostgreSQL module and BOM; Zero-config datasource binding via Spring Boot 4 @ServiceConnection (and its @Bean-method name gotcha); Sharing a static container and enabling reuse to keep integration tests fast; Migrating an existing H2-based test suite to real Postgres.

---

## Phase 5 — Production Readiness

Everything that turns a working app into a shippable one: migrations, configuration, docs, and operational endpoints.

### Lab 12 — Database Migrations with Flyway 🆕

Replaces the convenient-but-dangerous Hibernate ddl-auto schema management with versioned, reviewable Flyway migrations so the Bookstore schema evolves safely and reproducibly across every environment. This is the first true 'production readiness' lab: it converts the schema from an implicit side effect of your entities into an explicit, auditable artifact, and it builds directly on the real-Postgres Testcontainers foundation from Lab 11 so tests exercise the exact production schema.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Why ddl-auto Is Dangerous in Production** — How Hibernate's ddl-auto=update/create-drop works, why update silently mismatches or fails to apply destructive changes and create-drop destroys real data, and why production schemas must be controlled by explicit migrations instead.
- **What Flyway Is and How Versioned Migrations Work** — From zero: Flyway as a migration runner, plain SQL migration scripts, the flyway_schema_history table, checksums, and how Flyway decides which scripts have and haven't been applied.
- **Migration Naming, Versioning, and Ordering** — The V__/U__/R__ prefix conventions, version-number ordering rules, file placement under src/main/resources/db/migration, and what makes a versioned migration immutable once applied (changing an applied script's checksum is an error).
- **Repeatable Migrations and Seed Data** — When to use R__ repeatable migrations (views, functions, reference/seed data) versus versioned V__ scripts, how Flyway runs repeatables after all versioned ones, and how checksum-based re-execution of repeatables works.
- **Wiring Flyway into Spring Boot 4.0** — From zero: that Spring Boot 4.0's modularized auto-configuration requires the spring-boot-starter-flyway starter (org.springframework.boot) plus the database-specific flyway-database-postgresql module, key spring.flyway.* properties, and how Boot runs migrations automatically on startup before the JPA EntityManager initializes.
- **Baselining an Existing Schema** — From zero: what baselining means, the baseline-on-migrate and baseline-version properties, and how to adopt Flyway on a database that already has tables without re-creating them.
- **Migrations in the Test Pipeline** — How Flyway runs against the Lab 11 Testcontainers Postgres instance so tests exercise the exact production schema, replacing schema-from-entities in @DataJpaTest, and why this catches migration bugs early.

**🔨 Then build**

- Disable destructive schema generation by setting spring.jpa.hibernate.ddl-auto=validate, and add spring-boot-starter-flyway plus flyway-database-postgresql to the Bookstore build.
- Author V1__create_bookstore_schema.sql that creates the books, authors, categories, and join tables matching the Lab 3 entities, with the correct keys, foreign keys, and indexes.
- Add V2__add_book_isbn_unique_constraint.sql as an incremental, immutable migration that evolves the schema after V1.
- Add an R__seed_reference_data.sql repeatable migration that seeds baseline categories and demo authors/books.
- Confirm Hibernate validates cleanly (ddl-auto=validate) against the Flyway-built schema, proving entities and migrations agree, and intentionally introduce then fix a mismatch to see validation fail.
- Configure @DataJpaTest / Testcontainers so Flyway runs the same migrations in tests, and write a test that asserts flyway_schema_history contains the expected applied versions.
- Demonstrate baselining: point Flyway at a database that already has tables and use baseline-on-migrate to onboard it without errors.

**🆕 New skills unlocked:** Writing versioned and repeatable Flyway SQL migrations; Replacing ddl-auto with migration-driven schema management; Using ddl-auto=validate to keep entities and schema in sync; Baselining Flyway onto an existing database; Running migrations against Testcontainers in the test pipeline.

### Lab 13 — Profiles & Externalized Configuration 🆕

Teaches the student to cleanly separate configuration from code so one Bookstore build runs correctly in dev, test, and prod without recompilation, following 12-factor principles. It deepens the env-var injection first met in Lab 9's Docker wiring and the secret-handling raised in Lab 7's JWT keys, builds on the Flyway-managed schema from Lab 12 (different datasources per environment), and is the prerequisite for safely operating the app: documentation (Lab 14) and operational endpoints (Lab 15) all need environment-aware, secret-safe configuration.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Externalized Configuration and Spring's Environment Abstraction** — From zero: what 'externalized config' means, the Environment/PropertySource model, and how Spring resolves a property at runtime from many possible sources.
- **Spring Profiles and application-{profile} Files** — From zero: what a profile is, activating profiles via spring.profiles.active, per-profile property files (application-{profile}.yml), and the spring.config.import / multi-document YAML model for organizing dev/test/prod config.
- **Property Precedence and Override Order** — The concrete order Spring uses (command-line args > environment variables > application-{profile} > application > defaults), and how later sources override earlier ones, building on the profile files introduced above.
- **Type-Safe Binding with @ConfigurationProperties** — From zero: binding grouped properties into a typed record/class with @ConfigurationProperties, validating the bound values with Jakarta Validation (@Validated plus constraint annotations), relaxed binding, and why this beats scattered @Value injections.
- **Environment Variables and 12-Factor Config** — The 12-factor 'config in the environment' principle, mapping ENV_VAR names to Spring properties via relaxed binding, and why this is essential for containers (Lab 9) and CI (Lab 25).
- **Protecting Secrets** — Why secrets must never live in committed property files, supplying them via environment variables / external config (formalizing the JWT-secret handling from Lab 7), and the role of placeholder defaults and .gitignore'd local overrides.
- **@Profile-Conditional Beans** — Activating or swapping beans per environment with @Profile (e.g., a real vs. stub component), and how conditional bean wiring differs from conditional property values.

**🔨 Then build**

- Split Bookstore config into application.yml plus application-dev.yml, application-test.yml, and application-prod.yml with environment-appropriate datasource and logging settings.
- Create a typed @ConfigurationProperties record (e.g., BookstoreProperties for paging defaults and feature flags) with Jakarta Validation constraints and inject it into a service.
- Drive prod datasource credentials entirely from environment variables using ${DB_URL}/${DB_USERNAME}/${DB_PASSWORD} placeholders, with no secrets committed.
- Demonstrate override order by overriding one property via a command-line arg and via an environment variable, observing precedence.
- Register a @Profile('dev') stub bean and a @Profile('prod') real implementation of the same interface, and verify the correct one loads per active profile.
- Ensure the @DataJpaTest/@WebMvcTest slices from earlier labs run under the test profile with the test-specific configuration.

**🆕 New skills unlocked:** Structuring multi-environment config with Spring profiles; Type-safe property binding via @ConfigurationProperties with validation; Applying property precedence and override rules deliberately; Injecting environment-variable config the 12-factor way; Keeping secrets out of source and wiring @Profile-conditional beans.

### Lab 14 — API Documentation with OpenAPI & Swagger UI 🆕

Turns the Bookstore's controllers and DTOs into living, interactive API documentation so consumers (and your future self) can explore and call endpoints without reading source. It builds on the layered controller/DTO/ProblemDetail architecture from every prior lab, on the JWT security from Lab 7 (the bearer scheme it documents), and on the profile-aware config from Lab 13 (docs are exposed differently per environment), giving the API a professional, self-describing contract.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **What the OpenAPI Specification Is** — From zero: OpenAPI as a standard, language-agnostic JSON/YAML description of an HTTP API (paths, operations, schemas, responses) and why a machine-readable contract matters.
- **What Swagger UI Does** — From zero: how Swagger UI renders an OpenAPI document into a browsable page with 'Try it out' request execution, and how it differs from the spec document itself.
- **Wiring springdoc-openapi into Spring Boot 4.0** — From zero: adding the springdoc-openapi-starter-webmvc-ui dependency (the 3.0.x line built for Spring Boot 4 / Spring Framework 7 / Jakarta), the default /v3/api-docs and /swagger-ui.html endpoints, and how docs auto-generate from your existing controllers.
- **How Docs Are Derived from Controllers and DTOs** — How springdoc reads @RestController mappings, request/response DTO types, and validation annotations to produce paths and schemas automatically, with zero hand-written spec.
- **Enriching Docs with Annotations** — Using @Operation, @Parameter, @Schema, @ExampleObject, and @Tag to add summaries, descriptions, field constraints, and realistic examples to the generated documentation.
- **Documenting Errors and Security Schemes** — Documenting @ApiResponse error responses tied to your ProblemDetail (RFC 7807) bodies, and declaring a security scheme matching Lab 7's bearer JWT so protected endpoints display their auth requirement and can send the Authorization header from Swagger UI.
- **Customizing and Exposing the Docs** — Configuring API metadata via an OpenAPI bean (title, version, contact, license) and using springdoc properties to control or restrict where Swagger UI is served per profile (building on Lab 13).

**🔨 Then build**

- Add springdoc-openapi-starter-webmvc-ui (3.0.x) and confirm /swagger-ui.html and /v3/api-docs render the existing Bookstore endpoints with no other changes.
- Define a global OpenAPI bean providing title, version, description, and contact metadata for the Bookstore API.
- Annotate the Book and Author endpoints with @Operation summaries/descriptions, @Parameter docs, and @Tag groupings.
- Add @Schema constraints and @ExampleObject request/response examples to the Book DTOs so the docs show realistic payloads.
- Document the standard error responses (400/404/409) using @ApiResponse referencing the ProblemDetail schema produced by the global handler.
- Declare a bearer-JWT security scheme matching Lab 7 and mark a protected endpoint so Swagger UI displays the auth requirement and can send the Authorization header.
- Restrict Swagger UI exposure by profile (enabled in dev, disabled or locked down in prod) using springdoc properties.

**🆕 New skills unlocked:** Auto-generating OpenAPI docs from Spring controllers with springdoc; Serving and using interactive Swagger UI; Enriching docs with @Operation/@Schema/@ExampleObject; Documenting RFC 7807 error responses and security schemes; Controlling documentation exposure per environment.

### Lab 15 — Operational Endpoints with Spring Boot Actuator 🆕

Makes the Bookstore observable and operable in production by exposing standardized health, info, and metrics endpoints that platforms and orchestrators rely on. It is the capstone of Phase 5 and the single place Actuator is taught from zero — Labs 22, 24, and 25 all reuse the endpoints and exposure rules established here rather than re-teaching them. It builds on profile-aware config (Lab 13) for secure exposure, complements the documented API (Lab 14), and depends on the real datasource/Flyway setup (Lab 12) so health checks reflect actual infrastructure state.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **What Spring Boot Actuator Provides** — From zero: Actuator as a set of built-in production endpoints under /actuator, what comes out of the box, and why operational endpoints are kept separate from business endpoints. This is the foundational Actuator module the whole course refers back to.
- **The Core Endpoints: health, info, metrics** — What /actuator/health, /actuator/info, and /actuator/metrics report, the health status model (UP/DOWN plus details), and how built-in indicators (datasource, disk, etc.) contribute to overall status.
- **Exposing and Securing Endpoints** — From zero: management.endpoints.web.exposure include/exclude (only health and info are exposed over HTTP by default), why most endpoints are hidden, running management on a separate port, and locking endpoints down with auth/roles — the exposure model Labs 22, 24, and 25 reuse.
- **Custom HealthIndicators** — From zero: implementing the HealthIndicator interface to report the health of a domain-specific dependency, and how custom indicators aggregate into the overall health status.
- **Liveness vs. Readiness Probes** — From zero: the difference between liveness and readiness, the /actuator/health/liveness and /readiness probe groups (auto-enabled when Kubernetes is detected, otherwise turned on with management.endpoint.health.probes.enabled=true), and why orchestrators use each to restart vs. route traffic — the readiness probe Lab 25 gates its rollout on.
- **Build and Git Info Endpoints** — From zero: populating /actuator/info with build metadata (the Spring Boot Maven plugin's build-info goal) and git commit details (the git-commit-id-maven-plugin) so a running instance reports exactly which version it is.

**🔨 Then build**

- Add spring-boot-starter-actuator to the Bookstore and expose health, info, and metrics via management.endpoints.web.exposure.include.
- Verify /actuator/health reports the Flyway/Postgres datasource indicator as UP and shows component details when authorized.
- Implement a custom HealthIndicator (e.g., a BookstoreCatalogHealthIndicator) and confirm it aggregates into overall health.
- Enable the probe groups with management.endpoint.health.probes.enabled=true and confirm /actuator/health/liveness and /actuator/health/readiness respond correctly outside Kubernetes.
- Populate /actuator/info with build-info (Spring Boot Maven plugin) and git commit info (git-commit-id-maven-plugin).
- Secure the actuator endpoints so sensitive ones require authentication, and expose them appropriately per profile (open in dev, locked in prod).
- Inspect an HTTP request metric (e.g., http.server.requests) via /actuator/metrics to observe request timings.

**🆕 New skills unlocked:** Exposing and securing Spring Boot Actuator endpoints; Reading and interpreting health, info, and metrics endpoints; Writing custom HealthIndicators; Configuring liveness/readiness probes for orchestrators; Surfacing build and git version info on a running instance.

---

## Phase 6 — Caching & Performance

Make it fast — first the cache abstraction, then a real Redis cache.

### Lab 16 — The Spring Cache Abstraction (In-Memory First) 🆕

Introduces caching as a first-class performance tool by layering Spring's declarative cache abstraction over the Bookstore's hot read paths, using only the built-in in-memory ConcurrentMapCacheManager so the student learns the programming model with zero external infrastructure. It builds directly on the Lab 3 Bookstore service/repository layers and unlocks Lab 17, where the exact same annotations are repointed at Redis without touching business code.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Caching fundamentals: what, why, when, and the staleness risk** — What a cache is, the read-through / cache-aside mental model, when caching pays off (expensive or repeated reads) vs. when it hurts (write-heavy or rarely-repeated reads), and the core tradeoff: serving stale data. Frames why every later decision (eviction, TTL, invalidation) exists to manage staleness.
- **The Spring cache abstraction and @EnableCaching** — How Spring decouples 'what to cache' (annotations on methods) from 'where to store it' (a CacheManager bean), the role of @EnableCaching to switch the proxy machinery on, and that the abstraction is provider-agnostic by design — the same annotations work over any backing store. Unchanged in Spring Boot 4.0 (lives in spring-context).
- **ConcurrentMapCacheManager: in-memory caching with no infrastructure** — How adding spring-boot-starter-cache lets Spring Boot auto-configure a simple ConcurrentHashMap-backed CacheManager by default, how to declare named caches up front via spring.cache.cache-names, and its deliberate limitations: single-JVM only, no TTL, no eviction policy, lost on restart — exactly the gaps Lab 17 fixes.
- **@Cacheable, @CachePut, and @CacheEvict** — The three core annotations and their distinct jobs: populate-on-miss / return-on-hit reads (@Cacheable), always-execute-and-overwrite writes that keep the cache fresh (@CachePut), and entry or all-entries removal on mutation (@CacheEvict, including allEntries and beforeInvocation). Stresses that @Cacheable and @CachePut are NOT interchangeable.
- **Cache keys and SpEL: key generation and conditional caching** — How a default key is derived from a method's arguments, how to write explicit key, unless, and condition expressions in SpEL (e.g. #id, #book.isbn, #result), and why an explicit key matters when several methods share one named cache so entries cannot collide.
- **Verifying cache behavior in a test with @MockitoSpyBean** — How to prove a cache works inside a @SpringBootTest: wrap the real repository with Spring Boot 4.0's @MockitoSpyBean (the replacement for the older @SpyBean), call a cached method repeatedly, and assert with Mockito.verify(..., times(1)) that the repository was hit exactly once, then once more after an eviction. Gives the student a repeatable way to measure correctness rather than guessing.
- **The proxy self-invocation pitfall and measuring effectiveness** — Why an internal this.method() call bypasses the caching proxy entirely (the annotation silently does nothing) — the same Spring AOP proxy trap that resurfaces with @Async in Lab 18 — how to recognise and fix it (call through the injected bean / restructure), and how to demonstrate a real hit by measuring repeat-call latency and counting repository invocations.

**🔨 Then build**

- Add spring-boot-starter-cache and @EnableCaching, then declare named Bookstore caches (e.g. booksById, booksByIsbn) via spring.cache.cache-names, backed by the default ConcurrentMapCacheManager.
- Annotate the hot read paths (getBookById, findByIsbn) with @Cacheable using an explicit SpEL key, and verify a second call returns without hitting the repository.
- Keep the cache coherent under writes: @CachePut on updateBook so the returned book refreshes its entry, and @CacheEvict on deleteBook plus on a key-changing ISBN update so no stale entry survives.
- Add conditional caching: unless to skip caching empty/absent results, and condition to cache only when it is worthwhile (e.g. a result above a size threshold).
- Write a @SpringBootTest that wraps the repository with @MockitoSpyBean and asserts it is invoked exactly once across repeated reads of the same key, then invoked again after a mutation evicts the entry.
- Reproduce the self-invocation pitfall: show an internal call whose @Cacheable does nothing, fix it, and record before/after repeat-call latency in the lab notes.

**🆕 New skills unlocked:** Applying Spring's declarative cache abstraction to existing service code with @EnableCaching; Using @Cacheable / @CachePut / @CacheEvict correctly to keep a cache coherent under mutation; Writing SpEL cache keys and conditional (condition / unless) caching rules; Configuring named in-memory caches with ConcurrentMapCacheManager; Verifying cache hits and evictions in a test with @MockitoSpyBean and Mockito.verify; Diagnosing the proxy self-invocation pitfall and measuring cache effectiveness.

### Lab 17 — Redis as a Cache & Store 🆕

Replaces the in-memory cache from Lab 16 with Redis, a production-grade store that survives restarts, is shared across application instances, and supports TTL — repointing the exact same @Cacheable / @CachePut / @CacheEvict code at a real key-value store and confronting the concerns that only appear in production: serialization, expiry, invalidation, and cache stampede. It requires Docker Compose (Lab 9) to run Redis and the cache abstraction (Lab 16), and unlocks any later work needing shared state across instances.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Redis from zero: an in-memory key-value store and its core data types** — What Redis is and why it is fast, the key-value model, and the core data types (string, hash, list, set, sorted-set) with a one-line use case for each — enough to know which type a cache entry, a counter, and a recent-views list each map to.
- **TTL, expiry, and redis-cli** — Setting and inspecting per-key expiry (EXPIRE / TTL), the difference between expiry (time-based) and eviction (memory-pressure-based), and driving Redis interactively with redis-cli (SET / GET / HGETALL / KEYS / TTL / FLUSHDB) so the student can see what the app actually stores.
- **Adding Redis as a Docker Compose service** — Reusing Lab 9's Compose skills to add a Redis service to the existing compose.yaml (image, port 6379, named volume for persistence) and opening redis-cli inside the running container with docker compose exec to confirm it is live — a new service on the same stack, no new tooling and no Compose re-teaching.
- **Swapping the Spring cache provider to Redis** — How adding spring-boot-starter-data-redis (Lettuce client by default) causes Spring Boot 4.0 to auto-configure a RedisCacheManager, so Lab 16's unchanged annotations now write to Redis with zero business-code change, plus the connection properties (spring.data.redis.host / port / etc.). Reinforces the payoff of the provider-agnostic abstraction.
- **Serialization and per-cache TTL with RedisCacheConfiguration** — Why values must be serialized to bytes before Redis can store them, configuring readable JSON with GenericJacksonJsonRedisSerializer (the Jackson 3 / tools.jackson serializer in Spring Data Redis 4.0 — its Jackson-2 predecessor GenericJackson2JsonRedisSerializer is deprecated), and setting a global default plus per-cache TTLs via a RedisCacheManagerBuilderCustomizer.
- **Cache-aside vs. write-through, invalidation, and the cache-stampede problem** — The cache-aside and write-through patterns and which one @Cacheable/@CachePut implement, keeping Redis coherent under mutation via @CacheEvict / @CachePut, and how many concurrent misses on the same expired key cause a stampede on the database — plus mitigations (TTL jitter, and @Cacheable(sync=true) to serialise the recompute).
- **RedisTemplate for direct, non-cache use** — Using StringRedisTemplate / RedisTemplate to read and write Redis directly (opsForValue, opsForHash, and atomic INCR) for use cases that the declarative cache abstraction does not cover, such as counters and recent-activity lists.

**🔨 Then build**

- Add a Redis service to the Lab 9 compose.yaml (port 6379, named volume) and confirm connectivity via docker compose exec ... redis-cli (SET / GET / TTL) before touching the app.
- Add spring-boot-starter-data-redis and configure spring.data.redis.* so the unchanged Lab 16 @Cacheable annotations now persist Bookstore reads into Redis.
- Configure a RedisCacheManagerBuilderCustomizer using GenericJacksonJsonRedisSerializer for JSON values and distinct per-cache TTLs (e.g. a short TTL for book lists, a longer one for single books), then inspect the stored JSON and remaining TTL via redis-cli.
- Prove cross-restart and cross-instance survival: populate the cache, restart the app, and show the read is still served from Redis (a hit) rather than the database.
- Verify invalidation under mutation: update and delete a book and confirm via redis-cli that the corresponding Redis keys are refreshed (@CachePut) or removed (@CacheEvict).
- Demonstrate and mitigate cache stampede: simulate concurrent misses on a just-expired key, then apply a mitigation (@Cacheable(sync=true) and/or TTL jitter) and document the before/after difference.
- Use StringRedisTemplate / RedisTemplate directly to implement one non-cache feature — e.g. a per-book view counter via INCR or a recent-views list — proving direct access beyond declarative caching.

**🆕 New skills unlocked:** Operating Redis via redis-cli, the core Redis data types, and TTL vs. eviction semantics; Running Redis as a Docker Compose service alongside the application and reaching its CLI; Repointing the Spring cache abstraction from in-memory to a Redis-backed RedisCacheManager with no business-code change; Configuring Redis cache serialization with the Jackson 3 GenericJacksonJsonRedisSerializer and per-cache TTLs; Reasoning about cache-aside vs. write-through, invalidation, and cache-stampede mitigation (sync=true, TTL jitter); Using RedisTemplate / StringRedisTemplate for direct key-value operations beyond declarative caching.

---

## Phase 7 — Messaging, Async & Events

Decouple work in time and space — background tasks, in-process events, and a real message broker.

### Lab 18 — Asynchronous & Scheduled Tasks 🆕

Teaches how to run work off the request thread (@Async) and on a clock (@Scheduled) so slow or recurring jobs like report generation and email no longer block API responses. This is the first lab in Phase 7 and the foundation for decoupling: it establishes the threading, executor, and proxy model that the events lab (19) and the messaging lab (20) build on.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Synchronous vs. Asynchronous Execution (the mental model)** — What 'on the request thread' means, why a slow call (PDF/email) blocks the HTTP response, and what offloading to a background thread buys you and costs you (no inline return value, eventual consistency, error visibility, and the need to communicate 'accepted, not done' to the caller).
- **Threads, Thread Pools & the TaskExecutor Abstraction in Spring** — Threads vs. a bounded pool, what core/max pool size and queue capacity mean, the Spring TaskExecutor/AsyncTaskExecutor abstraction, and why an unbounded or shared pool is a production hazard.
- **@EnableAsync and @Async — Running Methods in the Background** — How @EnableAsync installs a proxy around async beans, how @Async dispatches a method to a TaskExecutor, the allowed return types (void / Future / CompletableFuture), and how to name and select a specific executor by bean name with @Async("executorName"). This @EnableAsync/executor setup is the one Lab 19's @Async listeners reuse.
- **Configuring a Dedicated ThreadPoolTaskExecutor (and Virtual Threads on Java 21)** — Defining a named ThreadPoolTaskExecutor bean (core/max/queue/thread-name-prefix/rejection policy), why a dedicated pool beats the default SimpleAsyncTaskExecutor, and the spring.threads.virtual.enabled=true option that auto-configures a virtual-thread-backed SimpleAsyncTaskExecutor on Java 21 — including the key caveat that virtual threads ignore pool-size settings (you bound concurrency with spring.task.execution.simple.concurrency-limit instead), which is exactly why this lab still selects an explicitly named executor.
- **Returning Results with CompletableFuture** — Returning CompletableFuture from an @Async method (CompletableFuture.completedFuture(...)), composing/joining several async calls (thenApply / thenCompose / allOf), and the difference between fire-and-forget (void) and awaiting a result.
- **Async Pitfalls: Self-Invocation and Exception Handling** — Why calling an @Async method from within the same bean bypasses the proxy and runs synchronously (the same Spring AOP proxy trap first met with caching in Lab 16), and how exceptions vanish from void async methods unless you register an AsyncUncaughtExceptionHandler (via AsyncConfigurer) or inspect the returned CompletableFuture.
- **@EnableScheduling and @Scheduled — fixedRate, fixedDelay, cron** — Wiring scheduled jobs, the precise difference between fixedRate / fixedDelay / initialDelay, writing a Spring cron expression (6 fields: second minute hour day-of-month month day-of-week), externalizing the schedule via properties, and that scheduling uses its own single-threaded scheduler separate from the @Async executor (so a long job can starve other scheduled jobs).

**🔨 Then build**

- Add a @Configuration class with @EnableAsync that defines a named ThreadPoolTaskExecutor bean ('bookstoreTaskExecutor') with explicit core/max pool size, queue capacity, thread-name prefix, and a CallerRunsPolicy rejection handler.
- Implement an async report service method annotated @Async("bookstoreTaskExecutor") that generates a 'catalog report' (e.g. book count per category) and returns CompletableFuture<ReportDto>; expose an endpoint that triggers it and returns 202 Accepted immediately.
- Implement a fire-and-forget async 'send confirmation email' method (void) simulating a slow send (Thread.sleep / log), and register an AsyncUncaughtExceptionHandler (via an AsyncConfigurer) so its failures are logged rather than silently swallowed.
- Demonstrate and then fix the self-invocation pitfall: show that an internal call runs synchronously (no async thread name in the log), then refactor so the async method is invoked across a bean boundary so the proxy applies.
- Add a @Scheduled(fixedDelayString = "${bookstore.lowstock.interval}") job that scans for low-stock books and logs/records them, with the interval externalized in application.properties.
- Add a @Scheduled(cron = "...") nightly job that emits a daily sales/catalog summary, using a 6-field cron expression documented field-by-field.
- Write a test that proves async behavior: assert the endpoint returns 202 immediately and that the CompletableFuture eventually completes with the expected result (e.g. using Awaitility's await().untilAsserted(...) or future.get()).

**🆕 New skills unlocked:** Offloading slow work from the request thread with @Async and returning 202 Accepted; Designing and tuning a bounded ThreadPoolTaskExecutor; Returning and composing CompletableFuture results; Avoiding the self-invocation proxy trap and handling async exceptions via AsyncUncaughtExceptionHandler; Scheduling recurring jobs with fixedRate / fixedDelay / 6-field cron; Enabling virtual-thread execution on Java 21 and knowing when pool-size tuning still matters.

### Lab 19 — Decoupling with Application Events 🆕

Teaches Spring's in-process event mechanism (ApplicationEventPublisher + @EventListener) so side effects (audit, notifications, cache busting) live outside the service that triggers them, keeping core logic clean and open for extension. It builds directly on Lab 18's @EnableAsync/executor work (listeners can run async on the same ThreadPoolTaskExecutor) and is the conceptual bridge to Lab 20: an application event is the in-JVM version of the message-broker pattern.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **The Observer Pattern & Why Decouple Side Effects** — The observer pattern in plain terms, why piling notifications/audit/cache logic into one service method creates tight coupling, and how 'publish a fact, let others react' inverts that dependency so new reactions are added without touching the publisher.
- **ApplicationEventPublisher & Defining Custom Events** — Injecting ApplicationEventPublisher, defining an immutable event type (a plain record in modern Spring — no need to extend ApplicationEvent), and publishing it from a service method.
- **Consuming Events with @EventListener** — Writing @EventListener methods, how Spring routes events by parameter type, conditional listening with the condition attribute (SpEL), and returning a non-null value from a listener to publish a follow-on event.
- **Synchronous vs. Asynchronous Listeners** — That @EventListener runs synchronously in the publisher's thread (and inside its transaction) by default, and how adding @Async moves a listener onto Lab 18's named executor — plus the trade-offs (no transaction sharing, isolated failures, eventual execution). Reuses Lab 18's @EnableAsync setup rather than re-establishing it.
- **@TransactionalEventListener — Reacting Only After Commit** — The danger of acting on an event before the DB transaction commits, and how @TransactionalEventListener with phase=AFTER_COMMIT (vs. AFTER_ROLLBACK / BEFORE_COMMIT) fires only when the data is durably saved — and that it no-ops if published outside an active transaction unless fallbackExecution is set. This AFTER_COMMIT pattern is exactly what Lab 20 uses to publish broker messages safely.
- **Listener Ordering and Error Propagation** — Controlling execution order of multiple listeners with @Order, and how an exception in a synchronous listener propagates back to the publisher (and can roll the transaction back) versus being isolated in an @Async listener (where it goes only to the AsyncUncaughtExceptionHandler from Lab 18).

**🔨 Then build**

- Define an immutable BookCreatedEvent record carrying the new book's id/title/isbn (and any data downstream listeners need without re-querying).
- Refactor BookService.create so that instead of performing a side effect inline, it persists the book and publishes BookCreatedEvent via ApplicationEventPublisher.
- Implement a synchronous @EventListener that writes an audit log entry for the created book, and verify it runs in the same thread and transaction as the publisher.
- Implement a @TransactionalEventListener(phase = AFTER_COMMIT) that triggers the 'notify' side effect, and demonstrate it does NOT fire when the surrounding transaction is rolled back.
- Add @Async to one listener (e.g. notification) so it runs on Lab 18's bookstoreTaskExecutor, and contrast its behavior (different thread, isolated failure) with the synchronous audit listener.
- Add a second listener and use @Order to guarantee deterministic execution order; write a test asserting both listeners fire (and in order) when a book is created.
- Write a test proving the AFTER_COMMIT listener fires on a committed transaction and stays silent on a rolled-back one.

**🆕 New skills unlocked:** Publishing in-process domain events with ApplicationEventPublisher; Reacting to events with @EventListener and SpEL conditions; Choosing synchronous vs. @Async listeners deliberately; Using @TransactionalEventListener to act only after commit; Ordering listeners and reasoning about error propagation; Refactoring side effects out of service methods for cleaner, extensible code.

### Lab 20 — Asynchronous Messaging with RabbitMQ (Spring AMQP) 🆕

Takes decoupling out of a single JVM and onto a real message broker, so services communicate via durable, buffered, retryable messages instead of in-process calls or events. It is the capstone of Phase 7 — generalizing Lab 19's events to cross-process messaging and reusing Lab 19's AFTER_COMMIT pattern to publish safely — and adds a RabbitMQ service to the Lab 9 Docker Compose stack. This is the architecture behind order processing, fan-out notifications, and resilient background pipelines in real systems.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **What a Message Broker Is and Why You Want One** — From zero: the difference between synchronous request/response and asynchronous messaging, and the three things a broker buys you — decoupling (producer doesn't know the consumer), buffering (absorb load spikes), and reliability (messages survive consumer downtime).
- **The AMQP Model: Producer, Exchange, Queue, Binding, Routing Key** — The core AMQP vocabulary and the key insight that producers publish to an EXCHANGE (never directly to a queue); the exchange uses bindings + routing keys to decide which queue(s) receive the message.
- **Exchange Types: direct, topic, fanout** — When to use each — direct (exact routing-key match), topic (wildcard pattern routing with * and #), fanout (broadcast to all bound queues) — with a concrete routing example for each.
- **Reliability: Acknowledgements, Durability & Dead-Letter Queues** — Consumer acks (and why auto-ack loses messages on failure), durable exchanges/queues + persistent messages surviving a broker restart, and dead-letter exchanges/queues (DLX/DLQ): a DLQ is just an ordinary queue that a DLX routes to, and a message reaches it only when it is rejected with requeue=false (or expires/overflows) — so the working queue must be declared with the x-dead-letter-exchange argument pointing at the DLX.
- **Adding RabbitMQ + Management UI to the Compose stack** — Adding rabbitmq:4-management as a service on the Lab 9 Compose stack, exposing ports 5672 (AMQP) and 15672 (UI), logging into the management console, and inspecting exchanges, queues, bindings, and messages by hand — reusing Lab 9's Compose skills, not re-teaching them.
- **Spring AMQP Basics: starter, RabbitTemplate, @RabbitListener** — Adding spring-boot-starter-amqp, configuring the connection in application.properties, declaring Exchange/Queue/Binding beans so Spring auto-creates the topology on startup, sending with RabbitTemplate, and consuming with @RabbitListener.
- **Sending Objects as JSON and Handling Failures (converter, retry, DLQ)** — Registering a JacksonJsonMessageConverter (the Jackson 3 converter in Spring AMQP 4.0, under org.springframework.amqp.support.converter) so POJOs serialize to JSON; enabling listener retry via spring.rabbitmq.listener.simple.retry.* properties; and the precise mechanism by which an exhausted message reaches the DLQ — after the final attempt the default RejectAndDontRequeueRecoverer rejects the message with requeue=false, which (because the queue was declared with x-dead-letter-exchange) routes it to the DLX/DLQ rather than looping forever.

**🔨 Then build**

- Add a rabbitmq:4-management service to the Lab 9 compose stack with ports 5672/15672 mapped, and confirm login to the management UI at localhost:15672.
- Add spring-boot-starter-amqp and configure the RabbitMQ connection (host/port/username/password) in application.properties.
- Declare the messaging topology as Spring beans: a topic exchange ('bookstore.exchange'), an 'order.fulfilment' queue, and a binding with routing key 'order.placed'.
- Register a JacksonJsonMessageConverter bean (Jackson 3) and wire it into both RabbitTemplate and the listener container factory so an OrderPlacedMessage POJO is sent and received as JSON.
- Build the producer side: when an order is placed, publish an OrderPlacedMessage via RabbitTemplate.convertAndSend(exchange, 'order.placed', message) — fired from an AFTER_COMMIT @TransactionalEventListener (reusing Lab 19) so messages publish only after the order is durably saved.
- Build the consumer side: a @RabbitListener on 'order.fulfilment' that performs fulfilment (e.g. marks the order fulfilled / decrements stock) and logs the outcome.
- Make the queue durable and declare it with x-dead-letter-exchange pointing at a dead-letter exchange that routes to a DLQ; enable listener retry (spring.rabbitmq.listener.simple.retry.*) so that after retries are exhausted the default recoverer rejects the message and it lands in the DLQ instead of looping forever.
- Demonstrate resilience: stop the consumer, place orders, restart the consumer, and show buffered messages are processed — then verify a deliberately-poisoned message (one that always throws) ends up in the DLQ via the management UI.

**🆕 New skills unlocked:** Explaining what a broker is and the AMQP producer/exchange/queue/binding model; Choosing direct vs. topic vs. fanout exchanges; Running RabbitMQ and its management UI as a Docker Compose service; Producing messages with RabbitTemplate and consuming with @RabbitListener; Serializing POJOs as JSON with JacksonJsonMessageConverter (Jackson 3); Configuring acks, durability, retry, and dead-letter routing (x-dead-letter-exchange + reject-without-requeue) for reliable delivery; Designing an event-driven cross-service flow (order-placed to async fulfilment).

---

## Phase 8 — Inter-Service Communication

Talk to other services without being fragile — modern HTTP clients and resilience patterns.

### Lab 21 — Calling Other Services with RestClient & WebClient 🆕

So far the Bookstore has only served data it owns; this lab turns it into a *client* that fetches data from another HTTP service. It is the foundation of every distributed system and the prerequisite for Lab 22's resilience work: you cannot make a remote call resilient until you can first make it correctly. Everything here uses the modern Spring Boot 4 client stack (RestClient + Jackson 3), so the patterns you build are the ones you keep.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Why services call services (and what changes when they do)** — The mental model of one process making an HTTP request to another over the network: synchronous request/response, latency, partial failure, and why a remote call is NOT a local method call — it can be slow, fail independently, or return data your process does not control.
- **Anatomy of an outbound HTTP call** — From zero: HTTP methods (GET/POST/PUT/DELETE), request/response headers, status-code families (2xx/4xx/5xx), request and response bodies, and content negotiation via Content-Type / Accept headers — the raw material every HTTP client manipulates.
- **RestClient: the modern synchronous client (Spring 6.1+, default on Boot 4)** — Building a RestClient with RestClient.builder(), setting a baseUrl and default headers, and the fluent get()/post()/uri()/header()/body()/retrieve() call chain; why it is the recommended synchronous client on Spring Boot 4 and fully replaces the legacy RestTemplate.
- **Serializing & deserializing JSON with Jackson 3** — How RestClient turns a Java DTO into a JSON request body and a JSON response into a typed object via body(MyDto.class) and body(new ParameterizedTypeReference<List<X>>(){}) for generic collections; that on Spring Boot 4 Jackson 3 is the default and its packages live under tools.jackson.databind (the com.fasterxml.jackson namespace is gone).
- **Sending headers and authentication** — Attaching custom headers, an API-key header, and an Authorization header (Bearer token / Basic auth) to outbound requests, plus the trade-off between setting them once as RestClient builder defaults vs. per-request on a single call.
- **Reading status codes and handling errors** — Default RestClient behavior on 4xx/5xx (retrieve() throws), customizing it with onStatus(...), mapping a remote error into your own ProblemDetail, and the difference between an error *response* (the server answered with 4xx/5xx) and a transport *failure* (timeout, connection refused — no response at all).
- **Configuring connect & read timeouts** — From zero: connect-timeout vs. read-timeout vs. overall response timeout, why an unbounded remote call is dangerous (it can hang a request thread indefinitely), and wiring timeouts on the RestClient builder via ClientHttpRequestFactorySettings / a configured request factory — the timeout foundation Lab 22 treats as the first line of defense.
- **When WebClient (reactive, non-blocking) is the right tool** — What 'reactive / non-blocking' means at a high level, Mono/Flux as eventual results, the spring-boot-starter-webflux dependency that brings WebClient in, and a clear decision rule: RestClient for blocking Spring MVC apps (this course's stack), WebClient for high-concurrency, streaming, or fully reactive stacks.

**🔨 Then build**

- Define a typed RestClient bean (baseUrl, default Accept: application/json header, and connect + read timeouts via a configured request factory) and register it through a @Configuration class.
- Add an external-book integration: a client class that GETs book metadata from a real external API (e.g. Open Library by ISBN) and maps the JSON into an external DTO kept deliberately distinct from your domain entities.
- Wire a service method that, on book lookup or creation, enriches the Bookstore response with data pulled from the external API.
- Implement a POST call that sends a JSON body plus an API-key or Bearer header to a second service (or a local mock endpoint) and deserializes the typed response.
- Use onStatus(...) to translate a remote 404 into your own NotFound and a remote 5xx into a 502/503-style ProblemDetail surfaced through the global @RestControllerAdvice handler.
- Add a second client built with WebClient that calls the same endpoint, and document (in code comments or the README) why RestClient was the default choice and when you would reach for WebClient instead.
- Test the integration with a stubbed HTTP server (Spring's MockRestServiceServer or a WireMock stub) asserting the request URL, headers, and body, and verifying the error-status mapping.

**🆕 New skills unlocked:** Making synchronous outbound HTTP calls with RestClient on Spring Boot 4; Choosing between RestClient and WebClient deliberately; Serializing/deserializing JSON request and response bodies with Jackson 3 (tools.jackson); Sending headers and Bearer/Basic/API-key authentication on outbound calls; Mapping remote status codes into local ProblemDetail errors; Configuring connect and read timeouts to bound remote calls; Testing HTTP clients against a stubbed server.

### Lab 22 — Resilience: Timeouts, Retries & Circuit Breakers 🆕

The Lab 21 integration works only while the remote service is healthy; this lab makes it survive slowness, failures, and traffic spikes without taking the Bookstore down with it. It introduces the production discipline of fault tolerance on the Spring Boot 4 stack: Spring Framework 7's *native* retry and concurrency limiting for the common cases, plus Spring Cloud Circuit Breaker 5.x (Resilience4j) for the circuit breaker, rate limiter, bulkhead, and time limiter — surfacing the results through the Actuator endpoints you already stood up in Lab 15.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Why remote calls fail and how failures cascade** — The fallacies of distributed computing in plain terms: the network is unreliable and slow; one slow dependency exhausts your request threads and cascades into a full outage; resilience is about *containing* failure, not preventing it.
- **Timeouts as the first line of defense** — Why every remote call must be time-bounded, the layers of timeout (connect / read / total), how to choose sane values, and how a single missing timeout silently turns one slow dependency into an outage under load — building directly on the timeouts wired in Lab 21.
- **Retries, backoff, jitter & idempotency** — When a retry helps vs. when it makes things worse, exponential backoff plus jitter to avoid synchronized retry storms, and the idempotency rule: GET/PUT/DELETE are generally safe to retry, a non-idempotent POST is dangerous to blindly retry.
- **Native retry & concurrency limiting in Spring Framework 7** — From zero: @EnableResilientMethods to turn on the feature, then @Retryable from org.springframework.resilience.annotation with its real attributes — maxRetries, delay, multiplier (exponential backoff), jitter, maxDelay, and includes/excludes — plus @Recover for a native fallback and @ConcurrencyLimit to cap concurrent invocations. This is built into the core framework on Spring Boot 4 with NO external dependency (it supersedes the old spring-retry project).
- **The circuit breaker pattern** — The closed / open / half-open state machine, what trips it (failure-rate or slow-call thresholds measured over a sliding window), why it gives a failing dependency time to recover, and how it converts slow failures into fast ones so callers fail immediately instead of piling up.
- **Bulkheads, rate limiting & time limiting** — Bulkheads isolating one dependency's threads/permits so it cannot sink the whole app (the ship-compartment analogy), rate limiting to cap outbound call volume and respect a downstream's quota, and a time limiter to bound total call duration — and how these complement the native @ConcurrencyLimit from the previous module.
- **Fallbacks: degrading gracefully** — Designing a fallback for when a call is blocked, rejected, or fails — cached/stale data, a sensible default, or a partial response — and where fallback logic lives (a fallbackMethod with a matching signature) relative to the protected method.
- **Spring Cloud Circuit Breaker 5.x with Resilience4j on Spring Boot 4** — From zero: that the circuit breaker, rate limiter, bulkhead, and time limiter come from Resilience4j wired through Spring Cloud Circuit Breaker 5.x (the Spring Cloud 2025.1 'Oakwood' train, aligned with Spring Boot 4 / Spring Framework 7). How to add spring-cloud-starter-circuitbreaker-resilience4j (with spring-boot-starter-aop for the AOP interception, and Lab 15's spring-boot-starter-actuator already present), apply the @CircuitBreaker / @RateLimiter / @Bulkhead / @TimeLimiter annotations, and configure instances in application.yml. Note the division of labor: native Spring @Retryable/@ConcurrencyLimit for retries and simple concurrency caps, Resilience4j for the circuit breaker and the richer rate-limit/bulkhead/time-limit patterns.
- **Observing resilience through Actuator** — Surfacing circuit-breaker state, call counts, and retry metrics through the Actuator health and metrics endpoints from Lab 15 (Resilience4j contributes a circuitbreakers health group and Micrometer meters), so you can literally SEE a breaker open and close — a focused application of Lab 15's endpoints, with the deeper Micrometer/Grafana treatment deferred to Lab 24.

**🔨 Then build**

- Apply native Spring @Retryable (via @EnableResilientMethods) with exponential backoff (multiplier) + jitter and a bounded maxRetries to the idempotent external GET from Lab 21, and document why the non-idempotent POST is NOT retried.
- Add Spring Cloud Circuit Breaker with Resilience4j to the external client via spring-cloud-starter-circuitbreaker-resilience4j (plus spring-boot-starter-aop; the Actuator starter from Lab 15 is already present).
- Protect the external call with a @CircuitBreaker configured in application.yml by a sliding window, failure-rate threshold, and wait-duration-in-open-state.
- Add a fallback method (matching signature) that returns cached/stale or default book data when the breaker is open or the call fails, asserted by a test.
- Add a @RateLimiter and/or @Bulkhead / @TimeLimiter to cap concurrent and total outbound calls, and verify excess calls are rejected or shed.
- Write tests that force the dependency to fail or slow (the Lab 21 stubbed server) and assert the breaker transitions closed -> open -> half-open and that the fallback is served.
- Expose and inspect resilience state via the Lab 15 Actuator endpoints (the circuitbreakers health group and retry/circuit-breaker metrics) and capture before/after evidence of the breaker tripping.

**🆕 New skills unlocked:** Bounding remote calls with connect/read/total timeouts; Applying native Spring @Retryable with backoff, jitter, maxRetries, and idempotency awareness; Implementing the circuit breaker pattern with Spring Cloud Circuit Breaker 5.x / Resilience4j on Spring Boot 4; Isolating dependencies with bulkheads, rate limiters, and the native @ConcurrencyLimit; Designing graceful fallbacks for degraded service; Testing state transitions and fallbacks for resilient calls; Observing resilience behavior through Lab 15's Actuator endpoints.

---

## Phase 9 — Observability & Deployment

See inside a running system and ship it continuously — logging, metrics, tracing, and CI/CD.

### Lab 23 — Structured Logging & Correlation IDs 🆕

Turns the Bookstore's logs from unsearchable console noise into machine-parseable JSON that a log aggregator can index, filter, and correlate. This is the first of two observability labs and the foundation for the next: once every log line carries a per-request id and JSON structure, a single request can be reconstructed end to end, and the metrics/traces you add in Lab 24 become far more useful when they line up with structured logs. Important framing for the course: in this lab you hand-roll a request correlation id at the application edge (no tracing library yet); in Lab 24 you add Micrometer Tracing, which AUTOMATICALLY injects traceId/spanId into the MDC. Lab 23 deliberately builds the manual version first so you understand the MDC mechanics from zero before the framework does it for you, and Lab 24 reconciles the two.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **Logging Levels and When to Use Each** — ERROR/WARN/INFO/DEBUG/TRACE, what belongs at each level in production, the cost of wrong-level or chatty logging, and parameterized logging ('{}' placeholders) instead of string concatenation so disabled levels cost nothing.
- **How Logging Works in Spring Boot (SLF4J over Logback)** — The SLF4J facade vs. the Logback implementation it binds to by default, where Spring Boot's default logging config lives, and tuning per-package levels via logging.level.* in application.yml.
- **Structured (JSON) Logging Built Into Spring Boot 4** — Why JSON logs beat plain text for aggregation, and enabling Spring Boot's built-in structured output (no extra dependency) by setting logging.structured.format.console and/or logging.structured.format.file to ecs, logstash, or gelf — a feature that has shipped since Spring Boot 3.4 and is standard in 4.0.
- **The MDC and a Per-Request Correlation ID** — What the Mapped Diagnostic Context (MDC) is (a thread-local key/value map the logging framework merges into every line), how the built-in structured formats automatically promote MDC entries to JSON fields, and how to stamp a per-request correlation id into the MDC so all of a request's lines share one searchable id.
- **OncePerRequestFilter and Safe MDC Lifecycle** — Reusing Spring's OncePerRequestFilter (first met as the JWT filter base in Lab 7) to read or generate the id, and the critical try/finally rule: always MDC.clear()/remove() in finally so a pooled worker thread never leaks one request's id onto the next. Notes the limitation that MDC is thread-bound and does not automatically follow the @Async or reactive boundaries from Lab 18.
- **Log Aggregation at a High Level (ELK / Loki)** — The ship -> index -> query pipeline, what Elasticsearch+Kibana vs. Grafana Loki each do, and why structured, correlated logs are the input that makes aggregation and field-level search worthwhile. Conceptual only — no stack is stood up in this lab.
- **What Must NEVER Be Logged** — Identifying PII, secrets, passwords, tokens, and full request/response payloads; redaction/masking strategies; and the security and compliance reasons logging them is dangerous.

**🔨 Then build**

- Switch the Bookstore to JSON logs by setting logging.structured.format.console (ECS or Logstash) for the deployed profile, while keeping the default human-readable console format under the local-dev profile.
- Add a CorrelationIdFilter extending OncePerRequestFilter that reads an inbound X-Correlation-Id header or generates a UUID, puts it in the MDC under a key like correlationId, echoes it back on the response header, and removes it from the MDC in a finally block.
- Confirm the correlation id appears as its own field on every JSON log line emitted during a single request as it passes through controller, service, and repository.
- Add deliberate, correctly-leveled log statements to the book-create and book-update flows (INFO for outcomes, WARN/ERROR for failures, DEBUG for diagnostics) using parameterized '{}' messages.
- Write a small log-sanitizing helper (or masking convention) that guarantees author email and any token-like values are masked before they reach a log line, and document the project's explicit 'never log' list.
- Demonstrate end to end: issue two requests with different X-Correlation-Id values and filter the JSON output to show each request's lines are independently traceable by id.

**🆕 New skills unlocked:** Choosing correct log levels and parameterized logging for production; Emitting structured JSON logs with Spring Boot 4's built-in support; Propagating a per-request correlation id through the MDC with OncePerRequestFilter and a safe finally cleanup; Designing structured, correlated logs as the input for ELK/Loki aggregation; Keeping PII, secrets, and tokens out of logs.

### Lab 24 — Metrics & Distributed Tracing with Micrometer 🆕

Adds the second and third pillars of observability — metrics and traces — on top of Lab 23's logs, so you can answer 'is it healthy, how fast, and where did the time go?' not just 'what happened.' Reusing the Actuator endpoints already exposed in Lab 15, you instrument the Bookstore with Micrometer, light up Prometheus-format metrics, visualize them in Grafana, and export OpenTelemetry traces over OTLP — producing a production-grade monitoring stack you run locally by adding Prometheus/Grafana/Tempo services to the Lab 9 Docker Compose stack. A key payoff: once Micrometer Tracing is on the classpath, Spring Boot AUTOMATICALLY injects traceId and spanId into the MDC, so Lab 23's structured logs gain real distributed trace ids for free — and you reconcile that framework-provided traceId with the manual correlation id you built in Lab 23.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **The Three Pillars of Observability** — How logs, metrics, and traces differ and complement each other, and which question each one answers — connecting back to the structured logs from Lab 23.
- **Micrometer: The Metrics Facade** — Micrometer as the 'SLF4J of metrics', the meter types (Counter, Gauge, Timer, DistributionSummary), tags/dimensions and why low, bounded cardinality matters, and the JVM/HTTP meters Spring Boot auto-registers. (Actuator itself and its exposure rules were covered in Lab 15 and are reused, not re-taught.)
- **Exposing Prometheus Metrics via Actuator** — Adding micrometer-registry-prometheus to light up the /actuator/prometheus endpoint, adding it to the Lab 15 exposure include list, and reading the Prometheus text exposition format it emits.
- **Prometheus + Grafana from Zero (on the Compose stack)** — Prometheus as a pull/scrape time-series database, writing a prometheus.yml scrape config targeting the app's /actuator/prometheus, a first PromQL query, and adding Prometheus as a Grafana datasource to build a dashboard — stood up by adding services to Lab 9's Compose stack (no Compose re-teaching).
- **Micrometer Tracing + the Spring Boot 4 OpenTelemetry Starter** — Traces and spans, W3C context propagation, and how Spring Boot 4 collapses tracing setup into a single starter, org.springframework.boot:spring-boot-starter-opentelemetry, which pulls the Micrometer tracing OTel bridge and OTLP exporter for you (no longer hand-adding micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp as in 3.x). Notes that the tracing autoconfiguration moved into the new org.springframework.boot.micrometer.tracing.* packages in 4.0.
- **Automatic Log Correlation: traceId/spanId in the MDC** — That with Micrometer Tracing present Spring Boot automatically populates traceId and spanId in the MDC, how to surface them in plain-text logs via logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}] (the Lab 23 structured formats pick MDC up automatically), and how this framework-provided traceId relates to — and can replace or sit alongside — the manual correlationId from Lab 23.

**🔨 Then build**

- Confirm the Lab 15 Actuator setup is in place, then add micrometer-registry-prometheus and add the prometheus endpoint to the existing exposure include list.
- Register custom meters: a Counter for books created (tagged by category, keeping cardinality bounded), a Timer around the book-search service method, and a Gauge reporting the current book count.
- Extend the Lab 9 Compose stack with Prometheus (scraping the app's /actuator/prometheus) and Grafana wired to Prometheus as a datasource.
- Build a Grafana dashboard showing request rate, p95 latency (from the auto-registered HTTP server timer), JVM heap, and your custom book-creation counter.
- Add spring-boot-starter-opentelemetry, point traces at an OTLP backend with management.opentelemetry.tracing.export.otlp.endpoint (e.g. http://tempo:4318/v1/traces), and add a Tempo or Zipkin container to the same Compose stack.
- Trace one end-to-end request and show the traceId from the tracing backend matching the traceId now appearing in the structured logs from Lab 23 (and note how it relates to, and can replace, the manual correlationId).

**🆕 New skills unlocked:** Instrumenting code with Micrometer counters, gauges, and timers with bounded-cardinality tags; Exposing Prometheus metrics through the Lab 15 Actuator endpoints; Scraping with Prometheus and querying with PromQL; Building Grafana dashboards over a Compose-run stack; Emitting and reading distributed traces via the Spring Boot 4 OpenTelemetry starter over OTLP; Correlating logs, metrics, and traces by traceId and reconciling it with a manual correlation id.

### Lab 25 — CI/CD & Deployment (Capstone) 🆕

The capstone: ships the Bookstore through a fully automated GitHub Actions pipeline that builds, tests (including the Lab 11 Testcontainers integration tests), packages the Lab 9 Docker image, pushes it to a registry, and deploys to a real host with a rollout gated on the Lab 15 Actuator readiness probe. It ties together everything the course built — Docker, testing, externalized config, secrets, and the observability from Labs 23-24 — into the way professional teams actually deliver software.

**📚 Learn first** — study these concept modules first (each becomes a standalone `COURSE_*.md`):

- **What CI/CD Is and Why It Exists** — Continuous Integration vs. Continuous Delivery vs. Continuous Deployment, the build->test->package->deploy pipeline, and why automation beats manual releases.
- **GitHub Actions from Zero** — Workflows, jobs, steps, triggers (push/pull_request), runners (ubuntu-latest), the marketplace-action model (actions/checkout, actions/setup-java with built-in Maven dependency caching), and how jobs pass artifacts/outputs between each other.
- **Running the Full Test Suite in CI (Including Testcontainers)** — Why ubuntu-latest ships with a Docker daemon so the Lab 11 Testcontainers integration tests run unchanged in CI, how mvn verify fails the build on any red test, and surfacing the test reports as a job summary or artifact.
- **Building and Pushing a Docker Image to a Registry** — docker/setup-buildx-action, docker/login-action, and docker/build-push-action building the Lab 9 image; image registries (GHCR vs. Docker Hub); authenticating GHCR with the built-in GITHUB_TOKEN plus the packages: write permission; and meaningful tagging (commit SHA + semver + latest).
- **Handling Secrets and Environments in CI** — GitHub Actions secrets vs. (non-secret) variables, GitHub Environments for promotion (e.g. staging -> production) including required-reviewer gates, and never baking credentials into images or echoing them into logs — the CI-side enforcement of Lab 13's secret discipline.
- **Deploying to a Real Target with a Health-Gated Rollout** — Deploying the pushed image to a container host (Render or Fly.io) — with a high-level look at the equivalent Kubernetes Deployment + Service manifest — and gating the rollout on the Lab 15 Actuator /actuator/health readiness probe so a failing build can't go live.

**🔨 Then build**

- Write a GitHub Actions CI workflow triggered on push and pull_request that checks out the repo, sets up Java 21 with Maven caching, and runs the full mvn verify suite including the Lab 11 Testcontainers integration tests.
- Add a job that builds the Lab 9 Bookstore Docker image with buildx and pushes it to GHCR tagged with the commit SHA and latest, authenticating via the built-in GITHUB_TOKEN and packages: write permission.
- Externalize deploy-time config (DB URL, OTLP endpoint from Lab 24) and inject the database password and any registry/host tokens from GitHub Actions secrets (applying Lab 13's discipline) — then verify nothing sensitive appears in the build logs.
- Define a staging and a production GitHub Environment and promote the SAME image artifact (by digest/tag) from staging to production rather than rebuilding it.
- Deploy the image to Render or Fly.io (and include a documented equivalent Kubernetes Deployment + Service + readiness-probe manifest) with the rollout gated on the Lab 15 Actuator /actuator/health endpoint.
- Verify the deployed Bookstore end to end: hit the live API, confirm /actuator/health is UP, and confirm the metrics and traces from Lab 24 are flowing from the deployed instance.

**🆕 New skills unlocked:** Authoring multi-job GitHub Actions pipelines with artifact/output passing; Running Testcontainers integration tests unchanged in CI; Building and publishing Docker images to a registry with proper tagging; Managing CI secrets and environment promotion of a single immutable artifact; Deploying to a container host with a health-gated rollout; Reading a basic Kubernetes Deployment/Service manifest.

---

## Appendix A — Course Module Backlog

Every **📚 Learn first** entry above is a standalone teaching module to be written later as its own `COURSE_*.md`. This is the consolidated authoring backlog, in build order. (Labs 1–3 are already covered by their existing `LAB_*` documents.)

**Lab 4 — Dynamic Search with JPA Specifications & the Criteria API**

- `COURSE_L4_why-dynamic-queries-the-combinatorial-explosion-of-derived-queries.md` — **Why dynamic queries: the combinatorial explosion of derived queries**: Why findByTitleAndAuthorAndPriceBetween... and a pile of @Query variants do not scale to N optional filters (2^N combinations), motivating a single query you assemble at runtime from independent pieces.
- `COURSE_L4_the-jpa-criteria-api-from-zero.md` — **The JPA Criteria API from zero**: What CriteriaBuilder, CriteriaQuery, Root, Path/Join, and Predicate are, and how to build a type-safe WHERE clause as Java objects (e.g. cb.like, cb.between, cb.equal) instead of a JPQL string, so the compiler catches mistakes a string would not.
- `COURSE_L4_the-specification-t-interface-in-spring-data-jpa-4-0.md` — **The Specification<T> interface in Spring Data JPA 4.0**: Implementing toPredicate(Root<T>, CriteriaQuery<?>, CriteriaBuilder) from org.springframework.data.jpa.domain.Specification as a lambda that returns one Predicate, and reading a Specification as 'a reusable WHERE fragment'.
- `COURSE_L4_composing-specifications-and-the-spring-boot-4-0-way-to-say-no-filter.md` — **Composing Specifications, and the Spring Boot 4.0 way to say 'no filter'**: Gluing independent predicates with the default .and()/.or()/.not() methods and the static allOf()/anyOf() factories; and the 4.0-specific point that the modern 'match everything' base is Specification.unrestricted() (which replaces the legacy where(null) idiom), while a single null-skipping predicate is still the cleanest way to drop an absent filter. Also introduces PredicateSpecification<T> (new in 4.0) so the student recognises it, while this lab standardises on Specification<T> for paginated repository search.
- `COURSE_L4_jpaspecificationexecutor-and-wiring-it-into-a-repository.md` — **JpaSpecificationExecutor and wiring it into a repository**: Extending JpaSpecificationExecutor<Book> to unlock findAll(Specification, Pageable) / findAll(Specification, Sort), how it coexists with the JpaRepository methods and @EntityGraph from Lab 3, and where the executor's generated query plugs the predicate into the WHERE clause.
- `COURSE_L4_a-searchcriteria-builder-pattern-for-optional-filters.md` — **A SearchCriteria + builder pattern for optional filters**: Mapping nullable query params into an immutable SearchCriteria record and a SpecificationBuilder that adds a predicate only when its filter is present, starting from Specification.unrestricted() and joining relationships (author, categories) safely so absent filters add nothing to the SQL.
- `COURSE_L4_specifications-vs-derived-queries-vs-query-choosing-the-right-tool.md` — **Specifications vs derived queries vs @Query: choosing the right tool**: A decision guide for when each technique wins (fixed simple queries -> derived; complex but static -> @Query; runtime-assembled optional filtering -> Specifications), plus the two Criteria pitfalls that bite here: a many-to-many join duplicating rows (needs distinct) and the Lab 3 rule that you cannot fetch a collection while paginating.

**Lab 5 — DTO Mapping at Scale with MapStruct & Read-Optimized Projections**

- `COURSE_L5_why-manual-mapping-first-and-what-an-annotation-processor-is.md` — **Why manual mapping first, and what an annotation processor is**: How a compile-time annotation processor runs during javac and generates plain Java source (no reflection at runtime), and why having written mappers by hand first means you can open and trust the generated code.
- `COURSE_L5_setting-up-mapstruct-with-lombok-on-spring-boot-4-0-java-21.md` — **Setting up MapStruct with Lombok on Spring Boot 4.0 / Java 21**: Adding org.mapstruct:mapstruct:1.6.3 as a dependency and the mapstruct-processor (1.6.3), lombok, and lombok-mapstruct-binding (0.2.0) entries inside maven-compiler-plugin annotationProcessorPaths, with Lombok ordered BEFORE MapStruct so Lombok-generated getters/setters exist when MapStruct runs.
- `COURSE_L5_mapper-components-and-spring-dependency-injection.md` — **@Mapper components and Spring dependency injection**: Declaring a @Mapper(componentModel = "spring") interface so the generated mapper becomes an injectable Spring bean, injecting it into a service, and locating the generated *MapperImpl under target/generated-sources to read what it produced.
- `COURSE_L5_field-mapping-mapping-ignore-expression-and-mappingtarget-update.md` — **Field mapping: @Mapping, ignore, expression, and @MappingTarget update**: Renaming and ignoring fields and using expression/constant with @Mapping, and writing an in-place update method annotated with @MappingTarget for PUT handlers so untouched fields are not overwritten.
- `COURSE_L5_nested-relationship-mapping-entity-response-only.md` — **Nested relationship mapping (entity -> response only)**: Mapping Book -> BookResponseDto including nested AuthorSummaryDto / PublisherSummaryDto / List<CategorySummaryDto>, and the deliberate asymmetry: the request -> entity mapping IGNORES relationships because resolving authorId/publisherId/categoryIds to managed entities is a repository job the service still owns (carried over from Lab 3), not the mapper's.
- `COURSE_L5_spring-data-interface-based-projections.md` — **Spring Data interface-based projections**: Defining closed projection interfaces (only entity-backed getters) and open projections (@Value with SpEL) so a repository query materialises only the named fields instead of full entities, including nested projection getters for associations.
- `COURSE_L5_class-based-record-and-dynamic-projections.md` — **Class-based (record) and dynamic projections**: Record/constructor projections, the generic <T> Class<T> dynamic-projection repository signature that lets one method return different shapes, and the rule that class-based projections cannot nest associations while interface-based ones can.

**Lab 6 — Spring Security Fundamentals**

- `COURSE_L6_how-spring-security-actually-works-the-servlet-filter-chain.md` — **How Spring Security Actually Works: The Servlet Filter Chain**: Why every request passes through a chain of servlet filters before reaching your controller, what the DelegatingFilterProxy and FilterChainProxy do, and how a SecurityFilterChain bean plugs your rules into that pipeline (built from zero, assuming no prior security knowledge). This filter-chain mental model is reused directly by Lab 7's JWT filter and Lab 8's method security.
- `COURSE_L6_authentication-vs-authorization-and-the-securitycontext.md` — **Authentication vs. Authorization (and the SecurityContext)**: The difference between proving who you are (authentication) vs. what you may do (authorization), plus how the Authentication object, the SecurityContext, and SecurityContextHolder carry the logged-in principal through a single request.
- `COURSE_L6_the-securityfilterchain-bean-in-spring-boot-4-spring-security-7.md` — **The SecurityFilterChain Bean in Spring Boot 4 / Spring Security 7**: Writing a modern SecurityFilterChain @Bean from a HttpSecurity object, using authorizeHttpRequests + requestMatchers — and that the Spring Security 5/6 APIs authorizeRequests, antMatchers, and WebSecurityConfigurerAdapter are REMOVED (not deprecated) in Spring Security 7, so older tutorials will not compile. Also covers that web apps are secured-by-default in 4.0.
- `COURSE_L6_passwords-done-right-passwordencoder-and-bcrypt.md` — **Passwords Done Right: PasswordEncoder and BCrypt**: Why passwords are never stored in plaintext, how BCrypt salting and adaptive hashing work, and wiring a BCryptPasswordEncoder bean used for both encoding on registration and matching on login (encode vs. matches).
- `COURSE_L6_userdetails-and-userdetailsservice-from-in-memory-to-db-backed-users.md` — **UserDetails and UserDetailsService: From In-Memory to DB-Backed Users**: The UserDetails / UserDetailsService contract, starting with an InMemoryUserDetailsManager to see it work, then implementing a custom UserDetailsService that loads a user from a JPA table and adapts it to UserDetails.
- `COURSE_L6_the-authenticationmanager-and-how-a-login-is-verified.md` — **The AuthenticationManager and How a Login Is Verified**: How Spring Security actually checks credentials: the AuthenticationManager / AuthenticationProvider (DaoAuthenticationProvider) wiring your UserDetailsService and PasswordEncoder together, and how to obtain the AuthenticationManager bean — the piece Lab 7 will call directly to authenticate a login request.
- `COURSE_L6_login-mechanisms-and-url-rules-http-basic-form-login-roles-vs-authorities.md` — **Login Mechanisms and URL Rules: HTTP Basic, Form Login, Roles vs. Authorities**: How httpBasic() and formLogin() differ, and how to express access rules with hasRole / hasAuthority / authenticated — including the ROLE_ prefix convention (hasRole("ADMIN") matches authority ROLE_ADMIN) so roles and authorities behave as intended.

**Lab 7 — Stateless Authentication with JWT**

- `COURSE_L7_what-a-jwt-is-header-payload-signature.md` — **What a JWT Is: Header, Payload, Signature**: The three base64url-encoded parts of a JWT, what standard claims (sub, iat, exp, plus a custom roles claim) mean, and why a signed JWT is tamper-evident and readable-but-not-secret rather than encrypted.
- `COURSE_L7_stateless-vs-session-authentication.md` — **Stateless vs. Session Authentication**: Why server-side sessions don't scale horizontally, how a signed token moves trust to the client, and the security tradeoffs of going stateless (notably no easy server-side revocation, which motivates short access-token lifetimes).
- `COURSE_L7_signing-and-verifying-tokens-hmac-hs256-vs-rsa.md` — **Signing and Verifying Tokens: HMAC/HS256 vs. RSA**: How symmetric (HS256, one shared secret) and asymmetric (RS256, key pair) signatures prove a token was issued by your server and was not tampered with, and how to store the secret/keys safely (externalized config / environment, never hard-coded; the disciplined version of this arrives in Lab 13).
- `COURSE_L7_choosing-a-jwt-library-jjwt-vs-spring-boot-starter-oauth2-resource-server.md` — **Choosing a JWT Library: jjwt vs. spring-boot-starter-oauth2-resource-server**: Comparing io.jsonwebtoken jjwt (lightweight, full control to self-issue tokens; current 0.13.x, split into the jjwt-api compile dependency plus jjwt-impl and jjwt-jackson runtime dependencies) against the OAuth2 resource server's Nimbus-based JwtEncoder/JwtDecoder, and picking jjwt for this self-issued-token lab — noting the resource-server path exists and that, in Spring Security 7, NimbusJwtDecoder validates the JWT 'typ' header by default, which trips up self-issued HS256 tokens unless you configure JwtTypeValidator.
- `COURSE_L7_access-tokens-vs-refresh-tokens-expiry-and-claims.md` — **Access Tokens vs. Refresh Tokens, Expiry and Claims**: Why access tokens are short-lived and refresh tokens long-lived, how the exp claim drives the refresh flow, and what is safe to put in claims (no secrets — the payload is readable).
- `COURSE_L7_stateless-session-policy-and-csrf-for-token-apis.md` — **Stateless Session Policy and CSRF for Token APIs**: Why a Bearer-token API sets SessionCreationPolicy.STATELESS (no HttpSession, no JSESSIONID), and why CSRF protection — on by default and aggressive in Spring Security 7 — is irrelevant to and safely disabled for an API authenticated by an Authorization header rather than a cookie.
- `COURSE_L7_the-once-per-request-jwt-authentication-filter.md` — **The Once-Per-Request JWT Authentication Filter**: Writing a OncePerRequestFilter that reads the Authorization: Bearer header, validates the token, builds an Authentication, and places it in the SecurityContext — and registering it before UsernamePasswordAuthenticationFilter with addFilterBefore, slotting into Lab 6's filter chain.

**Lab 8 — Method Security & Fine-Grained Authorization**

- `COURSE_L8_method-security-vs-url-security-enablemethodsecurity.md` — **Method Security vs. URL Security: @EnableMethodSecurity**: Why URL-level rules aren't enough (they can't see method arguments or row ownership), how @EnableMethodSecurity turns on annotation-driven authorization and enables @PreAuthorize/@PostAuthorize by default in Spring Security 7, and where method security (an AOP advisor on beans) sits relative to Lab 6's filter chain.
- `COURSE_L8_preauthorize-and-postauthorize-with-spel.md` — **@PreAuthorize and @PostAuthorize with SpEL**: Writing pre- and post-invocation authorization rules using Spring Expression Language: hasRole / hasAuthority, the authentication object, and referencing method arguments with #param — and the distinction that @PreAuthorize gates entry while @PostAuthorize inspects the returned object.
- `COURSE_L8_postfilter-and-prefilter-for-collections.md` — **@PostFilter and @PreFilter for Collections**: Why filtering the elements of a returned collection (dropping rows the principal may not see) needs @PostFilter with filterObject — distinct from @PostAuthorize, which allows-or-denies a single return value rather than trimming a list.
- `COURSE_L8_ownership-and-row-level-checks.md` — **Ownership and Row-Level Checks**: Enforcing that the current principal owns a resource before mutating it, using an authentication.name / owner comparison in SpEL or a custom @Bean permission evaluator referenced from a SpEL expression for non-trivial rules.
- `COURSE_L8_testing-security-with-spring-security-test-and-withmockuser.md` — **Testing Security with spring-security-test and @WithMockUser**: Adding the spring-security-test dependency and using @WithMockUser (including @WithMockUser(username=..., roles=...)) to assert that protected methods and endpoints allow or deny the right principals, and how it populates the SecurityContext in a test.

**Lab 9 — Docker & Containerization Fundamentals**

- `COURSE_L9_why-containers-vms-vs-containers-and-the-docker-engine.md` — **Why Containers: VMs vs. Containers and the Docker Engine**: What a container and an image are, how containers share the host kernel and differ from virtual machines, and what the Docker engine/daemon, client, and registry (Docker Hub) actually do.
- `COURSE_L9_images-vs-containers-and-the-docker-cli.md` — **Images vs. Containers and the Docker CLI**: The image-as-template vs. container-as-running-instance distinction, image tags and layers, and the core lifecycle commands: docker pull/images, docker run/ps/stop/rm, docker logs, and docker exec.
- `COURSE_L9_writing-a-dockerfile-instructions-and-the-layer-cache.md` — **Writing a Dockerfile: Instructions and the Layer Cache**: FROM, WORKDIR, COPY, RUN, ENV, EXPOSE, ENTRYPOINT/CMD, how each instruction creates a cached layer, and ordering instructions (dependencies before source) so code changes do not bust the dependency cache.
- `COURSE_L9_multi-stage-builds-and-the-spring-boot-layered-jar.md` — **Multi-Stage Builds and the Spring Boot Layered Jar**: Using multiple FROM stages to build the fat jar with a JDK image and run it on a slim JRE base; extracting the Spring Boot layered jar with the Spring Boot 4 tools jarmode (java -Djarmode=tools -jar app.jar extract --layers, which supersedes the removed layertools mode) for cache-friendly rebuilds; and adding a dedicated non-root user (USER) to the runtime stage so the container does not run as root.
- `COURSE_L9_runtime-wiring-ports-volumes-and-environment-variables.md` — **Runtime Wiring: Ports, Volumes, and Environment Variables**: Publishing ports (-p host:container), persisting data with named volumes vs. bind mounts, and injecting configuration (SPRING_DATASOURCE_* / SPRING_PROFILES_ACTIVE) via -e environment variables instead of baking it into the image (the deeper 12-factor treatment of this lands in Lab 13).
- `COURSE_L9_docker-compose-multi-container-apps-on-a-shared-network.md` — **Docker Compose: Multi-Container Apps on a Shared Network**: Declaring app + Postgres services in compose.yaml, service-name DNS on the default bridge network, depends_on with condition: service_healthy backed by a Postgres healthcheck, named volumes for Postgres data, and docker compose up/down/logs — the reusable Compose foundation that Labs 17, 20, and 24 extend with one more service each.

**Lab 10 — Integration Testing with @SpringBootTest**

- `COURSE_L10_the-test-pyramid-unit-vs-slice-vs-integration.md` — **The Test Pyramid: Unit vs. Slice vs. Integration**: The cost/speed/confidence tradeoff across unit tests (no Spring), slice tests (@WebMvcTest/@DataJpaTest from earlier labs), and full @SpringBootTest integration tests, and when each layer is worth its runtime.
- `COURSE_L10_springboottest-booting-the-full-application-context.md` — **@SpringBootTest: Booting the Full Application Context**: How @SpringBootTest (org.springframework.boot.test.context.SpringBootTest, unchanged in Spring Boot 4) loads the complete context, the webEnvironment modes (MOCK vs. RANDOM_PORT) and what each means for whether a real servlet container starts, and context caching across test classes for fast suites.
- `COURSE_L10_driving-the-api-in-spring-boot-4-resttestclient-and-mockmvctester.md` — **Driving the API in Spring Boot 4: RestTestClient and MockMvcTester**: That Spring Boot 4 no longer auto-configures HTTP test clients (TestRestTemplate is deprecated and not provided by @SpringBootTest), so you opt in with @AutoConfigureRestTestClient and inject RestTestClient (org.springframework.test.web.servlet.client.RestTestClient) — one fluent client that targets either MockMvc (webEnvironment=MOCK) or a real socket (RANDOM_PORT) — plus MockMvcTester, the AssertJ-based replacement for raw MockMvc, for fine-grained server-side assertions.
- `COURSE_L10_test-data-builders-and-fixtures.md` — **Test Data Builders and Fixtures**: Building readable, intention-revealing test data with builder helpers and object mothers for the Bookstore entities/DTOs, avoiding brittle shared setup and duplicated arrange code.
- `COURSE_L10_seeding-data-with-sql.md` — **Seeding Data with @Sql**: Using @Sql (and @SqlGroup) to seed and clean known database state per class or per method, executionPhase control (BEFORE_TEST_METHOD vs. AFTER_TEST_METHOD), and when SQL seeding beats programmatic setup.
- `COURSE_L10_test-isolation-transactional-rollback-between-tests.md` — **Test Isolation: Transactional Rollback Between Tests**: How @Transactional on a test rolls back automatically for isolation, why that rollback does NOT apply to real-socket tests on RANDOM_PORT (the server runs the request in its own transaction on a separate thread), and the cleanup strategies (@Sql AFTER_TEST_METHOD scripts, @DirtiesContext) you must use instead when rollback cannot apply.

**Lab 11 — Real Dependencies in Tests with Testcontainers**

- `COURSE_L11_why-h2-lies-real-postgres-vs-in-memory-databases.md` — **Why H2 Lies: Real Postgres vs. In-Memory Databases**: Concrete ways H2 (even in PostgreSQL-compatibility mode) diverges from real Postgres — SQL dialect, native queries, types like jsonb/uuid/arrays, sequences, identifier casing, and constraint/locking behavior — and why those gaps hide bugs until production.
- `COURSE_L11_what-testcontainers-is.md` — **What Testcontainers Is**: How Testcontainers programmatically launches disposable Docker containers from your test code, its dependence on the Docker engine installed in Lab 9, and the lifecycle (start, expose a mapped random port, stop) it manages for you.
- `COURSE_L11_junit-5-integration-testcontainers-and-container.md` — **JUnit 5 Integration: @Testcontainers and @Container**: Wiring the JUnit 5 extension with @Testcontainers (org.testcontainers.junit.jupiter), declaring a PostgreSQLContainer (org.testcontainers:postgresql module) with @Container, and the difference between a static field (one container per class) and an instance field (one per test method).
- `COURSE_L11_zero-config-wiring-with-spring-boot-4-serviceconnection.md` — **Zero-Config Wiring with Spring Boot 4 @ServiceConnection**: Using the spring-boot-testcontainers dependency and @ServiceConnection (org.springframework.boot.testcontainers.service.connection) to auto-bind the datasource to the container with no manual @DynamicPropertySource, and the Spring Boot 4 gotcha that a container declared as a @Bean method (rather than a typed @Container field) needs @ServiceConnection(name = "postgres") or it fails with ConnectionDetailsFactoryNotFoundException.
- `COURSE_L11_container-lifecycle-reuse-and-keeping-tests-fast.md` — **Container Lifecycle, Reuse, and Keeping Tests Fast**: Sharing one container across the whole suite via a static singleton in a base class, Testcontainers reuse (testcontainers.reuse.enable in ~/.testcontainers.properties), and why both avoid the per-test startup tax that makes container tests feel slow.

**Lab 12 — Database Migrations with Flyway**

- `COURSE_L12_why-ddl-auto-is-dangerous-in-production.md` — **Why ddl-auto Is Dangerous in Production**: How Hibernate's ddl-auto=update/create-drop works, why update silently mismatches or fails to apply destructive changes and create-drop destroys real data, and why production schemas must be controlled by explicit migrations instead.
- `COURSE_L12_what-flyway-is-and-how-versioned-migrations-work.md` — **What Flyway Is and How Versioned Migrations Work**: From zero: Flyway as a migration runner, plain SQL migration scripts, the flyway_schema_history table, checksums, and how Flyway decides which scripts have and haven't been applied.
- `COURSE_L12_migration-naming-versioning-and-ordering.md` — **Migration Naming, Versioning, and Ordering**: The V__/U__/R__ prefix conventions, version-number ordering rules, file placement under src/main/resources/db/migration, and what makes a versioned migration immutable once applied (changing an applied script's checksum is an error).
- `COURSE_L12_repeatable-migrations-and-seed-data.md` — **Repeatable Migrations and Seed Data**: When to use R__ repeatable migrations (views, functions, reference/seed data) versus versioned V__ scripts, how Flyway runs repeatables after all versioned ones, and how checksum-based re-execution of repeatables works.
- `COURSE_L12_wiring-flyway-into-spring-boot-4-0.md` — **Wiring Flyway into Spring Boot 4.0**: From zero: that Spring Boot 4.0's modularized auto-configuration requires the spring-boot-starter-flyway starter (org.springframework.boot) plus the database-specific flyway-database-postgresql module, key spring.flyway.* properties, and how Boot runs migrations automatically on startup before the JPA EntityManager initializes.
- `COURSE_L12_baselining-an-existing-schema.md` — **Baselining an Existing Schema**: From zero: what baselining means, the baseline-on-migrate and baseline-version properties, and how to adopt Flyway on a database that already has tables without re-creating them.
- `COURSE_L12_migrations-in-the-test-pipeline.md` — **Migrations in the Test Pipeline**: How Flyway runs against the Lab 11 Testcontainers Postgres instance so tests exercise the exact production schema, replacing schema-from-entities in @DataJpaTest, and why this catches migration bugs early.

**Lab 13 — Profiles & Externalized Configuration**

- `COURSE_L13_externalized-configuration-and-spring-s-environment-abstraction.md` — **Externalized Configuration and Spring's Environment Abstraction**: From zero: what 'externalized config' means, the Environment/PropertySource model, and how Spring resolves a property at runtime from many possible sources.
- `COURSE_L13_spring-profiles-and-application-profile-files.md` — **Spring Profiles and application-{profile} Files**: From zero: what a profile is, activating profiles via spring.profiles.active, per-profile property files (application-{profile}.yml), and the spring.config.import / multi-document YAML model for organizing dev/test/prod config.
- `COURSE_L13_property-precedence-and-override-order.md` — **Property Precedence and Override Order**: The concrete order Spring uses (command-line args > environment variables > application-{profile} > application > defaults), and how later sources override earlier ones, building on the profile files introduced above.
- `COURSE_L13_type-safe-binding-with-configurationproperties.md` — **Type-Safe Binding with @ConfigurationProperties**: From zero: binding grouped properties into a typed record/class with @ConfigurationProperties, validating the bound values with Jakarta Validation (@Validated plus constraint annotations), relaxed binding, and why this beats scattered @Value injections.
- `COURSE_L13_environment-variables-and-12-factor-config.md` — **Environment Variables and 12-Factor Config**: The 12-factor 'config in the environment' principle, mapping ENV_VAR names to Spring properties via relaxed binding, and why this is essential for containers (Lab 9) and CI (Lab 25).
- `COURSE_L13_protecting-secrets.md` — **Protecting Secrets**: Why secrets must never live in committed property files, supplying them via environment variables / external config (formalizing the JWT-secret handling from Lab 7), and the role of placeholder defaults and .gitignore'd local overrides.
- `COURSE_L13_profile-conditional-beans.md` — **@Profile-Conditional Beans**: Activating or swapping beans per environment with @Profile (e.g., a real vs. stub component), and how conditional bean wiring differs from conditional property values.

**Lab 14 — API Documentation with OpenAPI & Swagger UI**

- `COURSE_L14_what-the-openapi-specification-is.md` — **What the OpenAPI Specification Is**: From zero: OpenAPI as a standard, language-agnostic JSON/YAML description of an HTTP API (paths, operations, schemas, responses) and why a machine-readable contract matters.
- `COURSE_L14_what-swagger-ui-does.md` — **What Swagger UI Does**: From zero: how Swagger UI renders an OpenAPI document into a browsable page with 'Try it out' request execution, and how it differs from the spec document itself.
- `COURSE_L14_wiring-springdoc-openapi-into-spring-boot-4-0.md` — **Wiring springdoc-openapi into Spring Boot 4.0**: From zero: adding the springdoc-openapi-starter-webmvc-ui dependency (the 3.0.x line built for Spring Boot 4 / Spring Framework 7 / Jakarta), the default /v3/api-docs and /swagger-ui.html endpoints, and how docs auto-generate from your existing controllers.
- `COURSE_L14_how-docs-are-derived-from-controllers-and-dtos.md` — **How Docs Are Derived from Controllers and DTOs**: How springdoc reads @RestController mappings, request/response DTO types, and validation annotations to produce paths and schemas automatically, with zero hand-written spec.
- `COURSE_L14_enriching-docs-with-annotations.md` — **Enriching Docs with Annotations**: Using @Operation, @Parameter, @Schema, @ExampleObject, and @Tag to add summaries, descriptions, field constraints, and realistic examples to the generated documentation.
- `COURSE_L14_documenting-errors-and-security-schemes.md` — **Documenting Errors and Security Schemes**: Documenting @ApiResponse error responses tied to your ProblemDetail (RFC 7807) bodies, and declaring a security scheme matching Lab 7's bearer JWT so protected endpoints display their auth requirement and can send the Authorization header from Swagger UI.
- `COURSE_L14_customizing-and-exposing-the-docs.md` — **Customizing and Exposing the Docs**: Configuring API metadata via an OpenAPI bean (title, version, contact, license) and using springdoc properties to control or restrict where Swagger UI is served per profile (building on Lab 13).

**Lab 15 — Operational Endpoints with Spring Boot Actuator**

- `COURSE_L15_what-spring-boot-actuator-provides.md` — **What Spring Boot Actuator Provides**: From zero: Actuator as a set of built-in production endpoints under /actuator, what comes out of the box, and why operational endpoints are kept separate from business endpoints. This is the foundational Actuator module the whole course refers back to.
- `COURSE_L15_the-core-endpoints-health-info-metrics.md` — **The Core Endpoints: health, info, metrics**: What /actuator/health, /actuator/info, and /actuator/metrics report, the health status model (UP/DOWN plus details), and how built-in indicators (datasource, disk, etc.) contribute to overall status.
- `COURSE_L15_exposing-and-securing-endpoints.md` — **Exposing and Securing Endpoints**: From zero: management.endpoints.web.exposure include/exclude (only health and info are exposed over HTTP by default), why most endpoints are hidden, running management on a separate port, and locking endpoints down with auth/roles — the exposure model Labs 22, 24, and 25 reuse.
- `COURSE_L15_custom-healthindicators.md` — **Custom HealthIndicators**: From zero: implementing the HealthIndicator interface to report the health of a domain-specific dependency, and how custom indicators aggregate into the overall health status.
- `COURSE_L15_liveness-vs-readiness-probes.md` — **Liveness vs. Readiness Probes**: From zero: the difference between liveness and readiness, the /actuator/health/liveness and /readiness probe groups (auto-enabled when Kubernetes is detected, otherwise turned on with management.endpoint.health.probes.enabled=true), and why orchestrators use each to restart vs. route traffic — the readiness probe Lab 25 gates its rollout on.
- `COURSE_L15_build-and-git-info-endpoints.md` — **Build and Git Info Endpoints**: From zero: populating /actuator/info with build metadata (the Spring Boot Maven plugin's build-info goal) and git commit details (the git-commit-id-maven-plugin) so a running instance reports exactly which version it is.

**Lab 16 — The Spring Cache Abstraction (In-Memory First)**

- `COURSE_L16_caching-fundamentals-what-why-when-and-the-staleness-risk.md` — **Caching fundamentals: what, why, when, and the staleness risk**: What a cache is, the read-through / cache-aside mental model, when caching pays off (expensive or repeated reads) vs. when it hurts (write-heavy or rarely-repeated reads), and the core tradeoff: serving stale data. Frames why every later decision (eviction, TTL, invalidation) exists to manage staleness.
- `COURSE_L16_the-spring-cache-abstraction-and-enablecaching.md` — **The Spring cache abstraction and @EnableCaching**: How Spring decouples 'what to cache' (annotations on methods) from 'where to store it' (a CacheManager bean), the role of @EnableCaching to switch the proxy machinery on, and that the abstraction is provider-agnostic by design — the same annotations work over any backing store. Unchanged in Spring Boot 4.0 (lives in spring-context).
- `COURSE_L16_concurrentmapcachemanager-in-memory-caching-with-no-infrastructure.md` — **ConcurrentMapCacheManager: in-memory caching with no infrastructure**: How adding spring-boot-starter-cache lets Spring Boot auto-configure a simple ConcurrentHashMap-backed CacheManager by default, how to declare named caches up front via spring.cache.cache-names, and its deliberate limitations: single-JVM only, no TTL, no eviction policy, lost on restart — exactly the gaps Lab 17 fixes.
- `COURSE_L16_cacheable-cacheput-and-cacheevict.md` — **@Cacheable, @CachePut, and @CacheEvict**: The three core annotations and their distinct jobs: populate-on-miss / return-on-hit reads (@Cacheable), always-execute-and-overwrite writes that keep the cache fresh (@CachePut), and entry or all-entries removal on mutation (@CacheEvict, including allEntries and beforeInvocation). Stresses that @Cacheable and @CachePut are NOT interchangeable.
- `COURSE_L16_cache-keys-and-spel-key-generation-and-conditional-caching.md` — **Cache keys and SpEL: key generation and conditional caching**: How a default key is derived from a method's arguments, how to write explicit key, unless, and condition expressions in SpEL (e.g. #id, #book.isbn, #result), and why an explicit key matters when several methods share one named cache so entries cannot collide.
- `COURSE_L16_verifying-cache-behavior-in-a-test-with-mockitospybean.md` — **Verifying cache behavior in a test with @MockitoSpyBean**: How to prove a cache works inside a @SpringBootTest: wrap the real repository with Spring Boot 4.0's @MockitoSpyBean (the replacement for the older @SpyBean), call a cached method repeatedly, and assert with Mockito.verify(..., times(1)) that the repository was hit exactly once, then once more after an eviction. Gives the student a repeatable way to measure correctness rather than guessing.
- `COURSE_L16_the-proxy-self-invocation-pitfall-and-measuring-effectiveness.md` — **The proxy self-invocation pitfall and measuring effectiveness**: Why an internal this.method() call bypasses the caching proxy entirely (the annotation silently does nothing) — the same Spring AOP proxy trap that resurfaces with @Async in Lab 18 — how to recognise and fix it (call through the injected bean / restructure), and how to demonstrate a real hit by measuring repeat-call latency and counting repository invocations.

**Lab 17 — Redis as a Cache & Store**

- `COURSE_L17_redis-from-zero-an-in-memory-key-value-store-and-its-core-data-types.md` — **Redis from zero: an in-memory key-value store and its core data types**: What Redis is and why it is fast, the key-value model, and the core data types (string, hash, list, set, sorted-set) with a one-line use case for each — enough to know which type a cache entry, a counter, and a recent-views list each map to.
- `COURSE_L17_ttl-expiry-and-redis-cli.md` — **TTL, expiry, and redis-cli**: Setting and inspecting per-key expiry (EXPIRE / TTL), the difference between expiry (time-based) and eviction (memory-pressure-based), and driving Redis interactively with redis-cli (SET / GET / HGETALL / KEYS / TTL / FLUSHDB) so the student can see what the app actually stores.
- `COURSE_L17_adding-redis-as-a-docker-compose-service.md` — **Adding Redis as a Docker Compose service**: Reusing Lab 9's Compose skills to add a Redis service to the existing compose.yaml (image, port 6379, named volume for persistence) and opening redis-cli inside the running container with docker compose exec to confirm it is live — a new service on the same stack, no new tooling and no Compose re-teaching.
- `COURSE_L17_swapping-the-spring-cache-provider-to-redis.md` — **Swapping the Spring cache provider to Redis**: How adding spring-boot-starter-data-redis (Lettuce client by default) causes Spring Boot 4.0 to auto-configure a RedisCacheManager, so Lab 16's unchanged annotations now write to Redis with zero business-code change, plus the connection properties (spring.data.redis.host / port / etc.). Reinforces the payoff of the provider-agnostic abstraction.
- `COURSE_L17_serialization-and-per-cache-ttl-with-rediscacheconfiguration.md` — **Serialization and per-cache TTL with RedisCacheConfiguration**: Why values must be serialized to bytes before Redis can store them, configuring readable JSON with GenericJacksonJsonRedisSerializer (the Jackson 3 / tools.jackson serializer in Spring Data Redis 4.0 — its Jackson-2 predecessor GenericJackson2JsonRedisSerializer is deprecated), and setting a global default plus per-cache TTLs via a RedisCacheManagerBuilderCustomizer.
- `COURSE_L17_cache-aside-vs-write-through-invalidation-and-the-cache-stampede-problem.md` — **Cache-aside vs. write-through, invalidation, and the cache-stampede problem**: The cache-aside and write-through patterns and which one @Cacheable/@CachePut implement, keeping Redis coherent under mutation via @CacheEvict / @CachePut, and how many concurrent misses on the same expired key cause a stampede on the database — plus mitigations (TTL jitter, and @Cacheable(sync=true) to serialise the recompute).
- `COURSE_L17_redistemplate-for-direct-non-cache-use.md` — **RedisTemplate for direct, non-cache use**: Using StringRedisTemplate / RedisTemplate to read and write Redis directly (opsForValue, opsForHash, and atomic INCR) for use cases that the declarative cache abstraction does not cover, such as counters and recent-activity lists.

**Lab 18 — Asynchronous & Scheduled Tasks**

- `COURSE_L18_synchronous-vs-asynchronous-execution-the-mental-model.md` — **Synchronous vs. Asynchronous Execution (the mental model)**: What 'on the request thread' means, why a slow call (PDF/email) blocks the HTTP response, and what offloading to a background thread buys you and costs you (no inline return value, eventual consistency, error visibility, and the need to communicate 'accepted, not done' to the caller).
- `COURSE_L18_threads-thread-pools-and-the-taskexecutor-abstraction-in-spring.md` — **Threads, Thread Pools & the TaskExecutor Abstraction in Spring**: Threads vs. a bounded pool, what core/max pool size and queue capacity mean, the Spring TaskExecutor/AsyncTaskExecutor abstraction, and why an unbounded or shared pool is a production hazard.
- `COURSE_L18_enableasync-and-async-running-methods-in-the-background.md` — **@EnableAsync and @Async — Running Methods in the Background**: How @EnableAsync installs a proxy around async beans, how @Async dispatches a method to a TaskExecutor, the allowed return types (void / Future / CompletableFuture), and how to name and select a specific executor by bean name with @Async("executorName"). This @EnableAsync/executor setup is the one Lab 19's @Async listeners reuse.
- `COURSE_L18_configuring-a-dedicated-threadpooltaskexecutor-and-virtual-threads-on-java-21.md` — **Configuring a Dedicated ThreadPoolTaskExecutor (and Virtual Threads on Java 21)**: Defining a named ThreadPoolTaskExecutor bean (core/max/queue/thread-name-prefix/rejection policy), why a dedicated pool beats the default SimpleAsyncTaskExecutor, and the spring.threads.virtual.enabled=true option that auto-configures a virtual-thread-backed SimpleAsyncTaskExecutor on Java 21 — including the key caveat that virtual threads ignore pool-size settings (you bound concurrency with spring.task.execution.simple.concurrency-limit instead), which is exactly why this lab still selects an explicitly named executor.
- `COURSE_L18_returning-results-with-completablefuture.md` — **Returning Results with CompletableFuture**: Returning CompletableFuture from an @Async method (CompletableFuture.completedFuture(...)), composing/joining several async calls (thenApply / thenCompose / allOf), and the difference between fire-and-forget (void) and awaiting a result.
- `COURSE_L18_async-pitfalls-self-invocation-and-exception-handling.md` — **Async Pitfalls: Self-Invocation and Exception Handling**: Why calling an @Async method from within the same bean bypasses the proxy and runs synchronously (the same Spring AOP proxy trap first met with caching in Lab 16), and how exceptions vanish from void async methods unless you register an AsyncUncaughtExceptionHandler (via AsyncConfigurer) or inspect the returned CompletableFuture.
- `COURSE_L18_enablescheduling-and-scheduled-fixedrate-fixeddelay-cron.md` — **@EnableScheduling and @Scheduled — fixedRate, fixedDelay, cron**: Wiring scheduled jobs, the precise difference between fixedRate / fixedDelay / initialDelay, writing a Spring cron expression (6 fields: second minute hour day-of-month month day-of-week), externalizing the schedule via properties, and that scheduling uses its own single-threaded scheduler separate from the @Async executor (so a long job can starve other scheduled jobs).

**Lab 19 — Decoupling with Application Events**

- `COURSE_L19_the-observer-pattern-and-why-decouple-side-effects.md` — **The Observer Pattern & Why Decouple Side Effects**: The observer pattern in plain terms, why piling notifications/audit/cache logic into one service method creates tight coupling, and how 'publish a fact, let others react' inverts that dependency so new reactions are added without touching the publisher.
- `COURSE_L19_applicationeventpublisher-and-defining-custom-events.md` — **ApplicationEventPublisher & Defining Custom Events**: Injecting ApplicationEventPublisher, defining an immutable event type (a plain record in modern Spring — no need to extend ApplicationEvent), and publishing it from a service method.
- `COURSE_L19_consuming-events-with-eventlistener.md` — **Consuming Events with @EventListener**: Writing @EventListener methods, how Spring routes events by parameter type, conditional listening with the condition attribute (SpEL), and returning a non-null value from a listener to publish a follow-on event.
- `COURSE_L19_synchronous-vs-asynchronous-listeners.md` — **Synchronous vs. Asynchronous Listeners**: That @EventListener runs synchronously in the publisher's thread (and inside its transaction) by default, and how adding @Async moves a listener onto Lab 18's named executor — plus the trade-offs (no transaction sharing, isolated failures, eventual execution). Reuses Lab 18's @EnableAsync setup rather than re-establishing it.
- `COURSE_L19_transactionaleventlistener-reacting-only-after-commit.md` — **@TransactionalEventListener — Reacting Only After Commit**: The danger of acting on an event before the DB transaction commits, and how @TransactionalEventListener with phase=AFTER_COMMIT (vs. AFTER_ROLLBACK / BEFORE_COMMIT) fires only when the data is durably saved — and that it no-ops if published outside an active transaction unless fallbackExecution is set. This AFTER_COMMIT pattern is exactly what Lab 20 uses to publish broker messages safely.
- `COURSE_L19_listener-ordering-and-error-propagation.md` — **Listener Ordering and Error Propagation**: Controlling execution order of multiple listeners with @Order, and how an exception in a synchronous listener propagates back to the publisher (and can roll the transaction back) versus being isolated in an @Async listener (where it goes only to the AsyncUncaughtExceptionHandler from Lab 18).

**Lab 20 — Asynchronous Messaging with RabbitMQ (Spring AMQP)**

- `COURSE_L20_what-a-message-broker-is-and-why-you-want-one.md` — **What a Message Broker Is and Why You Want One**: From zero: the difference between synchronous request/response and asynchronous messaging, and the three things a broker buys you — decoupling (producer doesn't know the consumer), buffering (absorb load spikes), and reliability (messages survive consumer downtime).
- `COURSE_L20_the-amqp-model-producer-exchange-queue-binding-routing-key.md` — **The AMQP Model: Producer, Exchange, Queue, Binding, Routing Key**: The core AMQP vocabulary and the key insight that producers publish to an EXCHANGE (never directly to a queue); the exchange uses bindings + routing keys to decide which queue(s) receive the message.
- `COURSE_L20_exchange-types-direct-topic-fanout.md` — **Exchange Types: direct, topic, fanout**: When to use each — direct (exact routing-key match), topic (wildcard pattern routing with * and #), fanout (broadcast to all bound queues) — with a concrete routing example for each.
- `COURSE_L20_reliability-acknowledgements-durability-and-dead-letter-queues.md` — **Reliability: Acknowledgements, Durability & Dead-Letter Queues**: Consumer acks (and why auto-ack loses messages on failure), durable exchanges/queues + persistent messages surviving a broker restart, and dead-letter exchanges/queues (DLX/DLQ): a DLQ is just an ordinary queue that a DLX routes to, and a message reaches it only when it is rejected with requeue=false (or expires/overflows) — so the working queue must be declared with the x-dead-letter-exchange argument pointing at the DLX.
- `COURSE_L20_adding-rabbitmq-management-ui-to-the-compose-stack.md` — **Adding RabbitMQ + Management UI to the Compose stack**: Adding rabbitmq:4-management as a service on the Lab 9 Compose stack, exposing ports 5672 (AMQP) and 15672 (UI), logging into the management console, and inspecting exchanges, queues, bindings, and messages by hand — reusing Lab 9's Compose skills, not re-teaching them.
- `COURSE_L20_spring-amqp-basics-starter-rabbittemplate-rabbitlistener.md` — **Spring AMQP Basics: starter, RabbitTemplate, @RabbitListener**: Adding spring-boot-starter-amqp, configuring the connection in application.properties, declaring Exchange/Queue/Binding beans so Spring auto-creates the topology on startup, sending with RabbitTemplate, and consuming with @RabbitListener.
- `COURSE_L20_sending-objects-as-json-and-handling-failures-converter-retry-dlq.md` — **Sending Objects as JSON and Handling Failures (converter, retry, DLQ)**: Registering a JacksonJsonMessageConverter (the Jackson 3 converter in Spring AMQP 4.0, under org.springframework.amqp.support.converter) so POJOs serialize to JSON; enabling listener retry via spring.rabbitmq.listener.simple.retry.* properties; and the precise mechanism by which an exhausted message reaches the DLQ — after the final attempt the default RejectAndDontRequeueRecoverer rejects the message with requeue=false, which (because the queue was declared with x-dead-letter-exchange) routes it to the DLX/DLQ rather than looping forever.

**Lab 21 — Calling Other Services with RestClient & WebClient**

- `COURSE_L21_why-services-call-services-and-what-changes-when-they-do.md` — **Why services call services (and what changes when they do)**: The mental model of one process making an HTTP request to another over the network: synchronous request/response, latency, partial failure, and why a remote call is NOT a local method call — it can be slow, fail independently, or return data your process does not control.
- `COURSE_L21_anatomy-of-an-outbound-http-call.md` — **Anatomy of an outbound HTTP call**: From zero: HTTP methods (GET/POST/PUT/DELETE), request/response headers, status-code families (2xx/4xx/5xx), request and response bodies, and content negotiation via Content-Type / Accept headers — the raw material every HTTP client manipulates.
- `COURSE_L21_restclient-the-modern-synchronous-client-spring-6-1-default-on-boot-4.md` — **RestClient: the modern synchronous client (Spring 6.1+, default on Boot 4)**: Building a RestClient with RestClient.builder(), setting a baseUrl and default headers, and the fluent get()/post()/uri()/header()/body()/retrieve() call chain; why it is the recommended synchronous client on Spring Boot 4 and fully replaces the legacy RestTemplate.
- `COURSE_L21_serializing-and-deserializing-json-with-jackson-3.md` — **Serializing & deserializing JSON with Jackson 3**: How RestClient turns a Java DTO into a JSON request body and a JSON response into a typed object via body(MyDto.class) and body(new ParameterizedTypeReference<List<X>>(){}) for generic collections; that on Spring Boot 4 Jackson 3 is the default and its packages live under tools.jackson.databind (the com.fasterxml.jackson namespace is gone).
- `COURSE_L21_sending-headers-and-authentication.md` — **Sending headers and authentication**: Attaching custom headers, an API-key header, and an Authorization header (Bearer token / Basic auth) to outbound requests, plus the trade-off between setting them once as RestClient builder defaults vs. per-request on a single call.
- `COURSE_L21_reading-status-codes-and-handling-errors.md` — **Reading status codes and handling errors**: Default RestClient behavior on 4xx/5xx (retrieve() throws), customizing it with onStatus(...), mapping a remote error into your own ProblemDetail, and the difference between an error *response* (the server answered with 4xx/5xx) and a transport *failure* (timeout, connection refused — no response at all).
- `COURSE_L21_configuring-connect-and-read-timeouts.md` — **Configuring connect & read timeouts**: From zero: connect-timeout vs. read-timeout vs. overall response timeout, why an unbounded remote call is dangerous (it can hang a request thread indefinitely), and wiring timeouts on the RestClient builder via ClientHttpRequestFactorySettings / a configured request factory — the timeout foundation Lab 22 treats as the first line of defense.
- `COURSE_L21_when-webclient-reactive-non-blocking-is-the-right-tool.md` — **When WebClient (reactive, non-blocking) is the right tool**: What 'reactive / non-blocking' means at a high level, Mono/Flux as eventual results, the spring-boot-starter-webflux dependency that brings WebClient in, and a clear decision rule: RestClient for blocking Spring MVC apps (this course's stack), WebClient for high-concurrency, streaming, or fully reactive stacks.

**Lab 22 — Resilience: Timeouts, Retries & Circuit Breakers**

- `COURSE_L22_why-remote-calls-fail-and-how-failures-cascade.md` — **Why remote calls fail and how failures cascade**: The fallacies of distributed computing in plain terms: the network is unreliable and slow; one slow dependency exhausts your request threads and cascades into a full outage; resilience is about *containing* failure, not preventing it.
- `COURSE_L22_timeouts-as-the-first-line-of-defense.md` — **Timeouts as the first line of defense**: Why every remote call must be time-bounded, the layers of timeout (connect / read / total), how to choose sane values, and how a single missing timeout silently turns one slow dependency into an outage under load — building directly on the timeouts wired in Lab 21.
- `COURSE_L22_retries-backoff-jitter-and-idempotency.md` — **Retries, backoff, jitter & idempotency**: When a retry helps vs. when it makes things worse, exponential backoff plus jitter to avoid synchronized retry storms, and the idempotency rule: GET/PUT/DELETE are generally safe to retry, a non-idempotent POST is dangerous to blindly retry.
- `COURSE_L22_native-retry-and-concurrency-limiting-in-spring-framework-7.md` — **Native retry & concurrency limiting in Spring Framework 7**: From zero: @EnableResilientMethods to turn on the feature, then @Retryable from org.springframework.resilience.annotation with its real attributes — maxRetries, delay, multiplier (exponential backoff), jitter, maxDelay, and includes/excludes — plus @Recover for a native fallback and @ConcurrencyLimit to cap concurrent invocations. This is built into the core framework on Spring Boot 4 with NO external dependency (it supersedes the old spring-retry project).
- `COURSE_L22_the-circuit-breaker-pattern.md` — **The circuit breaker pattern**: The closed / open / half-open state machine, what trips it (failure-rate or slow-call thresholds measured over a sliding window), why it gives a failing dependency time to recover, and how it converts slow failures into fast ones so callers fail immediately instead of piling up.
- `COURSE_L22_bulkheads-rate-limiting-and-time-limiting.md` — **Bulkheads, rate limiting & time limiting**: Bulkheads isolating one dependency's threads/permits so it cannot sink the whole app (the ship-compartment analogy), rate limiting to cap outbound call volume and respect a downstream's quota, and a time limiter to bound total call duration — and how these complement the native @ConcurrencyLimit from the previous module.
- `COURSE_L22_fallbacks-degrading-gracefully.md` — **Fallbacks: degrading gracefully**: Designing a fallback for when a call is blocked, rejected, or fails — cached/stale data, a sensible default, or a partial response — and where fallback logic lives (a fallbackMethod with a matching signature) relative to the protected method.
- `COURSE_L22_spring-cloud-circuit-breaker-5-x-with-resilience4j-on-spring-boot-4.md` — **Spring Cloud Circuit Breaker 5.x with Resilience4j on Spring Boot 4**: From zero: that the circuit breaker, rate limiter, bulkhead, and time limiter come from Resilience4j wired through Spring Cloud Circuit Breaker 5.x (the Spring Cloud 2025.1 'Oakwood' train, aligned with Spring Boot 4 / Spring Framework 7). How to add spring-cloud-starter-circuitbreaker-resilience4j (with spring-boot-starter-aop for the AOP interception, and Lab 15's spring-boot-starter-actuator already present), apply the @CircuitBreaker / @RateLimiter / @Bulkhead / @TimeLimiter annotations, and configure instances in application.yml. Note the division of labor: native Spring @Retryable/@ConcurrencyLimit for retries and simple concurrency caps, Resilience4j for the circuit breaker and the richer rate-limit/bulkhead/time-limit patterns.
- `COURSE_L22_observing-resilience-through-actuator.md` — **Observing resilience through Actuator**: Surfacing circuit-breaker state, call counts, and retry metrics through the Actuator health and metrics endpoints from Lab 15 (Resilience4j contributes a circuitbreakers health group and Micrometer meters), so you can literally SEE a breaker open and close — a focused application of Lab 15's endpoints, with the deeper Micrometer/Grafana treatment deferred to Lab 24.

**Lab 23 — Structured Logging & Correlation IDs**

- `COURSE_L23_logging-levels-and-when-to-use-each.md` — **Logging Levels and When to Use Each**: ERROR/WARN/INFO/DEBUG/TRACE, what belongs at each level in production, the cost of wrong-level or chatty logging, and parameterized logging ('{}' placeholders) instead of string concatenation so disabled levels cost nothing.
- `COURSE_L23_how-logging-works-in-spring-boot-slf4j-over-logback.md` — **How Logging Works in Spring Boot (SLF4J over Logback)**: The SLF4J facade vs. the Logback implementation it binds to by default, where Spring Boot's default logging config lives, and tuning per-package levels via logging.level.* in application.yml.
- `COURSE_L23_structured-json-logging-built-into-spring-boot-4.md` — **Structured (JSON) Logging Built Into Spring Boot 4**: Why JSON logs beat plain text for aggregation, and enabling Spring Boot's built-in structured output (no extra dependency) by setting logging.structured.format.console and/or logging.structured.format.file to ecs, logstash, or gelf — a feature that has shipped since Spring Boot 3.4 and is standard in 4.0.
- `COURSE_L23_the-mdc-and-a-per-request-correlation-id.md` — **The MDC and a Per-Request Correlation ID**: What the Mapped Diagnostic Context (MDC) is (a thread-local key/value map the logging framework merges into every line), how the built-in structured formats automatically promote MDC entries to JSON fields, and how to stamp a per-request correlation id into the MDC so all of a request's lines share one searchable id.
- `COURSE_L23_onceperrequestfilter-and-safe-mdc-lifecycle.md` — **OncePerRequestFilter and Safe MDC Lifecycle**: Reusing Spring's OncePerRequestFilter (first met as the JWT filter base in Lab 7) to read or generate the id, and the critical try/finally rule: always MDC.clear()/remove() in finally so a pooled worker thread never leaks one request's id onto the next. Notes the limitation that MDC is thread-bound and does not automatically follow the @Async or reactive boundaries from Lab 18.
- `COURSE_L23_log-aggregation-at-a-high-level-elk-loki.md` — **Log Aggregation at a High Level (ELK / Loki)**: The ship -> index -> query pipeline, what Elasticsearch+Kibana vs. Grafana Loki each do, and why structured, correlated logs are the input that makes aggregation and field-level search worthwhile. Conceptual only — no stack is stood up in this lab.
- `COURSE_L23_what-must-never-be-logged.md` — **What Must NEVER Be Logged**: Identifying PII, secrets, passwords, tokens, and full request/response payloads; redaction/masking strategies; and the security and compliance reasons logging them is dangerous.

**Lab 24 — Metrics & Distributed Tracing with Micrometer**

- `COURSE_L24_the-three-pillars-of-observability.md` — **The Three Pillars of Observability**: How logs, metrics, and traces differ and complement each other, and which question each one answers — connecting back to the structured logs from Lab 23.
- `COURSE_L24_micrometer-the-metrics-facade.md` — **Micrometer: The Metrics Facade**: Micrometer as the 'SLF4J of metrics', the meter types (Counter, Gauge, Timer, DistributionSummary), tags/dimensions and why low, bounded cardinality matters, and the JVM/HTTP meters Spring Boot auto-registers. (Actuator itself and its exposure rules were covered in Lab 15 and are reused, not re-taught.)
- `COURSE_L24_exposing-prometheus-metrics-via-actuator.md` — **Exposing Prometheus Metrics via Actuator**: Adding micrometer-registry-prometheus to light up the /actuator/prometheus endpoint, adding it to the Lab 15 exposure include list, and reading the Prometheus text exposition format it emits.
- `COURSE_L24_prometheus-grafana-from-zero-on-the-compose-stack.md` — **Prometheus + Grafana from Zero (on the Compose stack)**: Prometheus as a pull/scrape time-series database, writing a prometheus.yml scrape config targeting the app's /actuator/prometheus, a first PromQL query, and adding Prometheus as a Grafana datasource to build a dashboard — stood up by adding services to Lab 9's Compose stack (no Compose re-teaching).
- `COURSE_L24_micrometer-tracing-the-spring-boot-4-opentelemetry-starter.md` — **Micrometer Tracing + the Spring Boot 4 OpenTelemetry Starter**: Traces and spans, W3C context propagation, and how Spring Boot 4 collapses tracing setup into a single starter, org.springframework.boot:spring-boot-starter-opentelemetry, which pulls the Micrometer tracing OTel bridge and OTLP exporter for you (no longer hand-adding micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp as in 3.x). Notes that the tracing autoconfiguration moved into the new org.springframework.boot.micrometer.tracing.* packages in 4.0.
- `COURSE_L24_automatic-log-correlation-traceid-spanid-in-the-mdc.md` — **Automatic Log Correlation: traceId/spanId in the MDC**: That with Micrometer Tracing present Spring Boot automatically populates traceId and spanId in the MDC, how to surface them in plain-text logs via logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}] (the Lab 23 structured formats pick MDC up automatically), and how this framework-provided traceId relates to — and can replace or sit alongside — the manual correlationId from Lab 23.

**Lab 25 — CI/CD & Deployment (Capstone)**

- `COURSE_L25_what-ci-cd-is-and-why-it-exists.md` — **What CI/CD Is and Why It Exists**: Continuous Integration vs. Continuous Delivery vs. Continuous Deployment, the build->test->package->deploy pipeline, and why automation beats manual releases.
- `COURSE_L25_github-actions-from-zero.md` — **GitHub Actions from Zero**: Workflows, jobs, steps, triggers (push/pull_request), runners (ubuntu-latest), the marketplace-action model (actions/checkout, actions/setup-java with built-in Maven dependency caching), and how jobs pass artifacts/outputs between each other.
- `COURSE_L25_running-the-full-test-suite-in-ci-including-testcontainers.md` — **Running the Full Test Suite in CI (Including Testcontainers)**: Why ubuntu-latest ships with a Docker daemon so the Lab 11 Testcontainers integration tests run unchanged in CI, how mvn verify fails the build on any red test, and surfacing the test reports as a job summary or artifact.
- `COURSE_L25_building-and-pushing-a-docker-image-to-a-registry.md` — **Building and Pushing a Docker Image to a Registry**: docker/setup-buildx-action, docker/login-action, and docker/build-push-action building the Lab 9 image; image registries (GHCR vs. Docker Hub); authenticating GHCR with the built-in GITHUB_TOKEN plus the packages: write permission; and meaningful tagging (commit SHA + semver + latest).
- `COURSE_L25_handling-secrets-and-environments-in-ci.md` — **Handling Secrets and Environments in CI**: GitHub Actions secrets vs. (non-secret) variables, GitHub Environments for promotion (e.g. staging -> production) including required-reviewer gates, and never baking credentials into images or echoing them into logs — the CI-side enforcement of Lab 13's secret discipline.
- `COURSE_L25_deploying-to-a-real-target-with-a-health-gated-rollout.md` — **Deploying to a Real Target with a Health-Gated Rollout**: Deploying the pushed image to a container host (Render or Fly.io) — with a high-level look at the equivalent Kubernetes Deployment + Service manifest — and gating the rollout on the Lab 15 Actuator /actuator/health readiness probe so a failing build can't go live.

## Beyond This Roadmap

A few topics are intentionally left out so the path stays focused. They are natural next
steps once these 25 labs are done:

- **Reactive stack (WebFlux / R2DBC)** — beyond the `WebClient` taste in Lab 21.
- **Kafka** — an alternative to RabbitMQ for high-throughput event streaming.
- **Kubernetes in depth** — Lab 25 introduces container deployment; production orchestration
  is its own journey.
- **gRPC / GraphQL** — alternative API styles to REST.

> This roadmap is a living document. The order and contents of planned labs may shift as the
> journey progresses.
