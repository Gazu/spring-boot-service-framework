package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.PrivateKeyLoader;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

public final class SigningJwkResolver {

    private final PrivateKeyLoader privateKeyLoader;

    public SigningJwkResolver(PrivateKeyLoader privateKeyLoader) {
        this.privateKeyLoader = Objects.requireNonNull(privateKeyLoader, "privateKeyLoader must not be null");
    }

    public JWK resolve(String keyStoreId) {
        String normalizedKeyStoreId = Objects.requireNonNullElse(keyStoreId, "").trim();
        if (normalizedKeyStoreId.isBlank()) {
            throw new AuthenticationException("key-store-id is required for JWT signing");
        }

        PrivateKey privateKey = privateKeyLoader.loadPrivateKey(normalizedKeyStoreId);
        Certificate certificate = privateKeyLoader.loadCertificate(normalizedKeyStoreId);

        if (!(privateKey instanceof RSAPrivateKey rsaPrivateKey)) {
            throw new AuthenticationException("Private key must be RSA for key store: " + normalizedKeyStoreId);
        }
        if (!(certificate.getPublicKey() instanceof RSAPublicKey rsaPublicKey)) {
            throw new AuthenticationException("Certificate public key must be RSA for key store: " + normalizedKeyStoreId);
        }

        return new RSAKey.Builder(rsaPublicKey)
                .privateKey(rsaPrivateKey)
                .build();
    }
}
