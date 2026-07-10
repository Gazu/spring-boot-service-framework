package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.domain.SslPolicy;
import com.smbtech.serviceframework.httpclient.exception.HttpClientConfigurationException;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreManager;
import org.apache.hc.core5.ssl.PrivateKeyStrategy;
import org.apache.hc.core5.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;

public final class SslContextFactory {

    private final KeyStoreManager keyStoreManager;

    public SslContextFactory() {
        this(null);
    }

    public SslContextFactory(KeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }

    public SSLContext build(HttpClientDefinition definition, SSLContext fallbackSslContext) {
        SslPolicy ssl = definition.apache().ssl();
        if (!ssl.enabled() && !ssl.usesConfiguredStores()) {
            return fallbackSslContext;
        }
        if (!ssl.usesConfiguredStores()) {
            return fallbackSslContext;
        }
        if (keyStoreManager == null) {
            throw new HttpClientConfigurationException("KeyStoreManager is required when SSL stores are configured");
        }

        try {
            var builder = SSLContexts.custom();

            if (!ssl.trustStoreId().isBlank()) {
                KeyStore trustStore = keyStoreManager.getKeyStore(ssl.trustStoreId());
                builder.loadTrustMaterial(trustStore, null);
            }

            if (!ssl.keyStoreId().isBlank()) {
                KeyStoreDefinition keyStoreDefinition = keyStoreManager.getDefinition(ssl.keyStoreId());
                validateKeyStoreAlias(ssl.keyStoreId(), keyStoreDefinition);
                KeyStore keyStore = keyStoreManager.getKeyStore(ssl.keyStoreId());
                validateKeyStoreContainsAlias(ssl.keyStoreId(), keyStoreDefinition, keyStore);
                builder.loadKeyMaterial(
                        keyStore,
                        KeyStoreManager.chars(keyStoreDefinition.keyPassword()),
                        aliasStrategy(keyStoreDefinition)
                );
            }

            return builder.build();
        } catch (Exception exception) {
            throw new HttpClientConfigurationException(
                    "Unable to build SSL context for rest client: " + definition.name(),
                    exception
            );
        }
    }

    private PrivateKeyStrategy aliasStrategy(KeyStoreDefinition definition) {
        return (aliases, sslParameters) -> aliases.containsKey(definition.keyAlias())
                ? definition.keyAlias()
                : null;
    }

    private void validateKeyStoreAlias(String keyStoreId, KeyStoreDefinition definition) {
        if (definition.keyAlias().isBlank()) {
            throw new HttpClientConfigurationException("keyAlias is required for mTLS key store: " + keyStoreId);
        }
    }

    private void validateKeyStoreContainsAlias(
            String keyStoreId,
            KeyStoreDefinition definition,
            KeyStore keyStore
    ) throws Exception {
        if (!keyStore.containsAlias(definition.keyAlias())) {
            throw new HttpClientConfigurationException(
                    "keyAlias not found in mTLS key store: " + keyStoreId + "/" + definition.keyAlias()
            );
        }
    }
}
