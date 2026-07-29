package com.smbtech.serviceframework.starter.logging.adapter.out.logback;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class AsyncAppenderConcurrencyTest {

    @Test
    @Timeout(15)
    void preservesEveryEventAndPerProducerOrderUnderBlockContention() throws Exception {
        int producerCount = 8;
        int eventsPerProducer = 500;
        ConcurrentRecordingAppender delegate = new ConcurrentRecordingAppender(0, false);
        PolicyAwareAsyncAppender appender = appender("BLOCK", 64, 0, delegate);
        ExecutorService executor = Executors.newFixedThreadPool(producerCount);

        try {
            runProducers(
                    executor,
                    producerCount,
                    producer ->
                            emitApplicationEvents(
                                    appender, producer, eventsPerProducer, "block", false));
            appender.stop();

            List<CapturedEvent> captured = delegate.events();
            assertThat(captured).hasSize(producerCount * eventsPerProducer);
            assertThat(new HashSet<>(captured.stream().map(CapturedEvent::message).toList()))
                    .hasSize(producerCount * eventsPerProducer);
            for (int producer = 0; producer < producerCount; producer++) {
                String producerId = Integer.toString(producer);
                assertThat(
                                captured.stream()
                                        .filter(
                                                event ->
                                                        producerId.equals(
                                                                event.mdc().get("producer")))
                                        .map(
                                                event ->
                                                        Integer.parseInt(
                                                                event.mdc().get("sequence"))))
                        .containsExactlyElementsOf(sequence(eventsPerProducer));
            }
            assertThat(appender.getDiscardedLowPriorityEventCount()).isZero();
            assertThat(appender.getDiscardedFullQueueEventCount()).isZero();
            assertThat(appender.getShutdownTimeoutCount()).isZero();
            assertThat(appender.getLastShutdownPendingEventCount()).isZero();
        } finally {
            stop(appender);
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(15)
    void accountsForConcurrentLowPriorityDiscardingWithoutLosingCriticalEvents() throws Exception {
        int producerCount = 8;
        int eventsPerProducer = 250;
        int criticalInterval = 25;
        ConcurrentRecordingAppender delegate =
                new ConcurrentRecordingAppender(MILLISECONDS.toNanos(1), false);
        PolicyAwareAsyncAppender appender = appender("DISCARD_LOW_PRIORITY", 32, 16, delegate);
        ExecutorService executor = Executors.newFixedThreadPool(producerCount);

        try {
            runProducers(
                    executor,
                    producerCount,
                    producer ->
                            emitMixedEvents(
                                    appender,
                                    producer,
                                    eventsPerProducer,
                                    criticalInterval,
                                    "discard"));
            appender.stop();

            int expectedCritical = producerCount * (eventsPerProducer / criticalInterval);
            int expectedApplication = producerCount * eventsPerProducer - expectedCritical;
            List<CapturedEvent> captured = delegate.events();
            long deliveredCritical =
                    captured.stream()
                            .filter(event -> event.message().startsWith("discard-critical-"))
                            .count();
            long deliveredApplication =
                    captured.stream()
                            .filter(event -> event.message().startsWith("discard-application-"))
                            .count();

            assertThat(deliveredCritical).isEqualTo(expectedCritical);
            assertThat(appender.getDiscardedLowPriorityEventCount()).isPositive();
            assertThat(deliveredApplication + appender.getDiscardedLowPriorityEventCount())
                    .isEqualTo(expectedApplication);
            assertThat(appender.getDiscardedFullQueueEventCount()).isZero();
            assertThat(appender.getCriticalFallbackEventCount()).isZero();
            assertThat(appender.getShutdownTimeoutCount()).isZero();
        } finally {
            stop(appender);
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(15)
    void accountsForConcurrentFullQueueDropsAndProtectsCriticalEvents() throws Exception {
        int queueSize = 16;
        int producerCount = 6;
        int eventsPerProducer = 120;
        int criticalInterval = 30;
        ConcurrentRecordingAppender delegate =
                new ConcurrentRecordingAppender(MILLISECONDS.toNanos(1), true);
        PolicyAwareAsyncAppender appender = appender("DROP_WHEN_FULL", queueSize, 0, delegate);
        ExecutorService executor = Executors.newFixedThreadPool(producerCount);

        try {
            appender.doAppend(event(appender, Level.INFO, "worker-blocker", Map.of()));
            assertThat(delegate.awaitWorker(2, SECONDS)).isTrue();
            for (int index = 0; index < queueSize; index++) {
                appender.doAppend(event(appender, Level.INFO, "seed-" + index, Map.of()));
            }
            appender.doAppend(event(appender, Level.INFO, "dropped-seed", Map.of()));

            List<Future<?>> producers =
                    startProducers(
                            executor,
                            producerCount,
                            producer ->
                                    emitMixedEvents(
                                            appender,
                                            producer,
                                            eventsPerProducer,
                                            criticalInterval,
                                            "drop"));
            assertThatThrownBy(() -> producers.get(0).get(150, MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            delegate.release();
            await(producers);
            appender.stop();

            int expectedCritical = producerCount * (eventsPerProducer / criticalInterval);
            int attempted = 1 + queueSize + 1 + producerCount * eventsPerProducer;
            List<CapturedEvent> captured = delegate.events();
            long deliveredCritical =
                    captured.stream()
                            .filter(event -> event.message().startsWith("drop-critical-"))
                            .count();

            assertThat(deliveredCritical).isEqualTo(expectedCritical);
            assertThat(appender.getDiscardedFullQueueEventCount()).isPositive();
            assertThat(captured.size() + appender.getDiscardedFullQueueEventCount())
                    .isEqualTo(attempted);
            assertThat(appender.getCriticalFallbackEventCount()).isPositive();
            assertThat(appender.getDiscardedLowPriorityEventCount()).isZero();
            assertThat(appender.getShutdownTimeoutCount()).isZero();
        } finally {
            delegate.release();
            stop(appender);
            executor.shutdownNow();
        }
    }

    private static void emitApplicationEvents(
            PolicyAwareAsyncAppender appender,
            int producer,
            int eventCount,
            String prefix,
            boolean critical) {
        for (int sequence = 0; sequence < eventCount; sequence++) {
            Level level = critical ? Level.WARN : Level.INFO;
            appender.doAppend(
                    event(
                            appender,
                            level,
                            prefix + "-" + producer + "-" + sequence,
                            mdc(producer, sequence)));
        }
    }

    private static void emitMixedEvents(
            PolicyAwareAsyncAppender appender,
            int producer,
            int eventCount,
            int criticalInterval,
            String prefix) {
        for (int sequence = 0; sequence < eventCount; sequence++) {
            boolean critical = sequence % criticalInterval == 0;
            String classification = critical ? "critical" : "application";
            Object[] arguments =
                    critical && sequence % (criticalInterval * 2) != 0
                            ? new Object[] {
                                StructuredEvent.builder(EventType.AUDIT)
                                        .message("concurrency audit")
                                        .build()
                            }
                            : new Object[0];
            Level level = critical && arguments.length == 0 ? Level.WARN : Level.INFO;
            appender.doAppend(
                    event(
                            appender,
                            level,
                            prefix + "-" + classification + "-" + producer + "-" + sequence,
                            mdc(producer, sequence),
                            arguments));
        }
    }

    private static Map<String, String> mdc(int producer, int sequence) {
        return Map.of(
                "producer", Integer.toString(producer),
                "sequence", Integer.toString(sequence));
    }

    private static List<Integer> sequence(int size) {
        List<Integer> values = new ArrayList<>(size);
        for (int value = 0; value < size; value++) {
            values.add(value);
        }
        return values;
    }

    private static void runProducers(
            ExecutorService executor, int producerCount, ProducerAction action) throws Exception {
        await(startProducers(executor, producerCount, action));
    }

    private static List<Future<?>> startProducers(
            ExecutorService executor, int producerCount, ProducerAction action)
            throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(producerCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>(producerCount);
        for (int producer = 0; producer < producerCount; producer++) {
            int producerId = producer;
            futures.add(
                    executor.submit(
                            () -> {
                                ready.countDown();
                                await(start);
                                action.run(producerId);
                            }));
        }
        assertThat(ready.await(2, SECONDS)).isTrue();
        start.countDown();
        return futures;
    }

    private static void await(List<Future<?>> futures) throws Exception {
        for (Future<?> future : futures) {
            future.get(10, SECONDS);
        }
    }

    private static void await(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException interruptedException) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static PolicyAwareAsyncAppender appender(
            String policy,
            int queueSize,
            int discardingThreshold,
            ConcurrentRecordingAppender delegate) {
        LoggerContext context = new LoggerContext();
        delegate.setContext(context);
        delegate.start();

        PolicyAwareAsyncAppender appender = new PolicyAwareAsyncAppender();
        appender.setContext(context);
        appender.setName("ASYNC");
        appender.setQueueSize(queueSize);
        appender.setSaturationPolicy(policy);
        appender.setConfiguredDiscardingThreshold(discardingThreshold);
        appender.setCriticalEventProtectionEnabled(true);
        appender.setMaxFlushTime(5000);
        appender.addAppender(delegate);
        appender.start();
        return appender;
    }

    private static void stop(PolicyAwareAsyncAppender appender) {
        appender.stop();
        if (appender.getContext() instanceof LoggerContext context) {
            context.stop();
        }
    }

    private static LoggingEvent event(
            PolicyAwareAsyncAppender appender,
            Level level,
            String message,
            Map<String, String> mdc,
            Object... arguments) {
        LoggerContext context = (LoggerContext) appender.getContext();
        Logger logger = context.getLogger("async-concurrency-test");
        LoggingEvent event =
                new LoggingEvent(
                        AsyncAppenderConcurrencyTest.class.getName(),
                        logger,
                        level,
                        message,
                        null,
                        arguments);
        event.setMDCPropertyMap(mdc);
        return event;
    }

    @FunctionalInterface
    private interface ProducerAction {
        void run(int producer);
    }

    private record CapturedEvent(String message, Map<String, String> mdc) {}

    private static final class ConcurrentRecordingAppender extends AppenderBase<ILoggingEvent> {
        private final List<CapturedEvent> events = Collections.synchronizedList(new ArrayList<>());
        private final long delayNanos;
        private final AtomicBoolean blockFirstEvent;
        private final CountDownLatch workerStarted = new CountDownLatch(1);
        private final CountDownLatch releaseWorker = new CountDownLatch(1);

        private ConcurrentRecordingAppender(long delayNanos, boolean blockFirstEvent) {
            this.delayNanos = delayNanos;
            this.blockFirstEvent = new AtomicBoolean(blockFirstEvent);
        }

        @Override
        protected void append(ILoggingEvent event) {
            if (blockFirstEvent.compareAndSet(true, false)) {
                workerStarted.countDown();
                await(releaseWorker);
            }
            if (delayNanos > 0) {
                LockSupport.parkNanos(delayNanos);
            }
            events.add(
                    new CapturedEvent(
                            event.getFormattedMessage(), Map.copyOf(event.getMDCPropertyMap())));
        }

        private boolean awaitWorker(long timeout, java.util.concurrent.TimeUnit unit)
                throws InterruptedException {
            return workerStarted.await(timeout, unit);
        }

        private void release() {
            releaseWorker.countDown();
        }

        private List<CapturedEvent> events() {
            synchronized (events) {
                return List.copyOf(events);
            }
        }
    }
}
