# Spring Boot Service Framework Actuator Core

Framework-neutral domain, ports, and services for bounded component diagnostics
and framework module information.

## When to use

Applications will normally consume
`spring-boot-service-framework-starter-actuator`. Use this module directly when
implementing framework-neutral diagnostic probes or adapters that must not
depend on Spring Boot Actuator.

## Dependency

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.1'
    )
    implementation 'com.smbtech:spring-boot-service-framework-actuator-core'
}
```

## Public API

| Type | Responsibility |
|---|---|
| `ComponentStatus` | Stable `UP`, `DOWN`, `OUT_OF_SERVICE`, and `UNKNOWN` model. |
| `ComponentHealth` | Immutable, bounded, and sanitized probe result. |
| `FrameworkDiagnosticsSnapshot` | Deterministically ordered point-in-time component state. |
| `FrameworkModuleInfo` | Immutable, non-sensitive framework module information. |
| `DiagnosticProbe` | Outbound extension port for application and module checks. |
| `FrameworkModuleInfoProvider` | Outbound extension port for module information. |
| `FrameworkDiagnostics` | Inbound snapshot and module information contract. |

Use `FrameworkDiagnostics.from(...)` when the default neutral aggregator is
needed outside the starter. Its implementation is internal; consumers depend
on the `FrameworkDiagnostics` port.

## Probe example

```java
import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import java.util.Map;

public final class CacheDiagnosticProbe implements DiagnosticProbe {

    @Override
    public String componentName() {
        return "cache";
    }

    @Override
    public ComponentHealth check() {
        return ComponentHealth.up(
                componentName(),
                Map.of("configured", true, "entries", 12)
        );
    }
}
```

Probe names must be unique and already normalized. Probe failures, `null`
results, and mismatched component names become `UNKNOWN` results without
leaking exception details.

Diagnostic detail maps:

- are recursively immutable and deterministically ordered;
- accept only structured values and safe scalar Java types;
- reject cycles, unsupported values, strings longer than 2,048 characters,
  nesting deeper than eight levels, and containers larger than 64 entries;
- redact credential, token, secret, scope, keystore, truststore, cookie,
  authorization, URL, and URI values.

## What this module does not do

- It does not depend on Spring Boot, Spring, Micrometer, SLF4J, Servlet,
  Jackson, or Apache HttpClient.
- It does not expose HTTP or JMX endpoints.
- It does not perform active external checks.
- It does not own endpoint security, exposure, or health groups.
- It does not serialize or publish diagnostic values.

## Main documentation

| Topic | Document |
|---|---|
| Architecture and safety contract | [Actuator Architecture Contract](../docs/actuator.md) |
| Supported API and changes | [Actuator Compatibility](../docs/actuator/compatibility.md) |
| Dependency management | [Dependency Management](../docs/dependency-management.md) |
| Compatibility policy | [Compatibility](../docs/compatibility.md) |
| Module README rules | [Module README Convention](../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-actuator-core:check
./gradlew actuatorContractCheck
./gradlew actuatorCompatibilityCheck
```
