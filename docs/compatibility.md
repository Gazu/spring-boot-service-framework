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

- Version `0.x` can evolve, but incompatible changes must be recorded in the
  affected module documentation or release notes.
- `StructuredEvent`, `StructuredLogger`, and `StructuredLoggerFactory` are the
  recommended APIs for new logging consumers.
- Properties under `smbtech.logging` are part of the logging starter contract.
- Properties under `smbtech.rest-clients` are part of the REST Client starter
  contract.
- A Java or Spring Boot major-version change requires updating this matrix and
  running `compatibilityCheck` successfully.
- `compatibilityCheck` runs the framework module baseline and standalone
  consumer smoke tests against published artifacts under each module's
  `build/repository`.
- `documentationCheck` validates relative Markdown links and scans example
  documentation/configuration for accidentally committed secrets or encoded
  keystore material. It is part of the root `check`, `baseline`, and
  `compatibilityCheck` flows.
- Starter BOM imports and standalone example Spring Boot plugins must read the
  same `springBootVersion` property from the root `gradle.properties`.
- The REST Client starter must remain compatible with Spring Boot OAuth2 Client
  auto-configuration creating the default `OAuth2AuthorizedClientService`. The
  starter applies its token-cache policy by wrapping the available service.

## Current scope

The HTTP correlation adapter supports Spring MVC/Servlet. WebFlux is not
auto-configured until a real consumer defines and tests the desired propagation
semantics.

`ApiClientFactory` uses Spring Web declarative HTTP interfaces (`@HttpExchange`,
`@GetExchange`, etc.) on top of `RestClient`. Automatic interface scanning is not
enabled yet; create proxies explicitly through `ApiClientFactory`.
