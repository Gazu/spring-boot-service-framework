package com.smbtech.serviceframework.error.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public security failure metadata.
 *
 * @param reason stable public failure reason
 * @param authenticationScheme authentication scheme, when known
 */
public record SecurityErrorMetadata(String reason, String authenticationScheme) {

    /** Creates and validates the record components. */
    public SecurityErrorMetadata {
        reason = MetadataValues.requireText(reason, "security reason");
        authenticationScheme =
                MetadataValues.optionalText(authenticationScheme)
                        .toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Returns this namespace as immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(StandardErrorMetadataKeys.Security.REASON, reason);
        if (!authenticationScheme.isEmpty()) {
            values.put(
                    StandardErrorMetadataKeys.Security.AUTHENTICATION_SCHEME, authenticationScheme);
        }
        return Collections.unmodifiableMap(values);
    }
}
