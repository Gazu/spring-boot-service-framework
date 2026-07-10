package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ApacheHttpClientBuilderCustomizer;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.util.List;

public final class ApacheHttpClientConfigurator {

    private final HttpClientConnectionManagerConfigurator connectionManagerConfigurator;
    private final ConnectionReuseStrategyConfigurator connectionReuseStrategyConfigurator;
    private final KeepAliveStrategyConfigurator keepAliveStrategyConfigurator;
    private final RequestConfigConfigurator requestConfigConfigurator;
    private final List<ApacheHttpClientBuilderCustomizer> customizers;

    public ApacheHttpClientConfigurator(
            HttpClientConnectionManagerConfigurator connectionManagerConfigurator,
            ConnectionReuseStrategyConfigurator connectionReuseStrategyConfigurator,
            KeepAliveStrategyConfigurator keepAliveStrategyConfigurator,
            RequestConfigConfigurator requestConfigConfigurator
    ) {
        this(
                connectionManagerConfigurator,
                connectionReuseStrategyConfigurator,
                keepAliveStrategyConfigurator,
                requestConfigConfigurator,
                List.of()
        );
    }

    public ApacheHttpClientConfigurator(
            HttpClientConnectionManagerConfigurator connectionManagerConfigurator,
            ConnectionReuseStrategyConfigurator connectionReuseStrategyConfigurator,
            KeepAliveStrategyConfigurator keepAliveStrategyConfigurator,
            RequestConfigConfigurator requestConfigConfigurator,
            List<ApacheHttpClientBuilderCustomizer> customizers
    ) {
        this.connectionManagerConfigurator = connectionManagerConfigurator;
        this.connectionReuseStrategyConfigurator = connectionReuseStrategyConfigurator;
        this.keepAliveStrategyConfigurator = keepAliveStrategyConfigurator;
        this.requestConfigConfigurator = requestConfigConfigurator;
        this.customizers = List.copyOf(customizers);
    }

    public HttpClient build(HttpClientDefinition definition) {
        HttpClientBuilder builder = HttpClients.custom()
                .setConnectionManager(connectionManagerConfigurator.build(definition))
                .setDefaultRequestConfig(requestConfigConfigurator.build(definition))
                .setConnectionReuseStrategy(connectionReuseStrategyConfigurator.build(definition))
                .setKeepAliveStrategy(keepAliveStrategyConfigurator.build(definition))
                .evictExpiredConnections()
                .evictIdleConnections(ApacheTime.timeValue(definition.pooling().keepAlive()));

        customizers.forEach(customizer -> customizer.customize(definition, builder));

        return builder.build();
    }
}
