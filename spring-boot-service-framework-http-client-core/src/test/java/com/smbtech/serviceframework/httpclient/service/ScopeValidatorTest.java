package com.smbtech.serviceframework.httpclient.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScopeValidatorTest {

    private final ScopeValidator validator = new ScopeValidator();

    @Test
    void parsesSpaceAndCommaSeparatedScopesIntoSortedSet() {
        Set<String> scopes = validator.parse("payments.write, payments.read payments.audit");

        assertEquals(Set.of("payments.audit", "payments.read", "payments.write"), scopes);
    }

    @Test
    void acceptsTokenWhenItContainsAllExpectedScopes() {
        assertDoesNotThrow(
                () ->
                        validator.validate(
                                "payments.read payments.write",
                                Set.of("payments.write", "payments.audit", "payments.read")));
    }

    @Test
    void rejectsTokenWhenExpectedScopeIsMissing() {
        HttpClientAuthenticationException exception =
                assertThrows(
                        HttpClientAuthenticationException.class,
                        () -> validator.validate("payments.write", Set.of("payments.read")));

        assertTrue(exception.getMessage().contains("expected=[payments.write]"));
        assertTrue(exception.getMessage().contains("actual=[payments.read]"));
    }

    @Test
    void acceptsEmptyExpectedScopes() {
        assertDoesNotThrow(() -> validator.validate("", Set.of()));
    }
}
