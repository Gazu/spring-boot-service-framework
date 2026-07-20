package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Carries immutable apache HTTP client policy data.
 *
 * @param hostnameVerificationEnabled hostname verification enabled value
 * @param validateAfterInactivity validate after inactivity value
 * @param connectionTimeToLive connection time to live value
 * @param ssl ssl value
 */
public record ApacheHttpClientPolicy(
        boolean hostnameVerificationEnabled,
        Duration validateAfterInactivity,
        Duration connectionTimeToLive,
        SslPolicy ssl) {
    /**
     * Performs the defaults operation.
     *
     * @return defaults result
     */
    public static ApacheHttpClientPolicy defaults() {
        return new ApacheHttpClientPolicy(
                true, Duration.ofSeconds(5), Duration.ofMinutes(5), SslPolicy.disabled());
    }

    /** Creates and validates the record components. */
    public ApacheHttpClientPolicy {
        validateAfterInactivity = positiveOrDefault(validateAfterInactivity, Duration.ofSeconds(5));
        connectionTimeToLive = positiveOrDefault(connectionTimeToLive, Duration.ofMinutes(5));
        ssl = Objects.requireNonNullElseGet(ssl, SslPolicy::disabled);
    }

    private static Duration positiveOrDefault(Duration value, Duration fallback) {
        return value == null || value.isZero() || value.isNegative() ? fallback : value;
    }
}
