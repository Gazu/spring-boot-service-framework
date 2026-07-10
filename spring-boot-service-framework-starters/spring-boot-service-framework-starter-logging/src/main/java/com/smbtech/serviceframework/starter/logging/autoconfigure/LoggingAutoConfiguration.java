package com.smbtech.serviceframework.starter.logging.autoconfigure;

import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.logging.StructuredLoggers;
import com.smbtech.serviceframework.starter.logging.adapter.in.servlet.TransactionalIdFilter;
import com.smbtech.serviceframework.starter.logging.adapter.out.context.MdcCorrelationContext;
import com.smbtech.serviceframework.starter.logging.adapter.out.slf4j.Slf4jStructuredLoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingAutoConfiguration {

    public LoggingAutoConfiguration(LoggingProperties properties) {
        StructuredLoggers.setProduction(properties.isProduction());
    }

    @Bean
    @ConditionalOnMissingBean
    StructuredLoggerFactory structuredLoggerFactory(LoggingProperties properties) {
        return new Slf4jStructuredLoggerFactory(properties.isProduction());
    }

    @Bean
    @ConditionalOnMissingBean
    CorrelationContext correlationContext() {
        return new MdcCorrelationContext();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = {
            "jakarta.servlet.Filter",
            "org.springframework.web.filter.OncePerRequestFilter"
    })
    static class ServletLoggingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(
                prefix = "smbtech.logging.transaction",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        TransactionalIdFilter transactionalIdFilter(
                LoggingProperties properties,
                CorrelationContext correlationContext
        ) {
            LoggingProperties.Transaction transaction = properties.getTransaction();
            return new TransactionalIdFilter(
                    transaction.getHeaderName(),
                    correlationContext,
                    transaction.isAcceptIncoming(),
                    transaction.getMaxLength()
            );
        }
    }
}
