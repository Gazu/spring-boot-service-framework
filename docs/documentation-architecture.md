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
| Use-case guide index | `docs/guides/index.md` |
| Java code conventions | `docs/code-conventions.md` |
| Public API and implementation package boundaries | `docs/public-api-boundaries.md` |
| Module README convention | `docs/module-readme-convention.md` |
| Supported versions and compatibility contract | `docs/compatibility.md` |
| OpenAPI code generation coordinate convention | `docs/openapi-codegen.md` |
| OpenAPI breaking change detection and baseline policy | `docs/openapi-breaking-changes.md` |
| OpenAPI contract testing | `docs/openapi-contract-testing.md` |
| OpenAPI generator evolution roadmap | `docs/openapi-evolution.md` |
| OpenAPI generator module boundary | `spring-boot-service-framework-openapi-generator/README.md` |
| OpenAPI contract testing module boundary | `spring-boot-service-framework-openapi-contract-testing/README.md` |
| OpenAPI Gradle build-logic boundary | `build-logic/openapi-generator-plugin/README.md` |
| Java and Spring Boot Gradle conventions | `build-logic/conventions/README.md` |
| Documentation validation build logic | `gradle/documentation-checks.gradle` |
| Generated public surface baseline | `docs/public-api-inventory.md` |
| Public boundary and marker validation build logic | `gradle/public-api-inventory.gradle` |
| Root lifecycle build logic | `gradle/lifecycle.gradle` |
| Release history | `CHANGELOG.md` |
| Release process | `docs/releasing.md` |
| Troubleshooting catalog | `docs/troubleshooting.md` |
| Structured logging usage and configuration | `docs/logging.md` |
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
| Migration of renamed public types, packages, and properties | `docs/guides/migrate-public-names-and-properties.md` |
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
- new OpenAPI generation behavior: update `docs/openapi-codegen.md`;
- new OpenAPI generator module behavior: update
  `spring-boot-service-framework-openapi-generator/README.md` and
  `docs/openapi-codegen.md`;
- new OpenAPI Gradle plugin behavior: update
  `build-logic/openapi-generator-plugin/README.md` and
  `docs/openapi-codegen.md`;
- new OpenAPI generator migration stage or long-term capability: update
  `docs/openapi-evolution.md`;
- new documentation validation task: update `gradle/documentation-checks.gradle`
  and this document;
- new root lifecycle task: update `gradle/lifecycle.gradle`, `README.md`, and
  `docs/releasing.md` when it affects release or validation workflow;
- new compatibility promise: update the compatibility document and tests;
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
