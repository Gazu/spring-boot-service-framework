# Spring Boot Service Framework Mock Core

Framework-neutral mock domain, ports, and default services. It is the
hexagonal core for mock response behavior without Spring, Jackson, Servlet,
`RestClient`, SLF4J, Apache, or other adapter APIs.

Runtime integration belongs in
`spring-boot-service-framework-starter-mock`.

## When to use

Most applications should consume
`spring-boot-service-framework-starter-mock`.

Use this module directly when building framework-neutral mock adapters, tests,
or custom clients that should not depend on Spring, Jackson, Servlet, or
`RestClient` APIs.

## Dependency

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.0'
    )
    implementation 'com.smbtech:spring-boot-service-framework-mock-core'
}
```

## Public API

- Domain types: `MockDefinition`, `MockRequest`, `MockResponse`, and
  related value objects.
- Inbound ports: `MockCatalog` and `MockResponder`.
- Outbound ports: `MockDefinitionSource` and `MockResponseSource`.
- Public exception: `MockException`.

Create the default implementations with `MockCatalog.from(...)` and
`MockResponder.from(...)`. Their concrete types are internal. See
[Public API Boundaries](../docs/public-api-boundaries.md).

## What this module does not do

- It does not load Spring resources or Jackson JSON by itself.
- It does not expose Spring MVC `ResponseEntity` helpers.
- It does not add `RestClient` interceptors.
- It does not contain business-specific mock scenarios.

## Main documentation

| Topic | Document |
|---|---|
| Mock guide | [Mock Core and Starter](../docs/mock.md) |
| Mock property reference | [Mock Property Reference](../docs/mock/property-reference.md) |
| Mock starter README | [Mock Starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-mock/README.md) |
| Names and packages migration | [Migration Guide](../docs/guides/migrate-public-names-and-properties.md) |
| Module README rules | [Module README Convention](../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-mock-core:check
./gradlew mockCompatibilityCheck
```
