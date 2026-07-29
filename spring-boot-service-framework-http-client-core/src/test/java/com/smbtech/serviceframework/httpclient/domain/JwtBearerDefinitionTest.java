package com.smbtech.serviceframework.httpclient.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtBearerDefinitionTest {

    @Test
    @SuppressWarnings("unchecked")
    void recursivelyCopiesCustomClaims() {
        List<Object> roles = new ArrayList<>(List.of("reader"));
        Map<String, Object> authorization = new LinkedHashMap<>();
        authorization.put("roles", roles);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("authorization", authorization);

        JwtBearerDefinition definition =
                new JwtBearerDefinition(
                        "signing-key",
                        "client-id",
                        "client-id",
                        "https://auth.example/token",
                        Duration.ofMinutes(5),
                        claims);
        roles.add("writer");
        authorization.put("tenant", "payments");

        Map<String, Object> immutableAuthorization =
                (Map<String, Object>) definition.customClaims().get("authorization");
        List<Object> immutableRoles = (List<Object>) immutableAuthorization.get("roles");
        assertEquals(List.of("reader"), immutableRoles);
        assertThrows(
                UnsupportedOperationException.class,
                () -> immutableAuthorization.put("tenant", "payments"));
        assertThrows(UnsupportedOperationException.class, () -> immutableRoles.add("writer"));
    }
}
