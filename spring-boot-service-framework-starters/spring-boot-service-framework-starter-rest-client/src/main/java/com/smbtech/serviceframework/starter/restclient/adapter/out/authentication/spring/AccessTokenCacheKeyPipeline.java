package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.AccessTokenCacheKeyContext;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/** Provides access token cache key pipeline behavior. */
public final class AccessTokenCacheKeyPipeline {

    private static final AuthorizationGrantType JWT_BEARER_GRANT =
            new AuthorizationGrantType(GrantType.JWT_BEARER.value());

    private final OAuth2ExtensionRegistry extensionRegistry;

    /**
     * Creates a access token cache key pipeline instance.
     *
     * @param extensionRegistry extension registry value
     */
    public AccessTokenCacheKeyPipeline(OAuth2ExtensionRegistry extensionRegistry) {
        this.extensionRegistry =
                Objects.requireNonNullElseGet(extensionRegistry, OAuth2ExtensionRegistry::empty);
    }

    /**
     * Performs the resolve operation.
     *
     * @param registration registration value
     * @param defaultPrincipalName default principal name value
     * @param authorizationAttributes authorization attributes value
     * @return resolve result
     */
    public String resolve(
            ClientRegistration registration,
            String defaultPrincipalName,
            Map<String, Object> authorizationAttributes) {
        return resolve(
                registration.getRegistrationId(),
                grantType(registration.getAuthorizationGrantType()),
                defaultPrincipalName,
                registration.getScopes(),
                authorizationAttributes);
    }

    /**
     * Performs the resolve operation.
     *
     * @param registrationId registration id value
     * @param grantType grant type value
     * @param defaultPrincipalName default principal name value
     * @param scopes scopes value
     * @param authorizationAttributes authorization attributes value
     * @return resolve result
     */
    public String resolve(
            String registrationId,
            GrantType grantType,
            String defaultPrincipalName,
            Set<String> scopes,
            Map<String, Object> authorizationAttributes) {
        return extensionRegistry
                .accessTokenCacheKeyResolver()
                .map(
                        resolver ->
                                resolver.resolve(
                                        new AccessTokenCacheKeyContext(
                                                registrationId,
                                                grantType,
                                                defaultPrincipalName,
                                                scopes,
                                                authorizationAttributes)))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse(defaultPrincipalName);
    }

    private GrantType grantType(AuthorizationGrantType grantType) {
        if (JWT_BEARER_GRANT.equals(grantType)) {
            return GrantType.JWT_BEARER;
        }
        return GrantType.CLIENT_CREDENTIALS;
    }
}
