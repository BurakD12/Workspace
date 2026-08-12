package com.example._2_intermediate.exception;

public class StudentNotFoundException extends RuntimeException{
    public StudentNotFoundException(Long id) {
        super("Student not found with id: " + id);
    }
}
