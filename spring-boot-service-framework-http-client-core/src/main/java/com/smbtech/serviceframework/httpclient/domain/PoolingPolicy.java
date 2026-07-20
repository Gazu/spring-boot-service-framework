package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Carries immutable pooling policy data.
 *
 * @param connectionReusePolicy connection reuse policy value
 * @param keepAlive keep alive value
 * @param maxConnections max connections value
 * @param maxConnectionsPerRoute max connections per route value
 * @param tcpKeepAlive tcp keep alive value
 */
public record PoolingPolicy(
        ConnectionReusePolicy connectionReusePolicy,
        Duration keepAlive,
        int maxConnections,
        int maxConnectionsPerRoute,
        boolean tcpKeepAlive) {
    /**
     * Performs the defaults operation.
     *
     * @return defaults result
     */
    public static PoolingPolicy defaults() {
        return new PoolingPolicy(
                ConnectionReusePolicy.DEFAULT, Duration.ofSeconds(30), 100, 20, false);
    }

    /** Creates and validates the record components. */
    public PoolingPolicy {
        connectionReusePolicy =
                Objects.requireNonNullElse(connectionReusePolicy, ConnectionReusePolicy.DEFAULT);
        keepAlive =
                keepAlive == null || keepAlive.isNegative() ? Duration.ofSeconds(30) : keepAlive;
        maxConnections = maxConnections > 0 ? maxConnections : 100;
        maxConnectionsPerRoute = maxConnectionsPerRoute > 0 ? maxConnectionsPerRoute : 20;
    }
}
