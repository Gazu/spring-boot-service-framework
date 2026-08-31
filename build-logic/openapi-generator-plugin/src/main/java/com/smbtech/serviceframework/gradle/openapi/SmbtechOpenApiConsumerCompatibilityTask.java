package com.smbtech.serviceframework.gradle.openapi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Verifies generated JAR separation, metadata, and consumer-visible contracts. */
public abstract class SmbtechOpenApiConsumerCompatibilityTask extends DefaultTask {

    private static final String METADATA = "META-INF/smbtech/openapi/contract.properties";

    /** Creates the consumer compatibility task. */
    public SmbtechOpenApiConsumerCompatibilityTask() {}

    /**
     * Returns the generated contract JARs.
     *
     * @return generated contract JARs
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract ConfigurableFileCollection getArtifacts();

    /**
     * Returns expected artifact descriptors.
     *
     * @return expected artifact descriptors
     */
    @Input
    public abstract ListProperty<String> getArtifactDescriptors();

    /**
     * Returns the compatibility report.
     *
     * @return compatibility report
     */
    @OutputFile
    public abstract RegularFileProperty getReportFile();

    /** Validates every generated artifact as a standalone consumer dependency. */
    @TaskAction
    public void verifyConsumers() {
        Map<String, File> artifacts = new HashMap<>();
        getArtifacts().getFiles().stream()
                .filter(File::isFile)
                .forEach(file -> artifacts.put(file.getName(), file));
        List<String> failures = new ArrayList<>();
        List<String> verified = new ArrayList<>();
        for (String descriptor : getArtifactDescriptors().getOrElse(List.of())) {
            String[] values = descriptor.split("\\|", -1);
            String artifactId = values[0];
            String version = values[1];
            OpenApiArtifactKind kind = OpenApiArtifactKind.valueOf(values[2]);
            String modelPackage = values[3];
            String apiPackage = values[4];
            String contractId = values[5];
            File artifact = artifacts.get(artifactId + "-" + version + ".jar");
            if (artifact == null) {
                failures.add(artifactId + ": generated JAR is missing");
                continue;
            }
            inspect(
                    artifact,
                    artifactId,
                    version,
                    kind,
                    modelPackage,
                    apiPackage,
                    contractId,
                    failures);
            verified.add(artifact.getName());
        }
        writeReport(verified, failures);
        if (!failures.isEmpty()) {
            throw new GradleException(
                    "OpenAPI consumer compatibility issues found:\n- "
                            + String.join("\n- ", failures));
        }
    }

    private static void inspect(
            File artifact,
            String artifactId,
            String version,
            OpenApiArtifactKind kind,
            String modelPackage,
            String apiPackage,
            String contractId,
            List<String> failures) {
        try (JarFile jar = new JarFile(artifact)) {
            Properties metadata = properties(jar, failures, artifactId);
            expect(metadata, "artifact.id", artifactId, failures);
            expect(metadata, "artifact.kind", kind.artifactSuffix(), failures);
            expect(metadata, "contract.version", version, failures);
            expect(metadata, "contract.id", contractId, failures);
            String uniqueContract =
                    "META-INF/smbtech/openapi/contracts/"
                            + contractId
                            + "/"
                            + version
                            + "/contract.yaml";
            if (jar.getEntry(uniqueContract) == null) {
                failures.add(artifactId + ": missing unique embedded contract " + uniqueContract);
            }

            List<String> classes =
                    jar.stream()
                            .map(JarEntry::getName)
                            .filter(name -> name.endsWith(".class"))
                            .toList();
            String modelPath = modelPackage.replace('.', '/') + "/";
            String apiPath = apiPackage.replace('.', '/') + "/";
            if (kind == OpenApiArtifactKind.MODELS) {
                if (classes.stream().noneMatch(name -> name.startsWith(modelPath))) {
                    failures.add(artifactId + ": no generated model classes found");
                }
            } else {
                if (classes.stream().anyMatch(name -> name.startsWith(modelPath))) {
                    failures.add(artifactId + ": duplicates model classes");
                }
                if (classes.stream().noneMatch(name -> name.startsWith(apiPath))) {
                    failures.add(artifactId + ": no generated API classes found");
                }
            }
            if (kind == OpenApiArtifactKind.SERVER_API
                    && classes.stream().noneMatch(name -> name.endsWith("ApiDelegate.class"))) {
                failures.add(artifactId + ": no Spring delegate interface found");
            }
            if (kind == OpenApiArtifactKind.CLIENT && !contains(jar, "HttpApiClient")) {
                failures.add(artifactId + ": generated HTTP interface lacks @HttpApiClient");
            }
        } catch (IOException exception) {
            failures.add(artifactId + ": cannot inspect JAR: " + exception.getMessage());
        }
    }

    private static Properties properties(JarFile jar, List<String> failures, String artifactId)
            throws IOException {
        Properties properties = new Properties();
        JarEntry entry = jar.getJarEntry(METADATA);
        if (entry == null) {
            failures.add(artifactId + ": missing " + METADATA);
            return properties;
        }
        try (InputStream input = jar.getInputStream(entry)) {
            properties.load(input);
        }
        return properties;
    }

    private static boolean contains(JarFile jar, String value) throws IOException {
        byte[] marker = value.getBytes(StandardCharsets.UTF_8);
        for (JarEntry entry :
                jar.stream().filter(item -> item.getName().endsWith(".class")).toList()) {
            try (InputStream input = jar.getInputStream(entry)) {
                byte[] content = input.readAllBytes();
                if (indexOf(content, marker) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int indexOf(byte[] content, byte[] marker) {
        for (int index = 0; index <= content.length - marker.length; index++) {
            int offset = 0;
            while (offset < marker.length && content[index + offset] == marker[offset]) {
                offset++;
            }
            if (offset == marker.length) {
                return index;
            }
        }
        return -1;
    }

    private static void expect(
            Properties properties, String key, String expected, List<String> failures) {
        if (!expected.equals(properties.getProperty(key))) {
            failures.add(key + ": expected '" + expected + "'");
        }
    }

    private void writeReport(List<String> verified, List<String> failures) {
        verified.sort(Comparator.naturalOrder());
        String report =
                "verified="
                        + verified.size()
                        + "\n"
                        + String.join("\n", verified)
                        + (failures.isEmpty() ? "\nstatus=compatible\n" : "\nstatus=failed\n");
        try {
            Files.createDirectories(getReportFile().get().getAsFile().toPath().getParent());
            Files.writeString(
                    getReportFile().get().getAsFile().toPath(), report, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException(
                    "Cannot write OpenAPI consumer compatibility report", exception);
        }
    }
}
