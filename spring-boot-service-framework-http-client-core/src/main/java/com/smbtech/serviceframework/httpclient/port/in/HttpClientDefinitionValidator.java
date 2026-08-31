package com.smbtech.serviceframework.httpclient.port.in;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;

/** Defines the http client definition validator contract. */
public interface HttpClientDefinitionValidator {

    /**
     * Returns the framework's neutral validation policy.
     *
     * @return default HTTP client definition validator
     */
    static HttpClientDefinitionValidator defaultValidator() {
        return new DefaultHttpClientDefinitionValidator();
    }

    /**
     * Performs the validate operation.
     *
     * @param definition definition value
     */
    void validate(HttpClientDefinition definition);
}
