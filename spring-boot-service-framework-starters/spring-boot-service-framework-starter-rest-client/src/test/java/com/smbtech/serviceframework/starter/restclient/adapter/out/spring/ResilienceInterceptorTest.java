package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.httpclient.domain.ApacheHttpClientPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuditPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import com.smbtech.serviceframework.httpclient.domain.CircuitBreakerPolicy;
import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.ConnectionReusePolicy;
import com.smbtech.serviceframework.httpclient.domain.ErrorHandlingPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.domain.PoolingPolicy;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.RetryPolicy;
import com.smbtech.serviceframework.httpclient.domain.TimeoutPolicy;
import com.smbtech.serviceframework.httpclient.exception.CircuitBreakerOpenException;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

class ResilienceInterceptorTest {

    @Test
    void retriesRetryableServerErrorsBeforeReturningResponse() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpClientDefinition definition =
                definition(
                        new ResiliencePolicy(
                                true,
                                new RetryPolicy(true, 3, Duration.ZERO, true, true, Set.of()),
                                CircuitBreakerPolicy.disabled()));
        ResilienceInterceptor interceptor = interceptor(definition);

        var response =
                interceptor.intercept(
                        request(),
                        new byte[0],
                        (httpRequest, body) ->
                                attempts.incrementAndGet() == 1 ? response(503) : response(200));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(attempts).hasValue(2);
    }

    @Test
    void opensCircuitBreakerAfterConfiguredConsecutiveFailures() {
        HttpClientDefinition definition =
                definition(
                        new ResiliencePolicy(
                                true,
                                RetryPolicy.disabled(),
                                new CircuitBreakerPolicy(true, 2, Duration.ofMinutes(1))));
        ResilienceInterceptor interceptor = interceptor(definition);

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request(),
                                        new byte[0],
                                        (httpRequest, body) -> {
                                            throw new IOException("downstream unavailable");
                                        }))
                .isInstanceOf(IOException.class);

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request(),
                                        new byte[0],
                                        (httpRequest, body) -> {
                                            throw new IOException("downstream unavailable");
                                        }))
                .isInstanceOf(IOException.class);

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request(),
                                        new byte[0],
                                        (httpRequest, body) -> response(200)))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessageContaining("payments");
    }

    @Test
    void doesNotApplyResilienceWhenDisabled() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        ResilienceInterceptor interceptor = interceptor(definition(ResiliencePolicy.disabled()));

        var response =
                interceptor.intercept(
                        request(),
                        new byte[0],
                        (httpRequest, body) -> {
                            attempts.incrementAndGet();
                            return response(503);
                        });

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(attempts).hasValue(1);
    }

    private ResilienceInterceptor interceptor(HttpClientDefinition definition) {
        return new ResilienceInterceptor(
                definition,
                new ResilienceStateRegistry(
                        Clock.fixed(Instant.parse("2026-07-07T00:00:00Z"), ZoneOffset.UTC)));
    }

    private MockClientHttpRequest request() {
        return new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://payments.example/v1/orders"));
    }

    private MockClientHttpResponse response(int status) {
        return new MockClientHttpResponse(new byte[0], status);
    }

    private HttpClientDefinition definition(ResiliencePolicy resilience) {
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
                        Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1)),
                new PoolingPolicy(
                        ConnectionReusePolicy.DEFAULT, Duration.ofSeconds(30), 100, 20, false),
                ApacheHttpClientPolicy.defaults(),
                ErrorHandlingPolicy.defaults(),
                ObservabilityPolicy.defaults(),
                resilience,
                AuditPolicy.disabled(),
                Map.of());
    }
}
