package com.smbtech.serviceframework.starter.restclient.api;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;

public interface AccessTokenClient {

    AccessToken clientCredentials(String tokenRequestId);

    AccessToken clientCredentials(String tokenRequestId, String expectedScopes);

    AccessToken jwtBearer(String tokenRequestId);

    AccessToken jwtBearer(String tokenRequestId, String expectedScopes);
}
