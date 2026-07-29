# REST Client Extension Points

This document defines the public extension contract for
`spring-boot-service-framework-starter-rest-client`.

The goal is to let consuming applications replace behavior or make targeted
customizations without depending on internal adapter classes. This follows the
same style used by Spring Boot and Spring Security: publish a bean of a public
type, and the auto-configuration either uses it, decorates it, or backs off.

---

## 1. Public API Boundary

The following packages are public API and may be used by consuming
applications:

| Package | Stability | Purpose |
|---|---|---|
| `com.smbtech.serviceframework.starter.restclient.api` | Public | REST client starter APIs such as `AccessTokenClient`, `ApiClientFactory`, `RestClientRegistry`, `RequestContext`, `RequestContextManager`, and `RequestContextScope`. |
| `com.smbtech.serviceframework.starter.restclient.api.customizer` | Public | Fine-grained HTTP customization hooks for `RestClient`, Apache HttpClient, and Spring request factories. |
| `com.smbtech.serviceframework.starter.restclient.api.oauth2` | Public | OAuth2 extension contracts for JWT bearer claims, `private_key_jwt` assertions, token request customization, and token cache identity. |
| `com.smbtech.serviceframework.httpclient.domain` | Public | Framework-neutral HTTP client domain objects used by APIs and customizers. |
| `com.smbtech.serviceframework.httpclient.port.in` | Public | Framework-neutral inbound ports such as `HttpClientCatalog` and `HttpClientDefinitionValidator`. |
| `com.smbtech.serviceframework.httpclient.port.out` | Public | Framework-neutral outbound ports such as `AccessTokenProvider`, `CredentialProvider`, `CorrelationHeadersProvider`, `HttpExchangeAuditSink`, and `HttpErrorResponseBodyReader`. |

The following packages are internal implementation details and should not be
used directly by consuming applications:

| Package Pattern | Status | Notes |
|---|---|---|
| `com.smbtech.serviceframework.starter.restclient.adapter.out.*` | Internal | Spring, Apache HttpClient, OAuth2, logging, resilience, keystore, and interceptor adapters. |
| `com.smbtech.serviceframework.starter.restclient.autoconfigure.*` | Internal with documented properties | Auto-configuration and property binding. Consumers should configure properties or publish public beans instead of calling these classes directly. |
| Any class outside `api`, `api.customizer`, `domain`, or `port` packages | Internal unless documented otherwise | Internal classes may change before `1.0.0`. |

Public APIs can still evolve during `0.x`, but breaking changes must be
documented in [compatibility.md](compatibility.md) or release notes.

Consumers upgrading from `0.2.0` must replace the former
`credential-token-requestor-id` property and HTTP client authentication
exception name. See
[Migrate Public Names And Properties](guides/migrate-public-names-and-properties.md)
for exact configuration, import, and environment-variable changes.

---

## 2. Replacement Points

Replacement points are public interfaces or Spring/Spring Security types that a
consumer can publish as beans. The starter provides defaults with
`@ConditionalOnMissingBean` where possible.

| Behavior | Public Type To Provide | Default Behavior | Replacement Notes |
|---|---|---|---|
| Programmatic token acquisition | `AccessTokenClient` | Uses Spring Security `OAuth2AuthorizedClientManager` for `client_credentials` and JWT bearer grants. | Provide a bean to replace the high-level token client. This is a full replacement point. |
| Legacy token access for configured clients | `AccessTokenProvider` | Delegates to the same Spring Security token client. | Provide this only when existing code needs the port-level API. |
| Request context storage | `RequestContextManager` | Thread-local scoped request context. | Provide a bean to change context storage, for example async-aware propagation. |
| Named `RestClient` lookup | `RestClientRegistry` | Builds clients from `smbtech.rest-clients.clients`. | Provide a bean to control registry behavior fully. |
| Declarative HTTP proxy creation | `ApiClientFactory` | Creates Spring HTTP interface proxies from configured clients. | Provide a bean to replace proxy creation. |
| HTTP client catalog | `HttpClientCatalog` | Loads and validates definitions from properties. | Provide a bean to load definitions from another source. |
| Definition source | `HttpClientDefinitionSource` | Reads `smbtech.rest-clients.clients`. | Provide a bean to add database, config server, or generated definitions. |
| Definition validation | `HttpClientDefinitionValidator` | Applies framework validation rules. | Provide a bean to replace validation rules. |
| Credential lookup | `CredentialProvider` | Reads configured credentials and base64 values from properties. | Provide a bean to integrate with Vault, AWS Secrets Manager, Kubernetes secrets, etc. |
| Credential definition source | `CredentialDefinitionSource` | Reads `authentication.credentials`. | Provide a bean to change credential metadata source. |
| Keystore definition source | `KeyStoreDefinitionSource` | Reads `authentication.key-stores`. | Provide a bean to load keystore metadata from another source. |
| Correlation headers | `CorrelationHeadersProvider` | Reads correlation values from MDC. | Provide a bean to integrate with a different tracing context. |
| Audit events | `HttpExchangeAuditSink` | Emits audit events through SLF4J. | Provide a bean to send audit events to a queue, SIEM, or internal audit service. |
| Error body decoding | `HttpErrorResponseBodyReader` | Uses `HttpErrorBodyDecoder` backed by the application `ObjectMapper` when available. | Provide a bean to use another decoder or error format. |
| Authentication mechanism | `RestClientAuthenticationConfigurer` | Basic authentication is built in; OAuth2 contributes a configurer only when Spring Security OAuth2 Client is present. | Provide an ordered bean for `OTHER` or to override a supported authentication type. |
| OAuth2 authorized client service | `OAuth2AuthorizedClientService` | Uses Spring Security in-memory service decorated with grant-aware cache policy. | User-provided services remain usable and may be decorated for cache policy. |
| OAuth2 authorized client provider | `OAuth2AuthorizedClientProvider` | Composes client credentials and JWT bearer providers. | Provide a bean to replace grant provider composition. |
| OAuth2 authorized client manager | `OAuth2AuthorizedClientManager` | Uses `AuthorizedClientServiceOAuth2AuthorizedClientManager`. | Provide a bean for full OAuth2 authorization control. |

Example full replacement:

```java
import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TokenClientConfiguration {

    @Bean
    AccessTokenClient accessTokenClient() {
        return new AccessTokenClient() {
            @Override
            public AccessToken clientCredentials(String tokenRequestId) {
                return clientCredentials(tokenRequestId, "");
            }

            @Override
            public AccessToken clientCredentials(String tokenRequestId, String expectedScopes) {
                throw new UnsupportedOperationException("Use company token service");
            }

            @Override
            public AccessToken jwtBearer(String tokenRequestId) {
                return jwtBearer(tokenRequestId, "");
            }

            @Override
            public AccessToken jwtBearer(String tokenRequestId, String expectedScopes) {
                throw new UnsupportedOperationException("Use company token service");
            }
        };
    }
}
```

---

## 3. Customizers For Small HTTP Changes

Customizers are intended for small, additive changes where replacing a whole
component would be too heavy.

| Customizer | Applies To | Typical Use |
|---|---|---|
| `RestClientBuilderCustomizer` | `RestClient.Builder` for each configured client | Add interceptors, default request attributes, message converters, or client-specific builder settings. |
| `ApacheHttpClientBuilderCustomizer` | Apache `HttpClientBuilder` | Add Apache interceptors, connection behavior, route planning, or low-level HTTP settings. |
| `ClientHttpRequestFactoryCustomizer` | Spring `ClientHttpRequestFactory` | Tune request factory details after the starter builds it. |
| `RestClientAuthenticationConfigurer` | Authentication for a configured client | Add an authentication mechanism without coupling the REST client factory to its implementation library. |

Customizers are collected from the application context with
`ObjectProvider#orderedStream()`, so `@Order` is supported.

Example:

```java
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RestClientCustomization {

    @Bean
    RestClientBuilderCustomizer addCompanyHeader() {
        return (definition, builder) -> builder.defaultHeader("X-Company-Client", definition.name());
    }
}
```

---

## 4. OAuth2 Extension Model

The current OAuth2 model supports replacement and composition:

- replace `AccessTokenClient` for complete token acquisition control;
- replace `OAuth2AuthorizedClientManager` for full Spring Security OAuth2
  authorization control;
- replace `OAuth2AuthorizedClientProvider` for custom grant composition;
- replace or provide `OAuth2AuthorizedClientService` for token persistence;
- provide `RequestContextManager` to control dynamic headers and JWT bearer
  claim context.

The `api.oauth2` package defines the public contracts that will be wired into
the runtime pipeline across the OAuth2 extension phases. Beans of these types
are discovered by auto-configuration with `ObjectProvider#orderedStream()`, so
`@Order` is supported for contributors and customizers:

| SPI | Purpose | Runtime Status |
|---|---|---|
| `JwtBearerClaimsContributor` | Add dynamic JWT bearer grant claims without replacing `AccessTokenClient`. | Auto-discovered, ordered, and applied by the JWT bearer claims pipeline. |
| `ClientAssertionCustomizer` | Customize `private_key_jwt` assertion headers and claims. | Auto-discovered, ordered, and applied before the client assertion is signed. |
| `OAuth2TokenRequestCustomizer` | Add, override, or remove token request parameters and headers. | Auto-discovered, ordered, and applied before the token endpoint request is sent. |
| `AccessTokenCacheKeyResolver` | Customize cache identity for token reuse. | Auto-discovered as a single optional bean and applied before Spring Security authorizes the client. |

The current model also composes Spring Security primitives internally:

- `ClientCredentialsOAuth2AuthorizedClientProvider`;
- `JwtBearerOAuth2AuthorizedClientProvider`;
- `OAuth2AuthorizedClientProviderBuilder`;
- `AuthorizedClientServiceOAuth2AuthorizedClientManager`;
- `OAuth2AccessTokenResponseClient` implementations for token endpoint calls.

Consumers should not depend on internal classes such as
`SpringOAuth2TokenResponseClientFactory`,
`SpringSecurityJwtBearerAssertionResolver`, or `JwtBearerCustomClaimsResolver`.
Use the public `api.oauth2` SPIs or the documented replacement points above for
OAuth2 behavior that cannot be expressed with properties.

---

## 5. Examples

### Replace Token Acquisition Completely

Use `AccessTokenClient` when the application wants full control over token
acquisition:

```java
import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Set;

@Configuration
class CompanyTokenConfiguration {

    @Bean
    AccessTokenClient accessTokenClient(CompanyTokenService tokenService) {
        return new AccessTokenClient() {
            @Override
            public AccessToken clientCredentials(String tokenRequestId) {
                return clientCredentials(tokenRequestId, "");
            }

            @Override
            public AccessToken clientCredentials(String tokenRequestId, String expectedScopes) {
                String token = tokenService.clientCredentials(tokenRequestId, expectedScopes);
                return new AccessToken(token, "Bearer", Instant.now().plusSeconds(300), Set.of());
            }

            @Override
            public AccessToken jwtBearer(String tokenRequestId) {
                return jwtBearer(tokenRequestId, "");
            }

            @Override
            public AccessToken jwtBearer(String tokenRequestId, String expectedScopes) {
                String token = tokenService.jwtBearer(tokenRequestId, expectedScopes);
                return new AccessToken(token, "Bearer", Instant.now().plusSeconds(300), Set.of());
            }
        };
    }
}
```

### Add JWT Bearer Claims

Use `JwtBearerClaimsContributor` when the application only needs to add dynamic
claims. The context, including nested structured containers, is immutable and
the contributor returns only the claims it
wants to add:

```java
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
class JwtBearerClaimsConfiguration {

    @Bean
    JwtBearerClaimsContributor tenantClaimsContributor(TenantContext tenantContext) {
        return context -> Map.of(
                "tenant_id", tenantContext.tenantId(),
                "channel", tenantContext.channel()
        );
    }
}
```

JWT bearer claims are resolved in this order:

- claims configured under `smbtech.rest-clients.authentication.jwt-bearer`;
- claims from `RequestContextManager`;
- claims explicitly passed to `AccessTokenClient#jwtBearer`;
- ordered `JwtBearerClaimsContributor` beans.

Later values override earlier values for the same claim name. Reserved and
sensitive claim names such as `iss`, `sub`, `aud`, `exp`, `password`, and
`client_secret` are ignored by the pipeline. Contributors should be
deterministic for a given request context, because resolved claims are also used
to isolate JWT bearer token cache entries.

### Customize `private_key_jwt` Client Assertions

Use `ClientAssertionCustomizer` for small changes to assertion headers or
custom claims. It returns a new context instead of mutating the original:

```java
import com.smbtech.serviceframework.starter.restclient.api.oauth2.ClientAssertionCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClientAssertionConfiguration {

    @Bean
    ClientAssertionCustomizer clientAssertionCustomizer() {
        return context -> context
                .withHeader("kid", "company-signing-key")
                .withClaim("application", "orders-service");
    }
}
```

Client assertion customization starts with claims configured under
`smbtech.rest-clients.authentication.client-assertions.<registration-id>` and
then applies ordered `ClientAssertionCustomizer` beans before Spring Security
signs the assertion. Use `withTokenLifetime(...)` to change the assertion
expiration. Registered JWT claims such as `iss`, `sub`, `aud`, `iat`, and `exp`
are controlled by the runtime and are ignored when supplied as custom claims.

### Customize Token Requests

Use `OAuth2TokenRequestCustomizer` when the authorization server needs
provider-specific token request parameters or headers:

```java
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.OAuth2TokenRequestCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TokenRequestConfiguration {

    @Bean
    OAuth2TokenRequestCustomizer audienceTokenRequestCustomizer() {
        return context -> {
            if (context.grantType() == GrantType.JWT_BEARER) {
                return context
                        .withParameter("resource", "payments-api")
                        .withHeader("X-Token-Client", "orders-service");
            }
            return context;
        };
    }
}
```

Token request customization receives the request after Spring Security has
created the default form parameters and headers, including `private_key_jwt`
client authentication parameters when configured. Use `withParameter(...)` and
`withHeader(...)` to add or override values, and `withoutParameter(...)` or
`withoutHeader(...)` to remove values before the token endpoint call is sent.

### Customize Token Cache Identity

Use `AccessTokenCacheKeyResolver` when the default token reuse identity is not
specific enough for the application:

```java
import com.smbtech.serviceframework.starter.restclient.api.oauth2.AccessTokenCacheKeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TokenCacheConfiguration {

    @Bean
    AccessTokenCacheKeyResolver tenantAwareTokenCacheKeyResolver() {
        return context -> context.registrationId()
                + "::" + context.grantType().value()
                + "::" + context.authorizationAttributes().getOrDefault("tenant_id", "default");
    }
}
```

The resolver receives the default Spring Security principal name that would be
used for token reuse. For JWT bearer tokens, that default already includes the
resolved dynamic-claims hash. Return the default value to preserve standard
behavior, return a tenant/account-aware value to partition the cache, or return
a broader stable value to intentionally share tokens across requests.

### Customize Outbound HTTP Clients

Use existing HTTP customizers when the change is about the outbound
`RestClient`, Apache HttpClient, or Spring request factory:

```java
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RestClientCustomization {

    @Bean
    RestClientBuilderCustomizer addClientHeader() {
        return (definition, builder) -> builder.defaultHeader(
                "X-Application-Client",
                definition.name()
        );
    }
}
```

Current status: the replacement points and HTTP customizers are active at
runtime. The `api.oauth2` contracts are public, covered by tests, and discovered
by auto-configuration; their behavioral application is intentionally staged for
the OAuth2 pipeline phases.

---

## 6. Composition Rules

The starter should prefer composition over inheritance:

- keep internal adapter classes `final` unless extension is explicitly
  supported;
- expose small public interfaces or customizers for consumer code;
- prefer decorating user-provided beans over replacing them;
- use `@ConditionalOnMissingBean` for default beans;
- collect multiple customizers with ordered streams;
- keep Spring Security extension points usable instead of hiding them behind
  framework-specific abstractions.

When adding a new feature, decide which level applies:

| Consumer Need | Preferred Extension Shape |
|---|---|
| Replace complete behavior | Public interface plus `@ConditionalOnMissingBean` default. |
| Add a small tweak | Public customizer/converter SPI. |
| Add multiple independent behaviors | Ordered list of contributor/customizer beans. |
| Integrate with Spring Security internals | Reuse Spring Security public interfaces when possible. |
| Configure declaratively | `smbtech.rest-clients.*` properties. |

---

## 7. Compatibility Requirements

New extension points should include:

- a public interface or record in a public package;
- a default implementation or integration path in auto-configuration;
- `@ConditionalOnMissingBean` when the default is replaceable;
- tests proving a user-provided bean is preserved or composed;
- documentation in this file and [rest-client.md](rest-client.md);
- compatibility coverage in `RestClientCompatibilityTest` when the point is
  intended to be stable for consumers.

Do not document an internal adapter class as an extension point unless it is
first moved to a public package or wrapped by a public interface.

Run `./gradlew httpClientCompatibilityCheck` after changing a supported REST
client API, extension point, property, or auto-configuration import.
