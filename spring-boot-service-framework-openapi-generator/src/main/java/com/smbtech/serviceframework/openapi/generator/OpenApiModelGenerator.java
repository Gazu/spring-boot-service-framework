package com.smbtech.serviceframework.openapi.generator;

import java.util.Objects;

/** Provides open api model generator behavior. */
public final class OpenApiModelGenerator {
    /** Creates an OpenAPI model generator instance. */
    public OpenApiModelGenerator() {}

    /**
     * Performs the artifact kind operation.
     *
     * @return artifact kind result
     */
    public OpenApiArtifactKind artifactKind() {
        return OpenApiArtifactKind.MODELS;
    }

    /**
     * Performs the package name operation.
     *
     * @param specInfo spec info value
     * @return package name result
     */
    public String packageName(OpenApiSpecInfo specInfo) {
        return Objects.requireNonNull(specInfo, "specInfo").basePackage() + ".model";
    }
}
