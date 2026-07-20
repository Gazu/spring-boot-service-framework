package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.Objects;

/** Sanitizes notifications before they are exposed outside the application. */
public interface NotificationSanitizer {

    /**
     * Sanitizes a notification without mutating the source instance.
     *
     * @param notification notification to sanitize
     * @return sanitized notification
     */
    Notification sanitize(Notification notification);

    /**
     * Sanitizes the public notification of a resolved error while preserving its internal
     * diagnostic information.
     *
     * @param resolvedError resolved error to sanitize
     * @return sanitized resolved error
     */
    default ResolvedError sanitize(ResolvedError resolvedError) {
        ResolvedError safeError =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        return new ResolvedError(
                sanitize(safeError.notification()),
                safeError.category(),
                safeError.exposure(),
                safeError.diagnosticMessage(),
                safeError.fieldViolations());
    }
}
