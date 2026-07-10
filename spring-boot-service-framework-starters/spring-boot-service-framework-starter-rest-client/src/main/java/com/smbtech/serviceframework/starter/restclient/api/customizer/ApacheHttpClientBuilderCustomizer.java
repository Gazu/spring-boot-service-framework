package com.smbtech.serviceframework.starter.restclient.api.customizer;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

@FunctionalInterface
public interface ApacheHttpClientBuilderCustomizer {

    void customize(HttpClientDefinition definition, HttpClientBuilder builder);
}
