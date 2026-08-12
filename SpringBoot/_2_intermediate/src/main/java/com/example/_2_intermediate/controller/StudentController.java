package com.example._2_intermediate.controller;

import com.example._2_intermediate.dto.StudentRequestDto;
import com.example._2_intermediate.dto.StudentResponseDto;
import com.example._2_intermediate.service.StudentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping()
    public ResponseEntity<StudentResponseDto> createStudent(@Valid @RequestBody StudentRequestDto studentRequestDto) {
        StudentResponseDto createdStudent = studentService.createStudent(studentRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStudent);
    }

    @GetMapping()
    public ResponseEntity<List<StudentResponseDto>> getAllStudents() {
        List<StudentResponseDto> allStudents = studentService.getAllStudents();
        return ResponseEntity.ok(allStudents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getStudentById(@PathVariable Long id) {
        StudentResponseDto targetStudent = studentService.getStudentById(id);
        return ResponseEntity.ok(targetStudent);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDto> updateStudentById(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDto studentRequestDto) {

        StudentResponseDto updatedStudent = studentService.updateStudent(id, studentRequestDto);
        return ResponseEntity.ok(updatedStudent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudentById(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<StudentResponseDto>> getStudentsByDepartment(@PathVariable String department) {
        List<StudentResponseDto> students = studentService.getStudentsByDepartment(department);
        return ResponseEntity.ok(students);
    }
}
