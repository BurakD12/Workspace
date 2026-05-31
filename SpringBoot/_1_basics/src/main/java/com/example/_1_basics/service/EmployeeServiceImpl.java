package com.example._1_basics.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example._1_basics.dto.EmployeeRequestDto;
import com.example._1_basics.dto.EmployeeResponseDto;
import com.example._1_basics.exception.DuplicateEmailException;
import com.example._1_basics.exception.EmployeeNotFoundException;
import com.example._1_basics.model.Employee;
import com.example._1_basics.repository.EmployeeRepository;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@AllArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private EmployeeRepository employeeRepository;

    @Override
    public EmployeeResponseDto createEmployee(EmployeeRequestDto employeeRequestDto) {
        String normalisedEmail = normaliseEmail(employeeRequestDto.getEmail());
        if (employeeRepository.existsByEmailIgnoreCase(normalisedEmail)) {
            throw new DuplicateEmailException("Email:" + normalisedEmail + " already exists.");            
        }
        Employee unsavedEmployee = toEntity(employeeRequestDto);
        Employee savedEmployee = employeeRepository.save(unsavedEmployee);
        
        return toResponseDto(savedEmployee);
    }

    @Override
    public List<EmployeeResponseDto> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public EmployeeResponseDto getEmployeeById(Long id) {
        return toResponseDto(findOrThrow(id));
    }

    @Override
    public EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto employeeRequestDto) {
        Employee employee = findOrThrow(id);
        
        String normalisedEmail = normaliseEmail(employeeRequestDto.getEmail());
        if (!employee.getEmail().equalsIgnoreCase(normalisedEmail)  && employeeRepository.existsByEmailIgnoreCase(normalisedEmail)) {
            throw new DuplicateEmailException("Email:" + normalisedEmail + " already exists.");            
        }
        
        employee.setFirstName(employeeRequestDto.getFirstName().trim());
        employee.setLastName(employeeRequestDto.getLastName().trim());
        employee.setDepartment(employeeRequestDto.getDepartment().trim());
        employee.setEmail(normalisedEmail);
        employee.setSalary(employeeRequestDto.getSalary());
        employee.setHireDate(employeeRequestDto.getHireDate());

        return toResponseDto(employeeRepository.save(employee));
    }

    @Override
    public void deleteEmployee(Long id) {
        findOrThrow(id);
        employeeRepository.deleteById(id);
    }

    @Override
    public List<EmployeeResponseDto> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartmentIgnoreCase(department)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }
    
    private String normaliseEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private Employee toEntity(EmployeeRequestDto employeeRequestDto) {
        Employee employee = new Employee();
        employee.setFirstName(employeeRequestDto.getFirstName().trim());
        employee.setLastName(employeeRequestDto.getLastName().trim());
        employee.setDepartment(employeeRequestDto.getDepartment().trim());
        employee.setEmail(employeeRequestDto.getEmail().trim());
        employee.setSalary(employeeRequestDto.getSalary());
        employee.setHireDate(employeeRequestDto.getHireDate());
        return employee;
    }

    private EmployeeResponseDto toResponseDto(Employee employee) {
        EmployeeResponseDto employeeResponseDto = new EmployeeResponseDto();
        employeeResponseDto.setId(employee.getId());
        employeeResponseDto.setFirstName(employee.getFirstName());
        employeeResponseDto.setLastName(employee.getLastName());
        employeeResponseDto.setEmail(employee.getEmail());
        employeeResponseDto.setDepartment(employee.getDepartment());
        employeeResponseDto.setSalary(employee.getSalary());
        employeeResponseDto.setHireDate(employee.getHireDate());
        return employeeResponseDto;
    }

    private Employee findOrThrow(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new EmployeeNotFoundException("Employee with id:" + id + " not found."));
    }
}
