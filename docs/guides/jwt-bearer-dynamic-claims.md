# JWT Bearer Dynamic Claims

Use this when the application must request an OAuth2 access token with
`urn:ietf:params:oauth:grant-type:jwt-bearer` and some JWT assertion claims are
known only during the current business operation.

Static signing configuration lives in `application.yml`. Dynamic claims should
be passed with `RequestContextManager`, explicit `AccessTokenClient` requests,
or a `JwtBearerClaimsContributor`.

## Static Configuration

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          my-provider:
            token-uri: ${OAUTH2_TOKEN_URI}
        registration:
          payments-jwt-token:
            provider: my-provider
            client-id: ${PAYMENTS_JWT_CLIENT_ID}
            client-authentication-method: none
            authorization-grant-type: urn:ietf:params:oauth:grant-type:jwt-bearer
            scope:
              - payment.write

smbtech:
  rest-clients:
    clients:
      payments:
        base-url: ${PAYMENTS_API_BASE_URL}
        authentication-type: JWT_BEARER
        token-request-id: payments-jwt-token
        scopes: payment.write
    request-context:
      enabled: true
      headers: true
      jwt-bearer-claims: true
    authentication:
      jwt-bearer:
        payments-jwt-token:
          key-store-id: payments-jwt-signing-key
          token-lifetime: 5m
          custom-claims:
            channel: backend
      key-stores:
        payments-jwt-signing-key:
          base64: ${PAYMENTS_JWT_KEYSTORE_BASE64}
          type: JKS
          password-ref: payments-jwt-store-password
          key-alias: ${PAYMENTS_JWT_KEY_ALIAS}
          key-password-ref: payments-jwt-key-password
      credentials:
        payments-jwt-store-password:
          base64: ${PAYMENTS_JWT_KEYSTORE_PASSWORD_BASE64}
        payments-jwt-key-password:
          base64: ${PAYMENTS_JWT_KEY_PASSWORD_BASE64}
```

If `issuer`, `subject`, or `audience` are omitted, the starter resolves them
from the Spring registration client id and provider token URI.

## Dynamic Claims Around A Call

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

    String findPayment(String customerId, String channel) {
        try (RequestContextScope ignored = requestContextManager.open(context -> context
                .header("X-Channel", channel)
                .jwtBearerClaim("customer_id", customerId)
                .jwtBearerClaim("channel", channel))) {
            return paymentsApi.dummy();
        }
    }
}
```

## Dynamic Claims For Direct Token Requests

```java
AccessToken token = accessTokenClient.jwtBearer(
        new JwtBearerTokenRequest(
                "payments-jwt-token",
                "payment.write",
                Map.of("customer_id", customerId)
        )
);
```

## Cache Behavior

When JWT bearer token cache is enabled, dynamic claims are part of the cache
identity. Equivalent dynamic claim sets can reuse an access token. Different
claim sets produce separate cache identities.

## See Also

- [JWT Bearer Access Token](../rest-client/authentication-jwt-bearer.md)
- [Request Context Propagation](../rest-client/request-context.md)
- [Token Cache and Scope Validation](../rest-client/token-cache.md)
- [Customize OAuth2](customize-oauth2.md)
