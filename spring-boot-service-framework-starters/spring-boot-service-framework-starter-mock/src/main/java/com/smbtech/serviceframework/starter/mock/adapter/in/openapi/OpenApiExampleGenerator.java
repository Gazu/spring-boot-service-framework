package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

final class OpenApiExampleGenerator {

    private final JsonNode root;
    private final boolean includeOptionalProperties;

    OpenApiExampleGenerator(JsonNode root, boolean includeOptionalProperties) {
        this.root = root;
        this.includeOptionalProperties = includeOptionalProperties;
    }

    JsonNode generate(JsonNode schema) {
        return generate(schema, new HashSet<>());
    }

    private JsonNode generate(JsonNode rawSchema, Set<String> references) {
        JsonNode schema = rawSchema;
        if (schema == null || schema.isMissingNode() || schema.isNull()) {
            return JsonNodeFactory.instance.nullNode();
        }
        if (schema.hasNonNull("$ref")) {
            String reference = schema.path("$ref").asString();
            if (!references.add(reference)) {
                return JsonNodeFactory.instance.nullNode();
            }
            JsonNode generated = generate(resolve(reference), references);
            references.remove(reference);
            return generated;
        }
        if (schema.has("example")) {
            return schema.get("example").deepCopy();
        }
        if (schema.has("default")) {
            return schema.get("default").deepCopy();
        }
        if (schema.path("enum").isArray() && !schema.path("enum").isEmpty()) {
            return schema.path("enum").get(0).deepCopy();
        }
        if (schema.path("allOf").isArray()) {
            ObjectNode merged = JsonNodeFactory.instance.objectNode();
            for (JsonNode member : schema.path("allOf")) {
                JsonNode generated = generate(member, references);
                if (generated.isObject()) {
                    merged.setAll((ObjectNode) generated);
                }
            }
            return merged;
        }
        JsonNode alternatives =
                schema.path("oneOf").isArray() ? schema.path("oneOf") : schema.path("anyOf");
        if (alternatives.isArray() && !alternatives.isEmpty()) {
            return generate(alternatives.get(0), references);
        }

        String type = schema.path("type").asString();
        if (type.isBlank() && schema.path("properties").isObject()) {
            type = "object";
        }
        return switch (type) {
            case "object" -> object(schema, references);
            case "array" -> array(schema, references);
            case "integer" -> integer(schema);
            case "number" -> number(schema);
            case "boolean" -> JsonNodeFactory.instance.booleanNode(true);
            case "string" -> string(schema);
            default -> JsonNodeFactory.instance.nullNode();
        };
    }

    private ObjectNode object(JsonNode schema, Set<String> references) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        Set<String> required = new HashSet<>();
        schema.path("required").forEach(value -> required.add(value.asString()));
        Iterator<Map.Entry<String, JsonNode>> properties =
                schema.path("properties").properties().iterator();
        while (properties.hasNext()) {
            Map.Entry<String, JsonNode> property = properties.next();
            if (property.getValue().path("writeOnly").asBoolean()) {
                continue;
            }
            if (includeOptionalProperties || required.contains(property.getKey())) {
                result.set(property.getKey(), generate(property.getValue(), references));
            }
        }
        return result;
    }

    private ArrayNode array(JsonNode schema, Set<String> references) {
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        int count = Math.max(1, schema.path("minItems").asInt(1));
        if (schema.has("maxItems")) {
            count = Math.min(count, schema.path("maxItems").asInt());
        }
        for (int index = 0; index < Math.min(count, 3); index++) {
            result.add(generate(schema.path("items"), references));
        }
        return result;
    }

    private JsonNode integer(JsonNode schema) {
        long value = schema.has("minimum") ? schema.path("minimum").asLong() : 0L;
        return JsonNodeFactory.instance.numberNode(value);
    }

    private JsonNode number(JsonNode schema) {
        BigDecimal value =
                schema.has("minimum") ? schema.path("minimum").decimalValue() : BigDecimal.ZERO;
        return JsonNodeFactory.instance.numberNode(value);
    }

    private JsonNode string(JsonNode schema) {
        String value =
                switch (schema.path("format").asString()) {
                    case "date" -> "2000-01-01";
                    case "date-time" -> "2000-01-01T00:00:00Z";
                    case "uuid" -> "00000000-0000-0000-0000-000000000000";
                    case "email" -> "mock@example.test";
                    case "uri", "url" -> "https://example.test/mock";
                    default -> "string";
                };
        value = matchingPatternValue(schema.path("pattern").asString(), value);
        int minimumLength = schema.path("minLength").asInt(0);
        if (value.length() < minimumLength) {
            value = value + "x".repeat(minimumLength - value.length());
        }
        int maximumLength = schema.path("maxLength").asInt(Integer.MAX_VALUE);
        if (value.length() > maximumLength) {
            value = value.substring(0, maximumLength);
        }
        return JsonNodeFactory.instance.stringNode(value);
    }

    private static String matchingPatternValue(String expression, String fallback) {
        if (expression.isBlank()) {
            return fallback;
        }
        try {
            Pattern pattern = Pattern.compile(expression);
            for (String candidate :
                    new String[] {fallback, "mock-001", "MOCK-001", "100", "mock"}) {
                if (pattern.matcher(candidate).matches()) {
                    return candidate;
                }
            }
            return fallback;
        } catch (PatternSyntaxException exception) {
            return fallback;
        }
    }

    private JsonNode resolve(String reference) {
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
}
