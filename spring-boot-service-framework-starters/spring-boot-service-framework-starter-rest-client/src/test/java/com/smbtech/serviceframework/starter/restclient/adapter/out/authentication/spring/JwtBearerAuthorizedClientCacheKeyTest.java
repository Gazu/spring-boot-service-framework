package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtBearerAuthorizedClientCacheKeyTest {

    @Test
    void keepsBasePrincipalWhenClaimsAreEmpty() {
        assertThat(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", Map.of()))
                .isEqualTo("spring-boot-service-framework");
    }

    @Test
    void createsSamePrincipalForSameClaimsWithDifferentInsertionOrder() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("customer_id", "17952397-3");
        first.put("channel", "mobile");
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("channel", "mobile");
        second.put("customer_id", "17952397-3");

        assertThat(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", first))
                .isEqualTo(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", second));
    }

    @Test
    void createsDifferentPrincipalsForClaimsThatWouldCollideWithFlatConcatenation() {
        Map<String, Object> first = Map.of("a", "b&c=d");
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("a", "b");
        second.put("c", "d");

        assertThat(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", first))
                .isNotEqualTo(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", second));
    }

    @Test
    void createsDifferentPrincipalsForDifferentClaimValueTypes() {
        Map<String, Object> stringClaim = Map.of("priority", "7");
        Map<String, Object> numberClaim = Map.of("priority", 7);

        assertThat(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", stringClaim))
                .isNotEqualTo(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", numberClaim));
    }

    @Test
    void canonicalizesNestedClaims() {
        Map<String, Object> first = Map.of("metadata", Map.of("roles", List.of("payer", "admin")));
        Map<String, Object> second = Map.of("metadata", Map.of("roles", List.of("payer", "admin")));
        Map<String, Object> third = Map.of("metadata", Map.of("roles", List.of("admin", "payer")));

        assertThat(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", first))
                .isEqualTo(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", second))
                .isNotEqualTo(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", third));
    }

    @Test
    void canonicalizesArrayClaims() {
        Map<String, Object> first = Map.of("roles", new String[] {"payer", "admin"});
        Map<String, Object> second = Map.of("roles", new String[] {"payer", "admin"});
        Map<String, Object> third = Map.of("roles", new String[] {"admin", "payer"});

        assertThat(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", first))
                .isEqualTo(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", second))
                .isNotEqualTo(
                        JwtBearerAuthorizedClientCacheKey.principalName(
                                "spring-boot-service-framework", third));
    }
}
