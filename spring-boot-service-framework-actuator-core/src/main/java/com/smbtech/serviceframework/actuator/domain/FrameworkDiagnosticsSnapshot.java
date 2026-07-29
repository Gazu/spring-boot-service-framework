package com.smbtech.serviceframework.actuator.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Carries an immutable, point-in-time framework diagnostics snapshot.
 *
 * @param capturedAt snapshot capture time
 * @param components component results keyed by stable component name
 */
public record FrameworkDiagnosticsSnapshot(
        Instant capturedAt, Map<String, ComponentHealth> components) {

    /** Creates and validates a diagnostics snapshot. */
    public FrameworkDiagnosticsSnapshot {
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
        components = copyComponents(components);
    }

    /**
     * Returns the aggregate status using deterministic worst-status semantics.
     *
     * @return aggregate status, or {@link ComponentStatus#UNKNOWN} when empty
     */
    public ComponentStatus status() {
        ComponentStatus aggregate = ComponentStatus.UNKNOWN;
        boolean found = false;
        for (ComponentHealth component : components.values()) {
            aggregate =
                    found
                            ? ComponentStatus.worst(aggregate, component.status())
                            : component.status();
            found = true;
        }
        return found ? aggregate : ComponentStatus.UNKNOWN;
    }

    /**
     * Finds a component by its normalized name.
     *
     * @param name component name
     * @return matching component, if present
     */
    public Optional<ComponentHealth> component(String name) {
        return Optional.ofNullable(components.get(Objects.requireNonNull(name, "name").trim()));
    }

    /**
     * Reports whether the snapshot contains no component results.
     *
     * @return {@code true} when no component results are present
     */
    public boolean isEmpty() {
        return components.isEmpty();
    }

    private static Map<String, ComponentHealth> copyComponents(
            Map<String, ComponentHealth> values) {
        Map<String, ComponentHealth> sorted = new TreeMap<>();
        Objects.requireNonNull(values, "components")
                .forEach(
                        (key, component) -> {
                            String normalizedKey =
                                    Objects.requireNonNull(key, "component key").trim();
                            if (normalizedKey.isEmpty()) {
                                throw new IllegalArgumentException(
                                        "Component keys must not be blank");
                            }
                            ComponentHealth required =
                                    Objects.requireNonNull(component, "component");
                            if (!normalizedKey.equals(required.name())) {
                                throw new IllegalArgumentException(
                                        "Component key must match the component name");
                            }
                            if (sorted.put(normalizedKey, required) != null) {
                                throw new IllegalArgumentException(
                                        "Component names must be unique after normalization");
                            }
                        });
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }
}
