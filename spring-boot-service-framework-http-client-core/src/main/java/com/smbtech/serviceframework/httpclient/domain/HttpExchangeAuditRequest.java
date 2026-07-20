package com.smbtech.serviceframework.httpclient.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable http exchange audit request data.
 *
 * @param method method value
 * @param uri uri value
 * @param headers headers value
 * @param body body value
 */
public record HttpExchangeAuditRequest(
        String method, String uri, Map<String, String> headers, String body) {
    /** Creates and validates the record components. */
    public HttpExchangeAuditRequest {
        method = Objects.requireNonNullElse(method, "");
        uri = Objects.requireNonNullElse(uri, "");
        headers = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(headers, Map.of())));
        body = Objects.requireNonNullElse(body, "");
    }
}
