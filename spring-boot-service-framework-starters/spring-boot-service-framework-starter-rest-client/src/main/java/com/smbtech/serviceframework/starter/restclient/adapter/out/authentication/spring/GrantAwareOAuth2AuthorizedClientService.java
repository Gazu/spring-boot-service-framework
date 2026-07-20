package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/** Provides grant aware OAuth2 authorized client service behavior. */
public final class GrantAwareOAuth2AuthorizedClientService
        implements OAuth2AuthorizedClientService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AuthorizedClientService delegate;
    private final OAuth2TokenCachePolicy cachePolicy;
    private final OAuth2TokenDiagnosticsLogger diagnosticsLogger;

    /**
     * Creates a grant aware OAuth2 authorized client service instance.
     *
     * @param clientRegistrationRepository client registration repository value
     * @param delegate delegate value
     * @param properties properties value
     */
    public GrantAwareOAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService delegate,
            RestClientProperties properties) {
        this(
                clientRegistrationRepository,
                delegate,
                OAuth2TokenCachePolicy.from(properties),
                OAuth2TokenDiagnosticsLogger.disabled());
    }

    /**
     * Creates a grant aware OAuth2 authorized client service instance.
     *
     * @param clientRegistrationRepository client registration repository value
     * @param delegate delegate value
     * @param properties properties value
     * @param diagnosticsLogger diagnostics logger value
     */
    public GrantAwareOAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService delegate,
            RestClientProperties properties,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger) {
        this(
                clientRegistrationRepository,
                delegate,
                OAuth2TokenCachePolicy.from(properties),
                diagnosticsLogger);
    }

    GrantAwareOAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService delegate,
            OAuth2TokenCachePolicy cachePolicy) {
        this(
                clientRegistrationRepository,
                delegate,
                cachePolicy,
                OAuth2TokenDiagnosticsLogger.disabled());
    }

    GrantAwareOAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService delegate,
            OAuth2TokenCachePolicy cachePolicy,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger) {
        this.clientRegistrationRepository =
                Objects.requireNonNull(
                        clientRegistrationRepository,
                        "clientRegistrationRepository must not be null");
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.cachePolicy = Objects.requireNonNull(cachePolicy, "cachePolicy must not be null");
        this.diagnosticsLogger =
                Objects.requireNonNull(diagnosticsLogger, "diagnosticsLogger must not be null");
    }

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
            String clientRegistrationId, String principalName) {
        ClientRegistration registration =
                clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (registration == null) {
            return null;
        }
        if (!cachePolicy.isCacheEnabled(registration)) {
            diagnosticsLogger.tokenCacheSkipped(registration, principalName);
            return null;
        }
        T authorizedClient = delegate.loadAuthorizedClient(clientRegistrationId, principalName);
        if (authorizedClient == null) {
            diagnosticsLogger.tokenCacheMiss(registration, principalName);
            return null;
        }
        diagnosticsLogger.tokenCacheHit(
                registration,
                principalName,
                new OAuth2TokenDiagnosticsLogger.OAuth2AuthorizedClientDetails(
                        authorizedClient.getAccessToken() == null
                                ? null
                                : authorizedClient.getAccessToken().getExpiresAt()));
        return authorizedClient;
    }

    @Override
    public void saveAuthorizedClient(
            OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        if (authorizedClient == null || principal == null) {
            return;
        }
        if (cachePolicy.isCacheEnabled(authorizedClient.getClientRegistration())) {
            delegate.saveAuthorizedClient(authorizedClient, principal);
            diagnosticsLogger.tokenCacheSaved(
                    authorizedClient.getClientRegistration(),
                    principal.getName(),
                    new OAuth2TokenDiagnosticsLogger.OAuth2AuthorizedClientDetails(
                            authorizedClient.getAccessToken() == null
                                    ? null
                                    : authorizedClient.getAccessToken().getExpiresAt()));
        } else {
            diagnosticsLogger.tokenCacheSkipped(
                    authorizedClient.getClientRegistration(), principal.getName());
        }
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        delegate.removeAuthorizedClient(clientRegistrationId, principalName);
    }
}
