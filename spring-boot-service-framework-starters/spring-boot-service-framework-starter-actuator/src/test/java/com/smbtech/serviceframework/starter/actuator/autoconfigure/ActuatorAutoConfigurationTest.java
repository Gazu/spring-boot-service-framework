package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.ComponentStatus;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import com.smbtech.serviceframework.starter.actuator.adapter.out.diagnostics.GuardedFrameworkDiagnostics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ActuatorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ActuatorAutoConfiguration.class));

    @Test
    void createsBaseDiagnosticsInfrastructure() {
        contextRunner.run(
                context -> {
                    assertThat(context)
                            .hasSingleBean(ActuatorProperties.class)
                            .hasSingleBean(FrameworkDiagnostics.class)
                            .hasSingleBean(GuardedFrameworkDiagnostics.class)
                            .doesNotHaveBean(Clock.class);
                    assertThat(context.getBean(ActuatorProperties.class).isEnabled()).isTrue();
                    assertThat(context.getBean(FrameworkDiagnostics.class).snapshot().isEmpty())
                            .isTrue();
                });
    }

    @Test
    void bindsDiagnosticsSecurityAndPerformanceProperties() {
        contextRunner
                .withPropertyValues(
                        "smbtech.actuator.diagnostics.cache-ttl=7s",
                        "smbtech.actuator.diagnostics.operation-timeout=3s",
                        "smbtech.actuator.diagnostics.max-components=12",
                        "smbtech.actuator.diagnostics.max-modules=8")
                .run(
                        context -> {
                            ActuatorProperties.Diagnostics diagnostics =
                                    context.getBean(ActuatorProperties.class).getDiagnostics();
                            assertThat(diagnostics.getCacheTtl()).isEqualTo(Duration.ofSeconds(7));
                            assertThat(diagnostics.getOperationTimeout())
                                    .isEqualTo(Duration.ofSeconds(3));
                            assertThat(diagnostics.getMaxComponents()).isEqualTo(12);
                            assertThat(diagnostics.getMaxModules()).isEqualTo(8);
                        });
    }

    @Test
    void aggregatesApplicationProbesAndModuleInformation() {
        contextRunner
                .withBean(
                        DiagnosticProbe.class,
                        () ->
                                new DiagnosticProbe() {
                                    @Override
                                    public String componentName() {
                                        return "cache";
                                    }

                                    @Override
                                    public ComponentHealth check() {
                                        return ComponentHealth.up(componentName());
                                    }
                                })
                .withBean(
                        FrameworkModuleInfoProvider.class,
                        () ->
                                new FrameworkModuleInfoProvider() {
                                    @Override
                                    public String moduleName() {
                                        return "logging";
                                    }

                                    @Override
                                    public FrameworkModuleInfo provide() {
                                        return FrameworkModuleInfo.of(moduleName(), "0.4.0");
                                    }
                                })
                .run(
                        context -> {
                            FrameworkDiagnostics diagnostics =
                                    context.getBean(FrameworkDiagnostics.class);

                            assertThat(diagnostics.snapshot().status())
                                    .isEqualTo(ComponentStatus.UP);
                            assertThat(diagnostics.snapshot().components())
                                    .containsOnlyKeys("cache");
                            assertThat(diagnostics.modules())
                                    .extracting(FrameworkModuleInfo::name)
                                    .containsExactly("logging");
                        });
    }

    @Test
    void usesUniqueApplicationClockWithoutPublishingAnotherClock() {
        Clock applicationClock = Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneOffset.UTC);

        contextRunner
                .withBean(Clock.class, () -> applicationClock)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(Clock.class);
                            assertThat(
                                            context.getBean(FrameworkDiagnostics.class)
                                                    .snapshot()
                                                    .capturedAt())
                                    .isEqualTo(Instant.parse("2026-07-27T12:00:00Z"));
                        });
    }

    @Test
    void backsOffForApplicationDiagnostics() {
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
                .withBean(FrameworkDiagnostics.class, () -> applicationDiagnostics)
                .run(
                        context ->
                                assertThat(context.getBean(FrameworkDiagnostics.class))
                                        .isSameAs(applicationDiagnostics));
    }

    @Test
    void canDisableBaseAutoConfiguration() {
        contextRunner
                .withPropertyValues("smbtech.actuator.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(ActuatorProperties.class)
                                        .doesNotHaveBean(FrameworkDiagnostics.class));
    }

    @Test
    void requiresSpringBootHealthInfrastructure() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(HealthContributor.class))
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(ActuatorProperties.class)
                                        .doesNotHaveBean(FrameworkDiagnostics.class));
    }
}
