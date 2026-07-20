package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.DefaultNotificationSanitizer;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FallbackThrowableErrorResolver;
import com.smbtech.serviceframework.error.FieldViolation;
import com.smbtech.serviceframework.error.NotificationSanitizer;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataKeys;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationHttpStatusResolver;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonSerializer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class DefaultNotificationResponseFactoryTest {

    @Test
    void createsSanitizedResponseAndAddsFieldViolations() throws Exception {
        Notification source =
                Notification.builder()
                        .code("E_REQUEST_0001")
                        .message("Request validation failed")
                        .metadata(
                                Map.of(
                                        "correlationId", "correlation-123",
                                        "password", "must-not-leak"))
                        .build();
        ResolvedError error =
                new ResolvedError(
                        source,
                        ErrorCategory.VALIDATION,
                        ErrorExposure.PUBLIC,
                        "Internal validation details",
                        List.of(
                                new FieldViolation(
                                        "customerId", "required", "customerId is required")));

        ResponseEntity<Notification> response =
                new DefaultNotificationResponseFactory().create(error);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        Notification body = response.getBody();
        assertNotNull(body);
        assertEquals("correlation-123", body.metadata().get("correlationId"));
        assertFalse(body.metadata().containsKey("password"));

        JsonNode json = notificationMapper().valueToTree(body);
        assertEquals("customerId", json.at("/metadata/violations/0/field_name").asText());
        assertEquals("required", json.at("/metadata/violations/0/code").asText());
        assertFalse(json.has("fieldName"));
    }

    @Test
    void replacesInternalNotificationAndOmitsFieldViolations() {
        Notification source =
                Notification.builder()
                        .code("E_DATABASE_0001")
                        .message("Database password is invalid")
                        .severity(NotificationSeverity.WARNING)
                        .fieldName("password")
                        .metadata(
                                Map.of(
                                        "schemaVersion",
                                        "untrusted-version",
                                        "category",
                                        "DOWNSTREAM",
                                        "correlationId",
                                        "correlation-123",
                                        "retryable",
                                        true,
                                        "request",
                                        Map.of(
                                                "method",
                                                "POST",
                                                "route",
                                                "/customers",
                                                "operationId",
                                                "createCustomer",
                                                "headers",
                                                Map.of("Authorization", "Bearer secret")),
                                        "custom",
                                        "application-detail",
                                        "violations",
                                        List.of(Map.of("fieldName", "password"))))
                        .build();
        ResolvedError error =
                new ResolvedError(
                        source,
                        ErrorCategory.INTERNAL,
                        ErrorExposure.INTERNAL,
                        "Database connection failed",
                        List.of(
                                new FieldViolation(
                                        "password", "invalid", "Database password is invalid")));

        DefaultNotificationSanitizer permissivePublicSanitizer =
                new DefaultNotificationSanitizer(
                        Set.of(
                                "schemaVersion",
                                "category",
                                "correlationId",
                                "retryable",
                                "request",
                                "custom",
                                "violations"));
        ResponseEntity<Notification> response =
                new DefaultNotificationResponseFactory(
                                new DefaultNotificationHttpStatusResolver(),
                                permissivePublicSanitizer)
                        .create(error);

        Notification body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(FallbackThrowableErrorResolver.DEFAULT_ERROR_CODE, body.code());
        assertEquals(FallbackThrowableErrorResolver.DEFAULT_PUBLIC_MESSAGE, body.message());
        assertEquals(NotificationSeverity.ERROR, body.severity());
        assertEquals("", body.fieldName());
        assertEquals(source.id(), body.id());
        assertEquals(source.timestamp(), body.timestamp());
        assertEquals("1", body.metadata().get(StandardErrorMetadataKeys.SCHEMA_VERSION));
        assertEquals("INTERNAL", body.metadata().get(StandardErrorMetadataKeys.CATEGORY));
        assertEquals("correlation-123", body.metadata().get("correlationId"));
        assertEquals(true, body.metadata().get("retryable"));
        assertEquals(
                Map.of(
                        "method", "POST",
                        "route", "/customers",
                        "operationId", "createCustomer"),
                body.metadata().get("request"));
        assertFalse(body.metadata().containsKey("custom"));
        assertFalse(body.metadata().containsKey("violations"));
    }

    @Test
    void usesReplaceableStatusAndSanitizationPolicies() {
        AtomicBoolean statusInvoked = new AtomicBoolean();
        AtomicBoolean sanitizerInvoked = new AtomicBoolean();
        Notification replacement = Notification.warning("W_CUSTOM_0001", "Custom response");
        HttpStatusCode customStatus = HttpStatusCode.valueOf(599);
        NotificationHttpStatusResolver statusResolver =
                error -> {
                    statusInvoked.set(true);
                    return customStatus;
                };
        NotificationSanitizer sanitizer =
                notification -> {
                    sanitizerInvoked.set(true);
                    return replacement;
                };
        DefaultNotificationResponseFactory factory =
                new DefaultNotificationResponseFactory(statusResolver, sanitizer);
        ResolvedError error =
                new ResolvedError(
                        Notification.error("E_ORIGINAL", "Original"),
                        ErrorCategory.INTERNAL,
                        ErrorExposure.PUBLIC,
                        "Diagnostic");

        ResponseEntity<Notification> response = factory.create(error);

        assertTrue(statusInvoked.get());
        assertTrue(sanitizerInvoked.get());
        assertEquals(replacement, response.getBody());
        assertEquals(customStatus, response.getStatusCode());
        assertSame(statusResolver, factory.statusResolver());
        assertSame(sanitizer, factory.notificationSanitizer());
    }

    @Test
    void alwaysRedactsAndSerializesPublicNotificationsAsSnakeCase() throws Exception {
        Notification source =
                Notification.builder()
                        .code("E_PUBLIC_0001")
                        .message("authorization=Bearer public-message-secret")
                        .fieldName("customerId")
                        .metadata(
                                Map.of(
                                        "correlationId",
                                        "correlation-123",
                                        "requestContext",
                                        Map.of(
                                                "operationId",
                                                "createCustomer",
                                                "clientSecret",
                                                "nested-public-secret")))
                        .build();
        ResolvedError error =
                new ResolvedError(
                        source,
                        ErrorCategory.VALIDATION,
                        ErrorExposure.PUBLIC,
                        "diagnostic-secret");
        DefaultNotificationResponseFactory factory =
                new DefaultNotificationResponseFactory(
                        new DefaultNotificationHttpStatusResolver(), notification -> notification);

        Notification body = factory.create(error).getBody();

        assertNotNull(body);
        JsonNode json = notificationMapper().valueToTree(body);
        String serialized = json.toString();
        assertEquals("customerId", json.at("/field_name").asText());
        assertEquals("correlation-123", json.at("/metadata/correlation_id").asText());
        assertEquals("createCustomer", json.at("/metadata/request_context/operation_id").asText());
        assertEquals(
                DefaultNotificationSanitizer.REDACTED_VALUE,
                json.at("/metadata/request_context/client_secret").asText());
        assertFalse(json.has("fieldName"));
        assertFalse(json.at("/metadata").has("correlationId"));
        assertFalse(serialized.contains("public-message-secret"));
        assertFalse(serialized.contains("nested-public-secret"));
        assertFalse(serialized.contains("diagnostic-secret"));
    }

    @Test
    void omitsFieldViolationsWhenFactoryOptionIsDisabled() {
        Notification source =
                Notification.builder()
                        .code("E_REQUEST_0002")
                        .message("Request validation failed")
                        .metadata(Map.of("correlationId", "correlation-456"))
                        .build();
        ResolvedError error =
                new ResolvedError(
                        source,
                        ErrorCategory.VALIDATION,
                        ErrorExposure.PUBLIC,
                        "Validation diagnostic",
                        List.of(new FieldViolation("email", "invalid", "Email is invalid")));
        DefaultNotificationResponseFactory factory =
                new DefaultNotificationResponseFactory(
                        new DefaultNotificationHttpStatusResolver(),
                        notification -> notification,
                        false);

        ResponseEntity<Notification> response = factory.create(error);

        assertEquals(source, response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().metadata().containsKey("violations"));
    }

    @Test
    void rejectsInvalidFactoryDependenciesAndResults() {
        NotificationHttpStatusResolver statusResolver = error -> HttpStatus.BAD_REQUEST;
        NotificationSanitizer sanitizer = notification -> notification;
        ResolvedError error =
                new ResolvedError(
                        Notification.error("E_TEST", "Failure"),
                        ErrorCategory.VALIDATION,
                        ErrorExposure.PUBLIC,
                        "Diagnostic");

        assertThrows(
                NullPointerException.class,
                () -> new DefaultNotificationResponseFactory(null, sanitizer));
        assertThrows(
                NullPointerException.class,
                () -> new DefaultNotificationResponseFactory(statusResolver, null));
        assertThrows(
                NullPointerException.class,
                () -> new DefaultNotificationResponseFactory().create(null));
        assertThrows(
                NullPointerException.class,
                () ->
                        new DefaultNotificationResponseFactory(errorValue -> null, sanitizer)
                                .create(error));
        assertThrows(
                NullPointerException.class,
                () ->
                        new DefaultNotificationResponseFactory(statusResolver, notification -> null)
                                .create(error));
    }

    private static ObjectMapper notificationMapper() {
        SimpleModule module = new SimpleModule("service-framework-notification-json");
        module.addSerializer(Notification.class, new NotificationJsonSerializer());
        return new ObjectMapper().registerModule(module);
    }
}
