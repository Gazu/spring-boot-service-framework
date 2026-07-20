package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import java.util.Map;

/** Defines the http client definition source contract. */
public interface HttpClientDefinitionSource {

    /**
     * Loads definitions.
     *
     * @return load definitions result
     */
    Map<String, HttpClientDefinition> loadDefinitions();
}
