package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.starter.actuator.adapter.in.health.ServiceFrameworkHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;

/** Provides the Service Framework health indicator auto-configuration. */
@AutoConfiguration(after = ActuatorAutoConfiguration.class)
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnBean(FrameworkDiagnostics.class)
@ConditionalOnProperty(
        prefix = "smbtech.actuator",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnEnabledHealthIndicator("service-framework")
public class ActuatorHealthAutoConfiguration {

    /** Creates an Actuator health auto-configuration instance. */
    public ActuatorHealthAutoConfiguration() {}

    @Bean(name = "serviceFrameworkHealthIndicator")
    @ConditionalOnMissingBean(name = "serviceFrameworkHealthIndicator")
    HealthIndicator serviceFrameworkHealthIndicator(FrameworkDiagnostics diagnostics) {
        return new ServiceFrameworkHealthIndicator(diagnostics);
    }
}
