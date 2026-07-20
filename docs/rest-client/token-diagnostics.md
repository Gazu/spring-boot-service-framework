# OAuth2 Token Diagnostics

Token diagnostics are disabled by default. Enable them when you need to inspect
how OAuth2 tokens are requested, how JWT assertions are built, and whether the
authorized client cache is used:

```yaml
smbtech:
  rest-clients:
    authentication:
      diagnostics:
        enabled: true
        include-claims: true
        include-cache-events: true
        include-token-preview: false
```

Diagnostics use `spring-boot-service-framework-logging-core`. If the logging
starter is present, events use the application's structured logging pipeline. If
it is not present, the REST starter writes the same structured event through an
internal SLF4J-backed adapter.

Example event:

```json
{
  "type": "OAUTH2_TOKEN_DIAGNOSTIC",
  "msg": "OAuth2 token request started",
  "data": {
    "event": "TOKEN_REQUEST_STARTED",
    "registrationId": "payments-jwt-token",
    "grantType": "urn:ietf:params:oauth:grant-type:jwt-bearer",
    "clientAuthenticationMethod": "none",
    "tokenUri": "https://auth.example/oauth2/token",
    "scopes": ["payments.write"]
  }
}
```

For JWT bearer assertion creation, the event includes safe metadata such as
issuer, subject, audience, issue/expiration time, algorithm, and key id. Custom
claims are included only when `include-claims` is `true`, and sensitive claim
names such as `password`, `secret`, `token`, `key`, `assertion`, and
`authorization` are redacted.

Access token and assertion previews are disabled by default. If
`include-token-preview` is enabled, only a short prefix is emitted, followed by
`...<redacted>`.
