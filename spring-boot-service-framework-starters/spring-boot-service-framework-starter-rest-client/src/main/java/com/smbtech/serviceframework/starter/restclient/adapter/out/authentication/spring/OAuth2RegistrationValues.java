package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.ClientAuthenticationMethod;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import java.net.URI;
import java.util.Objects;
import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

final class OAuth2RegistrationValues {

    private static final AuthorizationGrantType JWT_BEARER_GRANT =
            new AuthorizationGrantType(GrantType.JWT_BEARER.value());

    private OAuth2RegistrationValues() {}

    static URI tokenUri(ClientRegistration registration) {
        String tokenUri = requireRegistration(registration).getProviderDetails().getTokenUri();
        return tokenUri == null || tokenUri.isBlank() ? null : URI.create(tokenUri);
    }

    static GrantType grantType(AbstractOAuth2AuthorizationGrantRequest grantRequest) {
        Objects.requireNonNull(grantRequest, "grantRequest must not be null");
        return grantType(grantRequest.getGrantType());
    }

    static GrantType grantType(AuthorizationGrantType grantType) {
        return JWT_BEARER_GRANT.equals(grantType)
                ? GrantType.JWT_BEARER
                : GrantType.CLIENT_CREDENTIALS;
    }

    static ClientAuthenticationMethod clientAuthenticationMethod(ClientRegistration registration) {
        org.springframework.security.oauth2.core.ClientAuthenticationMethod method =
                requireRegistration(registration).getClientAuthenticationMethod();
        if (org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST
                .equals(method)) {
            return ClientAuthenticationMethod.CLIENT_SECRET_POST;
        }
        if (org.springframework.security.oauth2.core.ClientAuthenticationMethod.PRIVATE_KEY_JWT
                .equals(method)) {
            return ClientAuthenticationMethod.PRIVATE_KEY_JWT;
        }
        if (org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE.equals(
                method)) {
            return ClientAuthenticationMethod.NONE;
        }
        return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
    }

    private static ClientRegistration requireRegistration(ClientRegistration registration) {
        return Objects.requireNonNull(registration, "registration must not be null");
    }
}
