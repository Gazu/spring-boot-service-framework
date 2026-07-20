package com.smbtech.serviceframework.error.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public HTTP negotiation and method metadata.
 *
 * @param method requested HTTP method
 * @param allowedMethods allowed HTTP methods
 * @param contentType request content type
 * @param supportedMediaTypes supported request media types
 * @param acceptableMediaTypes producible response media types
 */
public record HttpErrorMetadata(
        String method,
        List<String> allowedMethods,
        String contentType,
        List<String> supportedMediaTypes,
        List<String> acceptableMediaTypes) {

    /** Creates and validates the record components. */
    public HttpErrorMetadata {
        method = MetadataValues.optionalUppercase(method);
        allowedMethods = MetadataValues.sortedUppercaseTexts(allowedMethods, "allowed HTTP method");
        contentType = MetadataValues.optionalText(contentType);
        supportedMediaTypes =
                MetadataValues.sortedTexts(supportedMediaTypes, "supported media type");
        acceptableMediaTypes =
                MetadataValues.sortedTexts(acceptableMediaTypes, "acceptable media type");
        if (method.isEmpty()
                && allowedMethods.isEmpty()
                && contentType.isEmpty()
                && supportedMediaTypes.isEmpty()
                && acceptableMediaTypes.isEmpty()) {
            throw new IllegalArgumentException("HTTP metadata must contain at least one value");
        }
    }

    /**
     * Returns this namespace as immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        putIfPresent(values, StandardErrorMetadataKeys.Http.METHOD, method);
        putIfNotEmpty(values, StandardErrorMetadataKeys.Http.ALLOWED_METHODS, allowedMethods);
        putIfPresent(values, StandardErrorMetadataKeys.Http.CONTENT_TYPE, contentType);
        putIfNotEmpty(
                values, StandardErrorMetadataKeys.Http.SUPPORTED_MEDIA_TYPES, supportedMediaTypes);
        putIfNotEmpty(
                values,
                StandardErrorMetadataKeys.Http.ACCEPTABLE_MEDIA_TYPES,
                acceptableMediaTypes);
        return Collections.unmodifiableMap(values);
    }

    private static void putIfPresent(Map<String, Object> values, String key, String value) {
        if (!value.isEmpty()) {
            values.put(key, value);
        }
    }

    private static void putIfNotEmpty(Map<String, Object> values, String key, List<String> value) {
        if (!value.isEmpty()) {
            values.put(key, value);
        }
    }
}
