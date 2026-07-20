package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.ClientAuthenticationMethod;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.ClientAssertionContext;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

final class ClientAssertionPipeline {

    private static final Set<String> REGISTERED_CLAIMS =
            Set.of("iss", "sub", "aud", "jti", "iat", "exp", "nbf");

    private final OAuth2ExtensionRegistry extensionRegistry;

    ClientAssertionPipeline(OAuth2ExtensionRegistry extensionRegistry) {
        this.extensionRegistry =
                Objects.requireNonNullElseGet(extensionRegistry, OAuth2ExtensionRegistry::empty);
    }

    ClientAssertionContext resolve(
            AbstractOAuth2AuthorizationGrantRequest authorizationGrantRequest,
            RestClientProperties.ClientAssertion assertion) {
        ClientRegistration registration = authorizationGrantRequest.getClientRegistration();
        ClientAssertionContext context =
                new ClientAssertionContext(
                        registration.getRegistrationId(),
                        registration.getClientId(),
                        tokenUri(registration),
                        ClientAuthenticationMethod.PRIVATE_KEY_JWT,
                        assertion.getTokenLifetime(),
                        Map.of(),
                        sanitizeClaims(assertion.getCustomClaims()));

        for (var customizer : extensionRegistry.clientAssertionCustomizers()) {
            context =
                    sanitize(
                            Objects.requireNonNull(
                                    customizer.customize(context),
                                    "ClientAssertionCustomizer must not return null"));
        }
        return context;
    }

    private ClientAssertionContext sanitize(ClientAssertionContext context) {
        return new ClientAssertionContext(
                context.registrationId(),
                context.clientId(),
                context.tokenUri(),
                context.clientAuthenticationMethod(),
                context.tokenLifetime(),
                sanitizeHeaders(context.headers()),
                sanitizeClaims(context.claims()));
    }

    private Map<String, Object> sanitizeHeaders(Map<String, Object> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        headers.forEach(
                (name, value) -> {
                    if (name != null && !name.isBlank() && value != null) {
                        sanitized.put(name.trim(), value);
                    }
                });
        return Map.copyOf(sanitized);
    }

    private Map<String, Object> sanitizeClaims(Map<String, Object> claims) {
        if (claims == null || claims.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        claims.forEach(
                (name, value) -> {
                    if (name != null
                            && !name.isBlank()
                            && value != null
                            && !isRegisteredClaim(name)) {
                        sanitized.put(name.trim(), value);
                    }
                });
        return Map.copyOf(sanitized);
    }

    private boolean isRegisteredClaim(String name) {
        return REGISTERED_CLAIMS.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    private URI tokenUri(ClientRegistration registration) {
        String tokenUri = registration.getProviderDetails().getTokenUri();
        if (tokenUri == null || tokenUri.isBlank()) {
            return null;
        }
        return URI.create(tokenUri);
    }
}
