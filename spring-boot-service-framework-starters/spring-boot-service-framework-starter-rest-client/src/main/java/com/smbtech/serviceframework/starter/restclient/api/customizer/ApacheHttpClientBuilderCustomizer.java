package com.smbtech.serviceframework.starter.restclient.api.customizer;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

/** Defines the apache http client builder customizer contract. */
@FunctionalInterface
public interface ApacheHttpClientBuilderCustomizer {

    /**
     * Performs the customize operation.
     *
     * @param definition definition value
     * @param builder builder value
     */
    void customize(HttpClientDefinition definition, HttpClientBuilder builder);
}
