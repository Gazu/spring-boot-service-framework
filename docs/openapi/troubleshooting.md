# OpenAPI Troubleshooting

Use this guide for failures in OpenAPI plugin configuration, document parsing,
generation, compatibility, publication, contract testing, or project
scaffolding. Diagnose the first failing task rather than starting with the final
aggregate exception.

## Fast Triage

Run each layer independently and stop at the first failure:

```bash
./gradlew smbtechOpenApiBuildLogicCheck --stacktrace
./gradlew smbtechOpenApiValidateSpecs --stacktrace
./gradlew smbtechOpenApiAssemble --stacktrace
./gradlew smbtechOpenApiCompatibilityCheck --stacktrace
```

The compatibility aggregate has five children. Rerun the named child directly
when Gradle reports one of them:

```bash
./gradlew smbtechOpenApiBreakingChangeCheck --stacktrace
./gradlew smbtechOpenApiReproducibilityCheck --stacktrace
./gradlew smbtechOpenApiConsumerTest --stacktrace
./gradlew smbtechOpenApiMockContractCheck --stacktrace
```

`smbtechOpenApiMigrationReport` normally creates evidence and is useful when a
consumer still uses retired coordinates or task names.

## Evidence Map

| Path | Produced by | Inspect when |
|---|---|---|
| `build/reports/smbtech-openapi/diff/summary.txt` | Breaking-change check | Baseline or SemVer policy fails. |
| `build/reports/smbtech-openapi/diff/<contract>.md` | Breaking-change check | The structural change is unclear. |
| `build/reports/smbtech-openapi/reproducibility.sha256` | Reproducibility check | JAR ordering, timestamps, or hashes change. |
| `build/reports/smbtech-openapi/consumer-test.txt` | Consumer check | Generated JAR content or separation fails. |
| `build/reports/smbtech-openapi/mock-contracts.properties` | Mock check | Embedded contract lookup or numeric responses fail. |
| `build/reports/smbtech-openapi/migration.md` | Migration report | A build still references legacy coordinates or tasks. |
| `build/generated/smbtech-openapi` | Generation tasks | Generated source does not compile or has an unexpected shape. |
| `build/libs/smbtech-openapi` | Assembly | A binary or source JAR is missing. |

Configuration and spec validation report directly to Gradle and do not create
separate files.

## Configuration Failures

| Message or symptom | Meaning | Resolution |
|---|---|---|
| `smbtechOpenApi.groupId must not be blank` | The effective default Maven group is empty. | Set a nonblank group in the DSL. |
| `outputDirectory`, `repositoryDirectory`, or `baselineDirectory must not be blank` | A required directory property resolved to blank text. | Remove the invalid override or set a valid directory provider. |
| `publicationRepositoryUrl must be an absolute URI` | Remote publication uses a relative or scheme-less URL. | Supply an absolute `https:` or `file:` URI. |
| `specs.<name>.input must be configured` | A named spec has no input document. | Set `input` to an existing YAML or JSON file. |
| `must be a valid Java package` | A package override contains uppercase, hyphenated, or invalid segments. | Use lowercase dot-separated Java identifiers. |
| `must enable at least one generated artifact` | Models, server API, and client are all disabled. | Enable at least one artifact kind. |
| `must enable models when server API or client generation is enabled` | Server or client code references models that were disabled. | Enable models or disable the dependent artifact kinds. |

Use the exact property path in the message to distinguish a global default from
a per-contract override. The complete DSL belongs to the
[Gradle Plugin Reference](plugin-reference.md).

## Document And Coordinate Failures

`smbtechOpenApiValidateSpecs` collects all document failures and prefixes each
one with its project-relative path. The aggregate message starts with
`OpenAPI spec validation issues found:`.

| Message fragment | Meaning | Resolution |
|---|---|---|
| `document could not be parsed as OpenAPI` | Swagger Parser could not construct an OpenAPI document. | Validate YAML or JSON syntax and local references. |
| `openapi must declare a supported 3.0.x or 3.1.x version` | The document is Swagger 2 or has an unsupported/missing version. | Declare a supported OpenAPI version. |
| `info.title is required` or `info.version is required` | Contract identity is incomplete. | Add nonblank values under `info`. |
| `paths must contain at least one operation` | The document has no executable operation. | Add a path with an HTTP operation. |
| `operationId is required` | An operation cannot be mapped to a stable generated method. | Add a unique operation ID. |
| `operationId ... must be unique` | Multiple operations produce the same method identity. | Rename operation IDs so each is unique. |
| `artifact base name is invalid` | The title or override cannot form a supported Maven artifact name. | Use lowercase alphanumeric hyphen-separated segments. |
| `artifact version is invalid` | The effective version is not SemVer/Maven compatible. | Use a version such as `1.2.0` or a supported prerelease. |
| `generated coordinate ... duplicates` | Two specs resolve to the same artifact kind and version. | Change title, group, artifact override, version, or enabled kinds. |
| `generated artifact collides with` | A contract artifact would reuse a protected framework artifact ID. | Choose a distinct contract title or artifact override. |

If an unexpected document is validated, check conventional discovery locations
and explicit spec registrations in [OpenAPI Validation](validation.md).

## Generation And Compilation Failures

| Symptom | Likely cause | Resolution |
|---|---|---|
| OpenAPI Generator reports an unsupported schema or mapping | The contract uses a shape outside the pinned generator's supported Spring output. | Reduce the contract to the failing schema, verify the pinned toolchain, and review generated source. |
| Generated Java does not compile | A schema, operation name, package override, or generator upgrade produced invalid or incompatible source. | Run the failing kind-specific generation task, inspect its source tree, and fix the contract or versioned template. |
| Server or client compilation cannot find a model | Models are disabled, package overrides disagree, or stale output remains. | Re-enable models, align packages, then run `clean` and assemble again. |
| A generated client lacks the framework annotation | The client template was not applied or the template bundle changed. | Run the plugin compatibility check and inspect the generated client interface. |
| Expected JAR is absent | Its artifact kind is disabled or assembly stopped earlier. | Check global and per-spec publish flags, then rerun assembly. |

Never patch generated source below `build/`. Contract-wide customization belongs
in the versioned template module.

## Baseline And SemVer Failures

| Message | Meaning | Resolution |
|---|---|---|
| `missing exact baseline for version` | Strict baseline mode cannot find the current immutable snapshot. | Add `<normalized-title>/<current-version>.yaml` below the configured baseline directory. |
| `current contract differs from its immutable ... baseline` | The document changed without changing its version. | Restore the contract or increment the version and add a byte-identical current baseline. |
| `compatible additions require a minor or major version increase` | A patch release added visible contract structure. | Increase minor or major according to policy. |
| `breaking changes require a major version increase` | An incompatible change did not increase major. | Restore compatibility or publish a major version. |
| `strict mode rejects breaking changes` | `failOnBreakingChanges` rejects incompatible changes even with a major bump. | Restore compatibility or disable strict mode only where organizational policy permits. |

`No earlier baseline was found.` is informational for a first recorded version.
It is not a failure when the exact current baseline exists and the summary says
the compatibility checks passed.

Inspect the contract-specific Markdown diff before changing the version. See
[OpenAPI Contract Versioning](versioning.md) for the complete policy.

## Reproducibility Failures

| Message fragment | Meaning | Resolution |
|---|---|---|
| `contains duplicate entries` | A JAR contains the same archive path more than once. | Fix task inputs or packaging so every entry is unique. |
| `has inconsistent timestamp` | Archive entries were not normalized consistently. | Check custom packaging and preserve reproducible archive settings. |
| `cannot inspect JAR` | The artifact is missing, corrupt, locked, or unreadable. | Run a clean assembly and verify filesystem access. |

Do not solve a hash change by editing the manifest. Compare generated sources,
metadata, templates, and dependency/toolchain versions first.

## Consumer And Mock Failures

| Message fragment | Meaning | Resolution |
|---|---|---|
| `generated JAR is missing` | An expected enabled artifact was not assembled. | Check artifact flags and rerun assembly. |
| `missing ... contract.properties` | Required embedded metadata was not packaged. | Check metadata task wiring and JAR inputs. |
| `missing unique embedded contract` | The collision-free contract resource is absent. | Restore metadata resources and regenerate the artifact. |
| `duplicates model classes` | Server API or client JAR incorrectly contains model bytecode. | Restore artifact separation in generation wiring. |
| `no Spring delegate interface found` | Server generation did not produce an `ApiDelegate`. | Verify operations and server generator options. |
| `generated HTTP interface lacks` | Client generation omitted the framework annotation. | Verify the client template and generated interface. |
| `mock server requires at least one numeric response` | An operation declares no concrete HTTP status. | Add at least one numeric response such as `200` or `404`. |
| `models JAR is missing for mock usage` | Mock adoption cannot locate the contract-bearing models JAR. | Enable and assemble models. |
| `missing mock resource` | The versioned embedded contract is absent from models. | Regenerate metadata and models JAR. |

Inspect `consumer-test.txt` for `status=failed` and
`mock-contracts.properties` for the classpath locations produced before the
failure.

## Publication Failures

| Symptom or message | Meaning | Resolution |
|---|---|---|
| `publicationRepositoryUrl is required for remote publication` | Remote publication was invoked without a target repository. | Configure the remote repository URI through a trusted provider. |
| HTTP `401` or `403` from the Maven registry | Credentials are absent, invalid, or unauthorized. | Verify the configured Gradle property or environment source without printing the secret. |
| `409 Conflict` or immutable-version rejection | The coordinate already exists remotely. | Do not overwrite it; increment the contract version. |
| Local consumer cannot resolve a generated artifact | The local OpenAPI repository was not published or declared. | Publish locally and add the configured repository directory to the consumer. |
| POM or sources artifact is missing | Publication did not complete or an artifact kind is disabled. | Inspect the named Maven publication and rerun after compatibility succeeds. |

Remote publication does not automatically run the complete compatibility gate.
Run it first. Credential precedence and CI sequencing are documented in
[OpenAPI Artifact Publishing](publishing.md).

## Scaffolding Failures

| Message or symptom | Meaning | Resolution |
|---|---|---|
| `Provide exactly one of --spec or --api-jar` | Both sources or neither source were supplied. | Select one contract source. |
| `Contract source does not exist` | The source path cannot be resolved. | Use an existing document or server API JAR. |
| `embedded contract metadata is missing` | The selected JAR is not a framework server API artifact. | Use the generated server API JAR, not models or client. |
| `contains no ApiDelegate interfaces` | The JAR cannot drive server implementation scaffolding. | Regenerate a server API from a contract with operations. |
| `Output directory is not empty` | Replacement is disabled for a non-empty target. | Select an empty directory; use force only for disposable output. |
| `Refusing to replace unsafe output directory` | Force mode targeted a filesystem root or user home. | Choose a dedicated generated directory. |
| Generated project cannot resolve the server API | Its Maven coordinate is unavailable from configured repositories. | Publish the server API locally or configure the contract repository. |
| Generated project fails an architecture rule | Application code crosses a generated hexagonal boundary. | Move the dependency behind the appropriate port or adapter. |

See [OpenAPI Project Scaffolding](scaffolding.md) before using force mode.

## Contract Test Failures

| Violation | Meaning | Resolution |
|---|---|---|
| `UNKNOWN_OPERATION` | The test references an operation ID absent from the contract. | Use the declared operation ID or update the test contract. |
| `MISSING_TEST_CASE` | `verifyAll` found an operation without a test case. | Add a case or use focused verification intentionally. |
| `MISSING_PATH_PARAMETER` or `MISSING_REQUEST_PARAMETER` | Required request data is absent. | Supply the declared path, query, or header value. |
| `UNDECLARED_REQUEST_BODY` or `UNDECLARED_REQUEST_CONTENT_TYPE` | The case sends input not declared by the operation. | Align the request or update the contract. |
| `INVALID_JSON_REQUEST` or `REQUEST_SCHEMA_MISMATCH` | Request JSON cannot satisfy the declared schema. | Fix fixture types, required fields, or enum values. |
| `UNEXPECTED_STATUS` or `UNDECLARED_STATUS` | Runtime status differs from the selected declared response. | Fix application behavior or declare the intended response. |
| `UNDECLARED_CONTENT_TYPE` | Runtime media type is not allowed by the response. | Return a declared content type. |
| `INVALID_JSON_RESPONSE` or `RESPONSE_SCHEMA_MISMATCH` | Runtime JSON cannot satisfy the response schema. | Align serialization and response models with the contract. |

Request and response coverage is documented in
[OpenAPI Contract Testing](../openapi-contract-testing.md).

## CI Diagnostics

Use `--info` only after the focused task and normal report are insufficient.
Use `--debug` cautiously because build logs can include repository URLs and
environment-derived values. Never publish credentials, authorization headers,
private registry tokens, or complete environment dumps in an issue.

Before reporting a framework defect, collect:

- the first failing task and complete exception type;
- the smallest sanitized OpenAPI document that reproduces the issue;
- effective framework, Spring Boot, OpenAPI Generator, and Java versions;
- the relevant generated report;
- whether a clean build reproduces the failure;
- expected and actual artifact kind or operation ID.

Do not attach private generated artifacts when the contract itself is
confidential.

## Validation

Protect this catalog and the repository examples with:

```bash
./gradlew validateOpenApiExamplesAndTroubleshooting
./gradlew documentationCheck
./gradlew smbtechOpenApiCompatibilityCheck
```

Return to the [OpenAPI Portal](index.md) after resolving the failure.
