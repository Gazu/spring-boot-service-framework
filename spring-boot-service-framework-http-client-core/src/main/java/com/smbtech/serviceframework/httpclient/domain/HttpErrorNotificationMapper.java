package com.smbtech.serviceframework.httpclient.domain;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Maps framework HTTP error responses into structured notifications. */
public final class HttpErrorNotificationMapper {
    /** Creates a http error notification mapper instance. */
    public HttpErrorNotificationMapper() {}

    /** Prefix reserved for notifications produced from downstream HTTP failures. */
    public static final String CODE_PREFIX = "E_SERVICE_FRAMEWORK_HTTP_CLIENT_";

    /** Fallback notification code when a downstream failure cannot be classified. */
    public static final String UNKNOWN_ERROR_CODE = CODE_PREFIX + "UNKNOWN";

    /**
     * Creates notifications for an HTTP error response.
     *
     * @param error HTTP error response
     * @return immutable notification list
     */
    public List<Notification> map(HttpErrorResponse error) {
        return List.of(notification(error));
    }

    /**
     * Creates notifications for an HTTP error response using policy-controlled code prefix and
     * metadata behavior.
     *
     * @param error HTTP error response
     * @param policy error handling policy
     * @return immutable notification list
     */
    public List<Notification> map(HttpErrorResponse error, ErrorHandlingPolicy policy) {
        return List.of(notification(error, policy));
    }

    /**
     * Creates the primary notification for an HTTP error response.
     *
     * @param error HTTP error response
     * @return notification
     */
    public Notification notification(HttpErrorResponse error) {
        return notification(error, ErrorHandlingPolicy.defaults());
    }

    /**
     * Creates the primary notification for an HTTP error response using policy-controlled code
     * prefix and metadata behavior.
     *
     * @param error HTTP error response
     * @param policy error handling policy
     * @return notification
     */
    public Notification notification(HttpErrorResponse error, ErrorHandlingPolicy policy) {
        HttpErrorResponse safeError = Objects.requireNonNull(error, "error must not be null");
        ErrorHandlingPolicy safePolicy =
                Objects.requireNonNullElseGet(policy, ErrorHandlingPolicy::defaults);
        return Notification.builder()
                .code(code(safeError.statusCode(), safePolicy.notificationCodePrefix()))
                .message(message(safeError))
                .metadata(safePolicy.includeNotificationMetadata() ? metadata(safeError) : Map.of())
                .build();
    }

    private String code(int statusCode, String prefix) {
        String safePrefix = Objects.requireNonNullElse(prefix, CODE_PREFIX);
        if (statusCode <= 0) {
            return safePrefix + "UNKNOWN";
        }
        return safePrefix + String.format("%04d", statusCode);
    }

    private String message(HttpErrorResponse error) {
        if (error.reasonPhrase().isBlank()) {
            return "HTTP " + error.statusCode() + " response received from downstream service";
        }
        return "HTTP "
                + error.statusCode()
                + " "
                + error.reasonPhrase()
                + " response received from downstream service";
    }

    private Map<String, Object> metadata(HttpErrorResponse error) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("clientName", error.clientName());
        metadata.put("method", error.method());
        metadata.put("uri", error.uri());
        metadata.put("statusCode", error.statusCode());
        metadata.put("reasonPhrase", error.reasonPhrase());
        metadata.put("category", error.category().name());
        metadata.put("contentType", error.contentType());
        metadata.put("charset", error.charset());
        metadata.put("bodyTruncated", error.bodyTruncated());
        return metadata;
    }
}
