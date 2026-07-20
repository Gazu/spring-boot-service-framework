package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Provides key store properties mapper behavior. */
public final class KeyStorePropertiesMapper {

    private final CredentialResolver credentialResolver;

    /**
     * Creates a key store properties mapper instance.
     *
     * @param credentialResolver credential resolver value
     */
    public KeyStorePropertiesMapper(CredentialResolver credentialResolver) {
        this.credentialResolver = credentialResolver;
    }

    /**
     * Performs the map operation.
     *
     * @param properties properties value
     * @return map result
     */
    public Map<String, KeyStoreDefinition> map(RestClientProperties properties) {
        Map<String, KeyStoreDefinition> definitions = new LinkedHashMap<>();
        RestClientProperties.Authentication authentication =
                Objects.requireNonNullElseGet(
                        properties.getAuthentication(), RestClientProperties.Authentication::new);

        authentication
                .getKeyStores()
                .forEach(
                        (id, keyStore) ->
                                definitions.put(
                                        id,
                                        new KeyStoreDefinition(
                                                id,
                                                keyStore.getLocation(),
                                                keyStore.getBase64(),
                                                keyStore.getType(),
                                                credentialResolver.resolve(
                                                        keyStore.getPassword(),
                                                        keyStore.getPasswordRef(),
                                                        "key store password"),
                                                keyStore.getKeyAlias(),
                                                credentialResolver.resolve(
                                                        keyStore.getKeyPassword(),
                                                        keyStore.getKeyPasswordRef(),
                                                        "key store keyPassword"))));

        return definitions;
    }
}
