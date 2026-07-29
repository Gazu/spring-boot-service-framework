package com.smbtech.serviceframework.starter.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

final class AsyncAppenderBaseline {
    private static final int WARMUP_EVENTS = 10_000;
    private static final int MEASURED_EVENTS = 100_000;

    private AsyncAppenderBaseline() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected the output report path");
        }

        run(false, WARMUP_EVENTS);
        run(true, WARMUP_EVENTS);
        Measurement synchronous = run(false, MEASURED_EVENTS);
        Measurement asynchronous = run(true, MEASURED_EVENTS);

        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());
        Files.writeString(output, report(synchronous, asynchronous));
        System.out.println("Async logging baseline written to " + output.toAbsolutePath());
    }

    private static Measurement run(boolean asynchronous, int eventCount) {
        LoggerContext context = new LoggerContext();
        CountingAppender delegate = new CountingAppender();
        delegate.setContext(context);
        delegate.start();
        Logger logger = context.getLogger("async-baseline");
        PolicyAwareAsyncAppender async = null;

        if (asynchronous) {
            async = new PolicyAwareAsyncAppender();
            async.setContext(context);
            async.setQueueSize(2048);
            async.setSaturationPolicy("BLOCK");
            async.setMaxFlushTime(30_000);
            async.addAppender(delegate);
            async.start();
        }

        long producerStart = System.nanoTime();
        for (int index = 0; index < eventCount; index++) {
            LoggingEvent event =
                    new LoggingEvent(
                            AsyncAppenderBaseline.class.getName(),
                            logger,
                            Level.INFO,
                            "baseline-event",
                            null,
                            new Object[] {index});
            event.setMDCPropertyMap(Map.of("transactionId", "baseline"));
            if (async != null) {
                async.doAppend(event);
            } else {
                delegate.doAppend(event);
            }
        }
        long producerEnd = System.nanoTime();
        if (async != null) {
            async.stop();
        }
        long completed = System.nanoTime();

        delegate.stop();
        context.stop();
        if (delegate.count() != eventCount) {
            throw new IllegalStateException(
                    "Expected " + eventCount + " events but delivered " + delegate.count());
        }
        return new Measurement(eventCount, producerEnd - producerStart, completed - producerStart);
    }

    private static String report(Measurement synchronous, Measurement asynchronous) {
        return String.format(
                Locale.ROOT,
                """
                # Async Appender Local Baseline

                Generated at: `%s`

                This report is diagnostic evidence for the current machine. It is not a portable
                performance target and does not fail the build.

                | Measurement | Synchronous | Asynchronous |
                |---|---:|---:|
                | Events | %,d | %,d |
                | Producer ns/event | %,.0f | %,.0f |
                | End-to-end events/second | %,.0f | %,.0f |
                | Drain after producer (ms) | 0 | %,.3f |

                Runtime: `%s %s`; Java: `%s`; processors: `%d`.
                """,
                Instant.now(),
                synchronous.events(),
                asynchronous.events(),
                synchronous.producerNanosPerEvent(),
                asynchronous.producerNanosPerEvent(),
                synchronous.eventsPerSecond(),
                asynchronous.eventsPerSecond(),
                asynchronous.drainMillis(),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors());
    }

    private record Measurement(int events, long producerNanos, long totalNanos) {
        private double producerNanosPerEvent() {
            return (double) producerNanos / events;
        }

        private double eventsPerSecond() {
            return events * 1_000_000_000.0 / totalNanos;
        }

        private double drainMillis() {
            return Math.max(0, totalNanos - producerNanos) / 1_000_000.0;
        }
    }

    private static final class CountingAppender extends AppenderBase<ILoggingEvent> {
        private final LongAdder count = new LongAdder();

        @Override
        protected void append(ILoggingEvent event) {
            count.increment();
        }

        private long count() {
            return count.sum();
        }
    }
}
