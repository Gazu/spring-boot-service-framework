# Dependency Management

The Spring Boot Service Framework platform is the canonical dependency
management entry point for applications that consume framework modules. It
aligns every supported framework artifact to one version and imports the
supported Spring Boot dependency management BOM.

## Gradle

Publish the framework locally or configure the private Maven registry, then
import the platform before declaring framework dependencies:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.2'
    )

    implementation 'com.smbtech:spring-boot-service-framework-starter-logging'
    implementation 'com.smbtech:spring-boot-service-framework-starter-rest-client'
    implementation 'com.smbtech:spring-boot-service-framework-starter-error-handling'
}
```

Import the platform in every Gradle configuration that consumes a managed
artifact. For example, test-only modules require a test platform:

```groovy
dependencies {
    testImplementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.2'
    )
    testImplementation 'com.smbtech:spring-boot-service-framework-openapi-contract-testing'
}
```

Do not add versions to individual framework dependencies when the platform is
present. Upgrade the framework by changing only the platform version.

## `platform` Versus `enforcedPlatform`

Use Gradle's `platform(...)` by default. It supplies recommended dependency
constraints while still allowing the consuming application to resolve a
deliberate, compatible override.

`enforcedPlatform(...)` forces all constraints from this platform, including
the imported Spring Boot BOM, onto the consumer's complete dependency graph.
Use it only when an organization intentionally requires strict central
enforcement and has verified the effect on all application dependencies.
Libraries should not export an enforced platform to their consumers.

## Maven

Import the platform POM in `dependencyManagement`, then declare framework
dependencies without versions:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.smbtech</groupId>
            <artifactId>spring-boot-service-framework-platform</artifactId>
            <version>0.5.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.smbtech</groupId>
        <artifactId>spring-boot-service-framework-starter-rest-client</artifactId>
    </dependency>
</dependencies>
```

The platform artifact has `pom` packaging and does not contain runtime classes.

## Managed Framework Artifacts

The platform manages these coordinates under the `com.smbtech` group:

| Artifact | Responsibility |
|---|---|
| `spring-boot-service-framework-actuator-core` | Framework-neutral diagnostic contracts and services |
| `spring-boot-service-framework-commons` | Shared framework-neutral notification contracts |
| `spring-boot-service-framework-error-core` | Framework-neutral error resolution contracts |
| `spring-boot-service-framework-http-client-core` | Framework-neutral HTTP client contracts |
| `spring-boot-service-framework-logging-core` | Framework-neutral structured logging contracts |
| `spring-boot-service-framework-mock-core` | Framework-neutral mock contracts |
| `spring-boot-service-framework-openapi-contract-testing` | Spring MVC OpenAPI contract tests |
| `spring-boot-service-framework-project-generator` | Build-time Spring Boot project scaffolding boundary |
| `spring-boot-service-framework-starter-actuator` | Spring Boot Actuator adapter |
| `spring-boot-service-framework-starter-error-handling` | Spring Boot error handling adapter |
| `spring-boot-service-framework-starter-logging` | Spring Boot structured logging adapter |
| `spring-boot-service-framework-starter-mock` | Spring Boot mock adapter |
| `spring-boot-service-framework-starter-rest-client` | Spring Boot REST client adapter |

Generated OpenAPI coordinates under `com.smbtech.openapi` are contract-specific
and are not managed by the framework platform.

## Spring Boot Dependency Management

The framework platform imports
`org.springframework.boot:spring-boot-dependencies:4.1.0`. This aligns Spring,
Jackson, Micrometer, Spring Security, Apache HTTP Client, and other dependency
versions managed by the supported Spring Boot release.

The platform:

- does not apply the Spring Boot Gradle plugin;
- does not turn a project into a Spring Boot application;
- does not add optional dependencies automatically;
- does not replace Spring Boot application configuration.

Applications must still add optional capabilities such as
`spring-boot-starter-oauth2-client` when their selected framework feature
requires them.

### Security Patch Overrides

The platform applies reviewed patch-level overrides when a supported Spring
Boot BOM still manages a dependency with a published vulnerability. These
constraints remain aligned with the dependency families selected by Spring
Boot and can be removed after the supported BOM includes the fixes.

| Dependency | Managed version |
|---|---|
| Apache HttpClient 5 | `5.6.3` |
| Apache HttpCore 5 | `5.4.3` |
| Jackson 3 | `3.1.5` |
| Log4j 2 API | `2.25.5` |

The platform publishes these versions as strict Gradle constraints. Applications
that apply `io.spring.dependency-management` must also override the matching
Spring Boot BOM properties because that plugin's resolution rules take
precedence over Gradle constraints:

```groovy
ext['httpclient5.version'] = '5.6.3'
ext['httpcore5.version'] = '5.4.3'
ext['jackson-bom.version'] = '3.1.5'
ext['log4j2.version'] = '2.25.5'
```

Keep these values aligned with the framework release. The standalone
applications under `examples/` load them from the repository's
`gradle.properties`.

## Local Publication

Publish all framework artifacts to module-local Maven repositories:

```bash
./gradlew publishLocalArtifacts
```

The platform is written to:

```text
spring-boot-service-framework-platform/build/repository
```

A standalone consumer can resolve it with:

```groovy
repositories {
    maven {
        url = uri(
                '../spring-boot-service-framework-platform/build/repository'
        )
    }
    mavenCentral()
}
```

Alternatively, publish all artifacts to the current user's Maven local
repository:

```bash
./gradlew publishToMavenLocal
```

The standalone applications under `examples/` demonstrate the module-local
repository workflow.

## Private Registry Publication

The platform uses the same private publication convention as the other
framework modules. Configure the repository URL and credentials through the
project's supported private registry properties or environment variables, then
run the private publication task documented in
[Releasing](releasing.md). Publishing the platform does not publish it to Maven
Central or any public repository.

Publish the platform and every managed artifact for the same framework version.
A platform POM that references artifacts missing from the target registry
cannot make those artifacts resolvable.

## Dependency Conflict Troubleshooting

Inspect the selected framework version and the rule that selected it:

```bash
./gradlew dependencyInsight \
    --dependency spring-boot-service-framework-http-client-core \
    --configuration runtimeClasspath
```

Common failures:

| Symptom | Cause | Resolution |
|---|---|---|
| A framework dependency has no version | The platform is missing from that Gradle configuration | Add `platform(...)` to the matching configuration |
| The platform cannot be found | Its repository was not configured or it was not published | Publish locally or add the private Maven registry |
| A different framework version is selected | An explicit version, another platform, or a resolution rule overrides the constraint | Remove individual framework versions and inspect `dependencyInsight` |
| A security patch override is downgraded to the Spring Boot BOM version | `io.spring.dependency-management` selected the BOM version through a resolution rule | Configure the security patch BOM properties listed above |
| Spring dependencies are unexpectedly forced | The consumer imported the BOM with `enforcedPlatform(...)` | Use `platform(...)` unless strict enforcement is required |
| A generated OpenAPI artifact has no version | Generated contract artifacts are intentionally outside this BOM | Declare the generated artifact version explicitly |

## Repository Validation

Run the platform-specific checks after changing managed modules, coordinates, or
versions:

```bash
./gradlew platformCompatibilityCheck
./gradlew consumerSmoke
```

Run the complete compatibility suite before release:

```bash
./gradlew compatibilityCheck
```

`platformCompatibilityCheck` validates the Gradle constraints, generated Maven
POM, published metadata, and committed compatibility contract.
`consumerSmoke` proves that standalone applications can import the platform and
resolve framework dependencies without individual versions.
