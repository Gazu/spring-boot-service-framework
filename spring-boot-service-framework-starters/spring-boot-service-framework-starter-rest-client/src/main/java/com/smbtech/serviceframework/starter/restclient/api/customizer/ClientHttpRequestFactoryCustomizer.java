package com.smbtech.serviceframework.starter.restclient.api.customizer;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.springframework.http.client.ClientHttpRequestFactory;

@FunctionalInterface
public interface ClientHttpRequestFactoryCustomizer {

    void customize(HttpClientDefinition definition, ClientHttpRequestFactory requestFactory);
}
