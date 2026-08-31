package com.smbtech.serviceframework.gradle.openapi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Produces an explicit coordinate and task migration report for generated contracts. */
public abstract class SmbtechOpenApiMigrationReportTask extends DefaultTask {

    /** Creates the migration report task. */
    public SmbtechOpenApiMigrationReportTask() {}

    /**
     * Returns the default generated artifact group.
     *
     * @return default Maven group
     */
    @Input
    public abstract Property<String> getDefaultGroupId();

    /**
     * Returns configured contract descriptors.
     *
     * @return configured contract descriptors
     */
    @Input
    public abstract ListProperty<String> getSpecConfigurations();

    /**
     * Returns the migration report.
     *
     * @return Markdown migration report
     */
    @OutputFile
    public abstract org.gradle.api.file.RegularFileProperty getReportFile();

    /** Writes mappings from legacy coordinates and commands to plugin-native equivalents. */
    @TaskAction
    public void writeReport() {
        List<String> rows = new ArrayList<>();
        for (String configuration : getSpecConfigurations().getOrElse(List.of())) {
            String[] parts = configuration.split("\\|", -1);
            OpenApiContractIdentity identity = OpenApiContractReader.read(new File(parts[1]));
            String group = parts[2].isBlank() ? getDefaultGroupId().get() : parts[2];
            String artifact = parts[3].isBlank() ? identity.artifactBaseName() : parts[3];
            String version = parts[4].isBlank() ? identity.version() : parts[4];
            boolean models = parts.length <= 9 || Boolean.parseBoolean(parts[9]);
            boolean server = parts.length <= 10 || Boolean.parseBoolean(parts[10]);
            boolean client = parts.length <= 11 || Boolean.parseBoolean(parts[11]);
            if (models) {
                rows.add(row("models", artifact + "-models", group, artifact + "-models", version));
            }
            if (server) {
                rows.add(
                        row(
                                "server API",
                                artifact + "-api",
                                group,
                                artifact + "-server-api",
                                version));
            }
            if (client) {
                rows.add(row("client", artifact + "-client", group, artifact + "-client", version));
            }
        }
        String report =
                """
                # OpenAPI Migration Report

                | Kind | Legacy coordinate | Plugin-native coordinate |
                |---|---|---|
                %s

                ## Task Mapping

                | Legacy task | Plugin-native task |
                |---|---|
                | `generateOpenApiModels` | `smbtechOpenApiGenerateModels` |
                | `generateOpenApiServerApi` | `smbtechOpenApiGenerateServerApi` |
                | `generateOpenApiClient` | `smbtechOpenApiGenerateClient` |
                | `openApiCompatibilityCheck` | `smbtechOpenApiCompatibilityCheck` |
                | `publishOpenApiArtifactsToLocalBuildRepository` | `smbtechOpenApiPublishToLocalRepository` |

                The legacy group was `com.smbtech.openapi`. Update dependency declarations and
                replace the legacy `-api` suffix with `-server-api`.
                """
                        .formatted(String.join("\n", rows));
        try {
            Files.createDirectories(getReportFile().get().getAsFile().toPath().getParent());
            Files.writeString(
                    getReportFile().get().getAsFile().toPath(), report, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot write OpenAPI migration report", exception);
        }
    }

    private static String row(
            String kind, String legacyArtifact, String group, String artifact, String version) {
        return "| "
                + kind
                + " | `com.smbtech.openapi:"
                + legacyArtifact
                + ":"
                + version
                + "` | `"
                + group
                + ":"
                + artifact
                + ":"
                + version
                + "` |";
    }
}
