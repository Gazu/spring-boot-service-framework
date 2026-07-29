package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import com.smbtech.serviceframework.starter.actuator.adapter.out.integration.RestClientDiagnosticProbe;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import com.smbtech.serviceframework.starter.logging.autoconfigure.LoggingProperties;
import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class ActuatorIntegrationsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ActuatorAutoConfiguration.class,
                                    ActuatorIntegrationsAutoConfiguration.class));

    @Test
    void registersPassiveIntegrationsForAvailableFrameworkStarters() {
        contextRunner
                .withBean(RestClientProperties.class, RestClientProperties::new)
                .withBean(RestClientRegistry.class, () -> passiveRegistry(Set.of("payments")))
                .withBean(MockProperties.class, MockProperties::new)
                .withBean(LoggingProperties.class, LoggingProperties::new)
                .withBean(ErrorHandlingProperties.class, ErrorHandlingProperties::new)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(DiagnosticProbe.class);
                            assertThat(context)
                                    .hasBean("serviceFrameworkRestClientDiagnosticProbe");
                            assertThat(context)
                                    .hasBean("serviceFrameworkRestClientModuleInfoProvider")
                                    .hasBean("serviceFrameworkMockModuleInfoProvider")
                                    .hasBean("serviceFrameworkLoggingModuleInfoProvider")
                                    .hasBean("serviceFrameworkErrorHandlingModuleInfoProvider");
                            assertThat(context.getBeansOfType(FrameworkModuleInfoProvider.class))
                                    .hasSize(4);

                            FrameworkDiagnostics diagnostics =
                                    context.getBean(FrameworkDiagnostics.class);
                            assertThat(diagnostics.snapshot().components())
                                    .containsOnlyKeys("rest-client");
                            assertThat(diagnostics.modules())
                                    .extracting(FrameworkModuleInfo::name)
                                    .containsExactly(
                                            "error-handling", "logging", "mock", "rest-client");
                        });
    }

    @Test
    void doesNotRegisterIntegrationsWhenOptionalStarterBeansAreAbsent() {
        contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(FrameworkDiagnostics.class);
                    assertThat(context).doesNotHaveBean(DiagnosticProbe.class);
                    assertThat(context).doesNotHaveBean(FrameworkModuleInfoProvider.class);
                });
    }

    @Test
    void startsWhenAllOptionalStarterTypesAreAbsent() {
        contextRunner
                .withClassLoader(
                        new FilteredClassLoader(
                                RestClientProperties.class,
                                RestClientRegistry.class,
                                MockProperties.class,
                                LoggingProperties.class,
                                ErrorHandlingProperties.class))
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(FrameworkDiagnostics.class)
                                        .doesNotHaveBean(DiagnosticProbe.class)
                                        .doesNotHaveBean(FrameworkModuleInfoProvider.class));
    }

    @Test
    void backsOffForNamedApplicationIntegrationBeans() {
        DiagnosticProbe applicationProbe =
                new DiagnosticProbe() {
                    @Override
                    public String componentName() {
                        return RestClientDiagnosticProbe.COMPONENT_NAME;
                    }

                    @Override
                    public ComponentHealth check() {
                        return ComponentHealth.unknown(componentName());
                    }
                };
        FrameworkModuleInfoProvider applicationProvider =
                new FrameworkModuleInfoProvider() {
                    @Override
                    public String moduleName() {
                        return "rest-client";
                    }

                    @Override
                    public FrameworkModuleInfo provide() {
                        return FrameworkModuleInfo.of(moduleName(), "application");
                    }
                };

        contextRunner
                .withBean(RestClientProperties.class, RestClientProperties::new)
                .withBean(RestClientRegistry.class, () -> passiveRegistry(Set.of()))
                .withBean(
                        "serviceFrameworkRestClientDiagnosticProbe",
                        DiagnosticProbe.class,
                        () -> applicationProbe)
                .withBean(
                        "serviceFrameworkRestClientModuleInfoProvider",
                        FrameworkModuleInfoProvider.class,
                        () -> applicationProvider)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(DiagnosticProbe.class);
                            assertThat(context).hasSingleBean(FrameworkModuleInfoProvider.class);
                            assertThat(
                                            context.getBean(
                                                    "serviceFrameworkRestClientDiagnosticProbe",
                                                    DiagnosticProbe.class))
                                    .isSameAs(applicationProbe);
                            assertThat(
                                            context.getBean(
                                                    "serviceFrameworkRestClientModuleInfoProvider",
                                                    FrameworkModuleInfoProvider.class))
                                    .isSameAs(applicationProvider);
                        });
    }

    @Test
    void globalStarterToggleDisablesIntegrations() {
        contextRunner
                .withPropertyValues("smbtech.actuator.enabled=false")
                .withBean(RestClientProperties.class, RestClientProperties::new)
                .withBean(RestClientRegistry.class, () -> passiveRegistry(Set.of()))
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(FrameworkDiagnostics.class)
                                        .doesNotHaveBean(DiagnosticProbe.class)
                                        .doesNotHaveBean(FrameworkModuleInfoProvider.class));
    }

    private static RestClientRegistry passiveRegistry(Set<String> names) {
        return new RestClientRegistry() {
            @Override
            public RestClient get(String name) {
                throw new AssertionError("The passive integration must not create REST clients");
            }

            @Override
            public Set<String> names() {
                return names;
            }

            @Override
            public Map<String, RestClient> all() {
                throw new AssertionError("The passive integration must not create REST clients");
            }
        };
    }
}
