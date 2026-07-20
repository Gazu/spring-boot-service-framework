package com.smbtech.serviceframework.openapi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenApiSpecReaderTest {

    @TempDir Path tempDir;

    private final OpenApiSpecReader reader = new OpenApiSpecReader();

    @Test
    void readsYamlInfoBlock() throws Exception {
        Path source = tempDir.resolve("merchant-order-status.yaml");
        Files.writeString(
                source,
                """
                openapi: 3.0.3
                info:
                  title: Merchant Order Status
                  version: '1.1.0'
                paths: {}
                """);

        OpenApiSpecInfo info = reader.read(source);

        assertEquals("Merchant Order Status", info.title());
        assertEquals("1.1.0", info.version());
        assertEquals("merchant-order-status", info.artifactBaseName());
        assertEquals("com.smbtech.openapi.merchantorderstatus", info.basePackage());
    }

    @Test
    void readsJsonInfoBlock() throws Exception {
        Path source = tempDir.resolve("merchant-order-status.json");
        Files.writeString(
                source,
                """
                {
                  "openapi": "3.0.3",
                  "info": {
                    "title": "merchant-order-status",
                    "version": "1.1.0"
                  },
                  "paths": {}
                }
                """);

        OpenApiSpecInfo info = reader.read(source);

        assertEquals("merchant-order-status", info.title());
        assertEquals("1.1.0", info.version());
        assertEquals("merchant-order-status", info.artifactBaseName());
    }

    @Test
    void readsYamlInfoBlockWithCommentsAndTabs() throws Exception {
        Path source = tempDir.resolve("retail-loyalty-rewards.yaml");
        Files.writeString(
                source,
                """
                openapi: 3.0.3
                info: # contract metadata
                \ttitle: "Retail Loyalty Rewards" # display name
                \tversion: "2.0.1"
                paths: {}
                """);

        OpenApiSpecInfo info = reader.read(source);

        assertEquals("Retail Loyalty Rewards", info.title());
        assertEquals("2.0.1", info.version());
        assertEquals("retail-loyalty-rewards", info.artifactBaseName());
    }

    @Test
    void rejectsSpecsWithoutInfoTitle() throws Exception {
        Path source = tempDir.resolve("missing-title.yaml");
        Files.writeString(
                source,
                """
                openapi: 3.0.3
                info:
                  version: '1.0.0'
                paths: {}
                """);

        assertThrows(IllegalArgumentException.class, () -> reader.read(source));
    }

    @Test
    void calculatesStableSha256() throws Exception {
        Path source = tempDir.resolve("contract.yaml");
        Files.writeString(source, "openapi: 3.0.3\n");

        assertEquals(
                "faa4988e76ddd0d66e9c95e3d7da6faecc44be96bf988ac35b22dd39213ff509",
                reader.sha256(source));
    }
}
