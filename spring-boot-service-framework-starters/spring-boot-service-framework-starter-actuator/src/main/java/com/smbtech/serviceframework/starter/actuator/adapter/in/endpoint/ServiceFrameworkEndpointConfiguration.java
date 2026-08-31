package com.smbtech.serviceframework.starter.actuator.adapter.in.endpoint;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnAvailableEndpoint(endpoint = ServiceFrameworkDiagnosticsEndpoint.class)
@ConditionalOnMissingBean(name = "serviceFrameworkDiagnosticsEndpoint")
class ServiceFrameworkEndpointConfiguration {

    @Bean(name = "serviceFrameworkDiagnosticsEndpoint")
    ServiceFrameworkDiagnosticsEndpoint serviceFrameworkDiagnosticsEndpoint(
            FrameworkDiagnostics diagnostics) {
        return new ServiceFrameworkDiagnosticsEndpoint(diagnostics);
    }
}
