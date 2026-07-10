package com.smbtech.serviceframework.starter.restclient.adapter.out.source;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientPropertiesMapper;

import java.util.Map;

public final class PropertiesHttpClientDefinitionSource implements HttpClientDefinitionSource {

    private final RestClientProperties properties;
    private final RestClientPropertiesMapper mapper;

    public PropertiesHttpClientDefinitionSource(
            RestClientProperties properties,
            RestClientPropertiesMapper mapper
    ) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public Map<String, HttpClientDefinition> loadDefinitions() {
        return mapper.map(properties);
    }
}
