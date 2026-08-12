package com.example._2_intermediate.service;

import com.example._2_intermediate.dto.StudentRequestDto;
import com.example._2_intermediate.dto.StudentResponseDto;

import java.util.List;

public interface StudentService {
    StudentResponseDto createStudent(StudentRequestDto studentRequestDto);
    List<StudentResponseDto> getAllStudents();
    StudentResponseDto getStudentById(Long id);
    StudentResponseDto updateStudent(Long id, StudentRequestDto studentRequestDto);
    void deleteStudent(Long id);
    List<StudentResponseDto> getStudentsByDepartment(String department);
}
