# Quality Pipeline

This repository is managed by the private Service Framework Quality Platform.
Jenkins is the authoritative quality system for pull requests and the main
branch.

## Repository entry points

`pipeline.jenkins` delegates the complete implementation to the trusted Jenkins
Shared Library. `.ci/quality.yml` declares the catalog identity and the
`standard` quality profile. Neither file contains credentials, deployment
steps, publication steps, scanner thresholds, or custom pipeline commands.

## Quality smoke module

`examples/quality-pilot` is an executable Spring Boot application used to prove
that the quality contract covers application code in addition to framework
libraries. Its `GET /api/pilot` endpoint returns:

```json
{
  "status": "ok"
}
```

Run its tests:

```bash
./gradlew :examples:quality-pilot:test \
  :examples:quality-pilot:jacocoTestReport
```

## Local quality evidence

Run the same local Gradle block used by Jenkins:

```bash
./gradlew check jacocoTestReport cyclonedxBom validateCycloneDxSbom \
  --no-daemon \
  --console=plain
```

The main evidence is written to:

| Evidence | Path |
|---|---|
| Gradle contract | `build/reports/quality/gradle-contract.json` |
| Aggregate CycloneDX JSON | `build/reports/bom.json` |
| Aggregate CycloneDX XML | `build/reports/bom.xml` |
| JUnit XML | `**/build/test-results/test/*.xml` |
| JaCoCo XML | `**/build/reports/jacoco/test/jacocoTestReport.xml` |

The quality platform performs the complete migration acceptance:

```bash
make validate-framework-migration \
  APP=/absolute/path/to/spring-boot-service-framework
```

Jenkins additionally runs SonarQube, Trivy, Gitleaks, and Dependency-Track.
Quality verification never publishes Maven artifacts and never deploys the
application.

## Workflow ownership

`.github/workflows/release-gate.yml` was removed during migration. Branch
quality must not be reintroduced outside the trusted Jenkins Shared Library.

GitHub Actions retains two isolated responsibilities:

- `.github/workflows/security-monitor.yml` re-evaluates unchanged source after
  vulnerability database updates.
- `.github/workflows/release.yml` verifies signed version tags and publishes
  artifacts from the protected `release` environment.

Publication and signing credentials are never available to Jenkins quality
builds.
