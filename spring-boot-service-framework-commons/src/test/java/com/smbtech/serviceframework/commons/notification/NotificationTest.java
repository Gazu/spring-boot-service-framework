package com.smbtech.serviceframework.commons.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    @SuppressWarnings("unchecked")
    void recursivelyCopiesStructuredMetadata() {
        List<Object> roles = new ArrayList<>(List.of("reader"));
        Map<String, Object> security = new LinkedHashMap<>();
        security.put("roles", roles);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("security", security);

        Notification notification =
                Notification.builder().code("E_SECURITY_0001").metadata(metadata).build();
        roles.add("writer");
        security.put("scheme", "bearer");

        Map<String, Object> immutableSecurity =
                (Map<String, Object>) notification.metadata().get("security");
        List<Object> immutableRoles = (List<Object>) immutableSecurity.get("roles");
        assertEquals(List.of("reader"), immutableRoles);
        assertThrows(
                UnsupportedOperationException.class,
                () -> immutableSecurity.put("scheme", "bearer"));
        assertThrows(UnsupportedOperationException.class, () -> immutableRoles.add("writer"));
    }

    @Test
    void preservesIdentityWhenReplacingMetadata() {
        Notification source = Notification.error("E_SERVICE_0001", "Failure");

        Notification updated = source.withMetadata(Map.of("category", "INTERNAL"));

        assertEquals(source.code(), updated.code());
        assertEquals(source.id(), updated.id());
        assertEquals(source.timestamp(), updated.timestamp());
        assertEquals(Map.of("category", "INTERNAL"), updated.metadata());
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
