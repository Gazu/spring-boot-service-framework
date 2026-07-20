# Client Credentials With Private Key JWT

Use this when the authorization server expects the OAuth2
`client_credentials` grant, but the client authenticates to the token endpoint
with `private_key_jwt` instead of a client secret.

Spring Boot owns the OAuth2 provider and registration. SMBTech owns only the
signing extension and keystore reference.

## Configuration

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          my-provider:
            token-uri: ${OAUTH2_TOKEN_URI}
        registration:
          payments-token:
            provider: my-provider
            client-id: ${PAYMENTS_CLIENT_ID}
            client-authentication-method: private_key_jwt
            authorization-grant-type: client_credentials
            scope:
              - payment.read

smbtech:
  rest-clients:
    clients:
      payments:
        base-url: ${PAYMENTS_API_BASE_URL}
        authentication-type: CLIENT_CREDENTIALS
        token-request-id: payments-token
        scopes: payment.read
    authentication:
      client-assertions:
        payments-token:
          key-store-id: payments-signing-key
          token-lifetime: 60s
          custom-claims:
            channel: backend
      key-stores:
        payments-signing-key:
          base64: ${PAYMENTS_SIGNING_KEYSTORE_BASE64}
          type: JKS
          password-ref: payments-signing-store-password
          key-alias: ${PAYMENTS_SIGNING_KEY_ALIAS}
          key-password-ref: payments-signing-key-password
      credentials:
        payments-signing-store-password:
          base64: ${PAYMENTS_SIGNING_KEYSTORE_PASSWORD_BASE64}
        payments-signing-key-password:
          base64: ${PAYMENTS_SIGNING_KEY_PASSWORD_BASE64}
```

## Rules

- `token-request-id` must match the Spring registration id.
- The Spring registration grant stays `client_credentials`.
- The Spring registration client authentication method is `private_key_jwt`.
- The SMBTech `client-assertions.<registration-id>` block signs only the client
  assertion used to authenticate the client at the token endpoint.
- Requested scopes come from the Spring registration. `clients.<name>.scopes`
  are expected scopes validated on the returned access token.

## Validate

```bash
./gradlew documentationCheck
./gradlew restClientConsumerSmoke
```

## See Also

- [Authentication: Basic and Client Credentials](../rest-client/authentication-client-credentials.md)
- [Base64 Keystore Configuration](base64-keystore.md)
- [REST Client Property Reference](../rest-client/property-reference.md)
