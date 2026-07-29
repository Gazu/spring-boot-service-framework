package com.smbtech.serviceframework.actuator.domain;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

final class ImmutableDiagnosticValues {

    static final int MAX_DEPTH = 8;
    static final int MAX_CONTAINER_ENTRIES = 64;
    static final int MAX_STRING_LENGTH = 2048;
    static final String REDACTED = "[REDACTED]";

    private static final Set<String> SENSITIVE_KEY_PARTS =
            Set.of(
                    "apikey",
                    "assertion",
                    "authorization",
                    "cookie",
                    "credential",
                    "keystore",
                    "keypassword",
                    "password",
                    "passphrase",
                    "privatekey",
                    "scope",
                    "secret",
                    "storepassword",
                    "token",
                    "truststore");

    private ImmutableDiagnosticValues() {}

    static Map<String, Object> structuredMap(Map<String, Object> values) {
        if (values.isEmpty()) {
            return Map.of();
        }
        return copyMap(values, new IdentityHashMap<>(), 0);
    }

    private static Map<String, Object> copyMap(
            Map<?, ?> values, IdentityHashMap<Object, Boolean> visiting, int depth) {
        validateContainer(values, depth);
        enter(values, visiting);
        try {
            Map<String, Object> sorted = new TreeMap<>();
            values.forEach(
                    (key, value) -> {
                        if (!(key instanceof String stringKey)) {
                            throw new IllegalArgumentException(
                                    "Diagnostic detail keys must be strings");
                        }
                        String normalizedKey = normalizeKey(stringKey);
                        Object copy =
                                isSensitiveKey(normalizedKey)
                                        ? REDACTED
                                        : copyValue(value, visiting, depth + 1);
                        if (sorted.put(normalizedKey, copy) != null) {
                            throw new IllegalArgumentException(
                                    "Diagnostic detail keys must be unique after normalization");
                        }
                    });
            return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
        } finally {
            visiting.remove(values);
        }
    }

    private static Object copyValue(
            Object value, IdentityHashMap<Object, Boolean> visiting, int depth) {
        if (value == null) {
            throw new IllegalArgumentException("Diagnostic detail values must not be null");
        }
        if (value instanceof String string) {
            if (string.length() > MAX_STRING_LENGTH) {
                throw new IllegalArgumentException(
                        "Diagnostic strings must not exceed " + MAX_STRING_LENGTH + " characters");
            }
            return string;
        }
        if (isSupportedScalar(value)) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            return copyMap(map, visiting, depth);
        }
        if (value instanceof Collection<?> collection) {
            return copyCollection(collection, visiting, depth);
        }
        if (value.getClass().isArray()) {
            return copyArray(value, visiting, depth);
        }
        throw new IllegalArgumentException(
                "Unsupported diagnostic detail value type: " + value.getClass().getName());
    }

    private static List<Object> copyCollection(
            Collection<?> values, IdentityHashMap<Object, Boolean> visiting, int depth) {
        validateContainer(values, depth);
        enter(values, visiting);
        try {
            List<Object> copy = new ArrayList<>(values.size());
            values.forEach(value -> copy.add(copyValue(value, visiting, depth + 1)));
            if (values instanceof Set<?>) {
                copy.sort(Comparator.comparing(ImmutableDiagnosticValues::stableValue));
            }
            return Collections.unmodifiableList(copy);
        } finally {
            visiting.remove(values);
        }
    }

    private static List<Object> copyArray(
            Object values, IdentityHashMap<Object, Boolean> visiting, int depth) {
        int length = Array.getLength(values);
        validateContainer(length, depth);
        enter(values, visiting);
        try {
            List<Object> copy = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                copy.add(copyValue(Array.get(values, index), visiting, depth + 1));
            }
            return Collections.unmodifiableList(copy);
        } finally {
            visiting.remove(values);
        }
    }

    private static boolean isSupportedScalar(Object value) {
        return value instanceof Boolean
                || value instanceof Character
                || value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Double
                || value instanceof BigInteger
                || value instanceof BigDecimal
                || value instanceof Enum<?>
                || value instanceof UUID
                || value instanceof TemporalAccessor
                || value instanceof TemporalAmount;
    }

    private static String normalizeKey(String key) {
        String normalized = key.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Diagnostic detail keys must not be blank");
        }
        if (normalized.length() > MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(
                    "Diagnostic detail keys must not exceed " + MAX_STRING_LENGTH + " characters");
        }
        return normalized;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.endsWith("url")
                || normalized.endsWith("uri")
                || SENSITIVE_KEY_PARTS.stream().anyMatch(normalized::contains);
    }

    private static void validateContainer(Collection<?> values, int depth) {
        validateContainer(values.size(), depth);
    }

    private static void validateContainer(Map<?, ?> values, int depth) {
        validateContainer(values.size(), depth);
    }

    private static void validateContainer(int size, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException(
                    "Diagnostic details must not exceed a depth of " + MAX_DEPTH);
        }
        if (size > MAX_CONTAINER_ENTRIES) {
            throw new IllegalArgumentException(
                    "Diagnostic containers must not exceed " + MAX_CONTAINER_ENTRIES + " entries");
        }
    }

    private static void enter(Object value, IdentityHashMap<Object, Boolean> visiting) {
        if (visiting.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Diagnostic details must not contain cycles");
        }
    }

    private static String stableValue(Object value) {
        return value.getClass().getName() + ':' + value;
    }
}
