# Check OpenAPI Breaking Changes

Use this workflow whenever a committed OpenAPI contract changes.

## 1. Preserve The Current Version

Before editing `docs/openapi/orders-api.yaml`, confirm that its current content
is copied exactly to:

```text
docs/openapi-baselines/orders-api/1.4.0.yaml
```

## 2. Update The Contract Version

Use a minor increase for compatible additions:

```yaml
info:
  title: orders-api
  version: '1.5.0'
```

Use a major increase for incompatible changes:

```yaml
info:
  title: orders-api
  version: '2.0.0'
```

Add an exact snapshot for the new version after finishing the edit.

## 3. Run Detection

```bash
./gradlew openApiBreakingChangeCheck
```

Review every `BREAKING` and `NON_BREAKING` line. To enforce that a branch has no
breaking changes at all, run:

```bash
./gradlew openApiBreakingChangeCheck -PopenApiFailOnBreakingChanges=true
```

## 4. Refresh Version Metadata

```bash
./gradlew generateOpenApiSpecVersionCatalog
./gradlew openApiCompatibilityCheck
```

Commit the current spec, its new baseline snapshot, and the updated version
catalog together. The full rule set is documented in
[OpenAPI Breaking Change Detection](../openapi-breaking-changes.md).
