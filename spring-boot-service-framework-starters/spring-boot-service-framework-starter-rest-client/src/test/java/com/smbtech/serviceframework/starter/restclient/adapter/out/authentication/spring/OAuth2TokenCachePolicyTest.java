package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2TokenCachePolicyTest {

    @Test
    void defaultsToCacheClientCredentialsAndJwtBearer() {
        OAuth2TokenCachePolicy policy = OAuth2TokenCachePolicy.from(new RestClientProperties());

        assertThat(policy.isCacheEnabled(clientCredentialsRegistration())).isTrue();
        assertThat(policy.isCacheEnabled(jwtBearerRegistration())).isTrue();
    }

    @Test
    void canDisableClientCredentialsCache() {
        RestClientProperties.TokenCache tokenCache = new RestClientProperties.TokenCache();
        tokenCache.setClientCredentials(false);
        tokenCache.setJwtBearer(true);

        OAuth2TokenCachePolicy policy = new OAuth2TokenCachePolicy(tokenCache);

        assertThat(policy.isCacheEnabled(clientCredentialsRegistration())).isFalse();
        assertThat(policy.isCacheEnabled(jwtBearerRegistration())).isTrue();
    }

    @Test
    void canDisableJwtBearerCache() {
        RestClientProperties.TokenCache tokenCache = new RestClientProperties.TokenCache();
        tokenCache.setClientCredentials(true);
        tokenCache.setJwtBearer(false);

        OAuth2TokenCachePolicy policy = new OAuth2TokenCachePolicy(tokenCache);

        assertThat(policy.isCacheEnabled(clientCredentialsRegistration())).isTrue();
        assertThat(policy.isCacheEnabled(jwtBearerRegistration())).isFalse();
    }

    @Test
    void unknownGrantTypesAreCacheableByDefault() {
        RestClientProperties.TokenCache tokenCache = new RestClientProperties.TokenCache();
        tokenCache.setClientCredentials(false);
        tokenCache.setJwtBearer(false);

        OAuth2TokenCachePolicy policy = new OAuth2TokenCachePolicy(tokenCache);

        assertThat(policy.isCacheEnabled(registration(new AuthorizationGrantType("custom_grant")))).isTrue();
    }

    @Test
    void handlesNullPropertiesAuthenticationAndTokenCache() {
        RestClientProperties properties = new RestClientProperties();
        properties.setAuthentication(null);

        assertThat(OAuth2TokenCachePolicy.from(properties).isCacheEnabled(clientCredentialsRegistration())).isTrue();
        assertThat(OAuth2TokenCachePolicy.from(null).isCacheEnabled(jwtBearerRegistration())).isTrue();
        assertThat(new OAuth2TokenCachePolicy(null).isCacheEnabled(clientCredentialsRegistration())).isTrue();
    }

    private ClientRegistration clientCredentialsRegistration() {
        return registration(AuthorizationGrantType.CLIENT_CREDENTIALS);
    }

    private ClientRegistration jwtBearerRegistration() {
        return registration(OAuth2TokenCachePolicy.JWT_BEARER_GRANT);
    }

    private ClientRegistration registration(AuthorizationGrantType grantType) {
        return ClientRegistration
                .withRegistrationId("payments-api")
                .tokenUri("https://auth.example/token")
                .clientId("payments-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(grantType)
                .scope("payment.read")
                .build();
    }
}
