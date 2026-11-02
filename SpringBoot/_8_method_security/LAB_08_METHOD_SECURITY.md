# Lab 08 — TaskFlow: Method Security & Fine-Grained Authorization

> **Start from your completed Lab 7 project.** This lab is an increment on TaskFlow, not a rebuild.
> You will copy your Lab 7 module, then add per-row authorization on top of it.
>
> **Read first:** `COURSE_08_METHOD_SECURITY.md` (in this folder). This assignment tells you *what* to
> build; the course document explains *why* each mechanism exists and how it works. Section numbers
> below reference it as **[C-§n]**.

| | |
| --- | --- |
| **Module** | `_8_method_security` (base package `com.example._8_method_security`) |
| **Starting point** | your finished Lab 7 TaskFlow project (`_7_jwt`), all tests green |
| **Stack** | Spring Boot 4.0.6 · Spring Security 7.0.5 · Java 21 · Maven · PostgreSQL (H2 in tests) |
| **New dependencies** | **none** — this is the rare lab that adds capability with zero new jars |
| **Deliverable** | the module below, `./mvnw test` fully green, plus your own Postman collection |
| **Reference test count** | **201** (98 carried forward from Lab 7 + 103 new) |

---

## 1. Problem Statement & Goal

Lab 7 gave TaskFlow real authentication: a signed JWT identifies the caller, the filter chain rejects
anonymous traffic, and `/api/admin/**` is restricted to ADMIN. And yet TaskFlow has a hole so large
that its own source code apologises for it:

```java
// Lab 7 — Project.java
// Per-row ownership enforcement (only the owner may mutate) is deliberately deferred to Lab 8
// (method security) — in Lab 7 any authenticated user may update or delete any project.
```

Concretely: alice owns project 42. bob registers, logs in, gets a completely valid token, and sends

```
PUT /api/projects/42          Authorization: Bearer <bob's valid token>
{ "name": "Mine now" }
```

Lab 7 answers **200 OK**. bob is authenticated, and `anyRequest().authenticated()` is the entire rule.

You cannot fix this with a better URL matcher, and understanding why is the point of the lab:
`PUT /api/projects/42` is byte-for-byte identical whether you own project 42 or not. The deciding fact
— who owns row 42 — is not available at the filter chain, only one layer down, in the service method
that receives the id. **The rule has to live where the data is.** [C-§1]

**Your goal:** move authorization from coarse URL rules down to individual methods and individual rows.
You will

1. turn on annotation-driven method security,
2. close Lab 7's ownership gap on `Project` and `Task`,
3. add a new owner-scoped resource of your own — task **`Comment`s** — to exercise every annotation,
4. make a denial render as a **403 ProblemDetail** rather than a 500, and
5. test the rules in the one layer where they actually run.

---

## 2. Learning Objectives (what this assesses)

By the end you must be able to demonstrate, in code:

- **Method vs. URL security.** `@EnableMethodSecurity` as a *second* layer beside the filter chain, and
  an articulate answer to "why not just write a better matcher?"
- **`@PreAuthorize` with SpEL** over roles, the principal, method arguments (`#id`), and properties of
  arguments (`#request.projectId`) — the last being a rule no URL layer can express.
- **`@PostAuthorize`** over `returnObject`, and the discipline that it belongs on reads only.
- **`@PostFilter`** to trim a returned collection per principal — including its mutable-collection
  requirement and why it must never be used on a paginated response.
- **Row-level ownership** two ways: direct argument comparison, and a fail-closed **permission bean**
  invoked from SpEL by bean name.
- **Rules that must read the request body** (blocking a task from being smuggled into another project).
- **Denial rendering:** `AuthorizationDeniedException` → 403 ProblemDetail, plus an
  `AccessDeniedHandler` so URL-rule 403s match.
- **The AOP self-invocation trap**, recognised as a *security* bug rather than an inefficiency.
- **Test-layer judgement:** proving rules with `@WithMockUser` in a minimal `@EnableMethodSecurity`
  context, and explaining why a Mockito unit test and a `@WebMvcTest` slice both prove nothing here.

---

## 3. Starting Point

### 3.1 Create the Lab 8 module

Copy your finished Lab 7 project and rename it. From the repository root:

1. Copy `_7_jwt` to a new folder `_8_method_security`.
2. Delete the copied `target/` directory and the Lab 7 documents
   (`COURSE_07_JWT.md`, `LAB_07_JWT_TASKFLOW.md`, `TaskFlow_JWT.postman_collection.json`).
3. Rename the package folders:
   `src/main/java/com/example/_7_jwt` → `src/main/java/com/example/_8_method_security`, and the same
   under `src/test/java/com/example/`.
4. Replace every occurrence of the text `_7_jwt` with `_8_method_security` across `*.java`, `pom.xml`
   and `*.properties`. This covers the `package`/`import` statements, the pom `<artifactId>`/`<name>`,
   and `spring.application.name`.
5. Update the pom `<description>` to `Lab 8 - TaskFlow: method security &amp; fine-grained
   authorization`.
6. **Confirm the baseline before changing anything:** `./mvnw test` → **98 tests, all green.** If it is
   not green, fix that first; you cannot tell which failures you caused otherwise.

Everything else — `application.properties`, the JWT secret and TTLs, the seeded `admin` / `admin12345`
account, the Postgres datasource (`jdbc:postgresql://localhost:5432/taskflow_lab`) — carries over
unchanged.

> **A flaky test to check while you are here — only if you wrote it yourself.** The `_7_jwt`
> *reference* already tampers with the first character of the signature segment, so if you copied that
> module you will find nothing to change and can skip this box. But if you wrote
> `JwtServiceTest.rejectsTampered` / `JwtAuthenticationFilterTest.tamperedTokenRejected` from LAB_07's
> prose, you probably flipped the token's **last** character — and that test fails roughly 1 run in 16.
> A 32-byte HS256 signature base64url-encodes to 43 unpadded characters, so the final character carries
> only 4 significant bits: its low 2 bits are ignored padding, and `'A'`↔`'B'` can decode to the
> identical signature, leaving the "tampered" token valid. Tamper with the **first** character of the
> signature segment instead (all 6 bits significant):
>
> ```java
> private String tamper(String token) {
>     int lastDot = token.lastIndexOf('.');
>     char first = token.charAt(lastDot + 1);
>     char replacement = (first == 'a') ? 'b' : 'a';
>     return token.substring(0, lastDot + 1) + replacement + token.substring(lastDot + 2);
> }
> ```

### 3.2 The Lab 7 classes this lab touches

You are not rebuilding TaskFlow. For orientation, here is exactly what Lab 8 leaves alone and what it
touches:

| Package | Untouched | Changed / added by Lab 8 |
| ------- | --------- | ------------------------ |
| `model` | `User`, `Project`, `Role`, `Status`, `Priority`, `RefreshToken` | **+ `Comment`**; `Task` (+one inverse collection, §7.4a) |
| `repository` | `UserRepository`, `RefreshTokenRepository` | `ProjectRepository`, `TaskRepository` (**+2 / +3 methods**), **+ `CommentRepository`** |
| `dto` | all Lab 7 DTOs unchanged | **+ `TaskSummaryDto`, `CommentRequestDto`, `CommentUpdateRequestDto`, `CommentResponseDto`** |
| `exception` | `ResourceNotFoundException`, `DuplicateResourceException`, `InvalidTokenException` | `GlobalExceptionHandler` (**+1 handler**) |
| `service` | `AuthService(+Impl)`, `RefreshTokenService(+Impl)` | `ProjectServiceImpl`, `TaskServiceImpl`, `UserServiceImpl` (**annotations**), **+ `CommentService(+Impl)`** |
| `security` | `JwtService`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`, `UserDetailsServiceImpl`, `AdminUserSeeder` | `SecurityConfig` (**+`@EnableMethodSecurity`, +one `@Bean`, +`accessDeniedHandler`**), **+ `TaskFlowPermissions`, `RestAccessDeniedHandler`** |
| `controller` | `AuthController`, `ProjectController`, `AdminController` (the class; its *test* gains one case, §10.2) | `TaskController`, `UserController` (**+1 endpoint each**), **+ `CommentController`** |

Note what is *not* in the "changed" column: no service **interface** signature changes at all, and the
only controller changes are added endpoints. Authorization is genuinely additive here.

---

## 4. The Big New Idea

**A rule that depends on data cannot be enforced before the data is available.**

That single sentence generates everything in this lab:

- A URL matcher runs in the filter chain, where it can see the verb, the path and the
  `Authentication` — and *cannot* see the request body, the row, or who owns it.
- So per-row rules move onto the **service methods**, which receive the id and can query.
- Spring Security enforces them with an **AOP proxy** around the bean, which is why an internal
  `this.method()` call silently skips them [C-§2.3], and why only a Spring context (not a Mockito
  test) can prove them [C-§8].
- Some rules can only be answered *after* the row is loaded → `@PostAuthorize` on `returnObject`.
- Some rules apply per *element* of a returned list → `@PostFilter` on `filterObject`.
- And some rules need data no annotation should compute inline → a **permission bean** called from
  SpEL, which must fail closed on every uncertainty.

Keep the URL rules coarse. You are adding a layer, not replacing one. [C-§1.3]

---

## 5. Setup Delta

**No new dependencies.** Method security lives in `spring-security-core` / `spring-security-config`
(already present via the security starter since Lab 6), and its AOP machinery comes from `spring-aop`
(transitive via `spring-context`). You do **not** need `spring-boot-starter-aop` or AspectJ.
`spring-security-test` — for `@WithMockUser` — has also been on the test classpath since Lab 6.

**No new properties.** `application.properties` is unchanged.

Two build facts you depend on without writing them:

- **`-parameters`.** `#username` in a SpEL expression resolves a *parameter name*, which requires javac's
  `-parameters` flag. `spring-boot-starter-parent` enables it, so you inherit it. If an expression ever
  fails to bind a name, this is why.
- **`proxyTargetClass = false`** is `@EnableMethodSecurity`'s default, so beans that implement an
  interface get **JDK dynamic proxies**. Consequence for your tests: autowire `ProjectService`, never
  `ProjectServiceImpl`.

---

## 6. File Inventory

**NEW (11 files)**

```
model/Comment.java
repository/CommentRepository.java
dto/TaskSummaryDto.java
dto/CommentRequestDto.java
dto/CommentUpdateRequestDto.java
dto/CommentResponseDto.java
security/TaskFlowPermissions.java          ← the heart of the lab
security/RestAccessDeniedHandler.java
service/CommentService.java
service/CommentServiceImpl.java
controller/CommentController.java
```

**CHANGED (9 files)**

```
security/SecurityConfig.java               (+@EnableMethodSecurity, +accessDeniedHandler)
exception/GlobalExceptionHandler.java      (+AuthorizationDeniedException → 403)
repository/ProjectRepository.java          (+2 methods)
repository/TaskRepository.java             (+3 methods)
service/ProjectServiceImpl.java            (annotations + scoped list query)
service/TaskServiceImpl.java               (annotations)
service/UserServiceImpl.java               (annotations + de-self-invocation)
controller/TaskController.java             (+GET /{id}/comments, +CommentService dep)
controller/UserController.java             (+GET /{username})
model/Task.java                            (+the inverse comments collection, for the delete cascade)
```

Note what is *not* in that list: **no existing service interface changes signature**. Authorization is
additive here.

**NEW tests (6 files)** + **CHANGED tests (5 files)** — see §10.

---

## 7. Implementation Inventory (class-by-class)

### 7.1 `security/SecurityConfig` — CHANGED (two additions)

**(a)** Add the class-level annotation, importing
`org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity`:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // ← Lab 8
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
```

Nothing else is needed to switch it on. `prePostEnabled` defaults to `true` in Spring Security 7, so
`@PreAuthorize` / `@PostAuthorize` / `@PreFilter` / `@PostFilter` are all active. **Without this
annotation every rule you write in this lab is an inert comment** — the code compiles, tests pass, and
nothing is enforced. [C-§2.1]

**(b)** Add a `RestAccessDeniedHandler` bean and register it on the chain:

```java
@Bean
RestAccessDeniedHandler restAccessDeniedHandler() {
    return new RestAccessDeniedHandler();
}
```

Add it as a parameter of `securityFilterChain(...)` and extend the existing `exceptionHandling`:

```java
.exceptionHandling(ex -> ex
        .authenticationEntryPoint(jwtAuthenticationEntryPoint)   // 401 — Lab 7
        .accessDeniedHandler(restAccessDeniedHandler))           // 403 — Lab 8
```

**Leave `authorizeHttpRequests` exactly as Lab 7 left it.** `/api/comments/**` needs no new entry: it
is already covered by `anyRequest().authenticated()`. Keeping the URL rules coarse is the design, and
worth a comment saying so.

### 7.2 `security/RestAccessDeniedHandler` — NEW

Implements `org.springframework.security.web.access.AccessDeniedHandler`. A plain class (instantiated
as a `@Bean` above, not component-scanned — matching how Lab 7 declares its entry point so a
`@WebMvcTest` that imports `SecurityConfig` gets a fully wired chain).

```java
@Override
public void handle(HttpServletRequest request, HttpServletResponse response,
        AccessDeniedException accessDeniedException) throws IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getWriter().write(
            "{\"type\":\"about:blank\",\"title\":\"Access denied\",\"status\":403,"
                    + "\"detail\":\"You do not have permission to perform this action.\"}");
}
```

Write the JSON by hand (no `ObjectMapper` dependency), exactly as `JwtAuthenticationEntryPoint` does,
so it behaves identically in every test slice.

**Why this exists at all:** there are now two places a 403 is born. A **URL rule** is evaluated by the
`AuthorizationFilter` before any controller runs, so nothing reaches your `@RestControllerAdvice` — this
handler writes that body. A **method rule** throws inside the dispatch, where the advice catches it
(§7.3). Both must report the same `title` / `status` / `detail`, or clients see two different 403s
depending on which layer said no. [C-§7.3]

### 7.3 `exception/GlobalExceptionHandler` — CHANGED (one handler)

Import `org.springframework.security.authorization.AuthorizationDeniedException` and add:

```java
@ExceptionHandler(AuthorizationDeniedException.class)
public ProblemDetail handleAuthorizationDenied(AuthorizationDeniedException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
            "You do not have permission to perform this action.");
    pd.setTitle("Access denied");
    return pd;
}
```

**This handler is not optional, and it is the single easiest thing to forget in this lab.**
`AuthorizationDeniedException` is a `RuntimeException`, and this class already ends with
`@ExceptionHandler(Exception.class)` returning 500. Without the entry above, **every legitimate denial
in your application returns 500** — your endpoints look broken rather than protected. Spring picks the
most specific handler, so adding it is the whole fix. [C-§7.2]

Two deliberate details:

- The detail is a **fixed generic sentence**. Never echo `ex.getMessage()` and never explain *why* —
  "you are not the owner of project 42" confirms that project 42 exists and belongs to someone else.
- Place it above the `ResourceNotFoundException` handler for readability; order does not affect
  matching, specificity does.

### 7.4 `model/Comment` — NEW

The lab's owner-scoped resource: a note on a `Task`, written by a `User`.

| Field | Type | Mapping |
| ----- | ---- | ------- |
| `id` | `Long` | `@Id @GeneratedValue(strategy = IDENTITY)` |
| `text` | `String` | `@Column(nullable = false, length = 2000)` |
| `createdAt` | `Instant` | `@Column(nullable = false, updatable = false)` |
| `task` | `Task` | `@ManyToOne(fetch = LAZY, optional = false)`, `@JoinColumn(name = "task_id", nullable = false)` |
| `author` | `User` | `@ManyToOne(fetch = LAZY, optional = false)`, `@JoinColumn(name = "author_id", nullable = false)` |

- `@Entity @Table(name = "comments")`, Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor
  @Builder` — the same house style as `Project` / `Task`.
- Both `@ManyToOne` sides LAZY (Lab 3 discipline); the repository finders pull them with an
  `@EntityGraph`.
- **`author` is the row's owner.** Every write rule in this lab is expressed relative to it, and unlike
  `Project.owner` it is load-bearing from day one.
- `createdAt` is `updatable = false` and stamped by a JPA lifecycle callback, so no caller (and no
  `@DataJpaTest` fixture) has to remember it:

```java
@PrePersist
void stampCreatedAt() {
    if (createdAt == null) {
        createdAt = Instant.now();
    }
}
```

### 7.4a `model/Task` — CHANGED (one inverse collection, and you must not skip it)

Add the inverse side of the new association to `Task`:

```java
@Builder.Default
@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Comment> comments = new ArrayList<>();
```

(New imports: `java.util.ArrayList`, `java.util.List`, `jakarta.persistence.CascadeType`,
`jakarta.persistence.OneToMany`.)

**This is not decoration — leave it out and you ship a 500.** Once comments exist,
`DELETE /api/tasks/{id}` on a task that has any violates the `comments.task_id` foreign key. The
resulting `DataIntegrityViolationException` falls through to your catch-all
`@ExceptionHandler(Exception.class)` and the project owner — who is perfectly entitled to delete the
task — gets **500 Internal Server Error** instead of `204`. A crash, not a refusal.

`cascade = ALL` + `orphanRemoval = true` is the correct domain semantic here rather than a workaround: a
comment is a **leaf** that has no meaning without its task, so the thread should die with it.

Note what this collection is *not* for. Nothing reads it: the thread is served by
`CommentRepository.findByTaskIdOrderByCreatedAtAsc` (so it can be filtered, and so a task list can
never trigger an N+1 through it), and `TaskResponseDto` deliberately does not expose comments. It
exists purely so the delete cascades. Prove it with the `@DataJpaTest` case in §10.2.

> **A related 500 that is NOT in scope.** `Task.project` has the same shape — `optional = false`, no
> cascade — so `DELETE /api/projects/{id}` on a project that still has tasks fails the same way. That
> behaviour is inherited unchanged from Lab 7 and this lab does not alter it; Lab 8 only owns the
> problem it introduced. Avoid it in manual testing, and see §15.

### 7.5 `repository/CommentRepository` — NEW

`extends JpaRepository<Comment, Long>`:

```java
@Override
@EntityGraph(attributePaths = {"task", "author"})
Optional<Comment> findById(Long id);

/** The comment thread of one task, oldest first. */
@EntityGraph(attributePaths = {"task", "author"})
List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

/** True when comment {@code id} exists AND was written by {@code username}. */
boolean existsByIdAndAuthorUsername(Long id, String username);
```

### 7.6 `repository/ProjectRepository` — CHANGED (+2)

```java
/**
 * The projects owned by one user, paginated — how Lab 8 scopes GET /api/projects. The WHERE clause
 * does the filtering, so totalElements / totalPages stay truthful.
 */
@EntityGraph(attributePaths = "owner")
Page<Project> findByOwnerUsername(String username, Pageable pageable);

/** True when project {@code id} exists AND is owned by {@code username}. */
boolean existsByIdAndOwnerUsername(Long id, String username);
```

### 7.7 `repository/TaskRepository` — CHANGED (+3)

```java
/** True when task {@code id} exists AND sits in a project owned by {@code username}. */
boolean existsByIdAndProjectOwnerUsername(Long id, String username);

/** True when task {@code id} exists AND is assigned to {@code username}. */
boolean existsByIdAndAssigneeUsername(Long id, String username);

/** True when task {@code id} exists AND already belongs to project {@code projectId}. */
boolean existsByIdAndProjectId(Long id, Long projectId);
```

Note the derived-query property traversal: `ByIdAndProjectOwnerUsername` resolves to
`id` + `project.owner.username`, two association hops from `Task`. Spring Data parses these names when
the repository is created, so a typo breaks **context startup** — which is exactly why §10 requires
`@DataJpaTest` coverage for them.

### 7.8 `dto` — four NEW classes

All four use the house style (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor`; add `@Builder`
to the two request DTOs).

| Class | Fields | Notes |
| ----- | ------ | ----- |
| `TaskSummaryDto` | `Long id`, `String title` | compact task reference, mirroring `ProjectSummaryDto` |
| `CommentRequestDto` | `Long taskId`, `String text` | `@NotNull(message = "Task id is required")` on `taskId`; `@NotBlank(message = "Text is required")` and `@Size(max = 2000, message = "Text must be at most 2000 characters")` on `text`. **No author field** — it is server-controlled |
| `CommentUpdateRequestDto` | `String text` | the same two text constraints, same messages. A separate DTO so the contract "only the text is editable" is unmissable |
| `CommentResponseDto` | `Long id`, `String text`, `Instant createdAt`, `TaskSummaryDto task`, `UserSummaryDto author` | — |

**`CommentResponseDto`'s field list is not arbitrary.** `author.username` and `task.id` are read by the
`@PostAuthorize` and `@PostFilter` expressions in §7.10. A post-invocation rule can only inspect what
the returned object exposes, so the DTO must carry what the rule needs. [C-§4.1]

### 7.9 `security/TaskFlowPermissions` — NEW (the heart of the lab)

```java
@Component("taskFlowPermissions")
@RequiredArgsConstructor
public class TaskFlowPermissions {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    …
}
```

**Name the bean explicitly.** SpEL's `@taskFlowPermissions` is a *bean-name* reference resolved from a
string at request time. Relying on the default name means a class rename breaks every expression — and
breaks it when a user hits the endpoint, not when you compile.

Six public methods, each `(… , Authentication authentication) → boolean`:

| Method | Returns true when | Backed by |
| ------ | ----------------- | --------- |
| `isProjectOwner(Long projectId, Authentication)` | caller owns that project | `projectRepository.existsByIdAndOwnerUsername` |
| `isProjectOwnerOfTask(Long taskId, Authentication)` | caller owns the task's project | `taskRepository.existsByIdAndProjectOwnerUsername` |
| `isTaskAssignee(Long taskId, Authentication)` | task is assigned to the caller | `taskRepository.existsByIdAndAssigneeUsername` |
| `canWorkOnTask(Long taskId, Authentication)` | `isProjectOwnerOfTask` **OR** `isTaskAssignee` | composition, no query of its own |
| `canPlaceTaskInProject(Long taskId, Long projectId, Authentication)` | see below | two probes |
| `isCommentAuthor(Long commentId, Authentication)` | caller wrote that comment | `commentRepository.existsByIdAndAuthorUsername` |

**Every method follows the same five rules** [C-§6.3] — these are correctness requirements, not style:

1. **Return a boolean, never throw.** A missing row is "no", not an error.
2. **Fail closed.** Null id, anonymous caller, or absent row → `false`.
3. **One boolean query; never load an entity.** These run on every protected call, and `@PostFilter`
   may invoke one *per element*.
4. **Compose in Java, not SpEL** (that is what `canWorkOnTask` is for).
5. **Treat anonymous as "no principal".**

The shape of the four simple probes, and the shared helper:

```java
public boolean isProjectOwner(Long projectId, Authentication authentication) {
    String username = usernameOf(authentication);
    if (projectId == null || username == null) {
        return false;
    }
    return projectRepository.existsByIdAndOwnerUsername(projectId, username);
}

/**
 * The caller's username, or null when there is no real principal. The AnonymousAuthenticationToken
 * check matters: an unauthenticated request does not carry a null Authentication, it carries an
 * anonymous token whose getName() is the literal string "anonymousUser".
 */
private static String usernameOf(Authentication authentication) {
    if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
        return null;
    }
    return authentication.getName();
}
```

`canWorkOnTask` is plain composition:

```java
public boolean canWorkOnTask(Long taskId, Authentication authentication) {
    return isProjectOwnerOfTask(taskId, authentication) || isTaskAssignee(taskId, authentication);
}
```

And `canPlaceTaskInProject` is the interesting one — **read the last clause carefully**:

```java
public boolean canPlaceTaskInProject(Long taskId, Long projectId, Authentication authentication) {
    // Note the principal guard HERE, ahead of the short-circuit below — not only inside the two
    // probes at the end. See the warning after this snippet.
    if (taskId == null || projectId == null || usernameOf(authentication) == null) {
        return false;
    }
    // Unchanged project → nothing is being moved, so no extra permission is required.
    if (taskRepository.existsByIdAndProjectId(taskId, projectId)) {
        return true;
    }
    // A real move: authority over the source AND the destination.
    return isProjectOwnerOfTask(taskId, authentication)
            && isProjectOwner(projectId, authentication);
}
```

> ⚠️ **Do not drop that `usernameOf(authentication) == null` clause.** It is the one place in this class
> where rule 2 is easy to get wrong, because the "nothing is being moved" short-circuit comes *before*
> the two ownership probes. Guard only the ids, and an anonymous caller whose task already sits in the
> named project falls into that branch and receives **`true`** — after issuing a query to find out.
> Fail-closed does not mean "eventually returns false"; it means **no principal, no work, no**. Today
> that leak is unreachable through `updateTask`, because `canWorkOnTask` is ANDed in front and rejects
> anonymous first — and that is exactly the reasoning that lets a permission primitive rot: it is safe
> until someone reuses it. §10.2's fail-closed group asserts this, so you will see it go red.

**The hole this closes** [C-§6.5]: `updateTask` legitimately allows the **assignee**, and
`TaskRequestDto` carries `projectId`. So bob — assigned a task in alice's project — could send a PUT
naming a project of his own and walk the task out of alice's project. Same path, same verb, same
principal: **only the body differs.** Note that owning the *destination* alone is deliberately **not**
sufficient — that is precisely the theft being prevented. A move restructures two projects, so it takes
authority over both.

No `@Transactional` is needed on this class: each method issues one statement and never navigates a
lazy association.

### 7.10 `service/CommentService` + `CommentServiceImpl` — NEW

**Interface** (`CommentService`) — five methods:

```java
CommentResponseDto createComment(CommentRequestDto request, String authorUsername);
List<CommentResponseDto> getCommentsByTask(Long taskId);
CommentResponseDto getCommentById(Long id);
CommentResponseDto updateComment(Long id, CommentUpdateRequestDto request);
void deleteComment(Long id);
```

**Implementation** — `@Service @RequiredArgsConstructor @Transactional(readOnly = true)`, injecting
`CommentRepository`, `TaskRepository`, `UserRepository`. Manual DTO mapping in a private
`toResponse(Comment)` (no MapStruct in TaskFlow), and a private
`getEntity(Long)` that throws `ResourceNotFoundException("Comment", id)`.

> **Put the annotations on the implementation, not the interface — and never on both.** Spring Security
> finds them on the target class either way. Duplicating them does **not** raise an error in Spring
> Security 7: the scanner returns the *closest* declaration, so the implementation silently wins and
> the interface's annotation is never read — which is worse than a failure, because a stale or
> stricter rule left on an interface looks protective and does nothing. [C-§2.4]

Method by method:

**`createComment`** — `@Transactional`

```java
@PreAuthorize("#authorUsername == authentication.name and (hasRole('ADMIN') "
        + "or @taskFlowPermissions.canWorkOnTask(#request.taskId, authentication))")
```

Body: resolve the task (404 if absent), resolve the author by username (404 if absent), build the
`Comment`, save, map. The author comes from `authorUsername` (the controller passes
`Principal.getName()`) and the expression pins it to the real principal, so nobody can post as someone
else. `#request.taskId` reads the target task out of the **request body** — a rule with nothing for a
URL matcher to match on.

**`getCommentsByTask`** — two annotations, two jobs

```java
@PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.canWorkOnTask(#taskId, authentication)")
@PostFilter("hasRole('ADMIN') or filterObject.author.username == authentication.name "
        + "or @taskFlowPermissions.isProjectOwnerOfTask(#taskId, authentication)")
public List<CommentResponseDto> getCommentsByTask(Long taskId) {
    if (!taskRepository.existsById(taskId)) {
        throw new ResourceNotFoundException("Task", taskId);
    }
    // MUST be mutable — see below.
    return new ArrayList<>(commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
            .map(this::toResponse)
            .toList());
}
```

`@PreAuthorize` gates the whole call (403 if you have no stake in the task); `@PostFilter` then trims
the rows you may not see, so an entitled caller with nothing visible gets **200 and an empty list**
rather than an error.

**TaskFlow's comment visibility policy:** a comment is visible to its **author**, to the **owner of the
enclosing project**, and to **ADMIN**. So an assignee who does not own the project can open the thread
and see their own notes, while the project owner sees the whole discussion.

> ⚠️ **The mutable-list requirement — this will bite you.** Spring Security filters by calling
> `clear()` and then `addAll()` on **the very collection you returned**. `stream().toList()` and
> `List.of(...)` are immutable, so `@PostFilter` fails at **runtime** with
> `UnsupportedOperationException` — not at compile time. Every *other* list-mapping method in TaskFlow
> uses `.toList()`; this one must not. [C-§5.1]

> **Cost — and be precise about what ordering does and does not buy.** The expression is evaluated
> **once per element**. SpEL short-circuits `or` left to right, so the two free clauses come first and
> the query-backed probe last — but that only helps for elements the free clauses *match*. A project
> owner reading a thread fails `filterObject.author.username` on every comment written by someone
> else, so `isProjectOwnerOfTask(#taskId, …)` fires once per such comment. Note that its argument is
> `#taskId`, a **method** argument, not `filterObject`: the answer is identical for every element, so
> it is the same query repeated N times. Ordering does not bound that; hoisting it would (compute it
> once before the filter), and for a long thread the query-level approach from
> `ProjectServiceImpl.getAllProjects` is the real answer.

**`getCommentById`**

```java
@PostAuthorize("hasRole('ADMIN') or returnObject.author.username == authentication.name "
        + "or @taskFlowPermissions.isProjectOwnerOfTask(returnObject.task.id, authentication)")
public CommentResponseDto getCommentById(Long id) {
    return toResponse(getEntity(id));
}
```

The rule that cannot be a `@PreAuthorize`: whether you may see this comment depends on who wrote it,
and the only way to learn that is to fetch it. Note the expression both navigates `returnObject` and
hands `returnObject.task.id` to the permission bean. On denial the value is discarded and the caller
gets 403.

**`updateComment`** — `@Transactional`

```java
@PreAuthorize("@taskFlowPermissions.isCommentAuthor(#id, authentication)")
```

**Author only — no ADMIN override.** Deliberate: editing someone else's comment would put words in
their mouth. Body: load via `getEntity(id)`, set **only** `text`, then
`return toResponse(commentRepository.save(comment))` — `task`, `author` and `createdAt` are never
reassigned. (The entity is managed inside the transaction, so the explicit `save` is not strictly
required; call it anyway to match `ProjectServiceImpl.updateProject`'s house style and to make the
returned DTO obviously reflect the persisted state.)

**`deleteComment`** — `@Transactional`

```java
@PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isCommentAuthor(#id, authentication)")
```

Author or ADMIN — removing a comment is legitimate moderation, unlike rewriting it. Body: 404 if
absent, else `deleteById`.

### 7.11 `service/ProjectServiceImpl` — CHANGED

Interface signatures are **unchanged**. Add annotations and rewrite one method body.

**`createProject`**

```java
@PreAuthorize("isAuthenticated() and #ownerUsername == authentication.name")
```

Pins the owner to the real principal. The controller already passes `Principal.getName()`, so this
looks redundant — it is not: the rule survives any future caller that passes something else, which is
the whole value of putting it on the method. Keep `isAuthenticated()` as the first clause, because an
anonymous request arrives with the name `"anonymousUser"`, which a crafted argument could match.

**`getProjectById`**

```java
@PostAuthorize("hasRole('ADMIN') or returnObject.owner.username == authentication.name")
```

Owner or ADMIN, decided after the fetch (the owner is a property of the row). This reuses the row the
method already loaded — a `@PreAuthorize` + probe would cost one extra query. That trade-off flips for
writes, where "already loaded" means "already changed". [C-§4.3]

**`updateProject` and `deleteProject`**

```java
@PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isProjectOwner(#id, authentication)")
```

Checked **before** the body — the right choice for a mutation.

**`getAllProjects` — no annotation; scope the QUERY instead**

```java
@Override
public PagedResponse<ProjectResponseDto> getAllProjects(Pageable pageable) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Page<Project> page = isAdmin(authentication)
            ? projectRepository.findAll(pageable)
            : projectRepository.findByOwnerUsername(usernameOf(authentication), pageable);
    return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
}

private static final String ROLE_ADMIN = "ROLE_ADMIN";

private static boolean isAdmin(Authentication authentication) {
    return authentication != null && authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(ROLE_ADMIN::equals);
}

/** Empty string when there is no principal, so the scoped query simply matches nothing. */
private static String usernameOf(Authentication authentication) {
    return authentication == null ? "" : authentication.getName();
}
```

**You must be able to explain why this one is programmatic** [C-§5.2]. `@PostFilter` cannot touch
`totalElements` / `totalPages` / `last`, because the database computed them from the *unfiltered*
result set. Post-filtering a page yields a response that contradicts itself — "page 1 of 4, 37 results"
beside two visible rows, and, with `size=20`, a user who owns 3 of 200 projects gets an **empty page 1**
while their projects sit on page 7. It cannot even be attempted here: `@PostFilter` needs a
`Collection`, and this returns a `PagedResponse`. An annotation can allow or deny a call; it cannot
**rewrite the query the call makes** — so the principal is read programmatically, exactly as
`UserServiceImpl.getCurrentUser` already does.

### 7.12 `service/TaskServiceImpl` — CHANGED (annotations only; no body changes)

| Method | Annotation |
| ------ | ---------- |
| `createTask` | `@PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isProjectOwner(#request.projectId, authentication)")` |
| `getTasks` | `@PreAuthorize("hasRole('ADMIN')")` |
| `getTasksByProject` | `@PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isProjectOwner(#projectId, authentication)")` |
| `getTaskById` | `@PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.canWorkOnTask(#id, authentication)")` |
| `updateTask` | `@PreAuthorize("hasRole('ADMIN') or (@taskFlowPermissions.canWorkOnTask(#id, authentication) and @taskFlowPermissions.canPlaceTaskInProject(#id, #request.projectId, authentication))")` |
| `deleteTask` | `@PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isProjectOwnerOfTask(#id, authentication)")` |

The design in one line: **two people have a stake in a task** — the owner of its project and its
assignee — so read/update allow both, while **create and delete are the project owner's alone**. Being
handed work is authority to progress it, not to destroy it.

`getTasks` (the cross-project feed) becomes ADMIN-only. This *could* have been a URL matcher; putting
it on the method means the restriction travels with the operation to any future caller. Ordinary users
are not stranded — they list tasks via `getTasksByProject` on a project they own.

Note the clause ordering in `updateTask`: `hasRole('ADMIN')` is free (it reads authorities already in
memory) while each probe costs a query, so an ADMIN request issues no extra SQL at all.

### 7.13 `service/UserServiceImpl` — CHANGED (annotations + remove a self-invocation)

```java
@PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
public UserResponse getByUsername(String username) { … }

@PreAuthorize("hasRole('ADMIN')")
public List<UserResponse> getAllUsers() { … }

// getCurrentUser: NO annotation — it has no argument to abuse and can only return the caller's row.
```

**`getAllUsers`'s annotation is redundant on purpose.** `/api/admin/**` is already ADMIN-only in
`SecurityConfig`. It is still right: the day someone exposes this service from a path outside
`/api/admin`, the URL rule stops applying and this one still holds. Two independent layers, and the one
attached to the code travels with it.

> ⚠️ **The self-invocation trap — the most important five lines in this lab.** Lab 7's
> `getCurrentUser()` calls `getByUsername(...)`. Once `getByUsername` is annotated, that internal call
> **bypasses the proxy entirely** and the `@PreAuthorize` does **nothing** — no warning, no log line,
> no exception. You met this trap with `@Cacheable` (Lab 16) and `@Async` (Lab 18), where it costs a
> cache hit or a background thread. Here it costs the authorization check, and a method that *looks*
> protected but is not is worse than one that was never annotated, because readers trust it. [C-§2.3]

Fix it by giving both public methods a shared **private, unannotated** helper — honest about having no
rule of its own — instead of routing one through the other:

```java
@Override
public UserResponse getByUsername(String username) {
    return toResponse(load(username));
}

@Override
public UserResponse getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
        throw new ResourceNotFoundException("User", "current");
    }
    return toResponse(load(authentication.getName()));
}

private User load(String username) {
    return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", username));
}
```

Add a comment in `getCurrentUser` saying *why* it does not delegate. In this particular case the check
would have passed anyway (`#username == authentication.name` by construction) — relying on that is
exactly the fragile reasoning to avoid.

### 7.14 `controller/CommentController` — NEW

`@RestController @RequestMapping("/api/comments") @RequiredArgsConstructor`, injecting `CommentService`.
Four endpoints, house style throughout (`ResponseEntity`, `@Valid`, `Location` on create via
`ServletUriComponentsBuilder`):

| Method | Path | Body | Returns |
| ------ | ---- | ---- | ------- |
| `POST` | `/api/comments` | `@Valid CommentRequestDto` + `Principal` | `201` + `Location`, `CommentResponseDto` |
| `GET` | `/api/comments/{id}` | — | `200 CommentResponseDto` |
| `PUT` | `/api/comments/{id}` | `@Valid CommentUpdateRequestDto` | `200 CommentResponseDto` |
| `DELETE` | `/api/comments/{id}` | — | `204` |

Pass `principal.getName()` as `createComment`'s second argument — the author comes from the token, never
from the body. **Declare no authorization rules here.** Every per-row decision lives on the service, so
a denial arrives as an exception and leaves as a 403 ProblemDetail. That is what makes the rules
reusable: `TaskController` exposes the thread listing (§7.15) and is protected by the same annotations
with nothing repeated at the web layer.

### 7.15 `controller/TaskController` — CHANGED

Inject `CommentService` alongside `TaskService` and add:

```java
@GetMapping("/{id}/comments")
public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable Long id) {
    return ResponseEntity.ok(commentService.getCommentsByTask(id));
}
```

A plain `List`, **not** a `PagedResponse` — and that is a requirement, not a preference:
`@PostFilter` can only operate on a `Collection`.

> **Gotcha:** the wider constructor means the existing `TaskControllerTest` no longer starts — see
> §10.3.

### 7.16 `controller/UserController` — CHANGED

```java
@GetMapping("/{username}")
public ResponseEntity<UserResponse> getByUsername(@PathVariable String username) {
    return ResponseEntity.ok(userService.getByUsername(username));
}
```

This exists to give `UserServiceImpl.getByUsername`'s rule a call site reachable from **outside** the
bean. alice → `/api/users/alice` is 200; alice → `/api/users/bob` is 403; ADMIN may read either.

The two mappings do not clash: a literal path segment outranks a template, so `/api/users/me` keeps
hitting `me()` and never binds `username = "me"`. Assert that in a test (§10.2) rather than trusting it.

---

## 8. API Contract & Authorization Matrix

### 8.1 New and changed endpoints

| Method | Path | Auth | Success | Errors |
| ------ | ---- | ---- | ------- | ------ |
| `POST` | `/api/comments` | author must be principal; can work on the task | `201` + `Location` | `400`; `401`; **`403`**; `404` (ADMIN only, unknown task) |
| `GET` | `/api/comments/{id}` | author, project owner, or ADMIN | `200` | `401`; **`403`** (exists, not yours); `404` (unknown id — see §8.4) |
| `PUT` | `/api/comments/{id}` | **author only** | `200` | `400`; `401`; **`403`**; `404` |
| `DELETE` | `/api/comments/{id}` | author or ADMIN | `204` | `401`; **`403`**; `404` |
| `GET` | `/api/tasks/{id}/comments` | project owner or assignee (ADMIN any) | `200` `List<CommentResponseDto>` — filtered | `401`; **`403`**; `404` |
| `GET` | `/api/users/{username}` | self or ADMIN | `200` | `401`; **`403`**; `404` |

Endpoints whose **contract shape is unchanged but whose authorization tightened**:
`GET/PUT/DELETE /api/projects/{id}`, `GET /api/projects`, `GET /api/projects/{id}/tasks`,
`POST/GET/PUT/DELETE /api/tasks`, `GET /api/tasks/{id}`, `GET /api/admin/users`.

### 8.2 The full authorization matrix

Fixture: **alice** owns project *Apollo*; task *T* lives in Apollo and is assigned to **bob**;
**carol** has no relationship to either; **admin** has ROLE_ADMIN.

| Operation | anonymous | alice (project owner) | bob (assignee) | carol (stranger) | admin |
| --------- | :-------: | :-------------------: | :------------: | :--------------: | :---: |
| `POST /api/projects` (self as owner) | 401 | 201 | 201 | 201 | 201 |
| `GET /api/projects` | 401 | own only | own only | own only | all |
| `GET /api/projects/{Apollo}` | 401 | 200 | **403** | **403** | 200 |
| `PUT` / `DELETE /api/projects/{Apollo}` | 401 | 200 / 204 | **403** | **403** | 200 / 204 |
| `GET /api/projects/{Apollo}/tasks` | 401 | 200 | **403** | **403** | 200 |
| `POST /api/tasks` into Apollo | 401 | 201 | **403** | **403** | 201 |
| `GET /api/tasks` (global feed) | 401 | **403** | **403** | **403** | 200 |
| `GET /api/tasks/{T}` | 401 | 200 | 200 | **403** | 200 |
| `PUT /api/tasks/{T}` (same project) | 401 | 200 | 200 | **403** | 200 |
| `PUT /api/tasks/{T}` (move to another project) | 401 | 200 *if she owns the destination* | **403** | **403** | 200 |
| `DELETE /api/tasks/{T}` | 401 | 204 | **403** | **403** | 204 |
| `POST /api/comments` on T | 401 | 201 | 201 | **403** | 201 |
| `GET /api/tasks/{T}/comments` | 401 | 200 — **all** | 200 — **own only** | **403** | 200 — all |
| `GET /api/comments/{bob's}` | 401 | 200 | 200 | **403** | 200 |
| `PUT /api/comments/{bob's}` | 401 | **403** | 200 | **403** | **403** |
| `DELETE /api/comments/{bob's}` | 401 | **403** | 204 | **403** | 204 |
| `GET /api/users/me` | 401 | 200 | 200 | 200 | 200 |
| `GET /api/users/alice` | 401 | 200 | **403** | **403** | 200 |
| `GET /api/admin/users` | 401 | **403** | **403** | **403** | 200 |

The two rows worth staring at: **`GET /api/tasks/{T}/comments`** returns *different content* for alice
and bob from one URL and one method — that is `@PostFilter`. And **`PUT /api/comments/{bob's}`** is the
only cell above where **admin is denied** — the deliberate author-only rule.

(Strictly, ADMIN is also unable to *act as another user*: the create rows are all scoped "self as
owner", and `createProject` / `createComment` pin the owner/author to the principal with no ADMIN
escape. So ADMIN may read and delete anything, but cannot create a project or comment owned by someone
else, and cannot edit someone else's comment.)

### 8.3 Sample bodies

`POST /api/comments` request / `201` response:

```json
// request
{ "taskId": 1, "text": "On it" }
// 201 response
{ "id": 1, "text": "On it", "createdAt": "2026-08-10T16:29:23.840424Z",
  "task": { "id": 1, "title": "Design the lander" },
  "author": { "id": 3, "username": "bob" } }
```

`GET /api/tasks/1/comments` — **as bob (assignee)** vs **as alice (project owner)**, same URL:

```json
// bob: only his own note
[ { "id": 1, "text": "On it", "createdAt": "...", "task": {...}, "author": { "id": 3, "username": "bob" } } ]

// alice: the whole thread
[ { "id": 1, "text": "On it",       "author": { "id": 3, "username": "bob"   }, ... },
  { "id": 2, "text": "Owner note",  "author": { "id": 2, "username": "alice" }, ... } ]
```

A **403** from a method rule (via `GlobalExceptionHandler`) and from a URL rule (via
`RestAccessDeniedHandler`):

```json
// method rule — PUT /api/comments/1 as admin
{ "detail": "You do not have permission to perform this action.",
  "instance": "/api/comments/1", "status": 403, "title": "Access denied" }

// URL rule — GET /api/admin/users as bob
{ "type": "about:blank", "title": "Access denied", "status": 403,
  "detail": "You do not have permission to perform this action." }
```

The meaningful fields match. They are not byte-identical: Spring's `ProblemDetail` serializer adds
`instance` and omits `type` when it is the default `about:blank`.

### 8.4 403 vs 404 — a contract change you must document

Because a permission probe fails closed, **a row that does not exist answers "not yours"**. So on every
`@PreAuthorize`-guarded operation a non-ADMIN caller receives **403, not 404**, for an id that does not
exist:

```
carol → DELETE /api/projects/9999        (no such project)  → 403
bob   → POST   /api/tasks {"projectId": 9999}               → 403
carol → PUT    /api/comments/9999                           → 403
```

This is deliberate and it is the security-correct answer: distinguishing "does not exist" from "not
yours" hands an attacker an oracle — iterate ids and 404-vs-403 maps out exactly which rows exist.
Authorization runs before the lookup and cannot tell them apart.

**The consequence:** an **ADMIN** (who passes `hasRole('ADMIN')` and reaches the body) sees the **404**,
while a non-ADMIN sees **403** for the very same id. That asymmetry is expected, and your tests must
assert it rather than treat it as a bug.

**And now the honest limitation, which you must not paper over.** The two `@PostAuthorize` reads —
`GET /api/projects/{id}` and `GET /api/comments/{id}` — **cannot** behave this way, for a structural
reason. A post-invocation rule needs the body to run first, and the body throws
`ResourceNotFoundException` before the rule is ever evaluated. So:

```
carol → GET /api/projects/9999        (no such project)      → 404   (to EVERY caller)
carol → GET /api/projects/{alice's}   (exists, not hers)      → 403
```

Those two differ, so **GET-by-id remains an existence oracle**. That is the price of the one-query
`@PostAuthorize` optimisation from [C-§4.3]: closing it would mean converting both reads to
`@PreAuthorize` plus a permission probe, at the cost of one extra query per read. This lab keeps
`@PostAuthorize` and accepts the leak *knowingly* — which is a normal engineering trade-off, and very
different from not having noticed. `MethodSecurityTest` pins the behaviour so it cannot drift silently,
and the reference marks it as such.

The general rule to take away: **`@PreAuthorize` can hide existence; `@PostAuthorize` cannot.** If
hiding existence matters more than the extra query, that is your deciding factor between them.

---

## 9. Business Rules

- **`@EnableMethodSecurity` is mandatory.** Without it, every rule in this lab is inert.
- **URL rules stay coarse.** Do not migrate per-row logic into `authorizeHttpRequests`.
- **A comment's `author` is always the authenticated principal**, never a request field; likewise a
  project's `owner`.
- **Projects are private to their owner** — reads included. Their task lists too.
- **Tasks:** project owner *or* assignee may read and update; **only the project owner** may create or
  delete. A task may never be moved into a project the caller does not own, and moving requires
  authority over both source and destination.
- **Comment update is author-only, with no ADMIN override**; comment delete allows ADMIN for moderation.
- **Comment visibility:** author, project owner, ADMIN. A comment's `task`, `author` and `createdAt` are
  immutable; only `text` may change.
- **The global task feed `GET /api/tasks` is ADMIN-only.**
- **A denial is a 403 `ProblemDetail` with a generic detail** — never a 500, and never an explanation
  of why.
- **An unauthorized caller sees 403 even for a non-existent id** on every `@PreAuthorize`-guarded
  operation. The two `@PostAuthorize` reads are the documented exception — they answer 404 for an
  unknown id, because the body runs before the rule (§8.4).
- **Deleting a task deletes its comment thread** (cascade + orphan removal on the new inverse
  collection). Without it the `comments.task_id` foreign key turns a legitimate delete into a 500.
- **No method annotated with an authorization rule may be called from inside its own bean.**
- **`@PostFilter` methods return a mutable collection.**
- **Paginated collections are scoped in the query, never post-filtered.**
- Carried over from Lab 7 and unchanged: BCrypt hashing, `ROLE_` prefix mapping, unique
  username/email → 409, stateless JWT with CSRF disabled, seeded ADMIN.

---

## 10. Testing

Target: **`./mvnw test` fully green — 201 tests** (98 carried forward + 103 new).

### 10.1 Where method-security tests must live — read this before writing any

Method security is enforced by an **AOP proxy**, so a test only exercises it if a Spring container
created that proxy. Two very natural-looking choices prove nothing:

- **A Mockito unit test** (`@InjectMocks`) constructs the service with `new`. No Spring, no proxy, and
  **every annotation is inert**. The danger is not failure — it is that a test like *"carol cannot
  delete bob's comment"* written here would **pass whether the annotation existed or not**.
- **A `@WebMvcTest` slice** replaces the service with a mock of the **interface**; the annotated
  implementation is never instantiated.

So build a **minimal method-security context**: `@EnableMethodSecurity` plus the real service
implementations over `@MockitoBean` repositories. No web server, no database, milliseconds per test.
Full-context `@SpringBootTest` arrives in **Lab 10** and is not needed here. [C-§8]

```java
@SpringJUnitConfig(MethodSecurityTest.Config.class)
@DisplayName("Method security (@PreAuthorize / @PostAuthorize / @PostFilter)")
class MethodSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {

        @Bean
        ProjectService projectService(ProjectRepository projects, UserRepository users) {
            return new ProjectServiceImpl(projects, users);
        }

        @Bean
        TaskService taskService(TaskRepository tasks, ProjectRepository projects, UserRepository users) {
            return new TaskServiceImpl(tasks, projects, users);
        }

        @Bean
        CommentService commentService(CommentRepository comments, TaskRepository tasks, UserRepository users) {
            return new CommentServiceImpl(comments, tasks, users);
        }

        @Bean
        UserService userService(UserRepository users) {
            return new UserServiceImpl(users);
        }

        /** Bean NAME matters: SpEL looks up "@taskFlowPermissions" by name. */
        @Bean
        TaskFlowPermissions taskFlowPermissions(ProjectRepository projects, TaskRepository tasks,
                CommentRepository comments) {
            return new TaskFlowPermissions(projects, tasks, comments);
        }
    }

    @MockitoBean private ProjectRepository projectRepository;
    @MockitoBean private TaskRepository taskRepository;
    @MockitoBean private CommentRepository commentRepository;
    @MockitoBean private UserRepository userRepository;

    @Autowired private ProjectService projectService;      // the INTERFACE — these are JDK proxies
    @Autowired private TaskService taskService;
    @Autowired private CommentService commentService;
    @Autowired private UserService userService;
    …
}
```

**Three things that will cost you an afternoon if you miss them:**

1. **Autowire the interfaces.** `proxyTargetClass` defaults to `false` → JDK dynamic proxies. Asking
   for `ProjectServiceImpl` fails to inject.
2. **Declare `taskFlowPermissions` as an explicit `@Bean` method** so the bean name comes from the
   method name. Registering the class via `@Import` names it after its *fully-qualified class name*,
   and every `@taskFlowPermissions` expression then fails at call time.
3. **No transaction manager is needed.** Without `@EnableTransactionManagement` the `@Transactional`
   annotations are simply ignored — fine, since this test is about authorization.

The assertion shape to use throughout — note the `never()`, which is what proves the rule fired
*before* the body:

```java
@Test
@WithMockUser(username = "carol", roles = "USER")     // authorities: ROLE_USER
@DisplayName("updateProject: denied for a non-owner, and the row is never touched")
void updateProjectAsStrangerDenied() {
    given(projectRepository.existsByIdAndOwnerUsername(PROJECT_ID, "carol")).willReturn(false);

    assertThatThrownBy(() ->
            projectService.updateProject(PROJECT_ID, new ProjectRequestDto("Hijacked", null)))
            .isInstanceOf(AuthorizationDeniedException.class);

    verify(projectRepository, never()).save(any(Project.class));
}
```

### 10.2 Test inventory

**NEW test classes**

| Class | Layer | Tests | What to verify |
| ----- | ----- | :---: | -------------- |
| **`security/MethodSecurityTest`** | minimal `@EnableMethodSecurity` context | **42** | The whole §8.2 matrix, allow **and** deny, per principal — broken out as a checklist in §10.2.1 below. |
| **`security/TaskFlowPermissionsTest`** | plain Mockito, `@Nested` groups | **18** | Four groups. **(a) Delegation (4):** each probe asks the right query. **(b) `canWorkOnTask` (3):** true for the owner (and **short-circuits** before the assignee query — `verify(..., never())`), true for the assignee, false for a stranger. **(c) `canPlaceTaskInProject` (4):** true when unchanged (`verifyNoInteractions(projectRepository)`), true when owning both ends, **false when owning only the destination** (and short-circuits), false when owning only the source. **(d) Fails closed (5):** a null `Authentication`, an `AnonymousAuthenticationToken`, and a null id each return `false` for every probe **with `verifyNoInteractions` on all repositories** — no query, no exception — plus one case each for `canWorkOnTask` and **`canPlaceTaskInProject` without a principal** (the regression warned about in §7.9). **(e) Bean name (2):** see §10.2.2. |
| **`service/CommentServiceImplTest`** | plain Mockito | **9** | Business logic only, and **name the class so nobody mistakes it for a security test** (e.g. `@DisplayName("CommentServiceImpl (business logic; authorization annotations are INERT here)")`). Create sets the author from the principal argument and maps nested summaries; create 404s on an unknown task; thread 404s on an unknown task; **the returned thread list is mutable** (call `.clear()` on it — this documents the `@PostFilter` requirement); update changes only `text` and leaves `task` / `author` / `createdAt`; update and delete 404 on unknown ids; `deleteComment` removes the row (`verify(...).deleteById(...)`); and `getCommentById` maps the row. That is 9 cases — count them as you go, since the total below depends on it. |
| **`controller/CommentControllerTest`** | `@WebMvcTest(CommentController.class)` | **10** | Use the Lab 6/7 slice recipe (§10.4). 401 anonymous; `201` + `Location` with the author from the token; `400` blank text and `400` missing `taskId` (assert `$.errors.text` / `$.errors.taskId`); `GET` 200 incl. `$.createdAt` as an ISO instant; `GET` 404; `PUT` 200; `DELETE` 204 with `verify(...)`. **Plus the 403 wiring:** make the mock `willThrow(new AuthorizationDeniedException("Access Denied"))` on update and delete and assert **403** (not 500), `$.title == "Access denied"`, and the generic `$.detail`. |
| **`repository/CommentRepositoryTest`** | `@DataJpaTest` | **6** | Thread ordered oldest-first (persist out of order so `ORDER BY` is really tested); empty for an uncommented task; `existsByIdAndAuthorUsername` true only for the real author; false for an unknown id; `@PrePersist` stamps `createdAt` when the caller omits it; **deleting a task cascades away its comments** instead of hitting the `task_id` FK (§7.4a). |
| **`repository/ProjectRepositoryTest`** | `@DataJpaTest` | **4** | `findByOwnerUsername` returns only the caller's projects **and `getTotalElements()` counts only those** (seed 2 for alice + 1 for bob and assert `2`, not `3` — the concrete argument against `@PostFilter` on a page); empty page for a user who owns nothing; `existsByIdAndOwnerUsername` true only for the owner; false for an unknown id. |

#### 10.2.1 `MethodSecurityTest` — the 42 cases as a checklist

Work through these in four groups. Every "denied" case asserts `AuthorizationDeniedException`; where a
mutation is involved, also assert the body never ran (`verify(..., never())`).

**Projects (11)**

- [ ] `createProject` as self → allowed
- [ ] `createProject` naming someone else as owner → denied, and `never()).save(...)` / `never()).findByUsername(...)`
- [ ] `createProject` under `@WithAnonymousUser` passing the literal `"anonymousUser"` → denied
- [ ] `getProjectById` as owner → allowed
- [ ] `getProjectById` as a stranger → denied, **and `verify(projectRepository).findById(...)`** — proving `@PostAuthorize` let the body run
- [ ] `getProjectById` as ADMIN → allowed
- [ ] `updateProject` as owner → allowed
- [ ] `updateProject` as a stranger → denied, `never()).save(...)`
- [ ] `deleteProject` with an id that does not exist → **denied (403), not 404**
- [ ] `getProjectById` with an id that does not exist → **`ResourceNotFoundException` (404)**, contrasted in the same test with an existing-but-foreign project giving 403 — pinning the §8.4 limitation
- [ ] `getAllProjects` uses `findByOwnerUsername` for a USER and `findAll` for an ADMIN (assert the other is `never()` called) — 2 tests

**Tasks (10)**

- [ ] `createTask` into a project the caller owns → allowed
- [ ] `createTask` naming a foreign `#request.projectId` → denied, `never()).save(...)`
- [ ] `getTasks` as USER → denied
- [ ] `getTasks` as ADMIN → allowed
- [ ] `getTasksByProject` as the assignee (not the owner) → denied
- [ ] `getTaskById` as the assignee → allowed
- [ ] `getTaskById` as a stranger → denied, `never()).findById(...)`
- [ ] `updateTask` as the assignee, project unchanged → allowed
- [ ] `updateTask` as the assignee **moving it — even into a project they own** → denied, `never()).save(...)`
- [ ] `updateTask` as the project owner moving it between two projects they own → allowed
- [ ] `deleteTask` as the assignee → denied; as the project owner → allowed (2 tests)

**Comments (13)**

- [ ] `createComment` as the assignee → allowed
- [ ] `createComment` on a task the caller has no stake in → denied
- [ ] `createComment` where the author argument ≠ principal → denied
- [ ] **`@PostFilter`: thread as the assignee → only their own row**
- [ ] **`@PostFilter`: thread as the project owner → both rows**
- [ ] `getCommentsByTask` as a stranger → denied, `never()).findByTaskIdOrderByCreatedAtAsc(...)`
- [ ] `getCommentById` as the author → allowed
- [ ] `getCommentById` as a stranger → denied
- [ ] `updateComment` as the author → allowed
- [ ] `updateComment` as **ADMIN → denied**, `never()).save(...)`
- [ ] `deleteComment` as ADMIN → allowed
- [ ] `deleteComment` as a stranger → denied, `never()).deleteById(...)`

**Users (6)**

- [ ] `getByUsername` for your own name → allowed
- [ ] `getByUsername` for someone else → denied, `never()).findByUsername("bob")`
- [ ] `getByUsername` as ADMIN → allowed
- [ ] `getCurrentUser` → always allowed (no annotation)
- [ ] `getAllUsers` as USER → denied; as ADMIN → allowed (2 tests)

#### 10.2.2 Two tests for the bean-name string

`@taskFlowPermissions` is resolved from a **string** at request time, so renaming the class — or letting
`@Component` fall back to its default name — breaks every expression when a user hits an endpoint, not
when you compile. Nothing else in the suite catches it: `MethodSecurityTest` declares its *own*
`taskFlowPermissions` `@Bean`, and the `@WebMvcTest` slices mock the service interfaces. So the loudest
warning in §7.9 is invisible to an otherwise-green build unless you add these two:

- [ ] the `@Component` annotation on `TaskFlowPermissions` has `value() == "taskFlowPermissions"` —
      read it reflectively, so the assertion fails if someone drops the explicit name
- [ ] every bean reference inside every `@PreAuthorize` / `@PostAuthorize` / `@PostFilter` on the four
      service implementations matches that name — scan the annotation strings reflectively for
      `@identifier` tokens and assert they are all equal to it

Check your work by deliberately changing `@Component("taskFlowPermissions")` to bare `@Component`: both
tests must go red. If they stay green, they are not guarding anything.

**CHANGED test classes** — see §10.3.

| Class | Change |
| ----- | ------ |
| **`repository/TaskRepositoryTest`** | **+5 tests** for the new probes. Capture task/project ids in the existing `seed()`. Verify `existsByIdAndProjectOwnerUsername` walks task→project→owner (true for the project owner, false for the mere assignee); `existsByIdAndAssigneeUsername` true only for the assignee **and false for an unassigned task** (no NPE on the null side); `existsByIdAndProjectId` distinguishes a real move; all three false for an unknown id. These `@DataJpaTest` cases are the **only** layer that proves the derived query *names* resolve — a mocked repository would let a typo through, but Spring Data parses names at repository-creation time, so a wrong one breaks context startup. |
| **`service/ProjectServiceImplTest`** | **Must be updated or it fails** — see §10.3. |
| **`controller/TaskControllerTest`** | **Must be updated or it fails** — see §10.3. Then add 3 tests for `GET /api/tasks/{id}/comments`: 401 anonymous; 200 returning a **JSON array** (`jsonPath("$").isArray()`, `$[0].text`, `$[0].author.username`, `$[0].task.id`) — note there is no `content` wrapper, because it is a `List`, not a page; and 200 with an **empty array** when nothing is visible. |
| **`controller/UserControllerTest`** | **+4 tests:** `GET /api/users/{username}` 200 for your own profile; **403** for someone else's (stub the mock to throw `AuthorizationDeniedException` — the real rule is proven in `MethodSecurityTest`); 401 anonymous; and that `GET /api/users/me` still routes to `me()` (`verify(userService, never()).getByUsername("me")`). |
| **`controller/AdminControllerTest`** | **+1 test:** `GET /api/admin/users` as a plain USER asserts the 403 **body** — `$.status == 403`, `$.title == "Access denied"`, and the same generic `$.detail`. This is the only automated proof that `RestAccessDeniedHandler` is wired: every other 403 assertion in the suite goes through `GlobalExceptionHandler` (a mocked service throwing), which is the *other* layer. Without this test §13's "URL-rule denial matches" criterion is hand-checked only. |

### 10.3 The carried-forward tests that BREAK — do not skip this

Two existing test classes **stop compiling or stop passing** because of Lab 8's changes. Both failures
are legitimate consequences, not accidents, and a reference implementation that merely compiles will not
warn you:

**(a) `controller/TaskControllerTest` — context fails to start.** `TaskController` now injects
`CommentService`, so the slice has an unsatisfied dependency. Add one field and one import:

```java
@MockitoBean private CommentService commentService;
```

**(b) `service/ProjectServiceImplTest` — `getAllPaged` fails.** `getAllProjects` no longer calls
`findAll`; it reads the principal from `SecurityContextHolder` and picks a query. And `@WithMockUser`
cannot help you here: this is a plain Mockito test with no Spring TestContext to run the annotation's
listener. Populate the context yourself, and clear it afterwards:

```java
@AfterEach
void clearSecurityContext() {
    SecurityContextHolder.clearContext();
}

private void authenticateAs(String username, String... authorities) {
    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, "n/a",
                    AuthorityUtils.createAuthorityList(authorities)));
}
```

Then replace the single `getAllPaged` test with two — a USER path stubbing
`findByOwnerUsername(eq("alice"), any(PageRequest.class))` and asserting
`verify(projectRepository, never()).findAll(any(PageRequest.class))`, and an ADMIN path doing the
reverse. **Clearing the context in `@AfterEach` is not optional:** `SecurityContextHolder` is a
thread-local, and JUnit reuses threads, so a leaked principal will make an unrelated test pass or fail
mysteriously later.

If your own Lab 7 tamper tests flip the token's *last* character, fix them too (§3.1) — the copied
reference already does the right thing.

### 10.3.1 Imports you will need (main sources)

The lab's three headline annotations live in a package whose name does **not** match the rest of Spring
Security's `…security.authorization.*` naming — a legacy location, and the single most fumbled import
here. One other is an outright trap: `AccessDeniedException` collides with `java.nio.file`, and an IDE
will happily offer you the wrong one.

```java
// the annotations (note: access.prepost, NOT authorization)
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;

// §7.1 SecurityConfig
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

// §7.2 RestAccessDeniedHandler — NOT java.nio.file.AccessDeniedException
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

// §7.3 GlobalExceptionHandler
import org.springframework.security.authorization.AuthorizationDeniedException;

// §7.9 TaskFlowPermissions
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

// §7.11 ProjectServiceImpl's scoped list query
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

// §7.4a Task's inverse collection
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
```

And for §10.3(b)'s `ProjectServiceImplTest` fix:

```java
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
```

### 10.4 The `@WebMvcTest`-with-security recipe (unchanged from Labs 6–7)

For reference, since the new `CommentControllerTest` needs it:

```java
@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
class CommentControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;          // tools.jackson.databind — Jackson 3
    @MockitoBean private CommentService commentService;
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

`@Import(SecurityConfig.class)` now also brings `@EnableMethodSecurity` — harmless in a slice, since
the mocked service interface carries no rules. Spring Boot 4.0 test imports, as established in Lab 7:

```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;        // @DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;       // TestEntityManager
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;           // @WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean;      // @MockitoBean
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
```

---

## 11. Suggested Build Order

Work in this order and you always have a compiling project and a meaningful test run.

1. **Create the module** (§3.1) and confirm the **98-test baseline is green** before touching anything.
2. **`@EnableMethodSecurity`** on `SecurityConfig` (§7.1(a)). Run the tests — still 98 green. You have now
   armed the mechanism with no rules written.
3. **`RestAccessDeniedHandler`** + wire it (§7.2, §7.1(b)), and the **`AuthorizationDeniedException`
   handler** in `GlobalExceptionHandler` (§7.3). Do this *early*: every 403 you produce from here on
   would otherwise surface as a confusing 500.
4. **`Comment` + `CommentRepository`** (§7.4, §7.5) and **the `Task.comments` inverse collection**
   (§7.4a — skip it and a legitimate task delete becomes a 500). Write **`CommentRepositoryTest`**,
   including the cascade case.
   > **Why the new entity comes this early:** `TaskFlowPermissions` (step 6) injects
   > `CommentRepository`, so it cannot compile until this exists.
5. **Repository probes:** the 2 methods on `ProjectRepository` and 3 on `TaskRepository` (§7.6, §7.7).
   Write **`ProjectRepositoryTest`** and the 5 new **`TaskRepositoryTest`** cases and green them — this
   proves the derived names resolve before anything depends on them.
6. **`TaskFlowPermissions`** (§7.9). Write **`TaskFlowPermissionsTest`**, including the fail-closed and
   bean-name groups, and green it.
7. **Project + Task rules** (§7.11, §7.12). Now **fix `ProjectServiceImplTest`** (§10.3(b)) and start
   **`MethodSecurityTest`** with the project and task cases.
8. **The four comment DTOs** (§7.8), then **`CommentService` + `CommentServiceImpl`** (§7.10). Write
   **`CommentServiceImplTest`** (business logic). Add the comment cases — especially the two
   `@PostFilter` ones — to **`MethodSecurityTest`**.
9. **`CommentController`** (§7.14) and **`CommentControllerTest`**, including the two 403-rendering
   tests.
10. **`TaskController`** nested thread (§7.15) → **fix `TaskControllerTest`** (§10.3(a)) and add its 3 new
    cases.
11. **`UserServiceImpl`** annotations and the **self-invocation fix** (§7.13); **`UserController`**
    `/{username}` (§7.16); add the user cases to `MethodSecurityTest` and the 4 new
    `UserControllerTest` cases.
12. **`AdminControllerTest`** — add the URL-rule 403-body case (§10.2), the one test that proves
    `RestAccessDeniedHandler` is actually wired.
13. **`./mvnw test` → 201 green.** Then exercise it by hand (§12) and build your Postman collection.

---

## 12. Manual Testing Flow

Method security is the kind of feature you should *watch* work. `curl` proves the matrix once (§12.1);
a Postman collection turns it into a repeatable suite (§12.2).

Before you start: create the database if you have not (`CREATE DATABASE taskflow_lab;`) and run
`./mvnw spring-boot:run`. The seeded admin is `admin` / `admin12345`.

### 12.1 With `curl`

Register three users and log each in, keeping the `accessToken`s (`$AT` alice, `$BT` bob, `$CT` carol,
`$ADT` admin):

```bash
curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@taskflow.io","password":"password123"}'
# repeat for bob and carol, then:
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password123"}'
```

Then walk the matrix. Add `-H "Authorization: Bearer $TOKEN"` to every call below and use `-i` (or
`-o /dev/null -w '%{http_code}'`) so you can see the status.

1. **alice creates a project** → `201`. Note the **project** id (careful: the response also contains
   `owner.id` — take the *first* `"id"`).
2. **alice creates a task** in it, assigned to bob (`GET /api/users/bob` as admin for bob's id) → `201`.
3. **bob creates a task in alice's project** → **`403`**. He is authenticated and it is still refused:
   this is the `#request.projectId` rule, reading the body.
4. **bob uses a project id that does not exist** → **`403`**, not 404 (§8.4).
5. **carol reads alice's project** (`GET /api/projects/{id}`) → **`403`**; **alice** → `200`. That is
   `@PostAuthorize` on `returnObject`.
6. **bob updates alice's project** → **`403`**. This is the Lab 7 hole, now closed.
7. **bob reads his assigned task** → `200`; **carol** → **`403`**.
8. **bob lists `GET /api/tasks`** → **`403`**; **admin** → `200` (ADMIN-only feed).
9. **bob deletes his assigned task** → **`403`** (assignee may progress, not destroy).
10. **bob and alice each comment on the task** → `201` each; **carol** → **`403`**.
11. **The `@PostFilter` money shot.** `GET /api/tasks/{taskId}/comments` as **bob** → one comment; as
    **alice** → both; as **admin** → both; as **carol** → **`403`**. *Same URL, same code, different
    content per principal.*
12. **admin edits bob's comment** → **`403`** (author-only, no ADMIN override) — and check the body is a
    403 ProblemDetail, not a 500. **admin deletes it** → `204` (moderation).
13. **bob reads `GET /api/users/bob`** → `200`; **`GET /api/users/alice`** → **`403`**; **admin** →
    `200`.
14. **bob hits `GET /api/admin/users`** → **`403`** from the *URL* rule — compare the body with step 12's
    to confirm both layers report the same `title` / `status` / `detail`.

### 12.2 Build a Postman collection (and test the app)

> **A ready-to-import reference collection ships with this module:
> `TaskFlow_MethodSecurity.postman_collection.json`** (Postman → *Import* → drop the file in). It is a
> superset of the table below — 42 requests / 79 assertions across six folders, adding a few extra
> negative cases — and it runs green top-to-bottom in the Collection Runner, repeatably. Use it to check
> your work, but **building your own is the exercise**; the 30 rows below are the required minimum.

A `curl` run proves the matrix once; a collection with assertions proves it **every** time, and this lab
is the one where that pays off most: the interesting behaviour is not "does the endpoint work" but "does
it answer differently for different principals".

**Set up the collection**

1. Create a collection named **`TaskFlow Method Security`**.
2. Add collection **variables**: `baseUrl` = `http://localhost:8080`; empty `aliceToken`, `bobToken`,
   `carolToken`, `adminToken`; empty `projectId`, `taskId`, `bobCommentId`, `aliceCommentId`, `bobUserId`.
3. Each login request gets a **post-response script** that captures its token into the right variable,
   so you never paste a token by hand:
   ```javascript
   pm.test("login 200", () => pm.response.to.have.status(200));
   pm.collectionVariables.set("aliceToken", pm.response.json().accessToken);
   ```
4. Protected requests set **Authorization → type = Bearer Token**, **Token** =
   `{{aliceToken}}` (or whichever principal that request is acting as).

**The requests to build**, in order — each with `pm.test` assertions:

| # | Folder / request | Acts as | Assert |
| - | ---------------- | ------- | ------ |
| 1 | *Setup* → Register alice / bob / carol | — | `201` (accept `409` on a re-run so the collection is repeatable) |
| 2 | *Setup* → Login alice / bob / carol / admin | — | `200`; capture each token |
| 3 | *Setup* → Get bob's id (`GET /api/users/bob`) | admin | `200`; capture `bobUserId` |
| 4 | *Setup* → Create project | alice | `201`; capture `projectId` from `pm.response.json().id` |
| 5 | *Setup* → Create task (assignee `{{bobUserId}}`) | alice | `201`; capture `taskId` |
| 6 | *Projects* → Create task in alice's project | bob | **`403`**, `title == "Access denied"` |
| 7 | *Projects* → Get project | carol | **`403`** |
| 8 | *Projects* → Get project | alice | `200`, `owner.username == "alice"` |
| 9 | *Projects* → Update project | bob | **`403`** (the Lab 7 hole, closed) |
| 10 | *Projects* → List projects | bob | `200`, and **`totalElements`** counts only bob's own |
| 11 | *Tasks* → Get task | bob | `200` (assignee) |
| 12 | *Tasks* → Get task | carol | **`403`** |
| 13 | *Tasks* → List all tasks | bob | **`403`** (ADMIN-only feed) |
| 14 | *Tasks* → List all tasks | admin | `200` |
| 15 | *Tasks* → Delete task | bob | **`403`** (assignee may not delete) |
| 16 | *Comments* → Create comment | bob | `201`; capture `bobCommentId` |
| 17 | *Comments* → Create comment | alice | `201`; capture `aliceCommentId` |
| 18 | *Comments* → Create comment | carol | **`403`** |
| 19 | *Comments* → **Thread as bob** | bob | `200`; `pm.expect(json.length).to.eql(1)` and the only author is `bob` |
| 20 | *Comments* → **Thread as alice** | alice | `200`; `pm.expect(json.length).to.eql(2)` — *same URL as #19* |
| 21 | *Comments* → Thread | carol | **`403`** |
| 22 | *Comments* → Get bob's comment | alice | `200` (project owner) |
| 23 | *Comments* → Get bob's comment | carol | **`403`** |
| 24 | *Comments* → Update bob's comment | bob | `200`, `text` changed |
| 25 | *Comments* → Update bob's comment | admin | **`403`** — the author-only rule |
| 26 | *Comments* → Delete bob's comment | admin | `204` — moderation |
| 27 | *Users* → `GET /api/users/bob` | bob | `200` |
| 28 | *Users* → `GET /api/users/alice` | bob | **`403`** |
| 29 | *Users* → `GET /api/admin/users` | bob | **`403`**; body has the **same** `title`/`status`/`detail` as #25 (URL layer vs method layer) |
| 30 | *Negative* → any protected request with **no** Authorization header | — | **`401`** (not 403) |

Requests 19 and 20 are the centrepiece: identical URL and method, different `length`. Make that a real
assertion, not an eyeball check.

Finally, run the whole collection in the **Collection Runner** top-to-bottom and confirm **every**
`pm.test` passes. Export it as `TaskFlow_MethodSecurity.postman_collection.json` and submit it with your
code.

---

## 13. Acceptance Criteria

Graded checklist. Every box must be tickable.

**Mechanism**

- [ ] `@EnableMethodSecurity` is on `SecurityConfig`, and `authorizeHttpRequests` was **not** used for
      per-row rules.
- [ ] `TaskFlowPermissions` is a `@Component` with an **explicit bean name**, exposing the six probes.
- [ ] Every probe **returns a boolean and never throws**; null id, `null` `Authentication`, and an
      `AnonymousAuthenticationToken` all yield `false` **without querying**.
- [ ] Every probe answers with **one** derived `existsBy…` query and loads no entity.

**Rules**

- [ ] `createProject` pins the owner to the principal (`isAuthenticated() and #ownerUsername == authentication.name`).
- [ ] `getProjectById` uses **`@PostAuthorize`** over `returnObject`; `updateProject`/`deleteProject` use
      **`@PreAuthorize`**; you can explain why the write must not use `@PostAuthorize`.
- [ ] `getAllProjects` is scoped by **query** (`findByOwnerUsername`), not by `@PostFilter`, and you can
      explain what post-filtering a page would break.
- [ ] `createTask` authorizes on **`#request.projectId`** (a value from the request body).
- [ ] `getTasks` is ADMIN-only; `getTaskById`/`updateTask` allow project owner **or** assignee;
      `deleteTask` allows the project owner only.
- [ ] `updateTask` blocks moving a task into a project the caller does not own, and **owning only the
      destination is not sufficient**.
- [ ] `getCommentsByTask` carries **both** `@PreAuthorize` and `@PostFilter`, and returns a **mutable**
      list.
- [ ] `getCommentById` uses `@PostAuthorize` over `returnObject`.
- [ ] `updateComment` is **author-only with no ADMIN override**; `deleteComment` allows author or ADMIN.
- [ ] `getByUsername` uses `#username == authentication.name`; `getAllUsers` is ADMIN-only.
- [ ] **`getCurrentUser` does not call `getByUsername`**, and a comment explains the self-invocation
      trap.

**Errors**

- [ ] A method-security denial returns **403** with `title == "Access denied"` and a **generic** detail —
      **not 500**, and never an explanation of why.
- [ ] A URL-rule denial returns a 403 with the **same** `title` / `status` / `detail` via
      `RestAccessDeniedHandler`.
- [ ] An anonymous request still returns **401**, not 403.
- [ ] A non-ADMIN caller gets **403 for an id that does not exist** on `@PreAuthorize`-guarded
      operations, and you can justify it.
- [ ] You can state the **`@PostAuthorize` exception** — `GET /api/projects/{id}` and
      `GET /api/comments/{id}` answer 404 for an unknown id — and explain why it is structural (§8.4).
- [ ] `DELETE /api/tasks/{id}` on a task **with comments** returns `204`, not 500 (§7.4a).

**Tests**

- [ ] `./mvnw test` is **fully green**, **201 tests**.
- [ ] Rules are proven in a **minimal `@EnableMethodSecurity` context** with `@WithMockUser`, covering
      both allow **and** deny for every rule.
- [ ] Deny tests assert the body did not run (`verify(..., never())`), and the `@PostAuthorize` deny test
      asserts the body **did** run.
- [ ] The `@PostFilter` behaviour is proven twice: assignee sees 1 row, project owner sees 2.
- [ ] `@DataJpaTest` cases cover all five new derived probe queries, plus the unassigned-task case, plus
      `findByOwnerUsername`'s **`totalElements`**.
- [ ] The two carried-forward tests from §10.3 are fixed, and `SecurityContextHolder` is cleared in
      `@AfterEach`.
- [ ] Your Mockito service test is named/documented so nobody mistakes it for a security test.

**Manual**

- [ ] The §12.1 `curl` walk produces every expected status.
- [ ] Your own Postman collection runs green in the Collection Runner, including requests 19/20 asserting
      **different list lengths from the same URL**.

---

## 14. Optional Stretch Goals (not graded)

1. **`@PreFilter`.** Add a bulk endpoint (e.g. `PUT /api/tasks/bulk-status`) and use `@PreFilter` to
    silently drop the tasks the caller may not touch. Then argue whether silently ignoring part of a
    request is better or worse API design than rejecting the whole thing.
2. **A `PermissionEvaluator`.** Replace one `@taskFlowPermissions.xxx(...)` call with a custom
    `PermissionEvaluator` bean and `hasPermission(#id, 'Project', 'write')`. Compare readability and
    decide which you would keep.
3. **`@AuthenticationPrincipal`.** Inject the principal into a controller method instead of `Principal`,
    and consider whether the service should take a username at all.
4. **Meta-annotations.** Collapse the repeated
    `hasRole('ADMIN') or @taskFlowPermissions.isProjectOwner(#id, authentication)` into a custom
    `@IsProjectOwnerOrAdmin` annotation using `@PreAuthorize` as a meta-annotation. Note the catch: a
    meta-annotation with a **template parameter** only resolves if you also declare an
    `AnnotationTemplateExpressionDefaults` bean — without it the placeholder is never substituted and
    the rule silently fails to behave as written.
5. **Audit the denials.** Listen for Spring Security's `AuthorizationDeniedEvent`
    (`org.springframework.security.authorization.event`) and log who was denied what — the beginning of
    a real audit trail (and a natural fit for Lab 19's application events). The catch: nothing is
    published unless you declare an `AuthorizationEventPublisher` bean (e.g.
    `SpringAuthorizationEventPublisher`), so a missing bean looks exactly like a broken listener.
6. **Prove the self-invocation trap empirically.** Temporarily make `getCurrentUser()` call
    `getByUsername(...)`, write a test showing the `@PreAuthorize` never fires, then revert. Keep the
    test's findings in a comment.

---

## 15. Instructor Notes

- **The single highest-value moment** in this lab is §12.1 step 11 / Postman 19–20: one URL, one method,
  two principals, different response bodies. Have students run it before they read the `@PostFilter`
  theory — the annotation makes far more sense once they have seen the effect.
- **Expect the 500-instead-of-403 bug.** Students who skip §7.3 will hit it, and it presents as "my
  endpoint is broken" rather than "my security works". Build order step 3 exists to front-load it; if a
  student reports mysterious 500s, ask to see their `GlobalExceptionHandler` first.
- **Expect the immutable-list bug.** `stream().toList()` is the habit everywhere else in TaskFlow, and
  `@PostFilter` fails on it at runtime only, with an `UnsupportedOperationException` that does not
  obviously point at security. Worth calling out at the start of §7.10.
- **Expect a green-but-meaningless test suite.** The most common deep mistake is asserting denials in a
  Mockito unit test, where annotations are inert. A useful diagnostic: ask the student to delete one
  `@PreAuthorize` and re-run. If the suite stays green, their tests prove nothing — §10.1.
- **The 403-vs-404 discussion** is worth ten minutes of class time. Students often report it as a bug.
  It is the correct answer, and the ADMIN-sees-404 asymmetry that falls out of it is a good exercise in
  reasoning about information leakage.
- **Two known 500s, one fixed and one deliberately not.** Deleting a task with comments is fixed in
  this lab by the cascade in §7.4a — students who skip it will report "delete is broken". Deleting a
  *project* that still has tasks fails identically, and that is **inherited from Lab 7 and left alone**:
  Lab 8 owns only the problem it introduced, and widening the domain model here would blur what the lab
  is about. It is a good candidate for a Lab 9+ cleanup (either cascade `Project → Task → Comment`
  consistently, or return a 409 the way Lab 3's Bookstore did for an author that still had books).
- **Grading shortcut:** the reference implementation is this module. `./mvnw test` → 201 green; the
  §8.2 matrix is the spec, and `MethodSecurityTest` is the fastest place to see whether a submission's
  rules are real.
- **Looking ahead:** the filter-chain-plus-method-security model is complete after this lab, so Phase 3
  ends here. Lab 9 (Docker) containerises TaskFlow; Lab 10 introduces full-context `@SpringBootTest`,
  which is the natural place to re-verify this authorization matrix end-to-end over real HTTP against a
  real database.
