package com.smbtech.serviceframework.starter.restclient.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record JwtBearerTokenRequest(
        String tokenRequestId,
        String expectedScopes,
        Map<String, Object> customClaims
) {

    public JwtBearerTokenRequest {
        tokenRequestId = normalizeTokenRequestId(tokenRequestId);
        expectedScopes = Objects.requireNonNullElse(expectedScopes, "").trim();
        customClaims = immutableCopy(customClaims);
    }

    public JwtBearerTokenRequest(String tokenRequestId) {
        this(tokenRequestId, "", Map.of());
    }

    public JwtBearerTokenRequest(String tokenRequestId, Map<String, Object> customClaims) {
        this(tokenRequestId, "", customClaims);
    }

    public JwtBearerTokenRequest withExpectedScopes(String expectedScopes) {
        return new JwtBearerTokenRequest(tokenRequestId, expectedScopes, customClaims);
    }

    public JwtBearerTokenRequest withCustomClaims(Map<String, Object> customClaims) {
        return new JwtBearerTokenRequest(tokenRequestId, expectedScopes, customClaims);
    }

    private static String normalizeTokenRequestId(String tokenRequestId) {
        String normalized = Objects.requireNonNullElse(tokenRequestId, "").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("tokenRequestId must not be blank");
        }
        return normalized;
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> customClaims) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNullElse(customClaims, Map.of())
        ));
    }
}
