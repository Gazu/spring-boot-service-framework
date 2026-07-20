package com.smbtech.serviceframework.httpclient.exception;

/** Raised when an HTTP client cannot obtain or validate authentication material. */
public class HttpClientAuthenticationException extends RuntimeException {

    /**
     * Creates a http client authentication exception instance.
     *
     * @param message message value
     */
    public HttpClientAuthenticationException(String message) {
        super(message);
    }

    /**
     * Creates a http client authentication exception instance.
     *
     * @param message message value
     * @param cause cause value
     */
    public HttpClientAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
