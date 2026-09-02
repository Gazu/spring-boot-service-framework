# Spring Boot Service Framework Platform

Gradle and Maven dependency management platform for supported Spring Boot
Service Framework modules and their Spring Boot dependency baseline.

## When to use

Import this platform in applications that consume one or more framework
artifacts. It keeps all framework modules on the same release and provides the
Spring Boot dependency versions supported by that release.

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

This module exposes Gradle module metadata and a Maven BOM POM. Its public
contract consists of the managed module coordinates, the imported Spring Boot
BOM, and the compatibility baseline in
`gradle/compatibility/contracts/platform.txt`.

It contains no Java packages or runtime classes.

## What this module does not do

- It does not add framework modules to an application automatically.
- It does not apply the Spring Boot Gradle plugin.
- It does not add optional runtime capabilities such as OAuth2 Client.
- It does not manage generated OpenAPI coordinates under `com.smbtech.openapi`.
- It does not provide auto-configuration or runtime behavior.

## Main documentation

| Topic | Document |
|---|---|
| Gradle, Maven, publication, and troubleshooting | [Dependency Management](../docs/dependency-management.md) |
| Supported dependency versions | [Compatibility](../docs/compatibility.md) |
| Release and private publication workflow | [Releasing](../docs/releasing.md) |
| Module README rules | [Module README Convention](../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-platform:check
./gradlew platformCompatibilityCheck
./gradlew consumerSmoke
```
