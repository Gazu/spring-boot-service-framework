package com.smbtech.serviceframework.starter.logging.autoconfigure;

import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.logging.StructuredLoggers;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/** Provides logging auto configuration behavior. */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@ImportRuntimeHints(LoggingRuntimeHints.class)
public class LoggingAutoConfiguration {

    /**
     * Creates a logging auto configuration instance.
     *
     * @param properties properties value
     */
    public LoggingAutoConfiguration(LoggingProperties properties) {
        AsyncLoggingPropertiesValidator.validate(properties);
        StructuredLoggers.setProduction(properties.isProduction());
    }

    @Bean
    @ConditionalOnMissingBean
    StructuredLoggerFactory structuredLoggerFactory() {
        return StructuredLoggers::get;
    }

    @Bean
    @ConditionalOnMissingBean
    CorrelationContext correlationContext() {
        return new MdcCorrelationContext();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(
            name = {
                "jakarta.servlet.Filter",
                "org.springframework.web.filter.OncePerRequestFilter"
            })
    static class ServletLoggingConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "transactionIdFilter")
        @ConditionalOnProperty(
                prefix = "smbtech.logging.transaction",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true)
        Filter transactionIdFilter(
                LoggingProperties properties, CorrelationContext correlationContext) {
            LoggingProperties.Transaction transaction = properties.getTransaction();
            return new TransactionIdFilter(
                    transaction.getHeaderName(),
                    correlationContext,
                    transaction.isAcceptIncoming(),
                    transaction.getMaxLength());
        }
    }
}
