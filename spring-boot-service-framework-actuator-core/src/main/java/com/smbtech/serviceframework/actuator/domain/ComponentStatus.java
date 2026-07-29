package com.smbtech.serviceframework.actuator.domain;

import java.util.Objects;

/** Represents the framework-neutral status of a diagnostic component. */
public enum ComponentStatus {
    /** The component is available. */
    UP(0),

    /** The component status could not be determined. */
    UNKNOWN(1),

    /** The component is intentionally unavailable. */
    OUT_OF_SERVICE(2),

    /** The component is unavailable because of a failure. */
    DOWN(3);

    private final int severity;

    ComponentStatus(int severity) {
        this.severity = severity;
    }

    /**
     * Returns the status with the greatest operational severity.
     *
     * @param left first status
     * @param right second status
     * @return the most severe status
     */
    public static ComponentStatus worst(ComponentStatus left, ComponentStatus right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return left.severity >= right.severity ? left : right;
    }
}
