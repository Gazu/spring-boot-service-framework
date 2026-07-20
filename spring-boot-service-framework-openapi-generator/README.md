# Spring Boot Service Framework OpenAPI Generator

Runtime-neutral OpenAPI generator services for service contracts. This module
contains reusable spec parsing, naming, metadata, packaging, and compatibility
behavior used by the build workflow.

The Gradle plugin, the `smbtechOpenApi` DSL, generation task implementation,
task wiring, and publication wiring belong in
`build-logic/openapi-generator-plugin`, not in this runtime-neutral module.

## When to use

Use this module when evolving or testing the OpenAPI generator implementation
itself.

Application teams should consume the generated artifacts, not this module
directly. Runtime REST client behavior still belongs in
`spring-boot-service-framework-starter-rest-client`.

## Dependency

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-openapi-generator:0.3.0'
}
```

## Public API

- Package boundary: `com.smbtech.serviceframework.openapi.generator`.
- Spec metadata: `OpenApiSpecInfo` and `OpenApiSpecReader`.
- Naming: `OpenApiNameNormalizer`.
- Artifacts: `OpenApiArtifactKind` and `OpenApiPublicationDescriptor`.
- Metadata: `OpenApiContractMetadata` and `OpenApiMetadataGenerator`.
- Source-generation boundaries: `OpenApiModelGenerator`,
  `OpenApiServerApiGenerator`, and `OpenApiClientGenerator`.
- Packaging: `OpenApiJarPackager`.
- Compatibility: `OpenApiBreakingChangeDetector`, `OpenApiBaselineResolver`,
  `OpenApiCompatibilityReport`, stable change codes, and SemVer comparison.

The current generator services expose structure and small reusable behavior.
Full source generation will move from the root build into these types in later
phases.

The package responsibility is documented by `package-info.java`; the artifact
does not expose an empty Java marker type.

## Compatibility Contract

This module is part of the root OpenAPI compatibility contract. Public reusable
types listed above must remain present during the migration unless a documented
replacement and compatibility path are added.

Validate only this module:

```bash
./gradlew :spring-boot-service-framework-openapi-generator:check
./gradlew openApiGeneratorCompatibilityCheck
```

Validate the root guard that protects the module boundary:

```bash
./gradlew validateOpenApiGeneratorModuleCompatibility
```

## What this module does not do

- It does not provide Spring Boot auto-configuration.
- It does not run at application runtime.
- It does not contain generated contract sources.
- It does not register Gradle tasks or Maven publications.

## Main documentation

| Topic | Document |
|---|---|
| OpenAPI code generation | [OpenAPI Code Generation](../docs/openapi-codegen.md) |
| Breaking change detection | [OpenAPI Breaking Change Detection](../docs/openapi-breaking-changes.md) |
| Generator evolution roadmap | [OpenAPI Generator Evolution](../docs/openapi-evolution.md) |
| Generated artifact workflow | [Generate OpenAPI Contract Artifacts](../docs/guides/openapi-generated-artifacts.md) |
| OpenAPI Gradle build logic | [OpenAPI Generator Build Logic](../build-logic/openapi-generator-plugin/README.md) |
| Compatibility policy | [Compatibility](../docs/compatibility.md) |
| Module README rules | [Module README Convention](../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-openapi-generator:check
```

The generator tests cover spec metadata parsing, name normalization,
publication descriptors, contract metadata, metadata file layout, package
boundaries, reproducible JAR packaging, breaking change classification,
baseline selection, CLI policy, and SemVer precedence.

For a complete OpenAPI compatibility run, use:

```bash
./gradlew openApiCompatibilityCheck
```
