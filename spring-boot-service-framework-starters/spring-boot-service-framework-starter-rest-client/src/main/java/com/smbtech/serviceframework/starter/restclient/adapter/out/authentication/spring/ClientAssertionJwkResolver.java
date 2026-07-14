package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.nimbusds.jose.jwk.JWK;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.util.Objects;

/**
 * Resolves signing JWKs for Spring Security private_key_jwt client authentication.
 */
public final class ClientAssertionJwkResolver {

    private final RestClientProperties properties;
    private final SigningJwkResolver signingJwkResolver;

    public ClientAssertionJwkResolver(
            RestClientProperties properties,
            SigningJwkResolver signingJwkResolver
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.signingJwkResolver = Objects.requireNonNull(signingJwkResolver, "signingJwkResolver must not be null");
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

        return signingJwkResolver.resolve(keyStoreId);
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
