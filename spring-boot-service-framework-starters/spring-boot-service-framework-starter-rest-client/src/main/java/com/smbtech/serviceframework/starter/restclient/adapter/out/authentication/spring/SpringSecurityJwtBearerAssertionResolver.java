package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.httpclient.domain.JwtBearerDefinition;
import com.smbtech.serviceframework.httpclient.domain.TokenRequestDefinition;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SpringSecurityJwtBearerAssertionResolver {

    private final RestClientProperties properties;
    private final SigningJwkResolver signingJwkResolver;
    private final JwtBearerCustomClaimsResolver customClaimsResolver;
    private final Clock clock;

    public SpringSecurityJwtBearerAssertionResolver(
            RestClientProperties properties,
            SigningJwkResolver signingJwkResolver,
            Clock clock
    ) {
        this(properties, signingJwkResolver, new JwtBearerCustomClaimsResolver(), clock);
    }

    SpringSecurityJwtBearerAssertionResolver(
            RestClientProperties properties,
            SigningJwkResolver signingJwkResolver,
            JwtBearerCustomClaimsResolver customClaimsResolver,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.signingJwkResolver = Objects.requireNonNull(signingJwkResolver, "signingJwkResolver must not be null");
        this.customClaimsResolver = Objects.requireNonNull(
                customClaimsResolver,
                "customClaimsResolver must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Jwt createAssertion(OAuth2AuthorizationContext context) {
        return createAssertion(toTokenRequestDefinition(context.getClientRegistration()), context);
    }

    public Jwt createAssertion(TokenRequestDefinition definition) {
        return createAssertion(definition, null);
    }

    private Jwt createAssertion(TokenRequestDefinition definition, OAuth2AuthorizationContext context) {
        if (definition.grantType() != GrantType.JWT_BEARER) {
            throw new AuthenticationException("Token request is not JWT_BEARER: " + definition.id());
        }
        if (!definition.jwtBearer().isConfigured()) {
            throw new AuthenticationException("jwtBearer.keyStoreId is required for token request: " + definition.id());
        }
        try {
            return jwtEncoder(definition.jwtBearer())
                    .encode(JwtEncoderParameters.from(jwsHeader(), claims(definition, context)));
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthenticationException(
                    "Unable to create JWT bearer assertion for token request: " + definition.id(),
                    exception
            );
        }
    }

    private TokenRequestDefinition toTokenRequestDefinition(ClientRegistration registration) {
        String registrationId = registration.getRegistrationId();
        RestClientProperties.JwtBearer jwtBearer = jwtBearer(registrationId);
        String tokenUri = registration.getProviderDetails().getTokenUri();
        if (tokenUri == null || tokenUri.isBlank()) {
            throw new AuthenticationException("token-uri is required for OAuth2 registration: " + registrationId);
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
                        jwtBearer.getCustomClaims()
                )
        );
    }

    private RestClientProperties.JwtBearer jwtBearer(String registrationId) {
        RestClientProperties.Authentication authentication = Objects.requireNonNullElseGet(
                properties.getAuthentication(),
                RestClientProperties.Authentication::new
        );
        RestClientProperties.JwtBearer jwtBearer = authentication.getJwtBearer().get(registrationId);
        if (jwtBearer == null) {
            throw new AuthenticationException(
                    "jwt-bearer configuration not found for OAuth2 registration: " + registrationId
            );
        }
        return jwtBearer;
    }

    private NimbusJwtEncoder jwtEncoder(JwtBearerDefinition definition) {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(
                new JWKSet(signingJwkResolver.resolve(definition.keyStoreId()))
        );
        return new NimbusJwtEncoder(jwkSource);
    }

    private JwsHeader jwsHeader() {
        return JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .build();
    }

    private JwtClaimsSet claims(TokenRequestDefinition definition, OAuth2AuthorizationContext context) {
        JwtBearerDefinition jwtBearer = definition.jwtBearer();
        Instant now = clock.instant();
        String issuer = defaultIfBlank(jwtBearer.issuer(), definition.clientId());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(defaultIfBlank(jwtBearer.subject(), issuer))
                .audience(List.of(defaultIfBlank(jwtBearer.audience(), definition.tokenUri().toString())))
                .issuedAt(now)
                .expiresAt(now.plus(jwtBearer.tokenLifetime()))
                .id(UUID.randomUUID().toString());

        if (context == null) {
            customClaimsResolver.resolve(jwtBearer.customClaims(), java.util.Map.of())
                    .forEach(claims::claim);
        } else {
            customClaimsResolver.resolve(jwtBearer, context)
                    .forEach(claims::claim);
        }

        return claims.build();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
