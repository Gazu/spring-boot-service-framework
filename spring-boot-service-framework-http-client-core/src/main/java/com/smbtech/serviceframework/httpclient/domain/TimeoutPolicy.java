package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.Objects;

public record TimeoutPolicy(
        Duration connectTimeout,
        Duration connectionRequestTimeout,
        Duration responseTimeout
) {
    public static TimeoutPolicy defaults() {
        return new TimeoutPolicy(
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                Duration.ofSeconds(15)
        );
    }

    public TimeoutPolicy {
        connectTimeout = positiveOrDefault(connectTimeout, defaultsConnectTimeout());
        connectionRequestTimeout = positiveOrDefault(connectionRequestTimeout, defaultsConnectionRequestTimeout());
        responseTimeout = positiveOrDefault(responseTimeout, defaultsResponseTimeout());
    }

    private static Duration positiveOrDefault(Duration value, Duration fallback) {
        if (Objects.isNull(value) || value.isZero() || value.isNegative()) {
            return fallback;
        }
        return value;
    }

    private static Duration defaultsConnectTimeout() {
        return Duration.ofSeconds(2);
    }

    private static Duration defaultsConnectionRequestTimeout() {
        return Duration.ofSeconds(2);
    }

    private static Duration defaultsResponseTimeout() {
        return Duration.ofSeconds(15);
    }
}
