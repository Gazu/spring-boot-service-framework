package com.smbtech.serviceframework.gradle.openapi;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
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

        Set<SmbtechOpenApiSpec> configuredSpecs =
                Collections.newSetFromMap(new IdentityHashMap<>());
        Action<SmbtechOpenApiSpec> configureSpec =
                spec -> {
                    if (configuredSpecs.add(spec)) {
                        generationConfigurer.configure(spec);
                        compatibilityConfigurer.configure(spec);
                    }
                };
        extension.getSpecs().configureEach(spec -> configureSpecConventions(extension, spec));
        extension.onSpecConfigured(configureSpec);

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
                                    .from(OpenApiSpecDiscovery.candidates(project))
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

        project.afterEvaluate(
                ignored -> {
                    registerDiscoveredSpecs(project, extension, configureSpec);
                    extension.getSpecs().stream()
                            .filter(spec -> spec.getInput().isPresent())
                            .forEach(configureSpec::execute);
                });
    }

    private static void registerDiscoveredSpecs(
            Project project,
            SmbtechOpenApiExtension extension,
            Action<SmbtechOpenApiSpec> configureSpec) {
        Map<Path, SmbtechOpenApiSpec> configuredByPath = new LinkedHashMap<>();
        extension.getSpecs().stream()
                .filter(spec -> spec.getInput().isPresent())
                .forEach(
                        spec -> {
                            Path path =
                                    OpenApiSpecDiscovery.normalizedPath(
                                            spec.getInput().get().getAsFile());
                            SmbtechOpenApiSpec previous = configuredByPath.putIfAbsent(path, spec);
                            if (previous != null) {
                                throw new GradleException(
                                        "OpenAPI input "
                                                + path
                                                + " is configured by both smbtechOpenApi.specs."
                                                + previous.getName()
                                                + " and smbtechOpenApi.specs."
                                                + spec.getName());
                            }
                        });

        for (File source : OpenApiSpecDiscovery.discover(project)) {
            Path sourcePath = OpenApiSpecDiscovery.normalizedPath(source);
            if (configuredByPath.containsKey(sourcePath)) {
                continue;
            }
            try {
                OpenApiContractReader.read(source);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            String name = OpenApiSpecDiscovery.registrationName(project, source);
            SmbtechOpenApiSpec conflicting = extension.getSpecs().findByName(name);
            if (conflicting != null) {
                String conflictingInput =
                        conflicting.getInput().isPresent()
                                ? conflicting.getInput().get().getAsFile().getPath()
                                : "<not configured>";
                throw new GradleException(
                        "Cannot auto-register OpenAPI contract "
                                + source.getPath()
                                + " as smbtechOpenApi.specs."
                                + name
                                + "; that name is already used by "
                                + conflictingInput);
            }
            SmbtechOpenApiSpec discovered = extension.getSpecs().create(name);
            discovered.getInput().set(source);
            configuredByPath.put(sourcePath, discovered);
            configureSpec.execute(discovered);
        }
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
