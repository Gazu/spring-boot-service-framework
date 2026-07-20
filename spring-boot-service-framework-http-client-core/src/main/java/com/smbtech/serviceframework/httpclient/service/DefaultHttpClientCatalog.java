package com.smbtech.serviceframework.httpclient.service;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.exception.HttpClientConfigurationException;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientCatalog;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientDefinitionValidator;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Provides default http client catalog behavior. */
public final class DefaultHttpClientCatalog implements HttpClientCatalog {

    private final Map<String, HttpClientDefinition> definitions;

    /**
     * Creates a default http client catalog instance.
     *
     * @param source source value
     * @param validator validator value
     */
    public DefaultHttpClientCatalog(
            HttpClientDefinitionSource source, HttpClientDefinitionValidator validator) {
        Map<String, HttpClientDefinition> loaded = new LinkedHashMap<>(source.loadDefinitions());
        loaded.values().forEach(validator::validate);
        this.definitions = Collections.unmodifiableMap(loaded);
    }

    @Override
    public Optional<HttpClientDefinition> findByName(String name) {
        return Optional.ofNullable(definitions.get(name));
    }

    @Override
    public HttpClientDefinition requireByName(String name) {
        return findByName(name)
                .orElseThrow(
                        () ->
                                new HttpClientConfigurationException(
                                        "HTTP client not configured: " + name));
    }

    @Override
    public Set<String> names() {
        return definitions.keySet();
    }

    @Override
    public Map<String, HttpClientDefinition> all() {
        return definitions;
    }
}
