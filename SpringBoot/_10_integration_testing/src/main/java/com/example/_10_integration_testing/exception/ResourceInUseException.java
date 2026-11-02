package com.example._10_integration_testing.exception;

public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String resource, Long id, String referencedBy) {
        super(resource + " " + id + " cannot be deleted: it is still referenced by " + referencedBy);
    }
}
