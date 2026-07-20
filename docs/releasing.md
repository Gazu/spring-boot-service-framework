# Releasing

This guide is the canonical release process for Spring Boot Service Framework.
It applies to private and internal distributions of the repository artifacts.

## Release Rules

- Use Semantic Versioning: `MAJOR.MINOR.PATCH`.
- Use English Conventional Commits.
- Update `CHANGELOG.md` in the same pull request as the release version.
- Every released version must have a Git tag named `vMAJOR.MINOR.PATCH`.
- Do not publish artifacts that contain real credentials, client ids, endpoints,
  keystores, or other environment-specific values.
- Keep source, documentation, examples, generated property references, and
  compatibility notes aligned before creating the tag.

## Version Sources

The framework artifact version is defined in `gradle.properties`:

```properties
frameworkVersion=0.3.0
```

The root build applies that version to every framework artifact. The supported
Spring Boot version is defined in the same file:

```properties
springBootVersion=4.1.0
```

When preparing a release, update every consumer-facing version example that
mentions the framework version. At minimum, check:

- `README.md`
- `docs/**/*.md`
- `examples/*/build.gradle`
- module `README.md` files

## Release Checklist

1. Create a release branch:

```bash
git checkout -b release/v0.3.0
```

2. Confirm the target version and update `gradle.properties` if needed:

```properties
frameworkVersion=0.3.0
```

3. Refresh generated property references after configuration changes:

```bash
./gradlew generatePropertyReferences
./gradlew generateModuleCompatibilityContracts
```

Review module compatibility contract diffs. Do not accept a generated change
without confirming that it belongs to a supported public surface.

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

8. Review the final diff:

```bash
git status --short
git diff
```

9. Commit the release changes with a Conventional Commit:

```bash
git add .
git commit -m "chore(release): prepare v0.3.0"
```

10. Create the release tag:

```bash
git tag -a v0.3.0 -m "Release v0.3.0"
```

11. Push the branch and tag when the release is approved:

```bash
git push origin release/v0.3.0
git push origin v0.3.0
```

## Publishing Locally

Use local publication to verify examples against published artifacts instead of
project dependencies:

```bash
./gradlew publishLocalArtifacts
./gradlew consumerSmoke
```

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

./gradlew publish
```

Equivalent Gradle properties:

```bash
./gradlew publish \
  -PprivateMavenUrl=https://maven.example.com/releases \
  -PprivateMavenUsername=user \
  -PprivateMavenPassword=secret
```

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
in release notes and compatibility documentation.

## Post-Release

After the release is published:

- verify the tag points to the intended commit;
- verify the private registry contains every expected artifact;
- verify a clean consumer can resolve the released version;
- create the next development version only when the repository workflow needs
  snapshot or pre-release versions.

Useful checks:

```bash
git show --stat v0.3.0
./gradlew publishLocalArtifacts consumerSmoke
```
