package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContextException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

class OAuth2RestClientConfigurationValidationRunnerTest {

    @Test
    void failsStartupWhenValidationHasErrors() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));

        OAuth2RestClientConfigurationValidationRunner runner =
                runner(properties, null, new CapturingLogger());

        assertThatThrownBy(runner::afterSingletonsInstantiated)
                .isInstanceOf(ApplicationContextException.class)
                .hasMessageContaining("Invalid SMBTech REST client OAuth2 configuration")
                .hasMessageContaining("Found 1 error(s) and 0 warning(s)")
                .hasMessageContaining("Errors:")
                .hasMessageContaining("ERROR clients.payments.token-request-id")
                .hasMessageContaining(
                        "references payments-token but no ClientRegistrationRepository is available")
                .hasMessageContaining(
                        "Fix: Set smbtech.rest-clients.clients.payments.token-request-id")
                .hasMessageContaining("Review the YAML paths above under smbtech.rest-clients");
    }

    @Test
    void logsWarningsAndAllowsStartupWhenFailOnWarningsIsDisabled() {
        RestClientProperties properties = new RestClientProperties();
        properties
                .getAuthentication()
                .setJwtBearer(Map.of("unused-token", new RestClientProperties.JwtBearer()));
        CapturingLogger logger = new CapturingLogger();

        runner(properties, null, logger).afterSingletonsInstantiated();

        assertThat(logger.events())
                .singleElement()
                .satisfies(
                        logged -> {
                            assertThat(logged.level()).isEqualTo(LogLevel.WARN);
                            assertThat(logged.event().type())
                                    .isEqualTo(EventType.named("OAUTH2_CONFIGURATION_VALIDATION"));
                            assertThat(logged.event().message())
                                    .isEqualTo("OAuth2 REST client configuration warning");
                            assertThat(logged.event().tags())
                                    .containsExactlyInAnyOrder("oauth2", "configuration");
                            assertThat(logged.event().data())
                                    .containsEntry("severity", "WARNING")
                                    .containsEntry("path", "authentication.jwt-bearer.unused-token")
                                    .containsEntry(
                                            "message",
                                            "is configured but no enabled JWT_BEARER REST client references this registration id")
                                    .containsEntry(
                                            "suggestedFix",
                                            "Add smbtech.rest-clients.authentication.jwt-bearer.unused-token.key-store-id "
                                                    + "for JWT bearer signing, or remove the unused JWT bearer block.");
                        });
    }

    @Test
    void failsStartupWhenOnlyWarningsExistAndFailOnWarningsIsEnabled() {
        RestClientProperties properties = new RestClientProperties();
        properties.getValidation().setFailOnWarnings(true);
        properties
                .getAuthentication()
                .setClientAssertions(
                        Map.of(
                                "unused-client-assertion",
                                new RestClientProperties.ClientAssertion()));

        OAuth2RestClientConfigurationValidationRunner runner =
                runner(properties, null, new CapturingLogger());

        assertThatThrownBy(runner::afterSingletonsInstantiated)
                .isInstanceOf(ApplicationContextException.class)
                .hasMessageContaining("Found 0 error(s) and 1 warning(s)")
                .hasMessageContaining("Warnings:")
                .hasMessageContaining(
                        "WARNING authentication.client-assertions.unused-client-assertion")
                .hasMessageContaining(
                        "Fix: Add smbtech.rest-clients.authentication.client-assertions."
                                + "unused-client-assertion.key-store-id");
    }

    private OAuth2RestClientConfigurationValidationRunner runner(
            RestClientProperties properties,
            ClientRegistrationRepository clientRegistrationRepository,
            StructuredLogger logger) {
        return new OAuth2RestClientConfigurationValidationRunner(
                properties,
                new OAuth2RestClientConfigurationValidator(),
                clientRegistrationRepositoryProvider(clientRegistrationRepository),
                logger);
    }

    private ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new ObjectProvider<>() {
            @Override
            public ClientRegistrationRepository getIfAvailable() {
                return clientRegistrationRepository;
            }
        };
    }

    private RestClientProperties.Client OAuth2Client(
            AuthenticationType authenticationType, String tokenRequestId) {
        RestClientProperties.Client client = new RestClientProperties.Client();
        client.setAuthenticationType(authenticationType);
        client.setTokenRequestId(tokenRequestId);
        return client;
    }

    private static final class CapturingLogger implements StructuredLogger {
        private final List<LoggedEvent> events = new ArrayList<>();

        @Override
        public boolean isEnabled(LogLevel level, EventType eventType) {
            return true;
        }

        @Override
        public void log(LogLevel level, StructuredEvent event) {
            events.add(new LoggedEvent(level, event));
        }

        private List<LoggedEvent> events() {
            return events;
        }
    }

    private record LoggedEvent(LogLevel level, StructuredEvent event) {}
}
