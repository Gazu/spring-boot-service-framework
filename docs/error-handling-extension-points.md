# Error Handling Extension Points

The starter provides defaults through auto-configuration and backs off when an
application supplies a bean for a replaceable contract. Prefer the smallest
extension point that solves the requirement.

## Core Policies

| Contract | Purpose | Composition |
|---|---|---|
| `ThrowableErrorResolver` | Convert a supported exception into `ResolvedError`. | Multiple beans, ordered by `order()`; first supporting resolver wins. |
| `NotificationAggregationPolicy` | Select a primary notification and aggregate field violations. | Single replaceable bean. |
| `NotificationSanitizer` | Apply allowlisting and redaction before writing a response. | Single replaceable bean. |
| `ErrorExposurePolicy` | Select the final `PUBLIC` or `INTERNAL` response audience. | Single replaceable bean; the configured default is applied after resolvers and resolved-error customizers. |

Custom resolver example:

```java
@Component
final class InventoryExceptionResolver implements ThrowableErrorResolver {
    public boolean supports(Throwable failure) {
        return failure instanceof InventoryUnavailableException;
    }

    public ResolvedError resolve(Throwable failure) {
        return new ResolvedError(
                Notification.error("E_INVENTORY_0001", "Inventory is unavailable"),
                ErrorCategory.DOWNSTREAM,
                ErrorExposure.PUBLIC,
                failure.getMessage()
        );
    }

    public int order() {
        return -1000;
    }
}
```

The `ErrorExposure` stored by a resolver is not the final response decision.
The global `ErrorExposurePolicy` overwrites it after all
`ResolvedErrorCustomizer` beans run. The default policy uses
`smbtech.error-handling.response.exposure`, whose default is `PUBLIC`.

## Request-Aware Customizers

| Contract | Runs | Composition |
|---|---|---|
| `ResolvedErrorCustomizer` | After resolution, before reporting and response creation. | Multiple ordered beans. |
| `NotificationResponseCustomizer` | After response creation, before returning or writing. | Multiple ordered beans. |

Customizers must return non-null values. Use them for correlation metadata,
application response headers, or bounded policy changes. Do not add secrets,
request bodies, credentials, or raw exception details. Response customizers
are followed by a mandatory final sanitization pass that preserves customized
HTTP status and headers while reapplying exposure, metadata allowlisting, and
secret redaction to the `Notification` body.

## Reporting And Metrics

| Contract | Purpose | Composition |
|---|---|---|
| `ErrorReporter` | Emit logs, audit events, or external reports. | Multiple ordered beans composed by the starter. |
| `ErrorMetricsRecorder` | Record bounded-cardinality metrics after status resolution. | Single replaceable bean. |

Reporter and metrics failures are isolated and do not replace the original
notification response.

## HTTP And Serialization

| Contract | Purpose | Replacement behavior |
|---|---|---|
| `NotificationHttpStatusResolver` | Map `ResolvedError` to an HTTP status. | Replaces the default category mapping. |
| `NotificationResponseFactory` | Build `ResponseEntity<Notification>`. | Replaces status, exposure, and response construction policy. |
| `NotificationSerializer` | Serialize the notification body. | Replaces the default snake-case JSON serializer. |
| `NotificationResponseWriter` | Write security responses to the servlet response. | Replaces the isolated JSON writer. |

Replacing `NotificationSerializer` changes a public wire contract. Treat that
as an application API decision and cover it with contract tests.

Replacing `NotificationResponseFactory` or `NotificationSerializer` makes the
application responsible for preserving the stable code and preventing
diagnostics, causes, credentials, and request or downstream payloads from
reaching the response. `NotificationResponseCustomizer` output remains behind
the framework's final response safety boundary.

## Security

The starter registers `AuthenticationEntryPoint` and `AccessDeniedHandler`
defaults only when Spring Security is present and no application bean of the
same interface exists. An application may replace either bean independently.

Smaller security changes use these public contracts:

| Contract | Purpose |
|---|---|
| `SecurityAuthenticationFailureResolver` | Classify authentication exceptions without parsing provider messages. |
| `SecurityAuthorizationFailureResolver` | Classify access-denied and CSRF exceptions. |
| `RequiredScopeResolver` | Supply scopes required by the protected operation. |
| `OAuth2SecurityMetadataFactory` | Build safe security and OAuth2 response metadata. |
| `OAuth2SecurityChallengeWriter` | Write the RFC 6750 `WWW-Authenticate` header. |

See [Security Error Handling](error-handling/security.md) for the catalog,
metadata examples, configuration, and replacement rules.

## Replacement Example

```java
@Bean
NotificationHttpStatusResolver applicationStatusResolver() {
    return error -> error.category() == ErrorCategory.CONFLICT
            ? HttpStatus.UNPROCESSABLE_CONTENT
            : HttpStatus.INTERNAL_SERVER_ERROR;
}
```

The application bean is discovered before the default and prevents the default
resolver bean from being created.

## Stable Public Boundary

The supported extension boundary consists of:

- `com.smbtech.serviceframework.error` core contracts and models;
- `com.smbtech.serviceframework.starter.errorhandling.api` interfaces;
- `ErrorHandlingProperties` configuration keys;
- the default snake-case JSON contract.

Adapter and auto-configuration implementation packages are inspectable but are
not preferred extension points. Compatibility is verified by
`ErrorHandlingPublicApiCompatibilityTest` and the root `compatibilityCheck`.

Applications migrating copied handlers should follow
[Migrate From shared/exception](guides/migrate-shared-exception.md). Applications
that catch the renamed outbound HTTP authentication exception should also follow
[Migrate Public Names And Properties](guides/migrate-public-names-and-properties.md)
and update custom `ThrowableErrorResolver` implementations to match
`HttpClientAuthenticationException`.
