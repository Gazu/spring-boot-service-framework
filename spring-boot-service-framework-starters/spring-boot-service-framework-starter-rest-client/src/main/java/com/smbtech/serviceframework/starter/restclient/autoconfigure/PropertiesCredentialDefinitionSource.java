package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.domain.CredentialDefinition;
import com.smbtech.serviceframework.httpclient.port.out.CredentialDefinitionSource;
import java.util.Map;
import java.util.Optional;

final class PropertiesCredentialDefinitionSource implements CredentialDefinitionSource {

    private final Map<String, CredentialDefinition> definitions;

    /**
     * Creates a properties credential definition source instance.
     *
     * @param properties properties value
     * @param mapper mapper value
     */
    PropertiesCredentialDefinitionSource(
            RestClientProperties properties, CredentialPropertiesMapper mapper) {
        this.definitions = Map.copyOf(mapper.map(properties));
    }

    @Override
    public Optional<CredentialDefinition> findById(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    @Override
    public Map<String, CredentialDefinition> all() {
        return definitions;
    }
}
