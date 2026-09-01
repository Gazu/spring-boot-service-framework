# Spring Boot Service Framework REST Client Starter

Spring Boot starter for creating configured `RestClient` beans from
`application.yml`.

This module adapts `spring-boot-service-framework-http-client-core` to Spring
Boot, Spring `RestClient`, Apache HTTP Client, optional Spring Security OAuth2 Client,
SSL stores, audit logs, Micrometer metrics, optional retry, optional circuit
breaker behavior, and declarative Spring HTTP interfaces.

## When to use

Use this starter in Spring Boot services that need:

- named outbound HTTP clients;
- declarative Spring HTTP interfaces;
- basic auth, OAuth2 `client_credentials`, or JWT bearer grant authentication;
- SSL, truststore, keystore, or mTLS configuration;
- downstream error handling with full response bodies;
- audit logs, Micrometer metrics, retry, or circuit breaker behavior;
- public extension points for custom token, HTTP, or context behavior.

Use `spring-boot-service-framework-http-client-core` directly only when building
framework adapters or tests that must remain independent from Spring Boot.

## Dependency

```groovy
dependencies {
    implementation platform(
            'com.smbtech:spring-boot-service-framework-platform:0.5.1'
    )
    implementation 'com.smbtech:spring-boot-service-framework-starter-rest-client'
}
```

Add OAuth2 only when the application uses bearer authentication or
`AccessTokenClient`:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
}
```

For local publication and consuming from module-local repositories, see
[Dependency and Local Publication](../../docs/rest-client/setup.md).

## Quick start

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        default-headers:
          X-Application-Name: orders-service
```

This creates:

- a `RestClient` bean named `paymentsRestClient`;
- a `payments` entry in `RestClientRegistry`;
- a configured client available through `ApiClientFactory`.

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

For declarative HTTP interfaces and runtime lookup, see
[Minimal Client and Runtime Access Patterns](../../docs/rest-client/quick-start.md).

## Public API

Common application-facing APIs:

- `RestClientRegistry`
- `ApiClientFactory`
- `HttpApiClient`
- `AccessTokenClient`
- `JwtBearerTokenRequest`
- `RequestContext`
- `RequestContextManager`
- `RequestContextScope`

Common customization APIs:

- `RestClientBuilderCustomizer`
- `ApacheHttpClientBuilderCustomizer`
- `ClientHttpRequestFactoryCustomizer`
- `RestClientAuthenticationConfigurer`
- `JwtBearerClaimsContributor`
- `ClientAssertionCustomizer`
- `OAuth2TokenRequestCustomizer`
- `AccessTokenCacheKeyResolver`

The full public extension contract is documented in
[REST Client Extension Points](../../docs/rest-client-extension-points.md).

## What this module does not do

- It does not own `spring.security.oauth2.client.*`; Spring Boot OAuth2 Client
  owns provider and registration binding.
- It does not add Spring Security transitively. OAuth2 applications opt in with
  `spring-boot-starter-oauth2-client`.
- It does not store real secrets in source-controlled configuration.
- It does not put business-specific domain logic in the framework.
- It does not make `http-client-core` depend on Spring, Apache, Jackson,
  Micrometer, or Spring Security APIs.

## Main documentation

| Topic | Document |
|---|---|
| REST client documentation entry point | [REST Client Starter Guide](../../docs/rest-client.md) |
| Dependency and local publication | [Dependency and Local Publication](../../docs/rest-client/setup.md) |
| Minimal client and declarative APIs | [Minimal Client](../../docs/rest-client/quick-start.md) |
| Basic auth and `client_credentials` | [Authentication: Basic and Client Credentials](../../docs/rest-client/authentication-client-credentials.md) |
| JWT bearer grant | [JWT Bearer Access Token](../../docs/rest-client/authentication-jwt-bearer.md) |
| Token cache and scope validation | [Token Cache and Scope Validation](../../docs/rest-client/token-cache.md) |
| Dynamic headers and JWT bearer claims | [Request Context Propagation](../../docs/rest-client/request-context.md) |
| Token diagnostics | [OAuth2 Token Diagnostics](../../docs/rest-client/token-diagnostics.md) |
| SSL, keystores, truststores, and mTLS | [SSL and HTTPS](../../docs/rest-client/ssl-keystore.md) |
| Error response body handling | [Error Handling](../../docs/rest-client/error-handling.md) |
| Observability, audit, retry, and circuit breaker | [Observability, Audit, and Resilience](../../docs/rest-client/observability-audit-resilience.md) |
| Customizers and mock integration | [Customizers](../../docs/rest-client/customizers.md) |
| Full property reference | [Property Reference](../../docs/rest-client/property-reference.md) |
| OAuth2 troubleshooting | [OAuth2 Troubleshooting](../../docs/rest-client/oauth2-troubleshooting.md) |
| Names and properties migration | [Migration Guide](../../docs/guides/migrate-public-names-and-properties.md) |
| Module README rules | [Module README Convention](../../docs/module-readme-convention.md) |

## Local validation

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-rest-client:check
./gradlew httpClientCompatibilityCheck
./gradlew publishLocalArtifacts
./gradlew restClientConsumerSmoke
```

For broader checks:

```bash
./gradlew documentationCheck
./gradlew check
```
