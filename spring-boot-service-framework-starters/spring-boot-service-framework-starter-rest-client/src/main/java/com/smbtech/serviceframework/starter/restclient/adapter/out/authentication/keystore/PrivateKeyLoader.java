package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore;

import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.Certificate;

final class PrivateKeyLoader {

    private final KeyStoreManager keyStoreManager;

    /**
     * Creates a private key loader instance.
     *
     * @param keyStoreManager key store manager
     */
    PrivateKeyLoader(KeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }

    /**
     * Loads private key.
     *
     * @param keyStoreId key store id value
     * @return load private key result
     */
    PrivateKey loadPrivateKey(String keyStoreId) {
        KeyStoreDefinition definition = definition(keyStoreId);
        validateKeyAlias(keyStoreId, definition);
        try {
            Key key =
                    keyStoreManager
                            .getKeyStore(keyStoreId)
                            .getKey(
                                    definition.keyAlias(),
                                    KeyStoreManager.chars(definition.keyPassword()));
            if (key instanceof PrivateKey privateKey) {
                return privateKey;
            }
            throw new HttpClientAuthenticationException(
                    "Alias does not contain a private key: " + definition.keyAlias());
        } catch (HttpClientAuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new HttpClientAuthenticationException(
                    "Unable to load private key from key store: " + keyStoreId, exception);
        }
    }

    /**
     * Loads certificate.
     *
     * @param keyStoreId key store id value
     * @return load certificate result
     */
    Certificate loadCertificate(String keyStoreId) {
        KeyStoreDefinition definition = definition(keyStoreId);
        validateKeyAlias(keyStoreId, definition);
        try {
            Certificate certificate =
                    keyStoreManager.getKeyStore(keyStoreId).getCertificate(definition.keyAlias());
            if (certificate == null) {
                throw new HttpClientAuthenticationException(
                        "Alias does not contain a certificate: " + definition.keyAlias());
            }
            return certificate;
        } catch (HttpClientAuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new HttpClientAuthenticationException(
                    "Unable to load certificate from key store: " + keyStoreId, exception);
        }
    }

    private void validateKeyAlias(String keyStoreId, KeyStoreDefinition definition) {
        if (definition.keyAlias().isBlank()) {
            throw new HttpClientAuthenticationException(
                    "keyAlias is required for key store private key access: " + keyStoreId);
        }
    }

    private KeyStoreDefinition definition(String keyStoreId) {
        return keyStoreManager.getDefinition(keyStoreId);
    }
}
