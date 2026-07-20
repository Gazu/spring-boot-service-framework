package com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor;

import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Provides default headers interceptor behavior. */
public final class DefaultHeadersInterceptor implements ClientHttpRequestInterceptor {

    private final Map<String, String> headers;

    /**
     * Creates a default headers interceptor instance.
     *
     * @param headers headers value
     */
    public DefaultHeadersInterceptor(Map<String, String> headers) {
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        headers.forEach(
                (name, value) -> {
                    if (!request.getHeaders().containsHeader(name)) {
                        request.getHeaders().add(name, value);
                    }
                });
        return execution.execute(request, body);
    }
}
