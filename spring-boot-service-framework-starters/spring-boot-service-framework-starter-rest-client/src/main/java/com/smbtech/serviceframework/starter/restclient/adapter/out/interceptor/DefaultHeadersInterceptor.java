package com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;

public final class DefaultHeadersInterceptor implements ClientHttpRequestInterceptor {

    private final Map<String, String> headers;

    public DefaultHeadersInterceptor(Map<String, String> headers) {
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        headers.forEach((name, value) -> {
            if (!request.getHeaders().containsHeader(name)) {
                request.getHeaders().add(name, value);
            }
        });
        return execution.execute(request, body);
    }
}
