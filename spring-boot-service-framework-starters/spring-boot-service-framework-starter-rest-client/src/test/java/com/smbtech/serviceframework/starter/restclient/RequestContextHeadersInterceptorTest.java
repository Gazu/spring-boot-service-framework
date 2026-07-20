package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.starter.restclient.adapter.out.context.ThreadLocalRequestContextManager;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.RequestContextHeadersInterceptor;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextScope;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

class RequestContextHeadersInterceptorTest {

    private final ThreadLocalRequestContextManager manager = new ThreadLocalRequestContextManager();
    private final RequestContextHeadersInterceptor interceptor =
            new RequestContextHeadersInterceptor(manager);

    @Test
    void addsCurrentRequestContextHeaders() throws Exception {
        MockClientHttpRequest request = request();

        try (RequestContextScope ignored =
                manager.openHeaders(
                        Map.of(
                                "X-Customer-Id", "17952397-3",
                                "X-Channel", "mobile"))) {
            interceptor.intercept(request, new byte[0], this::ok);
        }

        assertThat(request.getHeaders().getFirst("X-Customer-Id")).isEqualTo("17952397-3");
        assertThat(request.getHeaders().getFirst("X-Channel")).isEqualTo("mobile");
    }

    @Test
    void doesNotOverwriteExistingHeaders() throws Exception {
        MockClientHttpRequest request = request();
        request.getHeaders().add("X-Channel", "existing");

        try (RequestContextScope ignored = manager.openHeader("X-Channel", "dynamic")) {
            interceptor.intercept(request, new byte[0], this::ok);
        }

        assertThat(request.getHeaders().get("X-Channel")).containsExactly("existing");
    }

    @Test
    void ignoresBlankHeaderValues() throws Exception {
        MockClientHttpRequest request = request();

        try (RequestContextScope ignored = manager.openHeader("X-Channel", " ")) {
            interceptor.intercept(request, new byte[0], this::ok);
        }

        assertThat(request.getHeaders().containsHeader("X-Channel")).isFalse();
    }

    @Test
    void ignoresUnsafeHeaders() throws Exception {
        MockClientHttpRequest request = request();

        try (RequestContextScope ignored =
                manager.openHeaders(
                        Map.of(
                                "Authorization", "Bearer malicious",
                                "Cookie", "session=abc",
                                "X-Bad\nName", "value",
                                "X-Injected", "safe\r\nX-Other: injected",
                                "X-Channel", "mobile"))) {
            interceptor.intercept(request, new byte[0], this::ok);
        }

        assertThat(request.getHeaders().containsHeader("Authorization")).isFalse();
        assertThat(request.getHeaders().containsHeader("Cookie")).isFalse();
        assertThat(request.getHeaders().containsHeader("X-Bad\nName")).isFalse();
        assertThat(request.getHeaders().containsHeader("X-Injected")).isFalse();
        assertThat(request.getHeaders().getFirst("X-Channel")).isEqualTo("mobile");
    }

    @Test
    void ignoresConfiguredBlockedHeaders() throws Exception {
        MockClientHttpRequest request = request();
        RequestContextHeadersInterceptor interceptor =
                new RequestContextHeadersInterceptor(manager, Set.of("X-Customer-Id"));

        try (RequestContextScope ignored =
                manager.openHeaders(
                        Map.of(
                                "X-Customer-Id", "17952397-3",
                                "X-Channel", "mobile"))) {
            interceptor.intercept(request, new byte[0], this::ok);
        }

        assertThat(request.getHeaders().containsHeader("X-Customer-Id")).isFalse();
        assertThat(request.getHeaders().getFirst("X-Channel")).isEqualTo("mobile");
    }

    @Test
    void doesNothingWhenNoRequestContextIsOpen() throws Exception {
        MockClientHttpRequest request = request();

        interceptor.intercept(request, new byte[0], this::ok);

        assertThat(request.getHeaders().isEmpty()).isTrue();
    }

    private MockClientHttpRequest request() {
        return new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://payments.example/dummy"));
    }

    private MockClientHttpResponse ok(org.springframework.http.HttpRequest request, byte[] body) {
        return new MockClientHttpResponse("ok".getBytes(StandardCharsets.UTF_8), 200);
    }
}
