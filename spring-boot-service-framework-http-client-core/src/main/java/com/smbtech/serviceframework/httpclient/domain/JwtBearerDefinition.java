package com.smbtech.serviceframework.httpclient.domain;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record JwtBearerDefinition(
        String keyStoreId,
        String issuer,
        String subject,
        String audience,
        Duration tokenLifetime,
        Map<String, Object> customClaims
) {
    public static JwtBearerDefinition empty() {
        return new JwtBearerDefinition("", "", "", "", Duration.ofMinutes(5), Map.of());
    }

    public JwtBearerDefinition {
        keyStoreId = Objects.requireNonNullElse(keyStoreId, "");
        issuer = Objects.requireNonNullElse(issuer, "");
        subject = Objects.requireNonNullElse(subject, "");
        audience = Objects.requireNonNullElse(audience, "");
        tokenLifetime = tokenLifetime == null || tokenLifetime.isZero() || tokenLifetime.isNegative()
                ? Duration.ofMinutes(5)
                : tokenLifetime;
        customClaims = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(customClaims, Map.of())));
    }

    public boolean isConfigured() {
        return !keyStoreId.isBlank();
    }
}
