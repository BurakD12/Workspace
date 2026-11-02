package com.example._8_method_security.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String field, String value) {
        super(field + " already in use: " + value);
    }
}
