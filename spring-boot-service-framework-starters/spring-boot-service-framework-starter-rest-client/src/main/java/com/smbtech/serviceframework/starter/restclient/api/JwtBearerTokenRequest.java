package com.smbtech.serviceframework.starter.restclient.api;

import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable JWT bearer token request data.
 *
 * @param tokenRequestId token request id value
 * @param expectedScopes expected scopes value
 * @param customClaims custom claims value
 */
public record JwtBearerTokenRequest(
        String tokenRequestId, String expectedScopes, Map<String, Object> customClaims) {

    /** Creates and validates the record components. */
    public JwtBearerTokenRequest {
        tokenRequestId = normalizeTokenRequestId(tokenRequestId);
        expectedScopes = Objects.requireNonNullElse(expectedScopes, "").trim();
        customClaims = immutableCopy(customClaims);
    }

    /**
     * Creates a JWT bearer token request instance.
     *
     * @param tokenRequestId token request id value
     */
    public JwtBearerTokenRequest(String tokenRequestId) {
        this(tokenRequestId, "", Map.of());
    }

    /**
     * Creates a JWT bearer token request instance.
     *
     * @param tokenRequestId token request id value
     * @param customClaims custom claims value
     */
    public JwtBearerTokenRequest(String tokenRequestId, Map<String, Object> customClaims) {
        this(tokenRequestId, "", customClaims);
    }

    /**
     * Performs the with expected scopes operation.
     *
     * @param expectedScopes expected scopes value
     * @return with expected scopes result
     */
    public JwtBearerTokenRequest withExpectedScopes(String expectedScopes) {
        return new JwtBearerTokenRequest(tokenRequestId, expectedScopes, customClaims);
    }

    /**
     * Performs the with custom claims operation.
     *
     * @param customClaims custom claims value
     * @return with custom claims result
     */
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
        return ImmutableRequestValues.structuredMap(customClaims);
    }
}
