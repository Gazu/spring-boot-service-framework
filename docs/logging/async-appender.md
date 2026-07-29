# Async Appender Contract

This document defines the reviewed baseline for asynchronous logging in
`spring-boot-service-framework-starter-logging`. It describes the current
Logback topology, delivery behavior, operational limits, and verification
commands. It is a contract for later async appender work, not a guarantee of
durable delivery.

## Runtime Topology

The starter packages `logback-spring.xml` with two appenders:

```text
application thread
       |
       v
ASYNC (Logback AsyncAppender, bounded in-memory queue)
       |
       v
STDOUT (Spring Boot StructuredLogEncoder)
       |
       v
ServiceFrameworkStructuredLogFormatter -> JSON console event
```

When `smbtech.logging.async.enabled=true`, the root logger writes through
`ASYNC`. When it is `false`, the root logger writes directly to `STDOUT` on the
application thread.

Async processing changes the execution thread, not the JSON contract. Logback
prepares each event before enqueueing it, so the formatted message, MDC map,
structured event arguments, and exception remain available to the formatter.
The focused tests verify FIFO order, MDC, structured data, exceptions, queue
backpressure, and orderly shutdown.

## Extensible Logback Configuration

The packaged `logback-spring.xml` is composed from four reusable classpath
fragments:

| Fragment | Responsibility |
|---|---|
| `com/smbtech/serviceframework/starter/logging/logback/properties.xml` | Binds `smbtech.logging.*` values to Logback variables. |
| `com/smbtech/serviceframework/starter/logging/logback/structured-console-appender.xml` | Defines the default structured JSON `STDOUT` appender. |
| `com/smbtech/serviceframework/starter/logging/logback/async-appender.xml` | Defines `ASYNC` and its saturation, critical-event, and shutdown behavior. |
| `com/smbtech/serviceframework/starter/logging/logback/root.xml` | Routes the root logger through `ASYNC` or directly to its delegate. |

A service that accepts the default JSON console output does not need a Logback
file. To replace only the destination, add an application-owned
`src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="com/smbtech/serviceframework/starter/logging/logback/properties.xml"/>

    <appender name="CUSTOM_DESTINATION"
              class="ch.qos.logback.core.ConsoleAppender">
        <target>System.err</target>
        <encoder class="org.springframework.boot.logging.logback.StructuredLogEncoder">
            <format>com.smbtech.serviceframework.starter.logging.adapter.out.logback.ServiceFrameworkStructuredLogFormatter</format>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <property name="SERVICE_FRAMEWORK_LOGGING_DELEGATE"
              value="CUSTOM_DESTINATION"/>

    <include resource="com/smbtech/serviceframework/starter/logging/logback/async-appender.xml"/>
    <include resource="com/smbtech/serviceframework/starter/logging/logback/root.xml"/>
</configuration>
```

The order is part of the extension contract:

1. include `properties.xml`;
2. define and start the application-owned destination;
3. set `SERVICE_FRAMEWORK_LOGGING_DELEGATE` to that appender name;
4. include `async-appender.xml`;
5. include `root.xml`, or define an application-owned root logger.

`SERVICE_FRAMEWORK_LOGGING_DELEGATE` defaults to `STDOUT`. When async logging is
enabled, it is the single destination behind `ASYNC`. When async logging is
disabled, `root.xml` attaches it directly to the root logger.

Logback's `AsyncAppender` supports one destination. For fan-out, define
application-owned async appenders and a custom root instead of attaching
multiple destinations to the framework `ASYNC` appender. Keep the
`PolicyAwareAsyncAppender` when saturation policies, critical-event protection,
metrics, and safe shutdown are required.

Applications that need complete control can include only `properties.xml` and
define all appenders and loggers themselves. Omitting `async-appender.xml`
intentionally disables the framework async metrics because no framework
`ASYNC` appender exists.

The fragment resources are registered for native-image resource inclusion.
`ExtensibleLogbackConfigurationTest` verifies that a consumer configuration can
replace `STDOUT` without copying the framework async or root definitions.

## Reviewed Defaults

| Property | Default | Contract |
|---|---:|---|
| `smbtech.logging.async.enabled` | `true` | Uses `ASYNC`; `false` selects synchronous `STDOUT`. |
| `smbtech.logging.async.queue-size` | `2048` | Maximum number of waiting events in the bounded in-memory queue. |
| `smbtech.logging.async.saturation-policy` | `BLOCK` | Explicit queue saturation behavior. |
| `smbtech.logging.async.critical-event-protection-enabled` | `true` | Protects critical levels and structured event types from saturation-based loss. |
| `smbtech.logging.async.discarding-threshold` | `0` | Optional threshold used by `DISCARD_LOW_PRIORITY`. |
| `smbtech.logging.async.never-block` | `false` | Compatibility override; prefer `saturation-policy`. |
| `smbtech.logging.async.max-flush-time-ms` | `1000` | Maximum time Logback waits for queued events during appender shutdown. |
| `smbtech.logging.async.observability.enabled` | `true` | Registers bounded-cardinality async appender metrics when Micrometer is available. |

The Gradle contract validates these defaults against both
`LoggingProperties` and the packaged Logback configuration.

## Queue Saturation Policies

The policy is configured explicitly:

| Policy | Low-priority behavior | Full queue behavior |
|---|---|---|
| `BLOCK` | Does not proactively discard. | Applies `BLOCK_PRODUCER`; request threads wait for capacity. |
| `DISCARD_LOW_PRIORITY` | Discards `TRACE`, `DEBUG`, and `INFO` near capacity. | Blocks `WARN` and `ERROR` until capacity is available. |
| `DROP_WHEN_FULL` | Does not proactively select by level. | Never blocks and can drop events of any level. |

`DISCARD_LOW_PRIORITY` uses `discarding-threshold` when it is greater than zero.
When it is zero, the appender derives 20 percent of `queue-size`, with a minimum
of one queue slot.

The former low-level properties remain compatible:

- `never-block=true` forces `DROP_WHEN_FULL`;
- a positive `discarding-threshold` combined with the default `BLOCK` policy
  preserves the former low-priority discard behavior;
- an explicit non-`BLOCK` policy takes precedence over a positive legacy
  threshold.

`WARN` and `ERROR` can still be lost with `DROP_WHEN_FULL`, during forced
process termination, or when shutdown exceeds `max-flush-time-ms`.

Configuration examples:

```yaml
smbtech:
  logging:
    async:
      saturation-policy: BLOCK
```

```yaml
smbtech:
  logging:
    async:
      saturation-policy: DISCARD_LOW_PRIORITY
      discarding-threshold: 256
```

```yaml
smbtech:
  logging:
    async:
      saturation-policy: DROP_WHEN_FULL
```

## Critical Event Classification

The framework classifies the following as critical:

- levels `WARN` and `ERROR`;
- structured event types `AUDIT` and `SECURITY`.

`PolicyAwareAsyncAppender` protects these events when
`critical-event-protection-enabled=true`:

- `DISCARD_LOW_PRIORITY` never treats them as discardable;
- `DROP_WHEN_FULL` enqueues them normally while capacity remains;
- when a `DROP_WHEN_FULL` queue is full, it writes the critical event directly
  to the configured delegate on the producer thread.

The synchronous fallback can add latency and may overtake older events already
waiting in the async queue. It protects critical events only from
saturation-based queue loss. It cannot guarantee delivery after forced process
termination, output failure, container failure, or shutdown timeout. Durable
audit delivery requires a durable external transport.

Protection can be disabled explicitly:

```yaml
smbtech:
  logging:
    async:
      critical-event-protection-enabled: false
```

## Property Validation

The starter validates the async settings when its auto-configuration starts.
Invalid values fail-fast with an `Invalid async logging configuration` root
cause that identifies the property, accepted range, and configured value.

| Setting | Accepted value |
|---|---:|
| Queue size | `256` to `65536` events |
| Maximum shutdown flush time | `100` to `30000` ms |
| Discarding threshold | `0` inclusive to `queue-size` exclusive |

Validation applies even when async routing is disabled because the packaged
Logback configuration still creates the `ASYNC` appender. For example, this
configuration fails application startup:

```yaml
smbtech:
  logging:
    async:
      queue-size: 128
```

The root cause is:

```text
Invalid async logging configuration: smbtech.logging.async.queue-size must be
between 256 and 65536 (inclusive) (was 128)
```

A queue stores complete logging events and their structured data, so memory use
depends on payload size. Avoid placing response bodies, unbounded collections,
or large exception graphs in events. Size the queue from measured burst volume,
acceptable producer blocking, container memory, and shutdown grace time.

Use synchronous mode when deterministic delivery on the caller thread is more
important than isolating request latency:

```yaml
smbtech:
  logging:
    async:
      enabled: false
```

Use the reviewed asynchronous defaults:

```yaml
smbtech:
  logging:
    async:
      enabled: true
      queue-size: 2048
      saturation-policy: BLOCK
      critical-event-protection-enabled: true
      discarding-threshold: 0
      never-block: false
      max-flush-time-ms: 1000
      observability:
        enabled: true
```

## Safe Shutdown

`PolicyAwareAsyncAppender` coordinates event admission with Logback shutdown.
When Spring Boot closes the logging system, the appender:

1. closes admission so no new event can enter the async queue;
2. waits for producers that were already admitted, including producers blocked
   by the `BLOCK` policy;
3. stops the Logback worker and lets it drain accepted queued events;
4. waits up to `smbtech.logging.async.max-flush-time-ms`;
5. records the shutdown duration, timeout result, and queued events remaining
   when `stop()` returns.

The admission lock prevents a producer from enqueuing after the worker has
already completed. Events that reached the appender before admission closed are
part of the drain contract. Calls that race with shutdown after admission
closed are rejected and counted separately. Repeated `stop()` calls are
idempotent.

Spring Boot owns logging-system cleanup. Keep its
`logging.register-shutdown-hook=true` default, close the application context
normally, and configure the platform termination grace period to exceed both
the application shutdown budget and `max-flush-time-ms`.

A flush timeout is observable but cannot create a durable-delivery guarantee.
The Logback worker may still finish an event already inside the delegate after
`stop()` returns. The pending value counts only events still in the queue at
that instant. `SIGKILL`, `Runtime.halt`, container eviction after the grace
period, output failure, and process failure can still lose events. Use a
durable transport when audit delivery must survive process loss.

## Concurrency Verification

`AsyncAppenderConcurrencyTest` exercises bounded, coordinated contention
scenarios instead of depending on uncontrolled timing:

| Policy or lifecycle | Concurrent invariant |
|---|---|
| `BLOCK` | Eight producers deliver every event exactly once, preserve per-producer order, and retain each event's MDC snapshot. |
| `DISCARD_LOW_PRIORITY` | Every application event is either delivered or included in the low-priority discard counter; every concurrent `WARN` and `AUDIT` event is delivered. |
| `DROP_WHEN_FULL` | Every attempted event is either delivered or included in `DROP_WHEN_FULL` accounting; critical events are delivered and synchronous fallback is exercised. |
| Shutdown | Admission closes before the worker stops, an already accepted blocked producer drains, and a later producer is rejected. |

Each scenario has a 15-second test timeout and uses barriers or latches to make
queue pressure reproducible. Run the focused suite with:

```bash
./gradlew \
  :spring-boot-service-framework-starters:spring-boot-service-framework-starter-logging:test \
  --tests '*AsyncAppenderConcurrencyTest'
```

The concurrency tests validate invariants rather than global event order.
Ordering between independent producer threads is scheduler-dependent.

## Runnable Example

The standalone
[`logging-consumer`](../../examples/logging-consumer/README.md) application
enables the async appender, consumes locally published Maven artifacts, and
exposes a bounded endpoint that emits both normal and critical events:

```bash
./gradlew publishLocalArtifacts
cd examples/logging-consumer
../../gradlew bootRun
```

```bash
curl -sS -X POST \
  -H 'X-Transaction-Id: tx-async-001' \
  'http://localhost:8080/api/logging/async?events=1000&critical-every=100'
```

The endpoint reports attempted emissions. Every `critical-every` event is an
`AUDIT` event and therefore receives critical-event protection. Delivery and
saturation effects are observed through Actuator, for example:

```bash
curl -sS \
  http://localhost:8080/actuator/metrics/smbtech.logging.async.queue.depth
curl -sS \
  http://localhost:8080/actuator/metrics/smbtech.logging.async.events.discarded
```

The example accepts environment overrides for queue size and saturation policy.
Its integration test verifies real asynchronous dispatch, MDC preservation,
critical classification, and metric exposure.

## Observability

The logging starter keeps Micrometer optional. Metrics are registered only when
`io.micrometer:micrometer-core` is on the application classpath and
`smbtech.logging.async.observability.enabled=true`. Adding the framework
Actuator starter satisfies the Micrometer requirement. Disabling asynchronous
routing leaves these meters unregistered because there is no active `ASYNC`
appender.

| Metric | Type | Tags | Meaning |
|---|---|---|---|
| `smbtech.logging.async.queue.capacity` | Gauge | none | Configured maximum number of waiting events. |
| `smbtech.logging.async.queue.depth` | Gauge | none | Current number of waiting events. |
| `smbtech.logging.async.queue.remaining` | Gauge | none | Current remaining queue capacity. |
| `smbtech.logging.async.events.discarded` | Function counter | `reason=low_priority\|full_queue` | Events discarded proactively or because a non-blocking queue was full. |
| `smbtech.logging.async.critical.fallbacks` | Function counter | none | Critical events written synchronously because a `DROP_WHEN_FULL` queue was full. |
| `smbtech.logging.async.producer.block` | Function timer | none | Count and cumulative duration of producer calls observed waiting on a full queue. |
| `smbtech.logging.async.accepting` | Gauge | none | `1` while event admission is open and `0` during or after shutdown. |
| `smbtech.logging.async.events.rejected` | Function counter | none | Events rejected after shutdown admission closed. |
| `smbtech.logging.async.shutdown` | Function timer | none | Shutdown attempt count and cumulative duration. |
| `smbtech.logging.async.shutdown.timeouts` | Function counter | none | Shutdown attempts that exceeded `max-flush-time-ms`. |
| `smbtech.logging.async.shutdown.pending` | Gauge | none | Events still queued when the last shutdown attempt returned. |

All tags have a fixed, bounded value set. Logger names, messages, transaction
ids, trace ids, event data, and exception types are never metric tags.

The producer block timer observes calls that encounter an already-full queue.
It is an operational signal rather than a complete request-latency timer:
concurrent producers can fill the queue immediately after another producer
samples its capacity.

When the framework Actuator starter and logging integration are present, module
information also includes the observability toggle and an instantaneous,
bounded snapshot of queue capacity, depth, remaining capacity, discard counts,
critical fallback count, observed producer wait totals, admission state, and
shutdown result. It does not expose log messages, MDC values, or structured
event data.

Example Prometheus alerts should use a rate for monotonic counters:

```promql
rate(smbtech_logging_async_events_discarded_total[5m]) > 0
```

Queue pressure can be calculated from the gauges:

```promql
smbtech_logging_async_queue_depth
/
smbtech_logging_async_queue_capacity
> 0.8
```

## Local Performance Baseline

Run the local diagnostic baseline:

```bash
./gradlew loggingAsyncBaseline
```

`AsyncAppenderBaseline` performs warmup runs and then compares 100,000
synchronous and asynchronous in-memory events using the reviewed queue policy.
It writes:

```text
spring-boot-service-framework-starters/
  spring-boot-service-framework-starter-logging/
  build/reports/logging/async-appender-baseline.md
```

The report captures producer nanoseconds per event, end-to-end throughput, and
async drain time. It intentionally has no pass/fail threshold because results
depend on the CPU, JVM, operating system, and concurrent workload. Compare
results only on controlled, equivalent environments.

## Verification

Run the focused behavioral and configuration contract:

```bash
./gradlew asyncLoggingContractCheck
```

Run the complete logging compatibility lifecycle:

```bash
./gradlew loggingCompatibilityCheck
```

The supported runtime names, fragment paths, legacy property precedence, and
change policy are defined in
[Logging Compatibility](compatibility.md).

The reviewed machine-readable contract is defined in
`gradle/logging-async-contract.gradle` and recorded in
`gradle/compatibility/contracts/logging.txt`.
