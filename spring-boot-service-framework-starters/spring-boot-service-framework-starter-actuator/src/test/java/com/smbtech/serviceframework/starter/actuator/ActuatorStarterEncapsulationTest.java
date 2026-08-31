package com.smbtech.serviceframework.starter.actuator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorAutoConfiguration;
import com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorEndpointAutoConfiguration;
import com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorHealthAutoConfiguration;
import com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorInfoAutoConfiguration;
import com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorIntegrationsAutoConfiguration;
import com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorMetricsAutoConfiguration;
import com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorProperties;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActuatorStarterEncapsulationTest {

    private static final List<String> INTERNAL_TYPES =
            List.of(
                    "com.smbtech.serviceframework.starter.actuator.adapter.in.endpoint.ServiceFrameworkDiagnosticsEndpoint",
                    "com.smbtech.serviceframework.starter.actuator.adapter.in.health.ServiceFrameworkHealthIndicator",
                    "com.smbtech.serviceframework.starter.actuator.adapter.in.info.ServiceFrameworkInfoContributor",
                    "com.smbtech.serviceframework.starter.actuator.adapter.in.metrics.ServiceFrameworkMetrics",
                    "com.smbtech.serviceframework.starter.actuator.adapter.out.diagnostics.GuardedFrameworkDiagnostics",
                    "com.smbtech.serviceframework.starter.actuator.adapter.out.integration.RestClientDiagnosticProbe",
                    "com.smbtech.serviceframework.starter.actuator.adapter.out.integration.RestClientModuleInfoProvider",
                    "com.smbtech.serviceframework.starter.actuator.adapter.out.integration.MockModuleInfoProvider",
                    "com.smbtech.serviceframework.starter.actuator.adapter.out.integration.LoggingModuleInfoProvider",
                    "com.smbtech.serviceframework.starter.actuator.adapter.out.integration.ErrorHandlingModuleInfoProvider");

    @Test
    void exposesOnlyBootInfrastructure() throws ClassNotFoundException {
        List.of(
                        ActuatorAutoConfiguration.class,
                        ActuatorEndpointAutoConfiguration.class,
                        ActuatorHealthAutoConfiguration.class,
                        ActuatorInfoAutoConfiguration.class,
                        ActuatorIntegrationsAutoConfiguration.class,
                        ActuatorMetricsAutoConfiguration.class,
                        ActuatorProperties.class)
                .forEach(type -> assertTrue(Modifier.isPublic(type.getModifiers())));

        for (String typeName : INTERNAL_TYPES) {
            assertFalse(Modifier.isPublic(Class.forName(typeName).getModifiers()), typeName);
        }
    }
}
