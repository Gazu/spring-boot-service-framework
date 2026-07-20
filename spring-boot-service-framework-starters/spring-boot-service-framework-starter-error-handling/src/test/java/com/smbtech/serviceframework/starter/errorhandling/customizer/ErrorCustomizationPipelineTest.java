package com.smbtech.serviceframework.starter.errorhandling.customizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.api.ResolvedErrorCustomizer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class ErrorCustomizationPipelineTest {

    @Test
    void appliesResolvedErrorAndResponseCustomizersInOrder() {
        List<String> calls = new ArrayList<>();
        ErrorCustomizationPipeline pipeline =
                new ErrorCustomizationPipeline(
                        List.of(
                                resolvedCustomizer("second", 20, calls),
                                resolvedCustomizer("first", -10, calls)),
                        List.of(
                                responseCustomizer("fourth", 20, calls),
                                responseCustomizer("third", -10, calls)),
                        resolvedError -> {
                            calls.add("exposure");
                            return ErrorExposure.INTERNAL;
                        });
        ResolvedError source = error("E_SOURCE");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customers");

        ResolvedError customized = pipeline.customize(new IllegalStateException(), source, request);
        ResponseEntity<Notification> response =
                pipeline.customize(
                        ResponseEntity.badRequest().body(customized.notification()),
                        customized,
                        request);

        assertEquals(List.of("first", "second", "exposure", "third", "fourth"), calls);
        assertEquals("E_SOURCE_FIRST_SECOND", customized.notification().code());
        assertEquals(ErrorExposure.INTERNAL, customized.exposure());
        assertEquals(List.of("third", "fourth"), response.getHeaders().get("X-Customizer"));
    }

    @Test
    void rejectsNullCustomizerResults() {
        ErrorCustomizationPipeline resolvedPipeline =
                new ErrorCustomizationPipeline(List.of((cause, error, request) -> null), List.of());
        ErrorCustomizationPipeline responsePipeline =
                new ErrorCustomizationPipeline(
                        List.of(), List.of((response, error, request) -> null));
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(
                NullPointerException.class,
                () ->
                        resolvedPipeline.customize(
                                new IllegalStateException(), error("E_SOURCE"), request));
        assertThrows(
                NullPointerException.class,
                () ->
                        responsePipeline.customize(
                                ResponseEntity.badRequest()
                                        .body(Notification.error("E_SOURCE", "failure")),
                                error("E_SOURCE"),
                                request));
    }

    @Test
    void rejectsInvalidExposurePoliciesAndResults() {
        assertThrows(
                NullPointerException.class,
                () -> new ErrorCustomizationPipeline(List.of(), List.of(), null));

        ErrorCustomizationPipeline pipeline =
                new ErrorCustomizationPipeline(List.of(), List.of(), resolvedError -> null);
        assertThrows(
                NullPointerException.class,
                () ->
                        pipeline.customize(
                                new IllegalStateException(),
                                error("E_SOURCE"),
                                new MockHttpServletRequest()));
    }

    private static ResolvedErrorCustomizer resolvedCustomizer(
            String name, int order, List<String> calls) {
        return new ResolvedErrorCustomizer() {
            @Override
            public ResolvedError customize(
                    Throwable cause,
                    ResolvedError resolvedError,
                    jakarta.servlet.http.HttpServletRequest request) {
                calls.add(name);
                return error(resolvedError.notification().code() + "_" + name.toUpperCase());
            }

            @Override
            public int order() {
                return order;
            }
        };
    }

    private static NotificationResponseCustomizer responseCustomizer(
            String name, int order, List<String> calls) {
        return new NotificationResponseCustomizer() {
            @Override
            public ResponseEntity<Notification> customize(
                    ResponseEntity<Notification> response,
                    ResolvedError resolvedError,
                    jakarta.servlet.http.HttpServletRequest request) {
                calls.add(name);
                return ResponseEntity.status(response.getStatusCode())
                        .headers(response.getHeaders())
                        .header("X-Customizer", name)
                        .body(response.getBody());
            }

            @Override
            public int order() {
                return order;
            }
        };
    }

    private static ResolvedError error(String code) {
        return new ResolvedError(
                Notification.error(code, "failure"),
                ErrorCategory.VALIDATION,
                ErrorExposure.PUBLIC,
                "diagnostic");
    }
}
