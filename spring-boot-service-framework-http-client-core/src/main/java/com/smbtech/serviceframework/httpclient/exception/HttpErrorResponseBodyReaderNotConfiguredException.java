package com.smbtech.serviceframework.httpclient.exception;

import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import java.util.Objects;

/**
 * Raised when an HTTP error response body is requested as an object but no reader is available.
 *
 * @serial exclude
 */
public class HttpErrorResponseBodyReaderNotConfiguredException extends RuntimeException {

    private final HttpErrorResponse error;

    /**
     * Creates a http error response body reader not configured exception instance.
     *
     * @param error error value
     */
    public HttpErrorResponseBodyReaderNotConfiguredException(HttpErrorResponse error) {
        super(message(error));
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

    private static String message(HttpErrorResponse error) {
        HttpErrorResponse safeError = Objects.requireNonNull(error, "error must not be null");
        return "HTTP error response body reader is not configured"
                + " client="
                + safeError.clientName()
                + " method="
                + safeError.method()
                + " uri="
                + safeError.uri()
                + " status="
                + safeError.statusCode();
    }
}
