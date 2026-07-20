package com.smbtech.serviceframework.openapi.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
