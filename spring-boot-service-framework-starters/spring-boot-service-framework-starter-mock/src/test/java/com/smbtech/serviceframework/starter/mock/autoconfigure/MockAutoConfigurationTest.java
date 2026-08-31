package com.smbtech.serviceframework.starter.mock.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.mock.domain.MockResponse;
import com.smbtech.serviceframework.mock.port.in.MockCatalog;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import com.smbtech.serviceframework.mock.port.out.MockDefinitionSource;
import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import com.smbtech.serviceframework.starter.mock.api.MockService;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import tools.jackson.core.type.TypeReference;

class MockAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(MockAutoConfiguration.class));

    @Test
    void createsMockInfrastructureBeans() {
        contextRunner.run(
                context ->
                        assertThat(context)
                                .hasSingleBean(MockProperties.class)
                                .hasSingleBean(MockDefinitionSource.class)
                                .hasSingleBean(MockCatalog.class)
                                .hasSingleBean(MockResponseSource.class)
                                .hasSingleBean(MockResponder.class)
                                .hasBean("mockRestClientInterceptor")
                                .hasSingleBean(ClientHttpRequestInterceptor.class)
                                .hasSingleBean(MockService.class));
    }

    @Test
    void bindsPropertiesAndLoadsConfiguredMockResponse() {
        contextRunner
                .withPropertyValues(
                        "smbtech.mocks.endpoints.payments-success.enabled=true",
                        "smbtech.mocks.endpoints.payments-success.file=mocks/payments-success.json",
                        "smbtech.mocks.endpoints.payments-success.delay=0ms")
                .run(
                        context -> {
                            MockProperties properties = context.getBean(MockProperties.class);
                            assertThat(properties.getEndpoints())
                                    .containsOnlyKeys("payments-success");
                            assertThat(
                                            properties
                                                    .getEndpoints()
                                                    .get("payments-success")
                                                    .isEnabled())
                                    .isTrue();
                            assertThat(properties.getEndpoints().get("payments-success").getFile())
                                    .isEqualTo("mocks/payments-success.json");
                            assertThat(
                                            context.getBean(MockCatalog.class)
                                                    .findByKey("payments-success"))
                                    .isPresent();

                            MockService mockService = context.getBean(MockService.class);
                            var response =
                                    mockService.exchangeMock(
                                            "payments-success",
                                            new TypeReference<Map<String, Object>>() {});

                            assertThat(response).isPresent();
                            assertThat(response.get().getStatusCode().value()).isEqualTo(201);
                            assertThat(response.get().getHeaders().getFirst("X-Mock"))
                                    .isEqualTo("true");
                            assertThat(response.get().getBody())
                                    .containsEntry("id", "pay-123")
                                    .containsEntry("status", "MOCKED");

                            var coreResponse =
                                    context.getBean(MockResponder.class)
                                            .respond(
                                                    new com.smbtech.serviceframework.mock.domain
                                                            .MockRequest("payments-success"));
                            assertThat(coreResponse).isPresent();
                            assertThat(coreResponse.get().status()).isEqualTo(201);
                            assertThat(
                                            new String(
                                                    coreResponse.get().body(),
                                                    java.nio.charset.StandardCharsets.UTF_8))
                                    .contains("\"id\":\"pay-123\"")
                                    .contains("\"status\":\"MOCKED\"");
                        });
    }

    @Test
    void springFacadeCanReturnRawBytesOrStringBody() {
        contextRunner
                .withPropertyValues(
                        "smbtech.mocks.endpoints.payments-success.enabled=true",
                        "smbtech.mocks.endpoints.payments-success.file=mocks/payments-success.json")
                .run(
                        context -> {
                            MockService mockService = context.getBean(MockService.class);

                            var bytesResponse =
                                    mockService.exchangeMock(
                                            "payments-success", new TypeReference<byte[]>() {});
                            var stringResponse =
                                    mockService.exchangeMock(
                                            "payments-success", new TypeReference<String>() {});

                            assertThat(bytesResponse).isPresent();
                            assertThat(bytesResponse.get().getBody())
                                    .containsSequence((byte) '{', (byte) '"');
                            assertThat(stringResponse).isPresent();
                            assertThat(stringResponse.get().getBody())
                                    .contains("\"status\":\"MOCKED\"");
                        });
    }

    @Test
    void controllerFriendlyApiCanReturnClassBasedResponsesAndNotFoundFallback() {
        contextRunner
                .withPropertyValues(
                        "smbtech.mocks.endpoints.payments-success.enabled=true",
                        "smbtech.mocks.endpoints.payments-success.file=mocks/payments-success.json")
                .run(
                        context -> {
                            MockService mockService = context.getBean(MockService.class);

                            var optionalResponse =
                                    mockService.response("payments-success", PaymentMock.class);
                            var response =
                                    mockService.responseOrNotFound(
                                            "payments-success", PaymentMock.class);
                            var textResponse = mockService.responseOrNotFound("payments-success");
                            var notFound =
                                    mockService.responseOrNotFound("missing", PaymentMock.class);

                            assertThat(optionalResponse).isPresent();
                            assertThat(optionalResponse.get().getBody())
                                    .isEqualTo(new PaymentMock("pay-123", "MOCKED"));
                            assertThat(response.getStatusCode().value()).isEqualTo(201);
                            assertThat(response.getBody())
                                    .isEqualTo(new PaymentMock("pay-123", "MOCKED"));
                            assertThat(textResponse.getBody()).contains("\"status\":\"MOCKED\"");
                            assertThat(notFound.getStatusCode().value()).isEqualTo(404);
                            assertThat(notFound.getBody()).isNull();
                        });
    }

    @Test
    void returnsEmptyWhenMockEndpointIsMissingOrDisabled() {
        contextRunner
                .withPropertyValues(
                        "smbtech.mocks.endpoints.disabled.enabled=false",
                        "smbtech.mocks.endpoints.disabled.file=mocks/payments-success.json")
                .run(
                        context -> {
                            MockService mockService = context.getBean(MockService.class);

                            assertThat(
                                            mockService.exchangeMock(
                                                    "missing",
                                                    new TypeReference<Map<String, Object>>() {}))
                                    .isEmpty();
                            assertThat(
                                            mockService.exchangeMock(
                                                    "disabled",
                                                    new TypeReference<Map<String, Object>>() {}))
                                    .isEmpty();
                        });
    }

    @Test
    void restClientInterceptorReturnsConfiguredMockResponseWithoutExecutingRealRequest() {
        contextRunner
                .withPropertyValues(
                        "smbtech.mocks.endpoints.payments-success.enabled=true",
                        "smbtech.mocks.endpoints.payments-success.file=mocks/payments-success.json")
                .run(
                        context -> {
                            ClientHttpRequestInterceptor interceptor =
                                    context.getBean(
                                            "mockRestClientInterceptor",
                                            ClientHttpRequestInterceptor.class);
                            AtomicBoolean executed = new AtomicBoolean(false);

                            var response =
                                    interceptor.intercept(
                                            httpRequest(
                                                    HttpMethod.POST,
                                                    URI.create(
                                                            "https://payments.example.test/v1/payments?source=test"),
                                                    Map.of(
                                                            "X-Mock-Key",
                                                            List.of("payments-success"))),
                                            "{\"amount\":100}".getBytes(StandardCharsets.UTF_8),
                                            (request, body) -> {
                                                executed.set(true);
                                                return clientHttpResponse(
                                                        MockResponse.ok(
                                                                "real"
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)));
                                            });

                            assertThat(executed).isFalse();
                            assertThat(response.getStatusCode().value()).isEqualTo(201);
                            assertThat(response.getHeaders().getFirst("X-Mock")).isEqualTo("true");
                            assertThat(
                                            new String(
                                                    response.getBody().readAllBytes(),
                                                    StandardCharsets.UTF_8))
                                    .contains("\"id\":\"pay-123\"")
                                    .contains("\"status\":\"MOCKED\"");
                        });
    }

    @Test
    void restClientInterceptorExecutesRealRequestWhenMockDoesNotApply() {
        contextRunner.run(
                context -> {
                    ClientHttpRequestInterceptor interceptor =
                            context.getBean(
                                    "mockRestClientInterceptor",
                                    ClientHttpRequestInterceptor.class);
                    AtomicBoolean executed = new AtomicBoolean(false);

                    var response =
                            interceptor.intercept(
                                    httpRequest(
                                            HttpMethod.GET,
                                            URI.create("https://payments.example.test/v1/missing"),
                                            Map.of()),
                                    new byte[0],
                                    (request, body) -> {
                                        executed.set(true);
                                        return clientHttpResponse(
                                                new MockResponse(
                                                        202,
                                                        Map.of("X-Real", List.of("true")),
                                                        "real".getBytes(StandardCharsets.UTF_8)));
                                    });

                    assertThat(executed).isTrue();
                    assertThat(response.getStatusCode().value()).isEqualTo(202);
                    assertThat(response.getHeaders().getFirst("X-Real")).isEqualTo("true");
                    assertThat(
                                    new String(
                                            response.getBody().readAllBytes(),
                                            StandardCharsets.UTF_8))
                            .isEqualTo("real");
                });
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
                return Map.of("source", "test");
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

    private ClientHttpResponse clientHttpResponse(MockResponse response) {
        return new ClientHttpResponse() {
            @Override
            public HttpStatusCode getStatusCode() {
                return HttpStatusCode.valueOf(response.status());
            }

            @Override
            public String getStatusText() {
                return String.valueOf(response.status());
            }

            @Override
            public void close() {}

            @Override
            public ByteArrayInputStream getBody() {
                return new ByteArrayInputStream(response.body());
            }

            @Override
            public HttpHeaders getHeaders() {
                return httpHeaders(response.headers());
            }
        };
    }

    private record PaymentMock(String id, String status) {}
}
