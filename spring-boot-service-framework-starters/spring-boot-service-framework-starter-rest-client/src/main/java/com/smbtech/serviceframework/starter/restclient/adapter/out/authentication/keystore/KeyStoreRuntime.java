package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import javax.net.ssl.SSLContext;

final class KeyStoreRuntime {

    private final KeyStoreManager keyStoreManager;
    private final SslContextFactory sslContextFactory;

    KeyStoreRuntime(KeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
        this.sslContextFactory = new SslContextFactory(keyStoreManager);
    }

    SSLContext buildSslContext(HttpClientDefinition definition, SSLContext fallbackSslContext) {
        return sslContextFactory.build(definition, fallbackSslContext);
    }

    void validateLoadable(String keyStoreId) {
        keyStoreManager.getKeyStore(keyStoreId);
    }

    String validateMtls(String keyStoreId) {
        KeyStoreDefinition definition = keyStoreManager.getDefinition(keyStoreId);
        if (definition.keyAlias().isBlank()) {
            return "is required for mTLS key store content validation";
        }

        try {
            KeyStore keyStore = keyStoreManager.getKeyStore(keyStoreId);
            if (!keyStore.containsAlias(definition.keyAlias())) {
                return "references missing alias " + definition.keyAlias();
            }

            Key key =
                    keyStore.getKey(
                            definition.keyAlias(), KeyStoreManager.chars(definition.keyPassword()));
            if (!(key instanceof PrivateKey)) {
                return "does not contain a private key: " + definition.keyAlias();
            }
        } catch (HttpClientAuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new HttpClientAuthenticationException(
                    "Unable to validate mTLS key store: " + keyStoreId, exception);
        }
        return "";
    }
}
