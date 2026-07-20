package com.smbtech.serviceframework.openapi.generator;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** Provides open api jar packager behavior. */
public final class OpenApiJarPackager {
    /** Creates an OpenAPI jar packager instance. */
    public OpenApiJarPackager() {}

    /** Timestamp assigned to every archive entry to produce byte-for-byte reproducible JARs. */
    public static final long REPRODUCIBLE_ENTRY_TIME_MILLIS =
            Instant.parse("2000-01-01T00:00:00Z").toEpochMilli();

    /**
     * Performs the package jar operation.
     *
     * @param jarFile jar file value
     * @param roots roots value
     * @throws IOException when the operation cannot be completed
     */
    public void packageJar(Path jarFile, List<Path> roots) throws IOException {
        Objects.requireNonNull(jarFile, "jarFile");
        Objects.requireNonNull(roots, "roots");
        if (jarFile.getParent() != null) {
            Files.createDirectories(jarFile.getParent());
        }
        Files.deleteIfExists(jarFile);

        try (JarOutputStream jar =
                new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jarFile)))) {
            Set<String> writtenEntries = new HashSet<>();
            addEntry(
                    jar,
                    writtenEntries,
                    "META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0\r\n"
                            + "Created-By: spring-boot-service-framework-openapi\r\n\r\n");

            for (Path root : roots) {
                if (!Files.isDirectory(root)) {
                    continue;
                }
                try (var stream = Files.walk(root)) {
                    List<Path> files =
                            stream.filter(Files::isRegularFile)
                                    .sorted(
                                            Comparator.comparing(
                                                    path ->
                                                            root.relativize(path)
                                                                    .toString()
                                                                    .replace('\\', '/')))
                                    .toList();
                    for (Path file : files) {
                        String entryName = root.relativize(file).toString().replace('\\', '/');
                        addEntry(jar, writtenEntries, entryName, Files.readAllBytes(file));
                    }
                }
            }
        }
    }

    private static void addEntry(
            JarOutputStream jar, Set<String> writtenEntries, String entryName, String content)
            throws IOException {
        addEntry(
                jar,
                writtenEntries,
                entryName,
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void addEntry(
            JarOutputStream jar, Set<String> writtenEntries, String entryName, byte[] content)
            throws IOException {
        if (!writtenEntries.add(entryName)) {
            throw new IOException("duplicate JAR entry " + entryName);
        }
        JarEntry entry = new JarEntry(entryName);
        entry.setTime(REPRODUCIBLE_ENTRY_TIME_MILLIS);
        jar.putNextEntry(entry);
        jar.write(content);
        jar.closeEntry();
    }
}
