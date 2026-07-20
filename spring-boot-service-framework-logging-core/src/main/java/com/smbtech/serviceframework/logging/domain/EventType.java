package com.smbtech.serviceframework.logging.domain;

/**
 * Framework-independent classification of a structured event.
 *
 * @param value external event type name
 */
public record EventType(String value) {
    /** General application event. */
    public static final EventType APPLICATION = named("APP");

    /** Failure event. */
    public static final EventType ERROR = named("ERROR");

    /** Business or technical tracking event. */
    public static final EventType TRACK = named("TRACK");

    /** Security-relevant event. */
    public static final EventType SECURITY = named("SECURITY");

    /** Audit event. */
    public static final EventType AUDIT = named("AUDIT");

    /** Access event. */
    public static final EventType ACCESS = named("ACCESS");

    /** Metric-like event. */
    public static final EventType METRIC = named("METRIC");

    /**
     * Creates a validated event type.
     *
     * @param value external event type name
     */
    public EventType {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Event type may not be null or blank");
        }
    }

    /**
     * Creates an event type from its external name.
     *
     * @param value external event type name
     * @return event type
     */
    public static EventType named(String value) {
        return new EventType(value);
    }
}
