package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.starter.actuator.adapter.in.info.ServiceFrameworkInfoContributor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ActuatorInfoAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ActuatorAutoConfiguration.class,
                                    ActuatorInfoAutoConfiguration.class));

    @Test
    void createsNamedInfoContributorByDefault() {
        contextRunner.run(
                context ->
                        assertThat(context)
                                .hasSingleBean(FrameworkDiagnostics.class)
                                .hasSingleBean(InfoContributor.class)
                                .hasSingleBean(ServiceFrameworkInfoContributor.class)
                                .hasBean("serviceFrameworkInfoContributor"));
    }

    @Test
    void honorsSpringBootInfoContributorToggle() {
        contextRunner
                .withPropertyValues("management.info.service-framework.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(InfoContributor.class));
    }

    @Test
    void honorsSpringBootInfoDefaultsAndExplicitOverride() {
        contextRunner
                .withPropertyValues("management.info.defaults.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(InfoContributor.class));

        contextRunner
                .withPropertyValues(
                        "management.info.defaults.enabled=false",
                        "management.info.service-framework.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(InfoContributor.class));
    }

    @Test
    void backsOffForNamedApplicationInfoContributor() {
        InfoContributor applicationContributor = builder -> builder.withDetail("app", "value");

        contextRunner
                .withBean(
                        "serviceFrameworkInfoContributor",
                        InfoContributor.class,
                        () -> applicationContributor)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(InfoContributor.class);
                            assertThat(
                                            context.getBean(
                                                    "serviceFrameworkInfoContributor",
                                                    InfoContributor.class))
                                    .isSameAs(applicationContributor);
                        });
    }

    @Test
    void globalStarterToggleDisablesInfoContributor() {
        contextRunner
                .withPropertyValues("smbtech.actuator.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(FrameworkDiagnostics.class)
                                        .doesNotHaveBean(InfoContributor.class));
    }
}
