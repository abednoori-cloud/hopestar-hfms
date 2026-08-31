package com.hopestar.hfms.common.exception;

/**
 * Thrown when rendering an XHTML string to PDF fails (e.g. malformed
 * markup, an I/O failure while writing the output stream). Falls through
 * to {@link GlobalExceptionHandler}'s generic {@code Exception} handler
 * (HTTP 500) -- a rendering failure is a system-level problem, not a
 * client input error.
 */
public class PdfGenerationException extends RuntimeException {

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
