package com.example._2_intermediate.dto;


import jakarta.validation.constraints.*;
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
public class StudentRequestDto {
    @NotBlank(message = "First name is required.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    private String lastName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email should be valid.")
    private String email;

    @NotBlank(message = "Student number is required.")
    @Pattern(regexp = "S\\d{7}", message = "Student number must start with 'S' followed by 7 digits.")
    private String studentNumber;

    @NotBlank(message = "Department is required.")
    private String department;

    @NotNull(message = "GPA is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "GPA must be at least 0.0.")
    @DecimalMax(value = "4.0", inclusive = true, message = "GPA must be at most 4.0.")
    private BigDecimal gpa;

    @NotNull(message = "Enrollment date is required.")
    @PastOrPresent(message = "Enrollment must be in the past.")
    private LocalDate enrollmentDate;
}
