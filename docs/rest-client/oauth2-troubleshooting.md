# OAuth2 Troubleshooting

This page keeps the OAuth2-specific messages close to the REST client guide.
The full troubleshooting catalog lives in
[Troubleshooting](../troubleshooting.md#oauth2-and-token-acquisition).

| Symptom or message | Meaning | Fix |
|---|---|---|
| `OAuth2 client registration not configured for token request: <id>` | No supported Spring registration exists for the `token-request-id`. | Add `spring.security.oauth2.client.registration.<id>` or fix `token-request-id`. |
| `OAuth2 client registration not configured for client_credentials: <id>` | `AccessTokenClient.clientCredentials(...)` was called for a missing registration or a registration with another grant. | Use a `client_credentials` registration id. |
| `OAuth2 client registration not configured for JWT bearer grant: <id>` | `AccessTokenClient.jwtBearer(...)` was called for a missing registration or a registration with another grant. | Use a registration whose `authorization-grant-type` is `urn:ietf:params:oauth:grant-type:jwt-bearer`. |
| `client assertion configuration not found for OAuth2 registration: <id>` | A `private_key_jwt` registration has no SMBTech signing extension. | Add `smbtech.rest-clients.authentication.client-assertions.<id>`. |
| `key-store-id is required for private_key_jwt client assertion: <id>` | The client assertion extension exists but does not point to a signing keystore. | Set `authentication.client-assertions.<id>.key-store-id`. |
| `jwt-bearer configuration not found for OAuth2 registration: <id>` | A JWT bearer registration has no SMBTech JWT assertion extension. | Add `smbtech.rest-clients.authentication.jwt-bearer.<id>`. |
| `Access token does not contain expected scopes` | The returned token does not include every scope required by `clients.<name>.scopes` or `AccessTokenClient` expected scopes. | Align requested scopes in Spring registration and expected scopes in SMBTech client config. |

---
