# OpenAPI Generator Build Logic

Internal Gradle plugin boundary for OpenAPI contract artifact generation.

This build logic owns Gradle-facing concerns: plugin id, DSL shape, task
registration, task wiring, and Gradle validation. Runtime-neutral generator code
belongs in `spring-boot-service-framework-openapi-generator`.

## Plugin ID

```groovy
plugins {
    id 'com.smbtech.service-framework.openapi-generator'
}
```

## Current Scope

The plugin owns the `smbtechOpenApi` extension, named `specs`, configuration
validation, all public OpenAPI generation and verification tasks, reproducible
artifact assembly, and Maven publication wiring. Applying the plugin is enough
to expose the complete OpenAPI build workflow; the root build does not apply a
separate generator script.

The plugin should own Gradle-facing behavior only:

- plugin id and plugin metadata;
- `smbtechOpenApi` extension shape;
- task registration and task wiring;
- Gradle provider/property usage;
- validation of Gradle configuration.

Reusable OpenAPI parsing, naming, metadata, source generation, and packaging
behavior belongs in `spring-boot-service-framework-openapi-generator`.

## Configuration API

```groovy
smbtechOpenApi {
    groupId.set('com.smbtech.openapi')
    outputDirectory.set(layout.buildDirectory.dir('generated/smbtech-openapi'))
    repositoryDirectory.set(layout.buildDirectory.dir('repository/openapi'))

    specs {
        register('merchantOrderStatus') {
            input.set(file('docs/openapi/merchant-order-status.yaml'))
        }
    }
}
```

Per-spec overrides:

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

Validation:

```bash
./gradlew smbtechOpenApiBuildLogicCheck
```

Full build-logic compatibility:

```bash
./gradlew openApiBuildLogicCheck
```

## Boundary

Build logic is implementation by default. The supported Java type exceptions
are `SmbtechOpenApiExtension` and `SmbtechOpenApiSpec`, which define the public
DSL model. `SmbtechOpenApiGeneratorPlugin`, concrete task classes, and build
wiring are implementation. See
[Public API Boundaries](../../docs/public-api-boundaries.md).

| Concern | Location |
|---|---|
| Gradle plugin, task implementation, and publication wiring | `build-logic/openapi-generator-plugin` |
| Generator domain/services | `spring-boot-service-framework-openapi-generator` |
| User documentation | [OpenAPI Code Generation](../../docs/openapi-codegen.md) |
| Evolution roadmap | [OpenAPI Generator Evolution](../../docs/openapi-evolution.md) |

## Local validation

```bash
./gradlew -p build-logic :openapi-generator-plugin:check
```

From the repository root, use:

```bash
./gradlew openApiBuildLogicCheck
./gradlew openApiGradlePluginCompatibilityCheck
./gradlew openApiCompatibilityCheck
```
