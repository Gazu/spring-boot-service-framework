# Code Conventions

These conventions define the naming and source-structure contract for new or
modified framework code. Existing public names remain compatible until an
explicit migration is approved and recorded in the changelog and
[Public API Inventory](public-api-inventory.md).

## General Naming

- Use English for type names, members, documentation, exceptions, logs, and
  test names.
- Use `UpperCamelCase` for types, `lowerCamelCase` for methods and variables,
  and `UPPER_SNAKE_CASE` for constants.
- Name a type after one responsibility. Avoid generic names such as `Helper`,
  `Utils`, `Manager`, or `Processor` unless that term precisely describes its
  contract.
- Name interfaces by capability without an `I` prefix, for example
  `AccessTokenClient` and `NotificationSanitizer`.
- Use a role suffix consistently:

| Suffix | Responsibility |
|---|---|
| `Factory` | Creates a configured object or aggregate. |
| `Provider` | Supplies a value, potentially from an external source. |
| `Resolver` | Selects or derives one result from context or candidates. |
| `Mapper` | Converts one representation into another without owning policy. |
| `Validator` | Returns or raises validation findings without performing work. |
| `Customizer` | Applies a small ordered mutation to framework-owned configuration. |
| `Contributor` | Adds optional data to an immutable or builder-based pipeline. |
| `Policy` | Encapsulates a replaceable decision. |
| `Writer` | Writes to an output boundary. |
| `Reporter` | Publishes diagnostics, logs, or metrics. |

- Prefer records for immutable data carriers when their invariants fit a compact
  canonical constructor.
- Avoid `Dto` in framework-owned domain names. Use it only when a type explicitly
  represents an external transport contract.
- Do not add `*Module` marker classes. A module boundary is represented by its
  Gradle artifact, `package-info.java`, and module README. Existing markers with
  confirmed external consumers may survive only until the next planned
  incompatible release; no new marker is allowed.

## Acronyms

Use normal Java word casing inside type and member names. Do not copy the fully
capitalized protocol spelling into `UpperCamelCase` identifiers.

| Concept | Type-name form | Member form | Package/property form | Examples |
|---|---|---|---|---|
| OAuth 2 | `OAuth2` | `oauth2` | `oauth2` | `OAuth2TokenRequestCustomizer`, `oauth2Metadata` |
| OpenAPI | `OpenApi` | `openApi` | `openapi` | `OpenApiContractLoader`, `openApiSpec` |
| HTTP | `Http` | `http` | `http` | `HttpClientDefinition`, `httpStatus` |
| JWT | `Jwt` | `jwt` | `jwt` | `JwtBearerClaimsContributor`, `jwtClaims` |

Constants may preserve the protocol acronym as a separate word, for example
`OAUTH2_ERROR`, `OPEN_API_SPEC`, `HTTP_STATUS`, and `JWT_BEARER_GRANT`.
Do not introduce variants such as `Oauth2`, `OpenAPI`, `HTTPClient`, or
`JWTBearer` in Java type names.

## Packages

All framework code uses the `com.smbtech.serviceframework` base package.
Package segments are lowercase, singular where practical, and never contain
underscores or implementation version numbers.

The normative supported/implementation classification and its documented
exceptions are defined in [Public API Boundaries](public-api-boundaries.md).

Core modules use capability-owned hexagonal packages:

```text
com.smbtech.serviceframework.<capability>.domain
com.smbtech.serviceframework.<capability>.port.in
com.smbtech.serviceframework.<capability>.port.out
com.smbtech.serviceframework.<capability>.service
com.smbtech.serviceframework.<capability>.exception
```

Starter modules use this boundary:

```text
com.smbtech.serviceframework.starter.<capability>.api
com.smbtech.serviceframework.starter.<capability>.api.<topic>
com.smbtech.serviceframework.starter.<capability>.adapter.in.<technology>
com.smbtech.serviceframework.starter.<capability>.adapter.out.<technology>
com.smbtech.serviceframework.starter.<capability>.autoconfigure
```

- Consumer extension contracts belong in `api`, core ports, or another package
  explicitly listed in the public API inventory.
- Spring, Servlet, Apache HttpClient, Jackson, Micrometer, SLF4J, persistence,
  and filesystem implementations belong in adapter packages.
- Configuration binding and Spring Boot bean assembly belong in
  `autoconfigure`; business rules and reusable algorithms do not.
- Keep implementation classes package-private whenever cross-package or
  framework access does not require `public`.
- A public implementation outside a supported package is infrastructure, not an
  extension point. It must be listed under `Public Internal Types To Review` in
  the public API inventory until its visibility or package is corrected.
- Dependencies point from starters and adapters toward API and core packages.
  Core modules must not import starter or adapter packages.

## Javadocs

- Every supported public top-level type must have Javadoc, including records,
  enums, annotations, and extension interfaces.
- Every public member that forms part of a consumer contract must document its
  behavior, parameters, return value, nullability expectations, and relevant
  exceptions. Overridden methods may inherit documentation when behavior is
  unchanged.
- Every public infrastructure type must explain why it is public. Public types
  that are not supported extension points must include an `@implNote` stating
  that they are framework infrastructure.
- Start with one sentence describing responsibility, for example:

```java
/**
 * Writes a notification as snake-case JSON without changing the application ObjectMapper.
 */
```

- Use `{@link Type}` for related contracts and `{@code value}` for literals.
- Use `@param`, `@return`, and `@throws` when they add contract information.
  Record components are documented with `@param` on the record Javadoc.
- Do not repeat a field, type, or method name, narrate obvious assignments,
  describe private implementation steps, or add `@author` tags.
- Internal comments are reserved for design decisions, security constraints,
  compatibility requirements, and non-obvious logic.
- Methods annotated with `@Override` inherit the parent contract and do not
  repeat it. Add implementation documentation only when the override introduces
  a meaningful policy, constraint, or compatibility behavior.
- Document thread safety, ordering, lifecycle, caching, or sensitive-data
  handling whenever consumers must understand those semantics.
- Generated OpenAPI sources follow generator templates and are excluded from
  manual Javadoc requirements.

## Automated Formatting

- `.editorconfig` defines UTF-8, line endings, indentation, final newlines, and
  trailing-whitespace rules for editors.
- `./gradlew spotlessApply` formats Java sources and supported project files.
- `./gradlew spotlessCheck` verifies formatting and runs as part of `check`.
- Java uses `google-java-format` in AOSP mode, deterministic import groups, no
  wildcard imports, and automatic removal of unused imports.
- OpenAPI sources under `build/generated/smbtech-openapi` are generated output
  and are excluded from formatting and format verification.

## Gradle Conventions

- Framework libraries apply
  `com.smbtech.service-framework.java-library`.
- Spring Boot starters apply
  `com.smbtech.service-framework.spring-boot-starter`.
- Module build files do not repeat Java toolchains, encoding, `-parameters`,
  JUnit, sources/Javadoc JARs, or publication repositories.
- `./gradlew conventionPluginsCheck` validates plugin compilation and module
  adoption. It runs through both `check` and `baseline`.

## Source Quality

- Do not introduce identifiers or file names containing the legacy terms
  `Requestor` or `TransactionalId`. Historical migration references are allowed
  only in documentation, comments, and string literals.
- Do not leave Java declarations, statements, annotations, or assignments
  commented out. Remove them and rely on version control.
- Every supported public package has a documented `package-info.java`.
- `./gradlew codeQualityCheck` aggregates formatting, public API Javadocs,
  public package documentation, naming rules, and commented-code detection.
- Changes to supported public types, extension points, properties,
  auto-configuration imports, or Gradle plugin ids must refresh and review
  `gradle/compatibility/contracts` with
  `./gradlew generateModuleCompatibilityContracts`.

## Exceptions

- Exception type names end in `Exception`. Error catalogs and definitions are
  not exceptions and must not use that suffix.
- Reuse an exception from the owning core module before creating a
  starter-specific or adapter-specific exception.
- Framework exceptions are unchecked unless a caller has a concrete,
  documented recovery action that requires a checked exception.
- Preserve the original cause. Do not catch an exception only to discard it or
  expose it verbatim.
- Diagnostic messages are written in English and describe the failed operation
  with safe identifiers. They must never include tokens, passwords, private
  keys, authorization headers, request bodies, or unfiltered downstream bodies.
- Public HTTP responses are produced through error definitions, resolvers, and
  sanitizers. Do not derive a public message from `Exception.getMessage()`.
- HTTP status selection belongs to a status resolver or error catalog, not to
  the exception class.
- Prefer focused factories when construction requires a stable error code,
  notification set, or diagnostic context. Avoid broad constructors containing
  unrelated nullable arguments.
- Never throw raw `RuntimeException` or declare `throws Exception` from a public
  framework contract when a meaningful framework or standard exception exists.
- Follow the canonical [exception selection guide](error-handling/exception-selection.md)
  when choosing between `ServiceException`, owning-module configuration or
  authentication exceptions, and downstream exceptions.

## Default Implementations

- Use `Default<ContractName>` only for the framework-provided implementation of
  a replaceable interface, for example `DefaultRequiredScopeResolver` for
  `RequiredScopeResolver`.
- Keep the interface in a supported API or core port package and the default
  implementation in the owning service or adapter package.
- Provide one canonical `Default*` implementation per contract. Additional
  implementations use a semantic technology or policy name such as
  `MicrometerErrorMetricsRecorder`, `Slf4jLogEventSink`, `PropertiesMockDefinitionSource`,
  or `GrantAwareOAuth2AuthorizedClientService`.
- Do not use `Impl`, `Default` for data objects, or `Default` merely to indicate
  default field values.
- Auto-configured default beans must back off with
  `@ConditionalOnMissingBean` so applications can replace the contract.
- Composition points accept ordered collections of contributors, customizers,
  resolvers, or reporters instead of embedding application-specific branches in
  the default implementation.

## Auto-Configuration

- Name the entry point `<Capability>AutoConfiguration` and place it in the
  starter's `autoconfigure` package.
- Name configuration binding types `<Capability>Properties`. Use nested property
  types only to mirror a cohesive property namespace.
- Register auto-configurations in
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Use Spring Boot auto-configuration annotations and conditions:
  `@AutoConfiguration`, `@EnableConfigurationProperties`,
  `@ConditionalOnClass`, `@ConditionalOnProperty`, and
  `@ConditionalOnMissingBean` as appropriate.
- Declare bean dependencies as method parameters. Do not fetch application beans
  from the context manually when dependency injection can express the contract.
- Keep bean methods small. Move parsing, mapping, validation, cryptography, and
  runtime algorithms to focused collaborators.
- Back off for every documented replacement point. Auto-configuration must
  assemble defaults without overriding application-provided beans.
- Use deterministic ordering for collections of customizers, contributors,
  resolvers, and reporters.
- Avoid static mutable state and application startup network calls. Optional
  content validation must be bounded and controlled by configuration.
- Keep helper classes package-private unless Spring, Gradle, configuration
  binding, or cross-package wiring requires public visibility.
- Do not create a starter marker class when the auto-configuration and artifact
  already identify the starter.

## Compatibility Workflow

When a change adds, removes, renames, or changes the kind of a public type:

1. Apply these conventions to the new API.
2. Update the owning module README or extension-point guide.
3. Run `./gradlew generatePublicApiInventory`.
4. Review the generated inventory, especially public internal types.
5. Record incompatible changes in `CHANGELOG.md` and follow the versioning rules.
6. Run `./gradlew check` and `./gradlew compatibilityCheck` before release.
