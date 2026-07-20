package com.smbtech.serviceframework.openapi.generator;

import java.util.Objects;

/** Provides open api server api generator behavior. */
public final class OpenApiServerApiGenerator {
    /** Creates an OpenAPI server api generator instance. */
    public OpenApiServerApiGenerator() {}

    /**
     * Performs the artifact kind operation.
     *
     * @return artifact kind result
     */
    public OpenApiArtifactKind artifactKind() {
        return OpenApiArtifactKind.SERVER_API;
    }

    /**
     * Performs the package name operation.
     *
     * @param specInfo spec info value
     * @return package name result
     */
    public String packageName(OpenApiSpecInfo specInfo) {
        return Objects.requireNonNull(specInfo, "specInfo").basePackage() + ".api";
    }
}
