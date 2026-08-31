package com.smbtech.serviceframework.actuator.port.in;

import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import java.time.Clock;
import java.util.Collection;
import java.util.List;

/** Defines the inbound contract for framework diagnostics. */
public interface FrameworkDiagnostics {

    /**
     * Creates the default diagnostics aggregator.
     *
     * @param probes component diagnostic probes
     * @param moduleInfoProviders framework module information providers
     * @param clock clock used to timestamp snapshots
     * @return default framework diagnostics
     */
    static FrameworkDiagnostics from(
            Collection<? extends DiagnosticProbe> probes,
            Collection<? extends FrameworkModuleInfoProvider> moduleInfoProviders,
            Clock clock) {
        return new DefaultFrameworkDiagnostics(probes, moduleInfoProviders, clock);
    }

    /**
     * Captures the current diagnostic state.
     *
     * @return immutable diagnostics snapshot
     */
    FrameworkDiagnosticsSnapshot snapshot();

    /**
     * Returns the available framework module information.
     *
     * @return immutable module information ordered by module name
     */
    List<FrameworkModuleInfo> modules();
}
