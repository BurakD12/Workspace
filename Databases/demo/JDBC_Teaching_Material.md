# JDBC with PostgreSQL — Complete Teaching Material

> **Target Audience:** University / bootcamp students learning Java database programming  
> **Prerequisites:** Basic Java (OOP, exception handling), SQL fundamentals  
> **Tools Required:** JDK 17+, PostgreSQL 15+, PostgreSQL JDBC Driver (postgresql-42.x.x.jar), any IDE (IntelliJ / Eclipse / VS Code)

---

## Table of Contents

1. [Introduction to JDBC](#1-introduction-to-jdbc)
2. [Environment Setup](#2-environment-setup)
3. [JDBC Architecture](#3-jdbc-architecture)
4. [Connecting to PostgreSQL](#4-connecting-to-postgresql)
5. [Creating a Database](#5-creating-a-database)
6. [Creating Schemas](#6-creating-schemas)
7. [Creating Tables](#7-creating-tables)
8. [INSERT — Adding Data](#8-insert--adding-data)
9. [SELECT — Reading Data](#9-select--reading-data)
10. [UPDATE — Modifying Data](#10-update--modifying-data)
11. [DELETE — Removing Data](#11-delete--removing-data)
12. [PreparedStatement (Parameterized Queries)](#12-preparedstatement-parameterized-queries)
13. [Batch Operations](#13-batch-operations)
14. [Transactions](#14-transactions)
15. [ResultSetMetaData](#15-resultsetmetadata)
16. [DAO Pattern (Data Access Object)](#16-dao-pattern-data-access-object)
17. [Connection Pooling (HikariCP)](#17-connection-pooling-hikaricp)
18. [Common Exceptions and Troubleshooting](#18-common-exceptions-and-troubleshooting)
19. [Best Practices](#19-best-practices)
20. [Exercises](#20-exercises)

---

## 1. Introduction to JDBC

### What is JDBC?

**JDBC (Java Database Connectivity)** is a Java API that allows Java applications to interact with relational databases. It provides a standard interface for connecting to databases, executing SQL statements, and processing results.

### Why JDBC?

| Feature | Description |
|---------|-------------|
| **Portability** | Write once, connect to any database by switching the driver |
| **Standard API** | Part of Java SE — `java.sql` and `javax.sql` packages |
| **SQL Support** | Execute any SQL statement supported by the target database |
| **Security** | Supports parameterized queries to prevent SQL injection |

### Key JDBC Interfaces and Classes

| Interface / Class | Package | Purpose |
|-------------------|---------|---------|
| `DriverManager` | `java.sql` | Manages database drivers and creates connections |
| `Connection` | `java.sql` | Represents a connection to a specific database |
| `Statement` | `java.sql` | Executes static SQL statements |
| `PreparedStatement` | `java.sql` | Executes parameterized (pre-compiled) SQL statements |
| `CallableStatement` | `java.sql` | Executes stored procedures |
| `ResultSet` | `java.sql` | Holds the result of a SELECT query |
| `SQLException` | `java.sql` | Handles database errors |
| `ResultSetMetaData` | `java.sql` | Provides metadata about ResultSet columns |
| `DatabaseMetaData` | `java.sql` | Provides metadata about the database |

---

## 2. Environment Setup

### 2.1 Install PostgreSQL

Download and install from: [https://www.postgresql.org/download/](https://www.postgresql.org/download/)

Default settings during installation:
- **Port:** 5432
- **Username:** postgres
- **Password:** *(set during installation — remember this!)*

### 2.2 Add PostgreSQL JDBC Driver

#### Option A: Manual Download

Download the driver from: [https://jdbc.postgresql.org/download/](https://jdbc.postgresql.org/download/)

Add the `.jar` file to your project's classpath.

#### Option B: Maven (Recommended)

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>
```

#### Option C: Gradle

```groovy
implementation 'org.postgresql:postgresql:42.7.3'
```

### 2.3 Verify Installation

Open a terminal/command prompt:

```bash
psql -U postgres
```

If you see the `postgres=#` prompt, PostgreSQL is running correctly.

---

## 3. JDBC Architecture

### How JDBC Works (Simplified Flow)

```
┌──────────────────┐
│  Java Application│
└────────┬─────────┘
         │  uses JDBC API
         ▼
┌──────────────────┐
│   JDBC Driver    │
│   Manager        │
└────────┬─────────┘
         │  loads appropriate driver
         ▼
┌──────────────────┐
│  PostgreSQL JDBC │
│  Driver (.jar)   │
└────────┬─────────┘
         │  translates to database protocol
         ▼
┌──────────────────┐
│  PostgreSQL      │
│  Database Server │
└──────────────────┘
```

### JDBC Workflow (Step-by-Step)

```
1. Load the Driver       →  Class.forName("org.postgresql.Driver")  // optional in modern JDBC
2. Establish Connection  →  DriverManager.getConnection(url, user, password)
3. Create Statement      →  connection.createStatement()
4. Execute Query         →  statement.executeQuery(sql) or executeUpdate(sql)
5. Process Results       →  while (resultSet.next()) { ... }
6. Close Resources       →  resultSet.close(); statement.close(); connection.close();
```

> **Note:** Since JDBC 4.0 (Java 6+), the driver is auto-loaded via the Service Provider mechanism. You rarely need `Class.forName()` anymore.

---

## 4. Connecting to PostgreSQL

### 4.1 JDBC URL Format

```
jdbc:postgresql://host:port/database
```

| Component | Example | Description |
|-----------|---------|-------------|
| Protocol | `jdbc:postgresql` | Always the same for PostgreSQL |
| Host | `localhost` | Server address |
| Port | `5432` | Default PostgreSQL port |
| Database | `school_db` | Target database name |

### 4.2 Basic Connection Example

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionDemo {

    // Connection parameters
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASSWORD = "your_password";

    public static void main(String[] args) {

        // Try-with-resources ensures automatic closing
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {

            if (connection != null) {
                System.out.println("Connected to PostgreSQL successfully!");
                System.out.println("Database: " + connection.getCatalog());
                System.out.println("Schema: " + connection.getSchema());
            }

        } catch (SQLException e) {
            System.err.println("Connection failed!");
            System.err.println("Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
        }
    }
}
```

**Expected Output:**
```
Connected to PostgreSQL successfully!
Database: postgres
Schema: public
```

### 4.3 Reusable Connection Utility Class

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/school_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "your_password";

    // Private constructor — prevent instantiation
    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

Usage:
```java
try (Connection conn = DatabaseConnection.getConnection()) {
    // Use the connection
} catch (SQLException e) {
    e.printStackTrace();
}
```

---

## 5. Creating a Database

### 5.1 Create Database via JDBC

> **Important:** You must connect to an existing database (like `postgres`) first, then execute `CREATE DATABASE`.

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateDatabaseDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/postgres"; // connect to default db
        String user = "postgres";
        String password = "your_password";

        String sql = "CREATE DATABASE school_db";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
            System.out.println("Database 'school_db' created successfully!");

        } catch (SQLException e) {
            if (e.getSQLState().equals("42P04")) {
                System.out.println("Database 'school_db' already exists.");
            } else {
                e.printStackTrace();
            }
        }
    }
}
```

### 5.2 Drop Database

```java
String sql = "DROP DATABASE IF EXISTS school_db";

try (Connection conn = DriverManager.getConnection(url, user, password);
     Statement stmt = conn.createStatement()) {

    stmt.executeUpdate(sql);
    System.out.println("Database dropped successfully!");

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 5.3 List All Databases

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ListDatabasesDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "your_password";

        String sql = "SELECT datname FROM pg_database WHERE datistemplate = false";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("Databases on this server:");
            System.out.println("-------------------------");
            while (rs.next()) {
                System.out.println("  - " + rs.getString("datname"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

**Expected Output:**
```
Databases on this server:
-------------------------
  - postgres
  - school_db
```

---

## 6. Creating Schemas

### What is a Schema?

A **schema** is a namespace within a database that organizes tables, views, functions, etc. PostgreSQL's default schema is `public`.

### 6.1 Create a Schema

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateSchemaDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            // Create schemas
            stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS academic");
            System.out.println("Schema 'academic' created.");

            stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS administration");
            System.out.println("Schema 'administration' created.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 6.2 List All Schemas

```java
String sql = "SELECT schema_name FROM information_schema.schemata";

try (Connection conn = DriverManager.getConnection(url, user, password);
     Statement stmt = conn.createStatement();
     ResultSet rs = stmt.executeQuery(sql)) {

    System.out.println("Schemas in school_db:");
    while (rs.next()) {
        System.out.println("  - " + rs.getString("schema_name"));
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 6.3 Set Search Path (Active Schema)

```java
// Use a specific schema for subsequent queries
stmt.executeUpdate("SET search_path TO academic");

// Or set via connection
conn.setSchema("academic");
```

### 6.4 Drop a Schema

```java
// Drop empty schema
stmt.executeUpdate("DROP SCHEMA IF EXISTS academic");

// Drop schema and all its objects (CASCADE)
stmt.executeUpdate("DROP SCHEMA IF EXISTS academic CASCADE");
```

---

## 7. Creating Tables

### 7.1 Create a Single Table

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateTableDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        String sql = """
                CREATE TABLE IF NOT EXISTS students (
                    id          SERIAL PRIMARY KEY,
                    first_name  VARCHAR(50) NOT NULL,
                    last_name   VARCHAR(50) NOT NULL,
                    email       VARCHAR(100) UNIQUE,
                    age         INTEGER CHECK (age >= 16 AND age <= 100),
                    gpa         NUMERIC(3, 2),
                    enrolled    BOOLEAN DEFAULT true,
                    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
            System.out.println("Table 'students' created successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 7.2 Common PostgreSQL Data Types

| SQL Type | Java Type | Description |
|----------|-----------|-------------|
| `SERIAL` | `int` / `long` | Auto-incrementing integer |
| `INTEGER` | `int` | 4-byte integer |
| `BIGINT` | `long` | 8-byte integer |
| `NUMERIC(p,s)` | `BigDecimal` | Exact decimal |
| `REAL` | `float` | 4-byte floating point |
| `DOUBLE PRECISION` | `double` | 8-byte floating point |
| `VARCHAR(n)` | `String` | Variable-length text |
| `TEXT` | `String` | Unlimited-length text |
| `BOOLEAN` | `boolean` | true / false |
| `DATE` | `java.sql.Date` | Calendar date |
| `TIMESTAMP` | `java.sql.Timestamp` | Date + time |
| `BYTEA` | `byte[]` | Binary data |

### 7.3 Create Related Tables (Foreign Keys)

```java
public class CreateRelatedTablesDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        String createDepartments = """
                CREATE TABLE IF NOT EXISTS departments (
                    id              SERIAL PRIMARY KEY,
                    department_name VARCHAR(100) NOT NULL UNIQUE,
                    building        VARCHAR(50),
                    budget          NUMERIC(12, 2)
                )
                """;

        String createCourses = """
                CREATE TABLE IF NOT EXISTS courses (
                    id              SERIAL PRIMARY KEY,
                    course_name     VARCHAR(100) NOT NULL,
                    course_code     VARCHAR(10) UNIQUE NOT NULL,
                    credits         INTEGER DEFAULT 3,
                    department_id   INTEGER REFERENCES departments(id) ON DELETE SET NULL
                )
                """;

        String createEnrollments = """
                CREATE TABLE IF NOT EXISTS enrollments (
                    id          SERIAL PRIMARY KEY,
                    student_id  INTEGER REFERENCES students(id) ON DELETE CASCADE,
                    course_id   INTEGER REFERENCES courses(id) ON DELETE CASCADE,
                    grade       VARCHAR(2),
                    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(student_id, course_id)
                )
                """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(createDepartments);
            System.out.println("Table 'departments' created.");

            stmt.executeUpdate(createCourses);
            System.out.println("Table 'courses' created.");

            stmt.executeUpdate(createEnrollments);
            System.out.println("Table 'enrollments' created.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 7.4 Create a Table in a Specific Schema

```java
String sql = """
        CREATE TABLE IF NOT EXISTS academic.instructors (
            id          SERIAL PRIMARY KEY,
            first_name  VARCHAR(50) NOT NULL,
            last_name   VARCHAR(50) NOT NULL,
            department_id INTEGER REFERENCES departments(id)
        )
        """;

stmt.executeUpdate(sql);
```

### 7.5 Drop a Table

```java
stmt.executeUpdate("DROP TABLE IF EXISTS enrollments");
stmt.executeUpdate("DROP TABLE IF EXISTS courses");
stmt.executeUpdate("DROP TABLE IF EXISTS departments");
stmt.executeUpdate("DROP TABLE IF EXISTS students");
```

> **Important:** Drop tables with foreign key dependencies first, or use `CASCADE`.

### 7.6 Alter a Table

```java
// Add a column
stmt.executeUpdate("ALTER TABLE students ADD COLUMN phone VARCHAR(20)");

// Drop a column
stmt.executeUpdate("ALTER TABLE students DROP COLUMN phone");

// Rename a column
stmt.executeUpdate("ALTER TABLE students RENAME COLUMN gpa TO grade_point_average");

// Change column type
stmt.executeUpdate("ALTER TABLE students ALTER COLUMN age TYPE SMALLINT");

// Add a constraint
stmt.executeUpdate("ALTER TABLE students ADD CONSTRAINT chk_email CHECK (email LIKE '%@%')");
```

---

## 8. INSERT — Adding Data

### 8.1 Statement vs. PreparedStatement

| Feature | Statement | PreparedStatement |
|---------|-----------|-------------------|
| SQL Injection | **Vulnerable** | **Safe** |
| Performance | Compiled every time | Pre-compiled, cached |
| Readability | String concatenation | Clean placeholders |
| Use case | DDL, static SQL | DML with user input |

> **Rule of Thumb:** Always use `PreparedStatement` for INSERT, UPDATE, DELETE with dynamic values.

### 8.2 Insert with Statement (Simple / Static Data Only)

```java
import java.sql.*;

public class InsertStatementDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        String sql = """
                INSERT INTO departments (department_name, building, budget)
                VALUES ('Computer Science', 'Engineering Hall', 500000.00)
                """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            int rowsAffected = stmt.executeUpdate(sql);
            System.out.println(rowsAffected + " row(s) inserted.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 8.3 Insert with PreparedStatement (Recommended)

```java
import java.sql.*;

public class InsertPreparedDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        String sql = """
                INSERT INTO students (first_name, last_name, email, age, gpa, enrolled)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "Ali");
            pstmt.setString(2, "Yılmaz");
            pstmt.setString(3, "ali.yilmaz@example.com");
            pstmt.setInt(4, 20);
            pstmt.setBigDecimal(5, new java.math.BigDecimal("3.75"));
            pstmt.setBoolean(6, true);

            int rowsAffected = pstmt.executeUpdate();
            System.out.println(rowsAffected + " row(s) inserted.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 8.4 Insert and Get Generated Key (Auto-ID)

```java
String sql = "INSERT INTO students (first_name, last_name, email, age, gpa) VALUES (?, ?, ?, ?, ?)";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

    pstmt.setString(1, "Ayşe");
    pstmt.setString(2, "Kaya");
    pstmt.setString(3, "ayse.kaya@example.com");
    pstmt.setInt(4, 22);
    pstmt.setBigDecimal(5, new java.math.BigDecimal("3.90"));

    int rowsAffected = pstmt.executeUpdate();

    // Retrieve the auto-generated ID
    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
            long newId = generatedKeys.getLong(1);
            System.out.println("Inserted student with ID: " + newId);
        }
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 8.5 Insert Multiple Rows

```java
String sql = "INSERT INTO students (first_name, last_name, email, age, gpa) VALUES (?, ?, ?, ?, ?)";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    // Student 1
    pstmt.setString(1, "Mehmet");
    pstmt.setString(2, "Demir");
    pstmt.setString(3, "mehmet.demir@example.com");
    pstmt.setInt(4, 21);
    pstmt.setBigDecimal(5, new java.math.BigDecimal("3.50"));
    pstmt.executeUpdate();

    // Student 2
    pstmt.setString(1, "Zeynep");
    pstmt.setString(2, "Arslan");
    pstmt.setString(3, "zeynep.arslan@example.com");
    pstmt.setInt(4, 19);
    pstmt.setBigDecimal(5, new java.math.BigDecimal("3.85"));
    pstmt.executeUpdate();

    System.out.println("Multiple students inserted.");

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 8.6 Handling NULL Values

```java
// Insert NULL for optional fields
pstmt.setNull(3, Types.VARCHAR);  // email = NULL
pstmt.setNull(5, Types.NUMERIC); // gpa = NULL
```

---

## 9. SELECT — Reading Data

### 9.1 Select All Rows

```java
import java.sql.*;

public class SelectAllDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        String sql = "SELECT * FROM students";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("ID | First Name | Last Name | Email              | Age | GPA");
            System.out.println("---|------------|-----------|---------------------|-----|-----");

            while (rs.next()) {
                int id = rs.getInt("id");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String email = rs.getString("email");
                int age = rs.getInt("age");
                double gpa = rs.getDouble("gpa");

                System.out.printf("%2d | %-10s | %-9s | %-19s | %3d | %.2f%n",
                        id, firstName, lastName, email, age, gpa);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 9.2 ResultSet Getter Methods

| Method | Returns | Use For |
|--------|---------|---------|
| `rs.getInt("column")` | `int` | INTEGER columns |
| `rs.getLong("column")` | `long` | BIGINT columns |
| `rs.getString("column")` | `String` | VARCHAR, TEXT |
| `rs.getDouble("column")` | `double` | DOUBLE PRECISION |
| `rs.getBigDecimal("column")` | `BigDecimal` | NUMERIC, DECIMAL |
| `rs.getBoolean("column")` | `boolean` | BOOLEAN |
| `rs.getDate("column")` | `java.sql.Date` | DATE |
| `rs.getTimestamp("column")` | `Timestamp` | TIMESTAMP |
| `rs.getBytes("column")` | `byte[]` | BYTEA |
| `rs.getObject("column")` | `Object` | Any type |

> **Tip:** You can also use column index: `rs.getString(1)` — but column names are more readable and maintainable.

### 9.3 Select with WHERE Clause (PreparedStatement)

```java
String sql = "SELECT * FROM students WHERE age > ? AND gpa >= ?";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setInt(1, 20);
    pstmt.setBigDecimal(2, new java.math.BigDecimal("3.50"));

    try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
            System.out.println(rs.getString("first_name") + " " +
                               rs.getString("last_name") + " - GPA: " +
                               rs.getBigDecimal("gpa"));
        }
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 9.4 Select with LIKE (Pattern Matching)

```java
String sql = "SELECT * FROM students WHERE last_name LIKE ?";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setString(1, "%maz");  // ends with "maz"

    try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
            System.out.println(rs.getString("first_name") + " " + rs.getString("last_name"));
        }
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 9.5 Select with ORDER BY and LIMIT

```java
String sql = "SELECT * FROM students ORDER BY gpa DESC LIMIT ? OFFSET ?";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setInt(1, 5);  // limit — top 5
    pstmt.setInt(2, 0);  // offset — start from first row

    try (ResultSet rs = pstmt.executeQuery()) {
        System.out.println("Top 5 students by GPA:");
        int rank = 1;
        while (rs.next()) {
            System.out.printf("%d. %s %s — GPA: %.2f%n",
                    rank++,
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getDouble("gpa"));
        }
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 9.6 Select Single Row by ID

```java
String sql = "SELECT * FROM students WHERE id = ?";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setInt(1, 1);

    try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
            System.out.println("Found: " + rs.getString("first_name") + " " + rs.getString("last_name"));
        } else {
            System.out.println("No student found with ID 1.");
        }
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 9.7 Aggregate Functions (COUNT, SUM, AVG, MIN, MAX)

```java
String sql = """
        SELECT
            COUNT(*) AS total_students,
            AVG(gpa) AS average_gpa,
            MAX(gpa) AS highest_gpa,
            MIN(gpa) AS lowest_gpa,
            AVG(age) AS average_age
        FROM students
        WHERE enrolled = true
        """;

try (Connection conn = DriverManager.getConnection(url, user, password);
     Statement stmt = conn.createStatement();
     ResultSet rs = stmt.executeQuery(sql)) {

    if (rs.next()) {
        System.out.println("Total Students: " + rs.getInt("total_students"));
        System.out.printf("Average GPA:    %.2f%n", rs.getDouble("average_gpa"));
        System.out.printf("Highest GPA:    %.2f%n", rs.getDouble("highest_gpa"));
        System.out.printf("Lowest GPA:     %.2f%n", rs.getDouble("lowest_gpa"));
        System.out.printf("Average Age:    %.1f%n", rs.getDouble("average_age"));
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 9.8 GROUP BY Queries

```java
String sql = """
        SELECT d.department_name, COUNT(c.id) AS course_count
        FROM departments d
        LEFT JOIN courses c ON d.id = c.department_id
        GROUP BY d.department_name
        ORDER BY course_count DESC
        """;

try (Connection conn = DriverManager.getConnection(url, user, password);
     Statement stmt = conn.createStatement();
     ResultSet rs = stmt.executeQuery(sql)) {

    System.out.println("Department           | Courses");
    System.out.println("---------------------|--------");
    while (rs.next()) {
        System.out.printf("%-20s | %d%n",
                rs.getString("department_name"),
                rs.getInt("course_count"));
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 9.9 JOIN Queries

```java
// INNER JOIN — students with their enrolled courses
String sql = """
        SELECT s.first_name, s.last_name, c.course_name, c.course_code, e.grade
        FROM students s
        INNER JOIN enrollments e ON s.id = e.student_id
        INNER JOIN courses c ON c.id = e.course_id
        ORDER BY s.last_name, c.course_name
        """;

try (Connection conn = DriverManager.getConnection(url, user, password);
     Statement stmt = conn.createStatement();
     ResultSet rs = stmt.executeQuery(sql)) {

    while (rs.next()) {
        System.out.printf("%s %s — %s (%s) — Grade: %s%n",
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("course_name"),
                rs.getString("course_code"),
                rs.getString("grade") != null ? rs.getString("grade") : "N/A");
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 9.10 Subqueries

```java
// Find students with GPA above average
String sql = """
        SELECT first_name, last_name, gpa
        FROM students
        WHERE gpa > (SELECT AVG(gpa) FROM students)
        ORDER BY gpa DESC
        """;
```

---

## 10. UPDATE — Modifying Data

### 10.1 Update a Single Row

```java
import java.sql.*;

public class UpdateDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        String sql = "UPDATE students SET gpa = ?, email = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, new java.math.BigDecimal("3.95"));
            pstmt.setString(2, "ali.yilmaz.updated@example.com");
            pstmt.setInt(3, 1);

            int rowsAffected = pstmt.executeUpdate();
            System.out.println(rowsAffected + " row(s) updated.");

            if (rowsAffected == 0) {
                System.out.println("No student found with that ID.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 10.2 Update Multiple Rows

```java
// Increase all enrolled students' age by 1
String sql = "UPDATE students SET age = age + 1 WHERE enrolled = true";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    int rowsAffected = pstmt.executeUpdate();
    System.out.println(rowsAffected + " student(s) updated.");

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 10.3 Conditional Update

```java
// Set enrolled = false for students with GPA below 2.0
String sql = "UPDATE students SET enrolled = false WHERE gpa < ?";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setBigDecimal(1, new java.math.BigDecimal("2.00"));

    int rowsAffected = pstmt.executeUpdate();
    System.out.println(rowsAffected + " student(s) have been un-enrolled.");

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 10.4 Update with Returning (PostgreSQL Specific)

```java
// PostgreSQL supports RETURNING clause for UPDATE
String sql = "UPDATE students SET gpa = ? WHERE id = ? RETURNING first_name, last_name, gpa";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setBigDecimal(1, new java.math.BigDecimal("4.00"));
    pstmt.setInt(2, 1);

    try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
            System.out.printf("Updated: %s %s — New GPA: %s%n",
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getBigDecimal("gpa"));
        }
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

---

## 11. DELETE — Removing Data

### 11.1 Delete a Single Row

```java
import java.sql.*;

public class DeleteDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        String sql = "DELETE FROM students WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, 3);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Student deleted successfully.");
            } else {
                System.out.println("No student found with ID 3.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 11.2 Delete with Condition

```java
// Delete all students who are not enrolled
String sql = "DELETE FROM students WHERE enrolled = false";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    int rowsAffected = pstmt.executeUpdate();
    System.out.println(rowsAffected + " un-enrolled student(s) deleted.");

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 11.3 Delete All Rows (TRUNCATE)

```java
// DELETE removes row-by-row (can use WHERE, triggers fire)
stmt.executeUpdate("DELETE FROM students");

// TRUNCATE is faster for deleting all rows (no WHERE, no triggers)
stmt.executeUpdate("TRUNCATE TABLE students RESTART IDENTITY CASCADE");
```

### 11.4 Delete with RETURNING

```java
String sql = "DELETE FROM students WHERE id = ? RETURNING first_name, last_name";

try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setInt(1, 2);

    try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
            System.out.printf("Deleted: %s %s%n",
                    rs.getString("first_name"),
                    rs.getString("last_name"));
        } else {
            System.out.println("No student found with that ID.");
        }
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

---

## 12. PreparedStatement (Parameterized Queries)

### Why PreparedStatement is Critical

#### SQL Injection Example (DANGEROUS — Statement)

```java
// NEVER DO THIS — vulnerable to SQL injection!
String userInput = "'; DROP TABLE students; --";
String sql = "SELECT * FROM students WHERE last_name = '" + userInput + "'";
stmt.executeQuery(sql);
// This would execute: SELECT * FROM students WHERE last_name = ''; DROP TABLE students; --'
```

#### Safe Version (PreparedStatement)

```java
// ALWAYS DO THIS — safe from SQL injection
String sql = "SELECT * FROM students WHERE last_name = ?";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setString(1, userInput);  // userInput is treated as DATA, never as SQL
ResultSet rs = pstmt.executeQuery();
```

### Parameter Setter Methods Reference

| Method | Parameter Type | Example |
|--------|---------------|---------|
| `setString(index, value)` | VARCHAR, TEXT | `pstmt.setString(1, "Ali")` |
| `setInt(index, value)` | INTEGER | `pstmt.setInt(2, 25)` |
| `setLong(index, value)` | BIGINT | `pstmt.setLong(3, 1000000L)` |
| `setDouble(index, value)` | DOUBLE | `pstmt.setDouble(4, 3.14)` |
| `setBigDecimal(index, value)` | NUMERIC | `pstmt.setBigDecimal(5, new BigDecimal("99.99"))` |
| `setBoolean(index, value)` | BOOLEAN | `pstmt.setBoolean(6, true)` |
| `setDate(index, value)` | DATE | `pstmt.setDate(7, java.sql.Date.valueOf("2024-01-15"))` |
| `setTimestamp(index, value)` | TIMESTAMP | `pstmt.setTimestamp(8, Timestamp.valueOf("2024-01-15 10:30:00"))` |
| `setNull(index, sqlType)` | NULL | `pstmt.setNull(9, Types.VARCHAR)` |
| `setBytes(index, value)` | BYTEA | `pstmt.setBytes(10, byteArray)` |

### Complete PreparedStatement Example

```java
import java.math.BigDecimal;
import java.sql.*;

public class PreparedStatementFullDemo {

    private static final String URL = "jdbc:postgresql://localhost:5432/school_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "your_password";

    public static void main(String[] args) {
        insertStudent("Emre", "Özcan", "emre.ozcan@example.com", 23, new BigDecimal("3.60"));
        findStudentsByAge(20);
        updateStudentGpa(1, new BigDecimal("3.99"));
        deleteStudentByEmail("emre.ozcan@example.com");
    }

    static void insertStudent(String firstName, String lastName, String email, int age, BigDecimal gpa) {
        String sql = "INSERT INTO students (first_name, last_name, email, age, gpa) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, email);
            pstmt.setInt(4, age);
            pstmt.setBigDecimal(5, gpa);

            pstmt.executeUpdate();
            System.out.println("Inserted: " + firstName + " " + lastName);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void findStudentsByAge(int minAge) {
        String sql = "SELECT first_name, last_name, age, gpa FROM students WHERE age >= ? ORDER BY age";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, minAge);

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\nStudents aged " + minAge + " and above:");
                while (rs.next()) {
                    System.out.printf("  %s %s (Age: %d, GPA: %s)%n",
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getInt("age"),
                            rs.getBigDecimal("gpa"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void updateStudentGpa(int id, BigDecimal newGpa) {
        String sql = "UPDATE students SET gpa = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, newGpa);
            pstmt.setInt(2, id);

            int rows = pstmt.executeUpdate();
            System.out.println("\nUpdated " + rows + " student(s) GPA to " + newGpa);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void deleteStudentByEmail(String email) {
        String sql = "DELETE FROM students WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            int rows = pstmt.executeUpdate();
            System.out.println("\nDeleted " + rows + " student(s) with email: " + email);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

---

## 13. Batch Operations

Batch operations allow you to group multiple SQL statements and execute them in a single round-trip to the database. This is much more efficient than executing them one by one.

### 13.1 Batch Insert with PreparedStatement

```java
import java.math.BigDecimal;
import java.sql.*;

public class BatchInsertDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        String sql = "INSERT INTO students (first_name, last_name, email, age, gpa) VALUES (?, ?, ?, ?, ?)";

        // Sample data
        String[][] students = {
            {"Burak", "Şahin", "burak.sahin@example.com", "21", "3.45"},
            {"Elif", "Çelik", "elif.celik@example.com", "22", "3.80"},
            {"Can", "Aydın", "can.aydin@example.com", "20", "3.55"},
            {"Selin", "Koç", "selin.koc@example.com", "23", "3.92"},
            {"Deniz", "Yıldız", "deniz.yildiz@example.com", "19", "3.70"}
        };

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Disable auto-commit for better performance
            conn.setAutoCommit(false);

            for (String[] student : students) {
                pstmt.setString(1, student[0]);
                pstmt.setString(2, student[1]);
                pstmt.setString(3, student[2]);
                pstmt.setInt(4, Integer.parseInt(student[3]));
                pstmt.setBigDecimal(5, new BigDecimal(student[4]));
                pstmt.addBatch();  // Add to batch
            }

            int[] results = pstmt.executeBatch();  // Execute all at once
            conn.commit();  // Commit the transaction

            System.out.println("Batch insert completed. Rows affected per statement:");
            for (int i = 0; i < results.length; i++) {
                System.out.println("  Student " + (i + 1) + ": " + results[i] + " row(s)");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 13.2 Batch with Mixed Statements

```java
try (Connection conn = DriverManager.getConnection(url, user, password);
     Statement stmt = conn.createStatement()) {

    conn.setAutoCommit(false);

    stmt.addBatch("INSERT INTO departments (department_name, building) VALUES ('Mathematics', 'Science Hall')");
    stmt.addBatch("INSERT INTO departments (department_name, building) VALUES ('Physics', 'Science Hall')");
    stmt.addBatch("INSERT INTO departments (department_name, building) VALUES ('History', 'Arts Building')");

    int[] results = stmt.executeBatch();
    conn.commit();

    System.out.println("Inserted " + results.length + " departments.");

} catch (SQLException e) {
    e.printStackTrace();
}
```

---

## 14. Transactions

### What is a Transaction?

A transaction is a sequence of operations that are treated as a **single atomic unit**. Either **all operations succeed** (COMMIT) or **all operations are rolled back** (ROLLBACK).

### ACID Properties

| Property | Description |
|----------|-------------|
| **Atomicity** | All or nothing — partial completion is not allowed |
| **Consistency** | Database moves from one valid state to another |
| **Isolation** | Concurrent transactions don't interfere with each other |
| **Durability** | Committed changes survive system failures |

### 14.1 Basic Transaction Example

```java
import java.sql.*;

public class TransactionDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(url, user, password);
            conn.setAutoCommit(false);  // START TRANSACTION

            // Step 1: Insert a new course
            String insertCourse = "INSERT INTO courses (course_name, course_code, credits, department_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt1 = conn.prepareStatement(insertCourse, Statement.RETURN_GENERATED_KEYS)) {
                pstmt1.setString(1, "Database Systems");
                pstmt1.setString(2, "CS301");
                pstmt1.setInt(3, 4);
                pstmt1.setInt(4, 1);
                pstmt1.executeUpdate();

                ResultSet keys = pstmt1.getGeneratedKeys();
                int courseId = 0;
                if (keys.next()) {
                    courseId = keys.getInt(1);
                }

                // Step 2: Enroll a student in that course
                String insertEnrollment = "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)";
                try (PreparedStatement pstmt2 = conn.prepareStatement(insertEnrollment)) {
                    pstmt2.setInt(1, 1);  // student_id = 1
                    pstmt2.setInt(2, courseId);
                    pstmt2.executeUpdate();
                }
            }

            conn.commit();  // COMMIT — both operations saved
            System.out.println("Transaction committed successfully.");

        } catch (SQLException e) {
            // ROLLBACK — undo all operations if anything fails
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("Transaction rolled back due to error.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);  // Restore default
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

### 14.2 Savepoints

Savepoints allow you to roll back to a specific point within a transaction without rolling back the entire transaction.

```java
Connection conn = DriverManager.getConnection(url, user, password);
conn.setAutoCommit(false);

Savepoint savepoint1 = null;

try {
    // Operation 1 — always needed
    stmt.executeUpdate("INSERT INTO departments (department_name) VALUES ('Biology')");

    savepoint1 = conn.setSavepoint("after_department");

    // Operation 2 — might fail
    stmt.executeUpdate("INSERT INTO courses (course_name, course_code) VALUES ('Genetics', 'BIO201')");

    conn.commit();

} catch (SQLException e) {
    if (savepoint1 != null) {
        conn.rollback(savepoint1);  // Keep department, undo course
        conn.commit();              // Commit what we have
        System.out.println("Rolled back to savepoint. Department was saved.");
    } else {
        conn.rollback();  // Undo everything
    }
}
```

### 14.3 Transaction Isolation Levels

```java
// Set isolation level before starting operations
conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);  // Default in PostgreSQL
```

| Level | Constant | Dirty Read | Non-Repeatable Read | Phantom Read |
|-------|----------|------------|---------------------|-------------|
| Read Uncommitted | `TRANSACTION_READ_UNCOMMITTED` | Possible | Possible | Possible |
| Read Committed | `TRANSACTION_READ_COMMITTED` | No | Possible | Possible |
| Repeatable Read | `TRANSACTION_REPEATABLE_READ` | No | No | Possible |
| Serializable | `TRANSACTION_SERIALIZABLE` | No | No | No |

> **PostgreSQL default:** `READ COMMITTED`

---

## 15. ResultSetMetaData

`ResultSetMetaData` provides information about the columns of a `ResultSet` — useful for dynamic queries or building generic tools.

```java
import java.sql.*;

public class MetaDataDemo {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/school_db";
        String user = "postgres";
        String password = "your_password";

        String sql = "SELECT * FROM students LIMIT 1";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            System.out.println("Table: " + metaData.getTableName(1));
            System.out.println("Number of columns: " + columnCount);
            System.out.println();

            System.out.printf("%-5s %-20s %-20s %-10s %-10s%n",
                    "#", "Column Name", "Type Name", "Size", "Nullable");
            System.out.println("-".repeat(65));

            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("%-5d %-20s %-20s %-10d %-10s%n",
                        i,
                        metaData.getColumnName(i),
                        metaData.getColumnTypeName(i),
                        metaData.getColumnDisplaySize(i),
                        metaData.isNullable(i) == ResultSetMetaData.columnNullable ? "YES" : "NO");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### DatabaseMetaData — Information About the Database

```java
try (Connection conn = DriverManager.getConnection(url, user, password)) {

    DatabaseMetaData dbMeta = conn.getMetaData();

    System.out.println("Database Product: " + dbMeta.getDatabaseProductName());
    System.out.println("Database Version: " + dbMeta.getDatabaseProductVersion());
    System.out.println("Driver Name:      " + dbMeta.getDriverName());
    System.out.println("Driver Version:   " + dbMeta.getDriverVersion());
    System.out.println("JDBC Version:     " + dbMeta.getJDBCMajorVersion() + "." + dbMeta.getJDBCMinorVersion());
    System.out.println("URL:              " + dbMeta.getURL());
    System.out.println("User:             " + dbMeta.getUserName());

    // List all tables
    System.out.println("\nTables:");
    try (ResultSet tables = dbMeta.getTables(null, "public", "%", new String[]{"TABLE"})) {
        while (tables.next()) {
            System.out.println("  - " + tables.getString("TABLE_NAME"));
        }
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

---

## 16. DAO Pattern (Data Access Object)

The **DAO pattern** separates database operations from business logic. Each table typically gets its own DAO class.

### 16.1 Student Model (POJO)

```java
import java.math.BigDecimal;
import java.sql.Timestamp;

public class Student {

    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private int age;
    private BigDecimal gpa;
    private boolean enrolled;
    private Timestamp createdAt;

    // Default constructor
    public Student() {}

    // Parameterized constructor (without id — for inserts)
    public Student(String firstName, String lastName, String email, int age, BigDecimal gpa) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.age = age;
        this.gpa = gpa;
        this.enrolled = true;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public BigDecimal getGpa() { return gpa; }
    public void setGpa(BigDecimal gpa) { this.gpa = gpa; }

    public boolean isEnrolled() { return enrolled; }
    public void setEnrolled(boolean enrolled) { this.enrolled = enrolled; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("Student{id=%d, name='%s %s', email='%s', age=%d, gpa=%s, enrolled=%s}",
                id, firstName, lastName, email, age, gpa, enrolled);
    }
}
```

### 16.2 StudentDAO Interface

```java
import java.util.List;

public interface StudentDAO {

    void insert(Student student);
    Student findById(int id);
    List<Student> findAll();
    List<Student> findByLastName(String lastName);
    void update(Student student);
    void delete(int id);
    int count();
}
```

### 16.3 StudentDAO Implementation

```java
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAOImpl implements StudentDAO {

    private static final String URL = "jdbc:postgresql://localhost:5432/school_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "your_password";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    @Override
    public void insert(Student student) {
        String sql = "INSERT INTO students (first_name, last_name, email, age, gpa, enrolled) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, student.getFirstName());
            pstmt.setString(2, student.getLastName());
            pstmt.setString(3, student.getEmail());
            pstmt.setInt(4, student.getAge());
            pstmt.setBigDecimal(5, student.getGpa());
            pstmt.setBoolean(6, student.isEnrolled());

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    student.setId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting student", e);
        }
    }

    @Override
    public Student findById(int id) {
        String sql = "SELECT * FROM students WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToStudent(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding student by ID", e);
        }

        return null;
    }

    @Override
    public List<Student> findAll() {
        String sql = "SELECT * FROM students ORDER BY id";
        List<Student> students = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                students.add(mapRowToStudent(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding all students", e);
        }

        return students;
    }

    @Override
    public List<Student> findByLastName(String lastName) {
        String sql = "SELECT * FROM students WHERE last_name ILIKE ? ORDER BY first_name";
        List<Student> students = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + lastName + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapRowToStudent(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding students by last name", e);
        }

        return students;
    }

    @Override
    public void update(Student student) {
        String sql = "UPDATE students SET first_name = ?, last_name = ?, email = ?, age = ?, gpa = ?, enrolled = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, student.getFirstName());
            pstmt.setString(2, student.getLastName());
            pstmt.setString(3, student.getEmail());
            pstmt.setInt(4, student.getAge());
            pstmt.setBigDecimal(5, student.getGpa());
            pstmt.setBoolean(6, student.isEnrolled());
            pstmt.setInt(7, student.getId());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new RuntimeException("Student with ID " + student.getId() + " not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error updating student", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM students WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting student", e);
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM students";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error counting students", e);
        }

        return 0;
    }

    // Helper method — maps a ResultSet row to a Student object
    private Student mapRowToStudent(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setId(rs.getInt("id"));
        student.setFirstName(rs.getString("first_name"));
        student.setLastName(rs.getString("last_name"));
        student.setEmail(rs.getString("email"));
        student.setAge(rs.getInt("age"));
        student.setGpa(rs.getBigDecimal("gpa"));
        student.setEnrolled(rs.getBoolean("enrolled"));
        student.setCreatedAt(rs.getTimestamp("created_at"));
        return student;
    }
}
```

### 16.4 Using the DAO

```java
import java.math.BigDecimal;
import java.util.List;

public class DAOUsageDemo {

    public static void main(String[] args) {

        StudentDAO studentDAO = new StudentDAOImpl();

        // CREATE
        Student newStudent = new Student("Kerem", "Aktaş", "kerem.aktas@example.com", 21, new BigDecimal("3.65"));
        studentDAO.insert(newStudent);
        System.out.println("Inserted: " + newStudent);  // ID is now set

        // READ — single
        Student found = studentDAO.findById(newStudent.getId());
        System.out.println("Found: " + found);

        // READ — all
        List<Student> allStudents = studentDAO.findAll();
        System.out.println("\nAll students (" + studentDAO.count() + " total):");
        allStudents.forEach(s -> System.out.println("  " + s));

        // UPDATE
        found.setGpa(new BigDecimal("3.85"));
        found.setEmail("kerem.aktas.new@example.com");
        studentDAO.update(found);
        System.out.println("\nUpdated: " + studentDAO.findById(found.getId()));

        // DELETE
        studentDAO.delete(found.getId());
        System.out.println("\nDeleted student with ID: " + found.getId());
        System.out.println("Remaining students: " + studentDAO.count());
    }
}
```

---

## 17. Connection Pooling (HikariCP)

### Why Connection Pooling?

Creating a new database connection on every request is **expensive** (TCP handshake, authentication, etc.). A **connection pool** maintains a set of reusable connections.

| Without Pool | With Pool |
|-------------|-----------|
| ~50-100ms per connection | ~1ms per connection |
| No limit on connections | Controlled pool size |
| Resource exhaustion risk | Stable under load |

### 17.1 Maven Dependency

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

### 17.2 DataSource Configuration

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionPool {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/school_db");
        config.setUsername("postgres");
        config.setPassword("your_password");

        // Pool settings
        config.setMaximumPoolSize(10);       // Max connections in pool
        config.setMinimumIdle(2);            // Min idle connections
        config.setIdleTimeout(300000);       // 5 minutes idle before closing
        config.setConnectionTimeout(20000);  // 20 seconds to get a connection
        config.setMaxLifetime(1200000);      // 20 minutes max lifetime

        // PostgreSQL-specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
```

### 17.3 Using the Connection Pool

```java
// Same usage as before — just change the connection source
try (Connection conn = ConnectionPool.getConnection();
     PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM students")) {

    try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
            System.out.println(rs.getString("first_name") + " " + rs.getString("last_name"));
        }
    }

} catch (SQLException e) {
    e.printStackTrace();
}

// Shutdown pool when application exits
// ConnectionPool.close();
```

---

## 18. Common Exceptions and Troubleshooting

### Common SQL State Codes

| SQL State | Error | Cause |
|-----------|-------|-------|
| `08001` | Connection error | Wrong URL, database not running |
| `08006` | Connection failure | Server disconnected |
| `28P01` | Authentication failed | Wrong username or password |
| `3D000` | Database does not exist | Typo in database name |
| `42P01` | Table does not exist | Wrong table name or not created yet |
| `42P04` | Database already exists | Trying to CREATE existing database |
| `42P07` | Table already exists | Use `IF NOT EXISTS` |
| `42703` | Column does not exist | Wrong column name |
| `23505` | Unique violation | Duplicate value in UNIQUE column |
| `23503` | Foreign key violation | Referenced row doesn't exist |
| `23502` | NOT NULL violation | Missing required value |
| `23514` | CHECK constraint violation | Value doesn't meet CHECK condition |

### Exception Handling Template

```java
try {
    // Database operations
} catch (SQLException e) {
    switch (e.getSQLState()) {
        case "23505":
            System.err.println("Duplicate entry! A record with that value already exists.");
            break;
        case "23503":
            System.err.println("Invalid reference! The referenced record does not exist.");
            break;
        case "23502":
            System.err.println("Missing required field! Please provide all required values.");
            break;
        case "08001":
        case "08006":
            System.err.println("Cannot connect to the database. Is PostgreSQL running?");
            break;
        default:
            System.err.println("Database error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            break;
    }
}
```

### Common Mistakes and Fixes

| Mistake | Fix |
|---------|-----|
| Not closing resources | Use try-with-resources |
| String concatenation in SQL | Use `PreparedStatement` with `?` |
| Forgetting to call `executeUpdate()` | `executeQuery()` is for SELECT only |
| Using `executeQuery()` for INSERT/UPDATE/DELETE | Use `executeUpdate()` instead |
| Not handling `NULL` values from `ResultSet` | Check `rs.wasNull()` after getting value |
| Assuming column index starts at 0 | JDBC column indices start at **1** |
| Not committing after `setAutoCommit(false)` | Always call `conn.commit()` |

---

## 19. Best Practices

### Resource Management
```java
// ALWAYS use try-with-resources
try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement pstmt = conn.prepareStatement(sql);
     ResultSet rs = pstmt.executeQuery()) {
    // Resources are automatically closed, even if an exception occurs
}
```

### Security
- **Always** use `PreparedStatement` for queries with user input
- **Never** concatenate user input into SQL strings
- Store database credentials in configuration files or environment variables, not in source code
- Use the principle of least privilege for database users

### Performance
- Use connection pooling (HikariCP) in production
- Use batch operations for bulk inserts/updates
- Close resources as soon as you're done with them
- Only select the columns you need — avoid `SELECT *` in production
- Use database indexes for frequently queried columns

### Code Organization
- Use the DAO pattern to separate database logic from business logic
- Create a utility class for database connections
- Use model/POJO classes to represent database rows
- Handle exceptions appropriately — don't just print stack traces in production

### Summary of `execute` Methods

| Method | Use For | Returns |
|--------|---------|---------|
| `executeQuery(sql)` | SELECT statements | `ResultSet` |
| `executeUpdate(sql)` | INSERT, UPDATE, DELETE, DDL | `int` (rows affected) |
| `execute(sql)` | Any SQL (rarely used) | `boolean` (true if ResultSet) |
| `executeBatch()` | Multiple statements at once | `int[]` (rows affected per statement) |

---

## 20. Exercises

### Exercise 1: Library Database

Create a `library_db` database with the following tables:

- **authors** (id, first_name, last_name, birth_year, nationality)
- **books** (id, title, isbn, publish_year, pages, author_id → references authors)
- **members** (id, name, email, membership_date)
- **loans** (id, book_id → books, member_id → members, loan_date, return_date)

Tasks:
1. Create the database, tables, and insert 5 authors, 10 books, and 3 members.
2. Write a method to loan a book to a member (INSERT into loans).
3. Write a method to return a book (UPDATE return_date in loans).
4. Write a query to find all books currently on loan (return_date IS NULL).
5. Write a query to find the most loaned book.

---

### Exercise 2: Student Management System

Using the `school_db` from this material:

1. Add 10 students using batch insert.
2. Find all students with GPA above 3.5, ordered by GPA descending.
3. Update the GPA of a student by their email.
4. Delete all students who are not enrolled.
5. Display the average GPA per department using a JOIN query.
6. Implement the complete DAO pattern with a `CourseDAO`.

---

### Exercise 3: Transaction Challenge

Create a **money transfer** system:

- **accounts** table with `id`, `holder_name`, `balance`
- Write a `transfer(fromId, toId, amount)` method that:
  - Deducts `amount` from the sender's balance
  - Adds `amount` to the receiver's balance
  - Uses a transaction (both operations must succeed or both must fail)
  - Throws an exception if the sender has insufficient funds
  - Returns the updated balances of both accounts

---

### Exercise 4: Dynamic Query Builder

Write a method `searchStudents(String name, Integer minAge, Integer maxAge, BigDecimal minGpa)` that:

- Builds a SQL query dynamically based on which parameters are not null
- Uses `PreparedStatement` with proper parameter binding
- Returns a `List<Student>`

Example calls:
```java
searchStudents("Ali", null, null, null);        // search by name only
searchStudents(null, 20, 25, null);              // search by age range
searchStudents(null, null, null, new BigDecimal("3.5")); // search by min GPA
searchStudents("Ali", 18, 25, new BigDecimal("3.0"));   // all filters
```

---

### Exercise 5: Database Reporting

Write a program that generates the following report from `school_db`:

```
=== SCHOOL DATABASE REPORT ===

Total Students: 15
Average GPA: 3.62
Enrolled: 12 | Not Enrolled: 3

Departments:
  Computer Science — 5 courses, 30 enrollments
  Mathematics      — 3 courses, 18 enrollments

Top 3 Students:
  1. Ayşe Kaya — GPA: 3.95
  2. Selin Koç — GPA: 3.92
  3. Ali Yılmaz — GPA: 3.85

Recent Enrollments (last 30 days):
  Ali Yılmaz → Database Systems (2024-03-15)
  Zeynep Arslan → Linear Algebra (2024-03-18)
```

---

## Quick Reference Card

```
┌───────────────────────────────────────────────────────────────────┐
│                    JDBC Quick Reference                          │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  CONNECTION                                                       │
│  ──────────────────────────────────────────────────────────────── │
│  Connection conn = DriverManager.getConnection(url, user, pass); │
│  URL format: jdbc:postgresql://localhost:5432/dbname              │
│                                                                   │
│  STATEMENT (static SQL only)                                      │
│  ──────────────────────────────────────────────────────────────── │
│  Statement stmt = conn.createStatement();                         │
│  ResultSet rs = stmt.executeQuery("SELECT ...");                  │
│  int rows = stmt.executeUpdate("INSERT/UPDATE/DELETE ...");       │
│                                                                   │
│  PREPARED STATEMENT (use for all user input!)                     │
│  ──────────────────────────────────────────────────────────────── │
│  PreparedStatement ps = conn.prepareStatement("... WHERE x=?");  │
│  ps.setString(1, value);                                          │
│  ResultSet rs = ps.executeQuery();     // SELECT                  │
│  int rows = ps.executeUpdate();        // INSERT/UPDATE/DELETE    │
│                                                                   │
│  RESULT SET                                                       │
│  ──────────────────────────────────────────────────────────────── │
│  while (rs.next()) {                                              │
│      rs.getString("col");    rs.getInt("col");                    │
│      rs.getDouble("col");    rs.getBoolean("col");                │
│      rs.getDate("col");      rs.getBigDecimal("col");             │
│  }                                                                │
│                                                                   │
│  TRANSACTIONS                                                     │
│  ──────────────────────────────────────────────────────────────── │
│  conn.setAutoCommit(false);                                       │
│  // ... operations ...                                            │
│  conn.commit();      // or conn.rollback();                       │
│                                                                   │
│  GOLDEN RULES                                                     │
│  ──────────────────────────────────────────────────────────────── │
│  ✓ Always use try-with-resources                                  │
│  ✓ Always use PreparedStatement for user input                    │
│  ✓ Always handle SQLException                                     │
│  ✗ Never concatenate user input into SQL                          │
│  ✗ Never forget to close resources                                │
│  ✗ Never store passwords in source code (use config files)        │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

---

> **Document Version:** 1.0  
> **Last Updated:** April 2026  
> **License:** Free to use for educational purposes
