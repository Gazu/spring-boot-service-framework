# Spring Boot Service Framework Error Core

Framework-neutral module boundary for reusable error definitions, exceptions,
resolution policies, and notification handling without Spring or Jackson APIs.
The package responsibility is documented by `package-info.java`; the artifact
does not require a Java marker type.

## When to use

Most Spring Boot applications should use
`spring-boot-service-framework-starter-error-handling`. Use this module directly
when implementing framework-neutral error contracts or adapters.

## Dependency

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.4.0'
    )
    implementation 'com.smbtech:spring-boot-service-framework-error-core'
}
```

## Public API

The public API is available under `com.smbtech.serviceframework.error`.
Applications can implement `ErrorDefinition`, commonly with an enum, to define
stable error codes, categories, safe resolved messages, and notification
severities. The Spring Boot starter's global exposure policy determines whether
the resolved message or a generic external message is written to the response.

```java
public enum CustomerErrors implements ErrorDefinition {
    CUSTOMER_NOT_FOUND;

    public String code() { return "E_CUSTOMER_0001"; }
    public ErrorCategory category() { return ErrorCategory.NOT_FOUND; }
    public String publicMessage() { return "The requested customer does not exist"; }
    public NotificationSeverity severity() { return NotificationSeverity.ERROR; }
}
```

Create a service exception from the catalog while keeping internal diagnostics
separate from response data:

```java
throw ServiceException.from(
    CustomerErrors.CUSTOMER_NOT_FOUND,
    "Customer lookup failed for internal identifier 123",
    cause
);
```

`ServiceException` also accepts one `Notification` or an ordered list of
notifications directly. Direct notifications use `ErrorCategory.INTERNAL`;
catalog factories preserve the category of the first definition.

`Notification`, `NotificationSeverity`, and `NotifyingException` are reused
from `spring-boot-service-framework-commons` and exposed through the core API
dependency. This module does not declare replacement copies.

Resolution components use `ResolvedError` to keep the response candidate,
`ErrorCategory`, `ErrorExposure`, internal diagnostic message, and immutable
`FieldViolation` values separate. The response adapter applies the final global
exposure policy.

Implement `ThrowableErrorResolver` for application-specific failures and pass
the resolvers to `ThrowableErrorResolutionPipeline`. Resolvers run by ascending
`order()`; registration order breaks ties. `FallbackThrowableErrorResolver`
produces a generic framework notification when no resolver matches. A response
adapter applies its final exposure and sanitization policy afterward.

`ServiceExceptionThrowableErrorResolver` uses a replaceable
`NotificationAggregationPolicy`. The default policy selects the first ordered
notification and converts every notification with a `fieldName` into an
immutable `FieldViolation` on the resulting `ResolvedError`.

`DefaultNotificationSanitizer` applies a case-insensitive top-level metadata
allowlist and recursively redacts credentials, tokens, passwords, headers,
bodies, exception causes, stack traces, JWTs, and authorization values. The
default allowlist contains `correlationId`, `path`, and `violations`.

## What this module does not do

- It does not register Spring MVC exception handlers.
- It does not serialize HTTP responses.
- It does not depend on Spring, Jackson, Servlet, SLF4J, or Logback APIs.

## Main documentation

| Topic | Document |
|---|---|
| Error handling usage | [Error Handling Guide](../docs/error-handling.md) |
| Public extension points | [Error Handling Extension Points](../docs/error-handling-extension-points.md) |
| Names and properties migration | [Migration Guide](../docs/guides/migrate-public-names-and-properties.md) |
| Shared notifications | [Commons README](../spring-boot-service-framework-commons/README.md) |
| Compatibility policy | [Compatibility](../docs/compatibility.md) |
| Error handling starter | [Error Handling Starter](../spring-boot-service-framework-starters/spring-boot-service-framework-starter-error-handling/README.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-error-core:check
```
