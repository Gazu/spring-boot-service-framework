# Dependency and Local Publication

For local development, publish the framework artifacts into Maven local:

```bash
./gradlew publishToMavenLocal
```

Then consume the starter from another service:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-starter-rest-client:0.3.0'
}
```

The repository also supports module-local build repositories used by the smoke
examples:

```bash
./gradlew publishLocalArtifacts
```

This command also publishes generated OpenAPI contract artifacts into the root
local repository:

```text
build/repository/openapi
```

When using module-local repositories instead of `mavenLocal()`, include every
framework artifact repository required by transitive dependencies:

```groovy
repositories {
    maven {
        url = uri('../spring-boot-service-framework/spring-boot-service-framework-starters/spring-boot-service-framework-starter-rest-client/build/repository')
    }
    maven {
        url = uri('../spring-boot-service-framework/spring-boot-service-framework-http-client-core/build/repository')
    }
    maven {
        url = uri('../spring-boot-service-framework/spring-boot-service-framework-commons/build/repository')
    }
    maven {
        url = uri('../spring-boot-service-framework/spring-boot-service-framework-logging-core/build/repository')
    }
    maven {
        url = uri('../spring-boot-service-framework/build/repository/openapi')
    }
    mavenCentral()
}
```

When a private Maven registry is available, replace the local repositories with
that registry. No public external publication is required.

---
