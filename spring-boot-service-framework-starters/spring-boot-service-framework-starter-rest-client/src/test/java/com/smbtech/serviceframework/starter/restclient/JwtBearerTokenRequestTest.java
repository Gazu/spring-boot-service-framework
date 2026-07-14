package com.smbtech.serviceframework.starter.restclient;

import com.smbtech.serviceframework.starter.restclient.api.JwtBearerTokenRequest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        JwtBearerTokenRequest request = new JwtBearerTokenRequest(
                "payments-jwt-bearer-token",
                " payment.read ",
                claims
        );
        claims.put("channel", "backend");

        assertThat(request.expectedScopes()).isEqualTo("payment.read");
        assertThat(request.customClaims()).containsExactly(Map.entry("customer_id", "17952397-3"));
        assertThatThrownBy(() -> request.customClaims().put("channel", "backend"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBlankTokenRequestId() {
        assertThatThrownBy(() -> new JwtBearerTokenRequest(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tokenRequestId must not be blank");
    }
}
