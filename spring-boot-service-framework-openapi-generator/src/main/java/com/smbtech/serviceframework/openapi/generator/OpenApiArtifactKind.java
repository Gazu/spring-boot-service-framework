package com.smbtech.serviceframework.openapi.generator;

import java.util.Locale;

/** Defines supported open api artifact kind values. */
public enum OpenApiArtifactKind {

    /** Represents models. */
    MODELS("models"),
    /** Represents server api. */
    SERVER_API("api"),
    /** Represents client. */
    CLIENT("client");

    private final String classifier;

    OpenApiArtifactKind(String classifier) {
        this.classifier = classifier;
    }

    /**
     * Performs the classifier operation.
     *
     * @return classifier result
     */
    public String classifier() {
        return classifier;
    }

    /**
     * Performs the artifact id operation.
     *
     * @param artifactBaseName artifact base name value
     * @return artifact id result
     */
    public String artifactId(String artifactBaseName) {
        return artifactBaseName + "-" + classifier;
    }

    /**
     * Performs the default package suffix operation.
     *
     * @return default package suffix result
     */
    public String defaultPackageSuffix() {
        return classifier.replace("-", "").toLowerCase(Locale.ROOT);
    }
}
