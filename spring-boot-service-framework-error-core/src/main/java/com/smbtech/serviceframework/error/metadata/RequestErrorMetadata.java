package com.smbtech.serviceframework.error.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Safe public request context. Routes should be templates and must not include query parameters or
 * resource identifiers.
 *
 * @param method HTTP method
 * @param route route template
 * @param operationId stable operation identifier
 */
public record RequestErrorMetadata(String method, String route, String operationId) {

    /** Creates and validates the record components. */
    public RequestErrorMetadata {
        method = MetadataValues.optionalUppercase(method);
        route = MetadataValues.optionalText(route);
        operationId = MetadataValues.optionalText(operationId);
        if (method.isEmpty() && route.isEmpty() && operationId.isEmpty()) {
            throw new IllegalArgumentException("request metadata must contain at least one value");
        }
    }

    /**
     * Returns this namespace as immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        putIfPresent(values, StandardErrorMetadataKeys.Request.METHOD, method);
        putIfPresent(values, StandardErrorMetadataKeys.Request.ROUTE, route);
        putIfPresent(values, StandardErrorMetadataKeys.Request.OPERATION_ID, operationId);
        return Collections.unmodifiableMap(values);
    }

    private static void putIfPresent(Map<String, Object> values, String key, String value) {
        if (!value.isEmpty()) {
            values.put(key, value);
        }
    }
}
