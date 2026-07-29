package com.smbtech.serviceframework.httpclient.domain;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ImmutableHttpClientValues {

    private ImmutableHttpClientValues() {}

    static Map<String, Object> structuredMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        IdentityHashMap<Object, Boolean> visiting = new IdentityHashMap<>();
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(key, copyValue(value, visiting)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value, IdentityHashMap<Object, Boolean> visiting) {
        if (value == null) {
            throw new NullPointerException("HTTP client structured values must not be null");
        }
        if (value instanceof Map<?, ?> map) {
            enter(value, visiting);
            try {
                LinkedHashMap<Object, Object> copy = new LinkedHashMap<>();
                map.forEach((key, item) -> copy.put(key, copyValue(item, visiting)));
                return Collections.unmodifiableMap(copy);
            } finally {
                visiting.remove(value);
            }
        }
        if (value instanceof Set<?> set) {
            enter(value, visiting);
            try {
                LinkedHashSet<Object> copy = new LinkedHashSet<>();
                set.forEach(item -> copy.add(copyValue(item, visiting)));
                return Collections.unmodifiableSet(copy);
            } finally {
                visiting.remove(value);
            }
        }
        if (value instanceof Collection<?> collection) {
            enter(value, visiting);
            try {
                List<Object> copy = new ArrayList<>(collection.size());
                collection.forEach(item -> copy.add(copyValue(item, visiting)));
                return Collections.unmodifiableList(copy);
            } finally {
                visiting.remove(value);
            }
        }
        if (value.getClass().isArray()) {
            enter(value, visiting);
            try {
                int length = Array.getLength(value);
                List<Object> copy = new ArrayList<>(length);
                for (int index = 0; index < length; index++) {
                    copy.add(copyValue(Array.get(value, index), visiting));
                }
                return Collections.unmodifiableList(copy);
            } finally {
                visiting.remove(value);
            }
        }
        return value;
    }

    private static void enter(Object value, IdentityHashMap<Object, Boolean> visiting) {
        if (visiting.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException(
                    "HTTP client structured values must not contain cycles");
        }
    }
}
