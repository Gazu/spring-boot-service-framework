# OpenAPI Code Generation

The framework treats an OpenAPI document as a build-time contract source. One
Gradle plugin validates the document and coordinates, delegates Java generation
to OpenAPI Generator, assembles separated Maven artifacts, and verifies their
compatibility. Runtime applications consume those artifacts without depending
on generator implementation classes.

This page is an architectural overview. Exact options, commands, artifact
contents, and policies belong to the references linked below.

## Capability Map

| Need | Canonical documentation |
|---|---|
| Complete the first workflow | [OpenAPI Getting Started](openapi/getting-started.md) |
| Configure the Gradle plugin and tasks | [OpenAPI Gradle Plugin Reference](openapi/plugin-reference.md) |
| Understand models, server API, and client JARs | [OpenAPI Artifact Generation](openapi/generation.md) |
| Validate configuration, specs, and compatibility | [OpenAPI Validation](openapi/validation.md) |
| Publish artifacts to Maven repositories | [OpenAPI Artifact Publishing](openapi/publishing.md) |
| Manage identity, baselines, and SemVer | [OpenAPI Contract Versioning](openapi/versioning.md) |
| Test a Spring MVC implementation | [OpenAPI Contract Testing](openapi-contract-testing.md) |
| Bootstrap a new service | [OpenAPI Project Scaffolding](openapi/scaffolding.md) |

## Build-Time Boundary

Generation, compilation, packaging, metadata creation, publication, structural
comparison, reproducibility checks, and consumer checks run in Gradle. Generated
source remains under the build directory and is never an application-owned
editing surface.

The plugin produces three independently consumable boundaries:

- transport models shared by providers and consumers;
- a Spring MVC server contract implemented through delegates;
- declarative Spring HTTP client interfaces.

The exact package layout, dependencies, annotations, metadata, and artifact
coordinates are owned by
[OpenAPI Artifact Generation](openapi/generation.md).

## Runtime Boundary

Provider applications implement generated delegate interfaces and keep domain
behavior outside generated controllers. Consumer applications inject generated
HTTP interfaces while the REST client starter supplies transport,
authentication, TLS, resilience, observability, and endpoint configuration.

Generated artifacts do not contain business implementations, credentials,
environment URLs, or application-specific domain models.

## Workflow Boundaries

Artifact generation and service scaffolding are separate operations. Generation
is repeatable for every contract version and creates publishable JARs.
Scaffolding is a one-time bootstrap that creates an application repository from
a document or a generated server API JAR.

Contract testing is also separate. Build-time validation proves the shape and
compatibility of generated contracts; runtime contract tests prove that a
Spring MVC implementation returns responses allowed by the document.

## Ownership

| Boundary | Owner |
|---|---|
| Gradle DSL and orchestration | `build-logic/openapi-generator-plugin` |
| OpenAPI Generator overrides | `build-logic/openapi-templates` |
| Spring MVC contract verification | `spring-boot-service-framework-openapi-contract-testing` |
| One-time hexagonal service generation | `spring-boot-service-framework-project-generator` |

The module split and decision to use OpenAPI Generator are recorded in
[ADR 0001](adr/0001-openapi-generator-engine.md). Documentation ownership is
defined by the
[OpenAPI Documentation Architecture](openapi/documentation-architecture.md).

## Validation

Documentation ownership and duplication are checked by `documentationCheck`.
The executable OpenAPI build gates are defined in
[OpenAPI Validation](openapi/validation.md).
