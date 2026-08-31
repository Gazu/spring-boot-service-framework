# Test a Spring MVC API Against OpenAPI

Use this recipe to verify a Spring MVC controller or a generated server API
implementation with the committed OpenAPI contract.

## 1. Add The Test Dependency

Add the test-scope module and Spring Boot test support as documented in
[OpenAPI Contract Testing](../openapi-contract-testing.md#dependency).

## 2. Load The Published Contract

The generated models JAR already contains the exact source contract. Add it to
the test classpath and load its versioned resource:

```groovy
dependencies {
    testImplementation 'com.smbtech.contracts:warehouse-inventory-catalog-models:1.0.0'
}
```

```java
OpenApiContract contract = new OpenApiContractLoader().loadClasspath(
        "META-INF/smbtech/openapi/contracts/warehouse-inventory-catalog/1.0.0/contract.yaml"
);
```

## 3. Execute Every Important Operation

```java
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
