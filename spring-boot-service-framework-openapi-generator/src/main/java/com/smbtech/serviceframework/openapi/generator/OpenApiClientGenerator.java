package com.smbtech.serviceframework.openapi.generator;

import java.util.Objects;

/** Provides open api client generator behavior. */
public final class OpenApiClientGenerator {
    /** Creates an OpenAPI client generator instance. */
    public OpenApiClientGenerator() {}

    /**
     * Performs the artifact kind operation.
     *
     * @return artifact kind result
     */
    public OpenApiArtifactKind artifactKind() {
        return OpenApiArtifactKind.CLIENT;
    }

    /**
     * Performs the package name operation.
     *
     * @param specInfo spec info value
     * @return package name result
     */
    public String packageName(OpenApiSpecInfo specInfo) {
        return Objects.requireNonNull(specInfo, "specInfo").basePackage() + ".client";
    }
}
