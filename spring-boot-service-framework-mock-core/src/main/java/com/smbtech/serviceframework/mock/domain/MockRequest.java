package com.smbtech.serviceframework.mock.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable mock request data.
 *
 * @param key key value
 * @param method method value
 * @param path path value
 * @param headers headers value
 * @param queryParams query params value
 * @param body body value
 * @param attributes attributes value
 */
public record MockRequest(
        String key,
        String method,
        String path,
        Map<String, List<String>> headers,
        Map<String, List<String>> queryParams,
        byte[] body,
        Map<String, Object> attributes) {
    /** Creates and validates the record components. */
    public MockRequest {
        key = Objects.requireNonNullElse(key, "").trim();
        method = Objects.requireNonNullElse(method, "").trim();
        path = Objects.requireNonNullElse(path, "").trim();
        headers = copyMultiValueMap(headers);
        queryParams = copyMultiValueMap(queryParams);
        body = body == null ? new byte[0] : body.clone();
        attributes =
                Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(attributes, Map.of())));
    }

    /**
     * Creates a mock request instance.
     *
     * @param key key value
     */
    public MockRequest(String key) {
        this(key, "", "", Map.of(), Map.of(), new byte[0], Map.of());
    }

    /**
     * Performs the body operation.
     *
     * @return body result
     */
    public byte[] body() {
        return body.clone();
    }

    /**
     * Reports whether key.
     *
     * @return has key result
     */
    public boolean hasKey() {
        return !key.isBlank();
    }

    private static Map<String, List<String>> copyMultiValueMap(Map<String, List<String>> values) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        Objects.requireNonNullElse(values, Map.<String, List<String>>of())
                .forEach(
                        (key, value) ->
                                copy.put(
                                        Objects.requireNonNullElse(key, ""),
                                        List.copyOf(Objects.requireNonNullElse(value, List.of()))));
        return Map.copyOf(copy);
    }
}
