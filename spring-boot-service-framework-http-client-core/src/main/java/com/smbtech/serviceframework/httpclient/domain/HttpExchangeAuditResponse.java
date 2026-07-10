package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record HttpExchangeAuditResponse(
        String method,
        String uri,
        int statusCode,
        String statusText,
        Map<String, String> headers,
        String body,
        Duration duration
) {
    public HttpExchangeAuditResponse {
        method = Objects.requireNonNullElse(method, "");
        uri = Objects.requireNonNullElse(uri, "");
        statusText = Objects.requireNonNullElse(statusText, "");
        headers = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(headers, Map.of())));
        body = Objects.requireNonNullElse(body, "");
        duration = Objects.requireNonNullElse(duration, Duration.ZERO);
    }
}
