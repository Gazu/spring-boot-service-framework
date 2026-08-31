# Pre-1.0 API Policy

The framework may make intentional incompatible changes before `1.0.0`, but
supported APIs cannot change silently. Source boundaries, nullability, binary
compatibility, properties, and Spring Boot imports are reviewed independently.

## Supported Surface

Supported packages and type exceptions are defined by
[Public API Boundaries](public-api-boundaries.md) and generated into the
[Public API Inventory](public-api-inventory.md). Public adapters,
auto-configurations, serializers, internal classes, and Gradle task
implementations remain unsupported infrastructure unless explicitly listed.

The reviewed file `gradle/compatibility/public-internal-types.txt` freezes
technically public implementation classes. New entries fail the build. Reducing
visibility is encouraged and requires regenerating the file so the reduction
is visible during review.

The reviewed file `gradle/compatibility/public-type-classification.txt` freezes
the classification of every top-level public type as supported API, supported
extension point, technical infrastructure, or internal implementation. The
build fails when a type is added, removed, or reclassified without updating the
baseline explicitly.

The reviewed file `gradle/compatibility/concrete-replaceable-beans.txt` freezes
legacy `@ConditionalOnMissingBean` methods that expose concrete return types.
New entries are prohibited; the baseline is regenerated only after one or more
concrete returns have been replaced by supported contracts.

## Nullability

Supported packages use JSpecify `@NullMarked`. A type is non-null unless a
component, parameter, return, or type argument is annotated `@Nullable`.
`validatePublicApiNullability` ensures every supported package has a package
default. JSpecify is compile-time API metadata and does not add runtime
validation.

## Binary Compatibility

`binaryCompatibilityCheck` builds the Git tag configured by
`binaryCompatibilityBaselineVersion` and compares each current published JAR
with japicmp. Only supported packages and documented type exceptions are
checked; implementation classes are intentionally excluded from the binary
promise.

Run:

```bash
./gradlew binaryCompatibilityCheck
```

Approved pre-1.0 incompatibilities live in
`gradle/compatibility/binary-breaking-changes.txt`. Every entry must identify a
single japicmp member, have release notes, and have a consumer replacement in
the [Pre-1.0 Migration Guide](guides/migrate-public-names-and-properties.md).
Entries must be removed when the baseline advances beyond the affected
release.

Visibility reductions for unsupported implementation types do not require a
binary exception. When consumers may have imported such a type directly, the
migration guide must still document the supported replacement or state that no
replacement exists.

## JWT Bearer Extension Contract

`AccessTokenClient.jwtBearer(JwtBearerTokenRequest)` is the canonical extension
method. It carries expected scopes and dynamic claims together. String-based
overloads remain convenience defaults that construct this request. Application
implementations must implement the complete request method.

## Change Workflow

1. Modify the supported API and its consumer tests.
2. Run `generatePublicApiInventory` and review the source surface.
3. Run `generatePublicInternalTypeBaseline` only when technical visibility was
   intentionally reduced or reviewed.
4. Run `generatePublicTypeClassificationBaseline` only after reviewing every
   public type classification change.
5. Run `generateConcreteReplaceableBeanBaseline` only to record a reduction.
6. Run `binaryCompatibilityCheck` against the latest released tag.
7. Document every incompatible change in `CHANGELOG.md` and the
   [Pre-1.0 Migration Guide](guides/migrate-public-names-and-properties.md).
8. Add the narrowest possible binary exception only during `0.x`.
9. Run `clean releaseGate` before publishing.

After `1.0.0`, supported binary breaks require a major framework version and
must not be handled by adding a routine allowlist entry.
