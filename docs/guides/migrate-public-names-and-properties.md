# Migrate Public Names And Properties

This guide covers the source and configuration changes introduced after
`0.2.0` while the framework public boundary was standardized. Apply these
changes before upgrading to the next release.

The removed names do not have compatibility aliases. This keeps ambiguous or
misplaced contracts out of the public API before `1.0.0`.

## Migration Summary

| Previous contract | Current contract | Required action |
|---|---|---|
| `smbtech.rest-clients.clients.<name>.credential-token-requestor-id` | `smbtech.rest-clients.clients.<name>.token-request-id` | Rename the property in every configuration source. |
| `HttpClientDefinition.credentialTokenRequestorId()` | `HttpClientDefinition.tokenRequestId()` | Update code that reads the framework-neutral client definition. |
| `com.smbtech.serviceframework.httpclient.exception.AuthenticationException` | `com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException` | Update imports, catches, assertions, and exception mappings. |
| `com.smbtech.serviceframework.starter.mock.api.mock.MockService` | `com.smbtech.serviceframework.starter.mock.api.MockService` | Update the import. Bean behavior and method signatures are unchanged. |
| `MockRestClientException` | `MockException` or `HttpClientResponseException`, according to ownership | Replace catches with the exception from the capability that actually failed. |

The following implementation types were also normalized. They are not
supported extension points, but applications that referenced them directly
must update or remove those references:

| Previous implementation name | Current implementation name |
|---|---|
| `TransactionalIdFilter` | `TransactionIdFilter` |
| `SmbStructuredLogFormatter` | `ServiceFrameworkStructuredLogFormatter` |
| `starter.mock.adapter.in.restclient.*` | `starter.mock.adapter.out.restclient.*` |

Prefer the supported logging APIs, mock ports, `MockService`, and REST client
customizers instead of importing implementation packages.

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

`TransactionIdFilter` remains auto-configured. Applications should configure
`smbtech.logging.transaction.*` instead of constructing the filter.

Outbound mock adapters now live under `adapter.out.restclient` because they
invoke an external HTTP boundary. These classes remain implementation details.
Use `MockResponder` for a neutral replacement or `RestClientBuilderCustomizer`
for supported REST client composition.

## Verify The Migration

Search for removed names:

```bash
rg 'credential-token-requestor-id|httpclient\.exception\.AuthenticationException|api\.mock\.MockService|MockRestClientException|TransactionalIdFilter|SmbStructuredLogFormatter|adapter\.in\.restclient'
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
./gradlew compatibilityCheck
```
