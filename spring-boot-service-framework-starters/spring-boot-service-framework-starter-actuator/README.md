# Spring Boot Service Framework Actuator Starter

Spring Boot adapter that auto-configures framework-neutral diagnostics, health,
application information, bounded metrics, and a read-only diagnostic endpoint.

## When to use

Use this starter in Spring Boot services that need framework health, info,
diagnostic endpoint, and metrics integration through Spring Boot Actuator.

## Dependency

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.4.0'
    )
    implementation 'com.smbtech:spring-boot-service-framework-starter-actuator'
}
```

## Public API

The starter exposes the neutral domain and extension ports from
`spring-boot-service-framework-actuator-core` and registers
`ActuatorAutoConfiguration`.

The base auto-configuration:

- creates one `FrameworkDiagnostics` bean;
- discovers every application `DiagnosticProbe` and
  `FrameworkModuleInfoProvider`;
- registers the named `serviceFramework` health contributor;
- contributes module information under the `serviceFramework` info key;
- provides the disabled-by-default `serviceframework` diagnostic endpoint;
- detects logging, REST client, mock, and error handling starters without
  publishing them transitively;
- registers bounded-cardinality framework gauges when a `MeterRegistry` is
  available;
- uses a unique application `Clock` when available;
- backs off for an application-provided `FrameworkDiagnostics`;
- can be disabled with `smbtech.actuator.enabled=false`.

Spring Boot Actuator and `spring-boot-service-framework-actuator-core` are
transitive API dependencies. REST client and mock integrations remain optional
and are not published transitively.

## Optional integrations

| Starter | Health | Info |
|---|---|---|
| REST client | Passive client and resilience counts | Module version and bounded counts |
| Mock | None | Module version and endpoint/contract counts |
| Logging | None | Module version and feature state |
| Error handling | None | Module version and feature state |

The integrations do not execute network calls, construct REST clients, load
mock content, or expose names, URLs, credentials, headers, scopes, resource
locations, response bodies, or exception details.

## Metrics

Metrics are enabled by default when a `MeterRegistry` is available:

| Metric | Tags | Value |
|---|---|---|
| `smbtech.service.framework.status` | `status=up|down|out_of_service|unknown` | One-hot aggregate status (`1` for the current status, otherwise `0`) |
| `smbtech.service.framework.components` | `status=up|down|out_of_service|unknown` | Component count for that status |
| `smbtech.service.framework.modules` | None | Detected framework module count |

Component and module names are never used as metric tags. One diagnostic sample
is shared across gauge reads for 10 seconds by default.

```yaml
smbtech:
  actuator:
    metrics:
      enabled: true
      cache-ttl: 10s
```

Disable only these gauges with
`smbtech.actuator.metrics.enabled=false`. Spring Boot `management.*`
configuration remains responsible for exposing a metrics endpoint or exporter.

## Security and performance

The default diagnostics service applies a shared safety and performance guard:

- independently cached snapshot and module results;
- single-flight refreshes for concurrent readers;
- a two-worker daemon executor with no task queue;
- a timeout for each complete diagnostics operation;
- hard payload limits of at most 256 components and 256 modules;
- static safe fallback reasons without exception data.

```yaml
smbtech:
  actuator:
    diagnostics:
      cache-ttl: 5s
      operation-timeout: 2s
      max-components: 64
      max-modules: 64
```

The custom endpoint remains `Access.NONE` by default. The starter does not
create a security filter chain; the consuming application must authorize any
Actuator endpoint it exposes.

## Diagnostic endpoint

The endpoint is disabled and not exposed by default. Enable both access and web
exposure in the consuming application:

```yaml
management:
  endpoint:
    serviceframework:
      access: read-only
  endpoints:
    web:
      exposure:
        include: health,info,serviceframework
```

The endpoint is then available at `/actuator/serviceframework` when the default
Actuator base path is used.

Disable only the info contributor with:

```yaml
management:
  info:
    service-framework:
      enabled: false
```

Disable only the health contributor with:

```yaml
management:
  health:
    service-framework:
      enabled: false
```

## What this module does not do

- It does not create MVC or WebFlux controllers.
- It does not configure a Spring Security filter chain.
- It does not expose management endpoints or add health groups automatically.
- It does not perform active external checks.

## Main documentation

| Topic | Document |
|---|---|
| Architecture and safety contract | [Actuator Architecture Contract](../../docs/actuator.md) |
| Supported API and changes | [Actuator Compatibility](../../docs/actuator/compatibility.md) |
| Configuration properties | [Actuator Property Reference](../../docs/actuator/property-reference.md) |
| Standalone consumer | [Actuator Consumer Example](../../examples/actuator-consumer/README.md) |
| Dependency management | [Dependency Management](../../docs/dependency-management.md) |
| Compatibility policy | [Compatibility](../../docs/compatibility.md) |
| Module README rules | [Module README Convention](../../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-actuator:check
./gradlew actuatorContractCheck
./gradlew actuatorCompatibilityCheck
./gradlew actuatorConsumerSmoke
```
