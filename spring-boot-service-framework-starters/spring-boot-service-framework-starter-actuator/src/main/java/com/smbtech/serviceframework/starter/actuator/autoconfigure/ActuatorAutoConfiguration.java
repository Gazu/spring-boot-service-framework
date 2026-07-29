package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import com.smbtech.serviceframework.actuator.service.DefaultFrameworkDiagnostics;
import com.smbtech.serviceframework.starter.actuator.adapter.out.diagnostics.GuardedFrameworkDiagnostics;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;

/** Provides the base Service Framework Actuator auto-configuration. */
@AutoConfiguration
@ConditionalOnClass({HealthContributor.class, FrameworkDiagnostics.class})
@ConditionalOnProperty(
        prefix = "smbtech.actuator",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(ActuatorProperties.class)
public class ActuatorAutoConfiguration {

    /** Creates an Actuator auto-configuration instance. */
    public ActuatorAutoConfiguration() {}

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(FrameworkDiagnostics.class)
    FrameworkDiagnostics frameworkDiagnostics(
            ObjectProvider<DiagnosticProbe> probes,
            ObjectProvider<FrameworkModuleInfoProvider> moduleInfoProviders,
            ObjectProvider<Clock> clocks,
            ActuatorProperties properties) {
        Clock clock = clocks.getIfUnique(Clock::systemUTC);
        DefaultFrameworkDiagnostics diagnostics =
                new DefaultFrameworkDiagnostics(
                        probes.orderedStream().toList(),
                        moduleInfoProviders.orderedStream().toList(),
                        clock);
        ActuatorProperties.Diagnostics protection = properties.getDiagnostics();
        return new GuardedFrameworkDiagnostics(
                diagnostics,
                clock,
                protection.getCacheTtl(),
                protection.getOperationTimeout(),
                protection.getMaxComponents(),
                protection.getMaxModules());
    }
}
