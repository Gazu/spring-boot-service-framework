# Test a Spring MVC API Against OpenAPI

Use this recipe to verify a Spring MVC controller or a generated server API
implementation with the committed OpenAPI contract.

## 1. Add The Test Dependency

```groovy
dependencies {
    testImplementation 'com.smbtech:spring-boot-service-framework-openapi-contract-testing:0.3.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

## 2. Put The Contract On The Test Classpath

```text
src/test/resources/openapi/inventory-api.yaml
```

Use the same versioned contract that produced the models and server API JARs.

## 3. Execute Every Important Operation

```java
OpenApiContract contract = new OpenApiContractLoader()
        .loadClasspath("openapi/inventory-api.yaml");

OpenApiContractTestResult result = new OpenApiMvcContractTester(
        mockMvc,
        objectMapper,
        contract
).verifyAll(
        OpenApiContractTestCase.forOperation("getInventoryItem")
                .pathParameter("sku", "SKU-100")
                .build(),
        OpenApiContractTestCase.forOperation("getInventoryItem")
                .pathParameter("sku", "UNKNOWN")
                .expectedStatus(404)
                .build()
);

result.throwIfInvalid();
```

Provide data that makes the application reach the intended path. The tester
validates the HTTP contract; fixtures and business state remain owned by the
application test.

## 4. Run The Test

```bash
./gradlew test
```

The full API and supported schema subset are documented in
[OpenAPI Contract Testing](../openapi-contract-testing.md).
