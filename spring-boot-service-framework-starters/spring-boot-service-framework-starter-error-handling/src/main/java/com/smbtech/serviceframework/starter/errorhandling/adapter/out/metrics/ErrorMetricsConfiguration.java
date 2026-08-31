package com.smbtech.serviceframework.starter.errorhandling.adapter.out.metrics;

import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers bounded-cardinality error metrics when Micrometer is available. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MeterRegistry.class)
class ErrorMetricsConfiguration {

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
