package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonSerializer;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

class NotificationExposureContractTest {

    private static final String ERROR_CODE = "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003";
    private static final UUID NOTIFICATION_ID =
            UUID.fromString("aec02ef4-fea2-4b1c-b043-7727e75535e1");
    private static final Instant TIMESTAMP = Instant.parse("2026-07-20T22:19:36.689279Z");
    private static final String CORRELATION_ID = "test-credentials-001";

    @Test
    void publicExposureKeepsErrorIdentityAndReturnsMinimalDetails() {
        ResponseEntity<Notification> response = createResponse(ErrorExposure.PUBLIC);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(toJson(response.getBody()))
                .isEqualTo(
                        json(
                                """
                                {
                                  "code": "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003",
                                  "message": "The request could not be completed",
                                  "severity": "ERROR",
                                  "field_name": "",
                                  "metadata": {
                                    "correlation_id": "test-credentials-001",
                                    "category": "AUTHENTICATION"
                                  },
                                  "id": "aec02ef4-fea2-4b1c-b043-7727e75535e1",
                                  "timestamp": "2026-07-20T22:19:36.689279Z"
                                }
                                """));
    }

    @Test
    void internalExposureKeepsErrorIdentityAndReturnsSanitizedDetails() {
        ResponseEntity<Notification> response = createResponse(ErrorExposure.INTERNAL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(toJson(response.getBody()))
                .isEqualTo(
                        json(
                                """
                                {
                                  "code": "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003",
                                  "message": "Bearer <redacted> is invalid",
                                  "severity": "ERROR",
                                  "field_name": "",
                                  "metadata": {
                                    "schema_version": "1",
                                    "category": "AUTHENTICATION",
                                    "correlation_id": "test-credentials-001",
                                    "retryable": false,
                                    "request": {
                                      "method": "POST"
                                    },
                                    "security": {
                                      "reason": "invalid_token",
                                      "authentication_scheme": "bearer"
                                    },
                                    "oauth2": {
                                      "error": "invalid_token",
                                      "error_description": "The access token is invalid",
                                      "error_uri": "https://www.rfc-editor.org/rfc/rfc6750#section-3.1"
                                    }
                                  },
                                  "id": "aec02ef4-fea2-4b1c-b043-7727e75535e1",
                                  "timestamp": "2026-07-20T22:19:36.689279Z"
                                }
                                """));
    }

    private static ResponseEntity<Notification> createResponse(ErrorExposure exposure) {
        return new DefaultNotificationResponseFactory().create(resolvedError(exposure));
    }

    private static ResolvedError resolvedError(ErrorExposure exposure) {
        Notification notification =
                new Notification(
                        ERROR_CODE,
                        "Bearer secret-token-value is invalid",
                        NotificationSeverity.ERROR,
                        "",
                        Map.of(
                                "schemaVersion",
                                "1",
                                "category",
                                "AUTHENTICATION",
                                "correlationId",
                                CORRELATION_ID,
                                "retryable",
                                false,
                                "request",
                                Map.of("method", "POST"),
                                "security",
                                Map.of("reason", "invalid_token", "authenticationScheme", "bearer"),
                                "oauth2",
                                Map.of(
                                        "error",
                                        "invalid_token",
                                        "errorDescription",
                                        "The access token is invalid",
                                        "errorUri",
                                        "https://www.rfc-editor.org/rfc/rfc6750#section-3.1")),
                        NOTIFICATION_ID,
                        TIMESTAMP);
        return new ResolvedError(
                notification,
                ErrorCategory.AUTHENTICATION,
                exposure,
                "Bearer diagnostic-token-value was rejected");
    }

    private static JsonNode toJson(Notification notification) {
        return notificationMapper().valueToTree(notification);
    }

    private static JsonNode json(String value) {
        try {
            return new ObjectMapper().readTree(value);
        } catch (Exception exception) {
            throw new AssertionError("Invalid expected JSON", exception);
        }
    }

    private static ObjectMapper notificationMapper() {
        SimpleModule module = new SimpleModule("service-framework-notification-json");
        module.addSerializer(Notification.class, new NotificationJsonSerializer());
        return new ObjectMapper().rebuild().addModule(module).build();
    }
}
