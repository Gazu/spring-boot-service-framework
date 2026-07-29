package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.starter.actuator.adapter.in.health.ServiceFrameworkHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ActuatorHealthAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ActuatorAutoConfiguration.class,
                                    ActuatorHealthAutoConfiguration.class,
                                    HealthContributorRegistryAutoConfiguration.class));

    @Test
    void createsNamedHealthIndicatorByDefault() {
        contextRunner.run(
                context -> {
                    assertThat(context)
                            .hasSingleBean(FrameworkDiagnostics.class)
                            .hasSingleBean(HealthIndicator.class)
                            .hasSingleBean(ServiceFrameworkHealthIndicator.class)
                            .hasSingleBean(HealthContributorRegistry.class)
                            .hasBean("serviceFrameworkHealthIndicator");
                    HealthIndicator indicator =
                            context.getBean(
                                    "serviceFrameworkHealthIndicator", HealthIndicator.class);
                    assertThat(
                                    context.getBean(HealthContributorRegistry.class)
                                            .getContributor("serviceFramework"))
                            .isSameAs(indicator);
                    assertThat(indicator.health().getStatus()).isEqualTo(Status.UNKNOWN);
                });
    }

    @Test
    void honorsSpringBootHealthIndicatorToggle() {
        contextRunner
                .withPropertyValues("management.health.service-framework.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(FrameworkDiagnostics.class)
                                        .doesNotHaveBean(HealthIndicator.class));
    }

    @Test
    void honorsSpringBootHealthDefaultsAndExplicitOverride() {
        contextRunner
                .withPropertyValues("management.health.defaults.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(HealthIndicator.class));

        contextRunner
                .withPropertyValues(
                        "management.health.defaults.enabled=false",
                        "management.health.service-framework.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(HealthIndicator.class));
    }

    @Test
    void backsOffForNamedApplicationHealthIndicator() {
        HealthIndicator applicationIndicator = () -> Health.up().build();

        contextRunner
                .withBean(
                        "serviceFrameworkHealthIndicator",
                        HealthIndicator.class,
                        () -> applicationIndicator)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(HealthIndicator.class);
                            assertThat(
                                            context.getBean(
                                                    "serviceFrameworkHealthIndicator",
                                                    HealthIndicator.class))
                                    .isSameAs(applicationIndicator);
                        });
    }

    @Test
    void globalStarterToggleDisablesHealthIndicator() {
        contextRunner
                .withPropertyValues("smbtech.actuator.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(FrameworkDiagnostics.class)
                                        .doesNotHaveBean(HealthIndicator.class));
    }
}
