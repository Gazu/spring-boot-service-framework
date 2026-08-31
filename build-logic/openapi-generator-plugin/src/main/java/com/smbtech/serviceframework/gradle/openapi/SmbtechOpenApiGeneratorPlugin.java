package com.smbtech.serviceframework.gradle.openapi;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.component.SoftwareComponentFactory;

/** Gradle plugin that registers OpenAPI contract generation and verification tasks. */
public final class SmbtechOpenApiGeneratorPlugin implements Plugin<Project> {

    private final SoftwareComponentFactory componentFactory;

    /** Configures OpenAPI specifications in a consuming Gradle build. */
    public static final String EXTENSION_NAME = "smbtechOpenApi";

    /** Participates in the consuming build verification lifecycle. */
    public static final String BUILD_LOGIC_CHECK_TASK_NAME = "smbtechOpenApiBuildLogicCheck";

    /** Validates configured OpenAPI specifications. */
    public static final String VALIDATE_SPECS_TASK_NAME = "smbtechOpenApiValidateSpecs";

    /**
     * Creates the OpenAPI generator plugin.
     *
     * @param componentFactory Gradle component factory used for generated Maven variants
     */
    @Inject
    public SmbtechOpenApiGeneratorPlugin(SoftwareComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
    }

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("maven-publish");

        SmbtechOpenApiExtension extension =
                project.getExtensions().create(EXTENSION_NAME, SmbtechOpenApiExtension.class);
        OpenApiGenerationConfigurer generationConfigurer =
                new OpenApiGenerationConfigurer(project, componentFactory, extension);
        OpenApiCompatibilityConfigurer compatibilityConfigurer =
                new OpenApiCompatibilityConfigurer(project, extension);
        generationConfigurer.configureLifecycleAndRepositories();
        compatibilityConfigurer.configureLifecycle();

        extension.getSpecs().configureEach(spec -> configureSpecConventions(extension, spec));
        extension.onSpecConfigured(
                spec -> {
                    generationConfigurer.configure(spec);
                    compatibilityConfigurer.configure(spec);
                });

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
                            task.getBaselineDirectory()
                                    .convention(
                                            extension
                                                    .getBaselineDirectory()
                                                    .map(
                                                            directory ->
                                                                    directory
                                                                            .getAsFile()
                                                                            .getPath()));
                            task.getPublicationRepositoryUrl()
                                    .set(extension.getPublicationRepositoryUrl());
                            task.getRequireBaseline().convention(extension.getRequireBaseline());
                            task.getFailOnBreakingChanges()
                                    .convention(extension.getFailOnBreakingChanges());
                            task.getPublishModels().convention(extension.getPublishModels());
                            task.getPublishServerApi().convention(extension.getPublishServerApi());
                            task.getPublishClient().convention(extension.getPublishClient());
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
                            task.getDefaultGroupId().convention(extension.getGroupId());
                            task.getSpecConfigurations()
                                    .set(
                                            project.provider(
                                                    () ->
                                                            extension.getSpecs().stream()
                                                                    .map(
                                                                            SmbtechOpenApiGeneratorPlugin
                                                                                    ::specConfiguration)
                                                                    .collect(Collectors.toList())));
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
    }

    static String specConfiguration(SmbtechOpenApiSpec spec) {
        return String.join(
                "|",
                spec.getName(),
                spec.getInput().isPresent() ? spec.getInput().get().getAsFile().getPath() : "",
                spec.getGroupId().getOrElse(""),
                spec.getArtifactBaseName().getOrElse(""),
                spec.getVersion().getOrElse(""),
                spec.getBasePackage().getOrElse(""),
                spec.getModelPackage().getOrElse(""),
                spec.getServerApiPackage().getOrElse(""),
                spec.getClientPackage().getOrElse(""),
                spec.getPublishModels().getOrElse(true).toString(),
                spec.getPublishServerApi().getOrElse(true).toString(),
                spec.getPublishClient().getOrElse(true).toString());
    }

    private static void configureSpecConventions(
            SmbtechOpenApiExtension extension, SmbtechOpenApiSpec spec) {
        spec.getPublishModels().convention(extension.getPublishModels());
        spec.getPublishServerApi().convention(extension.getPublishServerApi());
        spec.getPublishClient().convention(extension.getPublishClient());
    }
}
