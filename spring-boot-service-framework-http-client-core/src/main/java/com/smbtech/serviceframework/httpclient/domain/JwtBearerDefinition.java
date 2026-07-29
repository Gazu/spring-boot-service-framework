package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable JWT bearer definition data.
 *
 * @param keyStoreId key store id value
 * @param issuer issuer value
 * @param subject subject value
 * @param audience audience value
 * @param tokenLifetime token lifetime value
 * @param customClaims custom claims value
 */
public record JwtBearerDefinition(
        String keyStoreId,
        String issuer,
        String subject,
        String audience,
        Duration tokenLifetime,
        Map<String, Object> customClaims) {
    /**
     * Performs the empty operation.
     *
     * @return empty result
     */
    public static JwtBearerDefinition empty() {
        return new JwtBearerDefinition("", "", "", "", Duration.ofMinutes(5), Map.of());
    }

    /** Creates and validates the record components. */
    public JwtBearerDefinition {
        keyStoreId = Objects.requireNonNullElse(keyStoreId, "");
        issuer = Objects.requireNonNullElse(issuer, "");
        subject = Objects.requireNonNullElse(subject, "");
        audience = Objects.requireNonNullElse(audience, "");
        tokenLifetime =
                tokenLifetime == null || tokenLifetime.isZero() || tokenLifetime.isNegative()
                        ? Duration.ofMinutes(5)
                        : tokenLifetime;
        customClaims = ImmutableHttpClientValues.structuredMap(customClaims);
    }

    /**
     * Reports whether configured.
     *
     * @return is configured result
     */
    public boolean isConfigured() {
        return !keyStoreId.isBlank();
    }
}
