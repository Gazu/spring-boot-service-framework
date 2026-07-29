package com.smbtech.serviceframework.starter.actuator.adapter.in.metrics;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.ComponentStatus;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Publishes bounded-cardinality Micrometer gauges for framework diagnostics. */
public final class ServiceFrameworkMetrics implements MeterBinder {

    /** One-hot aggregate status gauge. */
    public static final String STATUS_METRIC_NAME = "smbtech.service.framework.status";

    /** Number of diagnostic components grouped by status. */
    public static final String COMPONENTS_METRIC_NAME = "smbtech.service.framework.components";

    /** Number of detected framework modules. */
    public static final String MODULES_METRIC_NAME = "smbtech.service.framework.modules";

    private static final String STATUS_TAG = "status";

    private final FrameworkDiagnostics diagnostics;
    private final Clock clock;
    private final Duration cacheTtl;
    private final Object cacheMonitor = new Object();
    private volatile MetricsSample cachedSample;

    /**
     * Creates a metrics binder.
     *
     * @param diagnostics framework diagnostics
     * @param clock sample cache clock
     * @param cacheTtl duration for which one diagnostics sample is reused
     */
    public ServiceFrameworkMetrics(
            FrameworkDiagnostics diagnostics, Clock clock, Duration cacheTtl) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.cacheTtl = Objects.requireNonNull(cacheTtl, "cacheTtl must not be null");
        if (cacheTtl.isNegative()) {
            throw new IllegalArgumentException("cacheTtl must not be negative");
        }
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Objects.requireNonNull(registry, "registry must not be null");

        for (ComponentStatus status : ComponentStatus.values()) {
            String tagValue = tagValue(status);
            Gauge.builder(STATUS_METRIC_NAME, this, metrics -> metrics.aggregateStatusValue(status))
                    .description("Current aggregate Service Framework diagnostic status")
                    .tag(STATUS_TAG, tagValue)
                    .register(registry);
            Gauge.builder(COMPONENTS_METRIC_NAME, this, metrics -> metrics.componentCount(status))
                    .description("Service Framework diagnostic components by status")
                    .tag(STATUS_TAG, tagValue)
                    .register(registry);
        }

        Gauge.builder(MODULES_METRIC_NAME, this, ServiceFrameworkMetrics::moduleCount)
                .description("Detected Service Framework modules")
                .register(registry);
    }

    private double aggregateStatusValue(ComponentStatus status) {
        return sample().aggregateStatus() == status ? 1.0 : 0.0;
    }

    private double componentCount(ComponentStatus status) {
        return sample().componentCounts().get(status);
    }

    private double moduleCount() {
        return sample().moduleCount();
    }

    private MetricsSample sample() {
        Instant now = clock.instant();
        MetricsSample current = cachedSample;
        if (current != null && now.isBefore(current.expiresAt())) {
            return current;
        }

        synchronized (cacheMonitor) {
            now = clock.instant();
            current = cachedSample;
            if (current != null && now.isBefore(current.expiresAt())) {
                return current;
            }
            MetricsSample refreshed = capture(now);
            cachedSample = refreshed;
            return refreshed;
        }
    }

    private MetricsSample capture(Instant capturedAt) {
        ComponentStatus aggregateStatus = ComponentStatus.UNKNOWN;
        EnumMap<ComponentStatus, Integer> componentCounts = new EnumMap<>(ComponentStatus.class);
        for (ComponentStatus status : ComponentStatus.values()) {
            componentCounts.put(status, 0);
        }

        try {
            FrameworkDiagnosticsSnapshot snapshot = diagnostics.snapshot();
            if (snapshot != null) {
                aggregateStatus = snapshot.status();
                for (ComponentHealth component : snapshot.components().values()) {
                    componentCounts.compute(component.status(), (key, count) -> count + 1);
                }
            }
        } catch (RuntimeException ignored) {
            aggregateStatus = ComponentStatus.UNKNOWN;
        }

        int moduleCount = 0;
        try {
            List<?> modules = diagnostics.modules();
            moduleCount = modules == null ? 0 : modules.size();
        } catch (RuntimeException ignored) {
            moduleCount = 0;
        }

        return new MetricsSample(
                aggregateStatus, Map.copyOf(componentCounts), moduleCount, expiresAt(capturedAt));
    }

    private Instant expiresAt(Instant capturedAt) {
        try {
            return capturedAt.plus(cacheTtl);
        } catch (RuntimeException ignored) {
            return Instant.MAX;
        }
    }

    private static String tagValue(ComponentStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }

    private record MetricsSample(
            ComponentStatus aggregateStatus,
            Map<ComponentStatus, Integer> componentCounts,
            int moduleCount,
            Instant expiresAt) {}
}
