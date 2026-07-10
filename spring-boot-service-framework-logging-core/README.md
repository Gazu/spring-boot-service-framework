# spring-boot-service-framework-logging-core

Hexagonal core for structured logging.

It contains:

- immutable domain objects: `StructuredEvent`, `EventType`, `LogLevel`, and
  `Sensitivity`;
- input port: `StructuredLogger`;
- output ports: `LogEventSink` and `CorrelationContext`;
- application service: `StructuredLoggingService`.

The module depends only on the JDK. The `verifyHexagonalBoundaries` task rejects
imports from Spring, SLF4J, Logback, Servlet, and Jackson.

## Dependency

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-logging-core:0.1.0-SNAPSHOT'
}
```

Most applications should consume `spring-boot-service-framework-starter-logging` instead of
this core module directly.
