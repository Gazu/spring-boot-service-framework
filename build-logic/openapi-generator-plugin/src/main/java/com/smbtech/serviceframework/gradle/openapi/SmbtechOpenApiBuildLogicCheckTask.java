package com.smbtech.serviceframework.gradle.openapi;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/** Validates the OpenAPI generator extension before contract tasks execute. */
public abstract class SmbtechOpenApiBuildLogicCheckTask extends DefaultTask {

    /** Creates the validation task. */
    public SmbtechOpenApiBuildLogicCheckTask() {}

    /**
     * Returns the default Maven group used for generated artifacts.
     *
     * @return configured group identifier
     */
    @Input
    public abstract Property<String> getGroupId();

    /**
     * Returns the directory used for generated source and artifact output.
     *
     * @return configured output directory path
     */
    @Input
    public abstract Property<String> getOutputDirectory();

    /**
     * Returns the local Maven repository directory for generated artifacts.
     *
     * @return configured repository directory path
     */
    @Input
    public abstract Property<String> getRepositoryDirectory();

    /**
     * Returns the serialized contract configurations to validate.
     *
     * @return configured OpenAPI contract values
     */
    @Input
    public abstract ListProperty<String> getSpecConfigurations();

    /** Validates the extension and every configured OpenAPI contract. */
    @TaskAction
    public void validate() {
        if (getGroupId().getOrElse("").trim().isEmpty()) {
            throw new GradleException("smbtechOpenApi.groupId must not be blank");
        }
        if (getOutputDirectory().getOrElse("").trim().isEmpty()) {
            throw new GradleException("smbtechOpenApi.outputDirectory must not be blank");
        }
        if (getRepositoryDirectory().getOrElse("").trim().isEmpty()) {
            throw new GradleException("smbtechOpenApi.repositoryDirectory must not be blank");
        }
        for (String specConfiguration : getSpecConfigurations().getOrElse(java.util.List.of())) {
            String[] parts = specConfiguration.split("\\|", -1);
            String name = parts.length > 0 ? parts[0] : "";
            String input = parts.length > 1 ? parts[1] : "";
            String groupId = parts.length > 2 ? parts[2] : "";
            String artifactBaseName = parts.length > 3 ? parts[3] : "";
            String version = parts.length > 4 ? parts[4] : "";
            String basePackage = parts.length > 5 ? parts[5] : "";

            if (name.trim().isEmpty()) {
                throw new GradleException("smbtechOpenApi.specs contains a spec with a blank name");
            }
            if (input.trim().isEmpty()) {
                throw new GradleException(
                        "smbtechOpenApi.specs." + name + ".input must be configured");
            }
            if (!groupId.isEmpty() && groupId.trim().isEmpty()) {
                throw new GradleException(
                        "smbtechOpenApi.specs." + name + ".groupId must not be blank");
            }
            if (!artifactBaseName.isEmpty() && artifactBaseName.trim().isEmpty()) {
                throw new GradleException(
                        "smbtechOpenApi.specs." + name + ".artifactBaseName must not be blank");
            }
            if (!version.isEmpty() && version.trim().isEmpty()) {
                throw new GradleException(
                        "smbtechOpenApi.specs." + name + ".version must not be blank");
            }
            if (!basePackage.isEmpty() && basePackage.trim().isEmpty()) {
                throw new GradleException(
                        "smbtechOpenApi.specs." + name + ".basePackage must not be blank");
            }
        }
    }
}
