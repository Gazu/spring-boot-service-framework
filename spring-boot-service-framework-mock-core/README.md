# Spring Boot Service Framework Mock Core

Framework-independent mock domain and exceptions for SMB Tech services.

This module is the future hexagonal core for mock behavior. It must remain free
from Spring, Jackson, Servlet, RestClient, and other adapter APIs. Spring Boot
integration belongs in `spring-boot-service-framework-starters/spring-boot-service-framework-starter-mock`.

## Module coordinates

```groovy
dependencies {
    implementation 'com.smbtech:spring-boot-service-framework-mock-core:0.1.0-SNAPSHOT'
}
```

Most applications should consume the Spring Boot starter instead of depending on
this module directly.

## Package map

```text
com.smbtech.serviceframework.mock
├── domain      # Framework-neutral mock definitions, requests, and responses
├── exception   # Core mock exceptions
├── port.in     # Inbound mock catalog and responder APIs
├── port.out    # Outbound definition and response source ports
└── service     # Default core services
```

## Current model

### `MockDefinition`

`MockDefinition` describes one configured mock endpoint:

- `key`: logical mock key, for example `payments-success`;
- `enabled`: whether the mock can be used;
- `file`: location of the response definition;
- `delay`: optional artificial delay.

```java
MockDefinition definition = new MockDefinition(
        "payments-success",
        true,
        "classpath:mocks/payments-success.json",
        Duration.ofMillis(100)
);

boolean usable = definition.isUsable();
```

### `MockException`

`MockException` is the base runtime exception for mock configuration and loading
errors.

### `MockRequest`

`MockRequest` is the framework-neutral request passed to a mock responder. It is
designed to be created by different adapters, such as Spring MVC controllers,
RestClient interceptors, or custom test clients.

It contains:

- `key`: logical mock key, for example `payments-success`;
- `method`: optional HTTP method or operation name;
- `path`: optional path or resource name;
- `headers`: request headers as a multi-value map;
- `queryParams`: query parameters as a multi-value map;
- `body`: raw request body bytes;
- `attributes`: adapter-specific metadata.

```java
MockRequest request = new MockRequest(
        "payments-success",
        "GET",
        "/v1/payments",
        Map.of("X-Application-Name", List.of("orders-service")),
        Map.of("status", List.of("active")),
        new byte[0],
        Map.of("source", "rest-client")
);
```

### `MockResponse`

`MockResponse` is the framework-neutral response returned by a mock responder.
Adapters convert it to their runtime response type, such as Spring
`ResponseEntity`, `ClientHttpResponse`, or another custom response object.

It contains:

- `status`: HTTP-like status code, defaulting to `200` when invalid;
- `headers`: response headers as a multi-value map;
- `body`: raw response body bytes;
- `delay`: optional artificial delay;
- `metadata`: adapter-specific metadata.

```java
MockResponse response = new MockResponse(
        200,
        Map.of("Content-Type", List.of("application/json")),
        "{\"status\":\"MOCKED\"}".getBytes(StandardCharsets.UTF_8)
);
```

Both `MockRequest` and `MockResponse` defensively copy byte arrays and expose
immutable maps/lists.

## Inbound port

### `MockCatalog`

`MockCatalog` exposes configured mock definitions by key:

```java
Optional<MockDefinition> definition = catalog.findByKey("payments-success");
MockDefinition required = catalog.requireByKey("payments-success");
Set<String> keys = catalog.keys();
```

### `MockResponder`

`MockResponder` is the neutral entry point for asking the mock engine whether a
mock response exists for a request:

```java
public interface MockResponder {
    Optional<MockResponse> respond(MockRequest request);
}
```

Return `Optional.empty()` when no mock applies and the caller should continue
with its normal behavior.

This contract allows integrations such as:

- Spring MVC controllers returning mock responses;
- RestClient interceptors returning mock downstream responses;
- custom clients or tests using the same mock engine without Spring APIs.

## Outbound ports

### `MockDefinitionSource`

`MockDefinitionSource` loads configured mock definitions from an adapter source,
for example Spring Boot properties, a database, or a static test map.

```java
public interface MockDefinitionSource {
    Map<String, MockDefinition> loadDefinitions();
}
```

### `MockResponseSource`

`MockResponseSource` loads the actual mock response for a definition and
request. A Spring adapter may read JSON from `classpath:` resources, while a
test adapter may return an in-memory response.

```java
public interface MockResponseSource {
    MockResponse load(MockDefinition definition, MockRequest request);
}
```

## Core services

### `DefaultMockCatalog`

`DefaultMockCatalog` loads definitions from `MockDefinitionSource`, normalizes
keys, exposes immutable collections, and fails with `MockException` when a
required key is missing.

```java
MockCatalog catalog = new DefaultMockCatalog(definitionSource);
```

### `DefaultMockResponder`

`DefaultMockResponder` implements the neutral `MockResponder` use case.

Behavior:

- returns `Optional.empty()` when the request has no key;
- returns `Optional.empty()` when the key is missing;
- returns `Optional.empty()` when the definition exists but is disabled;
- throws `MockException` when a mock is enabled but has no response file;
- applies the definition delay before loading the response;
- delegates response loading to `MockResponseSource`.

```java
MockResponder responder = new DefaultMockResponder(catalog, responseSource);
Optional<MockResponse> response = responder.respond(new MockRequest("payments-success"));
```

## Hexagonal boundary

The `check` task runs `verifyHexagonalBoundaries` and rejects imports from
framework or adapter APIs.

```bash
./gradlew :spring-boot-service-framework-mock-core:check
```
