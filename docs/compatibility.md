# Compatibility

## Supported matrix

| Component | Supported version | Policy |
|---|---:|---|
| Java | 21 | Framework toolchain and bytecode target |
| Gradle | Wrapper-provided Gradle, currently 9.3.1 in generated reports | Use the repository wrapper when available |
| Spring Boot | 4.0.x | Current BOM: 4.0.4 |
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
  migration guide.
- `StructuredEvent`, `StructuredLogger`, and `StructuredLoggerFactory` are the
  recommended APIs for new logging consumers.
- Properties under `smbtech.logging` are part of the logging starter contract.
- Properties under `smbtech.rest-clients` are part of the REST Client starter
  contract.
- A Java or Spring Boot major-version change requires updating this matrix and
  running `baseline` and `consumerSmoke` successfully.

## Current scope

The HTTP correlation adapter supports Spring MVC/Servlet. WebFlux is not
auto-configured until a real consumer defines and tests the desired propagation
semantics.

`ApiClientFactory` uses Spring Web declarative HTTP interfaces (`@HttpExchange`,
`@GetExchange`, etc.) on top of `RestClient`. Automatic interface scanning is not
enabled yet; create proxies explicitly through `ApiClientFactory`.
