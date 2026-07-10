package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Client credentials token provider backed by Spring Security OAuth2 Client.
 */
public final class SpringSecurityClientCredentialsAccessTokenProvider {

    private final SpringClientRegistrationResolver clientRegistrationResolver;
    private final OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> tokenResponseClient;
    private final Clock clock;

    public SpringSecurityClientCredentialsAccessTokenProvider(
            SpringClientRegistrationResolver clientRegistrationResolver,
            SpringClientCredentialsTokenResponseClientFactory tokenResponseClientFactory,
            Clock clock
    ) {
        this(
                clientRegistrationResolver,
                tokenResponseClientFactory.create(),
                clock
        );
    }

    public SpringSecurityClientCredentialsAccessTokenProvider(
            SpringClientRegistrationResolver clientRegistrationResolver,
            OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> tokenResponseClient,
            Clock clock
    ) {
        this.clientRegistrationResolver = Objects.requireNonNull(
                clientRegistrationResolver,
                "clientRegistrationResolver must not be null"
        );
        this.tokenResponseClient = Objects.requireNonNull(tokenResponseClient, "tokenResponseClient must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public boolean supports(String registrationId) {
        return findClientCredentialsRegistration(registrationId).isPresent();
    }

    public Set<String> requestedScopes(String registrationId) {
        return findClientCredentialsRegistration(registrationId)
                .map(ClientRegistration::getScopes)
                .orElse(Set.of());
    }

    public Optional<AccessToken> fetchIfAvailable(String registrationId) {
        return findClientCredentialsRegistration(registrationId).map(this::fetch);
    }

    public AccessToken fetch(String registrationId) {
        ClientRegistration registration = clientRegistrationResolver.requireByRegistrationId(registrationId);
        if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(registration.getAuthorizationGrantType())) {
            throw new AuthenticationException(
                    "OAuth2 client registration " + registration.getRegistrationId()
                            + " is not client_credentials"
            );
        }
        return fetch(registration);
    }

    private Optional<ClientRegistration> findClientCredentialsRegistration(String registrationId) {
        return clientRegistrationResolver.findByRegistrationId(registrationId)
                .filter(registration -> AuthorizationGrantType.CLIENT_CREDENTIALS.equals(
                        registration.getAuthorizationGrantType()
                ));
    }

    private AccessToken fetch(ClientRegistration registration) {
        OAuth2AccessTokenResponse response = tokenResponseClient.getTokenResponse(
                new OAuth2ClientCredentialsGrantRequest(registration)
        );
        org.springframework.security.oauth2.core.OAuth2AccessToken springToken = response.getAccessToken();
        if (springToken == null || springToken.getTokenValue() == null || springToken.getTokenValue().isBlank()) {
            throw new AuthenticationException(
                    "Token endpoint did not return access_token for OAuth2 client registration: "
                            + registration.getRegistrationId()
            );
        }

        Instant expiresAt = springToken.getExpiresAt();
        if (expiresAt == null) {
            expiresAt = clock.instant().plusSeconds(300);
        }
        String tokenType = springToken.getTokenType() == null
                ? "Bearer"
                : springToken.getTokenType().getValue();
        Set<String> scopes = springToken.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            scopes = registration.getScopes();
        }

        return new AccessToken(springToken.getTokenValue(), tokenType, expiresAt, scopes);
    }
}
