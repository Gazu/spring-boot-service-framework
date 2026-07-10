package com.smbtech.serviceframework.starter.restclient.api;

import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;

import java.util.Objects;

/**
 * Raised when a downstream HTTP error body cannot be decoded into the requested type.
 */
public class HttpErrorBodyDecodingException extends RuntimeException {

    private final HttpErrorResponse error;

    public HttpErrorBodyDecodingException(HttpErrorResponse error, String targetType, Throwable cause) {
        super(message(error, targetType), cause);
        this.error = Objects.requireNonNull(error, "error must not be null");
    }

    public HttpErrorBodyDecodingException(HttpErrorResponse error, String message) {
        super(message(error, message));
        this.error = Objects.requireNonNull(error, "error must not be null");
    }

    public HttpErrorResponse error() {
        return error;
    }

    private static String message(HttpErrorResponse error, String detail) {
        HttpErrorResponse safeError = Objects.requireNonNull(error, "error must not be null");
        return "Could not decode HTTP error response body"
                + " client=" + safeError.clientName()
                + " method=" + safeError.method()
                + " uri=" + safeError.uri()
                + " status=" + safeError.statusCode()
                + " contentType=" + safeError.contentType()
                + " target=" + Objects.requireNonNullElse(detail, "");
    }
}
