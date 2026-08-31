package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.error.NotificationSanitizer;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorExposurePolicy;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationHttpStatusResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.ResolvedErrorCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Assembles the internal response pipeline from supported extension contracts. */
@Configuration(proxyBeanMethods = false)
class ErrorResponseConfiguration {

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
    ErrorResponsePipeline errorResponsePipeline(
            NotificationResponseFactory responseFactory,
            ObjectProvider<ErrorReporter> reporters,
            ObjectProvider<ErrorMetricsRecorder> metricsRecorder,
            ErrorCustomizationPipeline customizationPipeline,
            ErrorHandlingProperties properties) {
        NotificationResponseFactory finalResponseFactory =
                new DefaultNotificationResponseFactory(
                        new DefaultNotificationHttpStatusResolver(),
                        NotificationSanitizer.withMetadataAllowlist(
                                properties.getResponse().getMetadataAllowlist()),
                        properties.getResponse().isIncludeFieldViolations());
        return new ErrorResponsePipeline(
                responseFactory,
                ErrorReporter.composite(reporters.stream().toList()),
                metricsRecorder.getIfAvailable(ErrorMetricsRecorder::noop),
                customizationPipeline,
                finalResponseFactory);
    }

    @Bean("serviceFrameworkExceptionHandler")
    ServiceFrameworkExceptionHandler serviceFrameworkExceptionHandler(
            @Qualifier("throwableErrorResolutionPipeline") ThrowableErrorResolver resolutionPipeline,
            ErrorResponsePipeline responsePipeline) {
        return new ServiceFrameworkExceptionHandler(resolutionPipeline, responsePipeline);
    }
}
