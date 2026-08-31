package com.smbtech.serviceframework.project.generator;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

final class NameSupport {

    private static final Pattern PACKAGE_NAME =
            Pattern.compile("[a-z_$][a-z0-9_$]*(?:\\.[a-z_$][a-z0-9_$]*)*");
    private static final Pattern JAVA_TYPE = Pattern.compile("[A-Z_$][A-Za-z0-9_$]*");
    private static final Pattern ARTIFACT = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private NameSupport() {}

    static String normalizeArtifact(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_.]+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    static String compact(String artifact) {
        return artifact.replace("-", "");
    }

    static String javaType(String value) {
        String type =
                Arrays.stream(value.split("[^A-Za-z0-9]+"))
                        .filter(part -> !part.isBlank())
                        .map(NameSupport::capitalize)
                        .reduce("", String::concat);
        if (!type.isEmpty() && Character.isDigit(type.charAt(0))) {
            type = "Contract" + type;
        }
        return type;
    }

    static void requirePackage(String value, String field) {
        if (!PACKAGE_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    field + " must be a valid lower-case Java package: " + value);
        }
    }

    static void requireJavaType(String value, String field) {
        if (!JAVA_TYPE.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a valid Java type name: " + value);
        }
    }

    static void requireArtifact(String value, String field) {
        if (!ARTIFACT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    field + " must be a normalized Maven artifact name: " + value);
        }
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
