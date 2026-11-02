package com.example._9_docker.exception;

public class DuplicateResourceException extends RuntimeException{
    public DuplicateResourceException(String field, String value) {
        super(field + " already in use: " + value);
    }
}
