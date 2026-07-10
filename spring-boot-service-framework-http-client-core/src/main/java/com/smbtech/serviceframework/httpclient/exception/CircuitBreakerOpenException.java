package com.smbtech.serviceframework.httpclient.exception;

public class CircuitBreakerOpenException extends RuntimeException {

    public CircuitBreakerOpenException(String clientName) {
        super("Circuit breaker is open for HTTP client: " + clientName);
    }
}
