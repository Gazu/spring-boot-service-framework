package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.httpclient.domain.ApacheHttpClientPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuditPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.ConnectionReusePolicy;
import com.smbtech.serviceframework.httpclient.domain.ErrorHandlingPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.domain.PoolingPolicy;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.TimeoutPolicy;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.ConnectionReuseStrategyConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HostnameVerifierConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HttpClientConnectionManagerConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.RegistryConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.RequestConfigConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.SocketConfigConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.SslConnectionSocketFactoryConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.SslContextFactory;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.junit.jupiter.api.Test;

class ApacheHttpClientConfiguratorTest {

    @Test
    void requestConfigUsesConfiguredTimeoutsAndKeepAlive() {
        HttpClientDefinition definition = apacheDefinition(ConnectionReusePolicy.DEFAULT, false);

        RequestConfig requestConfig = new RequestConfigConfigurator().build(definition);

        assertThat(requestConfig.getConnectTimeout().toMilliseconds()).isEqualTo(1_000);
        assertThat(requestConfig.getConnectionRequestTimeout().toMilliseconds()).isEqualTo(2_000);
        assertThat(requestConfig.getResponseTimeout().toMilliseconds()).isEqualTo(3_000);
        assertThat(requestConfig.getConnectionKeepAlive().toMilliseconds()).isEqualTo(4_000);
    }

    @Test
    void socketConfigUsesResponseTimeoutAndTcpKeepAlive() {
        SocketConfig keepAliveSocketConfig =
                new SocketConfigConfigurator()
                        .build(apacheDefinition(ConnectionReusePolicy.DEFAULT, true));
        SocketConfig nonKeepAliveSocketConfig =
                new SocketConfigConfigurator()
                        .build(apacheDefinition(ConnectionReusePolicy.DEFAULT, false));

        assertThat(keepAliveSocketConfig.getSoTimeout().toMilliseconds()).isEqualTo(3_000);
        assertThat(keepAliveSocketConfig.isSoKeepAlive()).isTrue();
        assertThat(keepAliveSocketConfig.isTcpNoDelay()).isTrue();
        assertThat(nonKeepAliveSocketConfig.isSoKeepAlive()).isFalse();
    }

    @Test
    void connectionReuseStrategyCanBeForced() {
        BasicHttpRequest request = new BasicHttpRequest("GET", "/");
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);

        assertThat(
                        new ConnectionReuseStrategyConfigurator()
                                .build(apacheDefinition(ConnectionReusePolicy.ALWAYS, false))
                                .keepAlive(request, response, null))
                .isTrue();

        assertThat(
                        new ConnectionReuseStrategyConfigurator()
                                .build(apacheDefinition(ConnectionReusePolicy.NEVER, false))
                                .keepAlive(request, response, null))
                .isFalse();
    }

    @Test
    void hostnameVerifierCanBeDisabledExplicitly() {
        HttpClientDefinition definition = apacheDefinition(ConnectionReusePolicy.DEFAULT, false);

        assertThat(new HostnameVerifierConfigurator().build(definition))
                .isSameAs(NoopHostnameVerifier.INSTANCE);
    }

    @Test
    void connectionManagerUsesPoolingConfiguration() {
        HttpClientDefinition definition = apacheDefinition(ConnectionReusePolicy.DEFAULT, false);
        HttpClientConnectionManagerConfigurator configurator =
                new HttpClientConnectionManagerConfigurator(
                        new RegistryConfigurator(
                                new SslConnectionSocketFactoryConfigurator(
                                        new HostnameVerifierConfigurator(),
                                        new SslContextFactory(),
                                        null)),
                        new SocketConfigConfigurator());

        PoolingHttpClientConnectionManager connectionManager = configurator.build(definition);

        assertThat(connectionManager.getMaxTotal()).isEqualTo(50);
        assertThat(connectionManager.getDefaultMaxPerRoute()).isEqualTo(5);

        connectionManager.close();
    }

    private HttpClientDefinition apacheDefinition(
            ConnectionReusePolicy reusePolicy, boolean tcpKeepAlive) {
        return new HttpClientDefinition(
                "apache",
                null,
                URI.create("https://apache.example"),
                ClientType.APACHE_HTTP,
                AuthenticationType.NO_AUTH,
                new BasicAuthentication("", ""),
                "",
                "",
                new TimeoutPolicy(
                        Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)),
                new PoolingPolicy(reusePolicy, Duration.ofSeconds(4), 50, 5, tcpKeepAlive),
                new ApacheHttpClientPolicy(
                        false, Duration.ofSeconds(6), Duration.ofSeconds(7), null),
                ErrorHandlingPolicy.defaults(),
                ObservabilityPolicy.defaults(),
                ResiliencePolicy.disabled(),
                AuditPolicy.disabled(),
                Map.of());
    }
}
