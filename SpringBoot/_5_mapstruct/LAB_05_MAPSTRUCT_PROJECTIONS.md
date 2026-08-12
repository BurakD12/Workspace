# Lab 05 — DTO Mapping at Scale with MapStruct & Read-Optimized Projections

> **Hand this single document to the student.** It is fully self-contained: it specifies *what* to
> add (the requirements, the exact API contract, and a **class-by-class implementation checklist**
> in Section 6) and gives *guidance* on *how* to approach the genuinely new concepts. A student can
> implement the entire feature from this file alone, starting from a finished Lab 4 project.

---

## 1. Problem Statement & Goal

Through Labs 1–4 you wrote **entity↔DTO mapping by hand** — a `mapToDetailResponse`, a
`mapToListResponse`, a `toSummary`, one helper per type, all living inside the service. That code is
pure, repetitive boilerplate: it never contains business logic, yet it is easy to get subtly wrong (a
forgotten field, a null collection) and it grows linearly with every DTO. Lab 5 is an **increment on the
Lab 4 Bookstore**: you **delete every hand-written mapping helper** and replace it with **MapStruct**
mappers whose implementations are **generated at compile time**, then you add **Spring Data projections**
so read endpoints fetch only the columns they actually return.

There are **two** new ideas, and they pull in the same direction — *stop doing work you do not need to
do*:

1. **MapStruct** removes the hand-written mapping boilerplate while staying **fully type-safe** and
   **reflection-free** (the generated `*MapperImpl` is plain Java you can open and read).
2. **Projections** attack **over-fetching**: a list endpoint that only shows `id, title, isbn, author
   last name` should not `SELECT *` and hydrate full `Book` entities. This directly continues Lab 3's
   list-vs-detail split and its N+1 lessons.

The hard rule of this lab: **the Lab 3 API contract must not change by a single byte.** `GET /api/books`,
`GET /api/books/{id}`, `POST/PUT`, and the Lab 4 `GET /api/books/search` must return **exactly** the JSON
they returned before. MapStruct is an *internal* refactor. The projections are exposed through **new**
endpoints (`GET /api/books/summaries`) so they never disturb the existing contract.

You will deliver this feature added to your Lab 4 codebase, matching the **API Contract** in Section 12,
backed by the same PostgreSQL database, with automated tests proving the generated output is
byte-for-byte identical to the old manual output. The base package for this lab is
**`com.example._5_mapstruct`**.

## 2. Learning Objectives (what this assesses)

1. Explain what a **compile-time annotation processor** is: it runs during `javac`, generates plain Java
   source (no runtime reflection), and — because you already wrote mappers by hand — the generated
   `*MapperImpl` is *demystified* code you can open under `target/generated-sources` and trust.
2. Configure **MapStruct 1.6.3 with Lombok** on Spring Boot 4.0 / Java 21: the `mapstruct` dependency
   plus the `annotationProcessorPaths` ordering (`lombok` → `lombok-mapstruct-binding` →
   `mapstruct-processor`), and *why* Lombok must run first.
3. Declare **`@Mapper(componentModel = "spring")`** interfaces so the generated mapper is an injectable
   Spring bean, and **compose** mappers with `uses = {...}` for nested relationship mapping.
4. Use **`@Mapping(target = ..., ignore = true)`** for the deliberate **request→entity asymmetry**: the
   mapper ignores every relationship because **id-resolution stays in the service** (a repository lookup
   MapStruct must never do).
5. Write an in-place **`@MappingTarget` update** method so a PUT copies only the scalar fields and does
   **not** null out untouched associations.
6. Force **`"categories": []`** in the list view with a MapStruct **`expression`**, understanding that an
   *ignored* collection is left **null** by the mapper unless you set it explicitly.
7. Build **Spring Data interface projections** (closed, with a nested association getter) so a query
   materialises only the named columns, and expose them through a **new** endpoint.
8. Build a **class-based (record) projection** via a JPQL constructor expression and a **dynamic `<T>`
   projection** repository method, and — with `show-sql` on — observe that the projection query selects
   fewer columns than a full-entity fetch.
9. Prove with tests that the MapStruct output is **byte-for-byte** identical to the old manual mapper's
   output (the API contract is unchanged).

**Assumed background:** you have a working Lab 4 Bookstore (or are starting from the Lab 4 reference)
with the four entities, the hand-written mapping helpers, pagination, `PagedResponse`, `ProblemDetail`
errors, and the `GET /api/books/search` endpoint. This document re-states the Lab 3/4 pieces you must
touch so you can build the increment even if the details are rusty.

## 3. The One Big New Idea: Let the Compiler Write the Mapper

A hand-written mapper is a function `Entity → DTO` that you type out field by field. MapStruct lets you
declare that function as an **interface method signature** and generates the body for you **at compile
time**:

```java
@Mapper(componentModel = "spring")
public interface AuthorMapper {
    AuthorResponseDto toResponse(Author author);   // you declare the shape…
    // …MapStruct generates AuthorMapperImpl.toResponse() that copies id, firstName, lastName, bio
}
```

Three properties make this worth doing:

- **Compile-time, not runtime.** The annotation processor runs during `javac` and emits a plain Java
  class (`AuthorMapperImpl`) under `target/generated-sources/annotations/…`. There is **no reflection**
  at runtime and **no magic** — you can open that file and read exactly the `getX()/setX()` calls you
  would have written yourself. (You wrote mappers by hand first *precisely* so this generated code is
  familiar, not mysterious.)
- **Type-safe.** If a source field has no matching target, MapStruct fails the **build** (or warns),
  not production. A typo cannot silently drop a field.
- **It only shapes objects.** MapStruct copies fields between beans. It **cannot** and **must not** do a
  database lookup. Turning an `authorId` into a managed `Author` is a `repository.findById(...)` call —
  a **service** responsibility carried straight over from Lab 3. This asymmetry (the response mapping
  expands relationships, the request mapping *ignores* them) is the crux of Section 6.

The second idea — **projections** — is the read-side twin: instead of always fetching a full `Book` and
then throwing away most of it in the mapper, you ask Spring Data for a **narrower shape** (an interface
or a record) and the JPA provider selects **only those columns**.

## 4. What's New vs. Lab 4

Lab 5 changes a well-defined set of files. The entities, DTOs, exceptions, and the Lab 4
`specification` package stay **untouched**.

| Area | Change |
| ---- | ------ |
| `mapper` (**NEW package**) | **NEW** `AuthorMapper`, `PublisherMapper`, `CategoryMapper`, `BookMapper` — `@Mapper(componentModel = "spring")` interfaces. |
| `projection` (**NEW package**) | **NEW** `BookSummaryView` (interface projection, nested author) and `BookTitleIsbn` (record projection). |
| `service` | The **hand-written mapping helpers are DELETED**. Every service injects its mapper and delegates. `BookService`/`BookServiceImpl` gain **one** method: `getBookSummaries(Pageable)`. |
| `repository` | `BookRepository` gains **three** projection query methods (`findAllProjectedBy`, `findTitleIsbnByIsbn`, `findByIsbn(String, Class<T>)`). Every Lab 3/4 method stays exactly as it was. |
| `controller` | `BookController` gains **one** handler: `@GetMapping("/summaries")`. Every existing handler is unchanged. |
| `pom.xml` | Adds `org.mapstruct:mapstruct:1.6.3` and the `annotationProcessorPaths` (lombok → binding → processor). |
| `test` | **NEW** `BookMapperTest` (proves generated == manual, byte-for-byte); **NEW** `BookProjectionTest` (`@DataJpaTest`, the three projections); the service/controller tests are updated to compose the generated mappers. |

No entity changes, no new exceptions, no new error codes, and **no change to any existing endpoint's
response body**. The base package for this lab is **`com.example._5_mapstruct`**.

## 5. Tech Stack & Project Setup (required)

Same stack as Lab 4 — **Java 21**, **Maven**, **Spring Boot 4.0.x**, **PostgreSQL** at runtime, **H2**
(test scope) for tests, **Lombok** — plus **MapStruct 1.6.3**. Continue with the layered architecture
(`controller → service → repository → entity`, plus `dto`, `exception`, `specification`, and now
`mapper` and `projection`).

### 5.1 Spring Boot 4.0 specifics you MUST know (or you will get stuck)

Unchanged from Lab 4 but still trip people up:

- **Test slices live in dedicated starters** (`spring-boot-starter-data-jpa-test`,
  `spring-boot-starter-webmvc-test`); `spring-boot-starter-test` no longer brings them transitively.
- **The test-slice imports are renamed** (NOT the 3.x packages — see Section 14.1):
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
  `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`,
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.
- **Jackson 3**: the `ObjectMapper` is `tools.jackson.databind.ObjectMapper` (not `com.fasterxml...`).

### 5.2 MapStruct setup you MUST get exactly right (or nothing generates)

MapStruct is **not** a normal runtime dependency plus a bit of config — the *processor* has to be wired
into the compiler, and it has to **cooperate with Lombok**. Two pieces:

**(a) The runtime/annotation dependency** — only the annotations and the mapper API ship at runtime:

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
```

**(b) The annotation processors, in the right ORDER**, inside `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <!-- The lombok-mapstruct-binding makes the two processors cooperate across
                 compiler rounds so MapStruct sees Lombok's generated accessors.
                 Listing Lombok first is the conventional, safe ordering. -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.6.3</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Why each piece:

- **Lombok first (conventional ordering).** Our entities/DTOs use Lombok `@Getter/@Setter/@Builder`, and
  MapStruct reads those accessors to know how to copy fields. Listing Lombok first is the standard, safe
  ordering — but on its own the path order does not guarantee MapStruct sees Lombok's output.
- **`lombok-mapstruct-binding:0.2.0`** is the piece that actually makes it work: it coordinates the two
  processors across javac's annotation-processing rounds so MapStruct picks up the Lombok-generated
  members. Without it, MapStruct on recent Lombok can run before those accessors are visible and report
  "no property named …" or generate empty mappers.
- **`mapstruct-processor:1.6.3`** is the actual code generator. It must match the `mapstruct` API version.
- The Spring Boot parent (`4.0.x`) manages the Lombok version, so you do **not** pin Lombok's version
  here — but you **must** pin `lombok-mapstruct-binding` (0.2.0) and `mapstruct-processor` (1.6.3).

**Confirm it worked.** After `./mvnw compile` (or `test`), you MUST see the generated implementations:

```
target/generated-sources/annotations/com/example/_5_mapstruct/mapper/AuthorMapperImpl.java
target/generated-sources/annotations/com/example/_5_mapstruct/mapper/PublisherMapperImpl.java
target/generated-sources/annotations/com/example/_5_mapstruct/mapper/CategoryMapperImpl.java
target/generated-sources/annotations/com/example/_5_mapstruct/mapper/BookMapperImpl.java
```

Each is annotated `@Component` (that is `componentModel = "spring"` at work) and contains the plain
`getX()/setX()` copying you would have hand-written. **If these files are missing, your processor paths
are wrong — fix the pom before writing any mapper.**

## 6. Complete Implementation Inventory (class-by-class checklist)

This section lists **every Java class you create, change, or delete** for Lab 5. Use it as your build
checklist. Sections 7–14 explain the hard concepts; this section tells you **what each class is for and
what it must do**. Classes not listed here are **unchanged from Lab 4**.

### 6.1 `mapper` package — four NEW `@Mapper` interfaces (this package is new)

Each is a **plain interface** (MapStruct generates the `Impl`). All four use
**`@Mapper(componentModel = "spring")`** so the generated `*MapperImpl` is a `@Component` you can
`@Autowired`/constructor-inject. Import `org.mapstruct.Mapper`, `org.mapstruct.Mapping`,
`org.mapstruct.MappingTarget` (and, for `BookMapper`, `org.mapstruct.InjectionStrategy`).

The **three simple mappers** (`AuthorMapper`, `PublisherMapper`, `CategoryMapper`) share one shape. Using
Author as the template, produce **exactly** these signatures:

| Method (exact signature) | What it does |
| ------------------------ | ------------ |
| `AuthorResponseDto toResponse(Author author)` | Full response (id, firstName, lastName, bio) — replaces the old manual `toResponse`. |
| `AuthorSummaryDto toSummary(Author author)` | The nested summary (id, firstName, lastName) used inside `BookResponseDto`. |
| `@Mapping(target = "id", ignore = true)` `@Mapping(target = "books", ignore = true)` `Author toEntity(AuthorRequestDto dto)` | Request→entity. **Ignore** the DB-generated `id` and the `books` back-reference (owned elsewhere). |
| `@Mapping(target = "id", ignore = true)` `@Mapping(target = "books", ignore = true)` `void updateEntity(AuthorRequestDto dto, @MappingTarget Author author)` | In-place update for PUT — copies scalar fields onto the existing managed entity; ignores id and `books`. |

`PublisherMapper` is identical with `Publisher`/`PublisherRequestDto`/`PublisherResponseDto`/
`PublisherSummaryDto` (ignore `id` and `books`). `CategoryMapper` is identical with
`Category`/`CategoryRequestDto`/`CategoryResponseDto`/`CategorySummaryDto` (ignore `id` and `books`).

#### `BookMapper` — the composed mapper (the interesting one)

`Book` nests an `Author`, a `Publisher`, and a `Set<Category>`. Tell MapStruct to reuse the three simple
mappers for those nested types via **`uses = {...}`**, and use constructor injection for the generated
impl:

```java
@Mapper(componentModel = "spring",
        uses = {AuthorMapper.class, PublisherMapper.class, CategoryMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface BookMapper {

    // Detail view: expands the fully-initialised categories collection.
    BookResponseDto toDetailResponse(Book book);

    // List view: author + publisher come from the entity graph, but categories are NOT fetched
    // while paginating (Lab 3 rule). An IGNORED collection would be left null, so we force [].
    @Mapping(target = "categories", expression = "java(java.util.List.of())")
    BookResponseDto toListResponse(Book book);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "publisher", ignore = true)
    @Mapping(target = "categories", ignore = true)
    Book toEntity(BookRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "publisher", ignore = true)
    @Mapping(target = "categories", ignore = true)
    void updateEntity(BookRequestDto dto, @MappingTarget Book book);
}
```

The four methods, and why each is exactly this shape:

| Method | What it must do |
| ------ | --------------- |
| `toDetailResponse` | Maps every scalar plus nested `author` (via `AuthorMapper.toSummary`), `publisher` (via `PublisherMapper.toSummary`), and the **full** `categories` list (via `CategoryMapper.toSummary`). This is the single-book detail view; the collection **is** initialised (the repository fetched it), so it maps normally. Replaces the old manual `mapToDetailResponse`. |
| `toListResponse` | Same scalars + `author` + `publisher`, but **forces `categories = List.of()`**. Search/list results are paginated and Lab 3 forbids fetching a collection while paginating, so the collection is never loaded. **Key MapStruct fact:** if you merely *ignored* `categories` it would come out **`null`** and serialise as `"categories": null`, breaking the byte-for-byte contract. The `expression = "java(java.util.List.of())"` sets an **empty list** so the JSON stays `"categories": []`. Replaces the old manual `mapToListResponse`. |
| `toEntity` | Request→entity. **Ignore** `id`, `author`, `publisher`, `categories`. MapStruct copies only `title, isbn, publicationYear, price`; the **service** resolves the ids and sets the relationships. |
| `updateEntity` | In-place PUT update with `@MappingTarget Book book`. Copies the four scalars onto the existing managed `Book`; ignores `id` and all relationships so an untouched association is **not** nulled. The service re-resolves relationships afterwards. |

> **Read the generated `BookMapperImpl`** after your first build. Confirm `toListResponse` ends with
> `bookResponseDto.setCategories( java.util.List.of() );` and `toEntity` sets only the four scalars via
> `Book.builder()`. That file *is* the answer key — the generated code is what you are proving correct.

### 6.2 `projection` package — two NEW classes (this package is new)

#### `BookSummaryView` — a closed **interface** projection with a nested association

An interface whose getters are **all backed by real entity attributes**, so Spring Data lets the JPA
provider select **only** those columns instead of materialising a full `Book`. It also demonstrates that
interface projections **can traverse associations** (a nested `Author` projection):

```java
public interface BookSummaryView {
    Long getId();
    String getTitle();
    String getIsbn();
    Author getAuthor();

    interface Author {           // nested closed projection over Book -> Author
        String getLastName();
    }
}
```

Getter names must match entity property names exactly (`getId`, `getTitle`, `getIsbn`, `getAuthor`, and
on the nested type `getLastName`) — Spring Data maps them by name.

#### `BookTitleIsbn` — a class-based (record) projection

A flat **record** populated by a JPQL constructor expression. Unlike interface projections, a
class-based projection **cannot nest associations** — its constructor argument order must match the JPQL
`select` list exactly:

```java
public record BookTitleIsbn(Long id, String title, String isbn) { }
```

### 6.3 `repository` package — `BookRepository` CHANGED (three projection methods added)

Keep **every** Lab 3/4 member (the `existsBy*`, the `@EntityGraph` finders, `findDetailedById`,
`findAllWithAuthorAndPublisher`, and `JpaSpecificationExecutor<Book>`) **exactly** as it was. **Add**
three projection queries:

| Method (exact signature) | Kind | What it does |
| ------------------------ | ---- | ------------ |
| `Page<BookSummaryView> findAllProjectedBy(Pageable pageable);` | Interface projection (derived) | Returns the interface projection. Spring Data selects only `id, title, isbn` and the author's `lastName` (an implicit join to `Author`) instead of full `Book` rows. Backs `GET /api/books/summaries`. The `…ProjectedBy` derived-query name with no criteria means "all rows, projected". |
| `@Query("select new com.example._5_mapstruct.projection.BookTitleIsbn(b.id, b.title, b.isbn) from Book b where b.isbn = :isbn") Optional<BookTitleIsbn> findTitleIsbnByIsbn(@Param("isbn") String isbn);` | Class-based (record) projection | JPQL **constructor expression**; select-list order must match the `BookTitleIsbn` canonical constructor (`id, title, isbn`). |
| `<T> Optional<T> findByIsbn(String isbn, Class<T> type);` | **Dynamic** projection | One method, any shape: the caller passes the target type (`Book.class`, `BookSummaryView.class`, or `BookTitleIsbn.class`) and Spring Data returns that shape from the same query. |

Add the imports for `com.example._5_mapstruct.projection.BookSummaryView` and `…BookTitleIsbn`.

### 6.4 `service` package — DELETE the manual mappers; inject the MapStruct mappers

This is where the boilerplate disappears. In **every** service:

1. **Delete** the hand-written mapping helpers (`toResponse`, `toSummary`, `mapToListResponse`,
   `mapToDetailResponse`, etc.).
2. Add the mapper as a `final` field so `@RequiredArgsConstructor` injects it, and delegate.

**`AuthorServiceImpl`** (template for `Publisher`/`Category`): inject `private final AuthorMapper
authorMapper;` and rewrite the methods to delegate — `createAuthor` → `authorMapper.toEntity(...)` then
`authorMapper.toResponse(saved)`; `getAllAuthors` → `.map(authorMapper::toResponse)`; `getAuthorById` →
`authorMapper.toResponse(...)`; `updateAuthor` → `authorMapper.updateEntity(requestDto, author)` then
`authorMapper.toResponse(saved)`. The `RelationshipConflictException` on delete and the id-lookup logic
are **unchanged**. `PublisherServiceImpl` and `CategoryServiceImpl` mirror this exactly.

**`BookServiceImpl`** — inject `private final BookMapper bookMapper;` alongside the four repositories, and:

- **`createBook`**: `Book book = bookMapper.toEntity(requestDto);` then **keep** `resolveRelations(book,
  requestDto)` (the service still resolves ids → managed `Author`/`Publisher`/`Category`), then
  `bookMapper.toDetailResponse(bookRepository.save(book))`.
- **`updateBook`**: after the ISBN-change check, `bookMapper.updateEntity(requestDto, book)` then
  `resolveRelations(book, requestDto)` then `bookMapper.toDetailResponse(...)`. The `@MappingTarget`
  update copies scalars in place; relationships are re-resolved by the service, never the mapper.
- **`getBookById`**: `bookMapper.toDetailResponse(book)`.
- **The private `toPagedResponse(Page<Book>)` helper** maps each element with
  `bookMapper::toListResponse` (empty categories) and returns `PagedResponse.from(page, content)`. Reuse
  it for `getAllBooks`, `searchBooks`, `getBooksByAuthor/Publisher/Category` exactly as in Lab 4.
- **`resolveRelations` / `resolveCategories` stay** — they are the id→entity lookups MapStruct must not do.

Add **one new method** to the interface and impl:

**`BookService` — add:**
```java
PagedResponse<BookSummaryView> getBookSummaries(Pageable pageable);
```

**`BookServiceImpl` — add** (no extra `@Transactional`; it is a read under the class-level
`@Transactional(readOnly = true)`):
```java
@Override
public PagedResponse<BookSummaryView> getBookSummaries(Pageable pageable) {
    Page<BookSummaryView> page = bookRepository.findAllProjectedBy(pageable);
    return PagedResponse.from(page, page.getContent());
}
```

Note it returns `PagedResponse<BookSummaryView>` (the projection type) directly — there is nothing to map,
the projection **is** the response shape.

### 6.5 `controller` package — `BookController` CHANGED (one new handler)

Every existing handler (`create`, `getAll`, `search`, `getById`, `update`, `delete`) is **unchanged**.
Add **one** handler:

| Handler signature | Mapping | Returns |
| ----------------- | ------- | ------- |
| `getSummaries(@PageableDefault(size = 20, sort = "title") Pageable pageable)` | `@GetMapping("/summaries")` | `200`, `PagedResponse<BookSummaryView>` |

Body: `return ResponseEntity.ok(bookService.getBookSummaries(pageable));`.

> **Routing point (`/summaries` vs `/{id}`):** just like Lab 4's `/search`, the literal path
> `GET /api/books/summaries` is matched **before** the path-variable mapping `GET /api/books/{id}`, so
> `summaries` is never bound as an `{id}`. The two never collide.

### 6.6 Test classes — two NEW + updated existing tests

All must pass with **`./mvnw test`** using H2 (Section 14 for the Spring Boot 4.0 import packages).

| Class | Layer | Status | What you must verify |
| ----- | ----- | ------ | -------------------- |
| **`BookMapperTest`** | plain JUnit | NEW | The generated `BookMapper` output equals the old manual mapping **byte-for-byte**, including `"categories": []` in the list view. See 6.6.1. |
| **`BookProjectionTest`** | `@DataJpaTest` | NEW | The interface, record, and dynamic projections each return the right shape. See 6.6.2. |
| **`BookServiceImplTest`** | Mockito | CHANGED | Compose the **generated** mappers (not mocks) so the mapping is exercised for real; existing cases keep passing. See 6.6.3. |
| **`BookControllerTest`** | `@WebMvcTest` | UNCHANGED body, still green | Service is `@MockitoBean`; contract assertions (`$.author.lastName`, `$.categories[0].name`) prove the JSON is unchanged. |

#### 6.6.1 `BookMapperTest` (plain JUnit — the contract proof)

No Spring context. Instantiate the **real generated** mappers directly, composed exactly as Spring would:

```java
bookMapper = new BookMapperImpl(
        new AuthorMapperImpl(), new PublisherMapperImpl(), new CategoryMapperImpl());
```

Build a representative `Book` (id 10, "Clean Code", isbn `9780132350884`, year 2008, price `42.50`,
author Robert **Martin**, publisher **Prentice Hall**/USA, one category **Software**), then:

- **`toDetailResponse`**: assert the result `usingRecursiveComparison().isEqualTo(expected)` where
  `expected` is the object the **old manual `mapToDetailResponse`** produced (full nested author,
  publisher, and `categories = [Software]`). Spell out the load-bearing nested pieces too
  (`getAuthor().getLastName()` = `"Martin"`, `getPublisher().getName()` = `"Prentice Hall"`,
  `categories` extracting name contains exactly `"Software"`).
- **`toListResponse`**: assert scalars + `author`/`publisher` map, and — the whole point —
  `assertThat(actual.getCategories()).isNotNull().isEmpty()`. This is the byte-for-byte guarantee: the
  list view serialises `"categories": []`, **never** `null`.

#### 6.6.2 `BookProjectionTest` (`@DataJpaTest`)

Seed via `TestEntityManager` one author (Robert **Martin**), one publisher (Prentice Hall, `country`
`USA` — remember `Publisher.country` is `nullable = false`), and one book (Clean Code,
`9780132350884`, 2008, `42.50`), then `flush()` + `clear()`. Three tests:

- **interface projection**: `findAllProjectedBy(PageRequest.of(0, 10, Sort.by("title")))` →
  `totalElements == 1`; the single `BookSummaryView` has `getId() != null`, `getTitle() == "Clean
  Code"`, `getIsbn() == "9780132350884"`, and `getAuthor().getLastName() == "Martin"`.
- **record projection**: `findTitleIsbnByIsbn("9780132350884").orElseThrow()` → `id() != null`,
  `title() == "Clean Code"`, `isbn() == "9780132350884"`.
- **dynamic projection**: call `findByIsbn("9780132350884", …)` three times with `Book.class`,
  `BookSummaryView.class`, and `BookTitleIsbn.class`, and assert each returns the corresponding shape
  (full entity title; view title + nested author last name; record isbn).

With `spring.jpa.show-sql=true` in the test properties, eyeball the logged SQL: the projection query
selects only the projected columns, while `Book.class` selects the full row (Section 8.2).

#### 6.6.3 `BookServiceImplTest` — compose the generated mappers (Mockito)

Keep every Lab 4 case. The **only** structural change: build the service with the **real generated**
mapper instead of a mocked helper, so the mapping is exercised end to end:

```java
BookMapperImpl bookMapper = new BookMapperImpl(
        new AuthorMapperImpl(), new PublisherMapperImpl(), new CategoryMapperImpl());
service = new BookServiceImpl(
        bookRepository, authorRepository, publisherRepository, categoryRepository, bookMapper);
```

The repositories stay `@Mock`. The `searchBooks` case is unchanged from Lab 4 and still asserts the list
mapping leaves `getContent().get(0).getCategories()` **empty** — now proving the *generated* mapper
honours the contract. The `createHappyPath` / `updateUnchangedIsbn` cases now assert the generated
nested mapping (`getAuthor().getLastName()` = `"Martin"`, `getCategories()` extracting name =
`"Software"`).

### 6.7 Suggested file count for this increment

| Package | Files |
| ------- | ----- |
| `mapper` (new) | +4 (`AuthorMapper`, `PublisherMapper`, `CategoryMapper`, `BookMapper`) |
| `projection` (new) | +2 (`BookSummaryView`, `BookTitleIsbn`) |
| `repository` | 0 new (edit `BookRepository`: +3 methods) |
| `service` | 0 new (edit 4 services: delete manual helpers, inject mappers; `BookService` +1 method) |
| `controller` | 0 new (edit `BookController`: +1 handler) |
| `pom.xml` | edit (MapStruct dep + processor paths) |
| `test` | +2 (`BookMapperTest`, `BookProjectionTest`); edit the service test to compose generated mappers |
| **Total new files** | **8 new Java source files** (6 main + 2 test), plus edits to the pom, 5 main classes, and the test suite |

---

## 7. The Mappers (required + guidance)

### 7.1 Why delete the hand-written helpers

The manual helpers were correct but were pure boilerplate — dozens of `dto.setX(entity.getX())` lines
that add zero business value and are a standing invitation to typos. MapStruct generates the identical
code from the method signatures, and the build fails if a field cannot be mapped. You keep the *intent*
(the method names and shapes) and delete the *transcription*.

### 7.2 `componentModel = "spring"` and `uses`

`componentModel = "spring"` makes the generated `*MapperImpl` a `@Component`, so it is an injectable
bean — exactly why the services can take it as a `final` constructor param. `uses = {AuthorMapper.class,
PublisherMapper.class, CategoryMapper.class}` tells `BookMapper` to **delegate** nested types to those
mappers rather than trying to map an `Author` field by field itself. `injectionStrategy =
InjectionStrategy.CONSTRUCTOR` makes the generated `BookMapperImpl` take its three collaborators via
constructor — which is why `BookMapperTest` can `new BookMapperImpl(new AuthorMapperImpl(), …)`.

### 7.3 The request→entity asymmetry (the exam question)

The response direction **expands** relationships (Book → nested Author/Publisher/Category summaries). The
request direction **ignores** them (`@Mapping(target = "author", ignore = true)`, etc.). This is
deliberate: turning `authorId` into a **managed** `Author` requires `authorRepository.findById(...)` — a
database lookup that belongs in the **service** (`resolveRelations`), never in a mapper. MapStruct only
shapes objects. If you are ever tempted to make the mapper resolve an id, stop: that is the service's job,
carried straight over from Lab 3.

### 7.4 `@MappingTarget` for partial PUT updates

`void updateEntity(BookRequestDto dto, @MappingTarget Book book)` copies the request's scalar fields
**onto an existing managed entity** instead of building a new one. Because the relationships are ignored
and re-resolved by the service, a PUT never nulls out an untouched association. Call it from each PUT
service method (`updateAuthor`, `updateBook`, …) before saving.

### 7.5 The `null`-collection trap (why `expression`, not `ignore`)

`toListResponse` must emit `"categories": []`. If you write `@Mapping(target = "categories", ignore =
true)`, MapStruct leaves the field **null** and Jackson serialises `"categories": null` — a contract
break. `@Mapping(target = "categories", expression = "java(java.util.List.of())")` forces an empty list.
This single line is what keeps the Lab 3 list-view JSON byte-for-byte identical.

## 8. Projections (required + guidance)

### 8.1 Interface vs. class-based vs. dynamic

- **Interface projection** (`BookSummaryView`): closed (all getters entity-backed), **can nest
  associations** (the inner `Author` projection over `Book.author`). Spring Data returns a proxy; the JPA
  provider selects only the referenced columns.
- **Class-based (record) projection** (`BookTitleIsbn`): a flat DTO filled by a JPQL **constructor
  expression** (`select new …BookTitleIsbn(b.id, b.title, b.isbn)`). **Cannot** nest associations;
  constructor arg order must match the select list.
- **Dynamic projection** (`<T> Optional<T> findByIsbn(String, Class<T>)`): one method, the caller passes
  the shape as a `Class<T>`. Great for reusing a single finder across full-entity and projected reads.

### 8.2 Why projections (the over-fetching payoff)

Backing `GET /api/books/summaries` with `findAllProjectedBy` means the list read stops materialising full
`Book` entities — the SQL selects only `id, title, isbn` and the joined author `last_name`. Turn on
`spring.jpa.show-sql=true` and compare the summaries query against a full-entity `findAll`: the projection
selects fewer columns and does less work. This is the read-side complement to Lab 3's list-vs-detail split
and its N+1 fixes.

### 8.3 Projections are additive — the contract is safe

The projection endpoints are **new** (`GET /api/books/summaries`). The existing `GET /api/books` keeps
returning the full `BookResponseDto` list exactly as before. This is how the roadmap's "back the list
endpoint with a projection" step reconciles with the hard "contract must not change" rule: you *add* a
projection-backed endpoint rather than *replace* the existing one.

## 9. Service Layer (required + guidance)

Each service keeps its Lab 3/4 shape (`@Service`, `@RequiredArgsConstructor`, class-level
`@Transactional(readOnly = true)`, write methods annotated `@Transactional`). The change is purely
mechanical: delete the manual mapping helpers, add the mapper as a `final` field, delegate. `BookService`
gains `getBookSummaries`, a three-line read that returns the projection page directly via
`PagedResponse.from`. Do **not** add new mapping helpers — MapStruct owns mapping now, the service owns
id-resolution.

## 10. Controller (required + guidance)

`BookController` stays thin: add the `@GetMapping("/summaries")` handler with a
`@PageableDefault(size = 20, sort = "title") Pageable`, delegate to `bookService.getBookSummaries(...)`,
return `ResponseEntity.ok(...)`. Remember the `/summaries`-before-`/{id}` routing point (Section 6.5).
Every other handler is untouched.

## 11. Architecture & Code Constraints (required)

- Layered as before: `controller → service → repository → entity`, plus `dto`, `exception`,
  `specification`, and the new `mapper` and `projection` packages.
- The API still exposes **DTOs/projections only** — never entities.
- **Mapping lives in the `mapper` package**; **id-resolution lives in the service**. Keep that
  separation absolute — a mapper must never call a repository.
- Projections are **read-only** shapes; they back **new** endpoints and never replace an existing one.
- No new error codes. `GET /api/books/summaries` returns `200` for any page request.

## 12. API Contract (required — the authoritative spec)

### 12.1 Existing endpoints — UNCHANGED (byte-for-byte)

`POST /api/books`, `GET /api/books`, `GET /api/books/{id}`, `PUT /api/books/{id}`,
`DELETE /api/books/{id}`, `GET /api/books/search`, and all Author/Publisher/Category endpoints keep the
**exact** Lab 3/4 request and response shapes. The `BookResponseDto` shape is unchanged:

```json
{
  "id": 10,
  "title": "Clean Code",
  "isbn": "9780132350884",
  "publicationYear": 2008,
  "price": 42.50,
  "author": { "id": 1, "firstName": "Robert", "lastName": "Martin" },
  "publisher": { "id": 2, "name": "Prentice Hall" },
  "categories": [ { "id": 3, "name": "Software" } ]
}
```

The list/search views serialise `"categories": []` (empty, never null) — this is the invariant
`BookMapperTest` guards.

### 12.2 `GET /api/books/summaries` — NEW

| Method | Path | Success | Errors |
| ------ | ---- | ------- | ------ |
| GET | `/api/books/summaries` | `200`, paged `BookSummaryView` | — |

Standard `Pageable` params (`page`, `size`, `sort`; defaults `size=20`, `sort=title`). Response is the
`PagedResponse` wrapper around the **projection** shape:

```json
{
  "content": [
    { "id": 10, "title": "Clean Code", "isbn": "9780132350884", "author": { "lastName": "Martin" } }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "last": true
}
```

Note the projection carries **only** `id, title, isbn`, and a nested `author.lastName` — deliberately
narrower than `BookResponseDto`.

## 13. Business Rules (required)

- The MapStruct refactor is **internal**: every existing endpoint returns identical JSON. Prove it.
- The list/search views always emit `"categories": []` (empty list, never `null`).
- Request→entity mapping **ignores** all relationships; the **service** resolves ids → managed entities.
- PUT uses the `@MappingTarget` update so untouched fields/associations are not nulled.
- `GET /api/books/summaries` returns a **projection** (id, title, isbn, author last name), paginated and
  sorted, wrapped in `PagedResponse`.
- Mappers never touch the database; projections are read-only.

## 14. Testing (required + guidance)

All tests pass with `./mvnw test`, using H2 for the DB layer.

### 14.1 Spring Boot 4.0 test imports (different from 3.x!)

```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;     // @DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;    // TestEntityManager
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;        // @WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean;   // @MockitoBean (unchanged)
import tools.jackson.databind.ObjectMapper;                                  // Jackson 3! (was com.fasterxml.jackson...)
```

### 14.2 The contract proof (`BookMapperTest`, plain JUnit)

This is the most important test in the lab: it instantiates the **generated** `BookMapperImpl` (composed
with the generated Author/Publisher/Category impls) and asserts its output equals what the old manual
helpers produced — including `"categories": []` in the list view (Section 6.6.1). If this is green, the
API contract survived the refactor.

### 14.3 The projection test (`BookProjectionTest`, `@DataJpaTest`)

Seed one author/publisher/book against H2 and assert the interface, record, and dynamic projections each
return the right shape (Section 6.6.2). With `show-sql` on, confirm the projection query selects fewer
columns than a full-entity fetch.

### 14.4 Service and web tests

- **Service** (`BookServiceImplTest`): compose the **generated** mappers (Section 6.6.3) so the mapping
  is exercised for real; every Lab 4 case keeps passing, and `searchBooks` still asserts empty categories.
- **Web** (`BookControllerTest`, `@WebMvcTest(BookController.class)`, service `@MockitoBean`): unchanged
  from Lab 4 — its `$.author.lastName` / `$.categories[0].name` assertions independently confirm the JSON
  contract. Use the Jackson 3 `ObjectMapper` (`tools.jackson.databind.ObjectMapper`).

## 15. Suggested Build Order

1. Start from a working Lab 4 project (or the reference) and confirm `./mvnw test` is green.
2. Edit the pom: add `mapstruct:1.6.3` and the `annotationProcessorPaths` (lombok → binding →
   processor). Run `./mvnw compile` and **confirm the four `*MapperImpl` files appear** under
   `target/generated-sources` (Section 5.2). Do not proceed until they do.
3. Write the four `@Mapper` interfaces (Section 6.1). Rebuild; open `BookMapperImpl` and read it.
4. Delete the hand-written mapping helpers from all four services and wire in the injected mappers
   (Section 6.4).
5. Add the `projection` package (`BookSummaryView`, `BookTitleIsbn`) and the three repository methods
   (Sections 6.2, 6.3).
6. Add `getBookSummaries` to `BookService`/`BookServiceImpl` and the `/summaries` handler to
   `BookController` (Sections 6.4, 6.5).
7. Write `BookMapperTest` and `BookProjectionTest`; update `BookServiceImplTest` to compose the generated
   mappers (Section 6.6).
8. Run `./mvnw test` (all Lab 4 tests plus the new ones) and exercise the endpoints manually (Section 16).

## 16. Manual Testing Flow

With the app running and a few books seeded:

1. `GET /api/books` → `200`, unchanged Lab 3 list shape (`"categories": []`).
2. `GET /api/books/{id}` → `200`, unchanged detail shape (full `categories`).
3. `POST /api/books` then `PUT /api/books/{id}` changing only the title → the untouched author/publisher
   survive (the `@MappingTarget` update did not null them).
4. `GET /api/books/search?title=clean` → unchanged Lab 4 search behaviour.
5. `GET /api/books/summaries` → `200`, the narrow projection shape (id, title, isbn, author last name).
6. With `show-sql` on, compare the SQL of step 5 against step 1 — the summaries query selects fewer
   columns.

## 17. Acceptance Criteria (how it will be graded)

- [ ] The pom adds `mapstruct:1.6.3` and the `annotationProcessorPaths` in order (lombok →
      `lombok-mapstruct-binding:0.2.0` → `mapstruct-processor:1.6.3`); the four `*MapperImpl` files are
      generated under `target/generated-sources`.
- [ ] `AuthorMapper`, `PublisherMapper`, `CategoryMapper`, `BookMapper` exist as
      `@Mapper(componentModel = "spring")` interfaces with the documented signatures; `BookMapper` uses
      `uses = {...}` + `InjectionStrategy.CONSTRUCTOR`.
- [ ] Request→entity mappings `@Mapping(ignore = true)` every relationship and the id; the service still
      resolves ids → managed entities (`resolveRelations`).
- [ ] Each PUT calls an `@MappingTarget` update method so untouched fields/associations are not nulled.
- [ ] `toListResponse` forces `categories = List.of()` (empty, never null) via `expression`.
- [ ] The hand-written mapping helpers are **deleted** from all four services; each service injects and
      delegates to its mapper.
- [ ] `BookSummaryView` (interface, nested author) and `BookTitleIsbn` (record) exist; `BookRepository`
      adds `findAllProjectedBy`, `findTitleIsbnByIsbn`, and `findByIsbn(String, Class<T>)` while keeping
      every Lab 3/4 method unchanged.
- [ ] `BookService`/`BookServiceImpl` expose `getBookSummaries`; `BookController` exposes
      `GET /api/books/summaries` returning `PagedResponse<BookSummaryView>`.
- [ ] Every existing endpoint returns **byte-for-byte identical** JSON (`BookMapperTest` proves it,
      including `"categories": []`).
- [ ] `./mvnw test` is green: all Lab 4 tests **plus** `BookMapperTest` and `BookProjectionTest`, with the
      service test composing the generated mappers.

## 18. Optional Stretch Goals (not graded)

1. Add an **open** projection getter (`@Value("#{target.firstName + ' ' + target.lastName}")`) to
   `BookSummaryView` and note it forces the full entity to load (so it is *not* read-optimized).
2. Add `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` to a partial-update method and discuss
   PATCH-style semantics.
3. Add a `@Mapping(source = ..., target = ...)` rename plus a `@Named` qualified method to a mapper and
   read how it appears in the generated impl.
4. Back a second read endpoint with the **record** projection and compare its SQL to the interface
   projection's.
5. Add a `@DataJpaTest` that asserts (via SQL statement inspection) the summaries query selects strictly
   fewer columns than the full-entity finder.

## 19. Instructor Notes

Lab 4 taught dynamic querying; Lab 5 teaches the two everyday production skills that keep a layered app
lean: **generated mapping** and **read-optimized projections**. We taught manual mapping first on purpose
— now that the student knows exactly what a mapper must do, MapStruct's generated `*MapperImpl` is
readable code, not magic, and the point lands: an annotation processor runs at compile time and writes
the boilerplate you would have typed. The student should leave able to (a) configure MapStruct with the
Lombok binding in the correct processor order and confirm the generated sources, (b) write composed
`@Mapper` interfaces with nested mapping, the request→entity ignore-and-resolve asymmetry, and
`@MappingTarget` updates, (c) explain why an ignored collection is `null` and must be forced to `[]`, and
(d) build interface, record, and dynamic projections and justify projections vs full-entity fetches.
Hammer the hard rule: **MapStruct is an internal refactor — the API contract cannot change by a byte**,
which is exactly why `BookMapperTest` reproduces the old manual output and the projections are exposed
through *new* endpoints (`GET /api/books/summaries`) rather than by rewriting the existing ones. Note the
Spring Boot 4.0 specifics (Sections 5.1, 14.1): the dedicated test-slice starters, the renamed
`org.springframework.boot.*.test.autoconfigure` packages, and the Jackson 3 `tools.jackson.databind`
`ObjectMapper`.
