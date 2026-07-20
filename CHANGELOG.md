# Changelog

All notable changes to this project are documented in this file.

This project follows [Semantic Versioning](https://semver.org/) and uses
[Conventional Commits](https://www.conventionalcommits.org/) for commit
messages.

Release process details live in [docs/releasing.md](docs/releasing.md).

## Unreleased

Use this section for changes that have been merged but not released yet.

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
