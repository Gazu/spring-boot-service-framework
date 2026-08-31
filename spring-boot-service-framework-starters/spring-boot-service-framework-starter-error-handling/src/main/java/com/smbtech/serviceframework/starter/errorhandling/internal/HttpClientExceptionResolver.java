package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import com.smbtech.serviceframework.error.metadata.DependencyErrorMetadata;
import com.smbtech.serviceframework.error.metadata.RateLimitErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataBuilder;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Converts downstream HTTP client failures into safe public notifications while retaining complete
 * response diagnostics for internal reporting.
 */
final class HttpClientExceptionResolver implements ThrowableErrorResolver {

    /** Stable prefix used when the source exception has no notification. */
    public static final String ERROR_CODE_PREFIX = "E_SERVICE_FRAMEWORK_HTTP_CLIENT_";

    /** Public message that does not expose downstream response details. */
    public static final String PUBLIC_MESSAGE = "Downstream service request failed";

    /** Runs after request failures and before the generic fallback. */
    public static final int DEFAULT_ORDER = -700;

    /** Creates the HTTP client exception resolver. */
    public HttpClientExceptionResolver() {}

    @Override
    public boolean supports(Throwable throwable) {
        return throwable instanceof HttpClientResponseException;
    }

    @Override
    public ResolvedError resolve(Throwable throwable) {
        if (!(throwable instanceof HttpClientResponseException exception)) {
            throw new IllegalArgumentException("throwable must be an HttpClientResponseException");
        }
        return new ResolvedError(
                publicNotification(exception),
                category(exception),
                ErrorExposure.PUBLIC,
                diagnosticMessage(exception));
    }

    @Override
    public int order() {
        return DEFAULT_ORDER;
    }

    private static Notification publicNotification(HttpClientResponseException exception) {
        Map<String, Object> metadata = publicMetadata(exception);
        Notification source = exception.primaryNotification().orElse(null);
        if (source == null) {
            return Notification.builder()
                    .code(errorCode(exception.statusCode()))
                    .message(PUBLIC_MESSAGE)
                    .metadata(metadata)
                    .build();
        }
        return new Notification(
                source.code(),
                PUBLIC_MESSAGE,
                NotificationSeverity.ERROR,
                "",
                metadata,
                source.id(),
                source.timestamp());
    }

    private static Map<String, Object> publicMetadata(HttpClientResponseException exception) {
        HttpErrorResponse error = exception.error();
        ErrorCategory category = category(exception);
        StandardErrorMetadataBuilder metadata =
                StandardErrorMetadata.builder(category)
                        .retryable(retryable(error.statusCode()))
                        .dependency(
                                new DependencyErrorMetadata(
                                        "downstream",
                                        "",
                                        failureType(error.statusCode(), error.category().name())));
        retryAfterSeconds(error.headers())
                .ifPresent(seconds -> metadata.rateLimit(new RateLimitErrorMetadata(seconds)));
        return metadata.buildMap();
    }

    private static String failureType(int statusCode, String category) {
        if (statusCode == 429) {
            return "rate_limited";
        }
        return category == null || category.isBlank()
                ? "unknown"
                : category.toLowerCase(Locale.ROOT);
    }

    private static Boolean retryable(int statusCode) {
        if (statusCode == 429 || statusCode >= 500) {
            return true;
        }
        return statusCode >= 400 ? false : null;
    }

    private static OptionalLong retryAfterSeconds(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return OptionalLong.empty();
        }
        String value =
                headers.entrySet().stream()
                        .filter(entry -> "retry-after".equalsIgnoreCase(entry.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse("");
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds < 0 ? OptionalLong.empty() : OptionalLong.of(seconds);
        } catch (NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }

    private static ErrorCategory category(HttpClientResponseException exception) {
        return exception.statusCode() == 429 ? ErrorCategory.RATE_LIMIT : ErrorCategory.DOWNSTREAM;
    }

    private static String errorCode(int statusCode) {
        return statusCode > 0
                ? ERROR_CODE_PREFIX + String.format("%04d", statusCode)
                : ERROR_CODE_PREFIX + "UNKNOWN";
    }

    private static String diagnosticMessage(HttpClientResponseException exception) {
        HttpErrorResponse error = exception.error();
        StringBuilder diagnostic =
                new StringBuilder("HTTP client request failed")
                        .append(" client=")
                        .append(error.clientName())
                        .append(" method=")
                        .append(error.method())
                        .append(" uri=")
                        .append(error.uri())
                        .append(" status=")
                        .append(error.statusCode())
                        .append(" reason=")
                        .append(error.reasonPhrase())
                        .append(" contentType=")
                        .append(error.contentType())
                        .append(" charset=")
                        .append(error.charset())
                        .append(" bodyTruncated=")
                        .append(error.bodyTruncated())
                        .append(" headers=")
                        .append(error.headers())
                        .append(" body=")
                        .append(error.body());
        Throwable cause = exception.getCause();
        if (cause != null) {
            diagnostic.append(" cause=").append(cause.getClass().getName());
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                diagnostic.append(": ").append(cause.getMessage());
            }
        }
        return diagnostic.toString();
    }
}
