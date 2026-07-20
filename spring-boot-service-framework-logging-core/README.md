# spring-boot-service-framework-logging-core

Framework-neutral core for structured logging. It defines the logging domain,
input ports, output ports, and core logging service without depending on SLF4J,
Logback, Servlet APIs, Jackson, or Spring Boot.

Runtime integration belongs in
`spring-boot-service-framework-starter-logging`.

## When to use

Most applications should consume
`spring-boot-service-framework-starter-logging`.

Use this module directly when building framework-level logging adapters, tests,
or runtime integrations that should depend only on the neutral logging model and
ports.

## Dependency

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-logging-core:0.3.0'
}
```

## Public API

- `StructuredEvent`
- `EventType`
- `LogLevel`
- `Sensitivity`
- `StructuredLogger`
- `StructuredLoggerFactory`
- `LogEventSink`
- `CorrelationContext`

Supported contracts live in the `domain` and `port.*` packages.
`StructuredLoggingService` is the framework default implementation and is not a
supported extension point. See
[Public API Boundaries](../docs/public-api-boundaries.md).

## What this module does not do

- It does not write to SLF4J, Logback, files, stdout, or Spring Boot structured
  logging.
- It does not manage MDC or servlet transaction ids.
- It does not provide Spring beans or auto-configuration.
- It does not own application-specific logging policy.

## Main documentation

| Topic | Document |
|---|---|
| Logging guide | [Logging Guide](../docs/logging.md) |
| Logging property reference | [Logging Property Reference](../docs/logging/property-reference.md) |
| Logging starter README | [Logging Starter README](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-logging/README.md) |
| Names and properties migration | [Migration Guide](../docs/guides/migrate-public-names-and-properties.md) |
| Module README rules | [Module README Convention](../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-logging-core:check
./gradlew loggingCompatibilityCheck
```
