# Spring Boot Service Framework Mock Starter

Spring Boot starter for loading mock responses from configuration and resource
files.

This starter adapts `spring-boot-service-framework-mock-core` to Spring Boot. It currently
provides:

- `smbtech.mocks` configuration properties;
- core `MockDefinitionSource`, `MockCatalog`, `MockResponseSource`, and
  `MockResponder` beans;
- a controller-friendly `MockService` bean for optional mock lookup by key or
  direct `404 Not Found` fallback responses;
- a future-ready `MockRestClientInterceptor` bean for outbound Spring
  `RestClient` calls;
- JSON mock response loading from `classpath:` or `file:` locations;
- optional artificial response delay.

## When to use

Use this starter when a Spring Boot service needs configured mock responses for
controllers, integration tests, or outbound `RestClient` calls. It is useful for
local development, contract exploration, and deterministic test fixtures.

Use `spring-boot-service-framework-mock-core` directly only when building a
framework-neutral adapter or test helper that should not depend on Spring,
Jackson, Servlet, or `RestClient` APIs.

## Architecture

The starter is a Spring adapter over the framework-neutral mock core:

```text
smbtech.mocks properties
  -> PropertiesMockDefinitionSource
  -> DefaultMockCatalog
  -> DefaultMockResponder
  -> ResourceMockResponseSource
  -> SpringMockService / MockResponseEntityMapper
  -> MockRestClientInterceptor
```

Core contracts remain in `spring-boot-service-framework-mock-core`:

- `MockRequest`
- `MockResponse`
- `MockResponder`
- `MockDefinitionSource`
- `MockResponseSource`

Spring-specific conversion stays in this starter. This keeps the core reusable
for RestClient interceptors, controllers, test helpers, or custom clients.

## Module coordinates

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-starter-mock:0.2.0'
}
```

## Configuration

```yaml
smbtech:
  mocks:
    endpoints:
      payments-success:
        enabled: true
        file: classpath:mocks/payments-success.json
        delay: 100ms
```

If `file` does not start with `classpath:` or `file:`, it is treated as a
classpath resource.

## Mock file format

```json
{
  "status": 200,
  "headers": {
    "Content-Type": "application/json",
    "X-Mock": "true"
  },
  "body": {
    "id": "pay-123",
    "status": "MOCKED"
  }
}
```

## Usage

### Controller-friendly response

Use `responseOrNotFound` when the endpoint should return the configured mock or
`404 Not Found` when the mock key is missing or disabled.

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

Use the no-argument variant when the controller should return the raw JSON
payload as a `String`.

```java
@GetMapping("/api/dummy/raw")
ResponseEntity<String> rawDummy() {
    return mocks.responseOrNotFound("payments-success");
}
```

### Optional response

Use `response` or `exchangeMock` when the controller wants to decide the fallback
itself.

```java
class PaymentsController {

    private final MockService mocks;

    PaymentsController(MockService mocks) {
        this.mocks = mocks;
    }

    ResponseEntity<Map<String, Object>> dummy() {
        return mocks.response(
                "payments-success",
                new TypeReference<Map<String, Object>>() {
                }
        ).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

### Available `MockService` methods

```java
Optional<ResponseEntity<String>> response(String mockKey);
<T> Optional<ResponseEntity<T>> response(String mockKey, Class<T> responseType);
<T> Optional<ResponseEntity<T>> response(String mockKey, TypeReference<T> responseType);

ResponseEntity<String> responseOrNotFound(String mockKey);
<T> ResponseEntity<T> responseOrNotFound(String mockKey, Class<T> responseType);
<T> ResponseEntity<T> responseOrNotFound(String mockKey, TypeReference<T> responseType);
```

`exchangeMock` remains available as the lower-level alias behind the facade.

### Future RestClient integration

The starter exposes a `MockRestClientInterceptor` bean. The interceptor adapts
outbound Spring `RestClient` calls to the neutral core `MockResponder`.

Resolution rules:

1. If the outgoing request contains `X-Mock-Key`, that value is used as the mock
   key.
2. If `X-Mock-Key` is missing, the request path is used as fallback. For example
   `/v1/payments` becomes `v1/payments`.
3. If no enabled mock matches the resolved key, the real HTTP request continues.

This keeps mocks opt-in and safe. Adding the starter does not automatically
replace outbound HTTP calls.

Manual `RestClient` usage:

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

When using `spring-boot-service-framework-starter-rest-client`, connect both
starters with the existing customizer hook:

```java
@Bean
RestClientBuilderCustomizer mockRestClientCustomizer(MockRestClientInterceptor mockInterceptor) {
    return (definition, builder) -> builder.requestInterceptor(mockInterceptor);
}
```

Then configure the mock key as a default header on the generated RestClient:

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

The outgoing request still flows through the configured `RestClient` pipeline.
If `payments-success` is enabled, the interceptor returns the configured mock
response. If it is missing or disabled, the call continues to the real remote
service.

## Current limitations

The public `MockService` facade is intentionally Spring/Jackson oriented because
it returns `ResponseEntity<T>` and accepts Jackson `TypeReference<T>` for generic
payloads.

For framework-neutral integrations, depend on the core `MockResponder` bean
instead.

## Validation

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-mock:check
```

For the full mock guide, including architecture, property reference, RestClient
integration, test matrix, and troubleshooting, see
[../../docs/mock.md](../../docs/mock.md).
