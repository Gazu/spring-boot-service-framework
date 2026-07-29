package com.smbtech.serviceframework.starter.restclient.api.oauth2;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class OAuth2ApiSupport {

    private OAuth2ApiSupport() {}

    static String text(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    static <V> Map<String, V> immutableMap(Map<String, V> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        IdentityHashMap<Object, Boolean> visiting = new IdentityHashMap<>();
        LinkedHashMap<String, V> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(key, immutableValue(value, visiting)));
        return Collections.unmodifiableMap(copy);
    }

    static Set<String> immutableSet(Set<String> values) {
        return Collections.unmodifiableSet(
                new LinkedHashSet<>(Objects.requireNonNullElse(values, Set.of())));
    }

    static String name(String value, String name) {
        String normalized = text(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private static <V> V immutableValue(V value, IdentityHashMap<Object, Boolean> visiting) {
        if (value == null) {
            return null;
        }
        Object copy;
        if (value instanceof Map<?, ?> map) {
            enter(value, visiting);
            try {
                LinkedHashMap<Object, Object> entries = new LinkedHashMap<>();
                map.forEach((key, item) -> entries.put(key, immutableValue(item, visiting)));
                copy = Collections.unmodifiableMap(entries);
            } finally {
                visiting.remove(value);
            }
        } else if (value instanceof Set<?> set) {
            enter(value, visiting);
            try {
                LinkedHashSet<Object> entries = new LinkedHashSet<>();
                set.forEach(item -> entries.add(immutableValue(item, visiting)));
                copy = Collections.unmodifiableSet(entries);
            } finally {
                visiting.remove(value);
            }
        } else if (value instanceof Collection<?> collection) {
            enter(value, visiting);
            try {
                List<Object> entries = new ArrayList<>(collection.size());
                collection.forEach(item -> entries.add(immutableValue(item, visiting)));
                copy = Collections.unmodifiableList(entries);
            } finally {
                visiting.remove(value);
            }
        } else if (value.getClass().isArray()) {
            enter(value, visiting);
            try {
                int length = Array.getLength(value);
                List<Object> entries = new ArrayList<>(length);
                for (int index = 0; index < length; index++) {
                    entries.add(immutableValue(Array.get(value, index), visiting));
                }
                copy = Collections.unmodifiableList(entries);
            } finally {
                visiting.remove(value);
            }
        } else {
            copy = value;
        }
        return (V) copy;
    }

    private static void enter(Object value, IdentityHashMap<Object, Boolean> visiting) {
        if (visiting.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("OAuth2 context values must not contain cycles");
        }
    }
}
