package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Creates Spring Security token clients used for client_credentials.
 */
public final class SpringClientCredentialsTokenResponseClientFactory {

    private static final Set<String> REGISTERED_CLAIMS = Set.of("iss", "sub", "aud", "jti", "iat", "exp", "nbf");

    private final ClientAssertionJwkResolver jwkResolver;
    private final Clock clock;

    public SpringClientCredentialsTokenResponseClientFactory(
            ClientAssertionJwkResolver jwkResolver,
            Clock clock
    ) {
        this.jwkResolver = Objects.requireNonNull(jwkResolver, "jwkResolver must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public RestClientClientCredentialsTokenResponseClient create() {
        RestClientClientCredentialsTokenResponseClient tokenResponseClient =
                new RestClientClientCredentialsTokenResponseClient();

        NimbusJwtClientAuthenticationParametersConverter<OAuth2ClientCredentialsGrantRequest> converter =
                new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver::resolve);
        converter.setJwtClientAssertionCustomizer(context -> {
            String registrationId = context.getAuthorizationGrantRequest()
                    .getClientRegistration()
                    .getRegistrationId();
            RestClientProperties.ClientAssertion assertion = jwkResolver.clientAssertion(registrationId);

            context.getClaims().expiresAt(clock.instant().plus(assertion.getTokenLifetime()));
            for (Map.Entry<String, Object> entry : assertion.getCustomClaims().entrySet()) {
                if (isCustomClaim(entry)) {
                    context.getClaims().claim(entry.getKey(), entry.getValue());
                }
            }
        });
        tokenResponseClient.addParametersConverter(converter);
        return tokenResponseClient;
    }

    private boolean isCustomClaim(Map.Entry<String, Object> entry) {
        return entry.getKey() != null
                && !entry.getKey().isBlank()
                && !REGISTERED_CLAIMS.contains(entry.getKey())
                && entry.getValue() != null;
    }
}
