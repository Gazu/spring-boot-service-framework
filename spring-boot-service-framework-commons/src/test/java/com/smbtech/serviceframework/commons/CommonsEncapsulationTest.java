package com.smbtech.serviceframework.commons;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.commons.notification.NotifyingException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class CommonsEncapsulationTest {

    @Test
    void exposesOnlyDocumentedNotificationContracts() throws ClassNotFoundException {
        assertTrue(Modifier.isPublic(Notification.class.getModifiers()));
        assertTrue(Modifier.isPublic(NotificationSeverity.class.getModifiers()));
        assertTrue(Modifier.isPublic(NotifyingException.class.getModifiers()));
        assertFalse(
                Modifier.isPublic(
                        Class.forName(
                                        "com.smbtech.serviceframework.commons.notification.ImmutableNotificationMetadata")
                                .getModifiers()));
    }
}
