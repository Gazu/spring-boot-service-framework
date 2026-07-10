package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.Objects;

public record PoolingPolicy(
        ConnectionReusePolicy connectionReusePolicy,
        Duration keepAlive,
        int maxConnections,
        int maxConnectionsPerRoute,
        boolean tcpKeepAlive
) {
    public static PoolingPolicy defaults() {
        return new PoolingPolicy(
                ConnectionReusePolicy.DEFAULT,
                Duration.ofSeconds(30),
                100,
                20,
                false
        );
    }

    public PoolingPolicy {
        connectionReusePolicy = Objects.requireNonNullElse(connectionReusePolicy, ConnectionReusePolicy.DEFAULT);
        keepAlive = keepAlive == null || keepAlive.isNegative() ? Duration.ofSeconds(30) : keepAlive;
        maxConnections = maxConnections > 0 ? maxConnections : 100;
        maxConnectionsPerRoute = maxConnectionsPerRoute > 0 ? maxConnectionsPerRoute : 20;
    }
}
