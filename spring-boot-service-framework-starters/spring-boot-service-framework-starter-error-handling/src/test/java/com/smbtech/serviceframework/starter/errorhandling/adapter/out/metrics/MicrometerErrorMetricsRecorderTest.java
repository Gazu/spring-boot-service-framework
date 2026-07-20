package com.smbtech.serviceframework.starter.errorhandling.adapter.out.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MicrometerErrorMetricsRecorderTest {

    @Test
    void incrementsCounterByBoundedDimensions() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerErrorMetricsRecorder recorder = new MicrometerErrorMetricsRecorder(registry);
        ResolvedError error = resolvedError("E_CUSTOMER_0001", ErrorCategory.NOT_FOUND);

        recorder.record(error, 404);
        recorder.record(error, 404);

        Counter counter =
                registry.find(MicrometerErrorMetricsRecorder.DEFAULT_METRIC_NAME)
                        .tags(
                                "code", "E_CUSTOMER_0001",
                                "category", "NOT_FOUND",
                                "status", "404",
                                "security_reason",
                                        MicrometerErrorMetricsRecorder.NO_SECURITY_REASON)
                        .counter();
        assertNotNull(counter);
        assertEquals(2.0, counter.count());
        assertEquals(
                Set.of("code", "category", "status", "security_reason"),
                counter.getId().getTags().stream().map(Tag::getKey).collect(Collectors.toSet()));
    }

    @Test
    void recordsOnlyBoundedSecurityReasonWithoutSensitiveOrHighCardinalityTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerErrorMetricsRecorder recorder = new MicrometerErrorMetricsRecorder(registry);
        Notification notification =
                Notification.builder()
                        .code("E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003")
                        .message("Bearer token is invalid")
                        .metadata(
                                Map.of(
                                        "security", Map.of("reason", "invalid_token"),
                                        "oauth2", Map.of("scope", "customer-secret-scope"),
                                        "request", Map.of("route", "/customers/secret-id")))
                        .build();
        ResolvedError error =
                new ResolvedError(
                        notification,
                        ErrorCategory.AUTHENTICATION,
                        ErrorExposure.PUBLIC,
                        "provider-url=https://provider.internal");

        recorder.record(error, 401);

        Counter counter =
                registry.find(MicrometerErrorMetricsRecorder.DEFAULT_METRIC_NAME)
                        .tag("security_reason", "invalid_token")
                        .counter();
        assertNotNull(counter);
        assertEquals(
                Set.of("code", "category", "status", "security_reason"),
                counter.getId().getTags().stream().map(Tag::getKey).collect(Collectors.toSet()));
        assertEquals(
                Set.of(
                        "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003",
                        "AUTHENTICATION",
                        "401",
                        "invalid_token"),
                counter.getId().getTags().stream().map(Tag::getValue).collect(Collectors.toSet()));

        Notification customReason =
                Notification.builder()
                        .code("E_CUSTOM_SECURITY")
                        .message("Denied")
                        .metadata(Map.of("security", Map.of("reason", "customer-123456")))
                        .build();
        recorder.record(
                new ResolvedError(
                        customReason,
                        ErrorCategory.AUTHORIZATION,
                        ErrorExposure.PUBLIC,
                        "diagnostic"),
                403);
        assertNotNull(
                registry.find(MicrometerErrorMetricsRecorder.DEFAULT_METRIC_NAME)
                        .tag(
                                "security_reason",
                                MicrometerErrorMetricsRecorder.OTHER_SECURITY_REASON)
                        .counter());
    }

    @Test
    void supportsCustomMetricNameAndSeparatesBoundedDimensions() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerErrorMetricsRecorder recorder =
                new MicrometerErrorMetricsRecorder(registry, "application.errors");

        recorder.record(resolvedError("E_REQUEST_0001", ErrorCategory.VALIDATION), 400);
        recorder.record(resolvedError("E_REQUEST_0002", ErrorCategory.CONFLICT), 409);

        assertEquals("application.errors", recorder.metricName());
        assertEquals(2, registry.find("application.errors").counters().size());
    }

    @Test
    void rejectsInvalidConfigurationAndStatusCodes() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerErrorMetricsRecorder recorder = new MicrometerErrorMetricsRecorder(registry);

        assertThrows(NullPointerException.class, () -> new MicrometerErrorMetricsRecorder(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MicrometerErrorMetricsRecorder(registry, " "));
        assertThrows(NullPointerException.class, () -> recorder.record(null, 500));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        recorder.record(
                                resolvedError("E_INTERNAL_0001", ErrorCategory.INTERNAL), 99));
    }

    private static ResolvedError resolvedError(String code, ErrorCategory category) {
        return new ResolvedError(
                Notification.error(code, "Request failed"),
                category,
                ErrorExposure.PUBLIC,
                "diagnostic");
    }
}
