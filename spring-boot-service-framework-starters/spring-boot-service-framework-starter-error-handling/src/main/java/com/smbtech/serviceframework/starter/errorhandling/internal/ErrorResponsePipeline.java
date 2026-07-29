package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.FinalNotificationResponseSanitizer;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationPipeline;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.http.ResponseEntity;

/** Coordinates the common response stages shared by MVC and Spring Security adapters. */
public final class ErrorResponsePipeline {

    private final NotificationResponseFactory responseFactory;
    private final ErrorReporter errorReporter;
    private final ErrorMetricsRecorder metricsRecorder;
    private final ErrorCustomizationPipeline customizationPipeline;
    private final FinalNotificationResponseSanitizer finalResponseSanitizer;

    /**
     * Creates the response pipeline.
     *
     * @param responseFactory notification response factory
     * @param errorReporter resolved error reporter
     * @param metricsRecorder error metrics recorder
     * @param customizationPipeline error and response customization pipeline
     * @param finalResponseSanitizer final response sanitizer
     */
    public ErrorResponsePipeline(
            NotificationResponseFactory responseFactory,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline,
            FinalNotificationResponseSanitizer finalResponseSanitizer) {
        this.responseFactory =
                Objects.requireNonNull(responseFactory, "responseFactory must not be null");
        this.errorReporter =
                Objects.requireNonNull(errorReporter, "errorReporter must not be null");
        this.metricsRecorder =
                Objects.requireNonNull(metricsRecorder, "metricsRecorder must not be null");
        this.customizationPipeline =
                Objects.requireNonNull(
                        customizationPipeline, "customizationPipeline must not be null");
        this.finalResponseSanitizer =
                Objects.requireNonNull(
                        finalResponseSanitizer, "finalResponseSanitizer must not be null");
    }

    /**
     * Applies customization, exposure, response creation, and the final safety boundary.
     *
     * @param cause original request failure
     * @param resolvedError initially resolved error
     * @param request current request
     * @return prepared response and its final resolved error
     */
    public PreparedErrorResponse prepare(
            Throwable cause, ResolvedError resolvedError, HttpServletRequest request) {
        Throwable safeCause = Objects.requireNonNull(cause, "cause must not be null");
        HttpServletRequest safeRequest =
                Objects.requireNonNull(request, "request must not be null");
        ResolvedError customizedError =
                customizationPipeline.customize(
                        safeCause,
                        Objects.requireNonNull(resolvedError, "resolvedError must not be null"),
                        safeRequest);
        ResponseEntity<Notification> response = responseFactory.create(customizedError);
        response = customizationPipeline.customize(response, customizedError, safeRequest);
        if (!customizationPipeline.responseCustomizers().isEmpty()) {
            response = finalResponseSanitizer.sanitize(response, customizedError);
        }
        return new PreparedErrorResponse(safeCause, customizedError, safeRequest, response);
    }

    /**
     * Reports the prepared failure without allowing reporter failures to replace the response.
     *
     * @param preparedResponse prepared response
     */
    public void report(PreparedErrorResponse preparedResponse) {
        PreparedErrorResponse prepared = requirePrepared(preparedResponse);
        try {
            errorReporter.report(
                    prepared.cause(),
                    prepared.resolvedError(),
                    prepared.request(),
                    prepared.statusCode());
        } catch (RuntimeException ignored) {
            // Reporting is observational and must not replace the application response.
        }
    }

    /**
     * Records metrics without allowing recorder failures to replace the response.
     *
     * @param preparedResponse prepared response
     */
    public void record(PreparedErrorResponse preparedResponse) {
        PreparedErrorResponse prepared = requirePrepared(preparedResponse);
        try {
            metricsRecorder.record(prepared.resolvedError(), prepared.statusCode());
        } catch (RuntimeException ignored) {
            // Metrics are observational and must not replace the application response.
        }
    }

    private static PreparedErrorResponse requirePrepared(PreparedErrorResponse preparedResponse) {
        return Objects.requireNonNull(preparedResponse, "preparedResponse must not be null");
    }
}
