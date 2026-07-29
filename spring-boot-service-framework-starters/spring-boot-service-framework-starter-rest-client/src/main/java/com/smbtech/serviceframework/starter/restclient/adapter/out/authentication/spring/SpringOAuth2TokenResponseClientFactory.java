package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.api.oauth2.ClientAssertionContext;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.client.endpoint.AbstractRestClientOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestHeadersConverter;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestParametersConverter;
import org.springframework.security.oauth2.client.endpoint.JwtBearerGrantRequest;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.RestClientJwtBearerTokenResponseClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** Provides spring OAuth2 token response client factory behavior. */
public final class SpringOAuth2TokenResponseClientFactory {

    private final ClientAssertionJwkResolver jwkResolver;
    private final OAuth2TokenDiagnosticsLogger diagnosticsLogger;
    private final Clock clock;
    private final OAuth2ExtensionRegistry extensionRegistry;
    private final ClientAssertionPipeline clientAssertionPipeline;
    private final OAuth2TokenRequestPipeline tokenRequestPipeline;

    /**
     * Creates a spring OAuth2 token response client factory instance.
     *
     * @param jwkResolver jwk resolver value
     * @param clock clock value
     */
    public SpringOAuth2TokenResponseClientFactory(
            ClientAssertionJwkResolver jwkResolver, Clock clock) {
        this(jwkResolver, OAuth2TokenDiagnosticsLogger.disabled(), clock);
    }

    /**
     * Creates a spring OAuth2 token response client factory instance.
     *
     * @param jwkResolver jwk resolver value
     * @param diagnosticsLogger diagnostics logger value
     * @param clock clock value
     */
    public SpringOAuth2TokenResponseClientFactory(
            ClientAssertionJwkResolver jwkResolver,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            Clock clock) {
        this(jwkResolver, diagnosticsLogger, clock, OAuth2ExtensionRegistry.empty());
    }

    /**
     * Creates a spring OAuth2 token response client factory instance.
     *
     * @param jwkResolver jwk resolver value
     * @param diagnosticsLogger diagnostics logger value
     * @param clock clock value
     * @param extensionRegistry extension registry value
     */
    public SpringOAuth2TokenResponseClientFactory(
            ClientAssertionJwkResolver jwkResolver,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            Clock clock,
            OAuth2ExtensionRegistry extensionRegistry) {
        this.jwkResolver = Objects.requireNonNull(jwkResolver, "jwkResolver must not be null");
        this.diagnosticsLogger =
                Objects.requireNonNull(diagnosticsLogger, "diagnosticsLogger must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.extensionRegistry =
                Objects.requireNonNull(extensionRegistry, "extensionRegistry must not be null");
        this.clientAssertionPipeline = new ClientAssertionPipeline(this.extensionRegistry);
        this.tokenRequestPipeline = new OAuth2TokenRequestPipeline(this.extensionRegistry);
    }

    /**
     * Creates client credentials.
     *
     * @return create client credentials result
     */
    public OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest>
            createClientCredentials() {
        RestClientClientCredentialsTokenResponseClient tokenResponseClient =
                new RestClientClientCredentialsTokenResponseClient();
        customizeTokenRequest(tokenResponseClient, clientAuthenticationParametersConverter());
        return new DiagnosticOAuth2AccessTokenResponseClient<>(
                tokenResponseClient, diagnosticsLogger);
    }

    /**
     * Creates JWT bearer.
     *
     * @return create JWT bearer result
     */
    public OAuth2AccessTokenResponseClient<JwtBearerGrantRequest> createJwtBearer() {
        RestClientJwtBearerTokenResponseClient tokenResponseClient =
                new RestClientJwtBearerTokenResponseClient();
        customizeTokenRequest(tokenResponseClient, clientAuthenticationParametersConverter());
        return new DiagnosticOAuth2AccessTokenResponseClient<>(
                tokenResponseClient, diagnosticsLogger);
    }

    /**
     * Creates the converter that signs {@code private_key_jwt} client assertions.
     *
     * @param <T> OAuth2 authorization grant request type
     * @return configured client authentication parameter converter
     */
    public <T extends AbstractOAuth2AuthorizationGrantRequest>
            NimbusJwtClientAuthenticationParametersConverter<T>
                    clientAuthenticationParametersConverter() {
        NimbusJwtClientAuthenticationParametersConverter<T> converter =
                new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver::resolve);
        converter.setJwtClientAssertionCustomizer(
                context -> {
                    String registrationId =
                            context.getAuthorizationGrantRequest()
                                    .getClientRegistration()
                                    .getRegistrationId();
                    RestClientProperties.ClientAssertion assertion =
                            jwkResolver.clientAssertion(registrationId);
                    ClientAssertionContext assertionContext =
                            clientAssertionPipeline.resolve(
                                    context.getAuthorizationGrantRequest(), assertion);

                    assertionContext.headers().forEach(context.getHeaders()::header);
                    context.getClaims()
                            .expiresAt(clock.instant().plus(assertionContext.tokenLifetime()));
                    assertionContext.claims().forEach(context.getClaims()::claim);
                    diagnosticsLogger.clientAssertionCreated(
                            registrationId,
                            assertionContext.tokenLifetime(),
                            assertionContext.claims());
                });
        return converter;
    }

    /**
     * Performs the extension registry operation.
     *
     * @return extension registry result
     */
    public OAuth2ExtensionRegistry extensionRegistry() {
        return extensionRegistry;
    }

    private <T extends AbstractOAuth2AuthorizationGrantRequest> void customizeTokenRequest(
            AbstractRestClientOAuth2AccessTokenResponseClient<T> tokenResponseClient,
            Converter<T, MultiValueMap<String, String>> clientAuthenticationConverter) {
        if (extensionRegistry.tokenRequestCustomizers().isEmpty()) {
            tokenResponseClient.addParametersConverter(clientAuthenticationConverter);
            return;
        }
        DefaultOAuth2TokenRequestParametersConverter<T> defaultParametersConverter =
                new DefaultOAuth2TokenRequestParametersConverter<>();
        tokenResponseClient.setParametersConverter(
                grantRequest -> {
                    MultiValueMap<String, String> parameters =
                            defaultParametersConverter.convert(grantRequest);
                    if (parameters == null) {
                        parameters = new LinkedMultiValueMap<>();
                    }
                    MultiValueMap<String, String> clientAuthenticationParameters =
                            clientAuthenticationConverter.convert(grantRequest);
                    if (clientAuthenticationParameters != null) {
                        parameters.addAll(clientAuthenticationParameters);
                    }
                    MultiValueMap<String, String> resolved =
                            tokenRequestPipeline.resolveParameters(grantRequest, parameters);
                    parameters.keySet().stream()
                            .filter(name -> !resolved.containsKey(name))
                            .forEach(name -> resolved.put(name, List.of()));
                    return resolved;
                });
        DefaultOAuth2TokenRequestHeadersConverter<T> headersConverter =
                new DefaultOAuth2TokenRequestHeadersConverter<>();
        tokenResponseClient.setHeadersConverter(
                grantRequest -> {
                    HttpHeaders headers = headersConverter.convert(grantRequest);
                    return tokenRequestPipeline.resolveHeaders(grantRequest, headers);
                });
    }
}
