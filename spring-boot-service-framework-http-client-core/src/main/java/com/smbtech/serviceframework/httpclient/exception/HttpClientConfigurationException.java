package com.smbtech.serviceframework.httpclient.exception;

public class HttpClientConfigurationException extends RuntimeException {

    public HttpClientConfigurationException(String message) {
        super(message);
    }

    public HttpClientConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
