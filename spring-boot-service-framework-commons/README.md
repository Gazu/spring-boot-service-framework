# spring-boot-service-framework-commons

Small, stable, Spring-free utilities shared by framework modules and consuming
services.

## When to use

Use this module only for framework-neutral primitives that can be reused by more
than one framework module. Spring Boot auto-configuration, adapters, HTTP
clients, logging backends, and business-specific contracts belong elsewhere.

## Dependency

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-commons:0.2.0'
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

Most new code should prefer the dedicated module API that owns the use case. For
example, new structured logging consumers should use
`spring-boot-service-framework-logging-core` or
`spring-boot-service-framework-starter-logging` instead of the legacy logging
types in this module.

## Boundary rules

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
        .metadataEntry("status", 400)
        .build();

throw new NotifyingException(notification);
```

`Notification` is immutable. Metadata and notification lists are defensively
copied, so callers cannot mutate an exception after it has been created.

Convenience factories are available when the severity is known:

```java
Notification error = Notification.error(
        "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0500",
        "Downstream service returned an error"
);

Notification warning = Notification.warning(
        "W_SERVICE_FRAMEWORK_CONFIG_0001",
        "Optional configuration is missing"
);

Notification info = Notification.info(
        "I_SERVICE_FRAMEWORK_STARTUP_0001",
        "Framework module initialized"
);
```

When `severity` is not provided, it is inferred from the notification code
prefix:

| Prefix | Severity |
|---|---|
| `E` | `ERROR` |
| `W` | `WARNING` |
| `I` | `INFO` |
| anything else | `UNSPECIFIED` |

## Notifying exceptions

`NotifyingException` carries one or more immutable notifications and exposes the
first one as the primary notification:

```java
try {
    throw new NotifyingException(List.of(notification), "Request validation failed");
} catch (NotifyingException exception) {
    exception.primaryNotification().ifPresent(primary -> {
        String code = primary.code();
        String message = primary.message();
    });
}
```

Use `NotifyingException` as the base class when framework exceptions need both a
normal exception message and structured, machine-readable details for callers.

## Local validation

```bash
./gradlew :spring-boot-service-framework-commons:check
```
