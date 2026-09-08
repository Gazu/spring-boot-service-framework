package com.smbtech.serviceframework.gradle.openapi;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;

final class OpenApiGenerationConfigurer {

    static final String GENERATE_MODELS_TASK_NAME = "smbtechOpenApiGenerateModels";
    static final String GENERATE_SERVER_API_TASK_NAME = "smbtechOpenApiGenerateServerApi";
    static final String GENERATE_CLIENT_TASK_NAME = "smbtechOpenApiGenerateClient";
    static final String ASSEMBLE_TASK_NAME = "smbtechOpenApiAssemble";
    static final String PUBLISH_LOCAL_TASK_NAME = "smbtechOpenApiPublishToLocalRepository";
    static final String PUBLISH_REMOTE_TASK_NAME = "smbtechOpenApiPublish";
    static final String PUBLISH_CONTRACT_LOCAL_TASK_NAME =
            "smbtechOpenApiPublishContractToLocalRepository";
    static final String PUBLISH_CONTRACT_REMOTE_TASK_NAME = "smbtechOpenApiPublishContract";
    static final String CONTRACT_SELECTOR_PROPERTY = "openApiContract";

    private final Project project;
    private final SoftwareComponentFactory componentFactory;
    private final SmbtechOpenApiExtension extension;
    private final OpenApiToolchainVersions versions;
    private final Set<Path> configuredContractPaths = new LinkedHashSet<>();

    OpenApiGenerationConfigurer(
            Project project,
            SoftwareComponentFactory componentFactory,
            SmbtechOpenApiExtension extension) {
        this.project = project;
        this.componentFactory = componentFactory;
        this.extension = extension;
        this.versions = OpenApiToolchainVersions.load();
    }

    void configureLifecycleAndRepositories() {
        registerLifecycle(GENERATE_MODELS_TASK_NAME, "Generates all configured OpenAPI models.");
        registerLifecycle(
                GENERATE_SERVER_API_TASK_NAME, "Generates all configured Spring server APIs.");
        registerLifecycle(
                GENERATE_CLIENT_TASK_NAME,
                "Generates all configured Spring HTTP Interface and OpenFeign clients.");
        registerLifecycle(ASSEMBLE_TASK_NAME, "Builds all configured OpenAPI contract artifacts.");
        registerLifecycle(
                PUBLISH_LOCAL_TASK_NAME,
                "Publishes all configured OpenAPI contract artifacts to the local contract repository.");
        registerLifecycle(
                PUBLISH_REMOTE_TASK_NAME,
                "Publishes all configured OpenAPI contract artifacts to the configured Maven repository.");
        registerLifecycle(
                PUBLISH_CONTRACT_LOCAL_TASK_NAME,
                "Publishes one OpenAPI contract to the local contract repository.");
        registerLifecycle(
                PUBLISH_CONTRACT_REMOTE_TASK_NAME,
                "Publishes one OpenAPI contract to the configured Maven repository.");
        configureContractSelection(PUBLISH_CONTRACT_LOCAL_TASK_NAME);
        configureContractSelection(PUBLISH_CONTRACT_REMOTE_TASK_NAME);
        configureRemotePublication(PUBLISH_REMOTE_TASK_NAME);
        configureRemotePublication(PUBLISH_CONTRACT_REMOTE_TASK_NAME);
        project.getPluginManager()
                .withPlugin(
                        "base",
                        ignored -> {
                            project.getTasks()
                                    .named("assemble")
                                    .configure(task -> task.dependsOn(ASSEMBLE_TASK_NAME));
                            project.getTasks()
                                    .named("check")
                                    .configure(
                                            task ->
                                                    task.dependsOn(
                                                            SmbtechOpenApiGeneratorPlugin
                                                                    .VALIDATE_SPECS_TASK_NAME,
                                                            SmbtechOpenApiGeneratorPlugin
                                                                    .BUILD_LOGIC_CHECK_TASK_NAME));
                        });

        PublishingExtension publishing =
                project.getExtensions().getByType(PublishingExtension.class);
        publishing
                .getRepositories()
                .maven(
                        repository -> {
                            repository.setName("smbtechOpenApiLocal");
                            repository.setUrl(extension.getRepositoryDirectory());
                        });
        MavenArtifactRepository remoteRepository =
                publishing
                        .getRepositories()
                        .maven(
                                repository -> {
                                    repository.setName("smbtechOpenApiRemote");
                                    repository.setUrl(
                                            extension
                                                    .getPublicationRepositoryUrl()
                                                    .orElse(
                                                            project.getProviders()
                                                                    .provider(
                                                                            () ->
                                                                                    project.getLayout()
                                                                                            .getBuildDirectory()
                                                                                            .dir(
                                                                                                    "repository/openapi-remote-disabled")
                                                                                            .get()
                                                                                            .getAsFile()
                                                                                            .toURI()
                                                                                            .toString())));
                                });
        project.afterEvaluate(
                ignored -> {
                    if (!"file".equalsIgnoreCase(remoteRepository.getUrl().getScheme())) {
                        remoteRepository.credentials(
                                credentials -> {
                                    credentials.setUsername(
                                            project.getProviders()
                                                    .gradleProperty("openApiRepositoryUsername")
                                                    .orElse(
                                                            project.getProviders()
                                                                    .environmentVariable(
                                                                            "OPENAPI_REPOSITORY_USERNAME"))
                                                    .getOrElse(""));
                                    credentials.setPassword(
                                            project.getProviders()
                                                    .gradleProperty("openApiRepositoryPassword")
                                                    .orElse(
                                                            project.getProviders()
                                                                    .environmentVariable(
                                                                            "OPENAPI_REPOSITORY_PASSWORD"))
                                                    .getOrElse(""));
                                });
                    }
                });
        project.getTasks()
                .withType(PublishToMavenRepository.class)
                .configureEach(
                        task ->
                                task.onlyIf(
                                        ignored ->
                                                !task.getRepository()
                                                                .getName()
                                                                .equals("smbtechOpenApiRemote")
                                                        || extension
                                                                .getPublicationRepositoryUrl()
                                                                .isPresent()));
    }

    void configure(SmbtechOpenApiSpec spec) {
        File input = spec.getInput().get().getAsFile();
        Path inputPath = OpenApiSpecDiscovery.normalizedPath(input);
        configuredContractPaths.add(inputPath);
        boolean selected = inputPath.equals(selectedContractPath());
        OpenApiContractIdentity identity = OpenApiContractReader.read(input);
        OpenApiArtifactContract contract = OpenApiArtifactContract.resolve(spec, identity);
        String groupId = spec.getGroupId().getOrElse(extension.getGroupId().get());

        TaskProvider<Jar> modelsJar = null;
        if (spec.getPublishModels().get()) {
            modelsJar =
                    registerArtifact(
                            spec,
                            identity,
                            OpenApiArtifactKind.MODELS,
                            groupId,
                            contract.artifactBaseName(),
                            contract.version(),
                            contract.modelPackage(),
                            contract.serverApiPackage(),
                            contract.openFeignPackage(),
                            null,
                            selected);
        }
        if (spec.getPublishServerApi().get()) {
            registerArtifact(
                    spec,
                    identity,
                    OpenApiArtifactKind.SERVER_API,
                    groupId,
                    contract.artifactBaseName(),
                    contract.version(),
                    contract.modelPackage(),
                    contract.serverApiPackage(),
                    contract.openFeignPackage(),
                    modelsJar,
                    selected);
        }
        if (spec.getPublishClient().get()) {
            registerArtifact(
                    spec,
                    identity,
                    OpenApiArtifactKind.CLIENT,
                    groupId,
                    contract.artifactBaseName(),
                    contract.version(),
                    contract.modelPackage(),
                    contract.httpInterfacePackage(),
                    contract.openFeignPackage(),
                    modelsJar,
                    selected);
        }
    }

    private TaskProvider<Jar> registerArtifact(
            SmbtechOpenApiSpec spec,
            OpenApiContractIdentity identity,
            OpenApiArtifactKind kind,
            String groupId,
            String artifactBaseName,
            String version,
            String modelPackage,
            String apiPackage,
            String openFeignPackage,
            TaskProvider<Jar> modelsJar,
            boolean selected) {
        String artifactId = OpenApiArtifactContract.artifactId(artifactBaseName, kind);
        String prefix = spec.getName() + javaName(kind.artifactSuffix());
        String generationTaskName = "generate" + javaName(prefix) + "OpenApiSources";
        String compileTaskName = "compile" + javaName(prefix) + "OpenApiJava";
        String metadataTaskName = "generate" + javaName(prefix) + "OpenApiMetadata";
        String jarTaskName = "jar" + javaName(prefix) + "OpenApi";
        String sourcesJarTaskName = "sourcesJar" + javaName(prefix) + "OpenApi";
        String publicationName = prefix + "OpenApi";

        TaskProvider<SmbtechOpenApiGenerateTask> generate =
                project.getTasks()
                        .register(
                                generationTaskName,
                                SmbtechOpenApiGenerateTask.class,
                                task -> {
                                    task.setGroup("openapi generation");
                                    task.setDescription(
                                            "Generates " + artifactId + " Java sources.");
                                    task.dependsOn(
                                            SmbtechOpenApiGeneratorPlugin
                                                    .BUILD_LOGIC_CHECK_TASK_NAME,
                                            SmbtechOpenApiGeneratorPlugin.VALIDATE_SPECS_TASK_NAME);
                                    task.getInputSpec().set(spec.getInput());
                                    task.getArtifactKind().set(kind);
                                    task.getGroupId().set(groupId);
                                    task.getArtifactId().set(artifactId);
                                    task.getArtifactVersion().set(version);
                                    task.getModelPackage().set(modelPackage);
                                    task.getApiPackage().set(apiPackage);
                                    task.getOpenFeignPackage().set(openFeignPackage);
                                    task.getClientName().set(artifactBaseName);
                                    task.getOutputDirectory()
                                            .set(
                                                    extension
                                                            .getOutputDirectory()
                                                            .dir(
                                                                    spec.getName()
                                                                            + "/"
                                                                            + kind
                                                                                    .artifactSuffix()));
                                });

        Configuration compileClasspath = compileClasspath(prefix, kind);
        TaskProvider<JavaCompile> compile =
                project.getTasks()
                        .register(
                                compileTaskName,
                                JavaCompile.class,
                                task -> {
                                    task.setGroup("openapi generation");
                                    task.setDescription(
                                            "Compiles " + artifactId + " generated sources.");
                                    task.dependsOn(generate);
                                    if (modelsJar != null) {
                                        task.dependsOn(modelsJar);
                                    }
                                    Project starter = localRestClientStarter();
                                    if (kind == OpenApiArtifactKind.CLIENT && starter != null) {
                                        task.dependsOn(starter.getPath() + ":jar");
                                    }
                                    task.source(
                                            generate.flatMap(
                                                    generation ->
                                                            generation
                                                                    .getOutputDirectory()
                                                                    .dir("src/main/java")));
                                    task.setClasspath(
                                            modelsJar == null
                                                    ? compileClasspath
                                                    : compileClasspath.plus(
                                                            project.files(modelsJar)));
                                    task.getDestinationDirectory()
                                            .set(
                                                    project.getLayout()
                                                            .getBuildDirectory()
                                                            .dir(
                                                                    "classes/smbtech-openapi/"
                                                                            + spec.getName()
                                                                            + "/"
                                                                            + kind
                                                                                    .artifactSuffix()));
                                    task.getOptions().setEncoding("UTF-8");
                                    task.getOptions()
                                            .getRelease()
                                            .set(OpenApiArtifactContract.JAVA_RELEASE);
                                    task.getOptions().getCompilerArgs().add("-parameters");
                                    task.setSourceCompatibility(
                                            Integer.toString(OpenApiArtifactContract.JAVA_RELEASE));
                                    task.setTargetCompatibility(
                                            Integer.toString(OpenApiArtifactContract.JAVA_RELEASE));
                                });

        TaskProvider<SmbtechOpenApiMetadataTask> metadata =
                project.getTasks()
                        .register(
                                metadataTaskName,
                                SmbtechOpenApiMetadataTask.class,
                                task -> {
                                    task.setGroup("openapi generation");
                                    task.setDescription(
                                            "Writes reproducible metadata for " + artifactId + ".");
                                    task.getInputSpec().set(spec.getInput());
                                    task.getContractTitle().set(identity.title());
                                    task.getContractVersion().set(version);
                                    task.getGroupId().set(groupId);
                                    task.getArtifactId().set(artifactId);
                                    task.getContractId().set(artifactBaseName);
                                    task.getArtifactKind().set(kind);
                                    task.getFrameworkVersion().set(versions.frameworkVersion());
                                    task.getOpenApiGeneratorVersion()
                                            .set(versions.openApiGeneratorVersion());
                                    task.getSpringBootVersion().set(versions.springBootVersion());
                                    task.getOutputDirectory()
                                            .set(
                                                    project.getLayout()
                                                            .getBuildDirectory()
                                                            .dir(
                                                                    "generated/smbtech-openapi-metadata/"
                                                                            + spec.getName()
                                                                            + "/"
                                                                            + kind
                                                                                    .artifactSuffix()));
                                });

        TaskProvider<Jar> jar =
                project.getTasks()
                        .register(
                                jarTaskName,
                                Jar.class,
                                task -> {
                                    task.setGroup("build");
                                    task.setDescription("Builds " + artifactId + ".");
                                    task.dependsOn(compile, metadata);
                                    task.from(
                                            compile.flatMap(JavaCompile::getDestinationDirectory));
                                    task.from(
                                            metadata.flatMap(
                                                    SmbtechOpenApiMetadataTask
                                                            ::getOutputDirectory));
                                    task.getArchiveBaseName().set(artifactId);
                                    task.getArchiveVersion().set(version);
                                    task.getDestinationDirectory()
                                            .set(
                                                    project.getLayout()
                                                            .getBuildDirectory()
                                                            .dir("libs/smbtech-openapi"));
                                    task.setPreserveFileTimestamps(false);
                                    task.setReproducibleFileOrder(true);
                                });
        TaskProvider<Jar> sourcesJar =
                project.getTasks()
                        .register(
                                sourcesJarTaskName,
                                Jar.class,
                                task -> {
                                    task.setGroup("build");
                                    task.dependsOn(generate);
                                    task.from(
                                            generate.flatMap(
                                                    generation ->
                                                            generation
                                                                    .getOutputDirectory()
                                                                    .dir("src/main/java")));
                                    task.getArchiveBaseName().set(artifactId);
                                    task.getArchiveVersion().set(version);
                                    task.getArchiveClassifier().set("sources");
                                    task.getDestinationDirectory()
                                            .set(
                                                    project.getLayout()
                                                            .getBuildDirectory()
                                                            .dir("libs/smbtech-openapi"));
                                    task.setPreserveFileTimestamps(false);
                                    task.setReproducibleFileOrder(true);
                                });

        AdhocComponentWithVariants component =
                componentFactory.adhoc("smbtechOpenApi" + javaName(prefix));
        project.getComponents().add(component);
        Configuration apiElements =
                apiElements(prefix, jar, kind, groupId, artifactBaseName, version);
        component.addVariantsFromConfiguration(
                apiElements, details -> details.mapToMavenScope("compile"));
        Configuration runtimeElements =
                runtimeElements(prefix, jar, kind, groupId, artifactBaseName, version);
        component.addVariantsFromConfiguration(
                runtimeElements, details -> details.mapToMavenScope("runtime"));

        PublishingExtension publishing =
                project.getExtensions().getByType(PublishingExtension.class);
        publishing
                .getPublications()
                .create(
                        publicationName,
                        MavenPublication.class,
                        publication -> {
                            publication.setGroupId(groupId);
                            publication.setArtifactId(artifactId);
                            publication.setVersion(version);
                            publication.from(component);
                            publication.artifact(sourcesJar);
                            configurePom(publication, identity, kind);
                        });

        project.getTasks().named(aggregateTask(kind)).configure(task -> task.dependsOn(generate));
        project.getTasks().named(ASSEMBLE_TASK_NAME).configure(task -> task.dependsOn(jar));
        project.getTasks()
                .named(PUBLISH_LOCAL_TASK_NAME)
                .configure(
                        task ->
                                task.dependsOn(
                                        "publish"
                                                + javaName(publicationName)
                                                + "PublicationToSmbtechOpenApiLocalRepository"));
        project.getTasks()
                .named(PUBLISH_REMOTE_TASK_NAME)
                .configure(
                        task ->
                                task.dependsOn(
                                        "publish"
                                                + javaName(publicationName)
                                                + "PublicationToSmbtechOpenApiRemoteRepository"));
        if (selected) {
            project.getTasks()
                    .named(PUBLISH_CONTRACT_LOCAL_TASK_NAME)
                    .configure(
                            task ->
                                    task.dependsOn(
                                            "publish"
                                                    + javaName(publicationName)
                                                    + "PublicationToSmbtechOpenApiLocalRepository"));
            project.getTasks()
                    .named(PUBLISH_CONTRACT_REMOTE_TASK_NAME)
                    .configure(
                            task ->
                                    task.dependsOn(
                                            "publish"
                                                    + javaName(publicationName)
                                                    + "PublicationToSmbtechOpenApiRemoteRepository"));
        }
        return jar;
    }

    private void configureContractSelection(String taskName) {
        project.getTasks()
                .named(taskName)
                .configure(task -> task.doFirst(ignored -> validateContractSelection()));
    }

    private void configureRemotePublication(String taskName) {
        project.getTasks()
                .named(taskName)
                .configure(
                        task ->
                                task.doLast(
                                        ignored -> {
                                            if (!extension
                                                    .getPublicationRepositoryUrl()
                                                    .isPresent()) {
                                                throw new GradleException(
                                                        "smbtechOpenApi.publicationRepositoryUrl is required for remote publication");
                                            }
                                        }));
    }

    private void validateContractSelection() {
        Path selectedPath = selectedContractPath();
        if (selectedPath == null) {
            throw new GradleException(
                    "Contract publication requires -P"
                            + CONTRACT_SELECTOR_PROPERTY
                            + "=<project-relative-openapi-path>");
        }
        if (!configuredContractPaths.contains(selectedPath)) {
            String available =
                    configuredContractPaths.stream()
                            .map(this::displayPath)
                            .sorted()
                            .collect(Collectors.joining(", "));
            throw new GradleException(
                    "OpenAPI contract "
                            + displayPath(selectedPath)
                            + " is not configured or discovered. Available contracts: "
                            + (available.isEmpty() ? "<none>" : available));
        }
    }

    private Path selectedContractPath() {
        String selector =
                project.getProviders()
                        .gradleProperty(CONTRACT_SELECTOR_PROPERTY)
                        .getOrElse("")
                        .trim();
        return selector.isEmpty()
                ? null
                : OpenApiSpecDiscovery.normalizedPath(project.file(selector));
    }

    private String displayPath(Path path) {
        Path root = OpenApiSpecDiscovery.normalizedPath(project.getRootDir());
        Path displayed = path.startsWith(root) ? root.relativize(path) : path;
        return displayed.toString().replace(File.separatorChar, '/');
    }

    private Configuration compileClasspath(String prefix, OpenApiArtifactKind kind) {
        Configuration configuration =
                project.getConfigurations().create(prefix + "OpenApiCompileClasspath");
        configuration.setCanBeConsumed(false);
        configuration.setCanBeResolved(true);
        addDependency(configuration, "com.fasterxml.jackson.core:jackson-annotations:2.21");
        addDependency(configuration, "jakarta.annotation:jakarta.annotation-api:3.0.0");
        addDependency(configuration, "jakarta.validation:jakarta.validation-api:3.1.1");
        addDependency(configuration, "org.springframework:spring-context:7.0.8");
        if (kind == OpenApiArtifactKind.MODELS) {
            addDependency(configuration, "org.hibernate.validator:hibernate-validator:9.1.0.Final");
        } else {
            addDependency(configuration, "org.springframework:spring-web:7.0.8");
            if (kind == OpenApiArtifactKind.SERVER_API) {
                addDependency(configuration, "jakarta.servlet:jakarta.servlet-api:6.1.0");
            }
        }
        if (kind == OpenApiArtifactKind.CLIENT) {
            addDependency(
                    configuration,
                    "org.springframework.cloud:spring-cloud-openfeign-core:"
                            + versions.springCloudOpenFeignVersion()
                            + "@jar");
            Project starter =
                    project.findProject(
                            ":spring-boot-service-framework-starters:spring-boot-service-framework-starter-rest-client");
            addDependency(
                    configuration,
                    starter != null
                            ? project.files(
                                    starter.getLayout()
                                            .getBuildDirectory()
                                            .file(
                                                    "libs/"
                                                            + starter.getName()
                                                            + "-"
                                                            + versions.frameworkVersion()
                                                            + ".jar"))
                            : "com.smbtech:spring-boot-service-framework-starter-rest-client:"
                                    + versions.frameworkVersion());
        }
        return configuration;
    }

    private Configuration apiElements(
            String prefix,
            TaskProvider<Jar> jar,
            OpenApiArtifactKind kind,
            String groupId,
            String artifactBaseName,
            String version) {
        return elements(
                prefix + "OpenApiApiElements",
                Usage.JAVA_API,
                jar,
                kind,
                groupId,
                artifactBaseName,
                version);
    }

    private Configuration runtimeElements(
            String prefix,
            TaskProvider<Jar> jar,
            OpenApiArtifactKind kind,
            String groupId,
            String artifactBaseName,
            String version) {
        return elements(
                prefix + "OpenApiRuntimeElements",
                Usage.JAVA_RUNTIME,
                jar,
                kind,
                groupId,
                artifactBaseName,
                version);
    }

    private Configuration elements(
            String name,
            String usage,
            TaskProvider<Jar> jar,
            OpenApiArtifactKind kind,
            String groupId,
            String artifactBaseName,
            String version) {
        Configuration configuration = project.getConfigurations().create(name);
        configuration.setCanBeConsumed(true);
        configuration.setCanBeResolved(false);
        configuration.attributes(
                attributes -> {
                    attributes.attribute(
                            Category.CATEGORY_ATTRIBUTE,
                            project.getObjects().named(Category.class, Category.LIBRARY));
                    attributes.attribute(
                            Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, usage));
                    attributes.attribute(
                            Bundling.BUNDLING_ATTRIBUTE,
                            project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
                    attributes.attribute(
                            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                            project.getObjects().named(LibraryElements.class, LibraryElements.JAR));
                });
        configuration.getOutgoing().artifact(jar);
        configuration
                .getOutgoing()
                .capability(
                        groupId
                                + ":"
                                + OpenApiArtifactContract.artifactId(artifactBaseName, kind)
                                + ":"
                                + version);
        addDependency(configuration, "com.fasterxml.jackson.core:jackson-annotations:2.21");
        addDependency(configuration, "jakarta.validation:jakarta.validation-api:3.1.1");
        addDependency(configuration, "org.springframework:spring-context:7.0.8");
        if (kind == OpenApiArtifactKind.MODELS) {
            addDependency(configuration, "org.hibernate.validator:hibernate-validator:9.1.0.Final");
        } else {
            addDependency(configuration, "org.springframework:spring-web:7.0.8");
            if (kind == OpenApiArtifactKind.SERVER_API) {
                addDependency(configuration, "jakarta.servlet:jakarta.servlet-api:6.1.0");
            }
            addDependency(
                    configuration,
                    groupId
                            + ":"
                            + OpenApiArtifactContract.artifactId(
                                    artifactBaseName, OpenApiArtifactKind.MODELS)
                            + ":"
                            + version);
        }
        if (kind == OpenApiArtifactKind.CLIENT) {
            addDependency(
                    configuration,
                    "com.smbtech:spring-boot-service-framework-starter-rest-client:"
                            + versions.frameworkVersion());
        }
        return configuration;
    }

    private void configurePom(
            MavenPublication publication,
            OpenApiContractIdentity identity,
            OpenApiArtifactKind kind) {
        publication
                .getPom()
                .getName()
                .set(identity.title() + " " + kind.artifactSuffix() + " contract");
        publication
                .getPom()
                .getDescription()
                .set("Generated from OpenAPI contract " + identity.title() + ".");
        publication.getPom().getUrl().set("https://github.com/Gazu/spring-boot-service-framework");
        publication
                .getPom()
                .licenses(
                        licenses ->
                                licenses.license(
                                        license -> {
                                            license.getName().set("Apache License, Version 2.0");
                                            license.getUrl()
                                                    .set(
                                                            "https://www.apache.org/licenses/LICENSE-2.0.txt");
                                        }));
    }

    private void addDependency(Configuration configuration, Object notation) {
        configuration
                .getDependencies()
                .add(
                        notation instanceof Dependency dependency
                                ? dependency
                                : project.getDependencies().create(notation));
    }

    private void registerLifecycle(String name, String description) {
        project.getTasks()
                .register(
                        name,
                        task -> {
                            task.setGroup("openapi generation");
                            task.setDescription(description);
                            task.dependsOn(
                                    SmbtechOpenApiGeneratorPlugin.BUILD_LOGIC_CHECK_TASK_NAME,
                                    SmbtechOpenApiGeneratorPlugin.VALIDATE_SPECS_TASK_NAME);
                        });
    }

    private Project localRestClientStarter() {
        return project.findProject(
                ":spring-boot-service-framework-starters:spring-boot-service-framework-starter-rest-client");
    }

    private static String aggregateTask(OpenApiArtifactKind kind) {
        return switch (kind) {
            case MODELS -> GENERATE_MODELS_TASK_NAME;
            case SERVER_API -> GENERATE_SERVER_API_TASK_NAME;
            case CLIENT -> GENERATE_CLIENT_TASK_NAME;
        };
    }

    private static String javaName(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("[^A-Za-z0-9]+")) {
            if (!part.isEmpty()) {
                result.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
                result.append(part.substring(1));
            }
        }
        return result.toString();
    }
}
