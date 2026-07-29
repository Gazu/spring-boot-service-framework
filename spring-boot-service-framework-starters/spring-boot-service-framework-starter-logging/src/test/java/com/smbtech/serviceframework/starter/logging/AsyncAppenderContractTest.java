package com.smbtech.serviceframework.starter.logging;

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
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AsyncAppenderContractTest {

    @Test
    void preservesOrderMdcStructuredDataAndThrowable() throws InterruptedException {
        LoggerContext context = new LoggerContext();
        RecordingAppender delegate = new RecordingAppender(2);
        PolicyAwareAsyncAppender async = asyncAppender(context, delegate, 16, false, 1000);
        Logger logger = context.getLogger("async-contract");
        IllegalStateException failure = new IllegalStateException("boom");
        StructuredEvent payload =
                StructuredEvent.builder(EventType.AUDIT)
                        .message("audit event")
                        .with("paymentId", "pay-1")
                        .build();

        try {
            async.doAppend(event(logger, "first", null, Map.of("transactionId", "tx-1"), payload));
            async.doAppend(event(logger, "second", failure, Map.of("transactionId", "tx-2")));

            assertThat(delegate.await(2, SECONDS)).isTrue();
            assertThat(delegate.events())
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly("first", "second");
            assertThat(delegate.events().get(0).getMDCPropertyMap())
                    .containsEntry("transactionId", "tx-1");
            assertThat(delegate.events().get(0).getArgumentArray()).containsExactly(payload);
            assertThat(delegate.events().get(1).getThrowableProxy().getMessage()).isEqualTo("boom");
        } finally {
            async.stop();
            delegate.stop();
            context.stop();
        }
    }

    @Test
    void blocksProducerByDefaultWhenQueueIsFull() throws Exception {
        LoggerContext context = new LoggerContext();
        BlockingAppender delegate = new BlockingAppender();
        PolicyAwareAsyncAppender async = asyncAppender(context, delegate, 1, false, 1000);
        Logger logger = context.getLogger("async-backpressure-contract");
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            async.doAppend(event(logger, "worker-blocker", null, Map.of()));
            assertThat(delegate.awaitWorker(2, SECONDS)).isTrue();
            async.doAppend(event(logger, "queued", null, Map.of()));

            Future<?> blockedProducer =
                    executor.submit(
                            () ->
                                    async.doAppend(
                                            event(logger, "producer-blocker", null, Map.of())));

            assertThatThrownBy(() -> blockedProducer.get(150, MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            delegate.release();
            blockedProducer.get(2, SECONDS);
            async.stop();

            assertThat(delegate.messages())
                    .containsExactly("worker-blocker", "queued", "producer-blocker");
        } finally {
            delegate.release();
            async.stop();
            delegate.stop();
            context.stop();
            executor.shutdownNow();
        }
    }

    @Test
    void drainsAcceptedEventsDuringOrderlyShutdown() {
        LoggerContext context = new LoggerContext();
        RecordingAppender delegate = new RecordingAppender(100);
        PolicyAwareAsyncAppender async = asyncAppender(context, delegate, 128, false, 5000);
        Logger logger = context.getLogger("async-shutdown-contract");

        try {
            for (int index = 0; index < 100; index++) {
                async.doAppend(event(logger, "event-" + index, null, Map.of()));
            }

            async.stop();
            async.stop();

            assertThat(delegate.events()).hasSize(100);
            assertThat(delegate.events().get(99).getFormattedMessage()).isEqualTo("event-99");
            assertThat(async.isAcceptingEvents()).isFalse();
            assertThat(async.getShutdownCount()).isEqualTo(1);
            assertThat(async.getShutdownTimeoutCount()).isZero();
            assertThat(async.getLastShutdownPendingEventCount()).isZero();
            assertThat(async.isLastShutdownTimedOut()).isFalse();
        } finally {
            async.stop();
            delegate.stop();
            context.stop();
        }
    }

    @Test
    void closesAdmissionAndDrainsAnInFlightBlockedProducerBeforeStopping() throws Exception {
        LoggerContext context = new LoggerContext();
        BlockingAppender delegate = new BlockingAppender();
        PolicyAwareAsyncAppender async = asyncAppender(context, delegate, 1, false, 1000);
        Logger logger = context.getLogger("async-concurrent-shutdown-contract");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            async.doAppend(event(logger, "worker-blocker", null, Map.of()));
            assertThat(delegate.awaitWorker(2, SECONDS)).isTrue();
            async.doAppend(event(logger, "queued", null, Map.of()));

            Future<?> blockedProducer =
                    executor.submit(
                            () ->
                                    async.doAppend(
                                            event(
                                                    logger,
                                                    "accepted-before-shutdown",
                                                    null,
                                                    Map.of())));
            assertThatThrownBy(() -> blockedProducer.get(150, MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            Future<?> shutdown = executor.submit(async::stop);
            awaitAdmissionClosed(async);
            CountDownLatch rejectedStarted = new CountDownLatch(1);
            Future<?> rejected =
                    executor.submit(
                            () -> {
                                rejectedStarted.countDown();
                                async.doAppend(
                                        event(logger, "rejected-during-shutdown", null, Map.of()));
                            });

            assertThat(rejectedStarted.await(2, SECONDS)).isTrue();
            assertThatThrownBy(() -> shutdown.get(150, MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            assertThatThrownBy(() -> rejected.get(150, MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            delegate.release();
            blockedProducer.get(2, SECONDS);
            shutdown.get(2, SECONDS);
            rejected.get(2, SECONDS);

            assertThat(delegate.messages())
                    .containsExactly("worker-blocker", "queued", "accepted-before-shutdown");
            assertThat(async.getRejectedDuringShutdownEventCount()).isEqualTo(1);
            assertThat(async.getShutdownCount()).isEqualTo(1);
            assertThat(async.getShutdownTimeoutCount()).isZero();
            assertThat(async.getLastShutdownPendingEventCount()).isZero();
        } finally {
            delegate.release();
            async.stop();
            delegate.stop();
            context.stop();
            executor.shutdownNow();
        }
    }

    private static void awaitAdmissionClosed(PolicyAwareAsyncAppender async)
            throws InterruptedException {
        long deadline = System.nanoTime() + SECONDS.toNanos(2);
        while (async.isAcceptingEvents() && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertThat(async.isAcceptingEvents()).isFalse();
    }

    private static PolicyAwareAsyncAppender asyncAppender(
            LoggerContext context,
            AppenderBase<ILoggingEvent> delegate,
            int queueSize,
            boolean neverBlock,
            int maxFlushTime) {
        delegate.setContext(context);
        delegate.start();

        PolicyAwareAsyncAppender async = new PolicyAwareAsyncAppender();
        async.setContext(context);
        async.setName("ASYNC");
        async.setQueueSize(queueSize);
        async.setSaturationPolicy("BLOCK");
        async.setLegacyNeverBlock(neverBlock);
        async.setMaxFlushTime(maxFlushTime);
        async.addAppender(delegate);
        async.start();
        return async;
    }

    private static LoggingEvent event(
            Logger logger,
            String message,
            Throwable throwable,
            Map<String, String> mdc,
            Object... args) {
        LoggingEvent event =
                new LoggingEvent(
                        AsyncAppenderContractTest.class.getName(),
                        logger,
                        Level.INFO,
                        message,
                        throwable,
                        args);
        event.setMDCPropertyMap(mdc);
        return event;
    }

    private static final class RecordingAppender extends AppenderBase<ILoggingEvent> {
        private final List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
        private final CountDownLatch received;

        private RecordingAppender(int expectedEvents) {
            received = new CountDownLatch(expectedEvents);
        }

        @Override
        protected void append(ILoggingEvent event) {
            events.add(event);
            received.countDown();
        }

        private boolean await(long timeout, java.util.concurrent.TimeUnit unit)
                throws InterruptedException {
            return received.await(timeout, unit);
        }

        private List<ILoggingEvent> events() {
            return List.copyOf(events);
        }
    }

    private static final class BlockingAppender extends AppenderBase<ILoggingEvent> {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch workerStarted = new CountDownLatch(1);
        private final CountDownLatch releaseWorker = new CountDownLatch(1);
        private final List<String> messages = new CopyOnWriteArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            if (calls.getAndIncrement() == 0) {
                workerStarted.countDown();
                try {
                    releaseWorker.await(5, SECONDS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            messages.add(event.getFormattedMessage());
        }

        private boolean awaitWorker(long timeout, java.util.concurrent.TimeUnit unit)
                throws InterruptedException {
            return workerStarted.await(timeout, unit);
        }

        private void release() {
            releaseWorker.countDown();
        }

        private List<String> messages() {
            return List.copyOf(messages);
        }
    }
}
