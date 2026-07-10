package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.jwt;

import com.smbtech.serviceframework.httpclient.domain.TokenRequestDefinition;

import java.time.Instant;
import java.util.UUID;

public record JwtBearerGrantRequest(
        TokenRequestDefinition tokenRequest,
        String issuer,
        String subject,
        String audience,
        Instant issuedAt,
        Instant expiresAt,
        String jwtId
) {
    public static JwtBearerGrantRequest from(TokenRequestDefinition definition, Instant now) {
        String issuer = defaultIfBlank(definition.jwtBearer().issuer(), definition.clientId());
        String subject = defaultIfBlank(definition.jwtBearer().subject(), issuer);
        String audience = defaultIfBlank(definition.jwtBearer().audience(), definition.tokenUri().toString());

        return new JwtBearerGrantRequest(
                definition,
                issuer,
                subject,
                audience,
                now,
                now.plus(definition.jwtBearer().tokenLifetime()),
                UUID.randomUUID().toString()
        );
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
