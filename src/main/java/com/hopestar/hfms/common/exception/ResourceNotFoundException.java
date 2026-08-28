package com.hopestar.hfms.common.exception;

/**
 * Thrown when a lookup by id/code finds no matching, active record
 * (e.g. a student, invoice, or user that does not exist or was
 * soft-deleted). Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entityName, Object identifier) {
        super("%s not found with identifier: %s".formatted(entityName, identifier));
    }
}
