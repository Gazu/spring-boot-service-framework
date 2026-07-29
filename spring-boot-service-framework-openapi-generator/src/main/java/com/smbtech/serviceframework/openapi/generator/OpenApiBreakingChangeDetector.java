package com.smbtech.serviceframework.openapi.generator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

/** Provides open api breaking change detector behavior. */
public final class OpenApiBreakingChangeDetector {

    private static final Set<String> HTTP_METHODS =
            Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");

    private final ObjectMapper mapper;
    private final OpenApiSpecReader specReader;

    /** Creates an OpenAPI breaking change detector instance. */
    public OpenApiBreakingChangeDetector() {
        this(new ObjectMapper(new YAMLFactory()), new OpenApiSpecReader());
    }

    /**
     * Creates an OpenAPI breaking change detector instance.
     *
     * @param mapper mapper value
     * @param specReader spec reader value
     */
    public OpenApiBreakingChangeDetector(ObjectMapper mapper, OpenApiSpecReader specReader) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.specReader = Objects.requireNonNull(specReader, "specReader");
    }

    /**
     * Performs the compare operation.
     *
     * @param baselineSource baseline source value
     * @param currentSource current source value
     * @return compare result
     * @throws IOException when the operation cannot be completed
     */
    public OpenApiCompatibilityReport compare(Path baselineSource, Path currentSource)
            throws IOException {
        OpenApiSpecInfo baselineInfo = specReader.read(baselineSource);
        OpenApiSpecInfo currentInfo = specReader.read(currentSource);
        if (!baselineInfo.artifactBaseName().equals(currentInfo.artifactBaseName())) {
            throw new IllegalArgumentException(
                    "cannot compare different OpenAPI contracts: "
                            + baselineInfo.title()
                            + " and "
                            + currentInfo.title());
        }

        JsonNode baseline = mapper.readTree(baselineSource.toFile());
        JsonNode current = mapper.readTree(currentSource.toFile());
        List<OpenApiChange> changes = new ArrayList<>();
        comparePaths(baseline.path("paths"), current.path("paths"), changes);
        compareSchemas(
                baseline.path("components").path("schemas"),
                current.path("components").path("schemas"),
                changes);
        return OpenApiCompatibilityReport.create(baselineInfo, currentInfo, changes);
    }

    private void comparePaths(
            JsonNode baselinePaths, JsonNode currentPaths, List<OpenApiChange> changes) {
        Map<String, JsonNode> previous = properties(baselinePaths);
        Map<String, JsonNode> next = properties(currentPaths);
        for (Map.Entry<String, JsonNode> path : previous.entrySet()) {
            for (String method : HTTP_METHODS) {
                JsonNode previousOperation = path.getValue().get(method);
                JsonNode currentOperation =
                        next.containsKey(path.getKey())
                                ? next.get(path.getKey()).get(method)
                                : null;
                String location = method.toUpperCase(java.util.Locale.ROOT) + " " + path.getKey();
                if (previousOperation != null && currentOperation == null) {
                    add(
                            changes,
                            OpenApiChangeSeverity.BREAKING,
                            OpenApiChangeCode.OPERATION_REMOVED,
                            location,
                            "operation was removed");
                } else if (previousOperation != null) {
                    compareOperation(
                            location,
                            path.getValue(),
                            next.get(path.getKey()),
                            previousOperation,
                            currentOperation,
                            changes);
                }
            }
        }
        for (Map.Entry<String, JsonNode> path : next.entrySet()) {
            for (String method : HTTP_METHODS) {
                if (path.getValue().has(method)
                        && (!previous.containsKey(path.getKey())
                                || !previous.get(path.getKey()).has(method))) {
                    add(
                            changes,
                            OpenApiChangeSeverity.NON_BREAKING,
                            OpenApiChangeCode.OPERATION_ADDED,
                            method.toUpperCase(java.util.Locale.ROOT) + " " + path.getKey(),
                            "operation was added");
                }
            }
        }
    }

    private void compareOperation(
            String location,
            JsonNode previousPath,
            JsonNode currentPath,
            JsonNode previous,
            JsonNode current,
            List<OpenApiChange> changes) {
        String previousOperationId = previous.path("operationId").asString();
        String currentOperationId = current.path("operationId").asString();
        if (!previousOperationId.equals(currentOperationId)) {
            add(
                    changes,
                    OpenApiChangeSeverity.BREAKING,
                    OpenApiChangeCode.OPERATION_ID_CHANGED,
                    location + ".operationId",
                    "operationId changed from "
                            + previousOperationId
                            + " to "
                            + currentOperationId);
        }
        compareParameters(
                location,
                mergeParameters(previousPath.path("parameters"), previous.path("parameters")),
                mergeParameters(currentPath.path("parameters"), current.path("parameters")),
                changes);
        compareRequestBody(
                location, previous.get("requestBody"), current.get("requestBody"), changes);
        compareResponses(location, previous.path("responses"), current.path("responses"), changes);
    }

    private void compareParameters(
            String operationLocation,
            Map<String, JsonNode> previous,
            Map<String, JsonNode> current,
            List<OpenApiChange> changes) {
        for (Map.Entry<String, JsonNode> parameter : previous.entrySet()) {
            String location = operationLocation + ".parameters." + parameter.getKey();
            JsonNode next = current.get(parameter.getKey());
            if (next == null) {
                add(
                        changes,
                        OpenApiChangeSeverity.BREAKING,
                        OpenApiChangeCode.PARAMETER_REMOVED,
                        location,
                        "parameter was removed");
                continue;
            }
            boolean wasRequired = parameter.getValue().path("required").asBoolean(false);
            boolean isRequired = next.path("required").asBoolean(false);
            if (!wasRequired && isRequired) {
                add(
                        changes,
                        OpenApiChangeSeverity.BREAKING,
                        OpenApiChangeCode.PARAMETER_BECAME_REQUIRED,
                        location,
                        "parameter became required");
            } else if (wasRequired && !isRequired) {
                add(
                        changes,
                        OpenApiChangeSeverity.NON_BREAKING,
                        OpenApiChangeCode.PARAMETER_BECAME_OPTIONAL,
                        location,
                        "parameter became optional");
            }
            compareSchema(
                    location + ".schema",
                    parameter.getValue().path("schema"),
                    next.path("schema"),
                    changes);
        }
        current.forEach(
                (key, value) -> {
                    if (!previous.containsKey(key)) {
                        boolean required = value.path("required").asBoolean(false);
                        add(
                                changes,
                                required
                                        ? OpenApiChangeSeverity.BREAKING
                                        : OpenApiChangeSeverity.NON_BREAKING,
                                OpenApiChangeCode.PARAMETER_ADDED,
                                operationLocation + ".parameters." + key,
                                required
                                        ? "required parameter was added"
                                        : "optional parameter was added");
                    }
                });
    }

    private void compareRequestBody(
            String operationLocation,
            JsonNode previous,
            JsonNode current,
            List<OpenApiChange> changes) {
        String location = operationLocation + ".requestBody";
        if (previous != null && current == null) {
            add(
                    changes,
                    OpenApiChangeSeverity.BREAKING,
                    OpenApiChangeCode.REQUEST_BODY_REMOVED,
                    location,
                    "request body was removed");
            return;
        }
        if (previous == null && current != null) {
            boolean required = current.path("required").asBoolean(false);
            add(
                    changes,
                    required ? OpenApiChangeSeverity.BREAKING : OpenApiChangeSeverity.NON_BREAKING,
                    OpenApiChangeCode.REQUEST_BODY_ADDED,
                    location,
                    required
                            ? "required request body was added"
                            : "optional request body was added");
            return;
        }
        if (previous == null) {
            return;
        }
        if (!previous.path("required").asBoolean(false)
                && current.path("required").asBoolean(false)) {
            add(
                    changes,
                    OpenApiChangeSeverity.BREAKING,
                    OpenApiChangeCode.REQUEST_BODY_BECAME_REQUIRED,
                    location,
                    "request body became required");
        } else if (previous.path("required").asBoolean(false)
                && !current.path("required").asBoolean(false)) {
            add(
                    changes,
                    OpenApiChangeSeverity.NON_BREAKING,
                    OpenApiChangeCode.REQUEST_BODY_BECAME_OPTIONAL,
                    location,
                    "request body became optional");
        }
        compareContent(
                location + ".content", previous.path("content"), current.path("content"), changes);
    }

    private void compareResponses(
            String operationLocation,
            JsonNode previous,
            JsonNode current,
            List<OpenApiChange> changes) {
        Map<String, JsonNode> previousResponses = properties(previous);
        Map<String, JsonNode> currentResponses = properties(current);
        previousResponses.forEach(
                (status, response) -> {
                    String location = operationLocation + ".responses." + status;
                    JsonNode next = currentResponses.get(status);
                    if (next == null) {
                        add(
                                changes,
                                OpenApiChangeSeverity.BREAKING,
                                OpenApiChangeCode.RESPONSE_REMOVED,
                                location,
                                "response status was removed");
                    } else {
                        compareContent(
                                location + ".content",
                                response.path("content"),
                                next.path("content"),
                                changes);
                    }
                });
        currentResponses.forEach(
                (status, response) -> {
                    if (!previousResponses.containsKey(status)) {
                        add(
                                changes,
                                OpenApiChangeSeverity.NON_BREAKING,
                                OpenApiChangeCode.RESPONSE_ADDED,
                                operationLocation + ".responses." + status,
                                "response status was added");
                    }
                });
    }

    private void compareContent(
            String location, JsonNode previous, JsonNode current, List<OpenApiChange> changes) {
        Map<String, JsonNode> previousContent = properties(previous);
        Map<String, JsonNode> currentContent = properties(current);
        previousContent.forEach(
                (mediaType, content) -> {
                    JsonNode next = currentContent.get(mediaType);
                    if (next == null) {
                        add(
                                changes,
                                OpenApiChangeSeverity.BREAKING,
                                OpenApiChangeCode.MEDIA_TYPE_REMOVED,
                                location + "." + mediaType,
                                "media type was removed");
                    } else {
                        compareSchema(
                                location + "." + mediaType + ".schema",
                                content.path("schema"),
                                next.path("schema"),
                                changes);
                    }
                });
        currentContent.forEach(
                (mediaType, content) -> {
                    if (!previousContent.containsKey(mediaType)) {
                        add(
                                changes,
                                OpenApiChangeSeverity.NON_BREAKING,
                                OpenApiChangeCode.MEDIA_TYPE_ADDED,
                                location + "." + mediaType,
                                "media type was added");
                    }
                });
    }

    private void compareSchemas(JsonNode previous, JsonNode current, List<OpenApiChange> changes) {
        Map<String, JsonNode> previousSchemas = properties(previous);
        Map<String, JsonNode> currentSchemas = properties(current);
        previousSchemas.forEach(
                (name, schema) -> {
                    JsonNode next = currentSchemas.get(name);
                    if (next == null) {
                        add(
                                changes,
                                OpenApiChangeSeverity.BREAKING,
                                OpenApiChangeCode.SCHEMA_REMOVED,
                                "components.schemas." + name,
                                "schema was removed");
                    } else {
                        compareSchema("components.schemas." + name, schema, next, changes);
                    }
                });
        currentSchemas.forEach(
                (name, schema) -> {
                    if (!previousSchemas.containsKey(name)) {
                        add(
                                changes,
                                OpenApiChangeSeverity.NON_BREAKING,
                                OpenApiChangeCode.SCHEMA_ADDED,
                                "components.schemas." + name,
                                "schema was added");
                    }
                });
    }

    private void compareSchema(
            String location, JsonNode previous, JsonNode current, List<OpenApiChange> changes) {
        if (previous == null
                || previous.isMissingNode()
                || current == null
                || current.isMissingNode()) {
            return;
        }
        String previousReference = previous.path("$ref").asString();
        String currentReference = current.path("$ref").asString();
        if (!previousReference.equals(currentReference)) {
            add(
                    changes,
                    OpenApiChangeSeverity.BREAKING,
                    OpenApiChangeCode.REFERENCE_CHANGED,
                    location,
                    "schema reference changed from "
                            + previousReference
                            + " to "
                            + currentReference);
            return;
        }
        if (!previousReference.isEmpty()) {
            return;
        }

        compareTextValue(
                location, "type", OpenApiChangeCode.TYPE_CHANGED, previous, current, changes);
        compareTextValue(
                location, "format", OpenApiChangeCode.FORMAT_CHANGED, previous, current, changes);
        compareNullability(location, previous, current, changes);
        compareEnum(location, previous.path("enum"), current.path("enum"), changes);
        compareConstraints(location, previous, current, changes);
        compareComposition(location, previous, current, changes);

        if ("object".equals(previous.path("type").asString())
                || previous.has("properties")
                || current.has("properties")) {
            compareObjectProperties(location, previous, current, changes);
        }
        if ("array".equals(previous.path("type").asString())
                || previous.has("items")
                || current.has("items")) {
            compareSchema(
                    location + ".items", previous.path("items"), current.path("items"), changes);
        }
        if (previous.path("additionalProperties").isObject()
                && current.path("additionalProperties").isObject()) {
            compareSchema(
                    location + ".additionalProperties",
                    previous.path("additionalProperties"),
                    current.path("additionalProperties"),
                    changes);
        } else if (!previous.path("additionalProperties")
                .equals(current.path("additionalProperties"))) {
            boolean tightened =
                    !previous.path("additionalProperties").isBoolean()
                            || previous.path("additionalProperties").asBoolean(true)
                                    && !current.path("additionalProperties").asBoolean(true);
            add(
                    changes,
                    tightened ? OpenApiChangeSeverity.BREAKING : OpenApiChangeSeverity.NON_BREAKING,
                    tightened
                            ? OpenApiChangeCode.CONSTRAINT_TIGHTENED
                            : OpenApiChangeCode.CONSTRAINT_RELAXED,
                    location + ".additionalProperties",
                    "additionalProperties changed");
        }
    }

    private void compareObjectProperties(
            String location, JsonNode previous, JsonNode current, List<OpenApiChange> changes) {
        Set<String> previousRequired = textSet(previous.path("required"));
        Set<String> currentRequired = textSet(current.path("required"));
        Map<String, JsonNode> previousProperties = properties(previous.path("properties"));
        Map<String, JsonNode> currentProperties = properties(current.path("properties"));

        previousProperties.forEach(
                (name, schema) -> {
                    String propertyLocation = location + ".properties." + name;
                    JsonNode next = currentProperties.get(name);
                    if (next == null) {
                        add(
                                changes,
                                OpenApiChangeSeverity.BREAKING,
                                OpenApiChangeCode.PROPERTY_REMOVED,
                                propertyLocation,
                                "property was removed");
                        return;
                    }
                    if (!previousRequired.contains(name) && currentRequired.contains(name)) {
                        add(
                                changes,
                                OpenApiChangeSeverity.BREAKING,
                                OpenApiChangeCode.PROPERTY_BECAME_REQUIRED,
                                propertyLocation,
                                "property became required");
                    } else if (previousRequired.contains(name) && !currentRequired.contains(name)) {
                        add(
                                changes,
                                OpenApiChangeSeverity.NON_BREAKING,
                                OpenApiChangeCode.PROPERTY_BECAME_OPTIONAL,
                                propertyLocation,
                                "property became optional");
                    }
                    compareSchema(propertyLocation, schema, next, changes);
                });
        currentProperties.forEach(
                (name, schema) -> {
                    if (!previousProperties.containsKey(name)) {
                        boolean required = currentRequired.contains(name);
                        add(
                                changes,
                                required
                                        ? OpenApiChangeSeverity.BREAKING
                                        : OpenApiChangeSeverity.NON_BREAKING,
                                OpenApiChangeCode.PROPERTY_ADDED,
                                location + ".properties." + name,
                                required
                                        ? "required property was added"
                                        : "optional property was added");
                    }
                });
    }

    private void compareTextValue(
            String location,
            String property,
            OpenApiChangeCode code,
            JsonNode previous,
            JsonNode current,
            List<OpenApiChange> changes) {
        String before = previous.path(property).asString();
        String after = current.path(property).asString();
        if (!before.equals(after)) {
            add(
                    changes,
                    OpenApiChangeSeverity.BREAKING,
                    code,
                    location + "." + property,
                    property + " changed from " + before + " to " + after);
        }
    }

    private void compareNullability(
            String location, JsonNode previous, JsonNode current, List<OpenApiChange> changes) {
        boolean before = previous.path("nullable").asBoolean(false);
        boolean after = current.path("nullable").asBoolean(false);
        if (before != after) {
            boolean tightened = before;
            add(
                    changes,
                    tightened ? OpenApiChangeSeverity.BREAKING : OpenApiChangeSeverity.NON_BREAKING,
                    tightened
                            ? OpenApiChangeCode.CONSTRAINT_TIGHTENED
                            : OpenApiChangeCode.CONSTRAINT_RELAXED,
                    location + ".nullable",
                    tightened
                            ? "null values are no longer accepted"
                            : "null values are now accepted");
        }
    }

    private void compareEnum(
            String location, JsonNode previous, JsonNode current, List<OpenApiChange> changes) {
        Set<String> before = jsonValueSet(previous);
        Set<String> after = jsonValueSet(current);
        before.stream()
                .filter(value -> !after.contains(value))
                .forEach(
                        value ->
                                add(
                                        changes,
                                        OpenApiChangeSeverity.BREAKING,
                                        OpenApiChangeCode.ENUM_VALUE_REMOVED,
                                        location + ".enum",
                                        "enum value was removed: " + value));
        after.stream()
                .filter(value -> !before.contains(value))
                .forEach(
                        value ->
                                add(
                                        changes,
                                        OpenApiChangeSeverity.NON_BREAKING,
                                        OpenApiChangeCode.ENUM_VALUE_ADDED,
                                        location + ".enum",
                                        "enum value was added: " + value));
    }

    private void compareConstraints(
            String location, JsonNode previous, JsonNode current, List<OpenApiChange> changes) {
        compareMinimum(location, "minimum", previous, current, changes);
        compareMinimum(location, "minLength", previous, current, changes);
        compareMinimum(location, "minItems", previous, current, changes);
        compareMaximum(location, "maximum", previous, current, changes);
        compareMaximum(location, "maxLength", previous, current, changes);
        compareMaximum(location, "maxItems", previous, current, changes);

        JsonNode beforePattern = previous.get("pattern");
        JsonNode afterPattern = current.get("pattern");
        if (!Objects.equals(beforePattern, afterPattern)) {
            boolean tightened = afterPattern != null;
            add(
                    changes,
                    tightened ? OpenApiChangeSeverity.BREAKING : OpenApiChangeSeverity.NON_BREAKING,
                    tightened
                            ? OpenApiChangeCode.CONSTRAINT_TIGHTENED
                            : OpenApiChangeCode.CONSTRAINT_RELAXED,
                    location + ".pattern",
                    tightened
                            ? "pattern constraint was added or changed"
                            : "pattern constraint was removed");
        }
    }

    private void compareMinimum(
            String location,
            String name,
            JsonNode previous,
            JsonNode current,
            List<OpenApiChange> changes) {
        JsonNode before = previous.get(name);
        JsonNode after = current.get(name);
        if (Objects.equals(before, after)) {
            return;
        }
        boolean tightened =
                after != null
                        && (before == null
                                || after.decimalValue().compareTo(before.decimalValue()) > 0);
        addConstraintChange(location, name, tightened, changes);
    }

    private void compareMaximum(
            String location,
            String name,
            JsonNode previous,
            JsonNode current,
            List<OpenApiChange> changes) {
        JsonNode before = previous.get(name);
        JsonNode after = current.get(name);
        if (Objects.equals(before, after)) {
            return;
        }
        boolean tightened =
                after != null
                        && (before == null
                                || after.decimalValue().compareTo(before.decimalValue()) < 0);
        addConstraintChange(location, name, tightened, changes);
    }

    private void addConstraintChange(
            String location, String name, boolean tightened, List<OpenApiChange> changes) {
        add(
                changes,
                tightened ? OpenApiChangeSeverity.BREAKING : OpenApiChangeSeverity.NON_BREAKING,
                tightened
                        ? OpenApiChangeCode.CONSTRAINT_TIGHTENED
                        : OpenApiChangeCode.CONSTRAINT_RELAXED,
                location + "." + name,
                name + (tightened ? " constraint was tightened" : " constraint was relaxed"));
    }

    private void compareComposition(
            String location, JsonNode previous, JsonNode current, List<OpenApiChange> changes) {
        for (String keyword : List.of("oneOf", "anyOf", "allOf", "not", "discriminator")) {
            if (!previous.path(keyword).equals(current.path(keyword))) {
                add(
                        changes,
                        OpenApiChangeSeverity.BREAKING,
                        OpenApiChangeCode.COMPOSITION_CHANGED,
                        location + "." + keyword,
                        keyword + " definition changed");
            }
        }
    }

    private static Map<String, JsonNode> mergeParameters(
            JsonNode pathParameters, JsonNode operationParameters) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        addParameters(result, pathParameters);
        addParameters(result, operationParameters);
        return result;
    }

    private static void addParameters(Map<String, JsonNode> target, JsonNode parameters) {
        if (!parameters.isArray()) {
            return;
        }
        parameters.forEach(
                parameter -> {
                    String key =
                            parameter.path("in").asString()
                                    + ":"
                                    + parameter.path("name").asString();
                    target.put(key, parameter);
                });
    }

    private static Map<String, JsonNode> properties(JsonNode node) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.properties().forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private static Set<String> textSet(JsonNode node) {
        Set<String> values = new LinkedHashSet<>();
        if (node != null && node.isArray()) {
            node.forEach(value -> values.add(value.asString()));
        }
        return values;
    }

    private static Set<String> jsonValueSet(JsonNode node) {
        Set<String> values = new LinkedHashSet<>();
        if (node != null && node.isArray()) {
            node.forEach(value -> values.add(value.toString()));
        }
        return values;
    }

    private static void add(
            List<OpenApiChange> changes,
            OpenApiChangeSeverity severity,
            OpenApiChangeCode code,
            String location,
            String message) {
        changes.add(new OpenApiChange(severity, code, location, message));
    }
}
