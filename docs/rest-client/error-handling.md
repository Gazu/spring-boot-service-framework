# Error Handling and Full Response Bodies

When standard error handling is enabled, downstream HTTP error responses are
mapped into `HttpClientResponseException`. The exception carries:

- status code and reason phrase;
- response headers when `include-headers=true`;
- content type and charset;
- the complete response body when `include-body=true`;
- a structured `Notification` from `spring-boot-service-framework-commons`.

This type represents a downstream HTTP response. Authentication and token
failures use `HttpClientAuthenticationException`, while invalid static client
definitions use `HttpClientConfigurationException`. See
[Exception Selection](../error-handling/exception-selection.md).

The body available from `exception.getErrorResponseAsString()`,
`exception.responseBody()`, and `exception.error().body()` is not truncated by
audit settings. Audit logs can be truncated independently with
`audit.max-body-size`.

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        error-handling:
          enabled: true
          include-body: true
          include-headers: true
          include-notification-metadata: true
          notification-code-prefix: E_SERVICE_FRAMEWORK_HTTP_CLIENT_
        audit:
          enabled: true
          include-body: true
          max-body-size: 4096
```

For clients created by the starter, JSON error body decoding is attached to the
exception automatically:

```java
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;

try {
    paymentsApi.createOrder(request);
} catch (HttpClientResponseException exception) {
    DownstreamError error = exception.getJsonErrorResponseAsObject(DownstreamError.class);
    String completeBody = exception.getErrorResponseAsString();
}
```

`responseBody()` remains available for compatibility. For optional decoding,
inject `HttpErrorBodyDecoder` and use `decodeIfPresent(...)`; it returns
`Optional.empty()` when the exception does not carry a body:

```java
Optional<DownstreamError> error =
        errorBodyDecoder.decodeIfPresent(exception, DownstreamError.class);
```

Decode failures raise `HttpErrorBodyDecodingException`. If an exception is
created manually outside the starter and no reader is attached,
`getJsonErrorResponseAsObject(...)` raises
`HttpErrorResponseBodyReaderNotConfiguredException`.

---
