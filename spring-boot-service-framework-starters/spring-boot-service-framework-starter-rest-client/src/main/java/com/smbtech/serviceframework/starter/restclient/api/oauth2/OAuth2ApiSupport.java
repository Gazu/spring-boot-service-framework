package com.smbtech.serviceframework.starter.restclient.api.oauth2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class OAuth2ApiSupport {

    private OAuth2ApiSupport() {}

    static String text(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    static <V> Map<String, V> immutableMap(Map<String, V> values) {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(Objects.requireNonNullElse(values, Map.of())));
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
}
