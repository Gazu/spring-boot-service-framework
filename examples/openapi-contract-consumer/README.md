# OpenAPI Contract Consumer

Standalone Spring Boot consumer used by the repository gates to exercise the
published warehouse inventory contract artifacts. It resolves the generated
model, server API, client, and contract-testing module from local Maven
repositories created from the current checkout. It does not use Gradle project
dependencies or regenerate sources.

The tests implement the generated delegate, validate every operation from the
embedded OpenAPI contract, and call the same runtime through the generated
Spring HTTP Interface and OpenFeign interfaces. OpenFeign is an explicit
consumer dependency.

From the repository root, run:

```bash
./gradlew openApiContractConsumerSmoke
```
