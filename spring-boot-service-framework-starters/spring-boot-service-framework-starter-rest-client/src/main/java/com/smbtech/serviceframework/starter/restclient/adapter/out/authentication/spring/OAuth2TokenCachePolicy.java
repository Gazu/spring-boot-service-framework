package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.Objects;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

final class OAuth2TokenCachePolicy {

    static final AuthorizationGrantType JWT_BEARER_GRANT =
            new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer");

    private final boolean clientCredentials;
    private final boolean jwtBearer;

    OAuth2TokenCachePolicy(RestClientProperties.TokenCache tokenCache) {
        RestClientProperties.TokenCache safeTokenCache =
                Objects.requireNonNullElseGet(tokenCache, RestClientProperties.TokenCache::new);
        this.clientCredentials = safeTokenCache.isClientCredentials();
        this.jwtBearer = safeTokenCache.isJwtBearer();
    }

    static OAuth2TokenCachePolicy from(RestClientProperties properties) {
        RestClientProperties safeProperties =
                Objects.requireNonNullElseGet(properties, RestClientProperties::new);
        RestClientProperties.Authentication authentication =
                Objects.requireNonNullElseGet(
                        safeProperties.getAuthentication(),
                        RestClientProperties.Authentication::new);
        return new OAuth2TokenCachePolicy(authentication.getTokenCache());
    }

    boolean isCacheEnabled(ClientRegistration registration) {
        if (registration == null || registration.getAuthorizationGrantType() == null) {
            return true;
        }
        AuthorizationGrantType grantType = registration.getAuthorizationGrantType();
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(grantType)) {
            return clientCredentials;
        }
        if (JWT_BEARER_GRANT.equals(grantType)) {
            return jwtBearer;
        }
        return true;
    }
}
