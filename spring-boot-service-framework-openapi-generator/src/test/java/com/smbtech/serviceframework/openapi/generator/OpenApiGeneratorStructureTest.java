package com.smbtech.serviceframework.openapi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiGeneratorStructureTest {

    private final OpenApiSpecInfo specInfo =
            OpenApiSpecInfo.from(
                    Path.of("docs/openapi/merchant-order-status.yaml"),
                    "merchant-order-status",
                    "1.1.0");

    @Test
    void exposesExpectedArtifactDescriptors() {
        assertEquals(
                "merchant-order-status-models",
                OpenApiPublicationDescriptor.from(
                                specInfo, "com.smbtech.openapi", OpenApiArtifactKind.MODELS)
                        .artifactId());
        assertEquals(
                "merchant-order-status-api",
                OpenApiPublicationDescriptor.from(
                                specInfo, "com.smbtech.openapi", OpenApiArtifactKind.SERVER_API)
                        .artifactId());
        assertEquals(
                "merchant-order-status-client",
                OpenApiPublicationDescriptor.from(
                                specInfo, "com.smbtech.openapi", OpenApiArtifactKind.CLIENT)
                        .artifactId());
    }

    @Test
    void exposesArtifactKindNamingContract() {
        assertEquals("models", OpenApiArtifactKind.MODELS.classifier());
        assertEquals("api", OpenApiArtifactKind.SERVER_API.classifier());
        assertEquals("client", OpenApiArtifactKind.CLIENT.classifier());
        assertEquals(
                "merchant-order-status-api",
                OpenApiArtifactKind.SERVER_API.artifactId("merchant-order-status"));
    }

    @Test
    void separatesGeneratorPackageTargets() {
        assertEquals(
                "com.smbtech.openapi.merchantorderstatus.model",
                new OpenApiModelGenerator().packageName(specInfo));
        assertEquals(
                "com.smbtech.openapi.merchantorderstatus.api",
                new OpenApiServerApiGenerator().packageName(specInfo));
        assertEquals(
                "com.smbtech.openapi.merchantorderstatus.client",
                new OpenApiClientGenerator().packageName(specInfo));
    }

    @Test
    void rejectsInvalidPublicationDescriptorInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new OpenApiPublicationDescriptor(
                                "com.smbtech.openapi", " ", "1.1.0", OpenApiArtifactKind.MODELS));
    }
}
