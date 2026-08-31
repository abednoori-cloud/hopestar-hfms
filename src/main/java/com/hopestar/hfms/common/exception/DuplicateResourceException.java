package com.hopestar.hfms.common.exception;

/**
 * Thrown when an operation would violate a uniqueness business rule
 * (duplicate receipt number, student code, username, etc. — see the
 * approved architecture's Business Rules §4). Mapped to HTTP 409 by
 * {@link GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String entityName, String field, Object value) {
        super("%s already exists with %s: %s".formatted(entityName, field, value));
    }
}
