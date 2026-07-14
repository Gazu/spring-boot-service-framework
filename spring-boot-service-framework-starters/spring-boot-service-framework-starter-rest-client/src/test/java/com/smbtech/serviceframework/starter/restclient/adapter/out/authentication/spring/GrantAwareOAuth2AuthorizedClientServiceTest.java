package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GrantAwareOAuth2AuthorizedClientServiceTest {

    private static final String PRINCIPAL_NAME = "spring-boot-service-framework";

    private final Authentication principal = new TestingAuthenticationToken(PRINCIPAL_NAME, "N/A");

    @Test
    void loadsClientCredentialsWhenEnabled() {
        RecordingOAuth2AuthorizedClientService delegate = new RecordingOAuth2AuthorizedClientService();
        GrantAwareOAuth2AuthorizedClientService service = service(delegate, tokenCache(true, true));
        OAuth2AuthorizedClient authorizedClient = authorizedClient(clientCredentialsRegistration());
        delegate.saveAuthorizedClient(authorizedClient, principal);

        OAuth2AuthorizedClient loaded = service.loadAuthorizedClient("client-credentials-api", PRINCIPAL_NAME);

        assertThat(loaded).isSameAs(authorizedClient);
        assertThat(delegate.loadCalls).isEqualTo(1);
    }

    @Test
    void doesNotLoadClientCredentialsWhenDisabled() {
        RecordingOAuth2AuthorizedClientService delegate = new RecordingOAuth2AuthorizedClientService();
        GrantAwareOAuth2AuthorizedClientService service = service(delegate, tokenCache(false, true));
        delegate.saveAuthorizedClient(authorizedClient(clientCredentialsRegistration()), principal);

        OAuth2AuthorizedClient loaded = service.loadAuthorizedClient("client-credentials-api", PRINCIPAL_NAME);

        assertThat(loaded).isNull();
        assertThat(delegate.loadCalls).isZero();
    }

    @Test
    void savesClientCredentialsWhenEnabled() {
        RecordingOAuth2AuthorizedClientService delegate = new RecordingOAuth2AuthorizedClientService();
        GrantAwareOAuth2AuthorizedClientService service = service(delegate, tokenCache(true, true));
        OAuth2AuthorizedClient authorizedClient = authorizedClient(clientCredentialsRegistration());

        service.saveAuthorizedClient(authorizedClient, principal);

        assertThat(delegate.savedClients).containsExactly(authorizedClient);
    }

    @Test
    void doesNotSaveClientCredentialsWhenDisabled() {
        RecordingOAuth2AuthorizedClientService delegate = new RecordingOAuth2AuthorizedClientService();
        GrantAwareOAuth2AuthorizedClientService service = service(delegate, tokenCache(false, true));

        service.saveAuthorizedClient(authorizedClient(clientCredentialsRegistration()), principal);

        assertThat(delegate.savedClients).isEmpty();
    }

    @Test
    void loadsJwtBearerWhenEnabled() {
        RecordingOAuth2AuthorizedClientService delegate = new RecordingOAuth2AuthorizedClientService();
        GrantAwareOAuth2AuthorizedClientService service = service(delegate, tokenCache(true, true));
        OAuth2AuthorizedClient authorizedClient = authorizedClient(jwtBearerRegistration());
        delegate.saveAuthorizedClient(authorizedClient, principal);

        OAuth2AuthorizedClient loaded = service.loadAuthorizedClient("jwt-bearer-api", PRINCIPAL_NAME);

        assertThat(loaded).isSameAs(authorizedClient);
        assertThat(delegate.loadCalls).isEqualTo(1);
    }

    @Test
    void doesNotLoadJwtBearerWhenDisabled() {
        RecordingOAuth2AuthorizedClientService delegate = new RecordingOAuth2AuthorizedClientService();
        GrantAwareOAuth2AuthorizedClientService service = service(delegate, tokenCache(true, false));
        delegate.saveAuthorizedClient(authorizedClient(jwtBearerRegistration()), principal);

        OAuth2AuthorizedClient loaded = service.loadAuthorizedClient("jwt-bearer-api", PRINCIPAL_NAME);

        assertThat(loaded).isNull();
        assertThat(delegate.loadCalls).isZero();
    }

    @Test
    void savesJwtBearerWhenEnabled() {
        RecordingOAuth2AuthorizedClientService delegate = new RecordingOAuth2AuthorizedClientService();
        GrantAwareOAuth2AuthorizedClientService service = service(delegate, tokenCache(true, true));
        OAuth2AuthorizedClient authorizedClient = authorizedClient(jwtBearerRegistration());

        service.saveAuthorizedClient(authorizedClient, principal);

        assertThat(delegate.savedClients).containsExactly(authorizedClient);
    }

    @Test
    void doesNotSaveJwtBearerWhenDisabled() {
        RecordingOAuth2AuthorizedClientService delegate = new RecordingOAuth2AuthorizedClientService();
        GrantAwareOAuth2AuthorizedClientService service = service(delegate, tokenCache(true, false));

        service.saveAuthorizedClient(authorizedClient(jwtBearerRegistration()), principal);

        assertThat(delegate.savedClients).isEmpty();
    }

    @Test
    void removeAlwaysDelegates() {
        RecordingOAuth2AuthorizedClientService delegate = new RecordingOAuth2AuthorizedClientService();
        GrantAwareOAuth2AuthorizedClientService service = service(delegate, tokenCache(false, false));

        service.removeAuthorizedClient("jwt-bearer-api", PRINCIPAL_NAME);

        assertThat(delegate.removeCalls).isEqualTo(1);
        assertThat(delegate.removedClientRegistrationId).isEqualTo("jwt-bearer-api");
        assertThat(delegate.removedPrincipalName).isEqualTo(PRINCIPAL_NAME);
    }

    @Test
    void doesNotLoadWhenRegistrationIsMissing() {
        RecordingOAuth2AuthorizedClientService delegate = new RecordingOAuth2AuthorizedClientService();
        GrantAwareOAuth2AuthorizedClientService service = new GrantAwareOAuth2AuthorizedClientService(
                new InMemoryClientRegistrationRepository(clientCredentialsRegistration()),
                delegate,
                new OAuth2TokenCachePolicy(tokenCache(true, true))
        );

        OAuth2AuthorizedClient loaded = service.loadAuthorizedClient("missing-api", PRINCIPAL_NAME);

        assertThat(loaded).isNull();
        assertThat(delegate.loadCalls).isZero();
    }

    private GrantAwareOAuth2AuthorizedClientService service(
            RecordingOAuth2AuthorizedClientService delegate,
            RestClientProperties.TokenCache tokenCache
    ) {
        return new GrantAwareOAuth2AuthorizedClientService(
                new InMemoryClientRegistrationRepository(
                        clientCredentialsRegistration(),
                        jwtBearerRegistration()
                ),
                delegate,
                new OAuth2TokenCachePolicy(tokenCache)
        );
    }

    private RestClientProperties.TokenCache tokenCache(boolean clientCredentials, boolean jwtBearer) {
        RestClientProperties.TokenCache tokenCache = new RestClientProperties.TokenCache();
        tokenCache.setClientCredentials(clientCredentials);
        tokenCache.setJwtBearer(jwtBearer);
        return tokenCache;
    }

    private OAuth2AuthorizedClient authorizedClient(ClientRegistration registration) {
        return new OAuth2AuthorizedClient(
                registration,
                PRINCIPAL_NAME,
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token-" + registration.getRegistrationId(),
                        Instant.now(),
                        Instant.now().plusSeconds(60),
                        Set.of("payment.read")
                )
        );
    }

    private ClientRegistration clientCredentialsRegistration() {
        return registration("client-credentials-api", AuthorizationGrantType.CLIENT_CREDENTIALS);
    }

    private ClientRegistration jwtBearerRegistration() {
        return registration("jwt-bearer-api", OAuth2TokenCachePolicy.JWT_BEARER_GRANT);
    }

    private ClientRegistration registration(String registrationId, AuthorizationGrantType grantType) {
        return ClientRegistration
                .withRegistrationId(registrationId)
                .tokenUri("https://auth.example/token")
                .clientId("payments-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(grantType)
                .scope("payment.read")
                .build();
    }

    private static final class RecordingOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

        private final List<OAuth2AuthorizedClient> savedClients = new ArrayList<>();
        private int loadCalls;
        private int removeCalls;
        private String removedClientRegistrationId;
        private String removedPrincipalName;

        @Override
        @SuppressWarnings("unchecked")
        public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
                String clientRegistrationId,
                String principalName
        ) {
            loadCalls++;
            return (T) savedClients.stream()
                    .filter(client -> client.getClientRegistration().getRegistrationId().equals(clientRegistrationId))
                    .filter(client -> client.getPrincipalName().equals(principalName))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
            savedClients.add(authorizedClient);
        }

        @Override
        public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
            removeCalls++;
            removedClientRegistrationId = clientRegistrationId;
            removedPrincipalName = principalName;
        }
    }
}
