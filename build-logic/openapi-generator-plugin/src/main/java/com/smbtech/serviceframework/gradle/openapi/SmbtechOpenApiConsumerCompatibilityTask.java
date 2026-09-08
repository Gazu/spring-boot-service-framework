package com.smbtech.serviceframework.gradle.openapi;

import java.io.DataInputStream;
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

    private static final int CLASS_FILE_MAGIC = 0xCAFEBABE;
    private static final int CLASS_ACCESS_INTERFACE = 0x0200;
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
            String openFeignPackage = values[5];
            String contractId = values[6];
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
                    openFeignPackage,
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
            String openFeignPackage,
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
            if (kind == OpenApiArtifactKind.SERVER_API) {
                requireClass(classes, "Api.class", "API interface", artifactId, failures);
                requireClass(
                        classes,
                        "ApiController.class",
                        "Spring API controller",
                        artifactId,
                        failures);
                requireClass(
                        classes,
                        "ApiDelegate.class",
                        "Spring delegate interface",
                        artifactId,
                        failures);
                requireClass(classes, "ApiUtil.class", "ApiUtil support", artifactId, failures);
            }
            if (kind == OpenApiArtifactKind.CLIENT) {
                String openFeignPath = openFeignPackage.replace('.', '/') + "/";
                List<String> httpInterfaces = apiInterfaces(classes, apiPath);
                List<String> openFeignInterfaces = apiInterfaces(classes, openFeignPath);
                if (httpInterfaces.isEmpty()) {
                    failures.add(artifactId + ": no generated HTTP interfaces found");
                }
                if (openFeignInterfaces.isEmpty()) {
                    failures.add(artifactId + ": no generated OpenFeign interfaces found");
                }
                if (!httpInterfaces.equals(openFeignInterfaces)) {
                    failures.add(
                            artifactId
                                    + ": HTTP Interface and OpenFeign API families differ: "
                                    + httpInterfaces
                                    + " != "
                                    + openFeignInterfaces);
                }
                if (classes.stream()
                        .anyMatch(
                                name ->
                                        name.endsWith("ApiController.class")
                                                || name.endsWith("ApiDelegate.class")
                                                || name.endsWith("ApiUtil.class"))) {
                    failures.add(artifactId + ": contains server API implementation types");
                }
                if (classes.stream()
                        .filter(name -> name.startsWith(apiPath) || name.startsWith(openFeignPath))
                        .anyMatch(name -> !name.endsWith("Api.class"))) {
                    failures.add(artifactId + ": contains non-interface client support types");
                }
                for (String interfaceClass : httpInterfaces) {
                    String entry = apiPath + interfaceClass;
                    requireInterface(jar, entry, artifactId, failures);
                    requireMarker(
                            jar, entry, "HttpApiClient", "@HttpApiClient", artifactId, failures);
                    requireMarker(
                            jar, entry, "HttpExchange", "@HttpExchange", artifactId, failures);
                    rejectMarker(jar, entry, "FeignClient", "@FeignClient", artifactId, failures);
                }
                for (String interfaceClass : openFeignInterfaces) {
                    String entry = openFeignPath + interfaceClass;
                    String interfaceName =
                            interfaceClass.substring(
                                    interfaceClass.lastIndexOf('/') + 1,
                                    interfaceClass.length() - ".class".length());
                    String contextId =
                            contractId
                                    + "-"
                                    + OpenApiArtifactContract.versionSegment(version)
                                    + "-"
                                    + interfaceName;
                    requireInterface(jar, entry, artifactId, failures);
                    requireMarker(jar, entry, "FeignClient", "@FeignClient", artifactId, failures);
                    requireMarker(
                            jar, entry, "RequestMapping", "request mappings", artifactId, failures);
                    requireMarker(
                            jar, entry, contextId, "contextId " + contextId, artifactId, failures);
                    rejectMarker(
                            jar, entry, "HttpApiClient", "@HttpApiClient", artifactId, failures);
                }
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

    private static boolean contains(JarFile jar, String entryName, String value)
            throws IOException {
        byte[] marker = value.getBytes(StandardCharsets.UTF_8);
        JarEntry entry = jar.getJarEntry(entryName);
        if (entry == null) {
            return false;
        }
        try (InputStream input = jar.getInputStream(entry)) {
            return indexOf(input.readAllBytes(), marker) >= 0;
        }
    }

    private static void requireMarker(
            JarFile jar,
            String entry,
            String marker,
            String description,
            String artifactId,
            List<String> failures)
            throws IOException {
        if (!contains(jar, entry, marker)) {
            failures.add(artifactId + ": " + entry + " lacks " + description);
        }
    }

    private static void rejectMarker(
            JarFile jar,
            String entry,
            String marker,
            String description,
            String artifactId,
            List<String> failures)
            throws IOException {
        if (contains(jar, entry, marker)) {
            failures.add(artifactId + ": " + entry + " contains " + description);
        }
    }

    private static void requireInterface(
            JarFile jar, String entry, String artifactId, List<String> failures)
            throws IOException {
        if (!isInterface(jar, entry)) {
            failures.add(artifactId + ": " + entry + " is not a Java interface");
        }
    }

    private static boolean isInterface(JarFile jar, String entryName) throws IOException {
        JarEntry entry = jar.getJarEntry(entryName);
        if (entry == null) {
            return false;
        }
        try (DataInputStream input = new DataInputStream(jar.getInputStream(entry))) {
            if (input.readInt() != CLASS_FILE_MAGIC) {
                return false;
            }
            input.readUnsignedShort();
            input.readUnsignedShort();
            int constantPoolSize = input.readUnsignedShort();
            for (int index = 1; index < constantPoolSize; index++) {
                int tag = input.readUnsignedByte();
                switch (tag) {
                    case 1 -> input.skipNBytes(input.readUnsignedShort());
                    case 3, 4 -> input.skipNBytes(4);
                    case 5, 6 -> {
                        input.skipNBytes(8);
                        index++;
                    }
                    case 7, 8, 16, 19, 20 -> input.skipNBytes(2);
                    case 9, 10, 11, 12, 17, 18 -> input.skipNBytes(4);
                    case 15 -> input.skipNBytes(3);
                    default -> throw new IOException("Unsupported class constant pool tag " + tag);
                }
            }
            return (input.readUnsignedShort() & CLASS_ACCESS_INTERFACE) != 0;
        }
    }

    private static List<String> apiInterfaces(List<String> classes, String packagePath) {
        return classes.stream()
                .filter(name -> name.startsWith(packagePath) && name.endsWith("Api.class"))
                .map(name -> name.substring(packagePath.length()))
                .sorted()
                .toList();
    }

    private static void requireClass(
            List<String> classes,
            String suffix,
            String description,
            String artifactId,
            List<String> failures) {
        if (classes.stream().noneMatch(name -> name.endsWith(suffix))) {
            failures.add(artifactId + ": no generated " + description + " found");
        }
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
