package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class OpenApiMockContractLoaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenApiMockContractLoader loader =
            new OpenApiMockContractLoader(new DefaultResourceLoader(), objectMapper);

    @Test
    void loadsOperationsResponsesExamplesAndGeneratedSchemas() throws Exception {
        OpenApiMockContract contract =
                loader.load("classpath:fixtures/pet-store-mock.yaml", true, Duration.ofMillis(5));

        assertThat(contract.title()).isEqualTo("pet-store-mock");
        assertThat(contract.version()).isEqualTo("1.0.0");
        assertThat(contract.operations()).hasSize(3);

        OpenApiMockOperation getPet = operation(contract, "getPet");
        assertThat(getPet.defaultStatus()).isEqualTo(200);
        assertThat(getPet.responses()).containsOnlyKeys(200, 404);
        assertThat(getPet.delay()).isEqualTo(Duration.ofMillis(5));
        assertThat(objectMapper.readTree(getPet.responses().get(200).body()).path("name").asText())
                .isEqualTo("Luna");

        OpenApiMockOperation createPet = operation(contract, "createPet");
        var generatedBody = objectMapper.readTree(createPet.responses().get(201).body());
        assertThat(generatedBody.has("id")).isTrue();
        assertThat(generatedBody.has("status")).isTrue();
        assertThat(generatedBody.has("active")).isTrue();
        assertThat(generatedBody.has("createdAt")).isTrue();
        assertThat(generatedBody.has("tags")).isTrue();
    }

    @Test
    void canGenerateOnlyRequiredSchemaProperties() throws Exception {
        OpenApiMockContract contract =
                loader.load("fixtures/pet-store-mock.yaml", false, Duration.ZERO);

        var body =
                objectMapper.readTree(operation(contract, "createPet").responses().get(201).body());
        assertThat(body.has("id")).isTrue();
        assertThat(body.has("status")).isTrue();
        assertThat(body.has("active")).isFalse();
        assertThat(body.has("createdAt")).isFalse();
        assertThat(body.has("tags")).isFalse();
    }

    @Test
    void rejectsMissingContractResources() {
        assertThatThrownBy(
                        () -> loader.load("classpath:fixtures/missing.yaml", true, Duration.ZERO))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("does not exist");
    }

    private static OpenApiMockOperation operation(
            OpenApiMockContract contract, String operationId) {
        return contract.operations().stream()
                .filter(operation -> operation.operationId().equals(operationId))
                .findFirst()
                .orElseThrow();
    }
}
