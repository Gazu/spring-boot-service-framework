package com.smbtech.serviceframework.openapi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiSpecInfoTest {

    @Test
    void derivesArtifactBaseNameAndBasePackageFromTitle() {
        OpenApiSpecInfo specInfo =
                OpenApiSpecInfo.from(
                        Path.of("docs/openapi/../openapi/merchant-order-status.yaml"),
                        "Merchant Order Status",
                        "1.1.0");

        assertEquals(Path.of("docs/openapi/merchant-order-status.yaml"), specInfo.source());
        assertEquals("merchant-order-status", specInfo.artifactBaseName());
        assertEquals("com.smbtech.openapi.merchantorderstatus", specInfo.basePackage());
    }

    @Test
    void rejectsBlankVersion() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        OpenApiSpecInfo.from(
                                Path.of("docs/openapi/merchant-order-status.yaml"),
                                "Merchant Order Status",
                                " "));
    }
}
