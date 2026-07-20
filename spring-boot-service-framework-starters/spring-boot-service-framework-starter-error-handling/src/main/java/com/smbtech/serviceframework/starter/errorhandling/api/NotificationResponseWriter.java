package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.commons.notification.Notification;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.ResponseEntity;

/** Writes a notification response outside the normal Spring MVC return-value flow. */
@FunctionalInterface
public interface NotificationResponseWriter {

    /**
     * Writes a response entity to the servlet response.
     *
     * @param responseEntity notification response entity
     * @param servletResponse servlet response
     * @throws IOException when the response cannot be written
     */
    void write(ResponseEntity<Notification> responseEntity, HttpServletResponse servletResponse)
            throws IOException;
}
