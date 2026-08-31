package com.smbtech.serviceframework.gradle.openapi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

/** Generates one source set from a validated OpenAPI contract. */
public abstract class SmbtechOpenApiGenerateTask extends DefaultTask {

    /** Creates the generation task. */
    public SmbtechOpenApiGenerateTask() {}

    /**
     * Returns the OpenAPI input document.
     *
     * @return OpenAPI input document
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputSpec();

    /**
     * Returns the generated artifact kind.
     *
     * @return generated artifact kind
     */
    @Input
    public abstract Property<OpenApiArtifactKind> getArtifactKind();

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
     * Returns the Maven artifact version.
     *
     * @return Maven artifact version
     */
    @Input
    public abstract Property<String> getArtifactVersion();

    /**
     * Returns the generated model package.
     *
     * @return generated model package
     */
    @Input
    public abstract Property<String> getModelPackage();

    /**
     * Returns the generated API package.
     *
     * @return generated API package
     */
    @Input
    public abstract Property<String> getApiPackage();

    /**
     * Returns the logical client name used by {@code @HttpApiClient}.
     *
     * @return client name
     */
    @Input
    public abstract Property<String> getClientName();

    /**
     * Returns the generated source root.
     *
     * @return generated source root
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /** Generates deterministic Java sources with OpenAPI Generator. */
    @TaskAction
    public void generateSources() {
        File output = getOutputDirectory().get().getAsFile();
        clean(output.toPath());

        OpenApiArtifactKind kind = getArtifactKind().get();
        CodegenConfigurator configurator =
                new CodegenConfigurator()
                        .setGeneratorName("spring")
                        .setInputSpec(getInputSpec().get().getAsFile().getAbsolutePath())
                        .setOutputDir(output.getAbsolutePath())
                        .setGroupId(getGroupId().get())
                        .setArtifactId(getArtifactId().get())
                        .setArtifactVersion(getArtifactVersion().get())
                        .setModelPackage(getModelPackage().get())
                        .setApiPackage(getApiPackage().get())
                        .setInvokerPackage(getApiPackage().get() + ".support")
                        .setValidateSpec(true)
                        .setSkipOverwrite(false)
                        .setEnableMinimalUpdate(false);
        configureCommon(configurator);
        configureKind(configurator, kind);

        new DefaultGenerator().opts(configurator.toClientOptInput()).generate();
        if (kind == OpenApiArtifactKind.CLIENT) {
            annotateClientInterfaces(output.toPath().resolve("src/main/java"));
        }
    }

    private static void configureCommon(CodegenConfigurator configurator) {
        configurator.addAdditionalProperty("useSpringBoot4", true);
        configurator.addAdditionalProperty("useJakartaEe", true);
        configurator.addAdditionalProperty("useBeanValidation", true);
        configurator.addAdditionalProperty("performBeanValidation", true);
        configurator.addAdditionalProperty("useJackson3", true);
        configurator.addAdditionalProperty("openApiNullable", false);
        configurator.addAdditionalProperty("hideGenerationTimestamp", true);
        configurator.addAdditionalProperty("documentationProvider", "none");
        configurator.addAdditionalProperty("annotationLibrary", "none");
        configurator.addAdditionalProperty("useSwaggerUI", false);
        configurator.addAdditionalProperty("useTags", true);
        configurator.addGlobalProperty("modelDocs", "false");
        configurator.addGlobalProperty("modelTests", "false");
        configurator.addGlobalProperty("apiDocs", "false");
        configurator.addGlobalProperty("apiTests", "false");
        configurator.addGlobalProperty("supportingFiles", "false");
    }

    private static void configureKind(
            CodegenConfigurator configurator, OpenApiArtifactKind artifactKind) {
        switch (artifactKind) {
            case MODELS -> {
                configurator.setLibrary("spring-boot");
                configurator.addGlobalProperty("models", "");
                configurator.addGlobalProperty("apis", "false");
            }
            case SERVER_API -> {
                configurator.setLibrary("spring-boot");
                configurator.addGlobalProperty("models", "false");
                configurator.addGlobalProperty("apis", "");
                configurator.addGlobalProperty("supportingFiles", "ApiUtil.java");
                configurator.addAdditionalProperty("interfaceOnly", false);
                configurator.addAdditionalProperty("delegatePattern", true);
                configurator.addAdditionalProperty("skipDefaultInterface", false);
                configurator.addAdditionalProperty("useResponseEntity", true);
            }
            case CLIENT -> {
                configurator.setLibrary("spring-http-interface");
                configurator.addGlobalProperty("models", "false");
                configurator.addGlobalProperty("apis", "");
                configurator.addAdditionalProperty("interfaceOnly", true);
            }
        }
    }

    private void annotateClientInterfaces(Path sourceRoot) {
        String template = readAnnotationTemplate();
        String annotation =
                template.lines().filter(line -> line.startsWith("@")).findFirst().orElseThrow();
        String importLine =
                template.lines()
                        .filter(line -> line.startsWith("import "))
                        .findFirst()
                        .orElseThrow();
        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.getFileName().toString().endsWith("Api.java"))
                    .forEach(path -> annotate(path, importLine, annotation));
        } catch (IOException exception) {
            throw new GradleException(
                    "Cannot customize generated HTTP client interfaces", exception);
        }
    }

    private void annotate(Path source, String importLine, String annotation) {
        try {
            String content = Files.readString(source, StandardCharsets.UTF_8);
            String resolvedAnnotation = annotation.replace("{{clientName}}", getClientName().get());
            if (!content.contains(importLine)) {
                content = content.replaceFirst("(?m)^import ", importLine + "\n\nimport ");
            }
            if (!content.contains(resolvedAnnotation)) {
                content =
                        content.replaceFirst(
                                "(?m)^public interface ",
                                resolvedAnnotation + "\npublic interface ");
            }
            Files.writeString(source, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot customize generated source " + source, exception);
        }
    }

    private static String readAnnotationTemplate() {
        try (var input =
                SmbtechOpenApiGenerateTask.class.getResourceAsStream(
                        "/smbtech-openapi/client-interface-annotation.mustache")) {
            if (input == null) {
                throw new GradleException("Corporate OpenAPI client template is missing");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot read corporate OpenAPI client template", exception);
        }
    }

    private static void clean(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(SmbtechOpenApiGenerateTask::delete);
        } catch (IOException exception) {
            throw new GradleException(
                    "Cannot clean generated source directory " + directory, exception);
        }
    }

    private static void delete(Path path) {
        try {
            Files.delete(path);
        } catch (IOException exception) {
            throw new GradleException("Cannot delete " + path, exception);
        }
    }
}
