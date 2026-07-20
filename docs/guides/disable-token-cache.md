# Disable Token Cache

Use this when a service must request a fresh OAuth2 access token for
`client_credentials`, JWT bearer grant, or both.

By default, the starter-managed Spring OAuth2 authorized client cache is enabled
for both grants.

## Disable JWT Bearer Cache Only

```yaml
smbtech:
  rest-clients:
    authentication:
      token-cache:
        client-credentials: true
        jwt-bearer: false
```

Use this when JWT bearer assertions contain highly dynamic claims and the
authorization server should evaluate every request.

## Disable Client Credentials Cache Only

```yaml
smbtech:
  rest-clients:
    authentication:
      token-cache:
        client-credentials: false
        jwt-bearer: true
```

Use this when the provider requires a fresh `client_credentials` access token
for each authorization.

## Disable Both

```yaml
smbtech:
  rest-clients:
    authentication:
      token-cache:
        client-credentials: false
        jwt-bearer: false
```

## Important Detail

The cache stores returned OAuth2 access tokens. It does not cache or reuse the
signed `private_key_jwt` client assertion or the signed JWT bearer grant
assertion.

## See Also

- [Token Cache and Scope Validation](../rest-client/token-cache.md)
- [OAuth2 Token Diagnostics](../rest-client/token-diagnostics.md)
