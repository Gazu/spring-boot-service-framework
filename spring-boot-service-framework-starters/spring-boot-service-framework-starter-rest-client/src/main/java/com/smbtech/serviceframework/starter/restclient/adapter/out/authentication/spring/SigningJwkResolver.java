package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.nimbusds.jose.jwk.JWK;
import java.util.Objects;
import java.util.function.Function;

/** Provides signing jwk resolver behavior. */
final class SigningJwkResolver {

    private final Function<String, JWK> delegate;

    /**
     * Creates a signing jwk resolver instance.
     *
     * @param delegate signing JWK resolver delegate
     */
    SigningJwkResolver(Function<String, JWK> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Performs the resolve operation.
     *
     * @param keyStoreId key store id value
     * @return resolve result
     */
    JWK resolve(String keyStoreId) {
        return delegate.apply(keyStoreId);
    }
}
