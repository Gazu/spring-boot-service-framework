package com.smbtech.serviceframework.starter.logging.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.logging.adapter.in.servlet.TransactionIdFilter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class LoggingAutoConfigurationTest {
    private final WebApplicationContextRunner webContextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(LoggingAutoConfiguration.class));
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(LoggingAutoConfiguration.class));

    @Test
    void registersTransactionFilterByDefault() {
        webContextRunner.run(
                context ->
                        assertThat(context)
                                .hasSingleBean(LoggingProperties.class)
                                .hasSingleBean(StructuredLoggerFactory.class)
                                .hasSingleBean(CorrelationContext.class)
                                .hasSingleBean(TransactionIdFilter.class));
    }

    @Test
    void canDisableTransactionFilter() {
        webContextRunner
                .withPropertyValues("smbtech.logging.transaction.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TransactionIdFilter.class));
    }

    @Test
    void backsOffWhenConsumerProvidesPorts() {
        StructuredLoggerFactory customFactory = source -> null;
        CorrelationContext customContext =
                new CorrelationContext() {
                    @Override
                    public Map<String, String> snapshot() {
                        return Map.of();
                    }

                    @Override
                    public Scope open(Map<String, String> values) {
                        return () -> {};
                    }
                };

        webContextRunner
                .withBean(StructuredLoggerFactory.class, () -> customFactory)
                .withBean(CorrelationContext.class, () -> customContext)
                .run(
                        context -> {
                            assertThat(context)
                                    .getBean(StructuredLoggerFactory.class)
                                    .isSameAs(customFactory);
                            assertThat(context)
                                    .getBean(CorrelationContext.class)
                                    .isSameAs(customContext);
                            assertThat(context)
                                    .hasSingleBean(StructuredLoggerFactory.class)
                                    .hasSingleBean(CorrelationContext.class);
                        });
    }

    @Test
    void doesNotCreateServletAdapterOutsideServletApplication() {
        contextRunner.run(
                context ->
                        assertThat(context)
                                .hasSingleBean(StructuredLoggerFactory.class)
                                .hasSingleBean(CorrelationContext.class)
                                .doesNotHaveBean(TransactionIdFilter.class));
    }

    @Test
    void bindsTypedProperties() {
        webContextRunner
                .withPropertyValues(
                        "smbtech.logging.production=true",
                        "smbtech.logging.level=DEBUG",
                        "smbtech.logging.async.enabled=false",
                        "smbtech.logging.async.queue-size=512",
                        "smbtech.logging.async.discarding-threshold=8",
                        "smbtech.logging.async.never-block=true",
                        "smbtech.logging.async.max-flush-time-ms=2500",
                        "smbtech.logging.transaction.header-name=X-Correlation-Id",
                        "smbtech.logging.transaction.accept-incoming=false",
                        "smbtech.logging.transaction.max-length=64")
                .run(
                        context -> {
                            LoggingProperties properties = context.getBean(LoggingProperties.class);
                            assertThat(properties.isProduction()).isTrue();
                            assertThat(properties.getLevel()).isEqualTo("DEBUG");
                            assertThat(properties.getAsync().isEnabled()).isFalse();
                            assertThat(properties.getAsync().getQueueSize()).isEqualTo(512);
                            assertThat(properties.getAsync().getDiscardingThreshold()).isEqualTo(8);
                            assertThat(properties.getAsync().isNeverBlock()).isTrue();
                            assertThat(properties.getAsync().getMaxFlushTimeMs()).isEqualTo(2500);
                            assertThat(properties.getTransaction().getHeaderName())
                                    .isEqualTo("X-Correlation-Id");
                            assertThat(properties.getTransaction().isAcceptIncoming()).isFalse();
                            assertThat(properties.getTransaction().getMaxLength()).isEqualTo(64);
                        });
    }
}
