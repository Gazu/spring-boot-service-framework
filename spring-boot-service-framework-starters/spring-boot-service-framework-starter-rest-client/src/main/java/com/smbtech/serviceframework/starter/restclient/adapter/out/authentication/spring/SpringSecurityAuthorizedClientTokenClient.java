package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.httpclient.service.ScopeValidator;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.api.JwtBearerTokenRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class SpringSecurityAuthorizedClientTokenClient implements AccessTokenProvider, AccessTokenClient {

    private static final String PRINCIPAL_NAME = "spring-boot-service-framework";
    private static final AuthorizationGrantType JWT_BEARER_GRANT =
            new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer");

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final ScopeValidator scopeValidator;
    private final Authentication principal;

    public SpringSecurityAuthorizedClientTokenClient(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientManager authorizedClientManager,
            ScopeValidator scopeValidator
    ) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizedClientManager = authorizedClientManager;
        this.scopeValidator = Objects.requireNonNull(scopeValidator, "scopeValidator must not be null");
        this.principal = UsernamePasswordAuthenticationToken.authenticated(PRINCIPAL_NAME, "N/A", List.of());
    }

    @Override
    public String getAccessToken(String credentialTokenRequestorId, String scopes) {
        AccessToken token = authorizeForSupportedTokenRequest(
                normalize(credentialTokenRequestorId, "credentialTokenRequestorId")
        );
        scopeValidator.validate(scopes, token.scopes());
        return token.value();
    }

    @Override
    public AccessToken clientCredentials(String tokenRequestId) {
        return clientCredentials(tokenRequestId, "");
    }

    @Override
    public AccessToken clientCredentials(String tokenRequestId, String expectedScopes) {
        AccessToken token = authorizeForGrant(
                normalize(tokenRequestId, "tokenRequestId"),
                AuthorizationGrantType.CLIENT_CREDENTIALS,
                "client_credentials",
                Map.of()
        );
        scopeValidator.validate(expectedScopes, token.scopes());
        return token;
    }

    @Override
    public AccessToken jwtBearer(String tokenRequestId) {
        return jwtBearer(tokenRequestId, "");
    }

    @Override
    public AccessToken jwtBearer(String tokenRequestId, String expectedScopes) {
        return jwtBearer(new JwtBearerTokenRequest(tokenRequestId, expectedScopes, Map.of()));
    }

    @Override
    public AccessToken jwtBearer(String tokenRequestId, Map<String, Object> customClaims) {
        return jwtBearer(new JwtBearerTokenRequest(tokenRequestId, customClaims));
    }

    @Override
    public AccessToken jwtBearer(
            String tokenRequestId,
            String expectedScopes,
            Map<String, Object> customClaims
    ) {
        return jwtBearer(new JwtBearerTokenRequest(tokenRequestId, expectedScopes, customClaims));
    }

    @Override
    public AccessToken jwtBearer(JwtBearerTokenRequest request) {
        JwtBearerTokenRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
        AccessToken token = authorizeForGrant(
                safeRequest.tokenRequestId(),
                JWT_BEARER_GRANT,
                "JWT bearer grant",
                jwtBearerAuthorizationAttributes(safeRequest.customClaims())
        );
        scopeValidator.validate(safeRequest.expectedScopes(), token.scopes());
        return token;
    }

    private AccessToken authorizeForSupportedTokenRequest(String registrationId) {
        ClientRegistration registration = findRegistration(registrationId);
        AuthorizationGrantType grantType = registration.getAuthorizationGrantType();
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(grantType) || JWT_BEARER_GRANT.equals(grantType)) {
            return authorize(registration, Map.of());
        }
        throw new AuthenticationException(
                "OAuth2 client registration not configured for token request: " + registrationId
        );
    }

    private AccessToken authorizeForGrant(
            String registrationId,
            AuthorizationGrantType expectedGrantType,
            String grantName,
            Map<String, Object> authorizationAttributes
    ) {
        ClientRegistration registration = findRegistration(registrationId, grantName);
        if (!expectedGrantType.equals(registration.getAuthorizationGrantType())) {
            throw new AuthenticationException(
                    "OAuth2 client registration not configured for " + grantName + ": " + registrationId
            );
        }
        return authorize(registration, authorizationAttributes);
    }

    private ClientRegistration findRegistration(String registrationId) {
        if (clientRegistrationRepository == null) {
            throw new AuthenticationException(
                    "OAuth2 client registration not configured for token request: " + registrationId
            );
        }
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if (registration == null) {
            throw new AuthenticationException(
                    "OAuth2 client registration not configured for token request: " + registrationId
            );
        }
        return registration;
    }

    private ClientRegistration findRegistration(String registrationId, String grantName) {
        if (clientRegistrationRepository == null) {
            throw new AuthenticationException(
                    "OAuth2 client registration not configured for " + grantName + ": " + registrationId
            );
        }
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if (registration == null) {
            throw new AuthenticationException(
                    "OAuth2 client registration not configured for " + grantName + ": " + registrationId
            );
        }
        return registration;
    }

    private AccessToken authorize(ClientRegistration registration, Map<String, Object> authorizationAttributes) {
        if (authorizedClientManager == null) {
            throw new AuthenticationException(
                    "OAuth2 authorized client manager is not configured for token request: "
                            + registration.getRegistrationId()
            );
        }

        OAuth2AuthorizeRequest.Builder requestBuilder = OAuth2AuthorizeRequest
                .withClientRegistrationId(registration.getRegistrationId())
                .principal(principal(authorizationAttributes));
        if (!authorizationAttributes.isEmpty()) {
            requestBuilder.attributes(attributes -> attributes.putAll(authorizationAttributes));
        }

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(requestBuilder.build());
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new AuthenticationException(
                    "OAuth2 client registration not configured for token request: "
                            + registration.getRegistrationId()
            );
        }

        org.springframework.security.oauth2.core.OAuth2AccessToken springToken = authorizedClient.getAccessToken();
        String tokenType = springToken.getTokenType() == null
                ? "Bearer"
                : springToken.getTokenType().getValue();
        Set<String> scopes = springToken.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            scopes = registration.getScopes();
        }
        Instant expiresAt = springToken.getExpiresAt();
        if (expiresAt == null) {
            expiresAt = Instant.MAX;
        }

        return new AccessToken(springToken.getTokenValue(), tokenType, expiresAt, scopes);
    }

    private Map<String, Object> jwtBearerAuthorizationAttributes(Map<String, Object> customClaims) {
        Map<String, Object> sanitizedClaims = JwtBearerCustomClaimsResolver.sanitize(customClaims);
        if (sanitizedClaims.isEmpty()) {
            return Map.of();
        }
        return Map.of(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS, sanitizedClaims);
    }

    private Authentication principal(Map<String, Object> authorizationAttributes) {
        Object customClaims = authorizationAttributes.get(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS);
        if (!(customClaims instanceof Map<?, ?> claims) || claims.isEmpty()) {
            return principal;
        }
        return UsernamePasswordAuthenticationToken.authenticated(
                JwtBearerAuthorizedClientCacheKey.principalName(PRINCIPAL_NAME, JwtBearerCustomClaimsResolver.sanitize(claims)),
                "N/A",
                List.of()
        );
    }

    private String normalize(String id, String name) {
        String normalized = Objects.requireNonNullElse(id, "").trim();
        if (normalized.isBlank()) {
            throw new AuthenticationException(name + " must not be blank");
        }
        return normalized;
    }
}
