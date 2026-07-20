# SSL and HTTPS

For public HTTPS endpoints signed by a CA already trusted by the JVM, no custom
SSL configuration is required:

```yaml
smbtech:
  rest-clients:
    clients:
      oauth-certs:
        base-url: https://oauth.example.com
```

Configure custom SSL when the downstream service requires a private truststore,
a client certificate, mTLS, or a non-default keystore.

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        client-type: APACHE_HTTP
        apache:
          hostname-verification-enabled: true
          connection-time-to-live: 5m
          validate-after-inactivity: 2s
          ssl:
            enabled: true
            trust-store-id: payments-trust
            key-store-id: payments-client-cert
    authentication:
      key-stores:
        payments-trust:
          location: file:/opt/app/certs/truststore.p12
          type: PKCS12
          password-ref: truststore-password
        payments-client-cert:
          location: file:/opt/app/certs/client.p12
          type: PKCS12
          password-ref: keystore-password
          key-alias: client
          key-password-ref: key-password
      credentials:
        truststore-password:
          value: ${PAYMENTS_TRUSTSTORE_PASSWORD}
        keystore-password:
          value: ${PAYMENTS_KEYSTORE_PASSWORD}
        key-password:
          value: ${PAYMENTS_KEY_PASSWORD}
```

Keystores and truststores can also be supplied as base64 content. This supports
both `JKS` and `PKCS12`.

```yaml
smbtech:
  rest-clients:
    authentication:
      key-stores:
        payments-trust:
          base64: ${PAYMENTS_TRUSTSTORE_JKS_BASE64}
          type: JKS
          password-ref: truststore-password
        payments-client-cert:
          base64: ${PAYMENTS_CLIENT_CERT_JKS_BASE64}
          type: JKS
          password-ref: keystore-password
          key-alias: client
          key-password-ref: key-password
      credentials:
        truststore-password:
          base64: ${PAYMENTS_TRUSTSTORE_PASSWORD_BASE64}
        keystore-password:
          base64: ${PAYMENTS_KEYSTORE_PASSWORD_BASE64}
        key-password:
          base64: ${PAYMENTS_KEY_PASSWORD_BASE64}
```

If both `base64` and `location` are configured, `base64` has priority.

Credential values referenced by `password-ref` or `key-password-ref` can be
configured as plain `value` or explicit `base64`. When both are configured,
`base64` has priority. This is useful when JKS/PKCS12 content and its store/key
passwords are all delivered as base64 environment variables.

Password semantics:

- `authentication.key-stores.<id>.password` or `password-ref` is the store
  password used to load the `JKS`/`PKCS12` container.
- `authentication.key-stores.<id>.key-password` or `key-password-ref` is the
  private key password used for mTLS key material and JWT signing keys.
- `key-password` defaults to the resolved store password only when no key
  password is configured.
- `JKS` stores can use different store and key passwords. This is supported for
  Apache mTLS, `private_key_jwt`, and JWT bearer grant signing.

Example with different JKS store and key passwords:

```yaml
smbtech:
  rest-clients:
    authentication:
      key-stores:
        payments-signing-key:
          location: file:/opt/app/certs/signing-key.jks
          type: JKS
          password-ref: signing-store-password
          key-alias: auth
          key-password-ref: signing-key-password
      credentials:
        signing-store-password:
          value: ${SIGNING_STORE_PASSWORD}
        signing-key-password:
          value: ${SIGNING_KEY_PASSWORD}
```

---
