# Error Handling

The error handling starter provides one safe `Notification` response contract
for application exceptions, Spring MVC failures, Bean Validation, downstream
HTTP client errors, unexpected failures, and Spring Security 401/403 responses.

## Dependency

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-starter-error-handling:0.3.0'
}
```

The starter exposes `error-core` and the shared notification types transitively.
Add `spring-boot-service-framework-http-client-core` when the application also
throws `HttpClientResponseException`.

Use the [exception selection guide](error-handling/exception-selection.md) to
choose between application, configuration, authentication, mock, and downstream
exceptions.

## Minimal Configuration

```yaml
smbtech:
  error-handling:
    enabled: true
    response:
      exposure: INTERNAL
      include-field-violations: true
      metadata-allowlist:
        - correlationId
        - path
        - violations
    security:
      enabled: true
```

See the generated [property reference](error-handling/property-reference.md)
for every option and default.

## Response Exposure

`smbtech.error-handling.response.exposure` is a global policy. It is applied
after every resolver and `ResolvedErrorCustomizer`, so the selected value
controls application errors, MVC and validation failures, downstream failures,
Spring Security errors, and unexpected exceptions.

Use the default `INTERNAL` mode when clients must always receive the generic
framework notification:

```yaml
smbtech:
  error-handling:
    response:
      exposure: INTERNAL
```

```json
{
  "code": "E_SERVICE_FRAMEWORK_INTERNAL_0001",
  "message": "The request could not be completed",
  "severity": "ERROR",
  "field_name": "",
  "metadata": {
    "schema_version": "1",
    "category": "NOT_FOUND"
  }
}
```

Use `PUBLIC` only when the public codes, messages, field violations, and safe
metadata produced by all configured resolvers are part of the application's
external API contract:

```yaml
smbtech:
  error-handling:
    response:
      exposure: PUBLIC
```

```json
{
  "code": "E_ORDER_0001",
  "message": "The requested order does not exist",
  "severity": "ERROR",
  "field_name": "",
  "metadata": {
    "schema_version": "1",
    "category": "NOT_FOUND"
  }
}
```

> [!WARNING]
> `PUBLIC` is not configurable by category or error code. Enabling it affects
> every handled error in the application.

`PUBLIC` never exposes
`diagnosticMessage`, exception causes, stack traces, tokens, passwords,
sensitive headers, or downstream bodies: metadata allowlisting, recursive
sanitization, secret redaction, and snake-case serialization remain mandatory.
Applications that need a different selection policy can replace the
`ErrorExposurePolicy` bean.

> [!IMPORTANT]
> The default `INTERNAL` mode changes the earlier mixed behavior, where each
> resolver's own `ErrorExposure` determined the response. After upgrading, an
> application that omits this property receives the generic internal
> notification for every handled error. Set `exposure: PUBLIC` explicitly only
> when existing public catalog codes and messages must remain part of the API.

## Processing Pipeline

```mermaid
flowchart LR
    Failure["Throwable"] --> Resolver["ThrowableErrorResolutionPipeline"]
    Resolver --> Resolved["ResolvedError"]
    Resolved --> Customize["ResolvedErrorCustomizer"]
    Customize --> Report["ErrorReporter and ErrorMetricsRecorder"]
    Customize --> Factory["NotificationResponseFactory"]
    Factory --> Sanitize["NotificationSanitizer"]
    Sanitize --> Response["ResponseEntity<Notification>"]
    Response --> ResponseCustomize["NotificationResponseCustomizer"]
    ResponseCustomize --> Json["snake_case JSON"]
```

Resolvers run by ascending `order()`. The first supporting resolver wins. If no
resolver supports the failure, `FallbackThrowableErrorResolver` returns a safe
internal error. Diagnostics remain in `ResolvedError.diagnosticMessage()` for
logging and are not copied to the response.

## Application Error Catalog

Define stable application errors with an enum implementing `ErrorDefinition`:

```java
public enum OrderErrors implements ErrorDefinition {
    ORDER_NOT_FOUND;

    public String code() { return "E_ORDER_0001"; }
    public ErrorCategory category() { return ErrorCategory.NOT_FOUND; }
    public String publicMessage() { return "The requested order does not exist"; }
    public NotificationSeverity severity() { return NotificationSeverity.ERROR; }
}
```

Throw a `ServiceException` while keeping the internal diagnostic separate:

```java
throw ServiceException.from(
        OrderErrors.ORDER_NOT_FOUND,
        "Order lookup failed for internal identifier " + orderId,
        cause
);
```

The category of the first catalog entry controls the default HTTP status. A
`ServiceException` may contain multiple ordered notifications; notifications
with `fieldName` become entries under `metadata.violations`.

## Default Status Mapping

| Category | HTTP status |
|---|---:|
| `VALIDATION` | 400 |
| `AUTHENTICATION` | 401 |
| `AUTHORIZATION` | 403 |
| `NOT_FOUND` | 404 |
| `CONFLICT` | 409 |
| `DOWNSTREAM` | 502 |
| `RATE_LIMIT` | 429 |
| `METHOD_NOT_ALLOWED` | 405 |
| `UNSUPPORTED_MEDIA_TYPE` | 415 |
| `NOT_ACCEPTABLE` | 406 |
| `INTERNAL` | 500 |

Replace `NotificationHttpStatusResolver` to use a different mapping.

## Validation And MVC

The built-in resolvers cover:

- Bean Validation and binding errors;
- request parameter and path conversion errors;
- malformed JSON;
- missing request headers, parameters, and routes;
- unsupported HTTP methods and media types;
- unacceptable response media types.

Multiple validation failures are aggregated into one notification. Their JSON
shape is defined in the [snake-case contract](error-handling/json-contract.md).

## Downstream Errors

When `HttpClientResponseException` is on the classpath,
`HttpClientExceptionResolver` maps it to `DOWNSTREAM` or `RATE_LIMIT`. The
public notification never includes the downstream URI, headers, body, cookies,
cause, or source message. Complete details remain available only in the
internal diagnostic path.

## Spring Security

When Spring Security is present, the starter provides replaceable
`AuthenticationEntryPoint` and `AccessDeniedHandler` beans. Connect them to the
application filter chain:

```java
@Bean
SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuthenticationEntryPoint authenticationEntryPoint,
        AccessDeniedHandler accessDeniedHandler
) throws Exception {
    return http
            .exceptionHandling(errors -> errors
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .build();
}
```

Both handlers use the same serializer and response factory as MVC errors.
The complete catalog, RFC 6750 metadata contract, required-scope resolution,
configuration, and replacement points are documented in
[Security Error Handling](error-handling/security.md).

## Public Metadata Safety

In `PUBLIC` mode, only top-level metadata keys in
`response.metadata-allowlist` can reach the response. Allowed values are still
recursively sanitized. Tokens, credentials, passwords, authorization values,
headers, bodies, causes, exceptions, stack traces, JWT-shaped values, and
unsupported objects are redacted. Cyclic or excessively deep values are also
replaced with `<redacted>`. This final redaction remains active when an
application provides its own `NotificationSanitizer`.

Do not use the allowlist as a reason to place secrets in notifications. Keep
diagnostic data in exception causes, diagnostic messages, and internal reports.

## Observability

If `StructuredLoggerFactory` and `CorrelationContext` beans exist, the starter
registers a structured `ErrorReporter`. If a `MeterRegistry` exists, it records
the configured counter using bounded tags for error code, category, and HTTP
status. Application reporters are composed with the framework reporter.

## Customization

Use contributors and customizers for small changes, or provide an application
bean to replace a complete policy. See [Error Handling Extension Points](error-handling-extension-points.md).

## Migration

Applications currently copying `shared/exception` classes should follow
[Migrate From shared/exception](guides/migrate-shared-exception.md). The
migration removes local handlers and response builders after their behavior is
covered by the starter.

## Complete Example

The [error handling consumer](../examples/error-handling-consumer/README.md)
contains a runnable catalog, multiple validation failures, downstream failure,
unexpected exception, and Spring Security integration.
