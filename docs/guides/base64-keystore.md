# Base64 Keystore Configuration

Use this when JKS or PKCS12 keystore content and passwords are delivered through
environment variables instead of files.

The same pattern works for:

- SSL truststores;
- mTLS client certificates;
- `private_key_jwt` client assertion signing;
- JWT bearer grant assertion signing.

## Configuration Shape

```yaml
smbtech:
  rest-clients:
    authentication:
      key-stores:
        payments-signing-key:
          base64: ${PAYMENTS_KEYSTORE_BASE64}
          type: JKS
          password-ref: payments-keystore-password
          key-alias: ${PAYMENTS_KEY_ALIAS}
          key-password-ref: payments-key-password
      credentials:
        payments-keystore-password:
          base64: ${PAYMENTS_KEYSTORE_PASSWORD_BASE64}
        payments-key-password:
          base64: ${PAYMENTS_KEY_PASSWORD_BASE64}
```

## Password Semantics

- `password` or `password-ref` opens the JKS or PKCS12 container.
- `key-password` or `key-password-ref` recovers the private key entry.
- If `key-password` and `key-password-ref` are omitted, the key password falls
  back to the resolved store password.
- JKS stores can use different store and key passwords. That is supported.
- `base64` takes priority over `location` for keystore content.
- Credential `base64` takes priority over credential `value`.

## Use With Private Key JWT

```yaml
smbtech:
  rest-clients:
    authentication:
      client-assertions:
        payments-token:
          key-store-id: payments-signing-key
```

## Use With JWT Bearer Grant

```yaml
smbtech:
  rest-clients:
    authentication:
      jwt-bearer:
        payments-jwt-token:
          key-store-id: payments-signing-key
```

## Use With Apache SSL

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        client-type: APACHE_HTTP
        apache:
          ssl:
            enabled: true
            trust-store-id: payments-trust
            key-store-id: payments-client-cert
```

## See Also

- [SSL and HTTPS](../rest-client/ssl-keystore.md)
- [Client Credentials With Private Key JWT](client-credentials-private-key-jwt.md)
- [JWT Bearer Dynamic Claims](jwt-bearer-dynamic-claims.md)
