package com.smbtech.serviceframework.starter.logging;

import com.smbtech.serviceframework.logging.application.StructuredLoggingService;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.starter.logging.adapter.out.slf4j.Slf4jLogEventSink;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.LoggerFactory;

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
        return new StructuredLoggingService(
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
}
