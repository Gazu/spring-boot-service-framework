package com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor;

import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public final class CorrelationHeadersInterceptor implements ClientHttpRequestInterceptor {

    private final CorrelationHeadersProvider correlationHeadersProvider;

    public CorrelationHeadersInterceptor(CorrelationHeadersProvider correlationHeadersProvider) {
        this.correlationHeadersProvider = correlationHeadersProvider;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        correlationHeadersProvider.currentHeaders().forEach((name, value) -> {
            if (value != null && !value.isBlank() && !request.getHeaders().containsHeader(name)) {
                request.getHeaders().add(name, value);
            }
        });
        return execution.execute(request, body);
    }
}
