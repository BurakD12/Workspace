package com.example._10_integration_testing.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String field, String value) {
        super(field + " already in use: " + value);
    }
}
