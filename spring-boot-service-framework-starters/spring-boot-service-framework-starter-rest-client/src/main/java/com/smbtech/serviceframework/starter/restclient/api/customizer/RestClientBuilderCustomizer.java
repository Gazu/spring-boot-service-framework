package com.smbtech.serviceframework.starter.restclient.api.customizer;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.springframework.web.client.RestClient;

/** Defines the rest client builder customizer contract. */
@FunctionalInterface
public interface RestClientBuilderCustomizer {

    /**
     * Performs the customize operation.
     *
     * @param definition definition value
     * @param builder builder value
     */
    void customize(HttpClientDefinition definition, RestClient.Builder builder);
}
