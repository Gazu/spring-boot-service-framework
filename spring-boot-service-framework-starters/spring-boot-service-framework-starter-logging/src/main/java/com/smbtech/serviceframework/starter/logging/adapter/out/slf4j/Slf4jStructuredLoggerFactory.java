package com.smbtech.serviceframework.starter.logging.adapter.out.slf4j;

import com.smbtech.serviceframework.logging.application.StructuredLoggingService;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Creates core logging services backed by class-scoped SLF4J loggers.
 */
public final class Slf4jStructuredLoggerFactory implements StructuredLoggerFactory {
    private final boolean production;

    public Slf4jStructuredLoggerFactory(boolean production) {
        this.production = production;
    }

    @Override
    public StructuredLogger get(Class<?> source) {
        return new StructuredLoggingService(
                new Slf4jLogEventSink(LoggerFactory.getLogger(source)),
                production
        );
    }
}
