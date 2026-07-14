# Spring Boot Service Framework REST Client Starter

Spring Boot starter for creating configured `RestClient` instances from
`application.yml`.

Use this module in consuming services. It adapts the framework-neutral model in
`spring-boot-service-framework-http-client-core` to Spring Boot, `RestClient`, Apache HTTP
Client, SSL stores, OAuth2 token acquisition, audit logs, Micrometer metrics,
optional retry, optional circuit breaker behavior, and declarative HTTP
interfaces.

For the complete property reference, defaults, and deeper examples, see
[../../docs/rest-client.md](../../docs/rest-client.md).

## When to use

Use this starter in Spring Boot services that need named outbound HTTP clients,
declarative Spring HTTP interfaces, OAuth2 access tokens, SSL/keystore handling,
audit logs, Micrometer metrics, or simple retry/circuit-breaker policies.

Use `spring-boot-service-framework-http-client-core` directly only when building
framework adapters or tests that should remain independent from Spring Boot.

## Quick start

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-starter-rest-client:0.2.0'
}
```

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        default-headers:
          X-Application-Name: orders-service
```

This creates a Spring bean named `paymentsRestClient`:

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

## Declarative API clients

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

    PaymentsFacade(ApiClientFactory factory) {
        this.paymentsApi = factory.create(PaymentsApi.class);
    }
}
```

You can also use `factory.create("payments", PaymentsApi.class)` when the
interface does not declare `@HttpApiClient`.

## Runtime registry

Inject `RestClientRegistry` when the client name is dynamic:

```java
RestClient client = registry.get("payments");
Set<String> names = registry.names();
Map<String, RestClient> clients = registry.all();
```

## Authentication

Supported client authentication types:

- `NO_AUTH`
- `BASIC_AUTH`
- `CLIENT_CREDENTIALS`
- `JWT_BEARER`
- `OTHER`

OAuth2 configuration is Spring-registration based. Configure provider, token
URI, client id, client secret, grant type, authentication method, and requested
scopes under `spring.security.oauth2.client`. The RestClient references that
registration through `credential-token-requestor-id`.

This starter exposes Spring Boot OAuth2 Client auto-configuration through
`spring-boot-starter-oauth2-client`. It intentionally does not bind
`spring.security.oauth2.client.*` itself. It only consumes the
`ClientRegistrationRepository` bean produced by Spring Boot.

Supported OAuth2 flows:

| Flow | Spring registration | SMBTech extension |
|---|---|---|
| Client credentials with client secret | `authorization-grant-type: client_credentials` | none |
| Client credentials with `private_key_jwt` | `authorization-grant-type: client_credentials` and `client-authentication-method: private_key_jwt` | `authentication.client-assertions.<registration-id>` |
| JWT bearer grant | `authorization-grant-type: urn:ietf:params:oauth:grant-type:jwt-bearer` | `authentication.jwt-bearer.<registration-id>` |

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

smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        authentication-type: CLIENT_CREDENTIALS
        credential-token-requestor-id: payments-token
        scopes: payments.read
```

For `private_key_jwt`, add a SMBTech client assertion extension:

```yaml
smbtech:
  rest-clients:
    authentication:
      client-assertions:
        payments-token:
          key-store-id: payments-signing-key
          token-lifetime: 60s
          custom-claims:
            acgp: acgp.ct
```

JWT bearer grant supports provider-specific custom claims:

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

smbtech:
  rest-clients:
    authentication:
      jwt-bearer:
        payments-jwt-token:
          key-store-id: payments-signing-key
          issuer: payments-issuer
          subject: payments-subject
          audience: https://auth.example/oauth2/token
          custom-claims:
            tenant: payments
            channel: backend
```

The starter now carries Spring Security OAuth2 Client/Jose dependencies and
auto-configures an internal bridge when a `ClientRegistrationRepository` bean is
available. For `client_credentials`, matching Spring registrations are requested
with Spring Security's `RestClientClientCredentialsTokenResponseClient`. If no
Spring registration exists, token acquisition fails fast with a configuration
error.
For `private_key_jwt`, the starter resolves the signing key from
`smbtech.rest-clients.authentication.client-assertions.<registration-id>` and
delegates assertion generation to Spring Security/Nimbus.
For `urn:ietf:params:oauth:grant-type:jwt-bearer`, the starter reads the
token endpoint, client id, authentication method, and scopes from the matching
Spring registration, then signs the grant assertion with
`smbtech.rest-clients.authentication.jwt-bearer.<registration-id>`.

Signing keys are resolved from `authentication.key-stores`. The store content
can be loaded from `location` or inline `base64`, and both `JKS` and `PKCS12`
are supported.

```yaml
smbtech:
  rest-clients:
    authentication:
      client-assertions:
        payments-token:
          key-store-id: payments-signing-key
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

`password-ref` opens the keystore. `key-password-ref` recovers the private key
entry. They can point to different values, which is common for `JKS`. If
`key-password`/`key-password-ref` is omitted, the key password falls back to the
resolved store password.

Access tokens are cached by Spring registration id plus the sorted set of
requested scopes, for example `payments-token::payments.read payments.write`.
Scope validation is applied on every cached or newly fetched token before the
token is returned to callers.

The starter-managed Spring OAuth2 authorized client cache can be controlled per
grant type:

```yaml
smbtech:
  rest-clients:
    authentication:
      token-cache:
        client-credentials: true
        jwt-bearer: true
```

Both values default to `true`. Set `client-credentials` or `jwt-bearer` to
`false` when that grant should always request a fresh access token. These flags
control the returned OAuth2 `access_token`; signed `private_key_jwt` client
assertions and JWT bearer grant assertions are generated for the token request.
For JWT bearer requests with dynamic custom claims, equivalent claim sets share
the same cache identity when JWT bearer caching is enabled.

OAuth2 configuration checklist:

- `credential-token-requestor-id` must match a Spring registration id.
- `clients.<name>.scopes` are expected scopes for validation, not requested
  scopes. Requested scopes come from the Spring registration.
- `private_key_jwt` requires a signing keystore under
  `authentication.client-assertions.<registration-id>.key-store-id`.
- JWT bearer grant requires a signing keystore under
  `authentication.jwt-bearer.<registration-id>.key-store-id`.
- Token requests are not configured under SMBTech properties.

Use `AccessTokenClient` when application code needs a token directly:

```java
AccessToken clientCredentials = accessTokenClient.clientCredentials("payments-token", "payments.read");
AccessToken jwtBearer = accessTokenClient.jwtBearer("payments-jwt-token", "payments.write");
```

## SSL

Public HTTPS endpoints usually work without custom SSL because the JVM default
truststore is used. Configure SSL only when you need a private truststore,
client certificate, or mTLS.

Both `PKCS12` and `JKS` stores can be loaded from `location` or inline `base64`:

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        client-type: APACHE_HTTP
        apache:
          ssl:
            enabled: true
            trust-store-id: payments-trust
            key-store-id: payments-client-cert
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

Credential references such as `password-ref` and `key-password-ref` accept
either `value` or explicit `base64` credentials. If both are configured,
`base64` has priority.

Password semantics:

- `password` / `password-ref`: store password used to load the `JKS`/`PKCS12`.
- `key-password` / `key-password-ref`: private key password used for mTLS,
  `private_key_jwt`, and JWT bearer signing.
- Credential `base64` values are decoded by the starter before they reach SSL
  or OAuth2 signing code. Whitespace in the base64 value is ignored.

## Error handling

Downstream HTTP errors are mapped to `HttpClientResponseException` by default.
When `error-handling.include-body=true`, the exception keeps the complete
response body for application decisions:

```java
try {
    paymentsApi.dummy();
} catch (HttpClientResponseException exception) {
    String fullBody = exception.getErrorResponseAsString();
    int status = exception.statusCode();
}
```

For clients created by this starter, JSON error body decoding is wired into the
exception automatically through `HttpErrorBodyDecoder`:

```java
try {
    paymentsApi.dummy();
} catch (HttpClientResponseException exception) {
    PaymentError error = exception.getJsonErrorResponseAsObject(PaymentError.class);
    String fullBody = exception.getErrorResponseAsString();
}
```

`responseBody()` remains available for compatibility. `HttpErrorBodyDecoder`
can still be injected and used directly when optional decoding is preferred:

```java
Optional<PaymentError> error = errorBodyDecoder.decodeIfPresent(exception, PaymentError.class);
```

Audit logging has a separate truncation control with `audit.max-body-size`.

## Observability, audit, and resilience defaults

| Area | Default |
|---|---|
| Observability | Enabled, metric name `smbtech.http.client.requests`, URI tag disabled. |
| Audit | Disabled, headers and bodies disabled. |
| Error handling | Enabled, complete error body and headers captured. |
| Resilience | Disabled. |
| Apache SSL | Disabled. |

## Local validation

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-rest-client:check
./gradlew publishLocalArtifacts
./gradlew restClientConsumerSmoke
```
