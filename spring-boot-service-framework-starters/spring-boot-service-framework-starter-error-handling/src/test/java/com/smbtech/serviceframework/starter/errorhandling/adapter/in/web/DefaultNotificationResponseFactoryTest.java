package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

class DefaultNotificationResponseFactoryTest {

    @Test
    void createsMinimalPublicResponseAndOmitsFieldViolations() {
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
        assertEquals("E_REQUEST_0001", body.code());
        assertEquals(FallbackThrowableErrorResolver.DEFAULT_PUBLIC_MESSAGE, body.message());
        assertEquals(NotificationSeverity.ERROR, body.severity());
        assertEquals("", body.fieldName());
        assertEquals(
                Map.of("category", "VALIDATION", "correlationId", "correlation-123"),
                body.metadata());
    }

    @Test
    void createsDetailedSanitizedInternalNotification() {
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

        DefaultNotificationSanitizer permissiveInternalSanitizer =
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
                                permissiveInternalSanitizer)
                        .create(error);

        Notification body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(source.code(), body.code());
        assertEquals(source.message(), body.message());
        assertEquals(NotificationSeverity.WARNING, body.severity());
        assertEquals("password", body.fieldName());
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
                        "operationId", "createCustomer",
                        "headers", DefaultNotificationSanitizer.REDACTED_VALUE),
                body.metadata().get("request"));
        assertEquals("application-detail", body.metadata().get("custom"));
        assertEquals(
                List.of(
                        Map.of(
                                "fieldName",
                                "password",
                                "code",
                                "invalid",
                                "message",
                                "Database password is invalid")),
                body.metadata().get("violations"));
    }

    @Test
    void invokesReplaceablePoliciesWithoutAllowingPublicContractOverride() {
        AtomicBoolean statusInvoked = new AtomicBoolean();
        AtomicBoolean sanitizerInvoked = new AtomicBoolean();
        Notification replacement =
                Notification.builder()
                        .code("W_CUSTOM_0001")
                        .message("Custom response")
                        .severity(NotificationSeverity.WARNING)
                        .metadata(
                                Map.of(
                                        "category",
                                        "FORGED",
                                        "correlationId",
                                        "forged-correlation",
                                        "diagnosticMessage",
                                        "must-not-travel"))
                        .build();
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
        Notification source =
                Notification.builder()
                        .code("E_ORIGINAL")
                        .message("Original")
                        .metadata(Map.of("correlationId", "original-correlation"))
                        .build();
        ResolvedError error =
                new ResolvedError(
                        source, ErrorCategory.INTERNAL, ErrorExposure.PUBLIC, "Diagnostic");

        ResponseEntity<Notification> response = factory.create(error);

        assertTrue(statusInvoked.get());
        assertTrue(sanitizerInvoked.get());
        assertNotNull(response.getBody());
        assertEquals("E_ORIGINAL", response.getBody().code());
        assertEquals(
                FallbackThrowableErrorResolver.DEFAULT_PUBLIC_MESSAGE,
                response.getBody().message());
        assertEquals(
                Map.of("category", "INTERNAL", "correlationId", "original-correlation"),
                response.getBody().metadata());
        assertEquals(source.id(), response.getBody().id());
        assertEquals(source.timestamp(), response.getBody().timestamp());
        assertEquals(customStatus, response.getStatusCode());
        assertSame(statusResolver, factory.statusResolver());
        assertSame(sanitizer, factory.notificationSanitizer());
    }

    @Test
    void enforcesInternalIdentityAndFrameworkMetadataAfterCustomSanitization() {
        UUID sourceId = UUID.fromString("c22ef00b-bd65-430b-bccc-f57c8a422b1d");
        Instant sourceTimestamp = Instant.parse("2026-07-21T01:30:00Z");
        Notification source =
                new Notification(
                        "E_ORIGINAL_0001",
                        "Bearer secret-token-value is invalid",
                        NotificationSeverity.WARNING,
                        "token=field-secret",
                        Map.of("correlationId", "original-correlation"),
                        sourceId,
                        sourceTimestamp);
        ResolvedError error =
                new ResolvedError(
                        source,
                        ErrorCategory.VALIDATION,
                        ErrorExposure.INTERNAL,
                        "diagnostic-secret",
                        List.of(
                                new FieldViolation(
                                        "accessToken", "invalid", "password=violation-secret")));
        NotificationSanitizer sanitizer =
                notification ->
                        new Notification(
                                "W_FORGED_0001",
                                notification.message(),
                                NotificationSeverity.INFO,
                                "forgedField",
                                Map.of(
                                        "schemaVersion",
                                        "forged",
                                        "category",
                                        "FORGED",
                                        "correlationId",
                                        "forged-correlation",
                                        "violations",
                                        List.of(Map.of("message", "forged violation")),
                                        "diagnostic_message",
                                        "must-not-travel"),
                                UUID.fromString("884f653e-8107-47c3-863f-4e25c56ca75b"),
                                Instant.parse("2030-01-01T00:00:00Z"));

        Notification body =
                new DefaultNotificationResponseFactory(
                                new DefaultNotificationHttpStatusResolver(), sanitizer)
                        .create(error)
                        .getBody();

        assertNotNull(body);
        assertEquals(source.code(), body.code());
        assertEquals("Bearer <redacted> is invalid", body.message());
        assertEquals(source.severity(), body.severity());
        assertEquals("token=<redacted>", body.fieldName());
        assertEquals(sourceId, body.id());
        assertEquals(sourceTimestamp, body.timestamp());
        assertEquals("1", body.metadata().get("schemaVersion"));
        assertEquals("VALIDATION", body.metadata().get("category"));
        assertEquals("original-correlation", body.metadata().get("correlationId"));
        assertEquals(
                List.of(
                        Map.of(
                                "fieldName",
                                "accessToken",
                                "code",
                                "invalid",
                                "message",
                                "password=<redacted>")),
                body.metadata().get("violations"));
        assertFalse(body.metadata().containsKey("diagnostic_message"));
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
        assertEquals("", json.at("/field_name").asText());
        assertEquals(
                FallbackThrowableErrorResolver.DEFAULT_PUBLIC_MESSAGE,
                json.at("/message").asText());
        assertEquals("VALIDATION", json.at("/metadata/category").asText());
        assertEquals("correlation-123", json.at("/metadata/correlation_id").asText());
        assertFalse(json.at("/metadata").has("request_context"));
        assertFalse(json.has("fieldName"));
        assertFalse(json.at("/metadata").has("correlationId"));
        assertFalse(serialized.contains("public-message-secret"));
        assertFalse(serialized.contains("nested-public-secret"));
        assertFalse(serialized.contains("diagnostic-secret"));
    }

    @Test
    void internalResponseOmitsFieldViolationsWhenFactoryOptionIsDisabled() {
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
                        ErrorExposure.INTERNAL,
                        "Validation diagnostic",
                        List.of(new FieldViolation("email", "invalid", "Email is invalid")));
        DefaultNotificationResponseFactory factory =
                new DefaultNotificationResponseFactory(
                        new DefaultNotificationHttpStatusResolver(),
                        notification -> notification,
                        false);

        ResponseEntity<Notification> response = factory.create(error);

        assertNotNull(response.getBody());
        assertEquals(source.code(), response.getBody().code());
        assertEquals(source.message(), response.getBody().message());
        assertEquals(
                Map.of(
                        "schemaVersion",
                        "1",
                        "category",
                        "VALIDATION",
                        "correlationId",
                        "correlation-456"),
                response.getBody().metadata());
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
        return new ObjectMapper().rebuild().addModule(module).build();
    }
}
