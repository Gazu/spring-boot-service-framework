# REST Client Starter Guide

This guide is the canonical entry point for
`com.smbtech:spring-boot-service-framework-starter-rest-client`.

The starter reads `application.yml`, builds named Spring `RestClient` instances,
registers them as beans, exposes them through `RestClientRegistry`, and can
create declarative Spring HTTP interface proxies through `ApiClientFactory`.

The framework-neutral model lives in `spring-boot-service-framework-http-client-core`.
This starter is the adapter layer for Spring Boot property binding,
`RestClient`, Apache HTTP Client, SSL stores, OAuth2 token acquisition, audit
logs, Micrometer metrics, optional retry, and optional circuit breaker behavior.

For supported replacement points, customizers, and public API boundaries, see
[REST Client Extension Points](rest-client-extension-points.md).

## Start Here

| Need | Read |
|---|---|
| Add the dependency or publish local artifacts | [Dependency and Local Publication](rest-client/setup.md) |
| Configure the first client | [Minimal Client](rest-client/quick-start.md) |
| Inject clients dynamically or create declarative HTTP APIs | [Runtime Access Patterns](rest-client/quick-start.md#runtime-access-patterns) |
| Run the standalone consumer example | [REST client consumer example](../examples/rest-client-consumer/README.md) |
| Check all supported properties | [Property Reference](rest-client/property-reference.md) |
| Migrate renamed types or `token-request-id` | [Migrate Public Names And Properties](guides/migrate-public-names-and-properties.md) |

## Authentication

| Scenario | Guide |
|---|---|
| Basic authentication | [Authentication: Basic and Client Credentials](rest-client/authentication-client-credentials.md#basic-authentication) |
| OAuth2 `client_credentials` with client secret | [Authentication: Basic and Client Credentials](rest-client/authentication-client-credentials.md#client-credentials-access-token) |
| OAuth2 `client_credentials` with `private_key_jwt` client authentication | [Authentication: Basic and Client Credentials](rest-client/authentication-client-credentials.md#client-credentials-access-token) |
| OAuth2 JWT bearer grant (`urn:ietf:params:oauth:grant-type:jwt-bearer`) | [JWT Bearer Access Token](rest-client/authentication-jwt-bearer.md) |
| Token cache behavior and scope validation | [Token Cache and Scope Validation](rest-client/token-cache.md) |
| Dynamic headers and JWT bearer claims | [Request Context Propagation](rest-client/request-context.md) |
| Safe token diagnostics logs | [OAuth2 Token Diagnostics](rest-client/token-diagnostics.md) |
| OAuth2 error messages and fixes | [OAuth2 Troubleshooting](rest-client/oauth2-troubleshooting.md), [Troubleshooting](troubleshooting.md#oauth2-and-token-acquisition) |

## Use Case Guides

| Scenario | Guide |
|---|---|
| Copy a `client_credentials` + `private_key_jwt` configuration | [Client Credentials With Private Key JWT](guides/client-credentials-private-key-jwt.md) |
| Add dynamic claims to JWT bearer grant requests | [JWT Bearer Dynamic Claims](guides/jwt-bearer-dynamic-claims.md) |
| Configure base64 JKS or PKCS12 keystores and passwords | [Base64 Keystore Configuration](guides/base64-keystore.md) |
| Disable access token caching for one or both supported grants | [Disable Token Cache](guides/disable-token-cache.md) |
| Add OAuth2 claims, request parameters, assertion changes, or cache identity | [Customize OAuth2](guides/customize-oauth2.md) |
| Replace default framework behavior with application beans | [Replace Default Beans](guides/replace-default-beans.md) |

OAuth2 provider, registration, client id, client secret, grant type, token URI,
and requested scopes are configured with Spring Boot under
`spring.security.oauth2.client.provider` and
`spring.security.oauth2.client.registration`.

SMBTech properties under `smbtech.rest-clients.authentication` extend Spring
Boot configuration with credentials, keystores, `private_key_jwt` signing
metadata, JWT bearer grant signing metadata, diagnostics, and token-cache
policy.

## HTTP Client Behavior

| Area | Guide |
|---|---|
| SSL, truststores, keystores, mTLS, and base64 store material | [SSL and HTTPS](rest-client/ssl-keystore.md) |
| Downstream error mapping and JSON error decoding | [Error Handling and Full Response Bodies](rest-client/error-handling.md) |
| Micrometer metrics, audit logs, retry, and circuit breaker | [Observability, Audit, and Resilience](rest-client/observability-audit-resilience.md) |
| `RestClient`, Apache HttpClient, request factory, and mock integration hooks | [Customizers](rest-client/customizers.md) |

## Extension And Replacement

Use customizers when the default behavior is close and the application needs a
small additive change. Use replacement points when the application wants full
control over a framework behavior.

| Need | Read |
|---|---|
| Replace `AccessTokenClient`, `RestClientRegistry`, `ApiClientFactory`, or Spring Security OAuth2 components | [REST Client Extension Points](rest-client-extension-points.md#2-replacement-points) |
| Add JWT bearer claims, customize `private_key_jwt`, customize token requests, or customize cache identity | [OAuth2 Extension Model](rest-client-extension-points.md#4-oauth2-extension-model) |
| Add HTTP-level customizers | [Customizers](rest-client/customizers.md) |

## Property Reference

All `smbtech.rest-clients` properties are documented in
[Property Reference](rest-client/property-reference.md).

The most common groups are:

- `clients.<name>` for named `RestClient` definitions;
- `clients.<name>.basic-authentication` for basic auth credentials;
- `clients.<name>.timeout`, `pooling`, and `apache` for HTTP tuning;
- `clients.<name>.error-handling`, `audit`, `observability`, and `resilience`;
- `authentication.credentials`, `key-stores`, `client-assertions`,
  `jwt-bearer`, `token-cache`, and `diagnostics`;
- `request-context`;
- `validation`.

## Local Validation

Run focused REST client checks and consumer smoke tests with the commands in
[Local Validation](rest-client/local-validation.md).

For the full repository baseline:

```bash
./gradlew check
./gradlew httpClientCompatibilityCheck
./gradlew consumerSmoke
./gradlew compatibilityCheck
```
