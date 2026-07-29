package com.smbtech.serviceframework.starter.actuator.adapter.in.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ServiceFrameworkMetricsTest {

    @Test
    void publishesBoundedStatusComponentAndModuleGauges() {
        CountingDiagnostics diagnostics = new CountingDiagnostics();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ServiceFrameworkMetrics metrics =
                new ServiceFrameworkMetrics(
                        diagnostics,
                        Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneOffset.UTC),
                        Duration.ofSeconds(10));

        metrics.bindTo(registry);

        assertThat(status(registry, "up")).isZero();
        assertThat(status(registry, "down")).isEqualTo(1.0);
        assertThat(status(registry, "out_of_service")).isZero();
        assertThat(status(registry, "unknown")).isZero();
        assertThat(components(registry, "up")).isEqualTo(1.0);
        assertThat(components(registry, "down")).isEqualTo(1.0);
        assertThat(components(registry, "out_of_service")).isEqualTo(1.0);
        assertThat(components(registry, "unknown")).isEqualTo(1.0);
        assertThat(registry.get(ServiceFrameworkMetrics.MODULES_METRIC_NAME).gauge().value())
                .isEqualTo(2.0);
        assertThat(diagnostics.snapshotCalls).hasValue(1);
        assertThat(diagnostics.moduleCalls).hasValue(1);

        assertThat(registry.getMeters())
                .extracting(meter -> meter.getId().getName())
                .containsOnly(
                        ServiceFrameworkMetrics.STATUS_METRIC_NAME,
                        ServiceFrameworkMetrics.COMPONENTS_METRIC_NAME,
                        ServiceFrameworkMetrics.MODULES_METRIC_NAME);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .allMatch(
                        tag ->
                                tag.getKey().equals("status")
                                        && SetValues.STATUSES.contains(tag.getValue()));
        assertThat(registry.getMeters())
                .extracting(Meter::getId)
                .noneMatch(id -> id.toString().contains("sensitive-client-name"));
    }

    @Test
    void refreshesTheSampleOnlyAfterTheConfiguredCacheTtl() {
        CountingDiagnostics diagnostics = new CountingDiagnostics();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-27T12:00:00Z"));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ServiceFrameworkMetrics(diagnostics, clock, Duration.ofSeconds(5)).bindTo(registry);

        status(registry, "down");
        components(registry, "up");
        assertThat(diagnostics.snapshotCalls).hasValue(1);

        clock.advance(Duration.ofSeconds(4));
        registry.get(ServiceFrameworkMetrics.MODULES_METRIC_NAME).gauge().value();
        assertThat(diagnostics.snapshotCalls).hasValue(1);

        clock.advance(Duration.ofSeconds(1));
        status(registry, "down");
        assertThat(diagnostics.snapshotCalls).hasValue(2);
        assertThat(diagnostics.moduleCalls).hasValue(2);
    }

    @Test
    void convertsDiagnosticsFailuresToSafeUnknownAndZeroValues() {
        FrameworkDiagnostics failingDiagnostics =
                new FrameworkDiagnostics() {
                    @Override
                    public FrameworkDiagnosticsSnapshot snapshot() {
                        throw new IllegalStateException("secret failure");
                    }

                    @Override
                    public List<FrameworkModuleInfo> modules() {
                        throw new IllegalStateException("secret failure");
                    }
                };
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ServiceFrameworkMetrics(failingDiagnostics, Clock.systemUTC(), Duration.ofSeconds(10))
                .bindTo(registry);

        assertThat(status(registry, "unknown")).isEqualTo(1.0);
        assertThat(status(registry, "up")).isZero();
        assertThat(components(registry, "unknown")).isZero();
        assertThat(registry.get(ServiceFrameworkMetrics.MODULES_METRIC_NAME).gauge().value())
                .isZero();
    }

    private static double status(SimpleMeterRegistry registry, String status) {
        return registry.get(ServiceFrameworkMetrics.STATUS_METRIC_NAME)
                .tag("status", status)
                .gauge()
                .value();
    }

    private static double components(SimpleMeterRegistry registry, String status) {
        return registry.get(ServiceFrameworkMetrics.COMPONENTS_METRIC_NAME)
                .tag("status", status)
                .gauge()
                .value();
    }

    private static final class SetValues {
        private static final java.util.Set<String> STATUSES =
                java.util.Set.of("up", "down", "out_of_service", "unknown");

        private SetValues() {}
    }

    private static final class CountingDiagnostics implements FrameworkDiagnostics {

        private final AtomicInteger snapshotCalls = new AtomicInteger();
        private final AtomicInteger moduleCalls = new AtomicInteger();

        @Override
        public FrameworkDiagnosticsSnapshot snapshot() {
            snapshotCalls.incrementAndGet();
            return new FrameworkDiagnosticsSnapshot(
                    Instant.parse("2026-07-27T12:00:00Z"),
                    Map.of(
                            "available", ComponentHealth.up("available"),
                            "failed", ComponentHealth.down("failed"),
                            "maintenance", ComponentHealth.outOfService("maintenance"),
                            "sensitive-client-name",
                                    ComponentHealth.unknown("sensitive-client-name")));
        }

        @Override
        public List<FrameworkModuleInfo> modules() {
            moduleCalls.incrementAndGet();
            return List.of(
                    FrameworkModuleInfo.of("logging", "1.0.0"),
                    FrameworkModuleInfo.of("rest-client", "1.0.0"));
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
