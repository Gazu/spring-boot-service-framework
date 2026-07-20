package com.smbtech.serviceframework.openapi.generator;

import java.util.Objects;

/**
 * Carries immutable open api publication descriptor data.
 *
 * @param groupId group id value
 * @param artifactId artifact id value
 * @param version version value
 * @param artifactKind artifact kind value
 */
public record OpenApiPublicationDescriptor(
        String groupId, String artifactId, String version, OpenApiArtifactKind artifactKind) {

    /** Creates and validates the record components. */
    public OpenApiPublicationDescriptor {
        groupId = requireText(groupId, "groupId");
        artifactId = requireText(artifactId, "artifactId");
        version = requireText(version, "version");
        artifactKind = Objects.requireNonNull(artifactKind, "artifactKind");
    }

    /**
     * Creates the result.
     *
     * @param specInfo spec info value
     * @param groupId group id value
     * @param artifactKind artifact kind value
     * @return from result
     */
    public static OpenApiPublicationDescriptor from(
            OpenApiSpecInfo specInfo, String groupId, OpenApiArtifactKind artifactKind) {
        Objects.requireNonNull(specInfo, "specInfo");
        Objects.requireNonNull(artifactKind, "artifactKind");
        return new OpenApiPublicationDescriptor(
                groupId,
                artifactKind.artifactId(specInfo.artifactBaseName()),
                specInfo.version(),
                artifactKind);
    }

    private static String requireText(String value, String name) {
        String safeValue = Objects.requireNonNull(value, name).trim();
        if (safeValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return safeValue;
    }
}
