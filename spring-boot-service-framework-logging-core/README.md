# spring-boot-service-framework-logging-core

Hexagonal core for structured logging.

This module defines the framework-neutral logging model and ports. It does not
write to SLF4J, Logback, files, stdout, MDC, servlet APIs, or Spring Boot by
itself. Runtime integration belongs in
`spring-boot-service-framework-starter-logging`.

## Dependency

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-logging-core:0.2.0'
}
```

## When to use

Most applications should consume `spring-boot-service-framework-starter-logging`
instead of this core module directly.

Use this module directly when building framework-level logging adapters, tests,
or runtime integrations that should depend only on the neutral logging model and
ports.

## Public API

The module contains:

- immutable domain objects: `StructuredEvent`, `EventType`, `LogLevel`, and
  `Sensitivity`;
- input port: `StructuredLogger`;
- output ports: `LogEventSink` and `CorrelationContext`;
- application service: `StructuredLoggingService`.

## Boundary rules

The module depends only on the JDK. The `verifyHexagonalBoundaries` task rejects
imports from Spring, SLF4J, Logback, Servlet, and Jackson.

Keep new runtime-specific behavior in the starter. Keep this module focused on:

- event modeling;
- logging policy decisions;
- input and output ports;
- small domain helpers that do not require a framework.

## Structured events

`StructuredEvent` is the immutable payload passed through the logging ports:

```java
StructuredEvent event = StructuredEvent.builder(EventType.AUDIT)
        .message("Payment {} approved", "pay-123")
        .with("paymentId", "pay-123")
        .with("amount", 12990)
        .tag("PAYMENTS")
        .build();
```

The builder supports:

- `message(String, Object...)`: message template and arguments;
- `with(String, Object)`: structured data;
- `with(String, Consumer<Map<String, Object>>)`: nested structured data;
- `tag(String)`: classification tags;
- `sensitive()`: marks the event as containing sensitive data;
- `throwable(Throwable)`: attaches a failure.

Sensitive events are represented by `Sensitivity.SENSITIVE`; adapters decide
how to render, filter, or route them.

## Structured logger

Application code normally uses the `StructuredLogger` input port:

```java
final class PaymentUseCase {

    private final StructuredLogger log;

    PaymentUseCase(StructuredLogger log) {
        this.log = log;
    }

    void approve(String paymentId) {
        log.info(builder -> builder
                .type(EventType.AUDIT)
                .message("Payment {} approved", paymentId)
                .with("paymentId", paymentId)
                .tag("PAYMENTS"));
    }
}
```

`StructuredLogger` exposes level-specific helpers:

- `trace(...)`
- `debug(...)`
- `info(...)`
- `warn(...)`
- `error(...)`

All of them delegate to `log(LogLevel, StructuredEvent)`.

## Output port

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

The Spring Boot logging starter provides the production adapter that writes JSON
logs through Spring Boot structured logging and Logback.

## Logging service

`StructuredLoggingService` applies core logging policy before delegating to the
sink:

```java
LogEventSink sink = new ConsoleSink();
StructuredLogger log = new StructuredLoggingService(sink, false);

log.info(StructuredEvent.builder(EventType.APPLICATION)
        .message("Service started")
        .build());
```

When `production=true`, metric events are disabled by the core policy:

```java
StructuredLogger productionLog = new StructuredLoggingService(sink, true);

boolean enabled = productionLog.isEnabled(LogLevel.INFO, EventType.METRIC);
// enabled == false
```

## Correlation context

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

## Local validation

```bash
./gradlew :spring-boot-service-framework-logging-core:check
```
