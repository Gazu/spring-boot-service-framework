# REST Client starter guide

This guide is the canonical human and AI reference for
`com.smbtech:spring-boot-service-framework-starter-rest-client`.

The starter reads `application.yml`, builds named Spring `RestClient` instances,
registers them as beans, exposes them through `RestClientRegistry`, and can
create declarative Spring HTTP interface proxies through `ApiClientFactory`.

The framework-neutral model lives in `spring-boot-service-framework-http-client-core`. This
starter is the adapter layer: Spring Boot property binding, `RestClient`,
Apache HTTP Client, SSL stores, OAuth2 token acquisition, audit logs,
Micrometer metrics, optional retry, and optional circuit breaker behavior.

---

## 1. Dependency and local publication

For local development, publish the framework artifacts into Maven local:

```bash
./gradlew publishToMavenLocal
```

Then consume the starter from another service:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-starter-rest-client:0.1.0-SNAPSHOT'
}
```

The repository also supports module-local build repositories used by the smoke
examples:

```bash
./gradlew publishLocalArtifacts
```

When using module-local repositories instead of `mavenLocal()`, include every
framework artifact repository required by transitive dependencies:

```groovy
repositories {
    maven {
        url = uri('../spring-boot-service-framework/spring-boot-service-framework-starters/spring-boot-service-framework-starter-rest-client/build/repository')
    }
    maven {
        url = uri('../spring-boot-service-framework/spring-boot-service-framework-http-client-core/build/repository')
    }
    maven {
        url = uri('../spring-boot-service-framework/spring-boot-service-framework-commons/build/repository')
    }
    maven {
        url = uri('../spring-boot-service-framework/spring-boot-service-framework-logging-core/build/repository')
    }
    mavenCentral()
}
```

When a private Maven registry is available, replace the local repositories with
that registry. No public external publication is required.

---

## 2. Minimal client

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        default-headers:
          X-Application-Name: projects-service
```

This registers:

- a `RestClient` bean named `paymentsRestClient`;
- a `payments` entry in `RestClientRegistry`;
- a configured client available through `ApiClientFactory`.

Inject the generated bean directly with `@Qualifier`:

```java
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
class PaymentsService {

    private final RestClient payments;

    PaymentsService(@Qualifier("paymentsRestClient") RestClient payments) {
        this.payments = payments;
    }
}
```

Override the generated bean name when a service needs a specific name:

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        bean-name: paymentsApiRestClient
        base-url: https://payments.example
```

---

## 3. Runtime access patterns

### `RestClientRegistry`

Use the registry when a service needs to choose a client dynamically:

```java
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
class DynamicHttpService {

    private final RestClientRegistry registry;

    DynamicHttpService(RestClientRegistry registry) {
        this.registry = registry;
    }

    String call(String clientName) {
        RestClient client = registry.get(clientName);
        return client.get()
                .uri("/dummy")
                .retrieve()
                .body(String.class);
    }
}
```

Available methods:

| Method | Purpose |
|---|---|
| `get(String name)` | Returns the configured `RestClient` for a client name. |
| `names()` | Returns all registered client names. |
| `all()` | Returns the registered clients as a map. |

### Declarative HTTP interfaces

```java
import com.smbtech.serviceframework.starter.restclient.api.HttpApiClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpApiClient("payments")
@HttpExchange
public interface PaymentsApi {

    @GetExchange("/dummy")
    String dummy();
}
```

```java
import com.smbtech.serviceframework.starter.restclient.api.ApiClientFactory;
import org.springframework.stereotype.Service;

@Service
class PaymentsFacade {

    private final PaymentsApi paymentsApi;

    PaymentsFacade(ApiClientFactory apiClientFactory) {
        this.paymentsApi = apiClientFactory.create(PaymentsApi.class);
    }

    String dummy() {
        return paymentsApi.dummy();
    }
}
```

You can also bypass `@HttpApiClient` and pass the configured client name
explicitly:

```java
PaymentsApi paymentsApi = apiClientFactory.create("payments", PaymentsApi.class);
```

---

## 4. Authentication

### Basic authentication

Use inline values for non-secret examples, or `*-ref` properties for credentials
defined in `smbtech.rest-clients.authentication.credentials`.

```yaml
smbtech:
  rest-clients:
    clients:
      secure:
        base-url: https://secure.example
        authentication-type: BASIC_AUTH
        basic-authentication:
          username-ref: secure-username
          password-ref: secure-password
    authentication:
      credentials:
        secure-username:
          value: demo
        secure-password:
          value: ${SECURE_PASSWORD}
```

### Client credentials access token

Target configuration uses Spring Boot OAuth2 Client for OAuth2 provider and
registration data. `credential-token-requestor-id` must match the Spring
Security `registration-id`.

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          my-provider:
            token-uri: https://auth.example/oauth2/token
        registration:
          payments-token:
            provider: my-provider
            client-id: payments-client
            client-secret: ${PAYMENTS_CLIENT_SECRET}
            client-authentication-method: client_secret_basic
            authorization-grant-type: client_credentials
            scope:
              - payments.read
              - payments.write

smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        authentication-type: CLIENT_CREDENTIALS
        credential-token-requestor-id: payments-token
        scopes: payments.read payments.write
```

For `private_key_jwt`, keep the OAuth2 registration in Spring properties and add
the signing extension under `smbtech.rest-clients.authentication.client-assertions`:

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          my-provider:
            token-uri: https://auth.example/oauth2/token
            jwk-set-uri: https://auth.example/oauth2/certs
        registration:
          payments-token:
            provider: my-provider
            client-id: payments-client
            client-authentication-method: private_key_jwt
            authorization-grant-type: client_credentials
            scope:
              - payments.read

smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        authentication-type: CLIENT_CREDENTIALS
        credential-token-requestor-id: payments-token
        scopes: payments.read
    authentication:
      client-assertions:
        payments-token:
          key-store-id: payments-signing-key
          token-lifetime: 60s
          custom-claims:
            acgp: acgp.ct
      key-stores:
        payments-signing-key:
          base64: ${PAYMENTS_SIGNING_KEY_JKS_BASE64}
          type: JKS
          password-ref: signing-store-password
          key-alias: auth
          key-password-ref: signing-key-password
      credentials:
        signing-store-password:
          base64: ${PAYMENTS_SIGNING_STORE_PASSWORD_BASE64}
        signing-key-password:
          base64: ${PAYMENTS_SIGNING_KEY_PASSWORD_BASE64}
```

`password-ref` is used to open the keystore. `key-password-ref` is used to
recover the private key entry identified by `key-alias`. They may point to
different credential values, which is common for `JKS` stores.

OAuth2 token acquisition is Spring-registration based. The consuming
application must provide Spring Boot OAuth2 Client registrations under
`spring.security.oauth2.client.registration`. The starter resolves those
registrations by the id configured in
`smbtech.rest-clients.clients.<name>.credential-token-requestor-id`.

The starter exposes Spring Boot OAuth2 Client auto-configuration through
`spring-boot-starter-oauth2-client`. It intentionally does not bind
`spring.security.oauth2.client.*` directly. It only consumes the
`ClientRegistrationRepository` bean produced by Spring Boot OAuth2 Client
auto-configuration.

Supported token flows:

| Flow | Spring registration grant | SMBTech extension |
|---|---|---|
| Client credentials with client secret | `client_credentials` | none |
| Client credentials with `private_key_jwt` client authentication | `client_credentials` plus `client-authentication-method: private_key_jwt` | `authentication.client-assertions.<registration-id>` |
| JWT bearer grant | `urn:ietf:params:oauth:grant-type:jwt-bearer` | `authentication.jwt-bearer.<registration-id>` |

If no matching Spring registration exists, token acquisition fails fast with a
configuration error. Token requests are not read from SMBTech properties; the
token endpoint, client id, client secret, grant type, authentication method, and
requested scopes are owned by Spring Boot OAuth2 Client configuration.

Request a token directly when application code needs one:

```java
import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;

AccessToken token = accessTokenClient.clientCredentials("payments-token", "payments.read");
String bearer = token.value();
```

### JWT bearer access token

For `urn:ietf:params:oauth:grant-type:jwt-bearer`, define the OAuth2 client
registration with Spring Boot and place JWT assertion details under
`smbtech.rest-clients.authentication.jwt-bearer`.

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          my-provider:
            token-uri: https://auth.example/oauth2/token
        registration:
          payments-jwt-token:
            provider: my-provider
            client-id: payments-client
            client-authentication-method: none
            authorization-grant-type: urn:ietf:params:oauth:grant-type:jwt-bearer
            scope:
              - payments.write

smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        authentication-type: JWT_BEARER
        credential-token-requestor-id: payments-jwt-token
        scopes: payments.write
    authentication:
      jwt-bearer:
        payments-jwt-token:
          key-store-id: payments-signing-key
          issuer: payments-issuer
          subject: payments-subject
          audience: https://auth.example/oauth2/token
          token-lifetime: 2m
          custom-claims:
            tenant: payments
            channel: backend
            priority: 7
            audit: true
      key-stores:
        payments-signing-key:
          location: file:/opt/app/certs/signing-key.p12
          type: PKCS12
          password-ref: signing-store-password
          key-alias: auth
          key-password-ref: signing-key-password
      credentials:
        signing-store-password:
          value: ${SIGNING_STORE_PASSWORD}
        signing-key-password:
          value: ${SIGNING_KEY_PASSWORD}
```

The signing keystore may be `PKCS12` or `JKS`. For both formats, the store
password and private key password are resolved independently. If
`key-password`/`key-password-ref` is omitted, the framework falls back to the
store password.

The library generates registered JWT claims such as `iss`, `sub`, `aud`, `jti`,
`iat`, and `exp`. If any of those names are configured in `custom-claims`, they
are ignored to avoid duplicates.

For the target Spring-based flow, the token endpoint, client id, client
authentication method, and requested scopes come from
`spring.security.oauth2.client.registration.<registration-id>`. The SMBTech
`authentication.jwt-bearer.<registration-id>` block only describes how the JWT
grant assertion is created and signed.

Explicit token request:

```java
AccessToken token = accessTokenClient.jwtBearer("payments-jwt-token", "payments.write");
```

### Token cache and scope validation

The default cache is in-memory and stores tokens using a deterministic key:

```text
<registration-or-token-request-id>::<sorted requested scopes>
```

When no requested scopes exist, the key is only the registration or token
request id. This avoids collisions if the same OAuth2 client id is later used
with a different requested scope set.

`clients.<name>.scopes` and the `expectedScopes` argument in `AccessTokenClient`
do not change the scopes requested from the authorization server. They define
the minimum scopes that must be present in the returned access token. If the
token response omits the `scope` field in the Spring `client_credentials` flow,
the starter validates against the scopes declared in the Spring registration.

### OAuth2 troubleshooting

| Symptom or message | Meaning | Fix |
|---|---|---|
| `OAuth2 client registration not configured for token request: <id>` | No supported Spring registration exists for the `credential-token-requestor-id`. | Add `spring.security.oauth2.client.registration.<id>` or fix `credential-token-requestor-id`. |
| `OAuth2 client registration not configured for client_credentials: <id>` | `AccessTokenClient.clientCredentials(...)` was called for a missing registration or a registration with another grant. | Use a `client_credentials` registration id. |
| `OAuth2 client registration not configured for JWT bearer grant: <id>` | `AccessTokenClient.jwtBearer(...)` was called for a missing registration or a registration with another grant. | Use a registration whose `authorization-grant-type` is `urn:ietf:params:oauth:grant-type:jwt-bearer`. |
| `client assertion configuration not found for OAuth2 registration: <id>` | A `private_key_jwt` registration has no SMBTech signing extension. | Add `smbtech.rest-clients.authentication.client-assertions.<id>`. |
| `key-store-id is required for private_key_jwt client assertion: <id>` | The client assertion extension exists but does not point to a signing keystore. | Set `authentication.client-assertions.<id>.key-store-id`. |
| `jwt-bearer configuration not found for OAuth2 registration: <id>` | A JWT bearer registration has no SMBTech JWT assertion extension. | Add `smbtech.rest-clients.authentication.jwt-bearer.<id>`. |
| `Access token does not contain expected scopes` | The returned token does not include every scope required by `clients.<name>.scopes` or `AccessTokenClient` expected scopes. | Align requested scopes in Spring registration and expected scopes in SMBTech client config. |

---

## 5. SSL and HTTPS

For public HTTPS endpoints signed by a CA already trusted by the JVM, no custom
SSL configuration is required:

```yaml
smbtech:
  rest-clients:
    clients:
      oauth-certs:
        base-url: https://core-oauth-gateway.smb-tech.cl
```

Configure custom SSL when the downstream service requires a private truststore,
a client certificate, mTLS, or a non-default keystore.

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        client-type: APACHE_HTTP
        apache:
          hostname-verification-enabled: true
          connection-time-to-live: 5m
          validate-after-inactivity: 2s
          ssl:
            enabled: true
            trust-store-id: payments-trust
            key-store-id: payments-client-cert
    authentication:
      key-stores:
        payments-trust:
          location: file:/opt/app/certs/truststore.p12
          type: PKCS12
          password-ref: truststore-password
        payments-client-cert:
          location: file:/opt/app/certs/client.p12
          type: PKCS12
          password-ref: keystore-password
          key-alias: client
          key-password-ref: key-password
      credentials:
        truststore-password:
          value: ${PAYMENTS_TRUSTSTORE_PASSWORD}
        keystore-password:
          value: ${PAYMENTS_KEYSTORE_PASSWORD}
        key-password:
          value: ${PAYMENTS_KEY_PASSWORD}
```

Keystores and truststores can also be supplied as base64 content. This supports
both `JKS` and `PKCS12`.

```yaml
smbtech:
  rest-clients:
    authentication:
      key-stores:
        payments-trust:
          base64: ${PAYMENTS_TRUSTSTORE_JKS_BASE64}
          type: JKS
          password-ref: truststore-password
        payments-client-cert:
          base64: ${PAYMENTS_CLIENT_CERT_JKS_BASE64}
          type: JKS
          password-ref: keystore-password
          key-alias: client
          key-password-ref: key-password
      credentials:
        truststore-password:
          base64: ${PAYMENTS_TRUSTSTORE_PASSWORD_BASE64}
        keystore-password:
          base64: ${PAYMENTS_KEYSTORE_PASSWORD_BASE64}
        key-password:
          base64: ${PAYMENTS_KEY_PASSWORD_BASE64}
```

If both `base64` and `location` are configured, `base64` has priority.

Credential values referenced by `password-ref` or `key-password-ref` can be
configured as plain `value` or explicit `base64`. When both are configured,
`base64` has priority. This is useful when JKS/PKCS12 content and its store/key
passwords are all delivered as base64 environment variables.

Password semantics:

- `authentication.key-stores.<id>.password` or `password-ref` is the store
  password used to load the `JKS`/`PKCS12` container.
- `authentication.key-stores.<id>.key-password` or `key-password-ref` is the
  private key password used for mTLS key material and JWT signing keys.
- `key-password` defaults to the resolved store password only when no key
  password is configured.
- `JKS` stores can use different store and key passwords. This is supported for
  Apache mTLS, `private_key_jwt`, and JWT bearer grant signing.

Example with different JKS store and key passwords:

```yaml
smbtech:
  rest-clients:
    authentication:
      key-stores:
        payments-signing-key:
          location: file:/opt/app/certs/signing-key.jks
          type: JKS
          password-ref: signing-store-password
          key-alias: auth
          key-password-ref: signing-key-password
      credentials:
        signing-store-password:
          value: ${SIGNING_STORE_PASSWORD}
        signing-key-password:
          value: ${SIGNING_KEY_PASSWORD}
```

---

## 6. Error handling and full response bodies

When standard error handling is enabled, downstream HTTP error responses are
mapped into `HttpClientResponseException`. The exception carries:

- status code and reason phrase;
- response headers when `include-headers=true`;
- content type and charset;
- the complete response body when `include-body=true`;
- a structured `Notification` from `spring-boot-service-framework-commons`.

The body available from `exception.responseBody()` and
`exception.error().body()` is not truncated by audit settings. Audit logs can be
truncated independently with `audit.max-body-size`.

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        error-handling:
          enabled: true
          include-body: true
          include-headers: true
          include-notification-metadata: true
          notification-code-prefix: E_SERVICE_FRAMEWORK_HTTP_CLIENT_
        audit:
          enabled: true
          include-body: true
          max-body-size: 4096
```

Decode JSON error bodies with `HttpErrorBodyDecoder`:

```java
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecoder;

try {
    paymentsApi.createOrder(request);
} catch (HttpClientResponseException exception) {
    DownstreamError error = errorBodyDecoder.decode(exception, DownstreamError.class);
    String completeBody = exception.responseBody();
}
```

`decodeIfPresent(...)` returns `Optional.empty()` when the exception does not
carry a body. Decode failures raise `HttpErrorBodyDecodingException`.

---

## 7. Observability and audit

Observability uses Micrometer when a `MeterRegistry` is available.

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        observability:
          enabled: true
          metric-name: smbtech.http.client.requests
          include-uri: false
          include-status: true
          include-exception: true
          tags:
            system: payments
            layer: integration
        audit:
          enabled: true
          include-request: true
          include-response: true
          include-headers: false
          include-body: false
          max-body-size: 4096
```

Timer metric tags:

- `client`
- `method`
- `outcome`
- `status` when `include-status=true`
- `exception` when `include-exception=true`
- `uri` when `include-uri=true`
- custom `observability.tags`

The starter also increments `<metric-name>.errors` for exceptions and status
codes greater than or equal to 400.

Audit events are emitted through `HttpExchangeAuditSink`. The default starter
sink logs request, response, and failure events through SLF4J. Headers and
bodies are disabled by default because they can contain secrets or personal
data.

---

## 8. Optional resilience

Resilience is disabled by default and implemented without an external
Resilience4j dependency.

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        resilience:
          enabled: true
          retry:
            enabled: true
            max-attempts: 3
            backoff: 100ms
            retry-on-server-errors: true
            retry-on-exceptions: true
            retry-on-statuses: [429]
          circuit-breaker:
            enabled: true
            failure-threshold: 3
            open-duration: 30s
```

`retry` applies to transport exceptions, `5xx` responses when
`retry-on-server-errors=true`, and explicit status codes listed in
`retry-on-statuses`.

The circuit breaker opens after consecutive failures. While it is open, calls
fail fast with a message like:

```text
Circuit breaker is open for HTTP client: payments
```

---

## 9. Customizers

Use customizers when the framework default is close, but one service needs a
small adapter-specific adjustment.

```java
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;

@Bean
RestClientBuilderCustomizer addHeaderForPayments() {
    return (definition, builder) -> {
        if ("payments".equals(definition.name())) {
            builder.defaultHeader("X-Consumer", "orders-service");
        }
    };
}
```

Available customizer extension points:

| Interface | Responsibility |
|---|---|
| `RestClientBuilderCustomizer` | Customize `RestClient.Builder` before the final client is built. |
| `ApacheHttpClientBuilderCustomizer` | Customize Apache `HttpClientBuilder` when `client-type=APACHE_HTTP`. |
| `ClientHttpRequestFactoryCustomizer` | Customize the Spring `ClientHttpRequestFactory`. |

---

## 10. Property reference

All properties are under `smbtech.rest-clients`.

### Client properties

| Property | Required | Default | Description |
|---|---:|---|---|
| `clients.<name>.enabled` | No | `true` | Enables or disables this client definition. |
| `clients.<name>.bean-name` | No | `<name>RestClient` | Spring bean name for the generated `RestClient`. |
| `clients.<name>.base-url` | Yes | none | Base URL used by the generated client. |
| `clients.<name>.client-type` | No | `DEFAULT` | `DEFAULT`, `APACHE_HTTP`, or `OTHER`. |
| `clients.<name>.authentication-type` | No | `NO_AUTH` | `NO_AUTH`, `BASIC_AUTH`, `CLIENT_CREDENTIALS`, `JWT_BEARER`, or `OTHER`. |
| `clients.<name>.credential-token-requestor-id` | For bearer auth | none | OAuth2 `registration-id` under `spring.security.oauth2.client.registration`. |
| `clients.<name>.scopes` | No | empty | Expected scopes for token validation. Supports space/comma separated values. |
| `clients.<name>.default-headers.*` | No | empty | Headers added to every request. |

### Basic authentication

| Property | Default | Description |
|---|---|---|
| `clients.<name>.basic-authentication.username` | empty | Inline username. |
| `clients.<name>.basic-authentication.username-ref` | empty | Credential id for the username. |
| `clients.<name>.basic-authentication.password` | empty | Inline password. |
| `clients.<name>.basic-authentication.password-ref` | empty | Credential id for the password. |

### Timeout properties

| Property | Default | Description |
|---|---:|---|
| `clients.<name>.timeout.connect-timeout` | `2s` | Time allowed to establish a connection. |
| `clients.<name>.timeout.connection-request-timeout` | `2s` | Time allowed to obtain a pooled connection. |
| `clients.<name>.timeout.response-timeout` | `15s` | Time allowed to wait for the response. |

### Pooling properties

| Property | Default | Description |
|---|---:|---|
| `clients.<name>.pooling.connection-reuse-policy` | `DEFAULT` | Apache connection reuse policy name. |
| `clients.<name>.pooling.keep-alive` | `30s` | Default keep-alive duration. |
| `clients.<name>.pooling.max-connections` | `100` | Maximum total pooled connections. |
| `clients.<name>.pooling.max-connections-per-route` | `20` | Maximum pooled connections per route. |
| `clients.<name>.pooling.tcp-keep-alive` | `false` | Enables TCP keep-alive on sockets when supported. |

### Apache properties

| Property | Default | Description |
|---|---:|---|
| `clients.<name>.apache.hostname-verification-enabled` | `true` | Enables hostname verification for TLS connections. |
| `clients.<name>.apache.validate-after-inactivity` | `5s` | Validates idle pooled connections after inactivity. |
| `clients.<name>.apache.connection-time-to-live` | `5m` | Maximum lifetime for pooled connections. |
| `clients.<name>.apache.ssl.enabled` | `false` | Enables custom SSL configuration. |
| `clients.<name>.apache.ssl.trust-store-id` | empty | Truststore id under `authentication.key-stores`. |
| `clients.<name>.apache.ssl.key-store-id` | empty | Client keystore id under `authentication.key-stores`. |

### Error handling properties

| Property | Default | Description |
|---|---:|---|
| `clients.<name>.error-handling.enabled` | `true` | Converts downstream HTTP error responses into `HttpClientResponseException`. |
| `clients.<name>.error-handling.include-body` | `true` | Captures the complete downstream error body in the exception. |
| `clients.<name>.error-handling.include-headers` | `true` | Captures downstream response headers in the exception. |
| `clients.<name>.error-handling.include-notification-metadata` | `true` | Adds safe HTTP metadata to the generated `Notification`. |
| `clients.<name>.error-handling.notification-code-prefix` | `E_SERVICE_FRAMEWORK_HTTP_CLIENT_` | Prefix used for notification codes. A trailing underscore is added if missing. |
| `clients.<name>.error-handling.max-body-size` | `4096` | Reserved compatibility value. Audit body truncation is controlled by `audit.max-body-size`. |

### Audit properties

| Property | Default | Description |
|---|---:|---|
| `clients.<name>.audit.enabled` | `false` | Enables HTTP exchange audit events. |
| `clients.<name>.audit.include-request` | `false` | Emits request audit events. |
| `clients.<name>.audit.include-response` | `false` | Emits response audit events. |
| `clients.<name>.audit.include-headers` | `false` | Includes request/response headers in audit events. |
| `clients.<name>.audit.include-body` | `false` | Includes request/response bodies in audit events. |
| `clients.<name>.audit.max-body-size` | `4096` | Maximum characters copied to audit logs before appending `...[truncated]`. |

### Observability properties

| Property | Default | Description |
|---|---:|---|
| `clients.<name>.observability.enabled` | `true` | Enables Micrometer timer and error counter recording. |
| `clients.<name>.observability.metric-name` | `smbtech.http.client.requests` | Timer metric name. Error counter uses `<metric-name>.errors`. |
| `clients.<name>.observability.include-uri` | `false` | Adds the URI path as a metric tag. Keep disabled for high-cardinality paths. |
| `clients.<name>.observability.include-status` | `true` | Adds HTTP status as a metric tag. |
| `clients.<name>.observability.include-exception` | `true` | Adds exception class name as a metric tag. |
| `clients.<name>.observability.tags.*` | empty | Static custom metric tags. |

### Resilience properties

| Property | Default | Description |
|---|---:|---|
| `clients.<name>.resilience.enabled` | `false` | Enables the starter resilience interceptor. |
| `clients.<name>.resilience.retry.enabled` | `false` | Enables retry behavior. |
| `clients.<name>.resilience.retry.max-attempts` | `3` | Maximum attempts including the first call. |
| `clients.<name>.resilience.retry.backoff` | `100ms` | Wait time between retry attempts. |
| `clients.<name>.resilience.retry.retry-on-server-errors` | `true` | Retries `5xx` responses. |
| `clients.<name>.resilience.retry.retry-on-exceptions` | `true` | Retries transport/runtime exceptions. |
| `clients.<name>.resilience.retry.retry-on-statuses` | empty | Additional HTTP status codes to retry, for example `[429]`. |
| `clients.<name>.resilience.circuit-breaker.enabled` | `false` | Enables circuit breaker behavior. |
| `clients.<name>.resilience.circuit-breaker.failure-threshold` | `3` | Consecutive failures before opening the circuit. |
| `clients.<name>.resilience.circuit-breaker.open-duration` | `30s` | Time the circuit remains open before allowing calls again. |

### Credential, token, and keystore properties

OAuth2 provider, registration, client id, client secret, grant type, token URI,
and scopes should be configured with Spring Boot under
`spring.security.oauth2.client.provider` and
`spring.security.oauth2.client.registration`. The SMBTech authentication
properties below are extensions for keystores, client assertions, and JWT bearer
assertions.

| Property | Default | Description |
|---|---:|---|
| `authentication.credentials.<id>.value` | empty | Plain credential value resolved by `*-ref` properties such as `password-ref`, `key-password-ref`, username refs, and password refs. |
| `authentication.credentials.<id>.base64` | empty | Base64-encoded credential value resolved by `*-ref` properties. Takes priority over `value`; whitespace is ignored before decoding. |
| `authentication.client-assertions.<registration-id>.key-store-id` | empty | Signing keystore id for `private_key_jwt` client authentication. |
| `authentication.client-assertions.<registration-id>.token-lifetime` | `60s` | Client assertion JWT lifetime. |
| `authentication.client-assertions.<registration-id>.custom-claims.*` | empty | Provider-specific custom claims added to the client assertion. |
| `authentication.jwt-bearer.<registration-id>.key-store-id` | empty | Signing keystore id for `urn:ietf:params:oauth:grant-type:jwt-bearer`. |
| `authentication.jwt-bearer.<registration-id>.issuer` | empty | JWT bearer assertion issuer (`iss`). |
| `authentication.jwt-bearer.<registration-id>.subject` | empty | JWT bearer assertion subject (`sub`). |
| `authentication.jwt-bearer.<registration-id>.audience` | empty | JWT bearer assertion audience (`aud`). |
| `authentication.jwt-bearer.<registration-id>.token-lifetime` | `5m` | JWT bearer assertion lifetime. |
| `authentication.jwt-bearer.<registration-id>.custom-claims.*` | empty | Provider-specific custom JWT bearer claims. |
| `authentication.key-stores.<id>.location` | empty | Resource/file location for a keystore or truststore. |
| `authentication.key-stores.<id>.base64` | empty | Inline base64 keystore/truststore content. Takes priority over `location`. |
| `authentication.key-stores.<id>.type` | `PKCS12` | Store type, usually `PKCS12` or `JKS`. |
| `authentication.key-stores.<id>.password` | empty | Inline store password used to load the `JKS`/`PKCS12` container. Prefer `password-ref`. |
| `authentication.key-stores.<id>.password-ref` | empty | Credential id for the store password. The referenced credential may use `value` or `base64`. |
| `authentication.key-stores.<id>.key-alias` | empty | Private key alias for signing or mTLS. |
| `authentication.key-stores.<id>.key-password` | empty | Inline private key password. Falls back to the resolved store password when empty. |
| `authentication.key-stores.<id>.key-password-ref` | empty | Credential id for the private key password. The referenced credential may use `value` or `base64`, and may differ from the store password. |

---

## 11. Local validation

Run the starter tests:

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-rest-client:check
```

Run the full framework baseline:

```bash
./gradlew baseline
```

Run the standalone consumer smoke tests:

```bash
./gradlew restClientConsumerSmoke
./gradlew consumerSmoke
```

The example service lives in `examples/rest-client-consumer`. It consumes
published JARs instead of Gradle `project(...)` dependencies, which is closer to
how real services will consume the framework.
