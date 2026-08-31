package com.smbtech.serviceframework.project.generator;

/** Generates one-time Spring Boot project scaffolds with hexagonal boundaries. */
@FunctionalInterface
public interface HexagonalProjectGenerator {

    /**
     * Generates a project from the supplied request.
     *
     * @param request generation request
     * @return generated project details
     * @throws ProjectGenerationException when validation or generation fails
     */
    GeneratedProject generate(ProjectGenerationRequest request);

    /**
     * Creates the framework default generator.
     *
     * @return default generator
     */
    static HexagonalProjectGenerator create() {
        return new DefaultHexagonalProjectGenerator();
    }
}
