package com.smbtech.serviceframework.actuator.port.in;

import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import java.util.List;

/** Defines the inbound contract for framework diagnostics. */
public interface FrameworkDiagnostics {

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
