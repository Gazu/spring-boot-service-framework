package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Provides OAuth2 token diagnostic sanitizer behavior. */
public final class OAuth2TokenDiagnosticSanitizer {
    /** Creates a OAuth2 token diagnostic sanitizer instance. */
    public OAuth2TokenDiagnosticSanitizer() {}

    private static final String REDACTED = "<redacted>";
    private static final Pattern JWT_PATTERN =
            Pattern.compile("\\b[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b");
    private static final Pattern LONG_SECRET_PATTERN = Pattern.compile("\\b[A-Za-z0-9+/=]{40,}\\b");
    private static final Pattern SENSITIVE_ASSIGNMENT_PATTERN =
            Pattern.compile(
                    "(?i)(authorization|assertion|credential|key|password|secret|token)(\\s*[=:]\\s*)[^\\s,;]+");
    private static final List<String> SENSITIVE_TOKENS =
            List.of(
                    "authorization",
                    "assertion",
                    "credential",
                    "key",
                    "password",
                    "secret",
                    "token");

    /**
     * Performs the sanitize operation.
     *
     * @param values values value
     * @return sanitize result
     */
    public Map<String, Object> sanitize(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        values.forEach(
                (key, value) -> {
                    if (key != null && value != null) {
                        String name = key.toString();
                        sanitized.put(name, sanitizeValue(name, value));
                    }
                });
        return Map.copyOf(sanitized);
    }

    /**
     * Performs the preview operation.
     *
     * @param value sensitive value to preview
     * @param length length value
     * @return preview result
     */
    public String preview(String value, int length) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int safeLength = Math.max(8, length);
        if (value.length() <= safeLength) {
            return value + "...<redacted>";
        }
        return value.substring(0, safeLength) + "...<redacted>";
    }

    /**
     * Performs the sanitize text operation.
     *
     * @param value diagnostic text to sanitize
     * @return sanitize text result
     */
    public String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = JWT_PATTERN.matcher(value).replaceAll("<redacted-JWT>");
        sanitized = LONG_SECRET_PATTERN.matcher(sanitized).replaceAll(REDACTED);
        return SENSITIVE_ASSIGNMENT_PATTERN.matcher(sanitized).replaceAll("$1$2" + REDACTED);
    }

    private Object sanitizeValue(String key, Object value) {
        if (isSensitiveKey(key)) {
            return REDACTED;
        }
        if (value instanceof Map<?, ?> map) {
            return sanitize(map);
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(item -> sanitizeCollectionItem(key, item)).toList();
        }
        return value;
    }

    private Object sanitizeCollectionItem(String key, Object value) {
        if (value instanceof Map<?, ?> map) {
            return sanitize(map);
        }
        return sanitizeValue(key, value);
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_TOKENS.stream().anyMatch(normalized::contains);
    }
}
