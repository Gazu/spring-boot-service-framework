package com.smbtech.serviceframework.logging.port.in;

/**
 * Creates a structured logger whose category represents the calling component.
 */
@FunctionalInterface
public interface StructuredLoggerFactory {

    StructuredLogger get(Class<?> source);
}
