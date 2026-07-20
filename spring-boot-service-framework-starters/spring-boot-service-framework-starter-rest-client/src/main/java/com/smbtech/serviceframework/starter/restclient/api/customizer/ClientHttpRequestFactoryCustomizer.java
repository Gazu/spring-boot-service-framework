package com.smbtech.serviceframework.starter.restclient.api.customizer;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.springframework.http.client.ClientHttpRequestFactory;

/** Defines the client http request factory customizer contract. */
@FunctionalInterface
public interface ClientHttpRequestFactoryCustomizer {

    /**
     * Performs the customize operation.
     *
     * @param definition definition value
     * @param requestFactory request factory value
     */
    void customize(HttpClientDefinition definition, ClientHttpRequestFactory requestFactory);
}
