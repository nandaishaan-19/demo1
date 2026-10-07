package com.examly.springapp.exceptions;

import com.examly.springapp.dto.ErrorLogDTO;
import com.examly.springapp.model.ErrorLog;
import com.examly.springapp.repository.ErrorLogRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central exception handling: every handled exception is logged and stored in the
 * "ErrorLogs" table, and the client gets a short, user-friendly message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private ErrorLogRepo errorLogRepo;

    @Autowired
    private ModelMapper modelMapper;

    @ExceptionHandler({DriverDeletionException.class, DriverRequestDeletionException.class, DuplicateDriverException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, ex, ex.getMessage(), request);
    }

    /** A @Valid request body that breaks a validation rule: 400 with the first message and a field -> message map. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().stream()
                .sorted((a, b) -> a.getField().compareTo(b.getField()))
                .forEach((FieldError error) -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        String message = errors.isEmpty() ? "Validation failed" : errors.values().iterator().next();

        ResponseEntity<Map<String, Object>> response = respond(HttpStatus.BAD_REQUEST, ex, message, request);
        Map<String, Object> body = new LinkedHashMap<>(response.getBody());
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ex, ex.getMessage(), request);
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<Map<String, Object>> handleOtp(OtpException ex, HttpServletRequest request) {
        return respond(ex.getStatus(), ex, ex.getMessage(), request);
    }

    /**
     * Thrown by @PreAuthorize inside a controller (for example a customer calling an admin endpoint).
     * Without this, the catch-all handler below would turn it into a 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, ex, "You do not have permission to do this.", request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ex, "The request could not be processed. Please check the data and try again.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex, HttpServletRequest request) {
        // Standard Spring MVC errors (405, 415, ...) keep their own status code.
        HttpStatusCode status = ex instanceof ErrorResponse errorResponse
                ? errorResponse.getStatusCode()
                : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = status.is5xxServerError() ? "Something went wrong. Please try again later." : ex.getMessage();
        return respond(status, ex, message, request);
    }

    private ResponseEntity<Map<String, Object>> respond(HttpStatusCode status, Exception ex, String message, HttpServletRequest request) {
        log.error("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage());
        try {
            ErrorLogDTO errorLog = new ErrorLogDTO(status.value(), ex.getClass().getName(), ex.getMessage(), request.getRequestURI());
            errorLogRepo.save(modelMapper.map(errorLog, ErrorLog.class));
        } catch (Exception loggingFailure) {
            log.warn("Could not store the error log: {}", loggingFailure.getMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message == null ? "Unexpected error" : message);
        return ResponseEntity.status(status).body(body);
    }
}
