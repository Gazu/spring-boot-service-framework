package com.smbtech.serviceframework.logging.port.in;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.out.LogEventSink;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Inbound port used by application code to emit structured events. */
public interface StructuredLogger {

    /**
     * Creates the default logger backed by the supplied output port.
     *
     * @param sink output sink
     * @param production whether production-only restrictions apply
     * @return default structured logger
     */
    static StructuredLogger create(LogEventSink sink, boolean production) {
        return new DefaultStructuredLogger(sink, () -> production);
    }

    /**
     * Creates the default logger with a dynamically evaluated environment policy.
     *
     * @param sink output sink
     * @param production supplier indicating whether production restrictions apply
     * @return default structured logger
     */
    static StructuredLogger create(LogEventSink sink, BooleanSupplier production) {
        return new DefaultStructuredLogger(sink, production);
    }

    /**
     * Reports whether a level and event type are enabled.
     *
     * @param level requested log level
     * @param eventType event classification
     * @return {@code true} when the event would be accepted
     */
    boolean isEnabled(LogLevel level, EventType eventType);

    /**
     * Emits a structured event at the requested level.
     *
     * @param level log level
     * @param event event to emit
     */
    void log(LogLevel level, StructuredEvent event);

    /**
     * Emits a trace event.
     *
     * @param event event to emit
     */
    default void trace(StructuredEvent event) {
        log(LogLevel.TRACE, event);
    }

    /**
     * Builds and emits a trace application event.
     *
     * @param customizer event builder customizer
     */
    default void trace(Consumer<StructuredEvent.Builder> customizer) {
        log(LogLevel.TRACE, buildEvent(EventType.APPLICATION, customizer));
    }

    /**
     * Emits a debug event.
     *
     * @param event event to emit
     */
    default void debug(StructuredEvent event) {
        log(LogLevel.DEBUG, event);
    }

    /**
     * Builds and emits a debug application event.
     *
     * @param customizer event builder customizer
     */
    default void debug(Consumer<StructuredEvent.Builder> customizer) {
        log(LogLevel.DEBUG, buildEvent(EventType.APPLICATION, customizer));
    }

    /**
     * Emits an informational event.
     *
     * @param event event to emit
     */
    default void info(StructuredEvent event) {
        log(LogLevel.INFO, event);
    }

    /**
     * Builds and emits an informational application event.
     *
     * @param customizer event builder customizer
     */
    default void info(Consumer<StructuredEvent.Builder> customizer) {
        log(LogLevel.INFO, buildEvent(EventType.APPLICATION, customizer));
    }

    /**
     * Emits a warning event.
     *
     * @param event event to emit
     */
    default void warn(StructuredEvent event) {
        log(LogLevel.WARN, event);
    }

    /**
     * Builds and emits a warning application event.
     *
     * @param customizer event builder customizer
     */
    default void warn(Consumer<StructuredEvent.Builder> customizer) {
        log(LogLevel.WARN, buildEvent(EventType.APPLICATION, customizer));
    }

    /**
     * Emits an error event.
     *
     * @param event event to emit
     */
    default void error(StructuredEvent event) {
        log(LogLevel.ERROR, event);
    }

    /**
     * Builds and emits an error application event.
     *
     * @param customizer event builder customizer
     */
    default void error(Consumer<StructuredEvent.Builder> customizer) {
        log(LogLevel.ERROR, buildEvent(EventType.APPLICATION, customizer));
    }

    private static StructuredEvent buildEvent(
            EventType type, Consumer<StructuredEvent.Builder> customizer) {
        Objects.requireNonNull(customizer, "customizer must not be null");

        StructuredEvent.Builder builder = StructuredEvent.builder(type);
        customizer.accept(builder);
        return builder.build();
    }
}
