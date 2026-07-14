# Spring Boot Service Framework HTTP Client Core

Framework-independent HTTP client domain model, ports, policies, and exceptions
for SMB Tech services.

This module is the hexagonal core for HTTP client behavior. It defines what an
HTTP client is, how it is configured, how downstream errors are represented, and
which ports adapters must implement. It intentionally does not create HTTP
clients by itself. Runtime integration with Spring Boot, `RestClient`, Apache
HTTP Client, Jackson, Micrometer, SSL stores, and property binding belongs in
`spring-boot-service-framework-starters/spring-boot-service-framework-starter-rest-client`.

## Module coordinates

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-http-client-core:0.2.0'
}
```

When used through the Spring Boot starter, consumers usually do not need to
depend on this module directly because the starter exposes it transitively.

## When to use

Use this module directly when you are building or testing framework-level HTTP
client adapters and need the neutral model, policies, ports, or exception types.

Most Spring Boot applications should consume
`spring-boot-service-framework-starter-rest-client` instead. The starter creates
runtime `RestClient` instances, wires OAuth2, applies SSL, emits metrics/audit
events, and exposes the core types transitively.

## Design goals

- Keep the HTTP client model independent from Spring and other adapter
  frameworks.
- Provide stable domain records for generated HTTP clients.
- Define input and output ports for adapters.
- Represent downstream HTTP failures in a structured, inspectable way.
- Preserve the complete downstream error body in `HttpClientResponseException`
  when body capture is enabled by the adapter.
- Keep logs/audit truncation separate from exception payload capture.
- Keep the API suitable for humans and for AI agents that need to reason about
  the framework structure.

## Hexagonal boundaries

This module may depend on small framework-neutral shared utilities, currently:

- `spring-boot-service-framework-commons`

This module must not import adapter/runtime APIs such as:

- Spring: `org.springframework.*`
- SLF4J or Logback: `org.slf4j.*`, `ch.qos.logback.*`
- Jackson: `com.fasterxml.jackson.*`
- Servlet APIs: `jakarta.servlet.*`

The Gradle task `verifyHexagonalBoundaries` enforces this rule during `check`.

```bash
./gradlew :spring-boot-service-framework-http-client-core:check
```

## Package map

```text
com.smbtech.serviceframework.httpclient
├── domain      # Framework-neutral records and policies
├── exception   # Core exceptions exposed to consuming services
├── port.in     # Inbound ports used by application/service code
├── port.out    # Outbound ports implemented by adapters
└── service     # Small core services and validators
```

## Main domain concepts

### `HttpClientDefinition`

`HttpClientDefinition` is the central configuration aggregate for one named HTTP
client.

It contains:

- client identity: `name`, `beanName`, `baseUrl`
- client technology hint: `clientType`
- authentication: `authenticationType`, `basicAuthentication`,
  `credentialTokenRequestorId`, `scopes`
- timeouts: `TimeoutPolicy`
- pooling: `PoolingPolicy`
- Apache-specific intent: `ApacheHttpClientPolicy`
- error behavior: `ErrorHandlingPolicy`
- metrics/tracing intent: `ObservabilityPolicy`
- retry/circuit breaker intent: `ResiliencePolicy`
- audit intent: `AuditPolicy`
- static headers: `defaultHeaders`

Example:

```java
HttpClientDefinition definition = new HttpClientDefinition(
        "payments",
        null,
        URI.create("https://payments.example"),
        ClientType.DEFAULT,
        AuthenticationType.NO_AUTH,
        new BasicAuthentication("", ""),
        "",
        "",
        TimeoutPolicy.defaults(),
        PoolingPolicy.defaults(),
        ApacheHttpClientPolicy.defaults(),
        ErrorHandlingPolicy.defaults(),
        ObservabilityPolicy.defaults(),
        ResiliencePolicy.disabled(),
        AuditPolicy.disabled(),
        Map.of("X-Application-Name", "orders-service")
);

String beanName = definition.beanName(); // paymentsRestClient
```

### `ErrorHandlingPolicy`

`ErrorHandlingPolicy` controls how adapters should represent downstream HTTP
errors.

Defaults:

```java
ErrorHandlingPolicy policy = ErrorHandlingPolicy.defaults();
```

Default behavior:

- `enabled=true`
- `includeBody=true`
- `includeHeaders=true`
- `includeNotificationMetadata=true`
- `notificationCodePrefix=E_SERVICE_FRAMEWORK_HTTP_CLIENT_`

Important rule: when `includeBody=true`, adapters should preserve the complete
downstream response body in `HttpErrorResponse.body()`. Truncation is an
audit/logging concern and must not silently reduce the body carried by
`HttpClientResponseException`.

Example custom policy:

```java
ErrorHandlingPolicy policy = new ErrorHandlingPolicy(
        true,
        true,
        4096,
        false,
        false,
        "E_PAYMENTS_HTTP_CLIENT"
);
```

This produces notification codes such as:

```text
E_PAYMENTS_HTTP_CLIENT_0400
E_PAYMENTS_HTTP_CLIENT_0503
```

while omitting response headers and notification metadata.

### `HttpErrorResponse`

`HttpErrorResponse` is the framework-neutral representation of a downstream HTTP
error.

It contains:

- `clientName`
- `method`
- `uri`
- `statusCode`
- `reasonPhrase`
- `category`
- `headers`
- `body`
- `contentType`
- `charset`
- `bodyTruncated`

Example:

```java
HttpErrorResponse error = new HttpErrorResponse(
        "payments",
        "POST",
        "https://payments.example/v1/orders",
        400,
        "Bad Request",
        HttpErrorCategory.CLIENT_ERROR,
        Map.of("Content-Type", "application/json"),
        "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Invalid order\"}",
        "application/json",
        "UTF-8",
        false
);
```

### `HttpClientResponseException`

`HttpClientResponseException` is the standard exception for downstream HTTP
responses that are considered errors by an adapter.

It extends `NotifyingException` from `spring-boot-service-framework-commons`, so it can carry
one or more structured `Notification` values.

Example:

```java
try {
    // call adapter-created client
} catch (HttpClientResponseException exception) {
    int status = exception.statusCode();
    String completeBody = exception.responseBody();
    String errorBody = exception.getErrorResponseAsString();
    Map<String, String> headers = exception.responseHeaders();

    exception.primaryNotification().ifPresent(notification -> {
        String code = notification.code();
        String message = notification.message();
    });
}
```

Convenience methods:

- `error()`
- `statusCode()`
- `responseBody()`
- `getErrorResponseAsString()`
- `getJsonErrorResponseAsObject(Class<T> type)`
- `getJsonErrorResponseAsObject(Type type)`
- `responseHeaders()`
- `responseCharset()`
- `responseContentType()`
- `responseStatusText()`
- `isResponseBodyTruncated()`
- `notifications()`
- `primaryNotification()`

JSON decoding is exposed through the core `HttpErrorResponseBodyReader` port.
The core module does not depend on Jackson or Spring. Runtime adapters can
attach a reader when creating the exception:

```java
HttpClientResponseException exception = new HttpClientResponseException(error, notifications)
        .withErrorResponseBodyReader(errorResponseBodyReader);

DownstreamError payload = exception.getJsonErrorResponseAsObject(DownstreamError.class);
```

If no reader is attached, `getJsonErrorResponseAsObject(...)` raises
`HttpErrorResponseBodyReaderNotConfiguredException`.

### `HttpErrorNotificationMapper`

Maps an `HttpErrorResponse` into a structured notification.

```java
HttpErrorNotificationMapper mapper = new HttpErrorNotificationMapper();
Notification notification = mapper.notification(error);

notification.code();    // E_SERVICE_FRAMEWORK_HTTP_CLIENT_0400
notification.message(); // HTTP 400 Bad Request response received from downstream service
```

The mapper intentionally does not copy the response body into notification
metadata. If application code needs to inspect the downstream payload, it should
use:

```java
exception.responseBody();
exception.error().body();
```

### Authentication model

The core supports these authentication types:

- `NO_AUTH`
- `BASIC_AUTH`
- `CLIENT_CREDENTIALS`
- `JWT_BEARER`
- `OTHER`

The core also defines framework-neutral token records used by adapters:

- `TokenRequestDefinition`
- `GrantType`
- `ClientAuthenticationMethod`
- `JwtBearerDefinition`
- `AccessToken`

In the Spring Boot REST client starter, OAuth2 provider and registration
configuration comes from Spring Security OAuth2 Client. The starter maps Spring
registrations into these core records internally when it needs to request or
validate tokens. Consuming services should configure OAuth2 registrations in
Spring Boot properties, not by constructing these records directly.

Adapter-side example:

```java
TokenRequestDefinition tokenRequest = new TokenRequestDefinition(
        "payments-token",
        URI.create("https://auth.example/oauth2/token"),
        GrantType.CLIENT_CREDENTIALS,
        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
        "client-id",
        "client-secret",
        Set.of("payments.read"),
        Duration.ofSeconds(30),
        JwtBearerDefinition.empty()
);
```

### SSL model

SSL is described by identifiers, not loaded directly by the core.

```java
SslPolicy ssl = new SslPolicy(
        true,
        "payments-truststore",
        "payments-keystore"
);

ssl.usesConfiguredStores(); // true
```

Actual key/trust store loading is an adapter responsibility.

### Resilience model

The core describes retry and circuit breaker intent, but does not perform
resilience behavior itself.

```java
ResiliencePolicy resilience = new ResiliencePolicy(
        true,
        new RetryPolicy(true, 3, Duration.ofMillis(100), true, true, Set.of(429)),
        new CircuitBreakerPolicy(true, 3, Duration.ofSeconds(30))
);
```

If an adapter blocks a call because the circuit is open, it should throw:

```java
throw new CircuitBreakerOpenException("payments");
```

Message:

```text
Circuit breaker is open for HTTP client: payments
```

### Audit model

The core defines audit event records and an outbound sink port:

- `HttpExchangeAuditRequest`
- `HttpExchangeAuditResponse`
- `HttpExchangeAuditFailure`
- `HttpExchangeAuditSink`

Adapters decide when to emit events, how to truncate bodies, and where to send
the data.

```java
public final class MyAuditSink implements HttpExchangeAuditSink {

    @Override
    public void failure(HttpClientDefinition definition, HttpExchangeAuditFailure event) {
        // Send event to logs, SIEM, database, or another sink.
    }
}
```

### Key store model

`KeyStoreDefinition` describes a framework-neutral key/trust store. Runtime
adapters decide how to load the content, but the password semantics are part of
the domain contract:

- `password` is the store password used to load the `JKS` or `PKCS12`
  container.
- `keyAlias` identifies the private key entry used for mTLS or JWT signing.
- `keyPassword` is the private key password for that alias.
- When `keyPassword` is empty, it falls back to `password`.

This allows adapters to support stores where the keystore password and private
key password are different, especially `JKS` stores.

## Ports

### Inbound ports

Inbound ports are used by application code or adapter orchestration.

| Port | Responsibility |
|---|---|
| `HttpClientCatalog` | Query available client definitions. |
| `HttpClientDefinitionValidator` | Validate one client definition. |

### Outbound ports

Outbound ports are implemented by adapters.

| Port | Responsibility |
|---|---|
| `HttpClientDefinitionSource` | Load client definitions. |
| `CredentialDefinitionSource` | Load configured credential definitions. |
| `CredentialProvider` | Resolve secret values. |
| `AccessTokenProvider` | Resolve access tokens for authenticated clients. |
| `KeyStoreDefinitionSource` | Load key/trust store definitions. |
| `CorrelationHeadersProvider` | Provide trace/correlation headers. |
| `HttpExchangeAuditSink` | Receive audit events. |

## Core services

### `DefaultHttpClientCatalog`

Loads definitions from `HttpClientDefinitionSource`, validates them, and exposes
an immutable catalog.

```java
HttpClientCatalog catalog = new DefaultHttpClientCatalog(source, validator);
HttpClientDefinition payments = catalog.requireByName("payments");
```

### `DefaultHttpClientDefinitionValidator`

Validates required fields:

- `name`
- `beanName`
- `baseUrl`
- basic auth username/password when `BASIC_AUTH`
- `credentialTokenRequestorId` when `CLIENT_CREDENTIALS` or `JWT_BEARER`

### `ScopeValidator`

Parses and validates token scopes.

```java
ScopeValidator validator = new ScopeValidator();

validator.parse("payments.read payments.write");
validator.parse("payments.read,payments.write");
validator.validate("payments.read", Set.of("payments.read", "profile"));
```

## Adapter responsibilities

Adapters should:

- bind external configuration into core domain records;
- create concrete HTTP clients;
- apply authentication;
- add default and correlation headers;
- execute requests;
- map failed responses into `HttpErrorResponse`;
- throw `HttpClientResponseException`;
- generate notifications with `HttpErrorNotificationMapper`;
- apply retries and circuit breakers;
- emit audit and observability events;
- decode JSON error bodies outside the core.

Adapters should not:

- store Spring, Jackson, or Apache types in core records;
- truncate `HttpErrorResponse.body()` when the policy says to include the body;
- put raw response bodies into notification metadata;
- log secrets or complete payloads by default.

## Expected error flow

```text
Adapter receives HTTP error response
        |
        v
Map adapter response -> HttpErrorResponse
        |
        v
Map HttpErrorResponse -> Notification
        |
        v
Throw HttpClientResponseException(error, notifications)
        |
        v
Application can inspect status, headers, full body, and notification code
```

## AI implementation notes

If you are an AI agent modifying this module:

1. Keep this module framework-neutral.
2. Do not add Spring, Jackson, SLF4J, Logback, Servlet, or Apache HTTP Client
   imports here.
3. Add adapter-specific behavior to the REST client starter instead.
4. Preserve immutable records and defensive copies.
5. Do not silently truncate `HttpErrorResponse.body()`.
6. Keep notification metadata safe; do not duplicate raw bodies there.
7. Update this README when adding public records, ports, policies, or
   exceptions.
8. Run:

```bash
./gradlew :spring-boot-service-framework-http-client-core:check
./gradlew baseline
```

## Build and verification

Compile and test this module:

```bash
./gradlew :spring-boot-service-framework-http-client-core:check
```

Run the full framework baseline:

```bash
./gradlew baseline
```

Publish to the module-local Maven repository:

```bash
./gradlew :spring-boot-service-framework-http-client-core:publishAllPublicationsToLocalBuildRepository
```

Publish all framework artifacts locally:

```bash
./gradlew publishLocalArtifacts
```

## Publication

This module is configured with `maven-publish`.

By default, it publishes to:

```text
spring-boot-service-framework-http-client-core/build/repository
```

If these values are provided, it can also publish to a private Maven registry:

- Gradle property or environment variable: `privateMavenUrl` /
  `PRIVATE_MAVEN_URL`
- Gradle property or environment variable: `privateMavenUsername` /
  `PRIVATE_MAVEN_USERNAME`
- Gradle property or environment variable: `privateMavenPassword` /
  `PRIVATE_MAVEN_PASSWORD`

No public external publication is required for local development.
