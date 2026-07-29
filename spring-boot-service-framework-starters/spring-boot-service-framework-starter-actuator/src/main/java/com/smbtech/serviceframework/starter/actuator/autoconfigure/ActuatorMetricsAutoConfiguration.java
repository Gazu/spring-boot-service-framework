package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.starter.actuator.adapter.in.metrics.ServiceFrameworkMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Provides bounded-cardinality Service Framework metrics. */
@AutoConfiguration(
        after = {ActuatorAutoConfiguration.class, ActuatorIntegrationsAutoConfiguration.class},
        afterName = {
            "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
            "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration"
        })
@ConditionalOnClass({MeterRegistry.class, MeterBinder.class})
@ConditionalOnBean({FrameworkDiagnostics.class, MeterRegistry.class})
@ConditionalOnProperty(
        prefix = "smbtech.actuator",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnProperty(
        prefix = "smbtech.actuator.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ActuatorMetricsAutoConfiguration {

    /** Creates an Actuator metrics auto-configuration instance. */
    public ActuatorMetricsAutoConfiguration() {}

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
