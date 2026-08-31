package com.smbtech.serviceframework.actuator.port.in;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides deterministic aggregation and failure isolation for neutral diagnostic ports.
 *
 * <p><strong>Implementation note:</strong> This is the framework default implementation. It is
 * thread-safe when the supplied probes and providers are thread-safe.
 */
final class DefaultFrameworkDiagnostics implements FrameworkDiagnostics {

    private static final String REASON = "reason";

    private final List<RegisteredProbe> probes;
    private final List<RegisteredModuleInfoProvider> moduleInfoProviders;
    private final Clock clock;

    /**
     * Creates the default diagnostics service.
     *
     * @param probes component diagnostic probes
     * @param moduleInfoProviders framework module information providers
     * @param clock clock used to timestamp snapshots
     */
    DefaultFrameworkDiagnostics(
            Collection<? extends DiagnosticProbe> probes,
            Collection<? extends FrameworkModuleInfoProvider> moduleInfoProviders,
            Clock clock) {
        this.probes = copyProbes(probes);
        this.moduleInfoProviders = copyModuleInfoProviders(moduleInfoProviders);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public FrameworkDiagnosticsSnapshot snapshot() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        probes.forEach(probe -> components.put(probe.name(), safelyCheck(probe)));
        return new FrameworkDiagnosticsSnapshot(clock.instant(), components);
    }

    @Override
    public List<FrameworkModuleInfo> modules() {
        List<FrameworkModuleInfo> modules = new ArrayList<>();
        moduleInfoProviders.forEach(
                provider -> {
                    FrameworkModuleInfo module = safelyProvide(provider);
                    if (module != null) {
                        modules.add(module);
                    }
                });
        return List.copyOf(modules);
    }

    private ComponentHealth safelyCheck(RegisteredProbe registeredProbe) {
        String componentName = registeredProbe.name();
        try {
            ComponentHealth result = registeredProbe.probe().check();
            if (result == null) {
                return ComponentHealth.unknown(componentName, Map.of(REASON, "no_result"));
            }
            if (!componentName.equals(result.name())) {
                return ComponentHealth.unknown(
                        componentName, Map.of(REASON, "invalid_component_name"));
            }
            return result;
        } catch (RuntimeException ignored) {
            return ComponentHealth.unknown(componentName, Map.of(REASON, "probe_failed"));
        }
    }

    private FrameworkModuleInfo safelyProvide(RegisteredModuleInfoProvider registeredProvider) {
        try {
            FrameworkModuleInfo result = registeredProvider.provider().provide();
            return result != null && registeredProvider.name().equals(result.name())
                    ? result
                    : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static List<RegisteredProbe> copyProbes(Collection<? extends DiagnosticProbe> values) {
        List<RegisteredProbe> copy = new ArrayList<>();
        Map<String, RegisteredProbe> byName = new LinkedHashMap<>();
        Objects.requireNonNull(values, "probes")
                .forEach(
                        probe -> {
                            DiagnosticProbe required = Objects.requireNonNull(probe, "probe");
                            String name = normalizeName(required.componentName(), "component");
                            if (byName.put(name, new RegisteredProbe(name, required)) != null) {
                                throw new IllegalArgumentException(
                                        "Duplicate diagnostic probe: " + name);
                            }
                        });
        copy.addAll(byName.values());
        copy.sort(Comparator.comparing(RegisteredProbe::name));
        return Collections.unmodifiableList(copy);
    }

    private static List<RegisteredModuleInfoProvider> copyModuleInfoProviders(
            Collection<? extends FrameworkModuleInfoProvider> values) {
        List<RegisteredModuleInfoProvider> copy = new ArrayList<>();
        Map<String, RegisteredModuleInfoProvider> byName = new LinkedHashMap<>();
        Objects.requireNonNull(values, "moduleInfoProviders")
                .forEach(
                        provider -> {
                            FrameworkModuleInfoProvider required =
                                    Objects.requireNonNull(provider, "moduleInfoProvider");
                            String name = normalizeName(required.moduleName(), "module");
                            if (byName.put(name, new RegisteredModuleInfoProvider(name, required))
                                    != null) {
                                throw new IllegalArgumentException(
                                        "Duplicate module information provider: " + name);
                            }
                        });
        copy.addAll(byName.values());
        copy.sort(Comparator.comparing(RegisteredModuleInfoProvider::name));
        return Collections.unmodifiableList(copy);
    }

    private static String normalizeName(String value, String type) {
        String normalized = Objects.requireNonNull(value, type + " name").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(type + " name must not be blank");
        }
        if (!normalized.equals(value)) {
            throw new IllegalArgumentException(type + " name must already be normalized");
        }
        return normalized;
    }

    private record RegisteredProbe(String name, DiagnosticProbe probe) {}

    private record RegisteredModuleInfoProvider(
            String name, FrameworkModuleInfoProvider provider) {}
}
