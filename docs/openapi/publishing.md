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
<group>:<name>-jdk21-model:<version>
<group>:<name>-jdk21-api:<version>
<group>:<name>-jdk21-client:<version>
```

`<version>` is always the contract's `info.version`. The API and client POMs
declare the matching `-jdk21-model` coordinate as a transitive dependency.
The client POM does not expose Spring Cloud OpenFeign transitively.

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

Publish one contract without building or publishing the artifacts of another
contract:

```bash
./gradlew smbtechOpenApiPublishContractToLocalRepository \
  -PopenApiContract=ms-kyc-profile/swagger/openapi.yaml
```

`openApiContract` is the canonical project-relative path of an explicitly
configured or automatically discovered contract. The task fails when the
selector is absent or does not resolve to exactly one configured input.

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
  com/smbtech/contracts/warehouse-inventory-catalog-jdk21-model/1.0.0/
    warehouse-inventory-catalog-jdk21-model-1.0.0.jar
    warehouse-inventory-catalog-jdk21-model-1.0.0-sources.jar
    warehouse-inventory-catalog-jdk21-model-1.0.0.pom
    warehouse-inventory-catalog-jdk21-model-1.0.0.module
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
    implementation 'com.smbtech.contracts:warehouse-inventory-catalog-jdk21-client:1.0.0'
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

The plugin also resolves the URL from `openApiRepositoryUrl` and then
`OPENAPI_REPOSITORY_URL`, so CI does not need to modify the build script.

Then publish every enabled contract artifact:

```bash
./gradlew smbtechOpenApiPublish
```

Publish only one contract's enabled model, API, and client artifacts with:

```bash
./gradlew smbtechOpenApiPublishContract \
  -PopenApiContract=ms-kyc-profile/swagger/openapi.yaml
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
| Repository URL | `openApiRepositoryUrl` | `OPENAPI_REPOSITORY_URL` |
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

`.github/workflows/publish-openapi-contracts.yml` runs on changes to direct YAML,
YML, or JSON files in conventional `src/main/openapi`, `openapi`, and `swagger`
directories on `main`. Its detection job emits canonical project-relative
paths, the compatibility job runs once, and a matrix publishes each changed
contract independently.

The workflow can also be dispatched manually with one contract path. Each
matrix entry invokes:

```bash
./gradlew contractTestingCompatibilityCheck smbtechOpenApiCompatibilityCheck
./gradlew smbtechOpenApiPublishContract \
  -PopenApiContract=ms-kyc-profile/swagger/openapi.yaml
```

The first command publishes the current checkout only to disposable local Maven
repositories, runs the standalone generated-contract consumer, and then runs
the plugin compatibility suite. The remote matrix job starts only after both
validations succeed.

Provide these values through the CI secret store:

```text
OPENAPI_REPOSITORY_URL
OPENAPI_REPOSITORY_USERNAME
OPENAPI_REPOSITORY_PASSWORD
```

Restrict the publication job to the repository's approved release branch or tag
policy. The OpenAPI plugin itself does not infer whether the current Git ref is
authorized to publish.

The repository workflow uses the `release` environment and maps the existing
`PRIVATE_MAVEN_URL`, `PRIVATE_MAVEN_USERNAME`, and `PRIVATE_MAVEN_PASSWORD`
secrets to those OpenAPI variables. Deleted contracts are ignored. A failed
matrix entry does not cancel publication of another changed contract, while the
workflow concurrency policy never cancels an in-progress publication.

Each matrix entry stages the selected artifacts into a dedicated local Maven
repository, records their hashes with `generateOpenApiPilotManifest`, and
resolves them from an isolated consumer. Before upload it rejects partial or
conflicting coordinates; after upload it verifies the remote payload and Maven
dependency graph against the same manifest. The manual protected procedure is
documented in the [OpenAPI Contract Pilot](../pilot/README.md).

## Version Immutability

Treat `info.version` as immutable after publication. Never replace an existing
remote artifact with different contract content under the same coordinate. A
configured version override must equal `info.version`.

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
| `Contract publication requires -PopenApiContract` | An independent publication task was called without a selector. | Pass the project-relative path of one configured or discovered contract. |
| `OpenAPI contract ... is not configured or discovered` | The selector does not match a registered input. | Use an existing direct contract file in a conventional folder or register the path explicitly. |
| HTTP `401` or `403` | Credentials are absent, invalid, or not authorized for the target path. | Verify the CI secret names, Gradle property precedence, and registry permissions. |
| Repository rejects an existing version | The registry enforces immutable releases. | Increment the contract version; do not overwrite the published coordinate. |
| Consumer cannot resolve an artifact | The repository is missing, ordered incorrectly, or publication did not run. | Publish locally or remotely, then declare the matching repository and exact coordinate. |
| Server API or client publication is missing | Its artifact flag is disabled, or models are disabled. | Enable the required artifact; models must remain enabled for server API and client artifacts. |

## Validation

Validate publication documentation and behavior with:

```bash
./gradlew validateOpenApiPublishingDocumentation
./gradlew smbtechOpenApiPublishToLocalRepository
./gradlew smbtechOpenApiPublishContractToLocalRepository \
  -PopenApiContract=docs/openapi/warehouse-inventory-catalog.yaml
./gradlew smbtechOpenApiCompatibilityCheck
./gradlew documentationCheck
```

Return to the [OpenAPI Portal](index.md) for generation, implementation,
consumption, testing, and scaffolding workflows.
