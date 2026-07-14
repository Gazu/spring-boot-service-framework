# REST Client consumer example

Standalone Spring Boot application that consumes
`com.smbtech:spring-boot-service-framework-starter-rest-client:0.2.0` from the local
Maven repositories generated under each module `build/repository` directory.

The example intentionally consumes published local artifacts instead of Gradle
`project(...)` dependencies. This validates the same POMs and JARs that another
repository would consume.

From the framework root:

```bash
./gradlew restClientConsumerSmoke
```

It demonstrates:

- `smbtech.rest-clients` configuration;
- automatic creation of `paymentsRestClient`;
- `ApiClientFactory`;
- declarative `@HttpApiClient` + `@HttpExchange` interfaces;
- Spring Boot OAuth2 Client registrations;
- `client_credentials` with `private_key_jwt` client authentication;
- JWT bearer grant (`urn:ietf:params:oauth:grant-type:jwt-bearer`);
- signing keys loaded from base64 keystore properties;
- token cache policy for `client_credentials` and JWT bearer grants;
- retry and circuit breaker configuration.

## Configured clients

`application.yml` defines two HTTP clients:

| Client | Authentication type | OAuth2 registration | Purpose |
|---|---|---|---|
| `payments` | `CLIENT_CREDENTIALS` | `payments-client-credentials-token` | Demonstrates `client_credentials` authenticated with `private_key_jwt`. |
| `payments-jwt-bearer` | `JWT_BEARER` | `payments-jwt-bearer-token` | Demonstrates JWT bearer grant with a signed assertion and custom claims. |

`PaymentsApi` uses:

```java
@HttpApiClient("payments-jwt-bearer")
```

so the `/api/dummy` endpoint exercises the JWT bearer grant path. The
`payments` client remains configured to validate the client credentials path and
Spring Boot registration wiring.

## Required environment variables

The example expects sensitive values to be supplied through environment
variables when it is run manually. Do not paste keystores, passwords, private
keys, or real client ids into `application.yml`.

| Variable | Used by | Description |
|---|---|---|
| `OAUTH2_TOKEN_URI` | both registrations | OAuth2 token endpoint. Defaults to `http://localhost:8081/oauth2/token` for local testing. |
| `PAYMENTS_API_BASE_URL` | both clients | Downstream payments base URL. Defaults to `http://localhost:9999`. |
| `PAYMENTS_CLIENT_CREDENTIALS_CLIENT_ID` | client credentials | OAuth2 client id for the `client_credentials` registration. |
| `PAYMENTS_CLIENT_CREDENTIALS_KEYSTORE_BASE64` | client credentials | Base64-encoded keystore used to sign the `private_key_jwt` client assertion. |
| `PAYMENTS_CLIENT_CREDENTIALS_KEYSTORE_TYPE` | client credentials | Keystore type. Defaults to `JKS`. |
| `PAYMENTS_CLIENT_CREDENTIALS_KEY_ALIAS` | client credentials | Private key alias inside the keystore. |
| `PAYMENTS_CLIENT_CREDENTIALS_KEYSTORE_PASSWORD_BASE64` | client credentials | Base64-encoded keystore password. |
| `PAYMENTS_CLIENT_CREDENTIALS_KEY_PASSWORD_BASE64` | client credentials | Base64-encoded private key password. |
| `PAYMENTS_JWT_BEARER_CLIENT_ID` | JWT bearer | OAuth2 client id for the JWT bearer registration. |
| `PAYMENTS_JWT_BEARER_KEYSTORE_BASE64` | JWT bearer | Base64-encoded keystore used to sign the JWT bearer assertion. |
| `PAYMENTS_JWT_BEARER_KEYSTORE_TYPE` | JWT bearer | Keystore type. Defaults to `JKS`. |
| `PAYMENTS_JWT_BEARER_KEY_ALIAS` | JWT bearer | Private key alias inside the keystore. |
| `PAYMENTS_JWT_BEARER_KEYSTORE_PASSWORD_BASE64` | JWT bearer | Base64-encoded keystore password. |
| `PAYMENTS_JWT_BEARER_KEY_PASSWORD_BASE64` | JWT bearer | Base64-encoded private key password. |
| `PAYMENTS_JWT_BEARER_CUSTOMER_ID` | JWT bearer | Example custom claim added to the JWT bearer assertion. |

Optional JWT bearer assertion overrides:

| Variable | Default |
|---|---|
| `PAYMENTS_JWT_BEARER_ISSUER` | resolved by the starter from the client registration/client id when omitted |
| `PAYMENTS_JWT_BEARER_SUBJECT` | resolved by the starter from the client registration/client id when omitted |
| `PAYMENTS_JWT_BEARER_AUDIENCE` | resolved by the starter from the token URI when omitted |

## Manual run

Publish local framework artifacts first:

```bash
./gradlew publishLocalArtifacts
```

Then export the environment variables for the grant you want to test and run the
example:

```bash
cd examples/rest-client-consumer
../../gradlew bootRun
```

The default `application.yml` uses `http://localhost:9999` as an example URL.
Tests override it dynamically with a local HTTP server.

## Tests

The smoke test creates local token and payments servers, generates a temporary
PKCS12 keystore, and verifies that:

- the published starter artifacts can be consumed;
- `RestClientRegistry` exposes `payments` and `payments-jwt-bearer`;
- the declarative `PaymentsApi` proxy can call the downstream service;
- the JWT bearer assertion is signed and sent as the `assertion` token request
  parameter;
- the downstream request receives a bearer access token and default headers.
