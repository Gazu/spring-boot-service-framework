package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ThrowableErrorResolutionPipeline;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonSerializer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

class HttpClientExceptionResolverTest {

    private final HttpClientExceptionResolver resolver = new HttpClientExceptionResolver();

    @Test
    void createsSafeNotificationAndKeepsCompleteInternalDiagnostics() throws Exception {
        RuntimeException cause = new RuntimeException("TLS credential failure");
        HttpErrorResponse error = sensitiveError(503);
        HttpClientResponseException exception =
                new HttpClientResponseException(error, List.of(), cause);

        ResolvedError resolvedError = resolver.resolve(exception);

        assertEquals("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503", resolvedError.notification().code());
        assertEquals(
                HttpClientExceptionResolver.PUBLIC_MESSAGE, resolvedError.notification().message());
        assertEquals("1", resolvedError.notification().metadata().get("schemaVersion"));
        assertEquals("DOWNSTREAM", resolvedError.notification().metadata().get("category"));
        assertEquals(true, resolvedError.notification().metadata().get("retryable"));
        assertEquals(
                Map.of("name", "downstream", "failureType", "server_error"),
                resolvedError.notification().metadata().get("dependency"));
        assertEquals(ErrorCategory.DOWNSTREAM, resolvedError.category());
        assertEquals(ErrorExposure.PUBLIC, resolvedError.exposure());
        assertTrue(resolvedError.diagnosticMessage().contains("client=payments"));
        assertTrue(resolvedError.diagnosticMessage().contains("access_token=uri-secret"));
        assertTrue(resolvedError.diagnosticMessage().contains("Bearer downstream-token"));
        assertTrue(resolvedError.diagnosticMessage().contains("\"password\":\"body-secret\""));
        assertTrue(resolvedError.diagnosticMessage().contains("bodyTruncated=true"));
        assertTrue(resolvedError.diagnosticMessage().contains("TLS credential failure"));
        assertSame(cause, exception.getCause());

        ResponseEntity<Notification> response =
                new DefaultNotificationResponseFactory().create(resolvedError);
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        JsonNode body = notificationMapper().valueToTree(response.getBody());
        String publicJson = body.toString();
        assertFalse(publicJson.contains("uri-secret"));
        assertFalse(publicJson.contains("downstream-token"));
        assertFalse(publicJson.contains("body-secret"));
        assertFalse(publicJson.contains("TLS credential failure"));
    }

    @Test
    void preservesSourceNotificationIdentityAndCodeButReplacesItsContent() {
        Notification source =
                Notification.builder()
                        .code("E_PAYMENTS_0503")
                        .message("Sensitive downstream reason")
                        .fieldName("secretField")
                        .metadata(Map.of("body", "secret"))
                        .build();
        HttpClientResponseException exception =
                new HttpClientResponseException(sensitiveError(503), source);

        Notification notification = resolver.resolve(exception).notification();

        assertEquals(source.code(), notification.code());
        assertEquals(source.id(), notification.id());
        assertEquals(source.timestamp(), notification.timestamp());
        assertEquals(HttpClientExceptionResolver.PUBLIC_MESSAGE, notification.message());
        assertEquals("", notification.fieldName());
        assertEquals("1", notification.metadata().get("schemaVersion"));
        assertEquals("DOWNSTREAM", notification.metadata().get("category"));
        assertEquals(
                Map.of("name", "downstream", "failureType", "server_error"),
                notification.metadata().get("dependency"));
    }

    @Test
    void mapsDownstreamRateLimitToRateLimitCategory() {
        ResolvedError resolvedError =
                resolver.resolve(new HttpClientResponseException(sensitiveError(429)));

        ResponseEntity<Notification> response =
                new DefaultNotificationResponseFactory().create(resolvedError);

        assertEquals(ErrorCategory.RATE_LIMIT, resolvedError.category());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0429", resolvedError.notification().code());
        assertEquals(true, resolvedError.notification().metadata().get("retryable"));
        assertEquals(
                Map.of("name", "downstream", "failureType", "rate_limited"),
                resolvedError.notification().metadata().get("dependency"));
    }

    @Test
    void exposesOnlyNumericRetryAfterMetadata() {
        HttpErrorResponse source = sensitiveError(429);
        HttpErrorResponse error =
                new HttpErrorResponse(
                        source.clientName(),
                        source.method(),
                        source.uri(),
                        source.statusCode(),
                        source.reasonPhrase(),
                        source.category(),
                        Map.of("Retry-After", "30", "Authorization", "Bearer secret"),
                        source.body(),
                        source.contentType(),
                        source.charset(),
                        source.bodyTruncated());

        Notification notification =
                resolver.resolve(new HttpClientResponseException(error)).notification();

        assertEquals(Map.of("retryAfterSeconds", 30L), notification.metadata().get("rateLimit"));
        assertFalse(notification.metadata().toString().contains("secret"));
    }

    @Test
    void integratesWithTheResolutionPipeline() {
        ThrowableErrorResolutionPipeline pipeline =
                new ThrowableErrorResolutionPipeline(List.of(resolver));
        HttpClientResponseException exception =
                new HttpClientResponseException(sensitiveError(500));

        ResolvedError resolvedError = pipeline.resolve(exception);

        assertEquals("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0500", resolvedError.notification().code());
        assertEquals(ErrorCategory.DOWNSTREAM, resolvedError.category());
    }

    @Test
    void rejectsUnsupportedFailures() {
        assertFalse(resolver.supports(new RuntimeException()));
        assertThrows(
                IllegalArgumentException.class, () -> resolver.resolve(new RuntimeException()));
    }

    private static HttpErrorResponse sensitiveError(int statusCode) {
        return new HttpErrorResponse(
                "payments",
                "POST",
                "https://payments.example/orders?access_token=uri-secret",
                statusCode,
                "Sensitive downstream reason",
                HttpErrorResponse.categoryOf(statusCode),
                Map.of(
                        "Authorization",
                        "Bearer downstream-token",
                        "Set-Cookie",
                        "session=cookie-secret"),
                "{\"password\":\"body-secret\"}",
                "application/json",
                "UTF-8",
                true);
    }

    private static ObjectMapper notificationMapper() {
        SimpleModule module = new SimpleModule("service-framework-notification-json");
        module.addSerializer(Notification.class, new NotificationJsonSerializer());
        return new ObjectMapper().rebuild().addModule(module).build();
    }
}
