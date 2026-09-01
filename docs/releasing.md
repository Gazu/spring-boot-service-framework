# Releasing

This guide is the canonical release process for Spring Boot Service Framework.
It applies to private and internal distributions of the repository artifacts.

## Release Rules

- Use Semantic Versioning: `MAJOR.MINOR.PATCH`.
- Use English Conventional Commits.
- Update `CHANGELOG.md` in the same pull request as the release version.
- Every released version must have a Git tag named `vMAJOR.MINOR.PATCH`.
- Release tags and Maven publications must be signed by the configured release
  key.
- Do not publish artifacts that contain real credentials, client ids, endpoints,
  keystores, or other environment-specific values.
- Keep source, documentation, examples, generated property references, and
  compatibility notes aligned before creating the tag.
- Merge the release preparation through the protected `main` branch before
  creating the signed tag.

## Version Sources

The framework artifact version is defined in `gradle.properties`:

```properties
frameworkVersion=0.5.1
```

The root build applies that version to every framework artifact. The supported
Spring Boot version is defined in the same file:

```properties
springBootVersion=4.1.0
```

The platform publishes the same framework version and imports the Spring Boot
BOM at this exact Spring Boot version.

## Release Artifact Contract

The canonical publication set is versioned in
[`gradle/release-artifacts.txt`](../gradle/release-artifacts.txt). Every entry
uses `frameworkVersion`; generated `com.smbtech.contracts` example artifacts are
outside this framework release contract.

The manifest covers the platform, libraries, starters, OpenAPI Gradle plugin,
OpenAPI templates, and Gradle plugin marker. Any added, removed, or renamed Maven
publication requires an explicit manifest review.

Every release POM keeps its project URL, SCM, and issue tracker pointed at the
producer repository. Its deployment target is declared separately with the
canonical registry metadata:

```xml
<distributionManagement>
  <repository>
    <id>github</id>
    <name>SMB Tech GitHub Packages</name>
    <url>https://maven.pkg.github.com/gazu/service-framework-packages</url>
  </repository>
</distributionManagement>
```

Snapshot repository metadata is forbidden because snapshot publication is not
part of the registry contract. `validatePublishedPomMetadata` and the included
build-logic publication check validate the effective generated POM XML.

Validate the manifest against the publications exposed by the main build and
the included `build-logic` build:

```bash
./gradlew releaseArtifactManifestCheck
```

The check is part of `supplyChainCheck` and therefore runs from both pull request
and release gates.

## Release Lifecycle

The versioned lifecycle contract is stored in
[`gradle/release-lifecycle.properties`](../gradle/release-lifecycle.properties).
Its public Gradle entry points are:

| Task | Responsibility |
|---|---|
| `releaseGate` | Runs compatibility, documentation, consumer, AOT, supply-chain, and publication-contract checks without publishing remotely. |
| `releaseCandidate` | Runs the complete gate, verifies the unpublished unsigned bundle, and records its commit and checksum for review. |
| `generateReleaseNotes` | Extracts the current version from `CHANGELOG.md` into standalone notes with tag-stable documentation links. |
| `verifyReleaseBundle` | Verifies the exact manifest coordinates, complete archive content, payload hashes, external archive checksum, and signatures required during a release build. |
| `prepareRelease` | Requires `-PreleaseBuild=true`, validates the signed tag and changelog, runs the gate, and creates the verified signed bundle. |
| `publishRelease` | Requires private registry credentials and publishes the main build, OpenAPI Gradle plugin, plugin marker, and templates. |

Create the reviewable candidate from the release commit without signing or
registry credentials:

```bash
./gradlew clean releaseCandidate
```

Review `build/reports/release/candidate.properties`, the generated release
notes, archive, and external checksum. `source_clean=false` records that the
candidate was assembled from a dirty checkout; the signed release must always
come from the merged tag commit.

Use `releaseGate` during development. Only the signed tag workflow should invoke
the publishing lifecycle:

```bash
./gradlew clean prepareRelease -PreleaseBuild=true
./gradlew publishRelease -PreleaseBuild=true
```

`releaseLifecycleCheck` prevents CI, documentation, and task wiring from
drifting away from this contract.

The reproducible archive is written to
`build/distributions/spring-boot-service-framework-VERSION.zip`, accompanied by
`spring-boot-service-framework-VERSION.zip.sha256`. It contains only the 17
framework coordinates declared by `gradle/release-artifacts.txt`; generated
`com.smbtech.contracts` example artifacts are rejected.

The archive layout is:

```text
repository/                      Maven repository for the framework artifacts
sbom/bom.json                    CycloneDX JSON SBOM
sbom/bom.xml                     CycloneDX XML SBOM
documentation/README.md          Project overview
documentation/CHANGELOG.md       Release notes
documentation/RELEASE_NOTES.md   Standalone notes for the current version
documentation/LICENSE            Apache License 2.0
documentation/releasing.md       Release and publication procedure
metadata/release-artifacts.txt   Canonical publication contract
metadata/release-lifecycle.properties
metadata/release-manifest.json   Version and artifact identity
metadata/SHA256SUMS              SHA-256 for every other archive entry
```

Verify the downloaded archive before extracting it:

```bash
cd build/distributions
shasum -a 256 -c spring-boot-service-framework-0.5.1.zip.sha256
```

When preparing a release, update every consumer-facing version example that
mentions the framework version. At minimum, check:

- `README.md`
- `docs/dependency-management.md`
- `docs/**/*.md`
- `examples/*/build.gradle`
- `spring-boot-service-framework-platform/README.md`
- module `README.md` files

## Release Checklist

1. Create a release branch:

```bash
git checkout -b release/v0.5.1
```

2. Confirm the target version and update `gradle.properties` if needed:

```properties
frameworkVersion=0.5.1
```

3. Refresh generated property references after configuration changes:

```bash
./gradlew generatePropertyReferences
./gradlew generateModuleCompatibilityContracts
./gradlew generatePlatformCompatibilityContract
./gradlew generatePublicApiInventory
./gradlew generatePublicInternalTypeBaseline
./gradlew generatePublicTypeClassificationBaseline
./gradlew generateConcreteReplaceableBeanBaseline
```

Review module compatibility contract diffs. Do not accept a generated change
without confirming that it belongs to a supported public surface.
Review the internal-type baseline separately; these types remain unsupported
implementation even though JVM wiring requires public visibility.

4. Update `CHANGELOG.md`:

- move relevant entries from `Unreleased` into the target version;
- add the release date;
- group entries under `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, and
  `Security` when those groups apply;
- call out breaking changes clearly.

Generate and inspect the standalone notes that will accompany the release:

```bash
./gradlew generateReleaseNotes validateReleaseNotes
```

The generated file is written to
`build/release/documentation/RELEASE_NOTES.md`. Do not maintain a second source
file; correct `CHANGELOG.md` and regenerate it.

5. Run documentation validation:

```bash
./gradlew smbtechOpenApiBreakingChangeCheck
./gradlew openApiDocumentationCompatibilityCheck
./gradlew documentationCheck
./gradlew releaseArtifactManifestCheck
```

6. Run the full framework check:

```bash
./gradlew check
```

7. Run compatibility and standalone consumer smoke tests:

```bash
./gradlew compatibilityCheck
```

This includes `platformCompatibilityCheck`, which validates the framework
constraints, imported Spring Boot BOM, Maven POM, local publication metadata,
and committed platform contract.

It also includes `actuatorCompatibilityCheck`, which validates the neutral
Actuator API, stable runtime names, endpoint and metric contracts,
documentation, and the published HTTP/AOT consumer.

This includes japicmp comparison against the version configured by
`binaryCompatibilityBaselineVersion`. New binary incompatibilities fail unless
they are documented and narrowly approved in
`gradle/compatibility/binary-breaking-changes.txt`.

The same checks can be executed from a clean workspace through the release
gate used by the tag-only publication workflow:

```bash
./gradlew clean releaseGate
git diff --check
git diff --exit-code
```

`releaseGate` includes `nativeAotCheck`, which runs Spring Boot `processAot` for
all published consumers without requiring GraalVM. Before a native-image
release, additionally run `nativeCompile` with GraalVM 25 or a compatible
Native Image Kit in at least one consumer.
The framework release workflow guarantees AOT processing; native executable
certification remains an explicit consumer-level validation.

It also enforces module line coverage, verifies dependency checksums from
`gradle/verification-metadata.xml`, validates complete Maven POM metadata and
reproducible archives, and generates validated, deterministic CycloneDX 1.6
SBOMs containing only published framework runtime components and their
production dependencies. Examples, quality pilots, and test dependencies are
excluded. Framework components declare Apache-2.0, and the JSON/XML dependency
graphs must be complete, connected, and equivalent:

```bash
./gradlew dependencyTrackSbom
```

```text
build/reports/bom.json
build/reports/bom.xml
```

When an intentional dependency update introduces new artifacts, regenerate
and review checksums in the same pull request:

```bash
./gradlew --write-verification-metadata sha256 clean releaseGate
git diff -- gradle/verification-metadata.xml
```

8. Review the final diff:

```bash
git status --short
git diff
```

9. Commit the release changes with a Conventional Commit:

```bash
git add .
git commit -m "chore(release): prepare v0.5.1"
```

10. Push the release branch and open a pull request:

```bash
git push --set-upstream origin release/v0.5.1
```

Wait for `Pull Request / Policy`, `Pull Request / Quality`, and
`Pull Request / Security`, obtain the required independent approval, resolve
every conversation, and merge with squash or rebase.

11. Update the local protected branch to the approved commit:

```bash
git checkout main
git pull --ff-only origin main
```

12. Create the signed release tag on the merged commit:

```bash
git tag -s v0.5.1 -m "Release v0.5.1"
git verify-tag v0.5.1
```

13. Push the approved tag:

```bash
git push origin v0.5.1
```

## Publishing Locally

Use local publication to verify examples against published artifacts instead of
project dependencies:

```bash
./gradlew publishLocalArtifacts
./gradlew consumerSmoke
```

This publishes the framework platform before the standalone consumers resolve
their versionless framework dependencies.

Generated OpenAPI artifacts can be published and verified independently:

```bash
./gradlew smbtechOpenApiPublishToLocalRepository
./gradlew smbtechOpenApiCompatibilityCheck
```

Use Maven local when another local application consumes the framework through
`mavenLocal()`:

```bash
./gradlew publishToMavenLocal publishBuildLogicToMavenLocal
```

## Publishing To A Private Registry

Private registry publication requires repository URL and credentials. Prefer
environment variables or Gradle properties managed outside source control.

```bash
export PRIVATE_MAVEN_URL=https://maven.example.com/releases
export PRIVATE_MAVEN_USERNAME=user
export PRIVATE_MAVEN_PASSWORD=secret
export SIGNING_KEY="$(cat release-signing-key.asc)"
export SIGNING_PASSWORD=secret

./gradlew publishRelease -PreleaseBuild=true
```

`SIGNING_KEY` must contain an ASCII-armored private key. Repository credentials
also support equivalent Gradle properties:

```bash
./gradlew publishRelease \
  -PreleaseBuild=true \
  -PprivateMavenUrl=https://maven.example.com/releases \
  -PprivateMavenUsername=user \
  -PprivateMavenPassword=secret
```

`publishRelease` publishes the OpenAPI Gradle plugin implementation, its Gradle
plugin marker, and the versioned OpenAPI templates in addition to every
framework module. The lifecycle starts the included `build-logic` publication
only after release preparation has passed.

The producer entry point is `.github/workflows/release.yml`. It delegates the
release to the repository-local `.github/workflows/publish-gradle-maven.yml`.
The reusable workflow verifies that the tag matches `frameworkVersion`, cryptographically
verifies the tag, runs the release gate, creates a reproducible release bundle
with signed Maven artifacts and SBOMs, verifies the archive checksum, records
GitHub build provenance, publishes, and resolves every manifest POM. Release
credentials remain in this repository's `release` environment and are exposed
only to the reusable job steps that consume them.

GitHub does not allow this public repository to call a reusable workflow from
the private registry repository. The registry maintains the same workflow for
private producers; this public copy is required for this producer and is
reviewed by `releaseLifecycleCheck`. Other public producers must reference this
workflow using a full commit SHA. Branch references such as `@main` are not
allowed.

Required `release` environment secrets are:

- `PRIVATE_MAVEN_URL`, set to
  `https://maven.pkg.github.com/gazu/service-framework-packages`
- `PRIVATE_MAVEN_USERNAME`, the GitHub username that owns the token
- `PRIVATE_MAVEN_PASSWORD`, a classic personal access token with
  `read:packages` and `write:packages`
- `SIGNING_KEY`
- `SIGNING_PASSWORD`

The release workflow publishes to the private GitHub Packages repository at
the URL configured in `PRIVATE_MAVEN_URL`. Before the build starts, it verifies
the canonical registry URL, authenticates the declared GitHub user, confirms
the classic PAT scopes, and checks read access to an existing private package.

Publication, consumption, administration, and signing use separate identities:

- producer workflows use only the five `release` environment secrets above;
- consumer repositories use their own classic PAT with `read:packages` only;
- package cleanup uses the registry repository's ephemeral `github.token` and
  never receives the publisher PAT;
- OpenPGP signing secrets are never exposed to consumers or cleanup workflows.

Rotate the publisher PAT at least every 90 days. Create the replacement with
only `read:packages` and `write:packages`, replace
`PRIVATE_MAVEN_USERNAME` and `PRIVATE_MAVEN_PASSWORD` in the `release`
environment, run the release identity check, and revoke the previous token.
`PACKAGES_USERNAME` and `PACKAGES_TOKEN` are legacy duplicate names and must not
be used by this repository.

Reusable publication can be checked without writing packages from **Actions >
Publish Release > Run workflow** by entering an existing signed tag and leaving
`dry_run` enabled. Failed publications can be retried by entering the tag and
disabling `dry_run`; the workflow checks out and verifies that tag before
publishing it with the current release configuration.

## Breaking Changes

For any breaking change:

- use `!` in the Conventional Commit subject or add a `BREAKING CHANGE:` footer;
- increase the affected OpenAPI contract major version and review
  `smbtechOpenApiBreakingChangeCheck` output;
- update `docs/compatibility.md`;
- update affected guides and examples;
- add migration notes to `CHANGELOG.md`;
- use a major version bump after `1.0.0`.

During `0.x`, incompatible changes may still happen, but they must be explicit
in release notes and compatibility documentation. Update
`binaryCompatibilityBaselineVersion` only after the referenced release tag is
available and keep approved exclusions scoped to individual members.

## Post-Release

After the release is published:

- verify the tag points to the intended commit;
- verify the private registry contains the platform POM, every managed
  framework artifact, the OpenAPI Gradle plugin, its marker, and the OpenAPI
  templates at the same version;
- verify a clean consumer can resolve the released version;
- create the next development version only when the repository workflow needs
  snapshot or pre-release versions.

Useful checks:

```bash
git show --stat v0.5.1
./gradlew publishLocalArtifacts consumerSmoke
```
