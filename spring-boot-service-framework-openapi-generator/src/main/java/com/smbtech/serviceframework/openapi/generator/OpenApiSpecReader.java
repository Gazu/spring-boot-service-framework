package com.smbtech.serviceframework.openapi.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

/** Reads and validates the identity of OpenAPI 3.0 and 3.1 documents. */
public final class OpenApiSpecReader {

    private static final Pattern SUPPORTED_OPENAPI_VERSION = Pattern.compile("3\\.[01]\\.\\d+");

    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    /** Creates an OpenAPI spec reader backed by Jackson 3 JSON and YAML mappers. */
    public OpenApiSpecReader() {
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Performs the read operation.
     *
     * @param source source value
     * @return read result
     * @throws IOException when the operation cannot be completed
     */
    public OpenApiSpecInfo read(Path source) throws IOException {
        Path safeSource = java.util.Objects.requireNonNull(source, "source must not be null");
        JsonNode root = mapper(safeSource).readTree(safeSource.toFile());
        validateDocument(root, safeSource);
        JsonNode info = root.path("info");
        return OpenApiSpecInfo.from(
                safeSource,
                requiredText(info, "title", safeSource),
                requiredText(info, "version", safeSource));
    }

    /**
     * Performs the sha256 operation.
     *
     * @param source source value
     * @return sha256 result
     * @throws IOException when the operation cannot be completed
     */
    public String sha256(Path source) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (var input = Files.newInputStream(source)) {
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest()) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private ObjectMapper mapper(Path source) {
        String fileName = source.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return fileName.endsWith(".json") ? jsonMapper : yamlMapper;
    }

    private static void validateDocument(JsonNode root, Path source) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException(source + ": OpenAPI document must be an object");
        }
        String version = root.path("openapi").asString().trim();
        if (!SUPPORTED_OPENAPI_VERSION.matcher(version).matches()) {
            throw new IllegalArgumentException(
                    source + ": openapi must declare a supported 3.0.x or 3.1.x version");
        }
        if (!root.path("info").isObject()) {
            throw new IllegalArgumentException(source + ": missing info object");
        }
    }

    private static String requiredText(JsonNode parent, String name, Path source) {
        String value = parent.path(name).asString().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(source + ": missing info." + name);
        }
        return value;
    }
}
