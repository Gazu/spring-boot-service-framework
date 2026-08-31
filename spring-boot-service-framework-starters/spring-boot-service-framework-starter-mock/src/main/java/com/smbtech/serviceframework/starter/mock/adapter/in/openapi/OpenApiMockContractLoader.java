package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.RequestMethod;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

/** Provides open api mock contract loader behavior. */
final class OpenApiMockContractLoader {

    private static final Set<String> HTTP_METHODS =
            Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");

    private static final Pattern SUPPORTED_OPENAPI_VERSION = Pattern.compile("3\\.[01]\\.\\d+");

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final ObjectMapper yamlMapper;

    /**
     * Creates an OpenAPI mock contract loader instance.
     *
     * @param resourceLoader resource loader value
     * @param objectMapper object mapper value
     */
    OpenApiMockContractLoader(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    OpenApiMockContract load(String location, boolean includeOptionalProperties, Duration delay)
            throws IOException {
        String safeLocation = requireText(location, "OpenAPI contract location");
        Resource resource = resourceLoader.getResource(normalizeLocation(safeLocation));
        if (!resource.exists()) {
            throw new IOException("OpenAPI mock contract does not exist: " + safeLocation);
        }
        JsonNode root;
        try (InputStream input = resource.getInputStream()) {
            root = mapperFor(safeLocation).readTree(input);
        }
        validateRoot(root, safeLocation);
        OpenApiExampleGenerator exampleGenerator =
                new OpenApiExampleGenerator(root, includeOptionalProperties);
        List<OpenApiMockOperation> operations = readOperations(root, exampleGenerator, delay);
        return new OpenApiMockContract(
                root.path("info").path("title").asString(),
                root.path("info").path("version").asString(),
                operations);
    }

    private List<OpenApiMockOperation> readOperations(
            JsonNode root, OpenApiExampleGenerator exampleGenerator, Duration delay)
            throws IOException {
        List<OpenApiMockOperation> operations = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> paths = root.path("paths").properties().iterator();
        while (paths.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = paths.next();
            JsonNode pathItem = resolve(root, pathEntry.getValue());
            Iterator<Map.Entry<String, JsonNode>> methods = pathItem.properties().iterator();
            while (methods.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methods.next();
                String method = methodEntry.getKey().toLowerCase(Locale.ROOT);
                if (!HTTP_METHODS.contains(method)) {
                    continue;
                }
                JsonNode operation = methodEntry.getValue();
                Map<Integer, OpenApiMockResponse> responses =
                        readResponses(root, operation.path("responses"), exampleGenerator);
                if (responses.isEmpty()) {
                    throw new IllegalArgumentException(
                            method.toUpperCase(Locale.ROOT)
                                    + " "
                                    + pathEntry.getKey()
                                    + " must declare at least one numeric response");
                }
                int defaultStatus =
                        responses.keySet().stream()
                                .filter(status -> status >= 200 && status < 300)
                                .min(Integer::compareTo)
                                .orElseGet(
                                        () ->
                                                responses.keySet().stream()
                                                        .min(Integer::compareTo)
                                                        .orElseThrow());
                operations.add(
                        new OpenApiMockOperation(
                                operation.path("operationId").asString(method + pathEntry.getKey()),
                                RequestMethod.valueOf(method.toUpperCase(Locale.ROOT)),
                                pathEntry.getKey(),
                                defaultStatus,
                                responses,
                                delay));
            }
        }
        return operations;
    }

    private Map<Integer, OpenApiMockResponse> readResponses(
            JsonNode root, JsonNode rawResponses, OpenApiExampleGenerator exampleGenerator)
            throws IOException {
        Map<Integer, OpenApiMockResponse> responses = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> entries = rawResponses.properties().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, JsonNode> entry = entries.next();
            if (!entry.getKey().matches("[1-5][0-9]{2}")) {
                continue;
            }
            int status = Integer.parseInt(entry.getKey());
            JsonNode response = resolve(root, entry.getValue());
            Content content = readContent(response.path("content"), exampleGenerator);
            responses.put(
                    status,
                    new OpenApiMockResponse(
                            status,
                            content.contentType(),
                            readHeaders(root, response.path("headers"), exampleGenerator),
                            content.body()));
        }
        return responses;
    }

    private Content readContent(JsonNode content, OpenApiExampleGenerator exampleGenerator)
            throws IOException {
        if (!content.isObject() || content.isEmpty()) {
            return new Content("", new byte[0]);
        }
        Map.Entry<String, JsonNode> selected = selectContent(content);
        String contentType = selected.getKey();
        JsonNode definition = selected.getValue();
        JsonNode body = explicitExample(definition);
        if (body == null && definition.has("schema")) {
            body = exampleGenerator.generate(definition.path("schema"));
        }
        if (body == null) {
            return new Content(contentType, new byte[0]);
        }
        if (!isJson(contentType) && body.isString()) {
            return new Content(contentType, body.asString().getBytes(StandardCharsets.UTF_8));
        }
        return new Content(contentType, objectMapper.writeValueAsBytes(body));
    }

    private static JsonNode explicitExample(JsonNode contentDefinition) {
        if (contentDefinition.has("example")) {
            return contentDefinition.get("example");
        }
        Iterator<JsonNode> examples = contentDefinition.path("examples").iterator();
        while (examples.hasNext()) {
            JsonNode example = examples.next();
            if (example.has("value")) {
                return example.get("value");
            }
        }
        return null;
    }

    private static Map.Entry<String, JsonNode> selectContent(JsonNode content) {
        List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
        content.properties().forEach(entries::add);
        return entries.stream()
                .filter(entry -> "application/json".equalsIgnoreCase(entry.getKey()))
                .findFirst()
                .orElseGet(
                        () ->
                                entries.stream()
                                        .filter(
                                                entry ->
                                                        entry.getKey()
                                                                .toLowerCase(Locale.ROOT)
                                                                .endsWith("+json"))
                                        .findFirst()
                                        .orElse(entries.getFirst()));
    }

    private static Map<String, List<String>> readHeaders(
            JsonNode root, JsonNode headers, OpenApiExampleGenerator exampleGenerator) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        headers.properties()
                .forEach(
                        entry -> {
                            JsonNode header = resolve(root, entry.getValue());
                            JsonNode value =
                                    header.has("example")
                                            ? header.get("example")
                                            : exampleGenerator.generate(header.path("schema"));
                            if (value != null && !value.isNull()) {
                                result.put(
                                        entry.getKey(),
                                        List.of(
                                                value.isValueNode()
                                                        ? value.asString()
                                                        : value.toString()));
                            }
                        });
        return result;
    }

    private ObjectMapper mapperFor(String location) {
        String normalized = location.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".json") ? objectMapper : yamlMapper;
    }

    private static JsonNode resolve(JsonNode root, JsonNode value) {
        if (!value.hasNonNull("$ref")) {
            return value;
        }
        String reference = value.path("$ref").asString();
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

    private static void validateRoot(JsonNode root, String location) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException(location + " must contain an OpenAPI object");
        }
        String openApiVersion = root.path("openapi").asString().trim();
        if (!SUPPORTED_OPENAPI_VERSION.matcher(openApiVersion).matches()) {
            throw new IllegalArgumentException(
                    location + " must declare a supported OpenAPI 3.0.x or 3.1.x version");
        }
        if (root.path("info").path("title").asString().isBlank()) {
            throw new IllegalArgumentException(location + " must declare info.title");
        }
        if (root.path("info").path("version").asString().isBlank()) {
            throw new IllegalArgumentException(location + " must declare info.version");
        }
        if (!root.path("paths").isObject()) {
            throw new IllegalArgumentException(location + " must declare paths");
        }
    }

    private static boolean isJson(String contentType) {
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.equals("application/json") || normalized.endsWith("+json");
    }

    private static String normalizeLocation(String location) {
        return location.contains(":") ? location : "classpath:" + location;
    }

    private static String requireText(String value, String name) {
        String safeValue = Objects.requireNonNullElse(value, "").trim();
        if (safeValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return safeValue;
    }

    private record Content(String contentType, byte[] body) {}
}
