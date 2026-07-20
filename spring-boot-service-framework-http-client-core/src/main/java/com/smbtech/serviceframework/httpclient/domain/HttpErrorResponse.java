package com.smbtech.serviceframework.httpclient.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable http error response data.
 *
 * @param clientName client name value
 * @param method method value
 * @param uri uri value
 * @param statusCode status code value
 * @param reasonPhrase reason phrase value
 * @param category category value
 * @param headers headers value
 * @param body body value
 * @param contentType content type value
 * @param charset charset value
 * @param bodyTruncated body truncated value
 */
public record HttpErrorResponse(
        String clientName,
        String method,
        String uri,
        int statusCode,
        String reasonPhrase,
        HttpErrorCategory category,
        Map<String, String> headers,
        String body,
        String contentType,
        String charset,
        boolean bodyTruncated) {
    /** Creates and validates the record components. */
    public HttpErrorResponse {
        clientName = Objects.requireNonNullElse(clientName, "");
        method = Objects.requireNonNullElse(method, "");
        uri = Objects.requireNonNullElse(uri, "");
        reasonPhrase = Objects.requireNonNullElse(reasonPhrase, "");
        category = Objects.requireNonNullElse(category, HttpErrorCategory.UNKNOWN);
        headers = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(headers, Map.of())));
        body = Objects.requireNonNullElse(body, "");
        contentType = Objects.requireNonNullElse(contentType, "");
        charset = Objects.requireNonNullElse(charset, "");
    }

    /**
     * Creates a http error response instance.
     *
     * @param clientName client name value
     * @param method method value
     * @param uri uri value
     * @param statusCode status code value
     * @param reasonPhrase reason phrase value
     * @param category category value
     * @param headers headers value
     * @param body body value
     */
    public HttpErrorResponse(
            String clientName,
            String method,
            String uri,
            int statusCode,
            String reasonPhrase,
            HttpErrorCategory category,
            Map<String, String> headers,
            String body) {
        this(
                clientName,
                method,
                uri,
                statusCode,
                reasonPhrase,
                category,
                headers,
                body,
                "",
                "",
                false);
    }

    /**
     * Performs the category of operation.
     *
     * @param statusCode status code value
     * @return category of result
     */
    public static HttpErrorCategory categoryOf(int statusCode) {
        if (statusCode >= 400 && statusCode <= 499) {
            return HttpErrorCategory.CLIENT_ERROR;
        }
        if (statusCode >= 500 && statusCode <= 599) {
            return HttpErrorCategory.SERVER_ERROR;
        }
        return HttpErrorCategory.UNKNOWN;
    }
}
