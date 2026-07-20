# Request Context Propagation

`RequestContextManager` can propagate dynamic request headers and dynamic JWT
bearer custom claims from the current thread scope:

```yaml
smbtech:
  rest-clients:
    request-context:
      enabled: true
      headers: true
      jwt-bearer-claims: true
      blocked-headers:
        - X-Internal-Secret
      blocked-jwt-bearer-claims:
        - customer_secret
```

Open a context with try-with-resources around the outbound call that needs the
dynamic values:

```java
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextScope;
import org.springframework.stereotype.Service;

@Service
class PaymentLookupService {

    private final PaymentsApi paymentsApi;
    private final RequestContextManager requestContextManager;

    PaymentLookupService(
            PaymentsApi paymentsApi,
            RequestContextManager requestContextManager
    ) {
        this.paymentsApi = paymentsApi;
        this.requestContextManager = requestContextManager;
    }

    String findPayment(String customerId, String channel, String correlationId) {
        try (RequestContextScope ignored = requestContextManager.open(context -> context
                .header("X-Correlation-Id", correlationId)
                .header("X-Channel", channel)
                .jwtBearerClaim("customer_id", customerId)
                .jwtBearerClaim("channel", channel))) {
            return paymentsApi.dummy();
        }
    }
}
```

The scope is thread-bound and must be closed. Nested scopes are supported; the
previous context becomes active again when the inner scope closes. Use this API
for per-call data only, such as tenant, customer, channel, correlation, or
trace-related values that are known at execution time.

All three flags default to `true`. Set `request-context.enabled=false` to disable
all request context propagation. Set `request-context.headers=false` when dynamic
headers must not be copied to outgoing HTTP requests. Set
`request-context.jwt-bearer-claims=false` when `RequestContext` claims must not
be used while generating JWT bearer grant assertions. Explicit claims passed to
`AccessTokenClient.jwtBearer(...)` are still honored.

Dynamic header propagation is sanitized before the request is executed. Header
names must be valid HTTP token names, header values containing CR/LF are ignored,
blank values are ignored, and existing request headers are not overwritten.
`Authorization`, `Proxy-Authorization`, `Cookie`, `Set-Cookie`, `Host`,
`Content-Length`, `Transfer-Encoding`, and `Connection` are always blocked.
Use `blocked-headers` to add application-specific sensitive header names.

JWT bearer custom claims are also sanitized. Registered JWT claims such as
`iss`, `sub`, `aud`, `jti`, `iat`, `exp`, and `nbf` are always blocked because
the starter owns those values. Token and secret-shaped claims such as
`access_token`, `refresh_token`, `id_token`, `token`, `password`, `secret`,
`client_secret`, and `private_key` are blocked by default. Matching is
case-insensitive. Use `blocked-jwt-bearer-claims` to add domain-specific claims
that must never be propagated dynamically or emitted from configured custom
claims.

Static claims configured under
`authentication.jwt-bearer.<registration-id>.custom-claims` are the baseline for
the JWT bearer assertion. Dynamic `RequestContext` claims are applied after that
baseline and can override non-sensitive static custom claims for the current
call. Reserved and sensitive claims are removed from both sources before the
assertion is signed.

Request context propagation is synchronous. It does not automatically cross
thread, executor, scheduler, or reactive boundaries. If an outbound call is
executed on another thread, open a new `RequestContextScope` in that execution
path with the values that should be propagated.

`clients.<name>.scopes` and the `expectedScopes` argument in `AccessTokenClient`
do not change the scopes requested from the authorization server. They define
the minimum scopes that must be present in the returned access token. If the
token response omits the `scope` field in the Spring `client_credentials` flow,
the starter validates against the scopes declared in the Spring registration.
