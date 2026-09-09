package com.smbtech.serviceframework.gradle.openapi;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Validates OpenAPI 3.0 and 3.1 documents and their effective artifact coordinates. */
public abstract class SmbtechOpenApiValidateSpecsTask extends DefaultTask {

    private static final Pattern ARTIFACT_VERSION =
            Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][0-9A-Za-z][0-9A-Za-z.-]*)?");
    private static final Pattern ARTIFACT_BASE_NAME = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Set<String> FRAMEWORK_ARTIFACT_NAMES =
            Set.of(
                    "spring-boot-service-framework-commons",
                    "spring-boot-service-framework-logging-core",
                    "spring-boot-service-framework-http-client-core",
                    "spring-boot-service-framework-mock-core",
                    "spring-boot-service-framework-error-core",
                    "spring-boot-service-framework-openapi-gradle-plugin",
                    "spring-boot-service-framework-openapi-contract-testing",
                    "spring-boot-service-framework-starter-logging",
                    "spring-boot-service-framework-starter-rest-client",
                    "spring-boot-service-framework-starter-mock",
                    "spring-boot-service-framework-starter-error-handling");

    /** Creates the typed OpenAPI validation task. */
    public SmbtechOpenApiValidateSpecsTask() {}

    /**
     * Returns all discovered and configured documents.
     *
     * @return OpenAPI documents
     */
    @InputFiles
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSpecFiles();

    /**
     * Returns the default generated artifact group.
     *
     * @return Maven group identifier
     */
    @Input
    public abstract Property<String> getDefaultGroupId();

    /**
     * Returns effective named contract configurations.
     *
     * @return serialized configurations
     */
    @Input
    public abstract ListProperty<String> getSpecConfigurations();

    /**
     * Returns the root used for stable diagnostics.
     *
     * @return root directory
     */
    @Internal
    public abstract DirectoryProperty getRootDirectory();

    /** Validates document structure, operation identifiers, versions, and coordinates. */
    @TaskAction
    public void validateSpecs() {
        List<String> failures = new ArrayList<>();
        Map<String, String> resolvedContractIdentities = new LinkedHashMap<>();
        Map<String, String> resolvedCoordinates = new LinkedHashMap<>();
        Map<String, String> resolvedArtifactFiles = new LinkedHashMap<>();
        Map<String, EffectiveSpec> configuredSpecs = configuredSpecs();
        getSpecFiles().getFiles().stream()
                .sorted(Comparator.comparing(File::getAbsolutePath))
                .forEach(
                        source ->
                                validateSource(
                                        source,
                                        configuredSpecs.get(
                                                OpenApiSpecDiscovery.normalizedPath(source)
                                                        .toString()),
                                        resolvedContractIdentities,
                                        resolvedCoordinates,
                                        resolvedArtifactFiles,
                                        failures));
        if (!failures.isEmpty()) {
            throw new GradleException(
                    "OpenAPI spec validation issues found:\n" + String.join("\n", failures));
        }
    }

    private void validateSource(
            File source,
            EffectiveSpec configured,
            Map<String, String> resolvedContractIdentities,
            Map<String, String> resolvedCoordinates,
            Map<String, String> resolvedArtifactFiles,
            List<String> failures) {
        String relativePath = relativePath(source);
        OpenApiContractIdentity identity;
        try {
            identity = OpenApiContractReader.read(source);
        } catch (IllegalArgumentException exception) {
            failures.add(relativePath + ": " + exception.getMessage());
            return;
        }

        String contractIdentity = identity.artifactBaseName() + ":" + identity.version();
        String previousIdentity =
                resolvedContractIdentities.putIfAbsent(contractIdentity, relativePath);
        if (previousIdentity != null) {
            failures.add(
                    relativePath
                            + ": contract identity "
                            + contractIdentity
                            + " duplicates "
                            + previousIdentity);
        }

        String groupId = configured == null ? getDefaultGroupId().get() : configured.groupId();
        String artifactBaseName =
                configured == null || configured.artifactBaseName().isBlank()
                        ? identity.artifactBaseName()
                        : configured.artifactBaseName();
        String version = identity.version();
        if (!ARTIFACT_BASE_NAME.matcher(artifactBaseName).matches()) {
            failures.add(relativePath + ": artifact base name is invalid: " + artifactBaseName);
            return;
        }
        if (configured != null && !configured.version().isBlank()) {
            if (!ARTIFACT_VERSION.matcher(configured.version()).matches()) {
                failures.add(
                        relativePath
                                + ": configured artifact version is invalid: "
                                + configured.version());
            }
            if (!configured.version().equals(identity.version())) {
                failures.add(
                        relativePath
                                + ": configured version '"
                                + configured.version()
                                + "' must match OpenAPI info.version '"
                                + identity.version()
                                + "'");
            }
        }
        if (configured != null) {
            validatePackages(relativePath, identity, artifactBaseName, configured, failures);
        }
        Map<OpenApiArtifactKind, Boolean> enabled =
                configured == null
                        ? Map.of(
                                OpenApiArtifactKind.MODELS,
                                true,
                                OpenApiArtifactKind.SERVER_API,
                                true,
                                OpenApiArtifactKind.CLIENT,
                                true)
                        : configured.enabled();
        enabled.forEach(
                (kind, isEnabled) -> {
                    if (isEnabled) {
                        validateCoordinate(
                                relativePath,
                                groupId,
                                OpenApiArtifactContract.artifactId(artifactBaseName, kind),
                                version,
                                resolvedCoordinates,
                                resolvedArtifactFiles,
                                failures);
                    }
                });
    }

    private static void validateCoordinate(
            String relativePath,
            String groupId,
            String artifactName,
            String version,
            Map<String, String> resolvedCoordinates,
            Map<String, String> resolvedArtifactFiles,
            List<String> failures) {
        if (FRAMEWORK_ARTIFACT_NAMES.contains(artifactName)) {
            failures.add(relativePath + ": generated artifact collides with " + artifactName);
        }
        String coordinate = groupId + ":" + artifactName + ":" + version;
        String previous = resolvedCoordinates.putIfAbsent(coordinate, relativePath);
        if (previous != null) {
            failures.add(
                    relativePath
                            + ": generated coordinate "
                            + coordinate
                            + " duplicates "
                            + previous);
        }
        String artifactFile = artifactName + "-" + version + ".jar";
        String previousArtifact = resolvedArtifactFiles.putIfAbsent(artifactFile, relativePath);
        if (previousArtifact != null) {
            failures.add(
                    relativePath
                            + ": generated artifact file "
                            + artifactFile
                            + " duplicates "
                            + previousArtifact
                            + "; output file names do not include groupId");
        }
    }

    private Map<String, EffectiveSpec> configuredSpecs() {
        Map<String, EffectiveSpec> specs = new HashMap<>();
        for (String value : getSpecConfigurations().get()) {
            String[] fields = value.split("\\|", -1);
            if (fields.length != 12 || fields[1].isBlank()) {
                continue;
            }
            String groupId = fields[2].isBlank() ? getDefaultGroupId().get() : fields[2];
            String sourcePath = OpenApiSpecDiscovery.normalizedPath(new File(fields[1])).toString();
            EffectiveSpec effective =
                    new EffectiveSpec(
                            groupId,
                            fields[3],
                            fields[4],
                            fields[5],
                            fields[6],
                            fields[7],
                            fields[8],
                            Map.of(
                                    OpenApiArtifactKind.MODELS,
                                    Boolean.parseBoolean(fields[9]),
                                    OpenApiArtifactKind.SERVER_API,
                                    Boolean.parseBoolean(fields[10]),
                                    OpenApiArtifactKind.CLIENT,
                                    Boolean.parseBoolean(fields[11])));
            EffectiveSpec previous = specs.putIfAbsent(sourcePath, effective);
            if (previous != null) {
                throw new GradleException(
                        "OpenAPI input " + sourcePath + " is configured more than once");
            }
        }
        return specs;
    }

    private String relativePath(File source) {
        return getRootDirectory()
                .get()
                .getAsFile()
                .toPath()
                .relativize(source.toPath())
                .toString()
                .replace(File.separatorChar, '/');
    }

    private static void validatePackages(
            String relativePath,
            OpenApiContractIdentity identity,
            String artifactBaseName,
            EffectiveSpec configured,
            List<String> failures) {
        String packageRoot =
                configured.basePackage().isBlank()
                        ? OpenApiArtifactContract.defaultPackageRoot(artifactBaseName)
                        : configured.basePackage();
        if (packageRoot.matches(".*\\.v[0-9]+$")) {
            failures.add(
                    relativePath
                            + ": basePackage must not include a version segment; it is derived from OpenAPI info.version");
        }
        String versionedRoot =
                OpenApiArtifactContract.versionedPackageRoot(packageRoot, identity.version());
        validatePackageOverride(
                relativePath,
                "modelPackage",
                configured.modelPackage(),
                versionedRoot + ".model",
                failures);
        validatePackageOverride(
                relativePath,
                "serverApiPackage",
                configured.serverApiPackage(),
                versionedRoot + ".api",
                failures);
        validatePackageOverride(
                relativePath,
                "clientPackage",
                configured.clientPackage(),
                versionedRoot + ".client",
                failures);
    }

    private static void validatePackageOverride(
            String relativePath,
            String property,
            String configured,
            String expected,
            List<String> failures) {
        if (!configured.isBlank() && !configured.equals(expected)) {
            failures.add(
                    relativePath
                            + ": "
                            + property
                            + " must be '"
                            + expected
                            + "' because generated packages follow OpenAPI info.version");
        }
    }

    private record EffectiveSpec(
            String groupId,
            String artifactBaseName,
            String version,
            String basePackage,
            String modelPackage,
            String serverApiPackage,
            String clientPackage,
            Map<OpenApiArtifactKind, Boolean> enabled) {}
}
