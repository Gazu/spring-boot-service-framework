package com.smbtech.serviceframework.gradle.openapi;

import java.net.URL;
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
                        });

        project.afterEvaluate(
                ignored ->
                        project.getTasks()
                                .named(
                                        BUILD_LOGIC_CHECK_TASK_NAME,
                                        SmbtechOpenApiBuildLogicCheckTask.class)
                                .configure(
                                        task ->
                                                task.getSpecConfigurations()
                                                        .set(
                                                                extension.getSpecs().stream()
                                                                        .map(
                                                                                SmbtechOpenApiGeneratorPlugin
                                                                                        ::specConfiguration)
                                                                        .collect(
                                                                                Collectors
                                                                                        .toList()))));

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
