package com.smbtech.serviceframework.starter.errorhandling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.commons.notification.NotifyingException;
import org.junit.jupiter.api.Test;

class CommonsNotificationTransitivityTest {

    @Test
    void exposesCommonsNotificationTypesThroughErrorCore() {
        Notification notification =
                Notification.warning("W_SERVICE_FRAMEWORK_ERROR_0001", "Error handling warning");
        NotifyingException exception = new NotifyingException(notification);

        assertEquals(NotificationSeverity.WARNING, notification.severity());
        assertEquals(notification, exception.primaryNotification().orElseThrow());
    }
}
