# Run An OpenAPI Contract Pilot

First run a staging-only pilot from the current `main` revision:

```bash
gh workflow run "OpenAPI Contract Pilot" \
  --repo Gazu/spring-boot-service-framework \
  --ref main \
  -f contract=docs/openapi/warehouse-inventory-catalog.yaml \
  -f expected_version=1.0.0 \
  -f publish=false
```

Review the `openapi-contract-pilot-*` artifact. Its manifest must identify the
requested contract, the expected source commit, three coordinates at the same
version, and a successful local verification report.

After that evidence is accepted, repeat with `publish=true`. GitHub pauses the
deployment at the protected `release` environment. After approval, the workflow
checks whether the coordinates are absent or already identical, publishes only
when absent, and verifies the remote files and clean Maven resolution.

The run is successful only when the deployment receipt reports `verified` for
all three artifacts. A failure produces recovery evidence; follow the [immutable
coordinate recovery procedure](rollback-procedure.md) before retrying.
