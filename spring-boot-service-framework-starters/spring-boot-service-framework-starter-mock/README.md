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
- JSON mock response loading from `classpath:` or `file:` locations;
- optional artificial response delay.

## Architecture

The starter is a Spring adapter over the framework-neutral mock core:

```text
smbtech.mocks properties
  -> PropertiesMockDefinitionSource
  -> DefaultMockCatalog
  -> DefaultMockResponder
  -> ResourceMockResponseSource
  -> SpringMockService / MockResponseEntityMapper
```

Core contracts remain in `spring-boot-service-framework-mock-core`:

- `MockRequest`
- `MockResponse`
- `MockResponder`
- `MockDefinitionSource`
- `MockResponseSource`

Spring-specific conversion stays in this starter. This keeps the core reusable
for future RestClient interceptors, controllers, test helpers, or custom
clients.

## Module coordinates

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-starter-mock:0.1.0-SNAPSHOT'
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

## Current limitations

The public `MockService` facade is intentionally Spring/Jackson oriented because
it returns `ResponseEntity<T>` and accepts Jackson `TypeReference<T>` for generic
payloads.

For framework-neutral integrations, depend on the core `MockResponder` bean
instead. The next phases can add RestClient adapters on top of the same
responder without changing the core contract.

## Validation

```bash
./gradlew :spring-boot-service-framework-starters:spring-boot-service-framework-starter-mock:check
```
