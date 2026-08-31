package com.smbtech.serviceframework.starter.mock.adapter.out.restclient;

import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Provides mock rest client interceptor behavior. */
final class MockRestClientInterceptor implements ClientHttpRequestInterceptor {

    private final MockResponder mockResponder;
    private final MockRestClientRequestMapper requestMapper;

    /**
     * Creates a mock rest client interceptor instance.
     *
     * @param mockResponder mock responder value
     * @param requestMapper request mapper value
     */
    MockRestClientInterceptor(
            MockResponder mockResponder, MockRestClientRequestMapper requestMapper) {
        this.mockResponder =
                Objects.requireNonNull(mockResponder, "mockResponder must not be null");
        this.requestMapper =
                Objects.requireNonNull(requestMapper, "requestMapper must not be null");
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        MockRequest mockRequest = requestMapper.toMockRequest(request, body);
        Optional<MockResponse> mockResponse = mockResponder.respond(mockRequest);

        if (mockResponse.isPresent()) {
            return new MockClientHttpResponse(mockResponse.get());
        }

        return execution.execute(request, body);
    }
}
