package com.smbtech.serviceframework.openapi.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

class OpenApiMvcContractTesterTest {

    private OpenApiContract contract;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        contract = new OpenApiContractLoader().loadClasspath("contracts/inventory-api.yaml");
        objectMapper = new ObjectMapper();
    }

    @Test
    void acceptsDeclaredStatusContentTypeAndResponseSchema() {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ValidInventoryController()).build();
        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(mockMvc, objectMapper, contract)
                        .verify(
                                OpenApiContractTestCase.forOperation("getInventoryItem")
                                        .pathParameter("sku", "SKU 100")
                                        .header("X-Test-Run", "contract")
                                        .build());

        assertThat(result.isValid()).isTrue();
        assertThat(result.violations()).isEmpty();
        result.throwIfInvalid();
    }

    @Test
    void reportsAllSchemaViolationsWithJsonLocations() {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InvalidInventoryController()).build();
        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(mockMvc, objectMapper, contract)
                        .verify(
                                OpenApiContractTestCase.forOperation("getInventoryItem")
                                        .pathParameter("sku", "SKU-100")
                                        .build());

        assertThat(result.isValid()).isFalse();
        assertThat(result.violations())
                .extracting(OpenApiContractViolation::location)
                .contains("$.status", "$.quantity");
        assertThatThrownBy(result::throwIfInvalid)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("RESPONSE_SCHEMA_MISMATCH")
                .hasMessageContaining("$.status")
                .hasMessageContaining("$.quantity");
    }

    @Test
    void reportsUnknownOperationsAndMissingPathParametersWithoutExecuting() {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ValidInventoryController()).build();
        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(mockMvc, objectMapper, contract)
                        .verify(
                                OpenApiContractTestCase.forOperation("missingOperation").build(),
                                OpenApiContractTestCase.forOperation("getInventoryItem").build());

        assertThat(result.violations())
                .extracting(OpenApiContractViolation::code)
                .containsExactly(
                        OpenApiContractViolationCode.UNKNOWN_OPERATION,
                        OpenApiContractViolationCode.MISSING_PATH_PARAMETER);
    }

    @Test
    void failsWhenControllerReturnsAnErrorInsteadOfTheDefaultSuccessResponse() {
        MockMvc mockMvc =
                MockMvcBuilders.standaloneSetup(new NotFoundInventoryController()).build();
        OpenApiMvcContractTester tester =
                new OpenApiMvcContractTester(mockMvc, objectMapper, contract);

        OpenApiContractTestResult unexpected =
                tester.verify(
                        OpenApiContractTestCase.forOperation("getInventoryItem")
                                .pathParameter("sku", "UNKNOWN")
                                .build());
        OpenApiContractTestResult explicitErrorCase =
                tester.verify(
                        OpenApiContractTestCase.forOperation("getInventoryItem")
                                .pathParameter("sku", "UNKNOWN")
                                .expectedStatus(404)
                                .build());

        assertThat(unexpected.violations())
                .extracting(OpenApiContractViolation::code)
                .containsExactly(OpenApiContractViolationCode.UNEXPECTED_STATUS);
        assertThat(explicitErrorCase.isValid()).isTrue();
    }

    @Test
    void verifyAllReportsOperationsWithoutATestCase() {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ValidInventoryController()).build();

        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(mockMvc, objectMapper, contract).verifyAll();

        assertThat(result.violations())
                .extracting(OpenApiContractViolation::code)
                .containsExactly(
                        OpenApiContractViolationCode.MISSING_TEST_CASE,
                        OpenApiContractViolationCode.MISSING_TEST_CASE);
        assertThat(result.violations())
                .extracting(OpenApiContractViolation::operationId)
                .containsExactlyInAnyOrder("getInventoryItem", "createInventoryItem");
    }

    @Test
    void validatesRequestParametersBodyAndSchemaBeforeExecution() {
        RequestInventoryController controller = new RequestInventoryController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(mockMvc, objectMapper, contract)
                        .verify(validCreateCase().build());

        assertThat(result.isValid()).isTrue();
        assertThat(controller.calls).isEqualTo(1);
    }

    @Test
    void reportsMissingRequiredRequestValuesWithoutExecutingController() {
        RequestInventoryController controller = new RequestInventoryController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(mockMvc, objectMapper, contract)
                        .verify(
                                OpenApiContractTestCase.forOperation("createInventoryItem")
                                        .pathParameter("sku", "SKU-200")
                                        .build());

        assertThat(result.violations())
                .extracting(OpenApiContractViolation::code)
                .containsExactly(
                        OpenApiContractViolationCode.MISSING_REQUEST_PARAMETER,
                        OpenApiContractViolationCode.MISSING_REQUEST_PARAMETER,
                        OpenApiContractViolationCode.MISSING_REQUEST_PARAMETER,
                        OpenApiContractViolationCode.MISSING_REQUEST_BODY);
        assertThat(controller.calls).isZero();
    }

    @Test
    void reportsInvalidRequestParameterAndJsonWithoutExecutingController() {
        RequestInventoryController controller = new RequestInventoryController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(mockMvc, objectMapper, contract)
                        .verify(
                                OpenApiContractTestCase.forOperation("createInventoryItem")
                                        .pathParameter("sku", "SKU-200")
                                        .queryParameter("dryRun", "sometimes")
                                        .header("x-tenant", "tenant-a")
                                        .cookie("session", "session-1")
                                        .jsonBody("{")
                                        .build());

        assertThat(result.violations())
                .extracting(OpenApiContractViolation::code)
                .containsExactly(
                        OpenApiContractViolationCode.REQUEST_SCHEMA_MISMATCH,
                        OpenApiContractViolationCode.INVALID_JSON_REQUEST);
        assertThat(controller.calls).isZero();
    }

    @Test
    void reportsRequestSchemaAndContentTypeViolations() {
        RequestInventoryController controller = new RequestInventoryController();
        OpenApiMvcContractTester tester =
                new OpenApiMvcContractTester(
                        MockMvcBuilders.standaloneSetup(controller).build(),
                        objectMapper,
                        contract);

        OpenApiContractTestResult schemaResult =
                tester.verify(
                        validCreateCase()
                                .jsonBody("{\"status\":\"damaged\",\"quantity\":0}")
                                .build());
        OpenApiContractTestResult contentTypeResult =
                tester.verify(
                        validCreateCase()
                                .body("status=available", "application/x-www-form-urlencoded")
                                .build());

        assertThat(schemaResult.violations())
                .extracting(OpenApiContractViolation::code)
                .containsExactly(
                        OpenApiContractViolationCode.REQUEST_SCHEMA_MISMATCH,
                        OpenApiContractViolationCode.REQUEST_SCHEMA_MISMATCH);
        assertThat(contentTypeResult.violations())
                .extracting(OpenApiContractViolation::code)
                .containsExactly(OpenApiContractViolationCode.UNDECLARED_REQUEST_CONTENT_TYPE);
        assertThat(controller.calls).isZero();
    }

    @Test
    void reportsBodyForAnOperationThatDoesNotDeclareOne() {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ValidInventoryController()).build();

        OpenApiContractTestResult result =
                new OpenApiMvcContractTester(mockMvc, objectMapper, contract)
                        .verify(
                                OpenApiContractTestCase.forOperation("getInventoryItem")
                                        .pathParameter("sku", "SKU-100")
                                        .jsonBody("{}")
                                        .build());

        assertThat(result.violations())
                .extracting(OpenApiContractViolation::code)
                .containsExactly(OpenApiContractViolationCode.UNDECLARED_REQUEST_BODY);
    }

    private static OpenApiContractTestCase.Builder validCreateCase() {
        return OpenApiContractTestCase.forOperation("createInventoryItem")
                .pathParameter("sku", "SKU-200")
                .queryParameter("dryRun", "false")
                .header("X-Tenant", "tenant-a")
                .cookie("session", "session-1")
                .jsonBody("{\"status\":\"available\",\"quantity\":4}");
    }

    @RestController
    private static final class ValidInventoryController {

        @GetMapping(value = "/inventory/{sku}", produces = MediaType.APPLICATION_JSON_VALUE)
        InventoryItem get(@PathVariable String sku) {
            return new InventoryItem(sku, "available", 12);
        }
    }

    @RestController
    private static final class InvalidInventoryController {

        @GetMapping(value = "/inventory/{sku}", produces = MediaType.APPLICATION_JSON_VALUE)
        InvalidInventoryItem get(@PathVariable String sku) {
            return new InvalidInventoryItem(sku, "damaged", "many", List.of("unexpected"));
        }
    }

    @RestController
    private static final class NotFoundInventoryController {

        @GetMapping("/inventory/{sku}")
        org.springframework.http.ResponseEntity<Void> get(@PathVariable String sku) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }

    @RestController
    private static final class RequestInventoryController {

        private int calls;

        @PostMapping(
                value = "/inventory/{sku}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
        org.springframework.http.ResponseEntity<InventoryItem> create(
                @PathVariable String sku,
                @RequestParam boolean dryRun,
                @RequestHeader("X-Tenant") String tenant,
                @CookieValue("session") String session,
                @RequestBody CreateInventoryItem request) {
            calls++;
            return org.springframework.http.ResponseEntity.status(201)
                    .body(new InventoryItem(sku, request.status(), request.quantity()));
        }
    }

    private record InventoryItem(String sku, String status, int quantity) {}

    private record InvalidInventoryItem(
            String sku, String status, String quantity, List<String> extra) {}

    private record CreateInventoryItem(String status, int quantity) {}
}
