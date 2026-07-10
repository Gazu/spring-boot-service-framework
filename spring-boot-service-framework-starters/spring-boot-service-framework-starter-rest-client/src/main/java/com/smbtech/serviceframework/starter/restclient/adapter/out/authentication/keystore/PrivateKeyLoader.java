package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore;

import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;

import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public final class PrivateKeyLoader {

    private final KeyStoreManager keyStoreManager;

    public PrivateKeyLoader(KeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }

    public PrivateKey loadPrivateKey(String keyStoreId) {
        KeyStoreDefinition definition = keyStoreManager.getDefinition(keyStoreId);
        validateKeyAlias(keyStoreId, definition);
        try {
            Key key = keyStoreManager.getKeyStore(keyStoreId)
                    .getKey(definition.keyAlias(), KeyStoreManager.chars(definition.keyPassword()));
            if (key instanceof PrivateKey privateKey) {
                return privateKey;
            }
            throw new AuthenticationException("Alias does not contain a private key: " + definition.keyAlias());
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthenticationException("Unable to load private key from key store: " + keyStoreId, exception);
        }
    }

    public Certificate loadCertificate(String keyStoreId) {
        KeyStoreDefinition definition = keyStoreManager.getDefinition(keyStoreId);
        validateKeyAlias(keyStoreId, definition);
        try {
            Certificate certificate = keyStoreManager.getKeyStore(keyStoreId).getCertificate(definition.keyAlias());
            if (certificate == null) {
                throw new AuthenticationException("Alias does not contain a certificate: " + definition.keyAlias());
            }
            return certificate;
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthenticationException("Unable to load certificate from key store: " + keyStoreId, exception);
        }
    }

    private void validateKeyAlias(String keyStoreId, KeyStoreDefinition definition) {
        if (definition.keyAlias().isBlank()) {
            throw new AuthenticationException("keyAlias is required for key store private key access: " + keyStoreId);
        }
    }
}
