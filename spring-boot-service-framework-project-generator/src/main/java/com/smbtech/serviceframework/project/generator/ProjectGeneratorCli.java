package com.smbtech.serviceframework.project.generator;

import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Command-line entry point for one-time project scaffolding. */
public final class ProjectGeneratorCli {

    private ProjectGeneratorCli() {}

    /**
     * Generates a project from command-line options.
     *
     * @param args generator options
     */
    public static void main(String[] args) {
        try {
            Options options = Options.parse(args);
            if (options.help()) {
                System.out.println(usage());
                return;
            }
            GeneratedProject generated =
                    HexagonalProjectGenerator.create().generate(options.request());
            System.out.println("Generated project: " + generated.directory());
            System.out.println("Contract: " + generated.serverApiCoordinate());
            generated.delegateTypes().forEach(type -> System.out.println("Delegate: " + type));
        } catch (IllegalArgumentException | ProjectGenerationException exception) {
            System.err.println("Project generation failed: " + exception.getMessage());
            System.err.println(usage());
            System.exit(2);
        }
    }

    private static String usage() {
        return """
                Usage:
                  project-generator --spec=<openapi.yml> --output=<directory> [options]
                  project-generator --api-jar=<server-api.jar> --output=<directory> [options]

                Options:
                  --group=<group>                       Generated project group
                  --artifact=<artifact>                 Generated project artifact
                  --package=<package>                   Generated Java base package
                  --application-name=<class>            Spring Boot application class
                  --project-version=<version>           Generated project version
                  --framework-version=<version>         Framework platform version
                  --spring-boot-version=<version>       Spring Boot version
                  --contract-group=<group>              Server API group override
                  --contract-artifact=<artifact>        Server API artifact override
                  --contract-version=<version>          Server API version override
                  --contract-api-package=<package>      API package override for spec input
                  --contract-repository=<uri-or-path>   Maven repository for contract artifacts
                  --force                               Replace a non-empty output directory
                  --help                                Show this help
                """;
    }

    private record Options(Map<String, String> values, boolean force, boolean help) {

        static Options parse(String[] arguments) {
            Map<String, String> values = new LinkedHashMap<>();
            boolean force = false;
            boolean help = false;
            for (String argument : arguments) {
                if (argument.equals("--force")) {
                    force = true;
                } else if (argument.equals("--help")) {
                    help = true;
                } else if (argument.startsWith("--") && argument.contains("=")) {
                    int separator = argument.indexOf('=');
                    String key = argument.substring(2, separator);
                    String value = argument.substring(separator + 1);
                    if (value.isBlank()) {
                        throw new IllegalArgumentException("--" + key + " must not be blank");
                    }
                    if (values.putIfAbsent(key, value) != null) {
                        throw new IllegalArgumentException("Duplicate option --" + key);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown option " + argument);
                }
            }
            return new Options(Map.copyOf(values), force, help);
        }

        ProjectGenerationRequest request() {
            requireKnownOptions();
            String spec = values.get("spec");
            String apiJar = values.get("api-jar");
            if ((spec == null) == (apiJar == null)) {
                throw new IllegalArgumentException("Provide exactly one of --spec or --api-jar");
            }
            String output = required("output");
            ProjectContractSource source =
                    spec == null
                            ? new ServerApiJarSource(Path.of(apiJar))
                            : new OpenApiDocumentSource(Path.of(spec));
            ProjectGenerationRequest.Builder builder =
                    ProjectGenerationRequest.builder(source, Path.of(output)).overwrite(force);
            set("group", builder::groupId);
            set("artifact", builder::artifactId);
            set("package", builder::basePackage);
            set("application-name", builder::applicationName);
            set("project-version", builder::projectVersion);
            set("framework-version", builder::frameworkVersion);
            set("spring-boot-version", builder::springBootVersion);
            set("contract-group", builder::contractGroupId);
            set("contract-artifact", builder::contractArtifactId);
            set("contract-version", builder::contractVersion);
            set("contract-api-package", builder::contractApiPackage);
            String repository = values.get("contract-repository");
            if (repository != null) {
                builder.contractRepository(repository(repository));
            }
            return builder.build();
        }

        private void requireKnownOptions() {
            var known =
                    java.util.Set.of(
                            "spec",
                            "api-jar",
                            "output",
                            "group",
                            "artifact",
                            "package",
                            "application-name",
                            "project-version",
                            "framework-version",
                            "spring-boot-version",
                            "contract-group",
                            "contract-artifact",
                            "contract-version",
                            "contract-api-package",
                            "contract-repository");
            values.keySet().stream()
                    .filter(key -> !known.contains(key))
                    .findFirst()
                    .ifPresent(
                            key -> {
                                throw new IllegalArgumentException("Unknown option --" + key);
                            });
        }

        private String required(String key) {
            String value = values.get(key);
            if (value == null) {
                throw new IllegalArgumentException("--" + key + " is required");
            }
            return value;
        }

        private void set(String key, java.util.function.Consumer<String> setter) {
            String value = values.get(key);
            if (value != null) {
                setter.accept(value);
            }
        }

        private static URI repository(String value) {
            URI uri = URI.create(value);
            return uri.getScheme() == null
                    ? Path.of(value).toAbsolutePath().normalize().toUri()
                    : uri;
        }
    }
}
