# Documentation Index

This index is the main entry point for Spring Boot Service Framework
documentation. Start here when you need to consume the framework, extend it, run
examples, or maintain the repository.

## Start Here

| Need | Read |
|---|---|
| Understand the repository, modules, and quick starts | [Repository README](../README.md) |
| Import the framework BOM with Gradle or Maven | [Dependency Management](dependency-management.md) |
| Check supported Java, Spring Boot, and compatibility rules | [Compatibility](compatibility.md) |
| Build applications with Spring AOT or GraalVM native-image | [Spring Boot AOT And Native Images](native-image.md) |
| Diagnose build, configuration, token, mock, or logging failures | [Troubleshooting](troubleshooting.md) |
| Understand documentation ownership and anti-duplication rules | [Documentation Architecture](documentation-architecture.md) |
| Review accepted cross-module architecture decisions | [Architecture Decision Records](adr/README.md) |
| Write or review framework code | [Code Conventions](code-conventions.md) |
| Decide whether a package or type is supported API | [Public API Boundaries](public-api-boundaries.md), [Public API Inventory](public-api-inventory.md) |
| Review pre-1.0 nullability and binary compatibility policy | [Pre-1.0 API Policy](pre-1.0-api-policy.md) |
| Write or review module READMEs | [Module README Convention](module-readme-convention.md) |
| Run the private quality-platform pilot | [Quality Pipeline](quality-pipeline.md) |
| Review the public surface of every artifact | [Public API Inventory](public-api-inventory.md) |
| Upgrade across a pre-1.0 source, binary, dependency, or configuration change | [Pre-1.0 Migration Guide](guides/migrate-public-names-and-properties.md) |
| Contribute code or documentation | [Contributing](../CONTRIBUTING.md) |
| Review release history | [Changelog](../CHANGELOG.md) |
| Check provenance and publication constraints | [Provenance](../PROVENANCE.md) |

## Features

| Feature area | Main guide | Related docs |
|---|---|---|
| Framework BOM, version alignment, and dependency conflict diagnosis | [Dependency Management](dependency-management.md) | [Platform README](../spring-boot-service-framework-platform/README.md), [Compatibility](compatibility.md), [Releasing](releasing.md) |
| Framework diagnostics and Spring Boot Actuator integration | [Actuator Architecture Contract](actuator.md) | [Actuator compatibility](actuator/compatibility.md), [Actuator property reference](actuator/property-reference.md), [Actuator core README](../spring-boot-service-framework-actuator-core/README.md), [Actuator starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-actuator/README.md), [Actuator consumer example](../examples/actuator-consumer/README.md) |
| Reusable exception handling, safe notifications, validation, and Spring Security responses | [Error Handling Guide](error-handling.md) | [Exception selection](error-handling/exception-selection.md), [Security errors](error-handling/security.md), [JSON contract](error-handling/json-contract.md), [Property reference](error-handling/property-reference.md), [Extension points](error-handling-extension-points.md), [Migration](guides/migrate-shared-exception.md), [Example](../examples/error-handling-consumer/README.md) |
| REST clients, OAuth2, SSL, audit, metrics, resilience, and error handling | [REST Client Starter Guide](rest-client.md) | [REST client property reference](rest-client/property-reference.md), [Troubleshooting](troubleshooting.md), [REST Client Extension Points](rest-client-extension-points.md), [REST client consumer example](../examples/rest-client-consumer/README.md) |
| Public REST client replacement points, customizers, and OAuth2 SPIs | [REST Client Extension Points](rest-client-extension-points.md) | [Compatibility](compatibility.md), [REST Client Starter Guide](rest-client.md) |
| OpenAPI/Swagger code generation for models, server API, and REST clients | [OpenAPI Portal](openapi/index.md) | [OpenAPI Code Generation](openapi-codegen.md), [Current behavior inventory](openapi-behavior-inventory.md), [REST Client Starter Guide](rest-client.md) |
| OpenAPI Gradle plugin DSL, defaults, validation, and tasks | [OpenAPI Gradle Plugin Reference](openapi/plugin-reference.md) | [Getting Started](openapi/getting-started.md), [Artifact Generation](openapi/generation.md) |
| OpenAPI local and remote Maven publication | [OpenAPI Artifact Publishing](openapi/publishing.md) | [Gradle Plugin Reference](openapi/plugin-reference.md), [Contract Versioning](openapi/versioning.md) |
| OpenAPI generated artifact contents and dependency boundaries | [OpenAPI Artifact Generation](openapi/generation.md) | [Getting Started](openapi/getting-started.md), [Current behavior inventory](openapi-behavior-inventory.md) |
| OpenAPI contract identity, baselines, and SemVer policy | [OpenAPI Contract Versioning](openapi/versioning.md) | [Breaking change recipe](guides/check-openapi-breaking-changes.md), [OpenAPI Code Generation](openapi-codegen.md), [Compatibility](compatibility.md) |
| OpenAPI configuration, document, coordinate, and compatibility validation | [OpenAPI Validation](openapi/validation.md) | [Gradle Plugin Reference](openapi/plugin-reference.md), [Contract Versioning](openapi/versioning.md) |
| Executable OpenAPI contract tests for Spring MVC controllers | [OpenAPI Contract Testing](openapi-contract-testing.md) | [Contract testing recipe](guides/openapi-contract-testing.md), [OpenAPI Code Generation](openapi-codegen.md), [Compatibility](compatibility.md) |
| OpenAPI generator Gradle build logic | [OpenAPI Generator Build Logic](../build-logic/openapi-generator-plugin/README.md) | [OpenAPI Code Generation](openapi-codegen.md), [OpenAPI documentation architecture](openapi/documentation-architecture.md) |
| OpenAPI project scaffolding | [Project Scaffolding](openapi/scaffolding.md) | [Project generator README](../spring-boot-service-framework-project-generator/README.md), [OpenAPI Generator ADR](adr/0001-openapi-generator-engine.md) |
| OpenAPI repository examples and expected evidence | [OpenAPI Examples](openapi/examples.md) | [Getting Started](openapi/getting-started.md), [OpenAPI Validation](openapi/validation.md) |
| OpenAPI task failures and recovery | [OpenAPI Troubleshooting](openapi/troubleshooting.md) | [OpenAPI Portal](openapi/index.md), [General Troubleshooting](troubleshooting.md) |
| Java library and Spring Boot starter build conventions | [Gradle Convention Plugins](../build-logic/conventions/README.md) | [Code Conventions](code-conventions.md), [Contributing](../CONTRIBUTING.md) |
| Pre-1.0 API evolution, JSpecify, and japicmp | [Pre-1.0 API Policy](pre-1.0-api-policy.md) | [Public API Boundaries](public-api-boundaries.md), [Compatibility](compatibility.md), [Releasing](releasing.md) |
| Spring Boot AOT, GraalVM native-image, runtime hints, and declarative client proxies | [Spring Boot AOT And Native Images](native-image.md) | [REST Client Starter Guide](rest-client.md), [Compatibility](compatibility.md) |
| Copy-ready use-case recipes | [Use Case Guides](guides/index.md) | [REST Client Starter Guide](rest-client.md), [Troubleshooting](troubleshooting.md) |
| Mock responses for controllers and outbound `RestClient` calls | [Mock Core and Starter](mock.md) | [Mock property reference](mock/property-reference.md), [Mock core README](../spring-boot-service-framework-mock-core/README.md), [Mock starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-mock/README.md) |
| Structured JSON logging and transaction id propagation | [Logging Guide](logging.md) | [Logging compatibility](logging/compatibility.md), [Async appender contract](logging/async-appender.md), [Logging property reference](logging/property-reference.md), [Logging starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-logging/README.md), [Logging core README](../spring-boot-service-framework-logging-core/README.md), [Logging consumer example](../examples/logging-consumer/README.md) |
| Shared notifications and framework-neutral helpers | [Commons README](../spring-boot-service-framework-commons/README.md) | [HTTP client core README](../spring-boot-service-framework-http-client-core/README.md) |

## Use Case Guides

| Use case | Guide |
|---|---|
| OAuth2 `client_credentials` with `private_key_jwt` client authentication | [Client Credentials With Private Key JWT](guides/client-credentials-private-key-jwt.md) |
| JWT bearer grant with runtime claims | [JWT Bearer Dynamic Claims](guides/jwt-bearer-dynamic-claims.md) |
| Base64 JKS or PKCS12 keystore content and passwords | [Base64 Keystore Configuration](guides/base64-keystore.md) |
| Disable OAuth2 token cache per grant type | [Disable Token Cache](guides/disable-token-cache.md) |
| Add OAuth2 contributors, customizers, or cache key behavior | [Customize OAuth2](guides/customize-oauth2.md) |
| Replace framework defaults with application beans | [Replace Default Beans](guides/replace-default-beans.md) |
| Migrate copied shared exception handlers and response builders | [Migrate From shared/exception](guides/migrate-shared-exception.md) |
| Apply pre-1.0 source, binary, dependency, and configuration changes | [Pre-1.0 Migration Guide](guides/migrate-public-names-and-properties.md) |
| Generate OpenAPI contract artifacts | [Generate OpenAPI Contract Artifacts](guides/openapi-generated-artifacts.md) |
| Complete the first OpenAPI workflow | [OpenAPI Getting Started](openapi/getting-started.md) |
| Scaffold a hexagonal Spring Boot service from OpenAPI | [Project Scaffolding](openapi/scaffolding.md) |
| Run the repository OpenAPI fixtures | [OpenAPI Examples](openapi/examples.md) |
| Diagnose an OpenAPI failure | [OpenAPI Troubleshooting](openapi/troubleshooting.md) |

## Modules

| Module | Documentation |
|---|---|
| `spring-boot-service-framework-platform` | [Platform README](../spring-boot-service-framework-platform/README.md), [Dependency Management](dependency-management.md) |
| `spring-boot-service-framework-actuator-core` | [Actuator core README](../spring-boot-service-framework-actuator-core/README.md), [Actuator Architecture Contract](actuator.md), [Actuator compatibility](actuator/compatibility.md), [Actuator property reference](actuator/property-reference.md) |
| `spring-boot-service-framework-commons` | [Commons README](../spring-boot-service-framework-commons/README.md) |
| `spring-boot-service-framework-logging-core` | [Logging core README](../spring-boot-service-framework-logging-core/README.md), [Logging Guide](logging.md), [Logging compatibility](logging/compatibility.md) |
| `spring-boot-service-framework-http-client-core` | [HTTP client core README](../spring-boot-service-framework-http-client-core/README.md) |
| `spring-boot-service-framework-mock-core` | [Mock core README](../spring-boot-service-framework-mock-core/README.md) |
| `spring-boot-service-framework-error-core` | [Error core README](../spring-boot-service-framework-error-core/README.md), [Error Handling Guide](error-handling.md) |
| `spring-boot-service-framework-project-generator` | [Project generator README](../spring-boot-service-framework-project-generator/README.md), [Project Scaffolding](openapi/scaffolding.md), [OpenAPI Generator ADR](adr/0001-openapi-generator-engine.md) |
| `spring-boot-service-framework-starter-actuator` | [Actuator starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-actuator/README.md), [Actuator Architecture Contract](actuator.md), [Actuator compatibility](actuator/compatibility.md), [Actuator property reference](actuator/property-reference.md) |
| `spring-boot-service-framework-starter-logging` | [Logging starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-logging/README.md), [Logging Guide](logging.md), [Logging compatibility](logging/compatibility.md), [Async appender contract](logging/async-appender.md) |
| `spring-boot-service-framework-starter-rest-client` | [REST client starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-rest-client/README.md), [REST Client Starter Guide](rest-client.md) |
| `spring-boot-service-framework-starter-mock` | [Mock starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-mock/README.md), [Mock Core and Starter](mock.md) |
| `spring-boot-service-framework-starter-error-handling` | [Error handling starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-error-handling/README.md), [Error Handling Guide](error-handling.md), [Error handling consumer example](../examples/error-handling-consumer/README.md) |

## Examples

| Example | Purpose |
|---|---|
| [Actuator consumer](../examples/actuator-consumer/README.md) | Standalone Spring Boot app covering health, info, diagnostics, metrics, application probes, redaction, and endpoint authorization. |
| [Error handling consumer](../examples/error-handling-consumer/README.md) | Standalone Spring Boot app covering application catalogs, validation, downstream failures, unexpected exceptions, and security responses. |
| [Logging consumer](../examples/logging-consumer/README.md) | Standalone Spring Boot app that validates the logging starter from published local artifacts. |
| [REST client consumer](../examples/rest-client-consumer/README.md) | Standalone Spring Boot app that validates configured `RestClient` beans, OAuth2 flows, request context propagation, and downstream calls from published local artifacts. |
| [Quality pilot](../examples/quality-pilot/README.md) | Executable Spring Boot application used to validate the repository quality contract. |

## Maintainers

| Task | Read |
|---|---|
| Update public behavior or documentation | [Documentation Architecture](documentation-architecture.md) |
| Implement or review the Actuator starter architecture | [Actuator Architecture Contract](actuator.md) |
| Add, remove, or upgrade a platform-managed dependency | [Dependency Management](dependency-management.md), [Compatibility](compatibility.md) |
| Add or review framework code | [Code Conventions](code-conventions.md), [Public API Boundaries](public-api-boundaries.md), [Public API Inventory](public-api-inventory.md) |
| Update a module README | [Module README Convention](module-readme-convention.md) |
| Check compatibility expectations | [Compatibility](compatibility.md) |
| Review or change a public type | [Public API Inventory](public-api-inventory.md), [Pre-1.0 API Policy](pre-1.0-api-policy.md), [Compatibility](compatibility.md) |
| Add REST client extension points | [REST Client Extension Points](rest-client-extension-points.md) |
| Define or review OpenAPI generated coordinates | [OpenAPI Code Generation](openapi-codegen.md) |
| Review models, server API, and client artifact contents | [OpenAPI Artifact Generation](openapi/generation.md) |
| Navigate all OpenAPI capabilities by goal or role | [OpenAPI Portal](openapi/index.md) |
| Review the exact OpenAPI behavior currently protected by compatibility checks | [OpenAPI Current Behavior Inventory](openapi-behavior-inventory.md) |
| Add or publish generated OpenAPI artifacts | [Generate OpenAPI Contract Artifacts](guides/openapi-generated-artifacts.md), [OpenAPI Artifact Publishing](openapi/publishing.md) |
| Modify the OpenAPI generator implementation | [OpenAPI documentation architecture](openapi/documentation-architecture.md), [Current behavior inventory](openapi-behavior-inventory.md), [OpenAPI Generator Build Logic](../build-logic/openapi-generator-plugin/README.md) |
| Validate all OpenAPI documentation | Run `./gradlew openApiDocumentationCheck`, then inspect `build/reports/smbtech-openapi/documentation-check.txt` |
| Review OpenAPI documentation compatibility | Run `./gradlew openApiDocumentationCompatibilityCheck`, then review `gradle/compatibility/contracts/openApiDocumentation.txt` for intentional contract changes |
| Prepare contribution changes | [Contributing](../CONTRIBUTING.md) |
| Configure or troubleshoot pull request CI | [Quality Pipeline](quality-pipeline.md) |
| Prepare a release | [Releasing](releasing.md), [Changelog](../CHANGELOG.md) |
| Verify source ownership and publication constraints | [Provenance](../PROVENANCE.md) |

## Validation

Run documentation checks:

```bash
./gradlew documentationCheck
```

Run the full project checks:

```bash
./gradlew check
```
