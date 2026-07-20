# Error Handling JSON Contract

All framework-managed MVC and Spring Security errors return one flat
`Notification` object serialized with snake-case field names.

## Fields

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `code` | string | yes | Stable machine-readable notification code. |
| `message` | string | yes | Public message safe for consumers. |
| `severity` | string | yes | `ERROR`, `WARNING`, or `INFO`. |
| `field_name` | string | yes | Related field or an empty string. |
| `metadata` | object | yes | Sanitized allowlisted context. |
| `id` | UUID string | yes | Unique notification instance id. |
| `timestamp` | ISO-8601 instant | yes | Notification creation time in UTC. |

The response is directly the `Notification`; there is no `error`, `errors`, or
`notifications` wrapper.

## Validation Example

```json
{
  "code": "E_SERVICE_FRAMEWORK_VALIDATION_0001",
  "message": "Request validation failed",
  "severity": "ERROR",
  "field_name": "",
  "metadata": {
    "violations": [
      {
        "field_name": "customerId",
        "code": "NotBlank",
        "message": "must not be blank"
      }
    ]
  },
  "id": "de305d54-75b4-431b-adb2-eb6b9e546014",
  "timestamp": "2026-07-19T12:00:00Z"
}
```

Metadata keys are normalized recursively across nested maps, collections, and
arrays. A collision after normalization, such as `customerId` and
`customer_id` in the same map, is rejected instead of silently dropping data.

## Internal Error Example

```json
{
  "code": "E_SERVICE_FRAMEWORK_INTERNAL_0001",
  "message": "The request could not be completed",
  "severity": "ERROR",
  "field_name": "",
  "metadata": {},
  "id": "de305d54-75b4-431b-adb2-eb6b9e546014",
  "timestamp": "2026-07-19T12:00:00Z"
}
```

An error with `ErrorExposure.INTERNAL` is replaced with this generic code and
message while preserving its generated `id`, `timestamp`, and safe allowlisted
metadata. Diagnostic messages, causes, and stack traces are never serialized.

## Compatibility Rules

- Field names remain snake case.
- Existing required fields are not removed or renamed in a compatible release.
- Enum values and code semantics are case-sensitive.
- Consumers must treat unknown metadata keys as additive.
- Applications replacing `NotificationSerializer` own compatibility of their
  custom response contract.

The default serializer is scoped to notification responses and does not modify
the application's global `ObjectMapper`.
