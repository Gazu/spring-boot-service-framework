package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.Set;

/**
 * Carries immutable retry policy data.
 *
 * @param enabled enabled value
 * @param maxAttempts max attempts value
 * @param backoff backoff value
 * @param retryOnServerErrors retry on server errors value
 * @param retryOnExceptions retry on exceptions value
 * @param retryOnStatuses retry on statuses value
 */
public record RetryPolicy(
        boolean enabled,
        int maxAttempts,
        Duration backoff,
        boolean retryOnServerErrors,
        boolean retryOnExceptions,
        Set<Integer> retryOnStatuses) {
    /**
     * Performs the disabled operation.
     *
     * @return disabled result
     */
    public static RetryPolicy disabled() {
        return new RetryPolicy(false, 1, Duration.ZERO, true, true, Set.of());
    }

    /** Creates and validates the record components. */
    public RetryPolicy {
        maxAttempts = maxAttempts <= 0 ? 1 : maxAttempts;
        backoff = backoff == null || backoff.isNegative() ? Duration.ZERO : backoff;
        retryOnStatuses = Set.copyOf(retryOnStatuses == null ? Set.of() : retryOnStatuses);
    }

    /**
     * Performs the should retry status operation.
     *
     * @param statusCode status code value
     * @return should retry status result
     */
    public boolean shouldRetryStatus(int statusCode) {
        return retryOnStatuses.contains(statusCode)
                || (retryOnServerErrors && statusCode >= 500 && statusCode <= 599);
    }
}
