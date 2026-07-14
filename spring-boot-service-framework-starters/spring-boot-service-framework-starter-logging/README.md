# Spring Boot Service Framework Logging Starter

Structured JSON logging starter for Spring Boot services on Java 21.

## When to use

Use this module in consuming services. It adapts the framework-neutral logging
core to Spring Boot, SLF4J MDC, Logback, servlet transaction id propagation, and
Spring Boot structured logging configuration.

## What it provides

- JSON log events with `ts`, `uuid`, `type`, `msg`, `class`, `pii`, `thread`,
  `mdc`, `data`, `tags`, and `exception`.
- A structured logging API for application, audit, security, tracking, and
  metric events.
- `X-Transaction-Id` propagation into MDC for servlet applications.
- Logback `AsyncAppender` configuration controlled by properties.
- Spring Boot auto-configuration and configuration metadata.

## Dependency

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-starter-logging:0.2.0'
}
```

No `logback-spring.xml` is required in a consuming service. The starter ships a
default configuration. A service can still replace it by adding its own Logback
configuration.

## Recommended API

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

The temporary `com.smbtech.serviceframework.commons.logging.Logger`
compatibility API remains available during the pre-1.0 development cycle.

## Transaction id and MDC

For servlet applications, the starter registers `TransactionalIdFilter` when
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

## Dummy endpoint example

See [../../examples/logging-consumer](../../examples/logging-consumer) for a
working consumer application with a `/api/dummy` endpoint. Its test validates
that logs include:

- `mdc.transactionId` from `X-Transaction-Id`;
- `mdc.traceId`;
- `mdc.spanId`.

Manual call:

```bash
curl -i -H 'X-Transaction-Id: tx-demo-001' http://localhost:8080/api/dummy
```

Expected log shape:

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

## Configuration

```yaml
smbtech:
  logging:
    production: false
    level: INFO
    async:
      enabled: true
      queue-size: 2048
      discarding-threshold: 0
      never-block: false
      max-flush-time-ms: 1000
    transaction:
      enabled: true
      header-name: X-Transaction-Id
      accept-incoming: true
      max-length: 128
```

## Property reference

All properties are under `smbtech.logging`.

| Property | Default | Description |
|---|---:|---|
| `production` | `false` | Enables production filtering behavior. When `true`, metric events are disabled; audit, security, and tracking events remain enabled. |
| `level` | `INFO` | Root logging level used by the starter Logback configuration. |
| `async.enabled` | `true` | Enables Logback async console logging. |
| `async.queue-size` | `2048` | Async appender queue size. |
| `async.discarding-threshold` | `0` | Logback async discarding threshold. |
| `async.never-block` | `false` | Whether appenders should avoid blocking when the async queue is full. |
| `async.max-flush-time-ms` | `1000` | Maximum async appender flush time during shutdown. |
| `transaction.enabled` | `true` | Registers the servlet transaction id filter when servlet APIs are present. |
| `transaction.header-name` | `X-Transaction-Id` | Request and response header used for transaction id propagation. |
| `transaction.accept-incoming` | `true` | Accepts a valid incoming transaction id. If disabled, always generates a new UUID. |
| `transaction.max-length` | `128` | Maximum accepted incoming transaction id length. |

## Local validation

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-logging:check
./gradlew loggingConsumerSmoke
./gradlew consumerSmoke
```
