package com.smbtech.serviceframework.starter.mock.adapter.out.restclient;

import com.smbtech.serviceframework.mock.domain.MockResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

/** Provides mock client http response behavior. */
final class MockClientHttpResponse implements ClientHttpResponse {

    private final MockResponse response;
    private final HttpHeaders headers;

    /**
     * Creates a mock client http response instance.
     *
     * @param response response value
     */
    MockClientHttpResponse(MockResponse response) {
        this.response = Objects.requireNonNull(response, "response must not be null");
        this.headers = toHttpHeaders(response);
    }

    @Override
    public HttpStatusCode getStatusCode() {
        return HttpStatusCode.valueOf(response.status());
    }

    @Override
    public String getStatusText() {
        HttpStatus status = HttpStatus.resolve(response.status());
        if (status != null) {
            return status.getReasonPhrase();
        }
        return String.valueOf(response.status());
    }

    @Override
    public void close() {
        // In-memory mock response. Nothing to close.
    }

    @Override
    public InputStream getBody() {
        return new ByteArrayInputStream(response.body());
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    private HttpHeaders toHttpHeaders(MockResponse response) {
        HttpHeaders httpHeaders = new HttpHeaders();
        response.headers()
                .forEach((name, values) -> values.forEach(value -> httpHeaders.add(name, value)));
        return httpHeaders;
    }
}
