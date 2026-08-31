package com.smbtech.serviceframework.project.generator;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Published server API JAR containing framework OpenAPI metadata.
 *
 * @param path server API JAR path
 */
public record ServerApiJarSource(Path path) implements ProjectContractSource {

    /** Validates the source path. */
    public ServerApiJarSource {
        Objects.requireNonNull(path, "path must not be null");
    }
}
