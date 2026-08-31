package com.smbtech.serviceframework.starter.actuator.adapter.in.health;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ServiceFrameworkHealthConfiguration {

    @Bean(name = "serviceFrameworkHealthIndicator")
    @ConditionalOnMissingBean(name = "serviceFrameworkHealthIndicator")
    HealthIndicator serviceFrameworkHealthIndicator(FrameworkDiagnostics diagnostics) {
        return new ServiceFrameworkHealthIndicator(diagnostics);
    }
}
