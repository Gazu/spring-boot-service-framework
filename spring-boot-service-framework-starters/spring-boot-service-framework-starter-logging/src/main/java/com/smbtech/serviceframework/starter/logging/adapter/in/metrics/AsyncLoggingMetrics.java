package com.smbtech.serviceframework.starter.logging.adapter.in.metrics;

import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Publishes bounded-cardinality metrics for the active framework async appender. */
public final class AsyncLoggingMetrics implements MeterBinder {

    /** Configured asynchronous queue capacity. */
    public static final String QUEUE_CAPACITY_METRIC_NAME = "smbtech.logging.async.queue.capacity";

    /** Current number of queued logging events. */
    public static final String QUEUE_DEPTH_METRIC_NAME = "smbtech.logging.async.queue.depth";

    /** Current remaining asynchronous queue capacity. */
    public static final String QUEUE_REMAINING_METRIC_NAME =
            "smbtech.logging.async.queue.remaining";

    /** Number of discarded logging events, tagged by a bounded reason. */
    public static final String DISCARDED_EVENTS_METRIC_NAME =
            "smbtech.logging.async.events.discarded";

    /** Number of critical events delivered through synchronous fallback. */
    public static final String CRITICAL_FALLBACKS_METRIC_NAME =
            "smbtech.logging.async.critical.fallbacks";

    /** Producer wait count and cumulative duration observed on a full queue. */
    public static final String PRODUCER_BLOCK_METRIC_NAME = "smbtech.logging.async.producer.block";

    /** Whether the asynchronous appender currently accepts events. */
    public static final String ACCEPTING_EVENTS_METRIC_NAME = "smbtech.logging.async.accepting";

    /** Number of events rejected after shutdown admission closed. */
    public static final String REJECTED_EVENTS_METRIC_NAME =
            "smbtech.logging.async.events.rejected";

    /** Shutdown count and cumulative duration. */
    public static final String SHUTDOWN_METRIC_NAME = "smbtech.logging.async.shutdown";

    /** Number of shutdown attempts that exceeded the flush timeout. */
    public static final String SHUTDOWN_TIMEOUTS_METRIC_NAME =
            "smbtech.logging.async.shutdown.timeouts";

    /** Events left in the queue when the last shutdown attempt returned. */
    public static final String SHUTDOWN_PENDING_METRIC_NAME =
            "smbtech.logging.async.shutdown.pending";

    private static final String REASON_TAG = "reason";
    private static final String LOW_PRIORITY_REASON = "low_priority";
    private static final String FULL_QUEUE_REASON = "full_queue";

    private final Supplier<Optional<PolicyAwareAsyncAppender>> appenderSupplier;

    /** Creates a metrics binder for the active framework async appender. */
    public AsyncLoggingMetrics() {
        this(PolicyAwareAsyncAppender::findActive);
    }

    AsyncLoggingMetrics(Supplier<Optional<PolicyAwareAsyncAppender>> appenderSupplier) {
        this.appenderSupplier =
                Objects.requireNonNull(appenderSupplier, "appenderSupplier must not be null");
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Objects.requireNonNull(registry, "registry must not be null");
        appenderSupplier.get().ifPresent(appender -> bind(registry, appender));
    }

    private static void bind(MeterRegistry registry, PolicyAwareAsyncAppender appender) {
        Gauge.builder(QUEUE_CAPACITY_METRIC_NAME, appender, PolicyAwareAsyncAppender::getQueueSize)
                .description("Configured asynchronous logging queue capacity")
                .baseUnit("events")
                .register(registry);
        Gauge.builder(
                        QUEUE_DEPTH_METRIC_NAME,
                        appender,
                        PolicyAwareAsyncAppender::getNumberOfElementsInQueue)
                .description("Current asynchronous logging queue depth")
                .baseUnit("events")
                .register(registry);
        Gauge.builder(
                        QUEUE_REMAINING_METRIC_NAME,
                        appender,
                        PolicyAwareAsyncAppender::getRemainingCapacity)
                .description("Current remaining asynchronous logging queue capacity")
                .baseUnit("events")
                .register(registry);
        FunctionCounter.builder(
                        DISCARDED_EVENTS_METRIC_NAME,
                        appender,
                        PolicyAwareAsyncAppender::getDiscardedLowPriorityEventCount)
                .description("Logging events discarded by the asynchronous appender")
                .baseUnit("events")
                .tag(REASON_TAG, LOW_PRIORITY_REASON)
                .register(registry);
        FunctionCounter.builder(
                        DISCARDED_EVENTS_METRIC_NAME,
                        appender,
                        PolicyAwareAsyncAppender::getDiscardedFullQueueEventCount)
                .description("Logging events discarded by the asynchronous appender")
                .baseUnit("events")
                .tag(REASON_TAG, FULL_QUEUE_REASON)
                .register(registry);
        FunctionCounter.builder(
                        CRITICAL_FALLBACKS_METRIC_NAME,
                        appender,
                        PolicyAwareAsyncAppender::getCriticalFallbackEventCount)
                .description("Critical logging events delivered through synchronous fallback")
                .baseUnit("events")
                .register(registry);
        FunctionTimer.builder(
                        PRODUCER_BLOCK_METRIC_NAME,
                        appender,
                        PolicyAwareAsyncAppender::getBlockedProducerEventCount,
                        PolicyAwareAsyncAppender::getBlockedProducerDurationNanos,
                        TimeUnit.NANOSECONDS)
                .description("Observed producer waits caused by a full asynchronous logging queue")
                .register(registry);
        Gauge.builder(
                        ACCEPTING_EVENTS_METRIC_NAME,
                        appender,
                        value -> value.isAcceptingEvents() ? 1.0 : 0.0)
                .description("Whether the asynchronous logging appender currently accepts events")
                .register(registry);
        FunctionCounter.builder(
                        REJECTED_EVENTS_METRIC_NAME,
                        appender,
                        PolicyAwareAsyncAppender::getRejectedDuringShutdownEventCount)
                .description("Logging events rejected after shutdown admission closed")
                .baseUnit("events")
                .register(registry);
        FunctionTimer.builder(
                        SHUTDOWN_METRIC_NAME,
                        appender,
                        PolicyAwareAsyncAppender::getShutdownCount,
                        PolicyAwareAsyncAppender::getShutdownDurationNanos,
                        TimeUnit.NANOSECONDS)
                .description("Asynchronous logging shutdown attempts")
                .register(registry);
        FunctionCounter.builder(
                        SHUTDOWN_TIMEOUTS_METRIC_NAME,
                        appender,
                        PolicyAwareAsyncAppender::getShutdownTimeoutCount)
                .description("Asynchronous logging shutdown attempts exceeding the flush timeout")
                .register(registry);
        Gauge.builder(
                        SHUTDOWN_PENDING_METRIC_NAME,
                        appender,
                        PolicyAwareAsyncAppender::getLastShutdownPendingEventCount)
                .description("Events left queued when the last logging shutdown attempt returned")
                .baseUnit("events")
                .register(registry);
    }
}
