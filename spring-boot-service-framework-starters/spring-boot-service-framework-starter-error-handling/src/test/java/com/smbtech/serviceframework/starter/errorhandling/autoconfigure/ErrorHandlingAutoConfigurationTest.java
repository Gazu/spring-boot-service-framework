package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.DefaultNotificationAggregationPolicy;
import com.smbtech.serviceframework.error.DefaultNotificationSanitizer;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FallbackThrowableErrorResolver;
import com.smbtech.serviceframework.error.FieldViolation;
import com.smbtech.serviceframework.error.NotificationAggregationPolicy;
import com.smbtech.serviceframework.error.NotificationSanitizer;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ServiceException;
import com.smbtech.serviceframework.error.ServiceExceptionThrowableErrorResolver;
import com.smbtech.serviceframework.error.ThrowableErrorResolutionPipeline;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultOAuth2SecurityChallengeWriter;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultRequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultSecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultSecurityAuthorizationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.SecurityAccessDeniedHandler;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.SecurityAuthenticationEntryPoint;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.DefaultNotificationHttpStatusResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.DefaultNotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.HttpClientExceptionResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.NotificationHttpMessageConverter;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.NotificationWebMvcConfigurer;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.ServiceFrameworkExceptionHandler;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.SpringMvcExceptionResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.ValidationExceptionResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.out.logging.StructuredErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.adapter.out.metrics.MicrometerErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorExposurePolicy;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationHttpStatusResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import com.smbtech.serviceframework.starter.errorhandling.api.ResolvedErrorCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityChallengeWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.security.RequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthorizationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationPipeline;
import com.smbtech.serviceframework.starter.errorhandling.customizer.StandardErrorMetadataCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonSerializer;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationMetadataKeyNormalizer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

class ErrorHandlingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class))
                    .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void configuresServletErrorHandlingAndSnakeCaseSerializationDefaults() {
        contextRunner.run(
                context ->
                        assertThat(context)
                                .hasSingleBean(ErrorHandlingProperties.class)
                                .hasSingleBean(DefaultNotificationAggregationPolicy.class)
                                .hasSingleBean(DefaultNotificationSanitizer.class)
                                .hasSingleBean(DefaultNotificationHttpStatusResolver.class)
                                .hasSingleBean(DefaultNotificationResponseFactory.class)
                                .hasSingleBean(ServiceExceptionThrowableErrorResolver.class)
                                .hasSingleBean(ValidationExceptionResolver.class)
                                .hasSingleBean(SpringMvcExceptionResolver.class)
                                .hasSingleBean(HttpClientExceptionResolver.class)
                                .hasSingleBean(FallbackThrowableErrorResolver.class)
                                .hasSingleBean(ThrowableErrorResolutionPipeline.class)
                                .hasSingleBean(NotificationMetadataKeyNormalizer.class)
                                .hasSingleBean(NotificationJsonSerializer.class)
                                .hasSingleBean(NotificationResponseWriter.class)
                                .hasSingleBean(NotificationHttpMessageConverter.class)
                                .hasSingleBean(NotificationWebMvcConfigurer.class)
                                .hasSingleBean(ErrorExposurePolicy.class)
                                .hasSingleBean(ConfiguredErrorExposurePolicy.class)
                                .hasSingleBean(ErrorCustomizationPipeline.class)
                                .hasSingleBean(StandardErrorMetadataCustomizer.class)
                                .hasSingleBean(ServiceFrameworkExceptionHandler.class)
                                .hasSingleBean(SecurityAuthenticationEntryPoint.class)
                                .hasSingleBean(SecurityAccessDeniedHandler.class)
                                .hasSingleBean(DefaultSecurityAuthenticationFailureResolver.class)
                                .hasSingleBean(DefaultSecurityAuthorizationFailureResolver.class)
                                .hasSingleBean(DefaultRequiredScopeResolver.class)
                                .hasSingleBean(DefaultOAuth2SecurityChallengeWriter.class)
                                .hasSingleBean(SecurityAuthenticationFailureResolver.class)
                                .hasSingleBean(SecurityAuthorizationFailureResolver.class)
                                .hasSingleBean(RequiredScopeResolver.class)
                                .hasSingleBean(OAuth2SecurityChallengeWriter.class)
                                .hasSingleBean(OAuth2SecurityMetadataFactory.class)
                                .doesNotHaveBean(ErrorReporter.class)
                                .doesNotHaveBean(ErrorMetricsRecorder.class));
    }

    @Test
    void defaultsResponseExposureToInternal() {
        contextRunner.run(
                context -> {
                    assertThat(
                                    context.getBean(ErrorHandlingProperties.class)
                                            .getResponse()
                                            .getExposure())
                            .isEqualTo(ErrorExposure.INTERNAL);

                    ResolvedError source =
                            new ResolvedError(
                                    Notification.error("E_PUBLIC", "Public failure"),
                                    ErrorCategory.VALIDATION,
                                    ErrorExposure.PUBLIC,
                                    "diagnostic");
                    ResolvedError customized =
                            context.getBean(ErrorCustomizationPipeline.class)
                                    .customize(
                                            new IllegalArgumentException("failure"),
                                            source,
                                            new MockHttpServletRequest("GET", "/failure"));

                    assertThat(customized.exposure()).isEqualTo(ErrorExposure.INTERNAL);

                    Notification response =
                            context.getBean(ServiceFrameworkExceptionHandler.class)
                                    .handleException(
                                            new ServiceException(
                                                    Notification.error(
                                                            "E_APPLICATION_PUBLIC",
                                                            "Application failure")),
                                            new MockHttpServletRequest("GET", "/failure"))
                                    .getBody();
                    assertThat(response).isNotNull();
                    assertThat(response.code())
                            .isEqualTo(FallbackThrowableErrorResolver.DEFAULT_ERROR_CODE);
                });
    }

    @Test
    void backsOffForApplicationErrorExposurePolicy() {
        ErrorExposurePolicy applicationPolicy = resolvedError -> ErrorExposure.PUBLIC;

        contextRunner
                .withBean(ErrorExposurePolicy.class, () -> applicationPolicy)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ErrorExposurePolicy.class);
                            assertThat(context.getBean(ErrorExposurePolicy.class))
                                    .isSameAs(applicationPolicy);
                            assertThat(context)
                                    .doesNotHaveBean(ConfiguredErrorExposurePolicy.class);

                            ResolvedError source =
                                    new ResolvedError(
                                            Notification.error("E_INTERNAL", "Internal failure"),
                                            ErrorCategory.INTERNAL,
                                            ErrorExposure.INTERNAL,
                                            "diagnostic");
                            ResolvedError effective =
                                    context.getBean(ErrorCustomizationPipeline.class)
                                            .customize(
                                                    new IllegalStateException("failure"),
                                                    source,
                                                    new MockHttpServletRequest("GET", "/failure"));

                            assertThat(effective.exposure()).isEqualTo(ErrorExposure.PUBLIC);
                        });
    }

    @Test
    void rejectsUnknownResponseExposureWithClearStartupMessage() {
        contextRunner
                .withPropertyValues("smbtech.error-handling.response.exposure=EXTERNAL")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasStackTraceContaining(ErrorExposureConverter.ERROR_MESSAGE);
                        });
    }

    @Test
    void configuresLoggingAndMetricsOnlyWhenTheirInfrastructureExists() {
        contextRunner
                .withBean(StructuredLoggerFactory.class, () -> source -> new NoopStructuredLogger())
                .withBean(
                        CorrelationContext.class,
                        ErrorHandlingAutoConfigurationTest::correlationContext)
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "smbtech.error-handling.logging.include-diagnostics=false",
                        "smbtech.error-handling.metrics.metric-name=application.errors")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ErrorReporter.class);
                            assertThat(context.getBean(ErrorReporter.class))
                                    .isInstanceOf(StructuredErrorReporter.class);
                            assertThat(context).hasSingleBean(ErrorMetricsRecorder.class);
                            assertThat(context.getBean(ErrorMetricsRecorder.class))
                                    .isInstanceOf(MicrometerErrorMetricsRecorder.class);
                            assertThat(
                                            ((MicrometerErrorMetricsRecorder)
                                                            context.getBean(
                                                                    ErrorMetricsRecorder.class))
                                                    .metricName())
                                    .isEqualTo("application.errors");
                            assertThat(
                                            context.getBean(ErrorHandlingProperties.class)
                                                    .getLogging()
                                                    .isIncludeDiagnostics())
                                    .isFalse();
                        });
    }

    @Test
    void recordsSecurityLoggingAndMetricsWithBoundedDimensions() {
        AtomicReference<StructuredEvent> loggedEvent = new AtomicReference<>();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        StructuredLoggerFactory loggerFactory =
                source ->
                        new StructuredLogger() {
                            @Override
                            public boolean isEnabled(LogLevel level, EventType eventType) {
                                return true;
                            }

                            @Override
                            public void log(LogLevel level, StructuredEvent event) {
                                loggedEvent.set(event);
                            }
                        };

        contextRunner
                .withBean(StructuredLoggerFactory.class, () -> loggerFactory)
                .withBean(
                        CorrelationContext.class,
                        () ->
                                correlationContext(
                                        Map.of(
                                                StructuredErrorReporter.TRANSACTION_ID_KEY,
                                                "tx-security-123")))
                .withBean(MeterRegistry.class, () -> meterRegistry)
                .run(
                        context -> {
                            MockHttpServletRequest request =
                                    new MockHttpServletRequest("GET", "/payments/secret-id");
                            request.addHeader(
                                    HttpHeaders.AUTHORIZATION, "Bearer bearer-token-secret");
                            MockHttpServletResponse response = new MockHttpServletResponse();

                            try {
                                context.getBean(SecurityAuthenticationEntryPoint.class)
                                        .commence(
                                                request,
                                                response,
                                                new OAuth2AuthenticationException(
                                                        BearerTokenErrors.invalidToken(
                                                                "token=provider-secret")));
                            } catch (java.io.IOException exception) {
                                throw new AssertionError(exception);
                            }

                            StructuredEvent event = loggedEvent.get();
                            assertThat(event).isNotNull();
                            assertThat(event.data())
                                    .containsEntry(
                                            "code",
                                            SecurityErrorCatalog.BEARER_TOKEN_INVALID.code())
                                    .containsEntry("category", ErrorCategory.AUTHENTICATION.name())
                                    .containsEntry("correlationId", "tx-security-123")
                                    .containsEntry(
                                            "exceptionType",
                                            OAuth2AuthenticationException.class.getName())
                                    .containsEntry("oauth2ErrorCode", "invalid_token")
                                    .containsEntry("securityReason", "invalid_token")
                                    .containsEntry("status", 401);
                            assertThat(event.data().toString())
                                    .doesNotContain("bearer-token-secret", "provider-secret");

                            var counter =
                                    meterRegistry
                                            .find(
                                                    MicrometerErrorMetricsRecorder
                                                            .DEFAULT_METRIC_NAME)
                                            .tags(
                                                    "code",
                                                    SecurityErrorCatalog.BEARER_TOKEN_INVALID
                                                            .code(),
                                                    "category",
                                                    ErrorCategory.AUTHENTICATION.name(),
                                                    "status",
                                                    "401",
                                                    "security_reason",
                                                    "invalid_token")
                                            .counter();
                            assertThat(counter).isNotNull();
                            assertThat(counter.count()).isEqualTo(1.0);
                            assertThat(
                                            counter.getId().getTags().stream()
                                                    .map(Tag::getKey)
                                                    .collect(Collectors.toSet()))
                                    .containsExactlyInAnyOrder(
                                            "code", "category", "status", "security_reason");
                            assertThat(response.getContentAsString())
                                    .doesNotContain("bearer-token-secret", "provider-secret");
                        });
    }

    @Test
    void supportsFeatureFlagsAndGlobalDisable() {
        contextRunner
                .withBean(StructuredLoggerFactory.class, () -> source -> new NoopStructuredLogger())
                .withBean(
                        CorrelationContext.class,
                        ErrorHandlingAutoConfigurationTest::correlationContext)
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "smbtech.error-handling.logging.enabled=false",
                        "smbtech.error-handling.metrics.enabled=false",
                        "smbtech.error-handling.security.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(ErrorReporter.class)
                                        .doesNotHaveBean(ErrorMetricsRecorder.class)
                                        .doesNotHaveBean(SecurityAuthenticationEntryPoint.class)
                                        .doesNotHaveBean(SecurityAccessDeniedHandler.class)
                                        .hasSingleBean(ServiceFrameworkExceptionHandler.class));

        contextRunner
                .withPropertyValues("smbtech.error-handling.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(ErrorHandlingProperties.class)
                                        .doesNotHaveBean(ServiceFrameworkExceptionHandler.class));
    }

    @Test
    void bindsOAuth2MetadataExposureSettings() {
        contextRunner
                .withPropertyValues(
                        "smbtech.error-handling.security.oauth2-metadata.enabled=true",
                        "smbtech.error-handling.security.oauth2-metadata.include-error-description=false",
                        "smbtech.error-handling.security.oauth2-metadata.include-error-uri=false",
                        "smbtech.error-handling.security.oauth2-metadata.include-required-scope=true")
                .run(
                        context -> {
                            ErrorHandlingProperties.OAuth2Metadata metadata =
                                    context.getBean(ErrorHandlingProperties.class)
                                            .getSecurity()
                                            .getOauth2Metadata();

                            assertThat(metadata.isEnabled()).isTrue();
                            assertThat(metadata.isIncludeErrorDescription()).isFalse();
                            assertThat(metadata.isIncludeErrorUri()).isFalse();
                            assertThat(metadata.isIncludeRequiredScope()).isTrue();
                            assertThat(context).hasSingleBean(OAuth2SecurityMetadataFactory.class);
                        });
    }

    @Test
    void appliesOAuth2MetadataExposureSettingsToSecurityResponses() {
        contextRunner
                .withPropertyValues(
                        "smbtech.error-handling.response.exposure=PUBLIC",
                        "smbtech.error-handling.security.oauth2-metadata.include-error-description=false",
                        "smbtech.error-handling.security.oauth2-metadata.include-error-uri=false")
                .run(
                        context -> {
                            SecurityAuthenticationEntryPoint entryPoint =
                                    context.getBean(SecurityAuthenticationEntryPoint.class);
                            MockHttpServletRequest request =
                                    new MockHttpServletRequest("GET", "/secure/secret-id");
                            request.addHeader(
                                    HttpHeaders.AUTHORIZATION, "Bearer bearer-token-secret");
                            MockHttpServletResponse response = new MockHttpServletResponse();

                            try {
                                entryPoint.commence(
                                        request,
                                        response,
                                        new OAuth2AuthenticationException(
                                                BearerTokenErrors.invalidToken("provider-secret")));
                                JsonNode json =
                                        context.getBean(ObjectMapper.class)
                                                .readTree(response.getContentAsByteArray());

                                assertThat(json.at("/metadata/oauth2/error").asText())
                                        .isEqualTo("invalid_token");
                                assertThat(
                                                json.at("/metadata/oauth2/error_description")
                                                        .isMissingNode())
                                        .isTrue();
                                assertThat(json.at("/metadata/oauth2/error_uri").isMissingNode())
                                        .isTrue();
                                assertThat(json.at("/metadata/security/reason").asText())
                                        .isEqualTo("invalid_token");
                                assertThat(response.getContentAsString())
                                        .doesNotContain(
                                                "secret-id",
                                                "bearer-token-secret",
                                                "provider-secret");
                            } catch (java.io.IOException exception) {
                                throw new AssertionError(exception);
                            }
                        });
    }

    @Test
    void customAllowlistStillRedactsRestrictedAndSensitiveValues() {
        contextRunner
                .withPropertyValues(
                        "smbtech.error-handling.response.metadata-allowlist=context,responseBody")
                .run(
                        context -> {
                            Notification source =
                                    Notification.builder()
                                            .code("E_SECURITY")
                                            .message("Failure")
                                            .metadata(
                                                    Map.of(
                                                            "context",
                                                                    Map.of(
                                                                            "sessionId",
                                                                                    "session-secret",
                                                                            "cookie",
                                                                                    "cookie-secret",
                                                                            "safe", "public"),
                                                            "responseBody", "body-secret",
                                                            "unknown", "must-be-removed"))
                                            .build();
                            ResolvedError resolvedError =
                                    new ResolvedError(
                                            source,
                                            ErrorCategory.INTERNAL,
                                            ErrorExposure.PUBLIC,
                                            "diagnostic-secret");

                            Notification body =
                                    context.getBean(NotificationResponseFactory.class)
                                            .create(resolvedError)
                                            .getBody();

                            assertThat(body).isNotNull();
                            assertThat(body.metadata()).doesNotContainKey("unknown");
                            assertThat(body.metadata().get("responseBody"))
                                    .isEqualTo(DefaultNotificationSanitizer.REDACTED_VALUE);
                            Map<?, ?> sanitizedContext = (Map<?, ?>) body.metadata().get("context");
                            assertThat(sanitizedContext.get("sessionId"))
                                    .isEqualTo(DefaultNotificationSanitizer.REDACTED_VALUE);
                            assertThat(sanitizedContext.get("cookie"))
                                    .isEqualTo(DefaultNotificationSanitizer.REDACTED_VALUE);
                            assertThat(sanitizedContext.get("safe")).isEqualTo("public");
                        });
    }

    @Test
    void activatesOnlyForServletApplicationsAndBacksOffWithoutSecurity() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class))
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(ServiceFrameworkExceptionHandler.class));

        contextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.security"))
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(ServiceFrameworkExceptionHandler.class)
                                        .doesNotHaveBean(SecurityAuthenticationEntryPoint.class)
                                        .doesNotHaveBean(SecurityAccessDeniedHandler.class));

        contextRunner
                .withClassLoader(new FilteredClassLoader("com.smbtech.serviceframework.httpclient"))
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(ServiceFrameworkExceptionHandler.class)
                                        .doesNotHaveBean(HttpClientExceptionResolver.class));
    }

    @Test
    void keepsGenericSecurityHandlersWhenOAuth2ResourceServerIsAbsent() {
        contextRunner
                .withClassLoader(
                        new FilteredClassLoader(
                                "org.springframework.security.oauth2.core",
                                "org.springframework.security.oauth2.server.resource"))
                .withPropertyValues("smbtech.error-handling.response.exposure=PUBLIC")
                .run(
                        context -> {
                            assertThat(context)
                                    .hasSingleBean(SecurityAuthenticationEntryPoint.class)
                                    .hasSingleBean(SecurityAccessDeniedHandler.class)
                                    .hasSingleBean(SecurityAuthenticationFailureResolver.class)
                                    .hasSingleBean(OAuth2SecurityChallengeWriter.class)
                                    .doesNotHaveBean(
                                            DefaultSecurityAuthenticationFailureResolver.class)
                                    .doesNotHaveBean(DefaultOAuth2SecurityChallengeWriter.class);

                            MockHttpServletResponse response = new MockHttpServletResponse();
                            try {
                                context.getBean(SecurityAuthenticationEntryPoint.class)
                                        .commence(
                                                new MockHttpServletRequest("GET", "/secure"),
                                                response,
                                                new BadCredentialsException("invalid"));
                            } catch (java.io.IOException exception) {
                                throw new AssertionError(exception);
                            }
                            assertThat(response.getStatus()).isEqualTo(401);
                            assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
                            assertThat(response.getContentAsString())
                                    .contains("E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0001");
                        });
    }

    @Test
    void backsOffForApplicationBeans() {
        NotificationSanitizer sanitizer = notification -> notification;
        NotificationHttpStatusResolver statusResolver =
                resolvedError -> org.springframework.http.HttpStatusCode.valueOf(499);
        NotificationResponseFactory responseFactory =
                resolvedError ->
                        org.springframework.http.ResponseEntity.status(499)
                                .body(resolvedError.notification());
        ErrorReporter reporter = (cause, resolvedError, request) -> {};
        ErrorMetricsRecorder metricsRecorder = (resolvedError, statusCode) -> {};
        NotificationJsonSerializer serializer = new NotificationJsonSerializer();
        NotificationAggregationPolicy aggregationPolicy =
                (notifications, category, exposure, diagnosticMessage) ->
                        new ResolvedError(
                                notifications.getFirst(), category, exposure, diagnosticMessage);
        OAuth2SecurityMetadataFactory securityMetadataFactory =
                (securityContext, resolution) ->
                        com.smbtech.serviceframework.error.metadata.StandardErrorMetadata.builder(
                                        resolution.resolvedError().category())
                                .build();

        contextRunner
                .withBean(NotificationSanitizer.class, () -> sanitizer)
                .withBean(NotificationHttpStatusResolver.class, () -> statusResolver)
                .withBean(NotificationResponseFactory.class, () -> responseFactory)
                .withBean(ErrorReporter.class, () -> reporter)
                .withBean(ErrorMetricsRecorder.class, () -> metricsRecorder)
                .withBean(NotificationJsonSerializer.class, () -> serializer)
                .withBean(NotificationAggregationPolicy.class, () -> aggregationPolicy)
                .withBean(OAuth2SecurityMetadataFactory.class, () -> securityMetadataFactory)
                .run(
                        context -> {
                            assertThat(context)
                                    .getBean(NotificationSanitizer.class)
                                    .isSameAs(sanitizer);
                            assertThat(context)
                                    .getBean(NotificationHttpStatusResolver.class)
                                    .isSameAs(statusResolver);
                            assertThat(context)
                                    .getBean(NotificationResponseFactory.class)
                                    .isSameAs(responseFactory);
                            assertThat(context).getBean(ErrorReporter.class).isSameAs(reporter);
                            assertThat(context)
                                    .getBean(ErrorMetricsRecorder.class)
                                    .isSameAs(metricsRecorder);
                            assertThat(context)
                                    .getBean(NotificationJsonSerializer.class)
                                    .isSameAs(serializer);
                            assertThat(context)
                                    .getBean(NotificationAggregationPolicy.class)
                                    .isSameAs(aggregationPolicy);
                            assertThat(context.getBean(OAuth2SecurityMetadataFactory.class))
                                    .isSameAs(securityMetadataFactory);
                            assertThat(context).hasSingleBean(NotificationSanitizer.class);
                            assertThat(context).hasSingleBean(NotificationResponseFactory.class);
                        });
    }

    @Test
    void discoversResolversCustomizersAndMultipleReporters() {
        AtomicInteger reporterCalls = new AtomicInteger();
        ThrowableErrorResolver resolver =
                new ThrowableErrorResolver() {
                    @Override
                    public boolean supports(Throwable throwable) {
                        return throwable instanceof IllegalArgumentException;
                    }

                    @Override
                    public ResolvedError resolve(Throwable throwable) {
                        return new ResolvedError(
                                com.smbtech.serviceframework.commons.notification.Notification
                                        .error("E_CUSTOM_RESOLVER", "Custom resolver"),
                                ErrorCategory.VALIDATION,
                                ErrorExposure.PUBLIC,
                                "custom diagnostic");
                    }

                    @Override
                    public int order() {
                        return -2_000;
                    }
                };
        ResolvedErrorCustomizer resolvedCustomizer =
                (cause, error, request) ->
                        new ResolvedError(
                                com.smbtech.serviceframework.commons.notification.Notification
                                        .error("E_CUSTOMIZED", error.notification().message()),
                                error.category(),
                                error.exposure(),
                                error.diagnosticMessage(),
                                error.fieldViolations());
        NotificationResponseCustomizer responseCustomizer =
                (response, error, request) ->
                        org.springframework.http.ResponseEntity.status(422)
                                .headers(response.getHeaders())
                                .header("X-Error-Customized", "true")
                                .body(response.getBody());
        ErrorReporter firstReporter = (cause, error, request) -> reporterCalls.incrementAndGet();
        ErrorReporter secondReporter = (cause, error, request) -> reporterCalls.incrementAndGet();

        contextRunner
                .withPropertyValues("smbtech.error-handling.response.exposure=PUBLIC")
                .withBean(ThrowableErrorResolver.class, () -> resolver)
                .withBean(ResolvedErrorCustomizer.class, () -> resolvedCustomizer)
                .withBean(NotificationResponseCustomizer.class, () -> responseCustomizer)
                .withBean("firstErrorReporter", ErrorReporter.class, () -> firstReporter)
                .withBean("secondErrorReporter", ErrorReporter.class, () -> secondReporter)
                .run(
                        context -> {
                            ThrowableErrorResolutionPipeline pipeline =
                                    context.getBean(ThrowableErrorResolutionPipeline.class);
                            assertThat(pipeline.resolvers()).contains(resolver);

                            org.springframework.http.ResponseEntity<
                                            com.smbtech.serviceframework.commons.notification
                                                    .Notification>
                                    response =
                                            context.getBean(ServiceFrameworkExceptionHandler.class)
                                                    .handleException(
                                                            new IllegalArgumentException("failure"),
                                                            new MockHttpServletRequest(
                                                                    "GET", "/custom"));
                            assertThat(response.getStatusCode().value()).isEqualTo(422);
                            assertThat(response.getHeaders().getFirst("X-Error-Customized"))
                                    .isEqualTo("true");
                            assertThat(response.getBody().code()).isEqualTo("E_CUSTOMIZED");
                            assertThat(reporterCalls.get()).isEqualTo(2);
                        });
    }

    @Test
    void composesApplicationReporterWithStructuredLoggingReporter() {
        AtomicInteger applicationReports = new AtomicInteger();
        AtomicInteger structuredReports = new AtomicInteger();
        ErrorReporter applicationReporter =
                (cause, error, request) -> applicationReports.incrementAndGet();
        StructuredLoggerFactory loggerFactory =
                source ->
                        new StructuredLogger() {
                            @Override
                            public boolean isEnabled(LogLevel level, EventType eventType) {
                                return true;
                            }

                            @Override
                            public void log(LogLevel level, StructuredEvent event) {
                                structuredReports.incrementAndGet();
                            }
                        };

        contextRunner
                .withBean(StructuredLoggerFactory.class, () -> loggerFactory)
                .withBean(
                        CorrelationContext.class,
                        ErrorHandlingAutoConfigurationTest::correlationContext)
                .withBean(ErrorReporter.class, () -> applicationReporter)
                .run(
                        context -> {
                            assertThat(context.getBeansOfType(ErrorReporter.class)).hasSize(2);
                            context.getBean(ServiceFrameworkExceptionHandler.class)
                                    .handleException(
                                            new IllegalStateException("failure"),
                                            new MockHttpServletRequest("GET", "/failure"));
                            assertThat(applicationReports.get()).isEqualTo(1);
                            assertThat(structuredReports.get()).isEqualTo(1);
                        });
    }

    @Test
    void replacesDefaultSerializerThroughItsPublicContract() throws Exception {
        NotificationSerializer serializer =
                (notification, generator, serializers) -> {
                    generator.writeStartObject();
                    generator.writeStringField("custom_code", notification.code());
                    generator.writeEndObject();
                };

        contextRunner
                .withBean(NotificationSerializer.class, () -> serializer)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(NotificationSerializer.class);
                            assertThat(context)
                                    .getBean(NotificationSerializer.class)
                                    .isSameAs(serializer);
                            assertThat(context).doesNotHaveBean(NotificationJsonSerializer.class);

                            MockHttpServletResponse servletResponse = new MockHttpServletResponse();
                            try {
                                context.getBean(NotificationResponseWriter.class)
                                        .write(
                                                org.springframework.http.ResponseEntity.badRequest()
                                                        .body(
                                                                com.smbtech.serviceframework.commons
                                                                        .notification.Notification
                                                                        .error(
                                                                                "E_CUSTOM_JSON",
                                                                                "failure")),
                                                servletResponse);
                            } catch (java.io.IOException exception) {
                                throw new AssertionError(exception);
                            }
                            assertThat(servletResponse.getContentAsString())
                                    .isEqualTo("{\"custom_code\":\"E_CUSTOM_JSON\"}");
                        });
    }

    @Test
    void backsOffForApplicationSecurityEntryPoint() {
        AuthenticationEntryPoint entryPoint =
                (request, response, exception) -> response.setStatus(498);

        contextRunner
                .withBean(AuthenticationEntryPoint.class, () -> entryPoint)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(AuthenticationEntryPoint.class);
                            assertThat(context)
                                    .getBean(AuthenticationEntryPoint.class)
                                    .isSameAs(entryPoint);
                            assertThat(context)
                                    .doesNotHaveBean(SecurityAuthenticationEntryPoint.class);
                            assertThat(context).hasSingleBean(SecurityAccessDeniedHandler.class);
                        });
    }

    @Test
    void backsOffForApplicationAccessDeniedHandler() {
        AccessDeniedHandler accessDeniedHandler =
                (request, response, exception) -> response.setStatus(497);

        contextRunner
                .withBean(AccessDeniedHandler.class, () -> accessDeniedHandler)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(AccessDeniedHandler.class);
                            assertThat(context)
                                    .getBean(AccessDeniedHandler.class)
                                    .isSameAs(accessDeniedHandler);
                            assertThat(context).doesNotHaveBean(SecurityAccessDeniedHandler.class);
                            assertThat(context)
                                    .hasSingleBean(SecurityAuthenticationEntryPoint.class);
                        });
    }

    @Test
    void wiresApplicationSecurityExtensionBeansIntoHandlers() {
        AtomicInteger authenticationResolutions = new AtomicInteger();
        AtomicInteger authorizationResolutions = new AtomicInteger();
        AtomicInteger requiredScopeResolutions = new AtomicInteger();
        AtomicInteger challengeWrites = new AtomicInteger();
        SecurityAuthenticationFailureResolver authenticationResolver =
                securityContext -> {
                    authenticationResolutions.incrementAndGet();
                    return new SecurityFailureResolution(
                            new ResolvedError(
                                    Notification.error(
                                            "E_APPLICATION_AUTHENTICATION",
                                            "Application authentication"),
                                    ErrorCategory.AUTHENTICATION,
                                    ErrorExposure.PUBLIC,
                                    "application authentication diagnostic"),
                            SecurityFailureReason.BEARER_TOKEN_INVALID,
                            OAuth2SecurityError.invalidToken(),
                            true);
                };
        RequiredScopeResolver requiredScopeResolver =
                (request, authentication) -> {
                    requiredScopeResolutions.incrementAndGet();
                    return Set.of("payment.write");
                };
        SecurityAuthorizationFailureResolver authorizationResolver =
                securityContext -> {
                    authorizationResolutions.incrementAndGet();
                    assertThat(securityContext.requiredScopes()).containsExactly("payment.write");
                    return new SecurityFailureResolution(
                            new ResolvedError(
                                    Notification.error(
                                            "E_APPLICATION_AUTHORIZATION",
                                            "Application authorization"),
                                    ErrorCategory.AUTHORIZATION,
                                    ErrorExposure.PUBLIC,
                                    "application authorization diagnostic"),
                            SecurityFailureReason.INSUFFICIENT_SCOPE,
                            OAuth2SecurityError.insufficientScope(securityContext.requiredScopes()),
                            true);
                };
        OAuth2SecurityChallengeWriter challengeWriter =
                (request, response, securityContext, resolution) -> {
                    challengeWrites.incrementAndGet();
                    response.setHeader("X-Application-Challenge", resolution.reason().name());
                };

        contextRunner
                .withPropertyValues("smbtech.error-handling.response.exposure=PUBLIC")
                .withBean(SecurityAuthenticationFailureResolver.class, () -> authenticationResolver)
                .withBean(SecurityAuthorizationFailureResolver.class, () -> authorizationResolver)
                .withBean(RequiredScopeResolver.class, () -> requiredScopeResolver)
                .withBean(OAuth2SecurityChallengeWriter.class, () -> challengeWriter)
                .run(
                        context -> {
                            assertThat(context.getBean(SecurityAuthenticationFailureResolver.class))
                                    .isSameAs(authenticationResolver);
                            assertThat(context.getBean(SecurityAuthorizationFailureResolver.class))
                                    .isSameAs(authorizationResolver);
                            assertThat(context.getBean(RequiredScopeResolver.class))
                                    .isSameAs(requiredScopeResolver);
                            assertThat(context.getBean(OAuth2SecurityChallengeWriter.class))
                                    .isSameAs(challengeWriter);
                            assertThat(context)
                                    .doesNotHaveBean(
                                            DefaultSecurityAuthenticationFailureResolver.class);
                            assertThat(context)
                                    .doesNotHaveBean(
                                            DefaultSecurityAuthorizationFailureResolver.class);
                            assertThat(context).doesNotHaveBean(DefaultRequiredScopeResolver.class);
                            assertThat(context)
                                    .doesNotHaveBean(DefaultOAuth2SecurityChallengeWriter.class);

                            MockHttpServletResponse unauthorized = new MockHttpServletResponse();
                            MockHttpServletResponse forbidden = new MockHttpServletResponse();
                            try {
                                context.getBean(SecurityAuthenticationEntryPoint.class)
                                        .commence(
                                                new MockHttpServletRequest("GET", "/secure"),
                                                unauthorized,
                                                new BadCredentialsException("invalid"));
                                context.getBean(SecurityAccessDeniedHandler.class)
                                        .handle(
                                                new MockHttpServletRequest("GET", "/payments"),
                                                forbidden,
                                                new AccessDeniedException("denied"));
                            } catch (java.io.IOException exception) {
                                throw new AssertionError(exception);
                            }

                            assertThat(authenticationResolutions).hasValue(1);
                            assertThat(authorizationResolutions).hasValue(1);
                            assertThat(requiredScopeResolutions).hasValue(1);
                            assertThat(challengeWrites).hasValue(2);
                            assertThat(unauthorized.getStatus()).isEqualTo(401);
                            assertThat(forbidden.getStatus()).isEqualTo(403);
                            assertThat(unauthorized.getContentAsString())
                                    .contains("E_APPLICATION_AUTHENTICATION");
                            assertThat(forbidden.getContentAsString())
                                    .contains("E_APPLICATION_AUTHORIZATION");
                            assertThat(unauthorized.getHeader("X-Application-Challenge"))
                                    .isEqualTo("BEARER_TOKEN_INVALID");
                            assertThat(forbidden.getHeader("X-Application-Challenge"))
                                    .isEqualTo("INSUFFICIENT_SCOPE");
                        });
    }

    @Test
    void bindsResponsePropertiesAndOmitsFieldViolationsWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "smbtech.error-handling.response.exposure=PUBLIC",
                        "smbtech.error-handling.response.include-field-violations=false",
                        "smbtech.error-handling.response.metadata-allowlist=path,correlationId")
                .run(
                        context -> {
                            ErrorHandlingProperties properties =
                                    context.getBean(ErrorHandlingProperties.class);
                            assertThat(properties.getResponse().getExposure())
                                    .isEqualTo(ErrorExposure.PUBLIC);
                            assertThat(properties.getResponse().isIncludeFieldViolations())
                                    .isFalse();
                            assertThat(properties.getResponse().getMetadataAllowlist())
                                    .containsExactlyInAnyOrder("path", "correlationId");
                            DefaultNotificationSanitizer sanitizer =
                                    context.getBean(DefaultNotificationSanitizer.class);
                            assertThat(sanitizer.metadataAllowlist())
                                    .containsExactlyInAnyOrder("path", "correlationId");
                            NotificationResponseFactory responseFactory =
                                    context.getBean(NotificationResponseFactory.class);
                            ResolvedError error =
                                    new ResolvedError(
                                            com.smbtech.serviceframework.commons.notification
                                                    .Notification.error(
                                                    "E_REQUEST_0001", "Validation failed"),
                                            ErrorCategory.VALIDATION,
                                            ErrorExposure.PUBLIC,
                                            "diagnostic",
                                            List.of(
                                                    new FieldViolation(
                                                            "customerId", "required", "Required")));
                            assertThat(responseFactory.create(error).getBody().metadata())
                                    .doesNotContainKey("violations");
                        });
    }

    private static CorrelationContext correlationContext() {
        return correlationContext(Map.of());
    }

    private static CorrelationContext correlationContext(Map<String, String> values) {
        return new CorrelationContext() {
            @Override
            public Map<String, String> snapshot() {
                return values;
            }

            @Override
            public Scope open(Map<String, String> values) {
                return () -> {};
            }
        };
    }

    private static final class NoopStructuredLogger implements StructuredLogger {
        @Override
        public boolean isEnabled(LogLevel level, EventType eventType) {
            return true;
        }

        @Override
        public void log(LogLevel level, StructuredEvent event) {}
    }
}
