package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ThrowableErrorResolutionPipeline;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationPipeline;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class FinalNotificationResponseSanitizerTest {

    @Test
    void sanitizesInternalBodyAfterResponseCustomizers() {
        ServiceFrameworkExceptionHandler handler = handler(ErrorExposure.INTERNAL);

        ResponseEntity<Notification> response =
                handler.handleException(
                        new IllegalStateException("diagnostic-token"),
                        new MockHttpServletRequest("POST", "/payments"));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getHeaders().getFirst("X-Customized")).isEqualTo("true");
        assertThat(response.getBody().code()).isEqualTo("E_CUSTOMIZED");
        assertThat(response.getBody().message()).isEqualTo("Bearer <redacted> was rejected");
        assertThat(response.getBody().metadata())
                .doesNotContainKey("diagnostic")
                .containsKey("request");
        assertThat(response.getBody().metadata().get("request"))
                .isEqualTo(Map.of("authorization", "<redacted>"));
    }

    @Test
    void reducesCustomizedPublicBodyToMinimalDetails() {
        ServiceFrameworkExceptionHandler handler = handler(ErrorExposure.PUBLIC);

        ResponseEntity<Notification> response =
                handler.handleException(
                        new IllegalStateException("diagnostic-token"),
                        new MockHttpServletRequest("POST", "/payments"));

        assertThat(response.getBody().code()).isEqualTo("E_CUSTOMIZED");
        assertThat(response.getBody().message()).isEqualTo("The request could not be completed");
        assertThat(response.getBody().metadata()).containsOnlyKeys("category");
        assertThat(response.getBody().metadata().toString())
                .doesNotContain("response-token", "diagnostic-token");
    }

    private static ServiceFrameworkExceptionHandler handler(ErrorExposure exposure) {
        NotificationResponseCustomizer customizer =
                (response, resolvedError, request) ->
                        ResponseEntity.status(422)
                                .header("X-Customized", "true")
                                .body(customizedNotification());
        ErrorCustomizationPipeline customizationPipeline =
                new ErrorCustomizationPipeline(
                        List.of(), List.of(customizer), resolvedError -> exposure);
        return new ServiceFrameworkExceptionHandler(
                new ThrowableErrorResolutionPipeline(List.of()),
                new DefaultNotificationResponseFactory(),
                (cause, resolvedError, request) -> {},
                (resolvedError, statusCode) -> {},
                customizationPipeline);
    }

    private static Notification customizedNotification() {
        return new Notification(
                "E_CUSTOMIZED",
                "Bearer response-token was rejected",
                NotificationSeverity.ERROR,
                "",
                Map.of(
                        "request",
                        Map.of("authorization", "Bearer response-token"),
                        "diagnostic",
                        "diagnostic-token"),
                UUID.fromString("aec02ef4-fea2-4b1c-b043-7727e75535e1"),
                Instant.parse("2026-07-20T22:19:36.689279Z"));
    }
}
