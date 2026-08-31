package com.smbtech.serviceframework.gradle.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Writes reproducible contract metadata embedded in generated artifacts. */
public abstract class SmbtechOpenApiMetadataTask extends DefaultTask {

    /** Creates the metadata task. */
    public SmbtechOpenApiMetadataTask() {}

    /**
     * Returns the source OpenAPI document.
     *
     * @return source OpenAPI document
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputSpec();

    /**
     * Returns the original contract title.
     *
     * @return original contract title
     */
    @Input
    public abstract Property<String> getContractTitle();

    /**
     * Returns the contract version.
     *
     * @return contract version
     */
    @Input
    public abstract Property<String> getContractVersion();

    /**
     * Returns the Maven group identifier.
     *
     * @return Maven group identifier
     */
    @Input
    public abstract Property<String> getGroupId();

    /**
     * Returns the Maven artifact identifier.
     *
     * @return Maven artifact identifier
     */
    @Input
    public abstract Property<String> getArtifactId();

    /**
     * Returns the normalized contract identifier.
     *
     * @return normalized contract identifier
     */
    @Input
    public abstract Property<String> getContractId();

    /**
     * Returns the generated artifact kind.
     *
     * @return generated artifact kind
     */
    @Input
    public abstract Property<OpenApiArtifactKind> getArtifactKind();

    /**
     * Returns the framework generator version.
     *
     * @return framework generator version
     */
    @Input
    public abstract Property<String> getFrameworkVersion();

    /**
     * Returns the OpenAPI Generator version.
     *
     * @return OpenAPI Generator version
     */
    @Input
    public abstract Property<String> getOpenApiGeneratorVersion();

    /**
     * Returns the Spring Boot compatibility version.
     *
     * @return Spring Boot compatibility version
     */
    @Input
    public abstract Property<String> getSpringBootVersion();

    /**
     * Returns the metadata resource root.
     *
     * @return metadata resource root
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /** Writes the original specification and stable metadata properties. */
    @TaskAction
    public void writeMetadata() {
        var compatibilityRoot =
                getOutputDirectory().get().getAsFile().toPath().resolve("META-INF/smbtech/openapi");
        var contractRoot =
                compatibilityRoot
                        .resolve("contracts")
                        .resolve(getContractId().get())
                        .resolve(getContractVersion().get());
        try {
            Files.createDirectories(compatibilityRoot);
            Files.createDirectories(contractRoot);
            Files.copy(
                    getInputSpec().get().getAsFile().toPath(),
                    compatibilityRoot.resolve("contract.yaml"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(
                    compatibilityRoot.resolve("contract.properties"),
                    renderMetadata(),
                    StandardCharsets.UTF_8);
            Files.copy(
                    getInputSpec().get().getAsFile().toPath(),
                    contractRoot.resolve("contract.yaml"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(
                    contractRoot.resolve("contract.properties"),
                    renderMetadata(),
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot write generated OpenAPI metadata", exception);
        }
    }

    private String renderMetadata() {
        Map<String, String> values = new TreeMap<>();
        values.put("artifact.group", getGroupId().get());
        values.put("artifact.id", getArtifactId().get());
        values.put("artifact.kind", getArtifactKind().get().artifactSuffix());
        values.put("contract.sha256", sha256());
        values.put("contract.id", getContractId().get());
        values.put("contract.title", getContractTitle().get());
        values.put("contract.version", getContractVersion().get());
        values.put("framework.version", getFrameworkVersion().get());
        values.put("generator.name", "openapi-generator");
        values.put("generator.version", getOpenApiGeneratorVersion().get());
        values.put("spring-boot.version", getSpringBootVersion().get());
        return values.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + escape(entry.getValue()))
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse("")
                + "\n";
    }

    private String sha256() {
        try (InputStream input = Files.newInputStream(getInputSpec().get().getAsFile().toPath())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new GradleException("Cannot calculate OpenAPI contract digest", exception);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
}
