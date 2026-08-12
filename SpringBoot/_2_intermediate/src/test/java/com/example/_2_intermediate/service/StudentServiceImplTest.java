package com.example._2_intermediate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example._2_intermediate.dto.StudentRequestDto;
import com.example._2_intermediate.dto.StudentResponseDto;
import com.example._2_intermediate.exception.DuplicateResourceException;
import com.example._2_intermediate.exception.StudentNotFoundException;
import com.example._2_intermediate.model.Student;
import com.example._2_intermediate.repository.StudentRepository;

/**
 * Pure unit tests for StudentServiceImpl.
 *
 * Every external collaborator (StudentRepository) is mocked with Mockito.
 * No Spring context is loaded, so these tests run extremely fast.
 *
 * Test organization:
 *   - One @Nested class per public service method.
 *   - Each @Nested class contains happy-path and all error-path scenarios.
 *   - Interactions with the repository are verified to confirm the right
 *     methods are (or are NOT) called.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentServiceImpl – Unit Tests")
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentServiceImpl studentService;

    // ─────────────────────────────────────────────────────────────────────────
    // Shared test-data helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns a fully populated valid request DTO (Alice Johnson). */
    private StudentRequestDto validRequestDto() {
        return new StudentRequestDto(
                "Alice",
                "Johnson",
                "alice.johnson@example.com",
                "S2024001",
                "Computer Science",
                new BigDecimal("3.75"),
                LocalDate.of(2024, 9, 1)
        );
    }

    /** Builds a Student entity using all fields from the supplied DTO plus the given id. */
    private Student entityFrom(Long id, StudentRequestDto dto) {
        return Student.builder()
                .id(id)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .studentNumber(dto.getStudentNumber())
                .department(dto.getDepartment())
                .gpa(dto.getGpa())
                .enrollmentDate(dto.getEnrollmentDate())
                .build();
    }

    /** Builds the canonical Alice entity with id=1. */
    private Student aliceEntity() {
        return Student.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson@example.com")
                .studentNumber("S2024001")
                .department("Computer Science")
                .gpa(new BigDecimal("3.75"))
                .enrollmentDate(LocalDate.of(2024, 9, 1))
                .build();
    }

    /** Builds a second student (Bob Smith) with id=2. */
    private Student bobEntity() {
        return Student.builder()
                .id(2L)
                .firstName("Bob")
                .lastName("Smith")
                .email("bob.smith@example.com")
                .studentNumber("S2024002")
                .department("Mathematics")
                .gpa(new BigDecimal("3.20"))
                .enrollmentDate(LocalDate.of(2024, 9, 1))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createStudent
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createStudent()")
    class CreateStudent {

        @Test
        @DisplayName("happy path: saves entity and returns fully mapped ResponseDto with all fields")
        void happyPath_savesAndReturnsMappedResponseDto() {
            StudentRequestDto dto = validRequestDto();
            Student saved = entityFrom(1L, dto);

            given(studentRepository.existsByEmailIgnoreCase(dto.getEmail())).willReturn(false);
            given(studentRepository.existsByStudentNumberIgnoreCase(dto.getStudentNumber())).willReturn(false);
            given(studentRepository.save(any(Student.class))).willReturn(saved);

            StudentResponseDto result = studentService.createStudent(dto);

            // All response fields must be mapped correctly
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFirstName()).isEqualTo("Alice");
            assertThat(result.getLastName()).isEqualTo("Johnson");
            assertThat(result.getEmail()).isEqualTo("alice.johnson@example.com");
            assertThat(result.getStudentNumber()).isEqualTo("S2024001");
            assertThat(result.getDepartment()).isEqualTo("Computer Science");
            assertThat(result.getGpa()).isEqualByComparingTo(new BigDecimal("3.75"));
            assertThat(result.getEnrollmentDate()).isEqualTo(LocalDate.of(2024, 9, 1));

            // Repository must be called in the correct order
            verify(studentRepository).existsByEmailIgnoreCase("alice.johnson@example.com");
            verify(studentRepository).existsByStudentNumberIgnoreCase("S2024001");
            verify(studentRepository).save(any(Student.class));
        }

        @Test
        @DisplayName("duplicate email: throws DuplicateResourceException mentioning 'email' and the value")
        void duplicateEmail_throwsDuplicateResourceExceptionWithEmailField() {
            StudentRequestDto dto = validRequestDto();
            given(studentRepository.existsByEmailIgnoreCase(dto.getEmail())).willReturn(true);

            assertThatThrownBy(() -> studentService.createStudent(dto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email")
                    .hasMessageContaining("alice.johnson@example.com");

            // Short-circuit: studentNumber check and save must never run
            verify(studentRepository, never()).existsByStudentNumberIgnoreCase(any());
            verify(studentRepository, never()).save(any());
        }

        @Test
        @DisplayName("duplicate studentNumber: throws DuplicateResourceException mentioning 'studentNumber' and the value")
        void duplicateStudentNumber_throwsDuplicateResourceExceptionWithStudentNumberField() {
            StudentRequestDto dto = validRequestDto();
            given(studentRepository.existsByEmailIgnoreCase(dto.getEmail())).willReturn(false);
            given(studentRepository.existsByStudentNumberIgnoreCase(dto.getStudentNumber())).willReturn(true);

            assertThatThrownBy(() -> studentService.createStudent(dto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("studentNumber")
                    .hasMessageContaining("S2024001");

            // Save must never run when studentNumber is taken
            verify(studentRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllStudents
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllStudents()")
    class GetAllStudents {

        @Test
        @DisplayName("empty table: returns an empty list (not null, not an exception)")
        void emptyTable_returnsEmptyList() {
            given(studentRepository.findAll()).willReturn(Collections.emptyList());

            List<StudentResponseDto> result = studentService.getAllStudents();

            assertThat(result).isNotNull().isEmpty();
            verify(studentRepository).findAll();
        }

        @Test
        @DisplayName("two students exist: returns list of two correctly mapped ResponseDtos")
        void twoStudentsExist_returnsMappedListOfTwo() {
            given(studentRepository.findAll()).willReturn(List.of(aliceEntity(), bobEntity()));

            List<StudentResponseDto> result = studentService.getAllStudents();

            assertThat(result).hasSize(2);

            StudentResponseDto alice = result.get(0);
            assertThat(alice.getId()).isEqualTo(1L);
            assertThat(alice.getFirstName()).isEqualTo("Alice");
            assertThat(alice.getEmail()).isEqualTo("alice.johnson@example.com");
            assertThat(alice.getDepartment()).isEqualTo("Computer Science");

            StudentResponseDto bob = result.get(1);
            assertThat(bob.getId()).isEqualTo(2L);
            assertThat(bob.getFirstName()).isEqualTo("Bob");
            assertThat(bob.getEmail()).isEqualTo("bob.smith@example.com");
            assertThat(bob.getDepartment()).isEqualTo("Mathematics");
        }

        @Test
        @DisplayName("single student: list contains exactly that student's data")
        void oneStudent_returnsListOfOne() {
            given(studentRepository.findAll()).willReturn(List.of(aliceEntity()));

            List<StudentResponseDto> result = studentService.getAllStudents();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo("alice.johnson@example.com");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getStudentById
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStudentById()")
    class GetStudentById {

        @Test
        @DisplayName("existing id: returns fully mapped ResponseDto")
        void existingId_returnsMappedResponseDto() {
            given(studentRepository.findById(1L)).willReturn(Optional.of(aliceEntity()));

            StudentResponseDto result = studentService.getStudentById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFirstName()).isEqualTo("Alice");
            assertThat(result.getLastName()).isEqualTo("Johnson");
            assertThat(result.getEmail()).isEqualTo("alice.johnson@example.com");
            assertThat(result.getStudentNumber()).isEqualTo("S2024001");
            assertThat(result.getDepartment()).isEqualTo("Computer Science");
            assertThat(result.getGpa()).isEqualByComparingTo(new BigDecimal("3.75"));
            assertThat(result.getEnrollmentDate()).isEqualTo(LocalDate.of(2024, 9, 1));

            verify(studentRepository).findById(1L);
        }

        @Test
        @DisplayName("unknown id: throws StudentNotFoundException with the id in the message")
        void unknownId_throwsStudentNotFoundException() {
            given(studentRepository.findById(9999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.getStudentById(9999L))
                    .isInstanceOf(StudentNotFoundException.class)
                    .hasMessageContaining("9999");
        }

        @Test
        @DisplayName("verifies that findById is called with the exact id provided")
        void delegatesToRepositoryWithCorrectId() {
            given(studentRepository.findById(42L)).willReturn(Optional.of(aliceEntity()));

            studentService.getStudentById(42L);

            verify(studentRepository).findById(42L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateStudent
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStudent()")
    class UpdateStudent {

        @Test
        @DisplayName("same email and same studentNumber: no duplicate checks triggered, entity saved and returned")
        void sameEmailAndNumber_savesWithoutDuplicateChecks() {
            Student existing = aliceEntity();
            StudentRequestDto dto = validRequestDto(); // email + studentNumber unchanged
            Student saved = entityFrom(1L, dto);

            given(studentRepository.findById(1L)).willReturn(Optional.of(existing));
            given(studentRepository.save(any(Student.class))).willReturn(saved);

            StudentResponseDto result = studentService.updateStudent(1L, dto);

            assertThat(result.getId()).isEqualTo(1L);
            // Duplicate checks must NOT be called when neither field changed
            verify(studentRepository, never()).existsByEmailIgnoreCase(any());
            verify(studentRepository, never()).existsByStudentNumberIgnoreCase(any());
            verify(studentRepository).save(any(Student.class));
        }

        @Test
        @DisplayName("new email that is available: duplicate check passes and entity is saved")
        void newEmailAvailable_checksAndSavesSuccessfully() {
            Student existing = aliceEntity();
            StudentRequestDto dto = validRequestDto();
            dto.setEmail("alice.new@example.com"); // email changed
            Student saved = entityFrom(1L, dto);

            given(studentRepository.findById(1L)).willReturn(Optional.of(existing));
            given(studentRepository.existsByEmailIgnoreCase("alice.new@example.com")).willReturn(false);
            given(studentRepository.save(any(Student.class))).willReturn(saved);

            StudentResponseDto result = studentService.updateStudent(1L, dto);

            assertThat(result.getEmail()).isEqualTo("alice.new@example.com");
            verify(studentRepository).existsByEmailIgnoreCase("alice.new@example.com");
        }

        @Test
        @DisplayName("new email already taken: throws DuplicateResourceException, save never called")
        void newEmailTaken_throwsDuplicateResourceException() {
            Student existing = aliceEntity();
            StudentRequestDto dto = validRequestDto();
            dto.setEmail("taken@example.com");

            given(studentRepository.findById(1L)).willReturn(Optional.of(existing));
            given(studentRepository.existsByEmailIgnoreCase("taken@example.com")).willReturn(true);

            assertThatThrownBy(() -> studentService.updateStudent(1L, dto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email")
                    .hasMessageContaining("taken@example.com");

            verify(studentRepository, never()).save(any());
        }

        @Test
        @DisplayName("new studentNumber that is available: duplicate check passes and entity is saved")
        void newStudentNumberAvailable_checksAndSavesSuccessfully() {
            Student existing = aliceEntity();
            StudentRequestDto dto = validRequestDto();
            dto.setStudentNumber("S2024099"); // changed
            Student saved = entityFrom(1L, dto);

            given(studentRepository.findById(1L)).willReturn(Optional.of(existing));
            given(studentRepository.existsByStudentNumberIgnoreCase("S2024099")).willReturn(false);
            given(studentRepository.save(any(Student.class))).willReturn(saved);

            StudentResponseDto result = studentService.updateStudent(1L, dto);

            assertThat(result.getStudentNumber()).isEqualTo("S2024099");
            verify(studentRepository).existsByStudentNumberIgnoreCase("S2024099");
        }

        @Test
        @DisplayName("new studentNumber already taken: throws DuplicateResourceException, save never called")
        void newStudentNumberTaken_throwsDuplicateResourceException() {
            Student existing = aliceEntity();
            StudentRequestDto dto = validRequestDto();
            dto.setStudentNumber("S2024999");

            given(studentRepository.findById(1L)).willReturn(Optional.of(existing));
            given(studentRepository.existsByStudentNumberIgnoreCase("S2024999")).willReturn(true);

            assertThatThrownBy(() -> studentService.updateStudent(1L, dto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("studentNumber")
                    .hasMessageContaining("S2024999");

            verify(studentRepository, never()).save(any());
        }

        @Test
        @DisplayName("id not found: throws StudentNotFoundException before any update or duplicate logic")
        void idNotFound_throwsStudentNotFoundExceptionFirst() {
            given(studentRepository.findById(9999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.updateStudent(9999L, validRequestDto()))
                    .isInstanceOf(StudentNotFoundException.class)
                    .hasMessageContaining("9999");

            // None of the other repository methods must fire
            verify(studentRepository, never()).existsByEmailIgnoreCase(any());
            verify(studentRepository, never()).existsByStudentNumberIgnoreCase(any());
            verify(studentRepository, never()).save(any());
        }

        @Test
        @DisplayName("all seven mutable fields are written onto the existing entity before save")
        void allFieldsUpdatedOnExistingEntity() {
            Student existing = aliceEntity();
            // Same email and number to avoid duplicate-check stubbing
            StudentRequestDto dto = new StudentRequestDto(
                    "UpdatedFirst",
                    "UpdatedLast",
                    "alice.johnson@example.com",   // unchanged
                    "S2024001",                     // unchanged
                    "Physics",
                    new BigDecimal("2.50"),
                    LocalDate.of(2023, 3, 15)
            );
            Student saved = entityFrom(1L, dto);

            given(studentRepository.findById(1L)).willReturn(Optional.of(existing));
            given(studentRepository.save(any(Student.class))).willReturn(saved);

            StudentResponseDto result = studentService.updateStudent(1L, dto);

            assertThat(result.getFirstName()).isEqualTo("UpdatedFirst");
            assertThat(result.getLastName()).isEqualTo("UpdatedLast");
            assertThat(result.getDepartment()).isEqualTo("Physics");
            assertThat(result.getGpa()).isEqualByComparingTo(new BigDecimal("2.50"));
            assertThat(result.getEnrollmentDate()).isEqualTo(LocalDate.of(2023, 3, 15));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteStudent
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteStudent()")
    class DeleteStudent {

        @Test
        @DisplayName("existing student: findById is called, then deleteById is called exactly once")
        void existingStudent_callsFindByIdThenDeleteById() {
            given(studentRepository.findById(1L)).willReturn(Optional.of(aliceEntity()));

            studentService.deleteStudent(1L);

            verify(studentRepository).findById(1L);
            verify(studentRepository).deleteById(1L);
        }

        @Test
        @DisplayName("unknown id: throws StudentNotFoundException and deleteById is never called")
        void unknownId_throwsAndDoesNotCallDeleteById() {
            given(studentRepository.findById(9999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.deleteStudent(9999L))
                    .isInstanceOf(StudentNotFoundException.class)
                    .hasMessageContaining("9999");

            verify(studentRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("method returns void (no exception means success)")
        void returnsVoidOnSuccess() {
            given(studentRepository.findById(1L)).willReturn(Optional.of(aliceEntity()));

            // assertDoesNotThrow equivalent via assertThatNoException
            org.assertj.core.api.Assertions.assertThatNoException()
                    .isThrownBy(() -> studentService.deleteStudent(1L));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getStudentsByDepartment
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStudentsByDepartment()")
    class GetStudentsByDepartment {

        @Test
        @DisplayName("department with two students: returns list of two correctly mapped ResponseDtos")
        void twoStudentsInDept_returnsMappedListOfTwo() {
            Student carol = Student.builder()
                    .id(3L).firstName("Carol").lastName("Davis")
                    .email("carol.davis@example.com").studentNumber("S2024003")
                    .department("Computer Science").gpa(new BigDecimal("3.50"))
                    .enrollmentDate(LocalDate.of(2024, 9, 1)).build();

            given(studentRepository.findByDepartmentIgnoreCase("Computer Science"))
                    .willReturn(List.of(aliceEntity(), carol));

            List<StudentResponseDto> result = studentService.getStudentsByDepartment("Computer Science");

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(StudentResponseDto::getDepartment)
                    .containsOnly("Computer Science");
            assertThat(result)
                    .extracting(StudentResponseDto::getEmail)
                    .containsExactlyInAnyOrder(
                            "alice.johnson@example.com",
                            "carol.davis@example.com");
        }

        @Test
        @DisplayName("department with no students: returns empty list (not an error, not null)")
        void noDepartmentMatch_returnsEmptyList() {
            given(studentRepository.findByDepartmentIgnoreCase("Biology"))
                    .willReturn(Collections.emptyList());

            List<StudentResponseDto> result = studentService.getStudentsByDepartment("Biology");

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("passes department string as-is to repository without modification")
        void delegatesToRepositoryWithExactDepartmentString() {
            given(studentRepository.findByDepartmentIgnoreCase("Computer Science"))
                    .willReturn(Collections.emptyList());

            studentService.getStudentsByDepartment("Computer Science");

            verify(studentRepository).findByDepartmentIgnoreCase("Computer Science");
        }
    }
}
