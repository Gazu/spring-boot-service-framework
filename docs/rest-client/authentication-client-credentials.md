# Authentication

## Basic authentication

Use inline values for non-secret examples, or `*-ref` properties for credentials
defined in `smbtech.rest-clients.authentication.credentials`.

```yaml
smbtech:
  rest-clients:
    clients:
      secure:
        base-url: https://secure.example
        authentication-type: BASIC_AUTH
        basic-authentication:
          username-ref: secure-username
          password-ref: secure-password
    authentication:
      credentials:
        secure-username:
          value: demo
        secure-password:
          value: ${SECURE_PASSWORD}
```

## Client credentials access token

Target configuration uses Spring Boot OAuth2 Client for OAuth2 provider and
registration data. `token-request-id` must match the Spring
Security `registration-id`.

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          my-provider:
            token-uri: https://auth.example/oauth2/token
        registration:
          payments-token:
            provider: my-provider
            client-id: payments-client
            client-secret: ${PAYMENTS_CLIENT_SECRET}
            client-authentication-method: client_secret_basic
            authorization-grant-type: client_credentials
            scope:
              - payments.read
              - payments.write

smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        authentication-type: CLIENT_CREDENTIALS
        token-request-id: payments-token
        scopes: payments.read payments.write
```

For `private_key_jwt`, keep the OAuth2 registration in Spring properties and add
the signing extension under `smbtech.rest-clients.authentication.client-assertions`:

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          my-provider:
            token-uri: https://auth.example/oauth2/token
            jwk-set-uri: https://auth.example/oauth2/certs
        registration:
          payments-token:
            provider: my-provider
            client-id: payments-client
            client-authentication-method: private_key_jwt
            authorization-grant-type: client_credentials
            scope:
              - payments.read

smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        authentication-type: CLIENT_CREDENTIALS
        token-request-id: payments-token
        scopes: payments.read
    authentication:
      client-assertions:
        payments-token:
          key-store-id: payments-signing-key
          token-lifetime: 60s
          custom-claims:
            acgp: acgp.ct
      key-stores:
        payments-signing-key:
          base64: ${PAYMENTS_SIGNING_KEY_JKS_BASE64}
          type: JKS
          password-ref: signing-store-password
          key-alias: auth
          key-password-ref: signing-key-password
      credentials:
        signing-store-password:
          base64: ${PAYMENTS_SIGNING_STORE_PASSWORD_BASE64}
        signing-key-password:
          base64: ${PAYMENTS_SIGNING_KEY_PASSWORD_BASE64}
```

`password-ref` is used to open the keystore. `key-password-ref` is used to
recover the private key entry identified by `key-alias`. They may point to
different credential values, which is common for `JKS` stores.

OAuth2 token acquisition is Spring-registration based. The consuming
application must provide Spring Boot OAuth2 Client registrations under
`spring.security.oauth2.client.registration`. The starter resolves those
registrations by the id configured in
`smbtech.rest-clients.clients.<name>.token-request-id`.

### Configuration migration

`token-request-id` replaces the former `credential-token-requestor-id` key.
This is an explicit configuration change and the former key is not accepted as
an alias. Update every REST client configuration before upgrading.

The starter exposes Spring Boot OAuth2 Client auto-configuration through
`spring-boot-starter-oauth2-client`. It intentionally does not bind
`spring.security.oauth2.client.*` directly. It only consumes the
`ClientRegistrationRepository` bean produced by Spring Boot OAuth2 Client
auto-configuration.

Supported token flows:

| Flow | Spring registration grant | SMBTech extension |
|---|---|---|
| Client credentials with client secret | `client_credentials` | none |
| Client credentials with `private_key_jwt` client authentication | `client_credentials` plus `client-authentication-method: private_key_jwt` | `authentication.client-assertions.<registration-id>` |
| JWT bearer grant | `urn:ietf:params:oauth:grant-type:jwt-bearer` | `authentication.jwt-bearer.<registration-id>` |

If no matching Spring registration exists, token acquisition fails fast with a
configuration error. Token requests are not read from SMBTech properties; the
token endpoint, client id, client secret, grant type, authentication method, and
requested scopes are owned by Spring Boot OAuth2 Client configuration.

At startup, the validator checks enabled `CLIENT_CREDENTIALS` REST clients
against their Spring registrations. The registration must use
`authorization-grant-type: client_credentials`; `client_secret_basic` and
`client_secret_post` require a client secret; `private_key_jwt` requires
`authentication.client-assertions.<registration-id>.key-store-id`. If
`clients.<name>.scopes` contains scopes that are not requested by the Spring
registration, the validator emits a warning.

The same startup validation applies to enabled `JWT_BEARER` REST clients. The
Spring registration must use
`authorization-grant-type: urn:ietf:params:oauth:grant-type:jwt-bearer`;
supported client authentication methods are `none`, `client_secret_basic`,
`client_secret_post`, and `private_key_jwt`. The JWT bearer assertion extension
must exist under `authentication.jwt-bearer.<registration-id>` and provide a
`key-store-id`.

The validator also checks credential references at startup. Basic authentication
`username-ref` and `password-ref`, plus keystore `password-ref` and
`key-password-ref`, must point to existing
`authentication.credentials.<id>` entries.

Declared credentials and keystores that are not used by any enabled REST client,
SSL configuration, or OAuth2 signing configuration are reported as warnings.
Set `validation.fail-on-warnings=true` if unused references should fail startup.

Keystore content validation is opt-in through
`validation.validate-key-store-content=true`. When enabled, startup opens only
the keystores that are actually referenced. SSL truststores must load, mTLS
keystores must contain the configured alias/private key, and JWT signing
keystores must expose an RSA private key plus an RSA certificate. Keep it
disabled when startup must avoid opening secret material.

When validation fails, the startup exception lists each issue as
`<severity> <yaml-path> - <problem> Fix: <suggested action>`. Non-failing
warnings are logged with the same path, message, and `suggestedFix` field.

Example startup failure:

```text
Invalid SMBTech REST client OAuth2 configuration. Found 1 error(s) and 0 warning(s).

Errors:
- ERROR clients.payments.token-request-id - references missing OAuth2 registration payments-token Fix: Set smbtech.rest-clients.clients.payments.token-request-id to an existing Spring OAuth2 registration id with the expected grant type.

Review the YAML paths above under smbtech.rest-clients or spring.security.oauth2.client. To allow startup with warnings, keep smbtech.rest-clients.validation.fail-on-warnings=false.
```

Example warning event when `fail-on-warnings` is disabled:

```json
{
  "type": "OAUTH2_CONFIGURATION_VALIDATION",
  "msg": "OAuth2 REST client configuration warning",
  "data": {
    "severity": "WARNING",
    "path": "authentication.jwt-bearer.unused-token",
    "message": "is configured but no enabled JWT_BEARER REST client references this registration id",
    "suggestedFix": "Add smbtech.rest-clients.authentication.jwt-bearer.unused-token.key-store-id for JWT bearer signing, or remove the unused JWT bearer block."
  },
  "tags": ["oauth2", "configuration"]
}
```

Recommended validation posture:

| Environment | `enabled` | `fail-on-warnings` | `validate-key-store-content` | Reason |
|---|---:|---:|---:|---|
| Local development | `true` | `false` | `false` | Fail fast on broken OAuth2 wiring without forcing local keystore material. |
| CI/smoke tests | `true` | `true` | `true` when test keystores are available | Catch unused references and invalid signing material before merge. |
| Production | `true` | team choice | `false` unless startup may open secret stores | Keep configuration checks active while controlling startup access to secret material. |

`validate-key-store-content` only inspects keystores referenced by enabled
clients, SSL blocks, `private_key_jwt`, or JWT bearer signing configuration.
It does not enumerate unrelated keystore entries except to report unused ones
as warnings.

Request a token directly when application code needs one:

```java
import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;

AccessToken token = accessTokenClient.clientCredentials("payments-token", "payments.read");
String bearer = token.value();
```
