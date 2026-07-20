package com.smbtech.serviceframework.starter.errorhandling.api.security;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable input used to classify a Spring Security failure without carrying credentials, token
 * values, claims, headers, or a principal.
 *
 * @param failure original security failure, retained only for internal resolution
 * @param method HTTP method
 * @param route safe route template, never a raw URI with identifiers or query parameters
 * @param correlationId public correlation identifier
 * @param bearerCredentialsPresent whether a Bearer credential was present
 * @param authenticationType logical authentication type, when known
 * @param requiredScopes scopes required by the protected operation
 */
public record SecurityFailureContext(
        Throwable failure,
        String method,
        String route,
        String correlationId,
        boolean bearerCredentialsPresent,
        String authenticationType,
        Set<String> requiredScopes) {

    /** Creates and normalizes an immutable security failure context. */
    public SecurityFailureContext {
        failure = Objects.requireNonNull(failure, "security failure must not be null");
        method = optionalText(method).toUpperCase(Locale.ROOT);
        route = optionalText(route);
        correlationId = optionalText(correlationId);
        authenticationType = optionalText(authenticationType).toLowerCase(Locale.ROOT);
        requiredScopes = immutableScopes(requiredScopes);
    }

    /**
     * Creates a context containing only the original failure.
     *
     * @param failure original security failure
     */
    public SecurityFailureContext(Throwable failure) {
        this(failure, "", "", "", false, "", Set.of());
    }

    /**
     * Returns whether at least one required scope is known.
     *
     * @return result
     */
    public boolean hasRequiredScopes() {
        return !requiredScopes.isEmpty();
    }

    private static Set<String> immutableScopes(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return Set.of();
        }
        TreeSet<String> normalized = new TreeSet<>();
        for (String scope : scopes) {
            String value = optionalText(scope);
            if (value.isEmpty()) {
                throw new IllegalArgumentException("required scope must not be blank");
            }
            normalized.add(value);
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static String optionalText(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
