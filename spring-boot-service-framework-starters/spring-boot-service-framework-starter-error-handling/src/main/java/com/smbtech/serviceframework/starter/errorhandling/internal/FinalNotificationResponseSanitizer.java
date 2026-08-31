package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import java.util.Objects;
import org.springframework.http.ResponseEntity;

/** Enforces response exposure and sanitization after every application customizer. */
final class FinalNotificationResponseSanitizer {

    private final NotificationResponseFactory safetyFactory;

    FinalNotificationResponseSanitizer(NotificationResponseFactory safetyFactory) {
        this.safetyFactory =
                Objects.requireNonNull(safetyFactory, "safetyFactory must not be null");
    }

    ResponseEntity<Notification> sanitize(
            ResponseEntity<Notification> response, ResolvedError resolvedError) {
        ResponseEntity<Notification> source =
                Objects.requireNonNull(response, "response must not be null");
        ResolvedError error =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        Notification customized =
                Objects.requireNonNullElse(source.getBody(), error.notification());
        Notification safeBody = safetyFactory.create(error.withNotification(customized)).getBody();
        return new ResponseEntity<>(safeBody, source.getHeaders(), source.getStatusCode());
    }
}
