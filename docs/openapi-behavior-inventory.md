# OpenAPI Current Behavior Inventory

This inventory freezes the OpenAPI behavior implemented by framework version
`0.5.0`. It is a compatibility reference for maintainers, not a getting-started
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
| `build-logic/openapi-templates` | Versioned customization applied after OpenAPI Generator creates Spring HTTP interfaces |
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
Normalization, overrides, baselines, and SemVer are owned by
[OpenAPI Contract Versioning](openapi/versioning.md) and the
[Gradle Plugin Reference](openapi/plugin-reference.md).

## Generated Artifacts

For an effective identity `<group>:<name>:<version>`, the plugin creates:

| Kind | Maven coordinate | Generated contract |
|---|---|---|
| Models | `<group>:<name>-models:<version>` | DTOs and enums with Jackson and Jakarta Validation annotations |
| Server API | `<group>:<name>-server-api:<version>` | Spring MVC controller, API interfaces, delegate contracts, and `ApiUtil` support |
| Client | `<group>:<name>-client:<version>` | Spring HTTP interfaces annotated with `@HttpApiClient("<name>")` |

The server API and client variants depend on the matching models artifact. Each
enabled kind produces binary and source JARs under
`build/libs/smbtech-openapi`.

OpenAPI Generator uses the `spring` generator. Models and server APIs use the
`spring-boot` library; clients use `spring-http-interface`. Current generation
enables `useSpringBoot4`, `useJakartaEe`, `useBeanValidation`,
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
| Framework | `0.5.0` | `frameworkVersion` |
| Spring Boot | `4.1.0` | `springBootVersion` |
| OpenAPI Generator | `7.24.0` | `openApiGeneratorVersion` |
| OpenAPI Diff | `2.1.7` | `openApiDiffVersion` |
| Jackson build parser | `3.1.5` | `jackson3Version` |
| Handlebars | `4.5.2` | `handlebarsVersion` |
| Java | `21` | Plugin and generated-source toolchains |

`framework.version`, `generator.version`, and `spring-boot.version` are embedded
in every generated artifact. A change to any behavior in this inventory must
update the implementation, compatibility contract, this document, and the
corresponding tests in the same change.
