package com.smbtech.serviceframework.gradle.openapi;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.jar.JarFile;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SmbtechOpenApiGeneratorPluginTest {

    @TempDir Path testProjectDirectory;

    @Test
    void keepsArtifactKindsInsideThePluginImplementation() {
        assertFalse(Modifier.isPublic(OpenApiArtifactKind.class.getModifiers()));
    }

    @Test
    void registersExtensionAndBuildLogicCheckTask() {
        Project project = ProjectBuilder.builder().build();

        project.getPlugins().apply(SmbtechOpenApiGeneratorPlugin.class);

        SmbtechOpenApiExtension extension =
                project.getExtensions().findByType(SmbtechOpenApiExtension.class);

        assertNotNull(extension);
        assertEquals("com.smbtech.contracts", extension.getGroupId().get());
        assertEquals(
                project.getLayout()
                        .getBuildDirectory()
                        .dir("generated/smbtech-openapi")
                        .get()
                        .getAsFile(),
                extension.getOutputDirectory().get().getAsFile());
        assertEquals(
                project.getLayout().getBuildDirectory().dir("repository/openapi").get().getAsFile(),
                extension.getRepositoryDirectory().get().getAsFile());
        assertFalse(extension.getPublicationRepositoryUrl().isPresent());
        assertEquals(
                project.file("src/main/openapi-baselines"),
                extension.getBaselineDirectory().get().getAsFile());
        assertFalse(extension.getRequireBaseline().get());
        assertFalse(extension.getFailOnBreakingChanges().get());
        assertTrue(extension.getPublishModels().get());
        assertTrue(extension.getPublishServerApi().get());
        assertTrue(extension.getPublishClient().get());
        assertNotNull(
                project.getTasks()
                        .findByName(SmbtechOpenApiGeneratorPlugin.BUILD_LOGIC_CHECK_TASK_NAME));
        assertNotNull(project.getExtensions().findByType(PublishingExtension.class));
        assertNotNull(project.getTasks().findByName("smbtechOpenApiGenerateModels"));
        assertNotNull(project.getTasks().findByName("smbtechOpenApiGenerateServerApi"));
        assertNotNull(project.getTasks().findByName("smbtechOpenApiGenerateClient"));
        assertNotNull(project.getTasks().findByName("smbtechOpenApiBreakingChangeCheck"));
        assertNotNull(project.getTasks().findByName("smbtechOpenApiReproducibilityCheck"));
        assertNotNull(project.getTasks().findByName("smbtechOpenApiMigrationReport"));
        assertNotNull(project.getTasks().findByName("smbtechOpenApiConsumerTest"));
        assertNotNull(project.getTasks().findByName("smbtechOpenApiMockContractCheck"));
        assertNotNull(project.getTasks().findByName("smbtechOpenApiCompatibilityCheck"));
        assertInstanceOf(
                SmbtechOpenApiValidateSpecsTask.class,
                project.getTasks().findByName("smbtechOpenApiValidateSpecs"));
        assertFalse(project.getTasks().getNames().contains("generateOpenApiModels"));
        assertFalse(project.getTasks().getNames().contains("openApiCompatibilityCheck"));
    }

    @Test
    void exposesNamedSpecConfiguration() {
        Project project = ProjectBuilder.builder().build();

        project.getPlugins().apply(SmbtechOpenApiGeneratorPlugin.class);
        SmbtechOpenApiExtension extension =
                project.getExtensions().getByType(SmbtechOpenApiExtension.class);

        extension
                .getSpecs()
                .register(
                        "merchantOrderStatus",
                        spec -> {
                            spec.getInput()
                                    .set(project.file("docs/openapi/merchant-order-status.yaml"));
                            spec.getArtifactBaseName().set("merchant-order-status");
                            spec.getVersion().set("1.1.0");
                            spec.getBasePackage().set("com.smbtech.contracts.merchantorderstatus");
                            spec.getModelPackage()
                                    .set("com.smbtech.contracts.merchantorderstatus.model");
                            spec.getServerApiPackage()
                                    .set("com.smbtech.contracts.merchantorderstatus.api");
                            spec.getClientPackage()
                                    .set("com.smbtech.contracts.merchantorderstatus.client");
                            spec.getPublishClient().set(false);
                        });

        SmbtechOpenApiSpec spec = extension.getSpecs().getByName("merchantOrderStatus");

        assertEquals("merchantOrderStatus", spec.getName());
        assertEquals("merchant-order-status", spec.getArtifactBaseName().get());
        assertEquals("1.1.0", spec.getVersion().get());
        assertEquals(
                "com.smbtech.contracts.merchantorderstatus.model", spec.getModelPackage().get());
        assertTrue(spec.getPublishModels().get());
        assertTrue(spec.getPublishServerApi().get());
        assertFalse(spec.getPublishClient().get());
        assertEquals(
                project.file("docs/openapi/merchant-order-status.yaml"),
                spec.getInput().get().getAsFile());
    }

    @Test
    void exposesAndExecutesGeneratorTasksFromAConsumerBuild() throws IOException {
        Files.writeString(
                testProjectDirectory.resolve("settings.gradle"), "rootProject.name = 'consumer'\n");
        Files.writeString(
                testProjectDirectory.resolve("build.gradle"),
                """
                plugins {
                    id 'com.smbtech.service-framework.openapi-generator'
                }

                repositories {
                    mavenCentral()
                }
                """);
        Path spec = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(
                spec,
                """
                openapi: 3.1.1
                info:
                  title: orders
                  version: 1.0.0
                jsonSchemaDialect: https://json-schema.org/draft/2020-12/schema
                paths:
                  /orders:
                    get:
                      operationId: listOrders
                      responses:
                        '204':
                          description: No orders
                """);

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiValidateSpecs")
                        .build();

        assertEquals(SUCCESS, result.task(":smbtechOpenApiValidateSpecs").getOutcome());
    }

    @Test
    void generatesCompilesAndPackagesModelsFromAConsumerBuild() throws IOException {
        Files.writeString(
                testProjectDirectory.resolve("settings.gradle"), "rootProject.name = 'consumer'\n");
        Files.writeString(
                testProjectDirectory.resolve("build.gradle"),
                """
                plugins {
                    id 'com.smbtech.service-framework.openapi-generator'
                }

                repositories {
                    mavenCentral()
                }

                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                    specs {
                        register('orders') {
                            input.set(file('src/main/openapi/orders.yaml'))
                        }
                    }
                }
                """);
        Path spec = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(
                spec,
                """
                openapi: 3.1.0
                info:
                  title: store-orders
                  version: 2.1.0
                paths:
                  /orders/{orderId}:
                    get:
                      operationId: getOrder
                      parameters:
                        - in: path
                          name: orderId
                          required: true
                          schema:
                            type: string
                      responses:
                        '200':
                          description: Order found
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Order'
                components:
                  schemas:
                    Order:
                      type: object
                      required: [id]
                      properties:
                        id:
                          type: string
                """);

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiAssemble")
                        .build();

        assertEquals(SUCCESS, result.task(":smbtechOpenApiAssemble").getOutcome());
        assertTrue(
                Files.exists(
                        testProjectDirectory.resolve(
                                "build/libs/smbtech-openapi/store-orders-models-2.1.0.jar")));
        Path generatedModel =
                testProjectDirectory.resolve(
                        "build/generated/smbtech-openapi/orders/models/src/main/java/"
                                + "com/smbtech/contracts/storeorders/model/Order.java");
        assertTrue(Files.readString(generatedModel).contains("@JsonProperty"));
    }

    @Test
    void runsPluginNativeCompatibilityLifecycleAndProducesAdoptionReports() throws Exception {
        writeConsumerBuild();
        Path spec = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, ordersContract("1.0.0", true));

        BuildResult first =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiCompatibilityCheck")
                        .build();

        assertEquals(SUCCESS, first.task(":smbtechOpenApiCompatibilityCheck").getOutcome());
        Path models =
                testProjectDirectory.resolve(
                        "build/libs/smbtech-openapi/store-orders-models-1.0.0.jar");
        assertTrue(Files.isRegularFile(models));
        try (JarFile jar = new JarFile(models.toFile())) {
            assertNotNull(
                    jar.getEntry(
                            "META-INF/smbtech/openapi/contracts/store-orders/1.0.0/contract.yaml"));
        }
        assertTrue(
                Files.readString(
                                testProjectDirectory.resolve(
                                        "build/reports/smbtech-openapi/migration.md"))
                        .contains("store-orders-models"));
        assertTrue(
                Files.readString(
                                testProjectDirectory.resolve(
                                        "build/reports/smbtech-openapi/mock-contracts.properties"))
                        .contains(
                                "classpath:META-INF/smbtech/openapi/contracts/store-orders/1.0.0/contract.yaml"));

        String firstHash = sha256(models);
        deleteDirectory(testProjectDirectory.resolve("build"));
        GradleRunner.create()
                .withProjectDir(testProjectDirectory.toFile())
                .withPluginClasspath()
                .withArguments("smbtechOpenApiCompatibilityCheck")
                .build();
        assertEquals(firstHash, sha256(models));
    }

    @Test
    void rejectsBreakingChangeWithoutMajorVersionIncrease() throws IOException {
        Files.writeString(
                testProjectDirectory.resolve("settings.gradle"), "rootProject.name = 'consumer'\n");
        Files.writeString(
                testProjectDirectory.resolve("build.gradle"),
                """
                plugins {
                    id 'com.smbtech.service-framework.openapi-generator'
                }

                smbtechOpenApi {
                    baselineDirectory.set(layout.projectDirectory.dir('src/main/openapi-baselines'))
                    requireBaseline.set(true)
                    publishServerApi.set(false)
                    publishClient.set(false)
                    specs {
                        register('orders') {
                            input.set(file('src/main/openapi/orders.yaml'))
                        }
                    }
                }
                """);
        Path spec = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Path baselines = testProjectDirectory.resolve("src/main/openapi-baselines/store-orders");
        Files.createDirectories(spec.getParent());
        Files.createDirectories(baselines);
        Files.writeString(baselines.resolve("1.0.0.yaml"), ordersContract("1.0.0", true));
        String current = ordersContract("1.1.0", false);
        Files.writeString(spec, current);
        Files.writeString(baselines.resolve("1.1.0.yaml"), current);

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiBreakingChangeCheck")
                        .buildAndFail();

        assertTrue(
                result.getOutput().contains("breaking changes require a major version increase"));
    }

    @Test
    void typedValidationRejectsUnsupportedAndDuplicateContracts() throws IOException {
        Project project =
                ProjectBuilder.builder().withProjectDir(testProjectDirectory.toFile()).build();
        project.getPlugins().apply(SmbtechOpenApiGeneratorPlugin.class);
        Path first = testProjectDirectory.resolve("src/main/openapi/first.yaml");
        Path second = testProjectDirectory.resolve("src/main/openapi/second.yaml");
        Files.createDirectories(first.getParent());
        Files.writeString(
                first,
                """
                openapi: 3.1.0
                info:
                  title: Orders
                  version: 1.0.0
                paths:
                  /orders:
                    get:
                      operationId: listOrders
                      responses:
                        '204':
                          description: No orders
                """);
        Files.writeString(
                second,
                """
                openapi: 3.1.0
                info:
                  title: Orders
                  version: 1.0.0
                paths:
                  /orders:
                    get:
                      operationId: listOrders
                      responses:
                        '204':
                          description: No orders
                """);
        Path unsupported = testProjectDirectory.resolve("src/main/openapi/unsupported.yaml");
        Files.writeString(
                unsupported,
                """
                swagger: '2.0'
                info:
                  title: Unsupported
                  version: 1.0.0
                paths: {}
                """);
        SmbtechOpenApiValidateSpecsTask task =
                (SmbtechOpenApiValidateSpecsTask)
                        project.getTasks().getByName("smbtechOpenApiValidateSpecs");

        GradleException exception = assertThrows(GradleException.class, task::validateSpecs);

        assertTrue(exception.getMessage().contains("unsupported.yaml"));
        assertTrue(exception.getMessage().contains("duplicates src/main/openapi/first.yaml"));
    }

    @Test
    void buildLogicCheckRejectsBlankGlobalGroupId() {
        Project project = ProjectBuilder.builder().build();

        project.getPlugins().apply(SmbtechOpenApiGeneratorPlugin.class);
        SmbtechOpenApiBuildLogicCheckTask task =
                (SmbtechOpenApiBuildLogicCheckTask)
                        project.getTasks()
                                .getByName(
                                        SmbtechOpenApiGeneratorPlugin.BUILD_LOGIC_CHECK_TASK_NAME);

        task.getGroupId().set(" ");
        task.getOutputDirectory().set("build/generated/smbtech-openapi");
        task.getRepositoryDirectory().set("build/repository/openapi");
        task.getSpecConfigurations().set(java.util.List.of());

        assertThrows(GradleException.class, task::validate);
    }

    @Test
    void buildLogicCheckRejectsSpecWithoutInput() {
        Project project = ProjectBuilder.builder().build();

        project.getPlugins().apply(SmbtechOpenApiGeneratorPlugin.class);
        SmbtechOpenApiBuildLogicCheckTask task =
                (SmbtechOpenApiBuildLogicCheckTask)
                        project.getTasks()
                                .getByName(
                                        SmbtechOpenApiGeneratorPlugin.BUILD_LOGIC_CHECK_TASK_NAME);

        task.getGroupId().set("com.smbtech.openapi");
        task.getOutputDirectory().set("build/generated/smbtech-openapi");
        task.getRepositoryDirectory().set("build/repository/openapi");
        task.getSpecConfigurations().set(java.util.List.of("merchantOrderStatus|||||"));

        assertThrows(GradleException.class, task::validate);
    }

    @Test
    void buildLogicCheckRejectsConfigurationWithoutArtifacts() {
        Project project = ProjectBuilder.builder().build();

        project.getPlugins().apply(SmbtechOpenApiGeneratorPlugin.class);
        SmbtechOpenApiExtension extension =
                project.getExtensions().getByType(SmbtechOpenApiExtension.class);
        extension.getPublishModels().set(false);
        extension.getPublishServerApi().set(false);
        extension.getPublishClient().set(false);
        SmbtechOpenApiBuildLogicCheckTask task =
                (SmbtechOpenApiBuildLogicCheckTask)
                        project.getTasks()
                                .getByName(
                                        SmbtechOpenApiGeneratorPlugin.BUILD_LOGIC_CHECK_TASK_NAME);

        GradleException exception = assertThrows(GradleException.class, task::validate);

        assertTrue(exception.getMessage().contains("must enable at least one generated artifact"));
    }

    @Test
    void buildLogicCheckRejectsRelativePublicationRepositoryUrl() {
        Project project = ProjectBuilder.builder().build();

        project.getPlugins().apply(SmbtechOpenApiGeneratorPlugin.class);
        SmbtechOpenApiExtension extension =
                project.getExtensions().getByType(SmbtechOpenApiExtension.class);
        extension.getPublicationRepositoryUrl().set("repository/releases");
        SmbtechOpenApiBuildLogicCheckTask task =
                (SmbtechOpenApiBuildLogicCheckTask)
                        project.getTasks()
                                .getByName(
                                        SmbtechOpenApiGeneratorPlugin.BUILD_LOGIC_CHECK_TASK_NAME);

        GradleException exception = assertThrows(GradleException.class, task::validate);

        assertTrue(exception.getMessage().contains("must be an absolute URI"));
    }

    @Test
    void buildLogicCheckRejectsInvalidGeneratedPackage() {
        Project project = ProjectBuilder.builder().build();

        project.getPlugins().apply(SmbtechOpenApiGeneratorPlugin.class);
        SmbtechOpenApiExtension extension =
                project.getExtensions().getByType(SmbtechOpenApiExtension.class);
        extension
                .getSpecs()
                .register(
                        "orders",
                        spec -> {
                            spec.getInput().set(project.file("contracts/orders.yaml"));
                            spec.getModelPackage().set("com.smbtech.invalid-package");
                        });
        SmbtechOpenApiBuildLogicCheckTask task =
                (SmbtechOpenApiBuildLogicCheckTask)
                        project.getTasks()
                                .getByName(
                                        SmbtechOpenApiGeneratorPlugin.BUILD_LOGIC_CHECK_TASK_NAME);

        GradleException exception = assertThrows(GradleException.class, task::validate);

        assertTrue(exception.getMessage().contains("modelPackage must be a valid Java package"));
    }

    private void writeConsumerBuild() throws IOException {
        Files.writeString(
                testProjectDirectory.resolve("settings.gradle"), "rootProject.name = 'consumer'\n");
        Files.writeString(
                testProjectDirectory.resolve("build.gradle"),
                """
                plugins {
                    id 'com.smbtech.service-framework.openapi-generator'
                }

                repositories {
                    mavenCentral()
                }

                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                    specs {
                        register('orders') {
                            input.set(file('src/main/openapi/orders.yaml'))
                        }
                    }
                }
                """);
    }

    private static String ordersContract(String version, boolean includeGet) {
        String path =
                includeGet
                        ? """
                          /orders:
                            get:
                              operationId: listOrders
                              responses:
                                '200':
                                  description: Orders
                                  content:
                                    application/json:
                                      schema:
                                        type: array
                                        items:
                                          $ref: '#/components/schemas/Order'
                          """
                        : """
                          /health:
                            get:
                              operationId: health
                              responses:
                                '204':
                                  description: Healthy
                          """;
        return """
                openapi: 3.0.3
                info:
                  title: store-orders
                  version: %s
                paths:
                %s
                components:
                  schemas:
                    Order:
                      type: object
                      required: [id]
                      properties:
                        id:
                          type: string
                """
                .formatted(version, path.indent(2));
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Files.readAllBytes(path));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void deleteDirectory(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
