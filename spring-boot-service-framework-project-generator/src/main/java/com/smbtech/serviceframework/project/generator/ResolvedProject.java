package com.smbtech.serviceframework.project.generator;

import java.net.URI;
import java.nio.file.Path;

record ResolvedProject(
        Path outputDirectory,
        String groupId,
        String artifactId,
        String basePackage,
        String applicationName,
        String projectVersion,
        String frameworkVersion,
        String springBootVersion,
        URI contractRepository,
        ContractDescriptor contract) {

    static ResolvedProject from(ProjectGenerationRequest request, ContractDescriptor contract) {
        String artifact = value(request.artifactId(), contract.id() + "-service");
        String basePackage =
                value(
                        request.basePackage(),
                        request.groupId() + ".services." + NameSupport.compact(contract.id()));
        String application =
                value(
                        request.applicationName(),
                        NameSupport.javaType(contract.id()) + "Application");
        NameSupport.requireArtifact(artifact, "artifactId");
        NameSupport.requirePackage(basePackage, "basePackage");
        NameSupport.requireJavaType(application, "applicationName");
        return new ResolvedProject(
                request.outputDirectory().toAbsolutePath().normalize(),
                request.groupId(),
                artifact,
                basePackage,
                application,
                request.projectVersion(),
                request.frameworkVersion(),
                request.springBootVersion(),
                request.contractRepository(),
                contract);
    }

    private static String value(String configured, String fallback) {
        return configured == null ? fallback : configured;
    }
}
