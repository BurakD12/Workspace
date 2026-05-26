# Question 2 — Student Course Enrollment System (Medium)

**Difficulty:** Medium  
**Topic:** JDBC CRUD with Multiple Tables, Foreign Keys, Transactions, Service Layer  
**Estimated Time:** 2–3 hours

---

## Project Description

Build a console-based **Student Course Enrollment System** for a university. The system manages students, courses, and their enrollments. A student can enroll in a course only if there is remaining capacity. Enrolling a student is a **transactional operation** — both the enrollment record insertion and the capacity decrement on the course must succeed together or both must fail.

You will implement two full DAO pairs (Student, Course) and one **service layer** class (`EnrollmentService`) that coordinates multi-table operations with transactions.

---

## Database Setup

**Step 1 — Create the database:**
```sql
CREATE DATABASE university_db;
```

**Step 2 — Connect to `university_db` and create the tables (in this order):**
```sql
CREATE TABLE IF NOT EXISTS students (
    id         SERIAL PRIMARY KEY,
    full_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(100) NOT NULL UNIQUE,
    age        INTEGER      NOT NULL CHECK (age >= 16 AND age <= 100)
);

CREATE TABLE IF NOT EXISTS courses (
    id            SERIAL PRIMARY KEY,
    course_name   VARCHAR(150) NOT NULL,
    course_code   VARCHAR(20)  NOT NULL UNIQUE,
    credits       INTEGER      NOT NULL CHECK (credits >= 1 AND credits <= 6),
    max_capacity  INTEGER      NOT NULL CHECK (max_capacity > 0),
    enrolled_count INTEGER     NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS enrollments (
    id              SERIAL PRIMARY KEY,
    student_id      INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    course_id       INTEGER NOT NULL REFERENCES courses(id)  ON DELETE CASCADE,
    enrollment_date DATE    NOT NULL DEFAULT CURRENT_DATE,
    grade           VARCHAR(2),
    UNIQUE(student_id, course_id)
);
```

> **Note on `enrolled_count`:** This column tracks how many students are currently enrolled in a course. It starts at `0` and is incremented by `1` in the same transaction when a student enrolls. This makes capacity checks fast without needing `COUNT(*)` every time.

---

## Required Classes

---

### 1. `Student` (Model / POJO)

**Package:** `com.university.model`  
**File:** `Student.java`

Represents one row in the `students` table.

#### Attributes

| Field | Type | Column |
|-------|------|--------|
| `id` | `int` | `id` |
| `fullName` | `String` | `full_name` |
| `email` | `String` | `email` |
| `age` | `int` | `age` |

#### Constructors

| Constructor | Parameters | Purpose |
|-------------|------------|---------|
| `Student()` | none | Default, used when mapping from ResultSet |
| `Student(String fullName, String email, int age)` | 3 params | Used for inserting new students; `id` defaults to `0` |

#### Methods

All standard getters and setters for every field, plus:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `toString()` | `String` | `Student{id=1, name='Ali Yılmaz', email='ali@example.com', age=20}` |

---

### 2. `Course` (Model / POJO)

**Package:** `com.university.model`  
**File:** `Course.java`

Represents one row in the `courses` table.

#### Attributes

| Field | Type | Column |
|-------|------|--------|
| `id` | `int` | `id` |
| `courseName` | `String` | `course_name` |
| `courseCode` | `String` | `course_code` |
| `credits` | `int` | `credits` |
| `maxCapacity` | `int` | `max_capacity` |
| `enrolledCount` | `int` | `enrolled_count` |

#### Constructors

| Constructor | Parameters | Purpose |
|-------------|------------|---------|
| `Course()` | none | Default, used when mapping from ResultSet |
| `Course(String courseName, String courseCode, int credits, int maxCapacity)` | 4 params | For inserting new courses; `id` defaults to `0`, `enrolledCount` defaults to `0` |

#### Methods

All standard getters and setters, plus:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `hasAvailableSpot()` | `boolean` | Returns `enrolledCount < maxCapacity` |
| `toString()` | `String` | `Course{id=1, code='CS101', name='Data Structures', credits=3, capacity=30, enrolled=12}` |

---

### 3. `Enrollment` (Model / POJO)

**Package:** `com.university.model`  
**File:** `Enrollment.java`

Represents one row in the `enrollments` table.

#### Attributes

| Field | Type | Column |
|-------|------|--------|
| `id` | `int` | `id` |
| `studentId` | `int` | `student_id` |
| `courseId` | `int` | `course_id` |
| `enrollmentDate` | `java.sql.Date` | `enrollment_date` |
| `grade` | `String` | `grade` (nullable) |

#### Constructors

| Constructor | Parameters | Purpose |
|-------------|------------|---------|
| `Enrollment()` | none | Default |
| `Enrollment(int studentId, int courseId)` | 2 params | For creating new enrollment; date set to today, grade is `null` |

#### Methods

All standard getters and setters, plus `toString()`.

---

### 4. `DatabaseConnection` (Utility)

**Package:** `com.university.util`  
**File:** `DatabaseConnection.java`

Same pattern as Question 1, but points to `university_db`.

| Constant | Value |
|----------|-------|
| `URL` | `"jdbc:postgresql://localhost:5432/university_db"` |
| `USER` | `"postgres"` |
| `PASSWORD` | `"your_password"` |

| Method | Return Type | Throws | Description |
|--------|-------------|--------|-------------|
| `getConnection()` | `Connection` | `SQLException` | Returns a new connection each time it is called |

---

### 5. `StudentDAO` (Interface)

**Package:** `com.university.dao`  
**File:** `StudentDAO.java`

| Method | Parameters | Return Type | Description |
|--------|------------|-------------|-------------|
| `insert(Student student)` | `Student` | `void` | Inserts student; sets generated id on the object |
| `findById(int id)` | `int` | `Student` | Returns student or `null` |
| `findAll()` | — | `List<Student>` | All students ordered by `id` |
| `findByEmail(String email)` | `String` | `Student` | Returns student matching email exactly, or `null` |
| `update(Student student)` | `Student` | `void` | Updates `full_name`, `email`, `age` by `id` |
| `delete(int id)` | `int` | `void` | Deletes student (cascades to enrollments automatically) |

---

### 6. `StudentDAOImpl` (DAO Implementation)

**Package:** `com.university.dao`  
**File:** `StudentDAOImpl.java`

Implements `StudentDAO`. Opens its own connection per method.

#### Private Helper

| Method | Return | Description |
|--------|--------|-------------|
| `mapRow(ResultSet rs)` | `Student` | Maps current ResultSet row → `Student` object |

#### SQL Queries

| Method | SQL |
|--------|-----|
| `insert` | `INSERT INTO students (full_name, email, age) VALUES (?, ?, ?)` with `RETURN_GENERATED_KEYS` |
| `findById` | `SELECT * FROM students WHERE id = ?` |
| `findAll` | `SELECT * FROM students ORDER BY id` |
| `findByEmail` | `SELECT * FROM students WHERE email = ?` |
| `update` | `UPDATE students SET full_name = ?, email = ?, age = ? WHERE id = ?` |
| `delete` | `DELETE FROM students WHERE id = ?` |

---

### 7. `CourseDAO` (Interface)

**Package:** `com.university.dao`  
**File:** `CourseDAO.java`

| Method | Parameters | Return Type | Description |
|--------|------------|-------------|-------------|
| `insert(Course course)` | `Course` | `void` | Inserts course; sets generated id on the object |
| `findById(int id)` | `int` | `Course` | Returns course or `null` |
| `findAll()` | — | `List<Course>` | All courses ordered by `course_code` |
| `findByCode(String code)` | `String` | `Course` | Returns course matching code exactly, or `null` |
| `update(Course course)` | `Course` | `void` | Updates all fields by `id` |
| `delete(int id)` | `int` | `void` | Deletes course |

---

### 8. `CourseDAOImpl` (DAO Implementation)

**Package:** `com.university.dao`  
**File:** `CourseDAOImpl.java`

Implements `CourseDAO`. Same pattern as `StudentDAOImpl`.

#### SQL Queries

| Method | SQL |
|--------|-----|
| `insert` | `INSERT INTO courses (course_name, course_code, credits, max_capacity) VALUES (?, ?, ?, ?)` with `RETURN_GENERATED_KEYS` |
| `findById` | `SELECT * FROM courses WHERE id = ?` |
| `findAll` | `SELECT * FROM courses ORDER BY course_code` |
| `findByCode` | `SELECT * FROM courses WHERE course_code = ?` |
| `update` | `UPDATE courses SET course_name=?, course_code=?, credits=?, max_capacity=?, enrolled_count=? WHERE id=?` |
| `delete` | `DELETE FROM courses WHERE id = ?` |

---

### 9. `EnrollmentService` (Service / Business Logic Layer)

**Package:** `com.university.service`  
**File:** `EnrollmentService.java`

This is the most important class. It handles operations that involve **more than one table** and must use **explicit transactions** (`conn.setAutoCommit(false)`). It does **not** use `DatabaseConnection.getConnection()` through the DAOs — it manages its own connection and passes it to raw SQL operations directly.

> **Design note:** `EnrollmentService` does NOT use the DAO classes. It opens one connection, sets `autoCommit = false`, runs multiple statements on that same connection, and then commits or rolls back.

#### Constructor

```java
public EnrollmentService()
```
No-argument constructor. No fields needed.

---

#### Methods

---

**`enrollStudent(int studentId, int courseId)`**

- **Return type:** `void`
- **Throws:** `IllegalArgumentException`, `RuntimeException`
- **Description:** Enrolls a student in a course inside a single transaction.
- **Steps performed inside one transaction:**
  1. Query the `students` table to verify the student with `studentId` exists. If not, throw `IllegalArgumentException("Student not found with ID: " + studentId)`.
  2. Query the `courses` table to verify the course with `courseId` exists. If not, throw `IllegalArgumentException("Course not found with ID: " + courseId)`.
  3. Check if `enrolled_count < max_capacity`. If not, throw `IllegalArgumentException("Course is full: " + courseCode)`.
  4. Check the `enrollments` table: `SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND course_id = ?`. If count > 0, throw `IllegalArgumentException("Student is already enrolled in this course.")`.
  5. Insert a row into `enrollments (student_id, course_id)`.
  6. Execute `UPDATE courses SET enrolled_count = enrolled_count + 1 WHERE id = ?`.
  7. Commit.
- **On any `SQLException`:** Roll back and throw `RuntimeException("Enrollment failed", e)`.
- **Prints on success:** `"Student [fullName] enrolled in [courseName] successfully."`

---

**`dropEnrollment(int studentId, int courseId)`**

- **Return type:** `void`
- **Throws:** `IllegalArgumentException`, `RuntimeException`
- **Description:** Removes an enrollment inside a single transaction.
- **Steps:**
  1. Verify the enrollment exists. If not, throw `IllegalArgumentException("Enrollment not found.")`.
  2. Delete the row from `enrollments WHERE student_id = ? AND course_id = ?`.
  3. Execute `UPDATE courses SET enrolled_count = enrolled_count - 1 WHERE id = ?`.
  4. Commit.
- **Prints on success:** `"Enrollment dropped successfully."`

---

**`assignGrade(int studentId, int courseId, String grade)`**

- **Return type:** `void`
- **Throws:** `IllegalArgumentException`, `RuntimeException`
- **Description:** Sets the grade for an existing enrollment. Does **not** need a transaction (single UPDATE).
- **Grade validation:** `grade` must be one of: `"AA"`, `"BA"`, `"BB"`, `"CB"`, `"CC"`, `"DC"`, `"DD"`, `"FF"`. If not, throw `IllegalArgumentException("Invalid grade: " + grade)`.
- **SQL:** `UPDATE enrollments SET grade = ? WHERE student_id = ? AND course_id = ?`
- **If 0 rows updated:** throw `IllegalArgumentException("Enrollment not found.")`.
- **Prints on success:** `"Grade [grade] assigned successfully."`

---

**`getEnrollmentsForStudent(int studentId)`**

- **Return type:** `List<String>`
- **Description:** Returns a list of formatted strings, each describing one enrollment.
- **SQL (JOIN):**
  ```sql
  SELECT c.course_name, c.course_code, e.enrollment_date, e.grade
  FROM enrollments e
  JOIN courses c ON e.course_id = c.id
  WHERE e.student_id = ?
  ORDER BY e.enrollment_date
  ```
- **Each string format:** `"[courseCode] [courseName] | Enrolled: [enrollment_date] | Grade: [grade or N/A]"`
- **Example:** `"CS101 Data Structures | Enrolled: 2025-09-01 | Grade: BA"`

---

**`getEnrollmentsForCourse(int courseId)`**

- **Return type:** `List<String>`
- **Description:** Returns a list of formatted strings for all students in a course.
- **SQL (JOIN):**
  ```sql
  SELECT s.full_name, s.email, e.enrollment_date, e.grade
  FROM enrollments e
  JOIN students s ON e.student_id = s.id
  WHERE e.course_id = ?
  ORDER BY s.full_name
  ```
- **Each string format:** `"[fullName] ([email]) | Enrolled: [enrollment_date] | Grade: [grade or N/A]"`

---

### 10. `UniversityApp` (Main / Entry Point)

**Package:** `com.university`  
**File:** `UniversityApp.java`

Contains `main`. Uses `StudentDAOImpl`, `CourseDAOImpl`, and `EnrollmentService`. Displays a two-level menu.

#### Top-Level Menu

```
=== University Enrollment System ===

1. Student Management
2. Course Management
3. Enrollment Management
0. Exit
```

#### Student Management Sub-Menu (when 1 is selected)

```
--- Student Management ---
1. Add Student
2. List All Students
3. Find Student by ID
4. Find Student by Email
5. Delete Student
0. Back
```

#### Course Management Sub-Menu (when 2 is selected)

```
--- Course Management ---
1. Add Course
2. List All Courses
3. Find Course by Code
4. Back to Main Menu
0. Back
```

#### Enrollment Management Sub-Menu (when 3 is selected)

```
--- Enrollment Management ---
1. Enroll Student in Course
2. Drop Enrollment
3. Assign Grade
4. View Student's Enrollments
5. View Course Enrollments
0. Back
```

---

## Program Execution Flow

```
main()
  ├─ Create StudentDAOImpl, CourseDAOImpl, EnrollmentService
  ├─ Create Scanner
  └─ Top-level loop
       ├─ 1 → Student Management loop
       │    ├─ 1 → prompt fullName, email, age → dao.insert(student)
       │    ├─ 2 → dao.findAll() → print table
       │    ├─ 3 → prompt id → dao.findById(id) → print or "Not found"
       │    ├─ 4 → prompt email → dao.findByEmail(email) → print or "Not found"
       │    ├─ 5 → prompt id → dao.delete(id)
       │    └─ 0 → back
       ├─ 2 → Course Management loop
       │    ├─ 1 → prompt name, code, credits, capacity → dao.insert(course)
       │    ├─ 2 → dao.findAll() → print table
       │    ├─ 3 → prompt code → dao.findByCode(code) → print or "Not found"
       │    └─ 0 → back
       ├─ 3 → Enrollment Management loop
       │    ├─ 1 → prompt studentId, courseId → service.enrollStudent(...)
       │    ├─ 2 → prompt studentId, courseId → service.dropEnrollment(...)
       │    ├─ 3 → prompt studentId, courseId, grade → service.assignGrade(...)
       │    ├─ 4 → prompt studentId → service.getEnrollmentsForStudent(...) → print list
       │    ├─ 5 → prompt courseId → service.getEnrollmentsForCourse(...) → print list
       │    └─ 0 → back
       └─ 0 → exit
```

---

## Sample Console Interaction (Complete Run)

```
=== University Enrollment System ===
1. Student Management
2. Course Management
3. Enrollment Management
0. Exit

Select option: > 1

--- Student Management ---
1. Add Student
2. List All Students
3. Find Student by ID
4. Find Student by Email
5. Delete Student
0. Back

Select option: > 1
Enter full name: > Ali Yılmaz
Enter email: > ali@example.com
Enter age: > 20
Student added successfully! (ID: 1)

Select option: > 1
Enter full name: > Ayşe Kaya
Enter email: > ayse@example.com
Enter age: > 22
Student added successfully! (ID: 2)

Select option: > 2
ID  | Full Name        | Email                | Age
----|------------------|----------------------|----
1   | Ali Yılmaz       | ali@example.com      | 20
2   | Ayşe Kaya        | ayse@example.com     | 22

Select option: > 0

=== University Enrollment System ===
Select option: > 2

--- Course Management ---
1. Add Course
2. List All Courses
3. Find Course by Code
0. Back

Select option: > 1
Enter course name: > Data Structures
Enter course code: > CS101
Enter credits: > 3
Enter max capacity: > 30
Course added successfully! (ID: 1)

Select option: > 1
Enter course name: > Calculus I
Enter course code: > MATH101
Enter credits: > 4
Enter max capacity: > 2
Course added successfully! (ID: 2)

Select option: > 2
ID  | Code     | Course Name       | Credits | Capacity | Enrolled
----|----------|-------------------|---------|----------|---------
1   | CS101    | Data Structures   | 3       | 30       | 0
2   | MATH101  | Calculus I        | 4       | 2        | 0

Select option: > 0

=== University Enrollment System ===
Select option: > 3

--- Enrollment Management ---
Select option: > 1
Enter student ID: > 1
Enter course ID: > 1
Student Ali Yılmaz enrolled in Data Structures successfully.

Select option: > 1
Enter student ID: > 2
Enter course ID: > 1
Student Ayşe Kaya enrolled in Data Structures successfully.

Select option: > 1
Enter student ID: > 1
Enter course ID: > 2
Student Ali Yılmaz enrolled in Calculus I successfully.

Select option: > 1
Enter student ID: > 2
Enter course ID: > 2
Student Ayşe Kaya enrolled in Calculus I successfully.

Select option: > 1
Enter student ID: > 1
Enter course ID: > 2
Error: Student is already enrolled in this course.

Select option: > 4
Enter student ID: > 1
Enrollments for student ID 1:
  CS101  Data Structures | Enrolled: 2025-09-01 | Grade: N/A
  MATH101 Calculus I     | Enrolled: 2025-09-01 | Grade: N/A

Select option: > 3
Enter student ID: > 1
Enter course ID: > 1
Enter grade (AA/BA/BB/CB/CC/DC/DD/FF): > BA
Grade BA assigned successfully.

Select option: > 5
Enter course ID: > 1
Students enrolled in course ID 1:
  Ali Yılmaz (ali@example.com) | Enrolled: 2025-09-01 | Grade: BA
  Ayşe Kaya (ayse@example.com) | Enrolled: 2025-09-01 | Grade: N/A

Select option: > 2
Enter student ID: > 1
Enter course ID: > 1
Enrollment dropped successfully.

Select option: > 5
Enter course ID: > 1
Students enrolled in course ID 1:
  Ayşe Kaya (ayse@example.com) | Enrolled: 2025-09-01 | Grade: N/A

Select option: > 0

=== University Enrollment System ===
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
            └── university/
                ├── UniversityApp.java           ← main method here
                ├── model/
                │   ├── Student.java
                │   ├── Course.java
                │   └── Enrollment.java
                ├── dao/
                │   ├── StudentDAO.java
                │   ├── StudentDAOImpl.java
                │   ├── CourseDAO.java
                │   └── CourseDAOImpl.java
                ├── service/
                │   └── EnrollmentService.java
                └── util/
                    └── DatabaseConnection.java
```

---

## Key Concepts Practiced

| Concept | Where Applied |
|---------|--------------|
| DAO Pattern | `StudentDAOImpl`, `CourseDAOImpl` |
| Service Layer | `EnrollmentService` separates business logic |
| Foreign Keys | `enrollments` references `students` and `courses` |
| Transactions | `enrollStudent()` and `dropEnrollment()` |
| JOIN Queries | `getEnrollmentsForStudent()`, `getEnrollmentsForCourse()` |
| `UNIQUE` Constraint Handling | Duplicate enrollment detected in `enrollStudent()` |
| PreparedStatement | All queries with user input |
| Exception Handling | `IllegalArgumentException` for business rule violations |

---

## Checklist Before Submission

- [ ] Tables created in `university_db` in the correct order (students → courses → enrollments)
- [ ] `enrolledCount` is incremented/decremented atomically with enrollment insert/delete
- [ ] `enrollStudent()` and `dropEnrollment()` both use a single connection with `autoCommit = false`
- [ ] Grade validation rejects invalid letter grades
- [ ] JOIN queries return formatted strings (not raw model objects)
- [ ] Deleting a student automatically removes their enrollments (via `ON DELETE CASCADE`)
- [ ] Sub-menus return to the correct parent menu
- [ ] `IllegalArgumentException` messages are caught in `main` and printed without stack trace
