package com.smbtech.serviceframework.starter.errorhandling.internal;

import static com.smbtech.serviceframework.starter.errorhandling.internal.ErrorHandlingWebTestFixtures.responseFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ErrorResponsePipelineTest {

    @Test
    void executesResponseStagesInDeterministicOrder() {
        List<String> stages = new ArrayList<>();
        ErrorCustomizationPipeline customizers =
                new ErrorCustomizationPipeline(
                        List.of(
                                (cause, error, request) -> {
                                    stages.add("resolved-error-customizer");
                                    return error;
                                }),
                        List.of(
                                (response, error, request) -> {
                                    stages.add("response-customizer");
                                    return response;
                                }),
                        error -> {
                            stages.add("exposure-policy");
                            return ErrorExposure.INTERNAL;
                        });
        ErrorResponsePipeline pipeline =
                new ErrorResponsePipeline(
                        error -> {
                            stages.add("response-factory");
                            return responseFactory().create(error);
                        },
                        (cause, error, request) -> stages.add("reporter"),
                        (error, statusCode) -> stages.add("metrics"),
                        customizers,
                        responseFactory());

        PreparedErrorResponse prepared =
                pipeline.prepare(
                        new IllegalStateException("failure"),
                        resolvedError(),
                        new MockHttpServletRequest("GET", "/payments"));
        pipeline.report(prepared);
        pipeline.record(prepared);

        assertThat(stages)
                .containsExactly(
                        "resolved-error-customizer",
                        "exposure-policy",
                        "response-factory",
                        "response-customizer",
                        "reporter",
                        "metrics");
        assertThat(prepared.statusCode()).isEqualTo(500);
    }

    @Test
    void isolatesReporterAndMetricsFailures() {
        ErrorResponsePipeline pipeline =
                new ErrorResponsePipeline(
                        responseFactory(),
                        (cause, error, request) -> {
                            throw new IllegalStateException("reporting failed");
                        },
                        (error, statusCode) -> {
                            throw new IllegalStateException("metrics failed");
                        },
                        new ErrorCustomizationPipeline(List.of(), List.of()),
                        responseFactory());
        PreparedErrorResponse prepared =
                pipeline.prepare(
                        new IllegalStateException("failure"),
                        resolvedError(),
                        new MockHttpServletRequest());

        assertThatCode(() -> pipeline.report(prepared)).doesNotThrowAnyException();
        assertThatCode(() -> pipeline.record(prepared)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingCollaboratorsAndInputs() {
        ErrorCustomizationPipeline customizers =
                new ErrorCustomizationPipeline(List.of(), List.of());
        NotificationResponseFactory factory = responseFactory();

        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                new ErrorResponsePipeline(
                                        null,
                                        (cause, error, request) -> {},
                                        (error, status) -> {},
                                        customizers,
                                        factory));
        ErrorResponsePipeline pipeline =
                new ErrorResponsePipeline(
                        factory,
                        (cause, error, request) -> {},
                        (error, status) -> {},
                        customizers,
                        factory);
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                new ErrorResponsePipeline(
                                        factory,
                                        (cause, error, request) -> {},
                                        (error, status) -> {},
                                        customizers,
                                        null));
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                pipeline.prepare(
                                        null, resolvedError(), new MockHttpServletRequest()));
        assertThatNullPointerException().isThrownBy(() -> pipeline.report(null));
        assertThatNullPointerException().isThrownBy(() -> pipeline.record(null));
    }

    private static ResolvedError resolvedError() {
        return new ResolvedError(
                Notification.error("E_INTERNAL", "Failure"),
                ErrorCategory.INTERNAL,
                ErrorExposure.INTERNAL,
                "diagnostic");
    }
}
