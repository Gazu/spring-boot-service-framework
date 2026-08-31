package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;

/** Provides OAuth2 authorization context attributes mapper behavior. */
final class OAuth2AuthorizationContextAttributesMapper
        implements Function<OAuth2AuthorizeRequest, Map<String, Object>> {

    private final AuthorizedClientServiceOAuth2AuthorizedClientManager
                    .DefaultContextAttributesMapper
            delegate =
                    new AuthorizedClientServiceOAuth2AuthorizedClientManager
                            .DefaultContextAttributesMapper();
    private final RequestContextManager requestContextManager;
    private final boolean requestContextJwtBearerClaimsEnabled;
    private final Set<String> blockedJwtBearerClaims;
    private final OAuth2ExtensionRegistry extensionRegistry;

    /** Creates a OAuth2 authorization context attributes mapper instance. */
    public OAuth2AuthorizationContextAttributesMapper() {
        this(null);
    }

    /**
     * Creates a OAuth2 authorization context attributes mapper instance.
     *
     * @param requestContextManager request context manager value
     */
    public OAuth2AuthorizationContextAttributesMapper(RequestContextManager requestContextManager) {
        this(requestContextManager, true);
    }

    /**
     * Creates a OAuth2 authorization context attributes mapper instance.
     *
     * @param requestContextManager request context manager value
     * @param requestContextJwtBearerClaimsEnabled request context JWT bearer claims enabled value
     */
    public OAuth2AuthorizationContextAttributesMapper(
            RequestContextManager requestContextManager,
            boolean requestContextJwtBearerClaimsEnabled) {
        this(requestContextManager, requestContextJwtBearerClaimsEnabled, Set.of());
    }

    /**
     * Creates a OAuth2 authorization context attributes mapper instance.
     *
     * @param requestContextManager request context manager value
     * @param requestContextJwtBearerClaimsEnabled request context JWT bearer claims enabled value
     * @param blockedJwtBearerClaims blocked JWT bearer claims value
     */
    public OAuth2AuthorizationContextAttributesMapper(
            RequestContextManager requestContextManager,
            boolean requestContextJwtBearerClaimsEnabled,
            Set<String> blockedJwtBearerClaims) {
        this(
                requestContextManager,
                requestContextJwtBearerClaimsEnabled,
                blockedJwtBearerClaims,
                OAuth2ExtensionRegistry.empty());
    }

    /**
     * Creates a OAuth2 authorization context attributes mapper instance.
     *
     * @param requestContextManager request context manager value
     * @param requestContextJwtBearerClaimsEnabled request context JWT bearer claims enabled value
     * @param blockedJwtBearerClaims blocked JWT bearer claims value
     * @param extensionRegistry extension registry value
     */
    public OAuth2AuthorizationContextAttributesMapper(
            RequestContextManager requestContextManager,
            boolean requestContextJwtBearerClaimsEnabled,
            Set<String> blockedJwtBearerClaims,
            OAuth2ExtensionRegistry extensionRegistry) {
        this.requestContextManager = requestContextManager;
        this.requestContextJwtBearerClaimsEnabled = requestContextJwtBearerClaimsEnabled;
        this.blockedJwtBearerClaims =
                java.util.Set.copyOf(
                        java.util.Objects.requireNonNullElse(
                                blockedJwtBearerClaims, Set.<String>of()));
        this.extensionRegistry =
                java.util.Objects.requireNonNull(
                        extensionRegistry, "extensionRegistry must not be null");
    }

    @Override
    public Map<String, Object> apply(OAuth2AuthorizeRequest authorizeRequest) {
        Map<String, Object> attributes = new LinkedHashMap<>(delegate.apply(authorizeRequest));
        Map<String, Object> customClaims = dynamicJwtBearerClaims(authorizeRequest);
        if (!customClaims.isEmpty()) {
            attributes.put(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS, customClaims);
        }
        return attributes;
    }

    private Map<String, Object> dynamicJwtBearerClaims(OAuth2AuthorizeRequest authorizeRequest) {
        LinkedHashMap<String, Object> resolvedClaims = new LinkedHashMap<>();
        if (requestContextJwtBearerClaimsEnabled && requestContextManager != null) {
            resolvedClaims.putAll(
                    JwtBearerCustomClaimsResolver.sanitize(
                            requestContextManager.currentJwtBearerClaims(),
                            blockedJwtBearerClaims));
        }
        Object requestClaims =
                authorizeRequest.getAttribute(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS);
        if (requestClaims instanceof Map<?, ?> claims) {
            resolvedClaims.putAll(
                    JwtBearerCustomClaimsResolver.sanitize(claims, blockedJwtBearerClaims));
        }
        return Map.copyOf(resolvedClaims);
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
