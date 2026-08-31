# Error Handling Consumer Example

Standalone Spring Boot application that imports
`com.smbtech:spring-boot-service-framework-platform:0.5.0` and consumes
`com.smbtech:spring-boot-service-framework-starter-error-handling` without an
individual version from the framework module-local Maven repositories.

The example uses published artifacts instead of Gradle `project(...)`
dependencies and demonstrates the complete public error response flow.
See [Dependency Management](../../docs/dependency-management.md) for the
platform contract and publication options.

## Covered Scenarios

| Scenario | Endpoint | Expected status |
|---|---|---|
| Application error catalog | `GET /api/orders/missing` | `404` |
| Multiple Bean Validation failures | `POST /api/orders` with `{}` | `400` |
| Downstream HTTP failure | `GET /api/simulations/downstream` | `502` |
| Unexpected exception | `GET /api/simulations/unexpected` | `500` |
| Missing authentication | `GET /api/secure/profile` | `401` |
| Missing `ADMIN` role | `GET /api/secure/admin` | `403` |

All error endpoints return the same snake-case `Notification` JSON contract.
Internal diagnostics, stack traces, downstream headers, bodies, cookies, and
credentials are not included in responses.

The example sets `smbtech.error-handling.response.exposure=INTERNAL` explicitly
because it demonstrates application catalog codes, messages, and validation
violations. The starter default is `PUBLIC`; omitting the property preserves the
error code but returns a generic message and minimal metadata.

## Application Catalog

`OrderErrors` implements `ErrorDefinition` and keeps stable public codes,
categories, messages, and severities in application code:

```java
public enum OrderErrors implements ErrorDefinition {
    ORDER_NOT_FOUND(
            "E_ORDER_0001",
            ErrorCategory.NOT_FOUND,
            "The requested order does not exist",
            NotificationSeverity.ERROR
    );
}
```

`OrderService` throws the catalog entry while retaining a separate diagnostic:

```java
throw ServiceException.from(
        OrderErrors.ORDER_NOT_FOUND,
        "Order lookup failed for internal id " + orderId
);
```

The diagnostic remains internal. Because this example explicitly uses
`INTERNAL`, the HTTP response contains the catalog values and detailed safe
metadata after sanitization.

## Multiple Validation Errors

`CreateOrderRequest` validates `customerId`, `amount`, and `items`. Sending an
empty object produces three entries under `metadata.violations`:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{}'
```

Each violation uses `field_name`, `code`, and `message`. The framework does not
change the application's global `ObjectMapper`.

## Downstream And Unexpected Errors

`PaymentsGateway` raises an `HttpClientResponseException` containing simulated
downstream URI parameters, authorization headers, cookies, response body, and
cause details. The detailed sanitized response is reduced to:

```json
{
  "code": "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503",
  "message": "Downstream service request failed",
  "severity": "ERROR",
  "field_name": "",
  "metadata": {
    "schema_version": "1",
    "category": "DOWNSTREAM"
  }
}
```

The generated `id` and `timestamp` fields are omitted from the snippet for
brevity.

Unexpected exceptions use the generic `E_SERVICE_FRAMEWORK_INTERNAL_0001`
notification and never expose the exception message or stack trace.

## Security

`SecurityConfig` connects the auto-configured `AuthenticationEntryPoint` and
`AccessDeniedHandler` to Spring Security. Both return the same notification
contract used by MVC errors.

The default local user is configured through environment-backed placeholders:

| Variable | Default |
|---|---|
| `EXAMPLE_SECURITY_USERNAME` | `example-user` |
| `EXAMPLE_SECURITY_PASSWORD` | `change-me` |
| `EXAMPLE_SECURITY_ROLES` | `USER` |

Set `EXAMPLE_SECURITY_ROLES=ADMIN` to call the admin endpoint manually.

## Run

From the framework root, publish local artifacts and start the application:

```bash
./gradlew publishLocalArtifacts
cd examples/error-handling-consumer
../../gradlew bootRun
```

## Tests

The smoke tests verify the application catalog, all three validation
violations, downstream sanitization, unexpected exception fallback, and Spring
Security responses for unauthenticated and forbidden requests.

Run them from the framework root:

```bash
./gradlew errorHandlingConsumerSmoke
```
