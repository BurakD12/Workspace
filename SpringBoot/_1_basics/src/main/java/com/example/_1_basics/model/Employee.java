package com.example._1_basics.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity // JPA annotations to define the entity and table mapping 
@Table(name = "employees") // JPA annotations to define the entity and table mapping
@Getter // Lombok annotations to generate boilerplate code
@Setter // Lombok annotations to generate boilerplate code
@NoArgsConstructor // Lombok annotations to generate boilerplate code
@AllArgsConstructor // Lombok annotations to generate boilerplate code
@Builder // Lombok annotations to generate boilerplate code
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // JPA annotations to define the primary key and its generation strategy
    private Long id;

    @Column(nullable = false, length = 100) // JPA annotations to define the column properties
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(nullable = false)
    private LocalDate hireDate;
}
