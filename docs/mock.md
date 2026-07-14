# Mock Core and Starter

This document describes the mock module family:

- `spring-boot-service-framework-mock-core`
- `spring-boot-service-framework-starters:spring-boot-service-framework-starter-mock`

The goal is to provide one reusable mock engine that can be called from Spring
MVC controllers, outbound `RestClient` calls, tests, or future custom clients.

---

## 1. Design goals

The mock feature follows the same architecture rule as the rest of the
framework:

- core code is framework-neutral;
- Spring Boot code lives in the starter;
- adapters translate runtime-specific objects into the neutral core contracts.

```mermaid
flowchart LR
    App["Consuming service"]
    Controller["Spring MVC controller"]
    RestClient["Spring RestClient"]
    Starter["spring-boot-service-framework-starter-mock"]
    Core["spring-boot-service-framework-mock-core"]
    File["JSON mock files"]

    App --> Controller
    App --> RestClient
    Controller --> Starter
    RestClient --> Starter
    Starter --> Core
    Starter --> File
```

---

## 2. Module responsibilities

| Module | Responsibility | Must not contain |
|---|---|---|
| `spring-boot-service-framework-mock-core` | Domain model, ports, and default services for mock lookup and response loading orchestration. | Spring, Jackson, Servlet, `RestClient`, Apache, SLF4J. |
| `spring-boot-service-framework-starter-mock` | Spring Boot properties, JSON file loading, controller facade, and outbound `RestClient` interceptor. | Business-specific mock scenarios. |

---

## 3. Core contracts

### `MockRequest`

Neutral request sent to the mock engine.

| Field | Description |
|---|---|
| `key` | Logical mock key, such as `payments-success`. |
| `method` | Optional method or operation, commonly `GET`, `POST`, etc. |
| `path` | Optional HTTP path or resource name. |
| `headers` | Request headers as a multi-value map. |
| `queryParams` | Query parameters as a multi-value map. |
| `body` | Raw request body bytes. |
| `attributes` | Adapter-specific metadata. |

### `MockResponse`

Neutral response returned by the mock engine.

| Field | Description |
|---|---|
| `status` | HTTP-like status code. Invalid values default to `200`. |
| `headers` | Response headers as a multi-value map. |
| `body` | Raw response body bytes. |
| `delay` | Optional artificial delay. |
| `metadata` | Adapter-specific metadata such as source file or key. |

### `MockResponder`

Main core use case:

```java
Optional<MockResponse> respond(MockRequest request);
```

Return `Optional.empty()` when no mock applies. This allows the caller to
continue normal behavior, such as executing the real HTTP request.

---

## 4. Spring Boot properties

All starter properties are under `smbtech.mocks`.

```yaml
smbtech:
  mocks:
    endpoints:
      payments-success:
        enabled: true
        file: classpath:mocks/payments-success.json
        delay: 100ms
```

| Property | Required | Default | Description |
|---|---:|---|---|
| `endpoints.<key>.enabled` | No | `false` | Enables this mock endpoint. Disabled mocks are ignored. |
| `endpoints.<key>.file` | Yes when enabled | empty | Mock response JSON file. Supports `classpath:` and `file:` locations. Plain paths are treated as classpath resources. |
| `endpoints.<key>.delay` | No | `0ms` | Artificial delay applied before loading the response. |

---

## 5. Mock response file format

```json
{
  "status": 201,
  "headers": {
    "Content-Type": "application/json",
    "X-Mock": "true",
    "Set-Cookie": ["a=1", "b=2"]
  },
  "body": {
    "id": "pay-123",
    "status": "MOCKED"
  }
}
```

| Field | Required | Default | Description |
|---|---:|---|---|
| `status` | No | `200` | HTTP status code returned to the adapter. |
| `headers` | No | empty | Response headers. Values may be strings or arrays. |
| `body` | No | empty body | Response body. Objects and arrays are serialized as JSON bytes. |

---

## 6. Controller usage

Inject `MockService` when a controller should return a configured mock response.

```java
@RestController
class PaymentsController {

    private final MockService mocks;

    PaymentsController(MockService mocks) {
        this.mocks = mocks;
    }

    @GetMapping("/api/dummy")
    ResponseEntity<PaymentResponse> dummy() {
        return mocks.responseOrNotFound("payments-success", PaymentResponse.class);
    }
}
```

Available convenience methods:

```java
Optional<ResponseEntity<String>> response(String mockKey);
<T> Optional<ResponseEntity<T>> response(String mockKey, Class<T> responseType);
<T> Optional<ResponseEntity<T>> response(String mockKey, TypeReference<T> responseType);

ResponseEntity<String> responseOrNotFound(String mockKey);
<T> ResponseEntity<T> responseOrNotFound(String mockKey, Class<T> responseType);
<T> ResponseEntity<T> responseOrNotFound(String mockKey, TypeReference<T> responseType);
```

Use `response(...)` when the controller wants custom fallback logic. Use
`responseOrNotFound(...)` when a missing or disabled mock should return `404`.

---

## 7. RestClient usage

The starter exposes `MockRestClientInterceptor`. It adapts outbound Spring
`RestClient` requests to `MockRequest`.

Mock key resolution:

1. `X-Mock-Key` request header.
2. Normalized path fallback. `/v1/payments` becomes `v1/payments`.

If the mock exists and is enabled, the interceptor returns the mock response. If
not, it executes the real HTTP request.

### Manual RestClient

```java
@Bean
RestClient paymentsRestClient(
        RestClient.Builder builder,
        MockRestClientInterceptor mockInterceptor
) {
    return builder
            .baseUrl("https://payments.example.test")
            .defaultHeader("X-Mock-Key", "payments-success")
            .requestInterceptor(mockInterceptor)
            .build();
}
```

### Framework RestClient starter

Use the existing customizer hook:

```java
@Bean
RestClientBuilderCustomizer mockRestClientCustomizer(MockRestClientInterceptor mockInterceptor) {
    return (definition, builder) -> builder.requestInterceptor(mockInterceptor);
}
```

Configure the generated client with a default mock key:

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

---

## 8. Test matrix

The mock modules are covered by focused tests at three levels.

| Area | Test coverage |
|---|---|
| Core domain | Null normalization, defensive copies, immutability, default status/delay handling. |
| Core services | Catalog key normalization, missing/disabled mocks, unusable definitions, delay delegation, null response protection. |
| Properties adapter | Null endpoint maps, null endpoint entries, endpoint-to-definition mapping. |
| Resource adapter | Classpath loading, default status, single/multi-value headers, metadata, missing/blank files. |
| Controller facade | `Class<T>`, `TypeReference<T>`, raw `String`, raw `byte[]`, `404` fallback. |
| RestClient adapter | Request mapping, header-based key, path fallback key, mock response short-circuit, real request delegation. |
| Auto-configuration | All expected Spring beans are created and wired. |

Run only mock modules:

```bash
./gradlew :spring-boot-service-framework-mock-core:check \
  :spring-boot-service-framework-starters:spring-boot-service-framework-starter-mock:check
```

Run the full framework baseline:

```bash
./gradlew baseline
```

---

## 9. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Controller returns `404` | Mock key is missing, disabled, or not configured. | Check `smbtech.mocks.endpoints.<key>.enabled=true`. |
| `Mock file does not exist` | Wrong classpath/file location. | Use `classpath:mocks/name.json` or place the file under `src/main/resources/mocks`. |
| Outbound RestClient still calls real service | `MockRestClientInterceptor` was not added, `X-Mock-Key` is missing, or no path fallback mock exists. | Add the interceptor or configure `default-headers.X-Mock-Key`. |
| Response body conversion fails | Mock JSON body does not match the target DTO. | Validate the mock file body against the controller response type. |
| Core boundary check fails | Spring/Jackson/RestClient import was added to `mock-core`. | Move adapter code to the starter. |

---

## 10. Extension points

Future work can add new adapters without changing the core:

- a `RestClientBuilderCustomizer` auto-bridge controlled by properties;
- a servlet filter/controller helper;
- dynamic mock matching by method/path/query/body;
- response templating;
- test fixtures for consumer projects.

Keep those integrations outside `mock-core`. The stable core contract should
remain `MockRequest -> Optional<MockResponse>`.
