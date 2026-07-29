package com.smbtech.serviceframework.actuator.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable, non-sensitive information about a framework module.
 *
 * @param name stable module name
 * @param version module version
 * @param attributes bounded non-sensitive attributes
 */
public record FrameworkModuleInfo(String name, String version, Map<String, Object> attributes) {

    /** Creates and validates framework module information. */
    public FrameworkModuleInfo {
        name = requireText(name, "name");
        version = requireText(version, "version");
        attributes =
                ImmutableDiagnosticValues.structuredMap(
                        Objects.requireNonNull(attributes, "attributes"));
    }

    /**
     * Creates module information without additional attributes.
     *
     * @param name module name
     * @param version module version
     * @return module information
     */
    public static FrameworkModuleInfo of(String name, String version) {
        return new FrameworkModuleInfo(name, version, Map.of());
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Module " + field + " must not be blank");
        }
        return normalized;
    }
}
