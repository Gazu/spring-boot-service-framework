package com.smbtech.serviceframework.httpclient.exception;

/** Provides circuit breaker open exception behavior. */
public class CircuitBreakerOpenException extends RuntimeException {

    /**
     * Creates a circuit breaker open exception instance.
     *
     * @param clientName client name value
     */
    public CircuitBreakerOpenException(String clientName) {
        super("Circuit breaker is open for HTTP client: " + clientName);
    }
}
