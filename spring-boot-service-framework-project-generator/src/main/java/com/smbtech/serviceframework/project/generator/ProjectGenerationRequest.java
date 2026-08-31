package com.smbtech.serviceframework.project.generator;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Immutable configuration for generating one Spring Boot project. */
public final class ProjectGenerationRequest {

    private final ProjectContractSource contractSource;
    private final Path outputDirectory;
    private final String groupId;
    private final @Nullable String artifactId;
    private final @Nullable String basePackage;
    private final @Nullable String applicationName;
    private final String projectVersion;
    private final String frameworkVersion;
    private final String springBootVersion;
    private final @Nullable String contractGroupId;
    private final @Nullable String contractArtifactId;
    private final @Nullable String contractVersion;
    private final @Nullable String contractApiPackage;
    private final @Nullable URI contractRepository;
    private final boolean overwrite;

    private ProjectGenerationRequest(Builder builder) {
        this.contractSource = builder.contractSource;
        this.outputDirectory = builder.outputDirectory;
        this.groupId = builder.groupId;
        this.artifactId = builder.artifactId;
        this.basePackage = builder.basePackage;
        this.applicationName = builder.applicationName;
        this.projectVersion = builder.projectVersion;
        this.frameworkVersion = builder.frameworkVersion;
        this.springBootVersion = builder.springBootVersion;
        this.contractGroupId = builder.contractGroupId;
        this.contractArtifactId = builder.contractArtifactId;
        this.contractVersion = builder.contractVersion;
        this.contractApiPackage = builder.contractApiPackage;
        this.contractRepository = builder.contractRepository;
        this.overwrite = builder.overwrite;
    }

    /**
     * Creates a request builder.
     *
     * @param contractSource OpenAPI document or server API JAR
     * @param outputDirectory target project directory
     * @return request builder
     */
    public static Builder builder(ProjectContractSource contractSource, Path outputDirectory) {
        return new Builder(contractSource, outputDirectory);
    }

    /**
     * Returns the contract source.
     *
     * @return contract source
     */
    public ProjectContractSource contractSource() {
        return contractSource;
    }

    /**
     * Returns the target project directory.
     *
     * @return target project directory
     */
    public Path outputDirectory() {
        return outputDirectory;
    }

    /**
     * Returns the generated project group.
     *
     * @return generated project group identifier
     */
    public String groupId() {
        return groupId;
    }

    /**
     * Returns the explicit artifact identifier.
     *
     * @return explicit artifact identifier, or {@code null} when derived from the contract
     */
    public @Nullable String artifactId() {
        return artifactId;
    }

    /**
     * Returns the explicit base package.
     *
     * @return explicit base package, or {@code null} when derived from the project group
     */
    public @Nullable String basePackage() {
        return basePackage;
    }

    /**
     * Returns the explicit application class name.
     *
     * @return explicit application class name, or {@code null} when derived from the contract
     */
    public @Nullable String applicationName() {
        return applicationName;
    }

    /**
     * Returns the generated project version.
     *
     * @return generated project version
     */
    public String projectVersion() {
        return projectVersion;
    }

    /**
     * Returns the framework platform version.
     *
     * @return framework platform version
     */
    public String frameworkVersion() {
        return frameworkVersion;
    }

    /**
     * Returns the Spring Boot version.
     *
     * @return Spring Boot version
     */
    public String springBootVersion() {
        return springBootVersion;
    }

    /**
     * Returns the explicit server API group.
     *
     * @return explicit server API group, or {@code null} when obtained from source metadata
     */
    public @Nullable String contractGroupId() {
        return contractGroupId;
    }

    /**
     * Returns the explicit server API artifact.
     *
     * @return explicit server API artifact, or {@code null} when obtained from source metadata
     */
    public @Nullable String contractArtifactId() {
        return contractArtifactId;
    }

    /**
     * Returns the explicit contract version.
     *
     * @return explicit contract version, or {@code null} when obtained from source metadata
     */
    public @Nullable String contractVersion() {
        return contractVersion;
    }

    /**
     * Returns the explicit generated API package.
     *
     * @return explicit generated API package, or {@code null} when derived from the contract
     */
    public @Nullable String contractApiPackage() {
        return contractApiPackage;
    }

    /**
     * Returns the optional contract repository.
     *
     * @return optional Maven repository containing the generated contract artifacts
     */
    public @Nullable URI contractRepository() {
        return contractRepository;
    }

    /**
     * Returns whether replacement is enabled.
     *
     * @return whether an existing target directory may be replaced
     */
    public boolean overwrite() {
        return overwrite;
    }

    /** Builder for {@link ProjectGenerationRequest}. */
    public static final class Builder {

        private final ProjectContractSource contractSource;
        private final Path outputDirectory;
        private String groupId = "com.smbtech";
        private @Nullable String artifactId;
        private @Nullable String basePackage;
        private @Nullable String applicationName;
        private String projectVersion = "0.1.0-SNAPSHOT";
        private String frameworkVersion = ProjectGeneratorVersions.frameworkVersion();
        private String springBootVersion = ProjectGeneratorVersions.springBootVersion();
        private @Nullable String contractGroupId;
        private @Nullable String contractArtifactId;
        private @Nullable String contractVersion;
        private @Nullable String contractApiPackage;
        private @Nullable URI contractRepository;
        private boolean overwrite;

        private Builder(ProjectContractSource contractSource, Path outputDirectory) {
            this.contractSource =
                    Objects.requireNonNull(contractSource, "contractSource must not be null");
            this.outputDirectory =
                    Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        }

        /**
         * Sets the generated project group.
         *
         * @param groupId Maven group and default package root
         * @return this builder
         */
        public Builder groupId(String groupId) {
            this.groupId = required(groupId, "groupId");
            return this;
        }

        /**
         * Sets the generated project artifact.
         *
         * @param artifactId Maven artifact identifier
         * @return this builder
         */
        public Builder artifactId(String artifactId) {
            this.artifactId = required(artifactId, "artifactId");
            return this;
        }

        /**
         * Sets the Java package root.
         *
         * @param basePackage Java package root
         * @return this builder
         */
        public Builder basePackage(String basePackage) {
            this.basePackage = required(basePackage, "basePackage");
            return this;
        }

        /**
         * Sets the Spring Boot application class name.
         *
         * @param applicationName Java class name
         * @return this builder
         */
        public Builder applicationName(String applicationName) {
            this.applicationName = required(applicationName, "applicationName");
            return this;
        }

        /**
         * Sets the generated project version.
         *
         * @param projectVersion project version
         * @return this builder
         */
        public Builder projectVersion(String projectVersion) {
            this.projectVersion = required(projectVersion, "projectVersion");
            return this;
        }

        /**
         * Sets the framework platform version.
         *
         * @param frameworkVersion framework version
         * @return this builder
         */
        public Builder frameworkVersion(String frameworkVersion) {
            this.frameworkVersion = required(frameworkVersion, "frameworkVersion");
            return this;
        }

        /**
         * Sets the Spring Boot plugin version.
         *
         * @param springBootVersion Spring Boot version
         * @return this builder
         */
        public Builder springBootVersion(String springBootVersion) {
            this.springBootVersion = required(springBootVersion, "springBootVersion");
            return this;
        }

        /**
         * Overrides the generated server API group.
         *
         * @param contractGroupId server API group
         * @return this builder
         */
        public Builder contractGroupId(String contractGroupId) {
            this.contractGroupId = required(contractGroupId, "contractGroupId");
            return this;
        }

        /**
         * Overrides the generated server API artifact.
         *
         * @param contractArtifactId server API artifact
         * @return this builder
         */
        public Builder contractArtifactId(String contractArtifactId) {
            this.contractArtifactId = required(contractArtifactId, "contractArtifactId");
            return this;
        }

        /**
         * Overrides the generated server API version.
         *
         * @param contractVersion server API version
         * @return this builder
         */
        public Builder contractVersion(String contractVersion) {
            this.contractVersion = required(contractVersion, "contractVersion");
            return this;
        }

        /**
         * Overrides the generated API package used for a document source.
         *
         * @param contractApiPackage generated API package
         * @return this builder
         */
        public Builder contractApiPackage(String contractApiPackage) {
            this.contractApiPackage = required(contractApiPackage, "contractApiPackage");
            return this;
        }

        /**
         * Adds a Maven repository containing the contract artifacts.
         *
         * @param contractRepository repository URI
         * @return this builder
         */
        public Builder contractRepository(URI contractRepository) {
            this.contractRepository =
                    Objects.requireNonNull(
                            contractRepository, "contractRepository must not be null");
            return this;
        }

        /**
         * Controls replacement of an existing target directory.
         *
         * @param overwrite whether replacement is allowed
         * @return this builder
         */
        public Builder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        /**
         * Creates the immutable request.
         *
         * @return generation request
         */
        public ProjectGenerationRequest build() {
            return new ProjectGenerationRequest(this);
        }

        private static String required(String value, String name) {
            Objects.requireNonNull(value, name + " must not be null");
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value.trim();
        }
    }
}
