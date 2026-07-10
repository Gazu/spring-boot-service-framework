package com.smbtech.serviceframework.httpclient.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
        boolean bodyTruncated
) {
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

    public HttpErrorResponse(
            String clientName,
            String method,
            String uri,
            int statusCode,
            String reasonPhrase,
            HttpErrorCategory category,
            Map<String, String> headers,
            String body
    ) {
        this(clientName, method, uri, statusCode, reasonPhrase, category, headers, body, "", "", false);
    }

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
