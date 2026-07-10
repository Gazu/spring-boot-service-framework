package com.smbtech.serviceframework.commons.notification;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifyingExceptionTest {

    @Test
    void usesPrimaryNotificationMessageAsExceptionMessage() {
        Notification notification = Notification.error(
                "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0500",
                "Internal Server Error received from HTTP client"
        );

        NotifyingException exception = new NotifyingException(notification);

        assertEquals("Internal Server Error received from HTTP client", exception.getMessage());
        assertEquals(List.of(notification), exception.notifications());
        assertEquals(notification, exception.primaryNotification().orElseThrow());
    }

    @Test
    void preservesExplicitMessageAndCause() {
        RuntimeException cause = new RuntimeException("backend failed");
        Notification notification = Notification.error("E_SERVICE_FRAMEWORK_0001", "Failure");

        NotifyingException exception = new NotifyingException(List.of(notification), "HTTP client failed", cause);

        assertEquals("HTTP client failed", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals(notification, exception.primaryNotification().orElseThrow());
    }

    @Test
    void notificationsAreImmutableAndDefensivelyCopied() {
        Notification notification = Notification.error("E_SERVICE_FRAMEWORK_0001", "Failure");
        List<Notification> notifications = new ArrayList<>();
        notifications.add(notification);

        NotifyingException exception = new NotifyingException(notifications);
        notifications.clear();

        assertEquals(List.of(notification), exception.notifications());
        assertThrows(UnsupportedOperationException.class, () -> exception.notifications().add(notification));
    }

    @Test
    void canRepresentPlainExceptionWithoutNotifications() {
        NotifyingException exception = new NotifyingException("Plain failure");

        assertEquals("Plain failure", exception.getMessage());
        assertTrue(exception.notifications().isEmpty());
        assertTrue(exception.primaryNotification().isEmpty());
    }
}
