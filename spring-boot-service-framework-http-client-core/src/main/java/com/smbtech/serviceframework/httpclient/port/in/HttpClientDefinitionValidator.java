package com.smbtech.serviceframework.httpclient.port.in;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;

/** Defines the http client definition validator contract. */
public interface HttpClientDefinitionValidator {

    /**
     * Performs the validate operation.
     *
     * @param definition definition value
     */
    void validate(HttpClientDefinition definition);
}
