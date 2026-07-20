package com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor;

import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Provides basic authentication interceptor behavior. */
public final class BasicAuthenticationInterceptor implements ClientHttpRequestInterceptor {

    private final BasicAuthentication authentication;

    /**
     * Creates a basic authentication interceptor instance.
     *
     * @param authentication authentication value
     */
    public BasicAuthenticationInterceptor(BasicAuthentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (!request.getHeaders().containsHeader("Authorization")) {
            request.getHeaders()
                    .setBasicAuth(
                            authentication.username(),
                            authentication.password(),
                            StandardCharsets.UTF_8);
        }
        return execution.execute(request, body);
    }
}
