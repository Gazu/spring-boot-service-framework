package com.smbtech.serviceframework.httpclient.domain;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Carries immutable token request definition data.
 *
 * @param id id value
 * @param tokenUri token uri value
 * @param grantType grant type value
 * @param clientAuthenticationMethod client authentication method value
 * @param clientId client id value
 * @param clientSecret client secret value
 * @param scopes scopes value
 * @param cacheSkew cache skew value
 * @param jwtBearer JWT bearer value
 */
public record TokenRequestDefinition(
        String id,
        URI tokenUri,
        GrantType grantType,
        ClientAuthenticationMethod clientAuthenticationMethod,
        String clientId,
        String clientSecret,
        Set<String> scopes,
        Duration cacheSkew,
        JwtBearerDefinition jwtBearer) {
    /** Creates and validates the record components. */
    public TokenRequestDefinition {
        id = Objects.requireNonNullElse(id, "").trim();
        grantType = Objects.requireNonNullElse(grantType, GrantType.CLIENT_CREDENTIALS);
        clientAuthenticationMethod =
                Objects.requireNonNullElse(
                        clientAuthenticationMethod, ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        clientId = Objects.requireNonNullElse(clientId, "");
        clientSecret = Objects.requireNonNullElse(clientSecret, "");
        scopes =
                scopes == null
                        ? Set.of()
                        : Collections.unmodifiableSet(new LinkedHashSet<>(scopes));
        cacheSkew =
                cacheSkew == null || cacheSkew.isNegative() ? Duration.ofSeconds(30) : cacheSkew;
        jwtBearer = Objects.requireNonNullElseGet(jwtBearer, JwtBearerDefinition::empty);
    }

    /**
     * Performs the scope value operation.
     *
     * @return scope value result
     */
    public String scopeValue() {
        return String.join(" ", scopes);
    }
}
