package com.smbtech.serviceframework.actuator.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComponentHealthTest {

    @Test
    void createsEachSupportedStatus() {
        assertEquals(ComponentStatus.UP, ComponentHealth.up("logging").status());
        assertEquals(ComponentStatus.DOWN, ComponentHealth.down("logging").status());
        assertEquals(
                ComponentStatus.OUT_OF_SERVICE, ComponentHealth.outOfService("logging").status());
        assertEquals(ComponentStatus.UNKNOWN, ComponentHealth.unknown("logging").status());
        assertTrue(ComponentHealth.up("logging", Map.of("enabled", true)).isUp());
        assertFalse(ComponentHealth.down("logging", Map.of("failures", 1)).isUp());
        assertEquals(
                Map.of("reason", "maintenance"),
                ComponentHealth.outOfService("logging", Map.of("reason", "maintenance")).details());
        assertEquals(
                Map.of("reason", "not_configured"),
                ComponentHealth.unknown("logging", Map.of("reason", "not_configured")).details());
    }

    @Test
    void normalizesNameAndOrdersAndCopiesDetails() {
        List<Object> states = new ArrayList<>(List.of("ready"));
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("zeta", states);
        details.put("alpha", new String[] {"one", "two"});
        details.put("set", Set.of("beta", "alpha"));
        details.put("time", Instant.EPOCH);
        details.put("duration", Duration.ofSeconds(1));
        details.put("id", UUID.fromString("00000000-0000-0000-0000-000000000001"));

        ComponentHealth health = ComponentHealth.up(" logging ", details);
        states.add("changed");
        details.put("later", true);

        assertEquals("logging", health.name());
        assertEquals(
                List.of("alpha", "duration", "id", "set", "time", "zeta"),
                List.copyOf(health.details().keySet()));
        assertEquals(List.of("one", "two"), health.details().get("alpha"));
        assertEquals(List.of("alpha", "beta"), health.details().get("set"));
        assertEquals(List.of("ready"), health.details().get("zeta"));
        assertThrows(
                UnsupportedOperationException.class, () -> health.details().put("new", "value"));
    }

    @Test
    void redactsSensitiveDetailsBeforeTraversingTheirValues() {
        Map<String, Object> cyclicSecret = new LinkedHashMap<>();
        cyclicSecret.put("self", cyclicSecret);

        ComponentHealth health =
                ComponentHealth.up(
                        "rest-client",
                        Map.of(
                                "clientSecret",
                                "secret-value",
                                "tokenEndpointUri",
                                "https://internal.example/token",
                                "scopes",
                                List.of("payments.read"),
                                "keystore",
                                cyclicSecret,
                                "configuredClients",
                                2));

        assertEquals("[REDACTED]", health.details().get("clientSecret"));
        assertEquals("[REDACTED]", health.details().get("tokenEndpointUri"));
        assertEquals("[REDACTED]", health.details().get("scopes"));
        assertEquals("[REDACTED]", health.details().get("keystore"));
        assertEquals(2, health.details().get("configuredClients"));
    }

    @Test
    void rejectsUnsafeOrUnboundedDetails() {
        Map<String, Object> cyclic = new LinkedHashMap<>();
        cyclic.put("self", cyclic);
        Map<String, Object> tooMany = new LinkedHashMap<>();
        for (int index = 0; index <= 64; index++) {
            tooMany.put("key" + index, index);
        }

        assertThrows(IllegalArgumentException.class, () -> ComponentHealth.up("component", cyclic));
        assertThrows(
                IllegalArgumentException.class, () -> ComponentHealth.up("component", tooMany));
        assertThrows(
                IllegalArgumentException.class,
                () -> ComponentHealth.up("component", Map.of("value", "x".repeat(2049))));
        assertThrows(
                IllegalArgumentException.class,
                () -> ComponentHealth.up("component", Map.of("value", new Object())));
        assertThrows(
                IllegalArgumentException.class,
                () -> ComponentHealth.up("component", Map.of(" ", "value")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ComponentHealth(" ", ComponentStatus.UP, Map.of()));
    }

    @Test
    void aggregatesStatusesBySeverity() {
        assertEquals(
                ComponentStatus.UNKNOWN,
                ComponentStatus.worst(ComponentStatus.UP, ComponentStatus.UNKNOWN));
        assertEquals(
                ComponentStatus.OUT_OF_SERVICE,
                ComponentStatus.worst(ComponentStatus.OUT_OF_SERVICE, ComponentStatus.UNKNOWN));
        assertEquals(
                ComponentStatus.DOWN,
                ComponentStatus.worst(ComponentStatus.OUT_OF_SERVICE, ComponentStatus.DOWN));
    }
}
