# Logging Guide

This guide is the canonical reference for structured logging in Spring Boot
Service Framework.

Use `spring-boot-service-framework-starter-logging` in Spring Boot applications.
Use `spring-boot-service-framework-logging-core` directly only when building
framework adapters, tests, or runtime integrations that must remain independent
from Spring Boot, SLF4J, Logback, Servlet APIs, and MDC.

## Module Responsibilities

| Module | Responsibility | Does not own |
|---|---|---|
| `spring-boot-service-framework-logging-core` | Framework-neutral logging domain, input ports, output ports, and core logging policy. | SLF4J, Logback, Spring Boot, servlet filters, MDC, JSON formatting. |
| `spring-boot-service-framework-starter-logging` | Spring Boot auto-configuration, structured JSON output, MDC integration, servlet transaction id propagation, and Logback defaults. | Business-specific logging policy or application domain events. |

Most services should depend on the starter:

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.2'
    )
    implementation 'com.smbtech:spring-boot-service-framework-starter-logging'
}
```

Framework-level code can depend on the core:

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.2'
    )
    implementation 'com.smbtech:spring-boot-service-framework-logging-core'
}
```

## Structured Logging API

Application code should use `StructuredLoggerFactory` and `StructuredLogger`.

```java
import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import org.springframework.stereotype.Service;

@Service
class ProjectService {

    private final StructuredLogger log;

    ProjectService(StructuredLoggerFactory factory) {
        this.log = factory.get(ProjectService.class);
    }

    void update(long projectId) {
        log.info(StructuredEvent.builder(EventType.AUDIT)
                .message("Project {} updated", projectId)
                .with("projectId", projectId)
                .tag("PROJECT")
                .build());
    }
}
```

`StructuredLogger` exposes level-specific helpers:

- `trace(...)`
- `debug(...)`
- `info(...)`
- `warn(...)`
- `error(...)`

Consumers that referenced the former filter or Logback formatter names should
follow [Migrate Public Names And Properties](guides/migrate-public-names-and-properties.md).
Application logging should depend on `StructuredLogger` rather than starter
implementation classes.

## Structured Events

`StructuredEvent` is the immutable payload passed through the logging ports.
Nested maps, collections, and arrays in arguments or structured data are copied
recursively when the event is built.

```java
StructuredEvent event = StructuredEvent.builder(EventType.AUDIT)
        .message("Payment {} approved", "pay-123")
        .with("paymentId", "pay-123")
        .with("amount", 12990)
        .tag("PAYMENTS")
        .build();
```

The builder supports:

- `message(String, Object...)` for message templates and arguments;
- `with(String, Object)` for structured fields;
- `with(String, Consumer<Map<String, Object>>)` for nested structured data;
- `tag(String)` for classification tags;
- `sensitive()` for sensitive events;
- `throwable(Throwable)` for failures.

Sensitive events are represented by `Sensitivity.SENSITIVE`. Adapters decide
how to render, filter, or route them.

## JSON Log Shape

The starter emits structured JSON events with the following fields:

- `ts`
- `uuid`
- `type`
- `msg`
- `class`
- `pii`
- `thread`
- `mdc`
- `data`
- `tags`
- `exception`

Example event:

```json
{
  "type": "INFO",
  "msg": "Dummy endpoint invoked",
  "mdc": {
    "transactionId": "tx-demo-001",
    "traceId": "6a4d37b5257fd08aea71c8ce29eeda80",
    "spanId": "ea71c8ce29eeda80"
  },
  "data": {
    "transactionId": "tx-demo-001"
  },
  "tags": ["DUMMY"]
}
```

No `logback-spring.xml` is required in a consuming service. The starter ships a
default configuration. A service can still replace it by adding its own Logback
configuration. Reusable fragments allow the service to replace only the output
destination while retaining framework properties, saturation policies,
critical-event protection, metrics, and shutdown behavior. See
[Extensible Logback Configuration](logging/async-appender.md#extensible-logback-configuration).
Supported properties, runtime names, fragment paths, legacy precedence, and
change rules are defined in
[Logging Compatibility](logging/compatibility.md).

## Transaction Id And MDC

For servlet applications, the starter registers `TransactionIdFilter` when
`smbtech.logging.transaction.enabled=true`.

Behavior:

- reads the configured transaction header when `accept-incoming=true`;
- validates length and allowed characters;
- generates a UUID when the incoming value is missing or invalid;
- writes the final value to MDC as `transactionId`;
- returns the final value in the configured response header.

Allowed transaction id characters are letters, digits, `-`, `_`, `.`, and `:`.

The formatter copies the complete current SLF4J MDC map into the JSON `mdc`
field. If the service also uses Micrometer Tracing or another tracing bridge
that writes `traceId` and `spanId` to MDC, those values appear automatically in
the emitted JSON.

## Core Ports And Policy

The core module contains:

- immutable domain objects: `StructuredEvent`, `EventType`, `LogLevel`, and
  `Sensitivity`;
- input port: `StructuredLogger`;
- output ports: `LogEventSink` and `CorrelationContext`.

Adapters implement `LogEventSink` to send events to a concrete backend:

```java
final class ConsoleSink implements LogEventSink {

    @Override
    public boolean isEnabled(LogLevel level, EventType eventType) {
        return true;
    }

    @Override
    public void write(LogLevel level, StructuredEvent event) {
        System.out.println(level + " " + event.type().value() + " " + event.message());
    }
}
```

Create the default policy implementation with
`StructuredLogger.create(sink, production)`. Its concrete class is internal.
When `production=true`, metric events are disabled by the core policy; audit,
security, and tracking events remain enabled.

`CorrelationContext` is an outbound port for transport-independent correlation
values:

```java
try (CorrelationContext.Scope scope = correlationContext.open(Map.of(
        "transactionId", "tx-001"
))) {
    // adapter-specific context is active inside the scope
}
```

The starter implements this concept with SLF4J MDC and servlet transaction id
propagation.

## Configuration

```yaml
smbtech:
  logging:
    production: false
    level: INFO
    async:
      enabled: true
      queue-size: 2048
      saturation-policy: BLOCK
      critical-event-protection-enabled: true
      discarding-threshold: 0
      never-block: false
      max-flush-time-ms: 1000
    transaction:
      enabled: true
      header-name: X-Transaction-Id
      accept-incoming: true
      max-length: 128
```

The defaults, queue saturation behavior, critical-event limitations, sizing
guidance, shutdown behavior, and local performance measurement are defined in
the [Async Appender Contract](logging/async-appender.md).

## Property Reference

All `smbtech.logging` properties are documented in the generated
[Logging Property Reference](logging/property-reference.md).

## Consumer Example

The standalone example lives in
[examples/logging-consumer](../examples/logging-consumer/README.md). It
consumes published local artifacts instead of Gradle `project(...)`
dependencies.

From the framework root:

```bash
./gradlew loggingConsumerSmoke
```

Manual run:

```bash
./gradlew publishLocalArtifacts
cd examples/logging-consumer
../../gradlew bootRun
```

Manual call:

```bash
curl -i -H 'X-Transaction-Id: tx-demo-001' http://localhost:8080/api/dummy
```

The example verifies that the response includes `transactionId`, `traceId`, and
`spanId`, and that emitted JSON logs include the same identifiers in `mdc` and
`data`.

It also includes a bounded workload endpoint that exercises the real async
appender with normal and protected audit events:

```bash
curl -sS -X POST \
  -H 'X-Transaction-Id: tx-async-001' \
  'http://localhost:8080/api/logging/async?events=1000&critical-every=100'
```

Use `/actuator/metrics/smbtech.logging.async.queue.depth` and the other metrics
listed in the async appender contract to inspect its runtime behavior.

## Boundary Rules

`spring-boot-service-framework-logging-core` depends only on the JDK. The
`verifyHexagonalBoundaries` task rejects imports from Spring, SLF4J, Logback,
Servlet, and Jackson.

Keep new runtime-specific behavior in the starter. Keep the core focused on:

- event modeling;
- logging policy decisions;
- input and output ports;
- small domain helpers that do not require a framework.

## Local Validation

```bash
./gradlew :spring-boot-service-framework-logging-core:check
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-logging:check
./gradlew loggingCompatibilityCheck
./gradlew loggingConsumerSmoke
./gradlew consumerSmoke
```
