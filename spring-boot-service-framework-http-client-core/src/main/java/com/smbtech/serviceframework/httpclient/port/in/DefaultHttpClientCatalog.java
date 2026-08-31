package com.smbtech.serviceframework.httpclient.port.in;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.exception.HttpClientConfigurationException;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Default immutable catalog implementation hidden behind {@link HttpClientCatalog}. */
final class DefaultHttpClientCatalog implements HttpClientCatalog {

    private final Map<String, HttpClientDefinition> definitions;

    /**
     * Creates a default http client catalog instance.
     *
     * @param source source value
     * @param validator validator value
     */
    DefaultHttpClientCatalog(
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
