package com.smbtech.serviceframework.starter.logging.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LoggingMetricsAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(LoggingMetricsAutoConfiguration.class));

    @Test
    void registersMetricsBinderByDefaultWhenMicrometerIsAvailable() {
        contextRunner.run(
                context ->
                        assertThat(context)
                                .hasBean("serviceFrameworkAsyncLoggingMetrics")
                                .hasSingleBean(MeterBinder.class));
    }

    @Test
    void canDisableAsyncLoggingObservability() {
        contextRunner
                .withPropertyValues("smbtech.logging.async.observability.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean("serviceFrameworkAsyncLoggingMetrics"));
    }

    @Test
    void backsOffWhenConsumerProvidesNamedBinder() {
        MeterBinder customBinder = registry -> {};

        contextRunner
                .withBean(
                        "serviceFrameworkAsyncLoggingMetrics",
                        MeterBinder.class,
                        () -> customBinder)
                .run(
                        context ->
                                assertThat(context)
                                        .getBean(
                                                "serviceFrameworkAsyncLoggingMetrics",
                                                MeterBinder.class)
                                        .isSameAs(customBinder));
    }

    @Test
    void doesNotRequireMicrometerAtRuntime() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(MeterRegistry.class))
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean("serviceFrameworkAsyncLoggingMetrics"));
    }
}
