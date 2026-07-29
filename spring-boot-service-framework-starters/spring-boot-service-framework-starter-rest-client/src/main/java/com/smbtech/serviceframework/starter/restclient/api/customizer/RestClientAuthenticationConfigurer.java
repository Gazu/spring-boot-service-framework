package com.smbtech.serviceframework.starter.restclient.api.customizer;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.springframework.web.client.RestClient;

/** Configures a REST client for an application-provided authentication mechanism. */
public interface RestClientAuthenticationConfigurer {

    /**
     * Reports whether this configurer supports an authentication type.
     *
     * @param authenticationType configured authentication type
     * @return {@code true} when this configurer can configure the type
     */
    boolean supports(AuthenticationType authenticationType);

    /**
     * Applies authentication to a REST client builder.
     *
     * @param definition HTTP client definition
     * @param builder REST client builder
     */
    void configure(HttpClientDefinition definition, RestClient.Builder builder);
}
