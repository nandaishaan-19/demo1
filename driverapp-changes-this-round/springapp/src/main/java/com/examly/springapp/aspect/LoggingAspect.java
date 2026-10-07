package com.examly.springapp.aspect;

import com.examly.springapp.dto.ActivityLogDTO;
import com.examly.springapp.exceptions.DuplicateDriverException;
import com.examly.springapp.exceptions.InvalidRequestException;
import com.examly.springapp.exceptions.OtpException;
import com.examly.springapp.logging.ActivityLogger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Aspect-oriented logging. Without touching a single controller or service method it logs
 * <ul>
 *   <li>{@code @Before}  - a controller / service method is about to run,</li>
 *   <li>{@code @After}   - the method has finished (also when it threw), with how long it took,</li>
 *   <li>{@code @AfterThrowing} - the method threw an exception (an error event).</li>
 * </ul>
 * What is NOT logged: passwords, OTPs, e-mail addresses, request bodies - arguments are reduced to numbers and
 * booleans (for example the id in /driverRequest/5); everything else is shown as its type only.
 */
@Aspect
@Component
public class LoggingAspect {

    @Autowired
    private ActivityLogger activityLogger;

    /** Start times of the methods currently running on this thread (innermost last). */
    private final ThreadLocal<Deque<Long>> startTimes = ThreadLocal.withInitial(ArrayDeque::new);
    /** The last exception already reported on this thread, so a failure is not logged again at every layer. */
    private final ThreadLocal<Throwable> lastReported = new ThreadLocal<>();

    @Pointcut("within(com.examly.springapp.controller..*)")
    public void controllerLayer() {
    }

    @Pointcut("within(com.examly.springapp.service..*)")
    public void serviceLayer() {
    }

    @Before("controllerLayer() || serviceLayer()")
    public void logBefore(JoinPoint joinPoint) {
        startTimes.get().push(System.nanoTime());
        ActivityLogDTO event = newEvent(joinPoint, ActivityLogDTO.BEFORE);
        event.setDetails("args: " + describeArguments(joinPoint.getArgs()));
        activityLogger.record(event);
    }

    @After("controllerLayer() || serviceLayer()")
    public void logAfter(JoinPoint joinPoint) {
        Deque<Long> stack = startTimes.get();
        Long started = stack.isEmpty() ? null : stack.pop();
        if (stack.isEmpty()) {
            // the outermost call is over: leave nothing behind on a pooled thread
            startTimes.remove();
            lastReported.remove();
        }
        ActivityLogDTO event = newEvent(joinPoint, ActivityLogDTO.AFTER);
        if (started != null) {
            event.setDurationMs((System.nanoTime() - started) / 1_000_000);
        }
        activityLogger.record(event);
    }

    @AfterThrowing(pointcut = "controllerLayer() || serviceLayer()", throwing = "error")
    public void logError(JoinPoint joinPoint, Throwable error) {
        if (lastReported.get() == error) {
            return; // already reported by the inner layer
        }
        lastReported.set(error);
        ActivityLogDTO event = newEvent(joinPoint, ActivityLogDTO.FAILED);
        // a mistake of the caller (wrong input, no permission) is a warning; anything else is a real error
        event.setLevel(isExpected(error) ? ActivityLogDTO.WARN : ActivityLogDTO.ERROR);
        event.setDetails(error.getClass().getSimpleName() + ": " + error.getMessage());
        activityLogger.record(event);
    }

    private boolean isExpected(Throwable error) {
        return error instanceof InvalidRequestException
                || error instanceof OtpException
                || error instanceof DuplicateDriverException
                || error instanceof AccessDeniedException
                || error instanceof IllegalArgumentException;
    }

    private ActivityLogDTO newEvent(JoinPoint joinPoint, String phase) {
        ActivityLogDTO event = new ActivityLogDTO();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        event.setAction(className + "." + joinPoint.getSignature().getName());
        event.setLayer(className.endsWith("Controller") ? ActivityLogDTO.CONTROLLER : ActivityLogDTO.SERVICE);
        event.setPhase(phase);
        event.setUsername(currentUser());
        if (ActivityLogDTO.CONTROLLER.equals(event.getLayer())) {
            HttpServletRequest request = currentRequest();
            if (request != null) {
                event.setHttpMethod(request.getMethod());
                event.setPath(request.getRequestURI());
            }
        }
        return event;
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String describeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "none";
        }
        StringBuilder text = new StringBuilder();
        for (Object arg : args) {
            if (text.length() > 0) {
                text.append(", ");
            }
            if (arg == null) {
                text.append("null");
            } else if (arg instanceof Number || arg instanceof Boolean) {
                text.append(arg);
            } else {
                text.append(arg.getClass().getSimpleName());
            }
        }
        return text.toString();
    }
}
