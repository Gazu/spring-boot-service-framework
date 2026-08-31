package com.smbtech.serviceframework.starter.actuator.adapter.out.diagnostics;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorProperties;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class FrameworkDiagnosticsConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(FrameworkDiagnostics.class)
    FrameworkDiagnostics frameworkDiagnostics(
            ObjectProvider<DiagnosticProbe> probes,
            ObjectProvider<FrameworkModuleInfoProvider> moduleInfoProviders,
            ObjectProvider<Clock> clocks,
            ActuatorProperties properties) {
        Clock clock = clocks.getIfUnique(Clock::systemUTC);
        FrameworkDiagnostics diagnostics =
                FrameworkDiagnostics.from(
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
