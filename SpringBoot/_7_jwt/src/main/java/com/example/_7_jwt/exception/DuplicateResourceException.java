package com.example._7_jwt.exception;

public class DuplicateResourceException extends RuntimeException{
    public DuplicateResourceException(String field, String value) {
        super(field + " already in use: " + value);
    }
}
