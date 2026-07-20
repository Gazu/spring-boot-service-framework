package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ResolvedError;
import org.springframework.http.ResponseEntity;

/** Creates HTTP responses from resolved application errors. */
@FunctionalInterface
public interface NotificationResponseFactory {

    /**
     * Creates an HTTP response containing a public notification.
     *
     * @param resolvedError resolved error
     * @return notification response
     */
    ResponseEntity<Notification> create(ResolvedError resolvedError);
}
