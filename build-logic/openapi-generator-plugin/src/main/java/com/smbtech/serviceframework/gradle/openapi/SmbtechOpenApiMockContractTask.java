package com.smbtech.serviceframework.gradle.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Validates that generated contracts can drive the framework OpenAPI mock server. */
public abstract class SmbtechOpenApiMockContractTask extends DefaultTask {

    /** Creates the mock contract task. */
    public SmbtechOpenApiMockContractTask() {}

    /**
     * Returns configured OpenAPI documents.
     *
     * @return configured OpenAPI documents
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSpecFiles();

    /**
     * Returns models JARs containing contracts.
     *
     * @return generated models JARs
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract ConfigurableFileCollection getModelArtifacts();

    /**
     * Returns the mock adoption report.
     *
     * @return mock adoption report
     */
    @OutputFile
    public abstract RegularFileProperty getReportFile();

    /** Checks numeric responses and writes classpath locations for the mock starter. */
    @TaskAction
    public void verifyMockContracts() {
        List<String> failures = new ArrayList<>();
        List<String> locations = new ArrayList<>();
        Map<String, File> models =
                getModelArtifacts().getFiles().stream()
                        .filter(File::isFile)
                        .collect(Collectors.toMap(File::getName, Function.identity()));
        List<File> specs =
                getSpecFiles().getFiles().stream()
                        .filter(File::isFile)
                        .sorted(Comparator.comparing(File::getPath))
                        .toList();
        for (File spec : specs) {
            OpenApiContractIdentity identity = OpenApiContractReader.read(spec);
            OpenAPI document = OpenApiContractReader.readDocument(spec);
            document.getPaths()
                    .forEach(
                            (path, item) ->
                                    item.readOperationsMap()
                                            .forEach(
                                                    (method, operation) -> {
                                                        boolean numericResponse =
                                                                operation.getResponses() != null
                                                                        && operation
                                                                                .getResponses()
                                                                                .keySet()
                                                                                .stream()
                                                                                .anyMatch(
                                                                                        status ->
                                                                                                status
                                                                                                        .matches(
                                                                                                                "[1-5][0-9]{2}"));
                                                        if (!numericResponse) {
                                                            failures.add(
                                                                    method
                                                                            + " "
                                                                            + path
                                                                            + ": mock server requires at least one numeric response");
                                                        }
                                                    }));
            locations.add(
                    identity.artifactBaseName()
                            + "=classpath:META-INF/smbtech/openapi/contracts/"
                            + identity.artifactBaseName()
                            + "/"
                            + identity.version()
                            + "/contract.yaml");
            verifyEmbeddedContract(identity, models, failures);
        }
        try {
            Files.createDirectories(getReportFile().get().getAsFile().toPath().getParent());
            Files.writeString(
                    getReportFile().get().getAsFile().toPath(),
                    String.join("\n", locations) + "\n",
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot write OpenAPI mock contract report", exception);
        }
        if (!failures.isEmpty()) {
            throw new GradleException(
                    "OpenAPI mock compatibility issues found:\n- " + String.join("\n- ", failures));
        }
    }

    private static void verifyEmbeddedContract(
            OpenApiContractIdentity identity, Map<String, File> models, List<String> failures) {
        String artifactName =
                identity.artifactBaseName() + "-models-" + identity.version() + ".jar";
        File artifact = models.get(artifactName);
        if (artifact == null) {
            failures.add(identity.artifactBaseName() + ": models JAR is missing for mock usage");
            return;
        }
        String resource =
                "META-INF/smbtech/openapi/contracts/"
                        + identity.artifactBaseName()
                        + "/"
                        + identity.version()
                        + "/contract.yaml";
        try (JarFile jar = new JarFile(artifact)) {
            if (jar.getEntry(resource) == null) {
                failures.add(identity.artifactBaseName() + ": missing mock resource " + resource);
            }
        } catch (IOException exception) {
            failures.add(identity.artifactBaseName() + ": cannot inspect models JAR");
        }
    }
}
