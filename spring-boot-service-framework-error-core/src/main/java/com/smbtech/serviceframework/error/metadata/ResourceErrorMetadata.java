package com.smbtech.serviceframework.error.metadata;

import java.util.Map;

/**
 * Public logical resource metadata without a resource identifier.
 *
 * @param type public resource type
 */
public record ResourceErrorMetadata(String type) {

    /** Creates and validates the record components. */
    public ResourceErrorMetadata {
        type = MetadataValues.requireText(type, "resource type");
    }

    /**
     * Returns this namespace as immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        return Map.of(StandardErrorMetadataKeys.Resource.TYPE, type);
    }
}
