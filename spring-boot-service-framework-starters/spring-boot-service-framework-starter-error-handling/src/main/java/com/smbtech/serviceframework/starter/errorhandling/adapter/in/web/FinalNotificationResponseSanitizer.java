package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.DefaultNotificationSanitizer;
import com.smbtech.serviceframework.error.NotificationSanitizer;
import com.smbtech.serviceframework.error.ResolvedError;
import java.util.Objects;
import org.springframework.http.ResponseEntity;

/** Enforces response exposure and sanitization after every application customizer. */
public final class FinalNotificationResponseSanitizer {

    private final DefaultNotificationResponseFactory safetyFactory;

    /** Creates the final sanitizer with the mandatory framework defaults. */
    public FinalNotificationResponseSanitizer() {
        this(new DefaultNotificationSanitizer(), true);
    }

    /**
     * Creates the final sanitizer with the configured metadata allowlist.
     *
     * @param notificationSanitizer mandatory notification sanitizer
     * @param includeFieldViolations whether internal responses include field violations
     */
    public FinalNotificationResponseSanitizer(
            NotificationSanitizer notificationSanitizer, boolean includeFieldViolations) {
        this.safetyFactory =
                new DefaultNotificationResponseFactory(
                        new DefaultNotificationHttpStatusResolver(),
                        Objects.requireNonNull(
                                notificationSanitizer, "notificationSanitizer must not be null"),
                        includeFieldViolations);
    }

    /**
     * Applies the final non-bypassable response safety boundary.
     *
     * @param response customized HTTP response
     * @param resolvedError resolved framework error
     * @return response with a sanitized notification body
     */
    public ResponseEntity<Notification> sanitize(
            ResponseEntity<Notification> response, ResolvedError resolvedError) {
        ResponseEntity<Notification> source =
                Objects.requireNonNull(response, "response must not be null");
        ResolvedError error =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        Notification customized =
                Objects.requireNonNullElse(source.getBody(), error.notification());
        ResolvedError guardedError = error.withNotification(customized);
        Notification safeBody = safetyFactory.create(guardedError).getBody();
        return new ResponseEntity<>(safeBody, source.getHeaders(), source.getStatusCode());
    }
}
