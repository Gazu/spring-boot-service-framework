# Replace Default Beans

Use this when application code must fully replace framework behavior instead of
adding a small customizer.

The starter follows Spring Boot style replacement: publish a bean of the public
type, and auto-configuration backs off or decorates only when documented.

## Replace Token Acquisition

```java
import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Set;

@Configuration
class TokenClientConfiguration {

    @Bean
    AccessTokenClient accessTokenClient(CompanyTokenService tokenService) {
        return new AccessTokenClient() {
            @Override
            public AccessToken clientCredentials(String tokenRequestId) {
                return clientCredentials(tokenRequestId, "");
            }

            @Override
            public AccessToken clientCredentials(String tokenRequestId, String expectedScopes) {
                String token = tokenService.clientCredentials(tokenRequestId, expectedScopes);
                return new AccessToken(token, "Bearer", Instant.now().plusSeconds(300), Set.of());
            }

            @Override
            public AccessToken jwtBearer(String tokenRequestId) {
                return jwtBearer(tokenRequestId, "");
            }

            @Override
            public AccessToken jwtBearer(String tokenRequestId, String expectedScopes) {
                String token = tokenService.jwtBearer(tokenRequestId, expectedScopes);
                return new AccessToken(token, "Bearer", Instant.now().plusSeconds(300), Set.of());
            }
        };
    }
}
```

## Common Replacement Points

| Behavior | Bean type |
|---|---|
| Programmatic token acquisition | `AccessTokenClient` |
| Legacy token access for configured clients | `AccessTokenProvider` |
| Request context storage | `RequestContextManager` |
| Named `RestClient` lookup | `RestClientRegistry` |
| Declarative HTTP proxy creation | `ApiClientFactory` |
| OAuth2 authorized client service | `OAuth2AuthorizedClientService` |
| OAuth2 authorized client provider | `OAuth2AuthorizedClientProvider` |
| OAuth2 authorized client manager | `OAuth2AuthorizedClientManager` |

## Rules

- Prefer customizers for small additive changes.
- Use replacement points when the application owns the whole behavior.
- Keep consumer code on public API packages.
- Do not instantiate internal auto-configuration or adapter classes directly.

## See Also

- [REST Client Extension Points](../rest-client-extension-points.md)
- [Customize OAuth2](customize-oauth2.md)
- [Compatibility](../compatibility.md)
