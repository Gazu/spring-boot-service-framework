package com.smbtech.serviceframework.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.metadata.OAuth2ErrorMetadata;
import com.smbtech.serviceframework.error.metadata.SecurityErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultNotificationSanitizerTest {

    @Test
    void appliesDefaultMetadataAllowlistWithoutMutatingTheNotification() {
        Notification source =
                Notification.builder()
                        .code("E_REQUEST_0001")
                        .message("Request failed")
                        .metadata(
                                Map.of(
                                        "correlationId", "correlation-123",
                                        "path", "/customers/123",
                                        "clientName", "customers"))
                        .build();

        Notification sanitized = new DefaultNotificationSanitizer().sanitize(source);

        assertEquals(
                Map.of("correlationId", "correlation-123", "path", "/customers/123"),
                sanitized.metadata());
        assertEquals("customers", source.metadata().get("clientName"));
        assertEquals(source.id(), sanitized.id());
        assertEquals(source.timestamp(), sanitized.timestamp());
        assertNotSame(source, sanitized);
    }

    @Test
    void allowsCanonicalMetadataNamespacesAndStillRejectsUnknownTopLevelValues() {
        Map<String, Object> metadata =
                new LinkedHashMap<>(
                        StandardErrorMetadata.builder(ErrorCategory.AUTHENTICATION)
                                .retryable(false)
                                .security(new SecurityErrorMetadata("invalid_token", "bearer"))
                                .oauth2(
                                        new OAuth2ErrorMetadata(
                                                "invalid_token",
                                                "The access token is invalid",
                                                "https://www.rfc-editor.org/rfc/rfc6750#section-3.1",
                                                ""))
                                .buildMap());
        metadata.put("internalDiagnostic", "password=must-not-leak");
        Notification source =
                Notification.builder()
                        .code("E_AUTH_0001")
                        .message("Authentication failed")
                        .metadata(metadata)
                        .build();

        Notification sanitized = new DefaultNotificationSanitizer().sanitize(source);

        assertEquals("1", sanitized.metadata().get("schemaVersion"));
        assertEquals("AUTHENTICATION", sanitized.metadata().get("category"));
        assertEquals(false, sanitized.metadata().get("retryable"));
        assertEquals(
                Map.of("reason", "invalid_token", "authenticationScheme", "bearer"),
                sanitized.metadata().get("security"));
        assertFalse(sanitized.metadata().containsKey("internalDiagnostic"));
        assertFalse(sanitized.metadata().toString().contains("must-not-leak"));
    }

    @Test
    void redactsSensitiveHeadersBodiesTokensPasswordsAndCauses() {
        Set<String> allowlist =
                Set.of(
                        "Authorization",
                        "requestHeaders",
                        "responseBody",
                        "password",
                        "cause",
                        "context");
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("access_token", "access-token-value");
        context.put("sessionId", "private-session-id");
        context.put("cookie", "private-cookie");
        context.put("claims", Map.of("sub", "private-subject", "customer_id", "private-customer"));
        context.put("safe", "Bearer header.payload.signature");
        context.put("nested", Map.of("clientSecret", "secret-value", "status", 401));
        Notification source =
                Notification.builder()
                        .code("E_AUTH_0001")
                        .message("Authorization: Bearer abc.def.ghi password=plain-text")
                        .metadata(
                                Map.of(
                                        "Authorization",
                                        "Bearer eyJhbGciOiJSUzI1NiJ9.payload.signature",
                                        "requestHeaders",
                                        Map.of("X-Correlation-Id", "123"),
                                        "responseBody",
                                        "{\"access_token\":\"secret\"}",
                                        "password",
                                        "plain-text",
                                        "cause",
                                        new IllegalStateException("database password=secret"),
                                        "context",
                                        context))
                        .build();

        Notification sanitized = new DefaultNotificationSanitizer(allowlist).sanitize(source);

        assertEquals(
                DefaultNotificationSanitizer.REDACTED_VALUE,
                sanitized.metadata().get("Authorization"));
        assertEquals(
                DefaultNotificationSanitizer.REDACTED_VALUE,
                sanitized.metadata().get("requestHeaders"));
        assertEquals(
                DefaultNotificationSanitizer.REDACTED_VALUE,
                sanitized.metadata().get("responseBody"));
        assertEquals(
                DefaultNotificationSanitizer.REDACTED_VALUE, sanitized.metadata().get("password"));
        assertEquals(
                DefaultNotificationSanitizer.REDACTED_VALUE, sanitized.metadata().get("cause"));
        assertFalse(sanitized.message().contains("plain-text"));

        Map<?, ?> sanitizedContext = (Map<?, ?>) sanitized.metadata().get("context");
        assertEquals(
                DefaultNotificationSanitizer.REDACTED_VALUE, sanitizedContext.get("access_token"));
        assertEquals(
                DefaultNotificationSanitizer.REDACTED_VALUE, sanitizedContext.get("sessionId"));
        assertEquals(DefaultNotificationSanitizer.REDACTED_VALUE, sanitizedContext.get("cookie"));
        assertEquals(DefaultNotificationSanitizer.REDACTED_VALUE, sanitizedContext.get("claims"));
        assertEquals("Bearer <redacted>", sanitizedContext.get("safe"));
        Map<?, ?> nested = (Map<?, ?>) sanitizedContext.get("nested");
        assertEquals(DefaultNotificationSanitizer.REDACTED_VALUE, nested.get("clientSecret"));
        assertEquals(401, nested.get("status"));
    }

    @Test
    void sanitizesFieldViolationMessagesAndPreservesInternalDiagnostics() {
        Notification notification = Notification.error("E_REQUEST_0001", "Validation failed");
        FieldViolation violation =
                new FieldViolation(
                        "accessToken", "invalid", "Bearer secret-token-value is invalid");
        ResolvedError source =
                new ResolvedError(
                        notification,
                        ErrorCategory.VALIDATION,
                        ErrorExposure.PUBLIC,
                        "Internal cause includes password=diagnostic-secret",
                        List.of(violation));

        ResolvedError sanitized = new DefaultNotificationSanitizer().sanitize(source);

        assertEquals(
                "Bearer <redacted> is invalid", sanitized.fieldViolations().getFirst().message());
        assertEquals(source.diagnosticMessage(), sanitized.diagnosticMessage());
        assertSame(source.category(), sanitized.category());
    }

    @Test
    void protectsAgainstCyclesAndUnsupportedMetadataObjects() {
        Map<String, Object> cyclic = new LinkedHashMap<>();
        cyclic.put("self", cyclic);
        cyclic.put("unsupported", new Object());
        Notification source =
                Notification.builder()
                        .code("E_INTERNAL_0001")
                        .metadata(Map.of("context", cyclic))
                        .build();

        Notification sanitized =
                new DefaultNotificationSanitizer(Set.of("context")).sanitize(source);

        Map<?, ?> context = (Map<?, ?>) sanitized.metadata().get("context");
        assertEquals(DefaultNotificationSanitizer.REDACTED_VALUE, context.get("self"));
        assertEquals(DefaultNotificationSanitizer.REDACTED_VALUE, context.get("unsupported"));
    }

    @Test
    void sanitizesCollectionsAndUsesCaseInsensitiveAllowlistMatching() {
        List<Object> values = new ArrayList<>();
        values.add("safe");
        values.add(Map.of("password", "secret", "status", true));
        Notification source =
                Notification.builder()
                        .code("E_INTERNAL_0001")
                        .metadata(Map.of("Context", values))
                        .build();
        DefaultNotificationSanitizer sanitizer =
                new DefaultNotificationSanitizer(Set.of("context"));

        Notification sanitized = sanitizer.sanitize(source);
        List<?> context = (List<?>) sanitized.metadata().get("Context");

        assertEquals("safe", context.get(0));
        assertEquals(
                DefaultNotificationSanitizer.REDACTED_VALUE,
                ((Map<?, ?>) context.get(1)).get("password"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> sanitizer.metadataAllowlist().add("other"));
    }

    @Test
    void recursivelySanitizesMixedMapsCollectionsAndArrays() {
        Instant timestamp = Instant.parse("2026-07-19T12:00:00Z");
        UUID requestId = UUID.randomUUID();
        Object[] values = {
            Map.of(
                    "requestToken",
                    "secret-token",
                    "details",
                    List.of(Map.of("statusCode", 401, "authorization", "Bearer abc.def.ghi"))),
            new int[] {200, 401},
            timestamp,
            requestId
        };
        Notification source =
                Notification.builder()
                        .code("E_AUTH_0002")
                        .message("Authentication failed")
                        .metadata(Map.of("context", values))
                        .build();

        Notification sanitized =
                new DefaultNotificationSanitizer(Set.of("context")).sanitize(source);

        List<?> context = (List<?>) sanitized.metadata().get("context");
        Map<?, ?> first = (Map<?, ?>) context.get(0);
        assertEquals(DefaultNotificationSanitizer.REDACTED_VALUE, first.get("requestToken"));
        Map<?, ?> details = (Map<?, ?>) ((List<?>) first.get("details")).getFirst();
        assertEquals(401, details.get("statusCode"));
        assertEquals(DefaultNotificationSanitizer.REDACTED_VALUE, details.get("authorization"));
        assertEquals(List.of(200, 401), context.get(1));
        assertSame(timestamp, context.get(2));
        assertSame(requestId, context.get(3));
        assertThrows(UnsupportedOperationException.class, () -> context.add(null));
        assertThrows(UnsupportedOperationException.class, () -> first.put(null, null));
    }

    @Test
    void rejectsInvalidAllowlistAndArguments() {
        assertThrows(NullPointerException.class, () -> new DefaultNotificationSanitizer(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultNotificationSanitizer(Set.of(" ")));
        assertThrows(
                NullPointerException.class,
                () -> new DefaultNotificationSanitizer().sanitize((Notification) null));
        assertThrows(
                NullPointerException.class,
                () -> new DefaultNotificationSanitizer().sanitize((ResolvedError) null));
    }
}
