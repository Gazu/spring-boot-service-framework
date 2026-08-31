package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.NotificationAggregationPolicy;
import com.smbtech.serviceframework.error.NotificationSanitizer;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorExposurePolicy;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.web.servlet.DispatcherServlet;

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
@Import(ErrorHandlingConfigurationImportSelector.class)
@ImportRuntimeHints(ErrorHandlingRuntimeHints.class)
public class ErrorHandlingAutoConfiguration {

    /** Creates an error handling auto-configuration instance. */
    public ErrorHandlingAutoConfiguration() {}

    @Bean
    @ConfigurationPropertiesBinding
    static ErrorExposureConverter errorExposureConverter() {
        return new ErrorExposureConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationAggregationPolicy notificationAggregationPolicy() {
        return NotificationAggregationPolicy.defaultPolicy();
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationSanitizer notificationSanitizer(ErrorHandlingProperties properties) {
        return NotificationSanitizer.withMetadataAllowlist(
                properties.getResponse().getMetadataAllowlist());
    }

    @Bean("serviceExceptionThrowableErrorResolver")
    @ConditionalOnMissingBean(name = "serviceExceptionThrowableErrorResolver")
    ThrowableErrorResolver serviceExceptionThrowableErrorResolver(
            NotificationAggregationPolicy aggregationPolicy) {
        return ThrowableErrorResolver.serviceExceptions(aggregationPolicy);
    }

    @Bean("fallbackThrowableErrorResolver")
    @ConditionalOnMissingBean(name = "fallbackThrowableErrorResolver")
    ThrowableErrorResolver fallbackThrowableErrorResolver() {
        return ThrowableErrorResolver.fallback();
    }

    @Bean("throwableErrorResolutionPipeline")
    @ConditionalOnMissingBean(name = "throwableErrorResolutionPipeline")
    ThrowableErrorResolver throwableErrorResolutionPipeline(
            ObjectProvider<ThrowableErrorResolver> resolvers,
            @Qualifier("fallbackThrowableErrorResolver") ThrowableErrorResolver fallbackResolver) {
        List<ThrowableErrorResolver> specializedResolvers =
                resolvers.stream().filter(resolver -> resolver != fallbackResolver).toList();
        return ThrowableErrorResolver.composite(specializedResolvers, fallbackResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    ErrorExposurePolicy errorExposurePolicy(ErrorHandlingProperties properties) {
        return new ConfiguredErrorExposurePolicy(properties);
    }
}
