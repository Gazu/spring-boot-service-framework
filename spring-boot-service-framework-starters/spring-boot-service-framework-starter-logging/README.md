# Spring Boot Service Framework Logging Starter

Spring Boot starter for structured JSON logging, MDC correlation, servlet
transaction id propagation, and Logback output. It adapts
`spring-boot-service-framework-logging-core` to Spring Boot runtime behavior.

## When to use

Use this starter in Spring Boot services that need:

- structured JSON logs;
- `StructuredLogger` and `StructuredLoggerFactory` beans;
- `X-Transaction-Id` propagation into MDC;
- Logback async console configuration;
- reusable Logback fragments for application-owned destinations;
- Spring Boot configuration metadata for `smbtech.logging`.

Use `spring-boot-service-framework-logging-core` directly only when building a
logging adapter or test that must remain independent from Spring Boot.

## Dependency

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.2'
    )
    implementation 'com.smbtech:spring-boot-service-framework-starter-logging'
}
```

## Public API

- `StructuredLoggers` is the starter's documented public type exception.
- `StructuredLogger`, `StructuredLoggerFactory`, `StructuredEvent`, `EventType`,
  and `LogLevel` are supported contracts from `logging-core`.

`TransactionIdFilter`, logging adapters, `LoggingAutoConfiguration`, and
`LoggingProperties` are framework implementation or infrastructure. See
[Public API Boundaries](../../docs/public-api-boundaries.md).

## What this module does not do

- It does not require a consuming service to provide `logback-spring.xml`.
- It does not create tracing spans; it only copies MDC values already present.
- It does not store secrets or application-specific logging data.
- It does not put Spring Boot dependencies into `logging-core`.

## Main documentation

| Topic | Document |
|---|---|
| Logging guide | [Logging Guide](../../docs/logging.md) |
| Supported API and changes | [Logging Compatibility](../../docs/logging/compatibility.md) |
| Async appender contract | [Async Appender Contract](../../docs/logging/async-appender.md) |
| Custom Logback destination | [Extensible Logback Configuration](../../docs/logging/async-appender.md#extensible-logback-configuration) |
| Logging property reference | [Logging Property Reference](../../docs/logging/property-reference.md) |
| Logging consumer example | [Logging Consumer Example](../../examples/logging-consumer/README.md) |
| Names and properties migration | [Migration Guide](../../docs/guides/migrate-public-names-and-properties.md) |
| Logging core README | [Logging Core README](../../spring-boot-service-framework-logging-core/README.md) |
| Module README rules | [Module README Convention](../../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-logging:check
./gradlew asyncLoggingContractCheck
./gradlew loggingAsyncBaseline
./gradlew loggingCompatibilityCheck
./gradlew loggingConsumerSmoke
```
