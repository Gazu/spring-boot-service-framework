package com.smbtech.serviceframework.openapi.generator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenApiJarPackagerTest {

    @TempDir Path tempDir;

    @Test
    void writesStableJarEntries() throws Exception {
        Path root = tempDir.resolve("classes");
        Path source = root.resolve("com/example/Contract.class");
        Files.createDirectories(source.getParent());
        Files.write(source, new byte[] {0, 1, 2, 3});
        Path jar = tempDir.resolve("contract.jar");

        new OpenApiJarPackager().packageJar(jar, List.of(root));

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            assertNotNull(jarFile.getEntry("META-INF/MANIFEST.MF"));
            assertNotNull(jarFile.getEntry("com/example/Contract.class"));
            assertEquals(
                    OpenApiJarPackager.REPRODUCIBLE_ENTRY_TIME_MILLIS,
                    jarFile.getEntry("com/example/Contract.class").getTime());
        }
    }

    @Test
    void writesEntriesInStableOrderAndProducesSameBytes() throws Exception {
        Path firstRoot = tempDir.resolve("first/classes");
        Files.createDirectories(firstRoot.resolve("com/example"));
        Files.write(firstRoot.resolve("com/example/Zeta.class"), new byte[] {2});
        Files.write(firstRoot.resolve("com/example/Alpha.class"), new byte[] {1});
        Path firstJar = tempDir.resolve("first.jar");
        Path secondJar = tempDir.resolve("second.jar");

        OpenApiJarPackager packager = new OpenApiJarPackager();
        packager.packageJar(firstJar, List.of(firstRoot));
        packager.packageJar(secondJar, List.of(firstRoot));

        assertArrayEquals(Files.readAllBytes(firstJar), Files.readAllBytes(secondJar));
        try (JarFile jarFile = new JarFile(firstJar.toFile())) {
            List<String> entries =
                    Collections.list(jarFile.entries()).stream()
                            .map(entry -> entry.getName())
                            .toList();

            assertEquals(
                    List.of(
                            "META-INF/MANIFEST.MF",
                            "com/example/Alpha.class",
                            "com/example/Zeta.class"),
                    entries);
        }
    }

    @Test
    void rejectsDuplicateJarEntriesAcrossRoots() throws Exception {
        Path firstRoot = tempDir.resolve("first");
        Path secondRoot = tempDir.resolve("second");
        Files.createDirectories(firstRoot.resolve("com/example"));
        Files.createDirectories(secondRoot.resolve("com/example"));
        Files.write(firstRoot.resolve("com/example/Contract.class"), new byte[] {1});
        Files.write(secondRoot.resolve("com/example/Contract.class"), new byte[] {2});

        Exception exception =
                assertThrows(
                        java.io.IOException.class,
                        () ->
                                new OpenApiJarPackager()
                                        .packageJar(
                                                tempDir.resolve("duplicate.jar"),
                                                List.of(firstRoot, secondRoot)));

        assertTrue(
                exception.getMessage().contains("duplicate JAR entry com/example/Contract.class"));
    }
}
