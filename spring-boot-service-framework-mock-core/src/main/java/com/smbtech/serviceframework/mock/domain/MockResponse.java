package com.smbtech.serviceframework.mock.domain;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MockResponse(
        int status,
        Map<String, List<String>> headers,
        byte[] body,
        Duration delay,
        Map<String, Object> metadata
) {
    public MockResponse {
        status = status <= 0 ? 200 : status;
        headers = copyMultiValueMap(headers);
        body = body == null ? new byte[0] : body.clone();
        delay = Objects.requireNonNullElse(delay, Duration.ZERO);
        metadata = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(metadata, Map.of())));
    }

    public MockResponse(int status, Map<String, List<String>> headers, byte[] body) {
        this(status, headers, body, Duration.ZERO, Map.of());
    }

    public static MockResponse ok(byte[] body) {
        return new MockResponse(200, Map.of(), body);
    }

    public byte[] body() {
        return body.clone();
    }

    public boolean hasBody() {
        return body.length > 0;
    }

    public boolean hasDelay() {
        return !delay.isZero() && !delay.isNegative();
    }

    private static Map<String, List<String>> copyMultiValueMap(Map<String, List<String>> values) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        Objects.requireNonNullElse(values, Map.<String, List<String>>of())
                .forEach((key, value) -> copy.put(
                        Objects.requireNonNullElse(key, ""),
                        List.copyOf(Objects.requireNonNullElse(value, List.of()))
                ));
        return Map.copyOf(copy);
    }
}
