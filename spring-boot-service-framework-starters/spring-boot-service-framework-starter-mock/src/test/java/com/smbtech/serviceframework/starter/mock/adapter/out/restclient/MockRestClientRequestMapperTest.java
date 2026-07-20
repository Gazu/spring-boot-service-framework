package com.smbtech.serviceframework.starter.mock.adapter.out.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;

class MockRestClientRequestMapperTest {

    private final MockRestClientRequestMapper mapper = new MockRestClientRequestMapper();

    @Test
    void mapsSpringHttpRequestToNeutralMockRequest() {
        var request =
                mapper.toMockRequest(
                        httpRequest(
                                HttpMethod.POST,
                                URI.create("https://api.example.test/v1/payments?id=123&id=456"),
                                Map.of(
                                        "X-Mock-Key",
                                        List.of("payments-success"),
                                        "X-Trace-Id",
                                        List.of("trace-1"))),
                        "{\"amount\":100}".getBytes(StandardCharsets.UTF_8));

        assertThat(request.key()).isEqualTo("payments-success");
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/v1/payments");
        assertThat(request.headers()).containsEntry("X-Trace-Id", List.of("trace-1"));
        assertThat(request.queryParams()).containsEntry("id", List.of("123", "456"));
        assertThat(new String(request.body(), StandardCharsets.UTF_8))
                .isEqualTo("{\"amount\":100}");
        assertThat(request.attributes())
                .containsEntry("uri", "https://api.example.test/v1/payments?id=123&id=456")
                .containsEntry("scheme", "https")
                .containsEntry("host", "api.example.test")
                .containsEntry("port", -1)
                .containsEntry("source", "unit-test");
    }

    @Test
    void usesPathAsKeyWhenMockHeaderIsMissing() {
        var request =
                mapper.toMockRequest(
                        httpRequest(
                                HttpMethod.GET,
                                URI.create("https://api.example.test/v1/payments"),
                                Map.of()),
                        new byte[0]);

        assertThat(request.key()).isEqualTo("v1/payments");
    }

    @Test
    void supportsCustomMockKeyHeader() {
        MockRestClientRequestMapper customMapper = new MockRestClientRequestMapper("X-Test-Mock");

        var request =
                customMapper.toMockRequest(
                        httpRequest(
                                HttpMethod.GET,
                                URI.create("https://api.example.test/v1/payments"),
                                Map.of("X-Test-Mock", List.of("custom-payments"))),
                        new byte[0]);

        assertThat(request.key()).isEqualTo("custom-payments");
    }

    private HttpRequest httpRequest(HttpMethod method, URI uri, Map<String, List<String>> headers) {
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
                return Map.of("source", "unit-test");
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
