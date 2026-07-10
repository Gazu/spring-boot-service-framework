package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore;

import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;

import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;

public final class RsaKeyFactory {

    private final PrivateKeyLoader privateKeyLoader;

    public RsaKeyFactory(PrivateKeyLoader privateKeyLoader) {
        this.privateKeyLoader = privateKeyLoader;
    }

    public RSAPrivateKey rsaPrivateKey(String keyStoreId) {
        PrivateKey privateKey = privateKeyLoader.loadPrivateKey(keyStoreId);
        if (privateKey instanceof RSAPrivateKey rsaPrivateKey) {
            return rsaPrivateKey;
        }
        throw new AuthenticationException("Private key must be RSA for key store: " + keyStoreId);
    }
}
