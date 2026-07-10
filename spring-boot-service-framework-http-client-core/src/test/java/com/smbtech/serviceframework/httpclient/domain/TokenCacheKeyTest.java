package com.smbtech.serviceframework.httpclient.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenCacheKeyTest {

    @Test
    void createsStableKeyForSameScopesInDifferentOrder() {
        TokenCacheKey first = TokenCacheKey.of("payments-api", Set.of("payments.write", "payments.read"));
        TokenCacheKey second = TokenCacheKey.of("payments-api", "payments.read payments.write");

        assertEquals("payments-api::payments.read payments.write", first.value());
        assertEquals(first.value(), second.value());
    }

    @Test
    void usesPlainIdWhenScopesAreEmpty() {
        assertEquals("payments-api", TokenCacheKey.of("payments-api", "").value());
    }
}
