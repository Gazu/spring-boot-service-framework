# ADR 0002: OpenAPI Artifact Contract

- Status: Accepted
- Date: 2026-09-02

## Context

Each OpenAPI contract must produce a stable Java boundary that providers and
consumers can resolve independently. Artifact names, Java packages, version
identity, and dependency edges must not depend on repository layout or CI
implementation details.

The framework targets Java 21 and Spring Boot 4.1. A client artifact exposes
both Spring HTTP Interface and Spring Cloud OpenFeign interfaces without
forcing every consumer to use OpenFeign.

## Decision

### Runtime Baseline

Generated source and artifacts target Java 21 and Spring Boot 4.1. The Java
release is part of every external artifact ID so a future Java baseline can be
published without replacing existing coordinates.

Consumers of the OpenFeign interfaces must use Spring Cloud release train
`2025.1.2` or newer within the `2025.1.x` line for Spring Boot 4.1. The client
artifact targets the train-managed Spring Cloud OpenFeign `5.0.x` family but
does not publish it as a transitive dependency; the consuming application adds
`spring-cloud-starter-openfeign` explicitly.

### Maven Identity

For effective group `<group>`, normalized artifact base name `<base>`, and
OpenAPI `info.version` `<version>`, exactly these external artifacts exist:

| Boundary | Maven coordinate |
|---|---|
| Model | `<group>:<base>-jdk21-model:<version>` |
| API | `<group>:<base>-jdk21-api:<version>` |
| Client | `<group>:<base>-jdk21-client:<version>` |

`info.version` is the Maven version. A Gradle version override may be omitted
or equal to `info.version`; a divergent value is invalid. Released coordinates
are immutable.

The existing Gradle artifact-kind names, aggregate task names, and embedded
`artifact.kind` values remain internal compatibility identifiers. They do not
change the external Maven suffixes defined here.

### Java Package Identity

`basePackage` is the unversioned package root. The major component of
`info.version` is appended as `v<major>`:

| Boundary | Effective package |
|---|---|
| Model | `<basePackage>.v<major>.model` |
| API | `<basePackage>.v<major>.api` |
| Spring HTTP Interface client | `<basePackage>.v<major>.client.httpinterface` |
| Spring Cloud OpenFeign client | `<basePackage>.v<major>.client.openfeign` |

For example, `info.version: 1.8.4` uses `.v1`, while
`info.version: 2.1.0` uses `.v2`. Minor, patch, pre-release, and build metadata
do not change the package segment.

Kind-specific package overrides are compatibility inputs, not alternate package
schemes. When configured, they must resolve to the effective structure above or
validation rejects them. New contracts should configure only `basePackage`.

### Operation Type Naming

OpenAPI tags define the generated API family. A stable tag such as
`KycProfile` produces `KycProfileApi`, `KycProfileApiController`, and
`KycProfileApiDelegate` in the API package, plus a `KycProfileApi` interface in
each client package. An operation without tags belongs to the `DefaultApi`
family in the API and both client packages.
`operationId` remains mandatory and unique. Changing an operation tag moves its
public Java types and is therefore a breaking contract change.

### Artifact Contents And Dependencies

The model JAR owns all generated transport models used by the API and client
boundaries. Both the API and client Maven publications declare the matching
model artifact as a transitive dependency at the same group and version.

The API JAR contains generated `*Api`, `*ApiController`, and `*ApiDelegate`
types plus `ApiUtil`. It contains no model duplicates or business
implementation.

The client JAR contains interfaces only. Its two reserved package families are
Spring HTTP Interface and Spring Cloud OpenFeign; it contains no HTTP runtime,
endpoint configuration, credentials, or model duplicates. OpenFeign libraries
are compile-time generation inputs only and are not exposed transitively.
The HTTP Interface and OpenFeign variants use the same simple interface names
in their distinct packages. OpenFeign interfaces carry `@FeignClient` directly;
their context IDs include the API major and interface name. The JAR does not
generate `*ApiClient` wrappers or `ClientConfiguration`.

## Delivery Sequence

Phase 1 freezes and validates the identity, coordinate, package, and dependency
contract in this ADR. Existing Spring HTTP Interface generation moves to its
reserved `client.httpinterface` package.

Phase 2 extends the plugin with the reserved client package inputs, the
OpenFeign compile-only toolchain, and the client annotation templates.

Phase 3 runs the two client generators and merges their interfaces into the
single `-jdk21-client` artifact, including both reserved client packages and
per-interface compatibility checks.

Phase 4 promotes contracts found in the existing conventional OpenAPI
directories to complete generated specifications. Explicit registrations take
precedence for the same file, so coordinate, package, and artifact overrides
remain available without producing duplicate publications.

Phase 5 adds repository-aware CI that detects changed contract files and runs
one isolated publication job per canonical project-relative path. Each job
publishes only that contract's three immutable artifacts to GitHub Packages.
Aggregate local and remote Maven publication remain available separately.

## Consequences

A contract major version creates a new Java namespace while compatible minor
and patch releases preserve imports. API and client consumers receive models
immediately through Maven dependency resolution. Applications that do not use
OpenFeign do not receive its runtime or starter, while OpenFeign applications
choose and manage the compatible Spring Cloud stack explicitly.

Coordinate or package changes after this decision require a migration plan and
a new architecture decision; they cannot silently replace published artifacts.
