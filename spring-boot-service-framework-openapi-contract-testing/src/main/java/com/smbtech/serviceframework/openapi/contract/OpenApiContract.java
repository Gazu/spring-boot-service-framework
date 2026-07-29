package com.smbtech.serviceframework.openapi.contract;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

/** Provides open api contract behavior. */
public final class OpenApiContract {

    private final String title;
    private final String version;
    private final Map<String, OpenApiOperation> operations;
    private final Map<String, OpenApiRequestDefinition> requestDefinitions;
    private final JsonNode root;

    OpenApiContract(
            String title, String version, Collection<OpenApiOperation> operations, JsonNode root) {
        this(title, version, operations, Map.of(), root);
    }

    OpenApiContract(
            String title,
            String version,
            Collection<OpenApiOperation> operations,
            Map<String, OpenApiRequestDefinition> requestDefinitions,
            JsonNode root) {
        this.title = requireText(title, "title");
        this.version = requireText(version, "version");
        this.root = Objects.requireNonNull(root, "root");
        Map<String, OpenApiOperation> indexed = new LinkedHashMap<>();
        Objects.requireNonNull(operations, "operations")
                .forEach(
                        operation -> {
                            OpenApiOperation previous =
                                    indexed.put(operation.operationId(), operation);
                            if (previous != null) {
                                throw new IllegalArgumentException(
                                        "duplicate operationId " + operation.operationId());
                            }
                        });
        this.operations = Map.copyOf(indexed);
        this.requestDefinitions =
                Map.copyOf(Objects.requireNonNull(requestDefinitions, "requestDefinitions"));
    }

    /**
     * Performs the title operation.
     *
     * @return title result
     */
    public String title() {
        return title;
    }

    /**
     * Performs the version operation.
     *
     * @return version result
     */
    public String version() {
        return version;
    }

    /**
     * Performs the operations operation.
     *
     * @return operations result
     */
    public Collection<OpenApiOperation> operations() {
        return operations.values();
    }

    /**
     * Finds operation.
     *
     * @param operationId operation id value
     * @return find result
     */
    public Optional<OpenApiOperation> findOperation(String operationId) {
        return Optional.ofNullable(operations.get(operationId));
    }

    OpenApiRequestDefinition requestDefinition(String operationId) {
        return requestDefinitions.getOrDefault(operationId, OpenApiRequestDefinition.EMPTY);
    }

    JsonNode resolveSchema(JsonNode schema) {
        if (schema == null || !schema.hasNonNull("$ref")) {
            return schema;
        }
        String reference = schema.path("$ref").asString();
        if (!reference.startsWith("#/")) {
            throw new IllegalArgumentException(
                    "external schema references are not supported: " + reference);
        }
        JsonNode resolved = root.at(reference.substring(1));
        if (resolved.isMissingNode()) {
            throw new IllegalArgumentException("schema reference does not exist: " + reference);
        }
        return resolved;
    }

    private static String requireText(String value, String name) {
        String safeValue = Objects.requireNonNull(value, name).trim();
        if (safeValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return safeValue;
    }
}
