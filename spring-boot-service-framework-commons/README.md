# spring-boot-service-framework-commons

Small, stable, Spring-free utilities shared by framework modules and consuming
services.

## Dependency

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-commons:0.1.0-SNAPSHOT'
}
```

## Public API

- `Preconditions`: argument and invariant validation helpers.
- `ModuleDiagnosticContext`: null-safe immutable access to the SLF4J MDC.
- `notification.Notification`: immutable structured notification model for
  framework errors, warnings, and informational events.
- `notification.NotificationSeverity`: severity values and helpers for
  prefix-based error codes.
- `notification.NotifyingException`: runtime exception base class that carries
  one or more structured notifications.
- `logging.Type`: legacy structured event types such as `AUDIT`, `SECURITY`, and
  `METRIC`.
- `logging.Markers`: shared legacy markers, including `SENSITIVE`.
- `logging.Event`: legacy fluent structured event model.
- `logging.Logger`: legacy SLF4J facade for application, audit, and security
  events.

This module must remain small and framework-neutral. Do not add Spring
configuration, connectors, or application-specific domain logic here. A class
belongs in this module only when it is useful to several modules and keeps an API
that does not depend on the consuming application.

## Notification model

Notifications are intended to be stable, machine-readable details that can be
attached to framework exceptions or later mapped into API responses.

```java
Notification notification = Notification.builder()
        .code("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0400")
        .message("Bad Request received from HTTP client")
        .metadataEntry("clientName", "dummy")
        .build();

throw new NotifyingException(notification);
```

`Notification` is immutable. Metadata and notification lists are defensively
copied, so callers cannot mutate an exception after it has been created.
