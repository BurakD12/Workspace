package com.example._5_mapstruct.exception;

public class RelationshipConflictException extends RuntimeException {
    public RelationshipConflictException(String message) {
        super(message);
    }
}
