package com.hopestar.hfms.common.exception;

import com.hopestar.hfms.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central error handling for the whole application.
 * <p>
 * Requests made via AJAX/fetch (identified by the {@code X-Requested-With}
 * header or an {@code Accept: application/json} header) receive a JSON
 * {@link ApiResponse}; ordinary browser navigations receive a rendered
 * Thymeleaf error view instead, so both the AJAX-driven dashboard widgets
 * and the classic server-rendered pages get an appropriate response.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return respond(ex.getMessage(), "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public Object handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return respond(ex.getMessage(), "DUPLICATE_RESOURCE", HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(BusinessValidationException.class)
    public Object handleBusinessValidation(BusinessValidationException ex, HttpServletRequest request) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return respond(ex.getMessage(), "BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler(FileStorageException.class)
    public Object handleFileStorage(FileStorageException ex, HttpServletRequest request) {
        log.error("File storage error", ex);
        return respond(ex.getMessage(), "FILE_STORAGE_ERROR", HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        return respond("You do not have permission to perform this action.", "ACCESS_DENIED",
                HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation", ex);
        return respond("This action could not be completed because it conflicts with existing data.",
                "DATA_INTEGRITY_VIOLATION", HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed: {}", fieldErrors);

        if (isAjax(request)) {
            return ResponseEntity.unprocessableEntity()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Validation failed")
                            .errorCode("VALIDATION_ERROR")
                            .data(fieldErrors)
                            .build());
        }
        ModelAndView mav = new ModelAndView("error/validation-error");
        mav.addObject("fieldErrors", fieldErrors);
        mav.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return respond("An unexpected error occurred. Please try again or contact the administrator.",
                "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Object respond(String message, String errorCode, HttpStatus status, HttpServletRequest request) {
        if (isAjax(request)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error(message, errorCode));
        }
        ModelAndView mav = new ModelAndView("error/general-error");
        mav.addObject("errorMessage", message);
        mav.addObject("errorCode", errorCode);
        mav.setStatus(status);
        return mav;
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }
}
