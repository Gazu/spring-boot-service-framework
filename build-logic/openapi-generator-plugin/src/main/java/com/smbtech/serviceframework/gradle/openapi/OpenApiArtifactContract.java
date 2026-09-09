package com.smbtech.serviceframework.gradle.openapi;

import java.util.Locale;

/** Internal representation of the generated artifact and package contract. */
record OpenApiArtifactContract(
        String artifactBaseName,
        String version,
        String modelPackage,
        String serverApiPackage,
        String clientPackage,
        String httpInterfacePackage,
        String openFeignPackage) {

    static final int JAVA_RELEASE = 21;

    static OpenApiArtifactContract resolve(
            SmbtechOpenApiSpec spec, OpenApiContractIdentity identity) {
        String artifactBaseName = spec.getArtifactBaseName().getOrElse(identity.artifactBaseName());
        String packageRoot = spec.getBasePackage().getOrElse(defaultPackageRoot(artifactBaseName));

        String versionedRoot = versionedPackageRoot(packageRoot, identity.version());
        String modelPackage = versionedRoot + ".model";
        String serverApiPackage = versionedRoot + ".api";
        String clientPackage = versionedRoot + ".client";

        return new OpenApiArtifactContract(
                artifactBaseName,
                identity.version(),
                modelPackage,
                serverApiPackage,
                clientPackage,
                clientPackage + ".httpinterface",
                clientPackage + ".openfeign");
    }

    String artifactId(OpenApiArtifactKind kind) {
        return artifactId(artifactBaseName, kind);
    }

    static String artifactId(String artifactBaseName, OpenApiArtifactKind kind) {
        return artifactBaseName + "-jdk" + JAVA_RELEASE + "-" + role(kind);
    }

    static String versionSegment(String version) {
        return "v" + OpenApiCompatibilitySupport.version(version).major();
    }

    static String defaultPackageRoot(String artifactBaseName) {
        String normalizedPackage = artifactBaseName.replace("-", "").toLowerCase(Locale.ROOT);
        return "com.smbtech.contracts." + normalizedPackage;
    }

    static String versionedPackageRoot(String packageRoot, String version) {
        return packageRoot + "." + versionSegment(version);
    }

    private static String role(OpenApiArtifactKind kind) {
        return switch (kind) {
            case MODELS -> "model";
            case SERVER_API -> "api";
            case CLIENT -> "client";
        };
    }
}
