package com.smbtech.serviceframework.openapi.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class OpenApiContractLoaderTest {

    private final OpenApiContractLoader loader = new OpenApiContractLoader();

    @Test
    void loadsOperationsAndResponsesFromClasspathYaml() throws Exception {
        OpenApiContract contract = loader.loadClasspath("contracts/inventory-api.yaml");

        assertThat(contract.title()).isEqualTo("contract-test-inventory");
        assertThat(contract.version()).isEqualTo("1.0.0");
        assertThat(contract.operations()).hasSize(2);

        OpenApiOperation operation = contract.findOperation("getInventoryItem").orElseThrow();
        assertThat(operation.method()).isEqualTo("GET");
        assertThat(operation.pathTemplate()).isEqualTo("/inventory/{sku}");
        assertThat(operation.requiredPathParameters()).containsExactly("sku");
        assertThat(operation.responses()).containsOnlyKeys(200, 404);
        assertThat(operation.successfulResponse().contentSchemas()).containsKey("application/json");

        OpenApiRequestDefinition request = contract.requestDefinition("createInventoryItem");
        assertThat(request.parameters()).hasSize(4);
        assertThat(request.findParameter("query", "dryRun"))
                .get()
                .extracting(OpenApiRequestParameter::required)
                .isEqualTo(true);
        assertThat(request.requestBody().required()).isTrue();
        assertThat(request.requestBody().contentSchemas()).containsKey("application/json");
    }

    @Test
    void rejectsMissingClasspathResource() {
        assertThatThrownBy(() -> loader.loadClasspath("contracts/missing.yaml"))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void acceptsOpenApi31AndRejectsSwagger2() throws Exception {
        OpenApiContract contract = loader.load(document("3.1.1"), "openapi-31.yaml");

        assertThat(contract.title()).isEqualTo("inventory");
        assertThatThrownBy(() -> loader.load(swagger2Document(), "swagger-2.yaml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OpenAPI 3.0.x or 3.1.x");
    }

    private static ByteArrayInputStream document(String version) {
        return input(
                """
                openapi: %s
                info:
                  title: inventory
                  version: 1.0.0
                paths: {}
                """
                        .formatted(version));
    }

    private static ByteArrayInputStream swagger2Document() {
        return input(
                """
                swagger: '2.0'
                info:
                  title: inventory
                  version: 1.0.0
                paths: {}
                """);
    }

    private static ByteArrayInputStream input(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
