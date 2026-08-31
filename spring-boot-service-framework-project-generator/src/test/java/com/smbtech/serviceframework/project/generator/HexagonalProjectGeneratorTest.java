package com.smbtech.serviceframework.project.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HexagonalProjectGeneratorTest {

    @TempDir Path tempDirectory;

    @Test
    void generatesSpringInitializrProjectWithHexagonalBoundaries() throws IOException {
        Path spec = fixtureSpec();
        Path output = tempDirectory.resolve("customer-orders");

        GeneratedProject project =
                HexagonalProjectGenerator.create()
                        .generate(
                                ProjectGenerationRequest.builder(
                                                new OpenApiDocumentSource(spec), output)
                                        .groupId("com.example")
                                        .contractRepository(
                                                tempDirectory.resolve("repository").toUri())
                                        .build());

        assertEquals("customer-order-orchestration", project.contractId());
        assertEquals("2.1.0", project.contractVersion());
        assertEquals(2, project.delegateTypes().size());
        assertTrue(Files.isExecutable(output.resolve("gradlew")));
        assertTrue(Files.isRegularFile(output.resolve("gradle/wrapper/gradle-wrapper.jar")));
        assertTrue(Files.isRegularFile(output.resolve("src/main/openapi/contract.yaml")));
        assertTrue(Files.isRegularFile(javaFile(output, "domain/model/package-info.java")));
        assertTrue(Files.isRegularFile(javaFile(output, "application/port/in/package-info.java")));
        assertTrue(
                Files.isRegularFile(
                        javaFile(output, "adapter/in/web/OrdersApiDelegateAdapter.java")));
        assertTrue(
                Files.isRegularFile(
                        javaFile(output, "adapter/in/web/CustomersApiDelegateAdapter.java")));
        assertTrue(Files.isRegularFile(testJavaFile(output, "HexagonalArchitectureTest.java")));
        assertFalse(Files.exists(output.resolve("src/main/resources/application.yaml")));
        assertContains(
                output.resolve("build.gradle"), "com.tngtech.archunit:archunit-junit5:1.4.1");
        assertContains(
                output.resolve("build.gradle"),
                "com.smbtech.contracts:customer-order-orchestration-server-api:2.1.0");
        assertContains(
                testJavaFile(output, "HexagonalArchitectureTest.java"),
                "domain_is_framework_independent");
    }

    @Test
    void extractsContractAndDelegateTypesFromServerApiJar() throws IOException {
        Path jar = createServerApiJar(tempDirectory.resolve("orders-server-api.jar"));
        Path output = tempDirectory.resolve("jar-project");

        GeneratedProject project =
                HexagonalProjectGenerator.create()
                        .generate(
                                ProjectGenerationRequest.builder(
                                                new ServerApiJarSource(jar), output)
                                        .build());

        assertEquals(
                "com.example.contracts:orders-server-api:1.4.0", project.serverApiCoordinate());
        assertEquals(
                java.util.List.of("com.example.contracts.orders.api.OrdersApiDelegate"),
                project.delegateTypes());
        Path adapter =
                output.resolve(
                        "src/main/java/com/smbtech/services/orders/adapter/in/web/OrdersApiDelegateAdapter.java");
        assertTrue(Files.isRegularFile(adapter));
        assertContains(adapter, "implements OrdersApiDelegate");
    }

    @Test
    void protectsExistingOutputUnlessOverwriteIsExplicit() throws IOException {
        Path output = tempDirectory.resolve("existing");
        Files.createDirectories(output);
        Files.writeString(output.resolve("owned.txt"), "user content", StandardCharsets.UTF_8);
        ProjectGenerationRequest request =
                ProjectGenerationRequest.builder(new OpenApiDocumentSource(fixtureSpec()), output)
                        .build();

        assertThrows(
                ProjectGenerationException.class,
                () -> HexagonalProjectGenerator.create().generate(request));

        HexagonalProjectGenerator.create()
                .generate(
                        ProjectGenerationRequest.builder(request.contractSource(), output)
                                .overwrite(true)
                                .build());
        assertFalse(Files.exists(output.resolve("owned.txt")));
        assertTrue(Files.exists(output.resolve("build.gradle")));
    }

    @Test
    void rejectsJarWithoutFrameworkMetadata() throws IOException {
        Path jar = tempDirectory.resolve("invalid.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            add(output, "example.txt", "invalid");
        }

        ProjectGenerationRequest request =
                ProjectGenerationRequest.builder(
                                new ServerApiJarSource(jar),
                                tempDirectory.resolve("invalid-project"))
                        .build();
        assertThrows(
                ProjectGenerationException.class,
                () -> HexagonalProjectGenerator.create().generate(request));
    }

    private Path createServerApiJar(Path jar) throws IOException {
        String metadata =
                """
                artifact.group=com.example.contracts
                artifact.id=orders-server-api
                artifact.kind=server-api
                contract.id=orders
                contract.title=orders
                contract.version=1.4.0
                """;
        String contract = Files.readString(resource("/openapi/tagged-orders.txt"));
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            add(output, "META-INF/smbtech/openapi/contract.properties", metadata);
            add(output, "META-INF/smbtech/openapi/contract.yaml", contract);
            add(output, "com/example/contracts/orders/api/OrdersApiDelegate.class", "delegate");
        }
        return jar;
    }

    private static void add(JarOutputStream output, String name, String content)
            throws IOException {
        output.putNextEntry(new JarEntry(name));
        output.write(content.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private static Path resource(String name) {
        try {
            return Path.of(HexagonalProjectGeneratorTest.class.getResource(name).toURI());
        } catch (Exception exception) {
            throw new IllegalStateException("Missing test resource " + name, exception);
        }
    }

    private Path fixtureSpec() throws IOException {
        Path spec = tempDirectory.resolve("tagged-orders.yaml");
        Files.writeString(spec, Files.readString(resource("/openapi/tagged-orders.txt")));
        return spec;
    }

    private static Path javaFile(Path output, String suffix) {
        return output.resolve("src/main/java/com/example/services/customerorderorchestration")
                .resolve(suffix);
    }

    private static Path testJavaFile(Path output, String suffix) {
        return output.resolve("src/test/java/com/example/services/customerorderorchestration")
                .resolve(suffix);
    }

    private static void assertContains(Path file, String expected) throws IOException {
        assertTrue(Files.readString(file).contains(expected), file + " must contain " + expected);
    }
}
