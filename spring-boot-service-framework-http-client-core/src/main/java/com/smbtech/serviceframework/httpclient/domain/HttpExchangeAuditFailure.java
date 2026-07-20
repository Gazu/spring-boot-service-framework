package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable http exchange audit failure data.
 *
 * @param method method value
 * @param uri uri value
 * @param statusCode status code value
 * @param statusText status text value
 * @param headers headers value
 * @param requestBody request body value
 * @param responseBody response body value
 * @param duration duration value
 * @param exceptionType exception type value
 * @param exceptionMessage exception message value
 * @param throwable throwable value
 */
public record HttpExchangeAuditFailure(
        String method,
        String uri,
        int statusCode,
        String statusText,
        Map<String, String> headers,
        String requestBody,
        String responseBody,
        Duration duration,
        String exceptionType,
        String exceptionMessage,
        Throwable throwable) {
    public static final int STATUS_CODE_UNAVAILABLE = -1;

    /** Creates and validates the record components. */
    public HttpExchangeAuditFailure {
        method = Objects.requireNonNullElse(method, "");
        uri = Objects.requireNonNullElse(uri, "");
        statusText = Objects.requireNonNullElse(statusText, "");
        headers = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(headers, Map.of())));
        requestBody = Objects.requireNonNullElse(requestBody, "");
        responseBody = Objects.requireNonNullElse(responseBody, "");
        duration = Objects.requireNonNullElse(duration, Duration.ZERO);
        exceptionType = Objects.requireNonNullElse(exceptionType, "");
        exceptionMessage = Objects.requireNonNullElse(exceptionMessage, "");
    }

    /**
     * Reports whether status code.
     *
     * @return has status code result
     */
    public boolean hasStatusCode() {
        return statusCode > 0;
    }
}
