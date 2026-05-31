# Lab 01 - Employee Management System API

## 1. Lab Goal

Build your first layered Spring Boot backend project by creating an Employee Management System REST API.

By the end of this lab, the student should have a working backend that:

1. Connects to a local PostgreSQL database.
2. Stores employee data in a table.
3. Exposes REST endpoints for create, read, update, and delete operations.
4. Uses a clean layered architecture with controller, dto, model, repository, and service packages.
5. Validates incoming request data.

This lab document is intentionally written as implementation guidance only. It explains what to build, in what order to build it, and what each class or method is responsible for. It does not provide finished code.

## 2. Starting Project Context

Your current project already contains these packages under the base package `com.example._1_basics`:

1. `controller`
2. `dto`
3. `model`
4. `respository`
5. `service`

Important note:

The folder name `respository` is misspelled. Before starting the implementation, decide on one of these options:

1. Preferred option: rename it to `repository` so your package naming is correct.
2. Temporary option: keep `respository` and use the same spelling consistently everywhere.

For learning purposes, the preferred option is to rename it now so the rest of the project uses correct terminology.

## 3. Business Scenario

You are building a small backend for a company HR team.

The system must manage employees with information such as:

1. Employee ID
2. First name
3. Last name
4. Email
5. Department
6. Salary
7. Hire date

Each employee record must be stored in PostgreSQL and must be available through REST endpoints.

## 4. Suggested Build Order

The student should implement the project in this exact order:

1. Update project dependencies in `pom.xml`.
2. Configure the PostgreSQL connection in `application.properties`.
3. Create the database manually in PostgreSQL.
4. Implement the `Employee` entity in the `model` package.
5. Implement request and response DTO classes in the `dto` package.
6. Implement the repository interface.
7. Implement the service interface.
8. Implement the service implementation class.
9. Implement the REST controller.
10. Run the application and manually test all endpoints.

Do not skip layers. The goal of this lab is to understand how each layer depends on the previous one.

## 5. Dependencies To Add In `pom.xml`

The current `pom.xml` is very minimal. Expand it so the project can support REST APIs, JPA, validation, and PostgreSQL.

### 5.1 Required Dependencies

The student should add the following dependencies and understand why each one is needed:

| Dependency                       | Why it is needed                                                                               |
| -------------------------------- | ---------------------------------------------------------------------------------------------- |
| `spring-boot-starter-web`        | Enables REST controllers, JSON request/response handling, and embedded server support.         |
| `spring-boot-starter-data-jpa`   | Enables JPA, Hibernate, repository support, and entity persistence.                            |
| `postgresql`                     | Provides the PostgreSQL JDBC driver so the application can connect to the database.            |
| `spring-boot-starter-validation` | Enables bean validation annotations such as not blank, email validation, and positive numbers. |
| `lombok`                         | Optional but recommended to reduce boilerplate for constructors, getters, and setters.         |
| `spring-boot-starter-test`       | Keep this dependency for later testing work.                                                   |

### 5.2 Dependency Notes

1. If `spring-boot-starter-web` is added, the generic `spring-boot-starter` dependency usually becomes unnecessary because the web starter already brings core Spring Boot functionality.
2. If Lombok is used, the student must also make sure annotation processing is enabled in the IDE.
3. This first lab does not require Swagger or security dependencies yet. Keep the dependency list focused.

## 6. Database Setup

The student will use a local PostgreSQL server.

### 6.1 Database Credentials

Use these credentials in this lab:

1. Host: `localhost`
2. Port: `5432` unless your PostgreSQL installation uses a different local port
3. Username: `postgres`
4. Password: `123456`

### 6.2 Database Creation Task

Before running the application, create a database specifically for this lab. A clear name such as `employee_management_lab` is recommended.

The student should understand that Spring Boot can create tables, but it cannot connect unless the target database already exists.

## 7. Configuration In `application.properties`

The file `src/main/resources/application.properties` currently only contains the application name. Expand it with the database and JPA configuration needed for this project.

### 7.1 Properties The Student Must Add

The student should add and understand these properties:

| Property                                     | What it does                                                                      |
| -------------------------------------------- | --------------------------------------------------------------------------------- |
| `spring.application.name`                    | Names the application. Keep the current value unless you intentionally rename it. |
| `spring.datasource.url`                      | Points Spring Boot to the PostgreSQL database on localhost.                       |
| `spring.datasource.username`                 | Supplies the database username.                                                   |
| `spring.datasource.password`                 | Supplies the database password.                                                   |
| `spring.jpa.hibernate.ddl-auto`              | Controls whether Hibernate creates or updates tables automatically.               |
| `spring.jpa.show-sql`                        | Prints generated SQL to the console for learning and debugging.                   |
| `spring.jpa.properties.hibernate.format_sql` | Makes printed SQL easier to read.                                                 |

### 7.2 Configuration Guidance

1. For a first lab, `update` is a practical choice for `ddl-auto` because it allows Hibernate to create or update the employee table automatically.
2. SQL logging should be turned on while learning, so the student can see what JPA is doing behind the scenes.
3. A dialect property is usually optional because Hibernate can often detect PostgreSQL automatically.

## 8. Package And Class Plan

The finished project should contain at least these classes or interfaces:

| Package                       | Class or Interface    | Responsibility                                                 |
| ----------------------------- | --------------------- | -------------------------------------------------------------- |
| base package                  | `Application`         | Starts the Spring Boot application.                            |
| `model`                       | `Employee`            | Represents a database entity mapped to an employees table.     |
| `dto`                         | `EmployeeRequestDto`  | Carries incoming employee data from the client to the backend. |
| `dto`                         | `EmployeeResponseDto` | Carries employee data from the backend back to the client.     |
| `repository` or `respository` | `EmployeeRepository`  | Handles database access through Spring Data JPA.               |
| `service`                     | `EmployeeService`     | Defines the business operations for employee management.       |
| `service`                     | `EmployeeServiceImpl` | Implements the business operations using the repository.       |
| `controller`                  | `EmployeeController`  | Exposes REST endpoints for the employee API.                   |

No extra configuration class is required for this first lab.

## 9. Class 1 - `Application`

This class already exists.

### 9.1 What It Does

1. Marks the project as a Spring Boot application.
2. Starts the Spring context.
3. Boots the embedded web server.

### 9.2 Student Action

No major changes are required here for the first lab unless the package structure changes.

## 10. Class 2 - `Employee` Entity

Create the `Employee` class inside the `model` package.

### 10.1 Purpose

This class represents one employee row in the database.

### 10.2 Suggested Fields

The student should define fields such as:

1. `id` with a numeric type suitable for generated primary keys
2. `firstName`
3. `lastName`
4. `email`
5. `department`
6. `salary`
7. `hireDate`

### 10.3 Entity Design Responsibilities

The entity should:

1. Be mapped to a database table named something like `employees`.
2. Mark `id` as the primary key.
3. Configure the ID to be generated automatically.
4. Enforce uniqueness for `email` at the database level.
5. Use field types that match the meaning of the data, for example a decimal type for salary and a date type for hire date.

### 10.4 Constructors

The student should provide:

1. A no-argument constructor because JPA needs it.
2. A constructor that can initialize the business fields when creating a new employee object in application code.

If Lombok is used, the student should understand which Lombok annotations generate these constructors and why they are safe here.

### 10.5 Methods

The class should expose access to its fields through either:

1. Manually written getters and setters, or
2. Lombok-generated accessors

No business logic is required inside the entity for this first lab. Keep it focused on persistence mapping.

## 11. Class 3 - `EmployeeRequestDto`

Create this DTO inside the `dto` package.

### 11.1 Purpose

This class represents incoming JSON data for create and update requests.

### 11.2 Why A DTO Is Needed

The student should not expose the entity directly in the controller layer because:

1. API input and database structure should not be tightly coupled.
2. Validation belongs naturally on request objects.
3. DTOs make future changes safer.

### 11.3 Suggested Fields

Include the business fields that the client is allowed to send:

1. `firstName`
2. `lastName`
3. `email`
4. `department`
5. `salary`
6. `hireDate`

Do not include the generated database ID in the request DTO.

### 11.4 Validation Responsibilities

The student should apply validation rules such as:

1. Names must not be blank.
2. Email must be in a valid email format.
3. Department must not be blank.
4. Salary must be positive.
5. Hire date should not be in the future.

These rules are one of the main reasons the validation dependency is needed.

### 11.5 Constructors And Methods

The class should include:

1. A no-argument constructor if a normal Java class is used.
2. A full constructor if the student wants easier manual object creation in tests.
3. Getters and setters or Lombok-generated equivalents.

## 12. Class 4 - `EmployeeResponseDto`

Create this DTO inside the `dto` package.

### 12.1 Purpose

This class represents the JSON returned to the client.

### 12.2 Suggested Fields

This DTO should usually contain:

1. `id`
2. `firstName`
3. `lastName`
4. `email`
5. `department`
6. `salary`
7. `hireDate`

### 12.3 Why It Is Separate From The Request DTO

The response DTO exists so the API can return fields that the client should see, without forcing the create or update request format to be identical.

### 12.4 Constructors And Methods

The class should include:

1. A no-argument constructor if regular Java objects are used.
2. A full constructor that can represent a complete employee response.
3. Getters and setters or Lombok-generated equivalents.

## 13. Class 5 - `EmployeeRepository`

Create an interface named `EmployeeRepository` inside the repository package.

### 13.1 Purpose

This interface is the data access layer.

### 13.2 Main Responsibility

The student should make this interface extend the appropriate Spring Data JPA base repository so that CRUD operations are generated automatically.

### 13.3 Repository Methods To Declare

In addition to inherited CRUD operations, declare methods such as:

1. A method that finds an employee by email
2. A method that checks whether an email already exists, or alternatively a finder that can be used for duplicate checks
3. A method that returns all employees in a given department

The purpose of these methods is to teach derived query methods and to support business rules in the service layer.

### 13.4 Student Learning Point

No implementation class should be written manually. Spring Data JPA generates the repository behavior automatically.

## 14. Class 6 - `EmployeeService` Interface

Create an interface named `EmployeeService` inside the `service` package.

### 14.1 Purpose

This interface defines the business operations of the application.

### 14.2 Methods To Declare

The student should declare methods such as:

1. A method to create a new employee from a request DTO and return a response DTO
2. A method to return all employees
3. A method to return one employee by ID
4. A method to update an existing employee by ID
5. A method to delete an employee by ID
6. A method to return employees filtered by department

### 14.3 Return Types

This is the point where the student should decide that the service layer works with DTOs at its public boundary, not with raw entities.

That decision helps keep the controller thin and keeps entity mapping inside the service layer.

## 15. Class 7 - `EmployeeServiceImpl`

Create a class named `EmployeeServiceImpl` inside the `service` package.

### 15.1 Purpose

This class contains the actual business logic.

### 15.2 Class-Level Responsibilities

The class should:

1. Be marked as a Spring service.
2. Inject the repository through a constructor.
3. Implement the `EmployeeService` interface.
4. Convert between entity objects and DTO objects.
5. Enforce business rules such as unique email usage.

If Lombok is used, this is a good place to use a generated required-arguments constructor instead of writing the constructor manually.

### 15.3 Public Methods To Implement

#### `createEmployee(...)`

This method should:

1. Receive the request DTO.
2. Check whether another employee already uses the same email.
3. Convert the request DTO into an `Employee` entity.
4. Save the entity through the repository.
5. Convert the saved entity into a response DTO.
6. Return the response DTO.

#### `getAllEmployees()`

This method should:

1. Read all employee records from the repository.
2. Convert each entity into a response DTO.
3. Return a list of response DTOs.

#### `getEmployeeById(...)`

This method should:

1. Search by ID.
2. If the employee does not exist, signal a not-found situation.
3. Convert the found entity into a response DTO.

#### `updateEmployee(...)`

This method should:

1. Load the existing employee by ID.
2. Decide how to handle email uniqueness if the email is changed.
3. Copy updated fields from the request DTO into the existing entity.
4. Save the modified entity.
5. Return the updated response DTO.

#### `deleteEmployee(...)`

This method should:

1. Confirm the employee exists.
2. Delete the record.
3. Return no body, or return a simple success message if the controller design chooses to do so.

#### `getEmployeesByDepartment(...)`

This method should:

1. Query employees by department.
2. Convert the results into response DTOs.
3. Return the converted list.

### 15.4 Private Helper Methods

The student should add helper methods so the public methods remain readable. Suggested helper responsibilities:

1. A method that maps `EmployeeRequestDto` to `Employee`
2. A method that maps `Employee` to `EmployeeResponseDto`
3. A method that loads an employee by ID and handles the not-found case consistently

This is an important clean-code step. Mapping logic should not be duplicated in every service method.

### 15.5 Error Handling Expectation

For this first lab, the student must make sure that requesting a missing employee does not result in a generic server error.

At minimum, the application should return a clear not-found response. This may be handled with a framework exception, a custom exception, or a simple approach chosen by the student.

## 16. Class 8 - `EmployeeController`

Create a class named `EmployeeController` inside the `controller` package.

### 16.1 Purpose

This class exposes the REST API.

### 16.2 Class-Level Responsibilities

The controller should:

1. Be marked as a REST controller.
2. Use a base request mapping such as `/api/employees`.
3. Inject the service layer through a constructor.
4. Delegate business work to the service instead of implementing logic directly.

### 16.3 Endpoints To Implement

| HTTP Method | Path                                     | Purpose                        |
| ----------- | ---------------------------------------- | ------------------------------ |
| `POST`      | `/api/employees`                         | Create a new employee          |
| `GET`       | `/api/employees`                         | Return all employees           |
| `GET`       | `/api/employees/{id}`                    | Return one employee by ID      |
| `PUT`       | `/api/employees/{id}`                    | Update an employee             |
| `DELETE`    | `/api/employees/{id}`                    | Delete an employee             |
| `GET`       | `/api/employees/department/{department}` | Return employees by department |

### 16.4 Controller Methods And Responsibilities

#### Create Endpoint Method

The method should:

1. Accept a request body using the request DTO.
2. Trigger validation on the request body.
3. Call the service create method.
4. Return an appropriate success status for resource creation.

#### Get All Endpoint Method

The method should:

1. Call the service method that returns all employees.
2. Return a list of response DTOs.

#### Get By ID Endpoint Method

The method should:

1. Accept an ID from the URL.
2. Call the service method for a single employee.
3. Return the response DTO.

#### Update Endpoint Method

The method should:

1. Accept the ID from the URL.
2. Accept a validated request DTO from the request body.
3. Call the service update method.
4. Return the updated response DTO.

#### Delete Endpoint Method

The method should:

1. Accept the ID from the URL.
2. Call the service delete method.
3. Return an appropriate success status, commonly one with no response body.

#### Get By Department Endpoint Method

The method should:

1. Accept the department from the URL.
2. Call the service filter method.
3. Return a list of matching employees.

### 16.5 Validation Learning Point

The student should make sure validation actually runs in the controller layer by using the correct annotation on request-body parameters.

## 17. Layer Responsibilities Summary

The student should keep responsibilities separated like this:

| Layer      | What belongs there                                                  | What should not belong there                                  |
| ---------- | ------------------------------------------------------------------- | ------------------------------------------------------------- |
| Controller | HTTP handling, request mapping, validation triggering, status codes | Database access, entity mapping logic, complex business rules |
| Service    | Business rules, DTO mapping, orchestration, duplicate checks        | Raw HTTP concerns                                             |
| Repository | Data access                                                         | Request validation, HTTP responses                            |
| Entity     | Database mapping                                                    | Request-specific validation rules for API input               |
| DTO        | API request and response structure                                  | Persistence logic                                             |

This separation is one of the main goals of the lab.

## 18. Suggested Testing Flow

After implementation, the student should manually test the API with Postman, Insomnia, or another HTTP client.

### 18.1 Minimum Test Scenarios

1. Application starts successfully and connects to PostgreSQL.
2. The employee table is created automatically.
3. A valid employee can be created.
4. All employees can be listed.
5. One employee can be retrieved by ID.
6. An employee can be updated.
7. An employee can be deleted.
8. Employees can be filtered by department.
9. Invalid email input is rejected.
10. Blank required fields are rejected.
11. Negative salary input is rejected.
12. A non-existing employee request returns a not-found response.
13. Duplicate email creation is rejected.

## 19. Optional Stretch Tasks

If the student finishes early, these are good follow-up tasks:

1. Add a global exception handler.
2. Add pagination to the list endpoint.
3. Add sorting support.
4. Add created-at and updated-at timestamps.
5. Add unit tests for the service layer.
6. Add integration tests for the controller layer.

## 20. Final Deliverable Checklist

Before the lab is considered complete, the student should be able to say yes to all of these:

1. The project builds successfully with Maven.
2. PostgreSQL connection works with the configured local credentials.
3. The employee table exists.
4. All required classes have been created in the correct packages.
5. DTO validation works.
6. CRUD endpoints work correctly.
7. Department filtering works.
8. Duplicate email handling is implemented.
9. Missing employee requests are handled cleanly.
10. The controller, service, repository, DTO, and entity responsibilities are kept separate.

## 21. Instructor Notes

This lab is designed to teach the first full Spring Boot backend flow from database to REST API.

The student should be encouraged to implement each layer manually and understand why it exists before moving on to the next lab.
