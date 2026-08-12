package com.example._2_intermediate.repository;

import com.example._2_intermediate.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByStudentNumberIgnoreCase(String studentNumber);
    List<Student> findByDepartmentIgnoreCase(String department);
}
