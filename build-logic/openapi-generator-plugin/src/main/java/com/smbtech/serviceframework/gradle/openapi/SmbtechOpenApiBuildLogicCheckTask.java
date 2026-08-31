package com.smbtech.serviceframework.gradle.openapi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

/** Validates the OpenAPI generator extension before contract tasks execute. */
public abstract class SmbtechOpenApiBuildLogicCheckTask extends DefaultTask {

    private static final Pattern JAVA_PACKAGE =
            Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*");

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
     * Returns the contract baseline directory.
     *
     * @return configured baseline directory path
     */
    @Input
    public abstract Property<String> getBaselineDirectory();

    /**
     * Returns the optional remote Maven repository URL.
     *
     * @return configured remote repository URL
     */
    @Input
    @Optional
    public abstract Property<String> getPublicationRepositoryUrl();

    /**
     * Returns whether exact version baselines are required.
     *
     * @return configured baseline requirement
     */
    @Input
    public abstract Property<Boolean> getRequireBaseline();

    /**
     * Returns whether all breaking changes fail compatibility validation.
     *
     * @return configured strict compatibility flag
     */
    @Input
    public abstract Property<Boolean> getFailOnBreakingChanges();

    /**
     * Returns whether model artifacts are globally enabled.
     *
     * @return configured models artifact flag
     */
    @Input
    public abstract Property<Boolean> getPublishModels();

    /**
     * Returns whether server API artifacts are globally enabled.
     *
     * @return configured server API artifact flag
     */
    @Input
    public abstract Property<Boolean> getPublishServerApi();

    /**
     * Returns whether client artifacts are globally enabled.
     *
     * @return configured client artifact flag
     */
    @Input
    public abstract Property<Boolean> getPublishClient();

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
        if (getBaselineDirectory().getOrElse("").trim().isEmpty()) {
            throw new GradleException("smbtechOpenApi.baselineDirectory must not be blank");
        }
        validateRepositoryUrl();
        validateEnabledArtifacts(
                "smbtechOpenApi",
                getPublishModels().getOrElse(true),
                getPublishServerApi().getOrElse(true),
                getPublishClient().getOrElse(true));
        for (String specConfiguration : getSpecConfigurations().getOrElse(java.util.List.of())) {
            String[] parts = specConfiguration.split("\\|", -1);
            String name = parts.length > 0 ? parts[0] : "";
            String input = parts.length > 1 ? parts[1] : "";
            String groupId = parts.length > 2 ? parts[2] : "";
            String artifactBaseName = parts.length > 3 ? parts[3] : "";
            String version = parts.length > 4 ? parts[4] : "";
            String basePackage = parts.length > 5 ? parts[5] : "";
            String modelPackage = parts.length > 6 ? parts[6] : "";
            String serverApiPackage = parts.length > 7 ? parts[7] : "";
            String clientPackage = parts.length > 8 ? parts[8] : "";
            boolean publishModels = parts.length <= 9 || Boolean.parseBoolean(parts[9]);
            boolean publishServerApi = parts.length <= 10 || Boolean.parseBoolean(parts[10]);
            boolean publishClient = parts.length <= 11 || Boolean.parseBoolean(parts[11]);

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
            validatePackage(name, "basePackage", basePackage);
            validatePackage(name, "modelPackage", modelPackage);
            validatePackage(name, "serverApiPackage", serverApiPackage);
            validatePackage(name, "clientPackage", clientPackage);
            validateEnabledArtifacts(
                    "smbtechOpenApi.specs." + name, publishModels, publishServerApi, publishClient);
        }
    }

    private void validateRepositoryUrl() {
        String repositoryUrl = getPublicationRepositoryUrl().getOrElse("").trim();
        if (repositoryUrl.isEmpty()) {
            return;
        }
        try {
            URI uri = new URI(repositoryUrl);
            if (!uri.isAbsolute()) {
                throw new GradleException(
                        "smbtechOpenApi.publicationRepositoryUrl must be an absolute URI");
            }
        } catch (URISyntaxException exception) {
            throw new GradleException(
                    "smbtechOpenApi.publicationRepositoryUrl must be a valid URI", exception);
        }
    }

    private static void validatePackage(String name, String property, String value) {
        if (value.isEmpty()) {
            return;
        }
        if (value.trim().isEmpty()) {
            throw new GradleException(
                    "smbtechOpenApi.specs." + name + "." + property + " must not be blank");
        }
        if (!JAVA_PACKAGE.matcher(value).matches()) {
            throw new GradleException(
                    "smbtechOpenApi.specs."
                            + name
                            + "."
                            + property
                            + " must be a valid Java package");
        }
    }

    private static void validateEnabledArtifacts(
            String owner, boolean publishModels, boolean publishServerApi, boolean publishClient) {
        if (!publishModels && !publishServerApi && !publishClient) {
            throw new GradleException(owner + " must enable at least one generated artifact");
        }
        if (!publishModels && (publishServerApi || publishClient)) {
            throw new GradleException(
                    owner + " must enable models when server API or client generation is enabled");
        }
    }
}
