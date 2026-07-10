package com.smbtech.serviceframework.starter.mock.autoconfigure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smbtech.serviceframework.mock.port.in.MockCatalog;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import com.smbtech.serviceframework.mock.port.out.MockDefinitionSource;
import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import com.smbtech.serviceframework.starter.mock.adapter.in.spring.MockResponseEntityMapper;
import com.smbtech.serviceframework.starter.mock.api.mock.MockService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MockAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MockAutoConfiguration.class));

    @Test
    void createsMockInfrastructureBeans() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(MockProperties.class)
                .hasSingleBean(MockDefinitionSource.class)
                .hasSingleBean(MockCatalog.class)
                .hasSingleBean(MockResponseSource.class)
                .hasSingleBean(MockResponder.class)
                .hasSingleBean(MockResponseEntityMapper.class)
                .hasSingleBean(MockService.class));
    }

    @Test
    void bindsPropertiesAndLoadsConfiguredMockResponse() {
        contextRunner
                .withPropertyValues(
                        "smbtech.mocks.endpoints.payments-success.enabled=true",
                        "smbtech.mocks.endpoints.payments-success.file=mocks/payments-success.json",
                        "smbtech.mocks.endpoints.payments-success.delay=0ms"
                )
                .run(context -> {
                    MockProperties properties = context.getBean(MockProperties.class);
                    assertThat(properties.getEndpoints()).containsOnlyKeys("payments-success");
                    assertThat(properties.getEndpoints().get("payments-success").isEnabled()).isTrue();
                    assertThat(properties.getEndpoints().get("payments-success").getFile())
                            .isEqualTo("mocks/payments-success.json");
                    assertThat(context.getBean(MockCatalog.class).findByKey("payments-success")).isPresent();

                    MockService mockService = context.getBean(MockService.class);
                    var response = mockService.exchangeMock(
                            "payments-success",
                            new TypeReference<Map<String, Object>>() {
                            }
                    );

                    assertThat(response).isPresent();
                    assertThat(response.get().getStatusCode().value()).isEqualTo(201);
                    assertThat(response.get().getHeaders().getFirst("X-Mock")).isEqualTo("true");
                    assertThat(response.get().getBody())
                            .containsEntry("id", "pay-123")
                            .containsEntry("status", "MOCKED");

                    var coreResponse = context.getBean(MockResponder.class)
                            .respond(new com.smbtech.serviceframework.mock.domain.MockRequest("payments-success"));
                    assertThat(coreResponse).isPresent();
                    assertThat(coreResponse.get().status()).isEqualTo(201);
                    assertThat(new String(coreResponse.get().body(), java.nio.charset.StandardCharsets.UTF_8))
                            .contains("\"id\":\"pay-123\"")
                            .contains("\"status\":\"MOCKED\"");
                });
    }

    @Test
    void springFacadeCanReturnRawBytesOrStringBody() {
        contextRunner
                .withPropertyValues(
                        "smbtech.mocks.endpoints.payments-success.enabled=true",
                        "smbtech.mocks.endpoints.payments-success.file=mocks/payments-success.json"
                )
                .run(context -> {
                    MockService mockService = context.getBean(MockService.class);

                    var bytesResponse = mockService.exchangeMock("payments-success", new TypeReference<byte[]>() {
                    });
                    var stringResponse = mockService.exchangeMock("payments-success", new TypeReference<String>() {
                    });

                    assertThat(bytesResponse).isPresent();
                    assertThat(bytesResponse.get().getBody()).containsSequence((byte) '{', (byte) '"');
                    assertThat(stringResponse).isPresent();
                    assertThat(stringResponse.get().getBody()).contains("\"status\":\"MOCKED\"");
                });
    }

    @Test
    void controllerFriendlyApiCanReturnClassBasedResponsesAndNotFoundFallback() {
        contextRunner
                .withPropertyValues(
                        "smbtech.mocks.endpoints.payments-success.enabled=true",
                        "smbtech.mocks.endpoints.payments-success.file=mocks/payments-success.json"
                )
                .run(context -> {
                    MockService mockService = context.getBean(MockService.class);

                    var optionalResponse = mockService.response("payments-success", PaymentMock.class);
                    var response = mockService.responseOrNotFound("payments-success", PaymentMock.class);
                    var textResponse = mockService.responseOrNotFound("payments-success");
                    var notFound = mockService.responseOrNotFound("missing", PaymentMock.class);

                    assertThat(optionalResponse).isPresent();
                    assertThat(optionalResponse.get().getBody()).isEqualTo(new PaymentMock("pay-123", "MOCKED"));
                    assertThat(response.getStatusCode().value()).isEqualTo(201);
                    assertThat(response.getBody()).isEqualTo(new PaymentMock("pay-123", "MOCKED"));
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
                        "smbtech.mocks.endpoints.disabled.file=mocks/payments-success.json"
                )
                .run(context -> {
                    MockService mockService = context.getBean(MockService.class);

                    assertThat(mockService.exchangeMock("missing", new TypeReference<Map<String, Object>>() {
                    })).isEmpty();
                    assertThat(mockService.exchangeMock("disabled", new TypeReference<Map<String, Object>>() {
                    })).isEmpty();
                });
    }

    private record PaymentMock(String id, String status) {
    }
}
