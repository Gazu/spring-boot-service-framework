package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

class CoreHeadersInterceptorsTest {

    @Test
    void addsDefaultBasicAndCorrelationHeaders() throws Exception {
        MockClientHttpRequest request =
                new MockClientHttpRequest(
                        HttpMethod.GET, URI.create("https://secure.example/ping"));
        MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put("spanId", "a3ce929d0e0e4736");
        MDC.put("transactionId", "tx-123");

        try {
            new DefaultHeadersInterceptor(Map.of("X-Application-Name", "test-service"))
                    .intercept(request, new byte[0], this::response);
            new BasicAuthenticationInterceptor(new BasicAuthentication("demo", "secret"))
                    .intercept(request, new byte[0], this::response);
            new CorrelationHeadersInterceptor(
                            () ->
                                    Map.of(
                                            "X-B3-TraceId",
                                            MDC.get("traceId"),
                                            "X-B3-SpanId",
                                            MDC.get("spanId"),
                                            "X-Transaction-Id",
                                            MDC.get("transactionId")))
                    .intercept(request, new byte[0], this::response);

            HttpHeaders headers = request.getHeaders();
            assertThat(headers.getFirst("X-Application-Name")).isEqualTo("test-service");
            assertThat(headers.getFirst("Authorization"))
                    .isEqualTo("Basic " + basic("demo", "secret"));
            assertThat(headers.getFirst("X-B3-TraceId"))
                    .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
            assertThat(headers.getFirst("X-B3-SpanId")).isEqualTo("a3ce929d0e0e4736");
            assertThat(headers.getFirst("X-Transaction-Id")).isEqualTo("tx-123");
        } finally {
            MDC.clear();
        }
    }

    private MockClientHttpResponse response(
            org.springframework.http.HttpRequest request, byte[] body) {
        return new MockClientHttpResponse("ok".getBytes(StandardCharsets.UTF_8), 200);
    }

    private String basic(String username, String password) {
        return Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
