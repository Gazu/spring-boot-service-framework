package com.smbtech.serviceframework.openapi.generator;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Carries immutable open api contract metadata data.
 *
 * @param values values value
 */
public record OpenApiContractMetadata(Map<String, String> values) {

    public static final String METADATA_ENTRY = "META-INF/smbtech/openapi/contract.properties";

    /** Creates and validates the record components. */
    public OpenApiContractMetadata {
        TreeMap<String, String> sorted = new TreeMap<>();
        Objects.requireNonNull(values, "values")
                .forEach(
                        (key, value) ->
                                sorted.put(
                                        requireText(key, "metadata key"),
                                        value == null ? "" : value));
        values = Map.copyOf(sorted);
    }

    /**
     * Creates the result.
     *
     * @param specInfo spec info value
     * @param artifactKind artifact kind value
     * @param groupId group id value
     * @param sourceSha256 source sha256 value
     * @param generatorVersion generator version value
     * @return from result
     */
    public static OpenApiContractMetadata from(
            OpenApiSpecInfo specInfo,
            OpenApiArtifactKind artifactKind,
            String groupId,
            String sourceSha256,
            String generatorVersion) {
        Objects.requireNonNull(specInfo, "specInfo");
        Objects.requireNonNull(artifactKind, "artifactKind");
        TreeMap<String, String> values = new TreeMap<>();
        values.put(
                "openapi.artifact.api",
                OpenApiArtifactKind.SERVER_API.artifactId(specInfo.artifactBaseName()));
        values.put("openapi.artifact.base-name", specInfo.artifactBaseName());
        values.put(
                "openapi.artifact.client",
                OpenApiArtifactKind.CLIENT.artifactId(specInfo.artifactBaseName()));
        values.put("openapi.artifact.group-id", requireText(groupId, "groupId"));
        values.put("openapi.artifact.kind", artifactKind.classifier());
        values.put(
                "openapi.artifact.models",
                OpenApiArtifactKind.MODELS.artifactId(specInfo.artifactBaseName()));
        values.put("openapi.generator", "spring-boot-service-framework-openapi");
        values.put("openapi.generator.version", requireText(generatorVersion, "generatorVersion"));
        values.put("openapi.sha256", requireText(sourceSha256, "sourceSha256"));
        values.put("openapi.source", specInfo.source().toString());
        values.put("openapi.title", specInfo.title());
        values.put("openapi.version", specInfo.version());
        return new OpenApiContractMetadata(values);
    }

    /**
     * Performs the to properties string operation.
     *
     * @return to properties string result
     */
    public String toPropertiesString() {
        StringBuilder output = new StringBuilder();
        new TreeMap<>(values)
                .forEach(
                        (key, value) ->
                                output.append(key).append("=").append(escape(value)).append("\n"));
        return output.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String requireText(String value, String name) {
        String safeValue = Objects.requireNonNull(value, name).trim();
        if (safeValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return safeValue;
    }
}
