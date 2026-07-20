# Customize OAuth2

Use this when the default OAuth2 flow is correct, but the application needs a
small extension: extra JWT bearer claims, custom `private_key_jwt` claims,
token request parameters, or tenant-aware cache identity.

For full replacement, use [Replace Default Beans](replace-default-beans.md).

## Add JWT Bearer Claims

```java
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
class JwtBearerClaimsConfiguration {

    @Bean
    JwtBearerClaimsContributor tenantClaimsContributor(TenantContext tenantContext) {
        return context -> Map.of(
                "tenant_id", tenantContext.tenantId(),
                "channel", tenantContext.channel()
        );
    }
}
```

Claims are resolved in this order:

- configured JWT bearer custom claims;
- request-context JWT bearer claims;
- explicit `AccessTokenClient.jwtBearer(...)` claims;
- ordered `JwtBearerClaimsContributor` beans.

## Customize Private Key JWT Client Assertions

```java
import com.smbtech.serviceframework.starter.restclient.api.oauth2.ClientAssertionCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClientAssertionConfiguration {

    @Bean
    ClientAssertionCustomizer addClientAssertionClaim() {
        return context -> context.toBuilder()
                .claim("channel", "backend")
                .build();
    }
}
```

## Customize Token Requests

```java
import com.smbtech.serviceframework.starter.restclient.api.oauth2.OAuth2TokenRequestCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TokenRequestConfiguration {

    @Bean
    OAuth2TokenRequestCustomizer addTokenRequestParameter() {
        return context -> context.toBuilder()
                .parameter("resource", "payments")
                .build();
    }
}
```

## Customize Token Cache Identity

```java
import com.smbtech.serviceframework.starter.restclient.api.oauth2.AccessTokenCacheKeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TokenCacheConfiguration {

    @Bean
    AccessTokenCacheKeyResolver tenantAwareTokenCacheKey(TenantContext tenantContext) {
        return context -> context.defaultKey() + "::tenant=" + tenantContext.tenantId();
    }
}
```

## Rules

- Contributors and customizers can be ordered with `@Order`.
- `AccessTokenCacheKeyResolver` is a single optional bean.
- Do not depend on internal `adapter.out.*` classes.

## See Also

- [REST Client Extension Points](../rest-client-extension-points.md)
- [Token Cache and Scope Validation](../rest-client/token-cache.md)
- [JWT Bearer Dynamic Claims](jwt-bearer-dynamic-claims.md)
