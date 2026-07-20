# Local Validation

Run the starter tests:

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-rest-client:check
```

Run the full framework baseline:

```bash
./gradlew baseline
```

Run the standalone consumer smoke tests:

```bash
./gradlew restClientConsumerSmoke
./gradlew consumerSmoke
./gradlew compatibilityCheck
```

The example service lives in `examples/rest-client-consumer`. It consumes
published JARs instead of Gradle `project(...)` dependencies, which is closer to
how real services will consume the framework.
