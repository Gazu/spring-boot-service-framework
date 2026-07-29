# Gradle Convention Plugins

Internal Gradle convention plugins shared by framework libraries and Spring
Boot starters.

## Plugin IDs

| Plugin | Applies to | Centralized behavior |
|---|---|---|
| `com.smbtech.service-framework.java-library` | Framework Java libraries | Java 21 toolchain, UTF-8, `-parameters`, JUnit Platform, JaCoCo verification, reproducible sources/Javadoc/binary JARs, complete Maven POM metadata, signing, and publication repositories. |
| `com.smbtech.service-framework.spring-boot-starter` | Spring Boot starters | Java library convention plus the Spring Boot BOM, configuration processor, starter test dependency, and resolved Maven version mapping. |
| `com.smbtech.service-framework.java-platform` | Framework dependency platforms | Gradle platform dependency policy, complete Maven POM metadata, signing, reproducibility, and local/private publication repositories. |

Module build files retain only their description, dependencies, specialized
tasks, boundary checks, and human-readable POM name.

## Boundary

These plugins are internal repository build logic. They are not published as
consumer-facing framework artifacts and do not form part of the runtime public
API.

## Local Validation

From the repository root:

```bash
./gradlew conventionPluginsCheck
```

The check compiles and validates the plugins and rejects duplicated convention
configuration in framework module build files.
