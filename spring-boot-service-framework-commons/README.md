# spring-boot-service-framework-commons

Framework-neutral utilities shared by Spring Boot Service Framework modules.
This module is intentionally small and does not depend on Spring Boot, HTTP
clients, logging backends, or application-specific code.

## When to use

Use this module when framework code needs shared primitives that can be reused
by more than one module.

Prefer a feature-specific module when the code belongs to logging, REST client,
mock, or another concrete framework area.

## Dependency

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-commons:0.3.0'
}
```

## Public API

- `notification.Notification`: immutable structured notification model.
- `notification.NotificationSeverity`: notification severity values.
- `notification.NotifyingException`: runtime exception base class that carries
  structured notifications.

`com.smbtech.serviceframework.commons.notification` is a documented public
package exception. See [Public API Boundaries](../docs/public-api-boundaries.md).

## What this module does not do

- It does not provide Spring Boot auto-configuration.
- It does not contain HTTP, logging backend, mock, or OAuth2 adapters.
- It does not contain business-specific domain contracts.
- It does not replace feature-specific APIs owned by other modules.

## Main documentation

| Topic | Document |
|---|---|
| Repository module map | [Repository README](../README.md) |
| Documentation ownership | [Documentation Architecture](../docs/documentation-architecture.md) |
| Module README rules | [Module README Convention](../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-commons:check
./gradlew commonsCompatibilityCheck
```
