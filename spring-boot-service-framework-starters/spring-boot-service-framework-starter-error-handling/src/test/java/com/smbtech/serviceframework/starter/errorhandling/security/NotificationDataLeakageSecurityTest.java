package com.smbtech.serviceframework.starter.errorhandling.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.DefaultNotificationSanitizer;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FieldViolation;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ThrowableErrorResolutionPipeline;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataKeys;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.DefaultNotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.HttpClientExceptionResolver;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ConfiguredErrorExposurePolicy;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationPipeline;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonResponseWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class NotificationDataLeakageSecurityTest {

    private static final String REDACTED = DefaultNotificationSanitizer.REDACTED_VALUE;

    @Test
    void unexpectedFailureNeverExposesStackTraceOrExceptionSecrets() throws IOException {
        IllegalStateException failure =
                new IllegalStateException(
                        "password=runtime-secret authorization=runtime-authorization");
        failure.setStackTrace(
                new StackTraceElement[] {
                    new StackTraceElement(
                            "com.example.internal.PaymentRepository",
                            "loadPayment",
                            "PaymentRepository.java",
                            73)
                });
        ResolvedError resolvedError =
                new ThrowableErrorResolutionPipeline(List.of()).resolve(failure);

        ResponseEntity<Notification> response =
                new DefaultNotificationResponseFactory().create(resolvedError);
        Notification notification = requireBody(response);
        String json = serialize(notification);

        assertThat(notification.metadata())
                .containsOnly(
                        Map.entry(StandardErrorMetadataKeys.SCHEMA_VERSION, "1"),
                        Map.entry(StandardErrorMetadataKeys.CATEGORY, "INTERNAL"));
        assertThat(notification.message()).isEqualTo("The request could not be completed");
        assertThat(json)
                .doesNotContain(
                        "runtime-secret",
                        "runtime-authorization",
                        "PaymentRepository",
                        "loadPayment",
                        "PaymentRepository.java",
                        "IllegalStateException");
    }

    @Test
    void sanitizerRedactsHeadersBodiesCausesAndStackTracesEvenWhenAllowlisted() throws IOException {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestHeaders", Map.of("Authorization", "Bearer request-header-secret"));
        metadata.put("responseHeaders", Map.of("Set-Cookie", "session=response-cookie-secret"));
        metadata.put("requestBody", "{\"password\":\"request-body-secret\"}");
        metadata.put("downstreamBody", "{\"access_token\":\"downstream-body-secret\"}");
        metadata.put("stackTrace", "at com.example.SecretService.execute(SecretService.java:91)");
        metadata.put("cause", new IllegalArgumentException("cause-secret"));
        metadata.put(
                "context",
                Map.of(
                        "clientSecret", "nested-client-secret",
                        "accessToken", "nested-access-token",
                        "status", 401));
        Notification source =
                Notification.builder()
                        .code("E_SECURITY_0001")
                        .message("The request was rejected")
                        .metadata(metadata)
                        .build();
        ResolvedError resolvedError =
                new ResolvedError(
                        source,
                        ErrorCategory.AUTHENTICATION,
                        ErrorExposure.PUBLIC,
                        "diagnostic-secret");
        DefaultNotificationResponseFactory factory =
                new DefaultNotificationResponseFactory(
                        error -> org.springframework.http.HttpStatus.UNAUTHORIZED,
                        new DefaultNotificationSanitizer(metadata.keySet()));

        Notification notification = requireBody(factory.create(resolvedError));
        String json = serialize(notification);

        assertThat(notification.metadata())
                .containsEntry("requestHeaders", REDACTED)
                .containsEntry("responseHeaders", REDACTED)
                .containsEntry("requestBody", REDACTED)
                .containsEntry("downstreamBody", REDACTED)
                .containsEntry("stackTrace", REDACTED)
                .containsEntry("cause", REDACTED);
        Map<?, ?> context = (Map<?, ?>) notification.metadata().get("context");
        assertThat(context.get("clientSecret")).isEqualTo(REDACTED);
        assertThat(context.get("accessToken")).isEqualTo(REDACTED);
        assertThat(context.get("status")).isEqualTo(401);
        assertThat(json)
                .doesNotContain(
                        "request-header-secret",
                        "response-cookie-secret",
                        "request-body-secret",
                        "downstream-body-secret",
                        "SecretService",
                        "cause-secret",
                        "nested-client-secret",
                        "nested-access-token",
                        "diagnostic-secret");
    }

    @Test
    void downstreamFailureNeverExposesUriHeadersBodyOrCause() throws IOException {
        Notification downstreamNotification =
                Notification.builder()
                        .code("E_PAYMENTS_0503")
                        .message("Downstream message contains source-notification-secret")
                        .metadata(Map.of("responseBody", "source-metadata-secret"))
                        .build();
        HttpErrorResponse downstreamError =
                new HttpErrorResponse(
                        "payments-private-client",
                        "POST",
                        "https://payments.example/orders?access_token=uri-secret",
                        503,
                        "Service unavailable reason-secret",
                        HttpErrorResponse.categoryOf(503),
                        Map.of(
                                "Authorization", "Bearer authorization-header-secret",
                                "Set-Cookie", "session=cookie-header-secret",
                                "X-Downstream-Debug", "debug-header-secret"),
                        "{\"password\":\"downstream-body-secret\",\"trace\":\"body-stack-secret\"}",
                        "application/json",
                        "UTF-8",
                        false);
        HttpClientResponseException failure =
                new HttpClientResponseException(
                        downstreamError,
                        List.of(downstreamNotification),
                        new IllegalStateException("tls-cause-secret"));
        ThrowableErrorResolutionPipeline pipeline =
                new ThrowableErrorResolutionPipeline(List.of(new HttpClientExceptionResolver()));

        Notification notification =
                requireBody(
                        new DefaultNotificationResponseFactory().create(pipeline.resolve(failure)));
        String json = serialize(notification);

        assertThat(notification.code()).isEqualTo("E_PAYMENTS_0503");
        assertThat(notification.message()).isEqualTo(HttpClientExceptionResolver.PUBLIC_MESSAGE);
        assertThat(notification.metadata())
                .containsEntry("schemaVersion", "1")
                .containsEntry("category", "DOWNSTREAM")
                .containsEntry("retryable", true)
                .containsEntry(
                        "dependency", Map.of("name", "downstream", "failureType", "server_error"));
        assertThat(json)
                .doesNotContain(
                        "payments-private-client",
                        "uri-secret",
                        "reason-secret",
                        "authorization-header-secret",
                        "cookie-header-secret",
                        "debug-header-secret",
                        "downstream-body-secret",
                        "body-stack-secret",
                        "source-notification-secret",
                        "source-metadata-secret",
                        "tls-cause-secret");
    }

    @Test
    void publicExposureNeverLeaksSensitiveDataEvenWithNoOpApplicationSanitizer()
            throws IOException {
        Notification sourceNotification =
                Notification.builder()
                        .code("E_PUBLIC_SECURITY_0001")
                        .message("token=message-token-secret password=message-password-secret")
                        .metadata(
                                Map.of(
                                        "security",
                                                Map.of(
                                                        "authorization",
                                                        "Bearer header-token-secret",
                                                        "password",
                                                        "metadata-password-secret"),
                                        "dependency",
                                                Map.of(
                                                        "responseHeaders",
                                                        Map.of(
                                                                "Authorization",
                                                                "Bearer downstream-header-secret"),
                                                        "responseBody",
                                                        "{\"password\":\"downstream-body-secret\"}"),
                                        "http",
                                                Map.of(
                                                        "stackTrace",
                                                        "at com.example.internal.SecretRepository.load(SecretRepository.java:42)")))
                        .build();
        IllegalStateException rootCause =
                new IllegalStateException("password=root-password-secret");
        IllegalStateException failure =
                new IllegalStateException("token=cause-token-secret", rootCause);
        failure.setStackTrace(
                new StackTraceElement[] {
                    new StackTraceElement(
                            "com.example.internal.SensitiveController",
                            "execute",
                            "SensitiveController.java",
                            99)
                });
        ResolvedError sourceError =
                new ResolvedError(
                        sourceNotification,
                        ErrorCategory.INTERNAL,
                        ErrorExposure.INTERNAL,
                        "token=diagnostic-token-secret password=diagnostic-password-secret",
                        List.of(
                                new FieldViolation(
                                        "password",
                                        "invalid",
                                        "password=violation-password-secret")));
        ErrorHandlingProperties properties = new ErrorHandlingProperties();
        properties.getResponse().setExposure(ErrorExposure.PUBLIC);
        ErrorCustomizationPipeline pipeline =
                new ErrorCustomizationPipeline(
                        List.of(), List.of(), new ConfiguredErrorExposurePolicy(properties));

        ResolvedError effectiveError =
                pipeline.customize(
                        failure, sourceError, new MockHttpServletRequest("GET", "/sensitive"));
        DefaultNotificationResponseFactory factory =
                new DefaultNotificationResponseFactory(
                        error -> HttpStatus.INTERNAL_SERVER_ERROR, notification -> notification);
        Notification notification = requireBody(factory.create(effectiveError));
        String json = serialize(notification);

        assertThat(effectiveError.exposure()).isEqualTo(ErrorExposure.PUBLIC);
        assertThat(notification.code()).isEqualTo("E_PUBLIC_SECURITY_0001");
        assertThat(json)
                .contains(REDACTED)
                .doesNotContain(
                        "message-token-secret",
                        "message-password-secret",
                        "header-token-secret",
                        "metadata-password-secret",
                        "downstream-header-secret",
                        "downstream-body-secret",
                        "SensitiveController",
                        "SensitiveController.java",
                        "SecretRepository",
                        "diagnostic-token-secret",
                        "diagnostic-password-secret",
                        "cause-token-secret",
                        "root-password-secret",
                        "violation-password-secret");
    }

    private static Notification requireBody(ResponseEntity<Notification> response) {
        Notification body = response.getBody();
        assertNotNull(body);
        return body;
    }

    private static String serialize(Notification notification) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new NotificationJsonResponseWriter(new ObjectMapper()).write(notification, output);
        return output.toString(StandardCharsets.UTF_8);
    }
}
