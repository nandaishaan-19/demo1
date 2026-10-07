package com.examly.springapp.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for {@link com.examly.springapp.model.ErrorLog}.
 * {@code GlobalExceptionHandler} builds one of these for every handled error and the mapper turns it
 * into the entity row stored in the "ErrorLogs" table.
 */
public class ErrorLogDTO {
    private Long errorLogId;
    private LocalDateTime loggedAt;
    private int status;
    private String exceptionType;
    private String message;
    private String path;

    public ErrorLogDTO() {
    }

    public ErrorLogDTO(int status, String exceptionType, String message, String path) {
        this.loggedAt = LocalDateTime.now();
        this.status = status;
        this.exceptionType = exceptionType;
        // the column holds at most 1000 characters
        this.message = message != null && message.length() > 1000 ? message.substring(0, 1000) : message;
        this.path = path;
    }

    public Long getErrorLogId() {
        return errorLogId;
    }

    public void setErrorLogId(Long errorLogId) {
        this.errorLogId = errorLogId;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
