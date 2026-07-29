package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.starter.actuator.adapter.in.endpoint.ServiceFrameworkDiagnosticsEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Provides the read-only Service Framework diagnostics endpoint auto-configuration. */
@AutoConfiguration(after = ActuatorAutoConfiguration.class)
@ConditionalOnClass(Endpoint.class)
@ConditionalOnBean(FrameworkDiagnostics.class)
@ConditionalOnProperty(
        prefix = "smbtech.actuator",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnAvailableEndpoint(endpoint = ServiceFrameworkDiagnosticsEndpoint.class)
public class ActuatorEndpointAutoConfiguration {

    /** Creates an Actuator endpoint auto-configuration instance. */
    public ActuatorEndpointAutoConfiguration() {}

    @Bean(name = "serviceFrameworkDiagnosticsEndpoint")
    @ConditionalOnMissingBean(ServiceFrameworkDiagnosticsEndpoint.class)
    ServiceFrameworkDiagnosticsEndpoint serviceFrameworkDiagnosticsEndpoint(
            FrameworkDiagnostics diagnostics) {
        return new ServiceFrameworkDiagnosticsEndpoint(diagnostics);
    }
}
