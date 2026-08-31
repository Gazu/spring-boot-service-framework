package com.smbtech.serviceframework.gradle.openapi;

import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/** Gradle extension for configuring OpenAPI contract generation and publication. */
public abstract class SmbtechOpenApiExtension {

    private final Property<String> groupId;
    private final DirectoryProperty outputDirectory;
    private final DirectoryProperty repositoryDirectory;
    private final DirectoryProperty baselineDirectory;
    private final Property<String> publicationRepositoryUrl;
    private final Property<Boolean> requireBaseline;
    private final Property<Boolean> failOnBreakingChanges;
    private final Property<Boolean> publishModels;
    private final Property<Boolean> publishServerApi;
    private final Property<Boolean> publishClient;
    private final NamedDomainObjectContainer<SmbtechOpenApiSpec> specs;
    private Action<? super SmbtechOpenApiSpec> specConfiguredAction = ignored -> {};

    /**
     * Creates the extension with project-relative output conventions.
     *
     * @param objects Gradle object factory
     * @param layout current project layout
     */
    @Inject
    public SmbtechOpenApiExtension(ObjectFactory objects, ProjectLayout layout) {
        this.groupId = objects.property(String.class).convention("com.smbtech.contracts");
        this.outputDirectory =
                objects.directoryProperty()
                        .convention(layout.getBuildDirectory().dir("generated/smbtech-openapi"));
        this.repositoryDirectory =
                objects.directoryProperty()
                        .convention(layout.getBuildDirectory().dir("repository/openapi"));
        this.baselineDirectory =
                objects.directoryProperty()
                        .convention(layout.getProjectDirectory().dir("src/main/openapi-baselines"));
        this.publicationRepositoryUrl = objects.property(String.class);
        this.requireBaseline = objects.property(Boolean.class).convention(false);
        this.failOnBreakingChanges = objects.property(Boolean.class).convention(false);
        this.publishModels = objects.property(Boolean.class).convention(true);
        this.publishServerApi = objects.property(Boolean.class).convention(true);
        this.publishClient = objects.property(Boolean.class).convention(true);
        this.specs =
                objects.domainObjectContainer(
                        SmbtechOpenApiSpec.class,
                        name -> objects.newInstance(SmbtechOpenApiSpec.class, name));
    }

    /**
     * Returns the default Maven group for generated artifacts.
     *
     * @return configurable group identifier
     */
    public Property<String> getGroupId() {
        return groupId;
    }

    /**
     * Returns the root directory for generated files.
     *
     * @return configurable output directory
     */
    public DirectoryProperty getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Returns the local Maven repository used for generated artifacts.
     *
     * @return configurable repository directory
     */
    public DirectoryProperty getRepositoryDirectory() {
        return repositoryDirectory;
    }

    /**
     * Returns the directory containing immutable versioned contract baselines.
     *
     * @return configurable baseline directory
     */
    public DirectoryProperty getBaselineDirectory() {
        return baselineDirectory;
    }

    /**
     * Returns the optional remote Maven repository used to publish generated artifacts.
     *
     * @return configurable repository URL
     */
    public Property<String> getPublicationRepositoryUrl() {
        return publicationRepositoryUrl;
    }

    /**
     * Returns whether every current contract must have an exact baseline snapshot.
     *
     * @return configurable baseline requirement
     */
    public Property<Boolean> getRequireBaseline() {
        return requireBaseline;
    }

    /**
     * Returns whether any breaking change fails even after a valid major version increase.
     *
     * @return configurable strict compatibility flag
     */
    public Property<Boolean> getFailOnBreakingChanges() {
        return failOnBreakingChanges;
    }

    /**
     * Returns whether model artifacts are generated and published by default.
     *
     * @return configurable models artifact flag
     */
    public Property<Boolean> getPublishModels() {
        return publishModels;
    }

    /**
     * Returns whether server API artifacts are generated and published by default.
     *
     * @return configurable server API artifact flag
     */
    public Property<Boolean> getPublishServerApi() {
        return publishServerApi;
    }

    /**
     * Returns whether client artifacts are generated and published by default.
     *
     * @return configurable client artifact flag
     */
    public Property<Boolean> getPublishClient() {
        return publishClient;
    }

    /**
     * Returns the named OpenAPI contract configurations.
     *
     * @return mutable contract configuration container
     */
    public NamedDomainObjectContainer<SmbtechOpenApiSpec> getSpecs() {
        return specs;
    }

    /**
     * Configures the named OpenAPI contracts.
     *
     * @param action container configuration action
     */
    public void specs(Action<? super NamedDomainObjectContainer<SmbtechOpenApiSpec>> action) {
        action.execute(specs);
        specs.forEach(spec -> specConfiguredAction.execute(spec));
    }

    void onSpecConfigured(Action<? super SmbtechOpenApiSpec> action) {
        this.specConfiguredAction = action;
    }
}
