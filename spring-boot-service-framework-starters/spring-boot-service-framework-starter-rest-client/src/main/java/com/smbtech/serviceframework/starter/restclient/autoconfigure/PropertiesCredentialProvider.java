package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.port.out.CredentialDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;
import java.util.Optional;

final class PropertiesCredentialProvider implements CredentialProvider {

    private final CredentialDefinitionSource source;

    /**
     * Creates a properties credential provider instance.
     *
     * @param source source value
     */
    PropertiesCredentialProvider(CredentialDefinitionSource source) {
        this.source = source;
    }

    @Override
    public Optional<String> findSecret(String key) {
        return source.findById(key)
                .map(com.smbtech.serviceframework.httpclient.domain.CredentialDefinition::value);
    }
}
