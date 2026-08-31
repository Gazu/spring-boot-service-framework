# Use Case Guides

These guides are copy-oriented recipes for common consumer scenarios. They are
not the full reference. Use them to choose the right configuration shape, then
follow the linked canonical guide when you need every property or extension
detail.

| Use case | Guide |
|---|---|
| Call an OAuth2 provider with `client_credentials` and `private_key_jwt` client authentication | [Client Credentials With Private Key JWT](client-credentials-private-key-jwt.md) |
| Send dynamic JWT bearer grant claims from runtime context | [JWT Bearer Dynamic Claims](jwt-bearer-dynamic-claims.md) |
| Provide JKS or PKCS12 keystore content and passwords as base64 environment variables | [Base64 Keystore Configuration](base64-keystore.md) |
| Disable OAuth2 access token cache for one grant type or both grant types | [Disable Token Cache](disable-token-cache.md) |
| Add small OAuth2 custom behavior without replacing the full token client | [Customize OAuth2](customize-oauth2.md) |
| Replace framework defaults with application-provided beans | [Replace Default Beans](replace-default-beans.md) |
| Replace copied `shared/exception` classes and handlers | [Migrate From shared/exception](migrate-shared-exception.md) |
| Apply pre-1.0 source, binary, dependency, and configuration changes | [Pre-1.0 Migration Guide](migrate-public-names-and-properties.md) |
| Generate OpenAPI models, server API, and REST client artifacts | [Generate OpenAPI Contract Artifacts](openapi-generated-artifacts.md) |
| Detect breaking OpenAPI changes and enforce SemVer | [Check OpenAPI Breaking Changes](check-openapi-breaking-changes.md) |
| Test a Spring MVC API against its OpenAPI contract | [OpenAPI Contract Testing](openapi-contract-testing.md) |
| Run and inspect the repository OpenAPI fixtures | [OpenAPI Examples](../openapi/examples.md) |
| Diagnose an OpenAPI build or compatibility failure | [OpenAPI Troubleshooting](../openapi/troubleshooting.md) |

## Canonical References

- [REST Client Starter Guide](../rest-client.md)
- [REST Client Extension Points](../rest-client-extension-points.md)
- [REST Client Property Reference](../rest-client/property-reference.md)
- [Error Handling Guide](../error-handling.md)
- [Error Handling Extension Points](../error-handling-extension-points.md)
- [Error Handling Property Reference](../error-handling/property-reference.md)
- [OpenAPI Code Generation](../openapi-codegen.md)
- [OpenAPI Portal](../openapi/index.md)
- [REST client consumer example](../../examples/rest-client-consumer/README.md)
