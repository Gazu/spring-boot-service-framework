# OpenAPI Artifact Publishing

This is the canonical guide for publishing generated OpenAPI contract artifacts
to a local Maven repository or a remote private registry. Artifact contents and
dependency boundaries are defined in
[OpenAPI Artifact Generation](generation.md); plugin properties and task names
are defined in the [OpenAPI Gradle Plugin Reference](plugin-reference.md).

## Publication Contract

For each enabled contract artifact, the plugin creates a Maven publication with:

- the effective `groupId`, artifact base name, and contract version;
- a binary JAR;
- a generated sources JAR;
- a Maven POM;
- Gradle module metadata;
- Apache License 2.0 POM metadata; and
- the framework repository URL and generated contract description.

For effective identity `<group>:<name>:<version>`, the published coordinates
are:

```text
<group>:<name>-models:<version>
<group>:<name>-server-api:<version>
<group>:<name>-client:<version>
```

The `publishModels`, `publishServerApi`, and `publishClient` global and
per-contract flags control generation and publication together. A disabled
artifact does not create a publication.

## Before Publishing

Run the compatibility lifecycle explicitly before either publication task:

```bash
./gradlew smbtechOpenApiCompatibilityCheck
```

Publication generates and validates its required artifacts, but it does not run
`smbtechOpenApiCompatibilityCheck` automatically. CI should make compatibility
a required predecessor of remote publication.

Confirm that:

- `info.title` and `info.version` identify the intended release;
- the exact version has not already been published;
- the immutable baseline is committed when `requireBaseline` is enabled;
- generated consumer and reproducibility checks pass; and
- no repository credential is stored in tracked files.

## Local Publication

Publish all enabled generated artifacts with:

```bash
./gradlew smbtechOpenApiPublishToLocalRepository
```

The `smbtechOpenApiLocal` repository uses
`smbtechOpenApi.repositoryDirectory` and defaults to:

```text
build/repository/openapi
```

Change it only when the consuming build requires another project-local path:

```groovy
smbtechOpenApi {
    repositoryDirectory.set(layout.buildDirectory.dir('contract-repository'))
}
```

With the default group and repository, Maven files follow this layout:

```text
build/repository/openapi/
  com/smbtech/contracts/warehouse-inventory-catalog-models/1.0.0/
    warehouse-inventory-catalog-models-1.0.0.jar
    warehouse-inventory-catalog-models-1.0.0-sources.jar
    warehouse-inventory-catalog-models-1.0.0.pom
    warehouse-inventory-catalog-models-1.0.0.module
```

Gradle also writes `.md5`, `.sha1`, `.sha256`, and `.sha512` checksums beside
each published Maven file.

Local publication is project-scoped and does not write to `~/.m2/repository`.
The framework root convenience task `publishLocalArtifacts` includes generated
OpenAPI publication along with the framework modules.

## Consume Local Artifacts

Declare the generated repository before repositories that may contain an older
copy of the same coordinates:

```groovy
repositories {
    maven {
        url = uri('/absolute/path/to/contract-project/build/repository/openapi')
    }
    mavenCentral()
}

dependencies {
    implementation 'com.smbtech.contracts:warehouse-inventory-catalog-client:1.0.0'
}
```

Maven consumers can use the same file repository:

```xml
<repository>
  <id>smbtech-openapi-local</id>
  <url>file:///absolute/path/to/contract-project/build/repository/openapi</url>
</repository>
```

Do not use this directory as a shared or long-lived registry. It is a disposable
build output for local development and consumer verification.

## Remote Publication

Configure an absolute Maven repository URI with a Gradle provider:

```groovy
smbtechOpenApi {
    publicationRepositoryUrl.set(
            providers.environmentVariable('OPENAPI_REPOSITORY_URL')
    )
}
```

Then publish every enabled contract artifact:

```bash
./gradlew smbtechOpenApiPublish
```

The task targets the `smbtechOpenApiRemote` Maven repository. It fails with a
clear configuration error when `publicationRepositoryUrl` is absent and rejects
a configured URL that is not an absolute URI.

Repository retention, release promotion, signing, and overwrite prevention are
registry or CI responsibilities. The plugin does not implement those policies.

## Credentials

When the remote repository requires username and password authentication, the
plugin resolves each value in this order:

1. Gradle project property.
2. Environment variable.
3. Empty value for repositories that allow anonymous publication.

| Value | Gradle property | Environment variable |
|---|---|---|
| Username | `openApiRepositoryUsername` | `OPENAPI_REPOSITORY_USERNAME` |
| Password or token | `openApiRepositoryPassword` | `OPENAPI_REPOSITORY_PASSWORD` |

For local developer use, keep credentials in the user-level
`~/.gradle/gradle.properties`, never in the project:

```properties
openApiRepositoryUsername=<repository-user>
openApiRepositoryPassword=<repository-password-or-token>
```

For CI, use protected secret environment variables. Do not pass passwords with
`-P` on a shared runner because command arguments may be recorded.

## CI Publication

A remote publication job should run only after the compatibility gate succeeds:

```bash
./gradlew smbtechOpenApiCompatibilityCheck
./gradlew smbtechOpenApiPublish
```

Provide these values through the CI secret store:

```text
OPENAPI_REPOSITORY_URL
OPENAPI_REPOSITORY_USERNAME
OPENAPI_REPOSITORY_PASSWORD
```

Restrict the publication job to the repository's approved release branch or tag
policy. The OpenAPI plugin itself does not infer whether the current Git ref is
authorized to publish.

## Version Immutability

Treat `info.version` and every effective version override as immutable after
publication. Never replace an existing remote artifact with different contract
content under the same coordinate.

When the contract changes:

1. Classify the change with `smbtechOpenApiBreakingChangeCheck`.
2. Increment the version according to the documented SemVer policy.
3. Add the new immutable baseline.
4. Run `smbtechOpenApiCompatibilityCheck`.
5. Publish the new coordinates.

See [OpenAPI Contract Versioning](versioning.md) for the
baseline and versioning rules.

## Failure Modes

| Symptom | Cause | Resolution |
|---|---|---|
| `publicationRepositoryUrl is required for remote publication` | `smbtechOpenApiPublish` was invoked without a remote repository. | Configure `smbtechOpenApi.publicationRepositoryUrl` or use the local publication task. |
| `publicationRepositoryUrl must be an absolute URI` | The configured remote URL is relative or malformed. | Supply an absolute `https://` or supported Maven repository URI. |
| HTTP `401` or `403` | Credentials are absent, invalid, or not authorized for the target path. | Verify the CI secret names, Gradle property precedence, and registry permissions. |
| Repository rejects an existing version | The registry enforces immutable releases. | Increment the contract version; do not overwrite the published coordinate. |
| Consumer cannot resolve an artifact | The repository is missing, ordered incorrectly, or publication did not run. | Publish locally or remotely, then declare the matching repository and exact coordinate. |
| Server API or client publication is missing | Its artifact flag is disabled, or models are disabled. | Enable the required artifact; models must remain enabled for server API and client artifacts. |

## Validation

Validate publication documentation and behavior with:

```bash
./gradlew validateOpenApiPublishingDocumentation
./gradlew smbtechOpenApiPublishToLocalRepository
./gradlew smbtechOpenApiCompatibilityCheck
./gradlew documentationCheck
```

Return to the [OpenAPI Portal](index.md) for generation, implementation,
consumption, testing, and scaffolding workflows.
