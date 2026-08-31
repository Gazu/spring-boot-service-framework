package com.smbtech.serviceframework.starter.logging;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.logging.port.out.LogEventSink;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/** Provides structured loggers behavior. */
public final class StructuredLoggers {
    private static final AtomicBoolean PRODUCTION = new AtomicBoolean();

    private StructuredLoggers() {}

    /**
     * Performs the get operation.
     *
     * @param source source value
     * @return get result
     */
    public static StructuredLogger get(Class<?> source) {
        return StructuredLogger.create(
                new Slf4jLogEventSink(LoggerFactory.getLogger(source)), PRODUCTION::get);
    }

    /**
     * Performs the get operation.
     *
     * @param lookup lookup value
     * @return get result
     */
    public static StructuredLogger get(MethodHandles.Lookup lookup) {
        return get(lookup.lookupClass());
    }

    /**
     * Sets the configured production.
     *
     * @param production production value
     */
    public static void setProduction(boolean production) {
        PRODUCTION.set(production);
    }

    private static final class Slf4jLogEventSink implements LogEventSink {
        private static final String SENSITIVE_MARKER = "SENSITIVE";

        private final Logger logger;

        private Slf4jLogEventSink(Logger logger) {
            this.logger = Objects.requireNonNull(logger, "logger must not be null");
        }

        @Override
        public boolean isEnabled(LogLevel level, EventType eventType) {
            return switch (level) {
                case TRACE -> logger.isTraceEnabled();
                case DEBUG -> logger.isDebugEnabled();
                case INFO -> logger.isInfoEnabled();
                case WARN -> logger.isWarnEnabled();
                case ERROR -> logger.isErrorEnabled();
            };
        }

        @Override
        public void write(LogLevel level, StructuredEvent event) {
            Marker marker = MarkerFactory.getDetachedMarker(event.type().value());
            event.tags().forEach(tag -> marker.add(MarkerFactory.getDetachedMarker(tag)));
            if (event.isSensitive()) {
                marker.add(MarkerFactory.getDetachedMarker(SENSITIVE_MARKER));
            }

            LoggingEventBuilder builder =
                    logger.atLevel(toSlf4jLevel(level))
                            .addMarker(marker)
                            .setMessage(event.message());
            event.arguments().forEach(builder::addArgument);
            builder.addArgument(event);
            if (event.throwable() != null) {
                builder.setCause(event.throwable());
            }
            builder.log();
        }

        private static org.slf4j.event.Level toSlf4jLevel(LogLevel level) {
            return switch (level) {
                case TRACE -> org.slf4j.event.Level.TRACE;
                case DEBUG -> org.slf4j.event.Level.DEBUG;
                case INFO -> org.slf4j.event.Level.INFO;
                case WARN -> org.slf4j.event.Level.WARN;
                case ERROR -> org.slf4j.event.Level.ERROR;
            };
        }
    }
}
