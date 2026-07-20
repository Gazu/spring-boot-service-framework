package com.smbtech.serviceframework.openapi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenApiMetadataGeneratorTest {

    @TempDir Path tempDir;

    @Test
    void writesMetadataUnderArtifactKindDirectory() throws Exception {
        OpenApiSpecInfo specInfo =
                OpenApiSpecInfo.from(
                        Path.of("docs/openapi/merchant-order-status.yaml"),
                        "merchant-order-status",
                        "1.1.0");
        OpenApiContractMetadata metadata =
                OpenApiContractMetadata.from(
                        specInfo,
                        OpenApiArtifactKind.MODELS,
                        "com.smbtech.openapi",
                        "abc123",
                        "0.2.0");

        Path output =
                new OpenApiMetadataGenerator()
                        .write(tempDir, specInfo, OpenApiArtifactKind.MODELS, metadata);

        assertEquals(
                tempDir.resolve(
                        "merchant-order-status/models/META-INF/smbtech/openapi/contract.properties"),
                output);
        assertTrue(Files.readString(output).contains("openapi.artifact.kind=models"));
    }

    @Test
    void writesSeparateMetadataFilesForEachArtifactKind() throws Exception {
        OpenApiSpecInfo specInfo =
                OpenApiSpecInfo.from(
                        Path.of("docs/openapi/merchant-order-status.yaml"),
                        "merchant-order-status",
                        "1.1.0");
        OpenApiMetadataGenerator generator = new OpenApiMetadataGenerator();
        Map<OpenApiArtifactKind, Path> outputs = new EnumMap<>(OpenApiArtifactKind.class);

        for (OpenApiArtifactKind artifactKind : OpenApiArtifactKind.values()) {
            OpenApiContractMetadata metadata =
                    OpenApiContractMetadata.from(
                            specInfo, artifactKind, "com.smbtech.openapi", "abc123", "0.2.0");
            outputs.put(artifactKind, generator.write(tempDir, specInfo, artifactKind, metadata));
        }

        assertEquals(3, outputs.values().stream().distinct().count());
        assertTrue(
                Files.readString(outputs.get(OpenApiArtifactKind.MODELS))
                        .contains("openapi.artifact.kind=models"));
        assertTrue(
                Files.readString(outputs.get(OpenApiArtifactKind.SERVER_API))
                        .contains("openapi.artifact.kind=api"));
        assertTrue(
                Files.readString(outputs.get(OpenApiArtifactKind.CLIENT))
                        .contains("openapi.artifact.kind=client"));
    }
}
