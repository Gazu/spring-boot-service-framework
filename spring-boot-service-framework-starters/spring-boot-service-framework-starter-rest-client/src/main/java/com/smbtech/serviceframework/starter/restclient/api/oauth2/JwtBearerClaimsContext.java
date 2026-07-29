package com.smbtech.serviceframework.starter.restclient.api.oauth2;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Immutable context passed to {@link JwtBearerClaimsContributor}.
 *
 * @param registrationId registration id value
 * @param clientId client id value
 * @param tokenUri token uri value
 * @param requestedScopes requested scopes value
 * @param expectedScopes expected scopes value
 * @param configuredClaims configured claims value
 * @param requestContextClaims request context claims value
 * @param explicitClaims explicit claims value
 */
public record JwtBearerClaimsContext(
        String registrationId,
        String clientId,
        @Nullable URI tokenUri,
        Set<String> requestedScopes,
        String expectedScopes,
        Map<String, Object> configuredClaims,
        Map<String, Object> requestContextClaims,
        Map<String, Object> explicitClaims) {

    /** Creates and validates the record components. */
    public JwtBearerClaimsContext {
        registrationId = OAuth2ApiSupport.text(registrationId);
        clientId = OAuth2ApiSupport.text(clientId);
        requestedScopes = OAuth2ApiSupport.immutableSet(requestedScopes);
        expectedScopes = OAuth2ApiSupport.text(expectedScopes);
        configuredClaims = OAuth2ApiSupport.immutableMap(configuredClaims);
        requestContextClaims = OAuth2ApiSupport.immutableMap(requestContextClaims);
        explicitClaims = OAuth2ApiSupport.immutableMap(explicitClaims);
    }
}
