package com.smbtech.serviceframework.error.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public resource or domain conflict metadata.
 *
 * @param type stable conflict type
 * @param operation public operation name
 */
public record ConflictErrorMetadata(String type, String operation) {

    /** Creates and validates the record components. */
    public ConflictErrorMetadata {
        type = MetadataValues.requireText(type, "conflict type");
        operation = MetadataValues.optionalText(operation);
    }

    /**
     * Returns this namespace as immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(StandardErrorMetadataKeys.Conflict.TYPE, type);
        if (!operation.isEmpty()) {
            values.put(StandardErrorMetadataKeys.Conflict.OPERATION, operation);
        }
        return Collections.unmodifiableMap(values);
    }
}
