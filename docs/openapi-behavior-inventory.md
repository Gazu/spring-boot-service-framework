# OpenAPI Current Behavior Inventory

This inventory freezes the OpenAPI behavior implemented by framework version
`0.5.2`. It is a compatibility reference for maintainers, not a getting-started
guide. The executable sources of truth are the OpenAPI Gradle plugin, its module
compatibility contract, and the generated-artifact compatibility lifecycle.
It records only values protected by automated checks; canonical references own
their explanation and usage.
Documentation ownership is defined by the
[OpenAPI Documentation Architecture](openapi/documentation-architecture.md).
The supported Gradle DSL and task names are documented in the
[OpenAPI Gradle Plugin Reference](openapi/plugin-reference.md).

## Ownership

| Boundary | Current responsibility |
|---|---|
| `build-logic/openapi-generator-plugin` | Public Gradle DSL, validation, generation, compilation, packaging, publication, and compatibility orchestration |
| `build-logic/openapi-templates` | Versioned customization applied to generated Spring HTTP Interface and OpenFeign contracts |
| `spring-boot-service-framework-openapi-contract-testing` | Test-scope request and response verification for Spring MVC applications |
| `spring-boot-service-framework-project-generator` | One-time Spring Boot and hexagonal project scaffolding from a contract or server API JAR |

The retired `spring-boot-service-framework-openapi-generator` runtime module and
legacy root task aliases are not part of the current behavior.

## Public Gradle API

| Contract | Value |
|---|---|
| Plugin ID | `com.smbtech.service-framework.openapi-generator` |
| Extension | `smbtechOpenApi` |
| Extension type | `com.smbtech.serviceframework.gradle.openapi.SmbtechOpenApiExtension` |
| Named specification type | `com.smbtech.serviceframework.gradle.openapi.SmbtechOpenApiSpec` |

## Contract Identity

The parser accepts OpenAPI `3.0.x` and `3.1.x`. Effective identity defaults to
group `com.smbtech.contracts`, normalized `info.title`, and `info.version`.
`info.version` is both the Maven version and the source of the Java package
major segment. Normalization, overrides, baselines, and SemVer are owned by
[OpenAPI Contract Versioning](openapi/versioning.md) and the
[Gradle Plugin Reference](openapi/plugin-reference.md).

## Generated Artifacts

For an effective identity `<group>:<name>:<version>`, the plugin creates:

| Kind | Maven coordinate | Generated contract |
|---|---|---|
| Model | `<group>:<name>-jdk21-model:<version>` | DTOs and enums with Jackson and Jakarta Validation annotations |
| API | `<group>:<name>-jdk21-api:<version>` | Spring MVC controllers, API interfaces, delegate contracts, and `ApiUtil` support |
| Client | `<group>:<name>-jdk21-client:<version>` | Declarative client interfaces sharing the model artifact |

The API and client variants expose the matching model artifact as a transitive
Maven dependency. Each enabled kind produces binary and source JARs under
`build/libs/smbtech-openapi`.

For unversioned root `<basePackage>` and version major `<major>`, effective
packages are:

```text
<basePackage>.v<major>.model
<basePackage>.v<major>.api
<basePackage>.v<major>.client.httpinterface
<basePackage>.v<major>.client.openfeign
```

Client generation produces matching Spring HTTP Interface and OpenFeign
interfaces in the two reserved packages and merges them into one client JAR.
The variants use the same simple interface names in their distinct packages.
The OpenFeign starter is never a transitive client dependency.

OpenAPI Generator uses the `spring` generator. Models and APIs use the
`spring-boot` library; client generation runs separate interface-only passes
with the `spring-http-interface` and `spring-cloud` libraries. The OpenFeign
pass suppresses the generator's wrapper and configuration types, then adds the
corporate `@FeignClient` annotation directly to each API interface. Current
generation enables
`useSpringBoot4`, `useJakartaEe`, `useBeanValidation`,
`performBeanValidation`, and `useJackson3`. It disables Swagger UI, generated
documentation, and generated tests.

Artifact contents, dependencies, packages, and output behavior are owned by
[OpenAPI Artifact Generation](openapi/generation.md).

## Embedded Contract Metadata

Every binary JAR includes the source contract and deterministic properties at
`META-INF/smbtech/openapi/contract.yaml` and
`META-INF/smbtech/openapi/contract.properties`. Collision-free copies live at
`META-INF/smbtech/openapi/contracts/<name>/<version>/contract.yaml` and
`META-INF/smbtech/openapi/contracts/<name>/<version>/contract.properties`.

`contract.properties` contains these stable keys in sorted order:

| Key | Meaning |
|---|---|
| `artifact.group` | Effective Maven group |
| `artifact.id` | Effective artifact ID including its kind suffix |
| `artifact.kind` | `models`, `server-api`, or `client` |
| `contract.id` | Normalized contract name |
| `contract.sha256` | SHA-256 of the source document |
| `contract.title` | Original `info.title` |
| `contract.version` | Effective artifact and contract version |
| `framework.version` | Framework version used for generation |
| `generator.name` | `openapi-generator` |
| `generator.version` | OpenAPI Generator version used for generation |
| `spring-boot.version` | Spring Boot compatibility version |

## Validation Contract

The implemented configuration, document, coordinate, and compatibility layers
are documented in [OpenAPI Validation](openapi/validation.md).

## Publication Contract

Each enabled artifact kind is an independent Maven publication. The supported
local and remote workflows, repository layout, credentials, and immutable
release rules are defined in
[OpenAPI Artifact Publishing](openapi/publishing.md).

## Compatibility Evidence

OpenAPI compatibility reports are written under
`build/reports/smbtech-openapi`. Current evidence includes:

```text
diff/
reproducibility.sha256
migration.md
consumer-test.txt
mock-contracts.properties
```

The exact implemented baseline selection, SemVer enforcement, strict mode, and
known comparison limits are documented in
[OpenAPI Contract Versioning](openapi/versioning.md).

## Pinned Toolchain

| Component | Current value | Version source |
|---|---|---|
| Framework | `0.5.2` | `frameworkVersion` |
| Spring Boot | `4.1.0` | `springBootVersion` |
| OpenAPI Generator | `7.24.0` | `openApiGeneratorVersion` |
| OpenAPI Diff | `2.1.7` | `openApiDiffVersion` |
| Jackson build parser | `3.1.5` | `jackson3Version` |
| Handlebars | `4.5.2` | `handlebarsVersion` |
| Java | `21` | Plugin and generated-source toolchains and artifact IDs |
| Spring Cloud for OpenFeign consumers | `2025.1.2` minimum | Spring Boot `4.1.x` compatibility boundary |
| Spring Cloud OpenFeign compile input | `5.0.2` | `springCloudOpenFeignVersion` |

`framework.version`, `generator.version`, and `spring-boot.version` are embedded
in every generated artifact. A change to any behavior in this inventory must
update the implementation, compatibility contract, this document, and the
corresponding tests in the same change.

The external artifact contract and phased client delivery are frozen by
[ADR 0002](adr/0002-openapi-artifact-contract.md).
