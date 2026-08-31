package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.api.JwtBearerTokenRequest;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/** Provides spring security authorized client token client behavior. */
final class SpringSecurityAuthorizedClientTokenClient
        implements AccessTokenProvider, AccessTokenClient {

    private static final String PRINCIPAL_NAME = "spring-boot-service-framework";
    private static final AuthorizationGrantType JWT_BEARER_GRANT =
            new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer");

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final ScopeValidator scopeValidator;
    private final OAuth2TokenDiagnosticsLogger diagnosticsLogger;
    private final RequestContextManager requestContextManager;
    private final boolean requestContextJwtBearerClaimsEnabled;
    private final Set<String> blockedJwtBearerClaims;
    private final OAuth2ExtensionRegistry extensionRegistry;
    private final JwtBearerClaimsPipeline jwtBearerClaimsPipeline;
    private final AccessTokenCacheKeyPipeline accessTokenCacheKeyPipeline;
    private final Authentication principal;

    /**
     * Creates the Spring Security token client with the adapter's scope validation policy.
     *
     * @param clientRegistrationRepository client registration repository
     * @param authorizedClientManager authorized client manager
     * @param diagnosticsLogger diagnostics logger
     * @param requestContextManager request context manager
     * @param requestContextJwtBearerClaimsEnabled whether request-context claims are enabled
     * @param blockedJwtBearerClaims blocked JWT bearer claims
     * @param extensionRegistry OAuth2 extension registry
     */
    public SpringSecurityAuthorizedClientTokenClient(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            RequestContextManager requestContextManager,
            boolean requestContextJwtBearerClaimsEnabled,
            Set<String> blockedJwtBearerClaims,
            OAuth2ExtensionRegistry extensionRegistry) {
        this(
                clientRegistrationRepository,
                authorizedClientManager,
                new ScopeValidator(),
                diagnosticsLogger,
                requestContextManager,
                requestContextJwtBearerClaimsEnabled,
                blockedJwtBearerClaims,
                extensionRegistry);
    }

    /**
     * Creates a spring security authorized client token client instance.
     *
     * @param clientRegistrationRepository client registration repository value
     * @param authorizedClientManager authorized client manager value
     * @param scopeValidator scope validator value
     */
    SpringSecurityAuthorizedClientTokenClient(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientManager authorizedClientManager,
            ScopeValidator scopeValidator) {
        this(
                clientRegistrationRepository,
                authorizedClientManager,
                scopeValidator,
                OAuth2TokenDiagnosticsLogger.disabled(),
                null);
    }

    /**
     * Creates a spring security authorized client token client instance.
     *
     * @param clientRegistrationRepository client registration repository value
     * @param authorizedClientManager authorized client manager value
     * @param scopeValidator scope validator value
     * @param diagnosticsLogger diagnostics logger value
     */
    SpringSecurityAuthorizedClientTokenClient(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientManager authorizedClientManager,
            ScopeValidator scopeValidator,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger) {
        this(
                clientRegistrationRepository,
                authorizedClientManager,
                scopeValidator,
                diagnosticsLogger,
                null);
    }

    /**
     * Creates a spring security authorized client token client instance.
     *
     * @param clientRegistrationRepository client registration repository value
     * @param authorizedClientManager authorized client manager value
     * @param scopeValidator scope validator value
     * @param diagnosticsLogger diagnostics logger value
     * @param requestContextManager request context manager value
     */
    SpringSecurityAuthorizedClientTokenClient(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientManager authorizedClientManager,
            ScopeValidator scopeValidator,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            RequestContextManager requestContextManager) {
        this(
                clientRegistrationRepository,
                authorizedClientManager,
                scopeValidator,
                diagnosticsLogger,
                requestContextManager,
                true,
                Set.of());
    }

    /**
     * Creates a spring security authorized client token client instance.
     *
     * @param clientRegistrationRepository client registration repository value
     * @param authorizedClientManager authorized client manager value
     * @param scopeValidator scope validator value
     * @param diagnosticsLogger diagnostics logger value
     * @param requestContextManager request context manager value
     * @param requestContextJwtBearerClaimsEnabled request context JWT bearer claims enabled value
     */
    SpringSecurityAuthorizedClientTokenClient(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientManager authorizedClientManager,
            ScopeValidator scopeValidator,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            RequestContextManager requestContextManager,
            boolean requestContextJwtBearerClaimsEnabled) {
        this(
                clientRegistrationRepository,
                authorizedClientManager,
                scopeValidator,
                diagnosticsLogger,
                requestContextManager,
                requestContextJwtBearerClaimsEnabled,
                Set.of());
    }

    /**
     * Creates a spring security authorized client token client instance.
     *
     * @param clientRegistrationRepository client registration repository value
     * @param authorizedClientManager authorized client manager value
     * @param scopeValidator scope validator value
     * @param diagnosticsLogger diagnostics logger value
     * @param requestContextManager request context manager value
     * @param requestContextJwtBearerClaimsEnabled request context JWT bearer claims enabled value
     * @param blockedJwtBearerClaims blocked JWT bearer claims value
     */
    SpringSecurityAuthorizedClientTokenClient(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientManager authorizedClientManager,
            ScopeValidator scopeValidator,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            RequestContextManager requestContextManager,
            boolean requestContextJwtBearerClaimsEnabled,
            Set<String> blockedJwtBearerClaims) {
        this(
                clientRegistrationRepository,
                authorizedClientManager,
                scopeValidator,
                diagnosticsLogger,
                requestContextManager,
                requestContextJwtBearerClaimsEnabled,
                blockedJwtBearerClaims,
                OAuth2ExtensionRegistry.empty());
    }

    /**
     * Creates a spring security authorized client token client instance.
     *
     * @param clientRegistrationRepository client registration repository value
     * @param authorizedClientManager authorized client manager value
     * @param scopeValidator scope validator value
     * @param diagnosticsLogger diagnostics logger value
     * @param requestContextManager request context manager value
     * @param requestContextJwtBearerClaimsEnabled request context JWT bearer claims enabled value
     * @param blockedJwtBearerClaims blocked JWT bearer claims value
     * @param extensionRegistry extension registry value
     */
    SpringSecurityAuthorizedClientTokenClient(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientManager authorizedClientManager,
            ScopeValidator scopeValidator,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            RequestContextManager requestContextManager,
            boolean requestContextJwtBearerClaimsEnabled,
            Set<String> blockedJwtBearerClaims,
            OAuth2ExtensionRegistry extensionRegistry) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizedClientManager = authorizedClientManager;
        this.scopeValidator =
                Objects.requireNonNull(scopeValidator, "scopeValidator must not be null");
        this.diagnosticsLogger =
                Objects.requireNonNull(diagnosticsLogger, "diagnosticsLogger must not be null");
        this.requestContextManager = requestContextManager;
        this.requestContextJwtBearerClaimsEnabled = requestContextJwtBearerClaimsEnabled;
        this.blockedJwtBearerClaims =
                Set.copyOf(Objects.requireNonNullElse(blockedJwtBearerClaims, Set.of()));
        this.extensionRegistry =
                Objects.requireNonNull(extensionRegistry, "extensionRegistry must not be null");
        this.jwtBearerClaimsPipeline =
                new JwtBearerClaimsPipeline(this.extensionRegistry, this.blockedJwtBearerClaims);
        this.accessTokenCacheKeyPipeline = new AccessTokenCacheKeyPipeline(this.extensionRegistry);
        this.principal =
                UsernamePasswordAuthenticationToken.authenticated(PRINCIPAL_NAME, "N/A", List.of());
    }

    @Override
    public String getAccessToken(String tokenRequestId, String scopes) {
        AccessToken token =
                authorizeForSupportedTokenRequest(normalize(tokenRequestId, "tokenRequestId"));
        scopeValidator.validate(scopes, token.scopes());
        return token.value();
    }

    @Override
    public AccessToken clientCredentials(String tokenRequestId) {
        return clientCredentials(tokenRequestId, "");
    }

    @Override
    public AccessToken clientCredentials(String tokenRequestId, String expectedScopes) {
        AccessToken token =
                authorizeForGrant(
                        normalize(tokenRequestId, "tokenRequestId"),
                        AuthorizationGrantType.CLIENT_CREDENTIALS,
                        "client_credentials",
                        Map.of());
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
            String tokenRequestId, String expectedScopes, Map<String, Object> customClaims) {
        return jwtBearer(new JwtBearerTokenRequest(tokenRequestId, expectedScopes, customClaims));
    }

    @Override
    public AccessToken jwtBearer(JwtBearerTokenRequest request) {
        JwtBearerTokenRequest safeRequest =
                Objects.requireNonNull(request, "request must not be null");
        ClientRegistration registration =
                findRegistration(safeRequest.tokenRequestId(), "JWT bearer grant");
        if (!JWT_BEARER_GRANT.equals(registration.getAuthorizationGrantType())) {
            throw new HttpClientAuthenticationException(
                    "OAuth2 client registration not configured for JWT bearer grant: "
                            + safeRequest.tokenRequestId());
        }
        AccessToken token =
                authorize(
                        registration,
                        JwtBearerAuthorizationAttributes.authorizationAttributes(
                                effectiveJwtBearerClaims(
                                        registration,
                                        safeRequest.expectedScopes(),
                                        safeRequest.customClaims()),
                                blockedJwtBearerClaims));
        scopeValidator.validate(safeRequest.expectedScopes(), token.scopes());
        return token;
    }

    private AccessToken authorizeForSupportedTokenRequest(String registrationId) {
        ClientRegistration registration = findRegistration(registrationId);
        AuthorizationGrantType grantType = registration.getAuthorizationGrantType();
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(grantType)
                || JWT_BEARER_GRANT.equals(grantType)) {
            return authorize(registration, Map.of());
        }
        throw new HttpClientAuthenticationException(
                "OAuth2 client registration not configured for token request: " + registrationId);
    }

    private AccessToken authorizeForGrant(
            String registrationId,
            AuthorizationGrantType expectedGrantType,
            String grantName,
            Map<String, Object> authorizationAttributes) {
        ClientRegistration registration = findRegistration(registrationId, grantName);
        if (!expectedGrantType.equals(registration.getAuthorizationGrantType())) {
            throw new HttpClientAuthenticationException(
                    "OAuth2 client registration not configured for "
                            + grantName
                            + ": "
                            + registrationId);
        }
        return authorize(registration, authorizationAttributes);
    }

    private ClientRegistration findRegistration(String registrationId) {
        if (clientRegistrationRepository == null) {
            throw new HttpClientAuthenticationException(
                    "OAuth2 client registration not configured for token request: "
                            + registrationId);
        }
        ClientRegistration registration =
                clientRegistrationRepository.findByRegistrationId(registrationId);
        if (registration == null) {
            throw new HttpClientAuthenticationException(
                    "OAuth2 client registration not configured for token request: "
                            + registrationId);
        }
        return registration;
    }

    private ClientRegistration findRegistration(String registrationId, String grantName) {
        if (clientRegistrationRepository == null) {
            throw new HttpClientAuthenticationException(
                    "OAuth2 client registration not configured for "
                            + grantName
                            + ": "
                            + registrationId);
        }
        ClientRegistration registration =
                clientRegistrationRepository.findByRegistrationId(registrationId);
        if (registration == null) {
            throw new HttpClientAuthenticationException(
                    "OAuth2 client registration not configured for "
                            + grantName
                            + ": "
                            + registrationId);
        }
        return registration;
    }

    private AccessToken authorize(
            ClientRegistration registration, Map<String, Object> authorizationAttributes) {
        if (authorizedClientManager == null) {
            HttpClientAuthenticationException exception =
                    new HttpClientAuthenticationException(
                            "OAuth2 authorized client manager is not configured for token request: "
                                    + registration.getRegistrationId());
            diagnosticsLogger.tokenRequestFailed(registration, exception);
            throw exception;
        }

        OAuth2AuthorizeRequest.Builder requestBuilder =
                OAuth2AuthorizeRequest.withClientRegistrationId(registration.getRegistrationId())
                        .principal(principal(registration, authorizationAttributes));
        if (!authorizationAttributes.isEmpty()) {
            requestBuilder.attributes(attributes -> attributes.putAll(authorizationAttributes));
        }

        diagnosticsLogger.tokenRequestStarted(registration, authorizationAttributes);

        try {
            OAuth2AuthorizedClient authorizedClient =
                    authorizedClientManager.authorize(requestBuilder.build());
            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                throw new HttpClientAuthenticationException(
                        "OAuth2 client registration not configured for token request: "
                                + registration.getRegistrationId());
            }

            org.springframework.security.oauth2.core.OAuth2AccessToken springToken =
                    authorizedClient.getAccessToken();
            String tokenType =
                    springToken.getTokenType() == null
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

            diagnosticsLogger.tokenRequestSucceeded(registration, springToken, scopes);
            return new AccessToken(springToken.getTokenValue(), tokenType, expiresAt, scopes);
        } catch (RuntimeException exception) {
            diagnosticsLogger.tokenRequestFailed(registration, exception);
            throw exception;
        }
    }

    private Map<String, Object> effectiveJwtBearerClaims(
            ClientRegistration registration,
            String expectedScopes,
            Map<String, Object> explicitClaims) {
        Map<String, Object> contextClaims =
                !requestContextJwtBearerClaimsEnabled || requestContextManager == null
                        ? Map.of()
                        : requestContextManager.currentJwtBearerClaims();
        return jwtBearerClaimsPipeline.resolveForTokenClient(
                registration, expectedScopes, contextClaims, explicitClaims);
    }

    private Authentication principal(
            ClientRegistration registration, Map<String, Object> authorizationAttributes) {
        String defaultPrincipalName = PRINCIPAL_NAME;
        Object customClaims =
                authorizationAttributes.get(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS);
        if (!(customClaims instanceof Map<?, ?> claims) || claims.isEmpty()) {
            return principal(defaultPrincipalName, registration, authorizationAttributes);
        }
        defaultPrincipalName =
                JwtBearerAuthorizationAttributes.cachePrincipalName(
                        PRINCIPAL_NAME, claims, blockedJwtBearerClaims);
        return principal(defaultPrincipalName, registration, authorizationAttributes);
    }

    private Authentication principal(
            String defaultPrincipalName,
            ClientRegistration registration,
            Map<String, Object> authorizationAttributes) {
        String principalName =
                accessTokenCacheKeyPipeline.resolve(
                        registration, defaultPrincipalName, authorizationAttributes);
        if (PRINCIPAL_NAME.equals(principalName)) {
            return principal;
        }
        return UsernamePasswordAuthenticationToken.authenticated(principalName, "N/A", List.of());
    }

    private String normalize(String id, String name) {
        String normalized = Objects.requireNonNullElse(id, "").trim();
        if (normalized.isBlank()) {
            throw new HttpClientAuthenticationException(name + " must not be blank");
        }
        return normalized;
    }

    /**
     * Performs the extension registry operation.
     *
     * @return extension registry result
     */
    public OAuth2ExtensionRegistry extensionRegistry() {
        return extensionRegistry;
    }
}
