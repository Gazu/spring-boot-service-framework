# Compatibility

## Supported matrix

| Component | Supported version | Policy |
|---|---:|---|
| Java | 21 | Framework toolchain and bytecode target |
| Gradle | Wrapper-provided Gradle, currently 9.3.1 in generated reports | Use the repository wrapper when available |
| Spring Boot | 4.1.0 | Controlled by `springBootVersion` in the root `gradle.properties` |
| SLF4J | 2.0.x | Managed by Spring Boot in the logging starter |
| Logback | 1.5.x | Managed by Spring Boot in the logging starter |
| Apache HttpClient | 5.x | Managed by Spring Boot in `spring-boot-service-framework-starter-rest-client` |

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
- `moduleCompatibilityCheck` runs all module contracts and their behavioral
  checks. Focused checks are available as `commonsCompatibilityCheck`,
  `loggingCompatibilityCheck`, `httpClientCompatibilityCheck`,
  `mockCompatibilityCheck`, `openApiGeneratorCompatibilityCheck`,
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
- Properties under `smbtech.rest-clients` are part of the REST Client starter
  contract.
- The unreleased `token-request-id` rename is an explicit configuration break:
  `credential-token-requestor-id` is not an alias. Consumers must update their
  configuration before upgrading to the release that contains this change. The
  complete property and environment-variable migration is documented in
  [Migrate Public Names And Properties](guides/migrate-public-names-and-properties.md).
  The corresponding `HttpClientDefinition` record accessor is now
  `tokenRequestId()`.
- The unreleased HTTP client exception cleanup is an explicit source break:
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
  `INTERNAL` and supported values `INTERNAL` and `PUBLIC`. The public
  `ErrorExposurePolicy` functional interface remains the replacement point for
  applications that need a custom policy.
- The `INTERNAL` default intentionally replaces the earlier mixed behavior in
  which each resolver selected its final exposure. Upgrading applications that
  relied on public resolver responses must configure `PUBLIC` explicitly after
  reviewing its application-wide scope.
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
- `compatibilityCheck` runs the reviewed module contracts, framework baseline,
  generated OpenAPI compatibility checks, and standalone consumer smoke tests
  against published artifacts under each module's `build/repository`.
- `spring-boot-service-framework-openapi-contract-testing` is a test-scope
  Spring MVC module. Its public loader, test-case, tester, result, and violation
  types are covered by the module tests and root `baseline` task.
- `openApiCompatibilityCheck` validates the generated OpenAPI artifact
  contract: name normalization, spec metadata, spec version catalog, models JAR,
  server API JAR, client JAR, breaking change detection and SemVer policy,
  advanced model generation, artifact separation,
  reproducible generation, consumer-style compilation, local Maven publication
  layout, reusable generator module compatibility, Gradle build-logic checks,
  and public OpenAPI Gradle task name compatibility.
- `documentationCheck` validates the public API inventory, Markdown structure,
  relative links and anchors,
  canonical documentation coverage, changelog/release docs, framework version
  references, OpenAPI name normalization and `info.title`/`info.version`,
  OpenAPI spec version catalog, generated OpenAPI metadata, models JARs, server
  API JARs, client JARs, breaking change detection, advanced OpenAPI model generation, reproducible OpenAPI
  generation, generated OpenAPI compilation tests, generated property
  references, and example documentation/configuration for accidentally committed
  secrets or encoded keystore material. It is part of the root `check`, `baseline`, and
  `compatibilityCheck` flows.
- Starter BOM imports and standalone example Spring Boot plugins must read the
  same `springBootVersion` property from the root `gradle.properties`.
- The REST Client starter must remain compatible with Spring Boot OAuth2 Client
  auto-configuration creating the default `OAuth2AuthorizedClientService`. The
  starter applies its token-cache policy by wrapping the available service.
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
Generated OpenAPI artifacts are Maven-published under `com.smbtech.openapi`
from the root project, using `info.version` as the artifact version.
OpenAPI spec content for a given `info.title` and `info.version` must match
`docs/openapi/spec-versions.properties`; changing the contract requires a new
spec version and catalog update.
Generated OpenAPI JARs must use stable entry ordering and fixed entry
modification times so repeated generation from the same inputs produces the same
artifact hash.
Generated OpenAPI `models`, `api`, and `client` artifacts must compile together
from a consumer-style source set before they are considered valid.
Generated OpenAPI artifacts must also pass `openApiCompatibilityCheck` before a
release can be considered compatible with generated contract consumers.
Spring MVC implementations can be checked at test time with
`OpenApiMvcContractTester`; this verification does not change application
runtime behavior.
