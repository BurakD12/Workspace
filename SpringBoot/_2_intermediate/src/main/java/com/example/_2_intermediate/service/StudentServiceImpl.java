package com.example._2_intermediate.service;

import com.example._2_intermediate.dto.StudentRequestDto;
import com.example._2_intermediate.dto.StudentResponseDto;
import com.example._2_intermediate.exception.DuplicateResourceException;
import com.example._2_intermediate.exception.StudentNotFoundException;
import com.example._2_intermediate.model.Student;
import com.example._2_intermediate.repository.StudentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class StudentServiceImpl implements StudentService{
    private StudentRepository studentRepository;

    @Override
    public StudentResponseDto createStudent(StudentRequestDto studentRequestDto) {
        if (studentRepository.existsByEmailIgnoreCase(studentRequestDto.getEmail())) {
            throw new DuplicateResourceException("email", studentRequestDto.getEmail());
        }
        if (studentRepository.existsByStudentNumberIgnoreCase(studentRequestDto.getStudentNumber())) {
            throw new DuplicateResourceException("student number", studentRequestDto.getStudentNumber());
        }
        Student savedStudent = studentRepository.save(mapToEntity(studentRequestDto));
        return mapToResponseDto(savedStudent);
    }

    @Override
    public List<StudentResponseDto> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        List<StudentResponseDto> studentResponseDtos = new ArrayList<>();
        for (Student student : students) {
            studentResponseDtos.add(mapToResponseDto(student));
        }
        return studentResponseDtos;
    }

    @Override
    public StudentResponseDto getStudentById(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new StudentNotFoundException(id);
        }
        Student student = studentRepository.findById(id).orElseThrow();
        return mapToResponseDto(student);

    }

    @Override
    public StudentResponseDto updateStudent(Long id, StudentRequestDto studentRequestDto) {
        Student existing = getStudentEntityById(id);

        if (studentRepository.existsByEmailIgnoreCase(studentRequestDto.getEmail())) {
            throw new DuplicateResourceException("email", studentRequestDto.getEmail());
        }
        if (studentRepository.existsByStudentNumberIgnoreCase(studentRequestDto.getStudentNumber())) {
            throw new DuplicateResourceException("studentNumber", studentRequestDto.getStudentNumber());
        }
        existing.setFirstName(studentRequestDto.getFirstName());
        existing.setLastName(studentRequestDto.getFirstName());
        existing.setEmail(studentRequestDto.getEmail());
        existing.setStudentNumber(studentRequestDto.getStudentNumber());
        existing.setGpa(studentRequestDto.getGpa());
        existing.setDepartment(studentRequestDto.getDepartment());
        existing.setEnrollmentDate(studentRequestDto.getEnrollmentDate());

        studentRepository.save(existing);
        return mapToResponseDto(existing);
    }

    @Override
    public void deleteStudent(Long id) {
        Student existing = getStudentEntityById(id);

        studentRepository.delete(existing);
    }

    @Override
    public List<StudentResponseDto> getStudentsByDepartment(String department) {
        List<Student> students = studentRepository.findByDepartmentIgnoreCase(department);
        List<StudentResponseDto> studentResponseDtos = new ArrayList<>();
        for (Student student : students) {
            studentResponseDtos.add(mapToResponseDto(student));
        }
        return studentResponseDtos;
    }

    private Student mapToEntity(StudentRequestDto studentRequestDto){
        return Student.builder()
                .firstName(studentRequestDto.getFirstName())
                .lastName(studentRequestDto.getLastName())
                .email(studentRequestDto.getEmail())
                .studentNumber(studentRequestDto.getStudentNumber())
                .department(studentRequestDto.getDepartment())
                .gpa(studentRequestDto.getGpa())
                .enrollmentDate(studentRequestDto.getEnrollmentDate())
                .build();
    }

    private StudentResponseDto mapToResponseDto(Student student) {
        StudentResponseDto studentResponseDto = new StudentResponseDto();
        studentResponseDto.setId(student.getId());
        studentResponseDto.setFirstName(student.getFirstName());
        studentResponseDto.setLastName(student.getLastName());
        studentResponseDto.setEmail(student.getEmail());
        studentResponseDto.setStudentNumber(student.getStudentNumber());
        studentResponseDto.setDepartment(student.getDepartment());
        studentResponseDto.setGpa(student.getGpa());
        studentResponseDto.setEnrollmentDate(student.getEnrollmentDate());
        return studentResponseDto;
    }

    private Student getStudentEntityById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
    }

}
