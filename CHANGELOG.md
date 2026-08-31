# Changelog

All notable changes to this project are documented in this file.

This project follows [Semantic Versioning](https://semver.org/) and uses
[Conventional Commits](https://www.conventionalcommits.org/) for commit
messages.

Release process details live in [docs/releasing.md](docs/releasing.md).

## Unreleased

No unreleased changes.

## 0.5.0 - 2026-08-31

**Migration required:** This pre-1.0 release intentionally removes accidental
public implementation APIs and includes source, binary, dependency, and
configuration changes. Follow the
[pre-1.0 migration guide](docs/guides/migrate-public-names-and-properties.md)
before upgrading from `0.4.x`.

### Highlights

- Generate and publish independent OpenAPI models, Spring MVC server API, and
  Spring HTTP interface client artifacts directly from a validated contract.
- Scaffold Spring Boot 4.1 components with hexagonal boundaries and ArchUnit
  verification from a generated server API artifact.
- Establish a deliberately small public framework surface based on neutral
  contracts, replaceable Spring beans, factories, contributors, and customizers.
- Add reproducible, signed release bundles and read-only pull request gates for
  compatibility, quality, security, and supply-chain evidence.

### Added

- Add a publishable OpenAPI Gradle plugin and template bundle backed by OpenAPI
  Generator, with typed DSL configuration for artifacts, packages, and Maven
  publication.
- Add plugin-native contract validation, OpenAPI Diff and SemVer enforcement,
  reproducibility hashes, generated consumer checks, migration reporting,
  contract testing, and OpenAPI mock-server support.
- Add canonical OpenAPI guides for generation, publication, versioning,
  validation, testing, troubleshooting, and scaffolding, protected by a
  documentation compatibility gate.
- Add a project generator for Spring Boot 4.1 applications with delegate
  implementation, hexagonal packages, configuration, tests, and ArchUnit rules.
- Add a versioned release lifecycle, publication manifest, signed `build-logic`
  artifacts, deterministic metadata, complete payload checksums, CycloneDX
  SBOMs, and a verified release archive.
- Add an explicit unsigned `releaseCandidate` lifecycle with commit, source
  state, archive identity, and checksum evidence before tag creation.
- Add pull request gates for Conventional Commits, Gradle quality checks,
  compatibility, Gitleaks, Trivy, dependency review, evidence retention, and
  branch-protection verification.
- Add reviewed public API inventories and automated rules for package
  boundaries, replaceable beans, Javadocs, source layout, test conventions, and
  implementation-type containment.

### Changed

- Make the typed `smbtechOpenApi*` Gradle lifecycle the only supported OpenAPI
  generation, publication, and compatibility workflow.
- Align runtime serialization contracts with Spring Boot 4.1 and Jackson 3;
  generated model annotations remain on `com.fasterxml.jackson.annotation`.
- Rename REST client `credential-token-requestor-id` to `token-request-id` and
  `AuthenticationException` to `HttpClientAuthenticationException`.
- Move `MockService` to `starter.mock.api` and make
  `JwtBearerTokenRequest` the canonical dynamic JWT bearer request contract.
- Expose default behavior through supported factories including
  `ThrowableErrorResolver`, `NotificationSanitizer`,
  `NotificationAggregationPolicy`, `StructuredLogger`, `HttpClientCatalog`,
  `MockCatalog`, `MockResponder`, `HexagonalProjectGenerator`, and
  `FrameworkDiagnostics`.
- Keep error handling, OAuth2, transport, logging, mock, Actuator, and generator
  implementation classes behind their public ports and Spring replacement
  points.
- Remove the logging dependency from Commons and keep its notification API
  framework-neutral.
- Homologate tests with current production packages, remove obsolete source
  directories, and enforce the maintained module structure automatically.

### Removed

- Remove the custom `spring-boot-service-framework-openapi-generator` runtime
  module, embedded Groovy generator, spec version catalog, and legacy task
  aliases.
- Remove accidentally public concrete implementations from Error Core, error
  handling, HTTP client, REST client, logging, Mock, Actuator, OpenAPI, and the
  project generator. Supported interfaces, factories, properties, and
  customizers replace direct construction.
- Remove the misplaced core `ScopeValidator`, `MockRestClientException`, and
  unsupported implementation markers that had no runtime behavior.

### Fixed

- Track embedded OpenAPI toolchain versions as build inputs so clean release
  builds compile generated clients against the matching framework version.
- Correct release workflow permissions and secret scope, verify the bundle
  checksum before attestation, and retain lifecycle evidence.
- Exclude examples and test tooling from the production SBOM, add Apache 2.0
  license metadata, and validate CycloneDX 1.6 coverage and graph integrity.

### Security

- Prevent tokens, passwords, sensitive headers, downstream bodies, stack
  traces, and diagnostic messages from leaking through public notifications.
- Add Gitleaks and Trivy scanning with redacted evidence, dependency checksum
  verification, signed Maven publication, provenance attestation, and strict
  branch protection for `main`.

## 0.4.0 - 2026-07-29

### Added

- Define and automatically validate the async logging topology, delivery policy,
  critical-event limitations, operational ranges, shutdown behavior, behavioral
  tests, local performance baseline, and fail-fast configuration constraints.
- Add explicit `BLOCK`, `DISCARD_LOW_PRIORITY`, and `DROP_WHEN_FULL` async
  saturation policies while preserving the former Logback threshold and
  never-block settings as compatibility overrides.
- Protect `WARN`, `ERROR`, `AUDIT`, and `SECURITY` events from async queue
  saturation, including a synchronous delegate fallback when a
  `DROP_WHEN_FULL` queue is full.
- Add bounded async logging metrics, coordinated shutdown, deterministic
  concurrency tests, and a published-artifact workload example.
- Add reusable Logback fragments and
  `SERVICE_FRAMEWORK_LOGGING_DELEGATE` so applications can replace the output
  destination without copying framework async configuration.
- Add a logging compatibility guide and protect Logback resources, runtime
  names, legacy property precedence, documentation, and the published consumer
  in the focused compatibility lifecycle.
- Define and automatically validate the architecture, management ownership,
  passive health, security, and information-exposure contract for the planned
  Actuator core and starter.
- Add publishable Actuator core and starter module scaffolds, manage both
  artifacts through the framework platform, and keep REST client and mock
  integrations non-transitive.
- Add framework-neutral Actuator domain values, diagnostic and module
  information ports, deterministic aggregation, failure isolation, and bounded
  secret-safe diagnostic details.
- Add base Actuator auto-configuration with an opt-out property, neutral probe
  and module provider discovery, application diagnostics backoff, and optional
  application clock reuse.
- Add the `serviceFramework` Spring Boot health indicator with exact neutral
  status mapping, safe component details, failure isolation, standard
  management enablement, and application bean backoff.
- Add the `serviceFramework` info contribution and disabled-by-default,
  read-only diagnostic endpoint.
- Add passive, secret-safe Actuator integrations for REST client, mock,
  logging, and error handling starters while keeping those starters
  non-transitive.
- Add bounded-cardinality Actuator gauges for aggregate status, component
  status counts, and detected framework modules.
- Add guarded Actuator diagnostics with bounded payloads, operation timeouts,
  single-flight caching, bounded execution, and static secret-safe failures.
- Add a standalone published-artifact Actuator consumer with real HTTP,
  authorization, redaction, health, info, diagnostics, metrics, and AOT smoke
  tests.
- Add the Actuator compatibility guide, a method-level neutral API test,
  generated runtime-name contracts, and a focused compatibility lifecycle that
  includes documentation and the published consumer.
- Add the Spring Boot Service Framework dependency platform, Gradle and Maven
  BOM consumption, compatibility validation, standalone consumer coverage, and
  dependency management documentation.
- Add strict SHA-256 dependency verification, CycloneDX 1.6 aggregate SBOMs,
  per-module JaCoCo coverage gates, reproducible archives, complete Maven POM
  metadata, and signed Maven publications.
- Add pinned GitHub Actions for secret scanning, dependency review, signed-tag
  release publication, release bundle provenance attestation, and Dependabot.
- Add JSpecify nullness defaults to every supported Java package and publish the
  annotations as compile-time API metadata.
- Add japicmp binary compatibility checks against `v0.3.0`, reviewed pre-1.0
  exceptions, and a checked baseline that prevents growth of technically public
  implementation types.
- Add a production-profile guard for OpenAPI mock routes through
  `smbtech.mocks.openapi.allow-in-production` and
  `smbtech.mocks.openapi.production-profiles`.
- Add Spring Boot AOT processing to all standalone consumers and the release
  gate, GraalVM Native Build Tools configuration, starter runtime hints, and
  `HttpApiClientRuntimeHints` for application-defined declarative clients.

### Changed

- **Breaking source change:** make
  `AccessTokenClient.jwtBearer(JwtBearerTokenRequest)` the canonical JWT bearer
  extension contract. Existing string overloads remain as default convenience
  methods, while custom implementations must implement the complete request so
  dynamic claims cannot be discarded silently.
- Reduce six OAuth2 implementation types from public to package-private without
  changing supported REST client API packages or Spring beans.
- Make structured metadata and context containers recursively immutable across
  notifications, logging events, HTTP client JWT claims, mocks, request context,
  and OAuth2 extension contexts. Cyclic structured values are rejected.
- Add immutable copy methods to `Notification`, `ResolvedError`, and
  `SecurityFailureResolution`, and use them throughout the error pipeline.
- Consolidate MVC and Spring Security response preparation, final sanitization,
  reporting, and metrics in one internal pipeline with an explicit stage order.
- Enrich Spring Security metadata before application error customizers and
  centralize OAuth2 registration value mapping used by token pipelines.
- Modernize OpenAPI ingestion with Jackson 3 structural parsing, explicit 3.0
  and 3.1 support across generation, contract testing, and mock loading, and a
  typed `validateOpenApiSpecs` Gradle task wired without `afterEvaluate`.
- Replace deprecated Jackson 2-style tree APIs in OpenAPI runtime code with
  their Jackson 3 equivalents.
- **Breaking dependency change:** Spring Security OAuth2 Client is no longer a
  transitive dependency of the REST Client starter. OAuth2 consumers must add
  `org.springframework.boot:spring-boot-starter-oauth2-client` explicitly.
- Isolate OAuth2 auto-configuration and REST client authentication behind
  `RestClientAuthenticationConfigurer`, allowing non-OAuth2 applications to
  run without Spring Security.
- **Breaking source change:** migrate runtime and OpenAPI contract-testing
  Jackson APIs from Jackson 2 (`com.fasterxml.jackson`) to Boot 4's Jackson 3
  (`tools.jackson`). OpenAPI model annotations remain under
  `com.fasterxml.jackson.annotation`.
- Make OpenAPI mock response status selection opt-in through
  `smbtech.mocks.openapi.status-override-enabled`.
- Remove the temporary OAuth2 token request `ThreadLocal` while preserving
  ordered parameter and header customizers.
- **Breaking behavior change:** error responses now default globally to
  `PUBLIC`, which preserves the resolved code but returns a generic message and
  minimal safe metadata. Trusted applications requiring detailed sanitized
  responses must configure
  `smbtech.error-handling.response.exposure=INTERNAL` explicitly.
- Reinterpret `ErrorExposure.PUBLIC` as the minimal external contract and
  `ErrorExposure.INTERNAL` as the detailed sanitized contract for trusted
  consumers.
- Preserve the resolved error code, severity, notification id, and timestamp in
  both exposure modes.

### Security

- Redact authentication headers, cookies, API keys, token query parameters,
  body secrets, and exception credentials before audit events reach any sink.
- Reapply mandatory exposure and secret sanitization after notification
  response customizers for MVC and Spring Security handlers.
- Keep diagnostics, exception causes, stack traces, credentials, sensitive
  headers, and downstream bodies out of both response modes.
- Limit `PUBLIC` metadata to category and optional correlation ID while keeping
  RFC 6750 `WWW-Authenticate` challenges independent from body exposure.

## 0.3.0 - 2026-07-20

### Added

- Add global error response exposure through
  `smbtech.error-handling.response.exposure`, supporting `INTERNAL` and
  `PUBLIC`, with a replaceable `ErrorExposurePolicy` extension point.
- Add reviewed compatibility contracts and focused checks for Commons, Logging,
  HTTP Client, Mock, OpenAPI Generator, Contract Testing, and the OpenAPI Gradle
  plugin, covering supported APIs, properties, auto-configuration imports, and
  extension points without freezing internal implementations.
- Add a migration guide for renamed Java types, mock packages, logging
  infrastructure names, and the REST client `token-request-id` property.
- Add Gradle convention plugins for Java libraries and Spring Boot starters,
  centralizing Java 21, UTF-8, parameter metadata, JUnit, documentation/source
  artifacts, dependency management, and Maven publication.
- Add `codeQualityCheck` for formatting, public API Javadocs, public package
  documentation, prohibited legacy names, and commented-out Java code.
- Document new user-facing features, modules, public APIs, configuration
  properties, and examples.
- Define the OpenAPI code generation coordinate convention based on
  `info.title` and `info.version`.
- Add OpenAPI spec validation for `info.title`, `info.version`, normalized
  artifact names, and duplicate generated coordinates.
- Add an executable OpenAPI name normalization contract for generated artifact
  base names.
- Generate deterministic OpenAPI `contract.properties` metadata for planned
  models, api, and client artifacts.
- Generate and validate OpenAPI models JARs with Jackson annotations, required
  field validation, and embedded contract metadata.
- Generate and validate OpenAPI server API JARs with Spring delegate/controller
  types and embedded contract metadata.
- Generate and validate OpenAPI client JARs with `@HttpApiClient`, Spring HTTP
  exchange annotations, and embedded contract metadata.
- Publish generated OpenAPI models, server API, and client artifacts as Maven
  publications with generated POM dependencies.
- Add OpenAPI spec version catalog validation to prevent publishing changed spec
  content under the same `info.version`.
- Add reproducible OpenAPI generation validation with stable JAR entry ordering,
  fixed JAR timestamps, and repeated packaging hash checks.
- Add OpenAPI consumer-style compilation tests for generated models, server API,
  and client artifacts.
- Add OpenAPI artifact-separation validation so generated models, server API,
  and client JARs cannot embed each other's classes.
- Add `openApiCompatibilityCheck` as the aggregate generated OpenAPI artifact
  compatibility gate and include it in the root `compatibilityCheck` flow.
- Add the `spring-boot-service-framework-openapi-generator` module boundary for
  the future extracted OpenAPI generator implementation.
- Add internal OpenAPI generator Gradle build logic under
  `build-logic/openapi-generator-plugin` and wire it into `baseline`.
- Add the initial OpenAPI generator Java structure for spec metadata, name
  normalization, metadata generation, artifact descriptors, source-generation
  boundaries, and reproducible JAR packaging.
- Add generator module tests for spec parsing, metadata rendering, artifact
  descriptors, package boundaries, and reproducible JAR packaging.
- Add OpenAPI generator module compatibility validation and include it in the
  aggregate OpenAPI compatibility gate.
- Move OpenAPI Gradle functions, tasks, and publication wiring into the
  `com.smbtech.service-framework.openapi-generator` plugin and remove the root
  generator script coupling.
- Extract documentation validation and root lifecycle tasks from the root
  `build.gradle` into focused scripts under `gradle/`.
- Add the `smbtechOpenApi` Gradle configuration API with global defaults,
  named specs, per-spec overrides, and build-logic validation.
- Add `validateOpenApiTaskCompatibility` to keep public OpenAPI Gradle command
  names stable during the plugin migration.
- Add a complete `retail-loyalty-rewards` OpenAPI fixture that exercises
  multiple operations, path parameters, schemas, scalar types, dates, generated
  artifacts, publication, reproducibility, and compilation tests.
- Document the end-to-end OpenAPI artifact workflow with a copy-oriented guide,
  index links, and canonical quick navigation.
- Document OpenAPI generator module boundaries, Gradle build-logic boundaries,
  compatibility commands, and maintainer workflow.
- Document the OpenAPI generator post-split evolution roadmap with migration
  stages, ownership rules, capability roadmap, and compatibility gates.
- Expand OpenAPI model generation with component refs, typed arrays, typed maps,
  top-level and inline string enums, Jakarta validation constraints, and an
  advanced warehouse inventory fixture.
- Add the OpenAPI contract-testing module with YAML/JSON loading, `MockMvc`
  execution, status and media-type checks, JSON schema validation, inspectable
  violations, and explicit success or error response cases.
- Add OpenAPI breaking change detection with immutable version baselines,
  operation/parameter/request/response/schema comparison, stable change codes,
  SemVer enforcement, strict CI mode, and aggregate compatibility wiring.

### Changed

- **Behavior change:** error responses now default globally to `INTERNAL`,
  replacing the previous mixed behavior where each resolver selected its own
  exposure. Applications that require resolved public codes and messages must
  set `smbtech.error-handling.response.exposure=PUBLIC` explicitly.
- Document backward-compatible behavior changes, dependency upgrades, and
  documentation reorganizations.
- Replace the original OpenAPI example contract with the neutral
  `merchant-order-status` contract and generated artifact examples.
- **Breaking configuration change:** rename
  `smbtech.rest-clients.clients.<name>.credential-token-requestor-id` to
  `smbtech.rest-clients.clients.<name>.token-request-id`. The former property is
  not accepted as an alias. The corresponding `HttpClientDefinition` accessor
  is now `tokenRequestId()`.
- Rename `TransactionalIdFilter` to `TransactionIdFilter` and
  `SmbStructuredLogFormatter` to `ServiceFrameworkStructuredLogFormatter`.
- Rename the HTTP client `AuthenticationException` to
  `HttpClientAuthenticationException` to distinguish outbound client failures
  from Spring Security authentication failures.
- Move `MockService` from `starter.mock.api.mock` to the supported
  `starter.mock.api` package and move outbound REST client mock adapters from
  `adapter.in.restclient` to `adapter.out.restclient`.

### Fixed

- Document bug fixes and compatibility corrections.

### Removed

- Remove the unused `MockRestClientException`; mock failures continue to use the
  mock-core `MockException`.

## 0.2.0 - 2026-07-16

### Added

- REST client OAuth2 support for `client_credentials`,
  `private_key_jwt` client authentication, JWT bearer grant, token diagnostics,
  token-cache policy, startup configuration validation, and request context
  propagation for dynamic headers and JWT bearer claims.
- Public REST client extension contracts for OAuth2 contributors, customizers,
  token request customization, cache identity customization, and replacement
  points.
- Inspectable downstream HTTP exceptions with complete error response access and
  JSON decoding helpers.
- Generated property references for REST client, logging, and mock
  configuration.
- Canonical documentation index, use-case guides, compatibility guide,
  troubleshooting guide, and documentation architecture guide.

### Changed

- Repository naming, artifact coordinates, documentation, and examples now use
  `spring-boot-service-framework`.
- Module READMEs were reduced to concise entry points that link to canonical
  guides under `docs/`.
- Spring Boot support was aligned to the version managed by
  `springBootVersion` in `gradle.properties`.

### Fixed

- Example configuration validation now rejects committed secret-like values,
  literal client ids, oversized encoded material, and unsafe environment values.
- Documentation checks now validate Markdown links and generated property
  references as part of the root `check`.

## 0.1.0

### Added

- Initial framework modules for commons, structured logging, REST client
  support, mock support, starters, local publication, and standalone examples.
