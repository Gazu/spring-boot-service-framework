package com.smbtech.serviceframework.project.generator;

import io.spring.initializr.generator.buildsystem.gradle.GradleBuildSystem;
import io.spring.initializr.generator.configuration.format.yaml.YamlFormat;
import io.spring.initializr.generator.io.IndentingWriterFactory;
import io.spring.initializr.generator.io.template.MustacheTemplateRenderer;
import io.spring.initializr.generator.language.java.JavaLanguage;
import io.spring.initializr.generator.packaging.jar.JarPackaging;
import io.spring.initializr.generator.project.DefaultProjectAssetGenerator;
import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.project.ProjectGenerator;
import io.spring.initializr.generator.version.Version;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.springframework.context.support.StaticApplicationContext;

/** Framework default project generator. */
final class DefaultHexagonalProjectGenerator implements HexagonalProjectGenerator {

    private final ContractDescriptorLoader descriptorLoader = new ContractDescriptorLoader();

    DefaultHexagonalProjectGenerator() {}

    @Override
    public GeneratedProject generate(ProjectGenerationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        ContractDescriptor contract = descriptorLoader.load(request);
        ResolvedProject project = ResolvedProject.from(request, contract);
        prepareOutput(project.outputDirectory(), request.overwrite());

        try (StaticApplicationContext parent = parentContext()) {
            ProjectGenerator generator =
                    new ProjectGenerator(
                            context -> {
                                context.setParent(parent);
                                context.registerBean(
                                        HexagonalProjectContributor.class,
                                        () -> new HexagonalProjectContributor(project));
                            });
            Path directory =
                    generator.generate(
                            description(project),
                            new DefaultProjectAssetGenerator(ignored -> project.outputDirectory()));
            return new GeneratedProject(
                    directory.toAbsolutePath().normalize(),
                    contract.id(),
                    contract.version(),
                    contract.coordinate(),
                    contract.delegateTypes());
        } catch (RuntimeException exception) {
            if (exception instanceof ProjectGenerationException generationException) {
                throw generationException;
            }
            throw new ProjectGenerationException(
                    "Cannot generate project in " + project.outputDirectory(), exception);
        }
    }

    private static MutableProjectDescription description(ResolvedProject project) {
        MutableProjectDescription description = new MutableProjectDescription();
        description.setGroupId(project.groupId());
        description.setArtifactId(project.artifactId());
        description.setVersion(project.projectVersion());
        description.setName(project.artifactId());
        description.setDescription(
                "Spring Boot service generated from " + project.contract().title());
        description.setApplicationName(project.applicationName());
        description.setPackageName(project.basePackage());
        description.setPlatformVersion(Version.parse(project.springBootVersion()));
        description.setLanguage(new JavaLanguage("21"));
        description.setBuildSystem(new GradleBuildSystem());
        description.setPackaging(new JarPackaging());
        description.setConfigurationFileFormat(new YamlFormat());
        return description;
    }

    private static StaticApplicationContext parentContext() {
        StaticApplicationContext context = new StaticApplicationContext();
        context.registerBean(InitializrMetadata.class, DefaultHexagonalProjectGenerator::metadata);
        context.registerBean(
                IndentingWriterFactory.class, IndentingWriterFactory::withDefaultSettings);
        context.registerBean(
                MustacheTemplateRenderer.class,
                () -> new MustacheTemplateRenderer("classpath:/templates"));
        context.refresh();
        return context;
    }

    private static InitializrMetadata metadata() {
        return InitializrMetadataBuilder.create()
                .withCustomizer(
                        metadata ->
                                metadata.getConfiguration()
                                        .getEnv()
                                        .getGradle()
                                        .setDependencyManagementPluginVersion("1.1.7"))
                .build();
    }

    private static void prepareOutput(Path output, boolean overwrite) {
        if (!Files.exists(output)) {
            return;
        }
        try (var entries = Files.list(output)) {
            if (entries.findAny().isEmpty()) {
                return;
            }
        } catch (IOException exception) {
            throw new ProjectGenerationException(
                    "Cannot inspect output directory " + output, exception);
        }
        if (!overwrite) {
            throw new ProjectGenerationException(
                    "Output directory is not empty: "
                            + output
                            + ". Use overwrite only for disposable scaffolds.");
        }
        Path normalized = output.toAbsolutePath().normalize();
        if (normalized.getParent() == null
                || normalized.equals(Path.of(System.getProperty("user.home")))) {
            throw new ProjectGenerationException(
                    "Refusing to replace unsafe output directory " + normalized);
        }
        try (var paths = Files.walk(normalized)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(DefaultHexagonalProjectGenerator::delete);
        } catch (IOException exception) {
            throw new ProjectGenerationException(
                    "Cannot replace output directory " + normalized, exception);
        }
    }

    private static void delete(Path path) {
        try {
            Files.delete(path);
        } catch (IOException exception) {
            throw new ProjectGenerationException("Cannot delete " + path, exception);
        }
    }
}
