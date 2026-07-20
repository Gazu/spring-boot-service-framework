package com.smbtech.serviceframework.error;

import java.util.Objects;

/**
 * Describes a validation failure associated with a request or payload field.
 *
 * @param fieldName related field name, or an empty string for an object-level violation
 * @param code stable machine-readable validation code
 * @param message public validation message
 */
public record FieldViolation(String fieldName, String code, String message) {

    /** Normalizes field and code values and rejects a missing validation code. */
    public FieldViolation {
        fieldName = Objects.requireNonNullElse(fieldName, "").trim();
        code = requireText(code, "code");
        message = Objects.requireNonNullElse(message, "");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Field violation " + field + " must not be blank");
        }
        return value.trim();
    }
}
