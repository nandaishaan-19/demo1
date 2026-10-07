package com.examly.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One row per logged activity (an API call entering / leaving a controller, or an error), stored in the
 * "ActivityLogs" table. Written by the AOP logging aspect through {@code ActivityLogger}.
 */
@Entity
@Table(name = "ActivityLogs")
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activityLogId;

    private LocalDateTime loggedAt;
    /** INFO, WARN or ERROR. */
    @Column(name = "log_level")
    private String level;
    /** BEFORE (method entered), AFTER (method left) or ERROR (method threw). */
    private String phase;
    /** CONTROLLER or SERVICE. */
    private String layer;
    /** Class and method, e.g. DriverRequestController.updateDriverRequest. */
    private String action;
    /** E-mail of the logged-in user, or "anonymous". */
    private String username;
    private String httpMethod;
    private String path;
    private Long durationMs;

    @Column(length = 1000)
    private String details;

    public ActivityLog() {
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
        this.details = details;
    }
}
