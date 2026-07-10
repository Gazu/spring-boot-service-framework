package com.smbtech.serviceframework.httpclient.port.out;

public interface AccessTokenProvider {

    String getAccessToken(String credentialTokenRequestorId, String scopes);
}
