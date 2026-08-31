# OpenAPI Examples

The repository contains three executable contracts. Together they exercise
generation, publication, compatibility, consumer boundaries, mock resources,
and project scaffolding without treating example code as a second reference.

Use [OpenAPI Getting Started](getting-started.md) for the smallest adoption
workflow. Use this guide to understand and verify the repository fixtures.

## Example Matrix

| Contract | Version | Distinct purpose | Primary evidence |
|---|---:|---|---|
| [Merchant Order Status](merchant-order-status.yaml) | `1.1.0` | Compatible evolution from a committed `1.0.0` baseline. | OpenAPI Diff reports the optional `updatedAt` addition as backward compatible. |
| [Retail Loyalty Rewards](retail-loyalty-rewards.yaml) | `1.0.0` | Multiple operations and schemas in one generated API boundary. | Generated models, server API, and client JARs contain both reward operations. |
| [Warehouse Inventory Catalog](warehouse-inventory-catalog.yaml) | `1.0.0` | First adoption, local publication, and one-time hexagonal scaffolding. | Getting-started artifacts and a generated Spring Boot project that passes ArchUnit. |

Each current contract has an exact immutable baseline under
`docs/openapi-baselines`. Merchant Order Status additionally has an earlier
`1.0.0` baseline, allowing the compatibility task to produce a structural diff.

## Run All Examples

From the repository root, validate configuration and documents, generate every
artifact, and run compatibility:

```bash
./gradlew \
  smbtechOpenApiBuildLogicCheck \
  smbtechOpenApiValidateSpecs \
  smbtechOpenApiAssemble \
  smbtechOpenApiCompatibilityCheck
```

A successful run produces nine binary JARs and nine source JARs under
`build/libs/smbtech-openapi`: three artifact kinds for each contract.

The aggregate compatibility result is successful only when baseline policy,
archive reproducibility, artifact separation, consumer-visible contracts, and
mock resources all pass.

## Merchant Order Status

This fixture demonstrates a compatible minor release:

- `docs/openapi-baselines/merchant-order-status/1.0.0.yaml` is the previous
  published contract;
- `docs/openapi/merchant-order-status.yaml` is version `1.1.0`;
- `docs/openapi-baselines/merchant-order-status/1.1.0.yaml` is the immutable
  exact baseline for the current version;
- `getOrderStatus` remains the stable operation identity;
- the current response adds optional date-time property `updatedAt`.

Run the focused structural check:

```bash
./gradlew smbtechOpenApiBreakingChangeCheck
```

Inspect `build/reports/smbtech-openapi/diff/merchant-order-status.md`. It should
identify the added property and report that the API remains backward
compatible. `summary.txt` should contain `OpenAPI compatibility checks passed`.

This example is the starting point when changing baseline selection or SemVer
enforcement. The complete policy belongs to
[OpenAPI Contract Versioning](versioning.md).

## Retail Loyalty Rewards

This fixture demonstrates one contract containing multiple operations:

| Operation ID | Path | Response model |
|---|---|---|
| `getMemberRewardsSummary` | `GET /members/{memberId}/summary` | `RewardsSummaryResponse` |
| `getVoucher` | `GET /members/{memberId}/vouchers/{voucherId}` | `VoucherResponse` |

It is used to verify that all operations share one generated API boundary while
models remain in the separate models artifact. Inspect the generated source
trees after assembly:

```bash
find build/generated/smbtech-openapi/retailLoyaltyRewards \
  -type f -name '*.java' | sort
```

Because this is the first recorded version, its diff report says
`No earlier baseline was found.` That message is informational when the exact
`1.0.0` baseline exists and the build succeeds.

The copy-ready artifact procedure uses this fixture in
[Generate OpenAPI Contract Artifacts](../guides/openapi-generated-artifacts.md).

## Warehouse Inventory Catalog

This fixture contains path parameters `warehouseId` and `sku` and operation
`getWarehouseInventoryItem`. It drives:

- the end-to-end [OpenAPI Getting Started](getting-started.md);
- generation of models, server API, and client artifacts;
- local Maven publication;
- generation of a Spring Boot service from the OpenAPI document;
- compilation of the generated delegate adapter;
- Spring context and hexagonal architecture tests.

Run the scaffold consumer check:

```bash
./gradlew \
  :spring-boot-service-framework-project-generator:scaffoldingCompatibilityCheck
```

The generated fixture is created below
`spring-boot-service-framework-project-generator/build/scaffolding/generated`.
It is disposable build output. Do not use force generation against an existing
application repository.

Scaffolding inputs, defaults, output structure, and safety rules are defined in
[OpenAPI Project Scaffolding](scaffolding.md).

## Inspect Generated Artifacts

List the generated modules:

```bash
find build/libs/smbtech-openapi -maxdepth 1 -type f -name '*.jar' | sort
```

Inspect one binary artifact without extracting it:

```bash
jar --list \
  --file build/libs/smbtech-openapi/retail-loyalty-rewards-client-1.0.0.jar
```

Use the generated evidence for higher-level checks:

| Evidence | What to confirm |
|---|---|
| `build/reports/smbtech-openapi/diff/summary.txt` | Overall baseline and SemVer result. |
| `build/reports/smbtech-openapi/reproducibility.sha256` | One stable hash entry per binary and source JAR. |
| `build/reports/smbtech-openapi/consumer-test.txt` | `verified=9` and `status=compatible`. |
| `build/reports/smbtech-openapi/mock-contracts.properties` | One collision-free classpath contract location per example. |
| `build/reports/smbtech-openapi/migration.md` | Mapping from retired coordinates and tasks. |

Artifact contents and separation rules are defined in
[OpenAPI Artifact Generation](generation.md). Report semantics and child tasks
are defined in [OpenAPI Validation](validation.md).

## Add An Example

Add a repository fixture only when it proves behavior not already covered:

1. create a focused OpenAPI document with a unique title and operation IDs;
2. register it in the root `smbtechOpenApi.specs` container;
3. commit an exact baseline for its current version;
4. state its distinct purpose in this matrix;
5. update compatibility expectations that count generated artifacts;
6. run the complete OpenAPI and documentation gates;
7. inspect generated JARs and reports instead of committing build output.

Do not add literal credentials, environment endpoints, generated sources, JARs,
or local Maven repositories to an example.

## Validation

Validate examples and their documentation with:

```bash
./gradlew smbtechOpenApiCompatibilityCheck
./gradlew validateOpenApiExamplesAndTroubleshooting
./gradlew documentationCheck
```

For failures, continue with
[OpenAPI Troubleshooting](troubleshooting.md).
