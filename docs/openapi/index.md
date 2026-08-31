# OpenAPI

Use this portal to choose the shortest path for generating, publishing,
implementing, consuming, testing, or maintaining OpenAPI contracts with the
framework.

## Choose A Goal

| Goal | Start here | Outcome |
|---|---|---|
| Complete the first end-to-end workflow | [Getting Started](getting-started.md) | Create, validate, generate, publish, implement, and consume one contract. |
| Run the repository fixtures | [OpenAPI Examples](examples.md) | Exercise evolution, multi-operation generation, publication, and scaffolding. |
| Understand generated artifacts | [Artifact Generation](generation.md) | Learn how one contract becomes separated models, server API, and client JARs. |
| Review the exact current contract | [Current Behavior Inventory](../openapi-behavior-inventory.md) | Inspect the protected DSL, defaults, metadata, tasks, and toolchain. |
| Generate artifacts | [Generate OpenAPI Contract Artifacts](../guides/openapi-generated-artifacts.md) | Produce consumable Maven artifacts from an OpenAPI document. |
| Validate configuration and contracts | [OpenAPI Validation](validation.md) | Check DSL, documents, coordinates, and generated compatibility. |
| Publish artifacts locally | [Artifact Publishing](publishing.md) | Publish and consume a project-local Maven repository. |
| Implement a generated server API | [Artifact Generation](generation.md) | Understand the delegate and controller boundary before implementing it. |
| Consume a generated HTTP client | [Artifact Generation](generation.md) | Understand the HTTP interface and model dependencies before consuming it. |
| Publish artifacts remotely | [Artifact Publishing](publishing.md) | Configure a private Maven registry without committing credentials. |
| Define or review contract versions | [Contract Versioning](versioning.md) | Apply immutable baselines and the framework SemVer policy. |
| Run a focused breaking-change procedure | [Check OpenAPI Breaking Changes](../guides/check-openapi-breaking-changes.md) | Update a contract and review its compatibility result. |
| Test a Spring MVC implementation | [OpenAPI Contract Testing](../openapi-contract-testing.md) | Verify requests and responses against declared operations. |
| Follow a copy-ready MVC test recipe | [Test a Spring MVC API Against OpenAPI](../guides/openapi-contract-testing.md) | Add executable contract coverage to an application test suite. |
| Serve mock responses | [Mock Core And Starter](../mock.md) | Load OpenAPI examples as explicitly enabled runtime mock routes. |
| Scaffold a new service | [Project Scaffolding](scaffolding.md) | Create a one-time Spring Boot service with hexagonal boundaries. |
| Diagnose a failure | [OpenAPI Troubleshooting](troubleshooting.md) | Find the failing task, inspect its evidence, and apply a focused recovery. |

## Choose By Role

| Role | Recommended path |
|---|---|
| API designer | Start with [Contract Versioning](versioning.md), then review the [Current Behavior Inventory](../openapi-behavior-inventory.md). |
| Service implementer | Follow [Generate OpenAPI Contract Artifacts](../guides/openapi-generated-artifacts.md), then use [Project Scaffolding](scaffolding.md) when starting a new service. |
| Client developer | Review [Artifact Generation](generation.md) and the [REST Client Guide](../rest-client.md). |
| Test engineer | Use [OpenAPI Contract Testing](../openapi-contract-testing.md) and [Mock Core And Starter](../mock.md). |
| Build or platform maintainer | Read the [Current Behavior Inventory](../openapi-behavior-inventory.md), [Documentation Architecture](documentation-architecture.md), and [OpenAPI Generator ADR](../adr/0001-openapi-generator-engine.md). |

## Reference

| Topic | Canonical document |
|---|---|
| Generated artifact contents, packages, dependencies, and outputs | [Artifact Generation](generation.md) |
| Gradle plugin, DSL, defaults, validation, and tasks | [Gradle Plugin Reference](plugin-reference.md) |
| Local and remote Maven publication | [Artifact Publishing](publishing.md) |
| Coordinate conventions | [OpenAPI Code Generation](../openapi-codegen.md) |
| Configuration, document, coordinate, and compatibility rules | [OpenAPI Validation](validation.md) |
| Implemented behavior protected by checks | [Current Behavior Inventory](../openapi-behavior-inventory.md) |
| Contract identity, baselines, structural changes, and SemVer | [Contract Versioning](versioning.md) |
| Spring MVC request and response verification | [OpenAPI Contract Testing](../openapi-contract-testing.md) |
| One-time Spring Boot service generation and hexagonal boundaries | [Project Scaffolding](scaffolding.md) |
| Executable repository contracts and expected evidence | [OpenAPI Examples](examples.md) |
| Task failures, reports, messages, and recovery | [OpenAPI Troubleshooting](troubleshooting.md) |
| Documentation ownership and automated validation | [Documentation Architecture](documentation-architecture.md) |
| Framework compatibility policy | [Compatibility](../compatibility.md) |

## Implementation Boundaries

| Boundary | Documentation |
|---|---|
| Public Gradle plugin and task orchestration | [OpenAPI Gradle Plugin](../../build-logic/openapi-generator-plugin/README.md) |
| Versioned OpenAPI Generator templates | [OpenAPI Templates](../../build-logic/openapi-templates/README.md) |
| Executable Spring MVC contract tests | [OpenAPI Contract Testing Module](../../spring-boot-service-framework-openapi-contract-testing/README.md) |
| One-time Spring Boot project scaffolding implementation | [Project Generator](../../spring-boot-service-framework-project-generator/README.md) |
| Source-generation engine decision | [ADR 0001](../adr/0001-openapi-generator-engine.md) |

## Repository Examples

See [OpenAPI Examples](examples.md) for commands, expected artifacts, reports,
and the distinct purpose of each fixture.

| Contract | Purpose |
|---|---|
| [Merchant Order Status](merchant-order-status.yaml) | Demonstrates versioned evolution against immutable baselines. |
| [Retail Loyalty Rewards](retail-loyalty-rewards.yaml) | Exercises models, server API, client generation, and publication. |
| [Warehouse Inventory Catalog](warehouse-inventory-catalog.yaml) | Supports generated artifacts and hexagonal project scaffolding. |
