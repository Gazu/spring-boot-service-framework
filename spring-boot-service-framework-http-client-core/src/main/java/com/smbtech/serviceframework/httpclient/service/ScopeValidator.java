package com.smbtech.serviceframework.httpclient.service;

import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class ScopeValidator {

    public void validate(String expectedScopes, Set<String> actualScopes) {
        Set<String> expected = parse(expectedScopes);
        if (expected.isEmpty()) {
            return;
        }
        if (actualScopes == null || !actualScopes.containsAll(expected)) {
            throw new AuthenticationException(
                    "Access token does not contain expected scopes. expected="
                            + format(expected)
                            + ", actual="
                            + format(actualScopes)
            );
        }
    }

    public Set<String> parse(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scopes.split("[,\\s]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(TreeSet::new),
                        Collections::unmodifiableSet
                ));
    }

    private String format(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(" ", new TreeSet<>(scopes)) + "]";
    }
}
