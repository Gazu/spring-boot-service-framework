# Actuator Compatibility

This document defines the supported compatibility surface of
`spring-boot-service-framework-actuator-core` and
`spring-boot-service-framework-starter-actuator`.

## Supported Runtime

The repository-wide supported Java, Gradle, Spring Boot, Micrometer, and native
image versions are defined in [Compatibility](../compatibility.md). Consumers
should import `com.smbtech:spring-boot-service-framework-platform` so the core,
starter, and Spring Boot versions remain aligned.

## Compatibility Surface

| Surface | Compatibility promise |
|---|---|
| Maven coordinates | `com.smbtech:spring-boot-service-framework-actuator-core` and `com.smbtech:spring-boot-service-framework-starter-actuator` remain the supported coordinates. |
| Neutral API | Public domain records, `ComponentStatus`, and interfaces under `com.smbtech.serviceframework.actuator.port` are supported source and binary contracts. |
| Extension ports | `FrameworkDiagnostics`, `DiagnosticProbe`, and `FrameworkModuleInfoProvider` remain the application extension boundary. |
| Properties | Every property in the [Actuator Property Reference](property-reference.md), including its type and default, is reviewed for compatibility. |
| Auto-configuration | The six entries in `AutoConfiguration.imports` are protected by the generated module contract. |
| Runtime names | Health, info, endpoint, bean, metric, and status names listed below are stable integration contracts. |
| Concrete adapters | Endpoint, health, info, metrics, guard, and optional-integration implementations are internal. Replace their documented interfaces or bean names. |
| Management ownership | Spring Boot `management.*` and the consuming application continue to own access, exposure, security, health groups, and detail visibility. |
| Consumer behavior | The standalone published-artifact application is exercised by `actuatorConsumerSmoke`, including HTTP security and Spring AOT. |

## Stable Runtime Names

| Concern | Stable value |
|---|---|
| Property prefix | `smbtech.actuator` |
| Health contributor | `serviceFramework` |
| Health indicator bean | `serviceFrameworkHealthIndicator` |
| Info key | `serviceFramework` |
| Info contributor bean | `serviceFrameworkInfoContributor` |
| Diagnostic endpoint id | `serviceframework` |
| Diagnostic endpoint bean | `serviceFrameworkDiagnosticsEndpoint` |
| Diagnostic endpoint default access | `NONE` |
| Metrics binder bean | `serviceFrameworkMetrics` |
| Aggregate status metric | `smbtech.service.framework.status` |
| Component count metric | `smbtech.service.framework.components` |
| Module count metric | `smbtech.service.framework.modules` |
| Metric status tag values | `up`, `down`, `out_of_service`, `unknown` |
| Neutral statuses | `UP`, `DOWN`, `OUT_OF_SERVICE`, `UNKNOWN` |

The framework may add optional, bounded detail fields in a compatible release.
Consumers should evaluate documented fields by name and ignore unknown fields.
Component names and module attributes contributed by optional integrations are
informational and must not be treated as a versioned authorization contract.

## Diagnostic Payload Contract

The `serviceframework` read operation returns these stable top-level fields:

| Field | Type | Notes |
|---|---|---|
| `capturedAt` | ISO-8601 string | Present for a successful snapshot. |
| `status` | string | One of the neutral statuses. |
| `componentCount` | number | Count after configured bounding. |
| `components` | object | Keyed by normalized component name. |
| `moduleCount` | number | Count after configured bounding. |
| `modules` | array | Deterministically ordered module information. |
| `reason` | string | Static safe reason present only for unavailable output. |

Component entries contain `status` and optional sanitized `details`. Module
entries contain `name`, `version`, and optional sanitized `attributes`.
Sensitive values remain redacted. The endpoint remains read-only,
disabled by default, and application-secured.

## Compatible Changes

The following changes are normally compatible:

- adding a new optional integration without making it transitive;
- adding a new sanitized detail or module attribute;
- adding an auto-configured bean with complete backoff behavior;
- improving internal caching, timeout, or failure isolation without changing
  the documented defaults or output contract;
- adding a metric only when its name and bounded tag set are documented.

## Incompatible Changes

The following changes require explicit review and release notes:

- removing or changing a neutral API type, record component, factory, or port
  method;
- renaming a property, changing its type, default, accepted range, or meaning;
- changing a stable runtime name, status, metric, or tag value;
- changing the endpoint id, default access, read-only behavior, or required
  management ownership;
- exposing secrets, raw exception data, unbounded values, or active external
  checks by default;
- making an optional framework starter a transitive dependency.

During `0.x`, an approved incompatible change must be documented in
`CHANGELOG.md` and in the repository compatibility records before release.

## Validation

Run the focused compatibility lifecycle:

```bash
./gradlew actuatorCompatibilityCheck
```

It validates:

- the generated `gradle/compatibility/contracts/actuator.txt` baseline;
- the neutral public API and its explicit method-level compatibility test;
- core and starter behavioral tests and coverage gates;
- architecture, safety, runtime-name, property, endpoint, and metric contracts;
- documentation structure, links, versions, and property references;
- the `actuatorConsumerSmoke` published-artifact HTTP and AOT application.

When a reviewed compatibility surface changes, regenerate the module contract:

```bash
./gradlew generateModuleCompatibilityContracts
./gradlew actuatorCompatibilityCheck
```

Review the complete contract diff before committing it.
