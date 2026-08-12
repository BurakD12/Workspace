# Lab 03 — Bookstore API (Entity Relationships & Advanced JPA)

> **Hand this single document to the student.** It is fully self-contained: it specifies *what* to
> build (the requirements, the exact API contract, and a **class-by-class implementation checklist**
> in Section 6) and gives *guidance* on *how* to approach the genuinely new concepts. A student can
> implement the entire project from this file alone.

---

## 1. Problem Statement & Goal

Build the backend REST API for a small online **bookstore**. The system manages **authors**,
**publishers**, **categories**, and **books**, and the relationships between them. Unlike a simple
single-table CRUD app, this project is about **modelling related data correctly** and **querying it
efficiently**.

You will deliver a Spring Boot application that exposes a JSON REST API matching the **API Contract**
in Section 13, backed by a PostgreSQL database, with automated tests at three layers.

## 2. Learning Objectives (what this assesses)

1. Model `@OneToMany`, `@ManyToOne`, and `@ManyToMany` relationships — owning vs. inverse side,
   `mappedBy`, `@JoinColumn`, `@JoinTable`.
2. Use cascade types deliberately, and explain `orphanRemoval`.
3. Explain fetch types (LAZY vs. EAGER) and recognise the **N+1 query problem**.
4. Fix N+1 with `JOIN FETCH` and `@EntityGraph`.
5. Write a custom `@Query` (JPQL).
6. Implement **pagination and sorting** with `Pageable` / `Page`.
7. Apply `@Transactional` correctly (read-only vs. read-write).
8. Build a clean layered architecture with DTO mapping, bean validation, correct HTTP semantics, and
   `ProblemDetail` error handling.
9. Write `@DataJpaTest`, Mockito service, and `@WebMvcTest` tests.

**Assumed background (helpful but not required):** basic Spring Boot CRUD, the
`controller → service → repository → entity` layering, DTOs, `@Valid` bean validation, returning
`ResponseEntity` with deliberate status codes, and a `@RestControllerAdvice` exception handler. This
document re-states what you need, so you can build it even if those are rusty.

## 3. The One Big New Idea: Data Has Relationships

A single table can only describe one kind of thing. As soon as you have *authors* and *books*, you
need to express "this book was written by that author" — a **relationship**. JPA models three kinds,
and you will use all of them:

- **Many-to-one / one-to-many** — each book has one author; one author has many books.
- **Many-to-many** — a book belongs to many categories; a category contains many books.

The data model:

```
Author (1) ───< (N) Book (N) >─── (1) Publisher
                     │
                     │ (N) >───< (N)
                     ▼
                 Category
```

The hardest ideas here are not the annotations themselves but **owning vs. inverse side**, **fetch
strategy / the N+1 problem**, and **keeping both sides of a bidirectional relationship in sync**.
Sections 7 and 8 cover these directly.

## 4. Business Scenario

- An **Author** has a first name, last name, and an optional bio. An author writes many **Books**.
- A **Publisher** has a unique name and a country. A publisher publishes many **Books**.
- A **Book** has a title, a unique 13-digit **ISBN**, a publication year, a price, exactly one author,
  exactly one publisher, and zero or more **Categories**.
- A **Category** has a unique name and contains many **Books**.

Three unique fields create real `409 Conflict` scenarios (`Book.isbn`, `Publisher.name`,
`Category.name`), and two protected relationships create a new kind of conflict (Section 14).

## 5. Tech Stack & Project Setup (required)

- **Java 21**, **Maven**, **Spring Boot 4.0.x**.
- **PostgreSQL** at runtime; create a database named `bookstore_lab` (local credentials
  `postgres` / `123456`, or adjust to your environment).
- **H2** (test scope) for the tests.
- **Lombok** is allowed and encouraged.
- Base package `com.example._3_advanced`, with packages `model`, `dto`, `repository`, `service`,
  `controller`, `exception`.
- `application.properties`: configure the PostgreSQL datasource, `spring.jpa.hibernate.ddl-auto=update`,
  `spring.jpa.show-sql=true`, `spring.jpa.properties.hibernate.format_sql=true`.

### 5.1 Spring Boot 4.0 specifics you MUST know (or you will get stuck)

Spring Boot 4.0 reorganised some things versus 3.x:

- **Test slices moved into dedicated starters** (and `spring-boot-starter-test` no longer brings them
  transitively — each test starter brings it instead). Add these test dependencies:

  ```xml
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>   <!-- @DataJpaTest, TestEntityManager -->
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>     <!-- @WebMvcTest, @AutoConfigureMockMvc -->
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
  </dependency>
  ```

- **The test-slice imports are renamed** (these are NOT the 3.x packages — see Section 16.1):
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
  `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`,
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.
- **Jackson 3**: the `ObjectMapper` is `tools.jackson.databind.ObjectMapper` (not `com.fasterxml...`).

> If you see *"package `org.springframework.boot.test.autoconfigure.orm.jpa` does not exist"*, that is
> the old 3.x package. The slices were relocated, not removed. Do **not** work around it by switching
> to `@SpringBootTest`.

## 6. Complete Implementation Inventory (class-by-class checklist)

This section lists **every Java class you must create**. Use it as your build checklist. Sections 7–16
explain the hard concepts; this section tells you **what each class is for and what it must do** — not
how to code it (that is your job).

### 6.1 Bootstrap

| Class | Package | What you must do |
| ----- | ------- | ---------------- |
| **`Application`** | root (`com.example._3_advanced`) | Standard Spring Boot entry point with `@SpringBootApplication` and a `main` method that starts the app. |

### 6.2 `model` package — four JPA entities

Every entity carries the class-level annotations `@Entity`, `@Table(name = "…")`, and Lombok
`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` (so each entity has a **no-arg
constructor**, an **all-args constructor**, and a **builder** — you do not write these by hand).
`Category` and `Book` additionally carry `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`.
Do **not** put bean-validation annotations on entities — those belong on request DTOs (Section 9). Do
**not** use `@Data` or `@ToString` on entities with relationships (StackOverflow risk).

The tables below list **every attribute** (name, Java type, and the JPA/Lombok annotations it carries)
and **every method** you must add by hand. Getters/setters/constructors/builders are generated by
Lombok and are not repeated.

#### `Author` — `@Table(name = "authors")`

| Attribute | Type | Annotations / notes |
| --------- | ---- | ------------------- |
| `id` | `Long` | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `firstName` | `String` | `@Column(nullable = false)` |
| `lastName` | `String` | `@Column(nullable = false)` |
| `bio` | `String` | `@Column(length = 2000)` (optional) |
| `books` | `List<Book>` | inverse side: `@OneToMany(mappedBy = "author", cascade = {CascadeType.PERSIST, CascadeType.MERGE})`, `@Builder.Default`, initialised to `new ArrayList<>()` |

Methods to implement by hand:

- `public void addBook(Book book)` — add to `books` **and** call `book.setAuthor(this)`.
- `public void removeBook(Book book)` — remove from `books` **and** call `book.setAuthor(null)`.

#### `Publisher` — `@Table(name = "publishers")`

| Attribute | Type | Annotations / notes |
| --------- | ---- | ------------------- |
| `id` | `Long` | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `name` | `String` | `@Column(nullable = false, unique = true)` |
| `country` | `String` | `@Column(nullable = false)` |
| `books` | `List<Book>` | inverse side: `@OneToMany(mappedBy = "publisher", cascade = {CascadeType.PERSIST, CascadeType.MERGE})`, `@Builder.Default`, initialised to `new ArrayList<>()` |

Methods to implement by hand:

- `public void addBook(Book book)` — add to `books` **and** call `book.setPublisher(this)`.
- `public void removeBook(Book book)` — remove from `books` **and** call `book.setPublisher(null)`.

#### `Category` — `@Table(name = "categories")`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`

| Attribute | Type | Annotations / notes |
| --------- | ---- | ------------------- |
| `id` | `Long` | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `name` | `String` | `@Column(nullable = false, unique = true)`, `@EqualsAndHashCode.Include` (business key) |
| `books` | `Set<Book>` | inverse side: `@ManyToMany(mappedBy = "categories")`, `@Builder.Default`, initialised to `new HashSet<>()` |

No hand-written methods required (it is the inverse side of the many-to-many; sync helpers live on
`Book`).

#### `Book` — `@Table(name = "books")`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`

| Attribute | Type | Annotations / notes |
| --------- | ---- | ------------------- |
| `id` | `Long` | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `title` | `String` | `@Column(nullable = false)` |
| `isbn` | `String` | `@Column(nullable = false, unique = true)`, `@EqualsAndHashCode.Include` (business key) |
| `publicationYear` | `Integer` | `@Column(nullable = false)` |
| `price` | `BigDecimal` | `@Column(nullable = false, precision = 10, scale = 2)` |
| `author` | `Author` | owning side: `@ManyToOne(fetch = FetchType.LAZY, optional = false)`, `@JoinColumn(name = "author_id", nullable = false)` |
| `publisher` | `Publisher` | owning side: `@ManyToOne(fetch = FetchType.LAZY, optional = false)`, `@JoinColumn(name = "publisher_id", nullable = false)` |
| `categories` | `Set<Category>` | owning side: `@ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})`, `@JoinTable(name = "book_category", joinColumns = @JoinColumn(name = "book_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))`, `@Builder.Default`, initialised to `new HashSet<>()` |

Methods to implement by hand:

- `public void addCategory(Category category)` — add to `categories` **and** `category.getBooks().add(this)`.
- `public void removeCategory(Category category)` — remove from `categories` **and** `category.getBooks().remove(this)`.

Relationship ownership recap (Section 7 goes deeper): `Book` owns author, publisher, and category
links; `Author`, `Publisher`, and `Category` declare the inverse side with `mappedBy`.

### 6.3 `repository` package — four Spring Data interfaces

Each is a `public interface` extending `JpaRepository<Entity, Long>` (gives you `save`, `findById`,
`findAll`, `existsById`, `deleteById`, `delete`, … for free) and is picked up as a Spring bean
automatically. The exact method signatures you must declare on top of the inherited CRUD:

**`AuthorRepository extends JpaRepository<Author, Long>`** — no extra methods.

**`PublisherRepository extends JpaRepository<Publisher, Long>`**

- `boolean existsByNameIgnoreCase(String name);`

**`CategoryRepository extends JpaRepository<Category, Long>`**

- `boolean existsByNameIgnoreCase(String name);`
- `Optional<Category> findByNameIgnoreCase(String name);` (optional, only if you use it)

**`BookRepository extends JpaRepository<Book, Long>`**

| Method signature | Annotation | Purpose |
| ---------------- | ---------- | ------- |
| `boolean existsByIsbn(String isbn);` | — | duplicate-ISBN check |
| `boolean existsByAuthorId(Long authorId);` | — | protected delete of an author |
| `boolean existsByPublisherId(Long publisherId);` | — | protected delete of a publisher |
| `Page<Book> findAll(Pageable pageable);` | `@Override`, `@EntityGraph(attributePaths = {"author", "publisher"})` | paginated list, no collection fetch |
| `Page<Book> findByAuthorId(Long authorId, Pageable pageable);` | `@EntityGraph(attributePaths = {"author", "publisher"})` | books of one author |
| `Page<Book> findByPublisherId(Long publisherId, Pageable pageable);` | `@EntityGraph(attributePaths = {"author", "publisher"})` | books of one publisher |
| `Page<Book> findByCategoriesId(Long categoryId, Pageable pageable);` | `@EntityGraph(attributePaths = {"author", "publisher"})` | books in one category |
| `Optional<Book> findDetailedById(Long id);` | `@EntityGraph(attributePaths = {"author", "publisher", "categories"})` + `@Query("select b from Book b where b.id = :id")` with `@Param("id")` | single-book detail (includes categories) |
| `List<Book> findAllWithAuthorAndPublisher();` | `@Query("select b from Book b join fetch b.author join fetch b.publisher")` | teaching/test method proving the N+1 `JOIN FETCH` fix |

### 6.4 `dto` package — twelve classes

Never expose entities through the API. Map manually in services (no MapStruct). All DTOs are plain
classes with Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` (add `@Builder` where the
tests use a builder — `BookRequestDto` does). Each table below lists **every field with its exact type**
and the validation annotation it carries (request DTOs only).

**`AuthorRequestDto`**

| Field | Type | Validation |
| ----- | ---- | ---------- |
| `firstName` | `String` | `@NotBlank(message = "First name is required")` |
| `lastName` | `String` | `@NotBlank(message = "Last name is required")` |
| `bio` | `String` | `@Size(max = 2000, message = "Bio must be at most 2000 characters")` |

**`AuthorResponseDto`** — fields: `Long id`, `String firstName`, `String lastName`, `String bio`.

**`PublisherRequestDto`**

| Field | Type | Validation |
| ----- | ---- | ---------- |
| `name` | `String` | `@NotBlank(message = "Name is required")` |
| `country` | `String` | `@NotBlank(message = "Country is required")` |

**`PublisherResponseDto`** — fields: `Long id`, `String name`, `String country`.

**`CategoryRequestDto`**

| Field | Type | Validation |
| ----- | ---- | ---------- |
| `name` | `String` | `@NotBlank(message = "Name is required")` |

**`CategoryResponseDto`** — fields: `Long id`, `String name`.

**`BookRequestDto`** (use `@Builder` too — the tests build it)

| Field | Type | Validation |
| ----- | ---- | ---------- |
| `title` | `String` | `@NotBlank(message = "Title is required")` |
| `isbn` | `String` | `@NotBlank(...)` + `@Pattern(regexp = "\\d{13}", message = "ISBN must be 13 digits")` |
| `publicationYear` | `Integer` | `@NotNull(...)` + `@Min(1450)` + `@Max(2100)` |
| `price` | `BigDecimal` | `@NotNull(...)` + `@DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")` |
| `authorId` | `Long` | `@NotNull(message = "Author id is required")` |
| `publisherId` | `Long` | `@NotNull(message = "Publisher id is required")` |
| `categoryIds` | `List<Long>` | optional; `@Builder.Default` initialised to `new ArrayList<>()` |

**`BookResponseDto`** — fields: `Long id`, `String title`, `String isbn`, `Integer publicationYear`,
`BigDecimal price`, `AuthorSummaryDto author`, `PublisherSummaryDto publisher`,
`List<CategorySummaryDto> categories`.

**`AuthorSummaryDto`** — fields: `Long id`, `String firstName`, `String lastName` (no back-reference).

**`PublisherSummaryDto`** — fields: `Long id`, `String name`.

**`CategorySummaryDto`** — fields: `Long id`, `String name`.

**`PagedResponse<T>`** (generic wrapper, so the API never serializes Spring's raw `Page`)

| Field | Type |
| ----- | ---- |
| `content` | `List<T>` |
| `page` | `int` |
| `size` | `int` |
| `totalElements` | `long` |
| `totalPages` | `int` |
| `last` | `boolean` |

Plus a **static factory**:
`public static <E, T> PagedResponse<T> from(Page<E> page, List<T> content)` — builds the wrapper by
copying `page.getNumber()`, `page.getSize()`, `page.getTotalElements()`, `page.getTotalPages()`,
`page.isLast()` alongside the already-mapped `content`.

### 6.5 `exception` package — four classes

The three custom exceptions all `extend RuntimeException` (unchecked). Their constructors build the
message string that the handler surfaces in the `ProblemDetail.detail` field (see Section 13.8).

**`ResourceNotFoundException extends RuntimeException`**

- `public ResourceNotFoundException(String resource, Long id)` → `super(resource + " not found with id: " + id)`.

**`DuplicateResourceException extends RuntimeException`**

- `public DuplicateResourceException(String field, String value)` → `super(field + " already in use: " + value)`.

**`RelationshipConflictException extends RuntimeException`**

- `public RelationshipConflictException(String message)` → `super(message)` (free-form text for protected deletes).

**`GlobalExceptionHandler`** — a single class annotated `@RestControllerAdvice`. Each handler method is
annotated `@ExceptionHandler(...)` and returns a `ProblemDetail` built with
`ProblemDetail.forStatusAndDetail(status, detail)` plus `setTitle(...)`:

| Handler method | Handles | Status | `title` | Extra |
| -------------- | ------- | ------ | ------- | ----- |
| `handleNotFound` | `ResourceNotFoundException` | `404` | `"Resource not found"` | — |
| `handleDuplicate` | `DuplicateResourceException` | `409` | `"Duplicate value"` | — |
| `handleRelationshipConflict` | `RelationshipConflictException` | `409` | `"Relationship conflict"` | — |
| `handleValidation` | `MethodArgumentNotValidException` | `400` | `"Validation failed"` | add `errors` property: a `Map<String,String>` of field → message |
| `handleTypeMismatch` | `MethodArgumentTypeMismatchException` | `400` | `"Invalid parameter"` | non-numeric path id |
| `handleUnexpected` | `Exception` | `500` | `"Internal server error"` | generic safety net, never leak the stack trace |

### 6.6 `service` package — four interfaces + four implementations

Each aggregate has a **`XService` interface** and a **`XServiceImpl`** class. Every `*ServiceImpl` is
annotated `@Service`, `@RequiredArgsConstructor` (constructor injection of the `final` repository
fields), and class-level `@Transactional(readOnly = true)`; put `@Transactional` (read-write) on each
create/update/delete method. Keep entity↔DTO mapping and repeated lookup logic in **private helpers**.

#### `AuthorService` / `AuthorServiceImpl`

Interface methods (exact signatures):

- `AuthorResponseDto createAuthor(AuthorRequestDto requestDto);`
- `List<AuthorResponseDto> getAllAuthors();`
- `AuthorResponseDto getAuthorById(Long id);`
- `AuthorResponseDto updateAuthor(Long id, AuthorRequestDto requestDto);`
- `void deleteAuthor(Long id);`

Impl dependencies (inject via `@RequiredArgsConstructor`): `AuthorRepository`, `BookRepository`.

| Method | What it must do |
| ------ | --------------- |
| `createAuthor` | Build entity from request DTO, save, return response DTO. |
| `getAllAuthors` | Load all, map each to response DTO. |
| `getAuthorById` | Find by id or throw `ResourceNotFoundException`; map to response. |
| `updateAuthor` | Load (404 if missing), copy fields from request, save, return response. |
| `deleteAuthor` | Load author; if **`bookRepository.existsByAuthorId(id)`** throw **`RelationshipConflictException`**; otherwise delete. |

#### `PublisherService` / `PublisherServiceImpl`

Interface methods:

- `PublisherResponseDto createPublisher(PublisherRequestDto requestDto);`
- `List<PublisherResponseDto> getAllPublishers();`
- `PublisherResponseDto getPublisherById(Long id);`
- `PublisherResponseDto updatePublisher(Long id, PublisherRequestDto requestDto);`
- `void deletePublisher(Long id);`

Impl dependencies: `PublisherRepository`, `BookRepository`.

| Method | What it must do |
| ------ | --------------- |
| `createPublisher` / `updatePublisher` | Before save, check **`existsByNameIgnoreCase`**; on duplicate throw **`DuplicateResourceException`** (on update, skip the check if the name did not change). |
| `getAllPublishers` / `getPublisherById` | List, or load one with `404` when missing. |
| `deletePublisher` | Load (404 if missing); if **`existsByPublisherId`** throw **`RelationshipConflictException`**; otherwise delete. |

#### `CategoryService` / `CategoryServiceImpl`

Interface methods:

- `CategoryResponseDto createCategory(CategoryRequestDto requestDto);`
- `List<CategoryResponseDto> getAllCategories();`
- `CategoryResponseDto getCategoryById(Long id);`
- `CategoryResponseDto updateCategory(Long id, CategoryRequestDto requestDto);`
- `void deleteCategory(Long id);`

Impl dependencies: `CategoryRepository`.

| Method | What it must do |
| ------ | --------------- |
| `createCategory` / `updateCategory` | Duplicate **`name`** check via **`existsByNameIgnoreCase`** → `409` (skip on update if unchanged). |
| `getAllCategories` / `getCategoryById` | List, or load one with `404` when missing. |
| `deleteCategory` | Load (404 if missing); **detach from every linked book first** — iterate `category.getBooks()`, remove this category from each book's `categories` set (`Book` owns the join table), clear the inverse set, then delete the category. |

#### `BookService` / `BookServiceImpl`

Interface methods:

- `BookResponseDto createBook(BookRequestDto requestDto);`
- `PagedResponse<BookResponseDto> getAllBooks(Pageable pageable);`
- `PagedResponse<BookResponseDto> getBooksByCategory(Long categoryId, Pageable pageable);`
- `PagedResponse<BookResponseDto> getBooksByAuthor(Long authorId, Pageable pageable);`
- `PagedResponse<BookResponseDto> getBooksByPublisher(Long publisherId, Pageable pageable);`
- `BookResponseDto getBookById(Long id);`
- `BookResponseDto updateBook(Long id, BookRequestDto requestDto);`
- `void deleteBook(Long id);`

Impl dependencies (all four): `BookRepository`, `AuthorRepository`, `PublisherRepository`,
`CategoryRepository`.

| Method | What it must do |
| ------ | --------------- |
| `createBook` | Reject duplicate ISBN via **`existsByIsbn`** → `DuplicateResourceException`. Resolve `authorId`, `publisherId`, each `categoryId` to entities (`404` if missing). Set book scalars and relationships, save, return **detail** response (with categories). |
| `getAllBooks` | Call `findAll(pageable)`, map each book with **list mapping** (author + publisher summaries, **empty categories list**), wrap in `PagedResponse`. |
| `getBooksByCategory` | Verify category exists (`404` if not), then `findByCategoriesId`, list mapping. |
| `getBooksByAuthor` | Verify author exists (`404` if not), then `findByAuthorId`, list mapping. Used by `AuthorController`. |
| `getBooksByPublisher` | Verify publisher exists (`404` if not), then `findByPublisherId`, list mapping. Used by `PublisherController`. |
| `getBookById` | Load via **`findDetailedById`** (404 if missing), map with **detail mapping** (includes categories). |
| `updateBook` | Load via `findDetailedById` (404 if missing); duplicate ISBN check only when ISBN changed; re-apply request (re-resolve related ids); save; detail mapping. |
| `deleteBook` | `404` if missing; otherwise delete by id. |

Private helper methods to add inside `BookServiceImpl` (signatures are suggestions — names are yours):

- `private void applyRequest(Book book, BookRequestDto dto)` — resolve ids, copy scalars + relations.
- `private Set<Category> resolveCategories(List<Long> categoryIds)` — id → entity, `404` if any missing.
- `private BookResponseDto mapToListResponse(Book book)` — author + publisher summaries, **empty** categories.
- `private BookResponseDto mapToDetailResponse(Book book)` — includes the categories collection.
- `private PagedResponse<BookResponseDto> toPagedResponse(Page<Book> page)` — map content + wrap (Section 8.4).

### 6.7 `controller` package — four REST controllers

Each controller: `@RestController`, `@RequestMapping("/api/…")`, `@RequiredArgsConstructor` (inject the
`final` service field(s)). Writes take `@Valid @RequestBody`. Every handler returns a
**`ResponseEntity<…>`**. **`POST` → `201`** with a **`Location`** header built from
`ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")…`; **`DELETE` → `204`** via
`ResponseEntity.noContent()`. Delegate all logic to services — controllers must not touch repositories
or entities. List handlers bind a `Pageable` with `@PageableDefault(size = 20, sort = "title")`.

**`AuthorController`** — `@RequestMapping("/api/authors")`, injects `AuthorService` **and** `BookService`.

| Handler signature | Mapping | Returns |
| ----------------- | ------- | ------- |
| `create(@Valid @RequestBody AuthorRequestDto dto)` | `@PostMapping` | `201` + `Location`, `AuthorResponseDto` |
| `getAll()` | `@GetMapping` | `200`, `List<AuthorResponseDto>` |
| `getById(@PathVariable Long id)` | `@GetMapping("/{id}")` | `200`, `AuthorResponseDto` |
| `update(@PathVariable Long id, @Valid @RequestBody AuthorRequestDto dto)` | `@PutMapping("/{id}")` | `200`, `AuthorResponseDto` |
| `delete(@PathVariable Long id)` | `@DeleteMapping("/{id}")` | `204` |
| `getBooks(@PathVariable Long id, @PageableDefault(...) Pageable pageable)` | `@GetMapping("/{id}/books")` | `200`, `PagedResponse<BookResponseDto>` (via `bookService.getBooksByAuthor`) |

**`PublisherController`** — `@RequestMapping("/api/publishers")`, injects `PublisherService` **and**
`BookService`. Same six handlers as `AuthorController` with `PublisherRequestDto`/`PublisherResponseDto`,
and `getBooks` delegates to `bookService.getBooksByPublisher`.

**`CategoryController`** — `@RequestMapping("/api/categories")`, injects `CategoryService`. Five handlers:
`create`, `getAll`, `getById`, `update`, `delete` (no nested books endpoint), using
`CategoryRequestDto`/`CategoryResponseDto`.

**`BookController`** — `@RequestMapping("/api/books")`, injects `BookService`.

| Handler signature | Mapping | Returns |
| ----------------- | ------- | ------- |
| `create(@Valid @RequestBody BookRequestDto dto)` | `@PostMapping` | `201` + `Location`, `BookResponseDto` |
| `getAll(@RequestParam(required = false) Long categoryId, @PageableDefault(...) Pageable pageable)` | `@GetMapping` | `200`, `PagedResponse<BookResponseDto>` — if `categoryId` present, filter by category, else all |
| `getById(@PathVariable Long id)` | `@GetMapping("/{id}")` | `200`, `BookResponseDto` (with categories) |
| `update(@PathVariable Long id, @Valid @RequestBody BookRequestDto dto)` | `@PutMapping("/{id}")` | `200`, `BookResponseDto` |
| `delete(@PathVariable Long id)` | `@DeleteMapping("/{id}")` | `204` |

Exact status codes and JSON shapes: Section 13.

### 6.8 Test classes — ten test classes

All must pass with **`mvn test`** using H2 (Section 16 for Spring Boot 4.0 import packages).

| Class | Layer | What you must verify |
| ----- | ----- | -------------------- |
| **`EntityMappingTest`** | `@DataJpaTest` | Persist a fully linked `Book` (author, publisher, two categories), clear the persistence context, reload, and assert every relationship round-trips including the inverse `Author.books` collection. |
| **`BookRepositoryTest`** | `@DataJpaTest` | Seed data via `TestEntityManager`. Tests for: `existsByIsbn`; `findByAuthorId` pagination/filtering; `findDetailedById` initialises `categories` (use `PersistenceUnitUtil.isLoaded`); `findAllWithAuthorAndPublisher` initialises author + publisher; join table `book_category` persists and reloads. |
| **`AuthorServiceImplTest`** | Mockito | Mock `AuthorRepository` + `BookRepository`. Cover: `404` on missing id; **`RelationshipConflictException`** when deleting an author who still has books. |
| **`PublisherServiceImplTest`** | Mockito | Cover duplicate name `409` on create/update and protective delete `409`. |
| **`CategoryServiceImplTest`** | Mockito | Cover duplicate name `409` and that delete detaches from books (mock category with linked books if needed). |
| **`BookServiceImplTest`** | Mockito | Cover: duplicate ISBN `409`; unknown author/publisher/category id `404`; list mapping leaves categories empty; detail mapping includes categories. |
| **`AuthorControllerTest`** | `@WebMvcTest` | Mock services with `@MockitoBean`. Assert status codes, `Location` header on create, `ProblemDetail` JSON on errors, paged shape on `GET /{id}/books`. |
| **`PublisherControllerTest`** | `@WebMvcTest` | Same web-layer checks for publisher endpoints including duplicate `409` and nested books. |
| **`CategoryControllerTest`** | `@WebMvcTest` | CRUD status codes, validation `400`, duplicate `409`. |
| **`BookControllerTest`** | `@WebMvcTest` | Full contract checks: create `201` + nested JSON + `Location`; validation `400` with `$.errors.*`; duplicate `404`/`409`; paged list shape (`$.content`, `$.totalElements`, `$.last`); non-numeric id → `400`. Use Jackson 3 `ObjectMapper` (Section 16.1). |

### 6.9 Suggested file count

| Package | Files |
| ------- | ----- |
| root | 1 (`Application`) |
| `model` | 4 entities |
| `repository` | 4 interfaces |
| `dto` | 12 classes |
| `exception` | 4 classes |
| `service` | 4 interfaces + 4 implementations |
| `controller` | 4 classes |
| `test` | 10 test classes |
| **Total** | **47 Java source files** (plus `application.properties` and `pom.xml`) |

---

## 7. Data Model & Entities (required + guidance)

Implement four JPA entities in `model`. Use Lombok
(`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`). Validation annotations do **not**
belong on entities — they live on the request DTOs (Section 9).

### 7.1 Fields & constraints

**`Author`** — `id` (Long, generated PK), `firstName` (required), `lastName` (required),
`bio` (optional, ≤ 2000 chars). Has many books (one-to-many).

**`Publisher`** — `id` (PK), `name` (required, **unique**), `country` (required). Has many books.

**`Category`** — `id` (PK), `name` (required, **unique**). Has many books (many-to-many).

**`Book`** — `id` (PK), `title` (required), `isbn` (required, **unique**, 13 digits),
`publicationYear` (Integer, 1450–2100), `price` (BigDecimal, > 0), `author` (`@ManyToOne`, required),
`publisher` (`@ManyToOne`, required), `categories` (`Set<Category>`, many-to-many via a
`book_category` join table).

### 7.2 Owning side vs. inverse side

Every bidirectional relationship has an **owning side** (holds the FK / join table; JPA looks at it
when deciding what to write) and an **inverse side** (declares `mappedBy`; read-only for the link).

- `Author (1) ──< (N) Book`: **`Book` is the owning side** (holds `author_id` via `@ManyToOne` +
  `@JoinColumn`). `Author` is inverse (`@OneToMany(mappedBy = "author")`). `Publisher` is analogous.
- `Book (N) >──< (N) Category`: **`Book` is the owning side** (declares the `@JoinTable`).
  `Category` is inverse (`@ManyToMany(mappedBy = "categories")`).
- All `@ManyToOne` / `@ManyToMany` are **`FetchType.LAZY`**.

### 7.3 Relationship annotations — what goes where

On **`Book`** (owning side of all three associations):

- **`author`**: `@ManyToOne` (LAZY, required) with `@JoinColumn(name = "author_id", nullable = false)`.
- **`publisher`**: same pattern with `publisher_id`.
- **`categories`**: `@ManyToMany` (LAZY, cascade `{PERSIST, MERGE}`) with `@JoinTable(name = "book_category", …)` naming both join columns. Initialise to an empty `HashSet` (use `@Builder.Default` if you use `@Builder`).

On **`Author`** and **`Publisher`** (inverse one-to-many):

- Declare `@OneToMany(mappedBy = "…")` pointing at the field name on `Book`, cascade `{PERSIST, MERGE}` only, initialised collection.

On **`Category`** (inverse many-to-many):

- Declare `@ManyToMany(mappedBy = "categories")`, initialised `Set`.

### 7.4 Bidirectional sync helpers

If you set only one side of a bidirectional link, the in-memory object graph becomes inconsistent and
JPA may not persist what you expect. Add small methods on the parent/owning entity that update **both**
sides in one call:

- On **`Author`** / **`Publisher`**: `addBook` adds to the local list **and** sets the back-reference on
  the `Book`; `removeBook` clears both sides.
- On **`Book`**: `addCategory` / `removeCategory` update both `Book.categories` and
  `Category.books`.

Use these helpers whenever you attach or detach related objects in tests or service code.

### 7.5 `equals` / `hashCode` for entities used in a `Set`

`Book` lives inside `Category.books` and `Category` lives inside `Book.categories` (both `Set`s), so
they need stable `equals`/`hashCode`. Do **not** include relationship fields (infinite recursion) and
do **not** rely on the generated `id` (null before persist). Use a **business key**:

- **`Book`**: hash/equality based on **`isbn`** only.
- **`Category`**: hash/equality based on **`name`** only.

Use Lombok `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on the class and
`@EqualsAndHashCode.Include` on the business-key field.

Also do **not** put Lombok `@ToString`/`@Data` on entities with relationships — the default
`toString` walks the graph and StackOverflows.

### 7.6 Cascade and `orphanRemoval` (concept)

`cascade` controls which operations propagate from parent to child (`PERSIST`, `MERGE`, `REMOVE`,
`ALL`); `orphanRemoval = true` deletes a child when it is removed from its parent collection. This lab
deliberately uses only `{PERSIST, MERGE}` (non-destructive) and **protects** deletions instead of
cascading them (Section 14) — deleting an author that still has books returns `409` rather than
silently destroying the books. Understand the trade-off: cascade-remove is convenient but dangerous;
choosing *not* to cascade is a valid, common production decision. (See the stretch task in Section 21
for a concrete `orphanRemoval` example.)

## 8. Repositories & The N+1 Problem (required + guidance)

Each repository extends `JpaRepository<Entity, Long>`. Add `existsBy…` derived queries for the unique
fields (`existsByIsbn`, `existsByNameIgnoreCase`) and for the protected deletes (`existsByAuthorId`,
`existsByPublisherId` on `BookRepository`). See Section 6.3 for the full method list.

### 8.1 The N+1 problem

Because the relationships are LAZY, this innocent loop:

```java
for (Book b : bookRepository.findAll()) {
    System.out.println(b.getAuthor().getLastName()); // touches the lazy author
}
```

runs **1** query for the books plus **N** more (one per book) to load each author — "N+1". With many
rows this is a serious performance bug. Two standard fixes:

### 8.2 Fix A — `JOIN FETCH` (a custom JPQL `@Query`)

Add a method (e.g. `findAllWithAuthorAndPublisher`) whose `@Query` selects books **`join fetch`**
both `author` and `publisher` in **one** SQL round-trip. This is the classic JPQL fix for N+1 when you
need a non-paginated list with both associations initialised. Your `@DataJpaTest` should assert both
associations are loaded (Section 6.8).

### 8.3 Fix B — `@EntityGraph`

Add a method (e.g. `findDetailedById`) that combines a `@Query` filtering by id with an
`@EntityGraph(attributePaths = {"author", "publisher", "categories"})` so a single-book load brings
back everything needed for the **detail** API response.

### 8.4 Pagination + fetch: an important subtlety

You will paginate the book list. **Do not fetch a collection (the `@ManyToMany categories`) together
with pagination** — Hibernate cannot paginate a collection join in SQL and would load everything into
memory and paginate there (you will see a warning). So:

- **Paginated** finders (`findAll`, `findByAuthorId`, `findByPublisherId`, `findByCategoriesId`) use
  `@EntityGraph` to fetch only the **`author`** and **`publisher`** `@ManyToOne` sides — **not**
  `categories`.
- The **single-book** detail finder (`findDetailedById`, 8.3) additionally fetches `categories`.

This is why your service maps list responses **without** categories and detail responses **with** them
(Section 11).

## 9. DTOs (required + guidance)

Never expose entities through the API — serializing a bidirectional graph causes infinite recursion.
Use DTOs and map **manually** (no MapStruct).

- **Request DTOs reference relations by id.** `BookRequestDto` carries `authorId`, `publisherId`,
  `List<Long> categoryIds`, plus the scalar fields with validation (Section 13.7). The service resolves
  those ids to entities and returns `404` if any id is missing.
- **Response DTOs embed nested *summary* DTOs.** `BookResponseDto` embeds `AuthorSummaryDto`,
  `PublisherSummaryDto`, and `List<CategorySummaryDto>` — small flat objects with no back-references.
- Add request/response DTOs for each aggregate, plus the three summary DTOs (full list in Section 6.4).
- Add a generic **`PagedResponse<T>`** wrapper (`content`, `page`, `size`, `totalElements`,
  `totalPages`, `last`) so paginated endpoints never serialize Spring's raw `Page`.

Exact JSON shapes are in Section 13.5–13.6.

## 10. Exceptions (required)

Use a single `@RestControllerAdvice` that produces `ProblemDetail` (RFC 7807) responses. You need:

- `ResourceNotFoundException(String resource, Long id)` → `404` ("X not found with id: …").
- `DuplicateResourceException(String field, String value)` → `409` (isbn / publisher name / category name).
- `RelationshipConflictException(String message)` → `409` (deleting a referenced author/publisher).
- Handlers for `MethodArgumentNotValidException` → `400` (with a per-field `errors` map),
  `MethodArgumentTypeMismatchException` → `400` (non-numeric path id), and a generic `Exception` →
  controlled `500` (never leak a stack trace).

Exact error bodies are in Section 13.8. Class checklist: Section 6.5.

## 11. Service Layer (required + guidance)

Annotate each `@Service` with `@RequiredArgsConstructor` and class-level
`@Transactional(readOnly = true)`; put `@Transactional` (read-write) on the mutating methods.

> **Why `@Transactional`?** Read-only transactions let the provider skip dirty-checking and hint the
> DB for read optimisations; write methods need a real transaction so all changes commit/roll back
> together. Keeping LAZY associations loadable during mapping also relies on an open transaction.

Each `*ServiceImpl` follows the pattern in Section 6.6: class-level read-only transactions, read-write
on mutating methods, constructor injection via `@RequiredArgsConstructor`.

Business logic to implement (per-service method breakdown in Section 6.6):

- **Uniqueness (`409`):** check `existsByIsbn` / `existsByNameIgnoreCase` on create, and on update
  only when the value actually changed.
- **Resolve ids → entities (`404`):** in `BookService`, look up `authorId`, `publisherId`, and each
  `categoryId`; throw `ResourceNotFoundException` if any is missing. Collect categories into a `Set`.
- **Protective delete (`409`):** `deleteAuthor` / `deletePublisher` throw `RelationshipConflictException`
  if `existsByAuthorId/PublisherId(id)` — you cannot delete one that still has books.
- **Detaching delete:** `deleteCategory` removes the category from each of its books' `categories` set
  (Book is the owning side) before deleting, so the join rows go away.
- **List vs. detail mapping:** map paginated list results with the `@ManyToOne`-only data and an
  **empty** categories list (do not touch the lazy collection); map single-book detail results from
  `findDetailedById` **with** categories. (Payoff of Section 8.4.)
- Keep mapping and not-found logic in private helpers so it is not duplicated.

## 12. Controllers (required + guidance)

Each `@RestController` is thin, returns `ResponseEntity`, uses `@Valid @RequestBody`, and lets the
exception handler own the error codes. `POST` returns `201` + a `Location` header; `DELETE` returns
`204`. Spring binds pagination/sorting automatically when a handler takes a `Pageable` with
`@PageableDefault(size = 20, sort = "title")`.

Endpoint inventory per controller: Section 6.7. The repository returns `Page<Book>`; the service
converts it to `PagedResponse<BookResponseDto>` (map the content, copy
`getNumber()/getSize()/getTotalElements()/getTotalPages()/isLast()` — or use your `PagedResponse.from`
factory).

## 13. API Contract (required — the authoritative spec)

JSON in/out; every endpoint returns the documented status code. Path ids are numeric. All
list-of-books responses use the **paged** shape (13.6).

### 13.1 Authors — `/api/authors`
| Method | Path | Body | Success | Errors |
| ------ | ---- | ---- | ------- | ------ |
| POST | `/api/authors` | AuthorRequest | `201` + `Location`, AuthorResponse | `400` |
| GET | `/api/authors` | — | `200`, array of AuthorResponse | — |
| GET | `/api/authors/{id}` | — | `200`, AuthorResponse | `404`, `400` (non-numeric id) |
| PUT | `/api/authors/{id}` | AuthorRequest | `200`, AuthorResponse | `400`, `404` |
| DELETE | `/api/authors/{id}` | — | `204` | `404`, **`409` if the author still has books** |
| GET | `/api/authors/{id}/books` | — | `200`, paged BookResponse | `404` (author missing) |

### 13.2 Publishers — `/api/publishers`
Same shape as authors, plus:
- `POST` / `PUT` return **`409`** if `name` is already used by another publisher.
- `DELETE` returns **`409`** if the publisher still has books.
- `GET /api/publishers/{id}/books` → `200`, paged BookResponse (`404` if publisher missing).

### 13.3 Categories — `/api/categories`
| Method | Path | Body | Success | Errors |
| ------ | ---- | ---- | ------- | ------ |
| POST | `/api/categories` | CategoryRequest | `201` + `Location`, CategoryResponse | `400`, `409` (duplicate name) |
| GET | `/api/categories` | — | `200`, array | — |
| GET | `/api/categories/{id}` | — | `200` | `404` |
| PUT | `/api/categories/{id}` | CategoryRequest | `200` | `400`, `404`, `409` |
| DELETE | `/api/categories/{id}` | — | `204` (also detaches the category from all books) | `404` |

### 13.4 Books — `/api/books`
| Method | Path | Body | Success | Errors |
| ------ | ---- | ---- | ------- | ------ |
| POST | `/api/books` | BookRequest | `201` + `Location`, BookResponse | `400`, `404` (unknown author/publisher/category id), `409` (duplicate isbn) |
| GET | `/api/books` | — | `200`, paged BookResponse | — |
| GET | `/api/books?categoryId={id}` | — | `200`, paged BookResponse filtered by category | `404` (category missing) |
| GET | `/api/books/{id}` | — | `200`, BookResponse (with categories) | `404`, `400` (non-numeric id) |
| PUT | `/api/books/{id}` | BookRequest | `200`, BookResponse | `400`, `404`, `409` (duplicate isbn) |
| DELETE | `/api/books/{id}` | — | `204` | `404` |

All `GET` list endpoints accept `?page=0&size=20&sort=title,asc`.

### 13.5 Request / Response JSON shapes

```json
// AuthorRequest                                  // AuthorResponse
{ "firstName": "Robert", "lastName": "Martin",    { "id": 1, "firstName": "Robert",
  "bio": "Author of Clean Code" }                   "lastName": "Martin", "bio": "Author of Clean Code" }

// PublisherRequest                               // PublisherResponse
{ "name": "Prentice Hall", "country": "USA" }     { "id": 1, "name": "Prentice Hall", "country": "USA" }

// CategoryRequest                                // CategoryResponse
{ "name": "Software" }                            { "id": 1, "name": "Software" }
```

**BookRequest** (relations referenced by id):
```json
{
  "title": "Clean Code",
  "isbn": "9780132350884",
  "publicationYear": 2008,
  "price": 42.50,
  "authorId": 1,
  "publisherId": 1,
  "categoryIds": [1, 2]
}
```

**BookResponse** (relations embedded as nested summaries):
```json
{
  "id": 10,
  "title": "Clean Code",
  "isbn": "9780132350884",
  "publicationYear": 2008,
  "price": 42.50,
  "author": { "id": 1, "firstName": "Robert", "lastName": "Martin" },
  "publisher": { "id": 1, "name": "Prentice Hall" },
  "categories": [ { "id": 1, "name": "Software" } ]
}
```

> **List vs. detail:** in paged **list** responses (`GET /api/books`, `/api/authors/{id}/books`, …)
> the `categories` array may be empty — list views do not expand the many-to-many collection
> (Section 8.4). The single-book **detail** response (`GET /api/books/{id}`) MUST include the
> populated `categories`.

### 13.6 Paged response shape (do **not** return Spring's raw `Page`)
```json
{ "content": [ /* items */ ], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "last": true }
```

### 13.7 Validation rules (drive the `400` responses)
- **BookRequest:** `title` not blank; `isbn` matches `^\d{13}$`; `publicationYear` in `[1450, 2100]`;
  `price` > 0; `authorId` and `publisherId` not null; `categoryIds` optional.
- **AuthorRequest:** `firstName`/`lastName` not blank; `bio` ≤ 2000 chars.
- **PublisherRequest:** `name`/`country` not blank. **CategoryRequest:** `name` not blank.

### 13.8 Error response format (`ProblemDetail`, RFC 7807)
```json
// 404
{ "title": "Resource not found", "status": 404, "detail": "Book not found with id: 9999" }
// 409 duplicate unique field
{ "title": "Duplicate value", "status": 409, "detail": "isbn already in use: 9780132350884" }
// 409 protected relationship
{ "title": "Relationship conflict", "status": 409,
  "detail": "Cannot delete author 1 because they still have books" }
// 400 validation (per-field errors map)
{ "title": "Validation failed", "status": 400, "detail": "Request validation failed",
  "errors": { "isbn": "ISBN must be 13 digits", "price": "Price must be greater than 0" } }
```
A non-numeric path id (`GET /api/books/abc`) must return `400` (not `404`/`500`).

## 14. Business Rules (required)

- `isbn`, publisher `name`, and category `name` are unique → creating/updating a duplicate returns `409`.
- Creating/updating a book with an `authorId`, `publisherId`, or `categoryId` that does not exist
  returns `404`.
- Deleting an `Author` or `Publisher` that still has books returns `409`. Deleting a `Category`
  succeeds and detaches it from all its books.
- `PUT` updates an existing resource only; updating a missing id returns `404`.

## 15. Architecture & Code Constraints (required)

- Layered: `controller → service → repository → entity`, with separate `dto` and `exception` packages.
- The API exposes **DTOs only** — never serialize entities. Map manually (no MapStruct).
- Request DTOs reference relations by **id**; response DTOs embed **nested summary** objects.
- A single `@RestControllerAdvice` produces `ProblemDetail` errors.
- Controllers return `ResponseEntity` and own the **success** codes; the advice owns the **error** codes.

## 16. Testing (required + guidance)

Provide automated tests at three levels; all must pass with `mvn test`, using H2 for the DB layer.

### 16.1 Spring Boot 4.0 test imports (different from 3.x!)
```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;     // @DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;    // TestEntityManager
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;        // @WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean;   // @MockitoBean (unchanged)
import tools.jackson.databind.ObjectMapper;                                  // Jackson 3! (was com.fasterxml.jackson...)
```

### 16.2 `@DataJpaTest` — repository / relationship tests

`@DataJpaTest` starts only the JPA layer against in-memory H2 (no web, no services) and gives a
`TestEntityManager` to seed data. Create **`EntityMappingTest`** and **`BookRepositoryTest`** as
described in Section 6.8. Typical pattern: seed entities via `TestEntityManager.persist`, call
`flush()` and `clear()` to force a database round-trip, then assert through the repository.

Write `@DataJpaTest` tests that verify: the `book_category` join persists and reloads; `findByAuthorId`
returns only that author's books and is paginated; `findDetailedById` initialises the `categories`
collection; `findAllWithAuthorAndPublisher` initialises author + publisher (assert with
`PersistenceUnitUtil.isLoaded(entity, "association")`). Add an entity-mapping test that persists a
fully-linked `Book` and reloads every relationship.

### 16.3 Service tests (Mockito) and web tests (`@WebMvcTest`)

Full test-class inventory: Section 6.8.

- **Service** (`@ExtendWith(MockitoExtension.class)`): one test class per `*ServiceImpl`; mock the
  repositories; cover uniqueness `409`, id-resolution `404`, the protective delete
  (`RelationshipConflictException`), and list/detail mapping.
- **Web** (`@WebMvcTest(XController.class)`, service mocked via `@MockitoBean`): one test class per
  controller; assert status codes **and** JSON — the `ProblemDetail` shape (`$.title`, `$.detail`,
  `$.errors.<field>`), the `Location` header, and the paged shape (`$.content[0]…`, `$.totalElements`,
  `$.last`).

## 17. Layer Responsibilities Summary

| Layer | What belongs there | What does not |
| ----- | ------------------ | ------------- |
| Controller | HTTP mapping, `@Valid`, success codes, `Location`, binding `Pageable` | DB access, mapping, business rules |
| Exc. handler | Translating exceptions → `ProblemDetail` error codes | Business rules |
| Service | Business rules, `@Transactional`, id→entity resolution, uniqueness, delete policy, entity↔DTO mapping | Raw HTTP concerns |
| Repository | Data access, derived queries, `@Query`/`@EntityGraph`, pagination | Validation, HTTP |
| Entity | Persistence mapping, relationship ownership, sync helpers | Request-validation annotations, `@ToString` over relationships |
| DTO | API request/response shape + request validation | Persistence logic |

Key idea: relationship *management* lives in the service and the entity helper methods; the controller
never sees an entity.

## 18. Suggested Build Order

Start from Section 6 (inventory), then follow the conceptual sections: configure the project → entities
+ relationships (Section 7) → repositories incl. fetch/pagination queries (Section 8) → DTOs (Section 9)
→ exceptions + global handler (Section 10) → services (Section 11) → controllers (Section 12) → tests
(Section 16). Implement and test one aggregate end-to-end (e.g. Author) before moving on.

## 19. Manual Testing Flow

With the app running against `bookstore_lab` (Postman / curl):

1. Create an author, a publisher, and a couple of categories.
2. Create a book referencing those ids → `201` + `Location`; body has nested author/publisher/categories.
3. `GET /api/books?page=0&size=10&sort=title` → `200`, paged shape.
4. `GET /api/books?categoryId={id}` → only that category's books.
5. `GET /api/authors/{id}/books` → that author's books, paged.
6. `GET /api/books/{id}` → full detail including categories.
7. `GET /api/books/9999` → `404`; `GET /api/books/abc` → `400`.
8. `POST` a book with a duplicate ISBN → `409`; with a non-existent `authorId` → `404`; with an
   11-digit ISBN → `400`.
9. `DELETE` an author that still has a book → `409`; delete the book first, then the author → `204`.
10. `DELETE` a category attached to a book → `204`, and the book loses that category.

## 20. Acceptance Criteria (how it will be graded)

- [ ] The project builds and `mvn test` is green.
- [ ] Every class listed in Section 6 exists and fulfils its described responsibilities.
- [ ] The four entities exist with correct relationship ownership; a saved book reloads with author,
      publisher, and categories; the `book_category` join table and the FKs are created.
- [ ] All endpoints in Section 13 work and return the **exact** documented status codes and JSON shapes.
- [ ] `POST` returns `201` with a `Location` header; `DELETE` returns `204`.
- [ ] Validation produces `400` with a per-field `errors` map; non-numeric path ids produce `400`.
- [ ] Duplicate `isbn` / publisher `name` / category `name` produce `409`.
- [ ] Unknown related ids produce `404`; deleting an author/publisher with books produces `409`;
      deleting a category detaches it from books.
- [ ] Book lists are paginated and sortable and use the paged wrapper (not raw `Page`).
- [ ] N+1 is avoided on list endpoints (via `JOIN FETCH` and `@EntityGraph`); the detail endpoint
      returns categories.
- [ ] Services use `@Transactional`; the API exposes DTOs only, mapped manually.
- [ ] Tests exist and pass at all three levels (`@DataJpaTest`, Mockito service, `@WebMvcTest`).

## 21. Optional Stretch Goals (not graded)

1. **Concrete `orphanRemoval`:** add a `Review` entity owned by `Book`
   (`@OneToMany(cascade = ALL, orphanRemoval = true)`) so deleting a book deletes its reviews and
   removing a review from `book.getReviews()` deletes it — the textbook cascade/orphan-removal demo the
   main model deliberately avoids.
2. Add multi-field sorting and expose it through the API.
3. Add a `@DataJpaTest` that uses Hibernate statistics to assert the `JOIN FETCH` query runs exactly
   one SQL statement (prove the N+1 fix).
4. Preview the next lab: dynamic multi-field search with the JPA Specification/Criteria API.

## 22. Instructor Notes

Lab 1 taught the layered flow; Lab 2 taught HTTP semantics; Lab 3 teaches the **data layer** —
modelling relationships and querying them without falling into the N+1 trap. The student should leave
able to justify, for any association, which side owns it, how it is fetched, and why. Section 6 is the
student-facing build checklist (47 source files). Note the Spring Boot 4.0 specifics (Sections 5.1 and
16.1): the modular test starters and Jackson 3 packages trip up anyone expecting 3.x layouts.
