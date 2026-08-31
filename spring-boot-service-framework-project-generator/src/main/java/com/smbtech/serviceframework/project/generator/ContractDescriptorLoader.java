package com.smbtech.serviceframework.project.generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.openapitools.codegen.languages.SpringCodegen;

final class ContractDescriptorLoader {

    private static final String METADATA = "META-INF/smbtech/openapi/contract.properties";
    private static final String CONTRACT = "META-INF/smbtech/openapi/contract.yaml";
    private static final Pattern OPENAPI_VERSION = Pattern.compile("3\\.[01]\\.\\d+");
    private static final Pattern SEMVER =
            Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][0-9A-Za-z][0-9A-Za-z.-]*)?");

    ContractDescriptor load(ProjectGenerationRequest request) {
        Path source = request.contractSource().path().toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            throw new ProjectGenerationException("Contract source does not exist: " + source);
        }
        try {
            if (request.contractSource() instanceof OpenApiDocumentSource) {
                return fromDocument(source, request);
            }
            if (request.contractSource() instanceof ServerApiJarSource) {
                return fromJar(source, request);
            }
            throw new ProjectGenerationException(
                    "Unsupported contract source: "
                            + request.contractSource().getClass().getName());
        } catch (IOException exception) {
            throw new ProjectGenerationException(
                    "Cannot read contract source " + source, exception);
        }
    }

    private ContractDescriptor fromDocument(Path source, ProjectGenerationRequest request)
            throws IOException {
        OpenAPI contract = parse(source);
        String title = contract.getInfo().getTitle().trim();
        String id = NameSupport.normalizeArtifact(title);
        NameSupport.requireArtifact(id, "info.title");
        String version = first(request.contractVersion(), contract.getInfo().getVersion().trim());
        requireVersion(version);
        String group = first(request.contractGroupId(), "com.smbtech.contracts");
        String artifact = first(request.contractArtifactId(), id + "-server-api");
        String apiPackage =
                first(
                        request.contractApiPackage(),
                        "com.smbtech.contracts." + NameSupport.compact(id) + ".api");
        NameSupport.requirePackage(apiPackage, "contractApiPackage");
        return new ContractDescriptor(
                title,
                id,
                version,
                group,
                artifact,
                Files.readAllBytes(source),
                delegateTypes(contract, apiPackage));
    }

    private ContractDescriptor fromJar(Path source, ProjectGenerationRequest request)
            throws IOException {
        try (JarFile jar = new JarFile(source.toFile())) {
            JarEntry metadataEntry = jar.getJarEntry(METADATA);
            JarEntry contractEntry = jar.getJarEntry(CONTRACT);
            if (metadataEntry == null || contractEntry == null) {
                throw new ProjectGenerationException(
                        source
                                + " is not a framework server API JAR: embedded contract metadata is missing");
            }
            Properties metadata = properties(jar, metadataEntry);
            String kind = metadata.getProperty("artifact.kind", "").trim();
            if (!kind.equals("server-api") && !kind.equals("api")) {
                throw new ProjectGenerationException(
                        source + " must have artifact.kind=server-api, found '" + kind + "'");
            }
            List<String> delegates =
                    jar.stream()
                            .map(JarEntry::getName)
                            .filter(name -> name.endsWith("ApiDelegate.class"))
                            .filter(name -> !name.contains("$"))
                            .map(name -> name.substring(0, name.length() - ".class".length()))
                            .map(name -> name.replace('/', '.'))
                            .sorted()
                            .toList();
            if (delegates.isEmpty()) {
                throw new ProjectGenerationException(
                        source + " contains no ApiDelegate interfaces");
            }
            String title = required(metadata, "contract.title", source);
            String id = required(metadata, "contract.id", source);
            String version =
                    first(
                            request.contractVersion(),
                            required(metadata, "contract.version", source));
            requireVersion(version);
            String group =
                    first(request.contractGroupId(), required(metadata, "artifact.group", source));
            String artifact =
                    first(request.contractArtifactId(), required(metadata, "artifact.id", source));
            if (request.contractApiPackage() != null) {
                throw new ProjectGenerationException(
                        "contractApiPackage cannot override classes embedded in a server API JAR");
            }
            try (InputStream input = jar.getInputStream(contractEntry)) {
                return new ContractDescriptor(
                        title, id, version, group, artifact, input.readAllBytes(), delegates);
            }
        }
    }

    private static OpenAPI parse(Path source) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(false);
        options.setResolveCombinators(false);
        SwaggerParseResult result =
                new OpenAPIV3Parser().readLocation(source.toUri().toString(), null, options);
        List<String> failures = new ArrayList<>();
        if (result.getMessages() != null) {
            result.getMessages().stream()
                    .filter(message -> message != null && !message.isBlank())
                    .forEach(failures::add);
        }
        OpenAPI contract = result.getOpenAPI();
        if (contract == null) {
            failures.add("document could not be parsed as OpenAPI");
        } else {
            if (contract.getOpenapi() == null
                    || !OPENAPI_VERSION.matcher(contract.getOpenapi()).matches()) {
                failures.add("openapi must declare a supported 3.0.x or 3.1.x version");
            }
            if (contract.getInfo() == null
                    || blank(contract.getInfo().getTitle())
                    || blank(contract.getInfo().getVersion())) {
                failures.add("info.title and info.version are required");
            }
            if (contract.getPaths() == null || contract.getPaths().isEmpty()) {
                failures.add("paths must contain at least one operation");
            }
        }
        if (!failures.isEmpty()) {
            throw new ProjectGenerationException(
                    source + ": " + String.join("; ", failures.stream().distinct().toList()));
        }
        return contract;
    }

    private static List<String> delegateTypes(OpenAPI contract, String apiPackage) {
        TreeSet<String> groups = new TreeSet<>();
        contract.getPaths().values().stream()
                .flatMap(path -> path.readOperations().stream())
                .map(Operation::getTags)
                .forEach(
                        tags -> {
                            if (tags == null || tags.isEmpty()) {
                                groups.add("default");
                            } else {
                                groups.addAll(tags);
                            }
                        });
        SpringCodegen codegen = new SpringCodegen();
        return groups.stream()
                .map(codegen::toApiName)
                .map(name -> apiPackage + "." + name + "Delegate")
                .toList();
    }

    private static Properties properties(JarFile jar, JarEntry entry) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = jar.getInputStream(entry)) {
            properties.load(input);
        }
        return properties;
    }

    private static String required(Properties properties, String key, Path source) {
        String value = properties.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new ProjectGenerationException(
                    source + ": metadata property " + key + " is required");
        }
        return value;
    }

    private static String first(String preferred, String fallback) {
        return preferred == null ? fallback : preferred;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void requireVersion(String version) {
        if (!SEMVER.matcher(version).matches()) {
            throw new ProjectGenerationException(
                    "Contract version must be SemVer/Maven compatible, found '" + version + "'");
        }
    }
}
