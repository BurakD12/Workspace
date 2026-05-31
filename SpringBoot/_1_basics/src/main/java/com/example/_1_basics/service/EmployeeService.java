package com.example._1_basics.service;

import java.util.List;

import com.example._1_basics.dto.EmployeeRequestDto;
import com.example._1_basics.dto.EmployeeResponseDto;

public interface EmployeeService {
    EmployeeResponseDto createEmployee(EmployeeRequestDto employeeRequestDto);
    List<EmployeeResponseDto> getAllEmployees();
    EmployeeResponseDto getEmployeeById(Long id);
    EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto employeeRequestDto);
    void deleteEmployee(Long id);
    List<EmployeeResponseDto> getEmployeesByDepartment(String department);
}
