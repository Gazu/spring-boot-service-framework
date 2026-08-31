package com.smbtech.serviceframework.starter.errorhandling.serialization;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Recursively normalizes notification metadata map keys to snake case without changing scalar
 * values.
 */
final class NotificationMetadataKeyNormalizer {

    private static final Pattern ACRONYM_BOUNDARY = Pattern.compile("([A-Z]+)([A-Z][a-z])");
    private static final Pattern WORD_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");
    private static final Pattern SEPARATOR = Pattern.compile("[^A-Za-z0-9]+");
    private static final Pattern REPEATED_UNDERSCORE = Pattern.compile("_+");

    /** Creates a metadata key normalizer. */
    public NotificationMetadataKeyNormalizer() {}

    /**
     * Returns an immutable metadata map with recursively normalized keys.
     *
     * @param metadata source metadata
     * @return normalized metadata
     */
    public Map<String, Object> normalize(Map<?, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return normalizeMap(metadata, new IdentityHashMap<>());
    }

    /**
     * Converts a metadata key to snake case.
     *
     * @param key source key
     * @return snake-case key
     */
    public String normalizeKey(String key) {
        String source = Objects.requireNonNull(key, "metadata key must not be null").trim();
        if (source.isEmpty()) {
            throw new IllegalArgumentException("metadata key must not be blank");
        }
        String normalized = ACRONYM_BOUNDARY.matcher(source).replaceAll("$1_$2");
        normalized = WORD_BOUNDARY.matcher(normalized).replaceAll("$1_$2");
        normalized = SEPARATOR.matcher(normalized).replaceAll("_");
        normalized = REPEATED_UNDERSCORE.matcher(normalized).replaceAll("_");
        normalized = trimUnderscores(normalized).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("metadata key must contain letters or digits");
        }
        return normalized;
    }

    private Map<String, Object> normalizeMap(
            Map<?, ?> source, IdentityHashMap<Object, Boolean> visited) {
        enter(source, visited);
        try {
            Map<String, Object> normalized = new LinkedHashMap<>();
            source.forEach(
                    (key, value) -> {
                        if (key == null) {
                            throw new IllegalArgumentException("metadata key must not be null");
                        }
                        String normalizedKey = normalizeKey(key.toString());
                        if (normalized.containsKey(normalizedKey)) {
                            throw new IllegalArgumentException(
                                    "Metadata keys collide after snake-case normalization: "
                                            + normalizedKey);
                        }
                        normalized.put(normalizedKey, normalizeValue(value, visited));
                    });
            return Collections.unmodifiableMap(normalized);
        } finally {
            visited.remove(source);
        }
    }

    private Object normalizeValue(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value instanceof Map<?, ?> map) {
            return normalizeMap(map, visited);
        }
        if (value instanceof Collection<?> collection) {
            return normalizeCollection(collection, visited);
        }
        if (value != null && value.getClass().isArray()) {
            return normalizeArray(value, visited);
        }
        return value;
    }

    private List<Object> normalizeCollection(
            Collection<?> source, IdentityHashMap<Object, Boolean> visited) {
        enter(source, visited);
        try {
            List<Object> normalized = new ArrayList<>(source.size());
            for (Object value : source) {
                normalized.add(normalizeValue(value, visited));
            }
            return Collections.unmodifiableList(normalized);
        } finally {
            visited.remove(source);
        }
    }

    private List<Object> normalizeArray(Object source, IdentityHashMap<Object, Boolean> visited) {
        enter(source, visited);
        try {
            int length = Array.getLength(source);
            List<Object> normalized = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                normalized.add(normalizeValue(Array.get(source, index), visited));
            }
            return Collections.unmodifiableList(normalized);
        } finally {
            visited.remove(source);
        }
    }

    private static void enter(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (visited.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Cyclic notification metadata is not supported");
        }
    }

    private static String trimUnderscores(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '_') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '_') {
            end--;
        }
        return value.substring(start, end);
    }
}
