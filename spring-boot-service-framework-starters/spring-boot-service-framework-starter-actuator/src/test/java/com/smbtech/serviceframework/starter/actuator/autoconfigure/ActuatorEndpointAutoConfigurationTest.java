package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.starter.actuator.adapter.in.endpoint.ServiceFrameworkDiagnosticsEndpoint;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ActuatorEndpointAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ActuatorAutoConfiguration.class,
                                    ActuatorEndpointAutoConfiguration.class));

    @Test
    void endpointIsNotCreatedWithoutExplicitAccess() {
        contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(FrameworkDiagnostics.class);
                    assertThat(context).doesNotHaveBean(ServiceFrameworkDiagnosticsEndpoint.class);
                });
    }

    @Test
    void createsEndpointWhenReadOnlyAccessIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "management.endpoint.serviceframework.access=READ_ONLY",
                        "management.endpoints.web.exposure.include=serviceframework")
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(FrameworkDiagnostics.class)
                                        .hasSingleBean(ServiceFrameworkDiagnosticsEndpoint.class)
                                        .hasBean("serviceFrameworkDiagnosticsEndpoint"));
    }

    @Test
    void backsOffForApplicationEndpoint() {
        FrameworkDiagnostics applicationDiagnostics =
                new FrameworkDiagnostics() {
                    @Override
                    public FrameworkDiagnosticsSnapshot snapshot() {
                        return new FrameworkDiagnosticsSnapshot(Instant.EPOCH, Map.of());
                    }

                    @Override
                    public List<FrameworkModuleInfo> modules() {
                        return List.of();
                    }
                };

        contextRunner
                .withPropertyValues(
                        "management.endpoint.serviceframework.access=READ_ONLY",
                        "management.endpoints.web.exposure.include=serviceframework")
                .withBean(
                        ServiceFrameworkDiagnosticsEndpoint.class,
                        () -> new ServiceFrameworkDiagnosticsEndpoint(applicationDiagnostics))
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(ServiceFrameworkDiagnosticsEndpoint.class));
    }

    @Test
    void globalStarterToggleDisablesEndpoint() {
        contextRunner
                .withPropertyValues(
                        "smbtech.actuator.enabled=false",
                        "management.endpoint.serviceframework.access=READ_ONLY",
                        "management.endpoints.web.exposure.include=serviceframework")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(FrameworkDiagnostics.class)
                                        .doesNotHaveBean(
                                                ServiceFrameworkDiagnosticsEndpoint.class));
    }
}
