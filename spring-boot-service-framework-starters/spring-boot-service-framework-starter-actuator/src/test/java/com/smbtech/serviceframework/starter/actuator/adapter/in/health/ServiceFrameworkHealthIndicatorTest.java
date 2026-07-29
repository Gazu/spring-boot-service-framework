package com.smbtech.serviceframework.starter.actuator.adapter.in.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.ComponentStatus;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class ServiceFrameworkHealthIndicatorTest {

    @Test
    void mapsEveryNeutralStatusToSpringBoot() {
        assertThat(indicator(ComponentHealth.up("component")).health().getStatus())
                .isEqualTo(Status.UP);
        assertThat(indicator(ComponentHealth.down("component")).health().getStatus())
                .isEqualTo(Status.DOWN);
        assertThat(indicator(ComponentHealth.outOfService("component")).health().getStatus())
                .isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(indicator(ComponentHealth.unknown("component")).health().getStatus())
                .isEqualTo(Status.UNKNOWN);
    }

    @Test
    @SuppressWarnings("unchecked")
    void exposesOnlyBoundedNeutralDetails() {
        ComponentHealth component =
                new ComponentHealth(
                        "rest-client",
                        ComponentStatus.UP,
                        Map.of("configuredClients", 2, "clientSecret", "must-not-leak"));
        Health health = indicator(component).health();

        assertThat(health.getDetails())
                .containsEntry("capturedAt", "2026-07-27T12:00:00Z")
                .containsEntry("componentCount", 1);
        Map<String, Object> components =
                (Map<String, Object>) health.getDetails().get("components");
        Map<String, Object> restClient = (Map<String, Object>) components.get("rest-client");
        Map<String, Object> details = (Map<String, Object>) restClient.get("details");

        assertThat(restClient).containsEntry("status", "UP");
        assertThat(details)
                .containsEntry("configuredClients", 2)
                .containsEntry("clientSecret", "[REDACTED]");
        assertThat(health.toString()).doesNotContain("must-not-leak");
    }

    @Test
    void convertsDiagnosticsFailureToSafeUnknownResult() {
        FrameworkDiagnostics diagnostics =
                new FrameworkDiagnostics() {
                    @Override
                    public FrameworkDiagnosticsSnapshot snapshot() {
                        throw new IllegalStateException("secret failure");
                    }

                    @Override
                    public List<FrameworkModuleInfo> modules() {
                        return List.of();
                    }
                };

        Health health = new ServiceFrameworkHealthIndicator(diagnostics).health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsOnlyKeys("reason");
        assertThat(health.getDetails()).containsEntry("reason", "diagnostics_failed");
        assertThat(health.toString()).doesNotContain("secret failure");
    }

    @Test
    void convertsMissingSnapshotToSafeUnknownResult() {
        FrameworkDiagnostics diagnostics =
                new FrameworkDiagnostics() {
                    @Override
                    public FrameworkDiagnosticsSnapshot snapshot() {
                        return null;
                    }

                    @Override
                    public List<FrameworkModuleInfo> modules() {
                        return List.of();
                    }
                };

        Health health = new ServiceFrameworkHealthIndicator(diagnostics).health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("reason", "no_result");
    }

    private ServiceFrameworkHealthIndicator indicator(ComponentHealth component) {
        FrameworkDiagnostics diagnostics =
                new FrameworkDiagnostics() {
                    @Override
                    public FrameworkDiagnosticsSnapshot snapshot() {
                        return new FrameworkDiagnosticsSnapshot(
                                Instant.parse("2026-07-27T12:00:00Z"),
                                Map.of(component.name(), component));
                    }

                    @Override
                    public List<FrameworkModuleInfo> modules() {
                        return List.of();
                    }
                };
        return new ServiceFrameworkHealthIndicator(diagnostics);
    }
}
