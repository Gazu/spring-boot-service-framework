# OpenAPI Contract Pilot

The pilot proves that one validated OpenAPI contract can be staged, published,
and resolved from the private Maven registry without publishing unrelated
contracts or framework artifacts. It deploys generated contract libraries; it
does not deploy a service application.

## Workflow

Run `.github/workflows/openapi-contract-pilot.yml` manually from `main`. Its
inputs are:

- `contract`: one configured or discovered project-relative OpenAPI path;
- `expected_version`: the exact `info.version` expected from that contract;
- `publish`: `false` stages and verifies locally, while `true` also requests
  protected remote publication.

The workflow always executes the Phase 6 contract-testing compatibility gate,
the published-artifact consumer, and the repository-wide OpenAPI compatibility
gate. Contract validation cannot be disabled. It then calls
`generateOpenApiPilotManifest`, which publishes only the selected contract to a
dedicated local repository and records the source commit, contract hash,
coordinates, payload hashes, checksums, and embedded snapshot hash.

An isolated Gradle build under `.ci/openapi-pilot-consumer` resolves the staged
model, API, and client through Maven metadata. It verifies the shared model
dependency, absence of an OpenFeign runtime leak, exact artifact hashes, and all
required POM, module metadata, sources, and SHA-256 files.

## Protected Publication

Remote publication runs only when `publish` is true, the workflow was dispatched
from `main`, and the `release` GitHub Environment approves the job. That
environment supplies:

```text
PRIVATE_MAVEN_URL
PRIVATE_MAVEN_USERNAME
PRIVATE_MAVEN_PASSWORD
```

Secrets are mapped only into remote preflight, publication, and verification
steps. They are not written to command arguments, manifests, receipts, or
uploaded evidence.

Before upload, `checkPilotPublicationState` classifies the remote coordinates:

| State | Behavior |
|---|---|
| `absent` | Publish the selected contract, then verify every deployed file. |
| `identical` | Skip upload and verify the existing immutable payload. |
| `partial` | Fail and quarantine the version for recovery. |
| `conflict` | Fail because existing bytes differ from the validated payload. |

The remote task is `smbtechOpenApiPublishContract`; aggregate framework or
multi-contract publication tasks are never used by the pilot. A successful run
uploads a deployment receipt tied to the validated source commit and payload
hashes.

## Local Validation

Run the deterministic pilot fixture and policy checks without credentials:

```bash
./gradlew pilotDeploymentCompatibilityCheck
```

To stage another contract locally, use a dedicated empty repository directory:

```bash
./gradlew generateOpenApiPilotManifest \
  -PopenApiContract=docs/openapi/warehouse-inventory-catalog.yaml \
  -PopenApiPilotVersion=1.0.0 \
  -PopenApiRepositoryDirectory=build/pilot/manual/repository \
  -PopenApiPilotManifest=build/pilot/manual/publication-manifest.json

./gradlew -p .ci/openapi-pilot-consumer verifyPilotPublication \
  -PpilotManifest="$PWD/build/pilot/manual/publication-manifest.json" \
  -PpilotRepositoryUrl="file://$PWD/build/pilot/manual/repository"
```

Actual registry credentials, environment approval, immutability enforcement,
and remote availability still require an authorized GitHub Actions run. Local
success is not evidence of remote publication.

See [contract validation](contract-validation.md), the [example
procedure](example-pilot-process.md), and [recovery](rollback-procedure.md).
