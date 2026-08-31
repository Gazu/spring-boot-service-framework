# OpenAPI Contract Versioning

This is the canonical reference for OpenAPI contract identity, semantic
versioning, immutable baselines, structural comparison, and breaking-change
enforcement. Use the
[Check OpenAPI Breaking Changes](../guides/check-openapi-breaking-changes.md)
procedure for the shortest contract-update workflow.

## Version Source

Every contract must define a version in its OpenAPI metadata:

```yaml
openapi: 3.1.0
info:
  title: warehouse-inventory-catalog
  version: '1.2.0'
```

`info.version` is the canonical contract version used by baseline selection and
`smbtechOpenApiBreakingChangeCheck`. By default, it is also the Maven version of
the generated models, server API, and client artifacts.

The plugin accepts versions matching:

```text
MAJOR.MINOR.PATCH
MAJOR.MINOR.PATCH-PRERELEASE
MAJOR.MINOR.PATCH+BUILD
```

The current validation pattern is
`[0-9]+\.[0-9]+\.[0-9]+(?:[-+][0-9A-Za-z][0-9A-Za-z.-]*)?`.

## Effective Artifact Version

`smbtechOpenApi.specs.<name>.version` can override the Maven artifact version.
This override does not change the version read by the breaking-change task,
which always uses `info.version` from the OpenAPI document.

For version-governed contracts, leave the override unset or keep it exactly
equal to `info.version`:

```groovy
smbtechOpenApi {
    specs {
        register('warehouseInventoryCatalog') {
            input.set(file('src/main/openapi/warehouse-inventory-catalog.yaml'))
            version.set('1.2.0')
        }
    }
}
```

A divergent override can publish coordinates that do not match the baseline
version evaluated by CI. The plugin currently does not reject that divergence.

## Contract Identity

Baseline identity comes from normalized `info.title`, not the Gradle
registration name or an `artifactBaseName` override. Normalization:

- converts the title to lowercase;
- converts whitespace, underscores, and dots to hyphens;
- removes unsupported characters;
- collapses repeated hyphens; and
- trims leading and trailing hyphens.

For example, `Warehouse Inventory_Catalog` becomes
`warehouse-inventory-catalog`.

Keep `info.title` stable after the first publication. Changing it creates a new
contract identity and therefore a new baseline history. As with version
overrides, an `artifactBaseName` override affects generated coordinates but not
baseline identity.

## Baseline Layout

`smbtechOpenApi.baselineDirectory` controls snapshot storage. Its default is:

```text
src/main/openapi-baselines
```

It can be changed for repository-wide contract catalogs:

```groovy
smbtechOpenApi {
    baselineDirectory.set(layout.projectDirectory.dir('docs/openapi-baselines'))
}
```

Store immutable snapshots below the normalized `info.title`:

```text
docs/openapi-baselines/
  warehouse-inventory-catalog/
    1.0.0.yaml
    1.1.0.yaml
    1.2.0.yaml
```

Baseline documents may use `.yaml`, `.yml`, or `.json`. The conventional file
name is `<info.version>.<extension>`, while identity is verified from the
document's own `info.title` and `info.version`.

## Exact Baseline

The exact baseline represents the immutable snapshot of the current contract
version.

```groovy
smbtechOpenApi {
    requireBaseline.set(true)
}
```

When `smbtechOpenApi.requireBaseline` is `true`, every configured contract must
have an exact baseline. When it is `false`, a missing exact baseline is allowed
for a first or unreleased version.

If an exact baseline exists, its SHA-256 must match the current OpenAPI file
regardless of `requireBaseline`. Never edit a contract after committing or
publishing its exact baseline. Increment the version and add another snapshot.

## Previous Baseline Selection

For structural comparison, the plugin:

1. Opens the directory for the normalized current `info.title`.
2. Parses valid OpenAPI baseline documents.
3. Keeps versions numerically lower than the current version.
4. Selects the highest remaining `MAJOR.MINOR.PATCH` version.

A current `1.2.0` contract compares with `1.1.0` when both `1.0.0` and `1.1.0`
exist. Its exact `1.2.0` snapshot is checked for immutability but is never used
as the previous comparison baseline.

When no earlier baseline exists, the task writes a report stating that no
earlier baseline was found and performs no structural version check.

## Semantic Version Policy

The framework applies these rules to the difference reported by the pinned
OpenAPI Diff engine:

| Structural result | Required version change |
|---|---|
| No API structure change | Any higher version is accepted; `PATCH` is the recommended minimum. |
| Backward-compatible API change | Increase `MINOR` or `MAJOR`. |
| Breaking API change | Increase `MAJOR`. |

Examples:

| Previous | Current | Difference | Result |
|---:|---:|---|---|
| `1.1.0` | `1.1.1` | Documentation only | Pass |
| `1.1.0` | `1.1.1` | Optional response field added | Fail; compatible additions require minor or major. |
| `1.1.0` | `1.2.0` | Optional response field added | Pass. |
| `1.1.0` | `1.2.0` | Required request field added | Fail; breaking changes require major. |
| `1.1.0` | `2.0.0` | Required request field added | Pass in normal mode. |

There is no relaxed pre-`1.0.0` rule. A breaking change from `0.4.0` requires a
higher major component, such as `1.0.0`.

Pre-release and build suffixes are accepted in `info.version`, but baseline
ordering and policy compare only numeric `MAJOR.MINOR.PATCH`. Do not rely on a
suffix to represent a compatibility increase.

## Change Classification

OpenAPI Diff performs structural classification. Typical results include:

| Area | Usually breaking | Usually compatible |
|---|---|---|
| Operations | Remove a method or path; change an `operationId`. | Add a method or path. |
| Parameters | Add a required parameter; make one required; change its schema. | Add an optional parameter; make one optional. |
| Request bodies | Remove a body or media type; add or make a body required. | Add an optional body or media type. |
| Responses | Remove a status or media type; incompatibly change a schema. | Add a status or media type. |
| Schemas | Remove a schema/property; add a required property; change type, format, `$ref`, or composition. | Add a schema or optional property. |
| Enums | Remove an allowed value. | Add an allowed value. |
| Constraints | Tighten limits, patterns, or nullability. | Relax limits or allow null. |

Descriptions, summaries, examples, and other documentation-only changes do not
normally alter the API structure.

## Strict Mode

`smbtechOpenApi.failOnBreakingChanges` controls strict mode. Strict mode rejects
every breaking difference, even when `info.version` has a valid major increase:

```groovy
smbtechOpenApi {
    failOnBreakingChanges.set(true)
}
```

This is useful for a maintenance branch that must remain compatible with its
existing consumers.

The framework repository maps a project property to this DSL flag:

```groovy
failOnBreakingChanges.set(
        providers.gradleProperty('openApiFailOnBreakingChanges')
                .map { it.toBoolean() }
                .orElse(false)
)
```

That repository-specific mapping enables:

```bash
./gradlew smbtechOpenApiBreakingChangeCheck -PopenApiFailOnBreakingChanges=true
```

Consumer builds must add the mapping themselves before using that command-line
property. The plugin does not read `openApiFailOnBreakingChanges` implicitly.

## Contract Update Workflow

1. Preserve the currently published contract as its exact immutable baseline.
2. Modify the OpenAPI document.
3. Increment `info.version` according to the expected compatibility level.
4. Add an exact snapshot for the new version.
5. Run `./gradlew smbtechOpenApiBreakingChangeCheck`.
6. Review the generated Markdown diff.
7. Run `./gradlew smbtechOpenApiCompatibilityCheck`.
8. Publish the new immutable coordinates.

Commit the current contract and its baseline together. Do not overwrite an
existing remote Maven version; follow
[OpenAPI Artifact Publishing](publishing.md).

## Reports

The task writes deterministic evidence under:

```text
build/reports/smbtech-openapi/diff/
  <normalized-title>.md
  summary.txt
```

A compatible OpenAPI Diff report resembles:

```text
### merchant-order-status (v 1.1.0)

#### What's Changed

* Added property `updatedAt` (string)

#### Result

API changes are backward compatible
```

`summary.txt` contains `OpenAPI compatibility checks passed` when no policy
failure occurs. Gradle failures identify missing or modified exact baselines,
strict-mode rejection, or the required SemVer level.

## Failure Modes

| Symptom | Cause | Resolution |
|---|---|---|
| `missing exact baseline for version` | `requireBaseline` is enabled and the current snapshot is absent. | Add an exact baseline under the normalized title directory. |
| `current contract differs from its immutable ... baseline` | A same-version contract was edited. | Restore it or increment `info.version` and add a new baseline. |
| `compatible additions require a minor or major version increase` | A structural addition used only a patch increase. | Increase `MINOR` or `MAJOR`. |
| `breaking changes require a major version increase` | An incompatible change kept the same major component. | Increase `MAJOR` or restore compatibility. |
| `strict mode rejects breaking changes` | Strict mode found an incompatible difference. | Restore compatibility or disable strict mode only where policy permits. |
| `No earlier baseline was found` | The contract has no lower baseline version. | Expected for the first version; add history before later evolution. |

## Current Limits

- External `$ref` documents are not a supported compatibility boundary.
- Composition changes involving `oneOf`, `anyOf`, `allOf`, `not`, or a
  discriminator are treated conservatively when structure differs.
- The detector does not infer whether a schema is used exclusively for input or
  output, so some removals are conservatively breaking.
- Security schemes and response headers are not currently part of the protected
  comparison surface.
- Gradle `version` and `artifactBaseName` overrides are not used for baseline
  identity or SemVer comparison.

## Validation

Run the versioning and documentation gates with:

```bash
./gradlew validateOpenApiVersioningDocumentation
./gradlew smbtechOpenApiBreakingChangeCheck
./gradlew smbtechOpenApiCompatibilityCheck
./gradlew documentationCheck
```

Return to the [OpenAPI Portal](index.md) for generation, publication, testing,
mocks, and scaffolding.
