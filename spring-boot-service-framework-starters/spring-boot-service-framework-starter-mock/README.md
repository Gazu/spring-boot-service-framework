# Spring Boot Service Framework Mock Starter

Spring Boot starter for configured mock responses loaded from classpath or file
resources. It adapts `spring-boot-service-framework-mock-core` to Spring Boot,
Jackson, Spring MVC `ResponseEntity`, and opt-in Spring `RestClient`
interceptors.

## When to use

Use this starter when a Spring Boot service needs configured mock responses for:

- controllers;
- integration tests;
- local development;
- contract exploration;
- outbound `RestClient` calls.

Use `spring-boot-service-framework-mock-core` directly only when building a
framework-neutral adapter or test helper.

## Dependency

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.4.0'
    )
    implementation 'com.smbtech:spring-boot-service-framework-starter-mock'
}
```

## Quick start

```yaml
smbtech:
  mocks:
    endpoints:
      payments-success:
        enabled: true
        file: classpath:mocks/payments-success.json
```

## Public API

- `com.smbtech.serviceframework.starter.mock.api.MockService`
- Core `domain` and `port.*` contracts exposed through the `mock-core`
  dependency.

`MockRestClientInterceptor`, mock adapters, `MockAutoConfiguration`, and
`MockProperties` are framework implementation or infrastructure. See
[Public API Boundaries](../../docs/public-api-boundaries.md).

## What this module does not do

- It does not automatically replace outbound HTTP calls.
- It does not put Spring, Jackson, Servlet, or `RestClient` APIs into
  `mock-core`.
- It does not store business-specific mock scenarios in the framework.

## Main documentation

| Topic | Document |
|---|---|
| Mock guide | [Mock Core and Starter](../../docs/mock.md) |
| Mock property reference | [Mock Property Reference](../../docs/mock/property-reference.md) |
| Names and packages migration | [Migration Guide](../../docs/guides/migrate-public-names-and-properties.md) |
| Troubleshooting | [Troubleshooting](../../docs/troubleshooting.md#mock) |
| Mock core README | [Mock Core README](../../spring-boot-service-framework-mock-core/README.md) |
| Module README rules | [Module README Convention](../../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-mock:check
./gradlew :spring-boot-service-framework-mock-core:check
./gradlew mockCompatibilityCheck
```
