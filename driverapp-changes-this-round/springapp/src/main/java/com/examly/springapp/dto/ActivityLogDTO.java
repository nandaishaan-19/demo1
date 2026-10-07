package com.examly.springapp.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for {@link com.examly.springapp.model.ActivityLog}. The logging aspect fills one
 * of these for every logged event; {@code GET /api/admin/logs} returns them to admins.
 */
public class ActivityLogDTO {
    public static final String INFO = "INFO";
    public static final String WARN = "WARN";
    public static final String ERROR = "ERROR";

    public static final String BEFORE = "BEFORE";
    public static final String AFTER = "AFTER";
    public static final String FAILED = "ERROR";

    public static final String CONTROLLER = "CONTROLLER";
    public static final String SERVICE = "SERVICE";

    private Long activityLogId;
    private LocalDateTime loggedAt = LocalDateTime.now();
    private String level = INFO;
    private String phase;
    private String layer;
    private String action;
    private String username;
    private String httpMethod;
    private String path;
    private Long durationMs;
    private String details;

    public ActivityLogDTO() {
    }

    public Long getActivityLogId() {
        return activityLogId;
    }

    public void setActivityLogId(Long activityLogId) {
        this.activityLogId = activityLogId;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details != null && details.length() > 1000 ? details.substring(0, 1000) : details;
    }

    /** One readable line, used for the log file / console and for Jira comments. */
    public String toLine() {
        StringBuilder line = new StringBuilder();
        line.append('[').append(layer).append("] ").append(phase).append(' ').append(action);
        if (httpMethod != null) {
            line.append(" | ").append(httpMethod).append(' ').append(path);
        }
        line.append(" | user=").append(username);
        if (durationMs != null) {
            line.append(" | ").append(durationMs).append(" ms");
        }
        if (details != null && !details.isEmpty()) {
            line.append(" | ").append(details);
        }
        return line.toString();
    }
}
