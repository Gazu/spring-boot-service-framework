package com.smbtech.serviceframework.starter.errorhandling.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.metadata.OAuth2ErrorMetadata;
import com.smbtech.serviceframework.error.metadata.RateLimitErrorMetadata;
import com.smbtech.serviceframework.error.metadata.RequestErrorMetadata;
import com.smbtech.serviceframework.error.metadata.SecurityErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

class NotificationJsonSerializerTest {

    @Test
    void serializesStandardErrorMetadataWithThePublicSnakeCaseContract() throws Exception {
        Map<String, Object> metadata =
                StandardErrorMetadata.builder(ErrorCategory.RATE_LIMIT)
                        .correlationId("correlation-456")
                        .retryable(true)
                        .request(new RequestErrorMetadata("post", "/payments", "createPayment"))
                        .rateLimit(new RateLimitErrorMetadata(30))
                        .buildMap();
        Notification notification =
                Notification.builder()
                        .code("E_RATE_LIMIT_0001")
                        .message("Too many requests")
                        .metadata(metadata)
                        .build();

        ObjectMapper mapper = isolatedMapper(new ObjectMapper());
        JsonNode json = mapper.readTree(mapper.writeValueAsString(notification));

        assertEquals("1", json.at("/metadata/schema_version").asText());
        assertEquals("RATE_LIMIT", json.at("/metadata/category").asText());
        assertEquals("correlation-456", json.at("/metadata/correlation_id").asText());
        assertTrue(json.at("/metadata/retryable").asBoolean());
        assertEquals("createPayment", json.at("/metadata/request/operation_id").asText());
        assertEquals(30, json.at("/metadata/rate_limit/retry_after_seconds").asInt());
        assertFalse(json.get("metadata").has("schemaVersion"));
        assertFalse(json.get("metadata").has("rateLimit"));
    }

    @Test
    void serializesSecurityAndOAuth2MetadataAsSnakeCase() throws Exception {
        Notification notification =
                Notification.builder()
                        .code("E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0002")
                        .message("The access token does not grant the required scope")
                        .metadata(
                                StandardErrorMetadata.builder(ErrorCategory.AUTHORIZATION)
                                        .security(
                                                new SecurityErrorMetadata(
                                                        "insufficient_scope", "bearer"))
                                        .oauth2(
                                                new OAuth2ErrorMetadata(
                                                        "insufficient_scope",
                                                        "The access token does not grant the required scope",
                                                        "https://www.rfc-editor.org/rfc/rfc6750#section-3.1",
                                                        "payment.write"))
                                        .buildMap())
                        .build();

        ObjectMapper mapper = isolatedMapper(new ObjectMapper());
        JsonNode json = mapper.readTree(mapper.writeValueAsString(notification));

        assertEquals("insufficient_scope", json.at("/metadata/security/reason").asText());
        assertEquals("bearer", json.at("/metadata/security/authentication_scheme").asText());
        assertEquals("insufficient_scope", json.at("/metadata/oauth2/error").asText());
        assertEquals(
                "The access token does not grant the required scope",
                json.at("/metadata/oauth2/error_description").asText());
        assertEquals(
                "https://www.rfc-editor.org/rfc/rfc6750#section-3.1",
                json.at("/metadata/oauth2/error_uri").asText());
        assertEquals("payment.write", json.at("/metadata/oauth2/scope").asText());
        assertFalse(json.at("/metadata/security").has("authenticationScheme"));
        assertFalse(json.at("/metadata/oauth2").has("errorDescription"));
        assertFalse(json.at("/metadata/oauth2").has("errorUri"));
    }

    @Test
    void serializesNotificationAndNestedMetadataAsSnakeCase() throws Exception {
        UUID id = UUID.fromString("28cf4b47-9626-4937-a39e-781c472c3212");
        Instant timestamp = Instant.parse("2026-07-19T14:20:00Z");
        Notification notification =
                new Notification(
                        "E_REQUEST_0001",
                        "Request validation failed",
                        NotificationSeverity.ERROR,
                        "customerId",
                        Map.of(
                                "correlationId",
                                "7dc7b119-8eb1-4dd9",
                                "validationContext",
                                Map.of(
                                        "fieldErrors",
                                        List.of(
                                                Map.of(
                                                        "fieldName",
                                                        "customerId",
                                                        "errorCode",
                                                        "required")))),
                        id,
                        timestamp);

        JsonNode json =
                isolatedMapper(new ObjectMapper())
                        .readTree(
                                isolatedMapper(new ObjectMapper())
                                        .writeValueAsString(notification));

        assertEquals("E_REQUEST_0001", json.get("code").asText());
        assertEquals("Request validation failed", json.get("message").asText());
        assertEquals("ERROR", json.get("severity").asText());
        assertEquals("customerId", json.get("field_name").asText());
        assertEquals("7dc7b119-8eb1-4dd9", json.at("/metadata/correlation_id").asText());
        assertEquals(
                "customerId",
                json.at("/metadata/validation_context/field_errors/0/field_name").asText());
        assertEquals(
                "required",
                json.at("/metadata/validation_context/field_errors/0/error_code").asText());
        assertEquals(id.toString(), json.get("id").asText());
        assertEquals(timestamp.toString(), json.get("timestamp").asText());
        assertFalse(json.has("fieldName"));
    }

    @Test
    void localRegistrationDoesNotChangeTheSourceObjectMapper() throws Exception {
        ObjectMapper applicationMapper = new ObjectMapper();
        var originalModules = applicationMapper.registeredModules();

        ObjectMapper notificationMapper = isolatedMapper(applicationMapper);

        assertEquals(originalModules, applicationMapper.registeredModules());
        assertTrue(
                notificationMapper.registeredModules().stream()
                        .anyMatch(
                                module ->
                                        "service-framework-notification-json"
                                                .equals(module.getModuleName())));
    }

    @Test
    void exposesConfiguredNormalizerAndRejectsNull() {
        NotificationMetadataKeyNormalizer normalizer = new NotificationMetadataKeyNormalizer();
        NotificationJsonSerializer serializer = new NotificationJsonSerializer(normalizer);

        assertSame(normalizer, serializer.metadataKeyNormalizer());
        assertThrows(NullPointerException.class, () -> new NotificationJsonSerializer(null));
    }

    private static ObjectMapper isolatedMapper(ObjectMapper source) {
        SimpleModule notificationModule = new SimpleModule("service-framework-notification-json");
        notificationModule.addSerializer(Notification.class, new NotificationJsonSerializer());
        return source.rebuild().addModule(notificationModule).build();
    }
}
