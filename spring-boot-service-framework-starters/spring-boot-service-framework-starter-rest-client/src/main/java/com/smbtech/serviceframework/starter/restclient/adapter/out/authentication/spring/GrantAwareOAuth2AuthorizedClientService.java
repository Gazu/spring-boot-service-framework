package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Objects;

public final class GrantAwareOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AuthorizedClientService delegate;
    private final OAuth2TokenCachePolicy cachePolicy;

    public GrantAwareOAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService delegate,
            RestClientProperties properties
    ) {
        this(
                clientRegistrationRepository,
                delegate,
                OAuth2TokenCachePolicy.from(properties)
        );
    }

    GrantAwareOAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService delegate,
            OAuth2TokenCachePolicy cachePolicy
    ) {
        this.clientRegistrationRepository = Objects.requireNonNull(
                clientRegistrationRepository,
                "clientRegistrationRepository must not be null"
        );
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.cachePolicy = Objects.requireNonNull(cachePolicy, "cachePolicy must not be null");
    }

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
            String clientRegistrationId,
            String principalName
    ) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (registration == null || !cachePolicy.isCacheEnabled(registration)) {
            return null;
        }
        return delegate.loadAuthorizedClient(clientRegistrationId, principalName);
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        if (authorizedClient == null || principal == null) {
            return;
        }
        if (cachePolicy.isCacheEnabled(authorizedClient.getClientRegistration())) {
            delegate.saveAuthorizedClient(authorizedClient, principal);
        }
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        delegate.removeAuthorizedClient(clientRegistrationId, principalName);
    }
}
