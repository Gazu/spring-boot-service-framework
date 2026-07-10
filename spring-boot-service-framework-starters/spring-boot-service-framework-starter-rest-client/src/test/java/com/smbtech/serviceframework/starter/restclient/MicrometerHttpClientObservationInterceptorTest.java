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
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.domain.PoolingPolicy;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.TimeoutPolicy;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.MicrometerHttpClientObservationInterceptor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MicrometerHttpClientObservationInterceptorTest {

    private static final String METRIC_NAME = "test.http.client.requests";

    @Test
    void recordsSuccessfulHttpClientRequestTimer() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerHttpClientObservationInterceptor interceptor = interceptor(registry, true);

        interceptor.intercept(
                request(),
                new byte[0],
                (httpRequest, body) -> response(200, "ok")
        );

        assertThat(registry.find(METRIC_NAME)
                .tags(
                        "client", "payments",
                        "method", "GET",
                        "outcome", "SUCCESS",
                        "status", "200",
                        "exception", "none",
                        "uri", "/v1/orders",
                        "system", "orders"
                )
                .timer())
                .satisfies(timer -> {
                    assertThat(timer).isNotNull();
                    assertThat(timer.count()).isEqualTo(1);
                });
    }

    @Test
    void recordsStandardHttpErrorAsTimerAndErrorCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerHttpClientObservationInterceptor interceptor = interceptor(registry, true);

        HttpClientResponseException exception = new HttpClientResponseException(new HttpErrorResponse(
                "payments",
                "GET",
                "https://payments.example/v1/orders",
                503,
                "Service Unavailable",
                HttpErrorCategory.SERVER_ERROR,
                Map.of(),
                ""
        ));

        assertThatThrownBy(() -> interceptor.intercept(
                request(),
                new byte[0],
                (httpRequest, body) -> {
                    throw exception;
                }
        )).isSameAs(exception);

        assertThat(registry.find(METRIC_NAME)
                .tags(
                        "client", "payments",
                        "method", "GET",
                        "outcome", "SERVER_ERROR",
                        "status", "503",
                        "exception", "HttpClientResponseException",
                        "uri", "/v1/orders",
                        "system", "orders"
                )
                .timer())
                .satisfies(timer -> {
                    assertThat(timer).isNotNull();
                    assertThat(timer.count()).isEqualTo(1);
                });

        assertThat(registry.find(METRIC_NAME + ".errors")
                .tags(
                        "client", "payments",
                        "method", "GET",
                        "outcome", "SERVER_ERROR",
                        "status", "503",
                        "exception", "HttpClientResponseException",
                        "uri", "/v1/orders",
                        "system", "orders"
                )
                .counter())
                .satisfies(counter -> {
                    assertThat(counter).isNotNull();
                    assertThat(counter.count()).isEqualTo(1);
                });
    }

    @Test
    void skipsMetricsWhenObservabilityIsDisabled() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerHttpClientObservationInterceptor interceptor = interceptor(registry, false);

        interceptor.intercept(
                request(),
                new byte[0],
                (httpRequest, body) -> response(204, "")
        );

        assertThat(registry.find(METRIC_NAME).timer()).isNull();
    }

    private MicrometerHttpClientObservationInterceptor interceptor(
            SimpleMeterRegistry registry,
            boolean enabled
    ) {
        return new MicrometerHttpClientObservationInterceptor(definition(enabled), registry);
    }

    private MockClientHttpRequest request() {
        return new MockClientHttpRequest(
                HttpMethod.GET,
                URI.create("https://payments.example/v1/orders")
        );
    }

    private MockClientHttpResponse response(int statusCode, String body) {
        return new MockClientHttpResponse(
                body.getBytes(StandardCharsets.UTF_8),
                statusCode
        );
    }

    private HttpClientDefinition definition(boolean observabilityEnabled) {
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
                new ObservabilityPolicy(
                        observabilityEnabled,
                        METRIC_NAME,
                        true,
                        true,
                        true,
                        Map.of("system", "orders")
                ),
                ResiliencePolicy.disabled(),
                AuditPolicy.disabled(),
                Map.of()
        );
    }
}
