package com.smbtech.serviceframework.openapi.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Provides open api spec reader behavior. */
public final class OpenApiSpecReader {
    /** Creates an OpenAPI spec reader instance. */
    public OpenApiSpecReader() {}

    private static final Pattern JSON_INFO_BLOCK =
            Pattern.compile("\"info\"\\s*:\\s*\\{(?<body>.*?)\\}", Pattern.DOTALL);
    private static final Pattern JSON_STRING_PROPERTY =
            Pattern.compile("\"%s\"\\s*:\\s*\"(?<value>(?:\\\\.|[^\"])*)\"");

    /**
     * Performs the read operation.
     *
     * @param source source value
     * @return read result
     * @throws IOException when the operation cannot be completed
     */
    public OpenApiSpecInfo read(Path source) throws IOException {
        String fileName = source.getFileName().toString().toLowerCase();
        return fileName.endsWith(".json") ? readJson(source) : readYaml(source);
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

    private OpenApiSpecInfo readYaml(Path source) throws IOException {
        boolean inInfo = false;
        int infoIndent = -1;
        String title = null;
        String version = null;

        for (String rawLine : Files.readAllLines(source, StandardCharsets.UTF_8)) {
            String line = rawLine.replace("\t", "    ");
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }

            int indent = line.indexOf(trimmed);
            if (!inInfo) {
                if (trimmed.equals("info:") || trimmed.startsWith("info: #")) {
                    inInfo = true;
                    infoIndent = indent;
                }
                continue;
            }

            if (indent <= infoIndent && trimmed.matches("^[A-Za-z0-9_.-]+:.*")) {
                break;
            }

            if (trimmed.startsWith("title:")) {
                title = unquoteScalar(trimmed.substring("title:".length()));
            } else if (trimmed.startsWith("version:")) {
                version = unquoteScalar(trimmed.substring("version:".length()));
            }
        }

        if (title == null) {
            throw new IllegalArgumentException(source + ": missing info.title");
        }
        if (version == null) {
            throw new IllegalArgumentException(source + ": missing info.version");
        }
        return OpenApiSpecInfo.from(source, title, version);
    }

    private OpenApiSpecInfo readJson(Path source) throws IOException {
        String content = Files.readString(source, StandardCharsets.UTF_8);
        Matcher infoMatcher = JSON_INFO_BLOCK.matcher(content);
        if (!infoMatcher.find()) {
            throw new IllegalArgumentException(source + ": missing info object");
        }
        String body = infoMatcher.group("body");
        String title = jsonStringProperty(body, "title");
        String version = jsonStringProperty(body, "version");
        return OpenApiSpecInfo.from(source, title, version);
    }

    private static String jsonStringProperty(String body, String property) {
        Matcher matcher =
                Pattern.compile(JSON_STRING_PROPERTY.pattern().formatted(property)).matcher(body);
        if (!matcher.find()) {
            throw new IllegalArgumentException("missing info." + property);
        }
        return matcher.group("value").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String unquoteScalar(String rawValue) {
        String value = rawValue.trim();
        int commentIndex = value.indexOf(" #");
        if (commentIndex >= 0) {
            value = value.substring(0, commentIndex).trim();
        }
        if ((value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value.trim();
    }
}
