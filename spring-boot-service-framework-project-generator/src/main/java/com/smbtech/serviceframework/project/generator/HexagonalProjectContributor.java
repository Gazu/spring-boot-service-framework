package com.smbtech.serviceframework.project.generator;

import io.spring.initializr.generator.project.contributor.ProjectContributor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;

final class HexagonalProjectContributor implements ProjectContributor {

    private final ResolvedProject project;

    HexagonalProjectContributor(ResolvedProject project) {
        this.project = project;
    }

    @Override
    public void contribute(Path root) throws IOException {
        String packagePath = project.basePackage().replace('.', '/');
        Path mainJava = root.resolve("src/main/java").resolve(packagePath);
        Path testJava = root.resolve("src/test/java").resolve(packagePath);
        write(root.resolve("settings.gradle"), settingsGradle());
        write(root.resolve("build.gradle"), buildGradle());
        write(root.resolve("gradle.properties"), gradleProperties());
        write(root.resolve(".gitignore"), gitIgnore());
        write(root.resolve(".gitattributes"), "* text=auto eol=lf\n");
        write(root.resolve("README.md"), readme());
        Files.deleteIfExists(root.resolve("HELP.md"));
        Files.deleteIfExists(root.resolve("src/main/resources/application.yaml"));
        write(root.resolve("src/main/resources/application.yml"), applicationYaml());
        write(mainJava.resolve(project.applicationName() + ".java"), applicationJava());
        writePackage(
                mainJava.resolve("domain/model/package-info.java"),
                "Domain models and invariants.",
                "domain.model");
        writePackage(
                mainJava.resolve("application/port/in/package-info.java"),
                "Inbound use-case contracts.",
                "application.port.in");
        writePackage(
                mainJava.resolve("application/port/out/package-info.java"),
                "Outbound dependency contracts.",
                "application.port.out");
        writePackage(
                mainJava.resolve("application/service/package-info.java"),
                "Application use-case implementations.",
                "application.service");
        writePackage(
                mainJava.resolve("adapter/in/web/package-info.java"),
                "OpenAPI web adapters.",
                "adapter.in.web");
        writePackage(
                mainJava.resolve("adapter/out/package-info.java"),
                "Outbound infrastructure adapters.",
                "adapter.out");
        writePackage(
                mainJava.resolve("configuration/package-info.java"),
                "Application composition and configuration.",
                "configuration");
        for (String delegate : project.contract().delegateTypes()) {
            String simpleName = delegate.substring(delegate.lastIndexOf('.') + 1);
            write(
                    mainJava.resolve("adapter/in/web/" + simpleName + "Adapter.java"),
                    delegateAdapter(delegate));
        }
        Files.deleteIfExists(testJava.resolve(project.applicationName() + "Tests.java"));
        write(testJava.resolve("ApplicationContextTest.java"), contextTest());
        write(testJava.resolve("HexagonalArchitectureTest.java"), architectureTest());
        write(root.resolve("src/main/openapi/contract.yaml"), project.contract().document());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private String settingsGradle() {
        return "rootProject.name = '" + project.artifactId() + "'\n";
    }

    private String buildGradle() {
        String repository = "";
        if (project.contractRepository() != null) {
            repository =
                    "    maven { url = uri('"
                            + escape(project.contractRepository().toString())
                            + "') }\n";
        }
        return """
                plugins {
                    id 'java'
                    id 'org.springframework.boot' version '%s'
                }

                group = '%s'
                version = '%s'

                java {
                    toolchain {
                        languageVersion = JavaLanguageVersion.of(21)
                    }
                }

                repositories {
                    mavenLocal()
                %s    mavenCentral()
                }

                dependencies {
                    implementation platform('com.smbtech:spring-boot-service-framework-platform:%s')
                    implementation 'com.smbtech:spring-boot-service-framework-starter-error-handling'
                    implementation 'com.smbtech:spring-boot-service-framework-starter-logging'
                    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
                    implementation '%s'

                    testImplementation 'org.springframework.boot:spring-boot-starter-test'
                    testImplementation 'com.tngtech.archunit:archunit-junit5:%s'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
                }

                tasks.withType(JavaCompile).configureEach {
                    options.release = 21
                    options.encoding = 'UTF-8'
                    options.compilerArgs += ['-parameters']
                }

                tasks.withType(Test).configureEach {
                    useJUnitPlatform()
                }
                """
                .formatted(
                        project.springBootVersion(),
                        project.groupId(),
                        project.projectVersion(),
                        repository,
                        project.frameworkVersion(),
                        project.contract().coordinate(),
                        ProjectGeneratorVersions.archUnitVersion());
    }

    private static String gradleProperties() {
        return "org.gradle.caching=true\norg.gradle.parallel=true\n";
    }

    private static String gitIgnore() {
        return ".gradle/\nbuild/\nout/\n*.log\n.idea/\n*.iml\n.DS_Store\n";
    }

    private String applicationYaml() {
        return """
                spring:
                  application:
                    name: %s

                smbtech:
                  error-handling:
                    response:
                      exposure: PUBLIC
                """
                .formatted(project.artifactId());
    }

    private String applicationJava() {
        Set<String> scanPackages = new LinkedHashSet<>();
        scanPackages.add(project.basePackage());
        scanPackages.addAll(project.contract().apiPackages());
        String packages =
                scanPackages.stream()
                        .map(value -> "\"" + value + "\"")
                        .collect(Collectors.joining(", "));
        return """
                package %s;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication(scanBasePackages = {%s})
                public class %s {

                    public static void main(String[] args) {
                        SpringApplication.run(%s.class, args);
                    }
                }
                """
                .formatted(
                        project.basePackage(),
                        packages,
                        project.applicationName(),
                        project.applicationName());
    }

    private String delegateAdapter(String delegate) {
        String simpleName = delegate.substring(delegate.lastIndexOf('.') + 1);
        return """
                package %s.adapter.in.web;

                import %s;
                import org.springframework.stereotype.Component;

                @Component
                public final class %sAdapter implements %s {}
                """
                .formatted(project.basePackage(), delegate, simpleName, simpleName);
    }

    private String contextTest() {
        return """
                package %s;

                import org.junit.jupiter.api.Test;
                import org.springframework.boot.test.context.SpringBootTest;

                @SpringBootTest
                class ApplicationContextTest {

                    @Test
                    void contextLoads() {}
                }
                """
                .formatted(project.basePackage());
    }

    private String architectureTest() {
        return """
                package %s;

                import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

                import com.tngtech.archunit.core.importer.ImportOption;
                import com.tngtech.archunit.junit.AnalyzeClasses;
                import com.tngtech.archunit.junit.ArchTest;
                import com.tngtech.archunit.lang.ArchRule;

                @AnalyzeClasses(
                        packages = "%s",
                        importOptions = ImportOption.DoNotIncludeTests.class)
                class HexagonalArchitectureTest {

                    @ArchTest
                    static final ArchRule domain_is_framework_independent =
                            noClasses()
                                    .that()
                                    .resideInAPackage("..domain..")
                                    .should()
                                    .dependOnClassesThat()
                                    .resideInAnyPackage(
                                            "org.springframework..",
                                            "..application..",
                                            "..adapter..",
                                            "..configuration..")
                                    .allowEmptyShould(true);

                    @ArchTest
                    static final ArchRule application_does_not_depend_on_adapters =
                            noClasses()
                                    .that()
                                    .resideInAPackage("..application..")
                                    .should()
                                    .dependOnClassesThat()
                                    .resideInAnyPackage("..adapter..", "..configuration..")
                                    .allowEmptyShould(true);

                    @ArchTest
                    static final ArchRule inbound_adapters_do_not_depend_on_outbound_adapters =
                            noClasses()
                                    .that()
                                    .resideInAPackage("..adapter.in..")
                                    .should()
                                    .dependOnClassesThat()
                                    .resideInAPackage("..adapter.out..")
                                    .allowEmptyShould(true);
                }
                """
                .formatted(project.basePackage(), project.basePackage());
    }

    private String readme() {
        return """
                # %s

                Spring Boot service scaffold generated from `%s` version `%s`.

                ## Contract

                - Server API: `%s`
                - Embedded copy: `src/main/openapi/contract.yaml`
                - Generated delegates are implemented in `adapter.in.web`.

                ## Architecture

                Business models belong in `domain`. Use cases and ports belong in
                `application`. Spring MVC and infrastructure code remain in `adapter`.
                `HexagonalArchitectureTest` protects these dependency directions.

                ## Build

                ```bash
                ./gradlew clean build
                ```

                Generation is a one-time bootstrap operation. Subsequent contract changes
                must be adopted explicitly and must not overwrite application code.
                """
                .formatted(
                        project.artifactId(),
                        project.contract().title(),
                        project.contract().version(),
                        project.contract().coordinate());
    }

    private void writePackage(Path path, String description, String suffix) throws IOException {
        write(
                path,
                "/** "
                        + description
                        + " */\npackage "
                        + project.basePackage()
                        + "."
                        + suffix
                        + ";\n");
    }

    private static void write(Path path, String content) throws IOException {
        write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void write(Path path, byte[] content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(
                path,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
