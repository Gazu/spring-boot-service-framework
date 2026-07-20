# Module README Convention

Module READMEs are short entry points. They explain what the module is, when to
use it, which public API it exposes, what it intentionally does not do, where
the canonical documentation lives, and how to validate the module locally.

Long feature guides, complete property references, release rules, and
troubleshooting catalogs belong under `docs/`.

## Required Shape

Every module README must use this structure:

```text
# Human-readable module name

One or two short paragraphs describing the module purpose and boundary.

## When to use
## Dependency
## Public API
## What this module does not do
## Main documentation
## Local validation
```

The required sections must appear in that order. Additional short sections are
allowed only when they help orient the reader, for example `Quick start`.

## Section Rules

| Section | Must contain |
|---|---|
| `When to use` | Consumer-facing use cases and a clear note when another module should be preferred. |
| `Dependency` | A Gradle dependency snippet using the current framework version. |
| `Public API` | Public types, packages, ports, customizers, or services intended for consumers. |
| `What this module does not do` | Boundary statements that prevent the module from becoming a catch-all. |
| `Main documentation` | Links to canonical guides, property references, examples, or related modules. |
| `Local validation` | At least one runnable Gradle command for checking the module and its focused compatibility task when one exists. |

## Core Modules

Core modules should emphasize framework-neutral contracts and hexagonal
boundaries. They should not include Spring Boot configuration examples,
complete adapter behavior, or starter-specific instructions.

Core module READMEs should link to the starter or canonical guide that owns
runtime behavior.

## Starter Modules

Starter modules may include a minimal configuration or Java snippet when it is
the fastest way to identify the feature. They should not duplicate complete
property tables or long OAuth2, logging, mock, or troubleshooting guides.

Starter module READMEs should link to:

- the canonical feature guide;
- generated property reference;
- relevant example application;
- troubleshooting guide when useful.

## Validation

The root `documentationCheck` task runs `validateModuleReadmes`, which verifies:

- each module README exists;
- each module README has exactly one H1;
- an introductory paragraph appears before the first H2;
- the required sections exist in the required order;
- dependency snippets use the current root project version;
- local validation includes a Gradle command.
