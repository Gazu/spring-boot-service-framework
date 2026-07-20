package com.smbtech.serviceframework.logging.port.in;

/** Creates a structured logger whose category represents the calling component. */
@FunctionalInterface
public interface StructuredLoggerFactory {

    /**
     * Creates or retrieves a logger for a source type.
     *
     * @param source source type used as the logger category
     * @return structured logger
     */
    StructuredLogger get(Class<?> source);
}
