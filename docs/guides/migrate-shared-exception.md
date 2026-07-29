# Migrate From shared/exception

This guide replaces copied `shared/exception` implementations with the
framework error core and starter. Migrate behavior first, then delete the local
builders and handlers after contract tests pass.

## Type Mapping

| Existing shared type | Framework replacement |
|---|---|
| `ApplicationException` | `ServiceException` plus an application `ErrorDefinition` catalog. |
| `AuthenticationException` and session subclasses | Catalog entries using `AUTHENTICATION` or `AUTHORIZATION`. |
| `RequestValidationException` | Bean Validation for request models, or a field `Notification` inside `ServiceException`. |
| `RestConnectorException` | `HttpClientResponseException`, resolved by `HttpClientExceptionResolver`. |
| `RichErrorResponse` | Immutable commons `Notification`. |
| `SafeErrorResponse` | `ResolvedError`, `ErrorExposure`, and `NotificationSanitizer`. |
| `ExposureEnum` | `ErrorExposure`. |
| `ExceptionHelper` status logic | `ThrowableErrorResolver` and `NotificationHttpStatusResolver`. |
| `BaseExceptionHandler`, `GenericExceptionHandler`, `UnhandledExceptionHandler` | Auto-configured `ServiceFrameworkExceptionHandler`. |
| Local security exception handlers | `SecurityAuthenticationEntryPoint` and `SecurityAccessDeniedHandler`. |

## Step 1: Add The Starter

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.4.0'
    )
    implementation 'com.smbtech:spring-boot-service-framework-starter-error-handling'
}
```

Keep the old handlers temporarily while migrating one exception family at a
time. Their explicit `@Order` determines which handler receives overlapping
exceptions during this stage.

## Step 2: Replace Codes With A Catalog

Move code construction out of `ExceptionHelper` and into stable enum entries:

```java
enum AuthenticationErrors implements ErrorDefinition {
    INVALID_CREDENTIALS;

    public String code() { return "E_MS_AUTH_0001"; }
    public ErrorCategory category() { return ErrorCategory.AUTHENTICATION; }
    public String publicMessage() { return "Invalid credentials"; }
    public NotificationSeverity severity() { return NotificationSeverity.ERROR; }
}
```

Preserve existing codes during migration when consumers depend on them.

## Step 3: Replace ApplicationException

Before:

```java
throw ApplicationException.from(code, message, detail, cause);
```

After:

```java
throw ServiceException.from(
        AuthenticationErrors.INVALID_CREDENTIALS,
        "Authentication provider rejected the current session",
        cause
);
```

Do not move the old `detail` map into notification metadata. Put internal
details in the diagnostic message, cause, structured reporter, or audit event.
Only explicitly safe metadata should use the configured allowlist, which
applies to detailed `INTERNAL` responses.

## Step 4: Replace Validation Exceptions

Use Jakarta Bean Validation annotations for request shape constraints. The
starter aggregates all violations automatically. For domain validation, create
ordered notifications with `fieldName` and throw one `ServiceException`.

```java
Notification customerId = Notification.builder()
        .code("E_REQUEST_0001")
        .message("customerId is invalid")
        .fieldName("customerId")
        .build();

throw new ServiceException(List.of(customerId), "Domain validation failed");
```

## Step 5: Replace Connector Exceptions

Configure the REST client error decoder to produce
`HttpClientResponseException`. Remove local code that copies downstream headers,
bodies, and causes into API responses. The starter keeps those details in the
diagnostic path. `PUBLIC` returns the generic framework message; `INTERNAL`
returns the resolved downstream message after sanitization.

## Step 6: Replace Response Builders

Remove `RichErrorResponse`, `SafeErrorResponse`, and manual exposure mutation.
The new response is a flat immutable `Notification` with the documented
[snake-case contract](../error-handling/json-contract.md).

If the old API returned a generated wrapper or nested `notifications` object,
coordinate the response contract change with consumers. Do not configure a
custom serializer solely to preserve fields that expose stack traces or raw
details.

Humanized title and description fields have no direct core equivalent. Use the
catalog message as the safe resolved text for detailed `INTERNAL` responses.
The default `PUBLIC` mode uses the framework generic message. If a trusted
client contract requires additional safe metadata, define a stable schema, add
only that key to the allowlist, and test its JSON representation.

Configure `smbtech.error-handling.response.exposure=INTERNAL` only after
reviewing every consumer. The setting is global and cannot be scoped by code or
category.

## Step 7: Replace Web And Security Handlers

Delete local generic and unhandled `@ControllerAdvice` classes after equivalent
MockMvc tests pass. Connect the starter's `AuthenticationEntryPoint` and
`AccessDeniedHandler` to the security filter chain for consistent 401/403
responses.

## Step 8: Verify Compatibility

Add contract tests for:

- preserved application error codes and statuses;
- multiple validation violations;
- flat snake-case response fields;
- downstream and unexpected error sanitization;
- authentication and authorization responses;
- absence of stack traces, headers, bodies, credentials, and causes.

The complete runnable reference is the
[error handling consumer example](../../examples/error-handling-consumer/README.md).

After migration, remove the copied `shared/exception` package and its obsolete
generated response dependencies in the same release.
