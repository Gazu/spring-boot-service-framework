package com.smbtech.serviceframework.starter.mock.adapter.in.restclient;

import com.smbtech.serviceframework.mock.domain.MockResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MockRestClientInterceptorTest {

    @Test
    void returnsMockResponseWhenResponderMatches() throws Exception {
        AtomicReference<String> requestedKey = new AtomicReference<>();
        MockRestClientInterceptor interceptor = new MockRestClientInterceptor(
                request -> {
                    requestedKey.set(request.key());
                    return Optional.of(new MockResponse(
                            201,
                            Map.of("X-Mock", List.of("true")),
                            "{\"status\":\"MOCKED\"}".getBytes(StandardCharsets.UTF_8)
                    ));
                },
                new MockRestClientRequestMapper()
        );
        AtomicBoolean executed = new AtomicBoolean(false);

        var response = interceptor.intercept(
                httpRequest(
                        HttpMethod.GET,
                        URI.create("https://api.example.test/v1/payments"),
                        Map.of("X-Mock-Key", List.of("payments-success"))
                ),
                new byte[0],
                (request, body) -> {
                    executed.set(true);
                    return new MockClientHttpResponse(MockResponse.ok("real".getBytes(StandardCharsets.UTF_8)));
                }
        );

        assertThat(requestedKey).hasValue("payments-success");
        assertThat(executed).isFalse();
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getStatusText()).isEqualTo("Created");
        assertThat(response.getHeaders().getFirst("X-Mock")).isEqualTo("true");
        assertThat(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("{\"status\":\"MOCKED\"}");
    }

    @Test
    void delegatesToExecutionWhenResponderDoesNotMatch() throws Exception {
        MockRestClientInterceptor interceptor = new MockRestClientInterceptor(
                request -> Optional.empty(),
                new MockRestClientRequestMapper()
        );
        AtomicBoolean executed = new AtomicBoolean(false);

        var response = interceptor.intercept(
                httpRequest(HttpMethod.GET, URI.create("https://api.example.test/v1/payments"), Map.of()),
                new byte[0],
                (request, body) -> {
                    executed.set(true);
                    return new MockClientHttpResponse(new MockResponse(
                            202,
                            Map.of("X-Real", List.of("true")),
                            "real".getBytes(StandardCharsets.UTF_8)
                    ));
                }
        );

        assertThat(executed).isTrue();
        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getStatusText()).isEqualTo("Accepted");
        assertThat(response.getHeaders().getFirst("X-Real")).isEqualTo("true");
        assertThat(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("real");
    }

    private HttpRequest httpRequest(
            HttpMethod method,
            URI uri,
            Map<String, List<String>> headers
    ) {
        return new HttpRequest() {
            private final HttpHeaders httpHeaders = httpHeaders(headers);

            @Override
            public HttpMethod getMethod() {
                return method;
            }

            @Override
            public URI getURI() {
                return uri;
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Map.of();
            }

            @Override
            public HttpHeaders getHeaders() {
                return httpHeaders;
            }
        };
    }

    private HttpHeaders httpHeaders(Map<String, List<String>> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach((name, values) -> values.forEach(value -> httpHeaders.add(name, value)));
        return httpHeaders;
    }
}
