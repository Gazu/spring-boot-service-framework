package com.smbtech.serviceframework.starter.restclient.api.customizer;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.springframework.web.client.RestClient;

@FunctionalInterface
public interface RestClientBuilderCustomizer {

    void customize(HttpClientDefinition definition, RestClient.Builder builder);
}
