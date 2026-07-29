# Observability and Audit

Observability uses Micrometer when a `MeterRegistry` is available.

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        observability:
          enabled: true
          metric-name: smbtech.http.client.requests
          include-uri: false
          include-status: true
          include-exception: true
          tags:
            system: payments
            layer: integration
        audit:
          enabled: true
          include-request: true
          include-response: true
          include-headers: false
          include-body: false
          max-body-size: 4096
```

Timer metric tags:

- `client`
- `method`
- `outcome`
- `status` when `include-status=true`
- `exception` when `include-exception=true`
- `uri` when `include-uri=true`
- custom `observability.tags`

The starter also increments `<metric-name>.errors` for exceptions and status
codes greater than or equal to 400.

Audit events are emitted through `HttpExchangeAuditSink`. The default starter
sink logs request, response, and failure events through SLF4J. Headers and
bodies are disabled by default because they can contain secrets or personal
data.

Before any sink receives an event, the starter always redacts authentication
headers, cookies, API keys, token-like query parameters, known secret fields in
bodies, and authorization values embedded in exception messages. Raw
throwables are not forwarded to audit sinks. This mandatory protection cannot
be disabled, but it does not replace data classification: bodies may still
contain business or personal data and should remain disabled unless required.

---

## Optional Resilience

Resilience is disabled by default and implemented without an external
Resilience4j dependency.

```yaml
smbtech:
  rest-clients:
    clients:
      payments:
        base-url: https://payments.example
        resilience:
          enabled: true
          retry:
            enabled: true
            max-attempts: 3
            backoff: 100ms
            retry-on-server-errors: true
            retry-on-exceptions: true
            retry-on-statuses: [429]
          circuit-breaker:
            enabled: true
            failure-threshold: 3
            open-duration: 30s
```

`retry` applies to transport exceptions, `5xx` responses when
`retry-on-server-errors=true`, and explicit status codes listed in
`retry-on-statuses`.

The circuit breaker opens after consecutive failures. While it is open, calls
fail fast with a message like:

```text
Circuit breaker is open for HTTP client: payments
```

---
