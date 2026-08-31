# Quality Pipeline

GitHub Actions is the authoritative merge gate for pull requests and the
protected `main` branch. The private Service Framework Quality Platform may run
additional analysis, but it cannot replace the required repository checks.

## Pull Request CI Contract

The GitHub Actions contract is versioned in
`.ci/pull-request-contract.json`. It freezes the workflow path, supported
events, minimum permissions, security boundaries, Java version, and these
required status-check names:

- `Pull Request / Policy`
- `Pull Request / Quality`
- `Pull Request / Security`

`Pull Request / Policy` validates the CI contract, the pull request title, and
every non-merge commit subject using Conventional Commits. Automatic Git merge
commits are recognized by their multiple parents. Commit and title descriptions
must still be reviewed for English wording.

`Pull Request / Quality` runs `clean pullRequestGate` after `Policy` succeeds.
It covers compilation, tests, coverage, formatting, documentation, API and
binary compatibility, published consumers, OpenAPI generation, AOT, SBOM, and
supply-chain validation without remote publication.

OpenAPI documentation validation discovers published Gradle commands, runs the
complete documentation contract, and writes
`build/reports/smbtech-openapi/documentation-check.txt` as Quality evidence.

`Pull Request / Security` runs in parallel with `Quality` after `Policy`
succeeds. It generates the aggregate CycloneDX SBOM, scans the complete Git
history with Gitleaks, and scans dependencies and repository configuration with
Trivy. The gate rejects secrets, critical vulnerabilities, fixable high
vulnerabilities, and high or critical misconfigurations. SARIF evidence is
redacted and retained as a workflow artifact for seven days.

Both executable gates publish evidence even when they fail. `Quality` uploads
JUnit XML, JaCoCo XML, and the aggregate CycloneDX SBOM. `Security` uploads
redacted SARIF, the redacted execution summary, and the exact SBOM scanned by
Trivy. The artifacts are separate, retained for seven days, and never contain
compiled binaries or local Maven repositories. Each job also writes a compact
result to the GitHub Actions step summary.

The binary compatibility build extracts the released source tag but verifies
its build dependencies with the current reviewed
`gradle/verification-metadata.xml`. Historical verification metadata is not
relaxed or trusted implicitly when CI reconstructs the baseline from scratch.

Pull request workflows must use `pull_request`, support merge queues through
`merge_group`, run with read-only repository contents, and work for forks and
Dependabot. They must never use `pull_request_target`, repository secrets,
remote artifact publication, signing credentials, or the protected `release`
environment.

## Main Branch Protection

The `main` branch is protected in GitHub with the policy versioned under
`branchProtection` in `.ci/pull-request-contract.json`:

- branches must be current with `main` before merging;
- `Pull Request / Policy`, `Pull Request / Quality`, and
  `Pull Request / Security` are required from the GitHub Actions App;
- every change requires a pull request and one approving review;
- approvals are dismissed after new commits, and the last pusher cannot provide
  the final approval;
- administrators follow the same policy and have no configured bypass;
- review conversations must be resolved;
- merge commits, force pushes, and branch deletion are rejected.

Squash merge or rebase merge must be used to preserve linear history. Direct
pushes to `main` are not part of the supported workflow. Repository settings
disable merge commits, enable auto-merge, and delete merged head branches.

Validate the contract locally with:

```bash
./gradlew pullRequestCiCompatibilityCheck
```

Maintainers can compare the live GitHub settings with the versioned contract:

```bash
./gradlew verifyPullRequestBranchProtection
```

The live verification is intentionally separate from `pullRequestGate`: fork
and Dependabot jobs remain read-only and do not need repository administration
access.

Run the complete non-publishing gate with the same command reserved for pull
request automation:

```bash
./gradlew clean pullRequestGate \
  --no-daemon \
  --stacktrace \
  --console=plain
```

`pullRequestGate` delegates to `releaseGate` and contract validation. It covers
the existing compatibility, AOT, documentation, test, coverage, OpenAPI, SBOM,
and supply-chain controls without publishing to a remote repository.

Run the security gate locally after installing Gitleaks and Trivy:

```bash
./gradlew dependencyTrackSbom --no-daemon --stacktrace --console=plain
SECURITY_EXECUTION_PROFILE=pull-request .ci/security/scan.sh all
```

Redacted reports are written to `build/reports/security`.

Pull request evidence is available from the workflow run under **Artifacts**:

- `pull-request-quality-<pr-or-run>-<attempt>` contains test, coverage, OpenAPI documentation, and SBOM evidence.
- `pull-request-security-<pr-or-run>-<attempt>` contains redacted scan evidence and its SBOM.

The three GitHub Actions checks are implemented and required by branch
protection. A pull request cannot merge when any check is missing, pending,
cancelled, or unsuccessful.

## Repository entry points

`.github/workflows/pull-request.yml` owns the merge gate. `pipeline.jenkins`
delegates optional additional analysis to the trusted Jenkins Shared Library,
and `.ci/quality.yml` declares its catalog identity and `standard` profile.
Neither Jenkins entry point contains credentials, deployment steps, publication
steps, scanner thresholds, or custom pipeline commands.

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

Run a focused local evidence build:

```bash
./gradlew check jacocoTestReport cyclonedxBom validateCycloneDxSbom \
  --no-daemon \
  --console=plain
```

The main evidence is written to:

| Evidence | Path |
|---|---|
| Pull request CI contract | `.ci/pull-request-contract.json` |
| Aggregate CycloneDX JSON | `build/reports/bom.json` |
| Aggregate CycloneDX XML | `build/reports/bom.xml` |
| JUnit XML | `**/build/test-results/test/*.xml` |
| JaCoCo XML | `**/build/reports/jacoco/test/jacocoTestReport.xml` |

The private quality platform can perform additional migration acceptance:

```bash
make validate-framework-migration \
  APP=/absolute/path/to/spring-boot-service-framework
```

The private platform may additionally run SonarQube and submit the SBOM to
Dependency-Track. Pull request verification never publishes Maven artifacts and
never deploys the application.

## Workflow ownership

Pull request quality is owned exclusively by the versioned workflow and
contract above, without publication or release credentials.

GitHub Actions owns three isolated responsibilities:

- `.github/workflows/pull-request.yml` provides the read-only pull request checks.
- `.github/workflows/security-monitor.yml` re-evaluates unchanged source after
  vulnerability database updates.
- `.github/workflows/release.yml` verifies signed version tags and publishes
  artifacts from the protected `release` environment.

`.github/workflows/pull-request.yml` is reserved for the three contracted,
non-publishing checks. `Policy`, `Quality`, and `Security` run with read-only
repository permissions.

Publication and signing credentials are never available to pull request jobs or
private-platform quality builds.

## Rollout And Rollback

The rollout is complete after a pull request proves all three required checks,
both evidence artifacts, independent approval, and a squash or rebase merge.
The same pull request should also confirm that superseded runs are cancelled and
that the branch must be updated when `main` advances.

Required check names are compatibility-sensitive. To rename or replace a check:

1. add and validate the replacement check without removing the current one;
2. update branch protection to require the replacement;
3. merge the contract and documentation update;
4. remove the obsolete check only after no open pull request depends on it.

For an emergency rollback, remove the affected required check from branch
protection before disabling its workflow job. Do not grant secrets, write
permissions, direct pushes, or force pushes as a workaround.
