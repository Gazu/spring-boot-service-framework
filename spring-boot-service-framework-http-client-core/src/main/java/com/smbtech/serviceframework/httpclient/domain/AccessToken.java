package com.smbtech.serviceframework.httpclient.domain;

import java.time.Instant;
import java.util.Set;

/**
 * Carries immutable access token data.
 *
 * @param value serialized access token
 * @param tokenType token type value
 * @param expiresAt expires at value
 * @param scopes scopes value
 */
public record AccessToken(String value, String tokenType, Instant expiresAt, Set<String> scopes) {
    /** Creates and validates the record components. */
    public AccessToken {
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }

    /**
     * Reports whether active.
     *
     * @param now now value
     * @return is active result
     */
    public boolean isActive(Instant now) {
        return value != null && !value.isBlank() && expiresAt != null && expiresAt.isAfter(now);
    }
}
