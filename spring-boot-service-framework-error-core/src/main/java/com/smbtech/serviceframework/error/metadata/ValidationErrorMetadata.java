package com.smbtech.serviceframework.error.metadata;

import java.util.Map;

/**
 * Describes the public validation failure type.
 *
 * @param type stable validation type, such as {@code bean_validation} or {@code malformed_json}
 */
public record ValidationErrorMetadata(String type) {

    /** Creates and validates the record components. */
    public ValidationErrorMetadata {
        type = MetadataValues.requireText(type, "validation type");
    }

    /**
     * Returns this namespace as immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        return Map.of(StandardErrorMetadataKeys.Validation.TYPE, type);
    }
}
