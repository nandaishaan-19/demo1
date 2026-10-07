package com.examly.springapp.logging;

import com.examly.springapp.dto.ActivityLogDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Sends log events to Jira (Jira Cloud REST API v2, e-mail + API token).
 *
 * <ul>
 *   <li>jira.events=ERROR (default): every unexpected error becomes a new Jira issue
 *       (the same error is reported at most once every 10 minutes).</li>
 *   <li>jira.events=ALL: additionally every BEFORE / AFTER controller event is added as a comment to the
 *       issue named in jira.log-issue-key.</li>
 * </ul>
 *
 * Nothing is sent unless jira.enabled=true and the connection settings are filled in. Sending happens on a
 * background thread, so a slow or unreachable Jira never slows an API call down, and a failure here is only
 * written to the console - it can never break a request.
 */
@Component
public class JiraLogClient {

    private static final Logger log = LoggerFactory.getLogger(JiraLogClient.class);
    private static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(10);

    @Value("${jira.enabled:false}")
    private boolean enabled;
    @Value("${jira.base-url:}")
    private String baseUrl;
    @Value("${jira.email:}")
    private String email;
    @Value("${jira.api-token:}")
    private String apiToken;
    @Value("${jira.project-key:}")
    private String projectKey;
    @Value("${jira.issue-type:Task}")
    private String issueType;
    @Value("${jira.events:ERROR}")
    private String events;
    @Value("${jira.log-issue-key:}")
    private String logIssueKey;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<String, Instant> recentErrors = new ConcurrentHashMap<>();
    /** One worker, at most 200 waiting events; when Jira is slow the extra events are dropped. */
    private final ThreadPoolExecutor sender = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(200), runnable -> {
                Thread thread = new Thread(runnable, "jira-log-sender");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.DiscardPolicy());

    /** True when Jira is switched on and configured well enough to be used. */
    public boolean isActive() {
        return enabled && notBlank(baseUrl) && notBlank(email) && notBlank(apiToken) && notBlank(projectKey);
    }

    public boolean wantsAllEvents() {
        return isActive() && "ALL".equalsIgnoreCase(events) && notBlank(logIssueKey);
    }

    /** Creates a Jira issue for an error event (skipped when the same error was reported in the last 10 minutes). */
    public void reportError(ActivityLogDTO event) {
        if (!isActive()) {
            return;
        }
        String key = event.getAction() + "|" + event.getDetails();
        Instant now = Instant.now();
        Instant previous = recentErrors.get(key);
        if (previous != null && Duration.between(previous, now).compareTo(DUPLICATE_WINDOW) < 0) {
            return;
        }
        recentErrors.put(key, now);
        if (recentErrors.size() > 500) {
            recentErrors.values().removeIf(time -> Duration.between(time, Instant.now()).compareTo(DUPLICATE_WINDOW) >= 0);
        }
        sender.execute(() -> createIssue(event));
    }

    /** Adds a BEFORE / AFTER event as a comment on the log issue (only when jira.events=ALL). */
    public void addComment(ActivityLogDTO event) {
        if (!wantsAllEvents()) {
            return;
        }
        sender.execute(() -> postComment(event));
    }

    private void createIssue(ActivityLogDTO event) {
        try {
            ObjectNode fields = json.createObjectNode();
            fields.putObject("project").put("key", projectKey.trim());
            fields.putObject("issuetype").put("name", issueType.trim());
            fields.put("summary", "DriveU error in " + event.getAction());
            fields.put("description", "Time: " + event.getLoggedAt() + "\n" + event.toLine());
            ObjectNode body = json.createObjectNode();
            body.set("fields", fields);
            send("/rest/api/2/issue", body.toString(), "create issue");
        } catch (Exception e) {
            log.warn("Could not report the error to Jira: {}", e.getMessage());
        }
    }

    private void postComment(ActivityLogDTO event) {
        try {
            ObjectNode body = json.createObjectNode();
            body.put("body", event.getLoggedAt() + "  " + event.toLine());
            send("/rest/api/2/issue/" + logIssueKey.trim() + "/comment", body.toString(), "add comment");
        } catch (Exception e) {
            log.warn("Could not add the log comment to Jira: {}", e.getMessage());
        }
    }

    private void send(String path, String body, String what) throws Exception {
        String credentials = email.trim() + ":" + apiToken.trim();
        String authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(stripTrailingSlash(baseUrl.trim()) + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            log.warn("Jira refused to {} (HTTP {}): {}", what, response.statusCode(), abbreviate(response.body()));
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String abbreviate(String text) {
        return text == null ? "" : (text.length() > 300 ? text.substring(0, 300) + "..." : text);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    @PreDestroy
    void shutdown() {
        sender.shutdownNow();
    }
}
