package com.smbtech.serviceframework.error.metadata;

import com.smbtech.serviceframework.error.FieldViolation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Public field violation metadata with an optional request location.
 *
 * @param fieldName related field name
 * @param location request location such as {@code body}, {@code query}, or {@code header}
 * @param code stable validation code
 * @param message public validation message
 */
public record FieldViolationMetadata(
        String fieldName, String location, String code, String message) {

    /**
     * Creates normalized field violation metadata.
     *
     * @param fieldName related field name
     * @param location request location
     * @param code stable validation code
     * @param message public validation message
     */
    public FieldViolationMetadata {
        fieldName = MetadataValues.optionalText(fieldName);
        location = MetadataValues.optionalText(location);
        code = MetadataValues.requireText(code, "violation code");
        message = MetadataValues.optionalText(message);
    }

    /**
     * Creates metadata from an existing framework field violation.
     *
     * @param violation source violation
     * @return field violation metadata
     */
    public static FieldViolationMetadata from(FieldViolation violation) {
        return from(violation, "");
    }

    /**
     * Creates metadata from an existing violation and request location.
     *
     * @param violation source violation
     * @param location request location
     * @return field violation metadata
     */
    public static FieldViolationMetadata from(FieldViolation violation, String location) {
        FieldViolation source = Objects.requireNonNull(violation, "violation must not be null");
        return new FieldViolationMetadata(
                source.fieldName(), location, source.code(), source.message());
    }

    /**
     * Returns this violation as immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(StandardErrorMetadataKeys.Violation.FIELD_NAME, fieldName);
        if (!location.isEmpty()) {
            values.put(StandardErrorMetadataKeys.Violation.LOCATION, location);
        }
        values.put(StandardErrorMetadataKeys.Violation.CODE, code);
        values.put(StandardErrorMetadataKeys.Violation.MESSAGE, message);
        return Collections.unmodifiableMap(values);
    }
}
