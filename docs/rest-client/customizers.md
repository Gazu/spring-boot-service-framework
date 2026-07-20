# Customizers

Use customizers when the framework default is close, but one service needs a
small adapter-specific adjustment.

```java
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;

@Bean
RestClientBuilderCustomizer addHeaderForPayments() {
    return (definition, builder) -> {
        if ("payments".equals(definition.name())) {
            builder.defaultHeader("X-Consumer", "orders-service");
        }
    };
}
```

Available customizer extension points:

| Interface | Responsibility |
|---|---|
| `RestClientBuilderCustomizer` | Customize `RestClient.Builder` before the final client is built. |
| `ApacheHttpClientBuilderCustomizer` | Customize Apache `HttpClientBuilder` when `client-type=APACHE_HTTP`. |
| `ClientHttpRequestFactoryCustomizer` | Customize the Spring `ClientHttpRequestFactory`. |

## Mock starter integration

When the application also includes
`spring-boot-service-framework-starter-mock`, outbound calls can be mocked by
adding the mock interceptor through the standard customizer hook:

```java
import com.smbtech.serviceframework.starter.mock.adapter.out.restclient.MockRestClientInterceptor;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;

@Bean
RestClientBuilderCustomizer mockRestClientCustomizer(MockRestClientInterceptor mockInterceptor) {
    return (definition, builder) -> builder.requestInterceptor(mockInterceptor);
}
```

Use `X-Mock-Key` as a default header to select the mock response for a generated
client:

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example.test
        default-headers:
          X-Mock-Key: payments-success
  mocks:
    endpoints:
      payments-success:
        enabled: true
        file: classpath:mocks/payments-success.json
```

If the mock key is enabled, the call returns the configured mock response. If the
mock key is missing or disabled, the interceptor lets the real HTTP call
continue.

---
