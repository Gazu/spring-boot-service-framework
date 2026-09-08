package com.smbtech.serviceframework.gradle.openapi;

import javax.inject.Inject;
import org.gradle.api.Named;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/** Named Gradle configuration for one OpenAPI contract. */
public abstract class SmbtechOpenApiSpec implements Named {

    private final String name;

    /**
     * Creates a named contract configuration.
     *
     * @param name unique contract name within the extension
     */
    @Inject
    public SmbtechOpenApiSpec(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the OpenAPI document to process.
     *
     * @return configurable input file
     */
    public abstract RegularFileProperty getInput();

    /**
     * Returns the optional Maven group override.
     *
     * @return configurable group identifier
     */
    public abstract Property<String> getGroupId();

    /**
     * Returns the optional base artifact name override.
     *
     * @return configurable artifact base name
     */
    public abstract Property<String> getArtifactBaseName();

    /**
     * Returns the optional artifact version compatibility input.
     *
     * <p>When configured, it must equal the source contract's {@code info.version}.
     *
     * @return configurable artifact version
     */
    public abstract Property<String> getVersion();

    /**
     * Returns the optional unversioned generated Java package root.
     *
     * @return configurable base package
     */
    public abstract Property<String> getBasePackage();

    /**
     * Returns the optional generated model package compatibility input.
     *
     * <p>When configured, it must equal {@code <basePackage>.v<major>.model}.
     *
     * @return configurable model package
     */
    public abstract Property<String> getModelPackage();

    /**
     * Returns the optional generated server API package compatibility input.
     *
     * <p>When configured, it must equal {@code <basePackage>.v<major>.api}.
     *
     * @return configurable server API package
     */
    public abstract Property<String> getServerApiPackage();

    /**
     * Returns the optional generated client package compatibility input.
     *
     * <p>When configured, it must equal {@code <basePackage>.v<major>.client}. Generators own the
     * client transport subpackages.
     *
     * @return configurable client package
     */
    public abstract Property<String> getClientPackage();

    /**
     * Returns whether the models artifact is enabled for this contract.
     *
     * @return configurable models artifact flag
     */
    public abstract Property<Boolean> getPublishModels();

    /**
     * Returns whether the server API artifact is enabled for this contract.
     *
     * @return configurable server API artifact flag
     */
    public abstract Property<Boolean> getPublishServerApi();

    /**
     * Returns whether the client artifact is enabled for this contract.
     *
     * @return configurable client artifact flag
     */
    public abstract Property<Boolean> getPublishClient();
}
