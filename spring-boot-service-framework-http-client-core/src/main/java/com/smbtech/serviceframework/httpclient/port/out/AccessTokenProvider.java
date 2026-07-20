package com.smbtech.serviceframework.httpclient.port.out;

/** Defines the access token provider contract. */
public interface AccessTokenProvider {

    /**
     * Returns the configured access token.
     *
     * @param tokenRequestId token request id value
     * @param scopes scopes value
     * @return get access token result
     */
    String getAccessToken(String tokenRequestId, String scopes);
}
