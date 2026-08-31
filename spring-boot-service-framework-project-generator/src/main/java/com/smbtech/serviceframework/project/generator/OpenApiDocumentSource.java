package com.smbtech.serviceframework.project.generator;

import java.nio.file.Path;
import java.util.Objects;

/**
 * OpenAPI YAML or JSON source.
 *
 * @param path OpenAPI document path
 */
public record OpenApiDocumentSource(Path path) implements ProjectContractSource {

    /** Validates the source path. */
    public OpenApiDocumentSource {
        Objects.requireNonNull(path, "path must not be null");
    }
}
