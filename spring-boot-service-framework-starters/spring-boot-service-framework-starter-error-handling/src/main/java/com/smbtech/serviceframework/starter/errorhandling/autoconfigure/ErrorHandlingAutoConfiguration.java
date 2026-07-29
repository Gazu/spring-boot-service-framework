package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.DefaultNotificationAggregationPolicy;
import com.smbtech.serviceframework.error.DefaultNotificationSanitizer;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FallbackThrowableErrorResolver;
import com.smbtech.serviceframework.error.NotificationAggregationPolicy;
import com.smbtech.serviceframework.error.NotificationSanitizer;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ServiceExceptionThrowableErrorResolver;
import com.smbtech.serviceframework.error.ThrowableErrorResolutionPipeline;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultOAuth2SecurityChallengeWriter;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultOAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultRequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultSecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultSecurityAuthorizationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.SecurityAccessDeniedHandler;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.SecurityAuthenticationEntryPoint;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.DefaultNotificationHttpStatusResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.DefaultNotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.FinalNotificationResponseSanitizer;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.HttpClientExceptionResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.NotificationHttpMessageConverter;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.NotificationWebMvcConfigurer;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.ServiceFrameworkExceptionHandler;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.SpringMvcExceptionResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.ValidationExceptionResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.out.logging.StructuredErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.adapter.out.metrics.MicrometerErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.CompositeErrorReporter;
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
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.security.RequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthorizationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationPipeline;
import com.smbtech.serviceframework.starter.errorhandling.customizer.StandardErrorMetadataCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonSerializer;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationMetadataKeyNormalizer;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.DispatcherServlet;
import tools.jackson.databind.ObjectMapper;

/** Auto-configures reusable servlet error handling with replaceable defaults. */
@AutoConfiguration(
        afterName = {
            "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
            "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration"
        })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({DispatcherServlet.class, Notification.class})
@ConditionalOnProperty(
        prefix = "smbtech.error-handling",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(ErrorHandlingProperties.class)
@ImportRuntimeHints(ErrorHandlingRuntimeHints.class)
public class ErrorHandlingAutoConfiguration {
    /** Creates a error handling auto configuration instance. */
    public ErrorHandlingAutoConfiguration() {}

    @Bean
    @ConfigurationPropertiesBinding
    static ErrorExposureConverter errorExposureConverter() {
        return new ErrorExposureConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationAggregationPolicy notificationAggregationPolicy() {
        return new DefaultNotificationAggregationPolicy();
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationSanitizer notificationSanitizer(ErrorHandlingProperties properties) {
        return new DefaultNotificationSanitizer(properties.getResponse().getMetadataAllowlist());
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationHttpStatusResolver notificationHttpStatusResolver() {
        return new DefaultNotificationHttpStatusResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationResponseFactory notificationResponseFactory(
            NotificationHttpStatusResolver statusResolver,
            NotificationSanitizer notificationSanitizer,
            ErrorHandlingProperties properties) {
        return new DefaultNotificationResponseFactory(
                statusResolver,
                notificationSanitizer,
                properties.getResponse().isIncludeFieldViolations());
    }

    @Bean
    FinalNotificationResponseSanitizer finalNotificationResponseSanitizer(
            ErrorHandlingProperties properties) {
        return new FinalNotificationResponseSanitizer(
                new DefaultNotificationSanitizer(properties.getResponse().getMetadataAllowlist()),
                properties.getResponse().isIncludeFieldViolations());
    }

    @Bean
    @ConditionalOnMissingBean
    ServiceExceptionThrowableErrorResolver serviceExceptionThrowableErrorResolver(
            NotificationAggregationPolicy aggregationPolicy) {
        return new ServiceExceptionThrowableErrorResolver(aggregationPolicy);
    }

    @Bean
    @ConditionalOnMissingBean
    ValidationExceptionResolver validationExceptionResolver() {
        return new ValidationExceptionResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    SpringMvcExceptionResolver springMvcExceptionResolver() {
        return new SpringMvcExceptionResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    FallbackThrowableErrorResolver fallbackThrowableErrorResolver() {
        return new FallbackThrowableErrorResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    ThrowableErrorResolutionPipeline throwableErrorResolutionPipeline(
            ObjectProvider<ThrowableErrorResolver> resolvers,
            FallbackThrowableErrorResolver fallbackResolver) {
        List<ThrowableErrorResolver> specializedResolvers =
                resolvers.stream().filter(resolver -> resolver != fallbackResolver).toList();
        return new ThrowableErrorResolutionPipeline(specializedResolvers, fallbackResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationMetadataKeyNormalizer notificationMetadataKeyNormalizer() {
        return new NotificationMetadataKeyNormalizer();
    }

    @Bean
    @ConditionalOnMissingBean(NotificationSerializer.class)
    NotificationJsonSerializer notificationJsonSerializer(
            NotificationMetadataKeyNormalizer metadataKeyNormalizer) {
        return new NotificationJsonSerializer(metadataKeyNormalizer);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationResponseWriter.class)
    NotificationResponseWriter notificationResponseWriter(
            ObjectProvider<ObjectMapper> objectMapper, NotificationSerializer serializer) {
        return new NotificationJsonResponseWriter(
                objectMapper.getIfAvailable(ObjectMapper::new), serializer);
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationHttpMessageConverter notificationHttpMessageConverter(
            ObjectProvider<ObjectMapper> objectMapper, NotificationSerializer serializer) {
        return new NotificationHttpMessageConverter(
                objectMapper.getIfAvailable(ObjectMapper::new), serializer);
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationWebMvcConfigurer notificationWebMvcConfigurer(
            NotificationHttpMessageConverter notificationConverter) {
        return new NotificationWebMvcConfigurer(notificationConverter);
    }

    @Bean
    @ConditionalOnMissingBean
    StandardErrorMetadataCustomizer standardErrorMetadataCustomizer(
            ObjectProvider<CorrelationContext> correlationContext) {
        return correlationContext
                .orderedStream()
                .findFirst()
                .map(StandardErrorMetadataCustomizer::new)
                .orElseGet(StandardErrorMetadataCustomizer::new);
    }

    @Bean
    @ConditionalOnMissingBean
    ErrorExposurePolicy errorExposurePolicy(ErrorHandlingProperties properties) {
        return new ConfiguredErrorExposurePolicy(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    ErrorCustomizationPipeline errorCustomizationPipeline(
            ObjectProvider<ResolvedErrorCustomizer> resolvedErrorCustomizers,
            ObjectProvider<NotificationResponseCustomizer> responseCustomizers,
            ErrorExposurePolicy errorExposurePolicy) {
        return new ErrorCustomizationPipeline(
                resolvedErrorCustomizers.stream().toList(),
                responseCustomizers.stream().toList(),
                errorExposurePolicy);
    }

    @Bean
    @ConditionalOnMissingBean
    ServiceFrameworkExceptionHandler serviceFrameworkExceptionHandler(
            ThrowableErrorResolutionPipeline resolutionPipeline,
            NotificationResponseFactory responseFactory,
            ObjectProvider<ErrorReporter> errorReporter,
            ObjectProvider<ErrorMetricsRecorder> metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline,
            FinalNotificationResponseSanitizer finalResponseSanitizer) {
        return new ServiceFrameworkExceptionHandler(
                resolutionPipeline,
                responseFactory,
                composeReporters(errorReporter),
                metricsRecorder.getIfAvailable(ErrorMetricsRecorder::noop),
                customizationPipeline,
                finalResponseSanitizer);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(
            name = "com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException")
    static class HttpClientConfiguration {

        @Bean
        @ConditionalOnMissingBean
        HttpClientExceptionResolver httpClientExceptionResolver() {
            return new HttpClientExceptionResolver();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({StructuredLoggerFactory.class, CorrelationContext.class})
    static class LoggingConfiguration {

        @Bean
        @ConditionalOnBean({StructuredLoggerFactory.class, CorrelationContext.class})
        @ConditionalOnMissingBean(StructuredErrorReporter.class)
        @ConditionalOnProperty(
                prefix = "smbtech.error-handling.logging",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true)
        StructuredErrorReporter structuredErrorReporter(
                StructuredLoggerFactory loggerFactory,
                CorrelationContext correlationContext,
                ErrorHandlingProperties properties) {
            return new StructuredErrorReporter(
                    loggerFactory.get(StructuredErrorReporter.class),
                    correlationContext,
                    properties.getLogging().isIncludeDiagnostics());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    static class MetricsConfiguration {

        @Bean
        @ConditionalOnBean(MeterRegistry.class)
        @ConditionalOnMissingBean(ErrorMetricsRecorder.class)
        @ConditionalOnProperty(
                prefix = "smbtech.error-handling.metrics",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true)
        ErrorMetricsRecorder errorMetricsRecorder(
                MeterRegistry meterRegistry, ErrorHandlingProperties properties) {
            return new MicrometerErrorMetricsRecorder(
                    meterRegistry, properties.getMetrics().getMetricName());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({AuthenticationEntryPoint.class, AccessDeniedHandler.class})
    @ConditionalOnProperty(
            prefix = "smbtech.error-handling.security",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    static class SecurityConfiguration {

        @Bean
        @ConditionalOnMissingBean
        SecurityAuthorizationFailureResolver securityAuthorizationFailureResolver() {
            return new DefaultSecurityAuthorizationFailureResolver();
        }

        @Bean
        @ConditionalOnMissingBean
        RequiredScopeResolver requiredScopeResolver() {
            return new DefaultRequiredScopeResolver();
        }

        @Bean
        @ConditionalOnMissingBean
        OAuth2SecurityMetadataFactory OAuth2SecurityMetadataFactory(
                ErrorHandlingProperties properties) {
            ErrorHandlingProperties.OAuth2Metadata metadata =
                    properties.getSecurity().getOauth2Metadata();
            return new DefaultOAuth2SecurityMetadataFactory(
                    metadata.isEnabled(),
                    metadata.isIncludeErrorDescription(),
                    metadata.isIncludeErrorUri(),
                    metadata.isIncludeRequiredScope());
        }

        @Bean
        @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
        SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint(
                NotificationResponseFactory responseFactory,
                NotificationResponseWriter responseWriter,
                ObjectProvider<ErrorReporter> errorReporter,
                ObjectProvider<ErrorMetricsRecorder> metricsRecorder,
                ErrorCustomizationPipeline customizationPipeline,
                SecurityAuthenticationFailureResolver failureResolver,
                OAuth2SecurityMetadataFactory metadataFactory,
                OAuth2SecurityChallengeWriter challengeWriter,
                FinalNotificationResponseSanitizer finalResponseSanitizer) {
            return new SecurityAuthenticationEntryPoint(
                    responseFactory,
                    responseWriter,
                    composeReporters(errorReporter),
                    metricsRecorder.getIfAvailable(ErrorMetricsRecorder::noop),
                    customizationPipeline,
                    failureResolver,
                    metadataFactory,
                    challengeWriter,
                    finalResponseSanitizer);
        }

        @Bean
        @ConditionalOnMissingBean(AccessDeniedHandler.class)
        SecurityAccessDeniedHandler securityAccessDeniedHandler(
                NotificationResponseFactory responseFactory,
                NotificationResponseWriter responseWriter,
                ObjectProvider<ErrorReporter> errorReporter,
                ObjectProvider<ErrorMetricsRecorder> metricsRecorder,
                ErrorCustomizationPipeline customizationPipeline,
                SecurityAuthorizationFailureResolver failureResolver,
                RequiredScopeResolver requiredScopeResolver,
                OAuth2SecurityMetadataFactory metadataFactory,
                OAuth2SecurityChallengeWriter challengeWriter,
                FinalNotificationResponseSanitizer finalResponseSanitizer) {
            return new SecurityAccessDeniedHandler(
                    responseFactory,
                    responseWriter,
                    composeReporters(errorReporter),
                    metricsRecorder.getIfAvailable(ErrorMetricsRecorder::noop),
                    customizationPipeline,
                    failureResolver,
                    requiredScopeResolver,
                    metadataFactory,
                    challengeWriter,
                    finalResponseSanitizer);
        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(
                name = "org.springframework.security.oauth2.server.resource.BearerTokenError")
        static class OAuth2AuthenticationResolutionConfiguration {

            @Bean
            @ConditionalOnMissingBean
            SecurityAuthenticationFailureResolver securityAuthenticationFailureResolver() {
                return new DefaultSecurityAuthenticationFailureResolver();
            }
        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnMissingClass(
                "org.springframework.security.oauth2.server.resource.BearerTokenError")
        static class GenericAuthenticationResolutionConfiguration {

            @Bean
            @ConditionalOnMissingBean
            SecurityAuthenticationFailureResolver securityAuthenticationFailureResolver() {
                return context -> {
                    if (!(context.failure() instanceof AuthenticationException exception)) {
                        throw new IllegalArgumentException(
                                "context failure must be an AuthenticationException");
                    }
                    SecurityErrorCatalog definition = SecurityErrorCatalog.AUTHENTICATION_REQUIRED;
                    return new SecurityFailureResolution(
                            new ResolvedError(
                                    Notification.builder()
                                            .code(definition.code())
                                            .message(definition.publicMessage())
                                            .severity(definition.severity())
                                            .build(),
                                    definition.category(),
                                    ErrorExposure.PUBLIC,
                                    diagnosticMessage(exception)),
                            SecurityFailureReason.AUTHENTICATION_REQUIRED);
                };
            }
        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(
                name = {
                    "org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint",
                    "org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler"
                })
        static class OAuth2ChallengeConfiguration {

            @Bean
            @ConditionalOnMissingBean
            OAuth2SecurityChallengeWriter OAuth2SecurityChallengeWriter() {
                return new DefaultOAuth2SecurityChallengeWriter();
            }
        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnMissingClass(
                "org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint")
        static class GenericChallengeConfiguration {

            @Bean
            @ConditionalOnMissingBean
            OAuth2SecurityChallengeWriter OAuth2SecurityChallengeWriter() {
                return (request, response, context, resolution) -> {};
            }
        }

        private static String diagnosticMessage(AuthenticationException exception) {
            String message = exception.getMessage();
            return message == null || message.isBlank()
                    ? exception.getClass().getName()
                    : exception.getClass().getName() + ": " + message;
        }
    }

    private static ErrorReporter composeReporters(ObjectProvider<ErrorReporter> reporters) {
        List<ErrorReporter> available = reporters.stream().toList();
        if (available.isEmpty()) {
            return ErrorReporter.noop();
        }
        if (available.size() == 1) {
            return available.getFirst();
        }
        return new CompositeErrorReporter(available);
    }
}
