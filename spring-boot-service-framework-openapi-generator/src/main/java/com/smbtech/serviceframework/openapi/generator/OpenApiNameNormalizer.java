package com.smbtech.serviceframework.openapi.generator;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Provides open api name normalizer behavior. */
public final class OpenApiNameNormalizer {
    /** Creates an OpenAPI name normalizer instance. */
    public OpenApiNameNormalizer() {}

    private static final Pattern VALID_ARTIFACT_BASE_NAME =
            Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    /**
     * Performs the normalize operation.
     *
     * @param title title value
     * @return normalize result
     */
    public String normalize(String title) {
        return Objects.requireNonNull(title, "title")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_.]+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-+", "-");
    }

    /**
     * Reports whether valid artifact base name.
     *
     * @param artifactBaseName artifact base name value
     * @return is valid artifact base name result
     */
    public boolean isValidArtifactBaseName(String artifactBaseName) {
        return artifactBaseName != null
                && VALID_ARTIFACT_BASE_NAME.matcher(artifactBaseName).matches();
    }

    /**
     * Performs the normalize and validate operation.
     *
     * @param title title value
     * @return normalize and validate result
     */
    public String normalizeAndValidate(String title) {
        String normalized = normalize(title);
        if (!isValidArtifactBaseName(normalized)) {
            throw new IllegalArgumentException(
                    "OpenAPI info.title normalizes to an invalid artifact base name: " + title);
        }
        return normalized;
    }
}
