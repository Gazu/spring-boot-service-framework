package com.smbtech.serviceframework.openapi.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Carries immutable open api compatibility report data.
 *
 * @param baseline baseline value
 * @param current current value
 * @param changes changes value
 * @param versionPolicyViolations version policy violations value
 */
public record OpenApiCompatibilityReport(
        OpenApiSpecInfo baseline,
        OpenApiSpecInfo current,
        List<OpenApiChange> changes,
        List<String> versionPolicyViolations) {

    /** Creates and validates the record components. */
    public OpenApiCompatibilityReport {
        baseline = Objects.requireNonNull(baseline, "baseline");
        current = Objects.requireNonNull(current, "current");
        changes = List.copyOf(Objects.requireNonNull(changes, "changes"));
        versionPolicyViolations =
                List.copyOf(
                        Objects.requireNonNull(versionPolicyViolations, "versionPolicyViolations"));
    }

    /**
     * Creates the result.
     *
     * @param baseline baseline value
     * @param current current value
     * @param changes changes value
     * @return create result
     */
    public static OpenApiCompatibilityReport create(
            OpenApiSpecInfo baseline, OpenApiSpecInfo current, List<OpenApiChange> changes) {
        OpenApiSemanticVersion previous = OpenApiSemanticVersion.parse(baseline.version());
        OpenApiSemanticVersion next = OpenApiSemanticVersion.parse(current.version());
        List<String> violations = new ArrayList<>();

        if (next.compareTo(previous) <= 0) {
            violations.add(
                    "current info.version "
                            + current.version()
                            + " must be greater than baseline version "
                            + baseline.version());
        }
        boolean hasBreaking =
                changes.stream()
                        .anyMatch(change -> change.severity() == OpenApiChangeSeverity.BREAKING);
        boolean hasNonBreaking =
                changes.stream()
                        .anyMatch(
                                change -> change.severity() == OpenApiChangeSeverity.NON_BREAKING);
        if (hasBreaking && !next.isMajorIncreaseFrom(previous)) {
            violations.add(
                    "breaking changes require a major version increase from "
                            + baseline.version()
                            + " to at least "
                            + (previous.major() + 1)
                            + ".0.0");
        } else if (!hasBreaking && hasNonBreaking && !next.isMinorOrMajorIncreaseFrom(previous)) {
            violations.add(
                    "non-breaking API additions require a minor or major version increase from "
                            + baseline.version());
        }
        return new OpenApiCompatibilityReport(baseline, current, changes, violations);
    }

    /**
     * Reports whether breaking changes.
     *
     * @return has breaking changes result
     */
    public boolean hasBreakingChanges() {
        return changes.stream()
                .anyMatch(change -> change.severity() == OpenApiChangeSeverity.BREAKING);
    }

    /**
     * Performs the version policy valid operation.
     *
     * @return version policy valid result
     */
    public boolean versionPolicyValid() {
        return versionPolicyViolations.isEmpty();
    }
}
