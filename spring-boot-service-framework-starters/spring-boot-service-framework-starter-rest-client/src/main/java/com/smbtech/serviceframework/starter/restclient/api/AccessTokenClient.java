package com.smbtech.serviceframework.starter.restclient.api;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import java.util.Map;
import java.util.Objects;

/** Defines the access token client contract. */
public interface AccessTokenClient {

    /**
     * Performs the client credentials operation.
     *
     * @param tokenRequestId token request id value
     * @return client credentials result
     */
    AccessToken clientCredentials(String tokenRequestId);

    /**
     * Performs the client credentials operation.
     *
     * @param tokenRequestId token request id value
     * @param expectedScopes expected scopes value
     * @return client credentials result
     */
    AccessToken clientCredentials(String tokenRequestId, String expectedScopes);

    /**
     * Performs the JWT bearer operation.
     *
     * @param tokenRequestId token request id value
     * @return JWT bearer result
     */
    AccessToken jwtBearer(String tokenRequestId);

    /**
     * Performs the JWT bearer operation.
     *
     * @param tokenRequestId token request id value
     * @param expectedScopes expected scopes value
     * @return JWT bearer result
     */
    AccessToken jwtBearer(String tokenRequestId, String expectedScopes);

    /**
     * Performs the JWT bearer operation.
     *
     * @param tokenRequestId token request id value
     * @param customClaims custom claims value
     * @return JWT bearer result
     */
    default AccessToken jwtBearer(String tokenRequestId, Map<String, Object> customClaims) {
        return jwtBearer(new JwtBearerTokenRequest(tokenRequestId, customClaims));
    }

    /**
     * Performs the JWT bearer operation.
     *
     * @param tokenRequestId token request id value
     * @param expectedScopes expected scopes value
     * @param customClaims custom claims value
     * @return JWT bearer result
     */
    default AccessToken jwtBearer(
            String tokenRequestId, String expectedScopes, Map<String, Object> customClaims) {
        return jwtBearer(new JwtBearerTokenRequest(tokenRequestId, expectedScopes, customClaims));
    }

    /**
     * Performs the JWT bearer operation.
     *
     * @param request request value
     * @return JWT bearer result
     */
    default AccessToken jwtBearer(JwtBearerTokenRequest request) {
        JwtBearerTokenRequest safeRequest =
                Objects.requireNonNull(request, "request must not be null");
        return jwtBearer(safeRequest.tokenRequestId(), safeRequest.expectedScopes());
    }
}
