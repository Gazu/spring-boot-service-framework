package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import java.util.Map;

final class PropertiesHttpClientDefinitionSource implements HttpClientDefinitionSource {

    private final RestClientProperties properties;
    private final RestClientPropertiesMapper mapper;

    /**
     * Creates a properties http client definition source instance.
     *
     * @param properties properties value
     * @param mapper mapper value
     */
    PropertiesHttpClientDefinitionSource(
            RestClientProperties properties, RestClientPropertiesMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public Map<String, HttpClientDefinition> loadDefinitions() {
        return mapper.map(properties);
    }
}
