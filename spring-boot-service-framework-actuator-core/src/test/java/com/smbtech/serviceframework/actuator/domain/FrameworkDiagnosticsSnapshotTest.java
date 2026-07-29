package com.smbtech.serviceframework.actuator.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FrameworkDiagnosticsSnapshotTest {

    @Test
    void createsDeterministicSnapshotAndAggregatesWorstStatus() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("rest-client", ComponentHealth.up("rest-client"));
        components.put("logging", ComponentHealth.outOfService("logging"));
        components.put("mock", ComponentHealth.down("mock"));

        FrameworkDiagnosticsSnapshot snapshot =
                new FrameworkDiagnosticsSnapshot(Instant.EPOCH, components);
        components.clear();

        assertEquals(Instant.EPOCH, snapshot.capturedAt());
        assertEquals(
                List.of("logging", "mock", "rest-client"),
                List.copyOf(snapshot.components().keySet()));
        assertEquals(ComponentStatus.DOWN, snapshot.status());
        assertTrue(snapshot.component(" mock ").isPresent());
        assertFalse(snapshot.component("missing").isPresent());
        assertFalse(snapshot.isEmpty());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.components().put("other", ComponentHealth.up("other")));
    }

    @Test
    void returnsUnknownForEmptySnapshot() {
        FrameworkDiagnosticsSnapshot snapshot =
                new FrameworkDiagnosticsSnapshot(Instant.EPOCH, Map.of());

        assertTrue(snapshot.isEmpty());
        assertEquals(ComponentStatus.UNKNOWN, snapshot.status());
    }

    @Test
    void rejectsInvalidComponentMappings() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new FrameworkDiagnosticsSnapshot(
                                Instant.EPOCH,
                                Map.of("different", ComponentHealth.up("component"))));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new FrameworkDiagnosticsSnapshot(
                                Instant.EPOCH, Map.of(" ", ComponentHealth.up("component"))));
    }
}
