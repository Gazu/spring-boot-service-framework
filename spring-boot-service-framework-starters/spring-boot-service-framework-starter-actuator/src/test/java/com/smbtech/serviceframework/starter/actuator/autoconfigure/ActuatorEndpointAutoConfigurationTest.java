package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
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
                    assertThat(context).doesNotHaveBean("serviceFrameworkDiagnosticsEndpoint");
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
                                        .hasBean("serviceFrameworkDiagnosticsEndpoint"));
    }

    @Test
    void backsOffForApplicationEndpoint() {
        Object applicationEndpoint = new Object();

        contextRunner
                .withPropertyValues(
                        "management.endpoint.serviceframework.access=READ_ONLY",
                        "management.endpoints.web.exposure.include=serviceframework")
                .withBean(
                        "serviceFrameworkDiagnosticsEndpoint",
                        Object.class,
                        () -> applicationEndpoint)
                .run(
                        context ->
                                assertThat(context.getBean("serviceFrameworkDiagnosticsEndpoint"))
                                        .isSameAs(applicationEndpoint));
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
                                        .doesNotHaveBean("serviceFrameworkDiagnosticsEndpoint"));
    }
}
