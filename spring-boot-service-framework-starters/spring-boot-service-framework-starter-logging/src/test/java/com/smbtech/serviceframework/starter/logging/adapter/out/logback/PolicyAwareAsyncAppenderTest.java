package com.smbtech.serviceframework.starter.logging.adapter.out.logback;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PolicyAwareAsyncAppenderTest {

    @Test
    void blocksWithoutProactiveDiscardingByDefault() {
        PolicyAwareAsyncAppender appender = appender("BLOCK", 1000, 0, false);

        try {
            assertThat(appender.isNeverBlock()).isFalse();
            assertThat(appender.getDiscardingThreshold()).isZero();
        } finally {
            stop(appender);
        }
    }

    @Test
    void discardsLowPriorityEventsUsingTwentyPercentByDefault() {
        PolicyAwareAsyncAppender appender = appender("DISCARD_LOW_PRIORITY", 1000, 0, false);

        try {
            assertThat(appender.isNeverBlock()).isFalse();
            assertThat(appender.getDiscardingThreshold()).isEqualTo(200);
        } finally {
            stop(appender);
        }
    }

    @Test
    void usesExplicitLowPriorityDiscardingThreshold() {
        PolicyAwareAsyncAppender appender = appender("DISCARD_LOW_PRIORITY", 1000, 25, false);

        try {
            assertThat(appender.isNeverBlock()).isFalse();
            assertThat(appender.getDiscardingThreshold()).isEqualTo(25);
        } finally {
            stop(appender);
        }
    }

    @Test
    void dropsAnyLevelWithoutBlockingWhenConfigured() {
        PolicyAwareAsyncAppender appender = appender("DROP_WHEN_FULL", 1000, 25, false);

        try {
            assertThat(appender.isNeverBlock()).isTrue();
            assertThat(appender.getDiscardingThreshold()).isZero();
        } finally {
            stop(appender);
        }
    }

    @Test
    void preservesFormerNeverBlockAndDiscardingThresholdBehavior() {
        PolicyAwareAsyncAppender neverBlock = appender("BLOCK", 1000, 0, true);
        PolicyAwareAsyncAppender discarding = appender("BLOCK", 1000, 40, false);

        try {
            assertThat(neverBlock.isNeverBlock()).isTrue();
            assertThat(neverBlock.getDiscardingThreshold()).isZero();
            assertThat(discarding.isNeverBlock()).isFalse();
            assertThat(discarding.getDiscardingThreshold()).isEqualTo(40);
        } finally {
            stop(neverBlock);
            stop(discarding);
        }
    }

    @Test
    void neverDiscardsCriticalLevelsOrStructuredEventTypesProactively() {
        PolicyAwareAsyncAppender appender = appender("DISCARD_LOW_PRIORITY", 1000, 200, false);

        try {
            assertThat(appender.isDiscardable(event(appender, Level.INFO, "application"))).isTrue();
            assertThat(appender.isDiscardable(event(appender, Level.WARN, "warning"))).isFalse();
            assertThat(
                            appender.isDiscardable(
                                    event(
                                            appender,
                                            Level.INFO,
                                            "audit",
                                            structured(EventType.AUDIT))))
                    .isFalse();
            assertThat(
                            appender.isDiscardable(
                                    event(
                                            appender,
                                            Level.INFO,
                                            "security",
                                            structured(EventType.SECURITY))))
                    .isFalse();
            assertThat(appender.getDiscardedLowPriorityEventCount()).isEqualTo(1);
        } finally {
            stop(appender);
        }
    }

    @Test
    void usesSynchronousFallbackForCriticalEventWhenDropQueueIsFull() throws Exception {
        BlockingRecordingAppender delegate = new BlockingRecordingAppender();
        PolicyAwareAsyncAppender appender = appender("DROP_WHEN_FULL", 1, 0, false, true, delegate);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            appender.doAppend(event(appender, Level.INFO, "worker-blocker"));
            assertThat(delegate.awaitWorker(2, TimeUnit.SECONDS)).isTrue();
            appender.doAppend(event(appender, Level.INFO, "queued"));
            appender.doAppend(event(appender, Level.INFO, "dropped"));

            Future<?> critical =
                    executor.submit(
                            () ->
                                    appender.doAppend(
                                            event(
                                                    appender,
                                                    Level.INFO,
                                                    "audit",
                                                    structured(EventType.AUDIT))));

            assertThatThrownBy(() -> critical.get(150, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            delegate.release();
            critical.get(2, TimeUnit.SECONDS);
            appender.stop();

            assertThat(delegate.messages())
                    .contains("worker-blocker", "queued", "audit")
                    .doesNotContain("dropped");
            assertThat(appender.getDiscardedFullQueueEventCount()).isEqualTo(1);
            assertThat(appender.getCriticalFallbackEventCount()).isEqualTo(1);
        } finally {
            delegate.release();
            stop(appender);
            executor.shutdownNow();
        }
    }

    @Test
    void observesProducerWaitWhenBlockingQueueIsFull() throws Exception {
        BlockingRecordingAppender delegate = new BlockingRecordingAppender();
        PolicyAwareAsyncAppender appender = appender("BLOCK", 1, 0, false, true, delegate);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            appender.doAppend(event(appender, Level.INFO, "worker-blocker"));
            assertThat(delegate.awaitWorker(2, TimeUnit.SECONDS)).isTrue();
            appender.doAppend(event(appender, Level.INFO, "queued"));

            Future<?> blocked =
                    executor.submit(
                            () ->
                                    appender.doAppend(
                                            event(appender, Level.INFO, "blocked-producer")));

            assertThatThrownBy(() -> blocked.get(150, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            delegate.release();
            blocked.get(2, TimeUnit.SECONDS);

            assertThat(appender.getBlockedProducerEventCount()).isEqualTo(1);
            assertThat(appender.getBlockedProducerDurationNanos()).isPositive();
        } finally {
            delegate.release();
            stop(appender);
            executor.shutdownNow();
        }
    }

    @Test
    void reportsPendingEventsWhenShutdownExceedsFlushTimeout() throws Exception {
        UninterruptibleBlockingAppender delegate = new UninterruptibleBlockingAppender();
        PolicyAwareAsyncAppender appender = appender("BLOCK", 1, 0, false, true, delegate);
        appender.setMaxFlushTime(100);

        try {
            appender.doAppend(event(appender, Level.INFO, "worker-blocker"));
            assertThat(delegate.awaitWorker(2, TimeUnit.SECONDS)).isTrue();
            appender.doAppend(event(appender, Level.INFO, "queued"));

            appender.stop();

            assertThat(appender.isAcceptingEvents()).isFalse();
            assertThat(appender.getShutdownCount()).isEqualTo(1);
            assertThat(appender.getShutdownDurationNanos()).isPositive();
            assertThat(appender.getShutdownTimeoutCount()).isEqualTo(1);
            assertThat(appender.getLastShutdownPendingEventCount()).isEqualTo(1);
            assertThat(appender.isLastShutdownTimedOut()).isTrue();

            delegate.release();
            assertThat(delegate.awaitStopped(2, TimeUnit.SECONDS)).isTrue();
            assertThat(delegate.messages()).containsExactly("worker-blocker", "queued");
        } finally {
            delegate.release();
            stop(appender);
        }
    }

    private static PolicyAwareAsyncAppender appender(
            String policy, int queueSize, int discardingThreshold, boolean legacyNeverBlock) {
        return appender(
                policy, queueSize, discardingThreshold, legacyNeverBlock, true, new NoOpAppender());
    }

    private static PolicyAwareAsyncAppender appender(
            String policy,
            int queueSize,
            int discardingThreshold,
            boolean legacyNeverBlock,
            boolean criticalEventProtectionEnabled,
            AppenderBase<ILoggingEvent> delegate) {
        LoggerContext context = new LoggerContext();
        delegate.setContext(context);
        delegate.start();

        PolicyAwareAsyncAppender appender = new PolicyAwareAsyncAppender();
        appender.setContext(context);
        appender.setQueueSize(queueSize);
        appender.setSaturationPolicy(policy);
        appender.setConfiguredDiscardingThreshold(discardingThreshold);
        appender.setLegacyNeverBlock(legacyNeverBlock);
        appender.setCriticalEventProtectionEnabled(criticalEventProtectionEnabled);
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
            PolicyAwareAsyncAppender appender, Level level, String message, Object... arguments) {
        LoggerContext context = (LoggerContext) appender.getContext();
        Logger logger = context.getLogger("critical-event-test");
        LoggingEvent event =
                new LoggingEvent(
                        PolicyAwareAsyncAppenderTest.class.getName(),
                        logger,
                        level,
                        message,
                        null,
                        arguments);
        event.setMDCPropertyMap(Map.of("transactionId", "test"));
        return event;
    }

    private static StructuredEvent structured(EventType eventType) {
        return StructuredEvent.builder(eventType).message("critical").build();
    }

    private static final class NoOpAppender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {}
    }

    private static final class BlockingRecordingAppender extends AppenderBase<ILoggingEvent> {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch workerStarted = new CountDownLatch(1);
        private final CountDownLatch releaseWorker = new CountDownLatch(1);
        private final List<String> messages = new CopyOnWriteArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            if (calls.getAndIncrement() == 0) {
                workerStarted.countDown();
                try {
                    releaseWorker.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            messages.add(event.getFormattedMessage());
        }

        private boolean awaitWorker(long timeout, TimeUnit unit) throws InterruptedException {
            return workerStarted.await(timeout, unit);
        }

        private void release() {
            releaseWorker.countDown();
        }

        private List<String> messages() {
            return List.copyOf(messages);
        }
    }

    private static final class UninterruptibleBlockingAppender extends AppenderBase<ILoggingEvent> {
        private final CountDownLatch workerStarted = new CountDownLatch(1);
        private final CountDownLatch releaseWorker = new CountDownLatch(1);
        private final CountDownLatch stopped = new CountDownLatch(1);
        private final List<String> messages = new CopyOnWriteArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            workerStarted.countDown();
            boolean interrupted = false;
            while (true) {
                try {
                    releaseWorker.await();
                    break;
                } catch (InterruptedException interruptedException) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            messages.add(event.getFormattedMessage());
        }

        @Override
        public void stop() {
            super.stop();
            stopped.countDown();
        }

        private boolean awaitWorker(long timeout, TimeUnit unit) throws InterruptedException {
            return workerStarted.await(timeout, unit);
        }

        private void release() {
            releaseWorker.countDown();
        }

        private boolean awaitStopped(long timeout, TimeUnit unit) throws InterruptedException {
            return stopped.await(timeout, unit);
        }

        private List<String> messages() {
            return List.copyOf(messages);
        }
    }
}
