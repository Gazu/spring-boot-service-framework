# Public API Boundaries

This document defines which Java packages and types are supported consumer
contracts. A Java `public` modifier is necessary for technical access, but does
not by itself make a type part of the supported API.

The generated [Public API Inventory](public-api-inventory.md) is the executable
artifact-by-artifact baseline for these rules.

## Supported Package Convention

A framework package is supported when one of its complete package segments is:

- `domain`, including descendants such as `domain.validation`;
- `port`, including `port.in`, `port.out`, and their descendants;
- `api`, including every `api.*` package.

Examples:

```text
com.smbtech.serviceframework.httpclient.domain
com.smbtech.serviceframework.httpclient.port.out
com.smbtech.serviceframework.starter.restclient.api
com.smbtech.serviceframework.starter.restclient.api.oauth2
```

Types in these packages are supported for consumer compilation and documented
extension unless an individual type is explicitly excluded in a future major
version. Public API changes follow the compatibility and release policies.

## Implementation Packages

The following package segments are implementation by default:

| Segment | Ownership |
|---|---|
| `adapter` | Technology-specific inbound and outbound integration. |
| `autoconfigure` | Spring Boot conditions, configuration binding, and bean assembly. |
| `serialization` | Framework-owned wire serialization implementation. |
| `internal` | Explicit non-consumer implementation. |
| build logic | Gradle plugin implementation, task classes, and repository build orchestration. |

This implementation classification takes precedence over the general convention.
Consumers must not instantiate, subclass, inject by concrete class, or import
these types unless a documented exception below explicitly permits it.

Public implementation classes may be required by Spring, Gradle, reflection,
configuration binding, or cross-package wiring. They may change without the
source-compatibility guarantees applied to supported API. They are tracked as
`Public Infrastructure Types` or `Public Internal Types To Review` in the
inventory. Their reviewed names are frozen separately in
`gradle/compatibility/public-internal-types.txt` so the accidental surface can
shrink but cannot grow unnoticed.

## Nullability Contract

Every supported Java package declares JSpecify `@NullMarked` in its
`package-info.java`. Unannotated types in those packages are non-null by
default; nullable components, parameters, and returns use `@Nullable`.

Implementation packages do not carry a supported nullability contract.
`validatePublicApiNullability` prevents a supported package from being added
without an explicit nullness default.

## Package Exceptions

These existing packages are supported even though they do not follow the
`domain`, `port`, or `api` convention:

| Artifact | Supported package exception | Reason |
|---|---|---|
| `spring-boot-service-framework-commons` | `com.smbtech.serviceframework.commons.notification` | Shared notification response and exception contract. |
| `spring-boot-service-framework-http-client-core` | `com.smbtech.serviceframework.httpclient.exception` | Exceptions that consumers inspect or catch. |
| `spring-boot-service-framework-mock-core` | `com.smbtech.serviceframework.mock.exception` | Mock failures exposed by core ports. |
| `spring-boot-service-framework-error-core` | `com.smbtech.serviceframework.error` | Existing error definitions, policies, resolution pipeline, and service exception API. |
| `spring-boot-service-framework-error-core` | `com.smbtech.serviceframework.error.metadata` | Stable structured error metadata contract. |
| `spring-boot-service-framework-openapi-generator` | `com.smbtech.serviceframework.openapi.generator` | Existing reusable build-time generator API. |
| `spring-boot-service-framework-openapi-contract-testing` | `com.smbtech.serviceframework.openapi.contract` | Existing test-scope OpenAPI contract API. |

New capabilities must use a convention package instead of adding another
package exception. Moving an existing exception package requires an explicit
compatibility migration.

## Type Exceptions

Only these individual types are supported outside convention packages:

| Artifact | Supported type exception | Reason |
|---|---|---|
| `spring-boot-service-framework-starter-logging` | `com.smbtech.serviceframework.starter.logging.StructuredLoggers` | Consumer-facing static facade for structured logger lookup. |
| OpenAPI Gradle plugin | `com.smbtech.serviceframework.gradle.openapi.SmbtechOpenApiExtension` | Public `smbtechOpenApi` DSL model. |
| OpenAPI Gradle plugin | `com.smbtech.serviceframework.gradle.openapi.SmbtechOpenApiSpec` | Public named-spec DSL model. |

An exception applies only to the listed type. It does not make sibling types or
the containing package public API.

## Auto-Configuration Boundary

`*AutoConfiguration` and `*Properties` classes can be public because Spring Boot
must discover or bind them. They remain framework infrastructure:

- consumers configure documented property keys instead of constructing
  properties classes;
- consumers replace documented API interfaces or beans instead of importing an
  auto-configuration class;
- bean names and concrete default classes are not contracts unless documented;
- auto-configuration must back off for supported replacement points.

An auto-configuration or properties type becomes supported only if it is added
as an explicit type exception. No current auto-configuration type has that
status.

## Build Logic Boundary

The OpenAPI Gradle plugin exposes these supported non-Java contracts:

- plugin id `com.smbtech.service-framework.openapi-generator`;
- the `smbtechOpenApi` DSL and its documented properties;
- documented task names used by consumers and lifecycle checks;
- generated artifact coordinates and repository layout documented by the
  OpenAPI guides.

The plugin implementation class, concrete task classes, build scripts, and
repository lifecycle wiring are implementation. Only the two DSL model types
listed as type exceptions are supported Java types.

## Module Identification

Framework artifacts do not use empty Java `*Module` marker classes. Their
boundary and purpose are identified by:

- Gradle/Maven artifact coordinates;
- package-level Javadoc in `package-info.java`;
- the module README;
- Spring Boot auto-configuration metadata for starters.

Do not add new marker classes. A legacy marker with confirmed external consumers
may be retained only as an explicitly documented compatibility exception until
the next incompatible release.

Generated OpenAPI artifacts are governed by their OpenAPI contract, generated
coordinates, and compatibility checks. Their generated package layout is not
classified by this framework-source package convention.

## Review Rules

For every new or modified public type:

1. Place consumer contracts in `domain`, `port`, `api`, or `api.*`.
2. Keep implementation types package-private where possible.
3. Do not add a package or type exception without documenting its consumer need.
4. Regenerate the inventory with `./gradlew generatePublicApiInventory`.
5. Review any addition under `Public Internal Types To Review`; prefer reducing
   visibility and regenerate `generatePublicInternalTypeBaseline` only after
   intentional review.
6. Run `./gradlew validatePublicApiInventory`,
   `./gradlew validatePublicApiNullability`, and
   `./gradlew binaryCompatibilityCheck`.
7. Run `./gradlew compatibilityCheck`.

The inventory validates that documented package/type exceptions and public
infrastructure declarations still resolve to real source types. Any drift must
be reviewed rather than silently changing the boundary.
