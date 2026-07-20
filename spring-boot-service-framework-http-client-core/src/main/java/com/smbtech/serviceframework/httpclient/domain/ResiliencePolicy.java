package com.smbtech.serviceframework.httpclient.domain;

import java.util.Objects;

/**
 * Carries immutable resilience policy data.
 *
 * @param enabled enabled value
 * @param retry retry value
 * @param circuitBreaker circuit breaker value
 */
public record ResiliencePolicy(
        boolean enabled, RetryPolicy retry, CircuitBreakerPolicy circuitBreaker) {
    /**
     * Performs the disabled operation.
     *
     * @return disabled result
     */
    public static ResiliencePolicy disabled() {
        return new ResiliencePolicy(false, RetryPolicy.disabled(), CircuitBreakerPolicy.disabled());
    }

    /** Creates and validates the record components. */
    public ResiliencePolicy {
        retry = Objects.requireNonNullElseGet(retry, RetryPolicy::disabled);
        circuitBreaker =
                Objects.requireNonNullElseGet(circuitBreaker, CircuitBreakerPolicy::disabled);
        enabled = enabled || retry.enabled() || circuitBreaker.enabled();
    }
}
