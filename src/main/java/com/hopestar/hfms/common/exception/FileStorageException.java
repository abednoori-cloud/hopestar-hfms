package com.hopestar.hfms.common.exception;

/**
 * Thrown for file storage failures: disallowed file type, file too large,
 * or an I/O failure while saving/reading a stored document (student
 * documents, backup files, invoice PDFs). Mapped to HTTP 400 by
 * {@link GlobalExceptionHandler}.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
