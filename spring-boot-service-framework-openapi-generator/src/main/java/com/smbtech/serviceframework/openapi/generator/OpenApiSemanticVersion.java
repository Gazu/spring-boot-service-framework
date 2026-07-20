package com.smbtech.serviceframework.openapi.generator;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Carries immutable open api semantic version data.
 *
 * @param major major value
 * @param minor minor value
 * @param patch patch value
 * @param qualifier qualifier value
 */
public record OpenApiSemanticVersion(int major, int minor, int patch, String qualifier)
        implements Comparable<OpenApiSemanticVersion> {

    private static final Pattern PATTERN =
            Pattern.compile(
                    "^(?<major>0|[1-9][0-9]*)\\.(?<minor>0|[1-9][0-9]*)\\.(?<patch>0|[1-9][0-9]*)"
                            + "(?<qualifier>[-+][0-9A-Za-z][0-9A-Za-z.-]*)?$");

    /** Creates and validates the record components. */
    public OpenApiSemanticVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("semantic version numbers must not be negative");
        }
        qualifier = qualifier == null ? "" : qualifier;
    }

    /**
     * Performs the parse operation.
     *
     * @param value semantic version text
     * @return parse result
     */
    public static OpenApiSemanticVersion parse(String value) {
        Matcher matcher = PATTERN.matcher(Objects.requireNonNull(value, "value"));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("invalid semantic version: " + value);
        }
        return new OpenApiSemanticVersion(
                Integer.parseInt(matcher.group("major")),
                Integer.parseInt(matcher.group("minor")),
                Integer.parseInt(matcher.group("patch")),
                matcher.group("qualifier"));
    }

    @Override
    public int compareTo(OpenApiSemanticVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) {
            result = Integer.compare(minor, other.minor);
        }
        if (result == 0) {
            result = Integer.compare(patch, other.patch);
        }
        return result == 0 ? compareQualifier(qualifier, other.qualifier) : result;
    }

    /**
     * Reports whether major increase from.
     *
     * @param previous previous value
     * @return is major increase from result
     */
    public boolean isMajorIncreaseFrom(OpenApiSemanticVersion previous) {
        return major > previous.major;
    }

    /**
     * Reports whether minor or major increase from.
     *
     * @param previous previous value
     * @return is minor or major increase from result
     */
    public boolean isMinorOrMajorIncreaseFrom(OpenApiSemanticVersion previous) {
        return major > previous.major || (major == previous.major && minor > previous.minor);
    }

    private static int compareQualifier(String left, String right) {
        String leftPreRelease = preRelease(left);
        String rightPreRelease = preRelease(right);
        if (leftPreRelease.equals(rightPreRelease)) {
            return 0;
        }
        if (leftPreRelease.isEmpty()) {
            return 1;
        }
        if (rightPreRelease.isEmpty()) {
            return -1;
        }
        String[] leftParts = leftPreRelease.split("\\.");
        String[] rightParts = rightPreRelease.split("\\.");
        int length = Math.min(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            String leftPart = leftParts[index];
            String rightPart = rightParts[index];
            if (leftPart.equals(rightPart)) {
                continue;
            }
            boolean leftNumeric = leftPart.matches("[0-9]+");
            boolean rightNumeric = rightPart.matches("[0-9]+");
            if (leftNumeric && rightNumeric) {
                return new java.math.BigInteger(leftPart)
                        .compareTo(new java.math.BigInteger(rightPart));
            }
            if (leftNumeric != rightNumeric) {
                return leftNumeric ? -1 : 1;
            }
            return leftPart.compareTo(rightPart);
        }
        return Integer.compare(leftParts.length, rightParts.length);
    }

    private static String preRelease(String qualifier) {
        if (!qualifier.startsWith("-")) {
            return "";
        }
        int buildIndex = qualifier.indexOf('+');
        return qualifier.substring(1, buildIndex < 0 ? qualifier.length() : buildIndex);
    }
}
