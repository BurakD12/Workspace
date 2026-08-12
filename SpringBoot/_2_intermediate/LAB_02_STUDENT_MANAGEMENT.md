# Lab 02 - Student Management System API (HTTP Status Codes Done Right)

## 1. Lab Goal

Build a second layered Spring Boot backend, this time focusing on **correct HTTP semantics**. In Lab 01 you learned how the layers (controller, service, repository, entity, dto) fit together. In Lab 02 you keep that exact structure, but you make every endpoint return a **deliberate, correct HTTP status code** and an explicit `ResponseEntity`.

By the end of this lab, the student should have a working backend that:

1. Connects to a local PostgreSQL database.
2. Stores student data in a table.
3. Exposes REST endpoints for create, read, update, delete, and filter operations.
4. Uses the same clean layered architecture as Lab 01.
5. Validates incoming request data.
6. **Returns the right status code for every outcome**: `200`, `201`, `204`, `400`, `404`, `409`, and a controlled `500`.
7. Sends a `Location` header when a new resource is created.
8. Returns consistent, machine-readable error responses using `ProblemDetail`.

This document is implementation guidance. For the parts that are new in this lab — the **controller status handling**, the **validation annotations**, and the **exception handler** — short reference snippets are included, because that is the whole point of the lab. The entity, repository, and service business logic are described in prose, exactly like Lab 01, and you should write them yourself.

## 2. What You Should Already Know From Lab 01

This lab assumes you completed Lab 01 and understand:

1. How a request flows: `Controller -> Service -> Repository -> Database` and back.
2. Why DTOs are used instead of exposing the entity.
3. How Spring Data JPA generates repository implementations.
4. How `@Valid` triggers bean validation.
5. How a `@RestControllerAdvice` turns thrown exceptions into HTTP responses.

If any of those are unclear, revisit Lab 01 before continuing. Lab 02 builds directly on them.

## 3. The One Big New Idea: Status Codes Are Part Of Your API

In Lab 01, several endpoints simply returned an object and let Spring default to `200 OK`. That works, but it is sloppy. A real REST API treats the **status code as part of the contract**: the client decides what to do based on the code, not just the body.

The rule for this lab:

> Every controller method returns a `ResponseEntity<...>`, and every return path uses a status code that you chose on purpose.

You will learn the difference between codes that look interchangeable but are not:

1. `200 OK` vs `201 Created` vs `204 No Content`
2. `400 Bad Request` vs `404 Not Found` vs `409 Conflict`
3. Why an empty list is `200` with `[]`, **not** `404`.
4. Why an uncaught exception becoming `500` is a bug you must prevent.

Section 16 is the dedicated status-code reference. Read it before writing the controller.

## 4. Business Scenario

You are building a small backend for a university registrar office.

The system must manage students with information such as:

1. Student ID (database primary key)
2. First name
3. Last name
4. Email (must be unique)
5. Student number (a unique business identifier, e.g. `S2024001`)
6. Department / major (e.g. `Computer Science`)
7. GPA (a decimal between `0.0` and `4.0`)
8. Enrollment date (cannot be in the future)

Each student record must be stored in PostgreSQL and must be available through REST endpoints. Note two intentional design choices that create real status-code scenarios:

1. **Two unique fields** (`email` and `studentNumber`) → two distinct `409 Conflict` situations.
2. **A bounded decimal and a date constraint** → richer `400 Bad Request` validation cases than Lab 01.

## 5. Suggested Build Order

Implement the project in this exact order:

1. Create the new `_2_intermediate` project (Section 6).
2. Update dependencies in `pom.xml`.
3. Configure the PostgreSQL connection in `application.properties`.
4. Create the database manually in PostgreSQL.
5. Implement the `Student` entity in the `model` package.
6. Implement `StudentRequestDto` and `StudentResponseDto` in the `dto` package.
7. Implement the repository interface.
8. Implement the custom exceptions and the `GlobalExceptionHandler`.
9. Implement the service interface.
10. Implement the service implementation class.
11. **Read Section 16 (status code reference), then** implement the REST controller.
12. Run the application and manually test every status code (Section 19).

Do not skip layers. The goal of this lab is correct HTTP behavior on top of a clean architecture.

## 6. Creating The `_2_intermediate` Project

This is a new, standalone project that sits next to `_1_basics`. You have two options.

### 6.1 Option A — Spring Initializr (recommended)

Generate a new project at <https://start.spring.io> with:

1. Project: Maven
2. Language: Java
3. Spring Boot: a current `3.x` version
4. Group: `com.example`
5. Artifact / Name: `_2_intermediate`
6. Package name: `com.example._2_intermediate`
7. Dependencies: Spring Web, Spring Data JPA, Validation, PostgreSQL Driver, Lombok

Unzip it into a folder named `_2_intermediate` beside `_1_basics`.

### 6.2 Option B — Copy Lab 01 and rename

Copy the `_1_basics` project folder to `_2_intermediate`, then:

1. Rename the base package from `com.example._1_basics` to `com.example._2_intermediate`.
2. Delete all Lab 01 classes (`Employee*`, etc.) but keep `Application`.
3. Clear `pom.xml`'s `<artifactId>` / `<name>` to `_2_intermediate`.
4. Delete the `target` folder so nothing stale is reused.

### 6.3 Package note

Java identifiers may start with an underscore, so `com.example._2_intermediate` is valid (this is the same trick Lab 01 used with `_1_basics`). Keep the base package consistent everywhere.

### 6.4 Packages to create

Under `com.example._2_intermediate`, create:

1. `controller`
2. `dto`
3. `model`
4. `repository`
5. `service`
6. `exception`

## 7. Dependencies To Add In `pom.xml`

Same dependency set as Lab 01. Add and understand each one:

| Dependency                       | Why it is needed                                                                               |
| -------------------------------- | ---------------------------------------------------------------------------------------------- |
| `spring-boot-starter-web`        | REST controllers, JSON handling, embedded server, and `ResponseEntity`.                        |
| `spring-boot-starter-data-jpa`   | JPA, Hibernate, repository support, entity persistence.                                        |
| `postgresql`                     | PostgreSQL JDBC driver.                                                                         |
| `spring-boot-starter-validation` | Bean validation annotations (these produce your `400` responses).                              |
| `lombok`                         | Reduces boilerplate (constructors, getters, setters, builder).                                 |
| `spring-boot-starter-test`       | Keep for later testing work.                                                                    |

Notes:

1. If `spring-boot-starter-web` is present, the bare `spring-boot-starter` is redundant.
2. If you use Lombok, enable annotation processing in your IDE.
3. No Swagger or security dependencies are needed yet.

## 8. Database Setup

### 8.1 Credentials

1. Host: `localhost`
2. Port: `5432` (unless your install differs)
3. Username: `postgres`
4. Password: `123456`

### 8.2 Create the database

Create a dedicated database before running the app, for example `student_management_lab`. Spring Boot can create the *table*, but it cannot connect unless the *database* already exists.

## 9. Configuration In `application.properties`

In `src/main/resources/application.properties`, add the database and JPA configuration:

| Property                                     | What it does                                                          |
| -------------------------------------------- | --------------------------------------------------------------------- |
| `spring.application.name`                    | Names the application.                                                |
| `spring.datasource.url`                      | Points to the `student_management_lab` database on localhost.         |
| `spring.datasource.username`                 | Database username.                                                    |
| `spring.datasource.password`                 | Database password.                                                    |
| `spring.jpa.hibernate.ddl-auto`              | Use `update` so Hibernate creates/updates the `students` table.       |
| `spring.jpa.show-sql`                        | Prints generated SQL for learning.                                    |
| `spring.jpa.properties.hibernate.format_sql` | Makes printed SQL readable.                                           |

Guidance:

1. `update` is fine for a lab.
2. Keep SQL logging on while learning.
3. A dialect property is usually optional; Hibernate detects PostgreSQL automatically.

## 10. Package And Class Plan

The finished project should contain at least these types:

| Package      | Class or Interface           | Responsibility                                                        |
| ------------ | ---------------------------- | --------------------------------------------------------------------- |
| base         | `Application`                | Starts the Spring Boot application.                                   |
| `model`      | `Student`                    | JPA entity mapped to the `students` table.                            |
| `dto`        | `StudentRequestDto`          | Incoming data + validation rules (drives `400`).                      |
| `dto`        | `StudentResponseDto`         | Data returned to the client.                                          |
| `repository` | `StudentRepository`          | Spring Data JPA data access + derived queries.                        |
| `service`    | `StudentService`             | Business operations (interface).                                      |
| `service`    | `StudentServiceImpl`         | Business logic + DTO mapping + uniqueness checks (throws exceptions). |
| `controller` | `StudentController`          | REST endpoints; chooses the status code for every outcome.           |
| `exception`  | `StudentNotFoundException`   | Signals a missing student → maps to `404`.                           |
| `exception`  | `DuplicateResourceException` | Signals a unique-field clash → maps to `409`.                        |
| `exception`  | `GlobalExceptionHandler`     | Translates exceptions into `ProblemDetail` responses.                |

## 11. Class - `Student` Entity

Create `Student` inside `model`.

### 11.1 Purpose

Represents one student row in the database.

### 11.2 Suggested fields

1. `id` — numeric, generated primary key.
2. `firstName`
3. `lastName`
4. `email`
5. `studentNumber`
6. `department`
7. `gpa` — a decimal type (`BigDecimal`).
8. `enrollmentDate` — a date type (`LocalDate`).

### 11.3 Entity design responsibilities

The entity should:

1. Map to a table named `students`.
2. Mark `id` as the primary key, generated automatically.
3. Enforce **uniqueness on `email`** at the database level.
4. Enforce **uniqueness on `studentNumber`** at the database level.
5. Use types that match the data: `BigDecimal` for GPA, `LocalDate` for enrollment date.

### 11.4 Constructors and accessors

1. A no-argument constructor (JPA requires it).
2. A way to build/initialize the business fields. Lombok's `@Builder` (used in Lab 01) is a good fit.
3. Getters and setters, or Lombok equivalents.

Keep business logic out of the entity. Note: **validation annotations belong on the request DTO, not the entity.** The entity's job is persistence mapping.

## 12. Class - `StudentRequestDto` (this is where `400` comes from)

Create this DTO inside `dto`. This is the most important class for the `400 Bad Request` behavior, so a reference snippet is included.

### 12.1 Purpose

Represents incoming JSON for create and update requests. When any of these rules fail, Spring throws `MethodArgumentNotValidException`, which your handler turns into `400`.

### 12.2 Reference snippet (validation annotations)

```java
package com.example._2_intermediate.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequestDto {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Student number is required")
    @Pattern(regexp = "S\\d{7}", message = "Student number must look like S2024001")
    private String studentNumber;

    @NotBlank(message = "Department is required")
    private String department;

    @NotNull(message = "GPA is required")
    @DecimalMin(value = "0.0", message = "GPA cannot be below 0.0")
    @DecimalMax(value = "4.0", message = "GPA cannot exceed 4.0")
    private BigDecimal gpa;

    @NotNull(message = "Enrollment date is required")
    @PastOrPresent(message = "Enrollment date cannot be in the future")
    private LocalDate enrollmentDate;
}
```

### 12.3 Notes

1. Do **not** include `id` in the request DTO — the server generates it.
2. Each `message` you write here ends up in the `400` response body, so make them clear.
3. Use `@NotNull` (not `@NotBlank`) for non-string fields like `gpa` and `enrollmentDate`; `@NotBlank` only works on strings.

## 13. Class - `StudentResponseDto`

Create this DTO inside `dto`.

### 13.1 Purpose

Represents the JSON returned to the client.

### 13.2 Suggested fields

1. `id`
2. `firstName`
3. `lastName`
4. `email`
5. `studentNumber`
6. `department`
7. `gpa`
8. `enrollmentDate`

### 13.3 Constructors and accessors

1. A no-argument constructor.
2. A full constructor (handy for mapping in the service).
3. Getters/setters or Lombok equivalents.

No validation annotations here — this DTO is output only.

## 14. Class - `StudentRepository`

Create the interface inside `repository`.

### 14.1 Purpose

The data access layer.

### 14.2 Main responsibility

Extend the appropriate Spring Data JPA base repository so CRUD is generated automatically.

### 14.3 Derived query methods to declare

1. A method to check whether an email already exists (used for the `409` check on email).
2. A method to check whether a student number already exists (used for the `409` check on student number).
3. A method that returns all students in a given department (used by the filter endpoint).
4. Optionally, finders by email or student number.

Do not write an implementation class — Spring Data JPA generates it.

## 15. Exceptions And The Global Exception Handler (this is where `404`, `409`, and controlled `500` come from)

Because status-code handling is the theme of this lab, snippets are included here.

### 15.1 `StudentNotFoundException` → `404`

```java
package com.example._2_intermediate.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Long id) {
        super("Student not found with id: " + id);
    }
}
```

### 15.2 `DuplicateResourceException` → `409`

One reusable exception for both unique fields. It carries which field clashed, so the message is precise.

```java
package com.example._2_intermediate.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String field, String value) {
        super(field + " already in use: " + value);
    }
}
```

> Lab 01 used two separate duplicate exceptions. Here we generalize to one — fewer classes, one handler, and the message still tells the client exactly what clashed. This is a small clean-code upgrade.

### 15.3 `GlobalExceptionHandler`

This class is the single place that maps exceptions to HTTP status codes. Every error code in the API flows through here.

```java
package com.example._2_intermediate.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 NOT FOUND
    @ExceptionHandler(StudentNotFoundException.class)
    public ProblemDetail handleNotFound(StudentNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Student not found");
        return pd;
    }

    // 409 CONFLICT
    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Duplicate value");
        return pd;
    }

    // 400 BAD REQUEST - bean validation failed (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setTitle("Validation failed");

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(),
                error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
        pd.setProperty("errors", errors);
        return pd;
    }

    // 400 BAD REQUEST - wrong path-variable type, e.g. GET /api/students/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Parameter '" + ex.getName() + "' has an invalid value");
        pd.setTitle("Invalid parameter");
        return pd;
    }

    // 500 INTERNAL SERVER ERROR - safety net so unexpected errors do not leak stack traces
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred");
        pd.setTitle("Internal server error");
        return pd;
    }
}
```

### 15.4 Learning points

1. `ProblemDetail` is the built-in standard error format (RFC 7807). It gives a consistent JSON shape for every error.
2. The validation handler attaches a per-field `errors` map so the client knows *which* field was wrong.
3. The final `Exception.class` handler is a **safety net**. Unhandled exceptions would otherwise surface as an uncontrolled `500`. You control the message and avoid leaking internals.

## 16. HTTP Status Code Reference (read before writing the controller)

This is the heart of Lab 02.

### 16.1 The codes you will use

| Code  | Name                  | When you return it in this API                                               |
| ----- | --------------------- | ---------------------------------------------------------------------------- |
| `200` | OK                    | Successful `GET` (single or list) and successful `PUT` (returning the body). |
| `201` | Created               | Successful `POST`. Also send a `Location` header to the new resource.        |
| `204` | No Content            | Successful `DELETE`. No response body.                                       |
| `400` | Bad Request           | Validation failed, malformed JSON, or wrong parameter type.                  |
| `404` | Not Found             | A student with the requested `id` does not exist.                            |
| `409` | Conflict              | A unique field (`email` or `studentNumber`) is already taken.                |
| `500` | Internal Server Error | Something unexpected broke. You aim to *never* reach this uncontrolled.      |

### 16.2 Decisions that trip people up

1. **`200` vs `201`** — `POST` that creates a resource must return `201`, not `200`. `201` plus a `Location` header tells the client where the new resource lives.
2. **`200` vs `204`** — `DELETE` succeeded but has nothing to return → `204` with an empty body. Do not return `200` with a fake message unless you have a reason.
3. **Empty list is `200`, not `404`** — `GET /api/students` with zero students returns `200 OK` and `[]`. A collection that happens to be empty is a successful, valid result. Reserve `404` for "the specific resource you addressed does not exist."
4. **`400` vs `404`** — `400` means *the client's request was malformed* (bad data, wrong type). `404` means *the request was fine, but the thing isn't there*. `GET /api/students/abc` is `400` (id isn't a number); `GET /api/students/9999` is `404` (valid id, no such student).
5. **`404` vs `409`** — `404`: it doesn't exist. `409`: it conflicts with something that does exist (duplicate unique value).
6. **`PUT` design** — In this lab, `PUT /api/students/{id}` only **updates an existing** student. If the id doesn't exist, return `404` (we do not create on `PUT` here). On success it returns `200` with the updated body.

### 16.3 Status-code decision checklist

For any request, ask in order:

1. Is the input malformed (failed `@Valid`, wrong type, bad JSON)? → `400`
2. Does the addressed resource exist? If not → `404`
3. Would this violate a uniqueness rule? → `409`
4. Are you creating a new resource? → `201` + `Location`
5. Are you returning data successfully? → `200`
6. Did it succeed with nothing to return? → `204`
7. If you somehow reach an unexpected error → controlled `500` (via the global handler).

## 17. Class - `StudentService` Interface

Create inside `service`. Declare the business operations:

1. `createStudent(StudentRequestDto)` → returns `StudentResponseDto`
2. `getAllStudents()` → returns `List<StudentResponseDto>`
3. `getStudentById(Long id)` → returns `StudentResponseDto`
4. `updateStudent(Long id, StudentRequestDto)` → returns `StudentResponseDto`
5. `deleteStudent(Long id)` → returns `void`
6. `getStudentsByDepartment(String department)` → returns `List<StudentResponseDto>`

The service works with DTOs at its public boundary, not raw entities. This keeps the controller thin and keeps entity↔DTO mapping inside the service.

## 18. Class - `StudentServiceImpl`

Create inside `service`. This holds the business logic and is **where the exceptions are thrown** that become `404` and `409`.

### 18.1 Class-level responsibilities

1. Annotate with `@Service`.
2. Inject `StudentRepository` via constructor (Lombok `@RequiredArgsConstructor` is fine).
3. Implement `StudentService`.
4. Convert between entity and DTO.
5. Enforce uniqueness of `email` and `studentNumber`.

### 18.2 Methods to implement (described, not coded)

#### `createStudent(...)`

1. Check whether `email` already exists; if so, throw `DuplicateResourceException("email", ...)`. → `409`
2. Check whether `studentNumber` already exists; if so, throw `DuplicateResourceException("studentNumber", ...)`. → `409`
3. Map the request DTO to a `Student` entity.
4. Save it.
5. Map the saved entity to a response DTO and return it.

#### `getAllStudents()`

1. Read all students.
2. Map each to a response DTO.
3. Return the list (may be empty → controller returns `200` with `[]`).

#### `getStudentById(...)`

1. Look up by id; if absent, throw `StudentNotFoundException(id)`. → `404`
2. Map and return.

#### `updateStudent(...)`

1. Load the existing student by id; if absent, throw `StudentNotFoundException(id)`. → `404`
2. If the email changed, check the new email isn't taken by someone else → `409` if it is.
3. If the student number changed, do the same check → `409` if taken.
4. Copy the request values onto the existing entity.
5. Save and return the updated response DTO.

#### `deleteStudent(...)`

1. Confirm the student exists; if absent, throw `StudentNotFoundException(id)`. → `404`
2. Delete the record. Return nothing (controller returns `204`).

#### `getStudentsByDepartment(...)`

1. Query by department.
2. Map results to response DTOs.
3. Return the list (may be empty → `200` with `[]`).

### 18.3 Private helper methods

Keep the public methods readable with helpers such as:

1. `mapToEntity(StudentRequestDto)` → `Student`
2. `mapToResponseDto(Student)` → `StudentResponseDto`
3. `getStudentEntityById(Long)` → loads the entity or throws `StudentNotFoundException` (one consistent place for the not-found logic)
4. A uniqueness-check helper for email/student number

Do not duplicate mapping or not-found logic across methods.

## 19. Class - `StudentController` (every method returns an explicit `ResponseEntity`)

Create inside `controller`. This is where status codes are actually chosen. A full reference snippet is included because this is the lab's focus.

### 19.1 Endpoints

| HTTP Method | Path                                    | Purpose                      | Success code      |
| ----------- | --------------------------------------- | ---------------------------- | ----------------- |
| `POST`      | `/api/students`                         | Create a new student         | `201` + Location  |
| `GET`       | `/api/students`                         | Return all students          | `200`             |
| `GET`       | `/api/students/{id}`                    | Return one student by id     | `200`             |
| `PUT`       | `/api/students/{id}`                    | Update an existing student   | `200`             |
| `DELETE`    | `/api/students/{id}`                    | Delete a student             | `204`             |
| `GET`       | `/api/students/department/{department}` | Filter students by department| `200`             |

### 19.2 Reference snippet

```java
package com.example._2_intermediate.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example._2_intermediate.dto.StudentRequestDto;
import com.example._2_intermediate.dto.StudentResponseDto;
import com.example._2_intermediate.service.StudentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // 201 Created + Location header
    @PostMapping
    public ResponseEntity<StudentResponseDto> createStudent(
            @Valid @RequestBody StudentRequestDto requestDto) {
        StudentResponseDto created = studentService.createStudent(requestDto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()      // .../api/students
                .path("/{id}")             // .../api/students/{id}
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created); // 201
    }

    // 200 OK (empty list is still 200 with [])
    @GetMapping
    public ResponseEntity<List<StudentResponseDto>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents()); // 200
    }

    // 200 OK, or 404 if the service throws StudentNotFoundException
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id)); // 200
    }

    // 200 OK with the updated body, or 404 / 409 via the service
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDto> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDto requestDto) {
        return ResponseEntity.ok(studentService.updateStudent(id, requestDto)); // 200
    }

    // 204 No Content (no body)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build(); // 204
    }

    // 200 OK (empty list is still 200 with [])
    @GetMapping("/department/{department}")
    public ResponseEntity<List<StudentResponseDto>> getStudentsByDepartment(
            @PathVariable String department) {
        return ResponseEntity.ok(studentService.getStudentsByDepartment(department)); // 200
    }
}
```

### 19.3 What changed from Lab 01

1. **Every method returns `ResponseEntity<...>`** — no method relies on the framework default.
2. **`POST` returns `201` *and* a `Location` header** via `ResponseEntity.created(location)`. Lab 01 returned `201` but no `Location`. Test it: the response should carry a `Location: .../api/students/{id}` header.
3. **`DELETE` returns `204`** built with `ResponseEntity.noContent().build()` and a `ResponseEntity<Void>` type.
4. **`400`, `404`, `409`, `500` are not in the controller at all** — they come from validation (`@Valid`) and from exceptions thrown by the service, translated by the `GlobalExceptionHandler`. The controller stays thin and only owns the *success* codes; the handler owns the *error* codes.

### 19.4 Validation learning point

`@Valid` on the `@RequestBody` is what actually triggers validation. Without it, bad input would silently pass and you would never get a `400`. Confirm it is present on both `POST` and `PUT`.

## 20. Layer Responsibilities Summary

| Layer        | What belongs there                                                       | What should not belong there                          |
| ------------ | ------------------------------------------------------------------------ | ----------------------------------------------------- |
| Controller   | HTTP mapping, triggering validation, choosing **success** status codes, building `Location` | Database access, mapping logic, business rules        |
| Exc. Handler | Translating exceptions into **error** status codes (`400/404/409/500`)   | Business rules                                         |
| Service      | Business rules, DTO mapping, uniqueness checks, throwing domain exceptions | Raw HTTP concerns                                     |
| Repository   | Data access                                                              | Validation, HTTP responses                            |
| Entity       | Database mapping                                                         | Request-validation annotations                        |
| DTO          | API request/response structure + request validation rules                | Persistence logic                                     |

Key idea: **success codes live in the controller; error codes live in the exception handler.** Neither leaks into the service.

## 21. Suggested Testing Flow

Test with Postman, Insomnia, or `curl`. For this lab, **assert the status code of every response**, not just the body.

### 21.1 Status-code test matrix

| # | Scenario                                                       | Expect |
| - | -------------------------------------------------------------- | ------ |
| 1 | App starts and connects to PostgreSQL; `students` table created| —      |
| 2 | `POST` a valid student                                         | `201` + `Location` header |
| 3 | `GET` all students                                            | `200` (array)             |
| 4 | `GET` all students when table is empty                        | `200` with `[]` (not `404`) |
| 5 | `GET /api/students/{existing-id}`                             | `200`                     |
| 6 | `GET /api/students/9999` (valid number, no such student)      | `404`                     |
| 7 | `GET /api/students/abc` (non-numeric id)                      | `400`                     |
| 8 | `PUT` an existing student with valid data                     | `200` (updated body)      |
| 9 | `PUT /api/students/9999` (missing)                            | `404`                     |
| 10| `DELETE` an existing student                                  | `204` (no body)           |
| 11| `DELETE /api/students/9999` (missing)                        | `404`                     |
| 12| `GET /api/students/department/{dept}`                        | `200` (array, maybe `[]`) |
| 13| `POST` with invalid email                                    | `400` + field error       |
| 14| `POST` with blank required field                             | `400` + field error       |
| 15| `POST` with GPA `4.5` (out of range)                         | `400` + field error       |
| 16| `POST` with a future enrollment date                         | `400` + field error       |
| 17| `POST` a duplicate email                                      | `409`                     |
| 18| `POST` a duplicate student number                            | `409`                     |

### 21.2 What to look at

For each request, check **both** the status code line and the JSON body. The `400` and `409` bodies should be `ProblemDetail` JSON; the `400` validation body should include the per-field `errors` map.

## 22. Optional Stretch Tasks

If you finish early:

1. Add a `PATCH /api/students/{id}` for partial updates (return `200`, or `404` if missing). Discuss why `PATCH` differs from `PUT`.
2. Add pagination and sorting to `GET /api/students` (`Pageable`).
3. Add a `409`-safe handler for `DataIntegrityViolationException` as a backstop in case a duplicate slips past the service check (race condition).
4. Add `createdAt` / `updatedAt` timestamps.
5. Add a `GET /api/students/by-email?email=...` finder that returns `200` or `404`.
6. Write unit tests for the service and `@WebMvcTest` slice tests asserting status codes.

## 23. Final Deliverable Checklist

Before the lab is complete, you should be able to say yes to all of these:

1. The `_2_intermediate` project builds with Maven.
2. PostgreSQL connection works with the configured credentials.
3. The `students` table exists.
4. All required classes exist in the correct packages under `com.example._2_intermediate`.
5. DTO validation works and produces `400` with field errors.
6. CRUD endpoints work and each returns the correct success code (`200` / `201` / `204`).
7. `POST` returns a `Location` header.
8. Department filtering works and returns `200` (even when empty).
9. Missing students return `404`; non-numeric ids return `400`.
10. Duplicate email **and** duplicate student number each return `409`.
11. Every controller method returns an explicit `ResponseEntity`.
12. Unexpected errors are caught by the global handler (controlled `500`, not a leaked stack trace).
13. Controller, exception handler, service, repository, DTO, and entity responsibilities are kept separate.

## 24. Instructor Notes

Lab 01 taught the layered flow. Lab 02 teaches that **the status code is part of the API contract**. The student should leave this lab able to justify, for any endpoint, *why* it returns the code it returns, and able to keep success codes in the controller and error codes in the exception handler. Encourage testing the status line of every response, not just the body.
