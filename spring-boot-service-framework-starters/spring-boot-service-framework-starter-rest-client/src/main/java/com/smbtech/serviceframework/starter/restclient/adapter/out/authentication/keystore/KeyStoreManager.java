package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore;

import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeyStoreManager {

    private final KeyStoreDefinitionSource definitionSource;
    private final ResourceLoader resourceLoader;
    private final Map<String, KeyStore> keyStores = new ConcurrentHashMap<>();

    public KeyStoreManager(
            KeyStoreDefinitionSource definitionSource,
            ResourceLoader resourceLoader
    ) {
        this.definitionSource = definitionSource;
        this.resourceLoader = resourceLoader;
    }

    public KeyStore getKeyStore(String id) {
        return keyStores.computeIfAbsent(id, this::loadKeyStore);
    }

    public KeyStoreDefinition getDefinition(String id) {
        return definitionSource.findById(id)
                .orElseThrow(() -> new AuthenticationException("Key store not configured: " + id));
    }

    private KeyStore loadKeyStore(String id) {
        KeyStoreDefinition definition = getDefinition(id);
        if (!definition.hasInlineContent() && definition.location().isBlank()) {
            throw new AuthenticationException("location or base64 is required for key store: " + id);
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(definition.type());
            try (InputStream inputStream = inputStream(definition)) {
                keyStore.load(inputStream, chars(definition.password()));
            }
            return keyStore;
        } catch (Exception exception) {
            throw new AuthenticationException("Unable to load key store: " + id, exception);
        }
    }

    private InputStream inputStream(KeyStoreDefinition definition) throws Exception {
        if (definition.hasInlineContent()) {
            return new ByteArrayInputStream(Base64.getDecoder().decode(normalizedBase64(definition.base64())));
        }
        return resourceLoader.getResource(definition.location()).getInputStream();
    }

    private String normalizedBase64(String value) {
        return value.replaceAll("\\s", "");
    }

    public static char[] chars(String value) {
        return value == null ? new char[0] : value.toCharArray();
    }
}
