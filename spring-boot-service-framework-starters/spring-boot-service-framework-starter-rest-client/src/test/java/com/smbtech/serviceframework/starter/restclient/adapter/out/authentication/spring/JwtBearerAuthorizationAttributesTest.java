package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtBearerAuthorizationAttributesTest {

    @Test
    void createsAuthorizationAttributesWithSanitizedClaims() {
        Map<String, Object> attributes =
                JwtBearerAuthorizationAttributes.authorizationAttributes(
                        Map.of(
                                "customer_id", "17952397-3",
                                "iss", "malicious-issuer",
                                "client_secret", "secret",
                                "channel", "mobile"));

        assertThat(attributes)
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of(
                                "customer_id", "17952397-3",
                                "channel", "mobile"));
    }

    @Test
    void createsAuthorizationAttributesWithConfiguredBlockedClaims() {
        Map<String, Object> attributes =
                JwtBearerAuthorizationAttributes.authorizationAttributes(
                        Map.of(
                                "customer_id", "17952397-3",
                                "channel", "mobile"),
                        Set.of("customer_id"));

        assertThat(attributes)
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of("channel", "mobile"));
    }

    @Test
    void returnsEmptyAuthorizationAttributesWhenAllClaimsAreBlocked() {
        Map<String, Object> attributes =
                JwtBearerAuthorizationAttributes.authorizationAttributes(
                        Map.of(
                                "iss", "malicious-issuer",
                                "client_secret", "secret"));

        assertThat(attributes).isEmpty();
    }

    @Test
    void cachePrincipalNameUsesOnlySanitizedClaims() {
        String basePrincipal = "spring-boot-service-framework";

        assertThat(
                        JwtBearerAuthorizationAttributes.cachePrincipalName(
                                basePrincipal,
                                Map.of("iss", "malicious-issuer", "client_secret", "secret")))
                .isEqualTo(basePrincipal);

        assertThat(
                        JwtBearerAuthorizationAttributes.cachePrincipalName(
                                basePrincipal,
                                Map.of("customer_id", "17952397-3"),
                                Set.of("customer_id")))
                .isEqualTo(basePrincipal);

        assertThat(
                        JwtBearerAuthorizationAttributes.cachePrincipalName(
                                basePrincipal, Map.of("customer_id", "17952397-3")))
                .startsWith(basePrincipal + ":jwt-bearer:");
    }
}
