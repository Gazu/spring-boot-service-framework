package com.smbtech.serviceframework.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.commons.notification.NotifyingException;
import org.junit.jupiter.api.Test;

class CommonsNotificationReuseTest {

    @Test
    void usesNotificationTypesFromCommons() {
        Notification notification =
                Notification.builder()
                        .code("E_SERVICE_FRAMEWORK_ERROR_0001")
                        .message("Error handling failure")
                        .build();

        NotifyingException exception = new NotifyingException(notification);

        assertEquals(NotificationSeverity.ERROR, notification.severity());
        assertSame(notification, exception.primaryNotification().orElseThrow());
    }
}
