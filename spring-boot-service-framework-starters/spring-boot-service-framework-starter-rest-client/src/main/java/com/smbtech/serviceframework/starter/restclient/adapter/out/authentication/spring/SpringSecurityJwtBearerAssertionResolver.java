package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.httpclient.domain.JwtBearerDefinition;
import com.smbtech.serviceframework.httpclient.domain.TokenRequestDefinition;
import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Provides spring security JWT bearer assertion resolver behavior. */
final class SpringSecurityJwtBearerAssertionResolver {

    private final RestClientProperties properties;
    private final SigningJwkResolver signingJwkResolver;
    private final JwtBearerCustomClaimsResolver customClaimsResolver;
    private final OAuth2TokenDiagnosticsLogger diagnosticsLogger;
    private final Clock clock;
    private final OAuth2ExtensionRegistry extensionRegistry;
    private final JwtBearerClaimsPipeline jwtBearerClaimsPipeline;

    /**
     * Creates a spring security JWT bearer assertion resolver instance.
     *
     * @param properties properties value
     * @param signingJwkResolver signing jwk resolver value
     * @param clock clock value
     */
    public SpringSecurityJwtBearerAssertionResolver(
            RestClientProperties properties, SigningJwkResolver signingJwkResolver, Clock clock) {
        this(
                properties,
                signingJwkResolver,
                new JwtBearerCustomClaimsResolver(),
                OAuth2TokenDiagnosticsLogger.disabled(),
                clock);
    }

    /**
     * Creates a spring security JWT bearer assertion resolver instance.
     *
     * @param properties properties value
     * @param signingJwkResolver signing jwk resolver value
     * @param diagnosticsLogger diagnostics logger value
     * @param clock clock value
     */
    public SpringSecurityJwtBearerAssertionResolver(
            RestClientProperties properties,
            SigningJwkResolver signingJwkResolver,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            Clock clock) {
        this(
                properties,
                signingJwkResolver,
                diagnosticsLogger,
                clock,
                OAuth2ExtensionRegistry.empty());
    }

    /**
     * Creates a spring security JWT bearer assertion resolver instance.
     *
     * @param properties properties value
     * @param signingJwkResolver signing jwk resolver value
     * @param diagnosticsLogger diagnostics logger value
     * @param clock clock value
     * @param extensionRegistry extension registry value
     */
    public SpringSecurityJwtBearerAssertionResolver(
            RestClientProperties properties,
            SigningJwkResolver signingJwkResolver,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            Clock clock,
            OAuth2ExtensionRegistry extensionRegistry) {
        this(
                properties,
                signingJwkResolver,
                new JwtBearerCustomClaimsResolver(),
                diagnosticsLogger,
                clock,
                extensionRegistry);
    }

    SpringSecurityJwtBearerAssertionResolver(
            RestClientProperties properties,
            SigningJwkResolver signingJwkResolver,
            JwtBearerCustomClaimsResolver customClaimsResolver,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            Clock clock) {
        this(
                properties,
                signingJwkResolver,
                customClaimsResolver,
                diagnosticsLogger,
                clock,
                OAuth2ExtensionRegistry.empty());
    }

    SpringSecurityJwtBearerAssertionResolver(
            RestClientProperties properties,
            SigningJwkResolver signingJwkResolver,
            JwtBearerCustomClaimsResolver customClaimsResolver,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            Clock clock,
            OAuth2ExtensionRegistry extensionRegistry) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.signingJwkResolver =
                Objects.requireNonNull(signingJwkResolver, "signingJwkResolver must not be null");
        this.customClaimsResolver =
                Objects.requireNonNull(
                        customClaimsResolver, "customClaimsResolver must not be null");
        this.diagnosticsLogger =
                Objects.requireNonNull(diagnosticsLogger, "diagnosticsLogger must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.extensionRegistry =
                Objects.requireNonNull(extensionRegistry, "extensionRegistry must not be null");
        this.jwtBearerClaimsPipeline =
                new JwtBearerClaimsPipeline(this.extensionRegistry, blockedJwtBearerClaims());
    }

    /**
     * Creates assertion.
     *
     * @param context context value
     * @return create assertion result
     */
    public Jwt createAssertion(OAuth2AuthorizationContext context) {
        return createAssertion(toTokenRequestDefinition(context.getClientRegistration()), context);
    }

    /**
     * Creates assertion.
     *
     * @param definition definition value
     * @return create assertion result
     */
    public Jwt createAssertion(TokenRequestDefinition definition) {
        return createAssertion(definition, null);
    }

    /**
     * Performs the extension registry operation.
     *
     * @return extension registry result
     */
    public OAuth2ExtensionRegistry extensionRegistry() {
        return extensionRegistry;
    }

    private Jwt createAssertion(
            TokenRequestDefinition definition, OAuth2AuthorizationContext context) {
        if (definition.grantType() != GrantType.JWT_BEARER) {
            throw new HttpClientAuthenticationException(
                    "Token request is not JWT_BEARER: " + definition.id());
        }
        if (!definition.jwtBearer().isConfigured()) {
            throw new HttpClientAuthenticationException(
                    "jwtBearer.keyStoreId is required for token request: " + definition.id());
        }
        try {
            JWK signingJwk = signingJwkResolver.resolve(definition.jwtBearer().keyStoreId());
            Jwt jwt =
                    jwtEncoder(signingJwk)
                            .encode(
                                    JwtEncoderParameters.from(
                                            jwsHeader(), claims(definition, context)));
            diagnosticsLogger.jwtBearerAssertionCreated(definition.id(), jwt);
            return jwt;
        } catch (HttpClientAuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new HttpClientAuthenticationException(
                    "Unable to create JWT bearer assertion for token request: " + definition.id(),
                    exception);
        }
    }

    private TokenRequestDefinition toTokenRequestDefinition(ClientRegistration registration) {
        String registrationId = registration.getRegistrationId();
        RestClientProperties.JwtBearer jwtBearer = jwtBearer(registrationId);
        String tokenUri = registration.getProviderDetails().getTokenUri();
        if (tokenUri == null || tokenUri.isBlank()) {
            throw new HttpClientAuthenticationException(
                    "token-uri is required for OAuth2 registration: " + registrationId);
        }

        return new TokenRequestDefinition(
                registrationId,
                URI.create(tokenUri),
                GrantType.JWT_BEARER,
                com.smbtech.serviceframework.httpclient.domain.ClientAuthenticationMethod.NONE,
                registration.getClientId(),
                Objects.requireNonNullElse(registration.getClientSecret(), ""),
                new LinkedHashSet<>(registration.getScopes()),
                Duration.ofSeconds(30),
                new JwtBearerDefinition(
                        jwtBearer.getKeyStoreId(),
                        jwtBearer.getIssuer(),
                        jwtBearer.getSubject(),
                        jwtBearer.getAudience(),
                        jwtBearer.getTokenLifetime(),
                        jwtBearer.getCustomClaims()));
    }

    private RestClientProperties.JwtBearer jwtBearer(String registrationId) {
        RestClientProperties.Authentication authentication =
                Objects.requireNonNullElseGet(
                        properties.getAuthentication(), RestClientProperties.Authentication::new);
        RestClientProperties.JwtBearer jwtBearer =
                authentication.getJwtBearer().get(registrationId);
        if (jwtBearer == null) {
            throw new HttpClientAuthenticationException(
                    "jwt-bearer configuration not found for OAuth2 registration: "
                            + registrationId);
        }
        return jwtBearer;
    }

    private NimbusJwtEncoder jwtEncoder(JWK signingJwk) {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(signingJwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    private JwsHeader jwsHeader() {
        return JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
    }

    private JwtClaimsSet claims(
            TokenRequestDefinition definition, OAuth2AuthorizationContext context) {
        JwtBearerDefinition jwtBearer = definition.jwtBearer();
        Instant now = clock.instant();
        String issuer = defaultIfBlank(jwtBearer.issuer(), definition.clientId());
        JwtClaimsSet.Builder claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(defaultIfBlank(jwtBearer.subject(), issuer))
                        .audience(
                                List.of(
                                        defaultIfBlank(
                                                jwtBearer.audience(),
                                                definition.tokenUri().toString())))
                        .issuedAt(now)
                        .expiresAt(now.plus(jwtBearer.tokenLifetime()))
                        .id(UUID.randomUUID().toString());

        jwtBearerClaimsPipeline.resolveForAssertion(definition, context).forEach(claims::claim);

        return claims.build();
    }

    private Set<String> blockedJwtBearerClaims() {
        RestClientProperties.RequestContext requestContext =
                properties.getRequestContext() == null
                        ? new RestClientProperties.RequestContext()
                        : properties.getRequestContext();
        return Set.copyOf(
                Objects.requireNonNullElse(requestContext.getBlockedJwtBearerClaims(), Set.of()));
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
