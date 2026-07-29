package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.Objects;

/** Sanitizes notifications before they are written to an HTTP response. */
public interface NotificationSanitizer {

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
