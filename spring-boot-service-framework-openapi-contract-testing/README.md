# Spring Boot Service Framework OpenAPI Contract Testing

Test utility that executes Spring MVC endpoints with `MockMvc` and verifies
their responses against an OpenAPI 3 contract.

## When to use

Use this module in application tests when a controller implementation must keep
the status, content type, and JSON response shape declared by its OpenAPI file.

## Dependency

```groovy
dependencies {
    testImplementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.2'
    )
    testImplementation 'com.smbtech:spring-boot-service-framework-openapi-contract-testing'
}
```

## Public API

- `OpenApiContractLoader` loads YAML or JSON contracts from a path, stream, or
  classpath resource.
- `OpenApiContractTestCase` identifies an `operationId` and supplies dynamic
  path parameters, query parameters, headers, request JSON, and expected status.
- `OpenApiMvcContractTester` executes cases through `MockMvc`.
- `OpenApiMvcContractTester.verifyAll(...)` also fails when a declared operation
  has no test case.
- `OpenApiContractTestResult` and `OpenApiContractViolation` provide an
  inspectable result; `throwIfInvalid()` integrates with JUnit.

Parser-only request definitions remain package-private. The types listed above
are the complete supported Java surface.

The complete usage contract and examples live in
[OpenAPI Contract Testing](../docs/openapi-contract-testing.md).

## What this module does not do

- It does not start an application or a real HTTP server.
- It does not generate OpenAPI artifacts.
- It does not replace business-focused controller tests.
- It does not currently validate request schemas, external `$ref` files, or
  composed schemas such as `oneOf`, `anyOf`, and `allOf`.

## Main documentation

| Topic | Document |
|---|---|
| OpenAPI navigation | [OpenAPI Portal](../docs/openapi/index.md) |
| Contract testing reference | [OpenAPI Contract Testing](../docs/openapi-contract-testing.md) |
| Copy-ready test setup | [Test a Spring MVC API Against OpenAPI](../docs/guides/openapi-contract-testing.md) |
| OpenAPI generation | [OpenAPI Artifact Generation](../docs/openapi/generation.md) |
| Documentation ownership | [OpenAPI Documentation Architecture](../docs/openapi/documentation-architecture.md) |
| Compatibility policy | [Compatibility](../docs/compatibility.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-openapi-contract-testing:check
./gradlew contractTestingCompatibilityCheck
```
