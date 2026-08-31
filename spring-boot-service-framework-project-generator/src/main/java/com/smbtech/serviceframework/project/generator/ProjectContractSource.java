package com.smbtech.serviceframework.project.generator;

import java.nio.file.Path;

/** Source from which a generated project obtains its OpenAPI server contract. */
public sealed interface ProjectContractSource permits OpenApiDocumentSource, ServerApiJarSource {

    /**
     * Returns the source file.
     *
     * @return absolute or relative source path
     */
    Path path();
}
