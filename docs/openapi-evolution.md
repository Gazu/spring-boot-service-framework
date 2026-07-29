# OpenAPI Generator Evolution

This document defines the post-split evolution path for the OpenAPI generator.
The Gradle workflow is owned by an explicit plugin and runtime-neutral behavior
is owned by a reusable generator module.

## Goals

- Keep generated artifact coordinates stable while the implementation moves.
- Move reusable OpenAPI behavior into
  `spring-boot-service-framework-openapi-generator`.
- Move Gradle DSL, task registration, task wiring, and publication wiring into
  `build-logic/openapi-generator-plugin`.
- Keep public Gradle command names compatible during the migration.
- Expand generated source coverage without coupling runtime modules to build
  tooling.

## Non Goals

- Do not add Spring Boot runtime dependencies to the generator module.
- Do not make generated artifacts depend on the generator implementation.
- Do not remove legacy task names before a documented compatibility path exists.
- Do not publish changed OpenAPI content under the same `info.version`.

## Migration Stages

| Stage | Main move | Target owner | Compatibility requirement |
|---|---|---|---|
| 1 | Spec discovery, parsing, validation, and name normalization | `spring-boot-service-framework-openapi-generator` | `validateOpenApiSpecs` and `validateOpenApiNameNormalization` keep the same public names. |
| 2 | Contract metadata rendering and SHA/version catalog validation | `spring-boot-service-framework-openapi-generator` | Metadata keys and `docs/openapi/spec-versions.properties` semantics stay stable. |
| 3 | Models, server API, and client source rendering | `spring-boot-service-framework-openapi-generator` | Generated package names, class names, annotations, and artifact separation remain compatible. |
| 4 | JAR packaging and reproducibility services | `spring-boot-service-framework-openapi-generator` | JAR entry ordering, timestamps, manifests, and metadata paths remain deterministic. |
| 5 | Typed Gradle task classes and provider-based inputs/outputs | `build-logic/openapi-generator-plugin` | Existing task names remain available as plugin-registered tasks or aliases. |
| 6 | Maven publication registration for generated artifacts | `build-logic/openapi-generator-plugin` | `publishOpenApiArtifactsToLocalBuildRepository` and published coordinates remain compatible. |
| 7 | Remove root script coupling | `build-logic/openapi-generator-plugin` | Completed: applying the plugin exposes the existing public task names and publication workflow. |

Current modernization status:

- `OpenApiSpecReader` uses Jackson 3 structural parsing for YAML and JSON;
- generator, contract testing, and mock loading accept OpenAPI 3.0 and 3.1 and
  reject Swagger 2 explicitly;
- `validateOpenApiSpecs` is implemented by
  `SmbtechOpenApiValidateSpecsTask` with declared, path-sensitive inputs;
- extension inputs are wired with providers and no longer require
  `afterEvaluate`;
- the remaining source-generation and publication tasks retain their public
  names while their Groovy implementations are migrated incrementally to typed
  task classes.

## Public Task Compatibility

These task names are public compatibility points until a major release documents
a replacement:

```text
validateOpenApiNameNormalization
validateOpenApiSpecs
generateOpenApiSpecVersionCatalog
validateOpenApiSpecVersionCatalog
generateOpenApiMetadata
validateOpenApiMetadata
generateOpenApiModels
openApiModelsJar
validateOpenApiModelsJar
generateOpenApiServerApi
openApiServerApiJar
validateOpenApiServerApiJar
generateOpenApiClient
openApiClientJar
validateOpenApiClientJar
validateOpenApiArtifactSeparation
validateOpenApiReproducibleGeneration
generateOpenApiCompilationTests
validateOpenApiCompilationTests
openApiBreakingChangeCheck
publishOpenApiArtifactsToLocalBuildRepository
validatePublishedOpenApiArtifacts
validateOpenApiTaskCompatibility
validateOpenApiGeneratorModuleCompatibility
openApiBuildLogicCheck
openApiCompatibilityCheck
```

New plugin-native task types may be introduced at any stage, but the public
names above must continue to exist and keep meaningful descriptions and groups.

## Generator Capability Roadmap

### Contract Coverage

- Implemented: structural breaking change detection with committed baselines,
  stable change codes, SemVer enforcement, and optional strict mode.
- Request bodies with multiple media types.
- Query, path, header, and cookie parameters with required flags.
- Operation tags and configurable grouping.
- Standard error response models.
- Security schemes as generated documentation and optional client metadata.
- Pagination conventions when declared by the contract.

### Schema Coverage

- Implemented: top-level string enums with Jackson values.
- Implemented: inline string enums as nested Java enums.
- Implemented: typed arrays, typed maps, component `$ref` properties, and Java
  time type mapping for `date` and `date-time`.
- Implemented: string length, pattern, array size, integer range, decimal range,
  and required-field validation annotations.
- Future: nullable fields with explicit nullability metadata.
- Future: `oneOf`, `anyOf`, and `allOf` with explicit generation rules.
- Future: external `$ref` files and polymorphic schema annotations.
- OpenAPI 3.1 documents are structurally accepted; JSON Schema 2020-12 features
  are supported only when explicitly listed above.

### Server API Coverage

- Delegate interfaces with generated request/response types.
- Spring MVC annotations derived from the contract.
- Optional validation annotations on method parameters and request bodies.
- Generated operation names that are stable across regeneration.

### Client Coverage

- Spring HTTP interfaces compatible with `@HttpApiClient`.
- Header and query parameter generation.
- Optional generated client configuration metadata.
- Artifact dependencies limited to models and framework public APIs.

## Implementation Rules

- Reusable behavior belongs in Java services under
  `spring-boot-service-framework-openapi-generator`.
- Gradle behavior belongs in `build-logic/openapi-generator-plugin`.
- The root script must not regain large generator implementation blocks.
- Generated output must be reproducible for the same spec, framework version,
  and generator configuration.
- Every generated artifact must include
  `META-INF/smbtech/openapi/contract.properties`.
- Every public behavior change must update
  [OpenAPI Code Generation](openapi-codegen.md) and the generated artifact
  guide.

## Validation Gates

Run these commands when evolving the generator:

```bash
./gradlew :spring-boot-service-framework-openapi-generator:check
./gradlew openApiBuildLogicCheck
./gradlew openApiCompatibilityCheck
./gradlew documentationCheck
./gradlew compatibilityCheck
```

The minimum compatibility contract is:

- generator module tests pass;
- build-logic plugin checks pass;
- public OpenAPI task names remain available;
- generated metadata, JARs, publication layout, and compilation tests pass;
- documentation catalog and links remain valid.

## Completion Criteria

The migration is complete when:

- the generator module owns all spec parsing, source rendering, metadata, and
  packaging behavior;
- the build-logic plugin owns the DSL, typed task registration, task wiring, and
  publication registration;
- the root build has no generator implementation script or compatibility
  wrapper;
- `openApiCompatibilityCheck` still protects generated coordinates, source
  compatibility, reproducibility, artifact separation, and publication layout;
- module READMEs, `docs/openapi-codegen.md`, the generated artifact guide, and
  this roadmap describe the current owner of each public behavior.

## See Also

- [OpenAPI Code Generation](openapi-codegen.md)
- [Generate OpenAPI Contract Artifacts](guides/openapi-generated-artifacts.md)
- [OpenAPI generator README](../spring-boot-service-framework-openapi-generator/README.md)
- [OpenAPI Generator Build Logic](../build-logic/openapi-generator-plugin/README.md)
- [Compatibility](compatibility.md)
