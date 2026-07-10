package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.Set;

public record RetryPolicy(
        boolean enabled,
        int maxAttempts,
        Duration backoff,
        boolean retryOnServerErrors,
        boolean retryOnExceptions,
        Set<Integer> retryOnStatuses
) {
    public static RetryPolicy disabled() {
        return new RetryPolicy(false, 1, Duration.ZERO, true, true, Set.of());
    }

    public RetryPolicy {
        maxAttempts = maxAttempts <= 0 ? 1 : maxAttempts;
        backoff = backoff == null || backoff.isNegative() ? Duration.ZERO : backoff;
        retryOnStatuses = Set.copyOf(retryOnStatuses == null ? Set.of() : retryOnStatuses);
    }

    public boolean shouldRetryStatus(int statusCode) {
        return retryOnStatuses.contains(statusCode) || (retryOnServerErrors && statusCode >= 500 && statusCode <= 599);
    }
}
