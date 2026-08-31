# Spring Boot Service Framework Error Handling Starter

Spring Boot module boundary for reusable exception resolution, safe
`Notification` HTTP responses, structured logging, and error metrics.
The artifact and its auto-configuration identify the starter; package
responsibilities are documented by `package-info.java` instead of a Java marker
type.

## When to use

Use this starter in Spring Boot web applications that need consistent error
handling. Use `spring-boot-service-framework-error-core` directly only for
framework-neutral contracts or custom adapters.

## Dependency

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.0'
    )
    implementation 'com.smbtech:spring-boot-service-framework-starter-error-handling'
}
```

## Public API

Supported starter contracts live in
`com.smbtech.serviceframework.starter.errorhandling.api` and its descendants.
They include resolvers, response factories, status policies, serializers,
reporters, customizers, metrics contracts, and the security extension API.

The starter exposes the supported error-core and commons notification contracts
through its dependencies. Classes under `adapter`, `autoconfigure`,
`serialization`, and the starter root package are framework implementation or
infrastructure, even when technical wiring requires them to be public. See
[Error Handling Extension Points](../../docs/error-handling-extension-points.md)
and [Public API Boundaries](../../docs/public-api-boundaries.md).

## What this module does not do

- It does not contain application-specific error catalogs.
- It does not expose internal diagnostics by default.
- It does not provide reactive WebFlux exception handling.

## Response Exposure

The starter defaults to `smbtech.error-handling.response.exposure=PUBLIC`.
`PUBLIC` preserves the stable code but returns a generic message and only
category plus optional correlation metadata. Configure `INTERNAL` explicitly
for trusted consumers that require resolved sanitized messages, field
violations, and allowlisted metadata. Diagnostics, causes, stack traces, and
secrets are never response fields in either mode.

See the canonical [Error Handling Guide](../../docs/error-handling.md) for the
complete comparison and configuration contract.

## Main documentation

| Topic | Document |
|---|---|
| Usage and behavior | [Error Handling Guide](../../docs/error-handling.md) |
| JSON response contract | [Snake-case JSON Contract](../../docs/error-handling/json-contract.md) |
| Spring Security and OAuth2 errors | [Security Error Handling](../../docs/error-handling/security.md) |
| Configuration | [Property Reference](../../docs/error-handling/property-reference.md) |
| Extension points | [Error Handling Extension Points](../../docs/error-handling-extension-points.md) |
| Migration | [Migrate From shared/exception](../../docs/guides/migrate-shared-exception.md) |
| Renamed types and properties | [Migration Guide](../../docs/guides/migrate-public-names-and-properties.md) |
| Complete consumer example | [Error Handling Consumer](../../examples/error-handling-consumer/README.md) |
| Error core | [Error Core README](../../spring-boot-service-framework-error-core/README.md) |
| Shared notifications | [Commons README](../../spring-boot-service-framework-commons/README.md) |
| Troubleshooting | [Troubleshooting](../../docs/troubleshooting.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-error-handling:check
./gradlew errorHandlingCompatibilityCheck
```
