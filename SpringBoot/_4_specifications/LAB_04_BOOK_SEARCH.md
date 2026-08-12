# Lab 04 — Dynamic Search with JPA Specifications & the Criteria API

> **Hand this single document to the student.** It is fully self-contained: it specifies *what* to
> add (the requirements, the exact API contract, and a **class-by-class implementation checklist**
> in Section 6) and gives *guidance* on *how* to approach the genuinely new concepts. A student can
> implement the entire feature from this file alone, starting from a finished Lab 3 project.

---

## 1. Problem Statement & Goal

You already built the **Bookstore REST API** in Lab 3 (`Author 1─N Book N─1 Publisher`, plus
`Book N─N Category`, with CRUD, pagination, DTOs, and `ProblemDetail` errors). Lab 4 is an
**increment on that project**: you add **one** new endpoint — `GET /api/books/search` — that filters
books by **any combination** of *title*, *author name*, *category*, *price range*, and *publication-year
range*.

The naive way to do this is to write a repository method per filter combination
(`findByTitleContaining`, `findByTitleContainingAndAuthor…`, …) — that explodes combinatorially and is
unmaintainable. Instead you will build the query **dynamically at runtime** with the **JPA Criteria API**,
wrapped in **Spring Data JPA `Specification<Book>`** objects. The result is a single endpoint that
accepts every filter as an optional query parameter, assembles only the predicates the caller actually
supplied, and returns the same paginated shape as the rest of the API.

You will deliver this feature added to your Lab 3 codebase, matching the **API Contract** in Section 12,
backed by the same PostgreSQL database, with automated tests at three layers.

## 2. Learning Objectives (what this assesses)

1. Explain why per-combination derived query methods do not scale, and why dynamic queries are the fix.
2. Use the **JPA Criteria API** (`Root`, `CriteriaQuery`, `CriteriaBuilder`, `Predicate`, `Join`) to
   build type-safe `WHERE` fragments without JPQL strings.
3. Implement Spring Data JPA **`Specification<Book>`** factories and **compose** them with `.and(...)`.
4. Use the Spring Data JPA 4.0 **`Specification.unrestricted()`** base ("match everything") to fold
   optional filters cleanly, with no special "return everything" branch.
5. Wire **`JpaSpecificationExecutor<Book>`** into the repository so it coexists with the Lab 3
   `JpaRepository` methods and `@EntityGraph` finders.
6. Recognise and fix two Criteria pitfalls: **many-to-many row multiplication** (fixed with
   `query.distinct(true)`) and **fetching a collection while paginating** (Lab 3's rule — so search
   uses list mapping).
7. Bind a set of optional `@RequestParam`s plus a `Pageable` into a request object and delegate to the
   service.
8. Write `@DataJpaTest`, Mockito service, and `@WebMvcTest` tests for a dynamic-search feature.

**Assumed background:** you have a working Lab 3 Bookstore (or are starting from the Lab 3 reference).
This document re-states the Lab 3 pieces you must touch, so you can build the increment even if the
details are rusty.

## 3. The One Big New Idea: Build the Query at Runtime

A derived query like `findByTitleContaining(String)` bakes the filter into the **method name** at
compile time. With five independent, optional filters you would need `2⁵ = 32` method variants to cover
every combination — clearly absurd.

A **`Specification<Book>`** is a small object that represents one piece of a `WHERE` clause as a
**predicate built with the Criteria API**, not as a string. Spring Data JPA's interface is:

```java
package org.springframework.data.jpa.domain;

public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
```

- `root` is the `FROM Book` of the query — `root.get("title")`, `root.join("author")`, etc.
- `cb` (`CriteriaBuilder`) is the factory for predicates and functions — `cb.like(...)`, `cb.equal(...)`,
  `cb.lower(...)`, `cb.between(...)`, `cb.or(...)`.
- `query` is the surrounding `CriteriaQuery`; you use it to set `query.distinct(true)` when a join can
  multiply rows.

Because each `Specification` is just a fragment, you can **compose** them at runtime. You start from a
base that matches everything and `.and(...)` in a predicate **only for the filters that are present**.
That is the whole technique.

## 4. What's New vs. Lab 3

Lab 4 changes a small, well-defined set of files. Everything else from Lab 3 stays **untouched**.

| Area | Change |
| ---- | ------ |
| `dto` | **NEW** `BookSearchCriteria` record (the five logical filters). |
| `specification` (**NEW package**) | **NEW** `BookSpecifications` (Criteria factories) and `BookSpecificationBuilder` (composes them). |
| `repository` | `BookRepository` now **also** extends `JpaSpecificationExecutor<Book>`. Every existing method and `@EntityGraph` finder is kept exactly as in Lab 3. |
| `service` | `BookService` / `BookServiceImpl` gain **one** method: `searchBooks(BookSearchCriteria, Pageable)`. |
| `controller` | `BookController` gains **one** handler: `@GetMapping("/search")`. |
| `test` | **NEW** `BookSearchSpecificationTest` (`@DataJpaTest`); a new `searchBooks` case in `BookServiceImplTest`; new `search` + `searchNoParams` cases in `BookControllerTest`. |

No entity changes, no new exceptions, no new error codes. The base package for this lab is
**`com.example._4_specifications`**.

## 5. Tech Stack & Project Setup (required)

Same stack as Lab 3 — **Java 21**, **Maven**, **Spring Boot 4.0.x**, **PostgreSQL** at runtime, **H2**
(test scope) for tests, **Lombok**. Continue with the layered architecture
(`controller → service → repository → entity`, plus `dto`, `exception`, and now `specification`).

### 5.1 Spring Boot 4.0 specifics you MUST know (or you will get stuck)

These are unchanged from Lab 3 but still trip people up:

- **Test slices live in dedicated starters** (`spring-boot-starter-data-jpa-test`,
  `spring-boot-starter-webmvc-test`); `spring-boot-starter-test` no longer brings them transitively.
- **The test-slice imports are renamed** (NOT the 3.x packages — see Section 14.1):
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
  `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`,
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.
- **Jackson 3**: the `ObjectMapper` is `tools.jackson.databind.ObjectMapper` (not `com.fasterxml...`).

### 5.2 Spring Data JPA 4.0 `Specification` API you MUST know

The reference relies on the **4.0** shape of the API. State these precisely:

- `org.springframework.data.jpa.domain.Specification<T>` has
  `toPredicate(Root<T>, CriteriaQuery<?>, CriteriaBuilder)`.
- **`Specification.unrestricted()`** is the 4.0 "match everything" base that **replaces the legacy
  `where(null)` idiom**. Start composition from it.
- Static factories `Specification.allOf(...)` / `Specification.anyOf(...)` and the default
  `.and(...)` / `.or(...)` / `.not()` methods compose specs.
- **`PredicateSpecification<T>` is NEW in 4.0** (it drops the `CriteriaQuery` argument). You should
  **recognise** it, but this lab standardises on **`Specification<T>`** because search is paginated
  through a repository (and pagination plumbing expects `Specification`).
- **`JpaSpecificationExecutor<Book>`** adds `findAll(Specification, Pageable)` and
  `findAll(Specification, Sort)` (among others). It **coexists** with `JpaRepository` methods and the
  Lab 3 `@EntityGraph` finders — adding it changes nothing about the existing methods.

## 6. Complete Implementation Inventory (class-by-class checklist)

This section lists **every Java class you create or change** for Lab 4. Use it as your build checklist.
Sections 7–14 explain the hard concepts; this section tells you **what each class is for and what it
must do** — not how to code it (that is your job). Classes not listed here are **unchanged from Lab 3**.

### 6.1 `dto` package — one NEW class

#### `BookSearchCriteria` — an immutable Java **`record`** (NOT a Lombok class)

This is a `record` (so it is immutable and gets a canonical constructor and accessors for free). It
carries the **five logical filters** (title, author, category, price range, publication-year range),
expressed as **seven nullable components** — the two ranges each take a `min`/`max` pair. **Every
component is nullable**; a `null` value (or, for the `String` fields, a blank value) means *"the caller
did not supply this filter, so it must not constrain the query."*

| Component | Type | Meaning |
| --------- | ---- | ------- |
| `title` | `String` | case-insensitive "title contains" match |
| `author` | `String` | case-insensitive match on the author's first **OR** last name (a join) |
| `category` | `String` | case-insensitive exact match on a category name (a many-to-many join) |
| `minPrice` | `BigDecimal` | lower bound of an open-ended price range |
| `maxPrice` | `BigDecimal` | upper bound of an open-ended price range |
| `minYear` | `Integer` | lower bound of an open-ended publication-year range |
| `maxYear` | `Integer` | upper bound of an open-ended publication-year range |

Declare the components in **exactly this order** (`title, author, category, minPrice, maxPrice, minYear,
maxYear`) — the controller and the tests construct it positionally with the canonical constructor. No
validation annotations; "filter absent" is a normal, valid state, not an error.

### 6.2 `specification` package — two NEW classes (this package is new)

Both are **`final` utility classes** with a **private constructor** (not instantiable) and only
**`static`** methods. Import `org.springframework.data.jpa.domain.Specification` and the Criteria types
from `jakarta.persistence.criteria` (e.g. `Join`).

#### `BookSpecifications` — a library of single-responsibility `Specification<Book>` factories

Each method returns one reusable `WHERE` fragment as a `Specification<Book>` lambda
`(root, query, cb) -> predicate`. **Each factory assumes its argument(s) are present** — the builder
(below) only calls a factory after confirming the corresponding filter is non-null/non-blank, so a
factory never has to return a null predicate.

| Static method (exact signature) | What its predicate must do |
| -------------------------------- | -------------------------- |
| `public static Specification<Book> titleContains(String title)` | Case-insensitive `LIKE %title%` on `title`. Lower-case both sides: build the pattern `"%" + title.toLowerCase() + "%"` and return `cb.like(cb.lower(root.get("title")), pattern)`. |
| `public static Specification<Book> authorNameContains(String author)` | Join `Book.author` (`root.join("author")` — a mandatory `@ManyToOne`, so an inner join is correct), then match where **firstName OR lastName** contains `author`, case-insensitive: `cb.or(cb.like(cb.lower(authorJoin.get("firstName")), pattern), cb.like(cb.lower(authorJoin.get("lastName")), pattern))`. |
| `public static Specification<Book> hasCategory(String category)` | Set **`query.distinct(true)`** (guard `if (query != null)`), join the many-to-many `Book.categories` (`root.join("categories")`), and match `cb.equal(cb.lower(categoryJoin.get("name")), category.toLowerCase())`. The `distinct` is what stops the join from returning a book once per matching category row. |
| `public static Specification<Book> priceBetween(BigDecimal min, BigDecimal max)` | **Open-ended** range on `price`: both present → `cb.between(root.get("price"), min, max)`; min only → `cb.greaterThanOrEqualTo(root.get("price"), min)`; max only → `cb.lessThanOrEqualTo(root.get("price"), max)`. |
| `public static Specification<Book> publicationYearBetween(Integer min, Integer max)` | Same open-ended handling on `publicationYear` (`between` / `greaterThanOrEqualTo` / `lessThanOrEqualTo`). |

> The builder guarantees that for `priceBetween` / `publicationYearBetween` **at least one** bound is
> non-null before the factory is called, so the "max only" branch is reached only when `min` is null.

#### `BookSpecificationBuilder` — assembles one combined `Specification<Book>` from the criteria

One static method:

```java
public static Specification<Book> from(BookSearchCriteria criteria)
```

It must:

1. Start from **`Specification<Book> spec = Specification.unrestricted();`** (the 4.0 "match everything"
   base — see Section 5.2).
2. For each present filter, reassign `spec = spec.and(<the matching factory>)`:
   - `if (StringUtils.hasText(criteria.title()))` → `BookSpecifications.titleContains(criteria.title())`
   - `if (StringUtils.hasText(criteria.author()))` → `BookSpecifications.authorNameContains(criteria.author())`
   - `if (StringUtils.hasText(criteria.category()))` → `BookSpecifications.hasCategory(criteria.category())`
   - `if (criteria.minPrice() != null || criteria.maxPrice() != null)` → `BookSpecifications.priceBetween(criteria.minPrice(), criteria.maxPrice())`
   - `if (criteria.minYear() != null || criteria.maxYear() != null)` → `BookSpecifications.publicationYearBetween(criteria.minYear(), criteria.maxYear())`
3. Return `spec`.

Use `org.springframework.util.StringUtils.hasText(...)` for the text filters (it treats `null`, `""`,
and whitespace-only as "absent"). Use plain `null` checks for the numeric ranges, adding the range
predicate when **min OR max** is present. **There is no special "return everything" branch** — because
the base already matches all rows, all-null criteria naturally returns every book.

### 6.3 `repository` package — `BookRepository` CHANGED (one extra interface)

`BookRepository` now extends **both** `JpaRepository<Book, Long>` **and**
`JpaSpecificationExecutor<Book>`:

```java
public interface BookRepository
        extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    // ... every Lab 3 method below stays exactly as it was ...
}
```

Adding `JpaSpecificationExecutor<Book>` unlocks `findAll(Specification, Pageable)` and
`findAll(Specification, Sort)` for the runtime-assembled search. **Keep every existing method and
`@EntityGraph` finder from Lab 3 untouched** — for reference, the unchanged members are:

| Member (unchanged from Lab 3) | Annotation |
| ----------------------------- | ---------- |
| `boolean existsByIsbn(String isbn);` | — |
| `boolean existsByAuthorId(Long authorId);` | — |
| `boolean existsByPublisherId(Long publisherId);` | — |
| `Page<Book> findAll(Pageable pageable);` | `@Override`, `@EntityGraph(attributePaths = {"author", "publisher"})` |
| `Page<Book> findByAuthorId(Long authorId, Pageable pageable);` | `@EntityGraph(attributePaths = {"author", "publisher"})` |
| `Page<Book> findByPublisherId(Long publisherId, Pageable pageable);` | `@EntityGraph(attributePaths = {"author", "publisher"})` |
| `Page<Book> findByCategoriesId(Long categoryId, Pageable pageable);` | `@EntityGraph(attributePaths = {"author", "publisher"})` |
| `Optional<Book> findDetailedById(Long id);` | `@EntityGraph(attributePaths = {"author", "publisher", "categories"})` + `@Query("select b from Book b where b.id = :id")` |
| `List<Book> findAllWithAuthorAndPublisher();` | `@Query("select b from Book b join fetch b.author join fetch b.publisher")` |

> Note: the `JpaSpecificationExecutor.findAll(spec, pageable)` does **not** apply your `@EntityGraph`
> finders' fetch plans — but the service maps search results with the **list mapping** (no categories),
> so only the `@ManyToOne author`/`publisher` sides are touched during mapping. See Section 9.

### 6.4 `service` package — `BookService` / `BookServiceImpl` CHANGED (one new method)

Add **one** method to the `BookService` interface and implement it in `BookServiceImpl`. Everything else
in both files is unchanged from Lab 3.

**Interface — add this signature:**

```java
PagedResponse<BookResponseDto> searchBooks(BookSearchCriteria criteria, Pageable pageable);
```

**`BookServiceImpl` — add this method** (note: no extra `@Transactional` annotation is needed beyond the
class-level `@Transactional(readOnly = true)`, because search is a read):

| Method | What it must do |
| ------ | --------------- |
| `searchBooks` | Build the combined spec via `BookSpecificationBuilder.from(criteria)`, call `bookRepository.findAll(spec, pageable)` (the `JpaSpecificationExecutor` overload returning `Page<Book>`), and return `toPagedResponse(page)` — **reusing the existing `toPagedResponse` helper**, which maps each book with the **list mapping** (author + publisher summaries, **empty categories list**). |

Exact reference body:

```java
@Override
public PagedResponse<BookResponseDto> searchBooks(BookSearchCriteria criteria, Pageable pageable) {
    Specification<Book> spec = BookSpecificationBuilder.from(criteria);
    return toPagedResponse(bookRepository.findAll(spec, pageable));
}
```

The `toPagedResponse`, `mapToListResponse`, and `mapToDetailResponse` helpers are the **same Lab 3
helpers** — do not duplicate them. `searchBooks` reuses `toPagedResponse` → `mapToListResponse`.

> **Why list mapping (the collection-vs-pagination rule)?** Search is **paginated**. Lab 3 established
> that you must **not** fetch a collection (the `@ManyToMany categories`) together with pagination —
> Hibernate cannot paginate a collection join in SQL and would load everything into memory and paginate
> there. So `searchBooks` maps exactly like `getAllBooks`: it expands only the `@ManyToOne author` and
> `publisher`, and returns an **empty** `categories` list. (The single-book detail endpoint, which is
> not paginated, is the only place categories are expanded.)

### 6.5 `controller` package — `BookController` CHANGED (one new handler)

Add **one** handler. Everything else in `BookController` (the constructor, `create`, `getAll`, `getById`,
`update`, `delete`) is unchanged from Lab 3.

| Handler signature | Mapping | Returns |
| ----------------- | ------- | ------- |
| `search(@RequestParam(required = false) String title, @RequestParam(required = false) String author, @RequestParam(required = false) String category, @RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice, @RequestParam(required = false) Integer minYear, @RequestParam(required = false) Integer maxYear, @PageableDefault(size = 20, sort = "title") Pageable pageable)` | `@GetMapping("/search")` | `200`, `PagedResponse<BookResponseDto>` |

The handler must:

1. Take each filter as an **optional** `@RequestParam(required = false)` — `title`, `author`, `category`
   as `String`; `minPrice`, `maxPrice` as `BigDecimal`; `minYear`, `maxYear` as `Integer`.
2. Bind a `Pageable` with **`@PageableDefault(size = 20, sort = "title")`** (same default as the other
   list endpoints).
3. Construct `new BookSearchCriteria(title, author, category, minPrice, maxPrice, minYear, maxYear)`
   (positional, in the record's component order).
4. Delegate: `return ResponseEntity.ok(bookService.searchBooks(criteria, pageable));`.

> **Routing point (`/search` vs `/{id}`):** the literal path `GET /api/books/search` takes priority over
> the path-variable mapping `GET /api/books/{id}`, so the two never collide — `/search` is matched before
> Spring would try to bind `search` as an id. (If it bound as `{id}`, `search` is non-numeric and you'd
> get a `400`, which is the wrong behaviour — the literal mapping prevents that.)

### 6.6 Test classes — one NEW class + two CHANGED classes

All must pass with **`mvn test`** using H2 (Section 14 for the Spring Boot 4.0 import packages).

| Class | Layer | Status | What you must verify |
| ----- | ----- | ------ | -------------------- |
| **`BookSearchSpecificationTest`** | `@DataJpaTest` | NEW | Exercise the whole pipeline `BookSearchCriteria → BookSpecificationBuilder → bookRepository.findAll(spec, …)` against H2. See Section 6.6.1. |
| **`BookServiceImplTest`** | Mockito | CHANGED (add one case) | Add `searchBooks`: it delegates to `findAll(Specification, Pageable)` and maps the list view (categories empty). See Section 6.6.2. |
| **`BookControllerTest`** | `@WebMvcTest` | CHANGED (add two cases) | Add `search` (binds every optional param into the criteria, verified with an `ArgumentCaptor`) and `searchNoParams`. See Section 6.6.3. |

#### 6.6.1 `BookSearchSpecificationTest` (`@DataJpaTest`)

Seed (via `TestEntityManager`) three authors — Robert **Martin**, Joshua **Bloch**, **Martin** Fowler —
one publisher (e.g. name `Addison-Wesley`, and remember `Publisher.country` is `nullable = false`, so set
it too, e.g. `USA`), two categories (**Software**, **Architecture**), and four books, then `flush()` +
`clear()`:

| Book | ISBN | Year | Price | Author | Categories |
| ---- | ---- | ---- | ----- | ------ | ---------- |
| Clean Code | 9780132350884 | 2008 | 42.50 | Martin | Software |
| Clean Architecture | 9780134494166 | 2017 | 33.00 | Martin | Software, Architecture |
| Effective Java | 9780134685991 | 2018 | 50.00 | Bloch | Software |
| Refactoring | 9780201485677 | 1999 | 47.00 | Fowler | Architecture |

Add a private helper `search(BookSearchCriteria)` that does
`bookRepository.findAll(BookSpecificationBuilder.from(criteria), Sort.by("title"))` and a helper that
builds a `BookSearchCriteria` positionally. Then assert (results sorted by title):

| Test | Criteria | Expected titles (in order) |
| ---- | -------- | -------------------------- |
| no filters returns every book | all null | Clean Architecture, Clean Code, Effective Java, Refactoring |
| blank text filters are ignored | `title="   "`, `author=""` | all 4 |
| title is a case-insensitive contains | `title="clean"` | Clean Architecture, Clean Code |
| author matches first OR last name | `author="martin"` | Clean Architecture, Clean Code, Refactoring (matches Robert MARTIN and MARTIN Fowler) |
| category matches linked books | `category="Architecture"` | Clean Architecture, Refactoring |
| price both bounds | `minPrice=40, maxPrice=50` | Clean Code, Effective Java, Refactoring |
| price min only | `minPrice=45` | Effective Java, Refactoring |
| price max only | `maxPrice=40` | Clean Architecture |
| publication-year range | `minYear=2008, maxYear=2018` | Clean Architecture, Clean Code, Effective Java |
| multiple filters AND-ed | `title="clean", minPrice=40` | Clean Code (Clean Architecture is 33.00, excluded) |
| many-to-many join has no duplicates | `category="Software"` | Clean Architecture, Clean Code, Effective Java — and assert `extracting(Book::getId).doesNotHaveDuplicates()` (Clean Architecture has two categories but must appear once) |
| results stay paginated and sorted | all null, `PageRequest.of(0, 2, Sort.by("title"))` | page: `totalElements=4`, `totalPages=2`, `isLast()=false`, content = Clean Architecture, Clean Code |

#### 6.6.2 `BookServiceImplTest` — add a `searchBooks` case (Mockito)

Stub `bookRepository.findAll(any(Specification.class), any(PageRequest.class))` to return a
`new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1)` where `book` has an author and publisher set
(no categories on the entity is fine). Call `service.searchBooks(criteria, PageRequest.of(0, 20))` and
assert:

- `result.getTotalElements() == 1`, `result.isLast() == true`, `result.getContent()` has size 1.
- The mapped content's title is `"Clean Code"` and `getAuthor().getLastName()` is `"Martin"`.
- **`result.getContent().get(0).getCategories()` is empty** — the list mapping does not fetch the
  collection while paginating.

#### 6.6.3 `BookControllerTest` — add `search` + `searchNoParams` cases (`@WebMvcTest`)

For both cases, stub `bookService.searchBooks(any(BookSearchCriteria.class), any(Pageable.class))` to
return a `PagedResponse` containing one sample book.

- **`search`**: `GET /api/books/search` with **every** param
  (`title=clean, author=martin, category=Software, minPrice=40, maxPrice=50, minYear=2000, maxYear=2020`).
  Assert `200`, `$.content[0].id == 10`, `$.totalElements == 1`, `$.last == true`. Then capture the
  criteria with an **`ArgumentCaptor<BookSearchCriteria>`** (`verify(bookService).searchBooks(captor.capture(),
  any(Pageable.class))`) and assert every component was bound correctly: `title="clean"`,
  `author="martin"`, `category="Software"`, `minPrice` compares equal to `40`, `maxPrice` to `50`,
  `minYear == 2000`, `maxYear == 2020`. (Use `isEqualByComparingTo` for the `BigDecimal`s.)
- **`searchNoParams`**: `GET /api/books/search` with no params → `200`, `$.content[0].id == 10`.

Use the Jackson 3 `ObjectMapper` (`tools.jackson.databind.ObjectMapper`) — Section 14.1.

### 6.7 Suggested file count for this increment

| Package | Files |
| ------- | ----- |
| `dto` | +1 (`BookSearchCriteria`) |
| `specification` (new) | +2 (`BookSpecifications`, `BookSpecificationBuilder`) |
| `repository` | 0 new (edit `BookRepository`) |
| `service` | 0 new (edit `BookService` + `BookServiceImpl`) |
| `controller` | 0 new (edit `BookController`) |
| `test` | +1 (`BookSearchSpecificationTest`); edit 2 existing test classes |
| **Total new files** | **4 new Java source files** (3 main + 1 test), plus edits to 4 existing files |

---

## 7. The Specifications (required + guidance)

### 7.1 Why specifications instead of derived queries

`findByTitleContaining` is fine for **one** fixed filter. The moment filters are **optional and
combinable**, derived methods explode (5 optional filters → 32 combinations). A `Specification` is a
composable `WHERE`-fragment object; you keep one factory per filter (5 total) and combine **only the
present ones** at runtime. That is linear, not combinatorial.

### 7.2 Anatomy of a `Specification<Book>` factory

Each factory returns a lambda `(root, query, cb) -> predicate`:

- **`root`** — the query root (`FROM Book`). Navigate columns with `root.get("title")` and associations
  with `root.join("author")` / `root.join("categories")` (returns a `jakarta.persistence.criteria.Join`).
- **`cb`** — the `CriteriaBuilder`: `cb.like`, `cb.equal`, `cb.lower`, `cb.between`,
  `cb.greaterThanOrEqualTo`, `cb.lessThanOrEqualTo`, `cb.or`.
- **`query`** — used only to set `query.distinct(true)` for the many-to-many join.

Case-insensitive contains is `cb.like(cb.lower(path), "%" + value.toLowerCase() + "%")`. The author
factory joins `author` and OR-s a `like` over `firstName` and `lastName`.

### 7.3 Composing with the builder and `Specification.unrestricted()`

The builder starts from **`Specification.unrestricted()`** — the 4.0 base that matches every row (the
modern replacement for the old `where(null)` idiom) — and `.and(...)`s a predicate **only** for present
filters. Because the base already matches everything, the empty-criteria case (no filters) returns every
book with no special handling. `StringUtils.hasText(...)` decides "present" for text filters;
`min != null || max != null` decides it for each numeric range.

## 8. Two Criteria Pitfalls This Lab Teaches

### 8.1 Many-to-many join duplicates → `query.distinct(true)`

Joining the `@ManyToMany categories` collection produces **one result row per matching join row**, so a
book linked to two categories can come back **twice**. Inside `hasCategory`, set
**`query.distinct(true)`** (guarded by `if (query != null)`) so the query collapses duplicates and each
matching book appears exactly once. The `categoryJoinHasNoDuplicates` test proves this (Section 6.6.1).

### 8.2 Collection fetch + pagination → use list mapping

Lab 3's rule: **do not fetch a collection together with pagination** — Hibernate cannot paginate a
collection join in SQL and silently paginates in memory. Search is paginated, so `searchBooks` maps with
the **list mapping** (`mapToListResponse`): only `author`/`publisher` are expanded and `categories` is an
empty list. This is exactly how `getAllBooks` already behaves. (Filtering *by* category via the join is
fine — the `distinct` join is in the `WHERE`, not a fetch.)

## 9. Service Layer (required + guidance)

`BookServiceImpl` keeps its Lab 3 shape: `@Service`, `@RequiredArgsConstructor` (the four `final`
repositories), class-level `@Transactional(readOnly = true)`. `searchBooks` is a read, so it needs **no**
extra `@Transactional`. Its body is three lines: build the spec via `BookSpecificationBuilder.from(...)`,
call `bookRepository.findAll(spec, pageable)`, and return `toPagedResponse(page)` (reusing the existing
helper, which uses `mapToListResponse` → empty categories). Do not add new mapping helpers.

## 10. Controller (required + guidance)

`BookController` stays thin: add the `@GetMapping("/search")` handler with the seven optional
`@RequestParam`s and the `@PageableDefault(size = 20, sort = "title") Pageable`, build the
`BookSearchCriteria` positionally, and delegate to `bookService.searchBooks(...)`, returning
`ResponseEntity.ok(...)`. Remember the `/search`-before-`/{id}` routing point (Section 6.5).

## 11. Architecture & Code Constraints (required)

- Layered as before: `controller → service → repository → entity`, plus `dto`, `exception`, and the new
  `specification` package.
- The API still exposes **DTOs only** — search returns `PagedResponse<BookResponseDto>`, never entities.
- Specifications live in the `specification` package; the **builder** owns "which filters are present",
  the **factories** own "what each filter means in Criteria terms". Keep that separation.
- The controller owns the **success** code (`200`); no new error codes are introduced (an absent filter
  is valid, not an error).

## 12. API Contract (required — the authoritative spec)

### 12.1 `GET /api/books/search`

| Method | Path | Success | Errors |
| ------ | ---- | ------- | ------ |
| GET | `/api/books/search` | `200`, paged `BookResponse` | — (any filter combination, including none, is valid) |

**Query parameters** (all optional; absent/blank means "no constraint"):

| Param | Type | Filter semantics |
| ----- | ---- | ---------------- |
| `title` | `String` | case-insensitive contains on book title |
| `author` | `String` | case-insensitive contains on author first **or** last name |
| `category` | `String` | case-insensitive exact match on a category name |
| `minPrice` | `BigDecimal` | price ≥ `minPrice` |
| `maxPrice` | `BigDecimal` | price ≤ `maxPrice` |
| `minYear` | `Integer` | publication year ≥ `minYear` |
| `maxYear` | `Integer` | publication year ≤ `maxYear` |
| `page`, `size`, `sort` | pagination | standard `Pageable` params; defaults `size=20`, `sort=title` |

Multiple filters are **AND-ed**. Providing both bounds of a range gives `between`; providing one gives an
open-ended `≥` / `≤`. **Combining a `category` filter with paging must not duplicate rows** (the join is
`distinct`).

Example: `GET /api/books/search?title=clean&minPrice=40&sort=title,asc` returns books whose title
contains "clean" **and** whose price ≥ 40, paged and sorted by title.

### 12.2 Response shape — the same paged wrapper as Lab 3

The endpoint reuses the Lab 3 `PagedResponse<BookResponseDto>` wrapper (do **not** return Spring's raw
`Page`):

```json
{ "content": [ /* BookResponse items */ ], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "last": true }
```

Each item is a Lab 3 **`BookResponse`**. Because search is paginated (list view), the `categories` array
is **empty**:

```json
{
  "id": 10,
  "title": "Clean Code",
  "isbn": "9780132350884",
  "publicationYear": 2008,
  "price": 42.50,
  "author": { "id": 1, "firstName": "Robert", "lastName": "Martin" },
  "publisher": { "id": 1, "name": "Prentice Hall" },
  "categories": []
}
```

> The authoritative `BookResponse` / `PagedResponse` shapes are exactly Lab 3's (Section 13.5–13.6 there).
> The only difference for search is that `categories` is empty in the list view — see Section 8.2.

## 13. Business Rules (required)

- Every filter is **optional**; a `null`/blank filter imposes no constraint. No filters → all books.
- Text filters (`title`, `author`, `category`) are **case-insensitive**; `title` and `author` are
  *contains* matches, `category` is an *exact* name match.
- `author` matches the **first OR last** name.
- Price and year are **open-ended ranges**: both bounds → `between`; one bound → `≥` or `≤`.
- Multiple filters combine with **AND**.
- Filtering by `category` (a many-to-many join) must **not** produce duplicate books (`distinct`).
- Search results are **paginated and sorted** and use the `PagedResponse` wrapper; the list view leaves
  `categories` empty.

## 14. Testing (required + guidance)

Provide/extend automated tests at three levels; all must pass with `mvn test`, using H2 for the DB layer.

### 14.1 Spring Boot 4.0 test imports (different from 3.x!)

```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;     // @DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;    // TestEntityManager
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;        // @WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean;   // @MockitoBean (unchanged)
import org.springframework.data.jpa.domain.Specification;                    // Specification<T>
import tools.jackson.databind.ObjectMapper;                                  // Jackson 3! (was com.fasterxml.jackson...)
```

### 14.2 `@DataJpaTest` — the end-to-end specification test

`BookSearchSpecificationTest` (Section 6.6.1) starts only the JPA layer against H2, seeds data via
`TestEntityManager`, and asserts the full pipeline `criteria → builder → findAll(spec, …)`. Cover each
filter alone, no-filters (all), blank-text-ignored, both/min-only/max-only price, year range,
multi-filter AND, the no-duplicates category join, and pagination+sort.

### 14.3 Service test (Mockito) and web test (`@WebMvcTest`)

- **Service** (`BookServiceImplTest`, `@ExtendWith(MockitoExtension.class)`): add the `searchBooks` case
  (Section 6.6.2) — stub `findAll(Specification, Pageable)`, assert delegation and that categories are
  empty.
- **Web** (`BookControllerTest`, `@WebMvcTest(BookController.class)`, service mocked via `@MockitoBean`):
  add `search` and `searchNoParams` (Section 6.6.3) — assert the paged shape and, with an
  `ArgumentCaptor`, that every optional param binds into the `BookSearchCriteria`.

## 15. Suggested Build Order

1. Start from a working Lab 3 project (or the reference) and confirm `mvn test` is green.
2. Add the `BookSearchCriteria` record (Section 6.1).
3. Create the `specification` package: write `BookSpecifications` factories (Section 6.2), then
   `BookSpecificationBuilder` (Section 6.2).
4. Extend `BookRepository` with `JpaSpecificationExecutor<Book>` (Section 6.3).
5. Add `searchBooks` to `BookService` / `BookServiceImpl` (Section 6.4).
6. Add the `/search` handler to `BookController` (Section 6.5).
7. Write `BookSearchSpecificationTest`; add the service and controller search cases (Section 6.6).
8. Run `mvn test` and exercise the endpoint manually (Section 16).

## 16. Manual Testing Flow

With the app running and a few books seeded (Section 19 of Lab 3 covers seeding):

1. `GET /api/books/search` → `200`, all books, paged.
2. `GET /api/books/search?title=clean` → only titles containing "clean".
3. `GET /api/books/search?author=martin` → books by Robert Martin **and** Martin Fowler.
4. `GET /api/books/search?category=Software` → books in Software; a multi-category book appears once.
5. `GET /api/books/search?minPrice=40&maxPrice=50` → books priced 40–50.
6. `GET /api/books/search?minYear=2010` → books published in 2010 or later.
7. `GET /api/books/search?title=clean&minPrice=40` → AND of both filters.
8. `GET /api/books/search?sort=title,asc&size=2&page=0` → first page of 2, sorted; `categories` empty.

## 17. Acceptance Criteria (how it will be graded)

- [ ] The project builds and `mvn test` is green (all Lab 3 tests **plus** the new search tests).
- [ ] `BookSearchCriteria` is an immutable `record` with the seven components in the documented order.
- [ ] `BookSpecifications` is a final, non-instantiable utility class with the five documented static
      factories, each returning `Specification<Book>` with the documented Criteria behaviour.
- [ ] `hasCategory` sets `query.distinct(true)`; `priceBetween` / `publicationYearBetween` handle both,
      min-only, and max-only.
- [ ] `BookSpecificationBuilder.from(...)` starts from `Specification.unrestricted()` and `.and(...)`s a
      predicate only for present filters (text via `StringUtils.hasText`, ranges via null checks); no
      special "return everything" branch.
- [ ] `BookRepository` extends `JpaSpecificationExecutor<Book>` **and** keeps every Lab 3 method and
      `@EntityGraph` finder unchanged.
- [ ] `BookService`/`BookServiceImpl` expose `searchBooks(BookSearchCriteria, Pageable)`, which builds
      the spec, calls `findAll(spec, pageable)`, and maps with the **list** mapping (categories empty).
- [ ] `BookController` exposes `GET /api/books/search` with all seven optional `@RequestParam`s and a
      `@PageableDefault(size = 20, sort = "title")` `Pageable`, builds the criteria, and delegates.
- [ ] `GET /api/books/search` returns `200` with the `PagedResponse<BookResponseDto>` shape for any
      filter combination, including none; combining a `category` filter with paging does not duplicate
      rows.
- [ ] Tests exist and pass at all three levels: `@DataJpaTest` (`BookSearchSpecificationTest`), Mockito
      service (`searchBooks`), `@WebMvcTest` (`search`, `searchNoParams`).

## 18. Optional Stretch Goals (not graded)

1. Add an `isbn` exact-match filter and a `publisher` name filter as two more `Specification` factories.
2. Switch a filter to use **`PredicateSpecification<T>`** (the 4.0 no-`CriteriaQuery` variant) and
   discuss when it is appropriate (non-paginated, no `distinct` needed).
3. Replace string attribute names (`root.get("title")`) with the **JPA static metamodel**
   (`Book_.title`) for compile-time-checked paths.
4. Add a `@DataJpaTest` that asserts the `category` join runs a single `distinct` SQL statement.

## 19. Instructor Notes

Lab 3 taught the data layer (relationships, fetch strategy, N+1). Lab 4 adds the *querying* skill that
real APIs need: **dynamic, composable filters** via the Criteria API and `Specification`. The student
should leave able to (a) justify specifications over a combinatorial pile of derived methods, (b) build
type-safe predicates with `Root`/`CriteriaBuilder`/`Join`, (c) compose them from `Specification.unrestricted()`
adding only present filters, and (d) name the two Criteria pitfalls (`distinct` on the many-to-many join;
no collection fetch while paginating → list mapping). The increment is deliberately small (4 new files +
4 edits) so the focus stays on the new concept. Note the Spring Boot 4.0 / Spring Data JPA 4.0 specifics
(Sections 5.1, 5.2, 14.1): `Specification.unrestricted()` replaces `where(null)`, `PredicateSpecification`
is new, and the test-slice + Jackson 3 packages are the 4.0 layout.
