package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

class OAuth2TokenDiagnosticsLoggerTest {

    @Test
    void doesNotEmitEventsWhenDiagnosticsAreDisabled() {
        RecordingStructuredLogger structuredLogger = new RecordingStructuredLogger();
        OAuth2TokenDiagnosticsLogger diagnosticsLogger =
                new OAuth2TokenDiagnosticsLogger(
                        structuredLogger,
                        new RestClientProperties.Diagnostics(),
                        new OAuth2TokenDiagnosticSanitizer());

        diagnosticsLogger.tokenRequestStarted(clientCredentialsRegistration(), Map.of());

        assertThat(structuredLogger.events).isEmpty();
    }

    @Test
    void emitsTokenRequestSucceededWithoutTokenPreviewByDefault() {
        RecordingStructuredLogger structuredLogger = new RecordingStructuredLogger();
        OAuth2TokenDiagnosticsLogger diagnosticsLogger =
                new OAuth2TokenDiagnosticsLogger(
                        structuredLogger,
                        enabledDiagnostics(),
                        new OAuth2TokenDiagnosticSanitizer());

        diagnosticsLogger.tokenRequestSucceeded(
                clientCredentialsRegistration(),
                accessToken("very-sensitive-access-token-value"),
                Set.of("payment.read"));

        assertThat(structuredLogger.events).hasSize(1);
        StructuredEvent event = structuredLogger.events.getFirst();
        assertThat(event.type().value()).isEqualTo("OAUTH2_TOKEN_DIAGNOSTIC");
        assertThat(event.data())
                .containsEntry("event", "TOKEN_REQUEST_SUCCEEDED")
                .containsEntry("registrationId", "payments-token")
                .containsEntry("grantType", "client_credentials")
                .doesNotContainKey("accessTokenPreview");
        assertThat(event.data().toString()).doesNotContain("very-sensitive-access-token-value");
    }

    @Test
    void emitsOnlyTruncatedTokenPreviewWhenEnabled() {
        RecordingStructuredLogger structuredLogger = new RecordingStructuredLogger();
        RestClientProperties.Diagnostics properties = enabledDiagnostics();
        properties.setIncludeTokenPreview(true);
        properties.setTokenPreviewLength(12);
        OAuth2TokenDiagnosticsLogger diagnosticsLogger =
                new OAuth2TokenDiagnosticsLogger(
                        structuredLogger, properties, new OAuth2TokenDiagnosticSanitizer());

        diagnosticsLogger.tokenRequestSucceeded(
                clientCredentialsRegistration(),
                accessToken("very-sensitive-access-token-value"),
                Set.of("payment.read"));

        assertThat(structuredLogger.events.getFirst().data())
                .containsEntry("accessTokenPreview", "very-sensiti...<redacted>");
        assertThat(structuredLogger.events.getFirst().data().toString())
                .doesNotContain("very-sensitive-access-token-value");
    }

    @Test
    void redactsSensitiveCustomClaims() {
        RecordingStructuredLogger structuredLogger = new RecordingStructuredLogger();
        RestClientProperties.Diagnostics properties = enabledDiagnostics();
        properties.setIncludeClaims(true);
        OAuth2TokenDiagnosticsLogger diagnosticsLogger =
                new OAuth2TokenDiagnosticsLogger(
                        structuredLogger, properties, new OAuth2TokenDiagnosticSanitizer());

        diagnosticsLogger.clientAssertionCreated(
                "payments-token",
                Duration.ofSeconds(60),
                Map.of(
                        "customer_id", "17952397-3",
                        "client_secret", "sensitive-secret",
                        "nested", Map.of("api_key", "sensitive-key")));

        assertThat(structuredLogger.events.getFirst().data().toString())
                .contains("17952397-3")
                .doesNotContain("sensitive-secret")
                .doesNotContain("sensitive-key");
    }

    @Test
    void redactsSensitiveExceptionMessages() {
        RecordingStructuredLogger structuredLogger = new RecordingStructuredLogger();
        OAuth2TokenDiagnosticsLogger diagnosticsLogger =
                new OAuth2TokenDiagnosticsLogger(
                        structuredLogger,
                        enabledDiagnostics(),
                        new OAuth2TokenDiagnosticSanitizer());

        diagnosticsLogger.tokenRequestFailed(
                clientCredentialsRegistration(),
                new IllegalStateException(
                        "invalid token=abc.def.ghi and client_secret=sensitive-secret"));

        assertThat(structuredLogger.events.getFirst().data().get("message").toString())
                .doesNotContain("abc.def.ghi")
                .doesNotContain("sensitive-secret");
    }

    private RestClientProperties.Diagnostics enabledDiagnostics() {
        RestClientProperties.Diagnostics properties = new RestClientProperties.Diagnostics();
        properties.setEnabled(true);
        return properties;
    }

    private ClientRegistration clientCredentialsRegistration() {
        return ClientRegistration.withRegistrationId("payments-token")
                .tokenUri("https://auth.example/oauth2/token")
                .clientId("payments-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("payment.read")
                .build();
    }

    private OAuth2AccessToken accessToken(String value) {
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                value,
                Instant.now(),
                Instant.now().plusSeconds(300),
                Set.of("payment.read"));
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
