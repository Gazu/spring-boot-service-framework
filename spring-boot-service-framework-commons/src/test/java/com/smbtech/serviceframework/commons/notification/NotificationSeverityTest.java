package com.smbtech.serviceframework.commons.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NotificationSeverityTest {

    @Test
    void infersSeverityFromCodePrefix() {
        assertEquals(
                NotificationSeverity.ERROR,
                NotificationSeverity.fromCode("E_SERVICE_FRAMEWORK_0001"));
        assertEquals(
                NotificationSeverity.WARNING,
                NotificationSeverity.fromCode("w_service_framework_0001"));
        assertEquals(
                NotificationSeverity.INFO,
                NotificationSeverity.fromCode("I_SERVICE_FRAMEWORK_0001"));
    }

    @Test
    void returnsUnspecifiedForUnknownOrMissingPrefix() {
        assertEquals(
                NotificationSeverity.UNSPECIFIED,
                NotificationSeverity.fromCode("X_SERVICE_FRAMEWORK_0001"));
        assertEquals(NotificationSeverity.UNSPECIFIED, NotificationSeverity.fromCode(""));
        assertEquals(NotificationSeverity.UNSPECIFIED, NotificationSeverity.fromCode(null));
    }
}
