# Repository Instructions For Coding Agents

## Start Here And Respect Scope

- Read `README.md`, `docs/IMPLEMENTATION_PLAN.md`, the affected module README,
  and relevant accepted ADRs under `docs/adr/` before changing code.
- Use `docs/code-conventions.md`, `docs/public-api-boundaries.md`, and the
  executable checks in `gradle/` as the detailed convention references. Feature
  documentation has one canonical owner in `docs/documentation-architecture.md`.
- Inspect the working tree first. Preserve existing edits, completed work,
  compatibility exceptions, and unrelated files. Keep changes focused; do not
  rename APIs, reorganize modules, or refactor unrelated code opportunistically.
- A review or documentation request does not authorize implementation. Report
  contradictions or missing requirements instead of inventing policy or silently
  treating a draft as established behavior.

## Phased Implementation

The implementation plan covers: 1) artifact contract, 2) plugin extensions,
3) dual client generation, 4) folder discovery, and 5) independent publication.

- Read the entire plan, including completed work, pending tasks, assumptions,
  affected files, acceptance criteria, and required tests before starting.
- Implement only the requested phase. Do not start subsequent phases or absorb
  unrelated improvements into the current phase.
- Preserve completed phases. An unchecked task is not proof that its code is
  absent: inspect implementation and verification evidence before changing it.
- Update only the requested phase's checklist, and only after its acceptance
  criteria, relevant tests, and required Gradle tasks have passed. Do not mark
  a phase complete from compilation alone or from the presence of a workflow.
- The plan identifies Phase 5 as a partial draft; recheck its evidence rather
  than assuming remote publication or immutability has been verified.
- Report blockers, failed commands, and missing credentials/environment access.
  Do not weaken tests or invent requirements to close a checkbox.
- Preserve the completed engine migration in ADR 0001. Do not restore the
  retired custom generator module or embedded generation script.

## Purpose And Module Organization

This is a reusable cross-cutting service framework, not an application domain.
The runtime baseline is Java 21 and Spring Boot 4.1; read exact versions from
`gradle.properties` and use the checked-in Gradle wrapper.

Framework module names have the prefix `spring-boot-service-framework-`:

| Module/location | Responsibility |
|---|---|
| `commons` | Shared neutral notifications, severity, and notifying exception contract. |
| `logging-core` | Structured logging values and ports. |
| `http-client-core` | HTTP policies, authentication/token contracts, ports, and downstream exceptions. |
| `mock-core` | Neutral mock definitions, catalogs, requests/responses, and ports. |
| `error-core` | Error definitions, exceptions, resolution, aggregation, and sanitization. |
| `actuator-core` | Neutral diagnostic values, ports, and deterministic aggregation. |
| `platform` | Gradle platform/Maven BOM; no runtime classes. |
| `spring-boot-service-framework-starters/` | Logging, REST client, mock, error-handling, and Actuator Spring Boot adaptations. |
| `openapi-contract-testing` | Spring MVC/MockMvc contract verification for application test scope. |
| `project-generator` | One-time Spring Initializr/OpenAPI application scaffolding. |
| `build-logic/` | Included build: internal conventions, public OpenAPI Gradle plugin, resource-only templates. |
| `examples/*-consumer` | Standalone consumers of published artifacts, not project dependencies. |

`examples/quality-pilot` is a root-managed application example, not a published
framework library. New managed modules must be registered in their owning
`settings.gradle` and have a build file.

## Dependency Direction And Public Boundaries

- Dependencies point inward: starters/adapters depend on core contracts; cores
  never depend on starters or their implementation packages. Keep business
  rules in consuming services and feature-specific utilities in their owner,
  not in a growing generic `commons` module.
- Core/commons production code is framework-neutral. Do not add Spring,
  Servlet, SLF4J/Logback, Jackson, Apache HttpClient, or Micrometer dependencies
  there. Neutral cross-core contracts and JSpecify metadata are not prohibited.
  Test/build-time dependencies follow their own module rules.
- Do not apply the core restriction to every non-starter: contract-testing and
  project-generator intentionally use Spring and other build/test tooling.
- Supported source packages use complete `domain`, `port`, or `api` segments.
  `adapter`, `application`, `autoconfigure`, `service`, `serialization`, and
  `internal` are implementation by default, with precedence over that rule.
  Preserve the explicit package/type exceptions in `docs/public-api-boundaries.md`.
- Java `public` does not make a class a supported API. Prefer package-private
  implementations exposed through supported ports/factories. Do not import or
  expose internal concrete classes as new consumer extension points.
- New technically public implementations require a concrete infrastructure
  reason and the reviewed allowlist/classification. The accidental-public and
  concrete-replaceable-bean baselines may shrink, not grow unchecked.

## Java, Clean Code, And New Abstractions

- Framework packages start with `com.smbtech.serviceframework`; starter packages
  use `.starter.<capability>`. Keep package paths and filenames aligned. Do not
  force legacy exceptions or generated application packages into a uniform tree.
- Use English identifiers, documentation, diagnostics, and tests. Use Java
  acronym casing: `OpenApi`, `Http`, `Jwt`, `OAuth2`. Do not introduce legacy
  `Requestor` or `TransactionalId` identifiers, `I`-prefixed interfaces, `Impl`
  suffixes, or empty `*Module` marker classes.
- Keep components focused on one capability; separate policy from transport,
  serialization, and bean assembly. Inject collaborators and extend existing
  ports, policies, customizers, or contributors before introducing abstractions.
- Add an abstraction for a demonstrated extension/reuse need, not speculative
  symmetry or a mandatory interface for every class. Use ordered composition
  rather than application-specific branches in shared implementations.
- `Default<ContractName>` identifies the canonical replaceable implementation;
  alternative implementations use meaningful technology/policy names.
- Prefer immutable data carriers and defensive copies. Structured metadata
  must freeze nested mutable values and handle invalid/cyclic inputs as the
  owning models do. Do not mandate records for mutable configuration binding.
- Supported public packages require documented `package-info.java` with
  JSpecify `@NullMarked`; nullable contract positions use `@Nullable`. Public
  API Javadocs describe behavior, invariants, and relevant lifecycle/security
  semantics. Explain required infrastructure visibility; avoid obvious comments
  and commented-out code.
- Follow `.editorconfig` and Spotless: UTF-8/LF, four-space Java/Gradle indent,
  Google Java Format AOSP, ordered imports, no wildcard imports. Do not manually
  edit generated sources under `build/`.

## Spring Boot Adaptation

- Put runtime integration in adapters and binding/assembly in `autoconfigure`.
  Use `*AutoConfiguration`, `*Properties`, and documented `smbtech.*` namespaces.
  Register entry points in
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Use constructor injection and `@Bean` method parameters. Keep bean methods
  small; parsing, mapping, validation, and runtime algorithms need focused owners.
- Condition optional integrations on classpath/properties/web type as needed.
  Supported replacement beans back off with `@ConditionalOnMissingBean` and
  return supported interfaces or reviewed external contracts.
- Preserve optional dependency isolation: OAuth2, security, metrics, and
  cross-starter integrations must not become mandatory transitives accidentally.
  Test absent optional classes as well as enabled integrations.
- Applications own endpoint exposure, `management.*`, and `SecurityFilterChain`.
  Actuator integration is passive, bounded, sanitized, and avoids default
  network probes/high-cardinality metrics. Do not add startup network calls or
  mutate application-wide configuration for a local framework concern.
- Preserve runtime hints, dynamic client registration, and AOT compatibility
  when changing proxy creation, reflection, resources, or bean registration.

## Gradle And Dependency Management

- Keep root `build.gradle` focused on orchestration. Reuse `gradle/*.gradle`
  and `build-logic/conventions`; module builds retain dependencies, descriptions,
  specialized checks/tasks, and publication names.
- Apply the existing `com.smbtech.service-framework.java-library`,
  `.spring-boot-starter`, or `.java-platform` convention as appropriate. Do not
  duplicate toolchain, JUnit, coverage, encoding, archive, signing, or repository
  configuration in ordinary modules. Build-time plugin/template modules have
  their own established build configuration.
- Centralize versions in `gradle.properties` and existing platform/toolchain
  owners. Keep reviewed Boot security overrides aligned; do not scatter new
  version literals or downgrade dependencies to bypass resolution failures.
- Export `api` dependencies only where the consumer contract requires them;
  keep implementation and optional compile-only boundaries intact.
- Consumers normally import `platform(...)` and omit individual framework
  versions. Do not export an `enforcedPlatform(...)` from a library. Generated
  OpenAPI artifacts have explicit contract versions and are outside the BOM.
- Preserve dependency verification metadata and review dependency additions,
  checksums, licenses, and published POM/module metadata. Do not disable
  verification or regenerate trust/compatibility files merely to silence errors.
- Follow existing typed task inputs/outputs and provider-based configuration;
  preserve deterministic generation and archives. Do not assume every task
  already supports configuration cache.

## Testing And Contract Verification

- Use JUnit Jupiter/Platform and existing JUnit or AssertJ style in the affected
  tests. Keep test packages/files aligned; executable classes end in `Test`
  (including `IntegrationTest`, `ContractTest`, `CompatibilityTest`), shared
  fixtures in `TestFixtures`, executable baselines in `Baseline`.
- Add regression tests for changed behavior, negative inputs, invariants, and
  relevant public/encapsulation/dependency boundaries. Preserve architecture
  checks; do not change allowlists or expected results just to obtain green CI.
- For auto-configuration, use existing context-runner patterns to cover
  defaults, disabling, optional-class absence, property binding, and user-bean
  replacement. Use MockMvc/integration tests for HTTP and security behavior.
- Use Gradle TestKit and temporary fixture projects for plugin/task behavior;
  inspect generated classes, JARs, POMs, module metadata, and task isolation.
- OpenAPI MVC verification belongs in application `testImplementation`. Use
  `verifyAll`/`throwIfInvalid` for full operation coverage where appropriate,
  including explicitly declared error statuses; retain behavior-focused tests.
- Standalone consumer smoke tests must resolve published local artifacts, not
  replace them with `project(...)`, so transitivity and metadata remain tested.
- Respect configured JaCoCo thresholds and Javadoc failures. Read
  `minimumLineCoverage` rather than weakening it for an individual change.

## OpenAPI And Scaffolding

- Follow ADRs 0001/0002 and `docs/openapi/`. OpenAPI Generator is the engine;
  Gradle plugin owns selection, validation, identity, metadata, and publication.
  Versioned Mustache customization belongs in `build-logic/openapi-templates`,
  not a parallel parser/renderer or handwritten edits to generated output.
- `info.version` is the artifact version; its major creates `.v<major>`.
  Artifact IDs are `<base>-jdk21-model`, `-jdk21-api`, and `-jdk21-client`.
  `<base>` defaults to normalized `info.title`; preserve supported overrides.
- Model types are generated once. API/client depend transitively on the same
  model version, with no duplicate model classes. API contains `ApiUtil`,
  `*Api`, `*ApiController`, and `*ApiDelegate`.
- One client JAR contains matching `.client.httpinterface` and
  `.client.openfeign` interface families. Consumers add OpenFeign themselves;
  do not publish its starter/runtime transitively or add generated wrappers.
- Preserve unique operation IDs and stable tag-derived API families. Validate
  spec/coordinate collisions before generation. Discovery uses direct YAML/YML/
  JSON files in conventional directories; explicit canonical registrations win.
- Versioned OpenAPI snapshots and released coordinates are immutable; add a new
  version instead of rewriting a released snapshot. These are distinct from
  reviewed framework compatibility inventories. Generator/template changes to
  public bytecode need contract versioning, not replacement of an existing release.
- Project scaffolding is a separate one-time bootstrap. Do not regenerate
  business code or infer domain entities/use cases from transport schemas.

## Logging And Exception Handling

- Use supported `StructuredLogger`/`StructuredEvent` APIs for structured logging;
  SLF4J, Logback, MDC, and servlet details stay in the starter. Preserve event
  fields, correlation cleanup/propagation, and sensitive-event behavior.
- Preserve async logging saturation, critical-event, ordering, and shutdown
  policies; consult `docs/logging/async-appender.md` before changing them.
- Never log tokens, passwords, private keys, authorization headers, or raw
  request/downstream payloads. Keep telemetry bounded and sanitized.
- Reuse owning-module exceptions and commons notifications; preserve causes.
  Stable service errors use `ErrorDefinition`/`ServiceException`; downstream
  failures retain inspectable HTTP-client exceptions unless deliberately
  translated at the application boundary.
- Public HTTP errors go through resolvers, exposure policies, and sanitizers,
  not raw `Exception.getMessage()`. Preserve snake-case notification JSON without
  changing the application's global `ObjectMapper`. Diagnostic reporter/metrics
  failures must not replace the intended error response.

## Publishing, CI, And Compatibility

- Framework artifacts share `frameworkVersion` and release through the signed
  `vMAJOR.MINOR.PATCH`/`releaseBuild=true` lifecycle in `docs/releasing.md`.
  Preserve the release manifest, complete POMs, reproducibility, signatures,
  checksums, SBOMs, and provenance constraints in `PROVENANCE.md`.
- Generated contract publication is a separate lifecycle using `info.version`.
  Preserve aggregate tasks and the plan's single-contract scope. Do not assume
  framework tag/signing rules apply to contract tasks, or that three Maven
  uploads are atomic. Local verification is not proof of remote publication.
- Use local `build/repository` publication for consumers. Never commit secrets
  or put passwords in command arguments. Do not remotely publish, dispatch
  release workflows, create tags, commit, or push without the user's request.
- GitHub Actions and `.ci/pull-request-contract.json` define the merge gate;
  Jenkins is supplementary. Preserve `Pull Request / Policy`,
  `Pull Request / Quality`, and `Pull Request / Security` check identities.
  PR/merge-group jobs stay read-only and fork-safe, without release credentials
  or remote publication; do not introduce `pull_request_target`.
- Keep actions pinned to full commit SHAs, wrapper validation, and failure-safe
  redacted test/coverage/security evidence. Validate workflow changes against
  the existing CI contract.
- Supported API, nullability, properties, runtime names, auto-configuration
  imports, plugin IDs/tasks, wire formats, and published dependencies all have
  compatibility obligations. Follow `docs/pre-1.0-api-policy.md`; even `0.x`
  breaks require review, migration guidance, and changelog entries.
- Regenerate public inventories or compatibility baselines only for intentional
  supported changes and review the diff. Never broaden a baseline to hide drift.
  Update the canonical documentation, not duplicate reference material.
- Commits and PR titles use English Conventional Commits:
  `type(scope): description`, with `!`/`BREAKING CHANGE` for incompatibilities.
  Follow `CONTRIBUTING.md`; changes reach `main` through reviewed PRs with
  squash/rebase, not direct pushes or merge commits.

## Required Verification After Changes

Use focused tests while developing, then the relevant gates before reporting
completion. Respect a request limiting execution or edits; report unrun checks
and blockers explicitly rather than claiming success.

| Changed area | Required relevant commands |
|---|---|
| Module implementation | `./gradlew :<module-path>:check` |
| Java/source conventions | `./gradlew spotlessCheck codeQualityCheck` |
| Gradle conventions | `./gradlew conventionPluginsCheck` |
| OpenAPI plugin | `./gradlew -p build-logic :spring-boot-service-framework-openapi-gradle-plugin:check` |
| OpenAPI templates | `./gradlew -p build-logic :spring-boot-service-framework-openapi-templates:check` |
| OpenAPI phases | `./gradlew openApiGradlePluginCompatibilityCheck smbtechOpenApiCompatibilityCheck documentationCheck` plus the phase's tests |
| Scaffolding | `./gradlew :spring-boot-service-framework-project-generator:check :spring-boot-service-framework-project-generator:scaffoldingCompatibilityCheck` |
| Platform/dependencies | `./gradlew platformCompatibilityCheck consumerSmoke` |
| Public/cross-module behavior | `./gradlew compatibilityCheck` and the affected capability's compatibility/consumer checks |
| GitHub CI contract | `./gradlew pullRequestCiCompatibilityCheck` |
| Documentation | `./gradlew documentationCheck` |

Run `spotlessApply` only within authorized edit scope and inspect its diff.
For broad changes, run the complete non-remote-publishing PR gate:

```sh
./gradlew clean pullRequestGate --no-daemon --stacktrace --console=plain
```

Root `check`, compatibility, and PR gates are broader than unit tests: they may
generate artifacts, publish locally, run consumers/AOT, and produce SBOMs.
Report the commands actually run, their outcomes, and any remaining verification.
