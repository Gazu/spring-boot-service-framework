package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.domain.ClientAuthenticationMethod;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.httpclient.domain.JwtBearerDefinition;
import com.smbtech.serviceframework.httpclient.domain.TokenRequestDefinition;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.OAuth2AccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Bridges Spring Boot OAuth2 client registrations with the JWT bearer grant.
 */
public final class SpringSecurityJwtBearerAccessTokenProvider {

    private static final AuthorizationGrantType JWT_BEARER_GRANT =
            new AuthorizationGrantType(GrantType.JWT_BEARER.value());

    private final SpringClientRegistrationResolver registrationResolver;
    private final RestClientProperties properties;
    private final OAuth2AccessTokenProvider delegate;

    public SpringSecurityJwtBearerAccessTokenProvider(
            SpringClientRegistrationResolver registrationResolver,
            RestClientProperties properties,
            OAuth2AccessTokenProvider delegate
    ) {
        this.registrationResolver = Objects.requireNonNull(
                registrationResolver,
                "registrationResolver must not be null"
        );
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public boolean supports(String registrationId) {
        return registrationResolver.findByRegistrationId(registrationId)
                .map(ClientRegistration::getAuthorizationGrantType)
                .filter(JWT_BEARER_GRANT::equals)
                .isPresent();
    }

    public Set<String> requestedScopes(String registrationId) {
        return registrationResolver.findByRegistrationId(registrationId)
                .filter(registration -> JWT_BEARER_GRANT.equals(registration.getAuthorizationGrantType()))
                .map(ClientRegistration::getScopes)
                .orElse(Set.of());
    }

    public Optional<AccessToken> fetchIfAvailable(String registrationId) {
        return registrationResolver.findByRegistrationId(registrationId)
                .filter(registration -> JWT_BEARER_GRANT.equals(registration.getAuthorizationGrantType()))
                .map(registration -> delegate.fetch(toTokenRequestDefinition(registration)));
    }

    private TokenRequestDefinition toTokenRequestDefinition(ClientRegistration registration) {
        String registrationId = registration.getRegistrationId();
        RestClientProperties.JwtBearer jwtBearer = jwtBearer(registrationId);
        String tokenUri = registration.getProviderDetails().getTokenUri();
        if (tokenUri == null || tokenUri.isBlank()) {
            throw new AuthenticationException("token-uri is required for OAuth2 registration: " + registrationId);
        }

        return new TokenRequestDefinition(
                registrationId,
                URI.create(tokenUri),
                GrantType.JWT_BEARER,
                clientAuthenticationMethod(registration),
                registration.getClientId(),
                Objects.requireNonNullElse(registration.getClientSecret(), ""),
                new LinkedHashSet<>(registration.getScopes()),
                Duration.ofSeconds(30),
                new JwtBearerDefinition(
                        jwtBearer.getKeyStoreId(),
                        jwtBearer.getIssuer(),
                        jwtBearer.getSubject(),
                        jwtBearer.getAudience(),
                        jwtBearer.getTokenLifetime(),
                        jwtBearer.getCustomClaims()
                )
        );
    }

    private RestClientProperties.JwtBearer jwtBearer(String registrationId) {
        RestClientProperties.Authentication authentication = Objects.requireNonNullElseGet(
                properties.getAuthentication(),
                RestClientProperties.Authentication::new
        );
        RestClientProperties.JwtBearer jwtBearer = authentication.getJwtBearer().get(registrationId);
        if (jwtBearer == null) {
            throw new AuthenticationException(
                    "jwt-bearer configuration not found for OAuth2 registration: " + registrationId
            );
        }
        return jwtBearer;
    }

    private ClientAuthenticationMethod clientAuthenticationMethod(ClientRegistration registration) {
        org.springframework.security.oauth2.core.ClientAuthenticationMethod method =
                registration.getClientAuthenticationMethod();
        if (org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE.equals(method)) {
            return ClientAuthenticationMethod.NONE;
        }
        if (org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(method)) {
            return ClientAuthenticationMethod.CLIENT_SECRET_POST;
        }
        if (org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(method)) {
            return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        }
        throw new AuthenticationException(
                "Unsupported client authentication method for JWT bearer registration "
                        + registration.getRegistrationId()
                        + ": "
                        + method.getValue()
        );
    }
}
