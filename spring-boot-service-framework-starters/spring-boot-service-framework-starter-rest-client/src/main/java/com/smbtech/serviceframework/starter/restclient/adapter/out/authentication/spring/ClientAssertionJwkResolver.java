package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreManager;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.PrivateKeyLoader;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

/**
 * Resolves signing JWKs for Spring Security private_key_jwt client authentication.
 */
public final class ClientAssertionJwkResolver {

    private final RestClientProperties properties;
    private final KeyStoreManager keyStoreManager;
    private final PrivateKeyLoader privateKeyLoader;

    public ClientAssertionJwkResolver(
            RestClientProperties properties,
            KeyStoreManager keyStoreManager,
            PrivateKeyLoader privateKeyLoader
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.keyStoreManager = Objects.requireNonNull(keyStoreManager, "keyStoreManager must not be null");
        this.privateKeyLoader = Objects.requireNonNull(privateKeyLoader, "privateKeyLoader must not be null");
    }

    public JWK resolve(ClientRegistration clientRegistration) {
        Objects.requireNonNull(clientRegistration, "clientRegistration must not be null");
        String registrationId = clientRegistration.getRegistrationId();
        RestClientProperties.ClientAssertion assertion = clientAssertion(registrationId);
        String keyStoreId = Objects.requireNonNullElse(assertion.getKeyStoreId(), "").trim();
        if (keyStoreId.isBlank()) {
            throw new AuthenticationException(
                    "key-store-id is required for private_key_jwt client assertion: " + registrationId
            );
        }

        KeyStoreDefinition definition = keyStoreManager.getDefinition(keyStoreId);
        PrivateKey privateKey = privateKeyLoader.loadPrivateKey(keyStoreId);
        Certificate certificate = privateKeyLoader.loadCertificate(keyStoreId);

        if (!(privateKey instanceof RSAPrivateKey rsaPrivateKey)) {
            throw new AuthenticationException("Private key must be RSA for key store: " + keyStoreId);
        }
        if (!(certificate.getPublicKey() instanceof RSAPublicKey rsaPublicKey)) {
            throw new AuthenticationException("Certificate public key must be RSA for key store: " + keyStoreId);
        }

/*        RSAKey.Builder builder = new RSAKey.Builder(rsaPublicKey).privateKey(rsaPrivateKey);
        if (!definition.keyAlias().isBlank()) {
            builder.keyID(definition.keyAlias());
        }
        return builder.build();*/

        return new RSAKey.Builder(rsaPublicKey)
                .privateKey(rsaPrivateKey)
                .build();
    }

    public RestClientProperties.ClientAssertion clientAssertion(String registrationId) {
        RestClientProperties.Authentication authentication = Objects.requireNonNullElseGet(
                properties.getAuthentication(),
                RestClientProperties.Authentication::new
        );
        RestClientProperties.ClientAssertion assertion = authentication.getClientAssertions().get(registrationId);
        if (assertion == null) {
            throw new AuthenticationException(
                    "client assertion configuration not found for OAuth2 registration: " + registrationId
            );
        }
        return assertion;
    }
}
