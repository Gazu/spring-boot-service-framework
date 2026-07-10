package com.smbtech.serviceframework.httpclient.domain;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record TokenRequestDefinition(
        String id,
        URI tokenUri,
        GrantType grantType,
        ClientAuthenticationMethod clientAuthenticationMethod,
        String clientId,
        String clientSecret,
        Set<String> scopes,
        Duration cacheSkew,
        JwtBearerDefinition jwtBearer
) {
    public TokenRequestDefinition {
        id = Objects.requireNonNullElse(id, "").trim();
        grantType = Objects.requireNonNullElse(grantType, GrantType.CLIENT_CREDENTIALS);
        clientAuthenticationMethod = Objects.requireNonNullElse(
                clientAuthenticationMethod,
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC
        );
        clientId = Objects.requireNonNullElse(clientId, "");
        clientSecret = Objects.requireNonNullElse(clientSecret, "");
        scopes = scopes == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(scopes));
        cacheSkew = cacheSkew == null || cacheSkew.isNegative() ? Duration.ofSeconds(30) : cacheSkew;
        jwtBearer = Objects.requireNonNullElseGet(jwtBearer, JwtBearerDefinition::empty);
    }

    public String scopeValue() {
        return String.join(" ", scopes);
    }
}
