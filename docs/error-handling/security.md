# Security Error Handling

The error handling starter converts Spring Security authentication and
authorization failures into the same snake-case `Notification` contract used
by Spring MVC. OAuth2 Bearer failures also receive an RFC 6750
`WWW-Authenticate` challenge when the selected resolution requires one.

## Filter Chain Integration

Connect both auto-configured handlers to Spring Security and to the resource
server configuration:

```java
@Bean
SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuthenticationEntryPoint authenticationEntryPoint,
        AccessDeniedHandler accessDeniedHandler
) throws Exception {
    return http
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/payments/**").hasAuthority("SCOPE_payment.write")
                    .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .oauth2ResourceServer(resourceServer -> resourceServer
                    .jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .build();
}
```

Use Spring Security's normal JWT, opaque-token, or
`AuthenticationManagerResolver<HttpServletRequest>` configuration. The starter
does not validate tokens; it standardizes failures after Spring Security has
classified them.

## Security Error Catalog

| Catalog entry | Code | HTTP | Category | `metadata.security.reason` | OAuth2 error |
|---|---|---:|---|---|---|
| `AUTHENTICATION_REQUIRED` | `E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0001` | 401 | `AUTHENTICATION` | `authentication_required` | omitted |
| `BEARER_REQUEST_INVALID` | `E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0002` | 401 | `AUTHENTICATION` | `invalid_request` | `invalid_request` |
| `BEARER_TOKEN_INVALID` | `E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003` | 401 | `AUTHENTICATION` | `invalid_token` | `invalid_token` |
| `AUTHENTICATION_PROVIDER_FAILURE` | `E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0004` | 502 | `DOWNSTREAM` | `provider_failure` | omitted |
| `ACCESS_DENIED` | `E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0001` | 403 | `AUTHORIZATION` | `access_denied` | omitted |
| `INSUFFICIENT_SCOPE` | `E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0002` | 403 | `AUTHORIZATION` | `insufficient_scope` | `insufficient_scope` |
| `CSRF_ACCESS_DENIED` | `E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0003` | 403 | `AUTHORIZATION` | `csrf_rejected` | omitted |

JWT expiration, invalid signatures, invalid issuers, rejected claims, and
inactive opaque tokens all use the stable `BEARER_TOKEN_INVALID` entry. The
specific cause remains in internal diagnostics and structured logs.

## Metadata Contract

The default `PUBLIC` response preserves the catalog code and returns only the
generic message and minimal metadata:

```json
{
  "code": "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003",
  "message": "The request could not be completed",
  "severity": "ERROR",
  "field_name": "",
  "metadata": {
    "category": "AUTHENTICATION"
  }
}
```

With `exposure: INTERNAL`, trusted consumers may receive the detailed,
sanitized security metadata:

```json
{
  "metadata": {
    "category": "AUTHENTICATION",
    "retryable": false,
    "security": {
      "reason": "invalid_token",
      "authentication_scheme": "bearer"
    },
    "request": {
      "method": "GET",
      "route": "/payments/{paymentId}"
    }
  }
}
```

The route is a Spring MVC route template, not the raw URI. Query parameters,
tokens, claims, principals, headers, request bodies, exception messages, and
provider responses are never copied into response metadata.

OAuth2-classified failures add `metadata.oauth2` to `INTERNAL` responses when
enabled:

```json
{
  "code": "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003",
  "message": "Bearer token is invalid",
  "severity": "ERROR",
  "field_name": "",
  "metadata": {
    "category": "AUTHENTICATION",
    "retryable": false,
    "security": {
      "reason": "invalid_token",
      "authentication_scheme": "bearer"
    },
    "oauth2": {
      "error": "invalid_token",
      "error_description": "The access token is invalid",
      "error_uri": "https://www.rfc-editor.org/rfc/rfc6750#section-3.1"
    }
  }
}
```

The corresponding header is:

```http
WWW-Authenticate: Bearer error="invalid_token", error_description="The access token is invalid", error_uri="https://www.rfc-editor.org/rfc/rfc6750#section-3.1"
```

The header is generated independently of body exposure, so it remains the same
for `PUBLIC` and `INTERNAL`. Descriptions and URIs are fixed framework values.
Provider messages and `exception.getMessage()` are not exposed.

## Required Scopes

Spring Security does not expose the scope required by an authorization rule to
an `AccessDeniedHandler`. Provide a `RequiredScopeResolver` when
`insufficient_scope` classification is required:

```java
@Bean
RequiredScopeResolver requiredScopeResolver() {
    return (request, authentication) -> request.getRequestURI().startsWith("/payments/")
            ? Set.of("payment.write")
            : Set.of();
}
```

For a Bearer-authenticated request, a non-empty result produces the following
`INTERNAL` response metadata:

```json
{
  "code": "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0002",
  "message": "The access token does not grant the required scope",
  "severity": "ERROR",
  "field_name": "",
  "metadata": {
    "category": "AUTHORIZATION",
    "retryable": false,
    "security": {
      "reason": "insufficient_scope",
      "authentication_scheme": "bearer"
    },
    "oauth2": {
      "error": "insufficient_scope",
      "error_description": "The access token does not grant the required scope",
      "error_uri": "https://www.rfc-editor.org/rfc/rfc6750#section-3.1",
      "scope": "payment.write"
    }
  }
}
```

Required scopes are included in the detailed response only when
`include-required-scope` is enabled. The RFC challenge can still include them
when the body uses `PUBLIC` exposure.
Granted token scopes are never exposed as required scopes.
When no required scope is resolved, the failure remains the generic
`ACCESS_DENIED` response.

## Configuration

```yaml
smbtech:
  error-handling:
    response:
      exposure: PUBLIC
    security:
      enabled: true
      oauth2-metadata:
        enabled: true
        include-error-description: true
        include-error-uri: true
        include-required-scope: false
```

Disabling OAuth2 response metadata does not disable standards-compliant Bearer
challenge headers. See the generated [property reference](property-reference.md)
for defaults.

## Replacement Points

Applications can replace these beans independently:

| Contract | Responsibility |
|---|---|
| `SecurityAuthenticationFailureResolver` | Classify authentication exceptions. |
| `SecurityAuthorizationFailureResolver` | Classify authorization and CSRF exceptions. |
| `RequiredScopeResolver` | Return scopes required by the protected operation. |
| `OAuth2SecurityMetadataFactory` | Build safe security metadata used by detailed responses and Bearer challenges. |
| `OAuth2SecurityChallengeWriter` | Write the Bearer challenge header. |
| `AuthenticationEntryPoint` | Replace the complete authentication response adapter. |
| `AccessDeniedHandler` | Replace the complete authorization response adapter. |

Resolvers must classify from exception types and structured OAuth2 error codes,
not provider message text. Custom metadata and challenges must follow the same
redaction rules as the default implementation.

## Compatibility And Verification

The catalog names, codes, reasons, security records, extension signatures, RFC
URI, handler constructors, and configuration accessors are protected by
`ErrorHandlingPublicApiCompatibilityTest`. Filter-chain behavior is covered by
`SpringSecurityErrorHandlingIntegrationTest`, including JWT and opaque-token
authentication in the same resource server.

```bash
./gradlew errorHandlingCompatibilityCheck
```
