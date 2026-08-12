package com.example._5_mapstruct.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String field, String value) {
        super(field + " already in use: " + value);
    }
}
