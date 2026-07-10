package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Adapter around Spring Security's {@link ClientRegistrationRepository}.
 *
 * <p>The REST client starter uses SMBTech properties to decide which outbound
 * client needs a token. The token request itself is moving toward Spring
 * Security OAuth2 Client registrations. This resolver is the bridge between
 * the framework client name/credential-token-requestor-id and Spring's
 * registration model.</p>
 */
public final class SpringClientRegistrationResolver {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public SpringClientRegistrationResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = Objects.requireNonNull(
                clientRegistrationRepository,
                "clientRegistrationRepository must not be null"
        );
    }

    public Optional<ClientRegistration> findByRegistrationId(String registrationId) {
        String normalized = normalize(registrationId);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(clientRegistrationRepository.findByRegistrationId(normalized));
    }

    public ClientRegistration requireByRegistrationId(String registrationId) {
        String normalized = normalize(registrationId);
        if (normalized.isBlank()) {
            throw new AuthenticationException("OAuth2 client registration id must not be blank");
        }
        return findByRegistrationId(normalized)
                .orElseThrow(() -> new AuthenticationException(
                        "OAuth2 client registration not found: " + normalized
                ));
    }

    private String normalize(String registrationId) {
        return Objects.requireNonNullElse(registrationId, "").trim();
    }
}
