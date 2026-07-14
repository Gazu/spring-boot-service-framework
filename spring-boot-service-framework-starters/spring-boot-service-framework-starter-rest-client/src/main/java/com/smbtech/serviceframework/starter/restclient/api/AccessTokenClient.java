package com.smbtech.serviceframework.starter.restclient.api;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;

import java.util.Map;
import java.util.Objects;

public interface AccessTokenClient {

    AccessToken clientCredentials(String tokenRequestId);

    AccessToken clientCredentials(String tokenRequestId, String expectedScopes);

    AccessToken jwtBearer(String tokenRequestId);

    AccessToken jwtBearer(String tokenRequestId, String expectedScopes);

    default AccessToken jwtBearer(String tokenRequestId, Map<String, Object> customClaims) {
        return jwtBearer(new JwtBearerTokenRequest(tokenRequestId, customClaims));
    }

    default AccessToken jwtBearer(
            String tokenRequestId,
            String expectedScopes,
            Map<String, Object> customClaims
    ) {
        return jwtBearer(new JwtBearerTokenRequest(tokenRequestId, expectedScopes, customClaims));
    }

    default AccessToken jwtBearer(JwtBearerTokenRequest request) {
        JwtBearerTokenRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
        return jwtBearer(safeRequest.tokenRequestId(), safeRequest.expectedScopes());
    }
}
