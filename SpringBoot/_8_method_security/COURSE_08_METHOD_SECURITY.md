# Course 08 — Method Security & Fine-Grained Authorization: @PreAuthorize, @PostFilter, and Row-Level Rules

**Prerequisites:** Lab 6 (the filter chain, `UserDetailsService`, `authorizeHttpRequests`) and Lab 7
(the TaskFlow domain, stateless JWT, the `Authentication` in the `SecurityContext`). This course
assumes you can already read a `SecurityFilterChain` bean and know how a principal gets into the
`SecurityContextHolder`. Nothing here re-teaches that.

**Stack:** Spring Boot 4.0.6 · Spring Security 7.0.5 · Java 21. Every version-specific claim below was
checked against those exact jars.

---

## What you'll learn

Lab 6 and Lab 7 gave you authorization that works on **URLs**: "`POST /api/projects` requires
authentication", "`/api/admin/**` requires ADMIN". That is genuinely useful and it is also
structurally incapable of expressing the most common rule in any real application:

> *You may edit **your own** project. Not anyone else's.*

This course is about closing that gap. By the end you will be able to:

1. Explain precisely **why** a URL rule cannot express row ownership, and why method security is a
   second layer rather than a replacement.
2. Turn on annotation-driven authorization with **`@EnableMethodSecurity`**, and explain the AOP
   proxy that enforces it — including the **self-invocation trap**, which here is not an
   inefficiency but a silently unenforced security rule.
3. Write **`@PreAuthorize`** rules in SpEL over roles, the principal, method arguments (`#id`), and
   properties of arguments (`#request.projectId`).
4. Write **`@PostAuthorize`** rules over `returnObject`, and know why it belongs on reads only.
5. Trim collections with **`@PostFilter`** — and avoid its two runtime traps (the collection must be
   mutable; a paginated response cannot be post-filtered).
6. Implement **row-level ownership** two ways: argument comparison when no lookup is needed, and a
   **permission bean called from SpEL by bean name** when the answer requires a query — following
   the design rules that keep such a bean fail-closed.
7. Render a denial correctly: **`AuthorizationDeniedException` → 403 ProblemDetail**, and know that
   without an explicit handler your global advice turns every denial into a **500**.
8. Put method-security tests in the **only layer where they actually run** — and recognise the two
   layers where "denied" tests pass for the wrong reason.

---

## 1. The gap URL rules cannot close

### 1.1 The Lab 7 hole, concretely

TaskFlow's Lab 7 `Project` entity records an owner, and the service sets it to the authenticated
principal on create:

```java
// Lab 7 — ProjectServiceImpl
Project project = Project.builder()
        .name(request.getName())
        .description(request.getDescription())
        .owner(owner)          // ← the authenticated principal
        .build();
```

And the Lab 7 filter chain protects the endpoint:

```java
.anyRequest().authenticated()
```

So: writes require a token. Good. Now watch what that actually permits. alice owns project 42. bob
logs in, gets a perfectly valid token, and sends:

```
PUT /api/projects/42
Authorization: Bearer <bob's valid token>

{ "name": "Mine now" }
```

Lab 7 answers **200 OK**. bob is authenticated, and `authenticated()` is the whole rule. The Lab 7
source even says so out loud:

> *"Per-row ownership enforcement — restricting update/delete to the owner — is deliberately deferred
> to Lab 8 (method security); in this lab any authenticated user may mutate any project."*

That is the hole this lab closes.

### 1.2 What a request matcher can and cannot see

Ask why you cannot simply write a better matcher, and the answer becomes structural. A
`requestMatchers(...)` rule is evaluated by the `AuthorizationFilter` before any of your code runs.
At that moment it has:

| Available to a URL rule | Not available to a URL rule |
| ----------------------- | --------------------------- |
| the HTTP method (`PUT`) | the **row** being modified |
| the path (`/api/projects/42`) | who **owns** that row |
| path variables, as raw strings | the **request body** |
| the `Authentication` (name, authorities) | anything requiring a **database query** |

`PUT /api/projects/42` is byte-for-byte identical whether you own project 42 or not. The information
needed to decide simply is not present yet. It becomes available exactly one layer down — in the
service method that receives the id.

That is the whole argument. **The rule has to live where the data is.**

### 1.3 Two layers, not two choices

A common misreading is that method security *replaces* URL rules. It does not, and TaskFlow keeps
both deliberately:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated())
```

The two layers answer different questions, in this order:

1. **Filter chain (coarse, cheap, early):** *may this request reach a controller at all?* Rejects
   anonymous traffic before a single line of business code, a single database connection, or a single
   deserialised DTO.
2. **Method security (fine, per-row, late):** *may **this principal** run **this method** on **this
   row**?*

Keeping the first layer coarse is a feature. It stays readable, it fails fast, and it means the
per-row rules are all in one place — next to the code they protect — instead of half-expressed in a
matcher list and half in a service.

There is a second reason to prefer the method over the URL even for a rule you *could* express as a
matcher. A URL rule protects **one path**. A method annotation protects **the operation**, whoever
calls it — a second controller, a scheduled job, a future admin console. TaskFlow uses this for
`getTasks`, the cross-project feed:

```java
@PreAuthorize("hasRole('ADMIN')")
public PagedResponse<TaskResponseDto> getTasks(Status status, Long assigneeId, Pageable pageable) { … }
```

This could have been `requestMatchers(GET, "/api/tasks").hasRole("ADMIN")`. Putting it on the method
means the restriction travels with the code.

---

## 2. Turning it on: `@EnableMethodSecurity`

### 2.1 One annotation, no new dependency

```java
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity        // ← Lab 8
public class SecurityConfig { … }
```

The four annotations you will write live in a package whose name does **not** match the rest of
the lab's `org.springframework.security.authorization.*` naming — a legacy location, and the one import
students most often fumble:

```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.security.access.prepost.PostFilter;
```

That is the entire setup. No Maven dependency is required: method security ships in
`spring-security-core` / `spring-security-config`, which the security starter already brought in at
Lab 6, and the AOP machinery it needs comes from `spring-aop` (a transitive dependency of
`spring-context`). You do **not** need `spring-boot-starter-aop` / AspectJ.

**Nothing is on by default.** Before you add this annotation, every `@PreAuthorize` in your codebase
is an inert comment — the code compiles, the tests pass, and no rule is enforced. This is the single
most common way a method-security lab silently does nothing.

Verified defaults of `@EnableMethodSecurity` in Spring Security 7.0.5:

| Attribute | Default | Meaning |
| --------- | ------- | ------- |
| `prePostEnabled` | **`true`** | `@PreAuthorize` / `@PostAuthorize` / `@PreFilter` / `@PostFilter` are active |
| `securedEnabled` | `false` | the legacy `@Secured` annotation is off |
| `jsr250Enabled` | `false` | `@RolesAllowed` / `@PermitAll` are off |
| `proxyTargetClass` | `false` | JDK dynamic proxies where an interface exists (CGLIB otherwise) |
| `mode` | `PROXY` | proxy-based, not AspectJ weaving |

Because `prePostEnabled` already defaults to `true`, the bare `@EnableMethodSecurity` is all you
write. That default is the practical difference from the old annotation:

```java
// Spring Security 5 style — still present in 7.0.5 but @Deprecated. Do not use.
@EnableGlobalMethodSecurity(prePostEnabled = true)   // its prePostEnabled defaults to FALSE
```

Note the honest version story here, because it differs from what you saw in Lab 6:
`WebSecurityConfigurerAdapter`, `antMatchers` and `authorizeRequests` were genuinely **removed**, so
old code will not compile. `@EnableGlobalMethodSecurity` is **deprecated but still present** in
7.0.5 — old tutorials using it will compile and run. That makes it more dangerous, not less: nothing
forces you to notice. Use `@EnableMethodSecurity`.

### 2.2 The mechanism: an AOP proxy

`@EnableMethodSecurity` registers AOP advisors. For every bean with one of these annotations, Spring
substitutes a **proxy** into the context. Callers hold the proxy; the proxy evaluates the rule, and
only then forwards to your real object:

```
controller ──▶ [ proxy: evaluate @PreAuthorize ] ──▶ ProjectServiceImpl.updateProject(...)
                        │
                        └── denied ──▶ throw AuthorizationDeniedException
```

Two consequences follow directly from "it is a proxy", and both matter.

### 2.3 Consequence 1: the self-invocation trap

A call a bean makes **to itself** never leaves the real object, so it never passes through the proxy.
The annotation on the target method does **nothing** — no warning, no log line, no exception:

```java
@Service
public class UserServiceImpl implements UserService {

    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public UserResponse getByUsername(String username) { … }

    public UserResponse getCurrentUser() {
        // ❌ WRONG: an internal call. The @PreAuthorize above is BYPASSED entirely.
        return getByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
```

You met this proxy trap before — or will: with `@Cacheable` in Lab 16 it costs you a cache hit, with
`@Async` in Lab 18 it silently runs your task on the request thread. Here it costs you the
authorization check. A method that *looks* protected and is not is strictly worse than one that was
never annotated, because a reader will trust it.

The fix is to not route an authorization-checked method through an internal call. TaskFlow gives both
public methods a shared private helper, which is honest about having no rule of its own:

```java
@Override
@PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
public UserResponse getByUsername(String username) {
    return toResponse(load(username));       // ✅
}

@Override
public UserResponse getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
        throw new ResourceNotFoundException("User", "current");
    }
    return toResponse(load(authentication.getName()));   // ✅ no proxy crossing needed
}

/** Unannotated private helper — shared by both, with no rule of its own. */
private User load(String username) {
    return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", username));
}
```

**Rule of thumb:** a method whose annotation matters must be reached from *outside* its bean — a
controller, another bean, or (rarely, and it reads badly) a self-injected proxy reference.

### 2.4 Consequence 2: annotate the implementation, and pick one place

Spring Security finds the annotations on the **target class**, so putting them on the `@Service`
implementation always works, with either proxy type. TaskFlow does that: the rule sits next to the
code it guards, where a reader modifying the method cannot miss it.

You *may* annotate the interface instead. What you must not do is annotate **both** — and the reason
is worth knowing exactly, because the mechanism changed and most of what is written about it is now
out of date.

In Spring Security 7, `@PreAuthorize` is resolved by `UniqueSecurityAnnotationScanner`, which walks
outward from the invoked method and returns the **closest** declaration it finds, then stops. So with
an annotation on both the interface and the implementation, the **implementation silently wins** and
the interface's annotation is never even read. Nothing fails, nothing warns:

```java
public interface Dup {
    @PreAuthorize("hasRole('ADMIN')")   // never consulted
    String go();
}

public class DupImpl implements Dup {
    @Override
    @PreAuthorize("hasRole('USER')")    // this is the rule that runs
    public String go() { return "ran"; }
}
// A caller with only ROLE_USER is ALLOWED. The context starts fine.
```

That is **worse than an error**, and it is the real reason to pick one place: a stale or stricter
annotation left on an interface looks like it is protecting the method and is doing nothing. Spring
Security only raises `AnnotationConfigurationException` ("competing annotations") when two
declarations sit at the **same** distance — two sibling interfaces annotating the same method, say —
not for the interface-versus-implementation case.

> **On older sources:** up to Spring Security 6.3 this was handled by `AuthorizationAnnotationUtils`
> with a `TYPE_HIERARCHY` search, which *did* reject interface+implementation duplicates. That class
> no longer exists in 7.x. If you read that duplicates are "rejected", the source predates 7.0.

The `proxyTargetClass = false` default has one further practical consequence you will hit in tests: a
bean that implements an interface is proxied with a **JDK dynamic proxy**, so it can only be injected
as the **interface** type. Autowiring `ProjectServiceImpl` fails; autowire `ProjectService`.

---

## 3. `@PreAuthorize`: gating entry

`@PreAuthorize` evaluates a SpEL expression **before** the method body. If it is false, the body never
runs and `AuthorizationDeniedException` is thrown.

### 3.1 The vocabulary available in the expression

| Expression | Meaning |
| ---------- | ------- |
| `hasRole('ADMIN')` | the principal has authority `ROLE_ADMIN` (the prefix is added for you) |
| `hasAuthority('ROLE_ADMIN')` | exact authority match, no prefix magic |
| `hasAnyRole('USER','ADMIN')` | any of these roles |
| `isAuthenticated()` | a real, non-anonymous principal |
| `permitAll()` / `denyAll()` | constants |
| `authentication` | the `Authentication` object itself (`authentication.name`, `.authorities`) |
| `principal` | the principal (a `UserDetails`, or a `String` username) |
| `#paramName` | a method argument by name |
| `#param.property` | a property *of* an argument |
| `@beanName.method(...)` | call a Spring bean, looked up **by bean name** |

The `hasRole` / `hasAuthority` distinction is the same `ROLE_` convention from Lab 6:
`hasRole('ADMIN')` matches the authority `ROLE_ADMIN`. TaskFlow's `UserDetailsServiceImpl` maps
`Role.ADMIN` → `"ROLE_ADMIN"`, so `hasRole('ADMIN')` is the form to write.

### 3.2 Comparing an argument to the principal

The simplest useful ownership rule needs no database at all — just compare an argument to the
principal's name:

```java
@PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
public UserResponse getByUsername(String username) { … }
```

alice calling `GET /api/users/alice` → 200. alice calling `GET /api/users/bob` → 403. An ADMIN may
read either.

> **Build requirement for `#username` to resolve.** Parameter names must survive compilation, i.e.
> javac's `-parameters` flag. `spring-boot-starter-parent` enables it, so TaskFlow gets it for free —
> but if you ever see an expression fail to bind a name, that is the cause. The positional form
> `#p0` / `#a0` always works and always reads worse.

### 3.3 Reading *into* an argument: the rule a URL can never express

This is the sharpest illustration of §1.2. `POST /api/tasks` is one path serving every project — the
target project arrives in the **body**:

```java
@PreAuthorize("hasRole('ADMIN') "
        + "or @taskFlowPermissions.isProjectOwner(#request.projectId, authentication)")
public TaskResponseDto createTask(TaskRequestDto request) { … }
```

`#request.projectId` navigates into the DTO. A request matcher has nothing to match on here: the path
is the same for every caller and every project.

### 3.4 Combining conditions

SpEL is a real expression language, so multi-part rules are just boolean logic — and it
**short-circuits left to right**, which you should exploit to put the cheap checks first:

```java
@PreAuthorize("hasRole('ADMIN') or (@taskFlowPermissions.canWorkOnTask(#id, authentication) "
        + "and @taskFlowPermissions.canPlaceTaskInProject(#id, #request.projectId, authentication))")
public TaskResponseDto updateTask(Long id, TaskRequestDto request) { … }
```

`hasRole('ADMIN')` is free (it reads the authorities already in memory); the permission-bean calls
each cost a query. Ordering them this way means an ADMIN request issues no extra SQL at all.

Keep expressions short. When a rule needs more than a clause or two, that is the signal to move the
logic into a permission bean (§6.2) and leave one readable call in the annotation.

---

## 4. `@PostAuthorize`: deciding from the returned row

Some rules cannot be answered before the method runs, because the deciding fact *is* the row. "May
you read project 42?" depends on who owns project 42 — and the only way to learn that is to fetch it.

`@PostAuthorize` runs **after** the body and exposes the result as `returnObject`:

```java
@PostAuthorize("hasRole('ADMIN') or returnObject.owner.username == authentication.name")
public ProjectResponseDto getProjectById(Long id) {
    return toResponse(getEntity(id));
}
```

On denial the loaded value is discarded and `AuthorizationDeniedException` is thrown; the caller never
receives it.

### 4.1 `returnObject` can only reach what the DTO exposes

The expression navigates the **returned object**, which in a layered app is a DTO, not an entity. So
`ProjectResponseDto` must actually carry `owner.username` — and it does, which is not a coincidence:

```java
public class ProjectResponseDto {
    private Long id;
    private String name;
    private String description;
    private UserSummaryDto owner;    // ← what the @PostAuthorize rule reads
}
```

If a post-invocation rule needs a field, that field has to be on the response DTO. This occasionally
pulls data into a DTO purely for authorization — a legitimate reason, worth a comment.

### 4.2 The rule that follows: reads only

**Never guard a write with `@PostAuthorize` and assume the write is undone.** By the time the
expression is evaluated, the method body has already executed. TaskFlow's tests assert this plainly:

```java
@Test
@WithMockUser(username = "carol", roles = "USER")
void getProjectByIdAsStrangerDenied() {
    given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(aliceProject()));

    assertThatThrownBy(() -> projectService.getProjectById(PROJECT_ID))
            .isInstanceOf(AuthorizationDeniedException.class);

    // The body DID run and the row WAS fetched — it just never reached the caller.
    verify(projectRepository).findById(PROJECT_ID);
}
```

For a read, "wasted a query" is the entire cost. For an update, the row would already be changed.
Whether the surrounding transaction rolls back depends on interceptor ordering — which is exactly the
kind of thing you do not want a security guarantee resting on. **Writes get `@PreAuthorize`.**

### 4.3 `@PostAuthorize` vs. one extra query

`getProjectById` could have been written as `@PreAuthorize` with a permission-bean probe. The
trade-off:

| | Queries | Notes |
| --- | --- | --- |
| `@PostAuthorize` | 1 (the fetch) | reuses the row the method already loaded; **reads only** |
| `@PreAuthorize` + probe | 2 (probe + fetch) | decides before anything runs; safe for writes |

For reads, `@PostAuthorize` is one query cheaper. That is why TaskFlow uses `@PostAuthorize` on
`getProjectById` and `@PreAuthorize` on `updateProject` / `deleteProject`.

---

## 5. `@PostFilter`: trimming collections

`@PostAuthorize` makes an all-or-nothing decision about one value. Returning a **list** is a different
problem: you rarely want to reject the whole call, you want to drop the rows this principal may not
see. That is `@PostFilter`, and `filterObject` is the current element:

```java
@PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.canWorkOnTask(#taskId, authentication)")
@PostFilter("hasRole('ADMIN') or filterObject.author.username == authentication.name "
        + "or @taskFlowPermissions.isProjectOwnerOfTask(#taskId, authentication)")
public List<CommentResponseDto> getCommentsByTask(Long taskId) { … }
```

Two annotations, two jobs, on one method — and the pairing is the point:

- **`@PreAuthorize`** answers *may you open this thread at all?* If not: 403, nothing runs.
- **`@PostFilter`** answers *which rows may you see?* and silently drops the rest. An entitled caller
  with nothing visible gets **200 and an empty list**, not an error.

Note that the filter expression can still read **method arguments** (`#taskId`), not only
`filterObject`. Here that is what makes the project-owner check possible.

Here is the effect, from TaskFlow's verified end-to-end run — one URL, one method, two principals:

```
GET /api/tasks/1/comments   as bob   (assignee)      → 200  [ {"text":"On it"} ]
GET /api/tasks/1/comments   as alice (project owner) → 200  [ {"text":"On it"}, {"text":"Owner note"} ]
```

### 5.1 Trap 1 — the collection must be **mutable**

Spring Security does not build you a new list. `DefaultMethodSecurityExpressionHandler` filters by
mutating **the very collection you returned** — the disassembled bytecode calls
`Collection.clear()` and then `Collection.addAll(retained)`.

So an immutable list breaks it at **runtime**, with `UnsupportedOperationException` — not at compile
time, and not in any test that does not exercise the filter:

```java
// ❌ UnsupportedOperationException when @PostFilter tries to clear() it
return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
        .map(this::toResponse)
        .toList();                      // Stream.toList() is IMMUTABLE

// ✅ mutable
return new ArrayList<>(commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
        .map(this::toResponse)
        .toList());
```

This is worth dwelling on because it cuts against your habits: `List.of(...)` and `stream().toList()`
are the modern, preferred idioms, and every *other* list-mapping method in TaskFlow uses `.toList()`.
The one method with `@PostFilter` must not.

`@PostFilter` supports a `Collection`, an array, a `Map` and a `Stream`. Everything else — including
your own wrapper types — it cannot touch.

### 5.2 Trap 2 — you cannot post-filter a page

This one is a **design** trap, and it is the more important of the two.

`@PostFilter` can only remove elements from a collection. It cannot touch `totalElements`,
`totalPages` or `last`, because the database computed those from the *unfiltered* result set. Filter a
page after the fact and the response contradicts itself:

- "page 1 of 4, 37 results" next to two visible rows;
- page sizes that vary unpredictably as rows are dropped;
- and the memorable failure: with `size=20`, a user who owns 3 of 200 projects gets an **empty
  page 1** while their projects sit on page 7.

In TaskFlow it cannot even be attempted: `getAllProjects` returns a `PagedResponse`, not a
`Collection`. And note that `@PostFilter` does not *quietly ignore* an unsupported return type —
`DefaultMethodSecurityExpressionHandler.filter` checks for a `Collection`, array, `Map` or `Stream` and
otherwise throws `IllegalArgumentException`, so you would find out at runtime.

**The fix is to never fetch what the caller may not see.** Push the predicate into the WHERE clause:

```java
// ProjectRepository
@EntityGraph(attributePaths = "owner")
Page<Project> findByOwnerUsername(String username, Pageable pageable);
```

```java
// ProjectServiceImpl — the choice of QUERY depends on the caller, which no annotation can express
@Override
public PagedResponse<ProjectResponseDto> getAllProjects(Pageable pageable) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Page<Project> page = isAdmin(authentication)
            ? projectRepository.findAll(pageable)
            : projectRepository.findByOwnerUsername(usernameOf(authentication), pageable);
    return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
}
```

Note *why* this one reads `SecurityContextHolder` programmatically instead of using an annotation: an
annotation can allow or deny a call, but it cannot **rewrite the query the call makes**. That is a
real boundary of declarative security, and recognising it is part of the lesson.

Now the page metadata is correct by construction — which is precisely what the repository test
asserts:

```java
Page<Project> page = projectRepository.findByOwnerUsername("alice", PageRequest.of(0, 20, Sort.by("name")));

assertThat(page.getContent()).extracting(Project::getName).containsExactly("Apollo", "Artemis");
// 2, not 3: the third project belongs to bob and was never fetched or counted.
assertThat(page.getTotalElements()).isEqualTo(2);
```

**Decision rule:** short, unpaginated list (a comment thread) → `@PostFilter` is fine and pleasantly
declarative. Paginated collection → filter in the query, always.

### 5.3 The cost: once per element

The `@PostFilter` expression is evaluated **per element**. A query-backed probe inside it therefore
runs N times. Two mitigations, both used above — and be precise about what the first one actually buys:

1. **Order the clauses cheapest-first.** `or` short-circuits, so `hasRole('ADMIN')` and the free
   `filterObject.author.username` comparison come before `isProjectOwnerOfTask(...)`. This only helps
   for elements the free clauses *match*: a project owner reading a thread fails the author comparison
   on every comment written by someone else, so the probe still fires once per such comment. Ordering
   reduces the constant; it does not bound the count.
2. **Notice when the answer is constant.** `isProjectOwnerOfTask(#taskId, …)` keys on a **method**
   argument, not on `filterObject`, so it returns the same answer for every element — the identical
   query, repeated. That is the real smell: a value invariant across the filter wants to be computed
   *once* before it, or pushed into the WHERE clause as §5.2 does for pages.

### 5.4 `@PreFilter`, briefly

The mirror image: `@PreFilter` trims an incoming **collection argument** before the method runs, so a
bulk operation silently skips the elements the caller may not act on.

```java
// Note: a made-up DTO. TaskFlow's own ProjectRequestDto carries only name + description — it has no
// owner field, because the owner is server-controlled and never sent by a client.
@PreFilter("filterObject.owner == authentication.name")
public void bulkUpdate(List<BulkProjectUpdate> updates) { … }
```

It has the same mutability requirement, and it is much rarer — mostly because silently ignoring part
of a request is usually worse API design than rejecting the whole thing. TaskFlow does not use it; you
should recognise it and reach for it deliberately.

---

## 6. Ownership and row-level rules

### 6.1 Form 1 — compare an argument to the principal

Covered in §3.2. Use it whenever the rule needs **no lookup**: it is free, obvious, and unbreakable.

### 6.2 Form 2 — a permission bean called from SpEL

Once the answer requires a query, SpEL is the wrong place to write it. Delegate to a bean and keep the
expression one readable clause:

```java
@PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isProjectOwner(#id, authentication)")
public ProjectResponseDto updateProject(Long id, ProjectRequestDto request) { … }
```

`@taskFlowPermissions` is a **bean-name reference**: SpEL looks the bean up in the application context
by name. Hence the explicit name on the component:

```java
@Component("taskFlowPermissions")
@RequiredArgsConstructor
public class TaskFlowPermissions {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;

    public boolean isProjectOwner(Long projectId, Authentication authentication) {
        String username = usernameOf(authentication);
        if (projectId == null || username == null) {
            return false;
        }
        return projectRepository.existsByIdAndOwnerUsername(projectId, username);
    }
    …
}
```

Naming the bean explicitly matters more than it looks: the reference is a **string**, resolved at
request time. Rename the class and rely on the default name, and every expression referencing it
breaks — and it breaks when a user hits the endpoint, not when you compile. (This bites in tests too;
see §8.3.)

### 6.3 The five design rules for a permission bean

Every method in `TaskFlowPermissions` obeys all five. They are not style preferences.

1. **Return a boolean; never throw.** A probe answers yes/no. A missing row is "no", not an error.
2. **Fail closed.** A null id, an anonymous caller, or a row that does not exist → `false`. A check
   that returns `true` when it cannot tell is a vulnerability; one that throws an NPE turns a clean
   403 into a 500 and leaks a stack trace.
3. **One boolean query; do not load entities.** These run on the hot path of every protected call, and
   `@PostFilter` may invoke one *per element*. Derived `existsBy…` queries compile to a single
   `EXISTS`-style statement:
   ```java
   boolean existsByIdAndOwnerUsername(Long id, String username);            // ProjectRepository
   boolean existsByIdAndProjectOwnerUsername(Long id, String username);     // TaskRepository — walks task→project→owner
   boolean existsByIdAndAssigneeUsername(Long id, String username);         // TaskRepository
   boolean existsByIdAndAuthorUsername(Long id, String username);           // CommentRepository
   ```
4. **Compose in Java, not in SpEL.** When two probes always go together, give the combination a name
   so the annotation stays short:
   ```java
   /** The two people entitled to see a task and move it along. */
   public boolean canWorkOnTask(Long taskId, Authentication authentication) {
       return isProjectOwnerOfTask(taskId, authentication) || isTaskAssignee(taskId, authentication);
   }
   ```
5. **Treat anonymous as "no principal".** An unauthenticated request does not arrive with a `null`
   `Authentication` — it arrives with an `AnonymousAuthenticationToken` whose `getName()` is the
   literal string `"anonymousUser"`. Comparing that against a database column is harmless, but being
   explicit keeps every probe uniformly fail-closed:
   ```java
   private static String usernameOf(Authentication authentication) {
       if (authentication == null
               || !authentication.isAuthenticated()
               || authentication instanceof AnonymousAuthenticationToken) {
           return null;
       }
       return authentication.getName();
   }
   ```

The same `"anonymousUser"` subtlety explains a clause that otherwise looks redundant:

```java
@PreAuthorize("isAuthenticated() and #ownerUsername == authentication.name")
public ProjectResponseDto createProject(ProjectRequestDto request, String ownerUsername) { … }
```

Without `isAuthenticated()`, an anonymous caller passing the literal string `"anonymousUser"` would
satisfy the equality.

### 6.4 403 or 404? Do not leak existence

Follow rule 2 to its conclusion. Because a non-existent row makes the probe answer "no", an
unauthorized caller gets **403 even for an id that does not exist**:

```
carol → DELETE /api/projects/9999   (no such project)   → 403, not 404
bob   → POST   /api/tasks {"projectId": 9999}           → 403, not 404
```

This is deliberate, and it is the security-correct answer. Distinguishing "does not exist" from "not
yours" hands an attacker an oracle: iterate ids, and 404-vs-403 maps out exactly which rows exist.
Authorization runs before the lookup and *cannot* tell the two apart — and should not.

The consequence, which must be documented in your API contract: a **non-ADMIN** caller sees 403 where
an ADMIN (who passes `hasRole('ADMIN')` and reaches the body) sees 404. That asymmetry is expected.

### 6.4.1 The limitation: `@PostAuthorize` cannot hide existence

Everything above applies to `@PreAuthorize`. A **post**-invocation rule cannot deliver it, and the
reason is structural: the body must run first, and the body throws `ResourceNotFoundException` before
the rule is ever evaluated. So TaskFlow's two `@PostAuthorize` reads behave differently:

```
carol → GET /api/projects/9999          (no such project)   → 404   (to EVERY caller)
carol → GET /api/projects/{alice's}     (exists, not hers)  → 403
```

Those two differ, so **GET-by-id is still an existence oracle** — exactly the leak this section warns
about, surviving in the one place the technique cannot reach. Closing it means converting those reads
to `@PreAuthorize` plus a probe, paying one extra query per read (§4.3's trade-off, inverted).

This course keeps `@PostAuthorize` and accepts the leak **knowingly**, which is an ordinary engineering
decision — and completely different from not having noticed. Say which one you are doing.

**The rule to remember: `@PreAuthorize` can hide existence; `@PostAuthorize` cannot.** When hiding
existence matters more than saving a query, that is your deciding factor between them.

### 6.5 A rule that must read the request body

Ownership is not only about the row you name — sometimes it is about where you are *moving* it.
`TaskRequestDto` carries `projectId`, and `updateTask` legitimately allows the **assignee**. Put those
together and there is a hole: bob, assigned a task in alice's project, sends a PUT naming a project of
his own, and walks the task out of alice's project.

Same path, same verb, same principal — **only the body differs**. No URL rule can see it.

```java
public boolean canPlaceTaskInProject(Long taskId, Long projectId, Authentication authentication) {
    if (taskId == null || projectId == null) {
        return false;
    }
    // Unchanged project → nothing is being moved.
    if (taskRepository.existsByIdAndProjectId(taskId, projectId)) {
        return true;
    }
    // A real move: authority over the source AND the destination.
    return isProjectOwnerOfTask(taskId, authentication)
            && isProjectOwner(projectId, authentication);
}
```

Note the final clause carefully: owning the **destination** is not sufficient. That is precisely the
theft being prevented. A move restructures two projects, so it takes authority over both.

---

## 7. Rendering the denial

### 7.1 A method denial really does reach your `@RestControllerAdvice`

Lab 7's `GlobalExceptionHandler` javadoc notes that the filter chain's 401/403 responses are produced
*before* a request reaches a controller, so they never hit the advice. **Method security is
different.** It throws from *inside* the dispatch:

`org.springframework.security.authorization.AuthorizationDeniedException` — a subclass of
`org.springframework.security.access.AccessDeniedException`, itself a `RuntimeException`.

### 7.2 Without a handler, every denial is a 500

Read that last word again. `AuthorizationDeniedException` is a `RuntimeException`, and TaskFlow's
global handler ends with a catch-all:

```java
@ExceptionHandler(Exception.class)
public ProblemDetail handleUnexpected(Exception ex) { … 500 … }
```

So with no dedicated entry, **every legitimate 403 is reported as a 500** — the endpoint looks broken
rather than protected, and the client cannot distinguish "you may not" from "we crashed". Spring
selects the most specific handler, so adding one fixes it:

```java
@ExceptionHandler(AuthorizationDeniedException.class)
public ProblemDetail handleAuthorizationDenied(AuthorizationDeniedException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
            "You do not have permission to perform this action.");
    pd.setTitle("Access denied");
    return pd;
}
```

Two details in that body are deliberate:

- The detail is a **fixed, generic sentence**. Never echo `ex.getMessage()` or explain *why* — "you
  are not the owner of project 42" confirms project 42 exists and has a different owner. §6.4 again.
- It is a `ProblemDetail`, so denials match every other error the API produces.

### 7.3 The filter chain needs its own handler too

You now have two places a 403 is born, at different points in the request:

| Denial | Produced by | Rendered by |
| ------ | ----------- | ----------- |
| a URL rule (`/api/admin/**`) | `AuthorizationFilter`, before any controller | an `AccessDeniedHandler` on the filter chain |
| a method rule (`@PreAuthorize`) | the AOP proxy, inside the dispatch | `@RestControllerAdvice` |

Both must report the same `title` / `status` / `detail`, or a client sees two different 403 bodies
depending on which layer happened to say no. So TaskFlow adds a handler mirroring Lab 7's
`JwtAuthenticationEntryPoint`:

```java
.exceptionHandling(ex -> ex
        .authenticationEntryPoint(jwtAuthenticationEntryPoint)   // 401, from Lab 7
        .accessDeniedHandler(restAccessDeniedHandler))           // 403, new in Lab 8
```

Verified output from both paths:

```json
// method rule — PUT /api/comments/1 as admin (author-only rule)
{"detail":"You do not have permission to perform this action.","instance":"/api/comments/1","status":403,"title":"Access denied"}

// URL rule — GET /api/admin/users as bob
{"type":"about:blank","title":"Access denied","status":403,"detail":"You do not have permission to perform this action."}
```

The meaningful fields match. They are not byte-identical: Spring's `ProblemDetail` serializer adds
`instance` and omits `type` when it holds the default `about:blank`, which the hand-written handler
spells out.

---

## 8. Testing method security

This is where most method-security work quietly goes wrong, so read this section before writing a
single test.

### 8.1 A Mockito unit test proves nothing

```java
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {
    @InjectMocks private CommentServiceImpl service;   // ← constructed with `new`
```

`@InjectMocks` calls the constructor directly. **No Spring, no proxy, no rules.** Every method runs
unguarded.

The danger is not that such a test fails — it is that a test like *"carol cannot delete bob's
comment"* written here would **pass whether the annotation existed or not**, because nothing is
checking. A green suite that proves nothing is worse than no suite.

So a Mockito test is the right place for mapping, id-resolution and 404s, and the wrong place for
authorization. TaskFlow's says so in its own name:

```java
@DisplayName("CommentServiceImpl (business logic; authorization annotations are INERT here)")
```

### 8.2 A `@WebMvcTest` slice proves nothing either

`@WebMvcTest` replaces the service with `@MockitoBean CommentService` — a mock of the **interface**.
The rules live on the implementation, which is never instantiated. The slice is right for status
codes, JSON and `Location` headers; it cannot exercise a `@PreAuthorize`.

It *is* the right place to verify the **403 rendering** from §7, by making the mock throw:

```java
@Test
@WithMockUser(username = "carol", roles = "USER")
void updateDeniedIsForbidden() throws Exception {
    willThrow(new AuthorizationDeniedException("Access Denied"))
            .given(commentService).updateComment(eq(502L), any(CommentUpdateRequestDto.class));

    mockMvc.perform(put("/api/comments/502").contentType(MediaType.APPLICATION_JSON)
                    .content(json(new CommentUpdateRequestDto("Hijacked"))))
            .andExpect(status().isForbidden())          // not 500 — proves the handler exists
            .andExpect(jsonPath("$.title").value("Access denied"));
}
```

### 8.3 The right layer: a minimal `@EnableMethodSecurity` context

Method security needs a container that creates proxies — and *nothing else*. Not a web server, not a
database. So build the smallest context that qualifies: `@EnableMethodSecurity` plus the real service
implementations over `@MockitoBean` repositories.

`@SpringJUnitConfig(X.class)` is shorthand for `@ExtendWith(SpringExtension.class)` plus
`@ContextConfiguration(classes = X)`: a plain Spring TestContext with **no** Boot auto-configuration at
all. It is new here — Labs 6 and 7 only needed `@WebMvcTest`, `@DataJpaTest` and plain Mockito.

```java
@SpringJUnitConfig(MethodSecurityTest.Config.class)
class MethodSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {

        @Bean
        ProjectService projectService(ProjectRepository projects, UserRepository users) {
            return new ProjectServiceImpl(projects, users);
        }

        /** Bean NAME matters: SpEL looks up "@taskFlowPermissions" by name. */
        @Bean
        TaskFlowPermissions taskFlowPermissions(ProjectRepository projects, TaskRepository tasks,
                CommentRepository comments) {
            return new TaskFlowPermissions(projects, tasks, comments);
        }
        …
    }

    @MockitoBean private ProjectRepository projectRepository;
    …
    @Autowired private ProjectService projectService;   // the INTERFACE — it is a JDK proxy
```

Three details that will cost you an afternoon if missed:

- **Autowire the interface.** `proxyTargetClass` defaults to `false`, so these are JDK dynamic
  proxies; asking for `ProjectServiceImpl` fails to inject.
- **Declare `taskFlowPermissions` as an explicit `@Bean` method**, so the bean name comes from the
  method name. Registering the class via `@Import` names the bean after its *fully-qualified class
  name*, and every `@taskFlowPermissions` expression then fails at call time.
- **No transaction manager is needed.** Without `@EnableTransactionManagement`, the `@Transactional`
  annotations on the services are simply ignored — which is fine, because this test is about
  authorization.

Full-context `@SpringBootTest` integration testing arrives in **Lab 10**; you do not need it here, and
reaching for it would make every test slower for no extra confidence about the rules.

### 8.4 `@WithMockUser` and `@WithAnonymousUser`

`spring-security-test` (already on the classpath since Lab 6) populates the `SecurityContext` before
each test, so rules see a principal with no login happening. Method security only reads the
`Authentication`, so the mechanism — Basic, JWT — is irrelevant:

```java
@Test
@WithMockUser(username = "carol", roles = "USER")   // authorities: ROLE_USER
void updateProjectAsStrangerDenied() {
    given(projectRepository.existsByIdAndOwnerUsername(PROJECT_ID, "carol")).willReturn(false);

    assertThatThrownBy(() ->
            projectService.updateProject(PROJECT_ID, new ProjectRequestDto("Hijacked", null)))
            .isInstanceOf(AuthorizationDeniedException.class);

    verify(projectRepository, never()).save(any(Project.class));   // the body never ran
}
```

Note `roles = "USER"` produces the authority `ROLE_USER` — the prefix is added for you, exactly as
`hasRole('USER')` expects. Use `@WithAnonymousUser` for the anonymous-token case from §6.3.

Asserting **`never()`** on the write is the habit to build: it proves the rule fired *before* the body,
which is the actual claim being made.

### 8.5 One more layer: `@DataJpaTest` for the probes

The permission bean is only as good as its queries, and in `MethodSecurityTest` the repositories are
mocks — a misspelt finder would "pass" happily. Spring Data parses method names into property paths
when the repository is created, so a wrong name fails at **context startup**. Only a real database
slice catches it:

```java
@Test
@DisplayName("existsByIdAndProjectOwnerUsername walks task → project → owner → username")
void projectOwnerProbe() {
    assertThat(taskRepository.existsByIdAndProjectOwnerUsername(taskT2Id, "alice")).isTrue();
    assertThat(taskRepository.existsByIdAndProjectOwnerUsername(taskT2Id, "bob")).isFalse();
}
```

Also worth an explicit test: an `existsByIdAndAssigneeUsername` probe against an **unassigned** task
must return `false`, not blow up on the null association.

### 8.6 Which layer proves what

| Layer | Proves | Cannot prove |
| ----- | ------ | ------------ |
| Mockito unit (`@InjectMocks`) | mapping, id-resolution, 404s, which fields a write changes | anything about authorization — **annotations are inert** |
| `@DataJpaTest` | the derived `existsBy…` names resolve; scoped queries and their page totals | authorization rules |
| `@WebMvcTest` slice | status codes, JSON, `Location`, URL-level 401/403, denial **rendering** | rules on the service implementation |
| minimal `@EnableMethodSecurity` context | **the rules themselves**, allow and deny, per principal | SQL correctness, HTTP wiring |

---

## 9. TaskFlow's Lab 8 rules, in one place

The whole matrix, so you can see the shapes side by side. ADMIN may read and delete anything, but
**may not act *as* another user** — `createProject` and `createComment` both pin the owner/author to
the principal with no ADMIN escape — and **may not edit someone else's comment**.

| Operation | Rule | Technique |
| --------- | ---- | --------- |
| `createProject` | owner argument must be the principal | `@PreAuthorize` + argument comparison |
| `getAllProjects` | own projects only (ADMIN: all) | **scoped query** — no annotation |
| `getProjectById` | owner or ADMIN | `@PostAuthorize` on `returnObject` |
| `updateProject` / `deleteProject` | owner or ADMIN | `@PreAuthorize` + permission bean |
| `createTask` | must own the target project (from the **body**) | `@PreAuthorize` + `#request.projectId` |
| `getTasks` (global feed) | ADMIN only | `@PreAuthorize` + `hasRole` |
| `getTasksByProject` | project owner or ADMIN | `@PreAuthorize` + permission bean |
| `getTaskById` / `updateTask` | project owner **or assignee** | `@PreAuthorize` + `canWorkOnTask` |
| `updateTask` (moving projects) | authority over source **and** destination | `@PreAuthorize` + `canPlaceTaskInProject` |
| `deleteTask` | project owner only — **not** the assignee | `@PreAuthorize` + narrower probe |
| `createComment` | author must be the principal, on a task they can work on | `@PreAuthorize`, two clauses |
| `getCommentsByTask` | gate the thread, then trim to visible rows | `@PreAuthorize` **+** `@PostFilter` |
| `getCommentById` | author, project owner, or ADMIN | `@PostAuthorize` on `returnObject` |
| `updateComment` | **author only — no ADMIN override** | `@PreAuthorize` + permission bean |
| `deleteComment` | author or ADMIN (moderation) | `@PreAuthorize` + permission bean |
| `getByUsername` | self or ADMIN | `@PreAuthorize` + argument comparison |
| `getAllUsers` | ADMIN (behind the URL rule too) | `@PreAuthorize` — defense in depth |

Two asymmetries are worth defending out loud, because they show that "ADMIN can do everything" is a
default worth questioning per operation rather than pasting `hasRole('ADMIN') or …` everywhere:

- **Comment update is author-only; delete allows ADMIN.** Editing someone else's comment puts words in
  their mouth; removing it does not. Moderation is a legitimate admin power; ghostwriting is not.
- **A task's assignee may update it but not delete it.** Being handed work is authority to progress
  it, not to destroy it.

And one rule is redundant *on purpose*: `getAllUsers` is already behind
`requestMatchers("/api/admin/**").hasRole("ADMIN")`. The annotation adds nothing today — and it is
still right, because the day someone exposes that service from a path outside `/api/admin`, the URL
rule stops applying and this one still holds. Two independent layers, and the one attached to the code
travels with it.

---

## Checklist: you are ready to build Lab 8 when you can...

- [ ] Explain in one sentence why `PUT /api/projects/42` cannot be authorized by a URL rule.
- [ ] List what a request matcher can see, and the three things it cannot (body, row, owner).
- [ ] State what `@EnableMethodSecurity` enables by default, and what happens to your annotations
      without it.
- [ ] Explain why an internal `this.method()` call silently skips a `@PreAuthorize`, and fix it.
- [ ] Write a `@PreAuthorize` that reads a method argument, and one that reads a property of an
      argument — and say why the second is impossible at the URL layer.
- [ ] Explain when `@PostAuthorize` is correct, and why it must not guard a write.
- [ ] Say what `filterObject` is, why the returned collection must be mutable, and why a paginated
      response must be filtered in the query instead.
- [ ] Write a fail-closed permission bean and list the five design rules it follows.
- [ ] Justify returning 403 rather than 404 for a row that does not exist.
- [ ] Explain why `AuthorizationDeniedException` becomes a 500 without a dedicated handler, and why
      the filter chain needs a separate `AccessDeniedHandler`.
- [ ] Say which test layer can prove a `@PreAuthorize` rule, and why the other two cannot.

Now build it: **`LAB_08_METHOD_SECURITY.md`**.
