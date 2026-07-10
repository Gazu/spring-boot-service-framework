package com.smbtech.serviceframework.httpclient.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public record TokenCacheKey(
        String id,
        Set<String> scopes
) {

    private static final String SEPARATOR = "::";

    public TokenCacheKey {
        id = Objects.requireNonNullElse(id, "").trim();
        scopes = Collections.unmodifiableSet(new TreeSet<>(Objects.requireNonNullElse(scopes, Set.of())));
    }

    public static TokenCacheKey of(String id, Collection<String> scopes) {
        return new TokenCacheKey(id, scopes == null ? Set.of() : new TreeSet<>(scopes));
    }

    public static TokenCacheKey of(String id, String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return new TokenCacheKey(id, Set.of());
        }
        return new TokenCacheKey(
                id,
                Arrays.stream(scopes.split("[,\\s]+"))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .collect(Collectors.toUnmodifiableSet())
        );
    }

    public String value() {
        if (scopes.isEmpty()) {
            return id;
        }
        return id + SEPARATOR + String.join(" ", scopes);
    }
}
