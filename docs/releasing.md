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

## Version Sources

The framework artifact version is defined in `gradle.properties`:

```properties
frameworkVersion=0.4.0
```

The root build applies that version to every framework artifact. The supported
Spring Boot version is defined in the same file:

```properties
springBootVersion=4.1.0
```

The platform publishes the same framework version and imports the Spring Boot
BOM at this exact Spring Boot version.

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
git checkout -b release/v0.4.0
```

2. Confirm the target version and update `gradle.properties` if needed:

```properties
frameworkVersion=0.4.0
```

3. Refresh generated property references after configuration changes:

```bash
./gradlew generatePropertyReferences
./gradlew generateModuleCompatibilityContracts
./gradlew generatePlatformCompatibilityContract
./gradlew generatePublicApiInventory
./gradlew generatePublicInternalTypeBaseline
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

5. Run documentation validation:

```bash
./gradlew openApiBreakingChangeCheck
./gradlew documentationCheck
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
The release workflow performs that native compile and runtime smoke test for the
logging consumer.

It also enforces module line coverage, verifies dependency checksums from
`gradle/verification-metadata.xml`, validates complete Maven POM metadata and
reproducible archives, and generates validated, deterministic CycloneDX 1.6
SBOMs containing production runtime dependencies:

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
git commit -m "chore(release): prepare v0.4.0"
```

10. Create the signed release tag:

```bash
git tag -s v0.4.0 -m "Release v0.4.0"
git verify-tag v0.4.0
```

11. Push the branch and tag when the release is approved:

```bash
git push origin release/v0.4.0
git push origin v0.4.0
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
./gradlew publishOpenApiArtifactsToLocalBuildRepository
./gradlew validateOpenApiLocalPublication
./gradlew openApiCompatibilityCheck
```

Use Maven local when another local application consumes the framework through
`mavenLocal()`:

```bash
./gradlew publishToMavenLocal
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

./gradlew publish -PreleaseBuild=true
```

`SIGNING_KEY` must contain an ASCII-armored private key. Repository credentials
also support equivalent Gradle properties:

```bash
./gradlew publish \
  -PreleaseBuild=true \
  -PprivateMavenUrl=https://maven.example.com/releases \
  -PprivateMavenUsername=user \
  -PprivateMavenPassword=secret
```

The canonical remote path is `.github/workflows/release.yml`. It verifies that
the tag matches `frameworkVersion`, cryptographically verifies the tag, runs
the release gate, creates a reproducible release bundle with signed Maven
artifacts and SBOMs, records GitHub build provenance, and then publishes.

Required `release` environment secrets are:

- `PRIVATE_MAVEN_URL`
- `PRIVATE_MAVEN_USERNAME`
- `PRIVATE_MAVEN_PASSWORD`
- `SIGNING_KEY`
- `SIGNING_PASSWORD`

## Breaking Changes

For any breaking change:

- use `!` in the Conventional Commit subject or add a `BREAKING CHANGE:` footer;
- increase the affected OpenAPI contract major version and review
  `openApiBreakingChangeCheck` output;
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
- verify the private registry contains the platform POM and every managed
  framework artifact at the same version;
- verify a clean consumer can resolve the released version;
- create the next development version only when the repository workflow needs
  snapshot or pre-release versions.

Useful checks:

```bash
git show --stat v0.4.0
./gradlew publishLocalArtifacts consumerSmoke
```
