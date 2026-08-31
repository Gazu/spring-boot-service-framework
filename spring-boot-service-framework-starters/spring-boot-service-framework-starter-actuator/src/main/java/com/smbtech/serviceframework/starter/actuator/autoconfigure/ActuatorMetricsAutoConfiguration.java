package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

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
@Import(ActuatorConfigurationImportSelector.class)
public class ActuatorMetricsAutoConfiguration {

    /** Creates an Actuator metrics auto-configuration instance. */
    public ActuatorMetricsAutoConfiguration() {}
}
