# Pre-1.0 Migration Guide

This guide covers the source, binary, dependency, and configuration changes
introduced after `0.2.0` while the framework public boundary was standardized.
Apply these changes before upgrading to the next pre-1.0 release.

The removed names do not have compatibility aliases. This keeps ambiguous or
misplaced contracts out of the public API before `1.0.0`.

## Migration Order

1. Import the current framework platform and align with Spring Boot 4.1.
2. Migrate Jackson API imports from Jackson 2 to Jackson 3.
3. Update supported API signatures and renamed properties.
4. Replace direct construction of framework implementations with documented
   factories, interfaces, customizers, or bean names.
5. Run compilation, focused compatibility checks, and the complete release
   gate.

## Migration Summary

| Previous contract | Current contract | Required action |
|---|---|---|
| `smbtech.rest-clients.clients.<name>.credential-token-requestor-id` | `smbtech.rest-clients.clients.<name>.token-request-id` | Rename the property in every configuration source. |
| `HttpClientDefinition.credentialTokenRequestorId()` | `HttpClientDefinition.tokenRequestId()` | Update code that reads the framework-neutral client definition. |
| `com.smbtech.serviceframework.httpclient.exception.AuthenticationException` | `com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException` | Update imports, catches, assertions, and exception mappings. |
| `com.smbtech.serviceframework.starter.mock.api.mock.MockService` | `com.smbtech.serviceframework.starter.mock.api.MockService` | Update the import. Bean behavior and method signatures are unchanged. |
| `MockRestClientException` | `MockException` or `HttpClientResponseException`, according to ownership | Replace catches with the exception from the capability that actually failed. |

## Migrate Supported API Contracts

### Jackson 3

Spring Boot 4 runtime APIs use Jackson 3. Update imports used with framework
contracts:

| Jackson 2 | Jackson 3 |
|---|---|
| `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` |
| `com.fasterxml.jackson.databind.JsonNode` | `tools.jackson.databind.JsonNode` |
| `com.fasterxml.jackson.core.type.TypeReference` | `tools.jackson.core.type.TypeReference` |
| `com.fasterxml.jackson.core.JsonGenerator` | `tools.jackson.core.JsonGenerator` |
| `com.fasterxml.jackson.databind.SerializerProvider` | `tools.jackson.databind.SerializationContext` |

This affects constructors and methods on `OpenApiContractLoader`,
`OpenApiMvcContractTester`, `NotificationSerializer`, `MockService`, and
`HttpErrorBodyDecoder`. Generated model annotations remain under
`com.fasterxml.jackson.annotation`; do not migrate those annotations.

### JWT Bearer Requests

Custom `AccessTokenClient` implementations must implement the canonical
request method:

```java
@Override
public AccessToken jwtBearer(JwtBearerTokenRequest request) {
    return tokenService.exchange(
            request.tokenRequestId(),
            request.expectedScopes(),
            request.customClaims()
    );
}
```

The string overloads remain convenience methods and delegate to
`JwtBearerTokenRequest`. Callers that need dynamic claims should construct that
record explicitly.

### Error Reporter Composition

`CompositeErrorReporter` is no longer a public construction contract. Register
one or more `ErrorReporter` beans and let auto-configuration compose them, or
use `ErrorReporter.composite(reporters)` outside Spring:

```java
ErrorReporter reporter = ErrorReporter.composite(reporters);
```

Ordering remains controlled by `ErrorReporter.order()`.

## Migrate Direct Implementation References

The following implementation types were also normalized. They are not
supported extension points, but applications that referenced them directly
must update or remove those references:

| Previous implementation name | Current implementation name |
|---|---|
| `error.DefaultNotificationAggregationPolicy` | `NotificationAggregationPolicy.defaultPolicy()` |
| `error.DefaultNotificationSanitizer` | `NotificationSanitizer.defaultSanitizer()` or `withMetadataAllowlist(...)` |
| `error.FallbackThrowableErrorResolver` | `ThrowableErrorResolver.fallback()` |
| `error.ServiceExceptionThrowableErrorResolver` | `ThrowableErrorResolver.serviceExceptions(...)` |
| `error.ThrowableErrorResolutionPipeline` | `ThrowableErrorResolver.composite(...)` |
| Error handling MVC, security, serialization, logging, metrics, or composition adapters | Replace the corresponding public API interface or documented Spring bean. |
| `TransactionalIdFilter` or `TransactionIdFilter` | No public replacement; configure `smbtech.logging.transaction.*`. |
| `SmbStructuredLogFormatter` | `ServiceFrameworkStructuredLogFormatter` |
| `starter.mock.adapter.in.restclient.*` | `starter.mock.adapter.out.restclient.*` |
| `httpclient.service.DefaultHttpClientCatalog` | `HttpClientCatalog.from(...)` |
| `httpclient.service.DefaultHttpClientDefinitionValidator` | `HttpClientDefinitionValidator.defaultValidator()` |
| `httpclient.service.ScopeValidator` | No public replacement; OAuth2 scope validation is owned by the REST client starter. |
| `logging.application.StructuredLoggingService` | `StructuredLogger.create(...)` |
| Logging SLF4J, MDC, servlet, or metrics adapters | No public replacement; replace the documented ports or framework bean contracts. |
| `mock.service.DefaultMockCatalog` | `MockCatalog.from(...)` |
| `mock.service.DefaultMockResponder` | `MockResponder.from(...)` |
| `MockRestClientInterceptor` | `@Qualifier("mockRestClientInterceptor") ClientHttpRequestInterceptor` |
| Other Mock starter adapter implementations | No public replacement; use `MockService`, core ports, properties, or standard Spring contracts. |
| `project.generator.internal.DefaultHexagonalProjectGenerator` | `HexagonalProjectGenerator.create()` |
| `OpenApiArtifactKind` | No public replacement; configure `publishModels`, `publishServerApi`, and `publishClient` through the plugin DSL. |
| `actuator.service.DefaultFrameworkDiagnostics` | `FrameworkDiagnostics.from(...)` |
| Actuator endpoint, health, info, metrics, guard, or integration adapters | Replace `FrameworkDiagnostics`, `DiagnosticProbe`, `FrameworkModuleInfoProvider`, or the documented framework bean name. |

REST client classes under `adapter.out.apache` and
`adapter.out.authentication.spring` were implementation details despite being
public before 1.0. Applications must replace direct construction with the
public customizers and OAuth2 SPIs documented in
[REST Client Extension Points](../rest-client-extension-points.md).

Prefer the supported logging APIs, mock ports, `MockService`, and REST client
customizers instead of importing implementation packages.

Commons no longer exports `logging-core` or SLF4J transitively. Applications
that use logging APIs directly must declare the appropriate logging dependency
instead of relying on Commons.

## Rename The OAuth2 Token Request Property

Before:

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        authentication-type: CLIENT_CREDENTIALS
        credential-token-requestor-id: payments-token
```

After:

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        authentication-type: CLIENT_CREDENTIALS
        token-request-id: payments-token
```

The value still identifies a registration under
`spring.security.oauth2.client.registration`. Only the property name changed.
The same migration applies to JWT bearer clients.

Spring Boot relaxed binding also changes the equivalent environment variable:

```text
SMBTECH_REST_CLIENTS_CLIENTS_PAYMENTS_CREDENTIAL_TOKEN_REQUESTOR_ID
```

becomes:

```text
SMBTECH_REST_CLIENTS_CLIENTS_PAYMENTS_TOKEN_REQUEST_ID
```

Search deployment manifests, Helm values, ConfigMaps, CI variables, tests, and
configuration-server entries. Keeping only the old key causes startup
validation to report a missing `token-request-id`.

Code that reads the mapped core definition must use the renamed record
accessor:

```java
String registrationId = definition.tokenRequestId();
```

## Rename The HTTP Client Authentication Exception

Before:

```java
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;

try {
    tokenClient.clientCredentials("payments-token");
} catch (AuthenticationException exception) {
    // application recovery policy
}
```

After:

```java
import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;

try {
    tokenClient.clientCredentials("payments-token");
} catch (HttpClientAuthenticationException exception) {
    // application recovery policy
}
```

The explicit name avoids confusion with Spring Security's
`org.springframework.security.core.AuthenticationException`. Error resolvers
that classify outbound OAuth2, signing, keystore, or scope failures must also
match `HttpClientAuthenticationException`.

## Move MockService To The Public API Root

Before:

```java
import com.smbtech.serviceframework.starter.mock.api.mock.MockService;
```

After:

```java
import com.smbtech.serviceframework.starter.mock.api.MockService;
```

No method migration is required. `response(...)`, `responseOrNotFound(...)`,
and `exchangeMock(...)` retain their behavior.

`MockRestClientException` was removed because it did not represent an owned,
active mock contract. Use:

- `com.smbtech.serviceframework.mock.exception.MockException` for failures from
  mock definitions, sources, or responders;
- `HttpClientResponseException` for downstream HTTP responses;
- application-specific exceptions for business fallback decisions.

## Update Direct Infrastructure References

The logging starter's bundled Logback configuration already points to
`ServiceFrameworkStructuredLogFormatter`. A consuming application only needs a
change when its own `logback-spring.xml` names the formatter directly:

```xml
<format>com.smbtech.serviceframework.starter.logging.adapter.out.logback.ServiceFrameworkStructuredLogFormatter</format>
```

Transaction id propagation remains auto-configured. Applications should
configure `smbtech.logging.transaction.*` instead of constructing its filter.

Outbound mock adapters now live under `adapter.out.restclient` because they
invoke an external HTTP boundary. These classes remain implementation details.
Use `MockResponder` for a neutral replacement or `RestClientBuilderCustomizer`
for supported REST client composition.

## Verify The Migration

Search for removed names:

```bash
rg 'credential-token-requestor-id|httpclient\.exception\.AuthenticationException|api\.mock\.MockService|MockRestClientException|CompositeErrorReporter|DefaultNotificationAggregationPolicy|DefaultNotificationSanitizer|FallbackThrowableErrorResolver|ServiceExceptionThrowableErrorResolver|ThrowableErrorResolutionPipeline|DefaultHttpClientCatalog|StructuredLoggingService|DefaultMockCatalog|DefaultFrameworkDiagnostics|OpenApiArtifactKind|TransactionalIdFilter|SmbStructuredLogFormatter|adapter\.in\.restclient'
```

Refresh generated references and reviewed compatibility baselines when changing
the framework repository itself:

```bash
./gradlew generatePropertyReferences
./gradlew generatePublicApiInventory
./gradlew generateModuleCompatibilityContracts
```

Run the affected focused checks and the complete compatibility lifecycle:

```bash
./gradlew loggingCompatibilityCheck
./gradlew httpClientCompatibilityCheck
./gradlew mockCompatibilityCheck
./gradlew actuatorCompatibilityCheck
./gradlew errorHandlingCompatibilityCheck
./gradlew binaryCompatibilityCheck
./gradlew compatibilityCheck
```
