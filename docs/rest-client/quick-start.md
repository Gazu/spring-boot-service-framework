# Minimal Client

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        default-headers:
          X-Application-Name: orders-service
```

This registers:

- a `RestClient` bean named `paymentsRestClient`;
- a `payments` entry in `RestClientRegistry`;
- a configured client available through `ApiClientFactory`.

Inject the generated bean directly with `@Qualifier`:

```java
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
class PaymentsService {

    private final RestClient payments;

    PaymentsService(@Qualifier("paymentsRestClient") RestClient payments) {
        this.payments = payments;
    }
}
```

Override the generated bean name when a service needs a specific name:

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        bean-name: paymentsApiRestClient
        base-url: https://payments.example
```

---

## Runtime Access Patterns

## `RestClientRegistry`

Use the registry when a service needs to choose a client dynamically:

```java
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
class DynamicHttpService {

    private final RestClientRegistry registry;

    DynamicHttpService(RestClientRegistry registry) {
        this.registry = registry;
    }

    String call(String clientName) {
        RestClient client = registry.get(clientName);
        return client.get()
                .uri("/dummy")
                .retrieve()
                .body(String.class);
    }
}
```

Available methods:

| Method | Purpose |
|---|---|
| `get(String name)` | Returns the configured `RestClient` for a client name. |
| `names()` | Returns all registered client names. |
| `all()` | Returns the registered clients as a map. |

## Declarative HTTP interfaces

```java
import com.smbtech.serviceframework.starter.restclient.api.HttpApiClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpApiClient("payments")
@HttpExchange
public interface PaymentsApi {

    @GetExchange("/dummy")
    String dummy();
}
```

```java
import com.smbtech.serviceframework.starter.restclient.api.ApiClientFactory;
import org.springframework.stereotype.Service;

@Service
class PaymentsFacade {

    private final PaymentsApi paymentsApi;

    PaymentsFacade(ApiClientFactory apiClientFactory) {
        this.paymentsApi = apiClientFactory.create(PaymentsApi.class);
    }

    String dummy() {
        return paymentsApi.dummy();
    }
}
```

You can also bypass `@HttpApiClient` and pass the configured client name
explicitly:

```java
PaymentsApi paymentsApi = apiClientFactory.create("payments", PaymentsApi.class);
```

---
