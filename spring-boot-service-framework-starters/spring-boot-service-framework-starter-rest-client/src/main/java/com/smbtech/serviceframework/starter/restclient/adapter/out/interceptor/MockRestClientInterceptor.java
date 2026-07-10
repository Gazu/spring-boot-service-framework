/*package com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.exception.MockRestClientException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class MockRestClientInterceptor implements ClientHttpRequestInterceptor {

    private final HttpClientDefinition definition;
    private final MockService mockService;

    public MockRestClientInterceptor(
            HttpClientDefinition definition,
            MockService mockService
    ) {
        this.definition = definition;
        this.mockService = mockService;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        String mockKey = resolveMockKey(definition);

        return mockService.exchange(
                        mockKey,
                        new MockTypeReference<byte[]>() {}
                )
                .map(this::toClientHttpResponse)
                .orElseGet(() -> executeRealRequest(request, body, execution));
    }

    private String resolveMockKey(HttpClientDefinition definition) {
        if (definition.mockKey() != null && !definition.mockKey().isBlank()) {
            return definition.mockKey();
        }

        return definition.name();
    }

    private ClientHttpResponse toClientHttpResponse(MockResponse<byte[]> response) {
        return new MockClientHttpResponse(
                response.status(),
                response.headers(),
                response.body()
        );
    }

    private ClientHttpResponse executeRealRequest(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) {
        try {
            return execution.execute(request, body);
        } catch (IOException exception) {
            throw new MockRestClientException("Error executing real HTTP request", exception);
        }
    }
}*/
