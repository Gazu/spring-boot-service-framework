package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Import;

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
@Import(ActuatorConfigurationImportSelector.class)
public class ActuatorHealthAutoConfiguration {

    /** Creates an Actuator health auto-configuration instance. */
    public ActuatorHealthAutoConfiguration() {}
}
