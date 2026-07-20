# JWT Bearer Access Token

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
        token-request-id: payments-jwt-token
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
