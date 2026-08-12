# Course 04 — Dynamic Search with JPA Specifications & the Criteria API

> **Learn first.** This is the study material you read *before* you build Lab 4. By the end you will
> understand how to turn five optional query parameters into one SQL `WHERE` clause assembled at
> runtime — without writing a separate repository method for every possible combination of filters.

## What you'll learn

We are extending the Lab 3 Bookstore (`Author 1‑N Book N‑1 Publisher`, `Book N‑N Category`) with a
single new endpoint:

```
GET /api/books/search?title=clean&author=martin&category=Software&minPrice=40&maxPrice=50&minYear=2000&maxYear=2020
```

Every parameter is optional. The client may send all of them, some of them, or none. The result is
a paginated `PagedResponse<BookResponseDto>`, exactly like `GET /api/books`.

The seven modules below build up the tools you need:

1. **Why dynamic queries** — why derived queries and `@Query` collapse under combinatorial explosion.
2. **The JPA Criteria API from zero** — building a `WHERE` clause as type‑safe Java objects.
3. **The `Specification<T>` interface** — a single reusable `WHERE` fragment as a lambda.
4. **Composing Specifications** — `.and()`, `.or()`, `.not()`, `allOf`/`anyOf`, and `unrestricted()`.
5. **`JpaSpecificationExecutor`** — wiring the executor into the repository.
6. **A `SearchCriteria` + builder pattern** — mapping nullable params into one composed Specification.
7. **Choosing the right tool** — Specifications vs derived queries vs `@Query`, plus two Criteria pitfalls.

You already know layered CRUD, JPA relationships, `Pageable`/`Page`, and `@DataJpaTest` from Labs 1–3.
You have **never** seen the Criteria API or Specifications — that is what we fix here.

---

## 1. Why dynamic queries: the combinatorial explosion of derived queries

In Lab 3 you wrote derived queries like `findByAuthorId(Long, Pageable)` and `findByCategoriesId(...)`.
Those work beautifully because each is a **fixed** filter: exactly one criterion, always present.

Now look at the search requirement. We have **five** independent, **optional** filters:

| Filter | Source param(s) | Meaning |
|--------|-----------------|---------|
| title | `title` | case-insensitive "title contains" |
| author | `author` | case-insensitive match on first **or** last name |
| category | `category` | case-insensitive exact match on a category name |
| price range | `minPrice`, `maxPrice` | open-ended range, either bound may be absent |
| year range | `minYear`, `maxYear` | open-ended range, either bound may be absent |

If you tried to cover every *combination* with a derived query, the method names explode. With N
optional filters there are **2^N** combinations (each filter is either present or absent):

```java
// The road to madness — DO NOT do this:
Page<Book> findByTitleContainingIgnoreCase(String title, Pageable p);
Page<Book> findByTitleContainingIgnoreCaseAndPriceBetween(String t, BigDecimal lo, BigDecimal hi, Pageable p);
Page<Book> findByTitleContainingIgnoreCaseAndAuthor_LastNameContainingIgnoreCase(...);
Page<Book> findByTitleContainingIgnoreCaseAndAuthor_LastNameContainingIgnoreCaseAndPriceBetween(...);
// ... and dozens more, one per subset of filters
```

With 5 filters that is **32** method names, and each one is a wall of `AndXyz` you have to read
character by character. Add a sixth filter and it doubles to 64. The `author` "first OR last name"
rule alone already breaks the clean derived-query naming. This does not scale.

The `@Query` escape hatch is no better here. You *can* write a JPQL string with conditionals baked in:

```java
// Brittle: every filter must be "neutralised" when absent, and the string is unchecked.
@Query("""
    select b from Book b
    where (:title is null or lower(b.title) like lower(concat('%', :title, '%')))
      and (:minPrice is null or b.price >= :minPrice)
      and (:maxPrice is null or b.price <= :maxPrice)
      ...
    """)
Page<Book> search(@Param("title") String title, /* ...many params... */ Pageable p);
```

This "compiles" but it is a **string**: a typo in a property name (`b.titel`) is found only at
runtime, the `:param is null or ...` pattern repeats endlessly, and joins (author, categories)
make it worse. The query is also paying for filters that aren't even active.

**The insight:** instead of enumerating combinations, assemble **one** query at runtime from
**independent pieces**, adding a piece *only when its filter is present*. Five small, reusable
fragments — composed with logical AND — cover all 32 combinations with zero duplication. That is
exactly what the JPA **Criteria API** + Spring Data **Specifications** give us.

---

## 2. The JPA Criteria API from zero

The Criteria API is JPA's way of building a query as a **tree of Java objects** instead of a string.
You assemble the `WHERE` clause programmatically, and because every piece is a typed Java method
call, the **compiler** catches many mistakes that a JPQL string would only reveal at runtime.

Five core types do all the work:

| Type | What it is |
|------|------------|
| `CriteriaBuilder` (`cb`) | the factory: makes predicates, comparisons, functions (`like`, `between`, `equal`, `lower`, `or`, …) |
| `CriteriaQuery<?>` (`query`) | the whole query object — lets you set things like `distinct` |
| `Root<Book>` (`root`) | the `FROM Book` — your entry point into the entity's attributes |
| `Path` | a reference to an attribute reached via `root.get("title")` |
| `Join` | a join to a related entity via `root.join("author")` |
| `Predicate` | one boolean condition (or a combination) — the thing a `WHERE` clause is made of |

### Building conditions with `CriteriaBuilder`

You never write SQL/JPQL operators as text. You call methods on `cb`:

```java
// lower(b.title) like '%clean%'
Predicate p1 = cb.like(cb.lower(root.get("title")), "%clean%");

// b.price between 40 and 50
Predicate p2 = cb.between(root.get("price"), new BigDecimal("40"), new BigDecimal("50"));

// b.publicationYear >= 2000
Predicate p3 = cb.greaterThanOrEqualTo(root.get("publicationYear"), 2000);

// b.publicationYear <= 2020
Predicate p4 = cb.lessThanOrEqualTo(root.get("publicationYear"), 2020);

// lower(c.name) = 'software'
Predicate p5 = cb.equal(cb.lower(categoryJoin.get("name")), "software");
```

`cb.lower(...)` wraps a path in a SQL `LOWER(...)` so comparisons become case-insensitive — that is
how we get "contains, ignoring case" for free.

### Joining relationships

To filter on a related entity's columns, you `join` from the root and then `.get(...)` the joined
attribute. For the author "first OR last name" rule we join `Book.author` and `cb.or(...)` two
`like`s — note from the `Book` entity that `author` is a mandatory `@ManyToOne`, so an **inner** join
is correct:

```java
Join<Book, Author> authorJoin = root.join("author");
Predicate matchesAuthor = cb.or(
        cb.like(cb.lower(authorJoin.get("firstName")), "%martin%"),
        cb.like(cb.lower(authorJoin.get("lastName")),  "%martin%"));
```

### Why the compiler helps

In JPQL, `lower(b.titel)` is a string — it ships, runs, and blows up at request time. In the
Criteria API the structure is Java: `cb.lower(...)`, `cb.between(...)`, `root.join(...)` are real
method calls with real argument types. A mistyped *method* won't compile, and you get IDE
autocomplete and refactoring support. (Attribute *names* in `root.get("title")` are still strings —
the JPA static metamodel can make even those type-safe, but this lab stays with string attribute
names to keep things readable.)

---

## 3. The `Specification<T>` interface in Spring Data JPA 4.0

The raw Criteria API needs all three objects (`root`, `query`, `cb`) handed to you. Spring Data wraps
that in a tiny, composable interface:

```java
package org.springframework.data.jpa.domain;

public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
    // plus default and(...)/or()/not() and static factories — see Module 4
}
```

Read a `Specification<Book>` as **"one reusable `WHERE` fragment."** Because it is a single-method
interface, you implement it as a **lambda** that returns one `Predicate`:

```java
import org.springframework.data.jpa.domain.Specification;

// "title contains <title>", case-insensitive — a self-contained WHERE fragment.
public static Specification<Book> titleContains(String title) {
    String pattern = "%" + title.toLowerCase() + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern);
}
```

The reference implementation collects these factories in a small utility class,
`BookSpecifications`, one method per filter. Each method **assumes its argument is present** (the
builder only calls it after checking the filter is non-null/non-blank), so a factory never returns a
null predicate. Here are two more, straight from the reference:

```java
// Many-to-many category match (note query.distinct(true) — explained in Module 7).
public static Specification<Book> hasCategory(String category) {
    String wanted = category.toLowerCase();
    return (root, query, cb) -> {
        if (query != null) {
            query.distinct(true);
        }
        Join<Book, Category> categoryJoin = root.join("categories");
        return cb.equal(cb.lower(categoryJoin.get("name")), wanted);
    };
}

// Open-ended price range: either bound may be null, but the builder guarantees at least one is set.
public static Specification<Book> priceBetween(BigDecimal min, BigDecimal max) {
    return (root, query, cb) -> {
        if (min != null && max != null) {
            return cb.between(root.get("price"), min, max);
        }
        if (min != null) {
            return cb.greaterThanOrEqualTo(root.get("price"), min);
        }
        return cb.lessThanOrEqualTo(root.get("price"), max);
    };
}
```

> **Note the lambda parameter order is always `(root, query, cb)`** — matching
> `toPredicate(Root, CriteriaQuery, CriteriaBuilder)`. Inside the lambda you have the full Criteria
> API from Module 2 at your disposal.

The `publicationYearBetween(Integer, Integer)` factory follows the exact same open-ended shape as
`priceBetween`, operating on `root.get("publicationYear")`.

---

## 4. Composing Specifications, and the 4.0 way to say "no filter"

A single fragment isn't a search; we need to **combine** fragments. `Specification` gives you the
boolean algebra as default methods and static factories:

| Operation | Meaning |
|-----------|---------|
| `specA.and(specB)` | both fragments must match (logical AND) |
| `specA.or(specB)` | either fragment matches (logical OR) |
| `spec.not()` | negate a fragment |
| `Specification.allOf(a, b, c)` | AND a whole collection of specs together |
| `Specification.anyOf(a, b, c)` | OR a whole collection of specs together |
| `Specification.unrestricted()` | the **"match everything"** base (NEW in 4.0) |

### `unrestricted()` replaces the old `where(null)`

This is the single most important 4.0 change for this lab. In older Spring Data you started a dynamic
spec from `Specification.where(null)` and dealt with nulls as you composed. In **Spring Data JPA 4.0**
that idiom is gone; you start from:

```java
Specification<Book> spec = Specification.unrestricted();   // matches every row
```

`unrestricted()` is a real spec that imposes no constraint. Because the base already matches all rows,
the "no filters supplied" case **naturally returns everything** — there is no special "return all"
branch to write. You then `.and(...)` a fragment for each filter that is actually present:

```java
Specification<Book> spec = Specification.unrestricted();
if (titlePresent)  spec = spec.and(BookSpecifications.titleContains(title));
if (authorPresent) spec = spec.and(BookSpecifications.authorNameContains(author));
// ...absent filters add nothing, so they cost nothing in the generated SQL.
```

That is the **null-skipping idea**: an absent filter contributes *no* predicate, so it does not
appear in the `WHERE` clause at all.

### Recognise `PredicateSpecification<T>` (new in 4.0)

Spring Data JPA 4.0 also adds a sibling interface:

```java
public interface PredicateSpecification<T> {
    Predicate toPredicate(Root<T> root, CriteriaBuilder cb);   // NO CriteriaQuery argument
}
```

It is a lighter spec for cases where you don't need the `CriteriaQuery` (e.g. simple `delete`/`update`
or `exists` style predicates). **You should recognise it**, but this lab standardises on
`Specification<T>` for two reasons: (1) our search is **paginated through the repository**, which uses
`Specification`; and (2) our `hasCategory` fragment needs the `CriteriaQuery` to call
`query.distinct(true)` — something `PredicateSpecification` cannot give you.

---

## 5. `JpaSpecificationExecutor` and wiring it into a repository

A `Specification` is just a `WHERE` fragment; something has to **run** it. That something is
`JpaSpecificationExecutor<T>`, a second interface you add to your repository alongside `JpaRepository`:

```java
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    // ...existing Lab 3 methods unchanged...
}
```

Extending it unlocks Specification-aware finders, the two we care about being:

```java
Page<Book> findAll(Specification<Book> spec, Pageable pageable);   // paginated search
List<Book> findAll(Specification<Book> spec, Sort sort);           // sorted, unpaginated
```

When you call `findAll(spec, pageable)`, Spring Data invokes your spec's `toPredicate(root, query, cb)`
and **plugs the returned `Predicate` into the query's `WHERE` clause**, then applies the `Pageable`'s
paging and sorting on top. The executor is where your composed fragment becomes a live SQL query.

### Coexistence with Lab 3

`JpaSpecificationExecutor` **coexists** with everything from Lab 3 — they all live on the same
repository and keep working unchanged. The reference `BookRepository` does exactly this:

```java
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    boolean existsByIsbn(String isbn);
    boolean existsByAuthorId(Long authorId);
    boolean existsByPublisherId(Long publisherId);

    // Lab 3 @EntityGraph finders — still here, still working.
    @Override
    @EntityGraph(attributePaths = {"author", "publisher"})
    Page<Book> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "publisher"})
    Page<Book> findByCategoriesId(Long categoryId, Pageable pageable);
    // ...
}
```

So one repository now offers: derived queries, `@Query`/`@EntityGraph` finders, **and**
Specification-driven dynamic search.

---

## 6. A `SearchCriteria` + builder pattern for optional filters

Now we tie Modules 1–5 together into the actual feature. Two ideas:

1. Carry the raw, nullable filters in an **immutable record**.
2. A **builder** turns that record into one composed `Specification`, starting from `unrestricted()`
   and adding a fragment *only when its filter is present*.

> **Where these classes live** (base package `com.example._4_specifications`): the
> `BookSearchCriteria` record goes in the existing **`dto`** package, while `BookSpecifications` and
> `BookSpecificationBuilder` go in a **new `specification`** package. The entities (`Book`, `Author`,
> `Category`) are the Lab 3 ones in the **`model`** package.

### The criteria record

The reference uses a plain `record` where **every component is nullable** — `null` (or, for text
fields, blank) means "the caller did not supply this filter":

```java
public record BookSearchCriteria(
        String title,
        String author,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minYear,
        Integer maxYear) {
}
```

The controller binds each optional `@RequestParam` and constructs this record — nothing fancy, just
collecting the inputs into one typed value.

### The builder

`BookSpecificationBuilder.from(criteria)` is the heart of the feature. It starts from
`unrestricted()` and `.and(...)`s one fragment per present filter. Text presence is checked with
`org.springframework.util.StringUtils.hasText(...)` (so blank/whitespace counts as absent); range
filters are added when **either** bound is non-null:

```java
public static Specification<Book> from(BookSearchCriteria criteria) {
    Specification<Book> spec = Specification.unrestricted();

    if (StringUtils.hasText(criteria.title())) {
        spec = spec.and(BookSpecifications.titleContains(criteria.title()));
    }
    if (StringUtils.hasText(criteria.author())) {
        spec = spec.and(BookSpecifications.authorNameContains(criteria.author()));
    }
    if (StringUtils.hasText(criteria.category())) {
        spec = spec.and(BookSpecifications.hasCategory(criteria.category()));
    }
    if (criteria.minPrice() != null || criteria.maxPrice() != null) {
        spec = spec.and(BookSpecifications.priceBetween(criteria.minPrice(), criteria.maxPrice()));
    }
    if (criteria.minYear() != null || criteria.maxYear() != null) {
        spec = spec.and(BookSpecifications.publicationYearBetween(criteria.minYear(), criteria.maxYear()));
    }

    return spec;
}
```

Trace what this produces:

- **No filters** → `spec` stays `unrestricted()` → SQL has no `WHERE` → every book returned.
- **`title=clean` only** → one `LIKE` → just the title fragment in the `WHERE`.
- **`title=clean` & `minPrice=40`** → two `.and(...)`ed fragments → `WHERE ... LIKE ... AND price >= 40`.

Absent filters genuinely add **nothing** to the SQL — that is the payoff of starting from
`unrestricted()` and only joining relationships (author, categories) for filters that are present.

### Calling it from the service

The service stays thin: build the spec, hand it to the executor, map the page. Note it uses the
**LIST mapping** (categories left empty) — exactly like `getAllBooks` — because the search is
paginated (more on why in Module 7):

```java
@Override
public PagedResponse<BookResponseDto> searchBooks(BookSearchCriteria criteria, Pageable pageable) {
    Specification<Book> spec = BookSpecificationBuilder.from(criteria);
    return toPagedResponse(bookRepository.findAll(spec, pageable));
}
```

### Binding the request

The controller maps the seven optional params into the record and delegates. The literal `/search`
path is chosen so it never collides with `/{id}` (see Module 7):

```java
@GetMapping("/search")
public ResponseEntity<PagedResponse<BookResponseDto>> search(
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String author,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Integer minYear,
        @RequestParam(required = false) Integer maxYear,
        @PageableDefault(size = 20, sort = "title") Pageable pageable) {
    BookSearchCriteria criteria = new BookSearchCriteria(
            title, author, category, minPrice, maxPrice, minYear, maxYear);
    return ResponseEntity.ok(bookService.searchBooks(criteria, pageable));
}
```

---

## 7. Specifications vs derived queries vs `@Query`: choosing the right tool

These three approaches are not rivals — each is the right answer for a different shape of query.

| Situation | Best tool | Why |
|-----------|-----------|-----|
| Fixed, simple filter (1–2 always-present criteria) | **Derived query** (`findByAuthorId`) | Zero boilerplate, name *is* the query, fully type-checked by Spring Data |
| Complex but **static** query (fixed joins/projections/aggregation) | **`@Query`** (JPQL) | One readable query; structure never changes at runtime |
| **Runtime-assembled** query with **optional** filters (this lab) | **Specifications** | Compose independent fragments; 2^N combinations, zero duplication |

Reach for Specifications precisely when the *set of active conditions is decided at request time*.
For everything fixed, derived queries and `@Query` remain simpler and clearer — which is why the Lab 3
finders stay exactly as they were.

### Pitfall 1 — a many-to-many join can duplicate rows

`Book.categories` is a `@ManyToMany`. When you `root.join("categories")` and filter, a book that
matches via multiple join rows can come back **once per matching row** — duplicate `Book`s in your
result, and a wrong `totalElements` when paginating. The fix is to make the query distinct, from
*inside* the predicate, which is why `hasCategory` needs the `CriteriaQuery`:

```java
public static Specification<Book> hasCategory(String category) {
    String wanted = category.toLowerCase();
    return (root, query, cb) -> {
        if (query != null) {
            query.distinct(true);          // collapse the duplicate rows from the M-N join
        }
        Join<Book, Category> categoryJoin = root.join("categories");
        return cb.equal(cb.lower(categoryJoin.get("name")), wanted);
    };
}
```

The reference's `@DataJpaTest` proves this: *Clean Architecture* has two categories, yet filtering by
one category returns it **exactly once** (`extracting(Book::getId).doesNotHaveDuplicates()`).

### Pitfall 2 — never fetch a collection while paginating (the Lab 3 rule)

Lab 3 taught: **do not `JOIN FETCH` a collection together with pagination.** Hibernate cannot do
that in SQL, so it silently pulls *all* rows and paginates **in memory** — a correctness and
performance trap. Search is paginated, so the same rule applies: the service uses the **list
mapping** that leaves `categories` empty (just like `getAllBooks`), instead of the detail mapping
that expands them.

```java
// List view: author + publisher are safe @ManyToOne sides; categories deliberately NOT expanded.
private BookResponseDto mapToListResponse(Book book) {
    return new BookResponseDto(
            book.getId(), book.getTitle(), book.getIsbn(), book.getPublicationYear(),
            book.getPrice(),
            new AuthorSummaryDto(book.getAuthor().getId(),
                    book.getAuthor().getFirstName(), book.getAuthor().getLastName()),
            new PublisherSummaryDto(book.getPublisher().getId(), book.getPublisher().getName()),
            List.of());   // categories intentionally empty in the paginated list/search view
}
```

> Note the distinction: `hasCategory` **joins** categories to *filter* (which is fine and necessary),
> but the response mapping does **not fetch and expand** the categories collection. Joining to filter
> ≠ fetching to serialise.

### Route ordering note

A literal path like `GET /api/books/search` takes **priority** over the pattern `GET /api/books/{id}`,
so the two mappings never collide — `search` is matched as the concrete route, not as an `{id}` of
`"search"`.

---

## Testing notes (Spring Boot 4.0 packages)

The reference verifies this feature across three slices. Mind the **renamed 4.0 test-slice packages**:

```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;   // repository slice
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;  // seeding entities
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;      // controller slice
import tools.jackson.databind.ObjectMapper;                                // Jackson 3 (NOT com.fasterxml)
```

- **`@DataJpaTest`** runs the real stack `BookSearchCriteria → BookSpecificationBuilder →
  findAll(spec, …)` against H2: each filter alone, no filters (returns all), blank filters ignored,
  a multi-filter AND, the no-duplicates check, and that results stay paginated/sorted.
- **Service test** (`@ExtendWith(MockitoExtension.class)`) mocks the repository and asserts
  `searchBooks` delegates to `findAll(any(Specification.class), any(PageRequest.class))` and maps the
  list view (categories empty).
- **`@WebMvcTest`** asserts every optional `@RequestParam` binds into a `BookSearchCriteria`
  (captured with `ArgumentCaptor`) and that `/api/books/search` with no params still returns a page.

---

## Checklist: you are ready to build Lab 4 when you can...

- [ ] Explain why N optional filters yield 2^N derived-query combinations, and why one runtime-assembled query avoids that.
- [ ] Name the five core Criteria types — `CriteriaBuilder`, `CriteriaQuery`, `Root`, `Path`/`Join`, `Predicate` — and say what each does.
- [ ] Build a `Predicate` with `cb.like`, `cb.between`, `cb.equal`, `cb.greaterThanOrEqualTo`, `cb.lessThanOrEqualTo`, `cb.lower`, and `cb.or`.
- [ ] Implement a `Specification<Book>` as a `(root, query, cb) -> …` lambda returning one `Predicate`, and read it as "a reusable WHERE fragment".
- [ ] Compose specs with `.and()`/`.or()`/`.not()` (and recognise `allOf`/`anyOf`), and start from `Specification.unrestricted()` instead of the legacy `where(null)`.
- [ ] Recognise `PredicateSpecification<T>` and explain why this lab uses `Specification<T>` (pagination + needing `query.distinct(true)`).
- [ ] Add `JpaSpecificationExecutor<Book>` to the repository and call `findAll(spec, pageable)` / `findAll(spec, sort)` alongside the Lab 3 finders.
- [ ] Map nullable params into a `BookSearchCriteria` record and build the spec with the null-skipping pattern (`StringUtils.hasText`, `min/max != null`).
- [ ] Apply `query.distinct(true)` to stop the many-to-many category join from duplicating rows.
- [ ] State the Lab 3 rule — never fetch a collection while paginating — and use the empty-`categories` list mapping for search results.
- [ ] Pick the right tool: derived query (fixed simple), `@Query` (complex static), Specifications (runtime optional filtering).
