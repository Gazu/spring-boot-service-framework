package com.smbtech.serviceframework.starter.errorhandling.adapter.out.logging;

import com.smbtech.serviceframework.error.DefaultNotificationSanitizer;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataKeys;
import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Emits resolved HTTP failures through the framework structured logging port. */
public final class StructuredErrorReporter implements ErrorReporter {

    /** Correlation key populated by the logging starter transaction filter. */
    public static final String TRANSACTION_ID_KEY = "transactionId";

    private static final String EVENT_MESSAGE = "Request processing failed";
    private static final String EVENT_TAG = "error-handling";
    private static final String OAUTH2_AUTHENTICATION_EXCEPTION =
            "org.springframework.security.oauth2.core.OAuth2AuthenticationException";
    private static final int MAX_OAUTH2_ERROR_CODE_LENGTH = 128;
    private static final int MAX_CAUSE_DEPTH = 16;
    private static final Set<String> SECURITY_REASONS =
            Arrays.stream(SecurityFailureReason.values())
                    .map(SecurityFailureReason::metadataValue)
                    .collect(Collectors.toUnmodifiableSet());

    private final StructuredLogger logger;
    private final CorrelationContext correlationContext;
    private final DefaultNotificationSanitizer sanitizer;
    private final boolean includeDiagnostics;

    /**
     * Creates a structured error reporter.
     *
     * @param logger structured logger
     * @param correlationContext current correlation context
     */
    public StructuredErrorReporter(StructuredLogger logger, CorrelationContext correlationContext) {
        this(logger, correlationContext, true);
    }

    /**
     * Creates a structured error reporter.
     *
     * @param logger structured logger
     * @param correlationContext current correlation context
     * @param includeDiagnostics whether internal diagnostics are included
     */
    public StructuredErrorReporter(
            StructuredLogger logger,
            CorrelationContext correlationContext,
            boolean includeDiagnostics) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
        this.correlationContext =
                Objects.requireNonNull(correlationContext, "correlationContext must not be null");
        this.sanitizer = new DefaultNotificationSanitizer();
        this.includeDiagnostics = includeDiagnostics;
    }

    @Override
    public void report(Throwable cause, ResolvedError resolvedError, HttpServletRequest request) {
        report(cause, resolvedError, request, null);
    }

    @Override
    public void report(
            Throwable cause,
            ResolvedError resolvedError,
            HttpServletRequest request,
            int statusCode) {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("statusCode must be between 100 and 599");
        }
        report(cause, resolvedError, request, Integer.valueOf(statusCode));
    }

    private void report(
            Throwable cause,
            ResolvedError resolvedError,
            HttpServletRequest request,
            Integer statusCode) {
        Throwable safeCause = Objects.requireNonNull(cause, "cause must not be null");
        ResolvedError error =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        HttpServletRequest httpRequest =
                Objects.requireNonNull(request, "request must not be null");
        LogLevel level = logLevel(error.category());
        if (!logger.isEnabled(level, EventType.ERROR)) {
            return;
        }

        StructuredEvent.Builder event =
                StructuredEvent.builder(EventType.ERROR)
                        .message(EVENT_MESSAGE)
                        .with("code", error.notification().code())
                        .with("category", error.category().name())
                        .with(
                                "correlationId",
                                correlationContext.find(TRANSACTION_ID_KEY).orElse(""))
                        .with("exceptionType", safeCause.getClass().getName())
                        .with("request", requestData(httpRequest))
                        .with("cause", causeData(safeCause));
        if (statusCode != null) {
            event.with("status", statusCode);
        }
        String securityReason = securityReason(error);
        if (!securityReason.isEmpty()) {
            event.with("securityReason", securityReason);
        }
        String oauth2ErrorCode = oauth2ErrorCode(safeCause);
        if (!oauth2ErrorCode.isEmpty()) {
            event.with("oauth2ErrorCode", oauth2ErrorCode);
        }
        if (includeDiagnostics) {
            event.with("diagnostic", sanitizer.sanitizeText(error.diagnosticMessage()));
        }
        logger.log(
                level,
                event.tag(EVENT_TAG)
                        .sensitive()
                        .throwable(sanitizedThrowable(safeCause, 0))
                        .build());
    }

    private static String securityReason(ResolvedError error) {
        Object security = error.notification().metadata().get(StandardErrorMetadataKeys.SECURITY);
        if (!(security instanceof Map<?, ?> values)) {
            return "";
        }
        Object reason = values.get(StandardErrorMetadataKeys.Security.REASON);
        return reason instanceof String value && SECURITY_REASONS.contains(value) ? value : "";
    }

    private String oauth2ErrorCode(Throwable cause) {
        Throwable current = cause;
        int depth = 0;
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (isTypeNamed(current.getClass(), OAUTH2_AUTHENTICATION_EXCEPTION)) {
                String errorCode = invokeOAuth2ErrorCode(current);
                if (!errorCode.isEmpty()) {
                    String sanitized = sanitizer.sanitizeText(errorCode);
                    return sanitized.substring(
                            0, Math.min(sanitized.length(), MAX_OAUTH2_ERROR_CODE_LENGTH));
                }
            }
            Throwable nested = current.getCause();
            current = nested == current ? null : nested;
            depth++;
        }
        return "";
    }

    private static String invokeOAuth2ErrorCode(Throwable exception) {
        try {
            Object error = exception.getClass().getMethod("getError").invoke(exception);
            Object errorCode = error.getClass().getMethod("getErrorCode").invoke(error);
            return errorCode instanceof String value ? value : "";
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return "";
        }
    }

    private static boolean isTypeNamed(Class<?> type, String expectedName) {
        Class<?> current = type;
        while (current != null) {
            if (expectedName.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static LogLevel logLevel(ErrorCategory category) {
        return category == ErrorCategory.INTERNAL || category == ErrorCategory.DOWNSTREAM
                ? LogLevel.ERROR
                : LogLevel.WARN;
    }

    private static Map<String, Object> requestData(HttpServletRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", Objects.requireNonNullElse(request.getMethod(), ""));
        data.put("path", Objects.requireNonNullElse(request.getRequestURI(), ""));
        return Map.copyOf(data);
    }

    private Map<String, Object> causeData(Throwable cause) {
        Throwable rootCause = rootCause(cause);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", cause.getClass().getName());
        data.put("message", sanitizer.sanitizeText(cause.getMessage()));
        data.put("rootType", rootCause.getClass().getName());
        data.put("rootMessage", sanitizer.sanitizeText(rootCause.getMessage()));
        return Map.copyOf(data);
    }

    private Throwable sanitizedThrowable(Throwable source, int depth) {
        Throwable nestedCause = source.getCause();
        Throwable sanitizedCause =
                nestedCause == null || nestedCause == source || depth >= MAX_CAUSE_DEPTH
                        ? null
                        : sanitizedThrowable(nestedCause, depth + 1);
        ReportedErrorException sanitized =
                new ReportedErrorException(
                        source.getClass().getName(),
                        sanitizer.sanitizeText(source.getMessage()),
                        sanitizedCause);
        sanitized.setStackTrace(source.getStackTrace());
        return sanitized;
    }

    private static Throwable rootCause(Throwable source) {
        Throwable root = source;
        int depth = 0;
        while (root.getCause() != null && root.getCause() != root && depth < MAX_CAUSE_DEPTH) {
            root = root.getCause();
            depth++;
        }
        return root;
    }

    private static final class ReportedErrorException extends RuntimeException {
        private ReportedErrorException(String type, String message, Throwable cause) {
            super(type + (message == null || message.isBlank() ? "" : ": " + message), cause);
        }
    }
}
