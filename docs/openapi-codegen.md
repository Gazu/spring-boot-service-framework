# OpenAPI Code Generation

This document defines the contract for generating Java artifacts from
OpenAPI/Swagger specifications.

The generator is build-time oriented. It will read an OpenAPI document, derive
artifact coordinates from `info.title` and `info.version`, generate Java source
sets, and publish separate artifacts for models, server API, and REST client
interfaces.

## Quick Navigation

| Need | Section |
|---|---|
| Add a new spec and generate artifacts | [Generate OpenAPI Contract Artifacts](guides/openapi-generated-artifacts.md) |
| Understand artifact names and versions | [Coordinate Convention](#1-coordinate-convention) |
| Control spec version changes | [Spec Version Control](#5-spec-version-control) |
| Understand generated JAR responsibilities | [Generated Artifact Responsibilities](#8-generated-artifact-responsibilities) |
| Publish generated artifacts | [Artifact Publication](#12-artifact-publication) |
| Understand generator implementation boundaries | [Build Logic Boundary](#build-logic-boundary) |
| Plan generator migration work | [OpenAPI Generator Evolution](openapi-evolution.md) |
| Test a Spring MVC implementation against the contract | [OpenAPI Contract Testing](openapi-contract-testing.md) |
| Detect incompatible contract evolution | [OpenAPI Breaking Change Detection](openapi-breaking-changes.md) |
| Validate reproducibility, compilation, and compatibility | [Reproducible Generation](#15-reproducible-generation), [Compilation Tests](#16-compilation-tests), [Compatibility Check](#17-compatibility-check) |
| Review the complete fixture | [Complete Example Fixture](#18-complete-example-fixture) |

## 1. Coordinate Convention

The OpenAPI document is the default source of truth for generated artifact names
and versions.

Given:

```yaml
openapi: 3.0.3
info:
  title: merchant-order-status
  version: '1.1.0'
```

The recommended generated coordinates are:

```text
com.smbtech.openapi:merchant-order-status-models:1.1.0
com.smbtech.openapi:merchant-order-status-api:1.1.0
com.smbtech.openapi:merchant-order-status-client:1.1.0
```

Rules:

| Coordinate part | Default |
|---|---|
| `groupId` | `com.smbtech.openapi` |
| artifact base name | normalized `info.title` |
| artifact version | `info.version` |
| models artifact | `<artifactBaseName>-models` |
| server API artifact | `<artifactBaseName>-api` |
| client artifact | `<artifactBaseName>-client` |

This convention keeps generated contract artifacts separate from framework
artifacts. Framework modules continue using the `com.smbtech` group and
`spring-boot-service-framework-*` artifact names.

## 2. Why This Naming

Prefer:

```text
com.smbtech.openapi:merchant-order-status-models:1.1.0
```

over:

```text
com.smbtech:spring-boot-service-framework-openapi-models-merchant-order-status:1.1.0
```

Reasoning:

- generated contract artifacts are grouped under `com.smbtech.openapi`;
- artifact names stay short and readable;
- all artifacts for one contract sort together by prefix;
- framework artifact names do not grow with every contract name;
- consumers can depend on `models`, `api`, or `client` explicitly.

## 3. Title Normalization

`info.title` is normalized into `artifactBaseName`.

Normalization rules:

1. trim leading and trailing whitespace;
2. lowercase using `Locale.ROOT`;
3. replace spaces, underscores, and dots with `-`;
4. remove characters outside `[a-z0-9-]`;
5. collapse repeated `-`;
6. reject normalized names that do not match `[a-z0-9]+(-[a-z0-9]+)*`.

Examples:

| `info.title` | `artifactBaseName` |
|---|---|
| `merchant-order-status` | `merchant-order-status` |
| `Merchant Order Status` | `merchant-order-status` |
| `merchant_order_status` | `merchant-order-status` |
| `Merchant.Order.Status` | `merchant-order-status` |
| `Merchant   Order__Status` | `merchant-order-status` |
| `Payments 2 API` | `payments-2-api` |

Rejected examples:

| `info.title` | Reason |
|---|---|
| empty value | normalized name is empty |
| `---` | contains no alphanumeric segment |
| `_merchant-order-status_` | normalized name starts or ends with `-` |
| `.merchant-order-status.` | normalized name starts or ends with `-` |
| `@@@` | normalized name is empty |

The normalized artifact base name is also the default REST client name for the
generated client interface.

## 4. Version Rules

`info.version` becomes the Maven/Gradle artifact version.

Valid examples:

```yaml
info:
  version: '1.1.0'
```

```yaml
info:
  version: '1.1.0-SNAPSHOT'
```

Invalid examples:

```yaml
info:
  version: latest
```

```yaml
info:
  version: ''
```

The initial policy is Maven-compatible version strings with SemVer preferred.
Release artifacts should use stable SemVer values such as `1.1.0`.

## 5. Spec Version Control

Every committed OpenAPI spec version is tracked in:

```text
docs/openapi/spec-versions.properties
```

The catalog stores the normalized artifact base name, `info.version`, source
path, original title, and source SHA-256. The build uses this file to detect
accidental changes to a published contract version.

Validate the catalog:

```bash
./gradlew validateOpenApiSpecVersionCatalog
```

Update the catalog after an intentional spec version change:

```bash
./gradlew generateOpenApiSpecVersionCatalog
```

Policy:

- changing the OpenAPI file content while keeping the same `info.version` fails
  validation;
- intentional contract changes must bump `info.version`;
- after the bump, run `generateOpenApiSpecVersionCatalog` and commit the catalog
  update with the spec change;
- generated artifact publication is blocked when the current spec content does
  not match the catalog.

Structural compatibility with earlier versions is validated separately by
`openApiBreakingChangeCheck`. Baseline layout, change classification, SemVer
rules, and strict CI mode are defined in
[OpenAPI Breaking Change Detection](openapi-breaking-changes.md).

Example catalog entry:

```properties
merchant-order-status/1.1.0/title=merchant-order-status
merchant-order-status/1.1.0/source=docs/openapi/merchant-order-status.yaml
merchant-order-status/1.1.0/sha256=<spec-sha256>
```

## 6. Gradle Configuration API

The OpenAPI Gradle plugin exposes the `smbtechOpenApi` extension. The current
root build already applies the plugin and declares the committed example specs:

```groovy
smbtechOpenApi {
    groupId.set('com.smbtech.openapi')
    outputDirectory.set(layout.buildDirectory.dir('generated/smbtech-openapi'))
    repositoryDirectory.set(layout.buildDirectory.dir('repository/openapi'))

    specs {
        register('merchantOrderStatus') {
            input.set(file('docs/openapi/merchant-order-status.yaml'))
        }
        register('retailLoyaltyRewards') {
            input.set(file('docs/openapi/retail-loyalty-rewards.yaml'))
        }
    }
}
```

Per-spec overrides are available for repository or package conventions:

```groovy
smbtechOpenApi {
    specs {
        register('merchantOrderStatus') {
            input.set(file('docs/openapi/merchant-order-status.yaml'))
            groupId.set('com.smbtech.contracts')
            artifactBaseName.set('merchant-order-status')
            version.set('1.1.0')
            basePackage.set('com.smbtech.contracts.merchantorderstatus')
        }
    }
}
```

Configuration fields:

| Field | Scope | Default |
|---|---|---|
| `groupId` | global and per spec | `com.smbtech.openapi` |
| `outputDirectory` | global | `build/generated/smbtech-openapi` |
| `repositoryDirectory` | global | `build/repository/openapi` |
| `input` | per spec | required when a spec is declared |
| `artifactBaseName` | per spec | normalized `info.title` |
| `version` | per spec | `info.version` |
| `basePackage` | per spec | `com.smbtech.openapi.<normalizedTitleWithoutDash>` |

With the OpenAPI example above, this generates:

```text
com.smbtech.openapi:merchant-order-status-models:1.1.0
com.smbtech.openapi:merchant-order-status-api:1.1.0
com.smbtech.openapi:merchant-order-status-client:1.1.0
```

### Build Logic Boundary

OpenAPI generation uses two explicit boundaries:

| Boundary | Location | Owns |
|---|---|---|
| Gradle build logic | `build-logic/openapi-generator-plugin` | plugin id, DSL shape, generation tasks, task wiring, Gradle validation, publication wiring |
| Runtime-neutral generator code | `spring-boot-service-framework-openapi-generator` | spec readers, normalizers, source generators, packagers, publication descriptors |

Current ownership:

| Concern | Owner |
|---|---|
| Public Gradle task names and `smbtechOpenApi` DSL | `build-logic/openapi-generator-plugin` |
| Gradle source generation, compilation, and verification workflow | `build-logic/openapi-generator-plugin` |
| Maven publication wiring | `build-logic/openapi-generator-plugin` |
| Reusable spec parsing, naming, metadata, packaging, and compatibility services | `spring-boot-service-framework-openapi-generator` |

Migration rules:

- keep public task names stable until a documented replacement exists;
- add reusable Java behavior to `spring-boot-service-framework-openapi-generator`;
- add Gradle-facing behavior to `build-logic/openapi-generator-plugin`;
- update this document and the generated artifact guide whenever a public task,
  artifact coordinate, generated package, or publication layout changes.

The long-term migration roadmap is maintained in
[OpenAPI Generator Evolution](openapi-evolution.md).

The API validation task is:

```bash
./gradlew smbtechOpenApiBuildLogicCheck
```

The generator module now owns this initial Java structure:

```text
spring-boot-service-framework-openapi-generator/
  src/main/java/com/smbtech/serviceframework/openapi/generator/
    OpenApiArtifactKind.java
    OpenApiSpecInfo.java
    OpenApiSpecReader.java
    OpenApiNameNormalizer.java
    OpenApiContractMetadata.java
    OpenApiMetadataGenerator.java
    OpenApiModelGenerator.java
    OpenApiServerApiGenerator.java
    OpenApiClientGenerator.java
    OpenApiJarPackager.java
    OpenApiPublicationDescriptor.java
```

Run the generator module tests with:

```bash
./gradlew :spring-boot-service-framework-openapi-generator:check
```

These tests cover the reusable, runtime-neutral generator behavior independently
from Gradle task wiring.

The root compatibility guard for the reusable generator module is:

```bash
./gradlew validateOpenApiGeneratorModuleCompatibility
```

It runs the generator module `check` task and validates that the public reusable
generator types required by the migration remain present.

Maintainer workflow for OpenAPI generator changes:

```bash
./gradlew :spring-boot-service-framework-openapi-generator:check
./gradlew openApiBuildLogicCheck
./gradlew openApiCompatibilityCheck
./gradlew compatibilityCheck
```

## 7. Coordinate Overrides

Defaults may be overridden when repository or organizational naming requires it:

```groovy
smbtechOpenApi {
    specs {
        merchantOrderStatus {
            input = file('src/main/openapi/merchant-order-status.yaml')
            groupId = 'com.smbtech.contracts'
            artifactBaseName = 'order-status'
            version = '1.1.0'
        }
    }
}
```

Override rules:

- overrides must be explicit in Gradle configuration;
- generated logs must print the resolved coordinates;
- generated metadata must include both original OpenAPI values and resolved
  coordinates;
- overrides must not silently change `info.title` or `info.version`.

## 8. Generated Artifact Responsibilities

| Artifact suffix | Responsibility | Depends on |
|---|---|---|
| `models` | DTOs, enums, Jackson annotations, validation annotations, OpenAPI schema annotations when enabled. | no framework module by default |
| `api` | Server-side Spring interfaces/controllers or delegate contracts for implementing inbound APIs. | `<artifactBaseName>-models` |
| `client` | Spring HTTP interface client annotated with `@HttpApiClient` and Spring exchange annotations. | `<artifactBaseName>-models`, REST client starter API |

The `api` artifact must not depend on the `client` artifact. The `client`
artifact must not expose server controller implementation classes.

Validate artifact separation with:

```bash
./gradlew validateOpenApiArtifactSeparation
```

This check enforces that:

- `models` JARs contain only generated model classes and contract metadata;
- `api` JARs contain only generated server API classes and contract metadata;
- `client` JARs contain only generated REST client classes and contract
  metadata;
- generated classes are not duplicated across artifacts for the same contract.

## 9. Models JAR Generation

The current build can generate and package the models artifact for each
validated OpenAPI contract.

Run:

```bash
./gradlew openApiModelsJar
```

Validate:

```bash
./gradlew validateOpenApiModelsJar
```

For:

```yaml
openapi: 3.0.3
info:
  title: merchant-order-status
  version: '1.1.0'
```

the generated JAR is:

```text
build/libs/openapi/models/merchant-order-status-models-1.1.0.jar
```

The generated package defaults to:

```text
com.smbtech.openapi.merchantorderstatus.model
```

Current model generation scope:

- reads `components.schemas`;
- generates one Java class per object schema;
- generates one Java enum per top-level string enum schema;
- supports `required`;
- supports scalar properties: `string`, `integer`, `number`, `boolean`;
- supports `string` formats `date` and `date-time`;
- supports `$ref` properties that target generated component schemas;
- supports typed arrays through `items`;
- supports typed maps through `additionalProperties`;
- supports inline string enums as nested Java enums;
- adds Jackson annotations with `@JsonInclude` and `@JsonProperty`;
- adds Jackson `@JsonCreator` and `@JsonValue` for generated enums;
- adds Jakarta validation annotations for `required`, string length, pattern,
  array size, integer range, and decimal range constraints;
- includes `META-INF/smbtech/openapi/contract.properties` in the JAR.

Composition with `oneOf`, `anyOf`, and `allOf`, external `$ref` files, schema
annotations, polymorphic models, and deeper media-type handling belong to later
phases.

## 10. Server API JAR Generation

The current build can generate and package the server API artifact for each
validated OpenAPI contract.

Run:

```bash
./gradlew openApiServerApiJar
```

Validate:

```bash
./gradlew validateOpenApiServerApiJar
```

For:

```yaml
openapi: 3.0.3
info:
  title: merchant-order-status
  version: '1.1.0'
```

the generated JAR is:

```text
build/libs/openapi/api/merchant-order-status-api-1.1.0.jar
```

Generated package:

```text
com.smbtech.openapi.merchantorderstatus.api
```

Generated types:

```text
MerchantOrderStatusApiDelegate
MerchantOrderStatusApiController
```

Consumer implementation pattern:

```java
@Component
class MerchantOrderStatusHandler implements MerchantOrderStatusApiDelegate {
    @Override
    public OrderStatusResponse getOrderStatus(String orderId) {
        return new OrderStatusResponse(orderId, "PROCESSING", null);
    }
}
```

Current server API generation scope:

- reads `paths`;
- generates one delegate method per operation;
- generates a Spring `@RestController`;
- supports path parameters;
- supports successful JSON responses referencing `components.schemas`;
- returns `ResponseEntity<T>` from generated controller methods;
- compiles against the generated models JAR;
- includes `META-INF/smbtech/openapi/contract.properties` in the API JAR;
- does not embed model classes in the API JAR.

Request bodies, multiple status codes, headers, query parameters, validation
annotations on parameters, and advanced OpenAPI constructs belong to later
phases.

## 11. Client JAR Generation

The current build can generate and package the REST client artifact for each
validated OpenAPI contract.

Run:

```bash
./gradlew openApiClientJar
```

Validate:

```bash
./gradlew validateOpenApiClientJar
```

For:

```yaml
openapi: 3.0.3
info:
  title: merchant-order-status
  version: '1.1.0'
```

the generated JAR is:

```text
build/libs/openapi/client/merchant-order-status-client-1.1.0.jar
```

Generated package:

```text
com.smbtech.openapi.merchantorderstatus.client
```

Generated type:

```text
MerchantOrderStatusClient
```

Generated interface shape:

```java
@HttpApiClient("merchant-order-status")
@HttpExchange
public interface MerchantOrderStatusClient {
    @GetExchange("/orders/{orderId}/status")
    OrderStatusResponse getOrderStatus(@PathVariable("orderId") String orderId);
}
```

Current client generation scope:

- reads `paths`;
- generates one Spring HTTP interface method per operation;
- supports `GET`, `POST`, `PUT`, `PATCH`, and `DELETE`;
- uses the normalized OpenAPI title as the `@HttpApiClient` value;
- supports path parameters;
- supports successful JSON responses referencing `components.schemas`;
- compiles against the generated models JAR and REST client starter API;
- includes `META-INF/smbtech/openapi/contract.properties` in the client JAR;
- does not embed model classes or server API classes in the client JAR.

Request bodies, query parameters, headers, multipart payloads, multiple media
types, and advanced OpenAPI constructs belong to later phases.

## 12. Artifact Publication

Generated OpenAPI artifacts are Maven-published from the root project using the
same coordinates derived from `info.title` and `info.version`.

Publish generated OpenAPI artifacts to the root local build repository:

```bash
./gradlew publishOpenApiArtifactsToLocalBuildRepository
```

Validate the local publication:

```bash
./gradlew validateOpenApiLocalPublication
```

For:

```yaml
openapi: 3.0.3
info:
  title: merchant-order-status
  version: '1.1.0'
```

the local repository contains:

```text
build/repository/openapi/com/smbtech/openapi/merchant-order-status-models/1.1.0/
build/repository/openapi/com/smbtech/openapi/merchant-order-status-api/1.1.0/
build/repository/openapi/com/smbtech/openapi/merchant-order-status-client/1.1.0/
```

Consumers using the root local build repository can resolve:

```groovy
repositories {
    maven {
        url = uri('../spring-boot-service-framework/build/repository/openapi')
    }
    mavenCentral()
}

dependencies {
    implementation 'com.smbtech.openapi:merchant-order-status-client:1.1.0'
}
```

`publishLocalArtifacts` also publishes generated OpenAPI artifacts, so standalone
consumer checks can use one command for framework modules and generated
contracts.

When `PRIVATE_MAVEN_URL` or `-PprivateMavenUrl` is configured, the root
`publish` task can publish generated OpenAPI artifacts to the private registry.

Published POM dependency rules:

- `models` depends on Jackson annotations and Jakarta Validation API;
- `api` depends on `models` and Spring Web;
- `client` depends on `models` and the REST client starter.

## 13. Default Package Convention

If `basePackage` is not configured, derive it from the normalized title:

```text
com.smbtech.openapi.merchantorderstatus
```

Generated packages:

```text
com.smbtech.openapi.merchantorderstatus.model
com.smbtech.openapi.merchantorderstatus.api
com.smbtech.openapi.merchantorderstatus.client
```

Consumers may override `basePackage`:

```groovy
smbtechOpenApi {
    specs {
        merchantOrderStatus {
            basePackage = 'com.smbtech.contracts.merchantorderstatus'
        }
    }
}
```

## 14. Generated Metadata

Every generated artifact must include:

```text
META-INF/smbtech/openapi/contract.properties
```

Required metadata:

```properties
openapi.title=merchant-order-status
openapi.version=1.1.0
openapi.artifact.base-name=merchant-order-status
openapi.artifact.group-id=com.smbtech.openapi
openapi.artifact.kind=models
openapi.artifact.models=merchant-order-status-models
openapi.artifact.api=merchant-order-status-api
openapi.artifact.client=merchant-order-status-client
openapi.source=docs/openapi/merchant-order-status.yaml
openapi.sha256=<spec-sha256>
openapi.generator=spring-boot-service-framework-openapi
openapi.generator.version=<framework-version>
```

The metadata generator creates one metadata file per planned artifact kind:

```text
build/generated/smbtech-openapi/metadata/<artifactBaseName>/models/META-INF/smbtech/openapi/contract.properties
build/generated/smbtech-openapi/metadata/<artifactBaseName>/api/META-INF/smbtech/openapi/contract.properties
build/generated/smbtech-openapi/metadata/<artifactBaseName>/client/META-INF/smbtech/openapi/contract.properties
```

Run:

```bash
./gradlew generateOpenApiMetadata
```

Validate:

```bash
./gradlew validateOpenApiMetadata
```

Do not include timestamps in generated source or metadata by default. Stable
inputs should produce stable outputs.

## 15. Reproducible Generation

Generated OpenAPI sources, metadata, and JARs are expected to be reproducible
for the same input specs, framework version, Java version, and dependency
classpath.

Reproducibility rules:

- generated source and metadata must not include timestamps, build paths, random
  values, or environment-specific values;
- generated JAR entries are written in sorted order;
- generated JAR entry modification times are fixed to `2000-01-01T00:00:00Z`;
- repeated packaging of the same generated classes and metadata must produce the
  same SHA-256 hash;
- generated artifacts must match a reproducible rebuild performed by the
  validation task.

Validate reproducibility:

```bash
./gradlew validateOpenApiReproducibleGeneration
```

This validation is part of `documentationCheck`, so a non-reproducible OpenAPI
artifact blocks the root `check` flow.

## 16. Compilation Tests

Generated OpenAPI artifacts are also validated from a consumer compilation
point of view. The build generates small Java sources that import and use the
generated `models`, `api`, and `client` JARs together.

Run:

```bash
./gradlew validateOpenApiCompilationTests
```

The compile-test source for each spec exercises:

- model construction from the `models` artifact;
- delegate implementation and controller usage from the server `api` artifact;
- declarative client method usage from the `client` artifact;
- the dependency classpath required by Spring Web, Jakarta Validation, Jackson
  annotations, and the REST client starter API.

This catches missing generated types, broken imports, missing POM-equivalent
dependencies, and incompatible signatures before a consumer service attempts to
compile against the generated artifacts.

## 17. Compatibility Check

The complete generated OpenAPI compatibility contract is aggregated by:

```bash
./gradlew openApiCompatibilityCheck
```

This task validates public task names, spec naming, metadata, version catalog
entries, breaking change detection, advanced model generation, generated models/server API/client JARs,
artifact separation, reproducible packaging, consumer-style compilation, local
Maven publication layout, Gradle build-logic checks, and the reusable generator
module compatibility contract. It is also part of the root `compatibilityCheck`
flow.

Public OpenAPI task names are checked by:

```bash
./gradlew validateOpenApiTaskCompatibility
```

This guard is intentionally narrow: it fails when a public command is removed,
renamed, or left without a Gradle group/description.

Reusable generator module compatibility is checked by:

```bash
./gradlew validateOpenApiGeneratorModuleCompatibility
```

## 18. Complete Example Fixture

The repository includes a second complete OpenAPI fixture:

- [retail-loyalty-rewards.yaml](openapi/retail-loyalty-rewards.yaml)

It models a retail loyalty rewards API with:

- `GET /members/{memberId}/summary`;
- `GET /members/{memberId}/vouchers/{voucherId}`;
- `RewardsSummaryResponse`;
- `VoucherResponse`.

The spec uses:

- multiple operations;
- multiple path parameters;
- multiple schemas;
- required fields;
- scalar types: `string`, `integer`, `number`, `boolean`;
- date formats: `date` and `date-time`.

Generated coordinates:

```text
com.smbtech.openapi:retail-loyalty-rewards-models:1.0.0
com.smbtech.openapi:retail-loyalty-rewards-api:1.0.0
com.smbtech.openapi:retail-loyalty-rewards-client:1.0.0
```

Generated Java packages:

```text
com.smbtech.openapi.retailloyaltyrewards.model
com.smbtech.openapi.retailloyaltyrewards.api
com.smbtech.openapi.retailloyaltyrewards.client
```

Generated primary types:

```text
RewardsSummaryResponse
VoucherResponse
RetailLoyaltyRewardsApiDelegate
RetailLoyaltyRewardsApiController
RetailLoyaltyRewardsClient
```

The spec is tracked in `docs/openapi/spec-versions.properties`, participates in
models/API/client generation, local Maven publication, reproducibility
validation, and consumer-style compilation tests.

## 19. Current Validation

The current OpenAPI validation task is:

```bash
./gradlew validateOpenApiSpecs
```

The name normalization contract is validated separately by:

```bash
./gradlew validateOpenApiNameNormalization
```

The metadata contract is validated by:

```bash
./gradlew validateOpenApiMetadata
```

The spec version catalog is validated by:

```bash
./gradlew validateOpenApiSpecVersionCatalog
```

The models JAR contract is validated by:

```bash
./gradlew validateOpenApiModelsJar
```

The server API JAR contract is validated by:

```bash
./gradlew validateOpenApiServerApiJar
```

The client JAR contract is validated by:

```bash
./gradlew validateOpenApiClientJar
```

Generated artifact separation is validated by:

```bash
./gradlew validateOpenApiArtifactSeparation
```

The local publication contract is validated by:

```bash
./gradlew validateOpenApiLocalPublication
```

Reproducible generation is validated by:

```bash
./gradlew validateOpenApiReproducibleGeneration
```

Consumer-style compilation is validated by:

```bash
./gradlew validateOpenApiCompilationTests
```

The complete OpenAPI compatibility contract is validated by:

```bash
./gradlew openApiCompatibilityCheck
```

Public task-name compatibility is validated by:

```bash
./gradlew validateOpenApiTaskCompatibility
```

It scans OpenAPI/Swagger documents under:

```text
**/src/main/openapi/*.{yaml,yml,json}
**/openapi/*.{yaml,yml,json}
**/swagger/*.{yaml,yml,json}
```

The task currently rejects:

- missing `info.title`;
- missing `info.version`;
- empty normalized artifact base name;
- invalid Maven-compatible version;
- duplicate resolved coordinates across specs;
- generated artifact names that collide with framework module names.

The version catalog validation rejects:

- missing catalog entries for the current `info.title` and `info.version`;
- catalog entries whose `source`, `title`, or `sha256` no longer match the
  current spec;
- unsupported catalog key shapes.

The current YAML parser is intentionally narrow and validates only the `info`
block required for artifact coordinates. Full OpenAPI parsing belongs to the
future Gradle plugin implementation.

Accepted:

```yaml
openapi: 3.0.3
info:
  title: merchant-order-status
  version: '1.1.0'
```

The repository keeps this example as a validated fixture:

- [merchant-order-status.yaml](openapi/merchant-order-status.yaml)

Rejected:

```yaml
openapi: 3.0.3
info:
  title: ''
  version: latest
```

`validateOpenApiSpecs` is part of:

```bash
./gradlew documentationCheck
```

`validateOpenApiNameNormalization` is also part of `documentationCheck`.

`validateOpenApiSpecVersionCatalog` is also part of `documentationCheck`.

`validateOpenApiMetadata` is also part of `documentationCheck`.

`validateOpenApiModelsJar` is also part of `documentationCheck`.

`validateOpenApiServerApiJar` is also part of `documentationCheck`.

`validateOpenApiClientJar` is also part of `documentationCheck`.

`validateOpenApiArtifactSeparation` is also part of `documentationCheck`.

`validateOpenApiReproducibleGeneration` is also part of `documentationCheck`.

`validateOpenApiCompilationTests` is also part of `documentationCheck`.

`openApiBreakingChangeCheck` is also part of `documentationCheck`.
