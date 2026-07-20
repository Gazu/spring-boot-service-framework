package com.smbtech.serviceframework.error.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public downstream dependency metadata. The name must be a logical alias, not a hostname or URL.
 *
 * @param name public dependency alias
 * @param operation public operation name
 * @param failureType stable failure type
 */
public record DependencyErrorMetadata(String name, String operation, String failureType) {

    /** Creates and validates the record components. */
    public DependencyErrorMetadata {
        name = MetadataValues.requireText(name, "dependency name");
        operation = MetadataValues.optionalText(operation);
        failureType = MetadataValues.requireText(failureType, "dependency failure type");
    }

    /**
     * Returns this namespace as immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(StandardErrorMetadataKeys.Dependency.NAME, name);
        if (!operation.isEmpty()) {
            values.put(StandardErrorMetadataKeys.Dependency.OPERATION, operation);
        }
        values.put(StandardErrorMetadataKeys.Dependency.FAILURE_TYPE, failureType);
        return Collections.unmodifiableMap(values);
    }
}
