package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Adapter around Spring Security's {@link ClientRegistrationRepository}.
 *
 * <p>The REST client starter uses SMBTech properties to decide which outbound client needs a token.
 * The token request itself is moving toward Spring Security OAuth2 Client registrations. This
 * resolver is the bridge between the framework client name/token-request-id and Spring's
 * registration model.
 */
final class SpringClientRegistrationResolver {

    private final ClientRegistrationRepository clientRegistrationRepository;

    /**
     * Creates a spring client registration resolver instance.
     *
     * @param clientRegistrationRepository client registration repository value
     */
    public SpringClientRegistrationResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository =
                Objects.requireNonNull(
                        clientRegistrationRepository,
                        "clientRegistrationRepository must not be null");
    }

    /**
     * Finds by registration id.
     *
     * @param registrationId registration id value
     * @return find by registration id result
     */
    public Optional<ClientRegistration> findByRegistrationId(String registrationId) {
        String normalized = normalize(registrationId);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(clientRegistrationRepository.findByRegistrationId(normalized));
    }

    /**
     * Performs the require by registration id operation.
     *
     * @param registrationId registration id value
     * @return require by registration id result
     */
    public ClientRegistration requireByRegistrationId(String registrationId) {
        String normalized = normalize(registrationId);
        if (normalized.isBlank()) {
            throw new HttpClientAuthenticationException(
                    "OAuth2 client registration id must not be blank");
        }
        return findByRegistrationId(normalized)
                .orElseThrow(
                        () ->
                                new HttpClientAuthenticationException(
                                        "OAuth2 client registration not found: " + normalized));
    }

    private String normalize(String registrationId) {
        return Objects.requireNonNullElse(registrationId, "").trim();
    }
}
