package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
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
final class ServiceFrameworkExceptionHandler {

    private final ThrowableErrorResolver resolutionPipeline;

    private final ErrorResponsePipeline responsePipeline;

    ServiceFrameworkExceptionHandler(
            ThrowableErrorResolver resolutionPipeline, ErrorResponsePipeline responsePipeline) {
        this.resolutionPipeline =
                Objects.requireNonNull(resolutionPipeline, "resolutionPipeline must not be null");
        this.responsePipeline =
                Objects.requireNonNull(responsePipeline, "responsePipeline must not be null");
    }

    ServiceFrameworkExceptionHandler(
            ThrowableErrorResolver resolutionPipeline,
            NotificationResponseFactory responseFactory) {
        this(
                resolutionPipeline,
                responseFactory,
                ErrorReporter.noop(),
                ErrorMetricsRecorder.noop());
    }

    ServiceFrameworkExceptionHandler(
            ThrowableErrorResolver resolutionPipeline,
            NotificationResponseFactory responseFactory,
            ErrorReporter errorReporter) {
        this(resolutionPipeline, responseFactory, errorReporter, ErrorMetricsRecorder.noop());
    }

    ServiceFrameworkExceptionHandler(
            ThrowableErrorResolver resolutionPipeline,
            NotificationResponseFactory responseFactory,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder) {
        this(
                resolutionPipeline,
                responseFactory,
                errorReporter,
                metricsRecorder,
                new ErrorCustomizationPipeline(List.of(), List.of()));
    }

    ServiceFrameworkExceptionHandler(
            ThrowableErrorResolver resolutionPipeline,
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
                responseFactory);
    }

    ServiceFrameworkExceptionHandler(
            ThrowableErrorResolver resolutionPipeline,
            NotificationResponseFactory responseFactory,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline,
            NotificationResponseFactory finalResponseFactory) {
        this(
                resolutionPipeline,
                new ErrorResponsePipeline(
                        responseFactory,
                        errorReporter,
                        metricsRecorder,
                        customizationPipeline,
                        finalResponseFactory));
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
