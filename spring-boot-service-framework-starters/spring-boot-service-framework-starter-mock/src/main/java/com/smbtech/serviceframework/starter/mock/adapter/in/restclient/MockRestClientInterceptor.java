package com.smbtech.serviceframework.starter.mock.adapter.in.restclient;

import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public final class MockRestClientInterceptor implements ClientHttpRequestInterceptor {

    private final MockResponder mockResponder;
    private final MockRestClientRequestMapper requestMapper;

    public MockRestClientInterceptor(
            MockResponder mockResponder,
            MockRestClientRequestMapper requestMapper
    ) {
        this.mockResponder = Objects.requireNonNull(mockResponder, "mockResponder must not be null");
        this.requestMapper = Objects.requireNonNull(requestMapper, "requestMapper must not be null");
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        MockRequest mockRequest = requestMapper.toMockRequest(request, body);
        Optional<MockResponse> mockResponse = mockResponder.respond(mockRequest);

        if (mockResponse.isPresent()) {
            return new MockClientHttpResponse(mockResponse.get());
        }

        return execution.execute(request, body);
    }
}
