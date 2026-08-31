package com.hopestar.hfms.common.exception;

/**
 * Thrown when a request is well-formed but violates a domain/business rule
 * that Bean Validation cannot express — e.g. "students cannot have a
 * negative balance", "receipt numbers are unique", "salary already
 * generated for this period" (see approved architecture §4 Business
 * Rules). Mapped to HTTP 422 by {@link GlobalExceptionHandler}.
 */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}
