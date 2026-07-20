# OpenAPI Breaking Change Detection

The repository compares each current OpenAPI contract with its latest earlier
baseline before generated artifacts are considered compatible. The check is
structural and runs without starting a Spring application.

## Command

Run the normal compatibility policy:

```bash
./gradlew openApiBreakingChangeCheck
```

Run strict CI mode, which rejects every breaking change even when the major
version was increased correctly:

```bash
./gradlew openApiBreakingChangeCheck -PopenApiFailOnBreakingChanges=true
```

Normal mode accepts intentional breaking changes only when `info.version` has
a valid major increase. Strict mode is useful for branches that must remain
compatible with their current consumers.

## Baseline Layout

Baselines are immutable snapshots stored by normalized `info.title` and
`info.version`:

```text
docs/openapi-baselines/
  merchant-order-status/
    1.0.0.yaml
    1.1.0.yaml
  retail-loyalty-rewards/
    1.0.0.yaml
```

Every current contract must have an exact same-version snapshot whose content
matches byte for byte. This guarantees that the current released shape is ready
to become the comparison baseline for the next version.

The comparison ignores the same-version snapshot and selects the highest lower
SemVer from the contract directory. A new `1.2.0` contract therefore compares
with `1.1.0`, while `1.1.0` currently compares with `1.0.0`.

## Change Classification

| Area | Breaking examples | Non-breaking examples |
|---|---|---|
| Operations | Remove method/path; change `operationId`. | Add method/path. |
| Parameters | Remove a parameter; add a required parameter; make one required; change its schema. | Add an optional parameter; make one optional. |
| Request bodies | Remove the body; add a required body; make it required; remove a media type. | Add an optional body or media type. |
| Responses | Remove a status or media type; change its response schema. | Add a status or media type. |
| Schemas | Remove a schema or property; add a required property; change type, format, `$ref`, or composition. | Add a schema or optional property. |
| Enums | Remove an allowed value. | Add an allowed value. |
| Constraints | Increase minimums, reduce maximums, add/change patterns, or stop accepting null. | Relax limits, remove patterns, or allow null. |

Descriptions, summaries, tags, examples, and other documentation-only changes
are ignored.

## Version Policy

- Breaking changes require a higher `MAJOR` version.
- Compatible API additions require a higher `MINOR` or `MAJOR` version.
- A `PATCH` increase is suitable when the compared API structure is unchanged.
- The current version must always be greater than the selected baseline.
- Build metadata does not affect SemVer precedence.

| Baseline | Current | Changes | Result |
|---:|---:|---|---|
| `1.1.0` | `1.2.0` | Optional response field added | Pass |
| `1.1.0` | `1.1.1` | Optional response field added | Fail; minor increase required |
| `1.1.0` | `1.2.0` | Required field added | Fail; major increase required |
| `1.1.0` | `2.0.0` | Required field added | Pass normal mode; fail strict mode |

## Updating A Contract

1. Confirm that the committed spec has an identical snapshot under
   `docs/openapi-baselines/<artifact-base-name>/<current-version>.yaml`.
2. Change the OpenAPI document and increase `info.version` according to the
   expected compatibility level.
3. Add an exact snapshot of the new version under the same baseline directory.
4. Run `./gradlew generateOpenApiSpecVersionCatalog`.
5. Run `./gradlew openApiBreakingChangeCheck` and review every reported change.
6. Run `./gradlew openApiCompatibilityCheck` before committing.

Baseline snapshots must not be edited after publication. Add a new versioned
file instead.

## Example Output

```text
OpenAPI compatibility merchant-order-status: 1.0.0 -> 1.1.0
  NON_BREAKING [PROPERTY_ADDED] components.schemas.OrderStatusResponse.properties.updatedAt: optional property was added
```

```text
OpenAPI compatibility orders-api: 1.2.0 -> 1.3.0
  BREAKING [RESPONSE_REMOVED] GET /orders/{orderId}.responses.200: response status was removed
  BREAKING [TYPE_CHANGED] components.schemas.Order.properties.id.type: type changed from string to integer
OpenAPI breaking change issues found:
- orders-api: breaking changes require a major version increase from 1.2.0 to at least 2.0.0
```

## Integration

`openApiBreakingChangeCheck` is a public compatibility task and runs as part of:

```bash
./gradlew documentationCheck
./gradlew openApiCompatibilityCheck
./gradlew compatibilityCheck
```

The reusable implementation lives in
`spring-boot-service-framework-openapi-generator`. See
[OpenAPI Code Generation](openapi-codegen.md),
[Compatibility](compatibility.md), and
[Troubleshooting](troubleshooting.md) for related validation behavior.

## Current Limits

- Local component `$ref` changes and component schema contents are checked;
  external `$ref` files are not resolved yet.
- `oneOf`, `anyOf`, `allOf`, `not`, and `discriminator` changes are treated
  conservatively as breaking when their structure differs.
- The detector does not infer whether one schema is used exclusively for input
  or output, so property removal is conservatively breaking.
- Security schemes and response headers are not compared yet.
