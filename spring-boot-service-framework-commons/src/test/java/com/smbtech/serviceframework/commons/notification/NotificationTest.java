package com.smbtech.serviceframework.commons.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    void createsImmutableNotificationWithDefaults() {
        Notification notification =
                Notification.builder()
                        .code("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0400")
                        .message("Bad Request received from HTTP client")
                        .metadataEntry("clientName", "dummy")
                        .build();

        assertEquals("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0400", notification.code());
        assertEquals("Bad Request received from HTTP client", notification.message());
        assertEquals(NotificationSeverity.ERROR, notification.severity());
        assertEquals("", notification.fieldName());
        assertEquals("dummy", notification.metadata().get("clientName"));
        assertNotNull(notification.id());
        assertNotNull(notification.timestamp());
        assertThrows(
                UnsupportedOperationException.class,
                () -> notification.metadata().put("other", "value"));
    }

    @Test
    void allowsExplicitValues() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-07-07T12:00:00Z");

        Notification notification =
                Notification.builder()
                        .code("W_SERVICE_FRAMEWORK_VALIDATION_0001")
                        .message("Optional field is deprecated")
                        .severity(NotificationSeverity.WARNING)
                        .fieldName("legacyField")
                        .id(id)
                        .timestamp(timestamp)
                        .build();

        assertEquals(NotificationSeverity.WARNING, notification.severity());
        assertEquals("legacyField", notification.fieldName());
        assertEquals(id, notification.id());
        assertEquals(timestamp, notification.timestamp());
    }

    @Test
    void copiesMetadataDefensively() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("clientName", "catalog");

        Notification notification =
                Notification.builder()
                        .code("I_SERVICE_FRAMEWORK_HTTP_CLIENT_0001")
                        .metadata(metadata)
                        .build();

        metadata.put("clientName", "changed");

        assertEquals("catalog", notification.metadata().get("clientName"));
    }

    @Test
    void rejectsInvalidNotificationContent() {
        assertThrows(
                IllegalArgumentException.class, () -> Notification.builder().code(" ").build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Notification.builder()
                                .code("E_SERVICE_FRAMEWORK_0001")
                                .metadataEntry("", "value")
                                .build());
        assertThrows(
                NullPointerException.class,
                () ->
                        Notification.builder()
                                .code("E_SERVICE_FRAMEWORK_0001")
                                .metadataEntry("key", null)
                                .build());
    }

    @Test
    void exposesConvenienceFactories() {
        Notification error = Notification.error("E_SERVICE_FRAMEWORK_0001", "error");
        Notification warning = Notification.warning("W_SERVICE_FRAMEWORK_0001", "warning");
        Notification info = Notification.info("I_SERVICE_FRAMEWORK_0001", "info");

        assertEquals(NotificationSeverity.ERROR, error.severity());
        assertEquals("E_SERVICE_FRAMEWORK_0001", error.code());
        assertEquals("error", error.message());
        assertEquals(NotificationSeverity.WARNING, warning.severity());
        assertEquals("warning", warning.message());
        assertEquals(NotificationSeverity.INFO, info.severity());
        assertEquals("info", info.message());
        assertEquals("", error.fieldName());
        assertEquals(Map.of(), error.metadata());
    }
}
