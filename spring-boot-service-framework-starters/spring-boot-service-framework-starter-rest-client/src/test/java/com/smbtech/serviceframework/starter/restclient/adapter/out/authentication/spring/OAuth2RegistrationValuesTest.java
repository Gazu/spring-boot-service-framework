package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.smbtech.serviceframework.httpclient.domain.ClientAuthenticationMethod;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

class OAuth2RegistrationValuesTest {

    @Test
    void mapsSupportedGrantTypes() {
        assertThat(
                        OAuth2RegistrationValues.grantType(
                                new AuthorizationGrantType(
                                        "urn:ietf:params:oauth:grant-type:jwt-bearer")))
                .isEqualTo(GrantType.JWT_BEARER);
        assertThat(OAuth2RegistrationValues.grantType(AuthorizationGrantType.CLIENT_CREDENTIALS))
                .isEqualTo(GrantType.CLIENT_CREDENTIALS);
    }

    @Test
    void mapsSupportedClientAuthenticationMethods() {
        assertThat(
                        OAuth2RegistrationValues.clientAuthenticationMethod(
                                registration(
                                        org.springframework.security.oauth2.core
                                                .ClientAuthenticationMethod.PRIVATE_KEY_JWT)))
                .isEqualTo(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
        assertThat(
                        OAuth2RegistrationValues.clientAuthenticationMethod(
                                registration(
                                        org.springframework.security.oauth2.core
                                                .ClientAuthenticationMethod.CLIENT_SECRET_POST)))
                .isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        assertThat(
                        OAuth2RegistrationValues.clientAuthenticationMethod(
                                registration(
                                        org.springframework.security.oauth2.core
                                                .ClientAuthenticationMethod.NONE)))
                .isEqualTo(ClientAuthenticationMethod.NONE);
        assertThat(
                        OAuth2RegistrationValues.clientAuthenticationMethod(
                                registration(
                                        org.springframework.security.oauth2.core
                                                .ClientAuthenticationMethod.CLIENT_SECRET_BASIC)))
                .isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
    }

    @Test
    void resolvesAndValidatesRegistrationTokenUri() {
        assertThat(
                        OAuth2RegistrationValues.tokenUri(
                                registration(
                                        org.springframework.security.oauth2.core
                                                .ClientAuthenticationMethod.CLIENT_SECRET_BASIC)))
                .isEqualTo(URI.create("https://identity.example.test/oauth2/token"));
        assertThatNullPointerException()
                .isThrownBy(() -> OAuth2RegistrationValues.tokenUri(null))
                .withMessage("registration must not be null");
    }

    private static ClientRegistration registration(
            org.springframework.security.oauth2.core.ClientAuthenticationMethod method) {
        return ClientRegistration.withRegistrationId("payments-token")
                .clientId("payments-client")
                .clientSecret("secret")
                .clientAuthenticationMethod(method)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("https://identity.example.test/oauth2/token")
                .build();
    }
}
