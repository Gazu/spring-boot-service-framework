package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;

public record CircuitBreakerPolicy(
        boolean enabled,
        int failureThreshold,
        Duration openDuration
) {
    public static CircuitBreakerPolicy disabled() {
        return new CircuitBreakerPolicy(false, 3, Duration.ofSeconds(30));
    }

    public CircuitBreakerPolicy {
        failureThreshold = failureThreshold <= 0 ? 3 : failureThreshold;
        openDuration = openDuration == null || openDuration.isZero() || openDuration.isNegative()
                ? Duration.ofSeconds(30)
                : openDuration;
    }
}
