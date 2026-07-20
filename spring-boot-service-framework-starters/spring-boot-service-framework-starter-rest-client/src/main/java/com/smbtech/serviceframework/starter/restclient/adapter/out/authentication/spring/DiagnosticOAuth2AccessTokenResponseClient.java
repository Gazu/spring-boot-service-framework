package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import java.util.Objects;
import java.util.Set;
import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

final class DiagnosticOAuth2AccessTokenResponseClient<
                T extends AbstractOAuth2AuthorizationGrantRequest>
        implements OAuth2AccessTokenResponseClient<T> {

    private final OAuth2AccessTokenResponseClient<T> delegate;
    private final OAuth2TokenDiagnosticsLogger diagnosticsLogger;

    DiagnosticOAuth2AccessTokenResponseClient(
            OAuth2AccessTokenResponseClient<T> delegate,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.diagnosticsLogger =
                Objects.requireNonNull(diagnosticsLogger, "diagnosticsLogger must not be null");
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(T authorizationGrantRequest) {
        ClientRegistration registration = authorizationGrantRequest.getClientRegistration();
        try {
            OAuth2AccessTokenResponse response =
                    delegate.getTokenResponse(authorizationGrantRequest);
            OAuth2AccessToken token = response.getAccessToken();
            Set<String> scopes = token.getScopes();
            if (scopes == null || scopes.isEmpty()) {
                scopes = registration.getScopes();
            }
            diagnosticsLogger.tokenRequestSucceeded(registration, token, scopes);
            return response;
        } catch (RuntimeException exception) {
            diagnosticsLogger.tokenRequestFailed(registration, exception);
            throw exception;
        }
    }
}
