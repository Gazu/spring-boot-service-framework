package com.smbtech.serviceframework.starter.restclient.api;

import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import java.util.Objects;

/**
 * Raised when a downstream HTTP error body cannot be decoded into the requested type.
 *
 * @serial exclude
 */
public class HttpErrorBodyDecodingException extends RuntimeException {

    private final HttpErrorResponse error;

    /**
     * Creates a http error body decoding exception instance.
     *
     * @param error error value
     * @param targetType target type value
     * @param cause cause value
     */
    public HttpErrorBodyDecodingException(
            HttpErrorResponse error, String targetType, Throwable cause) {
        super(message(error, targetType), cause);
        this.error = Objects.requireNonNull(error, "error must not be null");
    }

    /**
     * Creates a http error body decoding exception instance.
     *
     * @param error error value
     * @param message message value
     */
    public HttpErrorBodyDecodingException(HttpErrorResponse error, String message) {
        super(message(error, message));
        this.error = Objects.requireNonNull(error, "error must not be null");
    }

    /**
     * Performs the error operation.
     *
     * @return error result
     */
    public HttpErrorResponse error() {
        return error;
    }

    private static String message(HttpErrorResponse error, String detail) {
        HttpErrorResponse safeError = Objects.requireNonNull(error, "error must not be null");
        return "Could not decode HTTP error response body"
                + " client="
                + safeError.clientName()
                + " method="
                + safeError.method()
                + " uri="
                + safeError.uri()
                + " status="
                + safeError.statusCode()
                + " contentType="
                + safeError.contentType()
                + " target="
                + Objects.requireNonNullElse(detail, "");
    }
}
