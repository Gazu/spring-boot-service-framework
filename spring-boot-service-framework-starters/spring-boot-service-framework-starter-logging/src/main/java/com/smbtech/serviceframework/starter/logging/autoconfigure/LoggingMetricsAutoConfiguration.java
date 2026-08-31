package com.smbtech.serviceframework.starter.logging.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Provides optional Micrometer metrics for asynchronous logging. */
@AutoConfiguration(after = LoggingAutoConfiguration.class)
@ConditionalOnClass({MeterRegistry.class, MeterBinder.class})
@ConditionalOnProperty(
        prefix = "smbtech.logging.async.observability",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class LoggingMetricsAutoConfiguration {

    /** Creates a logging metrics auto-configuration instance. */
    public LoggingMetricsAutoConfiguration() {}

    @Bean(name = "serviceFrameworkAsyncLoggingMetrics")
    @ConditionalOnMissingBean(name = "serviceFrameworkAsyncLoggingMetrics")
    MeterBinder serviceFrameworkAsyncLoggingMetrics() {
        return new AsyncLoggingMetrics();
    }
}
