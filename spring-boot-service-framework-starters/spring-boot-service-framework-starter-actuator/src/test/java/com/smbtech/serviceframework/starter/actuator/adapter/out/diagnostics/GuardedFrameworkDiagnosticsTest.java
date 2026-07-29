package com.smbtech.serviceframework.starter.actuator.adapter.out.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.ComponentStatus;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class GuardedFrameworkDiagnosticsTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void cachesSnapshotAndModuleRefreshesIndependently() {
        AtomicInteger snapshotCalls = new AtomicInteger();
        AtomicInteger moduleCalls = new AtomicInteger();
        AtomicLong ticker = new AtomicLong();
        FrameworkDiagnostics delegate =
                diagnostics(
                        () -> {
                            snapshotCalls.incrementAndGet();
                            return snapshot(ComponentHealth.up("component"));
                        },
                        () -> {
                            moduleCalls.incrementAndGet();
                            return List.of(FrameworkModuleInfo.of("logging", "1.0.0"));
                        });

        try (GuardedFrameworkDiagnostics guarded =
                guarded(delegate, Duration.ofSeconds(5), Duration.ofSeconds(1), 64, 64, ticker)) {
            guarded.snapshot();
            guarded.snapshot();
            guarded.modules();
            guarded.modules();

            assertThat(snapshotCalls).hasValue(1);
            assertThat(moduleCalls).hasValue(1);

            ticker.addAndGet(Duration.ofSeconds(5).toNanos());
            guarded.snapshot();
            guarded.modules();

            assertThat(snapshotCalls).hasValue(2);
            assertThat(moduleCalls).hasValue(2);
        }
    }

    @Test
    void appliesSingleFlightRefreshForConcurrentReaders() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        FrameworkDiagnostics delegate =
                diagnostics(
                        () -> {
                            calls.incrementAndGet();
                            Thread.sleep(50);
                            return snapshot(ComponentHealth.up("component"));
                        },
                        List::of);

        try (GuardedFrameworkDiagnostics guarded =
                        new GuardedFrameworkDiagnostics(
                                delegate,
                                CLOCK,
                                Duration.ofSeconds(5),
                                Duration.ofSeconds(1),
                                64,
                                64);
                var callers = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<FrameworkDiagnosticsSnapshot>> tasks = new ArrayList<>();
            for (int index = 0; index < 20; index++) {
                tasks.add(guarded::snapshot);
            }

            assertThat(callers.invokeAll(tasks))
                    .allSatisfy(
                            future ->
                                    assertThat(future.get().status())
                                            .isEqualTo(ComponentStatus.UP));
            assertThat(calls).hasValue(1);
        }
    }

    @Test
    void convertsTimeoutsAndFailuresToStaticSafeResults() {
        AtomicInteger timeoutCalls = new AtomicInteger();
        FrameworkDiagnostics timeoutDelegate =
                diagnostics(
                        () -> {
                            timeoutCalls.incrementAndGet();
                            Thread.sleep(Duration.ofSeconds(5));
                            return snapshot(ComponentHealth.up("component"));
                        },
                        List::of);
        FrameworkDiagnostics failingDelegate =
                diagnostics(
                        () -> {
                            throw new IllegalStateException("secret failure");
                        },
                        () -> {
                            throw new IllegalStateException("secret failure");
                        });

        try (GuardedFrameworkDiagnostics timeout =
                        new GuardedFrameworkDiagnostics(
                                timeoutDelegate,
                                CLOCK,
                                Duration.ofSeconds(5),
                                Duration.ofMillis(20),
                                64,
                                64);
                GuardedFrameworkDiagnostics failing =
                        new GuardedFrameworkDiagnostics(
                                failingDelegate,
                                CLOCK,
                                Duration.ofSeconds(5),
                                Duration.ofSeconds(1),
                                64,
                                64)) {
            assertThat(
                            timeout.snapshot()
                                    .components()
                                    .get("service-framework-diagnostics")
                                    .details())
                    .containsEntry("reason", "execution_timeout");
            timeout.snapshot();
            assertThat(timeoutCalls).hasValue(1);
            assertThat(
                            failing.snapshot()
                                    .components()
                                    .get("service-framework-diagnostics")
                                    .details())
                    .containsEntry("reason", "execution_failed");
            assertThat(failing.modules()).isEmpty();
            assertThat(failing.snapshot().toString()).doesNotContain("secret failure");
        }
    }

    @Test
    void boundsPayloadsAndPreservesTheOriginalWorstStatus() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("alpha", ComponentHealth.up("alpha"));
        components.put("beta", ComponentHealth.up("beta"));
        components.put("zeta", ComponentHealth.down("zeta"));
        FrameworkDiagnostics delegate =
                diagnostics(
                        () -> new FrameworkDiagnosticsSnapshot(CLOCK.instant(), components),
                        () ->
                                List.of(
                                        FrameworkModuleInfo.of("first", "1.0.0"),
                                        FrameworkModuleInfo.of("second", "1.0.0")));

        try (GuardedFrameworkDiagnostics guarded =
                new GuardedFrameworkDiagnostics(
                        delegate, CLOCK, Duration.ofSeconds(5), Duration.ofSeconds(1), 2, 1)) {
            FrameworkDiagnosticsSnapshot result = guarded.snapshot();

            assertThat(result.components()).containsOnlyKeys("alpha", "zeta");
            assertThat(result.status()).isEqualTo(ComponentStatus.DOWN);
            assertThat(guarded.modules())
                    .extracting(FrameworkModuleInfo::name)
                    .containsExactly("first");
        }
    }

    @Test
    void validatesSecurityAndPerformanceBounds() {
        FrameworkDiagnostics delegate =
                diagnostics(() -> snapshot(ComponentHealth.up("component")), List::of);

        assertThatThrownBy(
                        () ->
                                new GuardedFrameworkDiagnostics(
                                        delegate,
                                        CLOCK,
                                        Duration.ofSeconds(-1),
                                        Duration.ofSeconds(1),
                                        64,
                                        64))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cacheTtl must not be negative");
        assertThatThrownBy(
                        () ->
                                new GuardedFrameworkDiagnostics(
                                        delegate, CLOCK, Duration.ZERO, Duration.ZERO, 64, 64))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operationTimeout must be positive");
        assertThatThrownBy(
                        () ->
                                new GuardedFrameworkDiagnostics(
                                        delegate,
                                        CLOCK,
                                        Duration.ZERO,
                                        Duration.ofSeconds(1),
                                        GuardedFrameworkDiagnostics.MAX_COMPONENT_LIMIT + 1,
                                        64))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxComponents must be between");
    }

    private static GuardedFrameworkDiagnostics guarded(
            FrameworkDiagnostics delegate,
            Duration cacheTtl,
            Duration timeout,
            int maxComponents,
            int maxModules,
            AtomicLong ticker) {
        return new GuardedFrameworkDiagnostics(
                delegate, CLOCK, cacheTtl, timeout, maxComponents, maxModules, ticker::get);
    }

    private static FrameworkDiagnosticsSnapshot snapshot(ComponentHealth component) {
        return new FrameworkDiagnosticsSnapshot(
                CLOCK.instant(), Map.of(component.name(), component));
    }

    private static FrameworkDiagnostics diagnostics(
            ThrowingSupplier<FrameworkDiagnosticsSnapshot> snapshot,
            ThrowingSupplier<List<FrameworkModuleInfo>> modules) {
        return new FrameworkDiagnostics() {
            @Override
            public FrameworkDiagnosticsSnapshot snapshot() {
                return snapshot.get();
            }

            @Override
            public List<FrameworkModuleInfo> modules() {
                return modules.get();
            }
        };
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {

        T supply() throws Exception;

        default T get() {
            try {
                return supply();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
