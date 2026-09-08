package com.smbtech.examples.openapicontract;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.contracts.warehouseinventorycatalog.v1.api.DefaultApiController;
import com.smbtech.contracts.warehouseinventorycatalog.v1.api.DefaultApiDelegate;
import com.smbtech.contracts.warehouseinventorycatalog.v1.model.InventoryItemResponse;
import com.smbtech.contracts.warehouseinventorycatalog.v1.model.InventoryStatus;
import com.smbtech.contracts.warehouseinventorycatalog.v1.model.WarehouseSummary;
import com.smbtech.serviceframework.openapi.contract.OpenApiContract;
import com.smbtech.serviceframework.openapi.contract.OpenApiContractLoader;
import com.smbtech.serviceframework.openapi.contract.OpenApiContractTestCase;
import com.smbtech.serviceframework.openapi.contract.OpenApiContractTestResult;
import com.smbtech.serviceframework.openapi.contract.OpenApiContractViolation;
import com.smbtech.serviceframework.openapi.contract.OpenApiContractViolationCode;
import com.smbtech.serviceframework.openapi.contract.OpenApiMvcContractTester;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.banner-mode=off")
class GeneratedContractRuntimeIntegrationTest {

    private static final String CONTRACT_RESOURCE =
            "META-INF/smbtech/openapi/contracts/warehouse-inventory-catalog/1.0.0/contract.yaml";

    @Autowired private WebApplicationContext webApplicationContext;

    @Autowired private ApplicationContext applicationContext;

    @LocalServerPort private int port;

    @Test
    void validatesEveryPublishedContractOperationAgainstGeneratedServerApi() throws Exception {
        OpenApiContract contract = new OpenApiContractLoader().loadClasspath(CONTRACT_RESOURCE);
        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(
                                MockMvcBuilders.webAppContextSetup(webApplicationContext).build(),
                                new ObjectMapper(),
                                contract)
                        .verifyAll(validContractCase());

        assertThat(contract.operations())
                .extracting("operationId")
                .containsExactly("getWarehouseInventoryItem");
        result.throwIfInvalid();
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void generatedHttpInterfaceAndOpenFeignClientsUseTheSameRuntimeAndModel() {
        String baseUrl = "http://127.0.0.1:" + port;
        com.smbtech.contracts.warehouseinventorycatalog.v1.client.httpinterface.DefaultApi
                httpInterface =
                        HttpServiceProxyFactory.builderFor(
                                        RestClientAdapter.create(
                                                RestClient.builder().baseUrl(baseUrl).build()))
                                .build()
                                .createClient(
                                        com.smbtech.contracts.warehouseinventorycatalog.v1.client
                                                .httpinterface.DefaultApi.class);
        com.smbtech.contracts.warehouseinventorycatalog.v1.client.openfeign.DefaultApi openFeign =
                new FeignClientBuilder(applicationContext)
                        .forType(
                                com.smbtech.contracts.warehouseinventorycatalog.v1.client.openfeign
                                        .DefaultApi.class,
                                "warehouse-inventory-contract-test")
                        .url(baseUrl)
                        .build();

        ResponseEntity<InventoryItemResponse> httpResponse =
                httpInterface.getWarehouseInventoryItem("WH-01", "SKU-100");
        ResponseEntity<InventoryItemResponse> feignResponse =
                openFeign.getWarehouseInventoryItem("WH-01", "SKU-100");

        assertThat(httpResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(feignResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(httpResponse.getBody()).isEqualTo(feignResponse.getBody());
        assertThat(httpResponse.getBody())
                .extracting(
                        InventoryItemResponse::getSku,
                        InventoryItemResponse::getStatus,
                        InventoryItemResponse::getQuantity)
                .containsExactly("SKU-100", InventoryStatus.AVAILABLE, 42L);
    }

    @Test
    void reportsSchemaViolationsFromAnInvalidGeneratedDelegateResponse() throws Exception {
        MockMvc invalidRuntime =
                MockMvcBuilders.standaloneSetup(new DefaultApiController(new InvalidDelegate()))
                        .build();
        OpenApiContract contract = new OpenApiContractLoader().loadClasspath(CONTRACT_RESOURCE);

        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(invalidRuntime, new ObjectMapper(), contract)
                        .verifyAll(validContractCase());

        assertThat(result.isValid()).isFalse();
        assertThat(result.violations())
                .extracting(OpenApiContractViolation::code)
                .contains(OpenApiContractViolationCode.RESPONSE_SCHEMA_MISMATCH);
    }

    @Test
    void rejectsAnExpectedStatusThatThePublishedContractDoesNotDeclare() throws Exception {
        OpenApiContract contract = new OpenApiContractLoader().loadClasspath(CONTRACT_RESOURCE);
        OpenApiContractTestCase undeclaredErrorCase =
                OpenApiContractTestCase.forOperation("getWarehouseInventoryItem")
                        .pathParameter("warehouseId", "WH-01")
                        .pathParameter("sku", "SKU-100")
                        .expectedStatus(404)
                        .build();

        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(
                                MockMvcBuilders.webAppContextSetup(webApplicationContext).build(),
                                new ObjectMapper(),
                                contract)
                        .verify(undeclaredErrorCase);

        assertThat(result.violations())
                .extracting(OpenApiContractViolation::code)
                .containsExactly(OpenApiContractViolationCode.UNDECLARED_STATUS);
    }

    private static OpenApiContractTestCase validContractCase() {
        return OpenApiContractTestCase.forOperation("getWarehouseInventoryItem")
                .pathParameter("warehouseId", "WH-01")
                .pathParameter("sku", "SKU-100")
                .build();
    }

    private static final class InvalidDelegate implements DefaultApiDelegate {

        @Override
        public ResponseEntity<InventoryItemResponse> getWarehouseInventoryItem(
                String warehouseId, String sku) {
            return ResponseEntity.ok(
                    new InventoryItemResponse(
                            "invalid sku",
                            InventoryStatus.AVAILABLE,
                            -1L,
                            new WarehouseSummary(warehouseId, "CHL")));
        }
    }
}
