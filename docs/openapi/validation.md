# OpenAPI Validation

This is the canonical reference for build-time validation performed by the
OpenAPI Gradle plugin. It covers plugin configuration, OpenAPI documents,
effective coordinates, and generated-artifact compatibility. Runtime request
and response verification is a separate concern documented in
[OpenAPI Contract Testing](../openapi-contract-testing.md).

## Validation Model

The plugin exposes three validation layers:

| Layer | Public task | Scope |
|---|---|---|
| Gradle configuration | `smbtechOpenApiBuildLogicCheck` | DSL values, artifact switches, packages, directories, and repository URL. |
| Contract and coordinates | `smbtechOpenApiValidateSpecs` | OpenAPI structure, operation IDs, names, versions, and generated Maven coordinate collisions. |
| Generated compatibility | `smbtechOpenApiCompatibilityCheck` | Baseline diff, reproducibility, migration evidence, consumer boundary, and mock compatibility. |

Run all three explicitly in CI:

```bash
./gradlew \
  smbtechOpenApiBuildLogicCheck \
  smbtechOpenApiValidateSpecs \
  smbtechOpenApiCompatibilityCheck
```

The tasks are separate contracts. `smbtechOpenApiCompatibilityCheck` does not
directly aggregate the configuration and spec validation tasks.

## Configuration Validation

`smbtechOpenApiBuildLogicCheck` validates the effective `smbtechOpenApi` DSL
without generating source code.

Global validation rejects:

- blank `groupId`;
- blank `outputDirectory`;
- blank `repositoryDirectory`;
- blank `baselineDirectory`;
- a malformed or relative `publicationRepositoryUrl`;
- disabling models, server API, and client at the same time; and
- disabling models while server API or client remains enabled.

For every named specification, it rejects:

- a blank registration name;
- a missing `input` file configuration;
- blank group, artifact name, or version overrides;
- invalid `basePackage`, `modelPackage`, `serverApiPackage`, or `clientPackage`;
- disabling every artifact for that specification; and
- enabling server API or client while models are disabled.

Package overrides must contain dot-separated Java identifiers. Configuration
validation confirms shape and internal dependency rules; it does not parse the
OpenAPI document.

## Spec Discovery

`smbtechOpenApiValidateSpecs` validates every configured `input` plus YAML,
YML, and JSON files directly below these conventional directories anywhere in
the project:

```text
src/main/openapi/
openapi/
swagger/
```

This includes repository paths such as `docs/openapi/`. Files below `.git`,
`.gradle`, and `build` are excluded. Nested directories below a conventional
OpenAPI directory are not discovered automatically; register those files
explicitly:

```groovy
smbtechOpenApi {
    specs {
        register('warehouseInventoryCatalog') {
            input.set(file('contracts/inventory/v1/warehouse.yaml'))
        }
    }
}
```

Diagnostics use project-relative paths and process files in stable path order.

## Document Validation

Each discovered or configured document is parsed with Swagger Parser. Validation
accepts OpenAPI `3.0.x` and `3.1.x` and rejects:

- an unreadable or unparsable document;
- parser diagnostics returned for the document;
- Swagger 2 or another unsupported OpenAPI version;
- missing `info`;
- missing or blank `info.title`;
- missing, blank, or invalid `info.version`;
- missing paths or a document without operations;
- an operation without `operationId`;
- duplicate `operationId` values; and
- a title that cannot normalize to a valid artifact base name.

The accepted contract version shape and title normalization rules are defined
in [OpenAPI Contract Versioning](versioning.md).

## Coordinate Validation

After parsing a contract, `smbtechOpenApiValidateSpecs` resolves the effective
group, artifact base name, version, and enabled artifact kinds. It validates the
coordinates that generation would create:

`smbtechOpenApi.specs.<name>.artifactBaseName` overrides the normalized title
for generated coordinates when configured.

```text
<group>:<artifact-base-name>-models:<version>
<group>:<artifact-base-name>-server-api:<version>
<group>:<artifact-base-name>-client:<version>
```

The task rejects:

- an artifact base name outside `[a-z0-9]+(-[a-z0-9]+)*`;
- a version outside the supported SemVer/Maven-compatible shape;
- duplicate effective coordinates across contracts; and
- generated artifact IDs that collide with protected framework artifacts.

Protected artifact IDs currently include the framework Commons, Logging, HTTP
Client, Mock, Error, OpenAPI plugin, contract-testing, and Spring Boot starter
artifacts. Only enabled artifact kinds participate in coordinate collision
validation.

Coordinate validation does not query a Maven registry. Registry availability,
authorization, and immutable-version enforcement belong to
[OpenAPI Artifact Publishing](publishing.md).

## Compatibility Validation

`smbtechOpenApiCompatibilityCheck` aggregates five checks:

| Task | Evidence |
|---|---|
| `smbtechOpenApiBreakingChangeCheck` | OpenAPI Diff Markdown plus baseline and SemVer enforcement. |
| `smbtechOpenApiReproducibilityCheck` | Reproducible binary/source JAR verification and SHA-256 manifest. |
| `smbtechOpenApiMigrationReport` | Legacy coordinate and task mapping. |
| `smbtechOpenApiConsumerTest` | Generated metadata, artifact separation, delegate, HTTP interface, and consumer checks. |
| `smbtechOpenApiMockContractCheck` | Mock response status and collision-free contract resource validation. |

Baseline and structural-diff rules are defined in
[OpenAPI Contract Versioning](versioning.md). Generated JAR contents and
dependency boundaries are defined in
[OpenAPI Artifact Generation](generation.md).

## Gradle Lifecycle Wiring

When the consuming project applies Gradle's `base` plugin, `check` depends on:

```text
smbtechOpenApiBuildLogicCheck
smbtechOpenApiValidateSpecs
smbtechOpenApiCompatibilityCheck
```

Therefore this command runs the complete OpenAPI validation model together with
the project's other verification tasks:

```bash
./gradlew check
```

Generation tasks depend on `smbtechOpenApiValidateSpecs`, so malformed specs do
not proceed to source generation. Do not rely on that transitive relationship
as the complete CI gate because it does not replace configuration or
compatibility validation.

## Outputs And Reports

Configuration and spec validation report failures directly through Gradle and
do not create a separate report file. Compatibility evidence is written under:

```text
build/reports/smbtech-openapi/
  diff/
  reproducibility.sha256
  migration.md
  consumer-test.txt
  mock-contracts.properties
```

Generated source, classes, and JAR outputs remain under the locations documented
in [OpenAPI Artifact Generation](generation.md).

## Failure Diagnostics

Spec validation collects failures from all discovered documents before failing:

```text
OpenAPI spec validation issues found:
src/main/openapi/orders.yaml: operationId 'getOrder' must be unique
src/main/openapi/legacy.yaml: openapi must declare a supported 3.0.x or 3.1.x version
```

Configuration validation stops at the first invalid DSL rule and names its full
property path:

```text
smbtechOpenApi.specs.orders.modelPackage must be a valid Java package
```

For compatibility failures, rerun the named child task with `--stacktrace` and
inspect its report under `build/reports/smbtech-openapi`.

## Validation Boundaries

Build-time validation does not prove:

- that an application implements the generated server API correctly;
- that runtime requests and responses satisfy the contract;
- that business rules or examples are semantically correct;
- that a remote Maven repository accepts publication;
- that a configured base URL or authentication mechanism works;
- that every external `$ref` remains a supported compatibility boundary; or
- that a project contains at least one OpenAPI contract.

The plugin currently permits an empty specification set. It also requires a
nonblank Maven group but does not enforce a complete Maven group naming grammar.

Use [OpenAPI Contract Testing](../openapi-contract-testing.md) for Spring MVC
runtime verification and [Mock Core And Starter](../mock.md) for explicitly
enabled mock routes.

## CI Gate

For a project that applies `base`, prefer:

```bash
./gradlew check
```

For a focused OpenAPI-only job, use the three explicit public tasks:

```bash
./gradlew \
  smbtechOpenApiBuildLogicCheck \
  smbtechOpenApiValidateSpecs \
  smbtechOpenApiCompatibilityCheck
```

Remote publication must run only after this gate succeeds. Publication does not
implicitly execute the complete validation model.

## Validation

Protect this reference and the implementation contract with:

```bash
./gradlew openApiDocumentationCheck
./gradlew validateOpenApiValidationDocumentation
./gradlew smbtechOpenApiBuildLogicCheck
./gradlew smbtechOpenApiValidateSpecs
./gradlew smbtechOpenApiCompatibilityCheck
./gradlew documentationCheck
```

`openApiDocumentationCheck` validates every canonical OpenAPI document and all
published Gradle commands. Its summary is written to
`build/reports/smbtech-openapi/documentation-check.txt` and is uploaded by pull
request CI as quality evidence.

Return to the [OpenAPI Portal](index.md) for generation, publication,
versioning, testing, mocks, and scaffolding.
