package com.smbtech.serviceframework.gradle.openapi;

/** Generated OpenAPI artifact kinds supported by the plugin. */
enum OpenApiArtifactKind {
    /** Shared JSON and validation models. */
    MODELS("models"),

    /** Spring MVC API and delegate contracts. */
    SERVER_API("server-api"),

    /** Spring HTTP interface client contracts. */
    CLIENT("client");

    private final String artifactSuffix;

    OpenApiArtifactKind(String artifactSuffix) {
        this.artifactSuffix = artifactSuffix;
    }

    /**
     * Returns the Maven artifact suffix.
     *
     * @return stable artifact suffix
     */
    public String artifactSuffix() {
        return artifactSuffix;
    }
}
