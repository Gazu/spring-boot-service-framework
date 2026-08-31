# Spring Boot Service Framework OpenAPI Templates

Versioned OpenAPI Generator template bundle for framework contract artifacts.

The module is intentionally resource-only. It currently owns the corporate
Spring HTTP interface annotation fragment used to add `@HttpApiClient` to
generated client contracts. Additional model or server overrides belong here,
not in Gradle task code. The module must not contain parsing, task wiring,
publication logic, or application runtime behavior.

## Boundary

| Concern | Owner |
|---|---|
| OpenAPI Generator Mustache overrides | This module |
| Gradle DSL and task orchestration | `spring-boot-service-framework-openapi-gradle-plugin` |
| Hexagonal project scaffolding | `spring-boot-service-framework-project-generator` |

## Documentation

Start from the [OpenAPI Portal](../../docs/openapi/index.md). See
[OpenAPI Documentation Architecture](../../docs/openapi/documentation-architecture.md)
for canonical ownership and
[OpenAPI Artifact Generation](../../docs/openapi/generation.md) for generated
source and artifact behavior.

## Validation

```bash
./gradlew -p build-logic :spring-boot-service-framework-openapi-templates:check
```
