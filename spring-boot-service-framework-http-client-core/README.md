# Spring Boot Service Framework HTTP Client Core

Framework-neutral HTTP client core for client definitions, authentication
models, resilience policy, audit metadata, notifications, ports, and
inspectable downstream exceptions.

Runtime integration belongs in
`spring-boot-service-framework-starter-rest-client`.

## When to use

Most applications should consume
`spring-boot-service-framework-starter-rest-client`.

Use this module directly when building framework adapters, tests, or
integrations that should use the neutral HTTP client model without depending on
Spring, Apache HttpClient, Micrometer, Jackson, or Spring Security.

## Dependency

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-http-client-core:0.3.0'
}
```

## Public API

- HTTP client domain objects such as `HttpClientDefinition`,
  `BasicAuthentication`, `AccessToken`, `GrantType`, and `ResiliencePolicy`.
- Error response types such as `HttpErrorResponse`,
  `HttpClientResponseException`, and JSON error response decoding helpers.
- Ports such as `AccessTokenProvider`, `HttpExchangeAuditSink`, and
  `HttpErrorResponseBodyReader`.
- Core exceptions such as `HttpClientAuthenticationException`,
  `HttpClientConfigurationException`, and `CircuitBreakerOpenException`.

## What this module does not do

- It does not create Spring `RestClient` beans.
- It does not perform OAuth2 token exchange by itself.
- It does not configure Apache HttpClient, SSL, Micrometer, retry interceptors,
  or Spring Security.
- It does not depend on Spring, Jackson, Servlet APIs, Apache, or Micrometer.

## Main documentation

| Topic | Document |
|---|---|
| REST client starter guide | [REST Client Starter Guide](../docs/rest-client.md) |
| Error handling | [Error Handling](../docs/rest-client/error-handling.md) |
| Exception selection | [Exception Selection](../docs/error-handling/exception-selection.md) |
| Extension points | [REST Client Extension Points](../docs/rest-client-extension-points.md) |
| Names and properties migration | [Migration Guide](../docs/guides/migrate-public-names-and-properties.md) |
| REST client starter README | [REST Client Starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-rest-client/README.md) |
| Module README rules | [Module README Convention](../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-http-client-core:check
./gradlew httpClientCompatibilityCheck
```
