package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ThrowableErrorResolutionPipeline;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationPipeline;
import com.smbtech.serviceframework.starter.errorhandling.customizer.StandardErrorMetadataCustomizer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Coordinates exception resolution and notification response creation for MVC. */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public final class ServiceFrameworkExceptionHandler {

    private final ThrowableErrorResolutionPipeline resolutionPipeline;

    private final NotificationResponseFactory responseFactory;

    private final ErrorReporter errorReporter;

    private final ErrorMetricsRecorder metricsRecorder;

    private final ErrorCustomizationPipeline customizationPipeline;

    /**
     * Creates the framework MVC exception handler.
     *
     * @param resolutionPipeline throwable resolution pipeline
     * @param responseFactory notification response factory
     */
    public ServiceFrameworkExceptionHandler(
            ThrowableErrorResolutionPipeline resolutionPipeline,
            NotificationResponseFactory responseFactory) {
        this(
                resolutionPipeline,
                responseFactory,
                ErrorReporter.noop(),
                ErrorMetricsRecorder.noop());
    }

    /**
     * Creates the framework MVC exception handler with error reporting.
     *
     * @param resolutionPipeline throwable resolution pipeline
     * @param responseFactory notification response factory
     * @param errorReporter resolved error reporter
     */
    public ServiceFrameworkExceptionHandler(
            ThrowableErrorResolutionPipeline resolutionPipeline,
            NotificationResponseFactory responseFactory,
            ErrorReporter errorReporter) {
        this(resolutionPipeline, responseFactory, errorReporter, ErrorMetricsRecorder.noop());
    }

    /**
     * Creates the framework MVC exception handler with reporting and metrics.
     *
     * @param resolutionPipeline throwable resolution pipeline
     * @param responseFactory notification response factory
     * @param errorReporter resolved error reporter
     * @param metricsRecorder error metrics recorder
     */
    public ServiceFrameworkExceptionHandler(
            ThrowableErrorResolutionPipeline resolutionPipeline,
            NotificationResponseFactory responseFactory,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder) {
        this(
                resolutionPipeline,
                responseFactory,
                errorReporter,
                metricsRecorder,
                new ErrorCustomizationPipeline(
                        List.of(new StandardErrorMetadataCustomizer()), List.of()));
    }

    /**
     * Creates the framework MVC exception handler with all extension pipelines.
     *
     * @param resolutionPipeline throwable resolution pipeline
     * @param responseFactory notification response factory
     * @param errorReporter resolved error reporter
     * @param metricsRecorder error metrics recorder
     * @param customizationPipeline error and response customization pipeline
     */
    public ServiceFrameworkExceptionHandler(
            ThrowableErrorResolutionPipeline resolutionPipeline,
            NotificationResponseFactory responseFactory,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline) {
        this.resolutionPipeline =
                Objects.requireNonNull(resolutionPipeline, "resolutionPipeline must not be null");
        this.responseFactory =
                Objects.requireNonNull(responseFactory, "responseFactory must not be null");
        this.errorReporter =
                Objects.requireNonNull(errorReporter, "errorReporter must not be null");
        this.metricsRecorder =
                Objects.requireNonNull(metricsRecorder, "metricsRecorder must not be null");
        this.customizationPipeline =
                Objects.requireNonNull(
                        customizationPipeline, "customizationPipeline must not be null");
    }

    /**
     * Handles exceptions not claimed by a higher-precedence application advice.
     *
     * @param exception exception raised while processing the request
     * @param request current HTTP request
     * @return notification response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Notification> handleException(
            Exception exception, HttpServletRequest request) {
        Exception failure = Objects.requireNonNull(exception, "exception must not be null");
        HttpServletRequest httpRequest =
                Objects.requireNonNull(request, "request must not be null");
        ResolvedError resolvedError =
                customizationPipeline.customize(
                        failure, resolutionPipeline.resolve(failure), httpRequest);
        ResponseEntity<Notification> response =
                customizationPipeline.customize(
                        responseFactory.create(resolvedError), resolvedError, httpRequest);
        int statusCode = response.getStatusCode().value();
        reportSafely(failure, resolvedError, httpRequest, statusCode);
        recordSafely(resolvedError, statusCode);
        return response;
    }

    private void reportSafely(
            Throwable cause,
            ResolvedError resolvedError,
            HttpServletRequest request,
            int statusCode) {
        try {
            errorReporter.report(cause, resolvedError, request, statusCode);
        } catch (RuntimeException ignored) {
            // Error reporting must never replace the original HTTP response.
        }
    }

    private void recordSafely(ResolvedError resolvedError, int statusCode) {
        try {
            metricsRecorder.record(resolvedError, statusCode);
        } catch (RuntimeException ignored) {
            // Metrics must never replace the original HTTP response.
        }
    }
}
