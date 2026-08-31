# Logging Compatibility

This document defines the supported compatibility surface of
`spring-boot-service-framework-logging-core` and
`spring-boot-service-framework-starter-logging`.

## Supported Runtime

The repository-wide Java, Spring Boot, SLF4J, Logback, Gradle, and native-image
versions are defined in [Compatibility](../compatibility.md). Import the
framework platform so logging core, the starter, and Spring Boot remain aligned.

## Compatibility Surface

| Surface | Compatibility promise |
|---|---|
| Maven coordinates | `com.smbtech:spring-boot-service-framework-logging-core` and `com.smbtech:spring-boot-service-framework-starter-logging` remain the supported coordinates. |
| Neutral API | Public logging domain types and ports listed in the generated logging module contract remain reviewed source and binary contracts. |
| Properties | Every property in the [Logging Property Reference](property-reference.md), including its type and default, is reviewed for compatibility. |
| Auto-configuration | Logging auto-configuration imports are protected by the generated module contract. |
| Default topology | `logback-spring.xml` continues to provide structured `STDOUT`, policy-aware `ASYNC`, and root routing without application configuration. |
| Logback fragments | The four documented classpath fragments remain supported composition resources. |
| Delegate selection | `SERVICE_FRAMEWORK_LOGGING_DELEGATE` selects one application-defined destination and defaults to `STDOUT`. |
| Runtime behavior | Saturation policies, critical classification, shutdown semantics, and bounded metric names are protected by the async logging contract. |
| Consumer behavior | `loggingConsumerSmoke` validates published Maven artifacts, HTTP logging, tracing, metrics, and Spring AOT. |

`StructuredLoggers`, the logging-core domain, and logging-core ports are the
supported application-facing Java surface. The formatter and async appender
remain public only because Logback constructs them reflectively from XML.
Servlet, MDC, metrics, and SLF4J adapters are internal.

## Stable Logback Resources

The following resources are additive extension points:

```text
logback-spring.xml
com/smbtech/serviceframework/starter/logging/logback/properties.xml
com/smbtech/serviceframework/starter/logging/logback/structured-console-appender.xml
com/smbtech/serviceframework/starter/logging/logback/async-appender.xml
com/smbtech/serviceframework/starter/logging/logback/root.xml
```

The default appender names remain `STDOUT` and `ASYNC`. A consumer can replace
`STDOUT` by defining an appender and setting
`SERVICE_FRAMEWORK_LOGGING_DELEGATE` before including `async-appender.xml` and
`root.xml`. The complete ordering and XML example are defined in
[Extensible Logback Configuration](async-appender.md#extensible-logback-configuration).

Adding a new optional fragment is compatible. Removing or renaming one of these
resources, changing its required inclusion order, changing the delegate
property, or changing the default appender names is incompatible.

## Legacy Property Precedence

The explicit `saturation-policy` property is preferred for new applications.
The former low-level properties remain supported:

| Configuration | Effective behavior |
|---|---|
| `never-block=true` | `DROP_WHEN_FULL`, regardless of `saturation-policy`. |
| `saturation-policy=BLOCK` and `discarding-threshold>0` | `DISCARD_LOW_PRIORITY` using the configured threshold. |
| `saturation-policy=BLOCK`, `never-block=false`, and `discarding-threshold=0` | Lossless producer blocking. |
| Explicit `DISCARD_LOW_PRIORITY` | Uses the configured positive threshold or derives 20 percent of queue size. |
| Explicit `DROP_WHEN_FULL` | Non-blocking full-queue behavior; critical protection still applies when enabled. |

Changing this precedence requires an incompatible-change review. Deprecating a
legacy property requires release notes and a migration period before removal.

## Compatible Changes

The following changes are normally compatible:

- adding an optional metric with bounded tags and documenting it;
- adding an optional Logback fragment without changing existing resources;
- improving internal queue accounting, validation messages, or shutdown
  coordination without changing the documented behavior;
- adding optional structured JSON fields while preserving existing field names;
- adding an auto-configured bean with complete backoff behavior.

## Incompatible Changes

The following changes require explicit review and release notes:

- removing or changing a supported logging-core type, record component, or port;
- renaming a property or changing its type, default, accepted range, precedence,
  or meaning;
- changing `STDOUT`, `ASYNC`, `SERVICE_FRAMEWORK_LOGGING_DELEGATE`, a stable
  fragment path, a metric name, or a bounded tag value;
- weakening critical-event protection or changing shutdown admission and drain
  semantics;
- changing the default JSON field names or removing a field;
- making Micrometer, Actuator, tracing, servlet, or another optional integration
  mandatory.

During `0.x`, an approved incompatible change must be documented in
`CHANGELOG.md` and in the repository compatibility records before release.

## Validation

Run the focused compatibility lifecycle:

```bash
./gradlew loggingCompatibilityCheck
```

It validates:

- the generated `gradle/compatibility/contracts/logging.txt` baseline;
- logging core and starter tests and coverage gates;
- async topology, defaults, policies, concurrency, metrics, shutdown, and
  extension fragments;
- documentation structure, links, and property references;
- the `loggingConsumerSmoke` published-artifact HTTP and AOT application.

When a reviewed compatibility surface changes:

```bash
./gradlew generateModuleCompatibilityContracts
./gradlew loggingCompatibilityCheck
```

Review the complete generated contract diff before committing it.
