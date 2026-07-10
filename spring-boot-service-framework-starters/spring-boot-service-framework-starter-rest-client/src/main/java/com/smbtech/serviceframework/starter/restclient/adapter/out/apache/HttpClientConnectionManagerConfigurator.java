package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;

public final class HttpClientConnectionManagerConfigurator {

    private final RegistryConfigurator registryConfigurator;
    private final SocketConfigConfigurator socketConfigConfigurator;

    public HttpClientConnectionManagerConfigurator(
            RegistryConfigurator registryConfigurator,
            SocketConfigConfigurator socketConfigConfigurator
    ) {
        this.registryConfigurator = registryConfigurator;
        this.socketConfigConfigurator = socketConfigConfigurator;
    }

    public PoolingHttpClientConnectionManager build(HttpClientDefinition definition) {
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(registryConfigurator.build(definition));

        connectionManager.setMaxTotal(definition.pooling().maxConnections());
        connectionManager.setDefaultMaxPerRoute(definition.pooling().maxConnectionsPerRoute());
        connectionManager.setDefaultSocketConfig(socketConfigConfigurator.build(definition));
        connectionManager.setDefaultConnectionConfig(connectionConfig(definition));
        connectionManager.setValidateAfterInactivity(
                ApacheTime.timeValue(definition.apache().validateAfterInactivity())
        );

        return connectionManager;
    }

    private ConnectionConfig connectionConfig(HttpClientDefinition definition) {
        return ConnectionConfig.custom()
                .setConnectTimeout(ApacheTime.timeout(definition.timeout().connectTimeout()))
                .setSocketTimeout(ApacheTime.timeout(definition.timeout().responseTimeout()))
                .setValidateAfterInactivity(ApacheTime.timeValue(definition.apache().validateAfterInactivity()))
                .setTimeToLive(ApacheTime.timeValue(definition.apache().connectionTimeToLive()))
                .build();
    }
}
