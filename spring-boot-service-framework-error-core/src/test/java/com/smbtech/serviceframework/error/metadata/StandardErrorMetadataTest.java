package com.smbtech.serviceframework.error.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.FieldViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StandardErrorMetadataTest {

    @Test
    void buildsMinimalMetadataAndOmitsAbsentValues() {
        StandardErrorMetadata metadata =
                StandardErrorMetadata.builder(ErrorCategory.INTERNAL).build();

        assertEquals(
                Map.of(
                        StandardErrorMetadataKeys.SCHEMA_VERSION, "1",
                        StandardErrorMetadataKeys.CATEGORY, "INTERNAL"),
                metadata.toMap());
        assertEquals(StandardErrorMetadata.CURRENT_SCHEMA_VERSION, metadata.schemaVersion());
        assertEquals(List.of(), metadata.violations());
    }

    @Test
    void buildsEveryStandardNamespace() {
        StandardErrorMetadata metadata =
                StandardErrorMetadata.builder(ErrorCategory.AUTHORIZATION)
                        .correlationId(" correlation-123 ")
                        .retryable(false)
                        .request(
                                new RequestErrorMetadata(
                                        "post", "/payments/{paymentId}", "createPayment"))
                        .validation(new ValidationErrorMetadata("bean_validation"))
                        .addViolation(
                                new FieldViolationMetadata(
                                        "customerId",
                                        "body",
                                        "not_blank",
                                        "Customer ID is required"))
                        .security(new SecurityErrorMetadata("insufficient_scope", "Bearer"))
                        .oauth2(
                                new OAuth2ErrorMetadata(
                                        "insufficient_scope",
                                        "The access token does not grant the required scope",
                                        "https://www.rfc-editor.org/rfc/rfc6750#section-3.1",
                                        "payment.write"))
                        .resource(new ResourceErrorMetadata("payment"))
                        .conflict(new ConflictErrorMetadata("version_conflict", "update_payment"))
                        .dependency(
                                new DependencyErrorMetadata(
                                        "payments", "authorize_payment", "timeout"))
                        .rateLimit(new RateLimitErrorMetadata(30))
                        .http(
                                new HttpErrorMetadata(
                                        "post",
                                        List.of("put", "GET", "GET"),
                                        "application/xml",
                                        List.of("application/xml", "application/json"),
                                        List.of("application/problem+json", "application/json")))
                        .build();

        Map<String, Object> values = metadata.toMap();
        assertEquals("1", values.get("schemaVersion"));
        assertEquals("AUTHORIZATION", values.get("category"));
        assertEquals("correlation-123", values.get("correlationId"));
        assertEquals(false, values.get("retryable"));
        assertEquals(
                Map.of(
                        "method",
                        "POST",
                        "route",
                        "/payments/{paymentId}",
                        "operationId",
                        "createPayment"),
                values.get("request"));
        assertEquals(Map.of("type", "bean_validation"), values.get("validation"));
        assertEquals(
                List.of(
                        Map.of(
                                "fieldName", "customerId",
                                "location", "body",
                                "code", "not_blank",
                                "message", "Customer ID is required")),
                values.get("violations"));
        assertEquals(
                Map.of("reason", "insufficient_scope", "authenticationScheme", "bearer"),
                values.get("security"));
        assertEquals(
                Map.of(
                        "error", "insufficient_scope",
                        "errorDescription", "The access token does not grant the required scope",
                        "errorUri", "https://www.rfc-editor.org/rfc/rfc6750#section-3.1",
                        "scope", "payment.write"),
                values.get("oauth2"));
        assertEquals(Map.of("type", "payment"), values.get("resource"));
        assertEquals(
                Map.of("type", "version_conflict", "operation", "update_payment"),
                values.get("conflict"));
        assertEquals(
                Map.of(
                        "name",
                        "payments",
                        "operation",
                        "authorize_payment",
                        "failureType",
                        "timeout"),
                values.get("dependency"));
        assertEquals(Map.of("retryAfterSeconds", 30L), values.get("rateLimit"));
        assertEquals(
                Map.of(
                        "method", "POST",
                        "allowedMethods", List.of("GET", "PUT"),
                        "contentType", "application/xml",
                        "supportedMediaTypes", List.of("application/json", "application/xml"),
                        "acceptableMediaTypes",
                                List.of("application/json", "application/problem+json")),
                values.get("http"));
    }

    @Test
    void producesDefensiveAndDeeplyImmutableCollections() {
        List<FieldViolationMetadata> source = new ArrayList<>();
        source.add(new FieldViolationMetadata("amount", "body", "positive", "Must be positive"));
        StandardErrorMetadataBuilder builder =
                StandardErrorMetadata.builder(ErrorCategory.VALIDATION)
                        .violations(source)
                        .http(
                                new HttpErrorMetadata(
                                        "post", List.of("GET"), "", List.of(), List.of()));
        StandardErrorMetadata metadata = builder.build();
        source.add(new FieldViolationMetadata("currency", "body", "required", "Is required"));

        assertEquals(1, metadata.violations().size());
        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        metadata.violations()
                                .add(
                                        new FieldViolationMetadata(
                                                "other", "body", "invalid", "Invalid")));

        Map<String, Object> values = metadata.toMap();
        assertThrows(UnsupportedOperationException.class, () -> values.put("other", true));
        Map<?, ?> http = assertInstanceOf(Map.class, values.get("http"));
        assertThrows(UnsupportedOperationException.class, () -> put(http, "other", true));
        List<?> allowedMethods = assertInstanceOf(List.class, http.get("allowedMethods"));
        assertThrows(UnsupportedOperationException.class, () -> add(allowedMethods, "DELETE"));
    }

    @Test
    void convertsExistingFieldViolationWithoutChangingItsContract() {
        FieldViolation source =
                new FieldViolation("customerId", "required", "Customer is required");

        FieldViolationMetadata withoutLocation = FieldViolationMetadata.from(source);
        FieldViolationMetadata withLocation = FieldViolationMetadata.from(source, "query");

        assertFalse(withoutLocation.toMap().containsKey("location"));
        assertEquals("query", withLocation.toMap().get("location"));
        assertEquals("customerId", withLocation.toMap().get("fieldName"));
    }

    @Test
    void validatesRequiredAndStructuredValues() {
        assertThrows(NullPointerException.class, () -> StandardErrorMetadata.builder(null));
        assertThrows(IllegalArgumentException.class, () -> new RequestErrorMetadata("", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new ValidationErrorMetadata(" "));
        assertThrows(
                IllegalArgumentException.class,
                () -> new OAuth2ErrorMetadata("server_error", "", "", ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new OAuth2ErrorMetadata("invalid_token", "", "relative/path", ""));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitErrorMetadata(-1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpErrorMetadata("", List.of(), "", List.of(), List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        StandardErrorMetadata.builder(ErrorCategory.VALIDATION)
                                .violations(java.util.Arrays.asList((FieldViolationMetadata) null))
                                .build());
    }

    @Test
    void builderCanProduceTheNotificationMapDirectly() {
        Map<String, Object> metadata =
                new StandardErrorMetadataBuilder(ErrorCategory.RATE_LIMIT)
                        .retryable(true)
                        .rateLimit(new RateLimitErrorMetadata(15))
                        .buildMap();

        assertEquals("RATE_LIMIT", metadata.get("category"));
        assertEquals(true, metadata.get("retryable"));
        assertTrue(metadata.containsKey("rateLimit"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void put(Map<?, ?> values, String key, Object value) {
        ((Map) values).put(key, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void add(List<?> values, Object value) {
        ((List) values).add(value);
    }
}
