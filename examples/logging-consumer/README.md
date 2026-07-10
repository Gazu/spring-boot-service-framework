# Logging consumer example

Standalone Spring Boot application that consumes
`com.smbtech:spring-boot-service-framework-starter-logging:0.1.0-SNAPSHOT` from the local
Maven repositories generated under each module `build/repository` directory.

From the framework root:

```bash
./gradlew loggingConsumerSmoke
```

The example does not use Gradle `project(...)` dependencies. It validates the
same POMs and JARs that another repository would consume.

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
