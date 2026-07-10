package com.smbtech.serviceframework.starter.restclient;

import com.smbtech.serviceframework.httpclient.domain.ApacheHttpClientPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuditPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.ConnectionReusePolicy;
import com.smbtech.serviceframework.httpclient.domain.ErrorHandlingPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorCategory;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditFailure;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditRequest;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditResponse;
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.domain.PoolingPolicy;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.TimeoutPolicy;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.AuditLogInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditLogInterceptorTest {

    @Test
    void auditsRequestAndResponseWithBodiesAndDuration() throws Exception {
        RecordingAuditSink sink = new RecordingAuditSink();
        AuditLogInterceptor interceptor = new AuditLogInterceptor(definition(
                new AuditPolicy(true, true, true, true, true, 12)
        ), sink);

        MockClientHttpRequest request = request();

        interceptor.intercept(
                request,
                "request-body-too-large".getBytes(StandardCharsets.UTF_8),
                (httpRequest, body) -> response(201, "response-body-too-large")
        );

        assertThat(sink.request.get()).satisfies(event -> {
            assertThat(event.method()).isEqualTo("POST");
            assertThat(event.uri()).isEqualTo("https://payments.example/v1/orders");
            assertThat(event.headers()).containsEntry("X-Request-Id", "request-123");
            assertThat(event.body()).isEqualTo("request-body...[truncated]");
        });
        assertThat(sink.response.get()).satisfies(event -> {
            assertThat(event.statusCode()).isEqualTo(201);
            assertThat(event.headers()).containsEntry("Content-Type", "application/json");
            assertThat(event.body()).isEqualTo("response-bod...[truncated]");
            assertThat(event.duration()).isGreaterThanOrEqualTo(Duration.ZERO);
        });
    }

    @Test
    void auditsFailureUsingStandardHttpClientResponseException() {
        RecordingAuditSink sink = new RecordingAuditSink();
        AuditLogInterceptor interceptor = new AuditLogInterceptor(definition(
                new AuditPolicy(true, true, true, true, true, 16)
        ), sink);

        HttpClientResponseException exception = new HttpClientResponseException(new HttpErrorResponse(
                "payments",
                "POST",
                "https://payments.example/v1/orders",
                503,
                "Service Unavailable",
                HttpErrorCategory.SERVER_ERROR,
                Map.of("Content-Type", "application/json"),
                "{\"message\":\"downstream unavailable\"}"
        ));

        assertThatThrownBy(() -> interceptor.intercept(
                request(),
                "create-order".getBytes(StandardCharsets.UTF_8),
                (httpRequest, body) -> {
                    throw exception;
                }
        )).isSameAs(exception);

        assertThat(sink.failure.get()).satisfies(event -> {
            assertThat(event.statusCode()).isEqualTo(503);
            assertThat(event.statusText()).isEqualTo("Service Unavailable");
            assertThat(event.headers()).containsEntry("Content-Type", "application/json");
            assertThat(event.requestBody()).isEqualTo("create-order");
            assertThat(event.responseBody()).isEqualTo("{\"message\":\"down...[truncated]");
            assertThat(event.exceptionType()).isEqualTo(HttpClientResponseException.class.getName());
            assertThat(event.duration()).isGreaterThanOrEqualTo(Duration.ZERO);
        });
    }

    private MockClientHttpRequest request() {
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.POST,
                URI.create("https://payments.example/v1/orders")
        );
        request.getHeaders().add("X-Request-Id", "request-123");
        return request;
    }

    private MockClientHttpResponse response(int statusCode, String body) {
        MockClientHttpResponse response = new MockClientHttpResponse(
                body.getBytes(StandardCharsets.UTF_8),
                statusCode
        );
        response.getHeaders().add("Content-Type", "application/json");
        return response;
    }

    private HttpClientDefinition definition(AuditPolicy audit) {
        return new HttpClientDefinition(
                "payments",
                null,
                URI.create("https://payments.example"),
                ClientType.DEFAULT,
                AuthenticationType.NO_AUTH,
                new BasicAuthentication("", ""),
                "",
                "",
                new TimeoutPolicy(
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1)
                ),
                new PoolingPolicy(
                        ConnectionReusePolicy.DEFAULT,
                        Duration.ofSeconds(30),
                        100,
                        20,
                        false
                ),
                ApacheHttpClientPolicy.defaults(),
                ErrorHandlingPolicy.defaults(),
                ObservabilityPolicy.defaults(),
                ResiliencePolicy.disabled(),
                audit,
                Map.of()
        );
    }

    private static final class RecordingAuditSink implements HttpExchangeAuditSink {
        private final AtomicReference<HttpExchangeAuditRequest> request = new AtomicReference<>();
        private final AtomicReference<HttpExchangeAuditResponse> response = new AtomicReference<>();
        private final AtomicReference<HttpExchangeAuditFailure> failure = new AtomicReference<>();

        @Override
        public void request(HttpClientDefinition definition, HttpExchangeAuditRequest event) {
            request.set(event);
        }

        @Override
        public void response(HttpClientDefinition definition, HttpExchangeAuditResponse event) {
            response.set(event);
        }

        @Override
        public void failure(HttpClientDefinition definition, HttpExchangeAuditFailure event) {
            failure.set(event);
        }
    }
}
