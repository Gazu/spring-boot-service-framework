package com.smbtech.serviceframework.actuator.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Carries the immutable result of a component diagnostic probe.
 *
 * @param name stable component name
 * @param status component status
 * @param details bounded diagnostic details
 */
public record ComponentHealth(String name, ComponentStatus status, Map<String, Object> details) {

    /** Creates and validates a component health result. */
    public ComponentHealth {
        name = normalizeName(name);
        status = Objects.requireNonNull(status, "status");
        details =
                ImmutableDiagnosticValues.structuredMap(Objects.requireNonNull(details, "details"));
    }

    /**
     * Creates an available component result without details.
     *
     * @param name component name
     * @return available component result
     */
    public static ComponentHealth up(String name) {
        return up(name, Map.of());
    }

    /**
     * Creates an available component result.
     *
     * @param name component name
     * @param details bounded diagnostic details
     * @return available component result
     */
    public static ComponentHealth up(String name, Map<String, Object> details) {
        return new ComponentHealth(name, ComponentStatus.UP, details);
    }

    /**
     * Creates a failed component result without details.
     *
     * @param name component name
     * @return failed component result
     */
    public static ComponentHealth down(String name) {
        return down(name, Map.of());
    }

    /**
     * Creates a failed component result.
     *
     * @param name component name
     * @param details bounded diagnostic details
     * @return failed component result
     */
    public static ComponentHealth down(String name, Map<String, Object> details) {
        return new ComponentHealth(name, ComponentStatus.DOWN, details);
    }

    /**
     * Creates an intentionally unavailable component result without details.
     *
     * @param name component name
     * @return unavailable component result
     */
    public static ComponentHealth outOfService(String name) {
        return outOfService(name, Map.of());
    }

    /**
     * Creates an intentionally unavailable component result.
     *
     * @param name component name
     * @param details bounded diagnostic details
     * @return unavailable component result
     */
    public static ComponentHealth outOfService(String name, Map<String, Object> details) {
        return new ComponentHealth(name, ComponentStatus.OUT_OF_SERVICE, details);
    }

    /**
     * Creates an indeterminate component result without details.
     *
     * @param name component name
     * @return indeterminate component result
     */
    public static ComponentHealth unknown(String name) {
        return unknown(name, Map.of());
    }

    /**
     * Creates an indeterminate component result.
     *
     * @param name component name
     * @param details bounded diagnostic details
     * @return indeterminate component result
     */
    public static ComponentHealth unknown(String name, Map<String, Object> details) {
        return new ComponentHealth(name, ComponentStatus.UNKNOWN, details);
    }

    /**
     * Reports whether the component is available.
     *
     * @return {@code true} when the status is {@link ComponentStatus#UP}
     */
    public boolean isUp() {
        return status == ComponentStatus.UP;
    }

    private static String normalizeName(String value) {
        String normalized = Objects.requireNonNull(value, "name").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Component name must not be blank");
        }
        return normalized;
    }
}
