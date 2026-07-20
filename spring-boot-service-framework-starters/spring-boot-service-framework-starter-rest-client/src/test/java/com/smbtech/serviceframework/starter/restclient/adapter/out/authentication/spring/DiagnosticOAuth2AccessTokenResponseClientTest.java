package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

class DiagnosticOAuth2AccessTokenResponseClientTest {

    @Test
    void emitsAccessTokenPreviewForSpringTokenEndpointResponses() {
        RecordingStructuredLogger structuredLogger = new RecordingStructuredLogger();
        RestClientProperties.Diagnostics properties = new RestClientProperties.Diagnostics();
        properties.setEnabled(true);
        properties.setIncludeTokenPreview(true);
        properties.setTokenPreviewLength(12);
        OAuth2TokenDiagnosticsLogger diagnosticsLogger =
                new OAuth2TokenDiagnosticsLogger(
                        structuredLogger, properties, new OAuth2TokenDiagnosticSanitizer());
        DiagnosticOAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> client =
                new DiagnosticOAuth2AccessTokenResponseClient<>(
                        request ->
                                OAuth2AccessTokenResponse.withToken(
                                                "very-sensitive-access-token-value")
                                        .tokenType(OAuth2AccessToken.TokenType.BEARER)
                                        .expiresIn(300)
                                        .scopes(Set.of("payment.read"))
                                        .build(),
                        diagnosticsLogger);

        client.getTokenResponse(
                new OAuth2ClientCredentialsGrantRequest(clientCredentialsRegistration()));

        assertThat(structuredLogger.events).hasSize(1);
        assertThat(structuredLogger.events.getFirst().data())
                .containsEntry("event", "TOKEN_REQUEST_SUCCEEDED")
                .containsEntry("accessTokenPreview", "very-sensiti...<redacted>");
        assertThat(structuredLogger.events.getFirst().data().toString())
                .doesNotContain("very-sensitive-access-token-value");
    }

    private ClientRegistration clientCredentialsRegistration() {
        return ClientRegistration.withRegistrationId("payments-token")
                .tokenUri("https://auth.example/oauth2/token")
                .clientId("payments-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("payment.read")
                .build();
    }

    private static final class RecordingStructuredLogger implements StructuredLogger {
        private final List<StructuredEvent> events = new ArrayList<>();

        @Override
        public boolean isEnabled(LogLevel level, EventType eventType) {
            return true;
        }

        @Override
        public void log(LogLevel level, StructuredEvent event) {
            events.add(event);
        }
    }
}
