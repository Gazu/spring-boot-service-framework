package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.httpclient.domain.ApacheHttpClientPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuditPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.ConnectionReusePolicy;
import com.smbtech.serviceframework.httpclient.domain.ErrorHandlingPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorCategory;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditFailure;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditRequest;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditResponse;
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.domain.PoolingPolicy;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.TimeoutPolicy;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import com.smbtech.serviceframework.starter.restclient.adapter.out.error.HttpErrorResponseMapper;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.AuditLogInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.StandardErrorHandlingInterceptor;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import tools.jackson.databind.ObjectMapper;

class HttpClientErrorHandlingBaseTest {

    private static final URI REQUEST_URI = URI.create("https://payments.example/v1/orders/123");

    private final HttpErrorBodyDecoder bodyDecoder = new HttpErrorBodyDecoder(new ObjectMapper());

    @Test
    void clientErrorProducesNotifyingExceptionWithCompleteDecodableBody() {
        String fullBody =
                """
                {"code":"VALIDATION_ERROR","message":"1234567890-full-downstream-payload"}
                """;
        StandardErrorHandlingInterceptor interceptor =
                standardInterceptor(new ErrorHandlingPolicy(true, true, 8));

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request(),
                                        new byte[0],
                                        (httpRequest, body) -> jsonResponse(400, fullBody)))
                .isInstanceOfSatisfying(
                        HttpClientResponseException.class,
                        exception -> {
                            assertThat(exception.statusCode()).isEqualTo(400);
                            assertThat(exception.error().category())
                                    .isEqualTo(HttpErrorCategory.CLIENT_ERROR);
                            assertThat(exception.responseBody()).isEqualTo(fullBody);
                            assertThat(exception.isResponseBodyTruncated()).isFalse();
                            assertThat(exception.responseHeaders())
                                    .containsEntry("Content-Type", "application/json");
                            assertThat(exception.primaryNotification())
                                    .hasValueSatisfying(
                                            notification -> {
                                                assertThat(notification.code())
                                                        .isEqualTo(
                                                                "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0400");
                                                assertThat(notification.severity())
                                                        .isEqualTo(NotificationSeverity.ERROR);
                                                assertThat(notification.metadata())
                                                        .containsEntry("statusCode", 400)
                                                        .containsEntry("category", "CLIENT_ERROR")
                                                        .doesNotContainKey("body");
                                            });

                            ErrorPayload payload =
                                    bodyDecoder.decode(exception, ErrorPayload.class);
                            assertThat(payload.code()).isEqualTo("VALIDATION_ERROR");
                            assertThat(payload.message())
                                    .isEqualTo("1234567890-full-downstream-payload");
                        });
    }

    @Test
    void serverErrorProducesServerNotificationCodeAndKeepsCompleteBody() {
        String fullBody =
                """
                {"code":"DOWNSTREAM_ERROR","message":"backend unavailable with complete details"}
                """;
        StandardErrorHandlingInterceptor interceptor =
                standardInterceptor(new ErrorHandlingPolicy(true, true, 1));

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request(),
                                        new byte[0],
                                        (httpRequest, body) -> jsonResponse(503, fullBody)))
                .isInstanceOfSatisfying(
                        HttpClientResponseException.class,
                        exception -> {
                            assertThat(exception.statusCode()).isEqualTo(503);
                            assertThat(exception.error().category())
                                    .isEqualTo(HttpErrorCategory.SERVER_ERROR);
                            assertThat(exception.responseBody()).isEqualTo(fullBody);
                            assertThat(exception.primaryNotification())
                                    .hasValueSatisfying(
                                            notification ->
                                                    assertThat(notification.code())
                                                            .isEqualTo(
                                                                    "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503"));
                        });
    }

    @Test
    void errorHandlingPropertiesCanReduceHeadersAndNotificationMetadataWithoutReducingBody() {
        String fullBody = "{\"message\":\"1234567890-complete\"}";
        StandardErrorHandlingInterceptor interceptor =
                standardInterceptor(
                        new ErrorHandlingPolicy(true, true, 1, false, false, "E_PAYMENTS_HTTP"));

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request(),
                                        new byte[0],
                                        (httpRequest, body) -> jsonResponse(429, fullBody)))
                .isInstanceOfSatisfying(
                        HttpClientResponseException.class,
                        exception -> {
                            assertThat(exception.responseBody()).isEqualTo(fullBody);
                            assertThat(exception.responseHeaders()).isEmpty();
                            assertThat(exception.primaryNotification())
                                    .hasValueSatisfying(
                                            notification -> {
                                                assertThat(notification.code())
                                                        .isEqualTo("E_PAYMENTS_HTTP_0429");
                                                assertThat(notification.metadata()).isEmpty();
                                            });
                        });
    }

    @Test
    void auditLogTruncatesFailureBodyButExceptionStillCarriesCompleteBody() throws Exception {
        String fullBody = "{\"message\":\"downstream unavailable with complete body\"}";
        HttpClientResponseException exception =
                new HttpClientResponseException(
                        new HttpErrorResponseMapper()
                                .map(
                                        definition(
                                                ErrorHandlingPolicy.defaults(),
                                                AuditPolicy.disabled()),
                                        request(),
                                        jsonResponse(503, fullBody)));
        RecordingAuditSink sink = new RecordingAuditSink();
        AuditLogInterceptor interceptor =
                new AuditLogInterceptor(
                        definition(
                                ErrorHandlingPolicy.defaults(),
                                new AuditPolicy(true, true, true, true, true, 12)),
                        sink);

        assertThatThrownBy(
                        () ->
                                interceptor.intercept(
                                        request(),
                                        "create-order".getBytes(StandardCharsets.UTF_8),
                                        (httpRequest, body) -> {
                                            throw exception;
                                        }))
                .isSameAs(exception);

        assertThat(exception.responseBody()).isEqualTo(fullBody);
        assertThat(sink.failure.get().responseBody()).isEqualTo("{\"message\":\"...[truncated]");
    }

    private StandardErrorHandlingInterceptor standardInterceptor(ErrorHandlingPolicy policy) {
        return new StandardErrorHandlingInterceptor(
                definition(policy, AuditPolicy.disabled()), new HttpErrorResponseMapper());
    }

    private MockClientHttpRequest request() {
        return new MockClientHttpRequest(HttpMethod.GET, REQUEST_URI);
    }

    private MockClientHttpResponse jsonResponse(int statusCode, String body) {
        MockClientHttpResponse response =
                new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), statusCode);
        response.getHeaders().add("Content-Type", "application/json");
        return response;
    }

    private HttpClientDefinition definition(ErrorHandlingPolicy errorHandling, AuditPolicy audit) {
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
                audit,
                Map.of());
    }

    private record ErrorPayload(String code, String message) {}

    private static final class RecordingAuditSink implements HttpExchangeAuditSink {
        private final AtomicReference<HttpExchangeAuditFailure> failure = new AtomicReference<>();

        @Override
        public void request(HttpClientDefinition definition, HttpExchangeAuditRequest event) {}

        @Override
        public void response(HttpClientDefinition definition, HttpExchangeAuditResponse event) {}

        @Override
        public void failure(HttpClientDefinition definition, HttpExchangeAuditFailure event) {
            failure.set(event);
        }
    }
}
