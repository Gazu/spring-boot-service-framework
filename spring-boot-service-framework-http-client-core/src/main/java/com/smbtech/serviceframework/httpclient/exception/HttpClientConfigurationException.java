package com.smbtech.serviceframework.httpclient.exception;

/** Provides http client configuration exception behavior. */
public class HttpClientConfigurationException extends RuntimeException {

    /**
     * Creates a http client configuration exception instance.
     *
     * @param message message value
     */
    public HttpClientConfigurationException(String message) {
        super(message);
    }

    /**
     * Creates a http client configuration exception instance.
     *
     * @param message message value
     * @param cause cause value
     */
    public HttpClientConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
