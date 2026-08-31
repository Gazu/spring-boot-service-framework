package com.smbtech.serviceframework.starter.errorhandling.adapter.out.logging;

import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers structured error reporting when logging infrastructure is available. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({StructuredLoggerFactory.class, CorrelationContext.class})
class ErrorLoggingConfiguration {

    @Bean("structuredErrorReporter")
    @ConditionalOnBean({StructuredLoggerFactory.class, CorrelationContext.class})
    @ConditionalOnMissingBean(name = "structuredErrorReporter")
    @ConditionalOnProperty(
            prefix = "smbtech.error-handling.logging",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    ErrorReporter structuredErrorReporter(
            StructuredLoggerFactory loggerFactory,
            CorrelationContext correlationContext,
            ErrorHandlingProperties properties) {
        return new StructuredErrorReporter(
                loggerFactory.get(StructuredErrorReporter.class),
                correlationContext,
                properties.getLogging().isIncludeDiagnostics());
    }
}
