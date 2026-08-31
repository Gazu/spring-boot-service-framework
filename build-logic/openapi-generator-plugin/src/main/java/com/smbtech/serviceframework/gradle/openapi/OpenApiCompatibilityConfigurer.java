package com.smbtech.serviceframework.gradle.openapi;

import java.util.Locale;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

final class OpenApiCompatibilityConfigurer {

    static final String BREAKING_CHANGE_TASK_NAME = "smbtechOpenApiBreakingChangeCheck";
    static final String REPRODUCIBILITY_TASK_NAME = "smbtechOpenApiReproducibilityCheck";
    static final String MIGRATION_REPORT_TASK_NAME = "smbtechOpenApiMigrationReport";
    static final String CONSUMER_TEST_TASK_NAME = "smbtechOpenApiConsumerTest";
    static final String MOCK_CONTRACT_TASK_NAME = "smbtechOpenApiMockContractCheck";
    static final String COMPATIBILITY_TASK_NAME = "smbtechOpenApiCompatibilityCheck";

    private final Project project;
    private final SmbtechOpenApiExtension extension;
    private TaskProvider<SmbtechOpenApiReproducibilityTask> reproducibility;
    private TaskProvider<SmbtechOpenApiConsumerCompatibilityTask> consumerTest;
    private TaskProvider<SmbtechOpenApiMockContractTask> mockContract;

    OpenApiCompatibilityConfigurer(Project project, SmbtechOpenApiExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    void configureLifecycle() {
        TaskProvider<SmbtechOpenApiBreakingChangeTask> breakingChange =
                project.getTasks()
                        .register(
                                BREAKING_CHANGE_TASK_NAME,
                                SmbtechOpenApiBreakingChangeTask.class,
                                task -> {
                                    verification(
                                            task,
                                            "Detects breaking OpenAPI changes and enforces SemVer.");
                                    task.getSpecFiles().from(configuredSpecFiles());
                                    task.getBaselineDirectory()
                                            .set(extension.getBaselineDirectory());
                                    task.getBaselineDirectoryPath()
                                            .set(
                                                    extension
                                                            .getBaselineDirectory()
                                                            .map(
                                                                    directory ->
                                                                            directory
                                                                                    .getAsFile()
                                                                                    .getPath()));
                                    task.getRequireBaseline().set(extension.getRequireBaseline());
                                    task.getFailOnBreakingChanges()
                                            .set(extension.getFailOnBreakingChanges());
                                    task.getReportDirectory()
                                            .set(
                                                    project.getLayout()
                                                            .getBuildDirectory()
                                                            .dir("reports/smbtech-openapi/diff"));
                                });
        reproducibility =
                project.getTasks()
                        .register(
                                REPRODUCIBILITY_TASK_NAME,
                                SmbtechOpenApiReproducibilityTask.class,
                                task -> {
                                    verification(
                                            task, "Verifies reproducible generated OpenAPI JARs.");
                                    task.getHashManifest()
                                            .set(
                                                    project.getLayout()
                                                            .getBuildDirectory()
                                                            .file(
                                                                    "reports/smbtech-openapi/reproducibility.sha256"));
                                });
        TaskProvider<SmbtechOpenApiMigrationReportTask> migration =
                project.getTasks()
                        .register(
                                MIGRATION_REPORT_TASK_NAME,
                                SmbtechOpenApiMigrationReportTask.class,
                                task -> {
                                    task.setGroup("openapi migration");
                                    task.setDescription(
                                            "Writes legacy-to-plugin-native OpenAPI coordinate and task mappings.");
                                    task.getDefaultGroupId().set(extension.getGroupId());
                                    task.getSpecConfigurations()
                                            .set(
                                                    project.provider(
                                                            () ->
                                                                    extension.getSpecs().stream()
                                                                            .map(
                                                                                    SmbtechOpenApiGeneratorPlugin
                                                                                            ::specConfiguration)
                                                                            .toList()));
                                    task.getReportFile()
                                            .set(
                                                    project.getLayout()
                                                            .getBuildDirectory()
                                                            .file(
                                                                    "reports/smbtech-openapi/migration.md"));
                                });
        consumerTest =
                project.getTasks()
                        .register(
                                CONSUMER_TEST_TASK_NAME,
                                SmbtechOpenApiConsumerCompatibilityTask.class,
                                task -> {
                                    verification(
                                            task,
                                            "Tests generated artifacts from a consumer boundary.");
                                    task.getReportFile()
                                            .set(
                                                    project.getLayout()
                                                            .getBuildDirectory()
                                                            .file(
                                                                    "reports/smbtech-openapi/consumer-test.txt"));
                                });
        mockContract =
                project.getTasks()
                        .register(
                                MOCK_CONTRACT_TASK_NAME,
                                SmbtechOpenApiMockContractTask.class,
                                task -> {
                                    verification(
                                            task,
                                            "Validates contracts for the framework OpenAPI mock server.");
                                    task.getSpecFiles().from(configuredSpecFiles());
                                    task.getReportFile()
                                            .set(
                                                    project.getLayout()
                                                            .getBuildDirectory()
                                                            .file(
                                                                    "reports/smbtech-openapi/mock-contracts.properties"));
                                });
        TaskProvider<org.gradle.api.Task> compatibility =
                project.getTasks()
                        .register(
                                COMPATIBILITY_TASK_NAME,
                                task -> {
                                    verification(
                                            task,
                                            "Runs the plugin-native OpenAPI compatibility lifecycle.");
                                    task.dependsOn(
                                            breakingChange,
                                            reproducibility,
                                            migration,
                                            consumerTest,
                                            mockContract);
                                });
        project.getPluginManager()
                .withPlugin(
                        "base",
                        ignored ->
                                project.getTasks()
                                        .named("check")
                                        .configure(task -> task.dependsOn(compatibility)));
    }

    void configure(SmbtechOpenApiSpec spec) {
        OpenApiContractIdentity identity =
                OpenApiContractReader.read(spec.getInput().get().getAsFile());
        String artifactBaseName = spec.getArtifactBaseName().getOrElse(identity.artifactBaseName());
        String version = spec.getVersion().getOrElse(identity.version());
        String normalizedPackage = artifactBaseName.replace("-", "").toLowerCase(Locale.ROOT);
        String basePackage =
                spec.getBasePackage().getOrElse("com.smbtech.contracts." + normalizedPackage);
        String modelPackage = spec.getModelPackage().getOrElse(basePackage + ".model");
        String serverPackage = spec.getServerApiPackage().getOrElse(basePackage + ".api");
        String clientPackage = spec.getClientPackage().getOrElse(basePackage + ".client");

        if (spec.getPublishModels().get()) {
            addArtifact(
                    spec,
                    OpenApiArtifactKind.MODELS,
                    artifactBaseName,
                    version,
                    modelPackage,
                    modelPackage);
        }
        if (spec.getPublishServerApi().get()) {
            addArtifact(
                    spec,
                    OpenApiArtifactKind.SERVER_API,
                    artifactBaseName,
                    version,
                    modelPackage,
                    serverPackage);
        }
        if (spec.getPublishClient().get()) {
            addArtifact(
                    spec,
                    OpenApiArtifactKind.CLIENT,
                    artifactBaseName,
                    version,
                    modelPackage,
                    clientPackage);
        }
    }

    private void addArtifact(
            SmbtechOpenApiSpec spec,
            OpenApiArtifactKind kind,
            String artifactBaseName,
            String version,
            String modelPackage,
            String apiPackage) {
        String artifactId = artifactBaseName + "-" + kind.artifactSuffix();
        String prefix = spec.getName() + javaName(kind.artifactSuffix());
        TaskProvider<Jar> jar =
                project.getTasks().named("jar" + javaName(prefix) + "OpenApi", Jar.class);
        TaskProvider<Jar> sourcesJar =
                project.getTasks().named("sourcesJar" + javaName(prefix) + "OpenApi", Jar.class);
        reproducibility.configure(
                task -> {
                    task.dependsOn(jar, sourcesJar);
                    task.getArtifacts().from(jar.flatMap(Jar::getArchiveFile));
                    task.getArtifacts().from(sourcesJar.flatMap(Jar::getArchiveFile));
                });
        consumerTest.configure(
                task -> {
                    task.dependsOn(jar);
                    task.getArtifacts().from(jar.flatMap(Jar::getArchiveFile));
                    task.getArtifactDescriptors()
                            .add(
                                    String.join(
                                            "|",
                                            artifactId,
                                            version,
                                            kind.name(),
                                            modelPackage,
                                            apiPackage,
                                            artifactBaseName));
                });
        if (kind == OpenApiArtifactKind.MODELS) {
            mockContract.configure(
                    task -> {
                        task.dependsOn(jar);
                        task.getModelArtifacts().from(jar.flatMap(Jar::getArchiveFile));
                    });
        }
    }

    private Object configuredSpecFiles() {
        return project.provider(
                () ->
                        extension.getSpecs().stream()
                                .filter(spec -> spec.getInput().isPresent())
                                .map(spec -> spec.getInput().get().getAsFile())
                                .toList());
    }

    private static void verification(org.gradle.api.Task task, String description) {
        task.setGroup("verification");
        task.setDescription(description);
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
