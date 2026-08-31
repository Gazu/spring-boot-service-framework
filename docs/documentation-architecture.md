# Documentation Architecture

This repository treats documentation as part of the public API. Public behavior,
configuration, extension points, examples, and compatibility rules must have one
canonical location. Other documents may summarize and link to that canonical
source, but should not duplicate long explanations.

## Goals

- Help consumers find the right document quickly.
- Keep module READMEs short and stable.
- Avoid duplicated configuration examples that drift over time.
- Make public APIs, extension points, and compatibility promises explicit.
- Keep examples focused on runnable behavior instead of full reference material.

## Document Roles

| Location | Role | Should contain | Should not contain |
|---|---|---|---|
| `README.md` | Repository landing page | Project overview, module map, minimal quick starts, verification commands, documentation map. | Full property references, deep OAuth2 flows, long troubleshooting catalogs. |
| `docs/index.md` | Documentation portal | Links grouped by reader need, feature area, module, example, and maintainer task. | Full guides or duplicated reference material. |
| `docs/` | Canonical user and maintainer guides | Feature guides, public contracts, compatibility rules, troubleshooting, release process, property references. | Module build details that belong in a module README. |
| `docs/guides/` | Use-case recipes | Copy-oriented examples for common scenarios, with links to canonical references. | Full property references or broad feature contracts. |
| Module `README.md` files | Module cards | Purpose, when to use, dependency, public API summary, canonical documentation links, local validation command. | Repeated full guides already owned by `docs/`. |
| `examples/*/README.md` | Runnable example notes | What the example validates, how to run it, required environment variables, test behavior. | General framework theory or complete feature reference. |
| `CONTRIBUTING.md` | Contribution rules | Commit, PR, versioning, documentation ownership, test expectations. | Feature usage guides. |
| `CHANGELOG.md` | Release history | User-facing release notes grouped by version. | Release procedure or unreleased implementation plans. |
| `CODE_OF_CONDUCT.md` | Community behavior | Expected behavior and enforcement. | Technical usage guidance. |
| `PROVENANCE.md` | Source provenance | Origin, ownership, license, and publication constraints. | Feature guides or release notes. |

## Canonical Sources By Topic

| Topic | Canonical document |
|---|---|
| Repository overview and module map | `README.md` |
| Documentation navigation | `docs/index.md` |
| Actuator module architecture and safety boundaries | `docs/actuator.md` |
| Actuator supported API, runtime names, and change policy | `docs/actuator/compatibility.md` |
| Actuator property reference | `docs/actuator/property-reference.md` |
| Framework BOM and dependency version alignment | `docs/dependency-management.md` |
| Use-case guide index | `docs/guides/index.md` |
| Java code conventions | `docs/code-conventions.md` |
| Public API and implementation package boundaries | `docs/public-api-boundaries.md` |
| Module README convention | `docs/module-readme-convention.md` |
| Supported versions and compatibility contract | `docs/compatibility.md` |
| OpenAPI documentation navigation | `docs/openapi/index.md` |
| Minimal OpenAPI adoption workflow | `docs/openapi/getting-started.md` |
| OpenAPI generated artifact contents and boundaries | `docs/openapi/generation.md` |
| OpenAPI Gradle plugin DSL, defaults, validation, and tasks | `docs/openapi/plugin-reference.md` |
| OpenAPI local and remote Maven publication | `docs/openapi/publishing.md` |
| OpenAPI contract identity, baselines, and SemVer policy | `docs/openapi/versioning.md` |
| OpenAPI configuration, spec, coordinate, and compatibility validation | `docs/openapi/validation.md` |
| OpenAPI service scaffolding inputs, output, and hexagonal boundaries | `docs/openapi/scaffolding.md` |
| OpenAPI repository examples and expected evidence | `docs/openapi/examples.md` |
| OpenAPI task diagnostics, reports, and recovery | `docs/openapi/troubleshooting.md` |
| Frozen OpenAPI plugin and generated-artifact behavior | `docs/openapi-behavior-inventory.md` |
| OpenAPI documentation ownership and content boundaries | `docs/openapi/documentation-architecture.md` |
| OpenAPI code generation capability overview | `docs/openapi-codegen.md` |
| OpenAPI contract identity, baselines, and SemVer policy | `docs/openapi/versioning.md` |
| OpenAPI contract testing | `docs/openapi-contract-testing.md` |
| OpenAPI contract testing module boundary | `spring-boot-service-framework-openapi-contract-testing/README.md` |
| OpenAPI Gradle build-logic boundary | `build-logic/openapi-generator-plugin/README.md` |
| Java and Spring Boot Gradle conventions | `build-logic/conventions/README.md` |
| Documentation validation build logic | `gradle/documentation-checks.gradle` |
| Generated public surface baseline | `docs/public-api-inventory.md` |
| Pre-1.0 API and binary compatibility policy | `docs/pre-1.0-api-policy.md` |
| Public boundary and marker validation build logic | `gradle/public-api-inventory.gradle` |
| Root lifecycle build logic | `gradle/lifecycle.gradle` |
| Pull request CI contract and ownership | `docs/quality-pipeline.md` |
| Release history | `CHANGELOG.md` |
| Release process | `docs/releasing.md` |
| Troubleshooting catalog | `docs/troubleshooting.md` |
| Structured logging usage and configuration | `docs/logging.md` |
| Logging supported API, runtime names, resources, and change policy | `docs/logging/compatibility.md` |
| Async logging topology, saturation, limits, and baseline | `docs/logging/async-appender.md` |
| Structured logging property reference | `docs/logging/property-reference.md` |
| REST client usage and configuration | `docs/rest-client.md` |
| REST client property reference | `docs/rest-client/property-reference.md` |
| REST client public extension points | `docs/rest-client-extension-points.md` |
| Mock core and starter usage | `docs/mock.md` |
| Mock property reference | `docs/mock/property-reference.md` |
| Error handling usage and status policy | `docs/error-handling.md` |
| Spring Security error catalog and OAuth2 metadata contract | `docs/error-handling/security.md` |
| Error handling snake-case JSON contract | `docs/error-handling/json-contract.md` |
| Error handling property reference | `docs/error-handling/property-reference.md` |
| Error handling public extension points | `docs/error-handling-extension-points.md` |
| Migration from copied shared exception code | `docs/guides/migrate-shared-exception.md` |
| Pre-1.0 source, binary, dependency, and configuration migration | `docs/guides/migrate-public-names-and-properties.md` |
| Contribution rules | `CONTRIBUTING.md` |
| Code of conduct | `CODE_OF_CONDUCT.md` |
| Provenance and publication constraints | `PROVENANCE.md` |

When a new topic grows beyond a short section, create a dedicated document under
`docs/` and link to it from the closest existing guide.

## Duplication Rules

Short duplication is allowed only for orientation. Long examples, complete
property tables, troubleshooting catalogs, or lifecycle explanations must have
one owner.

Acceptable duplication:

- one-sentence summaries;
- a minimal quick start;
- a short code or YAML snippet that helps identify the feature;
- a link to the canonical document.

Avoid duplicating:

- full OAuth2 flow descriptions;
- complete `application.yml` examples;
- property references;
- error message catalogs;
- release or compatibility rules;
- extension point contracts.

Use-case guides may include focused copy-ready YAML or Java snippets when the
snippet is the point of the guide. They should link to canonical reference
documents instead of restating every option.

If a document needs a detailed explanation owned elsewhere, link to the canonical
document and add a brief note about why the reader should go there.

## Module README Template

Module READMEs must follow the short module-card convention documented in
[Module README Convention](module-readme-convention.md).

Starter READMEs may include a minimal YAML or Java example, but complete
configuration references and long feature guides should live in `docs/`.

Core module READMEs should emphasize boundaries, public domain types, ports, and
the fact that runtime integration belongs in starters.

## Example README Template

Each example README should answer:

- what artifact the example consumes;
- whether it consumes published local artifacts or project dependencies;
- what behavior the example validates;
- required environment variables;
- how to run it manually;
- which smoke test covers it.

Example READMEs should link to canonical guides for general feature behavior.

## Public API Documentation Rules

Any new public API, configuration property, extension point, or compatibility
promise must be documented before it is considered complete.

The generated `docs/public-api-inventory.md` is the repository-wide baseline.
Run `./gradlew generatePublicApiInventory` after an intentional public surface
change and review the resulting artifact-level diff.

Use these locations:

- Java naming, package, Javadoc, exception, implementation, and
  auto-configuration rules: `docs/code-conventions.md`;
- supported and implementation package boundaries:
  `docs/public-api-boundaries.md`;
- public REST client extension point: `docs/rest-client-extension-points.md`;
- public error handling extension point: `docs/error-handling-extension-points.md`;
- starter configuration property: the generated property reference for that
  starter under `docs/**/property-reference.md`;
- compatibility promise or replacement point: `docs/compatibility.md`;
- module-level public type: the owning module README;
- runnable consumer behavior: the relevant example README.

Do not document internal adapter classes as extension points. If consumers need
to customize behavior, expose a public interface, customizer, contributor, or
replacement point first.

## Change Checklist

When changing public behavior, update documentation in the same pull request:

- new configuration property: update the canonical property reference and at
  least one usage example when useful;
- new public interface or customizer: update the extension-point guide;
- new OpenAPI generation behavior: update `docs/openapi/generation.md` and
  `docs/openapi-behavior-inventory.md`;
- new OpenAPI Gradle plugin behavior: update
  `docs/openapi/plugin-reference.md`, then update the compatibility contract
  and `docs/openapi-behavior-inventory.md` when implemented behavior changes;
- new OpenAPI publication behavior: update `docs/openapi/publishing.md` and its
  executable documentation check;
- new OpenAPI versioning or baseline behavior: update
  `docs/openapi/versioning.md`, its executable documentation check, and the
  focused breaking-change procedure;
- new OpenAPI validation behavior: update `docs/openapi/validation.md` and its
  executable documentation check;
- new or changed OpenAPI fixture: update `docs/openapi/examples.md` and its
  executable documentation check;
- new OpenAPI failure message or report: update
  `docs/openapi/troubleshooting.md` and its executable documentation check;
- new OpenAPI documentation owner or content layer: update
  `docs/openapi/documentation-architecture.md`;
- new documentation validation task: update `gradle/documentation-checks.gradle`
  and this document;
- new root lifecycle task: update `gradle/lifecycle.gradle`, `README.md`, and
  `docs/releasing.md` when it affects release or validation workflow;
- new compatibility promise: update the compatibility document and tests;
- new platform-managed module, BOM import, or dependency policy: update
  `docs/dependency-management.md`, the platform compatibility contract, and
  consumer smoke tests;
- new Actuator core, starter, contributor, endpoint, metrics, or integration behavior:
  update `docs/actuator.md`, `docs/actuator/compatibility.md`, and the Actuator
  architecture and compatibility validations;
- release preparation: update `CHANGELOG.md`, `docs/releasing.md` when the
  process changes, and all version examples together;
- new starter feature: update the starter README with a short summary and link
  to the canonical guide;
- new example behavior: update the example README and keep sensitive values as
  environment placeholders;
- renamed module or artifact: update `README.md`, module README, examples, and
  documentation links together.
- renamed public type, package, or property: update the owning module README,
  generated property reference, extension-point guide, compatibility contract,
  examples, changelog, and migration guide together.

## Validation

Run documentation checks before opening a pull request:

```bash
./gradlew documentationCheck
```

This validates Markdown structure, relative links and anchors, canonical
documentation coverage, changelog/release docs, framework version references,
OpenAPI name normalization and `info.title`/`info.version`, generated property
references, OpenAPI spec version catalog, OpenAPI breaking change detection,
generated OpenAPI metadata, models JARs, server API JARs, client JARs, advanced OpenAPI model generation,
reproducible OpenAPI generation, generated OpenAPI compilation tests, and
example documentation/configuration for committed secrets or unsafe literal
values.
Broader changes should also run:

```bash
./gradlew check
```
