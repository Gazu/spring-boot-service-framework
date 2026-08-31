package com.smbtech.serviceframework.httpclient.port.in;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.exception.HttpClientConfigurationException;

/** Default validation policy hidden behind {@link HttpClientDefinitionValidator}. */
final class DefaultHttpClientDefinitionValidator implements HttpClientDefinitionValidator {
    /** Creates a default http client definition validator instance. */
    DefaultHttpClientDefinitionValidator() {}

    @Override
    public void validate(HttpClientDefinition definition) {
        if (definition.name().isBlank()) {
            throw new HttpClientConfigurationException("HTTP client name is required");
        }
        if (definition.beanName().isBlank()) {
            throw new HttpClientConfigurationException(
                    "HTTP client beanName is required for: " + definition.name());
        }
        if (definition.baseUrl() == null) {
            throw new HttpClientConfigurationException(
                    "baseUrl is required for HTTP client: " + definition.name());
        }
        validateAuthentication(definition);
    }

    private void validateAuthentication(HttpClientDefinition definition) {
        if (definition.authenticationType() == AuthenticationType.BASIC_AUTH
                && !definition.basicAuthentication().isConfigured()) {
            throw new HttpClientConfigurationException(
                    "basicAuthentication.username and basicAuthentication.password are required for HTTP client: "
                            + definition.name());
        }

        if ((definition.authenticationType() == AuthenticationType.CLIENT_CREDENTIALS
                        || definition.authenticationType() == AuthenticationType.JWT_BEARER)
                && definition.tokenRequestId().isBlank()) {
            throw new HttpClientConfigurationException(
                    "tokenRequestId is required for HTTP client: " + definition.name());
        }
    }
}
