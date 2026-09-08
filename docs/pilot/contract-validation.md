# Pilot Contract Validation

Pilot publication depends on the completed Phase 6 behavior rather than
reimplementing contract tests inside the deployment workflow.

The validation job runs:

```bash
./gradlew clean \
  contractTestingCompatibilityCheck \
  openApiContractConsumerSmoke \
  smbtechOpenApiCompatibilityCheck \
  pilotDeploymentCompatibilityCheck
```

This validates the contract-testing module, a real generated Spring MVC API,
both generated clients, published Maven metadata, OpenAPI compatibility, and the
pilot workflow contract. The deployment job has a hard `needs: validate`
dependency, so no remote step is reachable after a failed or skipped gate.

After the repository gates pass, staging validates the exact selected contract:

1. The selector must resolve to a configured or discovered repository file.
2. The requested version must equal the generated artifact version derived from
   `info.version`.
3. Exactly one model, API, and client coordinate may exist in the staging
   repository.
4. API and client metadata must reference that model coordinate exactly once.
5. Binary JARs must share the same embedded contract snapshot and must not
   duplicate model classes.
6. Every binary, sources JAR, POM, Gradle module file, and SHA-256 file is bound
   to the publication manifest.
7. An isolated consumer must resolve all three coordinates before remote
   publication and again from the remote repository afterward.

The normal changed-contract workflow uses the same staging manifest and remote
verification. This prevents the automatic and manual publication paths from
drifting around Phase 6 validation.
