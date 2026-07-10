package com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor;

import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public final class BearerTokenInterceptor implements ClientHttpRequestInterceptor {

    private final AccessTokenProvider accessTokenProvider;
    private final String credentialTokenRequestorId;
    private final String scopes;

    public BearerTokenInterceptor(
            AccessTokenProvider accessTokenProvider,
            String credentialTokenRequestorId,
            String scopes
    ) {
        this.accessTokenProvider = accessTokenProvider;
        this.credentialTokenRequestorId = credentialTokenRequestorId;
        this.scopes = scopes;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        if (!request.getHeaders().containsHeader("Authorization")) {
            request.getHeaders().setBearerAuth(accessTokenProvider.getAccessToken(credentialTokenRequestorId, scopes));
        }
        return execution.execute(request, body);
    }
}
