package com.smbtech.serviceframework.project.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProjectGenerationRequestTest {

    @Test
    void providesPinnedDefaultsAndExplicitOverrides() {
        ProjectGenerationRequest request =
                ProjectGenerationRequest.builder(
                                new OpenApiDocumentSource(Path.of("contract.yaml")),
                                Path.of("output"))
                        .groupId("com.example")
                        .artifactId("order-service")
                        .basePackage("com.example.orders")
                        .applicationName("OrderApplication")
                        .build();

        assertEquals("com.example", request.groupId());
        assertEquals("order-service", request.artifactId());
        assertEquals("com.example.orders", request.basePackage());
        assertEquals("OrderApplication", request.applicationName());
        assertEquals(ProjectGeneratorVersions.frameworkVersion(), request.frameworkVersion());
        assertEquals(ProjectGeneratorVersions.springBootVersion(), request.springBootVersion());
        assertFalse(request.overwrite());
    }

    @Test
    void rejectsBlankBuilderValues() {
        ProjectGenerationRequest.Builder builder =
                ProjectGenerationRequest.builder(
                        new OpenApiDocumentSource(Path.of("contract.yaml")), Path.of("output"));

        assertThrows(IllegalArgumentException.class, () -> builder.groupId(" "));
        assertThrows(NullPointerException.class, () -> builder.contractRepository(null));
    }
}
