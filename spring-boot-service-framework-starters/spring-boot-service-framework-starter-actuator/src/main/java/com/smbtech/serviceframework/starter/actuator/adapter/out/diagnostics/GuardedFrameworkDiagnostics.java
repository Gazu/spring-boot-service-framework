package com.smbtech.serviceframework.starter.actuator.adapter.out.diagnostics;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.ComponentStatus;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;

/**
 * Adds bounded execution, caching, and failure isolation to framework diagnostics.
 *
 * <p>The guard uses two daemon workers and no task queue. Snapshot and module refreshes are
 * independently single-flight, so concurrent readers reuse the same bounded result.
 */
public final class GuardedFrameworkDiagnostics implements FrameworkDiagnostics, AutoCloseable {

    /** Maximum configurable number of component results. */
    public static final int MAX_COMPONENT_LIMIT = 256;

    /** Maximum configurable number of module results. */
    public static final int MAX_MODULE_LIMIT = 256;

    private static final String FAILURE_COMPONENT = "service-framework-diagnostics";
    private static final int MAX_CONCURRENT_TASKS = 2;

    private final FrameworkDiagnostics delegate;
    private final Clock clock;
    private final long cacheTtlNanos;
    private final long operationTimeoutNanos;
    private final int maxComponents;
    private final int maxModules;
    private final LongSupplier ticker;
    private final ThreadPoolExecutor executor;
    private final Object snapshotMonitor = new Object();
    private final Object modulesMonitor = new Object();
    private volatile CacheEntry<FrameworkDiagnosticsSnapshot> snapshotCache;
    private volatile CacheEntry<List<FrameworkModuleInfo>> modulesCache;

    /**
     * Creates a guarded diagnostics service.
     *
     * @param delegate diagnostics implementation to protect
     * @param clock clock used for safe fallback snapshots
     * @param cacheTtl duration for which successful or safe fallback results are reused
     * @param operationTimeout maximum duration of one snapshot or module operation
     * @param maxComponents maximum component results retained in a snapshot
     * @param maxModules maximum module results retained
     */
    public GuardedFrameworkDiagnostics(
            FrameworkDiagnostics delegate,
            Clock clock,
            Duration cacheTtl,
            Duration operationTimeout,
            int maxComponents,
            int maxModules) {
        this(
                delegate,
                clock,
                cacheTtl,
                operationTimeout,
                maxComponents,
                maxModules,
                System::nanoTime);
    }

    GuardedFrameworkDiagnostics(
            FrameworkDiagnostics delegate,
            Clock clock,
            Duration cacheTtl,
            Duration operationTimeout,
            int maxComponents,
            int maxModules,
            LongSupplier ticker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.cacheTtlNanos = cacheTtlNanos(cacheTtl);
        this.operationTimeoutNanos = operationTimeoutNanos(operationTimeout);
        this.maxComponents = boundedLimit(maxComponents, MAX_COMPONENT_LIMIT, "maxComponents");
        this.maxModules = boundedLimit(maxModules, MAX_MODULE_LIMIT, "maxModules");
        this.ticker = Objects.requireNonNull(ticker, "ticker must not be null");
        this.executor = diagnosticsExecutor();
    }

    @Override
    public FrameworkDiagnosticsSnapshot snapshot() {
        long now = ticker.getAsLong();
        CacheEntry<FrameworkDiagnosticsSnapshot> current = snapshotCache;
        if (isCurrent(current, now)) {
            return current.value();
        }

        synchronized (snapshotMonitor) {
            now = ticker.getAsLong();
            current = snapshotCache;
            if (isCurrent(current, now)) {
                return current.value();
            }
            FrameworkDiagnosticsSnapshot refreshed = refreshSnapshot();
            snapshotCache = new CacheEntry<>(refreshed, ticker.getAsLong());
            return refreshed;
        }
    }

    @Override
    public List<FrameworkModuleInfo> modules() {
        long now = ticker.getAsLong();
        CacheEntry<List<FrameworkModuleInfo>> current = modulesCache;
        if (isCurrent(current, now)) {
            return current.value();
        }

        synchronized (modulesMonitor) {
            now = ticker.getAsLong();
            current = modulesCache;
            if (isCurrent(current, now)) {
                return current.value();
            }
            List<FrameworkModuleInfo> refreshed = refreshModules();
            modulesCache = new CacheEntry<>(refreshed, ticker.getAsLong());
            return refreshed;
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private FrameworkDiagnosticsSnapshot refreshSnapshot() {
        TimedResult<FrameworkDiagnosticsSnapshot> result = execute(delegate::snapshot);
        if (!result.success()) {
            return failureSnapshot(result.reason());
        }
        if (result.value() == null) {
            return failureSnapshot("no_result");
        }
        try {
            return boundedSnapshot(result.value());
        } catch (RuntimeException ignored) {
            return failureSnapshot("invalid_result");
        }
    }

    private List<FrameworkModuleInfo> refreshModules() {
        TimedResult<List<FrameworkModuleInfo>> result = execute(delegate::modules);
        if (!result.success() || result.value() == null) {
            return List.of();
        }
        try {
            int resultSize = Math.min(result.value().size(), maxModules);
            List<FrameworkModuleInfo> modules = new ArrayList<>(resultSize);
            for (int index = 0; index < resultSize; index++) {
                modules.add(Objects.requireNonNull(result.value().get(index), "module"));
            }
            return List.copyOf(modules);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private FrameworkDiagnosticsSnapshot boundedSnapshot(FrameworkDiagnosticsSnapshot snapshot) {
        if (snapshot.components().size() <= maxComponents) {
            return snapshot;
        }

        ComponentStatus aggregateStatus = snapshot.status();
        List<Map.Entry<String, ComponentHealth>> entries =
                new ArrayList<>(snapshot.components().entrySet());
        Map<String, ComponentHealth> selected = new LinkedHashMap<>();
        entries.stream()
                .limit(maxComponents)
                .forEach(entry -> selected.put(entry.getKey(), entry.getValue()));

        boolean aggregateStatusRetained =
                selected.values().stream()
                        .anyMatch(component -> component.status() == aggregateStatus);
        if (!aggregateStatusRetained) {
            Map.Entry<String, ComponentHealth> aggregateComponent =
                    entries.stream()
                            .filter(entry -> entry.getValue().status() == aggregateStatus)
                            .findFirst()
                            .orElseThrow();
            String lastSelected =
                    selected.keySet().stream().reduce((left, right) -> right).orElseThrow();
            selected.remove(lastSelected);
            selected.put(aggregateComponent.getKey(), aggregateComponent.getValue());
        }

        return new FrameworkDiagnosticsSnapshot(snapshot.capturedAt(), selected);
    }

    private FrameworkDiagnosticsSnapshot failureSnapshot(String reason) {
        Instant capturedAt;
        try {
            capturedAt = clock.instant();
        } catch (RuntimeException ignored) {
            capturedAt = Instant.EPOCH;
        }
        ComponentHealth failure =
                ComponentHealth.unknown(FAILURE_COMPONENT, Map.of("reason", reason));
        return new FrameworkDiagnosticsSnapshot(capturedAt, Map.of(FAILURE_COMPONENT, failure));
    }

    private <T> TimedResult<T> execute(Callable<T> task) {
        Future<T> future;
        try {
            future = executor.submit(task);
        } catch (RejectedExecutionException ignored) {
            return TimedResult.failure("execution_saturated");
        }

        try {
            return TimedResult.success(future.get(operationTimeoutNanos, TimeUnit.NANOSECONDS));
        } catch (TimeoutException ignored) {
            future.cancel(true);
            return TimedResult.failure("execution_timeout");
        } catch (InterruptedException ignored) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            return TimedResult.failure("execution_interrupted");
        } catch (ExecutionException | RuntimeException ignored) {
            return TimedResult.failure("execution_failed");
        }
    }

    private boolean isCurrent(CacheEntry<?> entry, long now) {
        if (entry == null || cacheTtlNanos == 0) {
            return false;
        }
        long age = now - entry.createdAtNanos();
        return age >= 0 && age < cacheTtlNanos;
    }

    private static long cacheTtlNanos(Duration value) {
        Duration duration = Objects.requireNonNull(value, "cacheTtl must not be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("cacheTtl must not be negative");
        }
        return toNanos(duration);
    }

    private static long operationTimeoutNanos(Duration value) {
        Duration duration = Objects.requireNonNull(value, "operationTimeout must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("operationTimeout must be positive");
        }
        return toNanos(duration);
    }

    private static long toNanos(Duration duration) {
        try {
            return duration.toNanos();
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static int boundedLimit(int value, int maximum, String name) {
        if (value < 1 || value > maximum) {
            throw new IllegalArgumentException(name + " must be between 1 and " + maximum);
        }
        return value;
    }

    private static ThreadPoolExecutor diagnosticsExecutor() {
        ThreadFactory threadFactory =
                Thread.ofPlatform()
                        .daemon(true)
                        .name("smbtech-framework-diagnostics-", 0)
                        .factory();
        return new ThreadPoolExecutor(
                0,
                MAX_CONCURRENT_TASKS,
                30,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    private record CacheEntry<T>(T value, long createdAtNanos) {}

    private record TimedResult<T>(T value, String reason) {

        private static <T> TimedResult<T> success(T value) {
            return new TimedResult<>(value, null);
        }

        private static <T> TimedResult<T> failure(String reason) {
            return new TimedResult<>(null, reason);
        }

        private boolean success() {
            return reason == null;
        }
    }
}
