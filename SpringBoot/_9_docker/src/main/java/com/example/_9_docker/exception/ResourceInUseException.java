package com.example._9_docker.exception;

public class ResourceInUseException extends RuntimeException{
    public ResourceInUseException(String resource, Long id, String referencedBy) {
        super(resource + " " + id + " cannot be deleted: it is still referenced by " + referencedBy);
    }
}
