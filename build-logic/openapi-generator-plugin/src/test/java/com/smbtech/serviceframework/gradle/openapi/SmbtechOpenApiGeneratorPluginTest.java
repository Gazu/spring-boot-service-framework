package com.smbtech.serviceframework.gradle.openapi;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
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
        assertNotNull(project.getTasks().findByName("smbtechOpenApiPublishContract"));
        assertNotNull(
                project.getTasks().findByName("smbtechOpenApiPublishContractToLocalRepository"));
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
    void discoversAndPackagesAllArtifactsFromAServiceFolder() throws IOException {
        writeAutoDiscoveredConsumerBuild("");
        Path spec = testProjectDirectory.resolve("ms-kyc-profile/swagger/openapi.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, taggedClientContract("2.1.0"));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments(
                                "smbtechOpenApiAssemble",
                                "smbtechOpenApiPublishToLocalRepository",
                                "smbtechOpenApiConsumerTest")
                        .build();

        assertEquals(SUCCESS, result.task(":smbtechOpenApiAssemble").getOutcome());
        assertEquals(SUCCESS, result.task(":smbtechOpenApiPublishToLocalRepository").getOutcome());
        assertEquals(SUCCESS, result.task(":smbtechOpenApiConsumerTest").getOutcome());
        Path libraries = testProjectDirectory.resolve("build/libs/smbtech-openapi");
        assertTrue(Files.isRegularFile(libraries.resolve("store-orders-jdk21-model-2.1.0.jar")));
        assertTrue(Files.isRegularFile(libraries.resolve("store-orders-jdk21-api-2.1.0.jar")));
        assertTrue(Files.isRegularFile(libraries.resolve("store-orders-jdk21-client-2.1.0.jar")));
        assertTrue(
                Files.isRegularFile(
                        testProjectDirectory.resolve(
                                "build/repository/openapi/com/smbtech/contracts/"
                                        + "store-orders-jdk21-client/2.1.0/"
                                        + "store-orders-jdk21-client-2.1.0.pom")));
        assertTrue(result.getOutput().contains("generateDiscoveredMsKycProfileSwaggerOpenapi"));
    }

    @Test
    void publishesOnlyTheContractSelectedByProjectRelativePath() throws IOException {
        writeAutoDiscoveredConsumerBuild("");
        Path selected = testProjectDirectory.resolve("ms-kyc-profile/swagger/openapi.yaml");
        Path ignored = testProjectDirectory.resolve("ms-payments/openapi/openapi.yaml");
        Files.createDirectories(selected.getParent());
        Files.createDirectories(ignored.getParent());
        Files.writeString(selected, taggedClientContract("1.0.0"));
        Files.writeString(
                ignored,
                taggedClientContract("2.0.0")
                        .replace("title: store-orders", "title: payment-orders"));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments(
                                "smbtechOpenApiPublishContractToLocalRepository",
                                "-PopenApiContract=ms-kyc-profile/swagger/openapi.yaml")
                        .build();

        assertEquals(
                SUCCESS,
                result.task(":smbtechOpenApiPublishContractToLocalRepository").getOutcome());
        Path repository =
                testProjectDirectory.resolve("build/repository/openapi/com/smbtech/contracts");
        for (String suffix : Set.of("model", "api", "client")) {
            assertTrue(
                    Files.isRegularFile(
                            repository.resolve(
                                    "store-orders-jdk21-"
                                            + suffix
                                            + "/1.0.0/store-orders-jdk21-"
                                            + suffix
                                            + "-1.0.0.pom")));
            assertFalse(Files.exists(repository.resolve("payment-orders-jdk21-" + suffix)));
        }
        assertFalse(result.getOutput().contains("generateDiscoveredMsPaymentsOpenapiOpenapi"));
    }

    @Test
    void rejectsIndependentPublicationWithoutAContractSelector() throws IOException {
        writeAutoDiscoveredConsumerBuild(
                """
                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                }
                """);
        Path spec = testProjectDirectory.resolve("ms-kyc-profile/swagger/openapi.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, ordersContract("1.0.0", true));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiPublishContractToLocalRepository")
                        .buildAndFail();

        assertTrue(
                result.getOutput()
                        .contains(
                                "Contract publication requires -PopenApiContract=<project-relative-openapi-path>"));
    }

    @Test
    void rejectsAnUnknownIndependentPublicationContract() throws IOException {
        writeAutoDiscoveredConsumerBuild(
                """
                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                }
                """);
        Path spec = testProjectDirectory.resolve("ms-kyc-profile/swagger/openapi.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, ordersContract("1.0.0", true));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments(
                                "smbtechOpenApiPublishContractToLocalRepository",
                                "-PopenApiContract=ms-unknown/swagger/openapi.yaml")
                        .buildAndFail();

        assertTrue(result.getOutput().contains("ms-unknown/swagger/openapi.yaml"));
        assertTrue(
                result.getOutput()
                        .contains("Available contracts: ms-kyc-profile/swagger/openapi.yaml"));
    }

    @Test
    void publishesASelectedContractToTheRemoteRepositoryFromProjectProperties() throws IOException {
        writeAutoDiscoveredConsumerBuild(
                """
                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                }
                """);
        Path spec = testProjectDirectory.resolve("ms-kyc-profile/swagger/openapi.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, ordersContract("1.0.0", true));
        Path remoteRepository = testProjectDirectory.resolve("remote-repository");
        Map<String, String> isolatedEnvironment = new HashMap<>(System.getenv());
        isolatedEnvironment.remove("OPENAPI_REPOSITORY_URL");
        isolatedEnvironment.remove("OPENAPI_REPOSITORY_USERNAME");
        isolatedEnvironment.remove("OPENAPI_REPOSITORY_PASSWORD");

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withEnvironment(isolatedEnvironment)
                        .withArguments(
                                "smbtechOpenApiPublishContract",
                                "-PopenApiContract=ms-kyc-profile/swagger/openapi.yaml",
                                "-PopenApiRepositoryUrl=" + remoteRepository.toUri(),
                                "-PopenApiRepositoryUsername=",
                                "-PopenApiRepositoryPassword=")
                        .build();

        assertEquals(SUCCESS, result.task(":smbtechOpenApiPublishContract").getOutcome());
        assertTrue(
                Files.isRegularFile(
                        remoteRepository.resolve(
                                "com/smbtech/contracts/store-orders-jdk21-model/1.0.0/"
                                        + "store-orders-jdk21-model-1.0.0.pom")));
    }

    @Test
    void discoversMultipleFoldersAndVersionsWithTheSameFileName() throws IOException {
        writeAutoDiscoveredConsumerBuild(
                """
                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                }
                """);
        Path first = testProjectDirectory.resolve("ms-kyc-profile/swagger/openapi.yaml");
        Path second = testProjectDirectory.resolve("ms-kyc-profile-v2/swagger/openapi.yaml");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.writeString(first, ordersContract("1.0.0", true));
        Files.writeString(second, ordersContract("2.1.0", true));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiCompatibilityCheck")
                        .build();

        assertEquals(SUCCESS, result.task(":smbtechOpenApiCompatibilityCheck").getOutcome());
        Path libraries = testProjectDirectory.resolve("build/libs/smbtech-openapi");
        assertTrue(Files.isRegularFile(libraries.resolve("store-orders-jdk21-model-1.0.0.jar")));
        assertTrue(Files.isRegularFile(libraries.resolve("store-orders-jdk21-model-2.1.0.jar")));
        Path reports = testProjectDirectory.resolve("build/reports/smbtech-openapi");
        assertTrue(Files.isRegularFile(reports.resolve("diff/store-orders-1.0.0.md")));
        assertTrue(Files.isRegularFile(reports.resolve("diff/store-orders-2.1.0.md")));
        String mockContracts = Files.readString(reports.resolve("mock-contracts.properties"));
        assertTrue(mockContracts.contains("store-orders.1.0.0=classpath:"));
        assertTrue(mockContracts.contains("store-orders.2.1.0=classpath:"));
    }

    @Test
    void explicitConfigurationOverridesDiscoveryForTheSameFile() throws IOException {
        writeAutoDiscoveredConsumerBuild(
                """
                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                    specs {
                        register('kycProfile') {
                            input.set(file('ms-kyc-profile/swagger/openapi.yaml'))
                            artifactBaseName.set('custom-kyc-profile')
                        }
                    }
                }
                """);
        Path spec = testProjectDirectory.resolve("ms-kyc-profile/swagger/openapi.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, ordersContract("1.0.0", true));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiAssemble")
                        .build();

        assertEquals(SUCCESS, result.task(":smbtechOpenApiAssemble").getOutcome());
        Path libraries = testProjectDirectory.resolve("build/libs/smbtech-openapi");
        assertTrue(
                Files.isRegularFile(libraries.resolve("custom-kyc-profile-jdk21-model-1.0.0.jar")));
        assertFalse(Files.exists(libraries.resolve("store-orders-jdk21-model-1.0.0.jar")));
        assertFalse(result.getOutput().contains("DiscoveredMsKycProfileSwaggerOpenapi"));
    }

    @Test
    void discoveryUsesOnlyDirectFilesInConventionalDirectories() throws IOException {
        Project project =
                ProjectBuilder.builder().withProjectDir(testProjectDirectory.toFile()).build();
        Path sourceOpenApi = testProjectDirectory.resolve("one/src/main/openapi/one.yaml");
        Path openApi = testProjectDirectory.resolve("two/openapi/two.yml");
        Path swagger = testProjectDirectory.resolve("three/swagger/three.json");
        Path nested = testProjectDirectory.resolve("four/swagger/nested/four.yaml");
        Path generated = testProjectDirectory.resolve("five/build/swagger/five.yaml");
        for (Path path : Set.of(sourceOpenApi, openApi, swagger, nested, generated)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "{}");
        }

        Set<Path> discovered =
                OpenApiSpecDiscovery.discover(project).stream()
                        .map(OpenApiSpecDiscovery::normalizedPath)
                        .collect(java.util.stream.Collectors.toSet());

        assertEquals(
                Set.of(
                        OpenApiSpecDiscovery.normalizedPath(sourceOpenApi.toFile()),
                        OpenApiSpecDiscovery.normalizedPath(openApi.toFile()),
                        OpenApiSpecDiscovery.normalizedPath(swagger.toFile())),
                discovered);
    }

    @Test
    void discoveredInvalidContractFailsTheCompatibilityGateThroughValidation() throws IOException {
        writeAutoDiscoveredConsumerBuild(
                """
                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                }
                """);
        Path invalid = testProjectDirectory.resolve("ms-kyc-profile/swagger/openapi.yaml");
        Files.createDirectories(invalid.getParent());
        Files.writeString(invalid, "{}\n");

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiCompatibilityCheck")
                        .buildAndFail();

        assertTrue(result.getOutput().contains("OpenAPI spec validation issues found"));
        assertTrue(result.getOutput().contains("ms-kyc-profile/swagger/openapi.yaml"));
    }

    @Test
    void discoveredCoordinateCollisionFailsWithBothPaths() throws IOException {
        writeAutoDiscoveredConsumerBuild(
                """
                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                }
                """);
        Path first = testProjectDirectory.resolve("first/swagger/openapi.yaml");
        Path second = testProjectDirectory.resolve("second/swagger/openapi.yaml");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.writeString(first, ordersContract("1.0.0", true));
        Files.writeString(second, ordersContract("1.0.0", true));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiValidateSpecs")
                        .buildAndFail();

        assertTrue(result.getOutput().contains("first/swagger/openapi.yaml"));
        assertTrue(result.getOutput().contains("second/swagger/openapi.yaml"));
        assertTrue(result.getOutput().contains("duplicates"));
    }

    @Test
    void rejectsTwoExplicitRegistrationsForTheSameCanonicalFile() throws IOException {
        writeAutoDiscoveredConsumerBuild(
                """
                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                    specs {
                        register('first') {
                            input.set(file('contracts/orders.yaml'))
                        }
                        register('second') {
                            input.set(file('contracts/orders.yaml'))
                        }
                    }
                }
                """);
        Path spec = testProjectDirectory.resolve("contracts/orders.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, ordersContract("1.0.0", true));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiValidateSpecs")
                        .buildAndFail();

        assertTrue(result.getOutput().contains("is configured by both"));
        assertTrue(result.getOutput().contains("smbtechOpenApi.specs.first"));
        assertTrue(result.getOutput().contains("smbtechOpenApi.specs.second"));
    }

    @Test
    void rejectsCoordinatesThatWouldWriteTheSameArtifactFile() throws IOException {
        writeAutoDiscoveredConsumerBuild(
                """
                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                    specs {
                        register('first') {
                            input.set(file('contracts/first.yaml'))
                            groupId.set('com.example.first')
                            artifactBaseName.set('shared-contract')
                        }
                        register('second') {
                            input.set(file('contracts/second.yaml'))
                            groupId.set('com.example.second')
                            artifactBaseName.set('shared-contract')
                        }
                    }
                }
                """);
        Path first = testProjectDirectory.resolve("contracts/first.yaml");
        Path second = testProjectDirectory.resolve("contracts/second.yaml");
        Files.createDirectories(first.getParent());
        Files.writeString(
                first,
                ordersContract("1.0.0", true).replace("title: store-orders", "title: first"));
        Files.writeString(
                second,
                ordersContract("1.0.0", true).replace("title: store-orders", "title: second"));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiValidateSpecs")
                        .buildAndFail();

        assertTrue(
                result.getOutput()
                        .contains(
                                "generated artifact file shared-contract-jdk21-model-1.0.0.jar duplicates"));
        assertTrue(result.getOutput().contains("output file names do not include groupId"));
    }

    @Test
    void configurationCacheDetectsANewConventionalContract() throws IOException {
        writeAutoDiscoveredConsumerBuild(
                """
                smbtechOpenApi {
                    publishServerApi.set(false)
                    publishClient.set(false)
                }
                """);
        Path first = testProjectDirectory.resolve("first/swagger/openapi.yaml");
        Files.createDirectories(first.getParent());
        Files.writeString(first, ordersContract("1.0.0", true));

        GradleRunner.create()
                .withProjectDir(testProjectDirectory.toFile())
                .withPluginClasspath()
                .withArguments("smbtechOpenApiAssemble", "--configuration-cache")
                .build();

        Path second = testProjectDirectory.resolve("second/swagger/openapi.yaml");
        Files.createDirectories(second.getParent());
        Files.writeString(
                second,
                ordersContract("1.0.0", true).replace("title: store-orders", "title: payments"));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiAssemble", "--configuration-cache")
                        .build();

        assertEquals(SUCCESS, result.task(":smbtechOpenApiAssemble").getOutcome());
        assertTrue(
                Files.isRegularFile(
                        testProjectDirectory.resolve(
                                "build/libs/smbtech-openapi/payments-jdk21-model-1.0.0.jar")));
    }

    @Test
    void configuresJava21ArtifactsAndVersionedPackagesFromInfoVersion() throws IOException {
        Project project =
                ProjectBuilder.builder().withProjectDir(testProjectDirectory.toFile()).build();
        project.getPlugins().apply(SmbtechOpenApiGeneratorPlugin.class);
        Path specFile = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Files.createDirectories(specFile.getParent());
        Files.writeString(specFile, ordersContract("2.1.0", true));
        SmbtechOpenApiExtension extension =
                project.getExtensions().getByType(SmbtechOpenApiExtension.class);

        extension.specs(
                specs -> specs.register("orders", spec -> spec.getInput().set(specFile.toFile())));

        SmbtechOpenApiGenerateTask models =
                (SmbtechOpenApiGenerateTask)
                        project.getTasks().getByName("generateOrdersModelsOpenApiSources");
        SmbtechOpenApiGenerateTask api =
                (SmbtechOpenApiGenerateTask)
                        project.getTasks().getByName("generateOrdersServerApiOpenApiSources");
        SmbtechOpenApiGenerateTask client =
                (SmbtechOpenApiGenerateTask)
                        project.getTasks().getByName("generateOrdersClientOpenApiSources");

        assertEquals("store-orders-jdk21-model", models.getArtifactId().get());
        assertEquals("store-orders-jdk21-api", api.getArtifactId().get());
        assertEquals("store-orders-jdk21-client", client.getArtifactId().get());
        assertEquals("2.1.0", models.getArtifactVersion().get());
        assertEquals("com.smbtech.contracts.storeorders.v2.model", models.getModelPackage().get());
        assertEquals("com.smbtech.contracts.storeorders.v2.api", api.getApiPackage().get());
        assertEquals(
                "com.smbtech.contracts.storeorders.v2.client.httpinterface",
                client.getApiPackage().get());
        assertEquals(
                "com.smbtech.contracts.storeorders.v2.client.openfeign",
                client.getOpenFeignPackage().get());

        PublishingExtension publishing =
                project.getExtensions().getByType(PublishingExtension.class);
        assertEquals(
                "store-orders-jdk21-model",
                ((MavenPublication) publishing.getPublications().getByName("ordersModelsOpenApi"))
                        .getArtifactId());
        assertEquals(
                "store-orders-jdk21-api",
                ((MavenPublication)
                                publishing.getPublications().getByName("ordersServerApiOpenApi"))
                        .getArtifactId());
        assertEquals(
                "store-orders-jdk21-client",
                ((MavenPublication) publishing.getPublications().getByName("ordersClientOpenApi"))
                        .getArtifactId());

        var apiDependencies =
                project.getConfigurations()
                        .getByName("ordersServerApiOpenApiApiElements")
                        .getDependencies();
        var clientDependencies =
                project.getConfigurations()
                        .getByName("ordersClientOpenApiApiElements")
                        .getDependencies();
        assertTrue(
                apiDependencies.stream()
                        .anyMatch(
                                dependency ->
                                        dependency.getName().equals("store-orders-jdk21-model")));
        assertTrue(
                clientDependencies.stream()
                        .anyMatch(
                                dependency ->
                                        dependency.getName().equals("store-orders-jdk21-model")));
        assertFalse(
                clientDependencies.stream()
                        .anyMatch(
                                dependency ->
                                        "org.springframework.cloud".equals(dependency.getGroup())));
    }

    @Test
    void generatesCompilesAndPackagesVersionedModelsFromAConsumerBuild() throws IOException {
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
        Path modelJar =
                testProjectDirectory.resolve(
                        "build/libs/smbtech-openapi/store-orders-jdk21-model-2.1.0.jar");
        assertTrue(Files.isRegularFile(modelJar));
        assertJarContains(modelJar, "com/smbtech/contracts/storeorders/v2/model/Order.class");
        assertJava21Bytecode(modelJar, "com/smbtech/contracts/storeorders/v2/model/Order.class");
        assertMetadata(modelJar, "spring-boot.version", "4.1.0");
        Path generatedModel =
                testProjectDirectory.resolve(
                        "build/generated/smbtech-openapi/orders/models/src/main/java/"
                                + "com/smbtech/contracts/storeorders/v2/model/Order.java");
        assertTrue(Files.readString(generatedModel).contains("@JsonProperty"));
    }

    @Test
    void generatesMatchingTaggedClientInterfaceFamiliesInOneContractJar() throws IOException {
        writeClientConsumerBuild();
        Path spec = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, taggedClientContract("2.1.0"));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments(
                                "smbtechOpenApiAssemble",
                                "sourcesJarOrdersClientOpenApi",
                                "smbtechOpenApiConsumerTest")
                        .build();

        assertEquals(SUCCESS, result.task(":smbtechOpenApiAssemble").getOutcome());
        assertEquals(SUCCESS, result.task(":smbtechOpenApiConsumerTest").getOutcome());
        Path libraries = testProjectDirectory.resolve("build/libs/smbtech-openapi");
        Path clientJar = libraries.resolve("store-orders-jdk21-client-2.1.0.jar");
        Path clientSourcesJar = libraries.resolve("store-orders-jdk21-client-2.1.0-sources.jar");
        String packageRoot = "com/smbtech/contracts/storeorders/v2/client/";
        String httpOrdersClass = packageRoot + "httpinterface/OrdersApi.class";
        String httpPaymentsClass = packageRoot + "httpinterface/PaymentsApi.class";
        String openFeignOrdersClass = packageRoot + "openfeign/OrdersApi.class";
        String openFeignPaymentsClass = packageRoot + "openfeign/PaymentsApi.class";
        String httpOrdersSource = packageRoot + "httpinterface/OrdersApi.java";
        String httpPaymentsSource = packageRoot + "httpinterface/PaymentsApi.java";
        String openFeignOrdersSource = packageRoot + "openfeign/OrdersApi.java";
        String openFeignPaymentsSource = packageRoot + "openfeign/PaymentsApi.java";

        assertTrue(Files.isRegularFile(clientJar));
        assertTrue(Files.isRegularFile(clientSourcesJar));
        assertEquals(
                Set.of(
                        httpOrdersClass,
                        httpPaymentsClass,
                        openFeignOrdersClass,
                        openFeignPaymentsClass),
                jarEntries(clientJar, ".class"));
        assertEquals(
                Set.of(
                        httpOrdersSource,
                        httpPaymentsSource,
                        openFeignOrdersSource,
                        openFeignPaymentsSource),
                jarEntries(clientSourcesJar, ".java"));
        for (String clientClass : jarEntries(clientJar, ".class")) {
            assertJava21Bytecode(clientJar, clientClass);
        }
        assertJarEntryContains(
                clientJar, httpOrdersClass, "HttpApiClient", "HttpExchange", "store-orders");
        assertJarEntryContains(
                clientJar, httpPaymentsClass, "HttpApiClient", "HttpExchange", "store-orders");
        assertJarEntryContains(
                clientJar,
                openFeignOrdersClass,
                "FeignClient",
                "RequestMapping",
                "store-orders-v2-OrdersApi");
        assertJarEntryContains(
                clientJar,
                openFeignPaymentsClass,
                "FeignClient",
                "RequestMapping",
                "store-orders-v2-PaymentsApi");
        assertJarEntryContains(
                clientSourcesJar,
                httpOrdersSource,
                "@HttpApiClient(\"store-orders\")",
                "@HttpExchange",
                "createOrder",
                "OrderRequest",
                "Order");
        assertJarEntryContains(
                clientSourcesJar,
                httpPaymentsSource,
                "@HttpApiClient(\"store-orders\")",
                "@HttpExchange",
                "getPayment",
                "Payment",
                "X-Correlation-Id");
        assertJarEntryContains(
                clientSourcesJar,
                openFeignOrdersSource,
                "@FeignClient(name = \"store-orders\"",
                "contextId = \"store-orders-v2-OrdersApi\"",
                "@RequestMapping",
                "createOrder",
                "OrderRequest",
                "Order");
        assertJarEntryContains(
                clientSourcesJar,
                openFeignPaymentsSource,
                "@FeignClient(name = \"store-orders\"",
                "contextId = \"store-orders-v2-PaymentsApi\"",
                "@RequestMapping",
                "getPayment",
                "Payment",
                "X-Correlation-Id");

        try (var artifacts = Files.list(libraries)) {
            long binaryClientJars =
                    artifacts
                            .map(path -> path.getFileName().toString())
                            .filter(name -> name.contains("-jdk21-client-"))
                            .filter(name -> name.endsWith(".jar"))
                            .filter(name -> !name.endsWith("-sources.jar"))
                            .count();
            assertEquals(1, binaryClientJars);
        }
    }

    @Test
    void publishesClientModelDependencyWithoutOpenFeignRuntime() throws IOException {
        writeClientConsumerBuild();
        Path spec = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, ordersContract("2.1.0", true));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiPublishToLocalRepository")
                        .build();

        assertEquals(SUCCESS, result.task(":smbtechOpenApiPublishToLocalRepository").getOutcome());
        Path pom =
                testProjectDirectory.resolve(
                        "build/repository/openapi/com/smbtech/contracts/"
                                + "store-orders-jdk21-client/2.1.0/"
                                + "store-orders-jdk21-client-2.1.0.pom");
        Path module =
                testProjectDirectory.resolve(
                        "build/repository/openapi/com/smbtech/contracts/"
                                + "store-orders-jdk21-client/2.1.0/"
                                + "store-orders-jdk21-client-2.1.0.module");
        assertTrue(Files.isRegularFile(pom));
        assertTrue(Files.isRegularFile(module));
        String pomContent = Files.readString(pom);
        assertTrue(pomContent.contains("<artifactId>store-orders-jdk21-model</artifactId>"));
        assertTrue(pomContent.contains("<version>2.1.0</version>"));
        assertTrue(pomContent.contains("<scope>compile</scope>"));
        assertNoOpenFeignDependency(pomContent);
        String moduleContent = Files.readString(module);
        assertTrue(moduleContent.contains("\"module\": \"store-orders-jdk21-model\""));
        assertTrue(moduleContent.contains("\"requires\": \"2.1.0\""));
        assertNoOpenFeignDependency(moduleContent);
    }

    @Test
    void compilesHttpInterfaceConsumerWithoutOpenFeignDependency() throws IOException {
        writeClientConsumerBuild();
        Path buildFile = testProjectDirectory.resolve("build.gradle");
        Files.writeString(
                buildFile,
                Files.readString(buildFile)
                        + """

                        apply plugin: 'java'

                        repositories {
                            maven { url = layout.buildDirectory.dir('repository/openapi') }
                        }

                        dependencies {
                            implementation 'com.smbtech.contracts:store-orders-jdk21-client:2.1.0'
                        }

                        tasks.named('compileJava') {
                            dependsOn 'smbtechOpenApiPublishToLocalRepository'
                        }
                        """);
        Path spec = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, taggedClientContract("2.1.0"));
        Path consumer = testProjectDirectory.resolve("src/main/java/example/OrdersGateway.java");
        Files.createDirectories(consumer.getParent());
        Files.writeString(
                consumer,
                """
                package example;

                import com.smbtech.contracts.storeorders.v2.client.httpinterface.OrdersApi;
                import com.smbtech.contracts.storeorders.v2.model.Order;
                import com.smbtech.contracts.storeorders.v2.model.OrderRequest;
                import org.springframework.http.ResponseEntity;

                final class OrdersGateway {

                    private final OrdersApi ordersApi;

                    OrdersGateway(OrdersApi ordersApi) {
                        this.ordersApi = ordersApi;
                    }

                    ResponseEntity<Order> create(OrderRequest request) {
                        return ordersApi.createOrder(request);
                    }
                }
                """);

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("compileJava")
                        .build();

        assertEquals(SUCCESS, result.task(":compileJava").getOutcome());
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
                        "build/libs/smbtech-openapi/store-orders-jdk21-model-1.0.0.jar");
        assertTrue(Files.isRegularFile(models));
        try (JarFile jar = new JarFile(models.toFile())) {
            assertNotNull(jar.getEntry("com/smbtech/contracts/storeorders/v1/model/Order.class"));
            assertNotNull(
                    jar.getEntry(
                            "META-INF/smbtech/openapi/contracts/store-orders/1.0.0/contract.yaml"));
        }
        assertTrue(
                Files.readString(
                                testProjectDirectory.resolve(
                                        "build/reports/smbtech-openapi/migration.md"))
                        .contains("store-orders-jdk21-model"));
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
    void rejectsTagsThatNormalizeToTheSameGeneratedApiFamily() throws IOException {
        Path spec = testProjectDirectory.resolve("src/main/openapi/tag-collision.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(
                spec,
                """
                openapi: 3.0.3
                info:
                  title: customer-profile
                  version: 1.0.0
                paths:
                  /customers:
                    get:
                      tags: [customer-profile]
                      operationId: listCustomers
                      responses:
                        '204':
                          description: No customers
                  /profiles:
                    get:
                      tags: [customer_profile]
                      operationId: listProfiles
                      responses:
                        '204':
                          description: No profiles
                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> OpenApiContractReader.readDocument(spec.toFile()));

        assertTrue(exception.getMessage().contains("normalize to generated API family"));
    }

    @Test
    void rejectsConfiguredVersionThatDiffersFromOpenApiInfoVersion() throws IOException {
        Files.writeString(
                testProjectDirectory.resolve("settings.gradle"), "rootProject.name = 'consumer'\n");
        Files.writeString(
                testProjectDirectory.resolve("build.gradle"),
                """
                plugins {
                    id 'com.smbtech.service-framework.openapi-generator'
                }

                smbtechOpenApi {
                    specs {
                        register('orders') {
                            input.set(file('src/main/openapi/orders.yaml'))
                            version.set('2.0.0')
                        }
                    }
                }
                """);
        Path spec = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, ordersContract("1.0.0", true));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiValidateSpecs")
                        .buildAndFail();

        assertTrue(result.getOutput().contains("info.version"));
        assertTrue(result.getOutput().contains("2.0.0"));
        assertTrue(result.getOutput().contains("1.0.0"));
    }

    @Test
    void rejectsPackageOverrideOutsideInfoVersionBoundary() throws IOException {
        Files.writeString(
                testProjectDirectory.resolve("settings.gradle"), "rootProject.name = 'consumer'\n");
        Files.writeString(
                testProjectDirectory.resolve("build.gradle"),
                """
                plugins {
                    id 'com.smbtech.service-framework.openapi-generator'
                }

                smbtechOpenApi {
                    specs {
                        register('orders') {
                            input.set(file('src/main/openapi/orders.yaml'))
                            basePackage.set('com.example.orders')
                            modelPackage.set('com.example.orders.model')
                        }
                    }
                }
                """);
        Path spec = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(spec, ordersContract("2.1.0", true));

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("smbtechOpenApiValidateSpecs")
                        .buildAndFail();

        assertTrue(result.getOutput().contains("modelPackage"));
        assertTrue(result.getOutput().contains("com.example.orders.v2.model"));
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

    private void writeAutoDiscoveredConsumerBuild(String configuration) throws IOException {
        writeRestClientStubRepository();
        Files.writeString(
                testProjectDirectory.resolve("settings.gradle"), "rootProject.name = 'consumer'\n");
        Files.writeString(
                testProjectDirectory.resolve("build.gradle"),
                """
                plugins {
                    id 'com.smbtech.service-framework.openapi-generator'
                }

                repositories {
                    maven { url = uri('test-repository') }
                    mavenCentral()
                }

                %s
                """
                        .formatted(configuration));
    }

    private void writeClientConsumerBuild() throws IOException {
        writeRestClientStubRepository();
        Files.writeString(
                testProjectDirectory.resolve("settings.gradle"), "rootProject.name = 'consumer'\n");
        Files.writeString(
                testProjectDirectory.resolve("build.gradle"),
                """
                plugins {
                    id 'com.smbtech.service-framework.openapi-generator'
                }

                repositories {
                    maven { url = uri('test-repository') }
                    mavenCentral()
                }

                smbtechOpenApi {
                    publishServerApi.set(false)
                    specs {
                        register('orders') {
                            input.set(file('src/main/openapi/orders.yaml'))
                        }
                    }
                }
                """);
    }

    private void writeRestClientStubRepository() throws IOException {
        String version = OpenApiToolchainVersions.load().frameworkVersion();
        Path source =
                testProjectDirectory.resolve(
                        "test-fixture-src/com/smbtech/serviceframework/starter/restclient/api/HttpApiClient.java");
        Files.createDirectories(source.getParent());
        Files.writeString(
                source,
                """
                package com.smbtech.serviceframework.starter.restclient.api;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Target(ElementType.TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                public @interface HttpApiClient {
                    String value();
                }
                """);
        Path classes = testProjectDirectory.resolve("test-fixture-classes");
        Files.createDirectories(classes);
        int compilation =
                ToolProvider.getSystemJavaCompiler()
                        .run(
                                null,
                                null,
                                null,
                                "--release",
                                "21",
                                "-d",
                                classes.toString(),
                                source.toString());
        assertEquals(0, compilation);

        Path module =
                testProjectDirectory.resolve(
                        "test-repository/com/smbtech/"
                                + "spring-boot-service-framework-starter-rest-client/"
                                + version);
        Files.createDirectories(module);
        String artifact = "spring-boot-service-framework-starter-rest-client-" + version;
        try (JarOutputStream jar =
                new JarOutputStream(Files.newOutputStream(module.resolve(artifact + ".jar")))) {
            try (var compiledFiles = Files.walk(classes)) {
                for (Path compiled : compiledFiles.filter(Files::isRegularFile).toList()) {
                    String entryName =
                            classes.relativize(compiled)
                                    .toString()
                                    .replace(File.separatorChar, '/');
                    jar.putNextEntry(new JarEntry(entryName));
                    Files.copy(compiled, jar);
                    jar.closeEntry();
                }
            }
        }
        Files.writeString(
                module.resolve(artifact + ".pom"),
                """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.smbtech</groupId>
                  <artifactId>spring-boot-service-framework-starter-rest-client</artifactId>
                  <version>%s</version>
                </project>
                """
                        .formatted(version));
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

    private static String taggedClientContract(String version) {
        return """
                openapi: 3.0.3
                info:
                  title: store-orders
                  version: %s
                paths:
                  /orders:
                    post:
                      tags: [Orders]
                      operationId: createOrder
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema:
                              $ref: '#/components/schemas/OrderRequest'
                      responses:
                        '201':
                          description: Order created
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Order'
                  /payments/{paymentId}:
                    get:
                      tags: [Payments]
                      operationId: getPayment
                      parameters:
                        - in: path
                          name: paymentId
                          required: true
                          schema:
                            type: string
                        - in: header
                          name: X-Correlation-Id
                          required: true
                          schema:
                            type: string
                      responses:
                        '200':
                          description: Payment found
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Payment'
                components:
                  schemas:
                    OrderRequest:
                      type: object
                      required: [sku]
                      properties:
                        sku:
                          type: string
                    Order:
                      type: object
                      required: [id]
                      properties:
                        id:
                          type: string
                    Payment:
                      type: object
                      required: [id]
                      properties:
                        id:
                          type: string
                """
                .formatted(version);
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Files.readAllBytes(path));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void assertJarContains(Path jarPath, String entry) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            assertNotNull(jar.getEntry(entry), () -> jarPath + " does not contain " + entry);
        }
    }

    private static Set<String> jarEntries(Path jarPath, String suffix) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Set<String> entries = new TreeSet<>();
            jar.stream()
                    .map(entry -> entry.getName())
                    .filter(name -> name.endsWith(suffix))
                    .forEach(entries::add);
            return entries;
        }
    }

    private static void assertJarEntryContains(
            Path jarPath, String entry, String... expectedMarkers) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var jarEntry = jar.getJarEntry(entry);
            assertNotNull(jarEntry, () -> jarPath + " does not contain " + entry);
            try (var input = jar.getInputStream(jarEntry)) {
                String content = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
                for (String marker : expectedMarkers) {
                    assertTrue(
                            content.contains(marker), () -> entry + " does not contain " + marker);
                }
            }
        }
    }

    private static void assertNoOpenFeignDependency(String metadata) {
        assertFalse(metadata.contains("org.springframework.cloud"));
        assertFalse(metadata.contains("spring-cloud-openfeign"));
        assertFalse(metadata.contains("spring-cloud-starter-openfeign"));
        assertFalse(metadata.contains("io.github.openfeign"));
    }

    private static void assertJava21Bytecode(Path jarPath, String entry) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile());
                var input = jar.getInputStream(jar.getJarEntry(entry))) {
            byte[] header = input.readNBytes(8);
            int majorVersion = ((header[6] & 0xff) << 8) | (header[7] & 0xff);
            assertEquals(65, majorVersion);
        }
    }

    private static void assertMetadata(Path jarPath, String key, String expected)
            throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile());
                var input =
                        jar.getInputStream(
                                jar.getJarEntry("META-INF/smbtech/openapi/contract.properties"))) {
            Properties metadata = new Properties();
            metadata.load(input);
            assertEquals(expected, metadata.getProperty(key));
        }
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
