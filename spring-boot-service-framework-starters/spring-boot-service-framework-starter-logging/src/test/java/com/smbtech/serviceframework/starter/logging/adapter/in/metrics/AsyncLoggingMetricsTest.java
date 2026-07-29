package com.smbtech.serviceframework.starter.logging.adapter.in.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AsyncLoggingMetricsTest {

    @Test
    void bindsBoundedMetricsForActiveAppender() {
        PolicyAwareAsyncAppender appender = appender(8);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        try {
            new AsyncLoggingMetrics(() -> Optional.of(appender)).bindTo(registry);

            assertThat(registry.get(AsyncLoggingMetrics.QUEUE_CAPACITY_METRIC_NAME).gauge().value())
                    .isEqualTo(8);
            assertThat(registry.get(AsyncLoggingMetrics.QUEUE_DEPTH_METRIC_NAME).gauge().value())
                    .isZero();
            assertThat(
                            registry.get(AsyncLoggingMetrics.QUEUE_REMAINING_METRIC_NAME)
                                    .gauge()
                                    .value())
                    .isEqualTo(8);
            assertThat(
                            registry.get(AsyncLoggingMetrics.DISCARDED_EVENTS_METRIC_NAME)
                                    .tag("reason", "low_priority")
                                    .functionCounter()
                                    .count())
                    .isZero();
            assertThat(
                            registry.get(AsyncLoggingMetrics.DISCARDED_EVENTS_METRIC_NAME)
                                    .tag("reason", "full_queue")
                                    .functionCounter()
                                    .count())
                    .isZero();
            assertThat(
                            registry.get(AsyncLoggingMetrics.CRITICAL_FALLBACKS_METRIC_NAME)
                                    .functionCounter()
                                    .count())
                    .isZero();
            assertThat(
                            registry.get(AsyncLoggingMetrics.PRODUCER_BLOCK_METRIC_NAME)
                                    .functionTimer()
                                    .count())
                    .isZero();
            assertThat(
                            registry.get(AsyncLoggingMetrics.ACCEPTING_EVENTS_METRIC_NAME)
                                    .gauge()
                                    .value())
                    .isEqualTo(1);
            assertThat(
                            registry.get(AsyncLoggingMetrics.REJECTED_EVENTS_METRIC_NAME)
                                    .functionCounter()
                                    .count())
                    .isZero();

            appender.stop();

            assertThat(
                            registry.get(AsyncLoggingMetrics.ACCEPTING_EVENTS_METRIC_NAME)
                                    .gauge()
                                    .value())
                    .isZero();
            assertThat(
                            registry.get(AsyncLoggingMetrics.SHUTDOWN_METRIC_NAME)
                                    .functionTimer()
                                    .count())
                    .isEqualTo(1);
            assertThat(
                            registry.get(AsyncLoggingMetrics.SHUTDOWN_METRIC_NAME)
                                    .functionTimer()
                                    .totalTime(java.util.concurrent.TimeUnit.NANOSECONDS))
                    .isPositive();
            assertThat(
                            registry.get(AsyncLoggingMetrics.SHUTDOWN_TIMEOUTS_METRIC_NAME)
                                    .functionCounter()
                                    .count())
                    .isZero();
            assertThat(
                            registry.get(AsyncLoggingMetrics.SHUTDOWN_PENDING_METRIC_NAME)
                                    .gauge()
                                    .value())
                    .isZero();
        } finally {
            appender.stop();
            ((LoggerContext) appender.getContext()).stop();
            registry.close();
        }
    }

    @Test
    void doesNotRegisterMetricsWithoutAnActiveAppender() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        try {
            new AsyncLoggingMetrics(Optional::empty).bindTo(registry);

            assertThat(registry.getMeters()).isEmpty();
        } finally {
            registry.close();
        }
    }

    private static PolicyAwareAsyncAppender appender(int queueSize) {
        LoggerContext context = new LoggerContext();
        NoOpAppender delegate = new NoOpAppender();
        delegate.setContext(context);
        delegate.start();

        PolicyAwareAsyncAppender appender = new PolicyAwareAsyncAppender();
        appender.setContext(context);
        appender.setQueueSize(queueSize);
        appender.setSaturationPolicy("BLOCK");
        appender.addAppender(delegate);
        appender.start();
        return appender;
    }

    private static final class NoOpAppender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {}
    }
}
