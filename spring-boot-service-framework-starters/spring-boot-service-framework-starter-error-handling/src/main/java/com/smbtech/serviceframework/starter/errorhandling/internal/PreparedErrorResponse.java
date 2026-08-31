package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ResolvedError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.http.ResponseEntity;

/**
 * Immutable result of preparing an error response before it is written by an adapter.
 *
 * @param cause original request failure
 * @param resolvedError final resolved error
 * @param request current request
 * @param response final notification response
 */
record PreparedErrorResponse(
        Throwable cause,
        ResolvedError resolvedError,
        HttpServletRequest request,
        ResponseEntity<Notification> response) {

    /** Creates and validates a prepared error response. */
    PreparedErrorResponse {
        cause = Objects.requireNonNull(cause, "cause must not be null");
        resolvedError = Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        request = Objects.requireNonNull(request, "request must not be null");
        response = Objects.requireNonNull(response, "response must not be null");
    }

    /**
     * Returns the final HTTP status.
     *
     * @return HTTP status code
     */
    int statusCode() {
        return response.getStatusCode().value();
    }
}
