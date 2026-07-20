package com.smbtech.serviceframework.error.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Safe RFC 6750 metadata for public OAuth2 responses.
 *
 * @param error public OAuth2 error code
 * @param errorDescription static public description
 * @param errorUri absolute documentation URI
 * @param scope required scope, never the scopes granted to the token
 */
public record OAuth2ErrorMetadata(
        String error, String errorDescription, String errorUri, String scope) {

    /** OAuth2 error codes supported by the public contract. */
    public static final Set<String> SUPPORTED_ERROR_CODES =
            Set.of("invalid_request", "invalid_token", "insufficient_scope");

    /** Creates and validates the record components. */
    public OAuth2ErrorMetadata {
        error = MetadataValues.requireText(error, "OAuth2 error");
        if (!SUPPORTED_ERROR_CODES.contains(error)) {
            throw new IllegalArgumentException("Unsupported public OAuth2 error: " + error);
        }
        errorDescription = MetadataValues.optionalText(errorDescription);
        errorUri = MetadataValues.optionalAbsoluteUri(errorUri, "OAuth2 error URI");
        scope = MetadataValues.optionalText(scope);
    }

    /**
     * Returns this namespace as immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(StandardErrorMetadataKeys.OAuth2.ERROR, error);
        putIfPresent(values, StandardErrorMetadataKeys.OAuth2.ERROR_DESCRIPTION, errorDescription);
        putIfPresent(values, StandardErrorMetadataKeys.OAuth2.ERROR_URI, errorUri);
        putIfPresent(values, StandardErrorMetadataKeys.OAuth2.SCOPE, scope);
        return Collections.unmodifiableMap(values);
    }

    private static void putIfPresent(Map<String, Object> values, String key, String value) {
        if (!value.isEmpty()) {
            values.put(key, value);
        }
    }
}
