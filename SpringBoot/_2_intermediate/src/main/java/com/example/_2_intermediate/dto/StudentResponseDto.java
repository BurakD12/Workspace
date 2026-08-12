package com.example._2_intermediate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentResponseDto {
    private Long id;
    private String firstName;    private String lastName;
    private String email;
    private String studentNumber;
    private String department;
    private BigDecimal gpa;
    private LocalDate enrollmentDate;
}
