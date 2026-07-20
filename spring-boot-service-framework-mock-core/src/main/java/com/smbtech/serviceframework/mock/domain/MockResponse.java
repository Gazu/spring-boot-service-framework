package com.smbtech.serviceframework.mock.domain;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable mock response data.
 *
 * @param status status value
 * @param headers headers value
 * @param body body value
 * @param delay delay value
 * @param metadata metadata value
 */
public record MockResponse(
        int status,
        Map<String, List<String>> headers,
        byte[] body,
        Duration delay,
        Map<String, Object> metadata) {
    /** Creates and validates the record components. */
    public MockResponse {
        status = status <= 0 ? 200 : status;
        headers = copyMultiValueMap(headers);
        body = body == null ? new byte[0] : body.clone();
        delay = Objects.requireNonNullElse(delay, Duration.ZERO);
        metadata = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(metadata, Map.of())));
    }

    /**
     * Creates a mock response instance.
     *
     * @param status status value
     * @param headers headers value
     * @param body body value
     */
    public MockResponse(int status, Map<String, List<String>> headers, byte[] body) {
        this(status, headers, body, Duration.ZERO, Map.of());
    }

    /**
     * Performs the ok operation.
     *
     * @param body body value
     * @return ok result
     */
    public static MockResponse ok(byte[] body) {
        return new MockResponse(200, Map.of(), body);
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
     * Reports whether body.
     *
     * @return has body result
     */
    public boolean hasBody() {
        return body.length > 0;
    }

    /**
     * Reports whether delay.
     *
     * @return has delay result
     */
    public boolean hasDelay() {
        return !delay.isZero() && !delay.isNegative();
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
