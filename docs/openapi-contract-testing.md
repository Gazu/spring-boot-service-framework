# OpenAPI Contract Testing

The `spring-boot-service-framework-openapi-contract-testing` module executes
Spring MVC endpoints through `MockMvc` and checks their actual responses against
an OpenAPI 3.0 or 3.1 YAML/JSON document. It is intended for application test
scope. Swagger 2 documents are rejected explicitly.

## Dependency

```groovy
dependencies {
    testImplementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.0'
    )
    testImplementation 'com.smbtech:spring-boot-service-framework-openapi-contract-testing'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

## Verification Flow

For each `OpenApiContractTestCase`, the tester:

1. Resolves the OpenAPI operation by `operationId`.
2. Substitutes required path parameters and adds query parameters and headers.
3. Executes the request through the application's `MockMvc` instance.
4. Expects the first declared `2xx` status unless the case selects another
   declared status with `expectedStatus(...)`.
5. Checks the response `Content-Type` against the declared media types.
6. Parses JSON and validates component `$ref` values, required properties,
   object, array and scalar types, nested properties, array items, and enums.
7. Returns all detected violations in one result. `verifyAll(...)` additionally
   reports each contract operation that has no supplied case.

An unmapped controller route normally produces a `404`, which is reported as an
unexpected status for the default successful test case.

## Basic Test

```java
@SpringBootTest
@AutoConfigureMockMvc
class InventoryContractTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void matchesOpenApiContract() throws Exception {
        OpenApiContract contract = new OpenApiContractLoader()
                .loadClasspath("openapi/inventory-api.yaml");

        OpenApiContractTestResult result = new OpenApiMvcContractTester(
                mockMvc,
                objectMapper,
                contract
        ).verifyAll(
                OpenApiContractTestCase.forOperation("getInventoryItem")
                        .pathParameter("sku", "SKU-100")
                        .header("X-Test-Run", "contract")
                        .build()
        );

        result.throwIfInvalid();
    }
}
```

Keep the OpenAPI file in the consuming application's test classpath, for
example `src/test/resources/openapi/inventory-api.yaml`.

## Error Response Cases

The default expected status is the lowest declared `2xx` response. Select an
explicit declared response when testing an error path:

```java
OpenApiContractTestCase.forOperation("getInventoryItem")
        .pathParameter("sku", "UNKNOWN")
        .expectedStatus(404)
        .build();
```

If the selected status is not declared in the OpenAPI operation, the result
contains `UNDECLARED_STATUS`. If the endpoint returns another status, it
contains `UNEXPECTED_STATUS`.

## Inspecting Violations

`verify(...)` does not stop at the first response schema mismatch. The result
contains stable codes and JSON locations:

```text
OpenAPI contract violations:
- [RESPONSE_SCHEMA_MISMATCH] getInventoryItem $.status: value is not part of the declared enum
- [RESPONSE_SCHEMA_MISMATCH] getInventoryItem $.quantity: expected integer but found string
```

Use `result.violations()` for custom assertions or reporting, and
`result.throwIfInvalid()` for the normal JUnit failure path.

Use `verify(...)` for focused tests of only selected operations. Prefer
`verifyAll(...)` for the API-level suite so newly added OpenAPI operations fail
with `MISSING_TEST_CASE` until a case is supplied.

## Supported Scope

Current support includes:

- OpenAPI 3.0 and 3.1 YAML and JSON documents;
- operation lookup by unique `operationId`;
- `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS`, and `TRACE`;
- path parameter substitution;
- query parameters, headers, and JSON request bodies supplied by the test;
- explicit numeric response statuses;
- media type compatibility;
- internal component schema `$ref` values;
- object required properties, nested values, arrays, scalar types, and enums.

Request-schema validation, response headers, format and numeric constraints,
external `$ref`, composed schemas, multipart requests, and WebFlux are future
extensions.

## Validation

Run the module tests:

```bash
./gradlew :spring-boot-service-framework-openapi-contract-testing:check
```

Run the complete repository compatibility gate:

```bash
./gradlew compatibilityCheck
```

See [Test a Spring MVC API Against OpenAPI](guides/openapi-contract-testing.md)
for a focused adoption recipe and [OpenAPI Code Generation](openapi-codegen.md)
for generated artifact behavior.
