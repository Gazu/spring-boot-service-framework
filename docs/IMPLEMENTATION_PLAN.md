# OpenAPI Contract Artifact Implementation Plan

## Objective

Maintain multiple OpenAPI contracts in one Git repository and generate three
independently consumable Java artifacts for each contract: model, server API,
and client. Publish changed contracts independently to the existing GitHub
Packages Maven repository, using each contract's `info.version` rather than the
framework release version.

This document records the agreed phases 1–7. It is a plan, not a certification
of the current implementation. Existing work, including the partial Phase 5
draft, must be preserved and checked against these criteria before closing a
checkbox. Creating this document does not authorize further implementation,
workflow execution, or remote publication.

## Architecture Decisions

### Runtime And Artifact Boundaries

Keep Java 21 and Spring Boot 4.1. The Java release is included in artifact IDs
as `-jdk<release>`; this plan does not introduce another runtime baseline.

| Boundary | Maven coordinate | Contents |
|---|---|---|
| Model | `<group>:<base>-jdk21-model:<info.version>` | Shared transport models. |
| API | `<group>:<base>-jdk21-api:<info.version>` | `ApiUtil`, `*Api`, `*ApiController`, and `*ApiDelegate`. |
| Client | `<group>:<base>-jdk21-client:<info.version>` | Spring HTTP Interface and OpenFeign interfaces in one JAR. |

These are library JARs, not executable Spring Boot archives or RAR files.
Sources JARs, POMs, checksums, and Gradle module metadata accompany the three
binary artifacts; they do not introduce another artifact boundary.

Both API and client declare the model artifact transitively at the same group
and version. Neither contains duplicate model classes. The client contains no
generated runtime wrapper or endpoint configuration. OpenFeign is a
compile-time generation dependency only; applications using that variant add
`spring-cloud-starter-openfeign` themselves. Existing framework dependencies
remain unchanged unless required by this contract.

### Identity And Versioning

- `info.version` is the Maven version for all three artifacts of one contract.
  A configured version override must be absent or equal to it.
- `<base>` defaults to normalized `info.title`, not the folder name. The
  existing `artifactBaseName` and `groupId` overrides remain supported.
- `basePackage` is an unversioned root. Packages are
  `<basePackage>.v<major>.model`, `.api`, `.client.httpinterface`, and
  `.client.openfeign`.
- `1.0.0` produces `.v1`; `2.1.0` produces `.v2`. Minor and patch releases do
  not change the package namespace.
- Both client variants use matching simple interface names in different
  packages. OpenAPI tags define API families; `operationId` is mandatory and
  unique. Screenshot naming is a structural reference, not a requirement to
  rename the existing generated `*Api` interfaces to `*Client`.
- Published coordinates and committed contract baselines are immutable.
  Breaking changes follow the existing SemVer and baseline policy.

### Repository And Plugin Ownership

The existing Gradle OpenAPI plugin remains responsible for discovery,
validation, generation, packaging, and Maven publication. No service-specific
Gradle subproject is required just to add a contract.

Direct `.yaml`, `.yml`, and `.json` files under conventional `src/main/openapi`,
`openapi`, or `swagger` directories are discovery candidates throughout the
repository. Examples include:

```text
ms-kyc-profile/swagger/openapi.yaml
ms-payments/openapi/payments.yaml
docs/openapi/warehouse-inventory-catalog.yaml
```

Exclude `.git`, `.gradle`, and `build`. An explicit DSL registration takes
precedence for the same canonical file and supports non-conventional paths.
Generated sources and intermediate files stay under `build`.

### Publication Boundary

Preserve the existing aggregate publication tasks. Add public single-contract
entry points using `-PopenApiContract=<project-relative-path>`:

- `smbtechOpenApiPublishContractToLocalRepository` for local Maven publication.
- `smbtechOpenApiPublishContract` for remote Maven publication.

Each entry point generates and publishes only the selected contract's enabled
artifacts. All three artifact flags remain enabled by default; existing opt-out
flags retain their semantics.

Independence refers to generation/publication dependencies and Maven versions,
not an exemption from repository validation. Configuration and coordinate
validation remain repository-wide. CI runs the compatibility gate once before
the publication matrix; publication tasks do not silently absorb that gate.

GitHub Actions detects changed contract files on `main` and publishes each
selected contract in a separate matrix job. Manual dispatch selects one
contract. Reuse the existing `release` environment and publisher secrets.
This workflow is separate from the signed framework release/tag workflow.

## Assumptions And Constraints

- Contracts use supported OpenAPI 3.0 or 3.1 documents with valid titles,
  versions, operation IDs, and model definitions.
- The Gradle plugin is applied once at the intended contract-repository root;
  contracts are files within that repository, not remote downloads.
- The agreed automatic CI scope is changed conventional contract documents.
  Dependency-aware detection of external `$ref` files and automatic republishing
  after generator/template-only changes are not part of these five phases.
- GitHub Packages, publisher authorization, and the `release` environment
  already exist. Their actual availability still requires a controlled
  integration test; local Maven success does not prove remote authorization.
- `PRIVATE_MAVEN_URL`, `PRIVATE_MAVEN_USERNAME`, and `PRIVATE_MAVEN_PASSWORD`
  are supplied securely by the environment. No secrets are committed.
- Repository/CI policy must prevent replacement of released coordinates. This
  must be verified rather than inferred from a successful first publication.
- Maven uploads of three artifacts are not atomic. A partially published
  release must be reported as failed and must not overwrite successful uploads
  during recovery.
- Business implementations of delegates, runtime client URLs, credentials,
  and application-specific client configuration are outside generation scope.

## Affected Components

Paths below are relative to the repository root. Phase file lists use these
component locations to avoid repeating long package prefixes.

| Component | Location |
|---|---|
| Plugin Java implementation | `build-logic/openapi-generator-plugin/src/main/java/com/smbtech/serviceframework/gradle/openapi/` |
| Plugin tests | `build-logic/openapi-generator-plugin/src/test/java/com/smbtech/serviceframework/gradle/openapi/` |
| Plugin toolchain resources | `build-logic/openapi-generator-plugin/src/main/resources/com/smbtech/serviceframework/gradle/openapi/` |
| Generator templates | `build-logic/openapi-templates/src/main/resources/smbtech-openapi/` |
| Scaffolding integration | `spring-boot-service-framework-project-generator/` |
| Contract examples and baselines | `docs/openapi/`, `docs/openapi-baselines/` |
| CI workflows | `.github/workflows/` |
| Compatibility evidence | `gradle/module-compatibility.gradle`, `gradle/compatibility/contracts/openApiGradlePlugin.txt` |
| Canonical documentation | `docs/adr/0002-openapi-artifact-contract.md`, `docs/openapi/` |

## Phase 1 — Freeze The Artifact Contract

### Tasks

- [ ] Centralize artifact names, Java release, package boundaries, and version
  derivation in one artifact-contract model.
- [ ] Enforce `info.version` as the common Maven version and derive the Java
  package major from that value.
- [ ] Validate configured identity/package overrides and reject incompatible
  values or coordinate collisions.
- [ ] Define ownership of models, API types, and client interfaces; publish the
  matching model dependency from API and client metadata.
- [ ] Align metadata and scaffolding consumers with the new coordinates and
  versioned packages without unrelated public API renames.
- [ ] Record the accepted artifact contract and compatibility expectations.

### Affected Files

Plugin: `OpenApiArtifactContract.java`, `OpenApiContractReader.java`,
`SmbtechOpenApiValidateSpecsTask.java`, `OpenApiGenerationConfigurer.java`,
`SmbtechOpenApiMigrationReportTask.java`, and
`SmbtechOpenApiConsumerCompatibilityTask.java`.

Scaffolding: `ContractDescriptorLoader.java`, `ProjectGenerationRequest.java`,
`ProjectGeneratorCli.java`, and their tests. Documentation:
`docs/adr/0002-openapi-artifact-contract.md`, `docs/openapi/generation.md`, and
`docs/openapi/versioning.md`.

### Acceptance Criteria

- The default contract produces exactly the three agreed binary coordinates.
- `1.0.0` and `2.1.0` map to `.v1` and `.v2`; divergent overrides fail clearly.
- API and client resolve the same model coordinate transitively, with no model
  classes duplicated in their JARs.
- Scaffolding can consume the resulting API metadata and delegate interfaces.

### Required Tests

- [ ] Test artifact ID construction and major-package derivation.
- [ ] Test invalid versions, divergent overrides, and coordinate collisions.
- [ ] Inspect JAR contents, POMs, and Gradle module dependency metadata.
- [ ] Run scaffolding regression tests using generated API artifacts.

## Phase 2 — Extend The OpenAPI Plugin

### Tasks

- [ ] Expose and derive the reserved HTTP Interface and OpenFeign package
  inputs while preserving the existing DSL compatibility boundary.
- [ ] Wire Java 21/Spring Boot 4.1 generation and pinned toolchain metadata.
- [ ] Add the OpenFeign compile-only generation dependency and annotation
  template without publishing its runtime or starter transitively.
- [ ] Configure server generation for `ApiUtil`, API interfaces, controllers,
  and delegate interfaces, reusing the model package.
- [ ] Validate incompatible package configuration and artifact switches before
  generating or publishing artifacts.
- [ ] Update plugin compatibility evidence and the relevant reference entries.

### Affected Files

Plugin: `SmbtechOpenApiSpec.java`, `SmbtechOpenApiBuildLogicCheckTask.java`,
`OpenApiGenerationConfigurer.java`, `OpenApiToolchainVersions.java`, and
`SmbtechOpenApiGenerateTask.java`. Build inputs:
`build-logic/openapi-generator-plugin/build.gradle`, `gradle.properties`, and
the plugin's `openapi-toolchain.properties` resource. Templates:
`client-interface-annotation.mustache` and
`openfeign-client-interface-annotation.mustache`.

### Acceptance Criteria

- Plugin inputs consistently derive the agreed package structure.
- Generated API sources compile against the existing Java/Spring baseline.
- OpenFeign is available to generate/compile its interfaces but absent from
  published transitive dependencies.
- Models cannot be disabled while API or client generation remains enabled.

### Required Tests

- [ ] Test DSL defaults, package validation, and artifact flag combinations.
- [ ] Test template/toolchain resource loading and generator configuration.
- [ ] Compile server API and delegate output against shared models.
- [ ] Verify published dependency metadata excludes OpenFeign runtime/starter.

## Phase 3 — Dual Client Generation

### Tasks

- [ ] Run separate HTTP Interface and OpenFeign generation passes against the
  same contract and shared model package.
- [ ] Merge only the intended interface sources into one client binary JAR
  and its sources attachment, using isolated intermediate output directories.
- [ ] Keep matching interface families and operation signatures across both
  client packages, including multi-tag and untagged contracts.
- [ ] Preserve HTTP Interface annotations and generate `@FeignClient`
  interfaces with stable, non-colliding context IDs.
- [ ] Exclude generated wrappers, endpoint configuration, and duplicate models.
- [ ] Extend consumer and reproducibility checks for both client variants.

### Affected Files

Plugin: `SmbtechOpenApiGenerateTask.java`, `OpenApiGenerationConfigurer.java`,
`SmbtechOpenApiConsumerCompatibilityTask.java`,
`SmbtechOpenApiReproducibilityTask.java`, and
`SmbtechOpenApiGeneratorPluginTest.java`. Both client annotation templates and
`docs/openapi/generation.md` are also affected.

### Acceptance Criteria

- One `-jdk21-client` JAR contains both reserved interface packages.
- Both variants reference the same model classes and equivalent operations.
- An HTTP Interface consumer works without OpenFeign dependencies; an
  OpenFeign consumer supplies its starter explicitly.
- Repeated clean generation produces reproducible artifacts.

### Required Tests

- [ ] Compile and inspect binary/source JARs for both interface families.
- [ ] Exercise multiple tags, untagged operations, parameters, request bodies,
  and response models; reject normalized family-name collisions.
- [ ] Compile an HTTP Interface consumer without OpenFeign and an OpenFeign
  consumer with its explicitly declared dependencies.
- [ ] Check for duplicate models, unwanted wrappers, and unstable output hashes.

## Phase 4 — Automatic Folder Discovery

### Tasks

- [ ] Discover direct YAML, YML, and JSON candidates in the conventional
  directories, excluding generated and repository-internal directories.
- [ ] Promote valid candidates to full specs participating in generation,
  compatibility, assembly, and publication.
- [ ] Preserve explicit overrides for the same canonical file and reject
  duplicate explicit registrations of one input.
- [ ] Assign deterministic internal names without making generated task names
  part of the public API.
- [ ] Keep malformed candidates in validation and report their paths clearly.
- [ ] Reject coordinate/output-file collisions and isolate compatibility
  reports for multiple versions of a contract.
- [ ] Ensure adding a conventional contract invalidates stale discovery state.

### Affected Files

Plugin: `OpenApiSpecDiscovery.java`, `SmbtechOpenApiGeneratorPlugin.java`,
`SmbtechOpenApiValidateSpecsTask.java`, `OpenApiGenerationConfigurer.java`,
`OpenApiCompatibilityConfigurer.java`, `SmbtechOpenApiBreakingChangeTask.java`,
`SmbtechOpenApiMockContractTask.java`, and
`SmbtechOpenApiGeneratorPluginTest.java`. References:
`docs/openapi/validation.md` and `docs/openapi/plugin-reference.md`.

### Acceptance Criteria

- Adding a valid conventional contract requires no per-contract DSL entry.
- Explicit configuration of the same file produces only one set of artifacts.
- Multiple folders can use `openapi.yaml` without internal task collisions.
- Invalid contracts and conflicting outputs fail before generation/publication.
- Different active versions retain separate artifacts and compatibility reports.

### Required Tests

- [ ] Cover all supported extensions, conventional locations, excluded
  directories, and non-discovered nested files.
- [ ] Verify explicit precedence and duplicate canonical-path rejection.
- [ ] Generate multiple contracts and versions with the same input filename.
- [ ] Test malformed inputs, coordinate collisions, and physical JAR-name
  collisions across different groups.
- [ ] Test discovery after adding a contract with configuration cache enabled.

## Phase 5 — Independent Publication Per Contract

### Tasks

- [ ] Add single-contract local and remote publication entry points using
  `-PopenApiContract=<path>`, while preserving aggregate task behavior.
- [ ] Resolve selectors against canonical configured inputs, retain explicit
  overrides, and fail clearly for absent, unknown, or out-of-repository
  selections.
- [ ] Wire selected publication dependencies only to that contract's enabled
  model, API, and client artifacts.
- [ ] Resolve the remote URL from DSL configuration or
  `openApiRepositoryUrl`/`OPENAPI_REPOSITORY_URL`; retain existing credential
  property/environment precedence.
- [ ] Detect added, modified, or renamed conventional contracts from the Git
  push range; ignore deleted files and handle an empty change set.
- [ ] Support manual dispatch for one selected contract and align its accepted
  paths with the public selector contract, including explicit registrations.
- [ ] Run compatibility before publication, then use one matrix entry per
  contract with `fail-fast: false` and no cancellation of active publication.
- [ ] Restrict publication to the approved release branch/environment and map
  the existing publisher secrets without exposing them in logs or arguments.
- [ ] Verify registry immutability and report existing coordinates or partial
  publication failures without overwriting released artifacts.
- [ ] Update public task compatibility evidence and publication references.

### Affected Files

Plugin: `OpenApiGenerationConfigurer.java`, `SmbtechOpenApiExtension.java`, and
`SmbtechOpenApiGeneratorPluginTest.java`. CI:
`.github/workflows/publish-openapi-contracts.yml`. Compatibility/documentation:
`gradle/module-compatibility.gradle`,
`gradle/compatibility/contracts/openApiGradlePlugin.txt`,
`gradle/documentation-checks.gradle`, `docs/openapi/publishing.md`,
`docs/openapi/plugin-reference.md`, and `CHANGELOG.md`.

The existing `.github/workflows/release.yml` and
`.github/workflows/publish-gradle-maven.yml` retain the independent framework
release lifecycle; contract releases must not depend on `frameworkVersion` tags.

### Acceptance Criteria

- Selecting contract A generates/publishes A's three default artifacts, POMs,
  source attachments, and module metadata without generating/publishing B.
- Selecting a missing contract fails; it never falls back to publishing all.
- A and B retain independent `info.version` values and release jobs.
- CI publishes only detected/explicitly selected contracts after the gate;
  no-change and deletion-only pushes perform no publication.
- Manual dispatch and Gradle selection have a documented, consistent path
  policy. Existing aggregate tasks continue to publish all enabled contracts.
- A failed matrix entry does not cancel another entry. Credentials stay secret,
  and released coordinates are not replaced on retries.
- Real GitHub Packages publication is accepted only after an authorized
  integration run, not on the basis of a file-repository test alone.

### Required Tests

- [ ] Use Gradle TestKit with two contracts to prove selected task-graph and
  local publication isolation for all three default artifact boundaries.
- [ ] Test absent/unknown selectors, canonical path aliases, explicit
  registrations, paths escaping the repository, and per-contract artifact flags.
- [ ] Test remote task wiring against an isolated file Maven repository, URL
  provider precedence, and missing/malformed remote configuration.
- [ ] Test change detection for additions, modifications, renames, deletions,
  initial push, multiple contracts, excluded paths, empty results, and manual
  selection; validate matrix JSON and workflow/shell syntax.
- [ ] Run plugin, public task compatibility, formatting, and documentation
  regression checks; preserve aggregate publication behavior.
- [ ] In an authorized integration environment, verify published coordinates,
  resolve API/client plus transitive models from a clean consumer, and exercise
  existing-version rejection and publication failure reporting.

## Phase 6 — Contract Testing

### Tasks

- [x] Add a standalone application consumer that resolves the locally published
  generated API and client artifacts, their transitive model, and the contract
  testing module exclusively through Maven metadata.
- [x] Implement the generated server delegate in a real Spring MVC runtime and
  validate every published operation with `verifyAll(...)` against the embedded
  versioned contract snapshot.
- [x] Invoke that runtime through both generated Spring HTTP Interface and
  OpenFeign clients, with OpenFeign supplied explicitly by the consumer.
- [x] Inspect resolved dependency graphs, binary JAR contents, and embedded
  snapshots to enforce the shared model coordinate and prevent duplicate
  models, generated wrappers, divergent contracts, or OpenFeign leakage.
- [x] Cover successful status, media type, and schema validation together with
  deterministic failure cases for invalid payloads and undeclared statuses.
- [x] Keep optional REST client integrations absent from the minimal consumer
  and verify that base auto-configuration still starts without those classes.
- [x] Wire the published-artifact consumer into module compatibility,
  `consumerSmoke`, `releaseGate`, pull-request CI, and contract publication CI.

### Affected Files

Consumer: `examples/openapi-contract-consumer/`. Contract testing:
`spring-boot-service-framework-openapi-contract-testing/` and its documentation.
REST client isolation:
`spring-boot-service-framework-starters/spring-boot-service-framework-starter-rest-client/`.
Lifecycle and compatibility: `gradle/lifecycle.gradle`,
`gradle/module-compatibility.gradle`,
`gradle/compatibility/contracts/contractTesting.txt`, and
`.github/workflows/publish-openapi-contracts.yml`.

### Acceptance Criteria

- A clean standalone build resolves generated and framework artifacts from the
  local Maven repositories without `project(...)` dependencies or included
  builds.
- `verifyAll(...)` executes every operation exposed by the generated Spring MVC
  API and validates the actual response status, media type, and JSON schema.
- Both generated client variants call the same runtime and deserialize the same
  shared generated model type.
- Artifact validation proves one matching model dependency, identical embedded
  contract snapshots, no duplicate models or wrappers, and no transitive
  OpenFeign runtime.
- Invalid response data and undeclared expected statuses produce stable contract
  violations; declared error responses remain covered by the module suite.
- Pull-request, release, and contract-publication workflows cannot reach their
  publication path unless the published-artifact contract consumer passes.

### Required Tests

- [x] Run the contract testing module `check` task, including declared error
  response and missing-operation coverage.
- [x] Run the REST client starter `check` task, including absent optional-class
  coverage.
- [x] Run `openApiContractConsumerSmoke` from a clean local publication.
- [x] Run module, OpenAPI, documentation, and pull-request CI compatibility
  checks.
- [x] Run the complete non-remote-publishing `pullRequestGate`.

## Phase 7 — Pilot And Deployment

### Tasks

- [x] Add a top-level, manually dispatched pilot workflow for one selected
  OpenAPI contract and require an exact expected `info.version`.
- [x] Make the completed Phase 6 contract validation and published-artifact
  consumer mandatory before pilot or automatic contract publication.
- [x] Stage exactly the selected model, API, and client in a dedicated local
  Maven repository and bind their payloads to the contract and source commit.
- [x] Verify POMs, Gradle metadata, sources, checksums, embedded snapshots,
  dependency topology, and clean consumer resolution before deployment.
- [x] Publish through the existing single-contract Gradle task from the
  protected `release` environment without exposing secrets to validation jobs.
- [x] Detect absent, identical, partial, and conflicting remote coordinates so
  retries cannot overwrite immutable artifacts.
- [x] Resolve and hash every deployed artifact after publication and retain a
  deployment receipt or failure-recovery evidence.
- [x] Document the operational pilot, external environment requirements, and
  safe recovery from a non-atomic three-artifact publication.

### Affected Files

Pilot build logic: `gradle/pilot-deployment.gradle`, `build.gradle`, and
`.ci/openapi-pilot-consumer/`. CI:
`.github/workflows/openapi-contract-pilot.yml` and
`.github/workflows/publish-openapi-contracts.yml`. Documentation:
`docs/pilot/`, `docs/openapi/publishing.md`, documentation indexes, and
`CHANGELOG.md`.

### Acceptance Criteria

- A staging-only pilot performs all Phase 6 and OpenAPI compatibility checks,
  publishes no remote artifact, and produces deterministic evidence for exactly
  three coordinates from one contract.
- A remote pilot is reachable only from `main`, after the validation job, with
  approval and credentials from the protected `release` environment.
- The requested version, generated coordinates, embedded contract, source
  contract hash, and source commit remain consistent through staging and remote
  verification.
- Automatic changed-contract publication and manual pilot deployment use the
  same manifest, immutability preflight, selected publication task, and clean
  remote consumer.
- Existing identical coordinates make retries idempotent; partial or differing
  coordinates fail without deletion or overwrite and produce recovery evidence.
- Framework release, aggregate OpenAPI publication, previous consumers, and
  pull-request security boundaries remain intact.

### Required Tests

- [x] Run `pilotDeploymentCompatibilityCheck`, including staged publication and
  isolated Maven consumer resolution.
- [x] Exercise missing version, mismatched version, non-isolated repository,
  missing remote payload, identical retry, partial publication, and conflicting
  payload failure behavior with local fixtures.
- [x] Run OpenAPI plugin, contract testing, documentation, CI compatibility,
  formatting, and code quality checks.
- [x] Run the complete non-remote-publishing `pullRequestGate`.
- [ ] In an authorized GitHub environment, run staging-only and publishing
  pilots, verify environment protection and credentials, inspect the deployment
  receipt, and exercise partial-publication recovery.

## Completion Gate

- [ ] Close phase checkboxes only with recorded acceptance evidence; reuse
  verified existing work instead of reimplementing it.
- [ ] Run the plugin's `check`, root `spotlessCheck`,
  `openApiGradlePluginCompatibilityCheck`, `smbtechOpenApiCompatibilityCheck`,
  and `documentationCheck` after implementation is authorized and complete.
- [ ] Confirm no unrelated edits, credentials, generated build output, or
  unrequested framework API changes are included.
- [ ] Report local verification separately from actual GitHub Packages
  publication, including any pending environment validation.
