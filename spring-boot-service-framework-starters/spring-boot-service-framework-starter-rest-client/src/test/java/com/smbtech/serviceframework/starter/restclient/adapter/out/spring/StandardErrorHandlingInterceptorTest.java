package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.httpclient.domain.ApacheHttpClientPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuditPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.ConnectionReusePolicy;
import com.smbtech.serviceframework.httpclient.domain.ErrorHandlingPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorCategory;
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.domain.PoolingPolicy;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.TimeoutPolicy;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import tools.jackson.databind.ObjectMapper;

class StandardErrorHandlingInterceptorTest {

    private final MockClientHttpRequest request =
            new MockClientHttpRequest(
                    HttpMethod.GET, URI.create("https://payments.example/v1/orders/123"));

    @Test
    void mapsErrorResponsesToStandardException() {
        StandardErrorHandlingInterceptor interceptor =
                interceptor(new ErrorHandlingPolicy(true, true, 4096));

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request,
                                        new byte[0],
                                        (httpRequest, body) ->
                                                jsonResponse(
                                                        500,
                                                        "{\"code\":\"DOWNSTREAM_ERROR\",\"message\":\"boom\"}")))
                .isInstanceOfSatisfying(
                        HttpClientResponseException.class,
                        exception -> {
                            assertThat(exception.statusCode()).isEqualTo(500);
                            assertThat(exception.error().clientName()).isEqualTo("payments");
                            assertThat(exception.error().method()).isEqualTo("GET");
                            assertThat(exception.error().uri())
                                    .isEqualTo("https://payments.example/v1/orders/123");
                            assertThat(exception.error().category())
                                    .isEqualTo(HttpErrorCategory.SERVER_ERROR);
                            assertThat(exception.error().body()).contains("DOWNSTREAM_ERROR");
                            assertThat(exception.error().contentType())
                                    .isEqualTo("application/json");
                            assertThat(exception.error().charset()).isEqualTo("UTF-8");
                            assertThat(exception.error().bodyTruncated()).isFalse();
                            assertThat(exception.primaryNotification())
                                    .hasValueSatisfying(
                                            notification -> {
                                                assertThat(notification.code())
                                                        .isEqualTo(
                                                                "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0500");
                                                assertThat(notification.message())
                                                        .isEqualTo(
                                                                "HTTP 500 Internal Server Error response received from downstream service");
                                                assertThat(notification.metadata())
                                                        .containsEntry("clientName", "payments");
                                                assertThat(notification.metadata())
                                                        .doesNotContainKey("body");
                                            });
                            assertThat(exception.error().headers())
                                    .containsEntry("Content-Type", "application/json");
                        });
    }

    @Test
    void keepsCompleteErrorBodyInExceptionEvenWhenConfiguredLimitIsSmall() {
        StandardErrorHandlingInterceptor interceptor =
                interceptor(new ErrorHandlingPolicy(true, true, 8));

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request,
                                        new byte[0],
                                        (httpRequest, body) -> jsonResponse(400, "1234567890")))
                .isInstanceOfSatisfying(
                        HttpClientResponseException.class,
                        exception -> assertThat(exception.responseBody()).isEqualTo("1234567890"));
    }

    @Test
    void injectsBodyReaderIntoStandardException() {
        StandardErrorHandlingInterceptor interceptor =
                new StandardErrorHandlingInterceptor(
                        definition(new ErrorHandlingPolicy(true, true, 4096)),
                        new HttpErrorResponseMapper(),
                        new com.smbtech.serviceframework.httpclient.domain
                                .HttpErrorNotificationMapper(),
                        new HttpErrorBodyDecoder(new ObjectMapper()));

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request,
                                        new byte[0],
                                        (httpRequest, body) ->
                                                jsonResponse(
                                                        400,
                                                        "{\"code\":\"DOWNSTREAM_ERROR\",\"message\":\"boom\"}")))
                .isInstanceOfSatisfying(
                        HttpClientResponseException.class,
                        exception -> {
                            ErrorPayload payload =
                                    exception.getJsonErrorResponseAsObject(ErrorPayload.class);

                            assertThat(payload.code()).isEqualTo("DOWNSTREAM_ERROR");
                            assertThat(payload.message()).isEqualTo("boom");
                        });
    }

    @Test
    void omitsErrorBodyWhenConfiguredButStillKeepsResponseMetadata() {
        StandardErrorHandlingInterceptor interceptor =
                interceptor(new ErrorHandlingPolicy(true, false, 8));

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request,
                                        new byte[0],
                                        (httpRequest, body) -> jsonResponse(400, "1234567890")))
                .isInstanceOfSatisfying(
                        HttpClientResponseException.class,
                        exception -> {
                            assertThat(exception.responseBody()).isEmpty();
                            assertThat(exception.error().contentType())
                                    .isEqualTo("application/json");
                            assertThat(exception.error().charset()).isEqualTo("UTF-8");
                            assertThat(exception.error().bodyTruncated()).isFalse();
                        });
    }

    @Test
    void canOmitHeadersAndNotificationMetadataWithoutOmittingBody() {
        StandardErrorHandlingInterceptor interceptor =
                interceptor(
                        new ErrorHandlingPolicy(
                                true, true, 1, false, false, "E_PAYMENTS_HTTP_CLIENT"));

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request,
                                        new byte[0],
                                        (httpRequest, body) -> jsonResponse(429, "1234567890")))
                .isInstanceOfSatisfying(
                        HttpClientResponseException.class,
                        exception -> {
                            assertThat(exception.responseBody()).isEqualTo("1234567890");
                            assertThat(exception.responseHeaders()).isEmpty();
                            assertThat(exception.primaryNotification())
                                    .hasValueSatisfying(
                                            notification -> {
                                                assertThat(notification.code())
                                                        .isEqualTo("E_PAYMENTS_HTTP_CLIENT_0429");
                                                assertThat(notification.metadata()).isEmpty();
                                            });
                        });
    }

    @Test
    void canBeDisabledPerClient() throws Exception {
        StandardErrorHandlingInterceptor interceptor =
                interceptor(new ErrorHandlingPolicy(false, true, 4096));

        var response =
                interceptor.intercept(
                        request,
                        new byte[0],
                        (httpRequest, body) -> jsonResponse(404, "{\"message\":\"not found\"}"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    private StandardErrorHandlingInterceptor interceptor(ErrorHandlingPolicy policy) {
        return new StandardErrorHandlingInterceptor(
                definition(policy), new HttpErrorResponseMapper());
    }

    private MockClientHttpResponse jsonResponse(int statusCode, String body) {
        MockClientHttpResponse response =
                new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), statusCode);
        response.getHeaders().add("Content-Type", "application/json");
        return response;
    }

    private HttpClientDefinition definition(ErrorHandlingPolicy errorHandling) {
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
                errorHandling,
                ObservabilityPolicy.defaults(),
                ResiliencePolicy.disabled(),
                AuditPolicy.disabled(),
                Map.of());
    }

    private record ErrorPayload(String code, String message) {}
}
