package com.smbtech.serviceframework.logging.port.in;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Inbound port used by application code to emit structured events.
 */
public interface StructuredLogger {

    boolean isEnabled(LogLevel level, EventType eventType);

    void log(LogLevel level, StructuredEvent event);

    default void trace(StructuredEvent event) {
        log(LogLevel.TRACE, event);
    }

    default void trace(Consumer<StructuredEvent.Builder> customizer) {
        log(
                LogLevel.TRACE,
                buildEvent(EventType.APPLICATION, customizer)
        );
    }

    default void debug(StructuredEvent event) {
        log(LogLevel.DEBUG, event);
    }

    default void debug(Consumer<StructuredEvent.Builder> customizer) {
        log(
                LogLevel.DEBUG,
                buildEvent(EventType.APPLICATION, customizer)
        );
    }

    default void info(StructuredEvent event) {
        log(LogLevel.INFO, event);
    }

    default void info(Consumer<StructuredEvent.Builder> customizer) {
        log(
                LogLevel.INFO,
                buildEvent(EventType.APPLICATION, customizer)
        );
    }

    default void warn(StructuredEvent event) {
        log(LogLevel.WARN, event);
    }

    default void warn(Consumer<StructuredEvent.Builder> customizer) {
        log(
                LogLevel.WARN,
                buildEvent(EventType.APPLICATION, customizer)
        );
    }

    default void error(StructuredEvent event) {
        log(LogLevel.ERROR, event);
    }

    default void error(Consumer<StructuredEvent.Builder> customizer) {
        log(
                LogLevel.ERROR,
                buildEvent(EventType.APPLICATION, customizer)
        );
    }

    private static StructuredEvent buildEvent(
            EventType type,
            Consumer<StructuredEvent.Builder> customizer
    ) {
        Objects.requireNonNull(customizer, "customizer must not be null");

        StructuredEvent.Builder builder = StructuredEvent.builder(type);
        customizer.accept(builder);
        return builder.build();
    }
}
