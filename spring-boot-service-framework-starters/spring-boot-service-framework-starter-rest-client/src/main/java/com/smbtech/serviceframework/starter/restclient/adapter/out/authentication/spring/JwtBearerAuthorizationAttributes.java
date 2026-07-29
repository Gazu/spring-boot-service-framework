package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import java.util.Map;
import java.util.Set;

/** Provides JWT bearer authorization attributes behavior. */
final class JwtBearerAuthorizationAttributes {

    private JwtBearerAuthorizationAttributes() {}

    /**
     * Performs the authorization attributes operation.
     *
     * @param customClaims custom claims value
     * @return authorization attributes result
     */
    public static Map<String, Object> authorizationAttributes(Map<?, ?> customClaims) {
        return authorizationAttributes(customClaims, Set.of());
    }

    /**
     * Performs the authorization attributes operation.
     *
     * @param customClaims custom claims value
     * @param blockedClaims blocked claims value
     * @return authorization attributes result
     */
    public static Map<String, Object> authorizationAttributes(
            Map<?, ?> customClaims, Set<String> blockedClaims) {
        Map<String, Object> sanitizedClaims =
                JwtBearerCustomClaimsResolver.sanitize(customClaims, blockedClaims);
        if (sanitizedClaims.isEmpty()) {
            return Map.of();
        }
        return Map.of(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS, sanitizedClaims);
    }

    /**
     * Performs the cache principal name operation.
     *
     * @param principalName principal name value
     * @param customClaims custom claims value
     * @return cache principal name result
     */
    public static String cachePrincipalName(String principalName, Map<?, ?> customClaims) {
        return cachePrincipalName(principalName, customClaims, Set.of());
    }

    /**
     * Performs the cache principal name operation.
     *
     * @param principalName principal name value
     * @param customClaims custom claims value
     * @param blockedClaims blocked claims value
     * @return cache principal name result
     */
    public static String cachePrincipalName(
            String principalName, Map<?, ?> customClaims, Set<String> blockedClaims) {
        return JwtBearerAuthorizedClientCacheKey.principalName(
                principalName, JwtBearerCustomClaimsResolver.sanitize(customClaims, blockedClaims));
    }

    /**
     * Performs the principal name operation.
     *
     * @param principalName principal name value
     * @param customClaims custom claims value
     * @return principal name result
     */
    public static String principalName(String principalName, Map<?, ?> customClaims) {
        return cachePrincipalName(principalName, customClaims);
    }
}
