package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.domain.TokenCacheKey;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.httpclient.port.out.TokenCache;
import com.smbtech.serviceframework.httpclient.service.ScopeValidator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityClientCredentialsAccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityJwtBearerAccessTokenProvider;

import java.time.Clock;
import java.util.Objects;

public final class CachedAccessTokenProvider implements AccessTokenProvider {

    private final TokenCache tokenCache;
    private final SpringSecurityClientCredentialsAccessTokenProvider springClientCredentialsProvider;
    private final SpringSecurityJwtBearerAccessTokenProvider springJwtBearerProvider;
    private final ScopeValidator scopeValidator;
    private final Clock clock;

    public CachedAccessTokenProvider(
            TokenCache tokenCache,
            ScopeValidator scopeValidator,
            Clock clock
    ) {
        this(tokenCache, null, null, scopeValidator, clock);
    }

    public CachedAccessTokenProvider(
            TokenCache tokenCache,
            SpringSecurityClientCredentialsAccessTokenProvider springClientCredentialsProvider,
            SpringSecurityJwtBearerAccessTokenProvider springJwtBearerProvider,
            ScopeValidator scopeValidator,
            Clock clock
    ) {
        this.tokenCache = Objects.requireNonNull(tokenCache, "tokenCache must not be null");
        this.springClientCredentialsProvider = springClientCredentialsProvider;
        this.springJwtBearerProvider = springJwtBearerProvider;
        this.scopeValidator = Objects.requireNonNull(scopeValidator, "scopeValidator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String getAccessToken(String credentialTokenRequestorId, String scopes) {
        String normalizedId = normalize(credentialTokenRequestorId);
        if (springClientCredentialsProvider != null && springClientCredentialsProvider.supports(normalizedId)) {
            String cacheKey = springClientCredentialsCacheKey(normalizedId);
            return tokenCache.find(cacheKey)
                    .filter(token -> token.isActive(clock.instant()))
                    .map(token -> validateAndReturn(scopes, token))
                    .orElseGet(() -> fetchSpringValidateCacheAndReturn(normalizedId, cacheKey, scopes));
        }
        if (springJwtBearerProvider != null && springJwtBearerProvider.supports(normalizedId)) {
            String cacheKey = springJwtBearerCacheKey(normalizedId);
            return tokenCache.find(cacheKey)
                    .filter(token -> token.isActive(clock.instant()))
                    .map(token -> validateAndReturn(scopes, token))
                    .orElseGet(() -> fetchSpringJwtBearerValidateCacheAndReturn(normalizedId, cacheKey, scopes));
        }

        throw new AuthenticationException(
                "OAuth2 client registration not configured for token request: " + normalizedId
        );
    }

    private String fetchSpringValidateCacheAndReturn(String registrationId, String cacheKey, String scopes) {
        AccessToken token = springClientCredentialsProvider.fetchIfAvailable(registrationId)
                .orElseThrow(() -> new AuthenticationException(
                        "OAuth2 client registration not configured for client_credentials: " + registrationId
                ));
        tokenCache.put(cacheKey, token);
        return validateAndReturn(scopes, token);
    }

    private String fetchSpringJwtBearerValidateCacheAndReturn(String registrationId, String cacheKey, String scopes) {
        AccessToken token = springJwtBearerProvider.fetchIfAvailable(registrationId)
                .orElseThrow(() -> new AuthenticationException(
                        "OAuth2 client registration not configured for JWT bearer grant: " + registrationId
                ));
        tokenCache.put(cacheKey, token);
        return validateAndReturn(scopes, token);
    }

    private String springClientCredentialsCacheKey(String registrationId) {
        return TokenCacheKey.of(registrationId, springClientCredentialsProvider.requestedScopes(registrationId)).value();
    }

    private String springJwtBearerCacheKey(String registrationId) {
        return TokenCacheKey.of(registrationId, springJwtBearerProvider.requestedScopes(registrationId)).value();
    }

    private String validateAndReturn(String expectedScopes, AccessToken token) {
        scopeValidator.validate(expectedScopes, token.scopes());
        return token.value();
    }

    private String normalize(String id) {
        String normalized = Objects.requireNonNullElse(id, "").trim();
        if (normalized.isBlank()) {
            throw new AuthenticationException("credentialTokenRequestorId must not be blank");
        }
        return normalized;
    }
}
