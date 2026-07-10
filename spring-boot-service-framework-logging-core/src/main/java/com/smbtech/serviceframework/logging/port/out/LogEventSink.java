package com.smbtech.serviceframework.logging.port.out;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;

/**
 * Outbound port implemented by a concrete logging technology.
 */
public interface LogEventSink {

    boolean isEnabled(LogLevel level, EventType eventType);

    void write(LogLevel level, StructuredEvent event);
}
