# Question 1 — Library Book Manager (Easy)

**Difficulty:** Easy  
**Topic:** JDBC CRUD with Single Table, DAO Pattern  
**Estimated Time:** 60–90 minutes

---

## Project Description

Build a console-based **Library Book Manager** that allows a librarian to manage a collection of books stored in a PostgreSQL database. The application must support adding, listing, searching, updating, and deleting books through a numbered menu. All database access must go through the DAO pattern.

---

## Database Setup

Connect to your existing PostgreSQL server and create the following database and table before running the application.

**Step 1 — Create the database:**
```sql
CREATE DATABASE library_db;
```

**Step 2 — Connect to library_db and create the table:**
```sql
CREATE TABLE IF NOT EXISTS books (
    id           SERIAL PRIMARY KEY,
    title        VARCHAR(200)  NOT NULL,
    author       VARCHAR(100)  NOT NULL,
    genre        VARCHAR(50)   NOT NULL,
    publish_year INTEGER       NOT NULL,
    available    BOOLEAN       NOT NULL DEFAULT TRUE
);
```

---

## Required Classes

You must implement exactly the following classes. Do not add extra classes or change the structure.

---

### 1. `Book` (Model / POJO)

**Package:** `com.library.model`  
**File:** `Book.java`

This class represents one row in the `books` table.

#### Attributes

| Field | Type | Corresponds To |
|-------|------|----------------|
| `id` | `int` | `id` column |
| `title` | `String` | `title` column |
| `author` | `String` | `author` column |
| `genre` | `String` | `genre` column |
| `publishYear` | `int` | `publish_year` column |
| `available` | `boolean` | `available` column |

#### Constructors

| Constructor | Parameters | Purpose |
|-------------|------------|---------|
| `Book()` | none | Default constructor (needed by DAO when mapping from ResultSet) |
| `Book(String title, String author, String genre, int publishYear)` | 4 params | Used when inserting a new book; `id` defaults to `0`, `available` defaults to `true` |

#### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getId()` | `int` | Returns the book id |
| `setId(int id)` | `void` | Sets the book id |
| `getTitle()` | `String` | Returns the title |
| `setTitle(String title)` | `void` | Sets the title |
| `getAuthor()` | `String` | Returns the author |
| `setAuthor(String author)` | `void` | Sets the author |
| `getGenre()` | `String` | Returns the genre |
| `setGenre(String genre)` | `void` | Sets the genre |
| `getPublishYear()` | `int` | Returns the publish year |
| `setPublishYear(int year)` | `void` | Sets the publish year |
| `isAvailable()` | `boolean` | Returns availability status |
| `setAvailable(boolean available)` | `void` | Sets availability |
| `toString()` | `String` | Returns formatted string (see format below) |

**`toString()` format:**
```
Book{id=1, title='The Pragmatic Programmer', author='Andrew Hunt', genre='Technology', year=1999, available=true}
```

---

### 2. `DatabaseConnection` (Utility)

**Package:** `com.library.util`  
**File:** `DatabaseConnection.java`

Provides a single static method to obtain a JDBC connection. Has a private constructor so it cannot be instantiated.

#### Constants (private static final)

| Constant | Value | Description |
|----------|-------|-------------|
| `URL` | `"jdbc:postgresql://localhost:5432/library_db"` | JDBC connection URL |
| `USER` | `"postgres"` | Database username |
| `PASSWORD` | `"your_password"` | Database password — change this to your actual password |

#### Methods

| Method | Return Type | Throws | Description |
|--------|-------------|--------|-------------|
| `getConnection()` | `Connection` | `SQLException` | Calls `DriverManager.getConnection(URL, USER, PASSWORD)` and returns the connection |

---

### 3. `BookDAO` (Interface)

**Package:** `com.library.dao`  
**File:** `BookDAO.java`

Declares all database operations for the `books` table.

#### Method Signatures

| Method | Parameters | Return Type | Description |
|--------|------------|-------------|-------------|
| `insert(Book book)` | `Book book` | `void` | Inserts a new book row. After insert, the generated `id` must be set on the `book` object using `book.setId(...)`. |
| `findById(int id)` | `int id` | `Book` | Returns the `Book` with the given id, or `null` if not found. |
| `findAll()` | none | `List<Book>` | Returns all books ordered by `id` ascending. |
| `findByGenre(String genre)` | `String genre` | `List<Book>` | Returns all books whose `genre` matches exactly (case-insensitive). Use `ILIKE` in SQL. |
| `update(Book book)` | `Book book` | `void` | Updates `title`, `author`, `genre`, `publish_year`, and `available` for the row whose `id` matches `book.getId()`. |
| `delete(int id)` | `int id` | `void` | Deletes the row with the given id. |
| `count()` | none | `int` | Returns the total number of rows in the `books` table. |

---

### 4. `BookDAOImpl` (DAO Implementation)

**Package:** `com.library.dao`  
**File:** `BookDAOImpl.java`

Implements `BookDAO`. Every method must open its own connection using `DatabaseConnection.getConnection()` inside a try-with-resources block.

#### Private Helper Method

| Method | Parameters | Return Type | Description |
|--------|------------|-------------|-------------|
| `mapRow(ResultSet rs)` | `ResultSet rs` | `Book` | Reads the current row of the ResultSet and returns a populated `Book` object. Throws `SQLException`. Called by `findById`, `findAll`, and `findByGenre` to avoid code duplication. |

**`mapRow` implementation detail:**  
Read columns by name: `"id"`, `"title"`, `"author"`, `"genre"`, `"publish_year"`, `"available"`.

#### SQL Queries Used

| Method | SQL |
|--------|-----|
| `insert` | `INSERT INTO books (title, author, genre, publish_year, available) VALUES (?, ?, ?, ?, ?)` — use `Statement.RETURN_GENERATED_KEYS` |
| `findById` | `SELECT * FROM books WHERE id = ?` |
| `findAll` | `SELECT * FROM books ORDER BY id` |
| `findByGenre` | `SELECT * FROM books WHERE genre ILIKE ? ORDER BY title` |
| `update` | `UPDATE books SET title = ?, author = ?, genre = ?, publish_year = ?, available = ? WHERE id = ?` |
| `delete` | `DELETE FROM books WHERE id = ?` |
| `count` | `SELECT COUNT(*) FROM books` |

#### Error Handling

If a `SQLException` is caught inside any method, wrap it and throw a `RuntimeException`:
```java
throw new RuntimeException("Error [operation description]", e);
```

---

### 5. `LibraryApp` (Main / Entry Point)

**Package:** `com.library`  
**File:** `LibraryApp.java`

Contains the `main` method. Displays a numbered menu in a loop. Reads user input using a single `Scanner` over `System.in`. Calls `BookDAOImpl` methods based on the chosen option.

#### Menu Options

| Option | Label | Action |
|--------|-------|--------|
| 1 | Add New Book | Prompts for title, author, genre, publish year → calls `dao.insert(book)` |
| 2 | List All Books | Calls `dao.findAll()` → prints formatted table |
| 3 | Find Book by ID | Prompts for id → calls `dao.findById(id)` → prints book or "Not found" |
| 4 | Find Books by Genre | Prompts for genre → calls `dao.findByGenre(genre)` → prints list |
| 5 | Update Book Availability | Prompts for id → calls `dao.findById(id)` → toggles `available` field → calls `dao.update(book)` |
| 6 | Delete Book | Prompts for id → calls `dao.delete(id)` → prints confirmation |
| 7 | Count All Books | Calls `dao.count()` → prints the number |
| 0 | Exit | Breaks the loop and exits the program |

#### Input Validation Rules

- If the user enters a non-integer where an integer is expected (id, year), print `"Invalid input. Please enter a number."` and return to the menu without crashing.
- If `findById` returns `null`, print `"No book found with ID [id]."`.
- If `findByGenre` returns an empty list, print `"No books found in genre '[genre]'."`.

---

## Program Execution Flow

```
Program starts
  └─ main() creates BookDAOImpl instance
  └─ main() creates Scanner
  └─ Loop begins
       ├─ Print menu
       ├─ Read option
       ├─ Dispatch to appropriate block
       │    └─ Read sub-inputs (if needed)
       │    └─ Call DAO method
       │    └─ Print result
       └─ Repeat until option 0
  └─ Print "Goodbye!"
  └─ Scanner closed
  └─ Program exits
```

---

## Sample Console Interaction (Complete Run)

The lines starting with `>` represent what the user types. Everything else is printed by the program.

```
=== Library Book Manager ===

1. Add New Book
2. List All Books
3. Find Book by ID
4. Find Books by Genre
5. Update Book Availability
6. Delete Book
7. Count All Books
0. Exit

Select option: > 1
Enter title: > The Pragmatic Programmer
Enter author: > Andrew Hunt
Enter genre: > Technology
Enter publish year: > 1999
Book added successfully! (ID: 1)

Select option: > 1
Enter title: > Clean Code
Enter author: > Robert C. Martin
Enter genre: > Technology
Enter publish year: > 2008
Book added successfully! (ID: 2)

Select option: > 1
Enter title: > Dune
Enter author: > Frank Herbert
Enter genre: > Science Fiction
Enter publish year: > 1965
Book added successfully! (ID: 3)

Select option: > 2
ID  | Title                       | Author               | Genre           | Year | Available
----|-----------------------------|-----------------------|-----------------|------|----------
1   | The Pragmatic Programmer    | Andrew Hunt           | Technology      | 1999 | Yes
2   | Clean Code                  | Robert C. Martin      | Technology      | 2008 | Yes
3   | Dune                        | Frank Herbert         | Science Fiction | 1965 | Yes

Select option: > 3
Enter book ID: > 2
Book{id=2, title='Clean Code', author='Robert C. Martin', genre='Technology', year=2008, available=true}

Select option: > 4
Enter genre: > Technology
Books in genre 'Technology':
  - [ID: 1] The Pragmatic Programmer — Andrew Hunt (1999) | Available: Yes
  - [ID: 2] Clean Code — Robert C. Martin (2008) | Available: Yes

Select option: > 5
Enter book ID to toggle availability: > 2
Book 'Clean Code' availability changed: true -> false

Select option: > 2
ID  | Title                       | Author               | Genre           | Year | Available
----|-----------------------------|-----------------------|-----------------|------|----------
1   | The Pragmatic Programmer    | Andrew Hunt           | Technology      | 1999 | Yes
2   | Clean Code                  | Robert C. Martin      | Technology      | 2008 | No
3   | Dune                        | Frank Herbert         | Science Fiction | 1965 | Yes

Select option: > 7
Total books in library: 3

Select option: > 6
Enter book ID to delete: > 3
Book with ID 3 deleted successfully.

Select option: > 3
Enter book ID: > 99
No book found with ID 99.

Select option: > 7
Total books in library: 2

Select option: > 0
Goodbye!
```

---

## Package and File Structure

```
src/
└── main/
    └── java/
        └── com/
            └── library/
                ├── LibraryApp.java          ← main method here
                ├── model/
                │   └── Book.java
                ├── dao/
                │   ├── BookDAO.java
                │   └── BookDAOImpl.java
                └── util/
                    └── DatabaseConnection.java
```

---

## Checklist Before Submission

- [ ] `books` table exists in `library_db` with the correct schema
- [ ] `DatabaseConnection.getConnection()` uses try-with-resources in every DAO method
- [ ] `BookDAOImpl.insert()` sets the generated ID back on the `Book` object
- [ ] `mapRow()` helper is used in all SELECT methods
- [ ] All menu options work correctly
- [ ] Invalid ID input (non-integer) does not crash the program
- [ ] `null` result from `findById` is handled gracefully
- [ ] `Scanner` is closed before the program exits
