package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.starter.restclient.api.JwtBearerTokenRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtBearerTokenRequestTest {

    @Test
    void normalizesTokenRequestIdAndDefaultsOptionalValues() {
        JwtBearerTokenRequest request = new JwtBearerTokenRequest(" payments-jwt-bearer-token ");

        assertThat(request.tokenRequestId()).isEqualTo("payments-jwt-bearer-token");
        assertThat(request.expectedScopes()).isEmpty();
        assertThat(request.customClaims()).isEmpty();
    }

    @Test
    void copiesCustomClaimsDefensively() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("customer_id", "17952397-3");

        JwtBearerTokenRequest request =
                new JwtBearerTokenRequest("payments-jwt-bearer-token", " payment.read ", claims);
        claims.put("channel", "backend");

        assertThat(request.expectedScopes()).isEqualTo("payment.read");
        assertThat(request.customClaims()).containsExactly(Map.entry("customer_id", "17952397-3"));
        assertThatThrownBy(() -> request.customClaims().put("channel", "backend"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void recursivelyCopiesCustomClaims() {
        List<Object> roles = new ArrayList<>(List.of("reader"));
        Map<String, Object> authorization = new LinkedHashMap<>();
        authorization.put("roles", roles);

        JwtBearerTokenRequest request =
                new JwtBearerTokenRequest(
                        "payments-jwt-bearer-token", Map.of("authorization", authorization));
        roles.add("writer");

        Map<String, Object> immutableAuthorization =
                (Map<String, Object>) request.customClaims().get("authorization");
        List<Object> immutableRoles = (List<Object>) immutableAuthorization.get("roles");
        assertThat(immutableRoles).containsExactly("reader");
        assertThatThrownBy(() -> immutableRoles.add("writer"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBlankTokenRequestId() {
        assertThatThrownBy(() -> new JwtBearerTokenRequest(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tokenRequestId must not be blank");
    }
}
