package com.example._2_intermediate.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example._2_intermediate.dto.StudentRequestDto;
import com.example._2_intermediate.dto.StudentResponseDto;
import com.example._2_intermediate.exception.DuplicateResourceException;
import com.example._2_intermediate.exception.StudentNotFoundException;
import com.example._2_intermediate.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web-layer slice tests for StudentController.
 *
 * @WebMvcTest loads only the web layer (controller + @ControllerAdvice).
 *             StudentService is replaced with a Mockito mock so no database is
 *             needed.
 *             Every test asserts BOTH the HTTP status code AND the relevant
 *             JSON fields.
 *
 *             Coverage:
 *             POST /api/students → 201, 400 (all validation rules), 409 (email
 *             + number)
 *             GET /api/students → 200 (empty list and populated list)
 *             GET /api/students/{id} → 200, 404, 400 (non-numeric id)
 *             PUT /api/students/{id} → 200, 404, 409, 400 (validation)
 *             DELETE /api/students/{id} → 204, 404
 *             GET /api/students/department/{dept} → 200 (populated and empty)
 */
@WebMvcTest(StudentController.class)
@DisplayName("StudentController – Web MVC Slice Tests")
class StudentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private StudentService studentService;

        // ─────────────────────────────────────────────────────────────────────────
        // Shared test-data helpers
        // ─────────────────────────────────────────────────────────────────────────

        /** Returns a fully valid request DTO that passes all @Valid constraints. */
        private StudentRequestDto validRequest() {
                return new StudentRequestDto(
                                "Alice",
                                "Johnson",
                                "alice.johnson@example.com",
                                "S2024001",
                                "Computer Science",
                                new BigDecimal("3.75"),
                                LocalDate.of(2024, 9, 1));
        }

        /** Returns the matching response DTO as if the service returned it. */
        private StudentResponseDto sampleResponse() {
                return new StudentResponseDto(
                                1L,
                                "Alice",
                                "Johnson",
                                "alice.johnson@example.com",
                                "S2024001",
                                "Computer Science",
                                new BigDecimal("3.75"),
                                LocalDate.of(2024, 9, 1));
        }

        /** Serializes the DTO to a JSON byte array for use in request bodies. */
        private byte[] json(Object dto) throws Exception {
                return objectMapper.writeValueAsBytes(dto);
        }

        // ─────────────────────────────────────────────────────────────────────────
        // POST /api/students
        // ─────────────────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("POST /api/students")
        class PostStudents {

                @Test
                @DisplayName("201 Created: valid request → response body with all fields + Location header pointing to new id")
                void validRequest_returns201WithLocationHeaderAndBody() throws Exception {
                        given(studentService.createStudent(any(StudentRequestDto.class)))
                                        .willReturn(sampleResponse());

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(validRequest())))
                                        .andExpect(status().isCreated())
                                        .andExpect(header().string("Location", containsString("/api/students/1")))
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.firstName").value("Alice"))
                                        .andExpect(jsonPath("$.lastName").value("Johnson"))
                                        .andExpect(jsonPath("$.email").value("alice.johnson@example.com"))
                                        .andExpect(jsonPath("$.studentNumber").value("S2024001"))
                                        .andExpect(jsonPath("$.department").value("Computer Science"))
                                        .andExpect(jsonPath("$.enrollmentDate").value("2024-09-01"));
                }

                // ── Validation: 400 Bad Request ──────────────────────────────────────

                @Test
                @DisplayName("400: blank firstName → ProblemDetail with errors.firstName message")
                void blankFirstName_returns400WithFieldError() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setFirstName("");

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Validation failed"))
                                        .andExpect(jsonPath("$.detail").value("Request validation failed"))
                                        .andExpect(jsonPath("$.errors.firstName").value("First name is required"));
                }

                @Test
                @DisplayName("400: blank lastName → ProblemDetail with errors.lastName message")
                void blankLastName_returns400WithFieldError() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setLastName("");

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.lastName").value("Last name is required"));
                }

                @Test
                @DisplayName("400: blank email → ProblemDetail with errors.email 'required' message")
                void blankEmail_returns400WithFieldError() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setEmail("");

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.email").isNotEmpty());
                }

                @Test
                @DisplayName("400: malformed email (missing @) → ProblemDetail with errors.email format message")
                void malformedEmail_returns400WithEmailFieldError() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setEmail("not-an-email");

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.email")
                                                        .value("Email must be a valid email address"));
                }

                @Test
                @DisplayName("400: studentNumber not matching S + 7 digits → ProblemDetail with pattern error")
                void invalidStudentNumberPattern_returns400WithPatternError() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setStudentNumber("INVALID123");

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.studentNumber")
                                                        .value("Student number must look like S2024001"));
                }

                @Test
                @DisplayName("400: studentNumber with too few digits (S12345) → pattern error")
                void studentNumberTooShort_returns400() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setStudentNumber("S12345"); // only 5 digits, pattern requires 7

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.studentNumber").isNotEmpty());
                }

                @Test
                @DisplayName("400: GPA = 4.5 (above maximum 4.0) → errors.gpa DecimalMax message")
                void gpaTooHigh_returns400WithGpaFieldError() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setGpa(new BigDecimal("4.5"));

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.gpa").value("GPA cannot exceed 4.0"));
                }

                @Test
                @DisplayName("400: GPA = -0.5 (below minimum 0.0) → errors.gpa DecimalMin message")
                void gpaTooLow_returns400WithGpaFieldError() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setGpa(new BigDecimal("-0.5"));

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.gpa").value("GPA cannot be below 0.0"));
                }

                @Test
                @DisplayName("400: null GPA → errors.gpa NotNull message")
                void nullGpa_returns400WithGpaFieldError() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setGpa(null);

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.gpa").value("GPA is required"));
                }

                @Test
                @DisplayName("400: enrollmentDate one year in the future → errors.enrollmentDate PastOrPresent message")
                void futureEnrollmentDate_returns400WithDateFieldError() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setEnrollmentDate(LocalDate.now().plusYears(1));

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.enrollmentDate")
                                                        .value("Enrollment date cannot be in the future"));
                }

                @Test
                @DisplayName("400: null enrollmentDate → errors.enrollmentDate NotNull message")
                void nullEnrollmentDate_returns400WithDateFieldError() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setEnrollmentDate(null);

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.enrollmentDate")
                                                        .value("Enrollment date is required"));
                }

                @Test
                @DisplayName("400: multiple fields invalid → errors map contains all violating fields at once")
                void multipleInvalidFields_returns400WithAllErrors() throws Exception {
                        StudentRequestDto req = validRequest();
                        req.setFirstName("");
                        req.setEmail("bad-email");
                        req.setGpa(new BigDecimal("9.9"));

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(req)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors.firstName").isNotEmpty())
                                        .andExpect(jsonPath("$.errors.email").isNotEmpty())
                                        .andExpect(jsonPath("$.errors.gpa").isNotEmpty());
                }

                // ── Conflict: 409 ────────────────────────────────────────────────────

                @Test
                @DisplayName("409 Conflict: duplicate email → ProblemDetail 'Duplicate value' title, detail contains 'email'")
                void duplicateEmail_returns409WithConflictTitle() throws Exception {
                        given(studentService.createStudent(any(StudentRequestDto.class)))
                                        .willThrow(new DuplicateResourceException("email",
                                                        "alice.johnson@example.com"));

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(validRequest())))
                                        .andExpect(status().isConflict())
                                        .andExpect(jsonPath("$.status").value(409))
                                        .andExpect(jsonPath("$.title").value("Duplicate value"))
                                        .andExpect(jsonPath("$.detail").value(containsString("email")))
                                        .andExpect(jsonPath("$.detail")
                                                        .value(containsString("alice.johnson@example.com")));
                }

                @Test
                @DisplayName("409 Conflict: duplicate studentNumber → ProblemDetail 'Duplicate value' title, detail contains 'studentNumber'")
                void duplicateStudentNumber_returns409WithConflictTitle() throws Exception {
                        given(studentService.createStudent(any(StudentRequestDto.class)))
                                        .willThrow(new DuplicateResourceException("studentNumber", "S2024001"));

                        mockMvc.perform(post("/api/students")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(validRequest())))
                                        .andExpect(status().isConflict())
                                        .andExpect(jsonPath("$.status").value(409))
                                        .andExpect(jsonPath("$.title").value("Duplicate value"))
                                        .andExpect(jsonPath("$.detail").value(containsString("studentNumber")));
                }
        }

        // ─────────────────────────────────────────────────────────────────────────
        // GET /api/students
        // ─────────────────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("GET /api/students")
        class GetAllStudents {

                @Test
                @DisplayName("200 OK: empty table → response is [] (not 404)")
                void emptyTable_returns200WithEmptyJsonArray() throws Exception {
                        given(studentService.getAllStudents()).willReturn(Collections.emptyList());

                        mockMvc.perform(get("/api/students"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$").isArray())
                                        .andExpect(jsonPath("$", hasSize(0)));
                }

                @Test
                @DisplayName("200 OK: two students exist → array of two with correct ids and emails")
                void twoStudents_returns200WithPopulatedArray() throws Exception {
                        StudentResponseDto bob = new StudentResponseDto(
                                        2L, "Bob", "Smith", "bob.smith@example.com",
                                        "S2024002", "Mathematics", new BigDecimal("3.20"), LocalDate.of(2024, 9, 1));

                        given(studentService.getAllStudents()).willReturn(List.of(sampleResponse(), bob));

                        mockMvc.perform(get("/api/students"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(2)))
                                        .andExpect(jsonPath("$[0].id").value(1))
                                        .andExpect(jsonPath("$[0].email").value("alice.johnson@example.com"))
                                        .andExpect(jsonPath("$[1].id").value(2))
                                        .andExpect(jsonPath("$[1].email").value("bob.smith@example.com"));
                }
        }

        // ─────────────────────────────────────────────────────────────────────────
        // GET /api/students/{id}
        // ─────────────────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("GET /api/students/{id}")
        class GetStudentById {

                @Test
                @DisplayName("200 OK: existing id → full student body with all fields")
                void existingId_returns200WithFullBody() throws Exception {
                        given(studentService.getStudentById(1L)).willReturn(sampleResponse());

                        mockMvc.perform(get("/api/students/1"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.firstName").value("Alice"))
                                        .andExpect(jsonPath("$.lastName").value("Johnson"))
                                        .andExpect(jsonPath("$.email").value("alice.johnson@example.com"))
                                        .andExpect(jsonPath("$.studentNumber").value("S2024001"))
                                        .andExpect(jsonPath("$.department").value("Computer Science"))
                                        .andExpect(jsonPath("$.enrollmentDate").value("2024-09-01"));
                }

                @Test
                @DisplayName("404 Not Found: valid number but no such student → ProblemDetail with 'Student not found' title")
                void unknownId_returns404WithProblemDetail() throws Exception {
                        given(studentService.getStudentById(9999L))
                                        .willThrow(new StudentNotFoundException(9999L));

                        mockMvc.perform(get("/api/students/9999"))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.status").value(404))
                                        .andExpect(jsonPath("$.title").value("Student not found"))
                                        .andExpect(jsonPath("$.detail").value(containsString("9999")));
                }

                @Test
                @DisplayName("400 Bad Request: non-numeric id 'abc' → ProblemDetail 'Invalid parameter' (not 404)")
                void nonNumericId_returns400WithInvalidParameterTitle() throws Exception {
                        mockMvc.perform(get("/api/students/abc"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid parameter"))
                                        .andExpect(jsonPath("$.detail").value(containsString("id")));
                }

                @Test
                @DisplayName("400 Bad Request: non-numeric id 'xyz123' → same 400 behavior")
                void alphanumericId_returns400() throws Exception {
                        mockMvc.perform(get("/api/students/xyz123"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.status").value(400));
                }
        }

        // ─────────────────────────────────────────────────────────────────────────
        // PUT /api/students/{id}
        // ─────────────────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("PUT /api/students/{id}")
        class UpdateStudent {

                @Test
                @DisplayName("200 OK: valid id and body → updated response body with new field values")
                void validRequest_returns200WithUpdatedBody() throws Exception {
                        StudentResponseDto updated = new StudentResponseDto(
                                        1L, "Alice", "Johnson", "alice.johnson@example.com",
                                        "S2024001", "Physics", new BigDecimal("3.90"), LocalDate.of(2024, 9, 1));

                        given(studentService.updateStudent(eq(1L), any(StudentRequestDto.class)))
                                        .willReturn(updated);

                        mockMvc.perform(put("/api/students/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(validRequest())))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.department").value("Physics"));
                }

                @Test
                @DisplayName("404 Not Found: id does not exist → ProblemDetail 404 from service exception")
                void unknownId_returns404() throws Exception {
                        given(studentService.updateStudent(eq(9999L), any(StudentRequestDto.class)))
                                        .willThrow(new StudentNotFoundException(9999L));

                        mockMvc.perform(put("/api/students/9999")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(validRequest())))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.status").value(404))
                                        .andExpect(jsonPath("$.title").value("Student not found"));
                }

                @Test
                @DisplayName("409 Conflict: updated email already taken → ProblemDetail 409")
                void duplicateEmailOnUpdate_returns409() throws Exception {
                        given(studentService.updateStudent(eq(1L), any(StudentRequestDto.class)))
                                        .willThrow(new DuplicateResourceException("email", "taken@example.com"));

                        mockMvc.perform(put("/api/students/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(validRequest())))
                                        .andExpect(status().isConflict())
                                        .andExpect(jsonPath("$.status").value(409))
                                        .andExpect(jsonPath("$.title").value("Duplicate value"));
                }

                @Test
                @DisplayName("409 Conflict: updated studentNumber already taken → ProblemDetail 409")
                void duplicateStudentNumberOnUpdate_returns409() throws Exception {
                        given(studentService.updateStudent(eq(1L), any(StudentRequestDto.class)))
                                        .willThrow(new DuplicateResourceException("studentNumber", "S2024999"));

                        mockMvc.perform(put("/api/students/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(validRequest())))
                                        .andExpect(status().isConflict())
                                        .andExpect(jsonPath("$.status").value(409));
                }

                @Test
                @DisplayName("400 Bad Request: PUT body fails @Valid (blank firstName + GPA > 4) → both errors in map")
                void invalidBody_returns400WithMultipleFieldErrors() throws Exception {
                        StudentRequestDto bad = validRequest();
                        bad.setFirstName("");
                        bad.setGpa(new BigDecimal("5.0"));

                        mockMvc.perform(put("/api/students/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(bad)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.errors.firstName").value("First name is required"))
                                        .andExpect(jsonPath("$.errors.gpa").value("GPA cannot exceed 4.0"));
                }
        }

        // ─────────────────────────────────────────────────────────────────────────
        // DELETE /api/students/{id}
        // ─────────────────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("DELETE /api/students/{id}")
        class DeleteStudent {

                @Test
                @DisplayName("204 No Content: existing student → empty response body (not 200, not a JSON body)")
                void existingId_returns204WithEmptyBody() throws Exception {
                        doNothing().when(studentService).deleteStudent(1L);

                        mockMvc.perform(delete("/api/students/1"))
                                        .andExpect(status().isNoContent())
                                        .andExpect(content().string(""));
                }

                @Test
                @DisplayName("404 Not Found: unknown id → ProblemDetail with 'Student not found' title and id in detail")
                void unknownId_returns404WithProblemDetail() throws Exception {
                        doThrow(new StudentNotFoundException(9999L)).when(studentService).deleteStudent(9999L);

                        mockMvc.perform(delete("/api/students/9999"))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.status").value(404))
                                        .andExpect(jsonPath("$.title").value("Student not found"))
                                        .andExpect(jsonPath("$.detail").value(containsString("9999")));
                }
        }

        // ─────────────────────────────────────────────────────────────────────────
        // GET /api/students/department/{department}
        // ─────────────────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("GET /api/students/department/{department}")
        class GetStudentsByDepartment {

                @Test
                @DisplayName("200 OK: department has students → array with matching entries")
                void departmentHasStudents_returns200WithArray() throws Exception {
                        given(studentService.getStudentsByDepartment("Computer Science"))
                                        .willReturn(List.of(sampleResponse()));

                        mockMvc.perform(get("/api/students/department/{dept}", "Computer Science"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(1)))
                                        .andExpect(jsonPath("$[0].department").value("Computer Science"));
                }

                @Test
                @DisplayName("200 OK: department has no students → [] (not 404 — an empty collection is valid)")
                void departmentEmpty_returns200WithEmptyArray() throws Exception {
                        given(studentService.getStudentsByDepartment("Biology"))
                                        .willReturn(Collections.emptyList());

                        mockMvc.perform(get("/api/students/department/{dept}", "Biology"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$").isArray())
                                        .andExpect(jsonPath("$", hasSize(0)));
                }
        }
}
