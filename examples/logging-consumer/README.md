# Logging consumer example

Standalone Spring Boot application that consumes
`com.smbtech:spring-boot-service-framework-starter-logging:0.2.0` from the local
Maven repositories generated under each module `build/repository` directory.

The example intentionally consumes published local artifacts instead of Gradle
`project(...)` dependencies. This validates the same POMs and JARs that another
repository would consume.

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

## What it demonstrates

- `spring-boot-service-framework-starter-logging` auto-configuration.
- Structured JSON logging through the framework logging API.
- `X-Transaction-Id` propagation into MDC and the HTTP response.
- Micrometer tracing values (`traceId` and `spanId`) included through MDC.
- Async logging disabled in the example to make test output deterministic.

## Important configuration

```yaml
smbtech:
  logging:
    production: false
    level: INFO
    async:
      enabled: false
    transaction:
      enabled: true
```

Tracing is enabled with 100% sampling only for the example. Production services
should choose sampling and async logging settings according to their runtime
needs.

## Tests

The smoke tests verify that:

- the published starter artifacts can be consumed;
- `/api/dummy` is available without additional application logging setup;
- the response includes the transaction id;
- emitted JSON logs include transaction, trace, and span identifiers.
