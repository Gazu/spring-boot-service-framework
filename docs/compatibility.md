# Compatibility

## Supported matrix

| Component | Supported version | Policy |
|---|---:|---|
| Framework platform | 0.5.2 | Import `com.smbtech:spring-boot-service-framework-platform` and omit versions from managed modules |
| Java | 21 | Framework toolchain and bytecode target |
| Gradle | Wrapper-provided Gradle, currently 9.3.1 in generated reports | Use the repository wrapper when available |
| Spring Boot | 4.1.0 | Controlled by `springBootVersion` in the root `gradle.properties` |
| Jackson | 3.1.x | Boot 4 runtime stack using `tools.jackson`; annotations remain `com.fasterxml.jackson.annotation` |
| GraalVM Native Image | 25+ | Native Build Tools 1.1.1; AOT is validated on every release |
| SLF4J | 2.0.x | Managed by Spring Boot in the logging starter |
| Logback | 1.5.x | Managed by Spring Boot in the logging starter |
| Apache HttpClient | 5.x | Managed by Spring Boot in `spring-boot-service-framework-starter-rest-client` |
| OpenAPI Generator | 7.24.0 | Pinned by `openApiGeneratorVersion`; generated public output is reviewed before upgrades |
| OpenAPI Diff | 2.1.7 | Pinned by `openApiDiffVersion`; adoption occurs during the compatibility migration |
| Spring Initializr | 0.24.0 | Pinned by `springInitializrVersion`; used only by the build-time project generator |

`spring-boot-service-framework-logging-core` depends only on the JDK. The Spring Boot, SLF4J,
and Logback matrix applies to the starter and its adapters.

`spring-boot-service-framework-http-client-core` also depends only on the JDK. Spring Web,
Apache HttpClient, Micrometer, and auto-configuration are implemented in
`spring-boot-service-framework-starter-rest-client`.

## Compatibility contract

- The generated [Public API Inventory](public-api-inventory.md) is the
  repository-wide source baseline for supported packages, public types,
  extension points, configuration properties, exceptions, framework
  infrastructure, and public internal types requiring review.
- Supported Java packages declare JSpecify `@NullMarked`. Nullable values in
  those contracts are annotated explicitly; implementation packages are not
  part of this nullability promise.
- `binaryCompatibilityCheck` runs japicmp against the release configured by
  `binaryCompatibilityBaselineVersion`. During `0.x`, an intentional break must
  be documented in `CHANGELOG.md` and narrowly recorded in
  `gradle/compatibility/binary-breaking-changes.txt`.
- `gradle/compatibility/public-internal-types.txt` prevents technically public
  implementation types from growing silently. It is an implementation review
  baseline, not a compatibility promise for those classes.
- `gradle/compatibility/public-type-classification.txt` assigns every top-level
  public type exactly one reviewed category. Additions, removals, and category
  changes fail `validatePublicTypeClassificationBaseline` until the baseline is
  intentionally regenerated.
- `gradle/compatibility/concrete-replaceable-beans.txt` records existing
  replaceable beans whose declared return type is concrete. New entries fail
  `validateConcreteReplaceableBeans`; the baseline can only decrease as bean
  methods move to supported interfaces or reviewed external contracts.
- Supported consumer packages and documented exceptions are defined by
  [Public API Boundaries](public-api-boundaries.md). A Java `public` modifier on
  an adapter, auto-configuration, serializer, internal class, or build-logic
  implementation does not create a compatibility promise.
- An intentional public surface change requires running
  `./gradlew generatePublicApiInventory` and reviewing the catalog diff.
  `validatePublicApiInventory` fails when the checked-in baseline is stale.
- Reviewed module contracts are stored under `gradle/compatibility/contracts`.
  They protect supported public types, extension points, configuration
  properties, Spring Boot auto-configuration imports, and Gradle plugin ids.
  Public implementation types in `adapter`, `autoconfigure`, `serialization`,
  `internal`, and build-logic implementation packages are excluded unless they
  form an explicitly listed framework contract such as an auto-configuration
  import.
- The framework platform contract is stored in
  `gradle/compatibility/contracts/platform.txt`. It protects the platform
  coordinate, managed framework modules, imported Spring Boot BOM, excluded
  coordinate groups, and Maven `pom` packaging.
- `platformCompatibilityCheck` validates the declared Gradle constraints,
  generated Maven POM, local publication metadata, committed platform contract,
  and absence of a runtime JAR.
- The Actuator module boundary is defined by
  [Actuator Architecture Contract](actuator.md), while supported API, runtime
  names, payload fields, and change policy are defined by
  [Actuator Compatibility](actuator/compatibility.md). `actuatorContractCheck`
  protects framework-neutral core dependencies, stable runtime names, passive
  health behavior, endpoint access defaults, and application ownership of
  security, exposure, and health groups as those modules evolve.
- The Actuator artifacts provide the neutral diagnostic API, Spring Boot
  health and info adapters, a disabled-by-default read-only endpoint, and
  passive optional-starter integrations with bounded Micrometer status and
  module gauges. Default diagnostics use bounded execution, payload limits,
  timeouts, and single-flight caching. Optional starter dependencies remain
  non-transitive.
- `moduleCompatibilityCheck` runs all module contracts and their behavioral
  checks. Focused checks are available as `commonsCompatibilityCheck`,
  `loggingCompatibilityCheck`, `httpClientCompatibilityCheck`,
  `mockCompatibilityCheck`, `actuatorCompatibilityCheck`,
  `openApiGeneratorCompatibilityCheck`,
  `contractTestingCompatibilityCheck`, and
  `openApiGradlePluginCompatibilityCheck`.
- An intentional compatible or breaking surface change requires running
  `./gradlew generateModuleCompatibilityContracts`, reviewing every generated
  contract diff, and recording incompatible changes in `CHANGELOG.md`.
- Version `0.x` can evolve, but incompatible changes must be recorded in the
  affected module documentation or release notes.
- `StructuredEvent`, `StructuredLogger`, and `StructuredLoggerFactory` are the
  recommended APIs for new logging consumers.
- Properties under `smbtech.logging` are part of the logging starter contract.
- Logging runtime names, packaged Logback fragments, legacy property precedence,
  and compatible-change rules are defined in
  [Logging Compatibility](logging/compatibility.md).
- Properties under `smbtech.rest-clients` are part of the REST Client starter
  contract.
- The `token-request-id` rename in `0.5.0` is an explicit configuration break:
  `credential-token-requestor-id` is not an alias. Consumers must update their
  configuration before upgrading to the release that contains this change. The
  complete property and environment-variable migration is documented in
  [Migrate Public Names And Properties](guides/migrate-public-names-and-properties.md).
  The corresponding `HttpClientDefinition` record accessor is now
  `tokenRequestId()`.
- The HTTP client exception cleanup in `0.5.0` is an explicit source break:
  `AuthenticationException` is now `HttpClientAuthenticationException`, and the
  unused `MockRestClientException` type has been removed without aliases.
- The supported mock facade moved from
  `com.smbtech.serviceframework.starter.mock.api.mock.MockService` to
  `com.smbtech.serviceframework.starter.mock.api.MockService`. Its methods and
  bean behavior are unchanged.
- `TransactionIdFilter`, `ServiceFrameworkStructuredLogFormatter`, and mock
  classes under `adapter.out.restclient` are implementation types. Their
  normalized names and packages are documented for migration, but they are not
  added to the supported extension boundary.
- Properties under `smbtech.error-handling` and their generated defaults are
  part of the error handling starter contract.
- `smbtech.error-handling.response.exposure` is protected with default
  `PUBLIC` and supported values `PUBLIC` and `INTERNAL`. The public
  `ErrorExposurePolicy` functional interface remains the replacement point for
  applications that need a custom policy.
- The `PUBLIC` default returns a stable code with a generic message and minimal
  metadata. Applications requiring detailed sanitized responses must configure
  `INTERNAL` explicitly after reviewing its application-wide scope.
- The error handling response is the flat snake-case `Notification` documented
  in [error-handling/json-contract.md](error-handling/json-contract.md).
- Spring Security catalog codes, public reasons, OAuth2 metadata, and RFC 6750
  challenge behavior are defined in
  [error-handling/security.md](error-handling/security.md). JWT validation
  details and opaque-token provider responses remain internal diagnostics.
- Core types under `com.smbtech.serviceframework.error` and interfaces under
  `com.smbtech.serviceframework.starter.errorhandling.api` are public extension
  contracts. Their documented signatures are protected by
  `ErrorHandlingPublicApiCompatibilityTest`.
- Error handling defaults must continue to back off for application-provided
  resolvers, policies, sanitizers, factories, serializers, writers, reporters,
  metrics recorders, customizers, and Spring Security handlers.
- Security defaults must continue to back off for application-provided
  authentication and authorization failure resolvers, required-scope resolver,
  OAuth2 metadata factory, and challenge writer.
- REST Client public extension points and internal package boundaries are
  defined in [rest-client-extension-points.md](rest-client-extension-points.md).
- A Java or Spring Boot major-version change requires updating this matrix and
  running `compatibilityCheck` successfully.
- Runtime modules use Jackson 3. Public APIs exposing `ObjectMapper`, `JsonNode`,
  `TypeReference`, generators, or serialization contexts use `tools.jackson`.
  This replaces the Jackson 2 signatures used before the `0.5.0` Boot 4 native
  alignment and is an explicit source incompatibility.
- `nativeAotCheck` runs `processAot` for every standalone consumer and is part
  of `releaseGate`. Native executables can be built with `nativeCompile` when a
  compatible GraalVM installation is available.
- Application-defined declarative HTTP interfaces created through
  `ApiClientFactory` must be registered with `HttpApiClientRuntimeHints` for a
  native image. The application owns this fixed build-time type list.
- `compatibilityCheck` runs the reviewed module contracts, platform contract,
  framework baseline, generated OpenAPI compatibility checks, and standalone
  consumer smoke tests against published artifacts under each module's
  `build/repository`.
- `spring-boot-service-framework-openapi-contract-testing` is a test-scope
  Spring MVC module. Its public loader, test-case, tester, result, and violation
  types are covered by the module tests and root `baseline` task.
- `smbtechOpenApiCompatibilityCheck` is the public plugin-native gate for
  OpenAPI Diff/SemVer validation, reproducible archives, generated consumer
  boundaries, migration reporting, and mock-server contract adoption.
- `openApiDocumentationCompatibilityCheck` protects canonical OpenAPI document
  paths, validation tasks, quality evidence, and pull request/release rollout
  against `gradle/compatibility/contracts/openApiDocumentation.txt`.
- `documentationCheck` validates the public API inventory, Markdown structure,
  relative links and anchors,
  canonical documentation coverage, changelog/release docs, framework version
  references, OpenAPI configuration and contract validation, generated property
  references, and example documentation/configuration for accidentally committed
  secrets or encoded keystore material. It is part of the root `check`, `baseline`, and
  `compatibilityCheck` flows.
- The framework platform import, starter dependency management, and standalone
  example Spring Boot plugins must read the same `springBootVersion` property
  from the root `gradle.properties`.
- The REST Client starter must remain compatible with Spring Boot OAuth2 Client
  auto-configuration creating the default `OAuth2AuthorizedClientService`. The
  starter applies its token-cache policy by wrapping the available service.
- Spring Security OAuth2 Client must not be a transitive dependency of the REST
  Client starter. The base auto-configuration must start with Spring Security
  absent, while OAuth2 support activates when the application explicitly adds
  `spring-boot-starter-oauth2-client`.
- `RestClientAuthenticationConfigurer` is the public boundary between REST
  client construction and optional authentication implementations.
- OAuth2 REST client startup validation is enabled by default for new
  consumers, but `smbtech.rest-clients.validation.enabled=false` must remain a
  compatibility escape hatch for legacy or staged migrations.
- The REST Client starter must not require the logging starter. When no
  `StructuredLoggerFactory` bean is provided by the application, OAuth2
  validation and diagnostics use the starter's internal SLF4J-backed structured
  logger adapter.
- User-provided `OAuth2AuthorizedClientService` beans must remain usable. The
  REST Client starter may decorate them to apply token-cache policy, but it must
  preserve delegation semantics instead of replacing the service behavior.
- `RequestContext`, `RequestContextManager`, and `RequestContextScope` are
  public REST Client starter APIs for dynamic outbound headers and JWT bearer
  claims. New releases must keep the documented builder/factory methods and
  default manager bean compatible unless an incompatible change is explicitly
  recorded.
- Types under `com.smbtech.serviceframework.starter.restclient.api.oauth2` are
  public OAuth2 extension contracts. Runtime integration can evolve during
  `0.x`, but method signatures should remain source-compatible unless a
  breaking change is explicitly recorded.
- `AccessTokenClient.jwtBearer(JwtBearerTokenRequest)` is the canonical JWT
  bearer extension method. The string overloads are convenience defaults. This
  pre-1.0 source break ensures custom claims are never discarded by a legacy
  implementation.
- Request context propagation is enabled by default and must remain opt-out
  through `smbtech.rest-clients.request-context.enabled`,
  `smbtech.rest-clients.request-context.headers`, and
  `smbtech.rest-clients.request-context.jwt-bearer-claims`. Custom
  `RequestContextManager` beans must be preserved by auto-configuration.
- Replacement points for `AccessTokenClient`, `AccessTokenProvider`,
  `RestClientRegistry`, `ApiClientFactory`, `OAuth2AuthorizedClientManager`,
  `OAuth2AuthorizedClientProvider`, and `OAuth2AuthorizedClientService` must be
  covered by `RestClientCompatibilityTest`.
- `compatibilityCheck` runs the error handling public API compatibility tests,
  security leak tests, integration tests, and standalone
  `error-handling-consumer` smoke test through the module baseline and consumer
  smoke lifecycle.
- `errorHandlingCompatibilityCheck` is the focused compatibility lifecycle for
  the error core, starter, shared `Notification` reuse, and standalone published
  consumer.

## Current scope

The HTTP correlation adapter supports Spring MVC/Servlet. WebFlux is not
auto-configured until a real consumer defines and tests the desired propagation
semantics.

`ApiClientFactory` uses Spring Web declarative HTTP interfaces (`@HttpExchange`,
`@GetExchange`, etc.) on top of `RestClient`. Automatic interface scanning is not
enabled yet; create proxies explicitly through `ApiClientFactory`.

Generated OpenAPI client JARs expose Spring HTTP interfaces annotated with
`@HttpApiClient` and Spring exchange annotations. They are compile-time contract
artifacts and do not embed generated model classes or server API classes.
Generated OpenAPI artifacts are Maven-published under `com.smbtech.contracts`
from the root project, using `info.version` as the artifact version.
OpenAPI spec content for a given `info.title` and `info.version` must match its
immutable versioned baseline; changing the contract requires a new spec version
and baseline snapshot.
Generated OpenAPI JARs must use stable entry ordering and fixed entry
modification times so repeated generation from the same inputs produces the same
artifact hash.
Generated OpenAPI `models`, `server-api`, and `client` artifacts must compile together
from a consumer-style source set before they are considered valid.
Generated OpenAPI artifacts must also pass `smbtechOpenApiCompatibilityCheck` before a
release can be considered compatible with generated contract consumers.
Spring MVC implementations can be checked at test time with
`OpenApiMvcContractTester`; this verification does not change application
runtime behavior.
