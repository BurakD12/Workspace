# Question 2 — Library Management System (Advanced)

**Difficulty:** Intermediate–Advanced  
**Topics:** Multi-table JOINs · Transactions · Generic DAO · HikariCP Connection Pool · Custom Exceptions · Service Layer · Config File · JUnit 5 + Mockito  
**Estimated Time:** 4–6 hours

---

## Overview

You will extend the basic Library Book Manager into a full **Library Management System**.  
The system now supports library **members** who can **borrow and return books**.

New concepts introduced on top of Question 1:

| Concept | Where It Appears |
|---|---|
| Multi-table SQL (JOIN, FK) | `loans` table joining `books` and `members` |
| Transactions (`commit`/`rollback`) | `LoanService.borrowBook()` and `returnBook()` |
| Generic DAO interface | `GenericDAO<T>` extended by all DAO interfaces |
| Connection pooling (HikariCP) | `ConnectionPool` replaces `DriverManager` calls |
| Custom exception hierarchy | `DatabaseException`, `RecordNotFoundException`, `BookNotAvailableException` |
| Service layer | `BookService`, `LoanService` hold business rules |
| Properties config file | `application.properties` holds DB credentials |
| Unit tests with mocking | JUnit 5 + Mockito tests for the service layer |

---

## Database Setup

Run these SQL statements on your PostgreSQL server before starting.

```sql
-- 1. Create the database
CREATE DATABASE library_db;

-- 2. Connect to library_db, then run:

CREATE TABLE IF NOT EXISTS books (
    id           SERIAL PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    author       VARCHAR(100) NOT NULL,
    genre        VARCHAR(50)  NOT NULL,
    publish_year INTEGER      NOT NULL,
    available    BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS members (
    id         SERIAL PRIMARY KEY,
    full_name  VARCHAR(150) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    joined_at  DATE         NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS loans (
    id          SERIAL PRIMARY KEY,
    book_id     INTEGER   NOT NULL REFERENCES books(id),
    member_id   INTEGER   NOT NULL REFERENCES members(id),
    loan_date   TIMESTAMP NOT NULL DEFAULT NOW(),
    return_date TIMESTAMP             -- NULL means the book has not been returned yet
);
```

**Relationship diagram:**

```
members ──< loans >── books
  (1)       (M:M)      (1)
```

- One member can have many loans.
- One book can appear in many loans (but only one **active** loan at a time — enforced by the service layer).
- `return_date IS NULL` means the book is currently borrowed.

---

## Package and File Structure

```
src/
└── main/
    └── java/
        └── com/
            └── library/
                ├── LibraryApp.java                  ← main method
                │
                ├── config/
                │   └── AppConfig.java               ← loads application.properties
                │
                ├── exception/
                │   ├── DatabaseException.java
                │   ├── RecordNotFoundException.java
                │   └── BookNotAvailableException.java
                │
                ├── model/
                │   ├── Book.java
                │   ├── Member.java
                │   └── Loan.java
                │
                ├── dao/
                │   ├── GenericDAO.java               ← generic interface
                │   ├── BookDAO.java
                │   ├── BookDAOImpl.java
                │   ├── MemberDAO.java
                │   ├── MemberDAOImpl.java
                │   ├── LoanDAO.java
                │   └── LoanDAOImpl.java
                │
                ├── service/
                │   ├── BookService.java
                │   └── LoanService.java
                │
                └── util/
                    └── ConnectionPool.java           ← HikariCP wrapper

src/
└── main/
    └── resources/
        └── application.properties                   ← DB credentials

src/
└── test/
    └── java/
        └── com/
            └── library/
                ├── service/
                │   ├── BookServiceTest.java
                │   └── LoanServiceTest.java
```

---

## Maven Dependencies

Add these to your `pom.xml` inside `<dependencies>`:

```xml
<!-- PostgreSQL JDBC Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>

<!-- HikariCP connection pool -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>

<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
```

---

## Part 1 — Configuration File

### `application.properties`

**Location:** `src/main/resources/application.properties`

This plain text file stores all database settings so credentials are never hardcoded in Java source files.

```properties
db.url=jdbc:postgresql://localhost:5432/library_db
db.user=postgres
db.password=your_password
db.pool.size=10
```

| Key | Purpose |
|---|---|
| `db.url` | Full JDBC connection URL |
| `db.user` | PostgreSQL username |
| `db.password` | PostgreSQL password — replace with your actual password |
| `db.pool.size` | Maximum number of connections HikariCP keeps open |

---

### `AppConfig` — Package `com.library.config`

Reads `application.properties` from the classpath exactly once (when the class is first loaded) and exposes the values through static getters.

#### How it works

Java's `ClassLoader.getSystemResourceAsStream("application.properties")` finds the file on the classpath (Maven puts `src/main/resources` on the classpath automatically). The values are loaded into a `java.util.Properties` object.

#### Fields

| Field | Type | Modifier | Description |
|---|---|---|---|
| `props` | `Properties` | `private static final` | Holds all key-value pairs loaded from the file |

#### Static initializer block

A `static { ... }` block runs once when the JVM first loads `AppConfig`. Inside it:
1. Create a `new Properties()` object.
2. Call `ClassLoader.getSystemResourceAsStream("application.properties")` to open the file as an `InputStream`.
3. Call `props.load(inputStream)` to parse the file.
4. If the stream is `null` (file not found), throw `new RuntimeException("application.properties not found on classpath")`.
5. Wrap the `load()` call in a try-with-resources so the stream is closed automatically; catch `IOException` and rethrow as `RuntimeException`.

#### Constructor

| Constructor | Description |
|---|---|
| `private AppConfig()` | Private — prevents instantiation. This class is a static utility. |

#### Methods

| Method | Return Type | Description |
|---|---|---|
| `getUrl()` | `String` | Returns `props.getProperty("db.url")` |
| `getUser()` | `String` | Returns `props.getProperty("db.user")` |
| `getPassword()` | `String` | Returns `props.getProperty("db.password")` |
| `getPoolSize()` | `int` | Returns `Integer.parseInt(props.getProperty("db.pool.size"))` |

---

## Part 2 — Connection Pool

### `ConnectionPool` — Package `com.library.util`

Wraps HikariCP to provide a shared pool of pre-opened database connections. Any DAO can borrow a connection, use it, and return it to the pool just by closing it (try-with-resources does this automatically).

#### Why a pool?

Opening a new TCP connection to PostgreSQL for every query is slow (~50 ms each). A pool keeps a fixed number of connections open and reuses them, reducing overhead to microseconds per borrow.

#### How HikariCP works (brief)

`HikariDataSource` is a `javax.sql.DataSource`. You configure it once with a `HikariConfig` object. After that, calling `dataSource.getConnection()` borrows an idle connection from the pool. When you close that connection, it is returned to the pool — it is **not** actually closed.

#### Fields

| Field | Type | Modifier | Description |
|---|---|---|---|
| `dataSource` | `HikariDataSource` | `private static final` | The single shared pool instance |

#### Static initializer block

Runs once when the class is first loaded:
1. Create `HikariConfig config = new HikariConfig()`.
2. Set `config.setJdbcUrl(AppConfig.getUrl())`.
3. Set `config.setUsername(AppConfig.getUser())`.
4. Set `config.setPassword(AppConfig.getPassword())`.
5. Set `config.setMaximumPoolSize(AppConfig.getPoolSize())`.
6. Set `config.setMinimumIdle(2)` — keep at least 2 connections open even when idle.
7. Set `config.setConnectionTimeout(30000)` — wait at most 30 seconds for a free connection before throwing an exception.
8. Assign `dataSource = new HikariDataSource(config)`.

#### Constructor

| Constructor | Description |
|---|---|
| `private ConnectionPool()` | Private — prevents instantiation. |

#### Methods

| Method | Return Type | Throws | Description |
|---|---|---|---|
| `getConnection()` | `Connection` | `SQLException` | Calls `dataSource.getConnection()` and returns the borrowed connection |

**Usage in a DAO method (example):**
```java
try (Connection conn = ConnectionPool.getConnection();
     PreparedStatement ps = conn.prepareStatement("SELECT * FROM books")) {
    // use ps, rs, etc.
}
// conn is returned to the pool here — not actually closed
```

---

## Part 3 — Custom Exceptions

All three exception classes live in package `com.library.exception`.

---

### `DatabaseException`

The **root** of your custom exception hierarchy. Signals that something went wrong while talking to the database.

| Detail | Value |
|---|---|
| Extends | `Exception` (it is a **checked** exception) |
| Purpose | Base class — catch this type when you want to handle any database-related error |

#### Constructors

| Constructor | Parameters | Body |
|---|---|---|
| `DatabaseException(String message)` | error message | `super(message)` |
| `DatabaseException(String message, Throwable cause)` | message + original exception | `super(message, cause)` |

> **Checked vs unchecked:** Because `DatabaseException` extends `Exception` (not `RuntimeException`), any method that throws it **must** declare `throws DatabaseException` in its signature, and callers must either catch it or propagate it. This forces the programmer to consciously handle database errors.

---

### `RecordNotFoundException`

Thrown when a DAO `findById` (or similar lookup) returns no row.

| Detail | Value |
|---|---|
| Extends | `DatabaseException` |
| Purpose | Signals "the requested row does not exist" — separate from a general DB failure |

#### Constructor

| Constructor | Parameters | Body |
|---|---|---|
| `RecordNotFoundException(String message)` | e.g. `"Book with ID 5 not found"` | `super(message)` |

---

### `BookNotAvailableException`

Thrown by the service layer when a member tries to borrow a book that is already on loan.

| Detail | Value |
|---|---|
| Extends | `RuntimeException` (it is an **unchecked** exception) |
| Purpose | Business-rule violation — the book exists but cannot be borrowed right now |

#### Constructor

| Constructor | Parameters | Body |
|---|---|---|
| `BookNotAvailableException(String message)` | e.g. `"Book 'Dune' is not available"` | `super(message)` |

> **Why unchecked here?** This is a business rule violation that the caller should prevent by checking availability first. Making it unchecked avoids cluttering every call site with `try/catch`.

---

## Part 4 — Model Classes

### `Book` — Package `com.library.model`

Same as Question 1. Keep all fields, constructors, getters/setters, and `toString()` exactly as before.

---

### `Member` — Package `com.library.model`

Represents one row in the `members` table.

#### Fields

| Field | Type | Corresponds To |
|---|---|---|
| `id` | `int` | `id` column |
| `fullName` | `String` | `full_name` column |
| `email` | `String` | `email` column |
| `joinedAt` | `java.time.LocalDate` | `joined_at` column |

#### Constructors

| Constructor | Parameters | Purpose |
|---|---|---|
| `Member()` | none | Default — needed by DAO when mapping from `ResultSet` |
| `Member(String fullName, String email)` | 2 params | Used when registering a new member; `id` defaults to `0`, `joinedAt` defaults to `LocalDate.now()` |

#### Methods

All standard getters and setters for each field, plus:

| Method | Return Type | Description |
|---|---|---|
| `getId()` | `int` | Returns member id |
| `setId(int id)` | `void` | Sets member id |
| `getFullName()` | `String` | Returns full name |
| `setFullName(String fullName)` | `void` | Sets full name |
| `getEmail()` | `String` | Returns email |
| `setEmail(String email)` | `void` | Sets email |
| `getJoinedAt()` | `LocalDate` | Returns join date |
| `setJoinedAt(LocalDate joinedAt)` | `void` | Sets join date |
| `toString()` | `String` | Format: `Member{id=1, name='Alice Smith', email='alice@example.com', joined=2024-01-15}` |

---

### `Loan` — Package `com.library.model`

Represents one row in the `loans` table, but with the full `Book` and `Member` objects embedded (not just their IDs). This is the result of a JOIN query.

#### Fields

| Field | Type | Corresponds To |
|---|---|---|
| `id` | `int` | `loans.id` |
| `book` | `Book` | Full `Book` object (populated from JOIN) |
| `member` | `Member` | Full `Member` object (populated from JOIN) |
| `loanDate` | `java.time.LocalDateTime` | `loans.loan_date` |
| `returnDate` | `java.time.LocalDateTime` | `loans.return_date` — `null` means not yet returned |

#### Constructors

| Constructor | Parameters | Purpose |
|---|---|---|
| `Loan()` | none | Default — needed by DAO |
| `Loan(Book book, Member member)` | 2 params | Used when creating a new loan; `id=0`, `loanDate=LocalDateTime.now()`, `returnDate=null` |

#### Methods

All standard getters and setters, plus:

| Method | Return Type | Description |
|---|---|---|
| `getId()` | `int` | Returns loan id |
| `setId(int id)` | `void` | Sets loan id |
| `getBook()` | `Book` | Returns the borrowed book |
| `setBook(Book book)` | `void` | Sets the book |
| `getMember()` | `Member` | Returns the borrowing member |
| `setMember(Member member)` | `void` | Sets the member |
| `getLoanDate()` | `LocalDateTime` | Returns when the book was borrowed |
| `setLoanDate(LocalDateTime loanDate)` | `void` | Sets the loan date |
| `getReturnDate()` | `LocalDateTime` | Returns when returned, or `null` if still out |
| `setReturnDate(LocalDateTime returnDate)` | `void` | Sets the return date |
| `isActive()` | `boolean` | Returns `returnDate == null` — convenience method |
| `toString()` | `String` | Format: `Loan{id=1, book='Dune', member='Alice Smith', loaned=2024-06-01T10:00, returned=null}` |

---

## Part 5 — Generic DAO Interface

### `GenericDAO<T>` — Package `com.library.dao`

A generic interface that defines the four operations every DAO must support. The type parameter `T` will be substituted by `Book`, `Member`, etc.

```
GenericDAO<T>
    ↑               ↑
BookDAO         MemberDAO
```

#### Type Parameter

| Parameter | Meaning |
|---|---|
| `T` | The model class this DAO manages (e.g., `Book`, `Member`) |

#### Method Signatures

| Method | Parameters | Return Type | Throws | Description |
|---|---|---|---|---|
| `insert(T entity)` | `T entity` | `void` | `DatabaseException` | Inserts a new row; must set the generated `id` back on the entity |
| `findById(int id)` | `int id` | `T` | `DatabaseException` | Returns the entity with that id, or `null` if not found |
| `findAll()` | none | `List<T>` | `DatabaseException` | Returns all rows ordered by `id` ascending |
| `delete(int id)` | `int id` | `void` | `DatabaseException` | Deletes the row with that id |

> All methods declare `throws DatabaseException`. This is your checked custom exception — callers must handle it.

---

### `BookDAO` — Package `com.library.dao`

Extends `GenericDAO<Book>` and adds book-specific operations.

```java
public interface BookDAO extends GenericDAO<Book>
```

#### Additional Methods

| Method | Parameters | Return Type | Throws | Description |
|---|---|---|---|---|
| `findByGenre(String genre)` | `String genre` | `List<Book>` | `DatabaseException` | Returns all books whose genre matches (case-insensitive, use `ILIKE`) ordered by title |
| `update(Book book)` | `Book book` | `void` | `DatabaseException` | Updates all editable fields for the row whose `id` matches `book.getId()` |
| `count()` | none | `int` | `DatabaseException` | Returns total number of rows in `books` |

---

### `MemberDAO` — Package `com.library.dao`

Extends `GenericDAO<Member>` and adds member-specific operations.

```java
public interface MemberDAO extends GenericDAO<Member>
```

#### Additional Methods

| Method | Parameters | Return Type | Throws | Description |
|---|---|---|---|---|
| `findByEmail(String email)` | `String email` | `Member` | `DatabaseException` | Returns the member with that exact email, or `null` if not found |
| `update(Member member)` | `Member member` | `void` | `DatabaseException` | Updates `full_name` and `email` for the matching row |

---

### `LoanDAO` — Package `com.library.dao`

Does **not** extend `GenericDAO` because loans have more complex lifecycle operations.

#### Method Signatures

| Method | Parameters | Return Type | Throws | Description |
|---|---|---|---|---|
| `insert(Loan loan, Connection conn)` | `Loan loan`, `Connection conn` | `void` | `DatabaseException` | Inserts a loan row using the **provided** connection (needed for transactions — see below) |
| `findById(int id)` | `int id` | `Loan` | `DatabaseException` | Returns the loan (with full book + member via JOIN), or `null` |
| `findByMember(int memberId)` | `int memberId` | `List<Loan>` | `DatabaseException` | Returns all loans for a member, newest first |
| `findActiveByBook(int bookId)` | `int bookId` | `Loan` | `DatabaseException` | Returns the single active loan for a book (`return_date IS NULL`), or `null` |
| `markReturned(int loanId, Connection conn)` | `int loanId`, `Connection conn` | `void` | `DatabaseException` | Sets `return_date = NOW()` for the loan using the provided connection |

> **Why does `insert` and `markReturned` accept a `Connection`?**  
> The service layer opens one connection, sets `autoCommit = false`, calls both DAO methods on that same connection, then commits or rolls back. If each DAO method opened its own connection, you could not include them in the same transaction.

---

## Part 6 — DAO Implementations

### `BookDAOImpl` — Package `com.library.dao`

Implements `BookDAO`. Every method obtains a connection via `ConnectionPool.getConnection()` inside try-with-resources **except** where noted.

#### Private Helper

| Method | Parameters | Return Type | Throws | Description |
|---|---|---|---|---|
| `mapRow(ResultSet rs)` | `ResultSet rs` | `Book` | `SQLException` | Reads current ResultSet row and returns a populated `Book`. Read columns: `"id"`, `"title"`, `"author"`, `"genre"`, `"publish_year"`, `"available"` |

#### SQL Queries

| Method | SQL |
|---|---|
| `insert` | `INSERT INTO books (title, author, genre, publish_year, available) VALUES (?, ?, ?, ?, ?)` with `Statement.RETURN_GENERATED_KEYS` |
| `findById` | `SELECT * FROM books WHERE id = ?` |
| `findAll` | `SELECT * FROM books ORDER BY id` |
| `findByGenre` | `SELECT * FROM books WHERE genre ILIKE ? ORDER BY title` |
| `update` | `UPDATE books SET title=?, author=?, genre=?, publish_year=?, available=? WHERE id=?` |
| `delete` | `DELETE FROM books WHERE id=?` |
| `count` | `SELECT COUNT(*) FROM books` |

#### Error handling

Catch `SQLException` inside each method and rethrow as:
```java
throw new DatabaseException("Error inserting book", e);
```
Use an appropriate description for each method.

---

### `MemberDAOImpl` — Package `com.library.dao`

Implements `MemberDAO`. Same pattern as `BookDAOImpl`.

#### Private Helper

| Method | Parameters | Return Type | Throws | Description |
|---|---|---|---|---|
| `mapRow(ResultSet rs)` | `ResultSet rs` | `Member` | `SQLException` | Reads current row and returns a `Member`. Use `rs.getDate("joined_at").toLocalDate()` to convert the SQL `DATE` to `LocalDate`. Read columns: `"id"`, `"full_name"`, `"email"`, `"joined_at"` |

#### SQL Queries

| Method | SQL |
|---|---|
| `insert` | `INSERT INTO members (full_name, email, joined_at) VALUES (?, ?, ?)` with `Statement.RETURN_GENERATED_KEYS`. Bind `joinedAt` using `Date.valueOf(member.getJoinedAt())` |
| `findById` | `SELECT * FROM members WHERE id = ?` |
| `findByEmail` | `SELECT * FROM members WHERE email = ?` |
| `findAll` | `SELECT * FROM members ORDER BY id` |
| `update` | `UPDATE members SET full_name=?, email=? WHERE id=?` |
| `delete` | `DELETE FROM members WHERE id=?` |

---

### `LoanDAOImpl` — Package `com.library.dao`

Implements `LoanDAO`. The JOIN query is more complex — read carefully.

#### JOIN Query (used by `findById`, `findByMember`, `findActiveByBook`)

```sql
SELECT
    l.id          AS loan_id,
    l.loan_date,
    l.return_date,
    b.id          AS book_id,
    b.title,
    b.author,
    b.genre,
    b.publish_year,
    b.available,
    m.id          AS member_id,
    m.full_name,
    m.email,
    m.joined_at
FROM loans l
JOIN books   b ON b.id = l.book_id
JOIN members m ON m.id = l.member_id
```

Add `WHERE` / `ORDER BY` clauses per method.

#### Private Helper

| Method | Parameters | Return Type | Throws | Description |
|---|---|---|---|---|
| `mapRow(ResultSet rs)` | `ResultSet rs` | `Loan` | `SQLException` | Builds a full `Loan` object from the JOIN result. Steps: (1) create a `Book` and set all its fields using the aliased column names (`"book_id"`, `"title"`, etc.). (2) create a `Member` and set all its fields (`"member_id"`, `"full_name"`, etc.). (3) create a `Loan`, set `id` from `"loan_id"`, set the book, set the member, set `loanDate` from `rs.getTimestamp("loan_date").toLocalDateTime()`, set `returnDate` as `null` if `rs.getTimestamp("return_date")` is `null`, otherwise `.toLocalDateTime()`. |

#### SQL per Method

| Method | Full SQL |
|---|---|
| `insert` | `INSERT INTO loans (book_id, member_id, loan_date) VALUES (?, ?, ?)` — bind `loan.getBook().getId()`, `loan.getMember().getId()`, `Timestamp.valueOf(loan.getLoanDate())`. Use `RETURN_GENERATED_KEYS`. |
| `findById` | Base JOIN + `WHERE l.id = ?` |
| `findByMember` | Base JOIN + `WHERE l.member_id = ? ORDER BY l.loan_date DESC` |
| `findActiveByBook` | Base JOIN + `WHERE l.book_id = ? AND l.return_date IS NULL` |
| `markReturned` | `UPDATE loans SET return_date = NOW() WHERE id = ?` — uses the provided `Connection` directly, no pool borrow |

#### Connection handling for `insert` and `markReturned`

These methods receive a `Connection` as a parameter. Do **not** open a new connection from the pool. Do **not** close the provided connection — that is the service layer's responsibility. Use the connection directly:
```java
try (PreparedStatement ps = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
    // bind params, execute
}
```

---

## Part 7 — Service Layer

The service layer sits between `LibraryApp` and the DAO layer. It contains **business rules** — logic about *what is allowed* — while DAOs only deal with *how to talk to the database*.

### `BookService` — Package `com.library.service`

#### Constructor

```java
public BookService(BookDAO bookDAO)
```

Stores the injected `BookDAO` as a private field `this.bookDAO`. This makes the service testable — in tests you pass a mock.

#### Fields

| Field | Type | Description |
|---|---|---|
| `bookDAO` | `BookDAO` | Injected via constructor; never reassigned |

#### Methods

| Method | Parameters | Return Type | Throws | Description |
|---|---|---|---|---|
| `addBook(Book book)` | `Book book` | `void` | `DatabaseException` | Validates that `title`, `author`, and `genre` are not blank (trim + isEmpty). If any is blank, throw `new IllegalArgumentException("Title, author, and genre must not be blank")`. Otherwise delegates to `bookDAO.insert(book)`. |
| `getAllBooks()` | none | `List<Book>` | `DatabaseException` | Delegates to `bookDAO.findAll()` |
| `getBookById(int id)` | `int id` | `Book` | `DatabaseException` | Calls `bookDAO.findById(id)`. If result is `null`, throw `new RecordNotFoundException("Book with ID " + id + " not found")` |
| `getBooksByGenre(String genre)` | `String genre` | `List<Book>` | `DatabaseException` | Delegates to `bookDAO.findByGenre(genre)` |
| `updateBook(Book book)` | `Book book` | `void` | `DatabaseException` | Same blank validation as `addBook`, then delegates to `bookDAO.update(book)` |
| `deleteBook(int id)` | `int id` | `void` | `DatabaseException` | Delegates to `bookDAO.delete(id)` |
| `countBooks()` | none | `int` | `DatabaseException` | Delegates to `bookDAO.count()` |

---

### `LoanService` — Package `com.library.service`

Handles borrowing and returning, which require **transactions** (multiple DB changes that must succeed or fail together).

#### Constructor

```java
public LoanService(LoanDAO loanDAO, BookDAO bookDAO, MemberDAO memberDAO)
```

Stores all three as private fields.

#### Fields

| Field | Type | Description |
|---|---|---|
| `loanDAO` | `LoanDAO` | For loan operations |
| `bookDAO` | `BookDAO` | For checking / updating book availability |
| `memberDAO` | `MemberDAO` | For verifying the member exists |

#### Methods

---

##### `borrowBook(int bookId, int memberId)`

| Detail | Value |
|---|---|
| Return type | `Loan` |
| Throws | `DatabaseException`, `BookNotAvailableException` |

**Step-by-step logic:**

1. Call `bookDAO.findById(bookId)`. If `null`, throw `RecordNotFoundException("Book with ID " + bookId + " not found")`.
2. Call `memberDAO.findById(memberId)`. If `null`, throw `RecordNotFoundException("Member with ID " + memberId + " not found")`.
3. If `book.isAvailable()` is `false`, throw `new BookNotAvailableException("Book '" + book.getTitle() + "' is not currently available")`.
4. **Open a transaction:**
   ```
   Connection conn = ConnectionPool.getConnection()
   conn.setAutoCommit(false)
   ```
5. Inside a try block:
   - Create `Loan loan = new Loan(book, member)`.
   - Call `loanDAO.insert(loan, conn)` — this sets `loan.getId()` via generated keys.
   - Set `book.setAvailable(false)`.
   - Call `bookDAO.update(book)` (this method opens its own connection from the pool — that is fine, it is a separate atomic update).
   - Call `conn.commit()`.
   - Return `loan`.
6. In the catch block (catch `Exception e`):
   - Call `conn.rollback()`.
   - Rethrow as `new DatabaseException("Failed to borrow book", e)`.
7. In the finally block:
   - Call `conn.setAutoCommit(true)`.
   - Close `conn`.

> **Why rollback?** If `loanDAO.insert` succeeds but `bookDAO.update` fails, without a rollback you would have a loan record but the book would still show as available — an inconsistent state. Rolling back removes the inserted loan row.

---

##### `returnBook(int loanId)`

| Detail | Value |
|---|---|
| Return type | `void` |
| Throws | `DatabaseException` |

**Step-by-step logic:**

1. Call `loanDAO.findById(loanId)`. If `null`, throw `RecordNotFoundException("Loan with ID " + loanId + " not found")`.
2. If `loan.isActive()` is `false` (already returned), throw `new IllegalStateException("Loan " + loanId + " has already been returned")`.
3. **Open a transaction** (same pattern as `borrowBook`).
4. Inside the try block:
   - Call `loanDAO.markReturned(loanId, conn)`.
   - Get `Book book = loan.getBook()`.
   - Set `book.setAvailable(true)`.
   - Call `bookDAO.update(book)`.
   - Call `conn.commit()`.
5. Catch, rollback, rethrow as `DatabaseException` (same pattern).
6. Finally: reset `autoCommit`, close `conn`.

---

##### `getLoansForMember(int memberId)`

| Return type | `List<Loan>` |
|---|---|
| Throws | `DatabaseException` |
| Description | Delegates to `loanDAO.findByMember(memberId)` |

---

## Part 8 — Main Application

### `LibraryApp` — Package `com.library`

Contains `main`. Create one instance each of `BookDAOImpl`, `MemberDAOImpl`, `LoanDAOImpl`, `BookService`, and `LoanService`. Use a single `Scanner` over `System.in`.

#### Menu

```
=== Library Management System ===

--- Books ---
1.  Add New Book
2.  List All Books
3.  Find Book by ID
4.  Find Books by Genre
5.  Update Book Availability
6.  Delete Book
7.  Count All Books

--- Members ---
8.  Register New Member
9.  List All Members
10. Find Member by ID
11. Delete Member

--- Loans ---
12. Borrow Book
13. Return Book
14. View Member Loan History

0. Exit
```

#### Input Validation Rules (same as Question 1, extended)

- Non-integer input where an integer is expected → print `"Invalid input. Please enter a number."` and return to menu.
- `RecordNotFoundException` caught → print the exception message.
- `BookNotAvailableException` caught → print the exception message.
- `DatabaseException` caught → print `"Database error: "` + exception message.

#### Option Details

| Option | Action |
|---|---|
| 1 | Prompt title, author, genre, year → `bookService.addBook(new Book(...))` → print `"Book added! (ID: X)"` |
| 2 | `bookService.getAllBooks()` → formatted table (same format as Question 1) |
| 3 | Prompt id → `bookService.getBookById(id)` → print book or catch `RecordNotFoundException` |
| 4 | Prompt genre → `bookService.getBooksByGenre(genre)` → print list or "No books found" message |
| 5 | Prompt id → find book → toggle `available` → `bookService.updateBook(book)` → print confirmation |
| 6 | Prompt id → `bookService.deleteBook(id)` → print `"Book deleted."` |
| 7 | `bookService.countBooks()` → print count |
| 8 | Prompt full name, email → `new Member(name, email)` → `memberDAO.insert(member)` → print `"Member registered! (ID: X)"` |
| 9 | `memberDAO.findAll()` → formatted table: ID, Name, Email, Joined |
| 10 | Prompt id → `memberDAO.findById(id)` → print member or "Not found" |
| 11 | Prompt id → `memberDAO.delete(id)` → print `"Member deleted."` |
| 12 | Prompt book id, member id → `loanService.borrowBook(bookId, memberId)` → print `"Book borrowed! (Loan ID: X)"` |
| 13 | Prompt loan id → `loanService.returnBook(loanId)` → print `"Book returned successfully."` |
| 14 | Prompt member id → `loanService.getLoansForMember(memberId)` → print each loan on one line: `[ID: X] 'Title' — loaned: DATE, returned: DATE or Active` |

---

## Part 9 — Unit Tests

### What you are testing

You are testing the **service layer in isolation** — without a real database. Mockito replaces the real DAO objects with **mock** objects that you control. You tell each mock exactly what to return for a given input.

### Key Mockito concepts

| Concept | Syntax | Meaning |
|---|---|---|
| Create a mock | `@Mock BookDAO bookDAO` | Creates a fake `BookDAO` where all methods return `null`/`0`/`false` by default |
| Stub a method | `when(bookDAO.findById(1)).thenReturn(someBook)` | When `findById(1)` is called, return `someBook` |
| Stub to throw | `when(bookDAO.findById(99)).thenThrow(new DatabaseException("..."))` | When called, throw the exception |
| Verify a call | `verify(bookDAO).insert(any(Book.class))` | Assert that `insert` was called exactly once with any `Book` argument |
| Verify not called | `verify(bookDAO, never()).insert(any())` | Assert `insert` was never called |

### Setup annotations

```java
@ExtendWith(MockitoExtension.class)   // tells JUnit 5 to process Mockito annotations
class BookServiceTest {

    @Mock
    BookDAO bookDAO;                  // Mockito creates the mock

    @InjectMocks
    BookService bookService;          // Mockito creates BookService and injects bookDAO into it
}
```

---

### `BookServiceTest` — Package `com.library.service`

Write the following test methods. Each method is annotated with `@Test`.

| Test Method | What to test |
|---|---|
| `addBook_validBook_callsInsert()` | Create a valid `Book`. Call `bookService.addBook(book)`. Verify `bookDAO.insert(book)` was called once. |
| `addBook_blankTitle_throwsIllegalArgument()` | Create a `Book` with `title = "  "`. Call `bookService.addBook(book)`. Assert `IllegalArgumentException` is thrown using `assertThrows`. Verify `bookDAO.insert(...)` was **never** called. |
| `getBookById_existingId_returnsBook()` | Stub `bookDAO.findById(1)` to return a sample `Book`. Call `bookService.getBookById(1)`. Assert the returned book equals the sample book (`assertEquals`). |
| `getBookById_missingId_throwsRecordNotFound()` | Stub `bookDAO.findById(99)` to return `null`. Call `bookService.getBookById(99)`. Assert `RecordNotFoundException` is thrown. |
| `countBooks_returnsCount()` | Stub `bookDAO.count()` to return `5`. Call `bookService.countBooks()`. Assert the result is `5`. |

---

### `LoanServiceTest` — Package `com.library.service`

```java
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanDAO loanDAO;
    @Mock BookDAO bookDAO;
    @Mock MemberDAO memberDAO;

    @InjectMocks
    LoanService loanService;
}
```

| Test Method | What to test |
|---|---|
| `borrowBook_available_createsLoan()` | Stub `bookDAO.findById(1)` to return a book where `available=true`. Stub `memberDAO.findById(1)` to return a member. **Note:** `loanDAO.insert(...)` involves a transaction with a real connection — for this test you should verify `bookDAO.update(...)` was called and that the book's availability was set to `false` before update. Because the transaction opens a real connection, you may skip the full integration test and instead test only the pre-condition checks (steps 1–3) by having `loanDAO.insert` throw an exception so the test focuses on validation logic. Alternatively, restructure `LoanService` so the transaction logic is in a separate injectable helper — either approach is acceptable. |
| `borrowBook_bookNotFound_throwsRecordNotFound()` | Stub `bookDAO.findById(5)` to return `null`. Call `loanService.borrowBook(5, 1)`. Assert `RecordNotFoundException` is thrown. |
| `borrowBook_bookNotAvailable_throwsBookNotAvailable()` | Stub `bookDAO.findById(1)` to return a book with `available=false`. Stub `memberDAO.findById(1)` to return a member. Call `loanService.borrowBook(1, 1)`. Assert `BookNotAvailableException` is thrown. |
| `returnBook_alreadyReturned_throwsIllegalState()` | Stub `loanDAO.findById(1)` to return a `Loan` where `returnDate` is not `null` (already returned). Call `loanService.returnBook(1)`. Assert `IllegalStateException` is thrown. |
| `returnBook_loanNotFound_throwsRecordNotFound()` | Stub `loanDAO.findById(99)` to return `null`. Call `loanService.returnBook(99)`. Assert `RecordNotFoundException` is thrown. |

---

## Sample Console Interaction

```
=== Library Management System ===

--- Books ---
1.  Add New Book
...

Select option: > 1
Enter title: > Dune
Enter author: > Frank Herbert
Enter genre: > Science Fiction
Enter publish year: > 1965
Book added! (ID: 1)

Select option: > 8
Enter full name: > Alice Smith
Enter email: > alice@example.com
Member registered! (ID: 1)

Select option: > 12
Enter book ID: > 1
Enter member ID: > 1
Book borrowed! (Loan ID: 1)

Select option: > 2
ID  | Title                | Author          | Genre           | Year | Available
----|----------------------|-----------------|-----------------|------|----------
1   | Dune                 | Frank Herbert   | Science Fiction | 1965 | No

Select option: > 12
Enter book ID: > 1
Enter member ID: > 1
Book 'Dune' is not currently available

Select option: > 13
Enter loan ID: > 1
Book returned successfully.

Select option: > 2
ID  | Title                | Author          | Genre           | Year | Available
----|----------------------|-----------------|-----------------|------|----------
1   | Dune                 | Frank Herbert   | Science Fiction | 1965 | Yes

Select option: > 14
Enter member ID: > 1
[ID: 1] 'Dune' — loaned: 2024-06-01T10:00, returned: 2024-06-03T14:22

Select option: > 0
Goodbye!
```

---

## Checklist Before Submission

- [ ] `application.properties` is in `src/main/resources` and loaded by `AppConfig`
- [ ] `ConnectionPool` uses HikariCP; no `DriverManager` calls remain in DAOs
- [ ] All three custom exception classes are in `com.library.exception`
- [ ] `GenericDAO<T>` is the parent interface of `BookDAO` and `MemberDAO`
- [ ] `LoanDAOImpl.insert` and `markReturned` accept a `Connection` parameter and do not open their own connection
- [ ] `LoanService.borrowBook` uses a transaction: one `Connection`, `setAutoCommit(false)`, `commit`/`rollback` in try/catch
- [ ] `BookService.addBook` validates blank fields **before** calling the DAO
- [ ] All five `BookServiceTest` tests pass
- [ ] All five `LoanServiceTest` tests pass (or the transaction-dependent test is documented as an integration concern)
- [ ] Invalid integer input never crashes the application
- [ ] `Scanner` is closed on exit
