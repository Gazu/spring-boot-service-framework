# Token Cache and Scope Validation

By default, Spring Security's in-memory authorized client service caches OAuth2
access tokens for both supported non-user grants:

```yaml
smbtech:
  rest-clients:
    authentication:
      token-cache:
        client-credentials: true
        jwt-bearer: true
```

| `client-credentials` | `jwt-bearer` | Behavior |
|---:|---:|---|
| `true` | `true` | Cache both `client_credentials` and JWT bearer access tokens. This is the default. |
| `true` | `false` | Cache only `client_credentials`; fetch a new JWT bearer access token for each authorization. |
| `false` | `true` | Cache only JWT bearer; fetch a new `client_credentials` access token for each authorization. |
| `false` | `false` | Disable the starter-managed OAuth2 access-token cache for both grants. |

The cache stores the returned OAuth2 `access_token`; it does not cache or reuse
the signed `private_key_jwt` client assertion or the signed JWT bearer grant
assertion. Those assertions are created as part of the token request.

For static requests, the cache key is deterministic:

```text
<registration-or-token-request-id>::<sorted requested scopes>
```

When no requested scopes exist, the key is only the registration or token
request id. This avoids collisions if the same OAuth2 client id is later used
with a different requested scope set.

For JWT bearer requests with dynamic custom claims, the starter includes the
resolved dynamic claims in the internal authorization principal used by Spring
Security. This means a cached JWT bearer access token is reused only for an
equivalent set of dynamic claims. If `authentication.token-cache.jwt-bearer` is
`false`, the dynamic-claims cache key is bypassed and every authorization fetches
a fresh JWT bearer access token.

These flags are enforced by wrapping the available
`OAuth2AuthorizedClientService`, including the default service contributed by
Spring Boot OAuth2 Client, when a `ClientRegistrationRepository` is available.
