package com.smbtech.serviceframework.gradle.openapi;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    void registersExtensionAndBuildLogicCheckTask() {
        Project project = ProjectBuilder.builder().build();

        project.getPlugins().apply(SmbtechOpenApiGeneratorPlugin.class);

        SmbtechOpenApiExtension extension =
                project.getExtensions().findByType(SmbtechOpenApiExtension.class);

        assertNotNull(extension);
        assertEquals("com.smbtech.openapi", extension.getGroupId().get());
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
        assertNotNull(
                project.getTasks()
                        .findByName(SmbtechOpenApiGeneratorPlugin.BUILD_LOGIC_CHECK_TASK_NAME));
        assertNotNull(project.getExtensions().findByType(PublishingExtension.class));
        assertNotNull(project.getTasks().findByName("generateOpenApiModels"));
        assertNotNull(project.getTasks().findByName("generateOpenApiServerApi"));
        assertNotNull(project.getTasks().findByName("generateOpenApiClient"));
        assertNotNull(project.getTasks().findByName("openApiCompatibilityCheck"));
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
                        });

        SmbtechOpenApiSpec spec = extension.getSpecs().getByName("merchantOrderStatus");

        assertEquals("merchantOrderStatus", spec.getName());
        assertEquals("merchant-order-status", spec.getArtifactBaseName().get());
        assertEquals("1.1.0", spec.getVersion().get());
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
                """);
        Path spec = testProjectDirectory.resolve("src/main/openapi/orders.yaml");
        Files.createDirectories(spec.getParent());
        Files.writeString(
                spec,
                """
                openapi: 3.0.3
                info:
                  title: orders
                  version: 1.0.0
                paths: {}
                """);

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(testProjectDirectory.toFile())
                        .withPluginClasspath()
                        .withArguments("validateOpenApiSpecs")
                        .build();

        assertEquals(SUCCESS, result.task(":validateOpenApiSpecs").getOutcome());
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
}
