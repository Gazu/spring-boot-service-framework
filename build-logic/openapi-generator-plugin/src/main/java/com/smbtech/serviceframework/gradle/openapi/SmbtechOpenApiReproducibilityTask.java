package com.smbtech.serviceframework.gradle.openapi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Verifies deterministic ordering, timestamps, and hashes of generated contract JARs. */
public abstract class SmbtechOpenApiReproducibilityTask extends DefaultTask {

    /** Creates the reproducibility task. */
    public SmbtechOpenApiReproducibilityTask() {}

    /**
     * Returns generated binary and source JARs.
     *
     * @return generated JARs
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract ConfigurableFileCollection getArtifacts();

    /**
     * Returns the stable hash manifest.
     *
     * @return artifact hash manifest
     */
    @OutputFile
    public abstract RegularFileProperty getHashManifest();

    /** Validates archive reproducibility and writes deterministic SHA-256 values. */
    @TaskAction
    public void verifyArtifacts() {
        List<String> failures = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        List<File> artifacts =
                getArtifacts().getFiles().stream()
                        .filter(File::isFile)
                        .sorted(Comparator.comparing(File::getName))
                        .toList();
        for (File artifact : artifacts) {
            inspect(artifact, failures);
            hashes.add(
                    OpenApiCompatibilitySupport.sha256(artifact.toPath())
                            + "  "
                            + artifact.getName());
        }
        try {
            Files.createDirectories(getHashManifest().get().getAsFile().toPath().getParent());
            Files.writeString(
                    getHashManifest().get().getAsFile().toPath(),
                    String.join("\n", hashes) + "\n",
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot write OpenAPI reproducibility manifest", exception);
        }
        if (!failures.isEmpty()) {
            throw new GradleException(
                    "OpenAPI reproducibility issues found:\n- " + String.join("\n- ", failures));
        }
    }

    private static void inspect(File artifact, List<String> failures) {
        try (JarFile jar = new JarFile(artifact)) {
            var entries = jar.stream().toList();
            List<String> names = entries.stream().map(entry -> entry.getName()).toList();
            if (names.stream().distinct().count() != names.size()) {
                failures.add(artifact.getName() + ": contains duplicate entries");
            }
            long archiveTimestamp = entries.isEmpty() ? -1 : entries.getFirst().getTime();
            entries.forEach(
                    entry -> {
                        if (entry.getTime() != archiveTimestamp || entry.getTime() < 0) {
                            failures.add(
                                    artifact.getName()
                                            + ": "
                                            + entry.getName()
                                            + " has inconsistent timestamp "
                                            + entry.getTime());
                        }
                    });
        } catch (IOException exception) {
            failures.add(artifact.getName() + ": cannot inspect JAR: " + exception.getMessage());
        }
    }
}
