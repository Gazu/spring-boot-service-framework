package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import java.util.Map;
import java.util.Optional;

final class PropertiesKeyStoreDefinitionSource implements KeyStoreDefinitionSource {

    private final Map<String, KeyStoreDefinition> definitions;

    /**
     * Creates a properties key store definition source instance.
     *
     * @param properties properties value
     * @param mapper mapper value
     */
    PropertiesKeyStoreDefinitionSource(
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
