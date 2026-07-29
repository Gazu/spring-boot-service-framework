package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ThrowableErrorResolutionPipeline;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationPipeline;
import com.smbtech.serviceframework.starter.errorhandling.customizer.StandardErrorMetadataCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.internal.ErrorResponsePipeline;
import com.smbtech.serviceframework.starter.errorhandling.internal.PreparedErrorResponse;
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

    private final ErrorResponsePipeline responsePipeline;

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
        this(
                resolutionPipeline,
                responseFactory,
                errorReporter,
                metricsRecorder,
                customizationPipeline,
                new FinalNotificationResponseSanitizer());
    }

    /**
     * Creates the framework MVC exception handler with a final response safety boundary.
     *
     * @param resolutionPipeline throwable resolution pipeline
     * @param responseFactory notification response factory
     * @param errorReporter resolved error reporter
     * @param metricsRecorder error metrics recorder
     * @param customizationPipeline error and response customization pipeline
     * @param finalResponseSanitizer final response sanitizer
     */
    public ServiceFrameworkExceptionHandler(
            ThrowableErrorResolutionPipeline resolutionPipeline,
            NotificationResponseFactory responseFactory,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline,
            FinalNotificationResponseSanitizer finalResponseSanitizer) {
        this.resolutionPipeline =
                Objects.requireNonNull(resolutionPipeline, "resolutionPipeline must not be null");
        this.responsePipeline =
                new ErrorResponsePipeline(
                        responseFactory,
                        errorReporter,
                        metricsRecorder,
                        customizationPipeline,
                        finalResponseSanitizer);
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
        PreparedErrorResponse prepared =
                responsePipeline.prepare(failure, resolutionPipeline.resolve(failure), httpRequest);
        responsePipeline.report(prepared);
        responsePipeline.record(prepared);
        return prepared.response();
    }
}
