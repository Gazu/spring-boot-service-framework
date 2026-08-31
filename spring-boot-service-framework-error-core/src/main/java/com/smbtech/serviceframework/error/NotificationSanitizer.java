package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataKeys;
import java.util.Objects;
import java.util.Set;

/** Sanitizes notifications before they are written to an HTTP response. */
public interface NotificationSanitizer {

    /** Value used in place of sensitive response content. */
    String REDACTED_VALUE = "<redacted>";

    /** Metadata keys accepted by the default response sanitizer. */
    Set<String> DEFAULT_METADATA_ALLOWLIST =
            Set.of(
                    StandardErrorMetadataKeys.SCHEMA_VERSION,
                    StandardErrorMetadataKeys.CATEGORY,
                    "correlationId",
                    "path",
                    StandardErrorMetadataKeys.RETRYABLE,
                    StandardErrorMetadataKeys.REQUEST,
                    StandardErrorMetadataKeys.VALIDATION,
                    StandardErrorMetadataKeys.VIOLATIONS,
                    StandardErrorMetadataKeys.SECURITY,
                    StandardErrorMetadataKeys.OAUTH2,
                    StandardErrorMetadataKeys.RESOURCE,
                    StandardErrorMetadataKeys.CONFLICT,
                    StandardErrorMetadataKeys.DEPENDENCY,
                    StandardErrorMetadataKeys.RATE_LIMIT,
                    StandardErrorMetadataKeys.HTTP);

    /**
     * Creates a sanitizer using the framework metadata allowlist.
     *
     * @return default notification sanitizer
     */
    static NotificationSanitizer defaultSanitizer() {
        return new DefaultNotificationSanitizer();
    }

    /**
     * Creates a sanitizer using a custom top-level metadata allowlist.
     *
     * @param metadataAllowlist metadata keys accepted in response notifications
     * @return configured notification sanitizer
     */
    static NotificationSanitizer withMetadataAllowlist(Set<String> metadataAllowlist) {
        return new DefaultNotificationSanitizer(metadataAllowlist);
    }

    /**
     * Redacts sensitive material embedded in diagnostic text.
     *
     * @param value source text
     * @return redacted text
     */
    static String redactText(String value) {
        return DefaultNotificationSanitizer.sanitizeText(value);
    }

    /**
     * Sanitizes a notification without mutating the source instance.
     *
     * @param notification notification to sanitize
     * @return sanitized notification
     */
    Notification sanitize(Notification notification);

    /**
     * Sanitizes the response notification of a resolved error while preserving its separate
     * diagnostic information.
     *
     * @param resolvedError resolved error to sanitize
     * @return sanitized resolved error
     */
    default ResolvedError sanitize(ResolvedError resolvedError) {
        ResolvedError safeError =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        return safeError.withNotification(sanitize(safeError.notification()));
    }
}
