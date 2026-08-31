# OpenAPI Documentation Architecture

OpenAPI documentation has one owner for each concept, reference, and procedure.
This contract keeps consumer guidance separate from Gradle implementation notes
and prevents module READMEs from becoming parallel manuals.

## Principles

- A reader enters through one OpenAPI portal and chooses a goal.
- Concepts explain why the framework behaves as it does.
- References define exact supported inputs, defaults, outputs, and commands.
- Procedures contain ordered, executable steps for one outcome.
- Module READMEs identify a module; they do not reproduce its manual.
- ADRs preserve decisions and context; they are not usage documentation.
- Release notes record delivered changes; completed roadmaps are removed.
- Every public behavior has one canonical owner and automated drift protection.

## Document Layers

| Layer | Responsibility | Content rule |
|---|---|---|
| Portal | Route readers by goal and role | Links and one-sentence summaries only |
| Concepts | Explain architecture, boundaries, and artifact relationships | Stable behavior without copy-ready procedures |
| Getting started | Complete the smallest supported workflow | One executable path with defaults |
| Reference | Define DSL, tasks, metadata, coordinates, and compatibility rules | Complete tables and exact contracts |
| Procedures | Complete one operational task | Ordered steps, commands, expected result, and recovery link |
| Module README | Identify artifact purpose, dependency, public API, boundaries, and validation | Short summary plus canonical links |
| ADR | Preserve a cross-module decision | Context, decision, consequences, and status |
| Example README | Run and inspect one repository example | Environment, command, assertions, and canonical links |
| Changelog | Record released or unreleased user-visible changes | Outcome only, without long-lived guidance |

## Canonical Ownership

The target structure is introduced incrementally. Until a target document is
created, the current owner in this table remains canonical.

| Topic | Current owner | Target owner |
|---|---|---|
| OpenAPI navigation | `docs/openapi/index.md` | Same portal |
| Documentation ownership and boundaries | This document | `docs/openapi/documentation-architecture.md` |
| Frozen implemented behavior | `docs/openapi-behavior-inventory.md` | `docs/openapi/behavior-inventory.md` |
| Minimal end-to-end adoption | `docs/openapi/getting-started.md` | Same getting-started guide |
| Artifact generation and contents | `docs/openapi/generation.md` | Same generation reference |
| Gradle DSL, defaults, and tasks | `docs/openapi/plugin-reference.md` | Same plugin reference |
| Configuration, document, coordinate, and compatibility validation | `docs/openapi/validation.md` | Same validation reference |
| Maven publication and credentials | `docs/openapi/publishing.md` | Same publishing guide |
| Coordinates, SemVer, baselines, and breaking changes | `docs/openapi/versioning.md` | Same versioning reference |
| MVC contract testing and mock behavior | `docs/openapi-contract-testing.md` and `docs/mock.md` | `docs/openapi/testing.md` |
| Hexagonal project generation | `docs/openapi/scaffolding.md` | Same scaffolding reference |
| Executable repository examples and expected evidence | `docs/openapi/examples.md` | Same examples guide |
| OpenAPI-specific failure diagnosis | `docs/openapi/troubleshooting.md` | Same troubleshooting guide |
| Source-generation engine decision | `docs/adr/0001-openapi-generator-engine.md` | Same ADR |
| Gradle plugin module boundary | `build-logic/openapi-generator-plugin/README.md` | Same module README |
| Template bundle boundary | `build-logic/openapi-templates/README.md` | Same module README |
| Contract-testing module boundary | `spring-boot-service-framework-openapi-contract-testing/README.md` | Same module README |

A target path does not become canonical merely because a placeholder exists.
It becomes canonical when its complete content, inbound links, and validation
rules replace the current owner in the same change.

## Content Boundaries

### Portal

`docs/openapi/index.md` owns navigation. It may summarize generation,
publication, testing, mocks, and scaffolding in one sentence each. It must not
contain DSL tables, full examples, compatibility policy, or troubleshooting
details.

### Concepts And Architecture

Concept documents own the relationships among the contract, generated
artifacts, applications, and build-time modules. They may explain why models are
shared or why scaffolding is separate, but exact Gradle properties belong in the
plugin reference.

### Getting Started And Procedures

Getting started owns one minimal happy path from an OpenAPI document to locally
published artifacts. Other procedures cover one task each, such as publishing
remotely or checking a breaking change. Procedures link to reference material
for optional settings instead of duplicating property tables.

### Reference

Reference documents are exhaustive for their declared surface. The plugin
reference owns the public `smbtechOpenApi` DSL and `smbtechOpenApi*` tasks. The
behavior inventory freezes the implementation until those dedicated references
are complete. Versioning owns baseline layout and SemVer policy. Generation owns
the contents and dependency boundaries of models, server API, and client JARs.
Validation owns build-time DSL, document, coordinate, and aggregate
compatibility rules; runtime request/response verification remains testing.
Scaffolding owns project-generator inputs, defaults, output structure,
hexagonal boundaries, replacement safety, and one-time evolution guidance.
Examples own the purpose, commands, and observable evidence for repository
fixtures. Troubleshooting owns exact failure symptoms, report locations, and
recovery steps.

The behavior inventory is a compatibility snapshot, not a second usage
reference. It may repeat only values that are compared with implementation or
compatibility baselines and must link to the canonical owner for explanation.

### Module READMEs

An OpenAPI module README contains only:

- purpose and intended consumer;
- published coordinate or plugin ID;
- supported public API or explicit absence of one;
- implementation boundary and non-goals;
- links to canonical OpenAPI documentation;
- local module validation command.

At most one minimal configuration fragment is allowed when needed to identify
the module. Full DSL, task, metadata, publication, testing, or troubleshooting
sections belong in canonical documents.

### ADRs And History

[ADR 0001](../adr/0001-openapi-generator-engine.md) owns the decision to use
OpenAPI Generator. ADR wording may retain historical context. Usage guides must
describe only current behavior and link to the ADR when the reason matters.
Completed implementation plans are removed; delivered outcomes remain in the
changelog and behavior inventory.

## Duplication Policy

Allowed duplication is limited to:

- one-sentence orientation;
- artifact coordinates needed to identify a dependency;
- one minimal command or configuration fragment;
- links to the canonical owner.

The following must have exactly one owner:

- complete DSL and task tables;
- generated JAR content and metadata contracts;
- publication credential rules;
- breaking-change and baseline policy;
- build-time configuration, document, coordinate, and compatibility validation;
- complete contract-testing examples;
- troubleshooting catalogs;
- scaffolding output structure.

When moving ownership, update the target document, all inbound links,
`docs/documentation-architecture.md`, and documentation checks atomically. The
old document is removed instead of retained as a second source.

## Naming And Links

- OpenAPI prose uses `OpenAPI`; Java and Gradle symbols use `OpenApi`.
- File names use lowercase kebab-case.
- Links use descriptive labels instead of raw paths.
- Repository and documentation portals link to canonical documents.
- Module READMEs link upward to canonical documentation.
- Canonical documents may link downward to module READMEs for implementation
  boundaries.
- Documents do not link to completed roadmaps or removed legacy modules.

## Change Ownership

| Change | Documents updated in the same change |
|---|---|
| DSL property, default, or public task | Plugin reference or current code-generation owner, behavior inventory, plugin README when its boundary changes |
| Artifact coordinate, package, dependency, or JAR content | Generation owner and behavior inventory |
| Metadata key or embedded path | Generation owner and behavior inventory |
| Publication repository or credential behavior | Publication owner and behavior inventory |
| SemVer, baseline, or compatibility behavior | Versioning owner, behavior inventory, and compatibility guide |
| Configuration, document, coordinate, or validation task behavior | Validation owner, behavior inventory, and plugin reference when its public task contract changes |
| Contract-testing behavior | Testing owner and contract-testing module README when its public boundary changes |
| Mock behavior | Testing owner, mock guide, and mock module README when its boundary changes |
| Scaffolding input or output | Scaffolding owner and project-generator README when its boundary changes |
| Repository fixture or expected evidence | Examples owner and the related contract or baseline |
| Failure message, report, or recovery behavior | Troubleshooting owner and the affected reference |
| Source-generation engine or module ownership | New ADR or ADR status update, this architecture, and behavior inventory |

## Validation Contract

`validateOpenApiDocumentationArchitecture` protects this ownership model. It
verifies canonical sources, module links, the accepted OpenAPI ADR, absence of
the completed evolution roadmap, and the target document map. The task is part
of `documentationCheck`.

`validateOpenApiDocumentationDeduplication` protects the same boundaries at the
content level. It keeps conceptual overviews free of executable examples,
prevents procedures from becoming parallel implementation guides, and confines
complete DSL, metadata, credentials, and scaffolding details to their declared
owners.

`validateOpenApiDocumentationCommands` discovers every `./gradlew` command in
canonical OpenAPI documents and verifies that its root, subproject, or included
build task exists. It writes the machine-readable summary
`build/reports/smbtech-openapi/documentation-check.txt`.

`openApiDocumentationCheck` aggregates all OpenAPI documentation contracts,
command validation, spec validation, and plugin build-logic validation. It is
part of `documentationCheck`, so the pull request and release gates execute it
without separate workflow logic.

## Compatibility And Rollout

The reviewed baseline
`gradle/compatibility/contracts/openApiDocumentation.txt` protects canonical
document paths, validation task names, the generated evidence path, and
lifecycle integrations. `openApiDocumentationCompatibilityCheck` compares the
current model with that baseline and runs the complete documentation gate.

| Layer | Enforcement | Status |
|---|---|---|
| Local authoring | `openApiDocumentationCheck` | Active |
| Pull requests | `pullRequestGate` through `documentationCheck` and `compatibilityCheck` | Blocking |
| Compatibility | `openApiDocumentationCompatibilityCheck` | Blocking |
| Release | `releaseGate` through `compatibilityCheck` | Blocking |

A canonical path, task, evidence location, or integration may change only with
an intentional compatibility update. Run
`generateOpenApiDocumentationCompatibilityContract`, review the baseline diff,
update inbound links and migration guidance, and record the change in the
changelog. Do not regenerate the baseline merely to make CI pass.

Run the complete documentation and OpenAPI compatibility gates with:

```bash
./gradlew openApiDocumentationCheck
./gradlew openApiDocumentationCompatibilityCheck
./gradlew documentationCheck
./gradlew smbtechOpenApiCompatibilityCheck
./gradlew openApiGradlePluginCompatibilityCheck
```
