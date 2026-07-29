package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import com.smbtech.serviceframework.starter.actuator.adapter.in.metrics.ServiceFrameworkMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ActuatorMetricsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ActuatorAutoConfiguration.class,
                                    ActuatorMetricsAutoConfiguration.class))
                    .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void createsNamedMetricsBinderAndUsesConfiguredCacheTtl() {
        contextRunner
                .withPropertyValues("smbtech.actuator.metrics.cache-ttl=3s")
                .run(
                        context -> {
                            assertThat(context)
                                    .hasSingleBean(FrameworkDiagnostics.class)
                                    .hasSingleBean(MeterBinder.class)
                                    .hasSingleBean(ServiceFrameworkMetrics.class)
                                    .hasBean("serviceFrameworkMetrics");
                            assertThat(
                                            context.getBean(ActuatorProperties.class)
                                                    .getMetrics()
                                                    .getCacheTtl())
                                    .isEqualTo(Duration.ofSeconds(3));

                            MeterRegistry registry = context.getBean(MeterRegistry.class);
                            context.getBean(MeterBinder.class).bindTo(registry);
                            assertThat(
                                            registry.get(ServiceFrameworkMetrics.STATUS_METRIC_NAME)
                                                    .tag("status", "unknown")
                                                    .gauge()
                                                    .value())
                                    .isEqualTo(1.0);
                        });
    }

    @Test
    void bindsMetricsToTheSpringBootManagedRegistry() {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                ActuatorAutoConfiguration.class,
                                ActuatorMetricsAutoConfiguration.class,
                                MetricsAutoConfiguration.class,
                                SimpleMetricsExportAutoConfiguration.class,
                                CompositeMeterRegistryAutoConfiguration.class))
                .run(
                        context -> {
                            assertThat(context)
                                    .hasSingleBean(MeterRegistry.class)
                                    .hasBean("serviceFrameworkMetrics");
                            assertThat(
                                            context.getBean(MeterRegistry.class)
                                                    .find(
                                                            ServiceFrameworkMetrics
                                                                    .MODULES_METRIC_NAME)
                                                    .gauge())
                                    .isNotNull();
                        });
    }

    @Test
    void metricsAreEnabledByDefault() {
        contextRunner.run(
                context ->
                        assertThat(
                                        context.getBean(ActuatorProperties.class)
                                                .getMetrics()
                                                .isEnabled())
                                .isTrue());
    }

    @Test
    void metricsToggleDisablesOnlyTheMetricsBinder() {
        contextRunner
                .withPropertyValues("smbtech.actuator.metrics.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(FrameworkDiagnostics.class)
                                        .doesNotHaveBean(MeterBinder.class));
    }

    @Test
    void doesNotCreateMetricsWithoutARegistry() {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                ActuatorAutoConfiguration.class,
                                ActuatorMetricsAutoConfiguration.class))
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(FrameworkDiagnostics.class)
                                        .doesNotHaveBean(MeterBinder.class));
    }

    @Test
    void backsOffForNamedApplicationMetricsBinder() {
        MeterBinder applicationBinder = registry -> {};

        contextRunner
                .withBean("serviceFrameworkMetrics", MeterBinder.class, () -> applicationBinder)
                .run(
                        context ->
                                assertThat(
                                                context.getBean(
                                                        "serviceFrameworkMetrics",
                                                        MeterBinder.class))
                                        .isSameAs(applicationBinder));
    }

    @Test
    void globalStarterToggleDisablesMetrics() {
        contextRunner
                .withPropertyValues("smbtech.actuator.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(FrameworkDiagnostics.class)
                                        .doesNotHaveBean(MeterBinder.class));
    }

    @Test
    void rejectsNegativeCacheTtl() {
        contextRunner
                .withPropertyValues("smbtech.actuator.metrics.cache-ttl=-1s")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasStackTraceContaining("cacheTtl must not be negative");
                        });
    }
}
