package com.example._6_security.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String field, String value) {
        super(field + " already in use: " + value);
    }
}
