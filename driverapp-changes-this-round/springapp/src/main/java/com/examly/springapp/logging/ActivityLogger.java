package com.examly.springapp.logging;

import com.examly.springapp.dto.ActivityLogDTO;
import com.examly.springapp.model.ActivityLog;
import com.examly.springapp.repository.ActivityLogRepo;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The one place where a logged event goes to all of its destinations:
 * <ol>
 *   <li>the application log (console and logs/driveu.log) through the "driveu.activity" logger,</li>
 *   <li>the "ActivityLogs" database table (API calls and errors; internal service calls only go to the log file),</li>
 *   <li>Jira (errors as issues, and - when jira.events=ALL - every API call as a comment).</li>
 * </ol>
 * Writing a log can never make a request fail: every destination is wrapped in its own try / catch.
 */
@Component
public class ActivityLogger {

    private static final Logger activity = LoggerFactory.getLogger("driveu.activity");
    private static final Logger log = LoggerFactory.getLogger(ActivityLogger.class);

    @Autowired
    private ActivityLogRepo activityLogRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private JiraLogClient jira;

    public void record(ActivityLogDTO event) {
        writeToLogFile(event);
        if (!ActivityLogDTO.SERVICE.equals(event.getLayer()) || ActivityLogDTO.ERROR.equals(event.getLevel())) {
            store(event);
        }
        sendToJira(event);
    }

    private void writeToLogFile(ActivityLogDTO event) {
        try {
            String line = event.toLine();
            if (ActivityLogDTO.ERROR.equals(event.getLevel())) {
                activity.error(line);
            } else if (ActivityLogDTO.WARN.equals(event.getLevel())) {
                activity.warn(line);
            } else {
                activity.info(line);
            }
        } catch (Exception e) {
            // never let logging break the request
        }
    }

    private void store(ActivityLogDTO event) {
        try {
            activityLogRepo.save(modelMapper.map(event, ActivityLog.class));
        } catch (Exception e) {
            log.warn("Could not store the activity log: {}", e.getMessage());
        }
    }

    private void sendToJira(ActivityLogDTO event) {
        try {
            if (ActivityLogDTO.ERROR.equals(event.getLevel())) {
                jira.reportError(event);
            } else if (ActivityLogDTO.CONTROLLER.equals(event.getLayer())) {
                jira.addComment(event);
            }
        } catch (Exception e) {
            log.warn("Could not send the activity log to Jira: {}", e.getMessage());
        }
    }
}
