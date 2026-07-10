package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication;

import com.smbtech.serviceframework.httpclient.domain.CredentialDefinition;
import com.smbtech.serviceframework.httpclient.port.out.CredentialDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.CredentialPropertiesMapper;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;

import java.util.Map;
import java.util.Optional;

public final class PropertiesCredentialDefinitionSource implements CredentialDefinitionSource {

    private final Map<String, CredentialDefinition> definitions;

    public PropertiesCredentialDefinitionSource(
            RestClientProperties properties,
            CredentialPropertiesMapper mapper
    ) {
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
