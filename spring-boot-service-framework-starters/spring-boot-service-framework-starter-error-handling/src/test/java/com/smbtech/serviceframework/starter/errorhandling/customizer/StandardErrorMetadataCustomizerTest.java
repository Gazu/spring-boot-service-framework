package com.smbtech.serviceframework.starter.errorhandling.customizer;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.HandlerMapping;

class StandardErrorMetadataCustomizerTest {

    @Test
    void appliesValidationRequestAndCorrelationMetadataWithoutPublishingTheRawUri() {
        CorrelationContext correlationContext =
                new CorrelationContext() {
                    @Override
                    public Map<String, String> snapshot() {
                        return Map.of("transactionId", "correlation-123");
                    }

                    @Override
                    public Scope open(Map<String, String> values) {
                        return () -> {};
                    }
                };
        StandardErrorMetadataCustomizer customizer =
                new StandardErrorMetadataCustomizer(correlationContext);
        Notification notification =
                Notification.builder()
                        .code("E_VALIDATION")
                        .message("Validation failed")
                        .metadata(Map.of("applicationMetadata", "preserved"))
                        .build();
        ResolvedError resolvedError =
                new ResolvedError(
                        notification, ErrorCategory.VALIDATION, ErrorExposure.PUBLIC, "diagnostic");
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/customers/customer-secret");
        request.setQueryString("access_token=query-secret");
        request.setAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/customers/{customerId}");

        ResolvedError customized =
                customizer.customize(
                        new BindException(new Object(), "request"), resolvedError, request);

        assertThat(customized.notification().metadata())
                .containsEntry("schemaVersion", "1")
                .containsEntry("category", "VALIDATION")
                .containsEntry("correlationId", "correlation-123")
                .containsEntry("retryable", false)
                .containsEntry("applicationMetadata", "preserved")
                .containsEntry(
                        "request",
                        Map.of(
                                "method", "POST",
                                "route", "/customers/{customerId}"))
                .containsEntry("validation", Map.of("type", "bean_validation"));
        assertThat(customized.notification().metadata().toString())
                .doesNotContain("customer-secret", "query-secret", "access_token");
    }

    @Test
    void appliesGenericCategoryMetadataWithoutInventingRetryability() {
        StandardErrorMetadataCustomizer customizer = new StandardErrorMetadataCustomizer();
        ResolvedError resolvedError =
                new ResolvedError(
                        Notification.error("E_INTERNAL", "Failure"),
                        ErrorCategory.INTERNAL,
                        ErrorExposure.INTERNAL,
                        "database password=secret");

        ResolvedError customized =
                customizer.customize(
                        new IllegalStateException("provider secret"),
                        resolvedError,
                        new MockHttpServletRequest("GET", "/failure/secret-id"));

        assertThat(customized.notification().metadata())
                .containsEntry("schemaVersion", "1")
                .containsEntry("category", "INTERNAL")
                .doesNotContainKey("retryable");
        assertThat(customized.notification().metadata().toString())
                .doesNotContain("provider secret", "password=secret", "secret-id");
    }
}
