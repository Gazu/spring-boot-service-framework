package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.port.in.HttpClientCatalog;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultRestClientRegistry implements RestClientRegistry {

    private final HttpClientCatalog catalog;
    private final ConfiguredRestClientFactory factory;
    private final Map<String, RestClient> cache = new ConcurrentHashMap<>();

    public DefaultRestClientRegistry(
            HttpClientCatalog catalog,
            ConfiguredRestClientFactory factory
    ) {
        this.catalog = catalog;
        this.factory = factory;
    }

    @Override
    public RestClient get(String name) {
        return cache.computeIfAbsent(name, key -> factory.create(catalog.requireByName(key)));
    }

    @Override
    public Set<String> names() {
        return catalog.names();
    }

    @Override
    public Map<String, RestClient> all() {
        catalog.names().forEach(this::get);
        return Map.copyOf(cache);
    }
}
