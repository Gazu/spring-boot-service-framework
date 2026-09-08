# OpenAPI Gradle Plugin Reference

This is the canonical reference for the public Gradle plugin, its DSL,
conventions, validation rules, lifecycle tasks, and build wiring. Start with
[OpenAPI Getting Started](getting-started.md) when adopting the plugin for the
first time.

## Plugin Contract

| Contract | Value |
|---|---|
| Plugin artifact | `com.smbtech:spring-boot-service-framework-openapi-gradle-plugin:0.5.2` |
| Plugin ID | `com.smbtech.service-framework.openapi-generator` |
| Extension | `smbtechOpenApi` |
| Extension type | `com.smbtech.serviceframework.gradle.openapi.SmbtechOpenApiExtension` |
| Named specification type | `com.smbtech.serviceframework.gradle.openapi.SmbtechOpenApiSpec` |

The extension and named specification types are the supported Java API. Plugin
implementation classes, concrete task types, and generated per-contract task
names are internal.

## Apply The Plugin

```groovy
plugins {
    id 'com.smbtech.service-framework.openapi-generator' version '0.5.2'
}
```

When the plugin is resolved from a private plugin repository, configure that
repository in `pluginManagement.repositories` in `settings.gradle`. Do not add
repository credentials to the build script or source control.

The plugin applies `maven-publish` and creates the `smbtechOpenApi` extension.
Contracts in the conventional directories defined by
[OpenAPI Validation](validation.md) are registered automatically. Use the DSL
only for a non-conventional path or when a contract needs explicit overrides.

## DSL

```groovy
smbtechOpenApi {
    groupId.set('com.smbtech.contracts')
    outputDirectory.set(layout.buildDirectory.dir('generated/smbtech-openapi'))
    repositoryDirectory.set(layout.buildDirectory.dir('repository/openapi'))
    baselineDirectory.set(layout.projectDirectory.dir('src/main/openapi-baselines'))
    requireBaseline.set(true)
    failOnBreakingChanges.set(false)
    publishModels.set(true)
    publishServerApi.set(true)
    publishClient.set(true)

    specs {
        register('warehouseInventoryCatalog') {
            input.set(file('src/main/openapi/warehouse-inventory-catalog.yaml'))
        }
    }
}
```

Remote repository configuration and credential providers are defined in
[OpenAPI Artifact Publishing](publishing.md).

## Global Properties

| Property | Gradle type | Default | Purpose |
|---|---|---|---|
| `smbtechOpenApi.groupId` | `Property<String>` | `com.smbtech.contracts` | Default Maven group for every contract. |
| `smbtechOpenApi.outputDirectory` | `DirectoryProperty` | `build/generated/smbtech-openapi` | Root for generated sources and intermediate files. |
| `smbtechOpenApi.repositoryDirectory` | `DirectoryProperty` | `build/repository/openapi` | Local Maven repository for generated artifacts. |
| `smbtechOpenApi.baselineDirectory` | `DirectoryProperty` | `src/main/openapi-baselines` | Immutable contract snapshots used for compatibility checks. |
| `smbtechOpenApi.publicationRepositoryUrl` | `Property<String>` | Not configured | Absolute remote Maven repository URI. |
| `smbtechOpenApi.requireBaseline` | `Property<Boolean>` | `false` | Require an exact baseline for every current contract. |
| `smbtechOpenApi.failOnBreakingChanges` | `Property<Boolean>` | `false` | Reject every breaking diff, including one with a major version increment. |
| `smbtechOpenApi.publishModels` | `Property<Boolean>` | `true` | Generate and publish models by default. |
| `smbtechOpenApi.publishServerApi` | `Property<Boolean>` | `true` | Generate and publish server APIs by default. |
| `smbtechOpenApi.publishClient` | `Property<Boolean>` | `true` | Generate and publish both HTTP Interface and OpenFeign clients by default. |

## Specification Properties

Each entry in `smbtechOpenApi.specs` describes one contract. Its registration
name must be unique in the project.

| Property | Gradle type | Default | Purpose |
|---|---|---|---|
| `smbtechOpenApi.specs.<name>.input` | `RegularFileProperty` | Required for explicit entries | OpenAPI YAML or JSON document. |
| `smbtechOpenApi.specs.<name>.groupId` | `Property<String>` | Global `groupId` | Maven group override. |
| `smbtechOpenApi.specs.<name>.artifactBaseName` | `Property<String>` | Normalized `info.title` | Base name before `-jdk21-model`, `-jdk21-api`, or `-jdk21-client`. |
| `smbtechOpenApi.specs.<name>.version` | `Property<String>` | `info.version` | Compatibility input; when configured, it must equal `info.version`. |
| `smbtechOpenApi.specs.<name>.basePackage` | `Property<String>` | `com.smbtech.contracts.<normalized-title-without-hyphens>` | Unversioned Java package root. |
| `smbtechOpenApi.specs.<name>.modelPackage` | `Property<String>` | `<basePackage>.v<major>.model` | Compatibility override; must equal the derived model package. |
| `smbtechOpenApi.specs.<name>.serverApiPackage` | `Property<String>` | `<basePackage>.v<major>.api` | Compatibility override; must equal the derived API package. |
| `smbtechOpenApi.specs.<name>.clientPackage` | `Property<String>` | `<basePackage>.v<major>.client` | Compatibility override for the client namespace root; generators own its subpackages. |
| `smbtechOpenApi.specs.<name>.publishModels` | `Property<Boolean>` | Global `publishModels` | Models switch for this contract. |
| `smbtechOpenApi.specs.<name>.publishServerApi` | `Property<Boolean>` | Global `publishServerApi` | Server API switch for this contract. |
| `smbtechOpenApi.specs.<name>.publishClient` | `Property<Boolean>` | Global `publishClient` | Shared switch for both client interface variants for this contract. |

Configure `basePackage` when the default root is not suitable. The package
major and boundary suffixes remain framework-owned:

```text
<basePackage>.v<major>.model
<basePackage>.v<major>.api
<basePackage>.v<major>.client.httpinterface
<basePackage>.v<major>.client.openfeign
```

For new contracts, avoid kind-specific package overrides. Existing builds may
retain them only when they equal the package derived from `basePackage` and the
major component of `info.version`.

Identity, package root, and artifact switches can be combined:

```groovy
smbtechOpenApi {
    specs {
        register('warehouseInventoryCatalog') {
            input.set(file('src/main/openapi/warehouse-inventory-catalog.yaml'))
            groupId.set('com.example.contracts')
            artifactBaseName.set('inventory-catalog')
            basePackage.set('com.example.inventory')
            publishModels.set(true)
            publishServerApi.set(true)
            publishClient.set(false)
        }
    }
}
```

## Configuration Rules

`smbtechOpenApiBuildLogicCheck` validates the DSL before generation, while
`smbtechOpenApiValidateSpecs` validates documents and effective coordinates.
The complete rule set, discovery behavior, diagnostics, and CI lifecycle are
defined in [OpenAPI Validation](validation.md).

## Public Tasks

| Task | Result |
|---|---|
| `smbtechOpenApiBuildLogicCheck` | Validates the plugin DSL and pinned build inputs. |
| `smbtechOpenApiValidateSpecs` | Validates contracts and effective Maven coordinates. |
| `smbtechOpenApiGenerateModels` | Generates models for all enabled contracts. |
| `smbtechOpenApiGenerateServerApi` | Generates Spring MVC APIs for all enabled contracts. |
| `smbtechOpenApiGenerateClient` | Generates Spring HTTP Interface and OpenFeign interfaces for all enabled client artifacts. |
| `smbtechOpenApiAssemble` | Generates, compiles, adds metadata, and packages all enabled artifacts. |
| `smbtechOpenApiPublishToLocalRepository` | Publishes generated artifacts to `repositoryDirectory`. |
| `smbtechOpenApiPublish` | Publishes generated artifacts to `publicationRepositoryUrl`. |
| `smbtechOpenApiPublishContractToLocalRepository` | Publishes only the contract selected by `-PopenApiContract=<path>` to `repositoryDirectory`. |
| `smbtechOpenApiPublishContract` | Publishes only the contract selected by `-PopenApiContract=<path>` to `publicationRepositoryUrl`. |
| `smbtechOpenApiBreakingChangeCheck` | Runs OpenAPI Diff and enforces baseline and SemVer policy. |
| `smbtechOpenApiReproducibilityCheck` | Verifies archives and writes SHA-256 evidence. |
| `smbtechOpenApiMigrationReport` | Writes legacy coordinate and task mappings. |
| `smbtechOpenApiConsumerTest` | Verifies generated artifacts at their consumer boundary. |
| `smbtechOpenApiMockContractCheck` | Validates contracts used by the OpenAPI mock server. |
| `smbtechOpenApiCompatibilityCheck` | Aggregates breaking-change, reproducibility, migration, consumer, and mock checks. |

Reports are written under `build/reports/smbtech-openapi`.

## Build Lifecycle Wiring

When the consuming project applies Gradle's `base` plugin:

- `assemble` depends on `smbtechOpenApiAssemble`;
- `check` depends on `smbtechOpenApiBuildLogicCheck`;
- `check` depends on `smbtechOpenApiValidateSpecs`; and
- `check` depends on `smbtechOpenApiCompatibilityCheck`.

Per-contract generate, compile, metadata, JAR, sources JAR, and Maven
publication tasks are implementation details. Automation selects a canonical
contract input through `-PopenApiContract=<project-relative-path>` and invokes a
public contract publication task; it must not invoke generated task names.

## Publication Integration

The DSL exposes the local repository directory, remote repository URL, artifact
flags, and aggregate publication tasks. Repository layout, credentials,
security, CI sequencing, and immutable release rules are documented in
[OpenAPI Artifact Publishing](publishing.md).

## Multiple Contracts

Conventional folders need no per-contract DSL. For example, both contracts are
discovered automatically:

```text
ms-kyc-profile/swagger/openapi.yaml
ms-payments/openapi/payments.yaml
```

Register a contract explicitly when it needs overrides:

```groovy
smbtechOpenApi {
    specs {
        register('warehouseInventoryCatalog') {
            input.set(file('src/main/openapi/warehouse-inventory-catalog.yaml'))
        }
        register('retailLoyaltyRewards') {
            input.set(file('src/main/openapi/retail-loyalty-rewards.yaml'))
            publishClient.set(false)
        }
    }
}
```

Each contract keeps independent coordinates, packages, baselines, artifacts,
and publications while sharing global conventions.

## Compatibility Contract

The plugin ID, public extension types, DSL property paths, and aggregate task
names are protected by
`gradle/compatibility/contracts/openApiGradlePlugin.txt`. A supported contract
change must update implementation, compatibility evidence, this reference, and
the changelog together.

Run the plugin and documentation gates with:

```bash
./gradlew -p build-logic :spring-boot-service-framework-openapi-gradle-plugin:check
./gradlew openApiGradlePluginCompatibilityCheck
./gradlew documentationCheck
```

Generated artifact contents are documented in
[OpenAPI Artifact Generation](generation.md). Compatibility and baseline
semantics are documented in
[OpenAPI Contract Versioning](versioning.md). Return to
the [OpenAPI Portal](index.md) for the complete workflow map.
