package com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor;

import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Provides correlation headers interceptor behavior. */
public final class CorrelationHeadersInterceptor implements ClientHttpRequestInterceptor {

    private final CorrelationHeadersProvider correlationHeadersProvider;

    /**
     * Creates a correlation headers interceptor instance.
     *
     * @param correlationHeadersProvider correlation headers provider value
     */
    public CorrelationHeadersInterceptor(CorrelationHeadersProvider correlationHeadersProvider) {
        this.correlationHeadersProvider = correlationHeadersProvider;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        correlationHeadersProvider
                .currentHeaders()
                .forEach(
                        (name, value) -> {
                            if (value != null
                                    && !value.isBlank()
                                    && !request.getHeaders().containsHeader(name)) {
                                request.getHeaders().add(name, value);
                            }
                        });
        return execution.execute(request, body);
    }
}
