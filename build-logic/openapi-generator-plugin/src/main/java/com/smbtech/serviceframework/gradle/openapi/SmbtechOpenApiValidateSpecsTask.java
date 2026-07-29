package com.smbtech.serviceframework.gradle.openapi;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

/** Validates OpenAPI 3.0 and 3.1 documents used to derive generated artifact coordinates. */
public abstract class SmbtechOpenApiValidateSpecsTask extends DefaultTask {

    private static final Pattern OPENAPI_VERSION = Pattern.compile("3\\.[01]\\.\\d+");
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
                    "spring-boot-service-framework-openapi-generator",
                    "spring-boot-service-framework-openapi-contract-testing",
                    "spring-boot-service-framework-starter-logging",
                    "spring-boot-service-framework-starter-rest-client",
                    "spring-boot-service-framework-starter-mock",
                    "spring-boot-service-framework-starter-error-handling");

    /** Creates the typed OpenAPI validation task. */
    public SmbtechOpenApiValidateSpecsTask() {}

    /**
     * Returns discovered OpenAPI documents.
     *
     * @return specification files
     */
    @InputFiles
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSpecFiles();

    /**
     * Returns the root used to render stable relative paths in diagnostics.
     *
     * @return root directory
     */
    @Internal
    public abstract DirectoryProperty getRootDirectory();

    /** Validates document structure, identity, version, and generated coordinates. */
    @TaskAction
    public void validateSpecs() {
        List<String> failures = new ArrayList<>();
        Map<String, String> resolvedCoordinates = new LinkedHashMap<>();
        getSpecFiles().getFiles().stream()
                .sorted(Comparator.comparing(File::getAbsolutePath))
                .forEach(source -> validateSource(source, resolvedCoordinates, failures));
        if (!failures.isEmpty()) {
            throw new GradleException(
                    "OpenAPI spec validation issues found:\n" + String.join("\n", failures));
        }
    }

    private void validateSource(
            File source, Map<String, String> resolvedCoordinates, List<String> failures) {
        String relativePath = relativePath(source);
        JsonNode root;
        try {
            root = mapper(source).readTree(source);
        } catch (RuntimeException exception) {
            failures.add(
                    relativePath + ": cannot parse OpenAPI document (" + message(exception) + ")");
            return;
        }
        if (root == null || !root.isObject()) {
            failures.add(relativePath + ": OpenAPI document must be an object");
            return;
        }
        String openApiVersion = root.path("openapi").asString().trim();
        if (!OPENAPI_VERSION.matcher(openApiVersion).matches()) {
            failures.add(
                    relativePath + ": openapi must declare a supported 3.0.x or 3.1.x version");
        }
        JsonNode info = root.path("info");
        if (!info.isObject()) {
            failures.add(relativePath + ": info is required");
            return;
        }
        String title = info.path("title").asString().trim();
        String version = info.path("version").asString().trim();
        if (title.isEmpty()) {
            failures.add(relativePath + ": info.title is required");
        }
        if (version.isEmpty()) {
            failures.add(relativePath + ": info.version is required");
        } else if (!ARTIFACT_VERSION.matcher(version).matches()) {
            failures.add(
                    relativePath
                            + ": info.version must be SemVer/Maven compatible, found '"
                            + version
                            + "'");
        }
        if (!title.isEmpty()) {
            validateCoordinates(relativePath, title, version, resolvedCoordinates, failures);
        }
    }

    private static void validateCoordinates(
            String relativePath,
            String title,
            String version,
            Map<String, String> resolvedCoordinates,
            List<String> failures) {
        String artifactBaseName = normalizeTitle(title);
        if (!ARTIFACT_BASE_NAME.matcher(artifactBaseName).matches()) {
            failures.add(
                    relativePath
                            + ": normalized info.title must match [a-z0-9]+(-[a-z0-9]+)*, found '"
                            + artifactBaseName
                            + "'");
            return;
        }
        for (String suffix : List.of("models", "api", "client")) {
            String artifactName = artifactBaseName + "-" + suffix;
            if (FRAMEWORK_ARTIFACT_NAMES.contains(artifactName)) {
                failures.add(
                        relativePath
                                + ": generated artifact "
                                + artifactName
                                + " collides with a framework artifact");
            }
            if (!version.isEmpty()) {
                String coordinate = "com.smbtech.openapi:" + artifactName + ":" + version;
                String previous = resolvedCoordinates.putIfAbsent(coordinate, relativePath);
                if (previous != null) {
                    failures.add(
                            relativePath
                                    + ": generated coordinate "
                                    + coordinate
                                    + " duplicates "
                                    + previous);
                }
            }
        }
    }

    private static String normalizeTitle(String title) {
        return title.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_.]+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-+", "-");
    }

    private static ObjectMapper mapper(File source) {
        return source.getName().toLowerCase(Locale.ROOT).endsWith(".json")
                ? new ObjectMapper()
                : new ObjectMapper(new YAMLFactory());
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

    private static String message(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
