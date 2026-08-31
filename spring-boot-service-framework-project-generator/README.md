# Spring Boot Service Framework Project Generator

Build-time generator for creating a compilable Spring Boot service from an
OpenAPI document or a published server API JAR. Spring Initializr creates the
base project and the framework contributor adds explicit hexagonal boundaries.

Generation is a one-time bootstrap operation. Existing application code is not
managed or synchronized after the project is created.

## When to use

Use this module to start a new service that must implement a generated OpenAPI
server delegate. Use the OpenAPI Gradle plugin instead when the goal is only to
generate and publish models, server APIs, or clients.

## Dependency

```groovy
implementation 'com.smbtech:spring-boot-service-framework-project-generator:0.5.0'
```

The CLI can also be executed from this repository without adding it to an
application runtime.

## Public API

- `HexagonalProjectGenerator`: project generation contract and default factory.
- `ProjectGenerationRequest`: immutable request with a builder.
- `OpenApiDocumentSource`: YAML or JSON OpenAPI input.
- `ServerApiJarSource`: generated server API JAR input.
- `GeneratedProject`: generated location, coordinate, and delegate types.
- `ProjectGenerationException`: validation and generation failure.

`HexagonalProjectGenerator.create()` returns the internal framework default.
Generator contributors, contract parsing and Spring Initializr composition are
not public extension points.

## What this module does not do

It does not generate OpenAPI JARs, publish contracts, infer domain entities from
DTOs, or overwrite a service when a contract changes. Transport models stay in
the generated contract artifact; application domain design remains explicit.

## Main documentation

- [OpenAPI Portal](../docs/openapi/index.md)
- [Project Scaffolding](../docs/openapi/scaffolding.md)
- [OpenAPI Artifact Generation](../docs/openapi/generation.md)
- [OpenAPI Documentation Architecture](../docs/openapi/documentation-architecture.md)
- [OpenAPI Generator ADR](../docs/adr/0001-openapi-generator-engine.md)

## Local validation

```bash
./gradlew :spring-boot-service-framework-project-generator:check
```

The canonical [Project Scaffolding](../docs/openapi/scaffolding.md) reference
defines all inputs, defaults, output files, safety rules, architecture
boundaries, Java API usage, and failure recovery.
