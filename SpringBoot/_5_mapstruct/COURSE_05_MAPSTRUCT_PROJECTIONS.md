# Course 05 — DTO Mapping at Scale with MapStruct & Read-Optimized Projections

> **Learn first.** This is the study material you read *before* you build Lab 5. By the end you will
> understand how to delete every hand-written `mapToResponse(...)` helper from Labs 1–4 and let a
> compile-time processor generate those mappers for you — plus how to make read endpoints fetch only
> the columns they actually return, instead of hydrating whole entities you throw most of away.

## What you'll learn

We are taking the Lab 3/4 Bookstore (`Author 1‑N Book N‑1 Publisher`, `Book N‑N Category`) and doing
two independent things to it:

1. Replacing the manual entity↔DTO mapping code with **MapStruct**-generated mappers.
2. Adding **Spring Data projections** so a read query materialises only the fields it needs, backing a
   brand-new endpoint:

```
GET /api/books/summaries
```

Two hard rules frame the whole lab:

- **The API contract from Lab 3 must not change.** `GET /api/books`, `GET /api/books/{id}`, POST, PUT
  and DELETE keep returning **byte-for-byte** the same JSON. MapStruct only changes *how* the DTO is
  built, never *what* it contains.
- **MapStruct never touches the database.** Resolving `authorId`/`publisherId`/`categoryIds` into
  managed entities is a repository lookup the **service** still owns (from Lab 3). The mapper only
  shapes objects that are already in memory.

The seven modules below build up the tools you need:

1. **Why manual mapping first** — what an annotation processor is, and why you can trust generated code.
2. **Setting up MapStruct with Lombok** — the exact dependency + `annotationProcessorPaths` ordering.
3. **`@Mapper` components and Spring DI** — `componentModel = "spring"` and reading the generated `*MapperImpl`.
4. **Field mapping** — `@Mapping` with `ignore`/`expression`/`constant`, and `@MappingTarget` in-place update.
5. **Nested relationship mapping** — entity→response only, and the deliberate request→entity asymmetry.
6. **Interface-based projections** — closed and open projections, and nested projection getters.
7. **Class-based (record) and dynamic projections** — constructor projections, the `<T> Class<T>` signature, and what each kind can and cannot nest.

You already know layered CRUD, JPA relationships, `Pageable`/`Page`, the list-vs-detail split, and the
Lab 3 rule about not fetching collections while paginating. You have **never** used a code-generating
annotation processor or a projection — that is what we fix here.

---

## 1. Why manual mapping first, and what an annotation processor is

Through Labs 1–4 you wrote every mapper by hand:

```java
// The kind of helper you wrote by hand in Labs 1–4.
private BookResponseDto mapToResponse(Book book) {
    return new BookResponseDto(
            book.getId(), book.getTitle(), book.getIsbn(), book.getPublicationYear(),
            book.getPrice(),
            new AuthorSummaryDto(book.getAuthor().getId(),
                    book.getAuthor().getFirstName(), book.getAuthor().getLastName()),
            new PublisherSummaryDto(book.getPublisher().getId(), book.getPublisher().getName()),
            book.getCategories().stream().map(this::mapCategory).toList());
}
```

That code is correct but **tedious and error-prone**: every new field means editing every mapper, and
a transposed argument (`firstName`/`lastName` swapped) compiles cleanly and ships a bug. Across four
entities with request, response and summary DTOs, this is dozens of near-identical methods.

MapStruct replaces all of it. But we taught the manual version **first on purpose**. MapStruct is not
magic — it is a code generator, and the code it generates is *exactly the mapper you would have
written by hand*. Because you have written that mapper, you can open the generated file, read it, and
trust it. Nobody should adopt a tool whose output they cannot read.

### What an annotation processor is

An **annotation processor** is a plugin that runs **inside the Java compiler** (`javac`) during a
normal build. When you declare an interface annotated with `@Mapper`, MapStruct's processor:

1. Runs as part of `javac` — *at compile time*, not at runtime.
2. Reads your interface and the getters/setters of your DTOs and entities.
3. **Generates a plain Java source file** — a class implementing your interface — and hands it back to
   the compiler, which compiles it alongside your own code.

The consequences matter:

| Property | Meaning for you |
|----------|-----------------|
| Compile-time | Mapping mistakes (a field with no source) become **build errors**, not runtime surprises. |
| Generates plain Java | The output is an ordinary `.java` file you can open and read — no bytecode tricks. |
| **No runtime reflection** | The generated mapper is straight getter→setter calls. It is as fast as your hand-written code and adds no startup or per-call reflection cost. |

Contrast this with reflection-based mappers (e.g. classic ModelMapper): those inspect objects with
reflection *at runtime*, which is slower and pushes mismatches to request time. MapStruct front-loads
all that work to the compiler. This is the same family of tool you already use without thinking:
Lombok is also an annotation processor (it generates your getters/setters). Lab 5 simply adds a second
one — and Module 2 is about making the two cooperate.

---

## 2. Setting up MapStruct with Lombok on Spring Boot 4.0 / Java 21

MapStruct ships in two pieces, and getting the build wiring right is the whole game.

- **`org.mapstruct:mapstruct`** — the annotations (`@Mapper`, `@Mapping`, …) and a tiny API. This is a
  normal compile/runtime dependency because your interfaces reference the annotations.
- **`org.mapstruct:mapstruct-processor`** — the code generator. This runs only during compilation, so
  it belongs in the compiler plugin's `annotationProcessorPaths`, **not** in `<dependencies>`.

### The dependency

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
```

Lombok is already present (marked `<optional>true</optional>`), exactly as in the earlier labs.

### The processor wiring — and why the Lombok binding matters

Both Lombok and MapStruct are annotation processors, and they have a **dependency between them**:
MapStruct reads the getters and setters of your entities/DTOs to know how to map them — but on a
Lombok-annotated class *those getters and setters do not exist in your source*; Lombok generates them.
MapStruct therefore has to see the accessors Lombok produced. That coordination is exactly what
**`lombok-mapstruct-binding`** guarantees: it makes the two processors cooperate across javac's
annotation-processing *rounds*, so MapStruct picks up Lombok's generated members no matter how the
compiler interleaves the processors. Listing Lombok first in `annotationProcessorPaths` is the
conventional, safe ordering — but it is the **binding**, not the literal path order, that prevents
MapStruct from running against a not-yet-Lombok-ed class and emitting empty mappers (or failing).

You express that ordering in `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <!-- The lombok-mapstruct-binding makes the two processors
                 cooperate across compiler rounds so MapStruct sees the
                 Lombok-generated accessors. Listing Lombok first is the
                 conventional, safe ordering. -->
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

Three paths, in this exact order:

| Path | Version | Role |
|------|---------|------|
| `org.projectlombok:lombok` | (from the Boot BOM) | Generates getters/setters/constructors first. |
| `org.projectlombok:lombok-mapstruct-binding` | **0.2.0** | The glue that makes the two processors cooperate across compiler rounds. |
| `org.mapstruct:mapstruct-processor` | **1.6.3** | Reads the now-present accessors and generates the `*MapperImpl`. |

The **`lombok-mapstruct-binding`** is the piece students always forget. Without it, MapStruct and
Lombok can run in an order that leaves MapStruct unable to see Lombok's output, and you get mappers
that silently map nothing. The binding tells Lombok to announce when it is done so MapStruct picks up
the completed accessors. Version `0.2.0` is the one compatible with current Lombok + MapStruct 1.6.3.

> **Spring Boot 4.0 / Java 21 specifics.** The `mapstruct` and `mapstruct-processor` versions must
> **match** (`1.6.3` in both places). The Spring Boot parent BOM manages the Lombok version, so you
> pin only `lombok-mapstruct-binding` (`0.2.0`) and the MapStruct versions yourself. MapStruct 1.6.3 is
> validated on Java 21; nothing about records or the Boot 4.0 test slices changes this wiring.

### Verifying the build

After `./mvnw compile`, the generated sources appear under
`target/generated-sources/annotations/com/example/_5_mapstruct/mapper/`. If `BookMapperImpl.java` is
there and non-empty, the wiring is correct. If it is empty or missing, the processor ordering (or the
missing binding) is the first thing to check.

---

## 3. `@Mapper` components and Spring dependency injection

A MapStruct mapper is just an **interface** you declare; MapStruct generates the implementation. To
make that implementation an injectable Spring bean, you set `componentModel = "spring"`:

```java
@Mapper(componentModel = "spring")
public interface AuthorMapper {

    AuthorResponseDto toResponse(Author author);

    AuthorSummaryDto toSummary(Author author);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    Author toEntity(AuthorRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    void updateEntity(AuthorRequestDto dto, @MappingTarget Author author);
}
```

`componentModel = "spring"` makes the generated `AuthorMapperImpl` a `@Component`, so Spring puts it in
the context and you inject it like any other collaborator. The reference service does exactly that via
constructor injection (Lombok's `@RequiredArgsConstructor` over a `final` field):

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final AuthorMapper authorMapper;          // the generated bean, injected

    @Override
    @Transactional
    public AuthorResponseDto createAuthor(AuthorRequestDto requestDto) {
        Author author = authorMapper.toEntity(requestDto);
        return authorMapper.toResponse(authorRepository.save(author));
    }
    // ...
}
```

Compare this with the old code: the hand-written `mapToResponse`/`mapToEntity` private methods are
**gone**, replaced by `authorMapper.toResponse(...)` / `authorMapper.toEntity(...)`. The service got
shorter and the mapping got type-checked at build time.

### Reading the generated `*MapperImpl`

Open `target/generated-sources/annotations/.../mapper/AuthorMapperImpl.java` and you will find
demystified, hand-writable code — roughly:

```java
@Component
public class AuthorMapperImpl implements AuthorMapper {

    @Override
    public AuthorResponseDto toResponse(Author author) {
        if (author == null) {
            return null;
        }
        AuthorResponseDto dto = new AuthorResponseDto();
        dto.setId(author.getId());
        dto.setFirstName(author.getFirstName());
        dto.setLastName(author.getLastName());
        // ...
        return dto;
    }
    // toEntity(...) sets every field EXCEPT id and books (they were ignored)
}
```

That is it — null checks and getter→setter calls. No reflection, no proxies. This is the payoff of
Module 1: because you wrote this by hand before, the generated version holds no surprises.

> **Spring Boot 4.0 specifics.** `injectionStrategy` on `@Mapper` controls how a mapper that *uses*
> other mappers receives them. The reference `BookMapper` sets
> `injectionStrategy = InjectionStrategy.CONSTRUCTOR` so the composed mappers are constructor-injected
> (immutable, testable) rather than field-injected — matching the constructor-injection style the rest
> of the Bookstore uses.

---

## 4. Field mapping: `@Mapping`, ignore, expression, constant, and `@MappingTarget`

By default MapStruct maps a source property to a target property **of the same name**. When the names
differ, when a target has no source, or when you want a literal value, you steer it with `@Mapping`.

| `@Mapping` form | What it does |
|-----------------|--------------|
| `@Mapping(source = "x", target = "y")` | Map source property `x` onto differently-named target `y`. |
| `@Mapping(target = "y", ignore = true)` | Leave target `y` untouched (no source, or the service will set it). |
| `@Mapping(target = "y", expression = "java(...)")` | Set `y` from an inline Java expression. |
| `@Mapping(target = "y", constant = "…")` | Set `y` to a fixed literal value. |

### `ignore` — a field the mapper must not set

Every request→entity mapping in the Bookstore ignores the DB-generated `id` and the back-reference
collection, because neither comes from the request:

```java
@Mapping(target = "id", ignore = true)
@Mapping(target = "books", ignore = true)
Author toEntity(AuthorRequestDto dto);
```

Ignoring `id` is important: without it MapStruct would try to find a source for `id`, fail to (the
request DTO has none), and — depending on config — warn or leave it null anyway. Being explicit
documents intent and keeps the build clean.

### `expression` — an inline Java value

Use `expression` when the target value is neither a copied field nor a constant. The reference uses it
to force an **empty list** for the list view (explained fully in Module 5):

```java
@Mapping(target = "categories", expression = "java(java.util.List.of())")
BookResponseDto toListResponse(Book book);
```

The `java(...)` wrapper contains real Java; use **fully-qualified** names (`java.util.List.of()`) so
the generated file compiles without needing an import.

### `constant` — a fixed literal

`constant = "SOME_VALUE"` sets a target to a literal string (MapStruct converts it to the target type).
It is the right tool for stamping a fixed status/flag onto a mapping; `expression` is for anything that
needs computation.

### `@MappingTarget` — in-place update for PUT

A PUT handler must update an **existing, managed** entity, not build a new one — otherwise every field
the request omits gets nulled out. `@MappingTarget` tells MapStruct to **copy fields into an object you
pass in** rather than constructing a fresh one:

```java
@Mapping(target = "id", ignore = true)
@Mapping(target = "books", ignore = true)
void updateEntity(AuthorRequestDto dto, @MappingTarget Author author);
```

The method returns `void` and mutates the passed `author` in place. The service loads the managed
entity, applies the update, and saves — the JPA dirty-check turns it into an `UPDATE`:

```java
@Override
@Transactional
public AuthorResponseDto updateAuthor(Long id, AuthorRequestDto requestDto) {
    Author author = getAuthorEntity(id);              // managed entity from the DB
    authorMapper.updateEntity(requestDto, author);    // copy request fields onto it
    return authorMapper.toResponse(authorRepository.save(author));
}
```

Because `id` is ignored, the update can never overwrite the primary key; because the entity was loaded
first, its associations and audit fields survive the partial update.

---

## 5. Nested relationship mapping, and the deliberate asymmetry

`Book` is the interesting case: its response DTO nests three associations, and its request DTO carries
only **ids**. MapStruct handles the two directions very differently — on purpose.

### entity → response: compose the summary mappers

`BookResponseDto` nests `AuthorSummaryDto`, `PublisherSummaryDto`, and `List<CategorySummaryDto>`.
Rather than teach `BookMapper` how to build those, we let it **reuse** the other mappers via
`uses = {...}`:

```java
@Mapper(componentModel = "spring",
        uses = {AuthorMapper.class, PublisherMapper.class, CategoryMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface BookMapper {

    // Detail view: expands the fully-initialised categories collection.
    BookResponseDto toDetailResponse(Book book);
    // ...
}
```

When MapStruct maps `Book.author` (an `Author`) to `BookResponseDto.author` (an `AuthorSummaryDto`), it
looks through the `uses` mappers, finds `AuthorMapper.toSummary(Author)`, and calls it. The
`List<Category>` → `List<CategorySummaryDto>` is handled the same way — MapStruct generates the loop and
delegates each element to `CategoryMapper.toSummary`. You write **one line** (`toDetailResponse`) and
the whole nested tree is wired for you.

### request → entity: ignore every relationship

Now the asymmetry. `BookRequestDto` has `authorId`, `publisherId`, `categoryIds` — **ids, not
objects**. Turning an id into a managed `Author` is a *database lookup*, and MapStruct does not touch
the database. So the request→entity mapping **ignores every relationship** and the id:

```java
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
```

MapStruct maps only the scalar fields (`title`, `isbn`, `publicationYear`, `price`). The **service**
resolves the ids — exactly the Lab 3 responsibility, unchanged:

```java
@Override
@Transactional
public BookResponseDto createBook(BookRequestDto requestDto) {
    if (bookRepository.existsByIsbn(requestDto.getIsbn())) {
        throw new DuplicateResourceException("isbn", requestDto.getIsbn());
    }
    // MapStruct maps the scalar fields; the service still owns id-resolution
    // (authorId/publisherId/categoryIds -> managed entities).
    Book book = bookMapper.toEntity(requestDto);
    resolveRelations(book, requestDto);
    return bookMapper.toDetailResponse(bookRepository.save(book));
}

// Resolves related ids to managed entities and sets them on the book. This stays in the
// service on purpose: MapStruct only shapes objects, it never performs a database lookup.
private void resolveRelations(Book book, BookRequestDto dto) {
    Author author = authorRepository.findById(dto.getAuthorId())
            .orElseThrow(() -> new ResourceNotFoundException("Author", dto.getAuthorId()));
    Publisher publisher = publisherRepository.findById(dto.getPublisherId())
            .orElseThrow(() -> new ResourceNotFoundException("Publisher", dto.getPublisherId()));
    book.setAuthor(author);
    book.setPublisher(publisher);
    book.setCategories(resolveCategories(dto.getCategoryIds()));
}
```

**Why keep id-resolution in the service?** Because resolving an id must be able to *fail loudly* — an
unknown `authorId` returns `404 ResourceNotFoundException`, and only the repository/service can decide
that. A mapper that silently fabricated an `Author` with just an id set would defeat referential
integrity. Mapping shapes objects; resolution talks to the database. Keeping them separate is the
whole point.

### The empty-list subtlety (read this twice)

The list/paginated view must not fetch the `categories` collection while paginating (the Lab 3 rule).
So `toListResponse` **ignores** categories — but here is the trap: **an ignored MapStruct collection is
left `null`, not empty.** A `null` would serialise as `"categories": null`, breaking the Lab 3 JSON
contract, which is `"categories": []`. We fix it by forcing an empty list with `expression`:

```java
// List view: author + publisher come from the entity graph, but categories are NOT
// fetched while paginating (Lab 3 rule). MapStruct would leave the ignored collection
// null, so we force an empty list to keep the JSON contract ("categories": []).
@Mapping(target = "categories", expression = "java(java.util.List.of())")
BookResponseDto toListResponse(Book book);
```

So the two response methods differ by exactly this:

| Method | `categories` | Used by |
|--------|--------------|---------|
| `toDetailResponse` | fully mapped via `CategoryMapper` | `GET /api/books/{id}`, create, update |
| `toListResponse` | forced to `List.of()` (empty) | `GET /api/books`, search |

The service's paginated mapper uses the list variant, keeping the contract intact:

```java
private PagedResponse<BookResponseDto> toPagedResponse(Page<Book> page) {
    List<BookResponseDto> content = page.getContent().stream()
            .map(bookMapper::toListResponse).toList();
    return PagedResponse.from(page, content);
}
```

> **Spring Boot 4.0 specifics.** Jackson 3 (package `tools.jackson`) serialises an empty `List` as
> `[]` and a `null` as `null`, exactly as Jackson 2 did — so the `List.of()` trick is what preserves
> the byte-for-byte contract. Do not "fix" the null by making Jackson omit the field; the Lab 3
> response includes the empty array explicitly.

---

## 6. Spring Data interface-based projections

MapStruct solved *shaping* objects. It did **not** solve *over-fetching*: `getAllBooks` still loads
whole `Book` rows (every column, plus the joined author/publisher) only to throw most of it away when
building a summary. Projections fix that at the source — the query selects **only the columns the
result needs**.

The simplest kind is an **interface-based projection**: you declare an interface of getters, and Spring
Data returns dynamic proxies that expose just those values.

### Closed projections

A **closed** projection has getters that all map directly to real entity attributes. Because Spring
Data can see every getter is entity-backed, it tells the JPA provider to `SELECT` only those columns —
no full entity is built. The reference `BookSummaryView` also shows a projection can **nest an
association**:

```java
public interface BookSummaryView {

    Long getId();

    String getTitle();

    String getIsbn();

    Author getAuthor();

    /** Nested closed projection over the Book -> Author association. */
    interface Author {
        String getLastName();
    }
}
```

`getAuthor()` returns a *nested projection interface*, not the full `Author` entity — so the query
pulls only `author.last_name`, joined in. You wire it to the repository by returning the projection
type from a query method:

```java
// Lab 5: interface-based projection. Spring Data materialises only id, title, isbn and the
// author's last name — the JPA provider selects just those columns instead of full Book rows.
// Backs GET /api/books/summaries; the derived-query name drives an implicit join to Author.
Page<BookSummaryView> findAllProjectedBy(Pageable pageable);
```

`findAllProjectedBy` is a derived query name: `findAll...By` with the projection type as the return
element. Spring Data reads the getters of `BookSummaryView`, generates a `SELECT b.id, b.title,
b.isbn, a.last_name ...` with the join to `Author`, and returns a `Page` of proxies — never a `Book`.

### Open projections

A **closed** projection's getters must match attributes one-for-one. An **open** projection can compute
a value with SpEL via `@Value`:

```java
public interface BookNameView {
    @Value("#{target.title + ' (' + target.isbn + ')'}")
    String getLabel();
}
```

The catch: because an open projection's getter is an arbitrary SpEL expression, Spring Data **cannot**
know which columns it needs, so it falls back to fetching the **whole entity** and evaluating the
expression in memory. Open projections buy expressiveness at the cost of the over-fetch you were trying
to avoid — use closed projections when the goal is read optimization, and reach for open ones only when
you genuinely need a computed field.

### Why this revisits Lab 3

Lab 3 taught the list-vs-detail split: the list view intentionally omits the categories collection.
Projections take that one step further — the list view now stops materialising the full `Book` at all.
`GET /api/books` (full `BookResponseDto`) and `GET /api/books/summaries` (projection) sit side by side,
demonstrating the trade-off directly.

---

## 7. Class-based (record) and dynamic projections

Interface projections aren't the only kind. Two more complete the picture.

### Class-based (record) projections

A **class-based** projection is a concrete type — typically a `record` — populated by a JPQL
**constructor expression**. The reference uses a flat record:

```java
public record BookTitleIsbn(Long id, String title, String isbn) {
}
```

populated by `select new ...`:

```java
// Lab 5: class-based (record) projection via a JPQL constructor expression. The select list
// order must match the BookTitleIsbn canonical constructor (id, title, isbn).
@Query("select new com.example._5_mapstruct.projection.BookTitleIsbn(b.id, b.title, b.isbn) "
        + "from Book b where b.isbn = :isbn")
Optional<BookTitleIsbn> findTitleIsbnByIsbn(@Param("isbn") String isbn);
```

Two things to get right:

- **Fully-qualified class name** in the `new ...` expression.
- **Argument order must match the constructor exactly** — `(b.id, b.title, b.isbn)` lines up with the
  record's canonical constructor `(Long id, String title, String isbn)`. There is no name matching
  here; it is positional, so a swapped select column is a silent bug.

### The rule: class-based projections cannot nest associations

This is the key distinction to memorise:

| | Interface projection | Class-based (record) projection |
|---|---|---|
| Column selection | Automatic from getters (closed) | Explicit in the JPQL select list |
| **Nested associations** | **Yes** — a getter can return a nested projection interface | **No** — must be a flat DTO |
| How it's built | Spring Data proxy | JPQL `new` constructor call |

A `record` is constructed by one flat constructor call, so it cannot express "and here nest the
author's projected fields." That is why `BookSummaryView` (interface) can carry `getAuthor().getLastName()`
but `BookTitleIsbn` (record) is flat `id/title/isbn` only. When you need nested association fields in a
projection, use an **interface** projection.

### Dynamic projections

Sometimes one query should return **different shapes** for different callers. Instead of writing three
finders, you write **one** with the generic `<T> ... Class<T> type` signature and pass the target type:

```java
// Lab 5: dynamic projection. One method returns whichever shape the caller asks for by
// passing the target type (e.g. Book.class, BookSummaryView.class, BookTitleIsbn.class).
<T> Optional<T> findByIsbn(String isbn, Class<T> type);
```

The same method backs all of these:

```java
Optional<Book>           full    = bookRepository.findByIsbn(isbn, Book.class);
Optional<BookSummaryView> summary = bookRepository.findByIsbn(isbn, BookSummaryView.class);
Optional<BookTitleIsbn>  flat    = bookRepository.findByIsbn(isbn, BookTitleIsbn.class);
```

Spring Data inspects the `Class<T>` you pass and materialises accordingly — the full entity for
`Book.class`, a proxy for the interface projection, a constructor call for the record. One method
name, three read shapes, no duplication.

### Confirming the over-fetch win

Turn on SQL logging and compare. Against the full-entity query you will see `SELECT` list every book
column plus the joined author/publisher columns; against `findAllProjectedBy` you will see only
`b.id, b.title, b.isbn, a.last_name`. That narrower `SELECT` is the whole point — the projection query
reads fewer columns off disk and moves less data over the wire.

```properties
# application-dev.properties — show the generated SQL to compare fetched columns
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
```

---

## The new endpoint, and what stays frozen

The controller adds exactly one route and leaves everything else alone:

```java
// Lab 5: read-optimized list backed by an interface projection. Distinct from GET /api/books
// (which still returns full BookResponseDto). The literal "/summaries" path is matched before
// "/{id}", so it never collides with GET /api/books/{id}.
@GetMapping("/summaries")
public ResponseEntity<PagedResponse<BookSummaryView>> getSummaries(
        @PageableDefault(size = 20, sort = "title") Pageable pageable) {
    return ResponseEntity.ok(bookService.getBookSummaries(pageable));
}
```

backed by a service method that hands the projection page straight through (no MapStruct — a projection
*is* already the read shape):

```java
@Override
public PagedResponse<BookSummaryView> getBookSummaries(Pageable pageable) {
    Page<BookSummaryView> page = bookRepository.findAllProjectedBy(pageable);
    return PagedResponse.from(page, page.getContent());
}
```

The deliberate decision: **`GET /api/books` keeps the Lab 3 contract byte-for-byte** (full
`BookResponseDto`, `"categories": []` in the list view), while **`GET /api/books/summaries` is the new
surface that demonstrates projections**. Existing clients see no change; the projection is purely
additive. As with `/search` in Lab 4, the literal `/summaries` path is matched ahead of the `/{id}`
pattern, so the routes never collide.

---

## Testing notes (Spring Boot 4.0 packages)

The reference verifies Lab 5 across the usual slices. Mind the **renamed 4.0 test-slice packages** and
the Jackson 3 import:

```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;   // repository/projection slice
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;  // seeding entities
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;      // controller slice
import tools.jackson.databind.ObjectMapper;                                // Jackson 3 (NOT com.fasterxml)
```

- **Mapper behaviour** is exercised through the service and web tests rather than in isolation — the
  generated `*MapperImpl` is real code, so the same `@WebMvcTest` that covered Lab 3 now proves the
  MapStruct output JSON **byte-for-byte matches** the old manual mapper (the contract must not change).
  Assert `"categories": []` on the list view and the fully-expanded categories on the detail view.
- **`@MappingTarget` update** is covered by the PUT service test: send a request that omits nothing but
  changes some fields, and assert untouched fields (and the `id`) survive — partial updates must not
  null out columns.
- **`@DataJpaTest`** runs the projection queries against H2: `findAllProjectedBy` returns a `Page` of
  `BookSummaryView` with the nested `getAuthor().getLastName()` populated; `findTitleIsbnByIsbn` returns
  the flat record with fields in the right positions; and the dynamic `findByIsbn(isbn, type)` returns
  each requested shape.
- **`@WebMvcTest`** asserts `GET /api/books/summaries` returns a paged body of summaries and that
  `GET /api/books` is unchanged from Lab 3.

---

## Checklist: you are ready to build Lab 5 when you can...

- [ ] Explain what an annotation processor is — it runs inside `javac`, generates plain Java source, and adds **no runtime reflection** — and why writing mappers by hand first lets you trust the generated code.
- [ ] Add `org.mapstruct:mapstruct:1.6.3` as a dependency and the `mapstruct-processor` (1.6.3), `lombok`, and `lombok-mapstruct-binding` (0.2.0) entries to `annotationProcessorPaths`.
- [ ] State why **Lombok must be ordered before MapStruct** (its getters/setters must exist before MapStruct inspects the beans) and what the `lombok-mapstruct-binding` is for.
- [ ] Declare a `@Mapper(componentModel = "spring")` interface, inject the generated bean into a service, and find the `*MapperImpl` under `target/generated-sources`.
- [ ] Use `@Mapping` with `ignore`, `expression`, and `constant`, and know when each applies.
- [ ] Write an `@MappingTarget` update method and explain why PUT must mutate a loaded managed entity instead of building a new one (so omitted fields aren't nulled).
- [ ] Map `Book → BookResponseDto` with nested author/publisher/categories by composing summary mappers via `uses = {...}`.
- [ ] Explain the request→entity asymmetry: relationships are `@Mapping(ignore = true)` because id-resolution is a repository job the service owns, not the mapper's.
- [ ] Explain the empty-list subtlety — an ignored MapStruct collection is **null**, so the list view forces `List.of()` to keep `"categories": []`.
- [ ] Define a closed interface-based projection with a nested association getter, and contrast it with an open (`@Value` SpEL) projection that re-fetches the whole entity.
- [ ] Write a class-based (record) projection with a JPQL constructor expression (positional argument order) and a dynamic `<T> ... Class<T>` finder.
- [ ] State the rule: class-based projections **cannot** nest associations; interface-based ones can.
- [ ] Explain why `GET /api/books` stays byte-for-byte identical while `GET /api/books/summaries` is the new projection-backed endpoint.
