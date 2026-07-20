package com.smbtech.serviceframework.openapi.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Provides open api contract loader behavior. */
public final class OpenApiContractLoader {

    private static final Set<String> HTTP_METHODS =
            Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");

    private final ObjectMapper mapper;

    /** Creates an OpenAPI contract loader instance. */
    public OpenApiContractLoader() {
        this(new ObjectMapper(new YAMLFactory()));
    }

    /**
     * Creates an OpenAPI contract loader instance.
     *
     * @param mapper mapper value
     */
    public OpenApiContractLoader(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    /**
     * Performs the load operation.
     *
     * @param source source value
     * @return load result
     * @throws IOException when the operation cannot be completed
     */
    public OpenApiContract load(Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        try (InputStream input = Files.newInputStream(source)) {
            return load(input, source.toString());
        }
    }

    /**
     * Loads classpath.
     *
     * @param resourceName resource name value
     * @return load classpath result
     * @throws IOException when the operation cannot be completed
     */
    public OpenApiContract loadClasspath(String resourceName) throws IOException {
        String normalized = Objects.requireNonNull(resourceName, "resourceName");
        normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(normalized)) {
            if (input == null) {
                throw new IOException("OpenAPI classpath resource not found: " + resourceName);
            }
            return load(input, "classpath:" + normalized);
        }
    }

    /**
     * Performs the load operation.
     *
     * @param input input value
     * @param description description value
     * @return load result
     * @throws IOException when the operation cannot be completed
     */
    public OpenApiContract load(InputStream input, String description) throws IOException {
        Objects.requireNonNull(input, "input");
        JsonNode root = mapper.readTree(input);
        validateRoot(root, description);
        Map<String, OpenApiRequestDefinition> requestDefinitions = new LinkedHashMap<>();
        List<OpenApiOperation> operations = readOperations(root, description, requestDefinitions);
        return new OpenApiContract(
                root.path("info").path("title").asText(),
                root.path("info").path("version").asText(),
                operations,
                requestDefinitions,
                root);
    }

    private static void validateRoot(JsonNode root, String description) {
        String source =
                description == null || description.isBlank() ? "OpenAPI document" : description;
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException(source + " must contain an object document");
        }
        if (root.path("openapi").asText().isBlank()) {
            throw new IllegalArgumentException(source + " must declare openapi");
        }
        if (root.path("info").path("title").asText().isBlank()) {
            throw new IllegalArgumentException(source + " must declare info.title");
        }
        if (root.path("info").path("version").asText().isBlank()) {
            throw new IllegalArgumentException(source + " must declare info.version");
        }
        if (!root.path("paths").isObject()) {
            throw new IllegalArgumentException(source + " must declare paths");
        }
    }

    private static List<OpenApiOperation> readOperations(
            JsonNode root,
            String description,
            Map<String, OpenApiRequestDefinition> requestDefinitions) {
        List<OpenApiOperation> operations = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> paths = root.path("paths").properties().iterator();
        while (paths.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = paths.next();
            JsonNode pathItem = pathEntry.getValue();
            Iterator<Map.Entry<String, JsonNode>> methods = pathItem.properties().iterator();
            while (methods.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methods.next();
                String method = methodEntry.getKey().toLowerCase(Locale.ROOT);
                if (!HTTP_METHODS.contains(method)) {
                    continue;
                }
                JsonNode operation = methodEntry.getValue();
                String operationId = operation.path("operationId").asText();
                if (operationId.isBlank()) {
                    throw new IllegalArgumentException(
                            sourceName(description)
                                    + " operation "
                                    + method.toUpperCase(Locale.ROOT)
                                    + " "
                                    + pathEntry.getKey()
                                    + " must declare operationId");
                }
                Map<String, OpenApiRequestParameter> parameters =
                        readParameters(
                                root, pathItem.path("parameters"), operation.path("parameters"));
                operations.add(
                        new OpenApiOperation(
                                operationId,
                                method,
                                pathEntry.getKey(),
                                parameters.values().stream()
                                        .filter(
                                                parameter ->
                                                        "path".equals(parameter.location())
                                                                && parameter.required())
                                        .map(OpenApiRequestParameter::name)
                                        .toList(),
                                readResponses(operation.path("responses"), operationId)));
                requestDefinitions.put(
                        operationId,
                        new OpenApiRequestDefinition(
                                parameters, readRequestBody(root, operation.path("requestBody"))));
            }
        }
        return operations;
    }

    private static Map<String, OpenApiRequestParameter> readParameters(
            JsonNode root, JsonNode... parameterGroups) {
        Map<String, OpenApiRequestParameter> parametersByKey = new LinkedHashMap<>();
        for (JsonNode parameters : parameterGroups) {
            if (!parameters.isArray()) {
                continue;
            }
            parameters.forEach(
                    parameter -> {
                        JsonNode resolvedParameter = resolveDocumentReference(root, parameter);
                        String name = resolvedParameter.path("name").asText();
                        String location = resolvedParameter.path("in").asText();
                        if (!name.isBlank()
                                && Set.of("path", "query", "header", "cookie").contains(location)) {
                            JsonNode schema = resolvedParameter.path("schema");
                            parametersByKey.put(
                                    OpenApiRequestDefinition.key(location, name),
                                    new OpenApiRequestParameter(
                                            name,
                                            location,
                                            resolvedParameter.path("required").asBoolean()
                                                    || "path".equals(location),
                                            schema.isMissingNode()
                                                    ? NullNode.getInstance()
                                                    : schema));
                        }
                    });
        }
        return parametersByKey;
    }

    private static OpenApiRequestBody readRequestBody(JsonNode root, JsonNode requestBody) {
        if (!requestBody.isObject()) {
            return null;
        }
        requestBody = resolveDocumentReference(root, requestBody);
        Map<String, JsonNode> schemas = new LinkedHashMap<>();
        requestBody
                .path("content")
                .properties()
                .forEach(
                        content -> {
                            JsonNode schema = content.getValue().path("schema");
                            schemas.put(
                                    content.getKey(),
                                    schema.isMissingNode() ? NullNode.getInstance() : schema);
                        });
        return new OpenApiRequestBody(requestBody.path("required").asBoolean(), schemas);
    }

    private static JsonNode resolveDocumentReference(JsonNode root, JsonNode value) {
        if (!value.hasNonNull("$ref")) {
            return value;
        }
        String reference = value.path("$ref").asText();
        if (!reference.startsWith("#/")) {
            throw new IllegalArgumentException(
                    "external OpenAPI references are not supported: " + reference);
        }
        JsonNode resolved = root.at(reference.substring(1));
        if (resolved.isMissingNode()) {
            throw new IllegalArgumentException("OpenAPI reference does not exist: " + reference);
        }
        return resolved;
    }

    private static Map<Integer, OpenApiResponse> readResponses(
            JsonNode responses, String operationId) {
        if (!responses.isObject()) {
            throw new IllegalArgumentException(
                    "operation " + operationId + " must declare responses");
        }
        Map<Integer, OpenApiResponse> result = new LinkedHashMap<>();
        responses
                .properties()
                .forEach(
                        entry -> {
                            if (!entry.getKey().matches("[1-5][0-9]{2}")) {
                                return;
                            }
                            int status = Integer.parseInt(entry.getKey());
                            Map<String, JsonNode> schemas = new LinkedHashMap<>();
                            entry.getValue()
                                    .path("content")
                                    .properties()
                                    .forEach(
                                            content -> {
                                                JsonNode schema = content.getValue().path("schema");
                                                schemas.put(
                                                        content.getKey(),
                                                        schema.isMissingNode()
                                                                ? NullNode.getInstance()
                                                                : schema);
                                            });
                            result.put(status, new OpenApiResponse(status, schemas));
                        });
        if (result.keySet().stream().noneMatch(status -> status >= 200 && status < 300)) {
            throw new IllegalArgumentException(
                    "operation " + operationId + " must declare a 2xx response");
        }
        return result;
    }

    private static String sourceName(String description) {
        return description == null || description.isBlank() ? "OpenAPI document" : description;
    }
}
