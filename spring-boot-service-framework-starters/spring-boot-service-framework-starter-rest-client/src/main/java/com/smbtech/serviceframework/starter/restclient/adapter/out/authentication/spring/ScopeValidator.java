package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Validates OAuth2 access-token scopes inside the Spring Security adapter. */
final class ScopeValidator {

    /**
     * Performs the validate operation.
     *
     * @param expectedScopes expected scopes value
     * @param actualScopes actual scopes value
     */
    void validate(String expectedScopes, Set<String> actualScopes) {
        Set<String> expected = parse(expectedScopes);
        if (expected.isEmpty()) {
            return;
        }
        if (actualScopes == null || !actualScopes.containsAll(expected)) {
            throw new HttpClientAuthenticationException(
                    "Access token does not contain expected scopes. expected="
                            + format(expected)
                            + ", actual="
                            + format(actualScopes));
        }
    }

    /**
     * Performs the parse operation.
     *
     * @param scopes scopes value
     * @return parse result
     */
    Set<String> parse(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scopes.split("[,\\s]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toCollection(TreeSet::new),
                                Collections::unmodifiableSet));
    }

    private String format(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(" ", new TreeSet<>(scopes)) + "]";
    }
}
