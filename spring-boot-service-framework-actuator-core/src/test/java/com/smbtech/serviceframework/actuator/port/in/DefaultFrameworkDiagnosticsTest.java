package com.smbtech.serviceframework.actuator.port.in;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.ComponentStatus;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class DefaultFrameworkDiagnosticsTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void aggregatesProbesInDeterministicOrder() {
        DefaultFrameworkDiagnostics diagnostics =
                new DefaultFrameworkDiagnostics(
                        List.of(
                                probe("zeta", () -> ComponentHealth.up("zeta")),
                                probe("alpha", () -> ComponentHealth.down("alpha"))),
                        List.of(),
                        CLOCK);

        FrameworkDiagnosticsSnapshot snapshot = diagnostics.snapshot();

        assertEquals(Instant.parse("2026-07-27T12:00:00Z"), snapshot.capturedAt());
        assertEquals(List.of("alpha", "zeta"), List.copyOf(snapshot.components().keySet()));
        assertEquals(ComponentStatus.DOWN, snapshot.status());
    }

    @Test
    void isolatesProbeFailuresAndInvalidResultsWithoutLeakingExceptions() {
        DefaultFrameworkDiagnostics diagnostics =
                new DefaultFrameworkDiagnostics(
                        List.of(
                                probe(
                                        "failure",
                                        () -> {
                                            throw new IllegalStateException(
                                                    "secret exception message");
                                        }),
                                probe("missing", () -> null),
                                probe("mismatch", () -> ComponentHealth.up("other"))),
                        List.of(),
                        CLOCK);

        FrameworkDiagnosticsSnapshot snapshot = diagnostics.snapshot();

        assertEquals(
                Map.of("reason", "probe_failed"), snapshot.components().get("failure").details());
        assertEquals(Map.of("reason", "no_result"), snapshot.components().get("missing").details());
        assertEquals(
                Map.of("reason", "invalid_component_name"),
                snapshot.components().get("mismatch").details());
    }

    @Test
    void returnsOnlyValidModuleInformationInDeterministicOrder() {
        DefaultFrameworkDiagnostics diagnostics =
                new DefaultFrameworkDiagnostics(
                        List.of(),
                        List.of(
                                provider("zeta", () -> FrameworkModuleInfo.of("zeta", "1.0.0")),
                                provider(
                                        "failure",
                                        () -> {
                                            throw new IllegalStateException("failure");
                                        }),
                                provider("missing", () -> null),
                                provider(
                                        "mismatch", () -> FrameworkModuleInfo.of("other", "1.0.0")),
                                provider("alpha", () -> FrameworkModuleInfo.of("alpha", "1.0.0"))),
                        CLOCK);

        assertEquals(
                List.of("alpha", "zeta"),
                diagnostics.modules().stream().map(FrameworkModuleInfo::name).toList());
    }

    @Test
    void rejectsDuplicateOrUnnormalizedPortNames() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DefaultFrameworkDiagnostics(
                                List.of(
                                        probe("duplicate", () -> ComponentHealth.up("duplicate")),
                                        probe("duplicate", () -> ComponentHealth.up("duplicate"))),
                                List.of(),
                                CLOCK));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DefaultFrameworkDiagnostics(
                                List.of(probe(" invalid ", () -> ComponentHealth.up("invalid"))),
                                List.of(),
                                CLOCK));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DefaultFrameworkDiagnostics(
                                List.of(),
                                List.of(
                                        provider(
                                                "duplicate",
                                                () -> FrameworkModuleInfo.of("duplicate", "1.0.0")),
                                        provider(
                                                "duplicate",
                                                () ->
                                                        FrameworkModuleInfo.of(
                                                                "duplicate", "1.0.0"))),
                                CLOCK));
    }

    private static DiagnosticProbe probe(String name, Supplier<ComponentHealth> healthSupplier) {
        return new DiagnosticProbe() {
            @Override
            public String componentName() {
                return name;
            }

            @Override
            public ComponentHealth check() {
                return healthSupplier.get();
            }
        };
    }

    private static FrameworkModuleInfoProvider provider(
            String name, Supplier<FrameworkModuleInfo> moduleSupplier) {
        return new FrameworkModuleInfoProvider() {
            @Override
            public String moduleName() {
                return name;
            }

            @Override
            public FrameworkModuleInfo provide() {
                return moduleSupplier.get();
            }
        };
    }
}
