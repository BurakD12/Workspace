package com.example._2_intermediate.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="student")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(nullable = false, length = 20, unique = true)
    private String studentNumber;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal gpa;

    @Column(nullable = false)
    private LocalDate enrollmentDate;

}
