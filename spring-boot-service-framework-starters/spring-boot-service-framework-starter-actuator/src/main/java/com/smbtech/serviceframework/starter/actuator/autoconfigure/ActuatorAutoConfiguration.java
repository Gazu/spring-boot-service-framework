package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Import;

/** Provides the base Service Framework Actuator auto-configuration. */
@AutoConfiguration
@ConditionalOnClass({HealthContributor.class, FrameworkDiagnostics.class})
@ConditionalOnProperty(
        prefix = "smbtech.actuator",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(ActuatorProperties.class)
@Import(ActuatorConfigurationImportSelector.class)
public class ActuatorAutoConfiguration {

    /** Creates an Actuator auto-configuration instance. */
    public ActuatorAutoConfiguration() {}
}
