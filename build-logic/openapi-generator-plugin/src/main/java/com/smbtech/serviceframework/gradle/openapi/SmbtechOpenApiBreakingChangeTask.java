package com.smbtech.serviceframework.gradle.openapi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.output.MarkdownRender;

/** Compares configured contracts with immutable baselines and enforces SemVer. */
public abstract class SmbtechOpenApiBreakingChangeTask extends DefaultTask {

    /** Creates the breaking change task. */
    public SmbtechOpenApiBreakingChangeTask() {}

    /**
     * Returns the configured OpenAPI documents.
     *
     * @return configured OpenAPI documents
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSpecFiles();

    /**
     * Returns the versioned baseline directory.
     *
     * @return versioned baseline directory
     */
    @Internal
    public abstract DirectoryProperty getBaselineDirectory();

    /**
     * Returns the baseline path task input.
     *
     * @return baseline directory path
     */
    @Input
    @Optional
    public abstract Property<String> getBaselineDirectoryPath();

    /**
     * Reports whether the current baseline is mandatory.
     *
     * @return baseline requirement
     */
    @Input
    public abstract Property<Boolean> getRequireBaseline();

    /**
     * Reports whether every incompatible diff fails.
     *
     * @return strict diff policy
     */
    @Input
    public abstract Property<Boolean> getFailOnBreakingChanges();

    /**
     * Returns the Markdown report directory.
     *
     * @return report directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getReportDirectory();

    /** Executes baseline and structural compatibility validation. */
    @TaskAction
    public void compareContracts() {
        Path reportRoot = getReportDirectory().get().getAsFile().toPath();
        Path baselineRoot = getBaselineDirectory().get().getAsFile().toPath();
        List<String> failures = new ArrayList<>();
        try {
            recreate(reportRoot);
            List<File> specs =
                    getSpecFiles().getFiles().stream()
                            .filter(File::isFile)
                            .sorted(Comparator.comparing(File::getPath))
                            .toList();
            for (File spec : specs) {
                compare(spec.toPath(), baselineRoot, reportRoot, failures);
            }
            Files.writeString(
                    reportRoot.resolve("summary.txt"),
                    failures.isEmpty()
                            ? "OpenAPI compatibility checks passed\n"
                            : String.join("\n", failures) + "\n",
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException(
                    "Cannot execute OpenAPI breaking change detection", exception);
        }
        if (!failures.isEmpty()) {
            throw new GradleException(
                    "OpenAPI breaking change issues found:\n- " + String.join("\n- ", failures));
        }
    }

    private void compare(Path current, Path baselineRoot, Path reportRoot, List<String> failures)
            throws IOException {
        OpenApiContractIdentity identity = OpenApiContractReader.read(current.toFile());
        Path exact = OpenApiCompatibilitySupport.exactBaseline(baselineRoot, identity).orElse(null);
        if (exact == null) {
            if (getRequireBaseline().get()) {
                failures.add(
                        identity.artifactBaseName()
                                + ": missing exact baseline for version "
                                + identity.version());
            }
        } else if (!OpenApiCompatibilitySupport.sha256(exact)
                .equals(OpenApiCompatibilitySupport.sha256(current))) {
            failures.add(
                    identity.artifactBaseName()
                            + ": current contract differs from its immutable "
                            + identity.version()
                            + " baseline");
        }

        Path previous =
                OpenApiCompatibilitySupport.previousBaseline(baselineRoot, identity).orElse(null);
        if (previous == null) {
            Files.writeString(
                    reportRoot.resolve(identity.artifactBaseName() + ".md"),
                    "# " + identity.title() + "\n\nNo earlier baseline was found.\n",
                    StandardCharsets.UTF_8);
            return;
        }

        OpenApiContractIdentity previousIdentity = OpenApiContractReader.read(previous.toFile());
        ChangedOpenApi difference =
                OpenApiCompare.fromLocations(previous.toString(), current.toString());
        try (OutputStreamWriter writer =
                new OutputStreamWriter(
                        Files.newOutputStream(
                                reportRoot.resolve(identity.artifactBaseName() + ".md")),
                        StandardCharsets.UTF_8)) {
            new MarkdownRender().render(difference, writer);
        }

        var previousVersion = OpenApiCompatibilitySupport.version(previousIdentity.version());
        var currentVersion = OpenApiCompatibilitySupport.version(identity.version());
        if (difference.isIncompatible()) {
            if (getFailOnBreakingChanges().get()) {
                failures.add(
                        identity.artifactBaseName() + ": strict mode rejects breaking changes");
            } else if (!currentVersion.isMajorIncreaseFrom(previousVersion)) {
                failures.add(
                        identity.artifactBaseName()
                                + ": breaking changes require a major version increase from "
                                + previousIdentity.version());
            }
        } else if (difference.isDifferent()
                && !currentVersion.isMinorOrMajorIncreaseFrom(previousVersion)) {
            failures.add(
                    identity.artifactBaseName()
                            + ": compatible additions require a minor or major version increase from "
                            + previousIdentity.version());
        }
    }

    private static void recreate(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(directory);
    }
}
