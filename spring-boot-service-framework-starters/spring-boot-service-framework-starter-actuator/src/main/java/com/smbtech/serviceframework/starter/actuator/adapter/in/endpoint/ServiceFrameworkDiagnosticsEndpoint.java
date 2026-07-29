package com.smbtech.serviceframework.starter.actuator.adapter.in.endpoint;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/** Read-only Actuator endpoint for bounded Service Framework diagnostics. */
@Endpoint(id = "serviceframework", defaultAccess = Access.NONE)
public final class ServiceFrameworkDiagnosticsEndpoint {

    private final FrameworkDiagnostics diagnostics;

    /**
     * Creates the Service Framework diagnostics endpoint.
     *
     * @param diagnostics framework diagnostics use case
     */
    public ServiceFrameworkDiagnosticsEndpoint(FrameworkDiagnostics diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Returns the current framework diagnostics and module information.
     *
     * @return bounded diagnostics response
     */
    @ReadOperation
    public Map<String, Object> diagnostics() {
        try {
            FrameworkDiagnosticsSnapshot snapshot = diagnostics.snapshot();
            List<FrameworkModuleInfo> modules = diagnostics.modules();
            if (snapshot == null || modules == null) {
                return unavailable("no_result");
            }
            return response(snapshot, modules);
        } catch (RuntimeException ignored) {
            return unavailable("diagnostics_failed");
        }
    }

    private static Map<String, Object> response(
            FrameworkDiagnosticsSnapshot snapshot, List<FrameworkModuleInfo> modules) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("capturedAt", snapshot.capturedAt().toString());
        response.put("status", snapshot.status().name());
        response.put("componentCount", snapshot.components().size());
        response.put("components", componentDetails(snapshot));
        response.put("moduleCount", modules.size());
        response.put("modules", moduleDetails(modules));
        return Collections.unmodifiableMap(response);
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

    private static List<Map<String, Object>> moduleDetails(List<FrameworkModuleInfo> modules) {
        List<Map<String, Object>> details = new ArrayList<>(modules.size());
        for (FrameworkModuleInfo module : modules) {
            FrameworkModuleInfo required = Objects.requireNonNull(module, "module");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", required.name());
            item.put("version", required.version());
            if (!required.attributes().isEmpty()) {
                item.put("attributes", required.attributes());
            }
            details.add(Collections.unmodifiableMap(item));
        }
        return Collections.unmodifiableList(details);
    }

    private static Map<String, Object> unavailable(String reason) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UNKNOWN");
        response.put("componentCount", 0);
        response.put("components", Map.of());
        response.put("moduleCount", 0);
        response.put("modules", List.of());
        response.put("reason", reason);
        return Collections.unmodifiableMap(response);
    }
}
