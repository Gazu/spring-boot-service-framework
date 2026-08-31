# Spring Boot Service Framework OpenAPI Gradle Plugin

Public Gradle plugin boundary for OpenAPI contract artifact generation.

This build logic owns Gradle-facing concerns: plugin id, DSL shape, task
registration, task wiring, and Gradle validation. Source generation is delegated
directly to OpenAPI Generator through typed task classes.

## Artifact

```text
com.smbtech:spring-boot-service-framework-openapi-gradle-plugin:<framework-version>
```

The Gradle plugin marker is published for
`com.smbtech.service-framework.openapi-generator`.

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

OpenAPI parsing, naming, metadata, source generation, packaging, and publication
are implemented inside this build-logic boundary.

## Usage

```groovy
plugins {
    id 'com.smbtech.service-framework.openapi-generator' version '0.5.0'
}

smbtechOpenApi {
    specs {
        register('merchantOrderStatus') {
            input.set(file('docs/openapi/merchant-order-status.yaml'))
        }
    }
}
```

Use the
[OpenAPI Gradle Plugin Reference](../../docs/openapi/plugin-reference.md) for
the complete DSL, defaults, validation rules, tasks, credentials, and build
wiring.

## Boundary

Build logic is implementation by default. The supported Java type exceptions
are `SmbtechOpenApiExtension` and `SmbtechOpenApiSpec`, which define the public
DSL model. `SmbtechOpenApiGeneratorPlugin`, concrete task classes, and build
wiring are implementation. Artifact kinds are internal task state and are not
part of the Java DSL. See
[Public API Boundaries](../../docs/public-api-boundaries.md).

| Concern | Location |
|---|---|
| Gradle plugin, task implementation, and publication wiring | This module |
| OpenAPI Generator template overrides | `spring-boot-service-framework-openapi-templates` |
| Hexagonal project scaffolding | `spring-boot-service-framework-project-generator` |
| User documentation | [OpenAPI Portal](../../docs/openapi/index.md) |
| First workflow | [OpenAPI Getting Started](../../docs/openapi/getting-started.md) |
| Gradle DSL and tasks | [OpenAPI Gradle Plugin Reference](../../docs/openapi/plugin-reference.md) |
| Generated artifacts | [OpenAPI Artifact Generation](../../docs/openapi/generation.md) |
| Documentation ownership | [OpenAPI Documentation Architecture](../../docs/openapi/documentation-architecture.md) |

## Local validation

```bash
./gradlew -p build-logic :spring-boot-service-framework-openapi-gradle-plugin:check
```

Repository-wide task and compatibility gates are defined in the
[OpenAPI Gradle Plugin Reference](../../docs/openapi/plugin-reference.md) and
[OpenAPI Validation](../../docs/openapi/validation.md).
