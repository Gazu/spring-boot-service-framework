package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication;

import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.KeyStorePropertiesMapper;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.Map;
import java.util.Optional;

/** Provides properties key store definition source behavior. */
public final class PropertiesKeyStoreDefinitionSource implements KeyStoreDefinitionSource {

    private final Map<String, KeyStoreDefinition> definitions;

    /**
     * Creates a properties key store definition source instance.
     *
     * @param properties properties value
     * @param mapper mapper value
     */
    public PropertiesKeyStoreDefinitionSource(
            RestClientProperties properties, KeyStorePropertiesMapper mapper) {
        this.definitions = Map.copyOf(mapper.map(properties));
    }

    @Override
    public Optional<KeyStoreDefinition> findById(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    @Override
    public Map<String, KeyStoreDefinition> all() {
        return definitions;
    }
}
