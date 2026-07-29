package com.smbtech.serviceframework.starter.actuator.adapter.in.health;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.ComponentStatus;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

/** Adapts framework-neutral diagnostics to a Spring Boot health indicator. */
public final class ServiceFrameworkHealthIndicator implements HealthIndicator {

    private static final String REASON = "reason";

    private final FrameworkDiagnostics diagnostics;

    /**
     * Creates the Service Framework health indicator.
     *
     * @param diagnostics framework diagnostics use case
     */
    public ServiceFrameworkHealthIndicator(FrameworkDiagnostics diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    @Override
    public Health health() {
        try {
            FrameworkDiagnosticsSnapshot snapshot = diagnostics.snapshot();
            if (snapshot == null) {
                return unavailable("no_result");
            }
            return Health.status(toSpringStatus(snapshot.status()))
                    .withDetail("capturedAt", snapshot.capturedAt().toString())
                    .withDetail("componentCount", snapshot.components().size())
                    .withDetail("components", componentDetails(snapshot))
                    .build();
        } catch (RuntimeException ignored) {
            return unavailable("diagnostics_failed");
        }
    }

    private static Health unavailable(String reason) {
        return Health.unknown().withDetail(REASON, reason).build();
    }

    private static Status toSpringStatus(ComponentStatus status) {
        return switch (status) {
            case UP -> Status.UP;
            case DOWN -> Status.DOWN;
            case OUT_OF_SERVICE -> Status.OUT_OF_SERVICE;
            case UNKNOWN -> Status.UNKNOWN;
        };
    }

    private static Map<String, Object> componentDetails(FrameworkDiagnosticsSnapshot snapshot) {
        Map<String, Object> components = new LinkedHashMap<>();
        snapshot.components()
                .forEach((name, component) -> components.put(name, componentDetails(component)));
        return Collections.unmodifiableMap(components);
    }

    private static Map<String, Object> componentDetails(ComponentHealth component) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", component.status().name());
        if (!component.details().isEmpty()) {
            details.put("details", component.details());
        }
        return Collections.unmodifiableMap(details);
    }
}
