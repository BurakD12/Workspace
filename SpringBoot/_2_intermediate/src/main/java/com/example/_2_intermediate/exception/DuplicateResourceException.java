package com.example._2_intermediate.exception;

public class DuplicateResourceException extends RuntimeException{
    public DuplicateResourceException(String field, String value) {
        super(field + " already in use: " + value);
    }
}
