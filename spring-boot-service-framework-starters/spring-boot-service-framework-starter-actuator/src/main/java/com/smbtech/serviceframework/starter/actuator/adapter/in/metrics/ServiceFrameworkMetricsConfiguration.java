package com.smbtech.serviceframework.starter.actuator.adapter.in.metrics;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorProperties;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ServiceFrameworkMetricsConfiguration {

    @Bean(name = "serviceFrameworkMetrics")
    @ConditionalOnMissingBean(name = "serviceFrameworkMetrics")
    MeterBinder serviceFrameworkMetrics(
            FrameworkDiagnostics diagnostics,
            ActuatorProperties properties,
            ObjectProvider<Clock> clocks) {
        return new ServiceFrameworkMetrics(
                diagnostics,
                clocks.getIfUnique(Clock::systemUTC),
                properties.getMetrics().getCacheTtl());
    }
}
