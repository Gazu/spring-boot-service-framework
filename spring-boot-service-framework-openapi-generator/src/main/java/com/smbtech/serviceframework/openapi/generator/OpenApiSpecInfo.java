package com.smbtech.serviceframework.openapi.generator;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Carries immutable open api spec info data.
 *
 * @param source source value
 * @param title title value
 * @param version version value
 * @param artifactBaseName artifact base name value
 */
public record OpenApiSpecInfo(Path source, String title, String version, String artifactBaseName) {

    /** Creates and validates the record components. */
    public OpenApiSpecInfo {
        source = Objects.requireNonNull(source, "source").normalize();
        title = requireText(title, "title");
        version = requireText(version, "version");
        artifactBaseName = requireText(artifactBaseName, "artifactBaseName");
    }

    /**
     * Creates the result.
     *
     * @param source source value
     * @param title title value
     * @param version version value
     * @return from result
     */
    public static OpenApiSpecInfo from(Path source, String title, String version) {
        return new OpenApiSpecInfo(
                source, title, version, new OpenApiNameNormalizer().normalizeAndValidate(title));
    }

    /**
     * Performs the base package operation.
     *
     * @return base package result
     */
    public String basePackage() {
        return "com.smbtech.openapi." + artifactBaseName.replace("-", "");
    }

    private static String requireText(String value, String name) {
        String safeValue = Objects.requireNonNull(value, name).trim();
        if (safeValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return safeValue;
    }
}
