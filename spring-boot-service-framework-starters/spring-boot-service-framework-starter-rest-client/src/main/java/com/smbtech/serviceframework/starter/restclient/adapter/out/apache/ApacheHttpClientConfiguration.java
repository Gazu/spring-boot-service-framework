package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ApacheHttpClientBuilderCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ClientHttpRequestFactoryCustomizer;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

@Configuration(proxyBeanMethods = false)
class ApacheHttpClientConfiguration {

    @Bean("restClientRequestFactoryBuilder")
    Function<HttpClientDefinition, ClientHttpRequestFactory> restClientRequestFactoryBuilder(
            @Qualifier("restClientSslContextBuilder") BiFunction<HttpClientDefinition, SSLContext, SSLContext> sslContextBuilder,
            ObjectProvider<SSLContext> sslContext,
            ObjectProvider<ApacheHttpClientBuilderCustomizer> httpClientCustomizers,
            ObjectProvider<ClientHttpRequestFactoryCustomizer> requestFactoryCustomizers) {
        HostnameVerifierConfigurator hostnameVerifierConfigurator =
                new HostnameVerifierConfigurator();
        SslConnectionSocketFactoryConfigurator sslSocketFactoryConfigurator =
                new SslConnectionSocketFactoryConfigurator(
                        hostnameVerifierConfigurator,
                        sslContextBuilder,
                        sslContext.getIfAvailable());
        RegistryConfigurator registryConfigurator =
                new RegistryConfigurator(sslSocketFactoryConfigurator);
        HttpClientConnectionManagerConfigurator connectionManagerConfigurator =
                new HttpClientConnectionManagerConfigurator(
                        registryConfigurator, new SocketConfigConfigurator());
        ApacheHttpClientConfigurator apacheConfigurator =
                new ApacheHttpClientConfigurator(
                        connectionManagerConfigurator,
                        new ConnectionReuseStrategyConfigurator(),
                        new KeepAliveStrategyConfigurator(),
                        new RequestConfigConfigurator(),
                        orderedList(httpClientCustomizers));
        HttpClientConfigurator configurator =
                new HttpClientConfigurator(
                        apacheConfigurator, orderedList(requestFactoryCustomizers));
        return configurator::build;
    }

    private static <T> List<T> orderedList(ObjectProvider<T> provider) {
        return provider.orderedStream().toList();
    }
}
