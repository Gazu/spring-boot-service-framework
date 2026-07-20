package com.smbtech.serviceframework.openapi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenApiContractMetadataTest {

    @Test
    void rendersDeterministicContractProperties() {
        OpenApiSpecInfo specInfo =
                OpenApiSpecInfo.from(
                        Path.of("docs/openapi/merchant-order-status.yaml"),
                        "merchant-order-status",
                        "1.1.0");

        OpenApiContractMetadata metadata =
                OpenApiContractMetadata.from(
                        specInfo,
                        OpenApiArtifactKind.CLIENT,
                        "com.smbtech.openapi",
                        "abc123",
                        "0.2.0");

        String properties = metadata.toPropertiesString();

        assertTrue(properties.contains("openapi.title=merchant-order-status\n"));
        assertTrue(properties.contains("openapi.artifact.client=merchant-order-status-client\n"));
        assertTrue(properties.contains("openapi.artifact.kind=client\n"));
        assertTrue(properties.contains("openapi.generator.version=0.2.0\n"));
    }

    @Test
    void rendersPropertiesInKeyOrderAndEscapesValues() {
        OpenApiContractMetadata metadata =
                new OpenApiContractMetadata(
                        Map.of(
                                "zeta", "line\nvalue",
                                "alpha", "tab\tvalue",
                                "middle", "slash\\value"));

        assertEquals(
                """
                        alpha=tab\\tvalue
                        middle=slash\\\\value
                        zeta=line\\nvalue
                        """,
                metadata.toPropertiesString());
    }

    @Test
    void rejectsBlankRequiredMetadataInputs() {
        OpenApiSpecInfo specInfo =
                OpenApiSpecInfo.from(
                        Path.of("docs/openapi/merchant-order-status.yaml"),
                        "merchant-order-status",
                        "1.1.0");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        OpenApiContractMetadata.from(
                                specInfo, OpenApiArtifactKind.MODELS, " ", "abc123", "0.2.0"));
    }
}
