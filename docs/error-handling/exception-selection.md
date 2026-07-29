# Exception Selection

Choose an exception according to who owns the failure and how it should be
resolved before the global exposure policy builds the HTTP response.

| Failure | Exception | Use it when |
|---|---|---|
| Application or domain rule | `ServiceException` | The application owns a stable `ErrorDefinition` with a code, safe resolved message, category, and metadata. The global response exposure determines the final representation. Keep diagnostics and the cause separate from response data. |
| HTTP client definition | `HttpClientConfigurationException` | A client definition or static framework setting is invalid. Prefer startup validation for property errors. Do not use it for a remote HTTP response. |
| Mock definition or resource | `MockException` | Mock configuration, matching, loading, or response creation fails inside the mock capability. |
| HTTP client authentication | `HttpClientAuthenticationException` | Credentials, keystore material, OAuth2 registration, token acquisition, JWT signing, or expected-scope validation fails for an outbound client. |
| Downstream HTTP response | `HttpClientResponseException` | A downstream server returned an unsuccessful HTTP response. The exception preserves inspectable response details for internal handling. |
| Open circuit | `CircuitBreakerOpenException` | An outbound call is rejected because the configured circuit breaker is open. |

Do not wrap `HttpClientResponseException` in `ServiceException` merely to obtain
an HTTP response. The error handling starter already converts it into a safe
`DOWNSTREAM` or `RATE_LIMIT` notification while retaining diagnostics
internally. Translate it to `ServiceException` only at an application boundary
that deliberately replaces the infrastructure failure with a stable
application error.

Configuration and authentication exception messages are diagnostic. They must
not be copied directly into response notifications. Preserve their causes and
let the resolution pipeline and global exposure policy build the response.

Do not create adapter-specific `RuntimeException` subclasses when one of these
owning-module exceptions describes the failure.
