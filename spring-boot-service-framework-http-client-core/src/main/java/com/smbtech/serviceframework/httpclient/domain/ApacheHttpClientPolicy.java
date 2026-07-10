package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.Objects;

public record ApacheHttpClientPolicy(
        boolean hostnameVerificationEnabled,
        Duration validateAfterInactivity,
        Duration connectionTimeToLive,
        SslPolicy ssl
) {
    public static ApacheHttpClientPolicy defaults() {
        return new ApacheHttpClientPolicy(
                true,
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                SslPolicy.disabled()
        );
    }

    public ApacheHttpClientPolicy {
        validateAfterInactivity = positiveOrDefault(validateAfterInactivity, Duration.ofSeconds(5));
        connectionTimeToLive = positiveOrDefault(connectionTimeToLive, Duration.ofMinutes(5));
        ssl = Objects.requireNonNullElseGet(ssl, SslPolicy::disabled);
    }

    private static Duration positiveOrDefault(Duration value, Duration fallback) {
        return value == null || value.isZero() || value.isNegative() ? fallback : value;
    }
}
