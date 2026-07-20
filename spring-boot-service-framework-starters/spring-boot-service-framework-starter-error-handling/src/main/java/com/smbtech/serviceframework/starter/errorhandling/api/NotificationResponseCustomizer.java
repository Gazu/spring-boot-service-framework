package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ResolvedError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/** Applies an ordered customization to a notification HTTP response. */
@FunctionalInterface
public interface NotificationResponseCustomizer {

    /**
     * Customizes a notification response after the response factory runs.
     *
     * @param response current notification response
     * @param resolvedError resolved framework error
     * @param request current HTTP request
     * @return customized response
     */
    ResponseEntity<Notification> customize(
            ResponseEntity<Notification> response,
            ResolvedError resolvedError,
            HttpServletRequest request);

    /**
     * Returns customizer precedence. Lower values run first.
     *
     * @return customizer order
     */
    default int order() {
        return 0;
    }
}
