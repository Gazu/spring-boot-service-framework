package com.smbtech.serviceframework.openapi.generator;

import java.util.Objects;

/**
 * Carries immutable open api change data.
 *
 * @param severity severity value
 * @param code code value
 * @param location location value
 * @param message message value
 */
public record OpenApiChange(
        OpenApiChangeSeverity severity, OpenApiChangeCode code, String location, String message) {

    /** Creates and validates the record components. */
    public OpenApiChange {
        severity = Objects.requireNonNull(severity, "severity");
        code = Objects.requireNonNull(code, "code");
        location = requireText(location, "location");
        message = requireText(message, "message");
    }

    private static String requireText(String value, String name) {
        String safeValue = Objects.requireNonNull(value, name).trim();
        if (safeValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return safeValue;
    }
}
