# Logging consumer example

Standalone Spring Boot application that imports
`com.smbtech:spring-boot-service-framework-platform:0.5.0` and consumes
`com.smbtech:spring-boot-service-framework-starter-logging` without an
individual version from the local Maven repositories generated under each
module `build/repository` directory.

The example intentionally consumes published local artifacts instead of Gradle
`project(...)` dependencies. This validates the same POMs and JARs that another
repository would consume.

For the canonical guides, see
[Dependency Management](../../docs/dependency-management.md) and
[Logging](../../docs/logging.md).

From the framework root:

```bash
./gradlew loggingConsumerSmoke
```

It includes an HTTP endpoint instrumented with Micrometer Tracing and Brave:

```bash
./gradlew publishLocalArtifacts
cd examples/logging-consumer
../../gradlew bootRun
```

In another terminal:

```bash
curl -i -H 'X-Transaction-Id: tx-demo-001' http://localhost:8080/api/dummy
```

The response contains `transactionId`, `traceId`, and `spanId`. The JSON log for
`Dummy endpoint invoked` contains the same identifiers in `mdc` and `data`.
Sampling is set to 100% only to keep the example deterministic.

The example relies on the auto-configured `TransactionIdFilter` and bundled
`ServiceFrameworkStructuredLogFormatter`; application code does not import
either implementation class. Consumers with a custom Logback configuration
should follow the
[names and properties migration guide](../../docs/guides/migrate-public-names-and-properties.md).

The application also provides a bounded async logging scenario:

```bash
curl -sS -X POST \
  -H 'X-Transaction-Id: tx-async-001' \
  'http://localhost:8080/api/logging/async?events=1000&critical-every=100'
```

The response reports attempted emissions. It does not claim that non-critical
events were delivered because the configured saturation policy can discard
them. Every `critical-every` event is emitted as `AUDIT`; the remaining events
are `APPLICATION`.

Inspect the live appender metrics with:

```bash
curl -sS \
  http://localhost:8080/actuator/metrics/smbtech.logging.async.queue.depth
curl -sS \
  http://localhost:8080/actuator/metrics/smbtech.logging.async.events.discarded
curl -sS \
  http://localhost:8080/actuator/metrics/smbtech.logging.async.critical.fallbacks
```

## What it demonstrates

- `spring-boot-service-framework-starter-logging` auto-configuration.
- Structured JSON logging through the framework logging API.
- `X-Transaction-Id` propagation into MDC and the HTTP response.
- Micrometer tracing values (`traceId` and `spanId`) included through MDC.
- Real asynchronous delivery through `PolicyAwareAsyncAppender`.
- Mixed `APPLICATION` and protected `AUDIT` events.
- Queue, discard, fallback, producer-block, and shutdown metrics through
  Actuator.

## Important configuration

```yaml
smbtech:
  logging:
    production: false
    level: INFO
    async:
      enabled: ${EXAMPLE_ASYNC_LOGGING_ENABLED:true}
      queue-size: ${EXAMPLE_ASYNC_LOGGING_QUEUE_SIZE:2048}
      saturation-policy: ${EXAMPLE_ASYNC_LOGGING_SATURATION_POLICY:BLOCK}
      critical-event-protection-enabled: ${EXAMPLE_ASYNC_LOGGING_CRITICAL_PROTECTION:true}
      discarding-threshold: ${EXAMPLE_ASYNC_LOGGING_DISCARDING_THRESHOLD:0}
      max-flush-time-ms: ${EXAMPLE_ASYNC_LOGGING_MAX_FLUSH_TIME_MS:1000}
      observability:
        enabled: true
    transaction:
      enabled: true
```

Tracing is enabled with 100% sampling only for the example. Production services
should choose sampling and async logging settings according to their runtime
needs.

To exercise another policy without changing source:

```bash
EXAMPLE_ASYNC_LOGGING_QUEUE_SIZE=256 \
EXAMPLE_ASYNC_LOGGING_SATURATION_POLICY=DROP_WHEN_FULL \
../../gradlew bootRun
```

Use `BLOCK`, `DISCARD_LOW_PRIORITY`, or `DROP_WHEN_FULL`. Queue size must remain
between `256` and `65536`.

## Tests

The smoke tests verify that:

- the published starter artifacts can be consumed;
- `/api/dummy` is available without additional application logging setup;
- the response includes the transaction id;
- emitted JSON logs include transaction, trace, and span identifiers.
- `/api/logging/async` emits mixed events through the real async appender;
- async appender metrics are registered and exposed by Actuator.
