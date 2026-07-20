package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore;

import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ResourceLoader;

/** Provides key store manager behavior. */
public final class KeyStoreManager {

    private final KeyStoreDefinitionSource definitionSource;
    private final ResourceLoader resourceLoader;
    private final Map<String, KeyStore> keyStores = new ConcurrentHashMap<>();

    /**
     * Creates a key store manager instance.
     *
     * @param definitionSource definition source value
     * @param resourceLoader resource loader value
     */
    public KeyStoreManager(
            KeyStoreDefinitionSource definitionSource, ResourceLoader resourceLoader) {
        this.definitionSource = definitionSource;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Returns the configured key store.
     *
     * @param id id value
     * @return get key store result
     */
    public KeyStore getKeyStore(String id) {
        return keyStores.computeIfAbsent(id, this::loadKeyStore);
    }

    /**
     * Returns the configured definition.
     *
     * @param id id value
     * @return get definition result
     */
    public KeyStoreDefinition getDefinition(String id) {
        return definitionSource
                .findById(id)
                .orElseThrow(
                        () ->
                                new HttpClientAuthenticationException(
                                        "Key store not configured: " + id));
    }

    private KeyStore loadKeyStore(String id) {
        KeyStoreDefinition definition = getDefinition(id);
        if (!definition.hasInlineContent() && definition.location().isBlank()) {
            throw new HttpClientAuthenticationException(
                    "location or base64 is required for key store: " + id);
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(definition.type());
            try (InputStream inputStream = inputStream(definition)) {
                keyStore.load(inputStream, chars(definition.password()));
            }
            return keyStore;
        } catch (Exception exception) {
            throw new HttpClientAuthenticationException(
                    "Unable to load key store: " + id, exception);
        }
    }

    private InputStream inputStream(KeyStoreDefinition definition) throws Exception {
        if (definition.hasInlineContent()) {
            return new ByteArrayInputStream(
                    Base64.getDecoder().decode(normalizedBase64(definition.base64())));
        }
        return resourceLoader.getResource(definition.location()).getInputStream();
    }

    private String normalizedBase64(String value) {
        return value.replaceAll("\\s", "");
    }

    /**
     * Performs the chars operation.
     *
     * @param value password text
     * @return chars result
     */
    public static char[] chars(String value) {
        return value == null ? new char[0] : value.toCharArray();
    }
}
