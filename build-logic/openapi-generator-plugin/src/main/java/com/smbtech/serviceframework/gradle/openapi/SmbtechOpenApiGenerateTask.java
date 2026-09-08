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

    private static final String HTTP_INTERFACE_LIBRARY = "spring-http-interface";
    private static final String OPENFEIGN_LIBRARY = "spring-cloud";

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
     * Returns the generated Spring Cloud OpenFeign package.
     *
     * @return generated OpenFeign package
     */
    @Input
    public abstract Property<String> getOpenFeignPackage();

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
        if (kind == OpenApiArtifactKind.CLIENT) {
            generate(output, kind, getApiPackage().get(), HTTP_INTERFACE_LIBRARY);
            generate(output, kind, getOpenFeignPackage().get(), OPENFEIGN_LIBRARY);
            Path sourceRoot = output.toPath().resolve("src/main/java");
            annotateClientInterfaces(
                    sourceRoot,
                    getApiPackage().get(),
                    "/smbtech-openapi/client-interface-annotation.mustache");
            annotateClientInterfaces(
                    sourceRoot,
                    getOpenFeignPackage().get(),
                    "/smbtech-openapi/openfeign-client-interface-annotation.mustache");
            return;
        }
        generate(output, kind, getApiPackage().get(), null);
    }

    private void generate(
            File output, OpenApiArtifactKind kind, String apiPackage, String clientLibrary) {
        CodegenConfigurator configurator =
                new CodegenConfigurator()
                        .setGeneratorName("spring")
                        .setInputSpec(getInputSpec().get().getAsFile().getAbsolutePath())
                        .setOutputDir(output.getAbsolutePath())
                        .setGroupId(getGroupId().get())
                        .setArtifactId(getArtifactId().get())
                        .setArtifactVersion(getArtifactVersion().get())
                        .setModelPackage(getModelPackage().get())
                        .setApiPackage(apiPackage)
                        .setInvokerPackage(apiPackage + ".support")
                        .setValidateSpec(true)
                        .setSkipOverwrite(false)
                        .setEnableMinimalUpdate(false);
        configureCommon(configurator);
        configureKind(configurator, kind, clientLibrary);

        new DefaultGenerator().opts(configurator.toClientOptInput()).generate();
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
            CodegenConfigurator configurator,
            OpenApiArtifactKind artifactKind,
            String clientLibrary) {
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
                configurator.setLibrary(clientLibrary);
                configurator.addGlobalProperty("models", "false");
                configurator.addGlobalProperty("apis", "");
                configurator.addAdditionalProperty("interfaceOnly", true);
                if (OPENFEIGN_LIBRARY.equals(clientLibrary)) {
                    configurator.addAdditionalProperty("skipDefaultInterface", true);
                    configurator.addAdditionalProperty("singleContentTypes", true);
                    configurator.addAdditionalProperty("requestMappingMode", "none");
                }
            }
        }
    }

    private void annotateClientInterfaces(
            Path sourceRoot, String apiPackage, String templateResource) {
        String template = readAnnotationTemplate(templateResource);
        String annotation =
                template.lines().filter(line -> line.startsWith("@")).findFirst().orElseThrow();
        String importLine =
                template.lines()
                        .filter(line -> line.startsWith("import "))
                        .findFirst()
                        .orElseThrow();
        Path packageRoot = packageRoot(sourceRoot, apiPackage);
        try (var files = Files.walk(packageRoot)) {
            files.filter(path -> path.getFileName().toString().endsWith("Api.java"))
                    .forEach(path -> annotate(path, importLine, annotation));
        } catch (IOException exception) {
            throw new GradleException(
                    "Cannot customize generated client interfaces in " + apiPackage, exception);
        }
    }

    private void annotate(Path source, String importLine, String annotation) {
        try {
            String content = Files.readString(source, StandardCharsets.UTF_8);
            String interfaceName = source.getFileName().toString().replaceFirst("\\.java$", "");
            String resolvedAnnotation =
                    annotation
                            .replace("{{clientName}}", getClientName().get())
                            .replace(
                                    "{{versionSegment}}",
                                    OpenApiArtifactContract.versionSegment(
                                            getArtifactVersion().get()))
                            .replace("{{interfaceName}}", interfaceName);
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

    private static String readAnnotationTemplate(String templateResource) {
        try (var input = SmbtechOpenApiGenerateTask.class.getResourceAsStream(templateResource)) {
            if (input == null) {
                throw new GradleException("Corporate OpenAPI client template is missing");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot read corporate OpenAPI client template", exception);
        }
    }

    private static Path packageRoot(Path sourceRoot, String packageName) {
        Path result = sourceRoot;
        for (String segment : packageName.split("\\.")) {
            result = result.resolve(segment);
        }
        return result;
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
