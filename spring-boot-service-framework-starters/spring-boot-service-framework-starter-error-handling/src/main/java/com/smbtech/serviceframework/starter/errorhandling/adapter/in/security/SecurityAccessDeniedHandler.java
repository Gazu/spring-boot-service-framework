package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.FinalNotificationResponseSanitizer;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityChallengeWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.security.RequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthorizationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationPipeline;
import com.smbtech.serviceframework.starter.errorhandling.internal.ErrorResponsePipeline;
import com.smbtech.serviceframework.starter.errorhandling.internal.PreparedErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Writes Spring Security authorization failures using the configured response exposure. */
public final class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    /** Preserves the original framework code for generic authorization failures. */
    public static final String ERROR_CODE = "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0001";

    /** Avoids exposing authorization policy details in the response. */
    public static final String PUBLIC_MESSAGE = "Access is denied";

    private final NotificationResponseWriter responseWriter;

    private final ErrorResponsePipeline responsePipeline;

    private final SecurityAuthorizationFailureResolver failureResolver;

    private final RequiredScopeResolver requiredScopeResolver;

    private final OAuth2SecurityChallengeWriter challengeWriter;

    private final SecurityFailureMetadataEnricher metadataEnricher;

    /**
     * Creates the access denied handler.
     *
     * @param responseFactory notification response factory
     * @param responseWriter notification response writer
     */
    public SecurityAccessDeniedHandler(
            NotificationResponseFactory responseFactory,
            NotificationResponseWriter responseWriter) {
        this(responseFactory, responseWriter, ErrorReporter.noop(), ErrorMetricsRecorder.noop());
    }

    /**
     * Creates the access denied handler with error reporting.
     *
     * @param responseFactory notification response factory
     * @param responseWriter notification response writer
     * @param errorReporter resolved error reporter
     */
    public SecurityAccessDeniedHandler(
            NotificationResponseFactory responseFactory,
            NotificationResponseWriter responseWriter,
            ErrorReporter errorReporter) {
        this(responseFactory, responseWriter, errorReporter, ErrorMetricsRecorder.noop());
    }

    /**
     * Creates the access denied handler with error reporting and metrics.
     *
     * @param responseFactory notification response factory
     * @param responseWriter notification response writer
     * @param errorReporter resolved error reporter
     * @param metricsRecorder error metrics recorder
     */
    public SecurityAccessDeniedHandler(
            NotificationResponseFactory responseFactory,
            NotificationResponseWriter responseWriter,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder) {
        this(
                responseFactory,
                responseWriter,
                errorReporter,
                metricsRecorder,
                new ErrorCustomizationPipeline(List.of(), List.of()));
    }

    /**
     * Creates the access denied handler with all extension pipelines.
     *
     * @param responseFactory notification response factory
     * @param responseWriter notification response writer
     * @param errorReporter resolved error reporter
     * @param metricsRecorder error metrics recorder
     * @param customizationPipeline error and response customization pipeline
     */
    public SecurityAccessDeniedHandler(
            NotificationResponseFactory responseFactory,
            NotificationResponseWriter responseWriter,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline) {
        this(
                responseFactory,
                responseWriter,
                errorReporter,
                metricsRecorder,
                customizationPipeline,
                new DefaultSecurityAuthorizationFailureResolver(),
                new DefaultRequiredScopeResolver(),
                new DefaultOAuth2SecurityMetadataFactory(),
                SecurityHandlerDefaults.challengeWriter());
    }

    /**
     * Creates the access denied handler with configured security metadata.
     *
     * @param responseFactory notification response factory
     * @param responseWriter notification response writer
     * @param errorReporter resolved error reporter
     * @param metricsRecorder error metrics recorder
     * @param customizationPipeline error and response customization pipeline
     * @param metadataFactory security response and challenge metadata factory
     */
    public SecurityAccessDeniedHandler(
            NotificationResponseFactory responseFactory,
            NotificationResponseWriter responseWriter,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline,
            OAuth2SecurityMetadataFactory metadataFactory) {
        this(
                responseFactory,
                responseWriter,
                errorReporter,
                metricsRecorder,
                customizationPipeline,
                new DefaultSecurityAuthorizationFailureResolver(),
                new DefaultRequiredScopeResolver(),
                metadataFactory,
                SecurityHandlerDefaults.challengeWriter());
    }

    /**
     * Creates the access denied handler with replaceable security resolution.
     *
     * @param responseFactory notification response factory
     * @param responseWriter notification response writer
     * @param errorReporter resolved error reporter
     * @param metricsRecorder error metrics recorder
     * @param customizationPipeline error and response customization pipeline
     * @param failureResolver authorization failure resolver
     * @param requiredScopeResolver required scope resolver
     * @param challengeWriter Bearer challenge writer
     */
    public SecurityAccessDeniedHandler(
            NotificationResponseFactory responseFactory,
            NotificationResponseWriter responseWriter,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline,
            SecurityAuthorizationFailureResolver failureResolver,
            RequiredScopeResolver requiredScopeResolver,
            OAuth2SecurityChallengeWriter challengeWriter) {
        this(
                responseFactory,
                responseWriter,
                errorReporter,
                metricsRecorder,
                customizationPipeline,
                failureResolver,
                requiredScopeResolver,
                new DefaultOAuth2SecurityMetadataFactory(),
                challengeWriter);
    }

    /**
     * Creates the access denied handler with replaceable resolution and metadata.
     *
     * @param responseFactory notification response factory
     * @param responseWriter notification response writer
     * @param errorReporter resolved error reporter
     * @param metricsRecorder error metrics recorder
     * @param customizationPipeline error and response customization pipeline
     * @param failureResolver authorization failure resolver
     * @param requiredScopeResolver required scope resolver
     * @param metadataFactory security response and challenge metadata factory
     * @param challengeWriter Bearer challenge writer
     */
    public SecurityAccessDeniedHandler(
            NotificationResponseFactory responseFactory,
            NotificationResponseWriter responseWriter,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline,
            SecurityAuthorizationFailureResolver failureResolver,
            RequiredScopeResolver requiredScopeResolver,
            OAuth2SecurityMetadataFactory metadataFactory,
            OAuth2SecurityChallengeWriter challengeWriter) {
        this(
                responseFactory,
                responseWriter,
                errorReporter,
                metricsRecorder,
                customizationPipeline,
                failureResolver,
                requiredScopeResolver,
                metadataFactory,
                challengeWriter,
                new FinalNotificationResponseSanitizer());
    }

    /**
     * Creates the access denied handler with a final response safety boundary.
     *
     * @param responseFactory notification response factory
     * @param responseWriter notification response writer
     * @param errorReporter resolved error reporter
     * @param metricsRecorder error metrics recorder
     * @param customizationPipeline error and response customization pipeline
     * @param failureResolver authorization failure resolver
     * @param requiredScopeResolver required scope resolver
     * @param metadataFactory security response and challenge metadata factory
     * @param challengeWriter Bearer challenge writer
     * @param finalResponseSanitizer final response sanitizer
     */
    public SecurityAccessDeniedHandler(
            NotificationResponseFactory responseFactory,
            NotificationResponseWriter responseWriter,
            ErrorReporter errorReporter,
            ErrorMetricsRecorder metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline,
            SecurityAuthorizationFailureResolver failureResolver,
            RequiredScopeResolver requiredScopeResolver,
            OAuth2SecurityMetadataFactory metadataFactory,
            OAuth2SecurityChallengeWriter challengeWriter,
            FinalNotificationResponseSanitizer finalResponseSanitizer) {
        this.responseWriter =
                Objects.requireNonNull(responseWriter, "responseWriter must not be null");
        this.responsePipeline =
                new ErrorResponsePipeline(
                        responseFactory,
                        errorReporter,
                        metricsRecorder,
                        customizationPipeline,
                        finalResponseSanitizer);
        this.failureResolver =
                Objects.requireNonNull(failureResolver, "failureResolver must not be null");
        this.requiredScopeResolver =
                Objects.requireNonNull(
                        requiredScopeResolver, "requiredScopeResolver must not be null");
        this.metadataEnricher = new SecurityFailureMetadataEnricher(metadataFactory);
        this.challengeWriter =
                Objects.requireNonNull(challengeWriter, "challengeWriter must not be null");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(response, "response must not be null");
        AccessDeniedException exception =
                Objects.requireNonNull(
                        accessDeniedException, "accessDeniedException must not be null");
        if (response.isCommitted()) {
            return;
        }
        SecurityFailureContext context =
                SecurityFailureContexts.authorization(request, exception, requiredScopeResolver);
        SecurityFailureResolution resolution =
                metadataEnricher.enrich(context, failureResolver.resolve(context));
        PreparedErrorResponse prepared =
                responsePipeline.prepare(exception, resolution.resolvedError(), request);
        SecurityFailureResolution preparedResolution =
                resolution.withResolvedError(prepared.resolvedError());
        responsePipeline.report(prepared);
        response.setStatus(prepared.statusCode());
        challengeWriter.write(request, response, context, preparedResolution);
        responseWriter.write(prepared.response(), response);
        responsePipeline.record(prepared);
    }
}
