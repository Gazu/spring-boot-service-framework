package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.JwtBearerDefinition;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class JwtBearerCustomClaimsResolver {

    private static final Set<String> REGISTERED_CLAIMS = Set.of("iss", "sub", "aud", "jti", "iat", "exp", "nbf");

    Map<String, Object> resolve(JwtBearerDefinition definition, OAuth2AuthorizationContext context) {
        return resolve(definition.customClaims(), dynamicClaims(context));
    }

    Map<String, Object> resolve(Map<String, Object> staticClaims, Map<String, Object> dynamicClaims) {
        LinkedHashMap<String, Object> resolvedClaims = new LinkedHashMap<>();
        putCustomClaims(resolvedClaims, staticClaims);
        putCustomClaims(resolvedClaims, dynamicClaims);
        return Collections.unmodifiableMap(resolvedClaims);
    }

    static Map<String, Object> dynamicClaims(OAuth2AuthorizationContext context) {
        Object customClaims = context.getAttribute(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS);
        if (!(customClaims instanceof Map<?, ?> claims) || claims.isEmpty()) {
            return Map.of();
        }
        return sanitize(claims);
    }

    static Map<String, Object> sanitize(Map<?, ?> claims) {
        if (claims == null || claims.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<String, Object> sanitizedClaims = new LinkedHashMap<>();
        claims.forEach((key, value) -> {
            if (key instanceof String claimName && isCustomClaim(claimName, value)) {
                sanitizedClaims.put(claimName, value);
            }
        });
        return Collections.unmodifiableMap(sanitizedClaims);
    }

    private void putCustomClaims(Map<String, Object> target, Map<String, Object> source) {
        sanitize(source).forEach(target::put);
    }

    private static boolean isCustomClaim(String name, Object value) {
        return name != null
                && !name.isBlank()
                && !REGISTERED_CLAIMS.contains(name)
                && value != null;
    }
}
