package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;

/**
 * Carries immutable circuit breaker policy data.
 *
 * @param enabled enabled value
 * @param failureThreshold failure threshold value
 * @param openDuration open duration value
 */
public record CircuitBreakerPolicy(boolean enabled, int failureThreshold, Duration openDuration) {
    /**
     * Performs the disabled operation.
     *
     * @return disabled result
     */
    public static CircuitBreakerPolicy disabled() {
        return new CircuitBreakerPolicy(false, 3, Duration.ofSeconds(30));
    }

    /** Creates and validates the record components. */
    public CircuitBreakerPolicy {
        failureThreshold = failureThreshold <= 0 ? 3 : failureThreshold;
        openDuration =
                openDuration == null || openDuration.isZero() || openDuration.isNegative()
                        ? Duration.ofSeconds(30)
                        : openDuration;
    }
}
