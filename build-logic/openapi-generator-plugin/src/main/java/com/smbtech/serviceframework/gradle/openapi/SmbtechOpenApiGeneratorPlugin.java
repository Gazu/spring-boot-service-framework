package com.smbtech.serviceframework.gradle.openapi;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/** Gradle plugin that registers OpenAPI contract generation and verification tasks. */
public final class SmbtechOpenApiGeneratorPlugin implements Plugin<Project> {

    /** Configures OpenAPI specifications in a consuming Gradle build. */
    public static final String EXTENSION_NAME = "smbtechOpenApi";

    /** Participates in the consuming build verification lifecycle. */
    public static final String BUILD_LOGIC_CHECK_TASK_NAME = "smbtechOpenApiBuildLogicCheck";

    /** Preserves the public OpenAPI specification validation task name. */
    public static final String VALIDATE_SPECS_TASK_NAME = "validateOpenApiSpecs";

    /** Creates the OpenAPI generator plugin. */
    public SmbtechOpenApiGeneratorPlugin() {}

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("maven-publish");

        SmbtechOpenApiExtension extension =
                project.getExtensions().create(EXTENSION_NAME, SmbtechOpenApiExtension.class);

        project.getTasks()
                .register(
                        BUILD_LOGIC_CHECK_TASK_NAME,
                        SmbtechOpenApiBuildLogicCheckTask.class,
                        task -> {
                            task.setGroup("verification");
                            task.setDescription(
                                    "Validates SMB Tech OpenAPI generator build-logic configuration.");
                            task.getGroupId().convention(extension.getGroupId());
                            task.getOutputDirectory()
                                    .convention(
                                            extension
                                                    .getOutputDirectory()
                                                    .map(
                                                            directory ->
                                                                    directory
                                                                            .getAsFile()
                                                                            .getPath()));
                            task.getRepositoryDirectory()
                                    .convention(
                                            extension
                                                    .getRepositoryDirectory()
                                                    .map(
                                                            directory ->
                                                                    directory
                                                                            .getAsFile()
                                                                            .getPath()));
                            task.getSpecConfigurations()
                                    .set(
                                            project.provider(
                                                    () ->
                                                            extension.getSpecs().stream()
                                                                    .map(
                                                                            SmbtechOpenApiGeneratorPlugin
                                                                                    ::specConfiguration)
                                                                    .collect(Collectors.toList())));
                        });

        project.getTasks()
                .register(
                        VALIDATE_SPECS_TASK_NAME,
                        SmbtechOpenApiValidateSpecsTask.class,
                        task -> {
                            task.setGroup("verification");
                            task.setDescription(
                                    "Validates OpenAPI 3.0/3.1 documents and generated artifact coordinates.");
                            task.getRootDirectory().set(project.getLayout().getProjectDirectory());
                            task.getSpecFiles()
                                    .from(
                                            project.fileTree(
                                                    project.getRootDir(),
                                                    patterns -> {
                                                        patterns.include(
                                                                List.of(
                                                                        "**/src/main/openapi/*.yaml",
                                                                        "**/src/main/openapi/*.yml",
                                                                        "**/src/main/openapi/*.json",
                                                                        "**/openapi/*.yaml",
                                                                        "**/openapi/*.yml",
                                                                        "**/openapi/*.json",
                                                                        "**/swagger/*.yaml",
                                                                        "**/swagger/*.yml",
                                                                        "**/swagger/*.json"));
                                                        patterns.exclude(
                                                                List.of(
                                                                        "**/.git/**",
                                                                        "**/.gradle/**",
                                                                        "**/build/**"));
                                                    }))
                                    .from(
                                            project.provider(
                                                    () ->
                                                            extension.getSpecs().stream()
                                                                    .filter(
                                                                            spec ->
                                                                                    spec.getInput()
                                                                                            .isPresent())
                                                                    .map(
                                                                            spec ->
                                                                                    spec.getInput()
                                                                                            .get()
                                                                                            .getAsFile())
                                                                    .collect(Collectors.toList())));
                        });

        applyGeneratorImplementation(project);
    }

    private static void applyGeneratorImplementation(Project project) {
        URL implementation =
                SmbtechOpenApiGeneratorPlugin.class.getResource(
                        "/com/smbtech/serviceframework/gradle/openapi/openapi-generator.gradle");
        if (implementation == null) {
            throw new IllegalStateException("OpenAPI generator implementation resource is missing");
        }
        project.apply(Map.of("from", implementation.toExternalForm()));
    }

    private static String specConfiguration(SmbtechOpenApiSpec spec) {
        return String.join(
                "|",
                spec.getName(),
                spec.getInput().isPresent() ? spec.getInput().get().getAsFile().getPath() : "",
                spec.getGroupId().getOrElse(""),
                spec.getArtifactBaseName().getOrElse(""),
                spec.getVersion().getOrElse(""),
                spec.getBasePackage().getOrElse(""));
    }
}
