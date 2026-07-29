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
                        "smbtech.logging.async.saturation-policy=DISCARD_LOW_PRIORITY",
                        "smbtech.logging.async.critical-event-protection-enabled=false",
                        "smbtech.logging.async.discarding-threshold=8",
                        "smbtech.logging.async.never-block=true",
                        "smbtech.logging.async.max-flush-time-ms=2500",
                        "smbtech.logging.async.observability.enabled=false",
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
                            assertThat(properties.getAsync().getSaturationPolicy())
                                    .isEqualTo(
                                            LoggingProperties.Async.SaturationPolicy
                                                    .DISCARD_LOW_PRIORITY);
                            assertThat(properties.getAsync().isCriticalEventProtectionEnabled())
                                    .isFalse();
                            assertThat(properties.getAsync().getDiscardingThreshold()).isEqualTo(8);
                            assertThat(properties.getAsync().isNeverBlock()).isTrue();
                            assertThat(properties.getAsync().getMaxFlushTimeMs()).isEqualTo(2500);
                            assertThat(properties.getAsync().getObservability().isEnabled())
                                    .isFalse();
                            assertThat(properties.getTransaction().getHeaderName())
                                    .isEqualTo("X-Correlation-Id");
                            assertThat(properties.getTransaction().isAcceptIncoming()).isFalse();
                            assertThat(properties.getTransaction().getMaxLength()).isEqualTo(64);
                        });
    }

    @Test
    void bindsReviewedAsyncDefaults() {
        contextRunner.run(
                context -> {
                    LoggingProperties.Async async =
                            context.getBean(LoggingProperties.class).getAsync();
                    assertThat(async.isEnabled()).isTrue();
                    assertThat(async.getQueueSize()).isEqualTo(2048);
                    assertThat(async.getSaturationPolicy())
                            .isEqualTo(LoggingProperties.Async.SaturationPolicy.BLOCK);
                    assertThat(async.isCriticalEventProtectionEnabled()).isTrue();
                    assertThat(async.getDiscardingThreshold()).isZero();
                    assertThat(async.isNeverBlock()).isFalse();
                    assertThat(async.getMaxFlushTimeMs()).isEqualTo(1000);
                    assertThat(async.getObservability().isEnabled()).isTrue();
                });
    }

    @Test
    void failsFastWhenQueueSizeIsInvalid() {
        contextRunner
                .withPropertyValues("smbtech.logging.async.queue-size=128")
                .run(
                        context ->
                                assertThat(context)
                                        .hasFailed()
                                        .getFailure()
                                        .hasRootCauseMessage(
                                                "Invalid async logging configuration: "
                                                        + "smbtech.logging.async.queue-size must "
                                                        + "be between 256 and 65536 (inclusive) "
                                                        + "(was 128)"));
    }

    @Test
    void failsFastWhenDiscardingThresholdIsNotSmallerThanQueue() {
        contextRunner
                .withPropertyValues(
                        "smbtech.logging.async.queue-size=512",
                        "smbtech.logging.async.discarding-threshold=512")
                .run(
                        context ->
                                assertThat(context)
                                        .hasFailed()
                                        .getFailure()
                                        .hasRootCauseMessage(
                                                "Invalid async logging configuration: "
                                                        + "smbtech.logging.async.discarding-threshold "
                                                        + "must be between 0 (inclusive) and "
                                                        + "smbtech.logging.async.queue-size "
                                                        + "(exclusive) (was 512)"));
    }

    @Test
    void failsFastWhenFlushTimeIsInvalid() {
        contextRunner
                .withPropertyValues("smbtech.logging.async.max-flush-time-ms=30001")
                .run(
                        context ->
                                assertThat(context)
                                        .hasFailed()
                                        .getFailure()
                                        .hasRootCauseMessage(
                                                "Invalid async logging configuration: "
                                                        + "smbtech.logging.async.max-flush-time-ms "
                                                        + "must be between 100 and 30000 "
                                                        + "(inclusive) (was 30001)"));
    }
}
