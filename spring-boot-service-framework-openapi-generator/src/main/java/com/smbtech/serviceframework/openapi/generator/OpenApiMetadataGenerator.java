package com.smbtech.serviceframework.openapi.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Provides open api metadata generator behavior. */
public final class OpenApiMetadataGenerator {
    /** Creates an OpenAPI metadata generator instance. */
    public OpenApiMetadataGenerator() {}

    /**
     * Performs the write operation.
     *
     * @param outputRoot output root value
     * @param specInfo spec info value
     * @param artifactKind artifact kind value
     * @param metadata metadata value
     * @return write result
     * @throws IOException when the operation cannot be completed
     */
    public Path write(
            Path outputRoot,
            OpenApiSpecInfo specInfo,
            OpenApiArtifactKind artifactKind,
            OpenApiContractMetadata metadata)
            throws IOException {
        Objects.requireNonNull(outputRoot, "outputRoot");
        Objects.requireNonNull(specInfo, "specInfo");
        Objects.requireNonNull(artifactKind, "artifactKind");
        Objects.requireNonNull(metadata, "metadata");

        Path target =
                outputRoot
                        .resolve(specInfo.artifactBaseName())
                        .resolve(artifactKind.classifier())
                        .resolve(OpenApiContractMetadata.METADATA_ENTRY);
        Files.createDirectories(target.getParent());
        Files.writeString(target, metadata.toPropertiesString(), StandardCharsets.UTF_8);
        return target;
    }
}
