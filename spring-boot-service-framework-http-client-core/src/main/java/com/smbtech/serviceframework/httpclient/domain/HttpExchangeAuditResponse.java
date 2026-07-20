package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable http exchange audit response data.
 *
 * @param method method value
 * @param uri uri value
 * @param statusCode status code value
 * @param statusText status text value
 * @param headers headers value
 * @param body body value
 * @param duration duration value
 */
public record HttpExchangeAuditResponse(
        String method,
        String uri,
        int statusCode,
        String statusText,
        Map<String, String> headers,
        String body,
        Duration duration) {
    /** Creates and validates the record components. */
    public HttpExchangeAuditResponse {
        method = Objects.requireNonNullElse(method, "");
        uri = Objects.requireNonNullElse(uri, "");
        statusText = Objects.requireNonNullElse(statusText, "");
        headers = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(headers, Map.of())));
        body = Objects.requireNonNullElse(body, "");
        duration = Objects.requireNonNullElse(duration, Duration.ZERO);
    }
}
