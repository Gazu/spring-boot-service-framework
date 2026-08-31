package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ApacheHttpClientBuilderCustomizer;
import java.util.List;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;

/** Provides apache HTTP client configurator behavior. */
final class ApacheHttpClientConfigurator {

    private final HttpClientConnectionManagerConfigurator connectionManagerConfigurator;
    private final ConnectionReuseStrategyConfigurator connectionReuseStrategyConfigurator;
    private final KeepAliveStrategyConfigurator keepAliveStrategyConfigurator;
    private final RequestConfigConfigurator requestConfigConfigurator;
    private final List<ApacheHttpClientBuilderCustomizer> customizers;

    /**
     * Creates an Apache HTTP client configurator instance.
     *
     * @param connectionManagerConfigurator connection manager configurator value
     * @param connectionReuseStrategyConfigurator connection reuse strategy configurator value
     * @param keepAliveStrategyConfigurator keep alive strategy configurator value
     * @param requestConfigConfigurator request config configurator value
     */
    public ApacheHttpClientConfigurator(
            HttpClientConnectionManagerConfigurator connectionManagerConfigurator,
            ConnectionReuseStrategyConfigurator connectionReuseStrategyConfigurator,
            KeepAliveStrategyConfigurator keepAliveStrategyConfigurator,
            RequestConfigConfigurator requestConfigConfigurator) {
        this(
                connectionManagerConfigurator,
                connectionReuseStrategyConfigurator,
                keepAliveStrategyConfigurator,
                requestConfigConfigurator,
                List.of());
    }

    /**
     * Creates an Apache HTTP client configurator instance.
     *
     * @param connectionManagerConfigurator connection manager configurator value
     * @param connectionReuseStrategyConfigurator connection reuse strategy configurator value
     * @param keepAliveStrategyConfigurator keep alive strategy configurator value
     * @param requestConfigConfigurator request config configurator value
     * @param customizers customizers value
     */
    public ApacheHttpClientConfigurator(
            HttpClientConnectionManagerConfigurator connectionManagerConfigurator,
            ConnectionReuseStrategyConfigurator connectionReuseStrategyConfigurator,
            KeepAliveStrategyConfigurator keepAliveStrategyConfigurator,
            RequestConfigConfigurator requestConfigConfigurator,
            List<ApacheHttpClientBuilderCustomizer> customizers) {
        this.connectionManagerConfigurator = connectionManagerConfigurator;
        this.connectionReuseStrategyConfigurator = connectionReuseStrategyConfigurator;
        this.keepAliveStrategyConfigurator = keepAliveStrategyConfigurator;
        this.requestConfigConfigurator = requestConfigConfigurator;
        this.customizers = List.copyOf(customizers);
    }

    /**
     * Creates the result.
     *
     * @param definition definition value
     * @return build result
     */
    public HttpClient build(HttpClientDefinition definition) {
        HttpClientBuilder builder =
                HttpClients.custom()
                        .setConnectionManager(connectionManagerConfigurator.build(definition))
                        .setDefaultRequestConfig(requestConfigConfigurator.build(definition))
                        .setConnectionReuseStrategy(
                                connectionReuseStrategyConfigurator.build(definition))
                        .setKeepAliveStrategy(keepAliveStrategyConfigurator.build(definition))
                        .evictExpiredConnections()
                        .evictIdleConnections(
                                ApacheTime.timeValue(definition.pooling().keepAlive()));

        customizers.forEach(customizer -> customizer.customize(definition, builder));

        return builder.build();
    }
}
