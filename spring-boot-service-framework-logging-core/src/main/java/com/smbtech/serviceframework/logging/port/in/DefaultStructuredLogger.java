package com.smbtech.serviceframework.logging.port.in;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.out.LogEventSink;
import java.util.Objects;
import java.util.function.BooleanSupplier;

final class DefaultStructuredLogger implements StructuredLogger {
    private final LogEventSink sink;
    private final BooleanSupplier production;

    DefaultStructuredLogger(LogEventSink sink, BooleanSupplier production) {
        this.sink = Objects.requireNonNull(sink, "sink must not be null");
        this.production = Objects.requireNonNull(production, "production must not be null");
    }

    @Override
    public boolean isEnabled(LogLevel level, EventType eventType) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        return !isProductionMetric(eventType) && sink.isEnabled(level, eventType);
    }

    @Override
    public void log(LogLevel level, StructuredEvent event) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(event, "event must not be null");
        if (isEnabled(level, event.type())) {
            sink.write(level, event);
        }
    }

    private boolean isProductionMetric(EventType eventType) {
        return production.getAsBoolean() && EventType.METRIC.equals(eventType);
    }
}
