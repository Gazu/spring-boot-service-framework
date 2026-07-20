package com.smbtech.serviceframework.logging.port.out;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;

/** Outbound port implemented by a concrete logging technology. */
public interface LogEventSink {

    /**
     * Reports whether the underlying logging technology accepts the event.
     *
     * @param level requested log level
     * @param eventType event classification
     * @return {@code true} when enabled
     */
    boolean isEnabled(LogLevel level, EventType eventType);

    /**
     * Writes an event to the underlying logging technology.
     *
     * @param level log level
     * @param event event to write
     */
    void write(LogLevel level, StructuredEvent event);
}
