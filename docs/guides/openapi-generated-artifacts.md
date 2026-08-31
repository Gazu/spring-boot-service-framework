# Generate OpenAPI Contract Artifacts

Use this procedure when an OpenAPI document must become versioned models,
server API, and REST client Maven artifacts. For a complete first adoption that
also implements and consumes those artifacts, use
[OpenAPI Getting Started](../openapi/getting-started.md).

## 1. Add The Spec

Place the document in a conventional OpenAPI directory or register its path in
the plugin DSL. It must declare a valid `info.title`, `info.version`, and at
least one operation with a unique `operationId`.

Use [OpenAPI Validation](../openapi/validation.md) for supported discovery
locations and document rules. Use the
[Gradle Plugin Reference](../openapi/plugin-reference.md) when explicit
registration or coordinate overrides are required.

## 2. Register The Baseline

Commit the immutable snapshot required by your compatibility policy. Baseline
layout, initial-version behavior, selection, and SemVer enforcement are owned
by [OpenAPI Contract Versioning](../openapi/versioning.md).

Do not modify a same-version contract after publication.

## 3. Generate Artifacts

Generate, compile, and package every enabled artifact kind:

```bash
./gradlew smbtechOpenApiAssemble
```

Binary and source JARs are written below `build/libs/smbtech-openapi`. Their
complete contents, package boundaries, Maven dependencies, metadata, and
kind-specific task names are defined in
[OpenAPI Artifact Generation](../openapi/generation.md).

## 4. Inspect The Result

For contract `retail-loyalty-rewards:1.0.0` with default settings, confirm these
coordinates:

```text
com.smbtech.contracts:retail-loyalty-rewards-models:1.0.0
com.smbtech.contracts:retail-loyalty-rewards-server-api:1.0.0
com.smbtech.contracts:retail-loyalty-rewards-client:1.0.0
```

Inspect generated sources only to diagnose the contract or template behavior.
Never edit build output.

## 5. Validate

Run the complete OpenAPI compatibility lifecycle before publication:

```bash
./gradlew smbtechOpenApiCompatibilityCheck
```

Review the evidence under `build/reports/smbtech-openapi`. The exact child tasks,
reports, and limits are documented in
[OpenAPI Validation](../openapi/validation.md).

## 6. Publish Locally

Publish the generated Maven modules to the configured local build repository:

```bash
./gradlew smbtechOpenApiPublishToLocalRepository
```

With defaults, consumers resolve them from `build/repository/openapi`. Repository
overrides, remote publication, credentials, CI ordering, and immutable release
rules are owned by
[OpenAPI Artifact Publishing](../openapi/publishing.md).

## Expected Result

The contract has immutable version identity, separated binary and source JARs,
compatibility evidence, and locally consumable Maven modules. Provider and
consumer implementation examples remain in
[OpenAPI Getting Started](../openapi/getting-started.md); new-service generation
is documented in [OpenAPI Project Scaffolding](../openapi/scaffolding.md).
