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
    private final NamedDomainObjectContainer<SmbtechOpenApiSpec> specs;

    /**
     * Creates the extension with project-relative output conventions.
     *
     * @param objects Gradle object factory
     * @param layout current project layout
     */
    @Inject
    public SmbtechOpenApiExtension(ObjectFactory objects, ProjectLayout layout) {
        this.groupId = objects.property(String.class).convention("com.smbtech.openapi");
        this.outputDirectory =
                objects.directoryProperty()
                        .convention(layout.getBuildDirectory().dir("generated/smbtech-openapi"));
        this.repositoryDirectory =
                objects.directoryProperty()
                        .convention(layout.getBuildDirectory().dir("repository/openapi"));
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
    }
}
