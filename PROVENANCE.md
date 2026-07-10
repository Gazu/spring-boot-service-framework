# Provenance and contribution policy

This document records the known source provenance of the code before any
publication. It is not a legal review and does not grant rights over third-party
or restricted code.

## Inventory

| Component | Known provenance | Publication status |
|---|---|---|
| Gradle multi-module build, auto-configuration, and tests | Created in this repository during July 2026 | Review repository ownership before publishing |
| `spring-boot-service-framework-logging-core` | New implementation based on requirements documented in `ROADMAP.md` | Technically publishable; ownership confirmation pending |
| `commons.logging` compatibility API | Migrated from the local `projects-service` project | Restricted until authorship/license is confirmed |
| Encoder and appenders migrated from `projects-service` | Removed in Phase 2 and replaced by new adapters | Not part of the current artifacts |
| `Slf4jLogEventSink`, `SmbStructuredLogFormatter`, and `MdcCorrelationContext` adapters | New implementation based on core ports and public APIs | Technically publishable; ownership confirmation pending |
| Chassis structure shown in screenshots | Conceptual reference for modular organization; not a license | Do not copy code, text, or assets |
| Maven dependencies | Third-party artifacts declared in Gradle | Keep and review licenses before distribution |

## Rules

1. Every new contribution must state whether it is original, derived, or
   third-party code.
2. Company, client, or private-repository code requires written authorization.
3. Open-source code must include origin, version, license, and obligations.
4. Clean reimplementation must start from requirements and public documentation,
   not cosmetic changes over a restricted implementation.
5. Do not remove history or attribution to simulate authorship.

## Template for new components

```text
Component:
Author or owner:
Origin:
License or authorization:
Date added:
Changes made:
Evidence:
Publication status:
```

Before private or public publication, every item marked as restricted must have
authorization or be replaced.
