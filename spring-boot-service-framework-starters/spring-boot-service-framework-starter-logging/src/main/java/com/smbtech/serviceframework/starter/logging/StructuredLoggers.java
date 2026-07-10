package com.smbtech.serviceframework.starter.logging;

import com.smbtech.serviceframework.logging.application.StructuredLoggingService;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.starter.logging.adapter.out.slf4j.Slf4jLogEventSink;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StructuredLoggers {
    private static final AtomicBoolean PRODUCTION = new AtomicBoolean();

    private StructuredLoggers() {
    }

    public static StructuredLogger get(Class<?> source) {
        return new StructuredLoggingService(
                new Slf4jLogEventSink(LoggerFactory.getLogger(source)),
                PRODUCTION::get
        );
    }

    public static StructuredLogger get(MethodHandles.Lookup lookup) {
        return get(lookup.lookupClass());
    }

    public static void setProduction(boolean production) {
        PRODUCTION.set(production);
    }
}
