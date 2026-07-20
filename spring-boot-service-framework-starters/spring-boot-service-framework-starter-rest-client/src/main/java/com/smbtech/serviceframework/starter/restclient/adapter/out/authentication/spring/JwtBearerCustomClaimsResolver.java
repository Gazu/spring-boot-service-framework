package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.JwtBearerDefinition;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;

final class JwtBearerCustomClaimsResolver {

    private static final Set<String> DEFAULT_BLOCKED_CLAIMS =
            Set.of(
                    "iss",
                    "sub",
                    "aud",
                    "jti",
                    "iat",
                    "exp",
                    "nbf",
                    "access_token",
                    "refresh_token",
                    "id_token",
                    "token",
                    "password",
                    "secret",
                    "client_secret",
                    "private_key");

    Map<String, Object> resolve(
            JwtBearerDefinition definition, OAuth2AuthorizationContext context) {
        return resolve(definition.customClaims(), dynamicClaims(context));
    }

    Map<String, Object> resolve(
            Map<String, Object> staticClaims, Map<String, Object> dynamicClaims) {
        return resolve(staticClaims, dynamicClaims, Set.of());
    }

    Map<String, Object> resolve(
            Map<String, Object> staticClaims,
            Map<String, Object> dynamicClaims,
            Set<String> blockedClaims) {
        LinkedHashMap<String, Object> resolvedClaims = new LinkedHashMap<>();
        putCustomClaims(resolvedClaims, staticClaims, blockedClaims);
        putCustomClaims(resolvedClaims, dynamicClaims, blockedClaims);
        return Collections.unmodifiableMap(resolvedClaims);
    }

    static Map<String, Object> dynamicClaims(OAuth2AuthorizationContext context) {
        Object customClaims =
                context.getAttribute(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS);
        if (!(customClaims instanceof Map<?, ?> claims) || claims.isEmpty()) {
            return Map.of();
        }
        return sanitize(claims);
    }

    static Map<String, Object> sanitize(Map<?, ?> claims) {
        return sanitize(claims, Set.of());
    }

    static Map<String, Object> sanitize(Map<?, ?> claims, Set<String> blockedClaims) {
        if (claims == null || claims.isEmpty()) {
            return Map.of();
        }

        Set<String> blocked = blockedClaims(blockedClaims);
        LinkedHashMap<String, Object> sanitizedClaims = new LinkedHashMap<>();
        claims.forEach(
                (key, value) -> {
                    if (key instanceof String claimName
                            && isCustomClaim(claimName, value, blocked)) {
                        sanitizedClaims.put(claimName.trim(), value);
                    }
                });
        return Collections.unmodifiableMap(sanitizedClaims);
    }

    private void putCustomClaims(
            Map<String, Object> target, Map<String, Object> source, Set<String> blockedClaims) {
        sanitize(source, blockedClaims).forEach(target::put);
    }

    private static boolean isCustomClaim(String name, Object value, Set<String> blockedClaims) {
        return name != null
                && !name.trim().isBlank()
                && !blockedClaims.contains(normalize(name))
                && value != null;
    }

    private static Set<String> blockedClaims(Set<String> configuredClaims) {
        LinkedHashSet<String> blocked = new LinkedHashSet<>(DEFAULT_BLOCKED_CLAIMS);
        java.util.Objects.requireNonNullElse(configuredClaims, Set.<String>of())
                .forEach(
                        claim -> {
                            if (claim != null && !claim.isBlank()) {
                                blocked.add(normalize(claim));
                            }
                        });
        return Collections.unmodifiableSet(blocked);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
