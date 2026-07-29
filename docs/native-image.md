# Spring Boot AOT And Native Images

The framework supports Spring Boot 4.1 AOT processing and GraalVM native-image
builds. Runtime starters use Jackson 3 and contribute hints for infrastructure
that Spring cannot infer from regular bean definitions.

## Build Setup

Use the versions managed by the application and apply Native Build Tools:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.0'
    id 'org.graalvm.buildtools.native' version '1.1.1'
}
```

Validate AOT processing without compiling a native executable:

```bash
./gradlew processAot
```

Compile the executable with GraalVM 25 or a compatible Native Image Kit:

```bash
./gradlew nativeCompile
```

The repository runs `processAot` for every standalone consumer through:

```bash
./gradlew nativeAotCheck
```

`nativeAotCheck` is part of `releaseGate`. It does not require GraalVM. Running
`nativeCompile` requires a local GraalVM installation with `native-image`.
The release workflow additionally compiles the logging consumer with GraalVM
25, starts the native executable, and verifies its HTTP endpoint.

## Declarative REST Clients

Interfaces created dynamically through `ApiClientFactory` must be known while
the application is processed ahead of time. Register every application-defined
interface through a `RuntimeHintsRegistrar`:

```java
final class ApplicationRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        HttpApiClientRuntimeHints.register(hints, PaymentsApi.class);
    }
}
```

Import the registrar from application configuration:

```java
@SpringBootApplication
@ImportRuntimeHints(ApplicationRuntimeHints.class)
class Application {
}
```

`HttpApiClientRuntimeHints` registers the JDK proxy, interface methods, and
request and response binding types. The REST client example contains the full
working setup.

## Mock Resources

The mock starter includes classpath resources under `mock/**`, `mocks/**`, and
`openapi/**`. Put native-image mock contracts under one of these conventional
locations. An application using another classpath location must register that
resource pattern in its own `RuntimeHintsRegistrar`. Files loaded through a
filesystem path do not require classpath resource hints.

## Jackson 3

Runtime modules use Boot 4's `tools.jackson` APIs and reuse the application
`ObjectMapper` when available. Public APIs that expose Jackson types use
Jackson 3 packages. Generated OpenAPI models continue to use
`com.fasterxml.jackson.annotation`; those annotations are also the annotation
contract used by Jackson 3.

The build logic used by the Gradle OpenAPI generator runs outside the
application runtime and is not included in native executables.
