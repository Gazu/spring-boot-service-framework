package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ClientHttpRequestFactoryCustomizer;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.List;

public final class HttpClientConfigurator {

    private final ApacheHttpClientConfigurator apacheHttpClientConfigurator;
    private final List<ClientHttpRequestFactoryCustomizer> customizers;

    public HttpClientConfigurator(ApacheHttpClientConfigurator apacheHttpClientConfigurator) {
        this(apacheHttpClientConfigurator, List.of());
    }

    public HttpClientConfigurator(
            ApacheHttpClientConfigurator apacheHttpClientConfigurator,
            List<ClientHttpRequestFactoryCustomizer> customizers
    ) {
        this.apacheHttpClientConfigurator = apacheHttpClientConfigurator;
        this.customizers = List.copyOf(customizers);
    }

    public ClientHttpRequestFactory build(HttpClientDefinition definition) {
        ClientHttpRequestFactory requestFactory = definition.clientType() == ClientType.APACHE_HTTP
                ? apacheRequestFactory(definition)
                : simpleRequestFactory(definition);

        customizers.forEach(customizer -> customizer.customize(definition, requestFactory));

        return requestFactory;
    }

    private ClientHttpRequestFactory simpleRequestFactory(HttpClientDefinition definition) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(definition.timeout().connectTimeout());
        requestFactory.setReadTimeout(definition.timeout().responseTimeout());
        return requestFactory;
    }

    private ClientHttpRequestFactory apacheRequestFactory(HttpClientDefinition definition) {
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(apacheHttpClientConfigurator.build(definition));
        requestFactory.setConnectionRequestTimeout(definition.timeout().connectionRequestTimeout());
        requestFactory.setReadTimeout(definition.timeout().responseTimeout());
        return requestFactory;
    }
}
