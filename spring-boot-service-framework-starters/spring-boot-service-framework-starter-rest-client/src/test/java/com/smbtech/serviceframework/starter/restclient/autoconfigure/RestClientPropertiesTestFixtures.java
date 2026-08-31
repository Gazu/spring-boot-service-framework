package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.port.out.CredentialDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;

public final class RestClientPropertiesTestFixtures {

    private RestClientPropertiesTestFixtures() {}

    public static KeyStoreDefinitionSource keyStoreDefinitionSource(
            RestClientProperties properties) {
        CredentialDefinitionSource credentialDefinitionSource =
                new PropertiesCredentialDefinitionSource(
                        properties, new CredentialPropertiesMapper());
        CredentialProvider credentialProvider =
                new PropertiesCredentialProvider(credentialDefinitionSource);
        CredentialResolver credentialResolver = new CredentialResolver(credentialProvider);
        return new PropertiesKeyStoreDefinitionSource(
                properties, new KeyStorePropertiesMapper(credentialResolver));
    }
}
