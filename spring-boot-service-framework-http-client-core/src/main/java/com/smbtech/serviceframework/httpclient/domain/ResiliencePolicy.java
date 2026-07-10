package com.smbtech.serviceframework.httpclient.domain;

import java.util.Objects;

public record ResiliencePolicy(
        boolean enabled,
        RetryPolicy retry,
        CircuitBreakerPolicy circuitBreaker
) {
    public static ResiliencePolicy disabled() {
        return new ResiliencePolicy(false, RetryPolicy.disabled(), CircuitBreakerPolicy.disabled());
    }

    public ResiliencePolicy {
        retry = Objects.requireNonNullElseGet(retry, RetryPolicy::disabled);
        circuitBreaker = Objects.requireNonNullElseGet(circuitBreaker, CircuitBreakerPolicy::disabled);
        enabled = enabled || retry.enabled() || circuitBreaker.enabled();
    }
}
