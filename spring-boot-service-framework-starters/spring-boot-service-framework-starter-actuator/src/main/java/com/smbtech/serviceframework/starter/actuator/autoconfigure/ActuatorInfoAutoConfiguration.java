package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import org.springframework.boot.actuate.autoconfigure.info.ConditionalOnEnabledInfoContributor;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/** Provides the Service Framework info contributor auto-configuration. */
@AutoConfiguration(after = ActuatorAutoConfiguration.class)
@ConditionalOnClass(InfoContributor.class)
@ConditionalOnBean(FrameworkDiagnostics.class)
@ConditionalOnProperty(
        prefix = "smbtech.actuator",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnEnabledInfoContributor("service-framework")
@Import(ActuatorConfigurationImportSelector.class)
public class ActuatorInfoAutoConfiguration {

    /** Creates an Actuator info auto-configuration instance. */
    public ActuatorInfoAutoConfiguration() {}
}
