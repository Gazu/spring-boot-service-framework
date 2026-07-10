package com.smbtech.serviceframework.logging.domain;

/**
 * Framework-independent classification of a structured event.
 *
 * @param value external event type name
 */
public record EventType(String value) {
    public static final EventType APPLICATION = named("APP");
    public static final EventType ERROR = named("ERROR");
    public static final EventType TRACK = named("TRACK");
    public static final EventType SECURITY = named("SECURITY");
    public static final EventType AUDIT = named("AUDIT");
    public static final EventType ACCESS = named("ACCESS");
    public static final EventType METRIC = named("METRIC");

    public EventType {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Event type may not be null or blank");
        }
    }

    public static EventType named(String value) {
        return new EventType(value);
    }
}
