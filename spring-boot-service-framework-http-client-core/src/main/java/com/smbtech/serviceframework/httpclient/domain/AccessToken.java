package com.smbtech.serviceframework.httpclient.domain;

import java.time.Instant;
import java.util.Set;

public record AccessToken(
        String value,
        String tokenType,
        Instant expiresAt,
        Set<String> scopes
) {
    public AccessToken {
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }

    public boolean isActive(Instant now) {
        return value != null && !value.isBlank() && expiresAt != null && expiresAt.isAfter(now);
    }
}
